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
import com.google.firebase.ai.type.LiveGenerativeModel
import com.google.firebase.ai.type.LiveServerContent
import com.google.firebase.ai.type.LiveServerMessage
import com.google.firebase.ai.type.LiveServerToolCall
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
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
 * АУДИО ФОРМАТ:
 *   Вход:  PCM 16-bit, 16 kHz, mono  → sendRealtimeInput(audioData, mimeType)
 *   Выход: PCM 16-bit, 24 kHz, mono  ← LiveServerContent.modelTurn.parts
 *
 * ════════════════════════════════════════════════════════════════════════════
 * BREAKING CHANGE (firebase-ai SDK, апрель 2025):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   LiveContentResponse УДАЛЁН → заменён на LiveServerMessage с subclasses:
 *   - LiveServerMessage с payload: LiveServerContent (аудио/текст/turnComplete)
 *   - LiveServerMessage с payload: LiveServerToolCall (function calls)
 *   - LiveServerMessage с payload: LiveServerToolCallCancellation
 *
 * @param config  конфигурация модели (model name, voice, sample rates и т.д.)
 * @param json    экземпляр Json для сериализации function declarations из FunctionRouter
 */
@OptIn(PublicPreviewAPI::class)
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

    private val responseChannel = Channel<GeminiResponse>(Channel.UNLIMITED)

    // ── Инициализация модели ──────────────────────────────────────────────────

    private fun buildLiveModel(context: ContextBuilder.SessionContext): LiveGenerativeModel {

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

        // ✅ GenerativeBackend — из com.google.firebase.ai (не .type)
        return Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
            modelName = config.modelName.ifBlank { DEFAULT_MODEL },
            generationConfig = liveGenerationConfig {
                responseModality = ResponseModality.AUDIO

                speechConfig = SpeechConfig(
                    voice = Voice(config.voiceName)
                )

                inputAudioTranscription  = AudioTranscriptionConfig()
                outputAudioTranscription = AudioTranscriptionConfig()
            },
            tools = tools,
            systemInstruction = systemInstruction,
        )
    }

    // ── Public API ────────────────────────────────────────────────────────────

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
     * Формат: PCM 16-bit signed, 16 kHz, mono
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
     * Отправляет результат выполнения функции обратно в Gemini.
     *
     * ✅ ИСПРАВЛЕНО: session.sendFunctionResponse(List<FunctionResponsePart>)
     * Создаём FunctionResponsePart из id + name + resultJson.
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
                id       = callId,
            )
            session.sendFunctionResponse(listOf(functionResponse))
        }.onFailure { e ->
            Log.e(TAG, "sendFunctionResult error: ${e.message}", e)
        }
    }

    /**
     * Cold Flow входящих ответов от Gemini.
     *
     * ✅ ИСПРАВЛЕНО: session.receive() теперь возвращает Flow<LiveServerMessage>
     * (не Flow<LiveContentResponse>). Маппим через mapLiveServerMessage().
     */
    fun receiveFlow(): Flow<GeminiResponse> = flow {
        val session = liveSession
            ?: throw GeminiConnectionException("receiveFlow: no active session")

        try {
            session.receive().collect { serverMessage ->
                mapLiveServerMessage(serverMessage)?.let { emit(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "receiveFlow error: ${e.message}", e)
            throw e
        }
    }

    /**
     * Режим A: SDK управляет захватом микрофона и воспроизведением аудио.
     * В текущей архитектуре НЕ используется — проект использует режим B.
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
     * ✅ ИСПРАВЛЕНО: LiveContentResponse УДАЛЁН из SDK.
     * Новый API — LiveServerMessage с payload:
     *
     *   message.serverContent → LiveServerContent (аудио/текст/isTurnComplete/isInterrupted)
     *   message.toolCall      → LiveServerToolCall (function calls)
     *
     * Доступ к аудио: serverContent.modelTurn?.parts → InlineDataPart (mimeType=audio/pcm)
     * Доступ к тексту: serverContent.modelTurn?.parts → TextPart
     * Транскрипции: serverContent.inputTranscription / outputTranscription
     */
    @OptIn(PublicPreviewAPI::class)
    private fun mapLiveServerMessage(message: LiveServerMessage): GeminiResponse? {
        // Проверяем toolCall (function calling)
        val toolCall = message.toolCall
        if (toolCall != null && toolCall.functionCalls.isNotEmpty()) {
            val call = toolCall.functionCalls.first()
            val functionCall = GeminiFunctionCall(
                id       = call.id ?: call.name,
                name     = call.name,
                argsJson = json.encodeToString(
                    JsonObject.serializer(),
                    call.args?.let { JsonObject(it) } ?: buildJsonObject {}
                ),
            )
            return GeminiResponse(
                audioData      = null,
                transcript     = null,
                functionCall   = functionCall,
                isTurnComplete = false,
                isInterrupted  = false,
            )
        }

        // Проверяем serverContent (аудио, текст, транскрипции, turnComplete, interrupted)
        val serverContent = message.serverContent ?: return null

        val isTurnComplete = serverContent.turnComplete
        val isInterrupted  = serverContent.interrupted

        // Собираем аудио из modelTurn.parts (InlineDataPart с mimeType audio/pcm)
        val audioData: ByteArray? = serverContent.modelTurn?.parts
            ?.filterIsInstance<com.google.firebase.ai.type.InlineDataPart>()
            ?.firstOrNull { it.mimeType.startsWith("audio/pcm") }
            ?.data
            ?.takeIf { it.isNotEmpty() }

        // Текстовый контент из modelTurn.parts (TextPart)
        val textContent: String? = serverContent.modelTurn?.parts
            ?.filterIsInstance<com.google.firebase.ai.type.TextPart>()
            ?.joinToString("") { it.text }
            ?.takeIf { it.isNotEmpty() }

        // Транскрипции
        val inputTranscript  = serverContent.inputTranscription?.text
        val outputTranscript = serverContent.outputTranscription?.text

        val transcript = outputTranscript ?: inputTranscript ?: textContent

        // Если ответ полностью пустой — не эмитируем
        if (audioData == null && transcript.isNullOrEmpty() &&
            !isTurnComplete && !isInterrupted) {
            return null
        }

        return GeminiResponse(
            audioData        = audioData,
            transcript       = transcript,
            functionCall     = null,
            isTurnComplete   = isTurnComplete,
            isInterrupted    = isInterrupted,
            inputTranscript  = inputTranscript,
            outputTranscript = outputTranscript,
        )
    }

    /**
     * Парсит JSON-строку объявления функции из FunctionRouter в FunctionDeclaration SDK.
     *
     * ✅ ИСПРАВЛЕНО: FunctionDeclaration(name, description, parameters: Map<String, Schema>)
     * Старый API принимал Schema? напрямую — новый принимает Map<String, Schema> для properties.
     */
    private fun parseFunctionDeclaration(declarationJson: String): FunctionDeclaration {
        val obj = json.parseToJsonElement(declarationJson) as JsonObject

        val name        = (obj["name"]        as? JsonPrimitive)?.content ?: error("Missing 'name'")
        val description = (obj["description"] as? JsonPrimitive)?.content ?: ""
        val parametersObj = obj["parameters"] as? JsonObject

        return if (parametersObj == null) {
            FunctionDeclaration(
                name        = name,
                description = description,
            )
        } else {
            val propertiesJson = parametersObj["properties"] as? JsonObject ?: JsonObject(emptyMap())
            val required = (parametersObj["required"] as? kotlinx.serialization.json.JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.content }
                ?: emptyList()

            val properties: Map<String, Schema> = propertiesJson.mapValues { (_, v) ->
                parseSchema(v as JsonObject)
            }

            FunctionDeclaration(
                name        = name,
                description = description,
                parameters  = properties,
                optionalParameters = properties.keys.filter { it !in required }.toList(),
            )
        }
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
