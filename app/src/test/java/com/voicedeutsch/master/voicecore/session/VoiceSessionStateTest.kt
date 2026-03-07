// Путь: src/test/java/com/voicedeutsch/master/voicecore/session/VoiceSessionStateTest.kt
package com.voicedeutsch.master.voicecore.session

import com.voicedeutsch.master.domain.model.LearningStrategy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VoiceSessionStateTest {

    // ── Default values ───────────────────────────────────────────────────

    @Test
    fun defaultState_engineStateIsIdle() {
        assertEquals(VoiceEngineState.IDLE, VoiceSessionState().engineState)
    }

    @Test
    fun defaultState_connectionStateIsDisconnected() {
        assertEquals(ConnectionState.DISCONNECTED, VoiceSessionState().connectionState)
    }

    @Test
    fun defaultState_audioStateIsIdle() {
        assertEquals(AudioState.IDLE, VoiceSessionState().audioState)
    }

    @Test
    fun defaultState_booleanFlagsAreFalse() {
        val state = VoiceSessionState()
        assertFalse(state.isVoiceActive)
        assertFalse(state.isListening)
        assertFalse(state.isSpeaking)
        assertFalse(state.isProcessing)
    }

    @Test
    fun defaultState_currentStrategyIsLinearBook() {
        assertEquals(LearningStrategy.LINEAR_BOOK, VoiceSessionState().currentStrategy)
    }

    @Test
    fun defaultState_numericFieldsAreZero() {
        val state = VoiceSessionState()
        assertEquals(0L, state.sessionDurationMs)
        assertEquals(0, state.wordsLearnedInSession)
        assertEquals(0, state.wordsReviewedInSession)
        assertEquals(0, state.exercisesCompleted)
        assertEquals(0, state.tokenUsage)
    }

    @Test
    fun defaultState_stringFieldsAreEmpty() {
        val state = VoiceSessionState()
        assertEquals("", state.currentTranscript)
        assertEquals("", state.voiceTranscript)
    }

    @Test
    fun defaultState_waveformDataAreEmptyArrays() {
        val state = VoiceSessionState()
        assertEquals(0, state.voiceWaveformData.size)
        assertEquals(0, state.userWaveformData.size)
    }

    @Test
    fun defaultState_errorMessageIsNull() {
        assertNull(VoiceSessionState().errorMessage)
    }

    @Test
    fun defaultState_sessionIdIsNull() {
        assertNull(VoiceSessionState().sessionId)
    }

    // ── isSessionActive ──────────────────────────────────────────────────

    @Test
    fun isSessionActive_sessionActiveState_returnsTrue() {
        val state = VoiceSessionState(engineState = VoiceEngineState.SESSION_ACTIVE)
        assertTrue(state.isSessionActive)
    }

    @Test
    fun isSessionActive_listeningState_returnsTrue() {
        val state = VoiceSessionState(engineState = VoiceEngineState.LISTENING)
        assertTrue(state.isSessionActive)
    }

    @Test
    fun isSessionActive_processingState_returnsTrue() {
        val state = VoiceSessionState(engineState = VoiceEngineState.PROCESSING)
        assertTrue(state.isSessionActive)
    }

    @Test
    fun isSessionActive_speakingState_returnsTrue() {
        val state = VoiceSessionState(engineState = VoiceEngineState.SPEAKING)
        assertTrue(state.isSessionActive)
    }

    @Test
    fun isSessionActive_waitingState_returnsTrue() {
        val state = VoiceSessionState(engineState = VoiceEngineState.WAITING)
        assertTrue(state.isSessionActive)
    }

    @Test
    fun isSessionActive_idleState_returnsFalse() {
        val state = VoiceSessionState(engineState = VoiceEngineState.IDLE)
        assertFalse(state.isSessionActive)
    }

    @Test
    fun isSessionActive_errorState_returnsFalse() {
        val state = VoiceSessionState(engineState = VoiceEngineState.ERROR)
        assertFalse(state.isSessionActive)
    }

    // ── equals — FloatArray structural comparison ────────────────────────

    @Test
    fun equals_identicalFloatArrayContents_returnsTrue() {
        val a = VoiceSessionState(voiceWaveformData = floatArrayOf(1f, 2f, 3f))
        val b = VoiceSessionState(voiceWaveformData = floatArrayOf(1f, 2f, 3f))
        assertEquals(a, b)
    }

    @Test
    fun equals_differentVoiceWaveformData_returnsFalse() {
        val a = VoiceSessionState(voiceWaveformData = floatArrayOf(1f, 2f))
        val b = VoiceSessionState(voiceWaveformData = floatArrayOf(1f, 3f))
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentUserWaveformData_returnsFalse() {
        val a = VoiceSessionState(userWaveformData = floatArrayOf(0.5f))
        val b = VoiceSessionState(userWaveformData = floatArrayOf(0.9f))
        assertNotEquals(a, b)
    }

    @Test
    fun equals_sameReference_returnsTrue() {
        val state = VoiceSessionState()
        assertEquals(state, state)
    }

    @Test
    fun equals_differentType_returnsFalse() {
        val state = VoiceSessionState()
        assertNotEquals(state, "not a state")
    }

    @Test
    fun equals_differentEngineState_returnsFalse() {
        val a = VoiceSessionState(engineState = VoiceEngineState.IDLE)
        val b = VoiceSessionState(engineState = VoiceEngineState.SESSION_ACTIVE)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentTokenUsage_returnsFalse() {
        val a = VoiceSessionState(tokenUsage = 100)
        val b = VoiceSessionState(tokenUsage = 200)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentErrorMessage_returnsFalse() {
        val a = VoiceSessionState(errorMessage = "error")
        val b = VoiceSessionState(errorMessage = null)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_allFieldsIdentical_returnsTrue() {
        val waveform = floatArrayOf(0.1f, 0.2f)
        val a = VoiceSessionState(
            engineState = VoiceEngineState.LISTENING,
            connectionState = ConnectionState.CONNECTED,
            audioState = AudioState.RECORDING,
            isVoiceActive = true,
            isListening = true,
            isSpeaking = false,
            isProcessing = false,
            currentStrategy = LearningStrategy.REPETITION,
            sessionDurationMs = 60_000L,
            wordsLearnedInSession = 5,
            wordsReviewedInSession = 3,
            exercisesCompleted = 2,
            currentTranscript = "Hallo",
            voiceTranscript = "Welt",
            voiceWaveformData = waveform,
            userWaveformData = waveform,
            errorMessage = null,
            sessionId = "session-1",
            tokenUsage = 42,
        )
        val b = a.copy(
            voiceWaveformData = waveform.copyOf(),
            userWaveformData = waveform.copyOf(),
        )
        assertEquals(a, b)
    }

    // ── hashCode ─────────────────────────────────────────────────────────

    @Test
    fun hashCode_twoEqualStates_haveSameHashCode() {
        val a = VoiceSessionState(sessionId = "abc", exercisesCompleted = 3, tokenUsage = 10)
        val b = VoiceSessionState(sessionId = "abc", exercisesCompleted = 3, tokenUsage = 10)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun hashCode_differentEngineState_producesDifferentHash() {
        val a = VoiceSessionState(engineState = VoiceEngineState.IDLE)
        val b = VoiceSessionState(engineState = VoiceEngineState.SESSION_ACTIVE)
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun hashCode_differentTokenUsage_producesDifferentHash() {
        val a = VoiceSessionState(tokenUsage = 0)
        val b = VoiceSessionState(tokenUsage = 999)
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun hashCode_differentTranscript_producesDifferentHash() {
        val a = VoiceSessionState(currentTranscript = "Hallo")
        val b = VoiceSessionState(currentTranscript = "Tschüss")
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    // ── copy ─────────────────────────────────────────────────────────────

    @Test
    fun copy_changesOnlySpecifiedField() {
        val original = VoiceSessionState(sessionId = "s1", tokenUsage = 5)
        val copied = original.copy(tokenUsage = 50)
        assertEquals("s1", copied.sessionId)
        assertEquals(50, copied.tokenUsage)
        assertEquals(original.engineState, copied.engineState)
    }

    @Test
    fun copy_waveformDataIsShallowCopy() {
        val waveform = floatArrayOf(1f, 2f, 3f)
        val original = VoiceSessionState(voiceWaveformData = waveform)
        val copied = original.copy(tokenUsage = 1)
        assertTrue(copied.voiceWaveformData.contentEquals(waveform))
    }
}
