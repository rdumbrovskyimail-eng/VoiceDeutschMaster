package com.voicedeutsch.master.voicecore.engine

import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeBackend
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.LiveContentResponse
import com.google.firebase.ai.type.LiveGenerativeModel
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.Transcription
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import com.voicedeutsch.master.voicecore.context.ContextBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * GeminiClient — обёртка над Firebase AI Logic Live API SDK.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * МИГРАЦИЯ: Ktor WebSocket → firebase-ai LiveSession
 * ════════════════════════════════════════════════════════════════════════════
 *
 * БЫЛО (кастомный WebSocket):
 *   wss://generativelanguage.googleapis.com/ws/...BidiGenerateContent?key=<ephemeral_token>
 *   - Ручной парсинг JSON фреймов (BidiGenerateContentSetup, serverContent, toolCall…)
 *   - Ручной Base64 encode/decode аудио
 *   - Ручное управление setupComplete deferred
 *   - EphemeralTokenService для получения ключа
 *   - Ktor HttpClient + WebSocketSession
 *
 * СТАЛО (firebase-ai SDK):
 *   Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(...)
 *   - SDK управляет WebSocket соединением, хэндшейком, переподключением
 *   - SDK управляет App Check токенами (прозрачно, без EphemeralTokenService)
 *   - SDK парсит все типы серверных сообщений
 *   - SDK управляет аудио-буферизацией и форматом PCM
 *   - startAudioConversation() — встроенная петля запись→отправка→приём→воспроизведение
 *   - receive() Flow — ручное управление аудио-данными, если нужен полный контроль
 *
 * ════════════════════════════════════════════════════════════════════════════
 * МОДЕЛЬ (актуально на февраль 2026):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   gemini-2.5-flash-native-audio-preview-12-2025
 *   ├── Native Audio: голос генерируется напрямую, без TTS постобработки
 *   ├── Thinking Levels: поддержка budgetTokens для reasoning
 *   ├── Input/Output transcription: встроенные субтитры
 *   ├── Voice activity detection: автоматическое определение конца фразы
 *   └── Function calling: синхронный в рамках LiveSession
 *
 * ════════════════════════════════════════════════════════════════════════════
 * АУДИО ФОРМАТ:
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   Вход:  PCM 16-bit, 16 kHz, mono  → sendRealtimeInput(audioData, mimeType)
 *   Выход: PCM 16-bit, 24 kHz, mono  ← LiveContentResponse.data (ByteArray)
 *
 * ════════════════════════════════════════════════════════════════════════════
 * THREAD SAFETY:
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   connect() / disconnect()   — последовательно, под lifecycleMutex в Engine
 *   sendAudioChunk()           — thread-safe, suspend
 *   sendText()                 — thread-safe, suspend
 *   receiveFlow()              — cold Flow, один подписчик за раз
 *   functionCallHandler        — вызывается из SDK потока, должен быть быстрым
 *
 * @param config  конфигурация модели (model name, voice, sample rates и т.д.)
 * @param json    экземпляр Json для сериализации function declarations из FunctionRouter
 */
