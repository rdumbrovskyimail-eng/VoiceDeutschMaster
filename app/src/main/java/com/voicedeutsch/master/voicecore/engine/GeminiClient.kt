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

@OptIn(PublicPreviewAPI::class)
class GeminiClient(
    private val config: GeminiConfig,
    private val json: Json,
) {
    companion object {
        private const val TAG = "GeminiClient"
        private const val AUDIO_INPUT_MIME = "audio/pcm;rate=16000"
    }

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var liveSession: LiveSession? = null

    // ── Connect ───────────────────────────────────────────────────────────────

    suspend fun connect(context: ContextBuilder.SessionContext) {
        try {
            Log.d(TAG, "Connecting to Gemini Live API [model=${config.modelName}]")

            // ИЗМЕНЕНО: нативный маппинг GeminiFunctionDeclaration → Firebase FunctionDeclaration
            // Вместо парсинга JSON-строк — прямое преобразование data class в SDK-объекты.
            val firebaseDeclarations = context.functionDeclarations.mapNotNull { decl ->
                runCatching { mapToFirebaseDeclaration(decl) }
                    .onFailure { Log.w(TAG, "Skipping invalid function: ${it.message}") }
                    .getOrNull()
            }

            val tools = firebaseDeclarations
                .takeIf { it.isNotEmpty() }
                ?.let { listOf(Tool.functionDeclarations(it)) }

            val liveModel = Firebase.ai.liveModel(
                modelName = config.modelName,
                generationConfig = liveGenerationConfig {
                    responseModality = ResponseModality.AUDIO
                    speechConfig = SpeechConfig(voice = Voice(config.voiceName))
                },
                tools = tools,
                systemInstruction = content(role = "user") { text(context.fullContext) },
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
            liveSession?.close()
            liveSession = null
            Log.d(TAG, "LiveSession closed")
        } catch (e: Exception) {
            Log.w(TAG, "disconnect() warning: ${e.message}")
        }
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    suspend fun sendAudioChunk(pcmBytes: ByteArray) {
        val session = liveSession ?: return
        runCatching {
            session.send(content { inlineData(pcmBytes, AUDIO_INPUT_MIME) })
        }.onFailure { e ->
            Log.e(TAG, "sendAudioChunk error: ${e.message}", e)
        }
    }

    suspend fun sendText(text: String) {
        val session = liveSession ?: return
        runCatching { session.send(text) }
            .onFailure { e -> Log.e(TAG, "sendText error: ${e.message}", e) }
    }

    suspend fun sendFunctionResult(callId: String, name: String, resultJson: String) {
        val session = liveSession ?: return
        runCatching {
            val responseJson = try {
                json.parseToJsonElement(resultJson) as? JsonObject
                    ?: buildJsonObject { put("result", JsonPrimitive(resultJson)) }
            } catch (e: Exception) {
                buildJsonObject { put("result", JsonPrimitive(resultJson)) }
            }
            session.sendFunctionResponse(listOf(FunctionResponsePart(name, responseJson)))
        }.onFailure { e ->
            Log.e(TAG, "sendFunctionResult error: ${e.message}", e)
        }
    }

    // ── Receive ───────────────────────────────────────────────────────────────

    fun receiveFlow(): Flow<GeminiResponse> = flow {
        val session = liveSession
            ?: throw GeminiConnectionException("receiveFlow: no active session")

        session.receive().collect { message ->
            if (message is LiveServerContent) {
                mapServerContent(message)?.let { emit(it) }
            }
        }
    }

    // ── Release ───────────────────────────────────────────────────────────────

    fun release() {
        clientScope.cancel()
    }

    // УДАЛЕНО: startManagedAudioConversation / stopManagedAudioConversation
    // Managed mode ломает ручной контроль прерываний (interrupted: true).
    // Используем архитектуру "2 корутины" в VoiceCoreEngineImpl.

    // ── Маппинг ответов ───────────────────────────────────────────────────────

    private fun mapServerContent(sc: LiveServerContent): GeminiResponse? {
        val parts = sc.content?.parts.orEmpty()

        val audioData = parts
            .filterIsInstance<InlineDataPart>()
            .firstOrNull()?.inlineData
            ?.takeIf { it.isNotEmpty() }

        val textContent = parts
            .filterIsInstance<TextPart>()
            .joinToString("") { it.text }
            .takeIf { it.isNotEmpty() }

        val functionCall = parts
            .filterIsInstance<FunctionCallPart>()
            .firstOrNull()?.let { fc ->
                GeminiFunctionCall(
                    id = fc.name,
                    name = fc.name,
                    argsJson = fc.args.toString(),
                )
            }

        val inTrans = sc.inputTranscription?.text?.takeIf { it.isNotEmpty() }
        val outTrans = sc.outputTranscription?.text?.takeIf { it.isNotEmpty() }

        if (audioData == null && textContent == null && functionCall == null &&
            !sc.turnComplete && !sc.interrupted &&
            inTrans == null && outTrans == null) return null

        return GeminiResponse(
            audioData = audioData,
            transcript = textContent,
            functionCall = functionCall,
            isTurnComplete = sc.turnComplete,
            isInterrupted = sc.interrupted,
            inputTranscript = inTrans,
            outputTranscript = outTrans,
        )
    }

    // ── Нативный маппинг функций (без JSON) ───────────────────────────────────

    // ИЗМЕНЕНО: вместо parseFunctionDeclaration(jsonString) и parseSchema(jsonObject)
    // теперь прямой маппинг из GeminiFunctionDeclaration → Firebase FunctionDeclaration.
    // В 10 раз быстрее и не падает с SerializationException.

    private fun mapToFirebaseDeclaration(decl: GeminiFunctionDeclaration): FunctionDeclaration {
        val params = decl.parameters
        return FunctionDeclaration(
            name = decl.name,
            description = decl.description,
            parameters = params?.let { p ->
                Schema.obj(
                    properties = p.properties.mapValues { (_, prop) ->
                        mapPropertyToSchema(prop)
                    },
                    optionalProperties = p.properties.keys
                        .filter { it !in p.required }
                        .toList(),
                )
            } ?: emptyMap<String, Schema>().let {
                // Функция без параметров
                Schema.obj(properties = it, optionalProperties = emptyList())
            },
        )
    }

    private fun mapPropertyToSchema(prop: com.voicedeutsch.master.voicecore.functions.GeminiProperty): Schema {
        return when (prop.type.uppercase()) {
            "STRING"  -> Schema.string(description = prop.description)
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
        return transcript == other.transcript &&
               functionCall == other.functionCall &&
               isTurnComplete == other.isTurnComplete &&
               isInterrupted == other.isInterrupted &&
               inputTranscript == other.inputTranscript &&
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