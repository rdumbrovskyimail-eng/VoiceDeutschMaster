package com.voicedeutsch.master.voicecore.audio

import android.content.Context
import com.voicedeutsch.master.util.AudioUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Audio pipeline coordinator — manages the full recording and playback lifecycle.
 *
 * Architecture lines 574-622 (AudioPipeline description).
 *
 * Input path (User → Gemini):
 *   Microphone → [AudioRecorder] → [VADProcessor] → outgoing audio channel
 *
 * Output path (Gemini → Speaker):
 *   Incoming PCM bytes → internal queue → [AudioPlayer]
 *
 * The pipeline exposes:
 *   - [incomingAudioFlow] / [audioChunks] — raw PCM frames from the microphone (for Gemini)
 *   - [vadStateFlow]                      — whether speech is currently detected
 *   - [enqueueAudio]                      — feed PCM bytes received from Gemini to the speaker
 *   - [startRecording] / [stopRecording] / [stopAll] — lifecycle control
 *
 * Lifecycle (C2 contract for VoiceCoreEngineImpl):
 *   [initialize] → (startRecording / stopRecording / enqueueAudio / stopAll)* → [release]
 *   After [stopAll], the pipeline can be restarted via [startRecording] / [enqueueAudio].
 *   After [release], the pipeline must not be used — call [initialize] again first.
 */
