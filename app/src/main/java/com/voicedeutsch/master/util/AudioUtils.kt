package com.voicedeutsch.master.util

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Audio processing utilities — amplitude calculation, RMS, format conversion, VAD.
 *
 * Used by AudioPipeline, AudioRecorder, VADProcessor, and VoiceWaveform visualization.
 */
object AudioUtils {

    /**
     * Calculate Root Mean Square (RMS) of audio samples.
     * Used for VAD and waveform visualization.
     *
     * @param samples PCM 16-bit audio samples
     * @return RMS value as a float (in the range of [Short] magnitudes)
     */
    fun calculateRMS(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0.0
        for (sample in samples) {
            sum += sample.toDouble() * sample.toDouble()
        }
        return sqrt(sum / samples.size).toFloat()
    }

    /**
     * Calculate RMS in decibels relative to full scale (dBFS).
     *
     * @param samples PCM 16-bit audio samples
     * @return dBFS value; returns -100f for silence
     */
    fun calculateRMSdB(samples: ShortArray): Float {
        val rms = calculateRMS(samples)
        if (rms <= 0f) return -100f
        return (20 * log10(rms / Short.MAX_VALUE.toFloat())).toFloat()
    }

    /**
     * Calculate peak amplitude of an audio buffer, normalized to [0.0, 1.0].
     *
     * @param samples PCM 16-bit audio samples
     * @return peak amplitude in [0.0, 1.0]
     */
    fun calculatePeakAmplitude(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        var max = 0
        for (sample in samples) {
            val absSample = abs(sample.toInt())
            if (absSample > max) max = absSample
        }
        return max.toFloat() / Short.MAX_VALUE.toFloat()
    }

    /**
     * Normalize audio samples to a float array of [targetSize] for waveform visualization.
     * Each element is the peak amplitude of the corresponding chunk, normalized to [0.0, 1.0].
     *
     * @param samples raw PCM 16-bit samples
     * @param targetSize desired output array size (number of waveform bars)
     * @return float array of normalized peak amplitudes
     */
    fun normalizeForVisualization(samples: ShortArray, targetSize: Int): FloatArray {
        if (samples.isEmpty()) return FloatArray(targetSize)

        val result = FloatArray(targetSize)
        val step = samples.size.toFloat() / targetSize

        for (i in 0 until targetSize) {
            val startIdx = (i * step).toInt()
            val endIdx = minOf(((i + 1) * step).toInt(), samples.size)

            var maxInChunk = 0f
            for (j in startIdx until endIdx) {
                val normalized = abs(samples[j].toFloat()) / Short.MAX_VALUE.toFloat()
                if (normalized > maxInChunk) maxInChunk = normalized
            }
            result[i] = maxInChunk
        }
        return result
    }

    /**
     * Convert PCM 16-bit samples to a little-endian byte array.
     *
     * @param samples PCM 16-bit samples
     * @return byte array (2 bytes per sample, little-endian)
     */
    fun shortArrayToByteArray(samples: ShortArray): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            bytes[i * 2] = (samples[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (samples[i].toInt() shr 8 and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Convert a little-endian byte array to PCM 16-bit samples.
     *
     * @param bytes byte array (2 bytes per sample, little-endian)
     * @return PCM 16-bit samples
     */
    fun byteArrayToShortArray(bytes: ByteArray): ShortArray {
        val samples = ShortArray(bytes.size / 2)
        for (i in samples.indices) {
            samples[i] = ((bytes[i * 2 + 1].toInt() shl 8) or
                    (bytes[i * 2].toInt() and 0xFF)).toShort()
        }
        return samples
    }

    /**
     * Simple energy-based Voice Activity Detection (VAD).
     * Returns `true` if speech is detected in the buffer.
     *
     * @param samples PCM 16-bit audio samples
     * @param threshold normalized RMS threshold (default from [Constants.VAD_SPEECH_START_THRESHOLD])
     * @return `true` when voice activity is detected
     */
    fun detectVoiceActivity(
        samples: ShortArray,
        threshold: Float = Constants.VAD_SPEECH_START_THRESHOLD
    ): Boolean {
        val rms = calculateRMS(samples) / Short.MAX_VALUE.toFloat()
        return rms > threshold
    }
}