// ✅ ИСПРАВЛЕНО: правильный пакет firebase-ai (был vertexai — старый пакет)
@OptIn(com.google.firebase.ai.type.internal.InternalFirebaseAiAPI::class)
class GeminiClient(
    private val config: GeminiConfig,
    private val json: Json,
) {
    companion object {
        private const val TAG = "GeminiClient"

        // Актуальная модель на февраль 2026: нативный аудио, Thinking, Live API
        private const val DEFAULT_MODEL = "gemini-2.5-flash-native-audio-preview-12-2025"

        // PCM формат входного аудио: 16-bit signed, 16 kHz, mono
        private const val AUDIO_INPUT_MIME = "audio/pcm;rate=16000"
    }

    // ── Состояние ─────────────────────────────────────────────────────────────

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var liveSession: LiveSession? = null
    private var audioConversationJob: Job? = null

    // Канал для передачи GeminiResponse в VoiceCoreEngineImpl.
    // Заполняется либо через receiveFlow() (ручной режим),
    // либо через transcriptHandler из startAudioConversation() (автоматический режим).
    private val responseChannel = Channel<GeminiResponse>(Channel.UNLIMITED)

    // ── Инициализация модели ──────────────────────────────────────────────────

    /**
     * Создаёт LiveGenerativeModel из firebase-ai SDK.
     *
     * Вызывается при каждом connect() — позволяет передавать актуальный
     * SessionContext (systemInstruction + functionDeclarations) без пересоздания
     * всего DI-графа.
     *
     * systemInstruction = context.fullContext:
     *   MasterPrompt + userContext + bookContext + strategyPrompt
     *   (объединён и оптимизирован в ContextBuilder)
     */
    private fun buildLiveModel(context: ContextBuilder.SessionContext): LiveGenerativeModel {

        // Function declarations: парсим из JSON-строк FunctionRouter → SDK-тип
        val functionDeclarations: List<FunctionDeclaration> = context.functionDeclarations
            .mapNotNull { declarationJson ->
                runCatching { parseFunctionDeclaration(declarationJson) }
                    .onFailure { Log.w(TAG, "Skipping invalid function declaration: ${it.message}") }
                    .getOrNull()
            }

        val tools: List<Tool>? = functionDeclarations
            .takeIf { it.isNotEmpty() }
            ?.let { listOf(Tool.functionDeclarations(it)) }

        val systemInstruction = content(role = "user") {
            text(context.fullContext)
        }

        return Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
            modelName = config.modelName.ifBlank { DEFAULT_MODEL },
            generationConfig = liveGenerationConfig {
                responseModality = ResponseModality.AUDIO

                speechConfig = SpeechConfig(
                    voice = Voice(config.voiceName)
                )

                // ✅ Встроенные транскрипции — не нужно парсить serverContent вручную.
                // input: что сказал пользователь (STT)
                // output: что ответил Gemini (TTS → text)
                inputAudioTranscription  = AudioTranscriptionConfig()
                outputAudioTranscription = AudioTranscriptionConfig()
            },
            tools = tools,
            systemInstruction = systemInstruction,
        )
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Устанавливает соединение с Gemini Live API через firebase-ai SDK.
     *
     * После connect() можно выбрать один из двух режимов работы:
     *
     *   A) [startManagedAudioConversation] — SDK сам захватывает микрофон,
     *      отправляет аудио и воспроизводит ответ. Функции обрабатываются через
     *      functionCallHandler. Подходит для простых сценариев.
     *
     *   B) [sendAudioChunk] + [receiveFlow] — ручное управление аудио-потоком.
     *      AudioPipeline сам управляет записью/воспроизведением через AudioRecorder
     *      и AudioPlayer. Функции обрабатываются в VoiceCoreEngineImpl.
     *      Используется в текущей архитектуре проекта.
     *
     * VoiceDeutschMaster использует режим B — полный контроль над аудио-пайплайном.
     */
    suspend fun connect(context: ContextBuilder.SessionContext) {
        try {
            Log.d(TAG, "Connecting to Gemini Live API [model=${config.modelName}]")
            val model = buildLiveModel(context)
            liveSession = model.connect()
            Log.d(TAG, "✅ LiveSession established")
        } catch (e: Exception) {
            Log.e(TAG, "❌ connect() failed: ${e.message}", e)
            throw GeminiConnectionException("Failed to connect to Gemini Live API", e)
        }
    }

    /**
     * Закрывает LiveSession и освобождает ресурсы.
     * SDK автоматически закрывает WebSocket соединение.
     */
    suspend fun disconnect() {
        try {
            audioConversationJob?.cancel()
            audioConversationJob = null
            liveSession?.close()
            liveSession = null
            responseChannel.close()
            Log.d(TAG, "LiveSession closed")
        } catch (e: Exception) {
            Log.w(TAG, "disconnect() warning: ${e.message}")
        }
    }

    /**
     * Отправляет chunk PCM-аудио в Gemini Live API.
     *
     * Формат: PCM 16-bit signed, 16 kHz, mono (совпадает с AudioRecorder.kt)
     * SDK сам кодирует в Base64 и формирует realtimeInput сообщение.
     *
     * Вызывается из AudioPipeline при каждом заполнении аудио-буфера.
     */
    suspend fun sendAudioChunk(pcmBytes: ByteArray) {
        val session = liveSession ?: run {
            Log.w(TAG, "sendAudioChunk: no active session, dropping chunk")
            return
        }
        runCatching {
            session.sendRealtimeInput(audioData = pcmBytes, mimeType = AUDIO_INPUT_MIME)
        }.onFailure { e ->
            Log.e(TAG, "sendAudioChunk error: ${e.message}", e)
        }
    }

    /**
     * Отправляет текстовое сообщение в Gemini Live API.
     *
     * Используется для:
     *   - Передачи команд смены стратегии из VoiceCoreEngine
     *   - Текстового ввода в гибридных сценариях (текст + голос)
     *   - Инициализации нового упражнения без аудио-ввода
     */
    suspend fun sendText(text: String, turnComplete: Boolean = true) {
        val session = liveSession ?: run {
            Log.w(TAG, "sendText: no active session")
            return
        }
        runCatching {
            session.sendText(text = text, turnComplete = turnComplete)
        }.onFailure { e ->
            Log.e(TAG, "sendText error: ${e.message}", e)
        }
    }

    /**
     * Cold Flow входящих ответов от Gemini.
     *
     * Собирает LiveContentResponse из session.receive() и маппит в GeminiResponse.
     * Flow завершается когда сессия закрывается или вызывается disconnect().
     *
     * Используется в VoiceCoreEngineImpl для обработки аудио и транскрипций
     * в рамках существующего пайплайна.
     *
     * Пример сбора в Engine:
     * ```kotlin
     * geminiClient.receiveFlow().collect { response ->
     *     when {
     *         response.hasAudio()        -> audioPlayer.enqueue(response.audioData!!)
     *         response.hasFunctionCall() -> functionRouter.handle(response.functionCall!!)
     *         response.isTurnComplete    -> onTurnComplete()
     *     }
     * }
     * ```
     */
    fun receiveFlow(): Flow<GeminiResponse> = flow {
        val session = liveSession
            ?: throw GeminiConnectionException("receiveFlow: no active session")

        try {
            session.receive().collect { liveResponse ->
                mapLiveResponse(liveResponse)?.let { emit(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "receiveFlow error: ${e.message}", e)
            throw e
        }
    }

    /**
     * Режим A: SDK управляет захватом микрофона и воспроизведением аудио.
     *
     * Используется если нужен минимальный код без ручного AudioPipeline.
     * В текущей архитектуре VoiceDeutschMaster НЕ используется — проект
     * использует режим B (sendAudioChunk + receiveFlow) для полного контроля.
     *
     * functionCallHandler: (FunctionCallPart) → FunctionResponsePart
     *   Вызывается из SDK потока синхронно — должен быть быстрым.
     *   Для тяжёлых операций используйте runBlocking { } или Channel.
     *
     * transcriptHandler: (input: Transcription?, output: Transcription?) → Unit
     *   Вызывается при получении транскрипций (input STT + output TTS→text).
     */
    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startManagedAudioConversation(
        functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)? = null,
        transcriptHandler: ((Transcription?, Transcription?) -> Unit)? = null,
    ) {
        val session = liveSession ?: run {
            Log.e(TAG, "startManagedAudioConversation: no active session")
            return
        }
        audioConversationJob = clientScope.launch {
            runCatching {
                session.startAudioConversation(
                    functionCallHandler = functionCallHandler,
                    transcriptHandler   = transcriptHandler,
                )
            }.onFailure { e ->
                Log.e(TAG, "startAudioConversation error: ${e.message}", e)
            }
        }
    }

    /**
     * Останавливает управляемую аудио-сессию (Режим A).
     * После остановки можно возобновить через startManagedAudioConversation().
     */
    fun stopManagedAudioConversation() {
        runCatching { liveSession?.stopAudioConversation() }
            .onFailure { Log.w(TAG, "stopAudioConversation warning: ${it.message}") }
        audioConversationJob?.cancel()
        audioConversationJob = null
    }

    /** Освобождает CoroutineScope. Вызывается из VoiceCoreEngine.release(). */
    fun release() {
        clientScope.cancel()
        responseChannel.close()
    }

    // ── Маппинг ответов ───────────────────────────────────────────────────────

    /**
     * Маппит LiveContentResponse → GeminiResponse.
     *
     * LiveContentResponse содержит:
     *   .data               — ByteArray PCM аудио (24 kHz, 16-bit, mono)
     *   .text               — текстовый контент (если responseModality = TEXT)
     *   .functionCalls      — список FunctionCallPart (function calling)
     *   .inputTranscription — Transcription? (что сказал пользователь)
     *   .outputTranscription — Transcription? (что ответил Gemini текстом)
     *   .isTurnComplete     — флаг завершения хода
     *   .isInterrupted      — пользователь перебил модель
     */
    private fun mapLiveResponse(response: LiveContentResponse): GeminiResponse? {
        val audioData     = response.data?.takeIf { it.isNotEmpty() }
        val functionCalls = response.functionCalls

        // Транскрипция: выход приоритетнее входа для отображения в UI
        val transcript = response.outputTranscription?.text
            ?: response.inputTranscription?.text
            ?: response.text

        // Если ответ полностью пустой — не эмитируем
        if (audioData == null && functionCalls.isEmpty() &&
            transcript.isNullOrEmpty() && !response.isTurnComplete && !response.isInterrupted) {
            return null
        }

        // Function calls: маппим первый (обычно один за раз в Live API)
        val functionCall = functionCalls.firstOrNull()?.let { call ->
            GeminiFunctionCall(
                id      = call.id ?: call.name, // Live API может не возвращать id
                name    = call.name,
                argsJson = json.encodeToString(
                    JsonObject.serializer(),
                    call.args?.let { JsonObject(it) } ?: buildJsonObject {}
                ),
            )
        }

        return GeminiResponse(
            audioData      = audioData,
            transcript     = transcript,
            functionCall   = functionCall,
            isTurnComplete = response.isTurnComplete,
            isInterrupted  = response.isInterrupted,
            inputTranscript  = response.inputTranscription?.text,
            outputTranscript = response.outputTranscription?.text,
        )
    }

    /**
     * Парсит JSON-строку объявления функции из FunctionRouter в FunctionDeclaration SDK.
     *
     * FunctionRouter хранит declarations в виде JSON:
     * {
     *   "name": "setStrategy",
     *   "description": "Меняет стратегию обучения",
     *   "parameters": { "type": "object", "properties": { ... } }
     * }
     *
     * Firebase AI SDK ожидает FunctionDeclaration(name, description, parameters: Schema?)
     */
    private fun parseFunctionDeclaration(declarationJson: String): FunctionDeclaration {
        val obj = json.parseToJsonElement(declarationJson) as JsonObject

        val name        = (obj["name"]        as? JsonPrimitive)?.content ?: error("Missing 'name'")
        val description = (obj["description"] as? JsonPrimitive)?.content ?: ""
        val parameters  = obj["parameters"]?.let { parseSchema(it as JsonObject) }

        return FunctionDeclaration(
            name        = name,
            description = description,
            parameters  = parameters,
        )
    }

    /**
     * Парсит JSON Schema объект в Schema SDK.
     * Поддерживает type=object с properties (достаточно для всех функций FunctionRouter).
     */
    private fun parseSchema(schemaJson: JsonObject): Schema {
        val type = (schemaJson["type"] as? JsonPrimitive)?.content?.uppercase() ?: "OBJECT"
        val description = (schemaJson["description"] as? JsonPrimitive)?.content

        return when (type) {
            "OBJECT" -> {
                val propertiesJson = schemaJson["properties"] as? JsonObject ?: JsonObject(emptyMap())
                val required = (schemaJson["required"] as? kotlinx.serialization.json.JsonArray)
                    ?.mapNotNull { (it as? JsonPrimitive)?.content }
                    ?: emptyList()

                val properties = propertiesJson.mapValues { (_, v) ->
                    parseSchema(v as JsonObject)
                }

                Schema.obj(
                    properties  = properties,
                    optionalProperties = properties.keys.filter { it !in required }.toList(),
                    description = description,
                )
            }
            "STRING"  -> Schema.string(description = description)
            "NUMBER"  -> Schema.double(description = description)
            "INTEGER" -> Schema.integer(description = description)
            "BOOLEAN" -> Schema.boolean(description = description)
            "ARRAY"   -> {
                val itemsJson = schemaJson["items"] as? JsonObject
                val items = itemsJson?.let { parseSchema(it) } ?: Schema.string()
                Schema.array(items = items, description = description)
            }
            else -> Schema.string(description = description)
        }
    }
}

