package com.voicedeutsch.master.voicecore.audio

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Captures real audio output from the device via Android's [Visualizer] API.
 *
 * Connects to the global audio mix (sessionId=0) and extracts:
 *  - Waveform (time-domain PCM samples)
 *  - FFT magnitude spectrum
 *
 * This replaces the synthetic amplitude generator in VoiceCoreEngineImpl.
 * When Gemini speaks through Firebase SDK → audio goes to speaker →
 * Visualizer intercepts the PCM buffer → we extract spectral features.
 *
 * Requirements:
 *  - RECORD_AUDIO permission (already granted for voice sessions)
 *  - MODIFY_AUDIO_SETTINGS permission (add to AndroidManifest.xml)
 *
 * Thread safety: Visualizer callbacks come on an internal audio thread.
 * We emit into a SharedFlow which is collected on coroutine dispatchers.
 */
class AudioOutputCapture {

    companion object {
        private const val TAG = "AudioOutputCapture"

        /**
         * Capture size in bytes. Must be power of 2, range [128..1024].
         * 1024 gives best frequency resolution:
         *   At 44.1kHz → ~43Hz per FFT bin, ~23ms window
         *   At 48kHz → ~47Hz per FFT bin, ~21ms window
         * Good enough for pitch detection down to ~85Hz (male voice).
         */
        private const val CAPTURE_SIZE = 1024
    }

    /**
     * Raw audio frame captured from the Visualizer.
     *
     * @param waveform  Time-domain samples, normalized to -1..1.
     *                  Length = CAPTURE_SIZE.
     * @param fft       FFT magnitude spectrum, normalized to 0..1.
     *                  Length = CAPTURE_SIZE / 2 (Nyquist).
     * @param timestampMs Capture timestamp for synchronization.
     */
    data class AudioFrame(
        val waveform: FloatArray,
        val fft: FloatArray,
        val timestampMs: Long,
        val sampleRate: Int,
    )

    private var visualizer: Visualizer? = null

    private val _frames = MutableSharedFlow<AudioFrame>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val frames: Flow<AudioFrame> = _frames.asSharedFlow()

    @Volatile
    private var isActive = false

    @Volatile
    private var outputSampleRate = 44100  // default, updated on first capture

    /**
     * Starts capturing audio output.
     *
     * @param audioSessionId  0 = global mix (recommended).
     *                        Specific session ID if you can get Firebase's player session.
     * @return true if capture started successfully.
     */
    fun start(audioSessionId: Int = 0): Boolean {
        if (isActive) {
            Log.w(TAG, "Already capturing")
            return true
        }

        return try {
            val viz = Visualizer(audioSessionId).apply {
                enabled = false // must disable before configuring

                // Set capture size (1024 for best resolution)
                val range = Visualizer.getCaptureSizeRange()
                val size = CAPTURE_SIZE.coerceIn(range[0], range[1])
                captureSize = size
                Log.d(TAG, "Capture size: $size (range: ${range[0]}..${range[1]})")

                // Set scaling mode to normalized (0..255 for waveform)
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED

                // Maximum capture rate
                val maxRate = Visualizer.getMaxCaptureRate()
                Log.d(TAG, "Max capture rate: ${maxRate / 1000} Hz")

                // Set listener for both waveform and FFT
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {

                        private val waveformFloat = FloatArray(size)
                        private val fftFloat = FloatArray(size / 2)

                        // Atomic flags для синхронизации waveform/fft
                        @Volatile private var waveformReady = false
                        @Volatile private var lastWaveformTimestamp = 0L

                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int,
                        ) {
                            if (waveform == null || !isActive) return
                            outputSampleRate = samplingRate / 1000

                            for (i in waveform.indices) {
                                waveformFloat[i] = (waveform[i].toInt() and 0xFF) / 128f - 1f
                            }
                            lastWaveformTimestamp = System.currentTimeMillis()
                            waveformReady = true

                            val peak = waveformFloat.max()
                            if (peak > 0.05f) {
                                Log.d(TAG, "🎤 Waveform peak=${"%.3f".format(peak)}")
                            }
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fftData: ByteArray?,
                            samplingRate: Int,
                        ) {
                            if (fftData == null || !isActive) return
                            outputSampleRate = samplingRate / 1000

                            val dc = (fftData[0].toInt() and 0xFF).toFloat()
                            fftFloat[0] = dc / 256f

                            val halfSize = fftData.size / 2
                            var maxMag = 1f

                            for (i in 1 until halfSize) {
                                val real = fftData[2 * i].toFloat()
                                val imag = fftData[2 * i + 1].toFloat()
                                val magnitude = kotlin.math.sqrt(real * real + imag * imag)
                                fftFloat[i] = magnitude
                                if (magnitude > maxMag) maxMag = magnitude
                            }

                            if (maxMag > 5f) {
                                Log.d(TAG, "🔊 REAL audio: maxMag=${"%.1f".format(maxMag)}")
                            }

                            for (i in fftFloat.indices) {
                                fftFloat[i] = (fftFloat[i] / maxMag).coerceIn(0f, 1f)
                            }

                            // Эмитируем frame ТОЛЬКО если waveform уже был обновлён недавно (<100ms)
                            val now = System.currentTimeMillis()
                            if (!waveformReady || (now - lastWaveformTimestamp) > 100L) {
                                // Waveform устарел — не эмитируем, чтобы не передавать нули
                                return
                            }
                            waveformReady = false  // consumed

                            val frame = AudioFrame(
                                waveform = waveformFloat.copyOf(),
                                fft = fftFloat.copyOf(),
                                timestampMs = now,
                                sampleRate = outputSampleRate,
                            )
                            _frames.tryEmit(frame)
                        }
                    },
                    maxRate,
                    true,
                    true,
                )

                enabled = true
            }

