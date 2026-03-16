package com.voicedeutsch.master.voicecore.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.InlineData
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.sqrt
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
    private val scope: CoroutineScope,
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

    private val _amplitudeFlow = MutableSharedFlow<Float>(replay = 0, extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val amplitudeFlow: Flow<Float> = _amplitudeFlow.asSharedFlow()

    private var audioTrack: AudioTrack? = null
    private var receiveJob: Job? = null
    private var sendJob: Job? = null

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

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(24000)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(
                    AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2
                )
                .build()
                .also { it.play() }

            Log.d(TAG, "LiveSession established")
        } catch (e: Exception) {
            Log.e(TAG, "connect() failed: ${e.message}", e)
            throw GeminiConnectionException("Failed to connect to Gemini Live API", e)
        }
    }

    /**
     * Запускает голосовой разговор.
     * Вручную управляет микрофоном, отправкой/приёмом PCM-аудио и воспроизведением через AudioTrack.
     * Function calls обрабатываются через [onFunctionCall] callback.
     */
    fun startConversation(
        onFunctionCall: (FunctionCallPart) -> FunctionResponsePart,
    ) {
        val session = liveSession
            ?: throw GeminiConnectionException("startConversation: no active session")

        receiveJob = scope.launch {
            session.receive().collect { response ->
                response.audio?.let { pcmBytes ->
                    val rms = computeRms(pcmBytes)
                    _amplitudeFlow.tryEmit(rms)
                    audioTrack?.write(pcmBytes, 0, pcmBytes.size)
                }
                response.functionCalls?.forEach { call ->
                    val result = onFunctionCall(call)
                    session.sendFunctionResponse(listOf(result))
                }
            }
        }

        sendJob = scope.launch {
            val bufferSize = AudioRecord.getMinBufferSize(
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )
            recorder.startRecording()
            val buffer = ByteArray(bufferSize)
            try {
                while (isActive) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        session.send(InlineData(buffer.copyOf(read), "audio/pcm"))
                    }
                }
            } finally {
                recorder.stop()
                recorder.release()
            }
        }

        Log.d(TAG, "Audio conversation started")
    }

    /**
     * Останавливает голосовой разговор.
     * Отменяет корутины приёма и отправки, останавливает AudioTrack.
     */
    fun stopConversation() {
        receiveJob?.cancel()
        receiveJob = null
        sendJob?.cancel()
        sendJob = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
        }.onFailure { e ->
            Log.w(TAG, "stopConversation audioTrack error: ${e.message}")
        }
        Log.d(TAG, "Audio conversation stopped")
    }

    /**
     * Закрывает LiveSession и освобождает ресурсы соединения.
     */
    suspend fun disconnect() {
        try {
            receiveJob?.cancel()
            receiveJob = null
            sendJob?.cancel()
            sendJob = null
            sessionMutex.withLock {
                liveSession?.close()
                liveSession = null
            }
            runCatching {
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
            }.onFailure { e ->
                Log.w(TAG, "disconnect() audioTrack release warning: ${e.message}")
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

    // ── PCM RMS ───────────────────────────────────────────────────────────────

    private fun computeRms(pcm: ByteArray): Float {
        val shorts = ShortArray(pcm.size / 2)
        ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        if (shorts.isEmpty()) return 0f
        val sum = shorts.sumOf { (it / 32768.0).pow(2) }
        return sqrt(sum / shorts.size).toFloat().coerceIn(0f, 1f)
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
