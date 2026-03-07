// Путь: src/test/java/com/voicedeutsch/master/presentation/screen/session/SessionStateTest.kt
package com.voicedeutsch.master.presentation.screen.session

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SessionUiStateTest {

    // ── Creation with defaults ────────────────────────────────────────────

    @Test
    fun create_withDefaults_isLoadingFalse() {
        val state = SessionUiState()
        assertFalse(state.isLoading)
    }

    @Test
    fun create_withDefaults_isSessionActiveFalse() {
        val state = SessionUiState()
        assertFalse(state.isSessionActive)
    }

    @Test
    fun create_withDefaults_errorMessageNull() {
        val state = SessionUiState()
        assertNull(state.errorMessage)
    }

    @Test
    fun create_withDefaults_sessionResultNull() {
        val state = SessionUiState()
        assertNull(state.sessionResult)
    }

    @Test
    fun create_withDefaults_showTextInputFalse() {
        val state = SessionUiState()
        assertFalse(state.showTextInput)
    }

    @Test
    fun create_withDefaults_showHintFalse() {
        val state = SessionUiState()
        assertFalse(state.showHint)
    }

    @Test
    fun create_withDefaults_snackbarMessageNull() {
        val state = SessionUiState()
        assertNull(state.snackbarMessage)
    }

    // ── Creation with explicit values ────────────────────────────────────

    @Test
    fun create_withAllFieldsSet_allValuesCorrect() {
        val state = SessionUiState(
            isLoading = true,
            isSessionActive = true,
            errorMessage = "Connection failed",
            sessionResult = null,
            showTextInput = true,
            showHint = true,
            snackbarMessage = "Session saved",
        )
        assertTrue(state.isLoading)
        assertTrue(state.isSessionActive)
        assertEquals("Connection failed", state.errorMessage)
        assertNull(state.sessionResult)
        assertTrue(state.showTextInput)
        assertTrue(state.showHint)
        assertEquals("Session saved", state.snackbarMessage)
    }

    // ── copy() ───────────────────────────────────────────────────────────

    @Test
    fun copy_setIsLoadingTrue_onlyIsLoadingChanges() {
        val original = SessionUiState()
        val copy = original.copy(isLoading = true)
        assertTrue(copy.isLoading)
        assertFalse(copy.isSessionActive)
        assertNull(copy.errorMessage)
        assertNull(copy.snackbarMessage)
    }

    @Test
    fun copy_setErrorMessage_errorMessageUpdated() {
        val original = SessionUiState()
        val copy = original.copy(errorMessage = "Network error")
        assertEquals("Network error", copy.errorMessage)
        assertFalse(copy.isLoading)
        assertFalse(copy.isSessionActive)
    }

    @Test
    fun copy_clearErrorMessage_errorMessageBecomesNull() {
        val original = SessionUiState(errorMessage = "Some error")
        val copy = original.copy(errorMessage = null)
        assertNull(copy.errorMessage)
    }

    @Test
    fun copy_setSnackbarMessage_snackbarUpdated() {
        val original = SessionUiState()
        val copy = original.copy(snackbarMessage = "Achievement unlocked!")
        assertEquals("Achievement unlocked!", copy.snackbarMessage)
        assertNull(original.snackbarMessage)
    }

    @Test
    fun copy_setIsSessionActiveTrue_sessionActiveUpdated() {
        val original = SessionUiState()
        val copy = original.copy(isSessionActive = true)
        assertTrue(copy.isSessionActive)
        assertFalse(original.isSessionActive)
    }

    @Test
    fun copy_setShowHintTrue_showHintUpdated() {
        val original = SessionUiState()
        val copy = original.copy(showHint = true)
        assertTrue(copy.showHint)
        assertFalse(original.showHint)
    }

    @Test
    fun copy_setShowTextInputTrue_showTextInputUpdated() {
        val original = SessionUiState()
        val copy = original.copy(showTextInput = true)
        assertTrue(copy.showTextInput)
        assertFalse(original.showTextInput)
    }

    // ── equals / hashCode ────────────────────────────────────────────────

    @Test
    fun equals_twoDefaultInstances_areEqual() {
        val a = SessionUiState()
        val b = SessionUiState()
        assertEquals(a, b)
    }

    @Test
    fun equals_sameNonDefaultValues_areEqual() {
        val a = SessionUiState(isLoading = true, errorMessage = "err")
        val b = SessionUiState(isLoading = true, errorMessage = "err")
        assertEquals(a, b)
    }

    @Test
    fun equals_differentIsLoading_notEqual() {
        val a = SessionUiState(isLoading = true)
        val b = SessionUiState(isLoading = false)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentErrorMessage_notEqual() {
        val a = SessionUiState(errorMessage = "error A")
        val b = SessionUiState(errorMessage = "error B")
        assertNotEquals(a, b)
    }

    @Test
    fun hashCode_equalInstances_sameHashCode() {
        val a = SessionUiState(isSessionActive = true, showHint = true)
        val b = SessionUiState(isSessionActive = true, showHint = true)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun toString_containsClassName() {
        val state = SessionUiState()
        assertTrue(state.toString().contains("SessionUiState"))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SessionEvent tests
// ═══════════════════════════════════════════════════════════════════════════════

class SessionEventTest {

    // ── Singleton data objects ────────────────────────────────────────────

    @Test
    fun startSession_isInstanceOfSessionEvent() {
        val event: SessionEvent = SessionEvent.StartSession
        assertTrue(event is SessionEvent.StartSession)
    }

    @Test
    fun endSession_isInstanceOfSessionEvent() {
        val event: SessionEvent = SessionEvent.EndSession
        assertTrue(event is SessionEvent.EndSession)
    }

    @Test
    fun pauseResume_isInstanceOfSessionEvent() {
        val event: SessionEvent = SessionEvent.PauseResume
        assertTrue(event is SessionEvent.PauseResume)
    }

    @Test
    fun dismissError_isInstanceOfSessionEvent() {
        val event: SessionEvent = SessionEvent.DismissError
        assertTrue(event is SessionEvent.DismissError)
    }

    @Test
    fun dismissResult_isInstanceOfSessionEvent() {
        val event: SessionEvent = SessionEvent.DismissResult
        assertTrue(event is SessionEvent.DismissResult)
    }

    @Test
    fun toggleTextInput_isInstanceOfSessionEvent() {
        val event: SessionEvent = SessionEvent.ToggleTextInput
        assertTrue(event is SessionEvent.ToggleTextInput)
    }

    @Test
    fun dismissHint_isInstanceOfSessionEvent() {
        val event: SessionEvent = SessionEvent.DismissHint
        assertTrue(event is SessionEvent.DismissHint)
    }

    @Test
    fun consumeSnackbar_isInstanceOfSessionEvent() {
        val event: SessionEvent = SessionEvent.ConsumeSnackbar
        assertTrue(event is SessionEvent.ConsumeSnackbar)
    }

    @Test
    fun permissionDenied_isInstanceOfSessionEvent() {
        val event: SessionEvent = SessionEvent.PermissionDenied
        assertTrue(event is SessionEvent.PermissionDenied)
    }

    // ── Singleton identity ─────────────────────────────────────────────────

    @Test
    fun startSession_sameInstance_sameObject() {
        assertSame(SessionEvent.StartSession, SessionEvent.StartSession)
    }

    @Test
    fun endSession_sameInstance_sameObject() {
        assertSame(SessionEvent.EndSession, SessionEvent.EndSession)
    }

    @Test
    fun pauseResume_sameInstance_sameObject() {
        assertSame(SessionEvent.PauseResume, SessionEvent.PauseResume)
    }

    @Test
    fun permissionDenied_sameInstance_sameObject() {
        assertSame(SessionEvent.PermissionDenied, SessionEvent.PermissionDenied)
    }

    // ── SendTextMessage data class ────────────────────────────────────────

    @Test
    fun sendTextMessage_withNormalText_holdsText() {
        val event = SessionEvent.SendTextMessage("Hallo Welt")
        assertEquals("Hallo Welt", event.text)
    }

    @Test
    fun sendTextMessage_withEmptyString_holdsEmptyString() {
        val event = SessionEvent.SendTextMessage("")
        assertEquals("", event.text)
    }

    @Test
    fun sendTextMessage_withWhitespace_holdsWhitespace() {
        val event = SessionEvent.SendTextMessage("   ")
        assertEquals("   ", event.text)
    }

    @Test
    fun sendTextMessage_withLongText_holdsFullText() {
        val longText = "a".repeat(1000)
        val event = SessionEvent.SendTextMessage(longText)
        assertEquals(1000, event.text.length)
    }

    @Test
    fun sendTextMessage_equals_sameText_equal() {
        val a = SessionEvent.SendTextMessage("Danke schön")
        val b = SessionEvent.SendTextMessage("Danke schön")
        assertEquals(a, b)
    }

    @Test
    fun sendTextMessage_equals_differentText_notEqual() {
        val a = SessionEvent.SendTextMessage("Danke")
        val b = SessionEvent.SendTextMessage("Bitte")
        assertNotEquals(a, b)
    }

    @Test
    fun sendTextMessage_hashCode_sameTextSameHash() {
        val a = SessionEvent.SendTextMessage("Guten Tag")
        val b = SessionEvent.SendTextMessage("Guten Tag")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun sendTextMessage_copy_updatesText() {
        val original = SessionEvent.SendTextMessage("Hallo")
        val copy = original.copy(text = "Tschüss")
        assertEquals("Tschüss", copy.text)
        assertEquals("Hallo", original.text)
    }

    // ── Distinct event types ───────────────────────────────────────────────

    @Test
    fun startSession_notEqualsEndSession() {
        assertNotEquals(SessionEvent.StartSession, SessionEvent.EndSession)
    }

    @Test
    fun startSession_notEqualsPauseResume() {
        assertNotEquals(SessionEvent.StartSession, SessionEvent.PauseResume)
    }

    @Test
    fun dismissError_notEqualsDismissResult() {
        assertNotEquals(SessionEvent.DismissError, SessionEvent.DismissResult)
    }

    @Test
    fun dismissHint_notEqualsConsumeSnackbar() {
        assertNotEquals(SessionEvent.DismissHint, SessionEvent.ConsumeSnackbar)
    }

    // ── when exhaustive matching ──────────────────────────────────────────

    @Test
    fun whenExhaustive_sendTextMessage_returnsCorrectLabel() {
        val event: SessionEvent = SessionEvent.SendTextMessage("Test")
        val label = when (event) {
            is SessionEvent.StartSession -> "start"
            is SessionEvent.EndSession -> "end"
            is SessionEvent.PauseResume -> "pause"
            is SessionEvent.SendTextMessage -> "text:${event.text}"
            is SessionEvent.DismissError -> "dismissError"
            is SessionEvent.DismissResult -> "dismissResult"
            is SessionEvent.ToggleTextInput -> "toggle"
            is SessionEvent.DismissHint -> "hint"
            is SessionEvent.ConsumeSnackbar -> "snackbar"
            is SessionEvent.PermissionDenied -> "denied"
        }
        assertEquals("text:Test", label)
    }

    @Test
    fun whenExhaustive_permissionDenied_returnsCorrectLabel() {
        val event: SessionEvent = SessionEvent.PermissionDenied
        val label = when (event) {
            is SessionEvent.StartSession -> "start"
            is SessionEvent.EndSession -> "end"
            is SessionEvent.PauseResume -> "pause"
            is SessionEvent.SendTextMessage -> "text"
            is SessionEvent.DismissError -> "dismissError"
            is SessionEvent.DismissResult -> "dismissResult"
            is SessionEvent.ToggleTextInput -> "toggle"
            is SessionEvent.DismissHint -> "hint"
            is SessionEvent.ConsumeSnackbar -> "snackbar"
            is SessionEvent.PermissionDenied -> "denied"
        }
        assertEquals("denied", label)
    }
}
