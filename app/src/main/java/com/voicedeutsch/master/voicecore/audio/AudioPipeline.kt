package com.voicedeutsch.master.voicecore.audio

import android.content.Context
import com.voicedeutsch.master.util.Constants
import com.voicedeutsch.master.util.AudioUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean

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
 *   - [incomingAudioFlow] — raw PCM frames from the microphone (for Gemini)
 *   - [vadStateFlow]      — whether speech is currently detected
 *   - [enqueueAudio]      — feed PCM bytes received from Gemini to the speaker
 *   - [startRecording] / [stopRecording] / [stopAll] — lifecycle control
 */
class AudioPipeline(
    private val context: Context,
) {
    // Sub-components (created lazily so tests can replace them)
    private val recorder = AudioRecorder()
    private val player   = AudioPlayer()
    private val vad      = VADProcessor()

    // ── State ─────────────────────────────────────────────────────────────────

    private val _isRecording = AtomicBoolean(false)
    private val _isPlaying   = AtomicBoolean(false)

    val isRecording: Boolean get() = _isRecording.get()
    val isPlaying:   Boolean get() = _isPlaying.get()

    // ── VAD state flow ────────────────────────────────────────────────────────

    private val _vadStateFlow = MutableStateFlow(VADProcessor.VadState.SILENCE)

    /** Emits the current VAD state: SPEECH or SILENCE. */
    val vadStateFlow: StateFlow<VADProcessor.VadState> = _vadStateFlow.asStateFlow()

    // ── Outgoing audio (mic → Gemini) ─────────────────────────────────────────

    private val outgoingChannel = Channel<ByteArray>(capacity = Channel.BUFFERED)

    /**
     * Flow of raw PCM frames captured from the microphone.
     * The engine collects this flow and streams bytes to Gemini.
     */
    val incomingAudioFlow: Flow<ByteArray> = outgoingChannel.receiveAsFlow()

    // ── Incoming audio queue (Gemini → speaker) ───────────────────────────────

    private val playbackQueue = Channel<ByteArray>(capacity = Channel.UNLIMITED)

    // ── Coroutine scope ───────────────────────────────────────────────────────

    private val pipelineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var recordingJob:  Job? = null
    private var playbackJob:   Job? = null
    private var vadJob:        Job? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Starts microphone recording and routes frames through VAD into [incomingAudioFlow].
     */
    fun startRecording() {
        if (_isRecording.getAndSet(true)) return

        recorder.start()

        recordingJob = pipelineScope.launch {
            recorder.audioFrameFlow.collect { pcmShorts ->
                // Run VAD on every frame
                val vadState = vad.process(pcmShorts)
                _vadStateFlow.value = vadState

                if (vadState == VADProcessor.VadState.SPEECH ||
                    vadState == VADProcessor.VadState.SPEECH_END) {
                    // Convert to bytes and enqueue for Gemini
                    val bytes = AudioUtils.shortArrayToByteArray(pcmShorts)
                    outgoingChannel.trySend(bytes)
                }
            }
        }
    }

    /** Stops microphone recording gracefully. */
    fun stopRecording() {
        if (!_isRecording.getAndSet(false)) return
        recorder.stop()
        recordingJob?.cancel()
        recordingJob = null
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
        _isPlaying.set(false)
        playbackJob?.cancel()
        playbackJob = null
        // Drain the queue
        while (playbackQueue.tryReceive().isSuccess) { /* discard */ }
        player.stop()
    }

    /** Stops both recording and playback; releases all audio resources. */
    fun stopAll() {
        stopRecording()
        stopPlayback()
        pipelineScope.cancel()
        recorder.release()
        player.release()
    }

    /**
     * Returns the current microphone amplitude (0.0–1.0) for waveform visualization.
     */
    fun getCurrentAmplitude(): Float = recorder.currentAmplitude

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun ensurePlaybackRunning() {
        if (_isPlaying.getAndSet(true)) return

        playbackJob = pipelineScope.launch {
            player.start()
            try {
                for (chunk in playbackQueue) {
                    player.write(chunk)
                }
            } finally {
                player.stop()
                _isPlaying.set(false)
            }
        }
    }
}