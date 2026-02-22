package com.voicedeutsch.master.presentation.screen.session

import com.voicedeutsch.master.domain.model.session.SessionResult
import com.voicedeutsch.master.voicecore.session.VoiceSessionState

// ═══════════════════════════════════════════════════════════════════════════════
// SessionState — UI state for the SessionScreen
// Architecture reference: lines 545-560 (SessionState structure, MVI Pattern)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Immutable UI state consumed by [SessionScreen].
 *
 * The voice state itself lives in [VoiceSessionState] (emitted by VoiceCoreEngine).
 * This state layer carries purely UI-level concerns:
 *  - loading / error indicators
 *  - session result for the post-session summary card
 *  - onboarding hints
 *  - text input visibility toggle
 */
data class SessionUiState(
    /** True while the engine is initialising / connecting. */
    val isLoading: Boolean = false,
    /** True after a session has been successfully started. */
    val isSessionActive: Boolean = false,
    /** Non-null when an unrecoverable error has occurred. */
    val errorMessage: String? = null,
    /** Non-null after a session has ended — used for summary overlay. */
    val sessionResult: SessionResult? = null,
    /** Show the "type a message" text field (accessibility fallback). */
    val showTextInput: Boolean = false,
    /** Display a first-launch hint overlay. */
    val showHint: Boolean = false,
    /** Snackbar message, consumed on display. */
    val snackbarMessage: String? = null,
)

// ═══════════════════════════════════════════════════════════════════════════════
// SessionEvent — user intents dispatched to SessionViewModel
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * All possible user actions on the Session screen — MVI intents.
 *
 * Processed exclusively by [SessionViewModel.onEvent].
 */
sealed interface SessionEvent {
    /** Start a new voice session. */
    data object StartSession : SessionEvent
    /** Gracefully end the current session and save results. */
    data object EndSession : SessionEvent
    /** Toggle listening pause / resume without ending the session. */
    data object PauseResume : SessionEvent
    /** Toggle microphone (start/stop recording). */
    data object ToggleMic : SessionEvent
    /** Send a typed text message as a fallback input. */
    data class SendTextMessage(val text: String) : SessionEvent
    /** Dismiss the current error message. */
    data object DismissError : SessionEvent
    /** Dismiss the post-session result card. */
    data object DismissResult : SessionEvent
    /** Toggle the text-input field visibility. */
    data object ToggleTextInput : SessionEvent
    /** Dismiss the hint overlay. */
    data object DismissHint : SessionEvent
    /** Acknowledge and clear a shown snackbar. */
    data object ConsumeSnackbar : SessionEvent
    /** Пользователь отказал в доступе к микрофону. */
    data object PermissionDenied : SessionEvent
}
