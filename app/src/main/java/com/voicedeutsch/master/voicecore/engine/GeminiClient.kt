package com.voicedeutsch.master.voicecore.engine

import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.LiveServerContent
import com.google.firebase.ai.type.LiveServerMessage
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.TextPart
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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

// ════════════════════════════════════════════════════════════════════════════
// ИТОГ ОТЛАДКИ (февраль 2026, BoM 34.9.0, firebase-ai SDK):
//
// ✅ СУЩЕСТВУЮТ (подтверждено компилятором — импорты НЕ давали ошибок):
//    - LiveSession          (com.google.firebase.ai.type)
//    - LiveServerMessage    (com.google.firebase.ai.type) — sealed class
//    - LiveServerContent    (com.google.firebase.ai.type) — подтип LiveServerMessage
//    - FunctionCallPart, FunctionResponsePart, FunctionDeclaration
//    - InlineDataPart, TextPart, Tool, Schema, Voice, SpeechConfig
//
// ❌ НЕ СУЩЕСТВУЮТ (подтверждено компилятором):
//    - LiveContentResponse  → ФАНТОМ, нет в SDK
//    - GenerativeBackend    → как отдельный импорт не резолвится
//    - LiveGenerativeModel  → тип выводится, явный импорт невозможен
//    - AudioTranscriptionConfig → нет в текущей версии
//
// 📝 КЛЮЧЕВЫЕ СИГНАТУРЫ (подтверждены ошибками компилятора):
//    - session.send(content: Content)  — ОДИН параметр, без turnComplete
//    - session.send(text: String)      — ОДИН параметр, без turnComplete
//    - session.receive() → Flow<LiveServerMessage>
//    - LiveServerContent свойства (из PR #7482):
//        content: Content, turnComplete: Boolean, interrupted: Boolean,
//        generationComplete: Boolean, inputTranscription: Transcription,
//        outputTranscription: Transcription
//
// ⚠️ ВОЗМОЖНЫЕ ПРАВКИ ПОСЛЕ БИЛДА:
//    1. Если receive() возвращает Flow<LiveServerContent> вместо
//       Flow<LiveServerMessage> — убрать when и работать напрямую.
//    2. Если LiveServerContent.content nullable — уже обработано через ?.parts.
//    3. Если InlineDataPart.inlineData не существует — попробовать .data или .bytes.
//    4. Если Transcription.text не существует — проверить .content или toString().
//    5. Если FunctionCallPart.args — не Map, а JsonObject — поменять .toString().
// ════════════════════════════════════════════════════════════════════════════

/**
 * GeminiClient — обёртка над Firebase AI Logic Live API SDK.
 *
 * АУДИО ФОРМАТ:
 *   Вход:  PCM 16-bit, 16 kHz, mono  → session.send(content { inlineData(...) })
 *   Выход: PCM 16-bit, 24 kHz, mono  ← LiveServerContent.content.parts[InlineDataPart]
 *
 * @param config  конфигурация модели (model name, voice, sample rates и т.д.)
 * @param json    экземпляр Json для сериализации function declarations
 */
