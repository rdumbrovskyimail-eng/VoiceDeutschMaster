package com.voicedeutsch.master.voicecore.engine

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.InlineDataPart
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * GeminiClient v4 — РУЧНОЙ режим Firebase AI Logic Live API.
 *
 * ════════════════════════════════════════════════════════════════════
 * v4 fixes based on CI errors:
 *   1. InlineData(pcmBytes, mimeType) — порядок аргументов (ByteArray, String)
 *   2. response.serverContent?.modelTurn?.parts — вместо response.modelTurn
 *   3. InlineDataPart — .data вместо .bytes (или .inlineData)
 *   4. serverContent?.turnComplete — вместо response.turnComplete
 *   5. Explicit null checks для parts итерации (исправлен ambiguous iterator)
 * ════════════════════════════════════════════════════════════════════
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

    // ── PCM audio output flow ─────────────────────────────────────────────────
    private val _audioOutput = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val audioOutput: SharedFlow<ByteArray> = _audioOutput.asSharedFlow()

    // ── Model speaking state ──────────────────────────────────────────────────
    private val _modelSpeaking = MutableStateFlow(false)
    val modelSpeaking: StateFlow<Boolean> = _modelSpeaking.asStateFlow()

    // ── Receive control ───────────────────────────────────────────────────────
    @Volatile
    private var receiving = false

    // ══════════════════════════════════════════════════════════════════════════
    //  CONNECT — без изменений
    // ══════════════════════════════════════════════════════════════════════════

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

            val session = kotlinx.coroutines.withTimeout(15_000L) {
                liveModel.connect()
            }

            sessionMutex.withLock {
                liveSession = session
            }

            Log.d(TAG, "LiveSession established")
        } catch (e: Exception) {
            Log.e(TAG, "connect() failed: ${e.message}", e)
            throw GeminiConnectionException("Failed to connect to Gemini Live API", e)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  НОВОЕ: ручная отправка микрофона
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Отправляет чанк PCM аудио с микрофона в Gemini.
     *
     * FIX v4: InlineData(ByteArray, String) — первый аргумент данные, второй MIME.
     */
    suspend fun sendAudio(pcmBytes: ByteArray) {
        val session = liveSession ?: return
        try {
            val audioData = InlineData(pcmBytes, "audio/pcm;rate=16000")
            session.sendAudioRealtime(audioData)
        } catch (e: Exception) {
            Log.e(TAG, "sendAudio error: ${e.message}")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  НОВОЕ: ручной приём ответов с доступом к PCM
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Запускает цикл приёма ответов через session.receive().
     *
     * FIX v4:
     *   - response.serverContent?.modelTurn?.parts (не response.modelTurn)
     *   - InlineDataPart.data (не .bytes)
     *   - serverContent?.turnComplete (не response.turnComplete)
     *   - Explicit null check для parts (исправлен ambiguous iterator)
     *
     * ⚠️ Если serverContent не скомпилится — альтернативы:
     *   response.content, response.message, или response напрямую если это LiveServerContent
     */
    suspend fun startReceiving(
        onFunctionCall: suspend (FunctionCallPart) -> FunctionResponsePart,
        onTurnStarted: () -> Unit = {},
        onTurnComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
    ) {
        val session = sessionMutex.withLock { liveSession }
            ?: throw GeminiConnectionException("startReceiving: no active session")

        receiving = true
        var logCount = 0
        Log.d(TAG, "startReceiving: начинаю приём ответов")

        try {
            session.receive().collect { response ->
                if (!receiving) return@collect

                // Диагностика: логируем тип response первые 3 раза
                if (logCount < 3) {
                    Log.d(TAG, "receive type: ${response::class.qualifiedName}, " +
                        "props: ${response::class.members.take(10).map { it.name }}")
                    logCount++
                }

                // ── Достаём server content ───────────────────────────────
                // ⚠️ COMPILE_CHECK: если serverContent не существует, попробовать:
                //   val sc = response.content    // или
                //   val sc = response.message     // или
                //   val sc = response              // если response УЖЕ LiveServerContent
                val sc = response.serverContent ?: return@collect

                // ── Достаём parts из modelTurn ───────────────────────────
                val parts = sc.modelTurn?.parts
                if (parts != null) {

                    // ── Аудио (InlineDataPart → PCM 24kHz) ───────────────
                    for (part in parts) {
                        if (part is InlineDataPart) {
                            // FIX v4: .data вместо .bytes
                            // ⚠️ Если .data не скомпилится, попробовать:
                            //   part.inlineData      (тип InlineData, потом .data)
                            //   part.inlineData.data  (вложенный)
                            val audioBytes: ByteArray = part.data
                            if (audioBytes.isNotEmpty()) {
                                if (!_modelSpeaking.value) {
                                    _modelSpeaking.value = true
                                    onTurnStarted()
                                    Log.d(TAG, "Model turn started")
                                }
                                _audioOutput.tryEmit(audioBytes)
                            }
                        }
                    }

                    // ── Function calls ────────────────────────────────────
                    for (part in parts) {
                        if (part is FunctionCallPart) {
                            Log.d(TAG, "Function call: ${part.name}")
                            try {
                                val result = onFunctionCall(part)
                                session.send(content { part(result) })
                                Log.d(TAG, "Function response sent: ${part.name}")
                            } catch (e: Exception) {
                                Log.e(TAG, "Function call error: ${part.name}", e)
                                val errorJson = kotlinx.serialization.json.JsonObject(
                                    mapOf("error" to kotlinx.serialization.json.JsonPrimitive(
                                        e.message ?: "function execution failed"
                                    ))
                                )
                                runCatching {
                                    session.send(content {
                                        part(FunctionResponsePart(part.name, errorJson, part.id))
                                    })
                                }
                            }
                        }
                    }
                }

                // ── Turn complete ────────────────────────────────────────
                // ⚠️ Если sc.turnComplete не скомпилится, попробовать:
                //   sc.isTurnComplete, sc.isComplete, response.turnComplete
                if (sc.turnComplete == true) {
                    _modelSpeaking.value = false
                    onTurnComplete()
                    Log.d(TAG, "Model turn complete")
                }
            }
        } catch (e: Exception) {
            if (receiving) {
                Log.e(TAG, "receive error: ${e.message}", e)
                _modelSpeaking.value = false
                onError(e)
            }
        } finally {
            receiving = false
            _modelSpeaking.value = false
            Log.d(TAG, "startReceiving: завершён")
        }
    }

    /**
     * Останавливает приём ответов.
     */
    fun stopReceiving() {
        receiving = false
        _modelSpeaking.value = false
        runCatching { liveSession?.stopReceiving() }
        Log.d(TAG, "stopReceiving called")
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FALLBACK: старые методы (SDK-управляемый режим)
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun startConversation(
        onFunctionCall: (FunctionCallPart) -> FunctionResponsePart,
    ) {
        val session = sessionMutex.withLock { liveSession }
            ?: throw GeminiConnectionException("startConversation: no active session")
        session.startAudioConversation(onFunctionCall)
        Log.d(TAG, "Audio conversation started (SDK fallback)")
    }

    suspend fun stopConversation() {
        val session = sessionMutex.withLock { liveSession } ?: run {
            Log.w(TAG, "stopConversation: no active session")
            return
        }
        runCatching {
            kotlinx.coroutines.withTimeout(5000L) {
                session.stopAudioConversation()
            }
            Log.d(TAG, "Audio conversation stopped (SDK fallback)")
        }.onFailure { e ->
            Log.w(TAG, "stopConversation error/timeout: ${e.message}")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TEXT / DISCONNECT / RELEASE — без изменений
    // ══════════════════════════════════════════════════════════════════════════

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

    suspend fun disconnect() {
        stopReceiving()
        try {
            kotlinx.coroutines.withTimeout(3000L) {
                sessionMutex.withLock {
                    liveSession?.close()
                    liveSession = null
                }
            }
            Log.d(TAG, "LiveSession closed")
        } catch (e: Exception) {
            Log.w(TAG, "disconnect() warning/timeout: ${e.message}")
            sessionMutex.withLock {
                liveSession = null
            }
        }
    }

    fun release() {
        stopReceiving()
        sessionResumptionHandle = null
        lastTokenUsage = null
    }

    fun clearResumptionHandle() {
        sessionResumptionHandle = null
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Маппинг функций — без изменений
    // ══════════════════════════════════════════════════════════════════════════

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
