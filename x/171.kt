package com.voicedeutsch.master.presentation.screen.session

import com.voicedeutsch.master.domain.model.session.SessionResult
import org.junit.Assert.*
import org.junit.Test

class SessionUiStateTest {

    @Test
    fun `default state has correct initial values`() {
        val state = SessionUiState()
        assertFalse(state.isLoading)
        assertFalse(state.isSessionActive)
        assertNull(state.errorMessage)
        assertNull(state.sessionResult)
        assertFalse(state.showTextInput)
        assertFalse(state.showHint)
        assertNull(state.snackbarMessage)
    }

    @Test
    fun `copy with isLoading true`() {
        val state = SessionUiState().copy(isLoading = true)
        assertTrue(state.isLoading)
    }

    @Test
    fun `copy with isSessionActive true`() {
        val state = SessionUiState().copy(isSessionActive = true)
        assertTrue(state.isSessionActive)
    }

    @Test
    fun `copy with errorMessage`() {
        val state = SessionUiState().copy(errorMessage = "Network error")
        assertEquals("Network error", state.errorMessage)
    }

    @Test
    fun `copy with showTextInput true`() {
        val state = SessionUiState().copy(showTextInput = true)
        assertTrue(state.showTextInput)
    }

    @Test
    fun `copy with showHint true`() {
        val state = SessionUiState().copy(showHint = true)
        assertTrue(state.showHint)
    }

    @Test
    fun `copy with snackbarMessage`() {
        val state = SessionUiState().copy(snackbarMessage = "Session saved")
        assertEquals("Session saved", state.snackbarMessage)
    }

    @Test
    fun `two states with same values are equal`() {
        val a = SessionUiState(isLoading = true, errorMessage = "err")
        val b = SessionUiState(isLoading = true, errorMessage = "err")
        assertEquals(a, b)
    }

    @Test
    fun `two states with different values are not equal`() {
        val a = SessionUiState(isLoading = true)
        val b = SessionUiState(isLoading = false)
        assertNotEquals(a, b)
    }

    @Test
    fun `clearing errorMessage via copy`() {
        val state = SessionUiState(errorMessage = "err").copy(errorMessage = null)
        assertNull(state.errorMessage)
    }

    @Test
    fun `clearing snackbarMessage via copy`() {
        val state = SessionUiState(snackbarMessage = "msg").copy(snackbarMessage = null)
        assertNull(state.snackbarMessage)
    }
}

class SessionEventTest {

    @Test
    fun `StartSession is SessionEvent`() {
        val event: SessionEvent = SessionEvent.StartSession
        assertTrue(event is SessionEvent.StartSession)
    }

    @Test
    fun `EndSession is SessionEvent`() {
        val event: SessionEvent = SessionEvent.EndSession
        assertTrue(event is SessionEvent.EndSession)
    }

    @Test
    fun `PauseResume is SessionEvent`() {
        val event: SessionEvent = SessionEvent.PauseResume
        assertTrue(event is SessionEvent.PauseResume)
    }

    @Test
    fun `SendTextMessage holds correct text`() {
        val event = SessionEvent.SendTextMessage("Hallo")
        assertEquals("Hallo", event.text)
    }

    @Test
    fun `SendTextMessage with empty string`() {
        val event = SessionEvent.SendTextMessage("")
        assertEquals("", event.text)
    }

    @Test
    fun `DismissError is SessionEvent`() {
        val event: SessionEvent = SessionEvent.DismissError
        assertTrue(event is SessionEvent.DismissError)
    }

    @Test
    fun `DismissResult is SessionEvent`() {
        val event: SessionEvent = SessionEvent.DismissResult
        assertTrue(event is SessionEvent.DismissResult)
    }

    @Test
    fun `ToggleTextInput is SessionEvent`() {
        val event: SessionEvent = SessionEvent.ToggleTextInput
        assertTrue(event is SessionEvent.ToggleTextInput)
    }

    @Test
    fun `DismissHint is SessionEvent`() {
        val event: SessionEvent = SessionEvent.DismissHint
        assertTrue(event is SessionEvent.DismissHint)
    }

    @Test
    fun `ConsumeSnackbar is SessionEvent`() {
        val event: SessionEvent = SessionEvent.ConsumeSnackbar
        assertTrue(event is SessionEvent.ConsumeSnackbar)
    }

    @Test
    fun `PermissionDenied is SessionEvent`() {
        val event: SessionEvent = SessionEvent.PermissionDenied
        assertTrue(event is SessionEvent.PermissionDenied)
    }

    @Test
    fun `SendTextMessage equality`() {
        val a = SessionEvent.SendTextMessage("test")
        val b = SessionEvent.SendTextMessage("test")
        assertEquals(a, b)
    }

    @Test
    fun `SendTextMessage inequality`() {
        val a = SessionEvent.SendTextMessage("hello")
        val b = SessionEvent.SendTextMessage("world")
        assertNotEquals(a, b)
    }
}