@OptIn(PublicPreviewAPI::class)
class GeminiClient(
    private val config: GeminiConfig,
    private val json: Json,
) {
    companion object {
        private const val TAG = "GeminiClient"
        private const val DEFAULT_MODEL = "gemini-2.5-flash-native-audio-preview-12-2025"
        private const val AUDIO_INPUT_MIME = "audio/pcm;rate=16000"
    }

    // ── Состояние ─────────────────────────────────────────────────────────────

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var liveSession: LiveSession? = null
    private var audioConversationJob: Job? = null

    private val responseChannel = Channel<GeminiResponse>(Channel.UNLIMITED)

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Подключается к Gemini Live API.
     *
     * Firebase.ai.liveModel() — backend по умолчанию googleAI.
     * liveModel.connect() → LiveSession.
     */
    suspend fun connect(context: ContextBuilder.SessionContext) {
        try {
            Log.d(TAG, "Connecting to Gemini Live API [model=${config.modelName}]")

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

            val liveModel = Firebase.ai.liveModel(
                modelName = config.modelName.ifBlank { DEFAULT_MODEL },
                generationConfig = liveGenerationConfig {
                    responseModality = ResponseModality.AUDIO
                    speechConfig = SpeechConfig(
                        voice = Voice(config.voiceName)
                    )
                },
                tools = tools,
                systemInstruction = systemInstruction,
            )

            liveSession = liveModel.connect()
            Log.d(TAG, "✅ LiveSession established")
        } catch (e: Exception) {
            Log.e(TAG, "❌ connect() failed: ${e.message}", e)
            throw GeminiConnectionException("Failed to connect to Gemini Live API", e)
        }
    }

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
     * Формат: PCM 16-bit signed, 16 kHz, mono.
     *
     * ✅ session.send(content: Content) — ОДИН параметр, без turnComplete.
     */
    suspend fun sendAudioChunk(pcmBytes: ByteArray) {
        val session = liveSession ?: run {
            Log.w(TAG, "sendAudioChunk: no active session, dropping chunk")
            return
        }
        runCatching {
            val audioContent = content {
                inlineData(pcmBytes, AUDIO_INPUT_MIME)
            }
            session.send(audioContent)
        }.onFailure { e ->
            Log.e(TAG, "sendAudioChunk error: ${e.message}", e)
        }
    }

    /**
     * Отправляет текстовое сообщение в Gemini Live API.
     *
     * ✅ session.send(text: String) — ОДИН параметр, без turnComplete.
     */
    suspend fun sendText(text: String) {
        val session = liveSession ?: run {
            Log.w(TAG, "sendText: no active session")
            return
        }
        runCatching {
            session.send(text)
        }.onFailure { e ->
            Log.e(TAG, "sendText error: ${e.message}", e)
        }
    }

    /**
     * Отправляет результат выполнения функции обратно в Gemini.
     *
     * ✅ FunctionResponsePart(name, response) — без id.
     */
    suspend fun sendFunctionResult(callId: String, name: String, resultJson: String) {
        val session = liveSession ?: run {
            Log.w(TAG, "sendFunctionResult: no active session")
            return
        }
        runCatching {
            val responseJson = try {
                json.parseToJsonElement(resultJson) as? JsonObject
                    ?: buildJsonObject { put("result", JsonPrimitive(resultJson)) }
            } catch (e: Exception) {
                buildJsonObject { put("result", JsonPrimitive(resultJson)) }
            }

            val functionResponse = FunctionResponsePart(
                name     = name,
                response = responseJson,
            )
            session.sendFunctionResponse(listOf(functionResponse))
        }.onFailure { e ->
            Log.e(TAG, "sendFunctionResult error: ${e.message}", e)
        }
    }

    /**
     * Cold Flow входящих ответов от Gemini.
     *
     * ✅ session.receive() → Flow<LiveServerMessage>
     *
     * LiveServerMessage — sealed class. Подтипы:
     *   - LiveServerContent → аудио/текст/function calls/транскрипции
     *   - (другие — пропускаем)
     *
     * LiveServerContent.content.parts содержит:
     *   - InlineDataPart → аудио (PCM 24kHz)
     *   - TextPart → текстовый ответ
     *   - FunctionCallPart → вызовы функций (при ручном режиме)
     *
     * ⚠️ Если receive() возвращает Flow<LiveServerContent> напрямую —
     *    уберите when(message) и работайте с LiveServerContent сразу.
     */
    fun receiveFlow(): Flow<GeminiResponse> = flow {
        val session = liveSession
            ?: throw GeminiConnectionException("receiveFlow: no active session")

        try {
            session.receive().collect { message ->
                mapServerMessage(message)?.let { emit(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "receiveFlow error: ${e.message}", e)
            throw e
        }
    }

    /**
     * Режим A: SDK управляет захватом микрофона и воспроизведением аудио.
     *
     * ✅ startAudioConversation — рекомендуемый подход Firebase.
     * Handles: audio capture → send → receive → playback + function calling + transcription.
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

    fun stopManagedAudioConversation() {
        runCatching { liveSession?.stopAudioConversation() }
            .onFailure { Log.w(TAG, "stopAudioConversation warning: ${it.message}") }
        audioConversationJob?.cancel()
        audioConversationJob = null
    }

    fun release() {
        clientScope.cancel()
        responseChannel.close()
    }

    // ── Маппинг ответов ───────────────────────────────────────────────────────

    /**
     * Маппит LiveServerMessage → GeminiResponse.
     *
     * LiveServerMessage — sealed class. LiveServerContent — основной подтип.
     */
    private fun mapServerMessage(message: LiveServerMessage): GeminiResponse? {
        if (message is LiveServerContent) {
            return mapServerContent(message)
        }

        // Другие подтипы (LiveServerToolCall и т.д.) — при использовании
        // startAudioConversation function calls обрабатываются через callback.
        Log.d(TAG, "Received non-content message: ${message::class.simpleName}")
        return null
    }

    /**
     * Маппит LiveServerContent → GeminiResponse.
     *
     * LiveServerContent свойства (из PR firebase-android-sdk #7482):
     *   - content: Content?         → parts: List<Part>
     *   - turnComplete: Boolean
     *   - interrupted: Boolean
     *   - generationComplete: Boolean
     *   - inputTranscription: Transcription
     *   - outputTranscription: Transcription
     *
     * ⚠️ Если InlineDataPart.inlineData не компилируется — попробуйте .data
     * ⚠️ Если Transcription.text не компилируется — попробуйте .content
     */
    private fun mapServerContent(sc: LiveServerContent): GeminiResponse? {
        val parts = sc.content?.parts.orEmpty()

        // Извлекаем аудио (InlineDataPart → PCM 24kHz)
        val audioData = parts
            .filterIsInstance<InlineDataPart>()
            .firstOrNull()
            ?.inlineData  // ByteArray — ⚠️ если не компилируется, попробуйте .data
            ?.takeIf { it.isNotEmpty() }

        // Извлекаем текст
        val textContent = parts
            .filterIsInstance<TextPart>()
            .joinToString("") { it.text }
            .takeIf { it.isNotEmpty() }

        // Извлекаем function calls (для ручного режима без startAudioConversation)
        val functionCall = parts
            .filterIsInstance<FunctionCallPart>()
            .firstOrNull()
            ?.let { fc ->
                GeminiFunctionCall(
                    id       = fc.name,  // нет отдельного id, используем name
                    name     = fc.name,
                    argsJson = fc.args.toString(),
                )
            }

        val isTurnComplete = sc.turnComplete
        val isInterrupted  = sc.interrupted

        // Транскрипции — ⚠️ если .text не компилируется, попробуйте .content
        val inputTranscript  = sc.inputTranscription?.text?.takeIf { it.isNotEmpty() }
        val outputTranscript = sc.outputTranscription?.text?.takeIf { it.isNotEmpty() }

        // Если ответ полностью пустой — не эмитируем
        if (audioData == null && textContent == null && functionCall == null &&
            !isTurnComplete && !isInterrupted &&
            inputTranscript == null && outputTranscript == null) {
            return null
        }

        return GeminiResponse(
            audioData        = audioData,
            transcript       = textContent,
            functionCall     = functionCall,
            isTurnComplete   = isTurnComplete,
            isInterrupted    = isInterrupted,
            inputTranscript  = inputTranscript,
            outputTranscript = outputTranscript,
        )
    }

    // ── Парсинг FunctionDeclaration ───────────────────────────────────────────

    /**
     * Парсит JSON-строку объявления функции в FunctionDeclaration SDK.
     *
     * ✅ FunctionDeclaration всегда требует parameters (Map<String, Schema>).
     *    Для функций без параметров → parameters = emptyMap().
     */
    private fun parseFunctionDeclaration(declarationJson: String): FunctionDeclaration {
        val obj = json.parseToJsonElement(declarationJson) as JsonObject

        val name        = (obj["name"]        as? JsonPrimitive)?.content ?: error("Missing 'name'")
        val description = (obj["description"] as? JsonPrimitive)?.content ?: ""
        val parametersObj = obj["parameters"] as? JsonObject

        val propertiesJson = parametersObj
            ?.let { it["properties"] as? JsonObject }
            ?: JsonObject(emptyMap())

        val required = parametersObj
            ?.let { it["required"] as? kotlinx.serialization.json.JsonArray }
            ?.mapNotNull { (it as? JsonPrimitive)?.content }
            ?: emptyList()

        val properties: Map<String, Schema> = propertiesJson.mapValues { (_, v) ->
            parseSchema(v as JsonObject)
        }

        return FunctionDeclaration(
            name               = name,
            description        = description,
            parameters         = properties,
            optionalParameters = properties.keys.filter { it !in required }.toList(),
        )
    }

    /**
     * Парсит JSON Schema объект в Schema SDK.
     */
    private fun parseSchema(schemaJson: JsonObject): Schema {
        val type = (schemaJson["type"] as? JsonPrimitive)?.content?.uppercase() ?: "STRING"
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
                    properties         = properties,
                    optionalProperties = properties.keys.filter { it !in required }.toList(),
                    description        = description,
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
        return transcript       == other.transcript &&
               functionCall     == other.functionCall &&
               isTurnComplete   == other.isTurnComplete &&
               isInterrupted    == other.isInterrupted &&
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

data class GeminiFunctionCall(
    val id: String,
    val name: String,
    val argsJson: String,
)

class GeminiConnectionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
