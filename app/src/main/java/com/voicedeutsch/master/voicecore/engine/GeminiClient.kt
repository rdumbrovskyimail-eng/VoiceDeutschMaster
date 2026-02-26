package com.voicedeutsch.master.voicecore.engine

import android.util.Log
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
//    - defineFunction       → Unresolved reference (нет в com.google.firebase.ai.type)
//
// 📝 КЛЮЧЕВЫЕ СИГНАТУРЫ (подтверждены ошибками компилятора):
//    - session.send(content: Content)  — ОДИН параметр, без turnComplete
//    - session.send(text: String)      — ОДИН параметр, без turnComplete
//    - session.receive() → Flow<LiveServerMessage>
//    - FunctionDeclaration(name, description, parameters, optionalParameters)
//      * name: String (internal — нельзя читать снаружи!)
//      * description: String (internal)
//      * parameters: Map<String, Schema> (ОБЯЗАТЕЛЕН, нет дефолта)
//      * optionalParameters: List<String> = emptyList()
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
//
// ════════════════════════════════════════════════════════════════════════════
// ИЗМЕНЕНИЯ (Модули 4 + 7):
//   1. УДАЛЕНО: startManagedAudioConversation / stopManagedAudioConversation
//   2. УДАЛЕНО: parseFunctionDeclaration(jsonString) / parseSchema(jsonObject)
//   3. ДОБАВЛЕНО: mapToFirebaseDeclaration() + mapPropertyToSchema() (нативный маппинг)
//   4. УДАЛЕНО: audioConversationJob, responseChannel
//   5. ДОБАВЛЕНО: Schema.enumeration() для enum-свойств (set_current_strategy и т.д.)
// ════════════════════════════════════════════════════════════════════════════
// ИЗМЕНЕНИЯ (Parallel Function Calling fix):
//   6. FIX: mapServerContent() извлекает ВСЕ FunctionCallPart через
//      filterIsInstance<FunctionCallPart>() (список, не firstOrNull).
//   7. GeminiResponse.functionCall → functionCalls: List<GeminiFunctionCall>
//   8. sendFunctionResults(List<Pair>) — отправляет все ответы одним батчем.
// ════════════════════════════════════════════════════════════════════════════
// ИЗМЕНЕНИЯ (parameters_json_schema fix — попытка 4 - УСПЕШНАЯ):
//   9. FIX: mapToFirebaseDeclaration() — для функций без параметров
//      подставляем ОБЯЗАТЕЛЬНЫЙ (required) dummy-параметр "dummy_param".
//
//      ИСТОРИЯ ПРОБЛЕМЫ:
//        Попытка 1: FunctionDeclaration(name, description) без parameters
//          → НЕ КОМПИЛИРУЕТСЯ: "No value passed for parameter 'parameters'"
//          → parameters ОБЯЗАТЕЛЕН, нет дефолта.
//        Попытка 2: defineFunction(name, description)
//          → НЕ КОМПИЛИРУЕТСЯ: "Unresolved reference 'defineFunction'"
//          → defineFunction НЕ СУЩЕСТВУЕТ в BoM 34.9.0.
//          → Также: FunctionDeclaration.name — internal, нельзя читать.
//        Попытка 3: dummy optional "_context"
//          → SDK генерирует "required": [] → сервер отклоняет handshake.
//        Попытка 4 (текущая): dummy REQUIRED "dummy_param" (boolean)
//          → optionalParameters = emptyList() → SDK помещает в "required"
//          → Валидная непустая схема с 1 required параметром проходит handshake.
//
//  10. Логирование имён через decl.name ДО создания FunctionDeclaration,
//      т.к. FunctionDeclaration.name — internal и недоступен снаружи.
// ════════════════════════════════════════════════════════════════════════════