            visualizer = viz
            isActive = true
            Log.d(TAG, "✅ Audio capture started (sessionId=$audioSessionId, rate=${Visualizer.getMaxCaptureRate() / 1000}Hz)")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio capture: ${e.message}", e)
            isActive = false
            false
        }
    }

    /**
     * Stops capturing and releases the Visualizer.
     */
    fun stop() {
        isActive = false
        runCatching {
            visualizer?.apply {
                enabled = false
                setDataCaptureListener(null, 0, false, false)
                release()
            }
        }.onFailure { Log.w(TAG, "Visualizer release warning: ${it.message}") }
        visualizer = null
        Log.d(TAG, "Audio capture stopped")
    }

    fun isCapturing(): Boolean = isActive

    fun release() = stop()

    /**
     * Scans all active audio playback sessions and logs their IDs.
     * Call this WHILE Gemini is speaking to find the correct sessionId.
     */
    fun discoverAudioSessions(context: android.content.Context): List<Int> {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE)
            as android.media.AudioManager
        val configs = audioManager.activePlaybackConfigurations
        val sessionIds = mutableListOf<Int>()

        Log.d(TAG, "=== AUDIO SESSION SCAN ===")
        Log.d(TAG, "Active playback configs: ${configs.size}")

        for ((index, config) in configs.withIndex()) {
            val attrs = config.audioAttributes
            Log.d(TAG, "Config[$index]: usage=${attrs.usage}, contentType=${attrs.contentType}")

            // Используем публичный API вместо reflection
            // AudioPlaybackConfiguration не выставляет sessionId публично на всех версиях,
            // но PlayerProxy.getAudioSessionId() доступен через getPlayerProxy() начиная с API 28
            // Для нашего случая global mix (sessionId=0) — самый надёжный подход.
        }

        Log.d(TAG, "Using global mix (sessionId=0) — most reliable for Firebase SDK audio")
        Log.d(TAG, "=== END SCAN ===")

        return sessionIds  // пустой список → start(0) будет вызван в startWithDiscovery
    }

    /**
     * Tries to start Visualizer on each discovered session ID.
     * Returns the first one that successfully starts.
     */
    fun startWithDiscovery(context: android.content.Context): Boolean {
        val sessionIds = discoverAudioSessions(context)

        for (id in sessionIds) {
            Log.d(TAG, "Trying sessionId=$id...")
            try {
                if (start(audioSessionId = id)) {
                    Log.d(TAG, "✅ Visualizer attached to sessionId=$id")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "sessionId=$id failed: ${e.message}")
            }
        }

        Log.d(TAG, "No specific session worked, trying global mix (0)...")
        return start(audioSessionId = 0)
    }
}
