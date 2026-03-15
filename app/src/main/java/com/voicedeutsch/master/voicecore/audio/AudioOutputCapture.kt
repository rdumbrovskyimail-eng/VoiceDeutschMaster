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

                        // Reusable buffers to avoid GC pressure at 20Hz
                        private val waveformFloat = FloatArray(size)
                        private val fftFloat = FloatArray(size / 2)

                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int,
                        ) {
                            if (waveform == null || !isActive) return

                            // samplingRate is in milliHertz
                            outputSampleRate = samplingRate / 1000

                            // Convert unsigned 8-bit (0..255) to float (-1..1)
                            for (i in waveform.indices) {
                                waveformFloat[i] = (waveform[i].toInt() and 0xFF) / 128f - 1f
                            }
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fftData: ByteArray?,
                            samplingRate: Int,
                        ) {
                            if (fftData == null || !isActive) return

                            outputSampleRate = samplingRate / 1000

                            // FFT data format from Visualizer:
                            // fftData[0] = DC component (real)
                            // fftData[1] = Nyquist component (real)
                            // fftData[2i], fftData[2i+1] = real, imaginary for bin i
                            //
                            // We compute magnitude = sqrt(real² + imag²)

                            // DC component
                            val dc = (fftData[0].toInt() and 0xFF).toFloat()
                            fftFloat[0] = dc / 256f

                            // Frequency bins 1..N/2-1
                            val halfSize = fftData.size / 2
                            var maxMag = 1f // prevent division by zero

                            for (i in 1 until halfSize) {
                                val real = fftData[2 * i].toFloat()
                                val imag = fftData[2 * i + 1].toFloat()
                                val magnitude = kotlin.math.sqrt(real * real + imag * imag)
                                fftFloat[i] = magnitude
                                if (magnitude > maxMag) maxMag = magnitude
                            }

                            // Normalize FFT to 0..1
                            for (i in fftFloat.indices) {
                                fftFloat[i] = (fftFloat[i] / maxMag).coerceIn(0f, 1f)
                            }

                            // Emit complete frame (waveform was filled in onWaveFormDataCapture)
                            val frame = AudioFrame(
                                waveform = waveformFloat.copyOf(),
                                fft = fftFloat.copyOf(),
                                timestampMs = System.currentTimeMillis(),
                                sampleRate = outputSampleRate,
                            )
                            _frames.tryEmit(frame)
                        }
                    },
                    maxRate,  // capture rate in milliHz
                    true,     // waveform enabled
                    true,     // fft enabled
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
}
