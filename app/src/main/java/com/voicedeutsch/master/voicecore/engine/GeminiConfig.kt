/**
 * Complete configuration for Gemini Live API connection.
 * Architecture lines 562-580 (GeminiConfig).
 */
data class GeminiConfig(
    val apiKey: String,
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
) {
    init {
        require(apiKey.isNotBlank()) { "API key must not be blank" }
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
        const val MODEL_GEMINI_LIVE = "gemini-2.0-flash-live-001"
        const val MAX_CONTEXT_TOKENS = 2_000_000
        const val DEFAULT_TEMPERATURE = 0.5f
        const val DEFAULT_TOP_P = 0.95f
        const val DEFAULT_TOP_K = 40
        const val DEFAULT_RECONNECT_ATTEMPTS = 3
        const val DEFAULT_RECONNECT_DELAY_MS = 2_000L
    }
}
FILE 2: voicecore/session/VoiceSessionState.kt
package com.voicedeutsch.master.voicecore.session

import com.voicedeutsch.master.domain.model.LearningStrategy

/**
 * Internal engine states — mirrors the lifecycle diagram from Architecture lines 533-538.
 * The UI observes [VoiceSessionState] (the data class), not this enum directly.
 */
enum class VoiceEngineState {
    IDLE,
    INITIALIZING,
    CONTEXT_LOADING,
    CONNECTING,
    CONNECTED,
    SESSION_ACTIVE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    WAITING,
    SESSION_ENDING,
    SAVING,
    ERROR,
    RECONNECTING,
}

/** WebSocket / gRPC connection states. */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED,
}

/** Current state of the audio subsystem. */
enum class AudioState {
    IDLE,
    RECORDING,
    PLAYING,
    PAUSED,
}