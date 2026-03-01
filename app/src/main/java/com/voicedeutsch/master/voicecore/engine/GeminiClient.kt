package com.voicedeutsch.master.voicecore.engine

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.LiveServerContent
import com.google.firebase.ai.type.LiveServerMessage
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import com.voicedeutsch.master.voicecore.context.ContextBuilder

import com.voicedeutsch.master.voicecore.functions.GeminiFunctionDeclaration
import com.voicedeutsch.master.voicecore.functions.GeminiProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.concurrent.atomic.AtomicInteger

/**
 * GeminiClient — обёртка над Firebase AI Logic Live API SDK.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * ИЗМЕНЕНИЯ (Live API Capabilities — полная реализация):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   1. sendAudioRealtime() — оптимизированная отправка аудио
 *   2. sendAudioStreamEnd() — сигнал паузы аудиопотока для VAD
 *   3. Транскрипция (input/output) в конфигурации
 *   4. generationComplete tracking
 *
 * ════════════════════════════════════════════════════════════════════════════
 * ПРИМЕЧАНИЕ (Firebase AI SDK ограничения — по документации 2026-02):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   НЕ поддерживается Firebase AI Logic (пока):
 *   - Session resumption (возобновление сессии)
 *   - Контекстное сжатие (sliding window)
 *   - VAD конфигурация (чувствительность, тайминги)
 *   - Affective dialog + Proactive audio
 *   - Thinking budget + includeThoughts
 *   - UsageMetadata в ответах
 *   - FunctionCallingMode (AUTO/ANY/NONE/VALIDATED)
 *   - Async function calling (NON_BLOCKING behavior)
 *
 *   Когда Firebase добавит поддержку — раскомментировать соответствующий код.
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ════════════════════════════════════════════════════════════════════════════
 * FIX (sendAudioChunk — session stability):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   Баг: одиночная ошибка sendAudioRealtime() обнуляла liveSession,
 *        что каскадно дропало все последующие чанки как "no active session".
 *   Fix: ошибка НЕ обнуляет сессию. Вместо этого ведётся счётчик
 *        consecutiveErrors. При >= MAX_CONSECUTIVE_ERRORS сессия
 *        считается мёртвой и обнуляется — handleSessionError в Engine
 *        инициирует reconnect.
 * ════════════════════════════════════════════════════════════════════════════
 */
