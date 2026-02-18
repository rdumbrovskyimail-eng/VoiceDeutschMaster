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

    override fun hashCode(): Int = engineState.hashCode() * 31 + sessionId.hashCode()
}