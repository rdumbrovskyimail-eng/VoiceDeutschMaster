package com.voicedeutsch.master.voicecore.engine

/**
 * Complete configuration for Gemini Live API connection.
 */
data class GeminiConfig(
    val modelName: String = MODEL_GEMINI_LIVE,
    val audioInputFormat: AudioFormat = AudioFormat.PCM_16KHZ_16BIT_MONO,
    val audioOutputFormat: AudioFormat = AudioFormat.PCM_24KHZ_16BIT_MONO,
    val streamingEnabled: Boolean = true,
    val maxContextTokens: Int = MAX_CONTEXT_TOKENS,
    val temperature: Float = DEFAULT_TEMPERATURE,
    val topP: Float = DEFAULT_TOP_P,
    val topK: Int = DEFAULT_TOP_K,
    val reconnectMaxAttempts: Int = DEFAULT_RECONNECT_ATTEMPTS,
    val reconnectDelayMs: Long = DEFAULT_RECONNECT_DELAY_MS,
    val voiceName: String = DEFAULT_VOICE,
) {
    init {
        require(temperature in 0f..2f) { "temperature must be in [0, 2]" }
        require(topP in 0f..1f) { "topP must be in [0, 1]" }
        require(topK > 0) { "topK must be positive" }
        require(reconnectMaxAttempts > 0) { "reconnectMaxAttempts must be positive" }
    }

    enum class AudioFormat(
        val sampleRateHz: Int,
        val bitsPerSample: Int,
        val channels: Int,
    ) {
        PCM_16KHZ_16BIT_MONO(sampleRateHz = 16_000, bitsPerSample = 16, channels = 1),
        PCM_24KHZ_16BIT_MONO(sampleRateHz = 24_000, bitsPerSample = 16, channels = 1),
    }

    companion object {
        const val MODEL_GEMINI_LIVE = "gemini-2.5-flash-native-audio-preview"

        // ⚠️ ИЗМЕНЕНО: 32_768 → 131_072
        // Gemini 2.5 Flash Live API поддерживает 131k токенов.
        // Старое значение 32k — ошибка из ранних preview-моделей.
        const val MAX_CONTEXT_TOKENS = 131_072

        const val DEFAULT_TEMPERATURE = 0.5f
        const val DEFAULT_TOP_P = 0.95f
        const val DEFAULT_TOP_K = 40
        const val DEFAULT_RECONNECT_ATTEMPTS = 3
        const val DEFAULT_RECONNECT_DELAY_MS = 2_000L
        const val DEFAULT_VOICE = "Kore"
    }
}