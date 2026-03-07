// Path: src/test/java/com/voicedeutsch/master/voicecore/session/VoiceSessionStateTest.kt
package com.voicedeutsch.master.voicecore.session

import com.voicedeutsch.master.domain.model.LearningStrategy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VoiceSessionStateTest {

    private fun makeState(
        engineState: VoiceEngineState = VoiceEngineState.IDLE,
        connectionState: ConnectionState = ConnectionState.DISCONNECTED,
        audioState: AudioState = AudioState.IDLE,
        isVoiceActive: Boolean = false,
        isListening: Boolean = false,
        isSpeaking: Boolean = false,
        isProcessing: Boolean = false,
        sessionId: String? = null,
        sessionDurationMs: Long = 0L,
        wordsLearnedInSession: Int = 0,
        wordsReviewedInSession: Int = 0,
        exercisesCompleted: Int = 0,
        currentTranscript: String = "",
        voiceTranscript: String = "",
        errorMessage: String? = null,
        tokenUsage: Int = 0,
    ) = VoiceSessionState(
        engineState = engineState,
        connectionState = connectionState,
        audioState = audioState,
        isVoiceActive = isVoiceActive,
        isListening = isListening,
        isSpeaking = isSpeaking,
        isProcessing = isProcessing,
        sessionId = sessionId,
        sessionDurationMs = sessionDurationMs,
        wordsLearnedInSession = wordsLearnedInSession,
        wordsReviewedInSession = wordsReviewedInSession,
        exercisesCompleted = exercisesCompleted,
        currentTranscript = currentTranscript,
        voiceTranscript = voiceTranscript,
        errorMessage = errorMessage,
        tokenUsage = tokenUsage,
    )

    // ── default values ────────────────────────────────────────────────────────

    @Test
    fun defaultState_engineState_isIdle() {
        val state = VoiceSessionState()
        assertEquals(VoiceEngineState.IDLE, state.engineState)
    }

    @Test
    fun defaultState_connectionState_isDisconnected() {
        assertEquals(ConnectionState.DISCONNECTED, VoiceSessionState().connectionState)
    }

    @Test
    fun defaultState_audioState_isIdle() {
        assertEquals(AudioState.IDLE, VoiceSessionState().audioState)
    }

    @Test
    fun defaultState_isVoiceActive_isFalse() {
        assertFalse(VoiceSessionState().isVoiceActive)
    }

    @Test
    fun defaultState_sessionId_isNull() {
        assertNull(VoiceSessionState().sessionId)
    }

    @Test
    fun defaultState_tokenUsage_isZero() {
        assertEquals(0, VoiceSessionState().tokenUsage)
    }

    @Test
    fun defaultState_errorMessage_isNull() {
        assertNull(VoiceSessionState().errorMessage)
    }

    // ── isSessionActive — computed property ───────────────────────────────────

    @Test
    fun isSessionActive_engineIdle_returnsFalse() {
        val state = makeState(engineState = VoiceEngineState.IDLE)
        assertFalse(state.isSessionActive)
    }

    @Test
    fun isSessionActive_engineSessionActive_returnsTrue() {
        val state = makeState(engineState = VoiceEngineState.SESSION_ACTIVE)
        assertTrue(state.isSessionActive)
    }

    @Test
    fun isSessionActive_engineListening_returnsTrue() {
        val state = makeState(engineState = VoiceEngineState.LISTENING)
        assertTrue(state.isSessionActive)
    }

    @Test
    fun isSessionActive_engineProcessing_returnsTrue() {
        val state = makeState(engineState = VoiceEngineState.PROCESSING)
        assertTrue(state.isSessionActive)
    }

    @Test
    fun isSessionActive_engineSpeaking_returnsTrue() {
        val state = makeState(engineState = VoiceEngineState.SPEAKING)
        assertTrue(state.isSessionActive)
    }

    @Test
    fun isSessionActive_engineWaiting_returnsTrue() {
        val state = makeState(engineState = VoiceEngineState.WAITING)
        assertTrue(state.isSessionActive)
    }

    @Test
    fun isSessionActive_engineError_returnsFalse() {
        val state = makeState(engineState = VoiceEngineState.ERROR)
        assertFalse(state.isSessionActive)
    }

    @Test
    fun isSessionActive_engineSaving_returnsFalse() {
        val state = makeState(engineState = VoiceEngineState.SAVING)
        assertFalse(state.isSessionActive)
    }

    @Test
    fun isSessionActive_engineInitializing_returnsFalse() {
        val state = makeState(engineState = VoiceEngineState.INITIALIZING)
        assertFalse(state.isSessionActive)
    }

    @Test
    fun isSessionActive_engineConnecting_returnsFalse() {
        val state = makeState(engineState = VoiceEngineState.CONNECTING)
        assertFalse(state.isSessionActive)
    }

    @Test
    fun isSessionActive_engineSessionEnding_returnsFalse() {
        val state = makeState(engineState = VoiceEngineState.SESSION_ENDING)
        assertFalse(state.isSessionActive)
    }

    // ── equals ────────────────────────────────────────────────────────────────

    @Test
    fun equals_twoDefaultInstances_areEqual() {
        val a = VoiceSessionState()
        val b = VoiceSessionState()
        assertEquals(a, b)
    }

    @Test
    fun equals_differentEngineState_areNotEqual() {
        val a = makeState(engineState = VoiceEngineState.IDLE)
        val b = makeState(engineState = VoiceEngineState.LISTENING)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentConnectionState_areNotEqual() {
        val a = makeState(connectionState = ConnectionState.DISCONNECTED)
        val b = makeState(connectionState = ConnectionState.CONNECTED)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentSessionId_areNotEqual() {
        val a = makeState(sessionId = "sess_1")
        val b = makeState(sessionId = "sess_2")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_nullVsNonNullSessionId_areNotEqual() {
        val a = makeState(sessionId = null)
        val b = makeState(sessionId = "sess_1")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentTokenUsage_areNotEqual() {
        val a = makeState(tokenUsage = 100)
        val b = makeState(tokenUsage = 200)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentWordsLearned_areNotEqual() {
        val a = makeState(wordsLearnedInSession = 5)
        val b = makeState(wordsLearnedInSession = 10)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentCurrentTranscript_areNotEqual() {
        val a = makeState(currentTranscript = "Hallo")
        val b = makeState(currentTranscript = "Tschüss")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_sameWaveformContent_areEqual() {
        val waveform = floatArrayOf(0.1f, 0.2f, 0.3f)
        val a = VoiceSessionState(voiceWaveformData = waveform.copyOf())
        val b = VoiceSessionState(voiceWaveformData = waveform.copyOf())
        assertEquals(a, b)
    }

    @Test
    fun equals_differentWaveformContent_areNotEqual() {
        val a = VoiceSessionState(voiceWaveformData = floatArrayOf(0.1f, 0.2f))
        val b = VoiceSessionState(voiceWaveformData = floatArrayOf(0.3f, 0.4f))
        assertNotEquals(a, b)
    }

    // ── hashCode ─────────────────────────────────────────────────────────────

    @Test
    fun hashCode_sameEngineAndSessionId_sameBucket() {
        val a = makeState(engineState = VoiceEngineState.IDLE, sessionId = "s1")
        val b = makeState(engineState = VoiceEngineState.IDLE, sessionId = "s1")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun hashCode_differentEngineState_differentHash() {
        val a = makeState(engineState = VoiceEngineState.IDLE)
        val b = makeState(engineState = VoiceEngineState.LISTENING)
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun hashCode_differentTokenUsage_differentHash() {
        val a = makeState(tokenUsage = 0)
        val b = makeState(tokenUsage = 999)
        assertNotEquals(a.hashCode(), b.hashCode())
    }
}