// ── Response models ───────────────────────────────────────────────────────────

/**
 * Результат одного ответного события от Gemini Live API.
 *
 * Одно событие может содержать только один тип payload:
 *   - audioData: chunk PCM 24kHz аудио для воспроизведения
 *   - functionCall: запрос на вызов функции из FunctionRouter
 *   - transcript: текстовая транскрипция (input STT или output TTS→text)
 *   - isTurnComplete: модель завершила свой ход
 *   - isInterrupted: пользователь перебил модель
 *
 * Отдельно хранятся inputTranscript и outputTranscript для UI:
 *   inputTranscript  → что сказал пользователь (жёлтый subtitle)
 *   outputTranscript → что ответил Gemini (белый subtitle)
 */
data class GeminiResponse(
    val audioData: ByteArray?,
    val transcript: String?,
    val functionCall: GeminiFunctionCall?,
    val isTurnComplete: Boolean = false,
    val isInterrupted: Boolean = false,
    val inputTranscript: String? = null,
    val outputTranscript: String? = null,
) {
    fun hasAudio(): Boolean = audioData != null && audioData.isNotEmpty()
    fun hasFunctionCall(): Boolean = functionCall != null
    fun hasTranscript(): Boolean = !transcript.isNullOrEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeminiResponse) return false
        return transcript     == other.transcript &&
               functionCall   == other.functionCall &&
               isTurnComplete == other.isTurnComplete &&
               isInterrupted  == other.isInterrupted &&
               inputTranscript  == other.inputTranscript &&
               outputTranscript == other.outputTranscript &&
               (audioData?.contentEquals(other.audioData) == true ||
                (audioData == null && other.audioData == null))
    }

    override fun hashCode(): Int {
        var result = transcript.hashCode()
        result = 31 * result + isTurnComplete.hashCode()
        result = 31 * result + isInterrupted.hashCode()
        result = 31 * result + (functionCall?.hashCode() ?: 0)
        return result
    }
}

/**
 * Описание вызова функции от Gemini.
 *
 * id: уникальный идентификатор вызова (используется в FunctionResponsePart).
 *     В Live API может отсутствовать — тогда используем name как fallback.
 * name: имя функции из FunctionRegistry (напр. "setStrategy", "markWordKnown")
 * argsJson: аргументы в виде JSON-строки для FunctionRouter.dispatch()
 */
data class GeminiFunctionCall(
    val id: String,
    val name: String,
    val argsJson: String,
)

/** Исключение при проблемах с соединением к Gemini Live API. */
class GeminiConnectionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)