
package com.voicedeutsch.master.util

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Audio processing utilities — amplitude calculation, RMS, format conversion.
 * ИЗМЕНЕНО: detectVoiceActivity() УДАЛЁН — клиентский VAD антипаттерн,
 * Gemini сам детектирует речь на сервере.
 */
object AudioUtils {

    fun calculateRMS(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0.0
        for (sample in samples) {
            sum += sample.toDouble() * sample.toDouble()
        }
        return sqrt(sum / samples.size).toFloat()
    }

    fun calculateRMSdB(samples: ShortArray): Float {
        val rms = calculateRMS(samples)
        if (rms &lt;= 0f) return -100f
        return (20 * log10(rms / Short.MAX_VALUE.toFloat())).toFloat()
    }

    fun calculatePeakAmplitude(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        var max = 0
        for (sample in samples) {
            val absSample = abs(sample.toInt())
            if (absSample &gt; max) max = absSample
        }
        return max.toFloat() / Short.MAX_VALUE.toFloat()
    }

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
                if (normalized &gt; maxInChunk) maxInChunk = normalized
            }
            result[i] = maxInChunk
        }
        return result
    }

    fun shortArrayToByteArray(samples: ShortArray): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            bytes[i * 2] = (samples[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (samples[i].toInt() shr 8 and 0xFF).toByte()
        }
        return bytes
    }

    fun byteArrayToShortArray(bytes: ByteArray): ShortArray {
        val samples = ShortArray(bytes.size / 2)
        for (i in samples.indices) {
            samples[i] = ((bytes[i * 2 + 1].toInt() shl 8) or
                    (bytes[i * 2].toInt() and 0xFF)).toShort()
        }
        return samples
    }

    // detectVoiceActivity() УДАЛЁН.
    // Gemini Live API сам детектирует речь на сервере (server-side VAD).
    // Клиентский VAD — антипаттерн в 2026 году.
}

