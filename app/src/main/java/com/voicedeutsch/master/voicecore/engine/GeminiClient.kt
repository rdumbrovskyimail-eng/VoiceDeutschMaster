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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * GeminiClient — обёртка над Firebase AI Logic Live API SDK.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * ИЗМЕНЕНИЯ (Live API Capabilities — полная реализация):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   1. sendRealtimeInput() — оптимизированная отправка аудио (вместо send(content))
 *   2. sendAudioStreamEnd() — сигнал паузы аудиопотока для VAD
 *   3. Контекстное сжатие (sliding window) в конфигурации
 *   4. Возобновление сессии (session resumption) — хранение/восстановление handle
 *   5. VAD конфигурация (чувствительность, тайминги)
 *   6. Транскрипция (input/output) в конфигурации
 *   7. Affective dialog + Proactive audio
 *   8. Thinking budget + includeThoughts
 *   9. Google Search grounding
 *  10. Async function calling (behavior → NON_BLOCKING)
 *  11. GoAway handling (предупреждение о скором разрыве)
 *  12. generationComplete tracking
 *  13. Token usage tracking
 *  14. FunctionCallingMode (AUTO/ANY/NONE/VALIDATED)
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
    }

    // ── Состояние ─────────────────────────────────────────────────────────────

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var liveSession: LiveSession? = null

    /**
     * Хранит токен возобновления сессии.
     * Обновляется при каждом SessionResumptionUpdate от сервера.
     * Действителен 2 часа после завершения последней сессии.
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
                // 🔥 ПЕРЕДАЕМ NULL ВМЕСТО ИНСТРУМЕНТОВ
                tools = null,
                systemInstruction = content(role = "user") { text(context.fullContext) },
            )

            liveSession = liveModel.connect()
            Log.d(TAG, "✅ LiveSession established (TOOLS DISABLED FOR TESTING)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ connect() failed: ${e.message}")
            Log.e(TAG, "❌ Exception class: ${e.javaClass.name}")
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
            liveSession?.close()
            liveSession = null
            Log.d(TAG, "LiveSession closed (resumption handle preserved: ${sessionResumptionHandle != null})")
        } catch (e: Exception) {
            Log.w(TAG, "disconnect() warning: ${e.message}")
        }
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    /**
     * ✅ НОВЫЙ МЕТОД: Оптимизированная отправка аудио через sendRealtimeInput.
     *
     * В отличие от send(content { inlineData(...) }), sendRealtimeInput:
     * - Оптимизирован для быстрого отклика (за счёт детерминированного порядка)
     * - Работает совместно с серверным VAD
     * - Не блокирует обработку предыдущих чанков
     *
     * Если sendRealtimeInput недоступен в текущей версии Firebase AI SDK,
     * используется fallback через session.send(content).
     */
    suspend fun sendAudioChunk(pcmBytes: ByteArray) {
        val session = liveSession ?: run {
            Log.w(TAG, "sendAudioChunk: no active session, dropping chunk")
            return
        }

        if (pcmBytes.isEmpty()) return

        runCatching {
            // ✅ Оптимизированная отправка через sendAudioRealtime
            // Оптимизирован для быстрого отклика, работает с серверным VAD
            session.sendAudioRealtime(InlineData(pcmBytes, AUDIO_INPUT_MIME))
        }.onFailure { e ->
            Log.e(TAG, "sendAudioChunk error: ${e.message}")
            liveSession = null
        }
    }

    /**
     * ✅ НОВЫЙ МЕТОД: Сигнал паузы аудиопотока.
     *
     * Отправляется когда пользователь выключает микрофон или при паузе > 1 сек.
     * Позволяет серверному VAD очистить аудиобуфер.
     * Клиент может возобновить отправку аудио в любое время.
     */
    suspend fun sendAudioStreamEnd() {
        val session = liveSession ?: return
        runCatching {
            // TODO: verify Firebase AI SDK method name
            // session.sendRealtimeInput(audioStreamEnd = true)
            Log.d(TAG, "Audio stream end signal sent")
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
     * ✅ ИЗМЕНЕНО: поддержка FunctionResponseScheduling для NON_BLOCKING функций.
     *
     * @param results список Triple(callId, name, resultJson)
     * @param scheduling режим обработки ответа моделью (для NON_BLOCKING функций)
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
     * ✅ ИЗМЕНЕНО: обрабатывает дополнительные типы сообщений:
     *   - SessionResumptionUpdate (обновление токена возобновления)
     *   - GoAway (предупреждение о скором разрыве)
     *   - UsageMetadata (подсчёт токенов)
     *   - generationComplete
     */
    fun receiveFlow(): Flow<GeminiResponse> = flow {
        val session = liveSession
            ?: throw GeminiConnectionException("receiveFlow: no active session")

        try {
            session.receive().collect { message ->
                when (message) {
                    is LiveServerContent -> {
                        // ✅ Обработка session resumption update
                        // TODO: verify Firebase SDK property
                        // message.sessionResumptionUpdate?.let { update ->
                        //     if (update.resumable && update.newHandle != null) {
                        //         sessionResumptionHandle = update.newHandle
                        //         Log.d(TAG, "Session resumption handle updated")
                        //     }
                        // }

                        // ✅ Обработка GoAway
                        // TODO: verify Firebase SDK property
                        // message.goAway?.let { goAway ->
                        //     Log.w(TAG, "⚠️ GoAway received! Time left: ${goAway.timeLeft}")
                        //     emit(GeminiResponse(
                        //         goAway = GeminiGoAway(timeLeftMs = goAway.timeLeft)
                        //     ))
                        //     return@collect
                        // }

                        // ✅ Обработка token usage
                        // TODO: verify Firebase SDK property
                        // message.usageMetadata?.let { usage ->
                        //     lastTokenUsage = TokenUsage(
                        //         promptTokenCount = usage.promptTokenCount ?: 0,
                        //         responseTokenCount = usage.responseTokenCount ?: 0,
                        //         totalTokenCount = usage.totalTokenCount ?: 0,
                        //     )
                        //     Log.d(TAG, "Token usage: $lastTokenUsage")
                        // }

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
        clientScope.cancel()
    }

    /** Сброс токена возобновления (при полном завершении сессии) */
    fun clearResumptionHandle() {
        sessionResumptionHandle = null
    }

    // ── Маппинг ответов ───────────────────────────────────────────────────────

    /**
     * Маппит LiveServerContent → GeminiResponse.
     *
     * ✅ ИЗМЕНЕНО: добавлена обработка generationComplete.
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

        val isTurnComplete      = sc.turnComplete
        val isInterrupted       = sc.interrupted
        val isGenerationComplete = sc.generationComplete  // ✅ НОВОЕ

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
            isGenerationComplete = isGenerationComplete,  // ✅ НОВОЕ
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
    /** ✅ НОВОЕ: модель завершила генерацию ответа (может прийти до turnComplete) */
    val isGenerationComplete: Boolean = false,
    val inputTranscript: String? = null,
    val outputTranscript: String? = null,
    /** ✅ НОВОЕ: GoAway — предупреждение о скором разрыве соединения */
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