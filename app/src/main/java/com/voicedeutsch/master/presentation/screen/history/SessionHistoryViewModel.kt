package com.voicedeutsch.master.presentation.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicedeutsch.master.domain.model.session.LearningSession
import com.voicedeutsch.master.domain.repository.SessionRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionHistoryUiState(
    val isLoading: Boolean = true,
    val sessions: List<LearningSession> = emptyList(),
    val errorMessage: String? = null,
)

class SessionHistoryViewModel(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionHistoryUiState())
    val uiState: StateFlow<SessionHistoryUiState> = _uiState.asStateFlow()

    init { loadSessions() }

    private fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val userId = userRepository.getActiveUserId() ?: error("No user")
                val sessions = sessionRepository.getRecentSessions(userId, 100)
                _uiState.update { it.copy(isLoading = false, sessions = sessions) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}