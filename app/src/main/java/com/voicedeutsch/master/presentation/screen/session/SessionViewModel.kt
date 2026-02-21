package com.voicedeutsch.master.presentation.screen.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.voicecore.engine.GeminiConfig
import com.voicedeutsch.master.voicecore.engine.VoiceCoreEngine
import com.voicedeutsch.master.voicecore.session.VoiceSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [SessionScreen].
 *
 * Manages voice session lifecycle via [VoiceCoreEngine].
 *
 * @param voiceCoreEngine  The voice engine (injected as singleton by Koin).
 * @param userRepository   Provides the active user ID.
 * @param preferencesDataStore  Provides Gemini API key and onboarding state.
 */
class SessionViewModel(
    private val voiceCoreEngine: VoiceCoreEngine,
    private val userRepository: UserRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
) : ViewModel() {

    val voiceState: StateFlow<VoiceSessionState> = voiceCoreEngine.sessionState

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    fun onEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.StartSession    -> startSession()
            is SessionEvent.EndSession      -> endSession()
            is SessionEvent.PauseResume     -> togglePause()
            is SessionEvent.ToggleMic       -> toggleMic()
            is SessionEvent.SendTextMessage -> sendText(event.text)
            is SessionEvent.DismissError    -> _uiState.update { it.copy(errorMessage = null) }
            is SessionEvent.DismissResult   -> _uiState.update { it.copy(sessionResult = null) }
            is SessionEvent.ToggleTextInput -> _uiState.update { it.copy(showTextInput = !it.showTextInput) }
            is SessionEvent.DismissHint     -> _uiState.update { it.copy(showHint = false) }
            is SessionEvent.ConsumeSnackbar -> _uiState.update { it.copy(snackbarMessage = null) }
        }
    }

    private fun startSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching {
                // 1. Resolve active user
                val userId = userRepository.getActiveUserId()
                    ?: error("No active user found. Please complete onboarding.")

                // 2. Resolve Gemini API key
                // FIX: getGeminiApiKey() is a suspend fun returning String? directly,
                // NOT a Flow<String?> — remove the invalid .firstOrNull() call
                val apiKey = preferencesDataStore.getGeminiApiKey()
                    ?: error("Gemini API key not configured. Go to Settings.")

                // 3. Initialise and start
                val geminiConfig = GeminiConfig(apiKey = apiKey)
                voiceCoreEngine.initialize(geminiConfig)
                voiceCoreEngine.startSession(userId)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSessionActive = true,
                        showHint = false,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unknown error starting session",
                    )
                }
            }
        }
    }

    private fun endSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = runCatching {
                voiceCoreEngine.endSession()
            }.getOrNull()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSessionActive = false,
                    sessionResult = result,
                )
            }
        }
    }

    private fun togglePause() {
        val current = voiceState.value
        if (current.isListening) {
            voiceCoreEngine.stopListening()
        } else if (current.isSessionActive) {
            voiceCoreEngine.startListening()
        }
    }

    private fun toggleMic() {
        val current = voiceState.value
        if (current.isListening) voiceCoreEngine.stopListening()
        else voiceCoreEngine.startListening()
    }

    private fun sendText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            runCatching {
                voiceCoreEngine.sendTextMessage(text)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(snackbarMessage = error.message ?: "Failed to send message")
                }
            }
        }
    }

    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCleared() {
        super.onCleared()
        cleanupScope.launch {
            runCatching { voiceCoreEngine.endSession() }
        }
        cleanupScope.cancel()
    }
}
