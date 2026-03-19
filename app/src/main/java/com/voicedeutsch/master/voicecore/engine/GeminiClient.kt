package com.voicedeutsch.master.voicecore.engine

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
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
 * GeminiClient v2 — РУЧНОЙ режим Firebase AI Logic Live API.
 *
 * ВМЕСТО startAudioConversation() (SDK сам управляет аудио) используем:
 *   - sendAudio(pcm)        → отправляем PCM с микрофона
 *   - session.receive()     → получаем PCM ответа + function calls
 *
 * Это даёт ПРЯМОЙ ДОСТУП к сырым PCM байтам Gemini → SpectralFeatureExtractor → аватар.
 *
 * ════════════════════════════════════════════════════════════════════
 * Основано на официальном Kotlin-сниппете из документации Firebase:
 *
 *   session.receive().collect {
 *       if (it.turnComplete) { session.stopReceiving() }
 *       playAudio(it.data)  // PCM 16bit 24kHz
 *   }
 *
 * ⚠️ BOM ВЕРСИЯ: требуется firebase-bom ≥ 34.8.0 для sendAudioRealtime.
 *    Если у тебя 34.5.0 — нужно обновить в libs.versions.toml.
 *
 * ⚠️ PREVIEW API: типы могут отличаться от документации.
 *    Помечены комментариями ⚠️COMPILE_CHECK — места возможных ошибок.
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
    // Сырые PCM байты (16bit, 24kHz, mono) из ответов Gemini.
    // Подписчики: AudioTrack (воспроизведение) + AvatarAudioAnalyzer (аватар)

    private val _audioOutput = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val audioOutput: SharedFlow<ByteArray> = _audioOutput.asSharedFlow()

    // ── Model speaking state ──────────────────────────────────────────────────
    // true когда Gemini говорит → пауза микрофона + анимация аватара

    private val _modelSpeaking = MutableStateFlow(false)
    val modelSpeaking: StateFlow<Boolean> = _modelSpeaking.asStateFlow()

    // ── Receive control ───────────────────────────────────────────────────────

    @Volatile
    private var receiving = false

    // ══════════════════════════════════════════════════════════════════════════
    //  CONNECT — без изменений от v1
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
     * Формат: raw PCM 16bit, 16kHz, mono, little-endian.
     *
     * ⚠️COMPILE_CHECK: метод может называться иначе в твоей версии SDK.
     *   Альтернативы: sendAudioRealtime(InlineDataPart), sendRealtimeInput(...)
     */
    fun sendAudio(pcmBytes: ByteArray) {
        val session = liveSession ?: return
        try {
            // ⚠️COMPILE_CHECK: sendAudioRealtime(ByteArray)
            session.sendAudioRealtime(pcmBytes)
        } catch (e: Exception) {
            Log.e(TAG, "sendAudio error: ${e.message}")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  НОВОЕ: ручной приём ответов с доступом к PCM
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Запускает цикл приёма ответов от Gemini через session.receive().
     *
     * Из документации Firebase (Kotlin):
     *   session.receive().collect {
     *       if (it.turnComplete) { session.stopReceiving() }
     *       playAudio(it.data)
     *   }
     *
     * PCM байты эмитятся в [audioOutput] → AudioTrack + AvatarAudioAnalyzer.
     * Function calls обрабатываются через [onFunctionCall].
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
        Log.d(TAG, "startReceiving: начинаю приём ответов")

        try {
            // ⚠️COMPILE_CHECK: session.receive() возвращает Flow<???>.
            // Из документации — элементы имеют .data (ByteArray?) и .turnComplete (Boolean).
            // Если тип другой (LiveServerResponse, LiveContentResponse) — исправим по ошибке CI.
            session.receive().collect { response ->
                if (!receiving) return@collect

                // ── PCM audio data ───────────────────────────────────────
                // ⚠️COMPILE_CHECK: response.data — может быть .audioData, .bytes, или
                //   нужно доставать из response.serverContent?.modelTurn?.parts
                //     ?.filterIsInstance<InlineDataPart>()
                //     ?.firstOrNull { it.mimeType.startsWith("audio") }?.bytes
                val pcmData = response.data
                if (pcmData != null && pcmData.isNotEmpty()) {
                    if (!_modelSpeaking.value) {
                        _modelSpeaking.value = true
                        onTurnStarted()
                        Log.d(TAG, "Model turn started")
                    }
                    _audioOutput.tryEmit(pcmData)
                }

                // ── Function calls ───────────────────────────────────────
                // ⚠️COMPILE_CHECK: response.functionCalls — может не существовать.
                //   Альтернатива: response.serverContent?.modelTurn?.parts
                //     ?.filterIsInstance<FunctionCallPart>()
                response.functionCalls.forEach { functionCall ->
                    Log.d(TAG, "Function call: ${functionCall.name}")
                    try {
                        val result = onFunctionCall(functionCall)
                        session.send(content { part(result) })
                        Log.d(TAG, "Function response sent: ${functionCall.name}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Function call error: ${functionCall.name}", e)
                        val errorJson = kotlinx.serialization.json.JsonObject(
                            mapOf("error" to kotlinx.serialization.json.JsonPrimitive(
                                e.message ?: "function execution failed"
                            ))
                        )
                        runCatching {
                            session.send(content {
                                part(FunctionResponsePart(functionCall.name, errorJson, functionCall.id))
                            })
                        }
                    }
                }

                // ── Turn complete ────────────────────────────────────────
                // ⚠️COMPILE_CHECK: response.turnComplete — может быть Boolean или Boolean?
                if (response.turnComplete == true) {
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
    //  Оставлены для быстрого отката если ручной режим не заработает.
    // ══════════════════════════════════════════════════════════════════════════

    /** [FALLBACK] SDK-управляемый голосовой разговор. */
    suspend fun startConversation(
        onFunctionCall: (FunctionCallPart) -> FunctionResponsePart,
    ) {
        val session = sessionMutex.withLock { liveSession }
            ?: throw GeminiConnectionException("startConversation: no active session")
        session.startAudioConversation(onFunctionCall)
        Log.d(TAG, "Audio conversation started (SDK fallback)")
    }

    /** [FALLBACK] Остановка SDK-управляемого разговора. */
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
    //  TEXT / DISCONNECT / RELEASE — без изменений от v1
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
    //  Маппинг функций — без изменений от v1
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
