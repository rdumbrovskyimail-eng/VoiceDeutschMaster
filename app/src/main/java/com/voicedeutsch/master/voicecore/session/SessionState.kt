package com.voicedeutsch.master.voicecore.session

import com.voicedeutsch.master.domain.model.LearningStrategy

/**
 * Observable session state — consumed by the UI layer (Presentation → VoiceCore).
 * Architecture lines 545-560 (SessionState structure).
 *
 * [FloatArray] fields have custom [equals] / [hashCode] to prevent spurious recompositions.
 */
data class VoiceSessionState(
    val engineState: VoiceEngineState = VoiceEngineState.IDLE,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val audioState: AudioState = AudioState.IDLE,
    val isVoiceActive: Boolean = false,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isProcessing: Boolean = false,
    val currentStrategy: LearningStrategy = LearningStrategy.LINEAR_BOOK,
    val sessionDurationMs: Long = 0L,
    val wordsLearnedInSession: Int = 0,
    val wordsReviewedInSession: Int = 0,
    val exercisesCompleted: Int = 0,
    val currentTranscript: String = "",
    val voiceTranscript: String = "",
    val voiceWaveformData: FloatArray = FloatArray(0),
    val userWaveformData: FloatArray = FloatArray(0),
    val errorMessage: String? = null,
    val sessionId: String? = null,
) {
    /** Derived: user is active in a session. */
    val isSessionActive: Boolean
        get() = engineState == VoiceEngineState.SESSION_ACTIVE ||
                engineState == VoiceEngineState.LISTENING ||
                engineState == VoiceEngineState.PROCESSING ||
                engineState == VoiceEngineState.SPEAKING ||
                engineState == VoiceEngineState.WAITING

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VoiceSessionState) return false
        // FloatArray has reference equality by default — compare structurally
        return engineState == other.engineState &&
                connectionState == other.connectionState &&
                audioState == other.audioState &&
                isVoiceActive == other.isVoiceActive &&
                isListening == other.isListening &&
                isSpeaking == other.isSpeaking &&
                isProcessing == other.isProcessing &&
                currentStrategy == other.currentStrategy &&
                sessionDurationMs == other.sessionDurationMs &&
                wordsLearnedInSession == other.wordsLearnedInSession &&
                wordsReviewedInSession == other.wordsReviewedInSession &&
                exercisesCompleted == other.exercisesCompleted &&
                currentTranscript == other.currentTranscript &&
                voiceTranscript == other.voiceTranscript &&
                voiceWaveformData.contentEquals(other.voiceWaveformData) &&
                userWaveformData.contentEquals(other.userWaveformData) &&
                errorMessage == other.errorMessage &&
                sessionId == other.sessionId
    }

    /**
     * M1 FIX: hashCode now includes the fields most likely to differ between
     * state snapshots — [engineState], [connectionState], [audioState],
     * [sessionId], [sessionDurationMs], and [exercisesCompleted].
     *
     * Previously only [engineState] and [sessionId] contributed, so two states
     * with the same engine state + session ID but different transcripts,
     * counters, or audio data produced identical hashes — violating the
     * equals/hashCode contract for hash-based collections.
     *
     * [FloatArray] fields (waveforms) are deliberately excluded: their content
     * changes on every audio frame, and [contentHashCode] is O(n) — too
     * expensive for a hash that may be called frequently by StateFlow's
     * equality check.
     */
    override fun hashCode(): Int {
        var hash = engineState.hashCode()
        hash = 31 * hash + connectionState.hashCode()
        hash = 31 * hash + audioState.hashCode()
        hash = 31 * hash + sessionId.hashCode()
        hash = 31 * hash + sessionDurationMs.hashCode()
        hash = 31 * hash + exercisesCompleted
        hash = 31 * hash + wordsLearnedInSession
        hash = 31 * hash + wordsReviewedInSession
        hash = 31 * hash + currentTranscript.hashCode()
        hash = 31 * hash + voiceTranscript.hashCode()
        hash = 31 * hash + isListening.hashCode()
        hash = 31 * hash + isSpeaking.hashCode()
        hash = 31 * hash + isProcessing.hashCode()
        hash = 31 * hash + (errorMessage?.hashCode() ?: 0)
        return hash
    }
}
