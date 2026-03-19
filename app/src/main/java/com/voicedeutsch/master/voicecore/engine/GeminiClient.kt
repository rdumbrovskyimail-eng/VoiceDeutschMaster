```kotlin
package com.voicedeutsch.master.voicecore.engine

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import com.voicedeutsch.master.voicecore.context.ContextBuilder
import com.voicedeutsch.master.voicecore.functions.GeminiFunctionDeclaration
import com.voicedeutsch.master.voicecore.functions.GeminiProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

@OptIn(PublicPreviewAPI::class)
class GeminiClient(
    config: GeminiConfig,
) {
    var config: GeminiConfig = config
        internal set

    companion object {
        private const val TAG = "GeminiClient"

        // Микрофон: 16kHz, 16bit PCM little-endian
        private const val MIC_SAMPLE_RATE   = 16_000
        private const val MIC_CHANNEL_IN    = AudioFormat.CHANNEL_IN_MONO
        private const val MIC_ENCODING      = AudioFormat.ENCODING_PCM_16BIT
        private const val MIC_MIME          = "audio/pcm;rate=16000"

        // Спикер: 24kHz, 16bit PCM little-endian (Gemini output)
        private const val SPK_SAMPLE_RATE   = 24_000
        private const val SPK_CHANNEL_OUT   = AudioFormat.CHANNEL_OUT_MONO
        private const val SPK_ENCODING      = AudioFormat.ENCODING_PCM_16BIT
    }

    // ── Session ───────────────────────────────────────────────────────────────

    @Volatile private var liveSession: LiveSession? = null
    private val sessionMutex = Mutex()

    @Volatile var sessionResumptionHandle: String? = null
        private set

    @Volatile var lastTokenUsage: TokenUsage? = null
        private set

    data class TokenUsage(
        val promptTokenCount: Int    = 0,
        val responseTokenCount: Int  = 0,
        val totalTokenCount: Int     = 0,
    )

    // ── Manual audio infra ────────────────────────────────────────────────────

    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var recordJob:  Job? = null
    @Volatile private var receiveJob: Job? = null
    @Volatile private var audioTrack: AudioTrack? = null

    // ── CONNECT ───────────────────────────────────────────────────────────────

    suspend fun connect(context: ContextBuilder.SessionContext) {
        try {
            Log.d(TAG, "Connecting [model=${config.modelName}]")

            val firebaseDeclarations = context.functionDeclarations.mapNotNull { decl ->
                runCatching { mapToFirebaseDeclaration(decl) }
                    .onFailure { Log.w(TAG, "Skip ${decl.name}: ${it.message}") }
                    .getOrNull()
            }

            val toolsList = buildList<Tool> {
                if (firebaseDeclarations.isNotEmpty())
                    add(Tool.functionDeclarations(firebaseDeclarations))
                if (config.enableSearchGrounding)
                    add(Tool.googleSearch())
            }

            val liveConfig = liveGenerationConfig {
                responseModality = ResponseModality.AUDIO
                speechConfig     = SpeechConfig(voice = Voice(config.voiceName))
                if (config.transcriptionConfig.outputTranscriptionEnabled)
                    outputAudioTranscription = AudioTranscriptionConfig()
                if (config.transcriptionConfig.inputTranscriptionEnabled)
                    inputAudioTranscription  = AudioTranscriptionConfig()
            }

            val liveModel = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
                modelName         = config.modelName,
                generationConfig  = liveConfig,
                systemInstruction = content { text(context.fullContext) },
                tools             = toolsList,
            )

            val session = withTimeout(15_000L) { liveModel.connect() }
            sessionMutex.withLock { liveSession = session }

            Log.d(TAG, "LiveSession established")
        } catch (e: Exception) {
            Log.e(TAG, "connect() failed: ${e.message}", e)
            throw GeminiConnectionException("Failed to connect to Gemini Live API", e)
        }
    }

    // ── START / STOP CONVERSATION (ручной режим) ──────────────────────────────

    /**
     * Запускает ручной режим:
     *  - AudioRecord → sendAudioRealtime (отправка PCM на Gemini)
     *  - session.receive() → AudioTrack (воспроизведение ответа)
     *  - function calls → onFunctionCall callback
     *
     * Блокирует до вызова stopConversation().
     */
    @SuppressLint("MissingPermission")
    suspend fun startConversation(
        onFunctionCall: (FunctionCallPart) -> FunctionResponsePart,
    ) {
        val session = sessionMutex.withLock { liveSession }
            ?: throw GeminiConnectionException("startConversation: no active session")

        Log.d(TAG, "startConversation: запуск ручного режима")

        // ── AudioTrack для воспроизведения ответа ────────────────────────────
        val bufSize = AudioTrack.getMinBufferSize(SPK_SAMPLE_RATE, SPK_CHANNEL_OUT, SPK_ENCODING)
        val track = AudioTrack.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SPK_SAMPLE_RATE)
                    .setEncoding(SPK_ENCODING)
                    .setChannelMask(SPK_CHANNEL_OUT)
                    .build()
            )
            .setBufferSizeInBytes(bufSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()
        audioTrack = track

        // ── Запись микрофона и отправка в Gemini ─────────────────────────────
        recordJob = audioScope.launch {
            val minBuf = AudioRecord.getMinBufferSize(MIC_SAMPLE_RATE, MIC_CHANNEL_IN, MIC_ENCODING)
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MIC_SAMPLE_RATE,
                MIC_CHANNEL_IN,
                MIC_ENCODING,
                minBuf * 4,
            )
            recorder.startRecording()
            Log.d(TAG, "AudioRecord started")

            val buf = ByteArray(minBuf)
            try {
                while (isActive) {
                    val read = recorder.read(buf, 0, buf.size)
                    if (read > 0) {
                        val chunk = buf.copyOf(read)
                        runCatching {
                            session.sendAudioRealtime(InlineData(chunk, MIC_MIME))
                        }.onFailure { Log.w(TAG, "sendAudioRealtime error: ${it.message}") }
                    }
                }
            } finally {
                recorder.stop()
                recorder.release()
                Log.d(TAG, "AudioRecord stopped")
            }
        }

        // ── Приём ответа от Gemini ────────────────────────────────────────────
        receiveJob = audioScope.launch {
            Log.d(TAG, "receive loop started")
            try {
                session.receive().collect { response ->
                    // Аудио PCM 24kHz
                    val parts = response.serverContent?.modelTurn?.parts
                    parts?.forEach { part ->
                        if (part is InlineDataPart) {
                            val pcm = part.data
                            if (pcm.isNotEmpty()) {
                                audioTrack?.write(pcm, 0, pcm.size)
                            }
                        }
                        if (part is FunctionCallPart) {
                            Log.d(TAG, "Function call: ${part.name}")
                            try {
                                val result = onFunctionCall(part)
                                session.send(content { part(result) })
                            } catch (e: Exception) {
                                Log.e(TAG, "Function call error: ${part.name}", e)
                            }
                        }
                    }
                    if (response.serverContent?.turnComplete == true) {
                        Log.d(TAG, "Turn complete")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "receive loop ended: ${e.message}")
            } finally {
                Log.d(TAG, "receive loop finished")
            }
        }

        // Ждём завершения receive (до stopConversation)
        receiveJob?.join()
    }

    /**
     * Останавливает ручной режим.
     */
    suspend fun stopConversation() {
        Log.d(TAG, "stopConversation")
        recordJob?.cancelAndJoin()
        recordJob = null
        receiveJob?.cancelAndJoin()
        receiveJob = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    // ── TEXT ──────────────────────────────────────────────────────────────────

    suspend fun sendText(text: String) {
        val session = sessionMutex.withLock { liveSession } ?: run {
            Log.w(TAG, "sendText: no session")
            return
        }
        runCatching { session.send(content { text(text) }) }
            .onFailure { Log.e(TAG, "sendText error: ${it.message}") }
    }

    // ── DISCONNECT ────────────────────────────────────────────────────────────

    suspend fun disconnect() {
        stopConversation()
        try {
            withTimeout(3_000L) {
                sessionMutex.withLock {
                    liveSession?.close()
                    liveSession = null
                }
            }
            Log.d(TAG, "Disconnected")
        } catch (e: Exception) {
            Log.w(TAG, "disconnect warning: ${e.message}")
            sessionMutex.withLock { liveSession = null }
        }
    }

    fun release() {
        sessionResumptionHandle = null
        lastTokenUsage = null
    }

    fun clearResumptionHandle() {
        sessionResumptionHandle = null
    }

    // ── Function mapping ──────────────────────────────────────────────────────

    private fun mapToFirebaseDeclaration(decl: GeminiFunctionDeclaration): FunctionDeclaration? {
        val params = decl.parameters
        if (params == null || params.properties.isEmpty()) {
            return FunctionDeclaration(
                name               = decl.name,
                description        = decl.description,
                parameters         = mapOf("unused_parameter" to Schema.boolean("Ignored")),
                optionalParameters = listOf("unused_parameter"),
            )
        }
        val properties = params.properties.mapValues { (_, p) -> mapPropertyToSchema(p) }
        val optional   = properties.keys.filter { it !in params.required }
        return FunctionDeclaration(
            name               = decl.name,
            description        = decl.description,
            parameters         = properties,
            optionalParameters = optional,
        )
    }

    private fun mapPropertyToSchema(prop: GeminiProperty): Schema = when (prop.type.uppercase()) {
        "STRING"  -> if (prop.enum != null) Schema.enumeration(prop.enum, prop.description)
                     else Schema.string(prop.description)
        "INTEGER" -> Schema.integer(prop.description)
        "NUMBER"  -> Schema.double(prop.description)
        "BOOLEAN" -> Schema.boolean(prop.description)
        "ARRAY"   -> Schema.array(Schema.string(), prop.description)
        else      -> Schema.string(prop.description)
    }
}

class GeminiConnectionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
```