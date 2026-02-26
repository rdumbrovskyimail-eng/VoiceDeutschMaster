package com.voicedeutsch.master.util

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Audio processing utilities — amplitude calculation, RMS, format conversion.
 *
 * ✅ КЛИЕНТСКИЙ VAD УДАЛЁН (25.02.2026)
 *    detectVoiceActivity() полностью удалён.
 *    Мы отправляем сырой 16 kHz поток — Gemini сам детектирует речь на сервере.
 *    Это устраняет все проблемы с ложными срабатываниями и задержками.
 */
object AudioUtils {

    /**
     * Calculate Root Mean Square (RMS) amplitude.
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
     * Calculate RMS in decibels (dBFS).
     */
    fun calculateRMSdB(samples: ShortArray): Float {
        val rms = calculateRMS(samples)
        if (rms <= 0f) return -100f
        return (20 * log10(rms / Short.MAX_VALUE)).toFloat() // Removed .toFloat() from Short.MAX_VALUE
    }

    /**
     * Calculate peak amplitude (0.0 — 1.0).
     */
    fun calculatePeakAmplitude(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        var max = 0
        for (sample in samples) {
            val absSample = abs(sample.toInt())
            if (absSample > max) max = absSample
        }
        return max.toFloat() / Short.MAX_VALUE // Removed .toFloat() from Short.MAX_VALUE
    }

    /**
     * Normalize ShortArray for waveform visualization (downsample to target size).
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
                val normalized = abs(samples[j].toFloat()) / Short.MAX_VALUE // Removed .toFloat() from Short.MAX_VALUE
                if (normalized > maxInChunk) maxInChunk = normalized
            }
            result[i] = maxInChunk
        }
        return result
    }

    /**
     * Convert ShortArray (16-bit PCM) to ByteArray (little-endian).
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
     * Convert ByteArray (little-endian 16-bit PCM) back to ShortArray.
     */
    fun byteArrayToShortArray(bytes: ByteArray): ShortArray {
        val samples = ShortArray(bytes.size / 2)
        for (i in samples.indices) {
            samples[i] = ((bytes[i * 2 + 1].toInt() shl 8) or
                    (bytes[i * 2].toInt() and 0xFF)).toShort()
        }
        return samples
    }
}