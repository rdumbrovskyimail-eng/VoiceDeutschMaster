package com.voicedeutsch.master.voicecore.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.voicedeutsch.master.util.AudioUtils
import com.voicedeutsch.master.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Records PCM audio from the device microphone.
 *
 * Specification (Architecture lines 574-580):
 *   - Sample rate : 16 000 Hz
 *   - Bit depth   : 16-bit (PCM_16BIT)
 *   - Channels    : Mono
 *   - Frame size  : [FRAME_SIZE_SAMPLES] samples (~10 ms per frame)
 *
 * Usage:
 * ```
 * recorder.start()
 * recorder.audioFrameFlow.collect { pcmShorts -> /* send to Gemini / VAD */ }
 * recorder.stop()
 * recorder.release()
 * ```
 */
class AudioRecorder {

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        /** Input sample rate required by Gemini Live API. */
        const val SAMPLE_RATE = Constants.AUDIO_INPUT_SAMPLE_RATE   // 16 000 Hz

        /** 10 ms frame duration at 16 kHz = 160 samples */
        const val FRAME_SIZE_SAMPLES = 160

        /** Minimum buffer size in bytes for AudioRecord. */
        private val MIN_BUFFER_SIZE: Int = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(FRAME_SIZE_SAMPLES * 2 * 4) // at least 4 frames
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private var audioRecord: AudioRecord? = null
    private val _isRecording = AtomicBoolean(false)

    /** Current RMS amplitude, normalized to [0.0, 1.0]. Updated on every frame. */
    @Volatile
    var currentAmplitude: Float = 0f
        private set

    // ── Audio frame flow ──────────────────────────────────────────────────────

    private val frameChannel = Channel<ShortArray>(capacity = Channel.UNLIMITED)

    /**
     * Flow of raw PCM frames (ShortArray, [FRAME_SIZE_SAMPLES] samples each).
     * Collect this to receive microphone input.
     */
    val audioFrameFlow: Flow<ShortArray> = frameChannel.receiveAsFlow()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Initializes [AudioRecord] and starts a background recording thread.
     *
     * @throws IllegalStateException if AudioRecord cannot be initialized
     *         (e.g. RECORD_AUDIO permission not granted).
     */
    fun start() {
        if (_isRecording.getAndSet(true)) return

        val ar = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            MIN_BUFFER_SIZE,
        )
        check(ar.state == AudioRecord.STATE_INITIALIZED) {
            "AudioRecord failed to initialize. Check RECORD_AUDIO permission."
        }
        audioRecord = ar
        ar.startRecording()

        // Blocking read loop — runs on a dedicated thread via the caller's coroutine dispatcher
        Thread(::recordingLoop, "AudioRecorder-Thread").start()
    }

    /** Signals the recording thread to stop. Non-blocking. */
    fun stop() {
        _isRecording.set(false)
    }

    /** Releases native AudioRecord resources. Must be called after [stop]. */
    fun release() {
        stop()
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                stop()
                release()
            }
        }
        audioRecord = null
        frameChannel.close()
    }

    // ── Private recording loop ────────────────────────────────────────────────

    private fun recordingLoop() {
        val buffer = ShortArray(FRAME_SIZE_SAMPLES)
        while (_isRecording.get()) {
            val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
            if (read > 0) {
                val frame = buffer.copyOf(read)
                // Update amplitude for waveform UI
                currentAmplitude = AudioUtils.calculateRMS(frame) / Short.MAX_VALUE.toFloat()
                // Non-blocking send — if consumer is slow, older frames are discarded
                frameChannel.trySend(frame)
            }
        }
        audioRecord?.stop()
    }
}