class AudioPipeline(
    private val context: Context,
) {
    // Sub-components (created lazily so tests can replace them)
    private var recorder = AudioRecorder()
    private var player = AudioPlayer()
    private val vad = VADProcessor()

    // ── State ─────────────────────────────────────────────────────────────────

    /**
     * H2 FIX: Mutex guards all state transitions so the flag and the actual
     * hardware operation are atomic. [AtomicBoolean] alone only protected the
     * flag read/write but not the compound (flag + start/stop) operation.
     */
    private val stateMutex = Mutex()
    private var _isRecording = false
    private var _isPlaying = false
    private var _isInitialized = false

    val isRecording: Boolean get() = _isRecording
    val isPlaying: Boolean get() = _isPlaying

    // ── VAD state flow ────────────────────────────────────────────────────────

    private val _vadStateFlow = MutableStateFlow(VADProcessor.VadState.SILENCE)

    /** Emits the current VAD state: SPEECH or SILENCE. */
    val vadStateFlow: StateFlow<VADProcessor.VadState> = _vadStateFlow.asStateFlow()

    // ── Outgoing audio (mic → Gemini) ─────────────────────────────────────────

    private val outgoingChannel = Channel<ByteArray>(capacity = Channel.BUFFERED)

    /**
     * Flow of raw PCM frames captured from the microphone after VAD filtering.
     * Collect this flow to stream audio bytes to Gemini Live API.
     *
     * Алиас: [audioChunks] — используется в VoiceCoreEngineImpl.startListening()
     * для форвардинга через GeminiClient.sendAudioChunk().
     */
    val incomingAudioFlow: Flow<ByteArray> = outgoingChannel.receiveAsFlow()

    // ── ADD: алиас для VoiceCoreEngineImpl ────────────────────────────────────
    /**
     * Алиас для [incomingAudioFlow].
     * VoiceCoreEngineImpl вызывает audioPipeline.audioChunks().collect { ... }
     * в audioForwardJob для стриминга PCM-чанков в GeminiClient.sendAudioChunk().
     */
    fun audioChunks(): Flow<ByteArray> = incomingAudioFlow

    // ── Incoming audio queue (Gemini → speaker) ───────────────────────────────

    /**
     * H3 FIX: Bounded channel instead of [Channel.UNLIMITED].
     *
     * At 24 kHz × 16-bit × mono = 48 KB/sec, 100 frames ≈ 2–4 seconds of audio.
     * When the queue is full, the oldest frame is dropped so the playback stays
     * close to real-time rather than accumulating unbounded latency.
     */
    private var playbackQueue = Channel<ByteArray>(
        capacity = PLAYBACK_QUEUE_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    // ── Coroutine scope ───────────────────────────────────────────────────────

    /**
     * H1 FIX: The scope's [SupervisorJob] is stored separately so that
     * [stopAll] can cancel the job (and all child coroutines) without killing
     * the scope permanently. A new job is created on next [startRecording] or
     * [ensurePlaybackRunning] call, or eagerly in [initialize].
     */
    private var scopeJob = SupervisorJob()
    private var pipelineScope = CoroutineScope(Dispatchers.IO + scopeJob)

    private var recordingJob: Job? = null
    private var playbackJob: Job? = null

    // ── Lifecycle (C2 contract) ───────────────────────────────────────────────

    /**
     * Prepares the pipeline for use. Must be called before [startRecording]
     * or [enqueueAudio].
     *
     * Safe to call multiple times — subsequent calls are no-ops if already initialized.
     *
     * Suspend-совместим: VoiceCoreEngineImpl вызывает его внутри runCatching { }.
     */
    fun initialize() {
        if (_isInitialized) return
        ensureScopeAlive()
        _isInitialized = true
    }

    /**
     * Releases all audio resources permanently. After this call the pipeline
     * must not be used until [initialize] is called again.
     *
     * Calls [stopAll] internally, then tears down the coroutine scope.
     *
     * Suspend-совместим: VoiceCoreEngineImpl вызывает его внутри lifecycleMutex.withLock { }.
     */
    fun release() {
        if (!_isInitialized) return
        stopAll()
        pipelineScope.cancel()
        _isInitialized = false
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Starts microphone recording and routes frames through VAD into [incomingAudioFlow].
     *
     * H2 FIX: The entire check-then-act sequence is inside [stateMutex].
     */
    fun startRecording() {
        pipelineScope.launch {
            stateMutex.withLock {
                if (_isRecording) return@withLock
                ensureScopeAlive()

                recorder.start()
                _isRecording = true

                recordingJob = pipelineScope.launch {
                    recorder.audioFrameFlow.collect { pcmShorts ->
                        val vadState = vad.process(pcmShorts)
                        _vadStateFlow.value = vadState

                        if (vadState == VADProcessor.VadState.SPEECH ||
                            vadState == VADProcessor.VadState.SPEECH_END
                        ) {
                            val bytes = AudioUtils.shortArrayToByteArray(pcmShorts)
                            outgoingChannel.trySend(bytes)
                        }
                    }
                }
            }
        }
    }

    /**
     * Stops microphone recording gracefully.
     *
     * H2 FIX: guarded by [stateMutex].
     */
    fun stopRecording() {
        pipelineScope.launch {
            stateMutex.withLock {
                if (!_isRecording) return@withLock
                recorder.stop()
                recordingJob?.cancel()
                recordingJob = null
                _isRecording = false
            }
        }
    }

    /**
     * Enqueues a PCM byte array received from Gemini for playback.
     * Thread-safe; can be called from any coroutine context.
     */
    fun enqueueAudio(pcmBytes: ByteArray) {
        if (pcmBytes.isEmpty()) return
        playbackQueue.trySend(pcmBytes)
        ensurePlaybackRunning()
    }

    /** Pauses audio playback without clearing the queue. */
    fun pausePlayback() {
        player.pause()
    }

    /** Resumes paused playback. */
    fun resumePlayback() {
        player.resume()
    }

    /** Clears the playback queue and stops the speaker immediately. */
    fun stopPlayback() {
        pipelineScope.launch {
            stateMutex.withLock {
                if (!_isPlaying) return@withLock
                playbackJob?.cancel()
                playbackJob = null
                while (playbackQueue.tryReceive().isSuccess) { /* discard */ }
                player.stop()
                _isPlaying = false
            }
        }
    }

    /**
     * Stops both recording and playback; resets the pipeline to idle state.
     *
     * H1 FIX: Cancels the scope's [Job] and recreates it, so the pipeline
     * can be reused for a new session without restarting the app.
     */
    fun stopAll() {
        scopeJob.cancel()
        recorder.release()
        player.release()
        while (playbackQueue.tryReceive().isSuccess) { /* discard */ }
        _isRecording = false
        _isPlaying = false
        recordingJob = null
        playbackJob = null
        ensureScopeAlive()
        recorder = AudioRecorder()
        player = AudioPlayer()
        playbackQueue = Channel(
            capacity = PLAYBACK_QUEUE_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    }

    /**
     * Returns the current microphone amplitude (0.0–1.0) for waveform visualization.
     */
    fun getCurrentAmplitude(): Float = recorder.currentAmplitude

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Starts the playback coroutine if it's not already running.
     *
     * H2 FIX: guarded by [stateMutex].
     */
    private fun ensurePlaybackRunning() {
        pipelineScope.launch {
            stateMutex.withLock {
                if (_isPlaying) return@withLock
                _isPlaying = true
                ensureScopeAlive()

                playbackJob = pipelineScope.launch {
                    player.start()
                    try {
                        for (chunk in playbackQueue) {
                            player.write(chunk)
                        }
                    } finally {
                        player.stop()
                        stateMutex.withLock {
                            _isPlaying = false
                        }
                    }
                }
            }
        }
    }

    /**
     * H1 FIX: Ensures the coroutine scope has a live [Job]. If the previous
     * job was cancelled (by [stopAll]), creates a new [SupervisorJob] and scope.
     */
    private fun ensureScopeAlive() {
        if (scopeJob.isCancelled || scopeJob.isCompleted) {
            scopeJob = SupervisorJob()
            pipelineScope = CoroutineScope(Dispatchers.IO + scopeJob)
        }
    }

    companion object {
        /**
         * H3 FIX: Max number of audio frames buffered for playback.
         * At 24 kHz × 16-bit mono with ~480 samples/frame ≈ 20 ms per frame,
         * 100 frames ≈ 2 seconds of buffered audio — enough to absorb jitter
         * without risking OOM on low-end devices.
         */
        private const val PLAYBACK_QUEUE_CAPACITY = 100
    }
}