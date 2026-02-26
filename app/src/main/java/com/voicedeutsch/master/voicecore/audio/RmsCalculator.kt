package com.voicedeutsch.master.voicecore.audio

/**
 * Вычисляет RMS-амплитуду PCM-чанка с микрофона.
 *
 * PCM формат: 16-bit signed, little-endian (стандарт AudioRecord на Android).
 * Результат нормализован в 0f..1f относительно максимального значения Int16 (32768).
 *
 * Используется в VoiceCoreEngineImpl.amplitudeFlow для обновления
 * VirtualAvatar через State<Float> без рекомпозиции.
 */
object RmsCalculator {

    /**
     * @param pcm ByteArray из AudioPipeline.audioChunks() — 16-bit PCM, little-endian.
     * @return RMS в диапазоне 0f..1f. Возвращает 0f для пустого массива.
     */
    fun calculate(pcm: ByteArray): Float {
        if (pcm.size < 2) return 0f

        var sumOfSquares = 0.0
        val sampleCount  = pcm.size / 2

        // 16-bit little-endian: младший байт первый
        for (i in 0 until pcm.size - 1 step 2) {
            val low    = pcm[i].toInt() and 0xFF
            val high   = pcm[i + 1].toInt()
            val sample = (high shl 8) or low   // signed 16-bit
            sumOfSquares += sample.toDouble() * sample.toDouble()
        }

        val rms = Math.sqrt(sumOfSquares / sampleCount)
        return (rms / 32768.0).toFloat()
    }
}
