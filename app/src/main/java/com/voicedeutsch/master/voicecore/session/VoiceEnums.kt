package com.voicedeutsch.master.voicecore.session

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
