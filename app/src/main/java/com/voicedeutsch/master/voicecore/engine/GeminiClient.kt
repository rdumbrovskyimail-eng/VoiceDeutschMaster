package com.voicedeutsch.master.voicecore.engine

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import com.voicedeutsch.master.voicecore.context.ContextBuilder
import com.voicedeutsch.master.voicecore.functions.GeminiFunctionDeclaration
import com.voicedeutsch.master.voicecore.functions.GeminiProperty
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
/**
 * GeminiClient — обёртка над Firebase AI Logic Live API SDK.
 *
 * Использует startAudioConversation / stopAudioConversation:
 * SDK **сам** управляет микрофоном, отправкой аудио, приёмом ответов,
 * воспроизведением голоса AI, обработкой function calls через callback.
 */
@OptIn(PublicPreviewAPI::class)
class GeminiClient(
    config: GeminiConfig,
) {
    var config: GeminiConfig = config
        internal set

    companion object {
        private const val TAG = "GeminiClient"
    }

    @Volatile private var liveSession: LiveSession? = null
    private val sessionMutex = Mutex()

    @Volatile var sessionResumptionHandle: String? = null
        private set

    @Volatile var lastTokenUsage: TokenUsage? = null
        private set

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

            val liveModel = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
                modelName = config.modelName,
                generationConfig = liveConfig,
                systemInstruction = content { text(context.fullContext) },
                tools = toolsList,
            )

            val session = liveModel.connect()

            sessionMutex.withLock {
                liveSession = session
            }

            Log.d(TAG, "LiveSession established")
        } catch (e: Exception) {
            Log.e(TAG, "connect() failed: ${e.message}", e)
            throw GeminiConnectionException("Failed to connect to Gemini Live API", e)
        }
    }

    /**
     * Запускает голосовой разговор.
     * SDK сам управляет микрофоном, отправкой/приёмом аудио и воспроизведением.
     * Function calls обрабатываются синхронно через [onFunctionCall] callback.
     */
    suspend fun startConversation(
        onFunctionCall: (FunctionCallPart) -> FunctionResponsePart,
    ) {
        val session = sessionMutex.withLock { liveSession }
            ?: throw GeminiConnectionException("startConversation: no active session")

        session.startAudioConversation(onFunctionCall)
        Log.d(TAG, "Audio conversation started")
    }

    /**
     * Останавливает голосовой разговор.
     * Микрофон и воспроизведение останавливаются SDK-ом.
     */
    suspend fun stopConversation() {
        val session = sessionMutex.withLock { liveSession } ?: run {
            Log.w(TAG, "stopConversation: no active session")
            return
        }
        runCatching {
            session.stopAudioConversation()
            Log.d(TAG, "Audio conversation stopped")
        }.onFailure { e ->
            Log.w(TAG, "stopConversation error: ${e.message}")
        }
    }

    /**
     * Закрывает LiveSession и освобождает ресурсы соединения.
     */
    suspend fun disconnect() {
        try {
            sessionMutex.withLock {
                liveSession?.close()
                liveSession = null
            }
            Log.d(TAG, "LiveSession closed")
        } catch (e: Exception) {
            Log.w(TAG, "disconnect() warning: ${e.message}")
        }
    }

    /**
     * Отправляет текстовое сообщение в Gemini Live API.
     */
    suspend fun sendText(text: String) {
        val session = sessionMutex.withLock { liveSession } ?: run {
            Log.w(TAG, "sendText: no active session")
            return
        }
        runCatching {
            session.send(content { text(text) })
        }.onFailure { e ->
            Log.e(TAG, "sendText error: ${e.message}", e)
        }
    }

    // ── Release ───────────────────────────────────────────────────────────────

    fun release() {
        sessionResumptionHandle = null
        lastTokenUsage = null
    }

    fun clearResumptionHandle() {
        sessionResumptionHandle = null
    }

    // ── Нативный маппинг функций ──────────────────────────────────────────────

    private fun mapToFirebaseDeclaration(decl: GeminiFunctionDeclaration): FunctionDeclaration? {
        val params = decl.parameters

        if (params == null || params.properties.isEmpty()) {
            Log.d(TAG, "  ${decl.name} — no parameters (injecting dummy optional param)")
            return FunctionDeclaration(
                name = decl.name,
                description = decl.description,
                parameters = mapOf(
                    "unused_parameter" to Schema.boolean("Ignored parameter for Live API compatibility")
                ),
                optionalParameters = listOf("unused_parameter")
            )
        }

        val properties = params.properties.mapValues { (_, prop) ->
            mapPropertyToSchema(prop)
        }

        val optionalProperties = properties.keys.filter { it !in params.required }

        Log.d(TAG, "  ${decl.name} — params: ${properties.keys}, " +
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

class GeminiConnectionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