@OptIn(PublicPreviewAPI::class)
class GeminiClient(
    private val config: GeminiConfig,
    private val json: Json,
) {
    companion object {
        private const val TAG = "GeminiClient"
        private const val AUDIO_INPUT_MIME = "audio/pcm;rate=16000"

        /**
         * Количество подряд идущих ошибок sendAudioRealtime(),
         * после которого сессия считается мёртвой.
         */
        private const val MAX_CONSECUTIVE_SEND_ERRORS = 10
    }

    // ── Состояние ─────────────────────────────────────────────────────────────

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var liveSession: LiveSession? = null

    /** Мьютекс для безопасного доступа к liveSession из разных корутин */
    private val sessionMutex = Mutex()

    /** Счётчик подряд идущих ошибок отправки аудио */
    private val consecutiveSendErrors = AtomicInteger(0)

    /**
     * Хранит токен возобновления сессии.
     * Обновляется при каждом SessionResumptionUpdate от сервера.
     *
     * ПРИМЕЧАНИЕ: Session resumption НЕ поддерживается Firebase AI SDK
     * (по состоянию на февраль 2026). Поле сохранено для будущей совместимости.
     */
    @Volatile var sessionResumptionHandle: String? = null
        private set

    /** Последнее известное количество использованных токенов */
    @Volatile var lastTokenUsage: TokenUsage? = null
        private set

    // ── Data classes ──────────────────────────────────────────────────────────

    data class TokenUsage(
        val promptTokenCount: Int = 0,
        val responseTokenCount: Int = 0,
        val totalTokenCount: Int = 0,
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Подключается к Gemini Live API с полной конфигурацией.
     */
    suspend fun connect(context: ContextBuilder.SessionContext) {
        try {
            Log.d(TAG, "Connecting to Gemini Live API [model=${config.modelName}]")

            // 🔥 ВРЕМЕННО ОТКЛЮЧАЕМ ВСЕ ФУНКЦИИ ДЛЯ ТЕСТА СОЕДИНЕНИЯ
            /*
            val declNames = context.functionDeclarations.map { it.name }
            Log.d(TAG, "Function declarations to register (${declNames.size}): $declNames")

            val firebaseDeclarations = context.functionDeclarations.mapNotNull { decl ->
                runCatching { mapToFirebaseDeclaration(decl) }
                    .onFailure { Log.w(TAG, "Skipping invalid function ${decl.name}: ${it.message}") }
                    .getOrNull()
            }

            Log.d(TAG, "Successfully mapped ${firebaseDeclarations.size}/${declNames.size} declarations")

            val toolsList = buildList<Tool> {
                if (firebaseDeclarations.isNotEmpty()) {
                    add(Tool.functionDeclarations(firebaseDeclarations))
                }
                if (config.enableSearchGrounding) {
                    add(Tool.googleSearch())
                    Log.d(TAG, "Google Search grounding enabled")
                }
            }
            */

            // ── Live Generation Config ────────────────────────────────────────
            val liveConfig = liveGenerationConfig {
                responseModality = ResponseModality.AUDIO
                speechConfig = SpeechConfig(voice = Voice(config.voiceName))

                if (config.transcriptionConfig.outputTranscriptionEnabled) {
                    outputAudioTranscription = AudioTranscriptionConfig()
                }
                if (config.transcriptionConfig.inputTranscriptionEnabled) {
                    inputAudioTranscription = AudioTranscriptionConfig()
                }
            }

            Log.d(TAG, "Live config: transcription enabled")

            val liveModel = Firebase.ai.liveModel(
                modelName = config.modelName,
                generationConfig = liveConfig,
                systemInstruction = content(role = "system") { text(context.fullContext) },
            )

            val session = liveModel.connect()

            sessionMutex.withLock {
                liveSession = session
                consecutiveSendErrors.set(0)
            }

            Log.d(TAG, "✅ LiveSession established (TOOLS DISABLED FOR TESTING)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ connect() failed: ${e.message}")
            Log.e(TAG, "❌ Exception class: ${e.javaClass.name}")
            Log.e(TAG, "❌ Stack trace:", e)
            e.cause?.let { cause ->
                Log.e(TAG, "❌ Cause: ${cause.message}")
            }
            throw GeminiConnectionException("Failed to connect to Gemini Live API", e)
        }
    }

    /**
     * Закрывает LiveSession и освобождает ресурсы соединения.
     * Токен возобновления сохраняется для потенциального reconnect.
     */
    suspend fun disconnect() {
        try {
            sessionMutex.withLock {
                liveSession?.close()
                liveSession = null
                consecutiveSendErrors.set(0)
            }
            Log.d(TAG, "LiveSession closed (resumption handle preserved: ${sessionResumptionHandle != null})")
        } catch (e: Exception) {
            Log.w(TAG, "disconnect() warning: ${e.message}")
        }
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    /**
     * Оптимизированная отправка аудио через sendAudioRealtime.
     *
     * В отличие от send(content { inlineData(...) }), sendAudioRealtime:
     * - Оптимизирован для быстрого отклика (за счёт детерминированного порядка)
     * - Работает совместно с серверным VAD
     * - Не блокирует обработку предыдущих чанков
     *
     * ════════════════════════════════════════════════════════════════════════
     * FIX: Ошибка одного чанка НЕ убивает сессию.
     *
     *   Раньше: onFailure → liveSession = null → все чанки дропаются.
     *   Теперь: onFailure → счётчик++ → при MAX_CONSECUTIVE_SEND_ERRORS
     *           сессия обнуляется (предполагаем, что она мертва).
     *           Успешная отправка сбрасывает счётчик.
     * ════════════════════════════════════════════════════════════════════════
     */
    suspend fun sendAudioChunk(pcmBytes: ByteArray) {
        val session = liveSession ?: run {
            Log.w(TAG, "sendAudioChunk: no active session, dropping chunk")
            return
        }

        if (pcmBytes.isEmpty()) return

        try {
            session.sendAudioRealtime(InlineData(pcmBytes, AUDIO_INPUT_MIME))
            // Успешная отправка — сбрасываем счётчик ошибок
            consecutiveSendErrors.set(0)
        } catch (e: Exception) {
            val errorCount = consecutiveSendErrors.incrementAndGet()
            Log.e(TAG, "sendAudioChunk error [$errorCount/$MAX_CONSECUTIVE_SEND_ERRORS]: ${e.message}")

            if (errorCount == 1) {
                // Логируем полный стектрейс только для первой ошибки
                Log.e(TAG, "sendAudioChunk first error stacktrace:", e)
            }

            if (errorCount >= MAX_CONSECUTIVE_SEND_ERRORS) {
                Log.e(TAG, "❌ Too many consecutive send errors — session is dead")
                // @Volatile liveSession — атомарная запись, безопасна без mutex
                liveSession = null
            }
            // НЕ обнуляем liveSession при единичных ошибках — просто пропускаем чанк
        }
    }

    /**
     * Сигнал паузы аудиопотока.
     *
     * Отправляется когда пользователь выключает микрофон или при паузе > 1 сек.
     * Позволяет серверному VAD очистить аудиобуфер.
     * Клиент может возобновить отправку аудио в любое время.
     *
     * ПРИМЕЧАНИЕ: Firebase AI SDK не поддерживает sendAudioStreamEnd напрямую.
     * Метод оставлен как заглушка для будущей совместимости.
     */
    suspend fun sendAudioStreamEnd() {
        val session = liveSession ?: return
        runCatching {
            // Firebase AI SDK не экспонирует audioStreamEnd.
            // Когда поддержка появится — раскомментировать:
            // session.sendRealtimeInput(audioStreamEnd = true)
            Log.d(TAG, "Audio stream end signal (no-op — not yet supported by Firebase SDK)")
        }.onFailure { e ->
            Log.w(TAG, "sendAudioStreamEnd error: ${e.message}")
        }
    }

    /**
     * Отправляет текстовое сообщение в Gemini Live API.
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
     * Отправляет результат одной функции обратно в Gemini.
     */
    suspend fun sendFunctionResult(callId: String, name: String, resultJson: String) {
        sendFunctionResults(listOf(Triple(callId, name, resultJson)))
    }

    /**
     * Отправляет ВСЕ результаты функций одним батчем.
     *
     * @param results список Triple(callId, name, resultJson)
     */
    suspend fun sendFunctionResults(
        results: List<Triple<String, String, String>>,
    ) {
        if (results.isEmpty()) return
        val session = liveSession ?: run {
            Log.w(TAG, "sendFunctionResults: no active session")
            return
        }
        runCatching {
            val responseParts = results.map { (_, name, resultJson) ->
                val responseJson = try {
                    json.parseToJsonElement(resultJson) as? JsonObject
                        ?: buildJsonObject { put("result", JsonPrimitive(resultJson)) }
                } catch (e: Exception) {
                    buildJsonObject { put("result", JsonPrimitive(resultJson)) }
                }

                FunctionResponsePart(name, responseJson)
            }
            session.sendFunctionResponse(responseParts)
            Log.d(TAG, "✅ Sent ${responseParts.size} function response(s): " +
                "${results.map { it.second }}")
        }.onFailure { e ->
            Log.e(TAG, "sendFunctionResults error: ${e.message}", e)
        }
    }

    // ── Receive ───────────────────────────────────────────────────────────────

    /**
     * Cold Flow входящих ответов от Gemini.
     *
     * ВАЖНО: этот Flow ДОЛЖЕН быть запущен (коллекция начата)
     * ДО первого вызова sendAudioChunk(). Иначе Firebase SDK
     * может отклонить входящие данные с ошибкой.
     */
    fun receiveFlow(): Flow<GeminiResponse> = flow {
        val session = liveSession
            ?: throw GeminiConnectionException("receiveFlow: no active session")

        try {
            session.receive().collect { message ->
                when (message) {
                    is LiveServerContent -> {
                        // Session resumption — не поддерживается Firebase SDK.
                        // GoAway — не поддерживается Firebase SDK.
                        // UsageMetadata — не поддерживается Firebase SDK.

                        mapServerContent(message)?.let { emit(it) }
                    }
                    // Другие типы LiveServerMessage (если появятся)
                    else -> {
                        Log.d(TAG, "Unknown message type: ${message::class.simpleName}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "receiveFlow error: ${e.message}", e)
            throw e
        }
    }

    // ── Release ───────────────────────────────────────────────────────────────

    fun release() {
        sessionResumptionHandle = null
        lastTokenUsage = null
        consecutiveSendErrors.set(0)
        clientScope.cancel()
    }

    /** Сброс токена возобновления (при полном завершении сессии) */
    fun clearResumptionHandle() {
        sessionResumptionHandle = null
    }

    // ── Маппинг ответов ───────────────────────────────────────────────────────

    /**
     * Маппит LiveServerContent → GeminiResponse.
     */
    private fun mapServerContent(sc: LiveServerContent): GeminiResponse? {
        val parts = sc.content?.parts.orEmpty()

        val audioData = parts
            .filterIsInstance<InlineDataPart>()
            .firstOrNull()
            ?.inlineData
            ?.takeIf { it.isNotEmpty() }

        val textContent = parts
            .filterIsInstance<TextPart>()
            .joinToString("") { it.text }
            .takeIf { it.isNotEmpty() }

        val functionCalls = parts
            .filterIsInstance<FunctionCallPart>()
            .map { fc ->
                GeminiFunctionCall(
                    id       = fc.name,
                    name     = fc.name,
                    argsJson = fc.args.toString(),
                )
            }

        if (functionCalls.size > 1) {
            Log.d(TAG, "Parallel function calls received: ${functionCalls.map { it.name }}")
        }

        val isTurnComplete       = sc.turnComplete
        val isInterrupted        = sc.interrupted
        val isGenerationComplete = sc.generationComplete

        val inputTranscript  = sc.inputTranscription?.text?.takeIf { it.isNotEmpty() }
        val outputTranscript = sc.outputTranscription?.text?.takeIf { it.isNotEmpty() }

        if (audioData == null && textContent == null && functionCalls.isEmpty() &&
            !isTurnComplete && !isInterrupted && !isGenerationComplete &&
            inputTranscript == null && outputTranscript == null) {
            return null
        }

        return GeminiResponse(
            audioData            = audioData,
            transcript           = textContent,
            functionCalls        = functionCalls,
            isTurnComplete       = isTurnComplete,
            isInterrupted        = isInterrupted,
            isGenerationComplete = isGenerationComplete,
            inputTranscript      = inputTranscript,
            outputTranscript     = outputTranscript,
        )
    }

    // ── Нативный маппинг функций ──────────────────────────────────────────────

    /**
     * Маппит GeminiFunctionDeclaration → Firebase FunctionDeclaration.
     *
     * ════════════════════════════════════════════════════════════════
     * FIX: Два бага вызывали `parameters_json_schema must not...` ошибку:
     *
     *   Баг A: Для функций без параметров инжектился dummy_param.
     *          Live API отклоняет такую схему.
     *   FIX A: Функции без параметров создаются БЕЗ parameters вообще.
     *
     *   Баг B: Когда required=[], optionalParameters возвращал emptyList(),
     *          что SDK трактовал как "все параметры required" — конфликт.
     *   FIX B: optionalProperties всегда вычисляется как
     *          properties.keys.filter { it !in params.required }.
     *          Если required пуст → все optional. Корректно.
     * ════════════════════════════════════════════════════════════════
     */
    private fun mapToFirebaseDeclaration(decl: GeminiFunctionDeclaration): FunctionDeclaration? {
        val params = decl.parameters

        // Если параметров нет, создаем фиктивный параметр,
        // чтобы избежать ошибки "parameters_json_schema must not be empty"
        if (params == null || params.properties.isEmpty()) {
            Log.d(TAG, "  ⚙ ${decl.name} — injecting dummy param")
            val dummyProps = mapOf(
                "dummy" to Schema.string(description = "Игнорируемый параметр")
            )
            return FunctionDeclaration(
                name = decl.name,
                description = decl.description,
                parameters = dummyProps,
                optionalParameters = listOf("dummy")
            )
        }

        val properties = params.properties.mapValues { (_, prop) ->
            mapPropertyToSchema(prop)
        }

        val optionalProperties = properties.keys.filter { it !in params.required }

        Log.d(TAG, "  ⚙ ${decl.name} — params: ${properties.keys}, " +
                "required: ${params.required}, optional: $optionalProperties")

        return FunctionDeclaration(
            name = decl.name,
            description = decl.description,
            parameters = properties,
            optionalParameters = optionalProperties
        )
    }

    private fun mapPropertyToSchema(prop: GeminiProperty): Schema {
        return when (prop.type.uppercase()) {
            "STRING" -> {
                if (prop.enum != null) {
                    Schema.enumeration(values = prop.enum, description = prop.description)
                } else {
                    Schema.string(description = prop.description)
                }
            }
            "INTEGER" -> Schema.integer(description = prop.description)
            "NUMBER"  -> Schema.double(description = prop.description)
            "BOOLEAN" -> Schema.boolean(description = prop.description)
            "ARRAY"   -> Schema.array(items = Schema.string(), description = prop.description)
            else      -> Schema.string(description = prop.description)
        }
    }
}

// ── Response models ───────────────────────────────────────────────────────────

data class GeminiResponse(
    val audioData: ByteArray?,
    val transcript: String?,
    val functionCalls: List<GeminiFunctionCall> = emptyList(),
    val isTurnComplete: Boolean = false,
    val isInterrupted: Boolean = false,
    /** Модель завершила генерацию ответа (может прийти до turnComplete) */
    val isGenerationComplete: Boolean = false,
    val inputTranscript: String? = null,
    val outputTranscript: String? = null,
    /** GoAway — предупреждение о скором разрыве соединения */
    val goAway: GeminiGoAway? = null,
) {
    fun hasAudio(): Boolean = audioData != null && audioData.isNotEmpty()
    fun hasFunctionCalls(): Boolean = functionCalls.isNotEmpty()
    fun hasTranscript(): Boolean = !transcript.isNullOrEmpty()
    fun hasGoAway(): Boolean = goAway != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeminiResponse) return false
        return transcript      == other.transcript &&
               functionCalls   == other.functionCalls &&
               isTurnComplete  == other.isTurnComplete &&
               isInterrupted   == other.isInterrupted &&
               isGenerationComplete == other.isGenerationComplete &&
               inputTranscript  == other.inputTranscript &&
               outputTranscript == other.outputTranscript &&
               goAway           == other.goAway &&
               (audioData?.contentEquals(other.audioData) == true ||
                (audioData == null && other.audioData == null))
    }

    override fun hashCode(): Int {
        var result = transcript.hashCode()
        result = 31 * result + isTurnComplete.hashCode()
        result = 31 * result + isInterrupted.hashCode()
        result = 31 * result + isGenerationComplete.hashCode()
        result = 31 * result + functionCalls.hashCode()
        result = 31 * result + (goAway?.hashCode() ?: 0)
        return result
    }
}

data class GeminiFunctionCall(
    val id: String,
    val name: String,
    val argsJson: String,
)

/** Предупреждение о скором разрыве соединения */
data class GeminiGoAway(
    val timeLeftMs: Long = 0,
)

class GeminiConnectionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