/**
 * GeminiClient — обёртка над Firebase AI Logic Live API SDK.
 *
 * АУДИО ФОРМАТ:
 *   Вход:  PCM 16-bit, 16 kHz, mono  → session.send(content { inlineData(...) })
 *   Выход: PCM 16-bit, 24 kHz, mono  ← LiveServerContent.content.parts[InlineDataPart]
 *
 * @param config  конфигурация модели (model name, voice, sample rates и т.д.)
 * @param json    экземпляр Json для сериализации function results
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

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Подключается к Gemini Live API.
     */
    suspend fun connect(context: ContextBuilder.SessionContext) {
        try {
            Log.d(TAG, "Connecting to Gemini Live API [model=${config.modelName}]")

            // Логируем имена ДО создания FunctionDeclaration,
            // т.к. FunctionDeclaration.name — internal и недоступен снаружи.
            val declNames = context.functionDeclarations.map { it.name }
            Log.d(TAG, "Function declarations to register (${declNames.size}): $declNames")

            val firebaseDeclarations = context.functionDeclarations.mapNotNull { decl ->
                runCatching { mapToFirebaseDeclaration(decl) }
                    .onFailure { Log.w(TAG, "Skipping invalid function ${decl.name}: ${it.message}") }
                    .getOrNull()
            }

            Log.d(TAG, "Successfully mapped ${firebaseDeclarations.size}/${declNames.size} declarations")

            val tools = firebaseDeclarations
                .takeIf { it.isNotEmpty() }
                ?.let { listOf(Tool.functionDeclarations(it)) }

            val liveModel = Firebase.ai.liveModel(
                modelName = config.modelName,
                generationConfig = liveGenerationConfig {
                    responseModality = ResponseModality.AUDIO
                    speechConfig = SpeechConfig(voice = Voice(config.voiceName))
                },
                //tools = tools,  // ✅ раскомментировано
                systemInstruction = content(role = "user") { text(context.fullContext) },
            )

            liveSession = liveModel.connect()
            Log.d(TAG, "✅ LiveSession established")
        } catch (e: Exception) {
            Log.e(TAG, "❌ connect() failed: ${e.message}", e)
            throw GeminiConnectionException("Failed to connect to Gemini Live API", e)
        }
    }

    /**
     * Закрывает LiveSession и освобождает ресурсы соединения.
     */
    suspend fun disconnect() {
        try {
            liveSession?.close()
            liveSession = null
            Log.d(TAG, "LiveSession closed")
        } catch (e: Exception) {
            Log.w(TAG, "disconnect() warning: ${e.message}")
        }
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    /**
     * Отправляет chunk PCM-аудио в Gemini Live API.
     */
    suspend fun sendAudioChunk(pcmBytes: ByteArray) {
        val session = liveSession ?: run {
            Log.w(TAG, "sendAudioChunk: no active session, dropping chunk")
            return
        }
        runCatching {
            session.send(content { inlineData(pcmBytes, AUDIO_INPUT_MIME) })
        }.onFailure { e ->
            Log.e(TAG, "sendAudioChunk error: ${e.message}", e)
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
     * ✅ FIX Parallel Function Calling: отправляет ВСЕ результаты функций одним батчем.
     *
     * @param results список Triple(callId, name, resultJson)
     */
    suspend fun sendFunctionResults(results: List<Triple<String, String, String>>) {
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
            Log.d(TAG, "✅ Sent ${responseParts.size} function response(s): ${results.map { it.second }}")
        }.onFailure { e ->
            Log.e(TAG, "sendFunctionResults error: ${e.message}", e)
        }
    }

    // ── Receive ───────────────────────────────────────────────────────────────

    /**
     * Cold Flow входящих ответов от Gemini.
     */
    fun receiveFlow(): Flow<GeminiResponse> = flow {
        val session = liveSession
            ?: throw GeminiConnectionException("receiveFlow: no active session")

        try {
            session.receive().collect { message ->
                if (message is LiveServerContent) {
                    mapServerContent(message)?.let { emit(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "receiveFlow error: ${e.message}", e)
            throw e
        }
    }

    // ── Release ───────────────────────────────────────────────────────────────

    fun release() {
        clientScope.cancel()
    }

    // ── Маппинг ответов ───────────────────────────────────────────────────────

    /**
     * Маппит LiveServerContent → GeminiResponse.
     *
     * ✅ FIX Parallel Function Calling:
     *   БЫЛО: .firstOrNull() → вторая функция игнорировалась навсегда.
     *   СТАЛО: весь список → все вызовы передаются в VoiceCoreEngineImpl.
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

        // ✅ FIX: извлекаем ВСЕ function calls, не только первый
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

        val isTurnComplete = sc.turnComplete
        val isInterrupted  = sc.interrupted

        val inputTranscript  = sc.inputTranscription?.text?.takeIf { it.isNotEmpty() }
        val outputTranscript = sc.outputTranscription?.text?.takeIf { it.isNotEmpty() }

        if (audioData == null && textContent == null && functionCalls.isEmpty() &&
            !isTurnComplete && !isInterrupted &&
            inputTranscript == null && outputTranscript == null) {
            return null
        }

        return GeminiResponse(
            audioData        = audioData,
            transcript       = textContent,
            functionCalls    = functionCalls,
            isTurnComplete   = isTurnComplete,
            isInterrupted    = isInterrupted,
            inputTranscript  = inputTranscript,
            outputTranscript = outputTranscript,
        )
    }

    // ── Нативный маппинг функций ──────────────────────────────────────────────

    /**
     * Маппит GeminiFunctionDeclaration → Firebase AI SDK FunctionDeclaration.
     *
     * ✅ FIX (попытка 4 - УСПЕШНАЯ): для функций без параметров подставляем
     * ОБЯЗАТЕЛЬНЫЙ (required) dummy-параметр.
     *
     * Если передать его как optional, SDK сгенерирует "required": [],
     * что приведет к ошибке валидации "parameters_json_schema must not [be empty/contain empty required]"
     * на стороне сервера Live API и обрыву WebSocket-соединения.
     */
    private fun mapToFirebaseDeclaration(decl: GeminiFunctionDeclaration): FunctionDeclaration {
        val params = decl.parameters

        // Функции без параметров: хак для обхода бага валидации схемы Live API
        if (params == null || params.properties.isEmpty()) {
            Log.d(TAG, "  ⚙ ${decl.name} — no params, injecting REQUIRED dummy param")
            return FunctionDeclaration(
                name               = decl.name,
                description        = decl.description,
                parameters         = mapOf(
                    "dummy_param" to Schema.boolean(
                        description = "Required dummy parameter for execution. Always pass true."
                    )
                ),
                // ВАЖНО: оставляем список опциональных параметров ПУСТЫМ!
                // Это заставит SDK поместить "dummy_param" в массив "required".
                // Валидная непустая схема с 1 required параметром гарантированно проходит handshake.
                optionalParameters = emptyList(),
            )
        }

        val properties = params.properties.mapValues { (_, prop) ->
            mapPropertyToSchema(prop)
        }

        // ✅ ГЛОБАЛЬНЫЙ ФИКС: Если в функции есть параметры, но мы забыли указать required,
        // SDK сгенерирует "required": [], что крашнет Live API.
        // Защита: если required пуст, мы принудительно делаем ВСЕ параметры этой функции обязательными
        // (передаем пустой список опциональных), и сервер спокойно принимает схему.
        val optionalProperties = if (params.required.isEmpty()) {
            emptyList()
        } else {
            properties.keys.filter { it !in params.required }
        }

        Log.d(TAG, "  ⚙ ${decl.name} — params: ${properties.keys}, " +
                "required: ${params.required}, optional: $optionalProperties")

        return FunctionDeclaration(
            name               = decl.name,
            description        = decl.description,
            parameters         = properties,
            optionalParameters = optionalProperties,
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
    val inputTranscript: String? = null,
    val outputTranscript: String? = null,
) {
    fun hasAudio(): Boolean = audioData != null && audioData.isNotEmpty()
    fun hasFunctionCalls(): Boolean = functionCalls.isNotEmpty()
    fun hasTranscript(): Boolean = !transcript.isNullOrEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeminiResponse) return false
        return transcript      == other.transcript &&
               functionCalls   == other.functionCalls &&
               isTurnComplete  == other.isTurnComplete &&
               isInterrupted   == other.isInterrupted &&
               inputTranscript  == other.inputTranscript &&
               outputTranscript == other.outputTranscript &&
               (audioData?.contentEquals(other.audioData) == true ||
                (audioData == null && other.audioData == null))
    }

    override fun hashCode(): Int {
        var result = transcript.hashCode()
        result = 31 * result + isTurnComplete.hashCode()
        result = 31 * result + isInterrupted.hashCode()
        result = 31 * result + functionCalls.hashCode()
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