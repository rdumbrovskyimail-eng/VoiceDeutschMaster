package com.voicedeutsch.master.presentation.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.domain.usecase.user.ConfigureUserPreferencesUseCase
import com.voicedeutsch.master.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── State / Event ─────────────────────────────────────────────────────────────

data class SettingsUiState(
    val isLoading: Boolean = true,
    val geminiApiKey: String = "",
    val geminiApiKeyVisible: Boolean = false,
    val theme: String = "system",
    val sessionDurationMinutes: Int = 30,
    val dailyGoalWords: Int = 10,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val successMessage: String? = null,
    val errorMessage: String? = null,
)

sealed interface SettingsEvent {
    data class UpdateApiKey(val key: String) : SettingsEvent
    data object ToggleApiKeyVisibility : SettingsEvent
    data object SaveApiKey : SettingsEvent
    data class UpdateTheme(val theme: String) : SettingsEvent
    data class UpdateSessionDuration(val minutes: Int) : SettingsEvent
    data class UpdateDailyGoal(val words: Int) : SettingsEvent
    data class ToggleReminder(val enabled: Boolean) : SettingsEvent
    data class UpdateReminderTime(val hour: Int, val minute: Int) : SettingsEvent
    data object DismissMessages : SettingsEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel for [SettingsScreen].
 *
 * Manages API key, theme, session preferences, and reminders.
 *
 * @param configureUserPreferences  Use case to save learning preferences to UserProfile.
 * @param preferencesDataStore      DataStore for API key, theme, and onboarding flags.
 * @param userRepository            Provides the active user ID.
 */
class SettingsViewModel(
    private val configureUserPreferences: ConfigureUserPreferencesUseCase,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { loadSettings() }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.UpdateApiKey          -> _uiState.update { it.copy(geminiApiKey = event.key) }
            is SettingsEvent.ToggleApiKeyVisibility -> _uiState.update { it.copy(geminiApiKeyVisible = !it.geminiApiKeyVisible) }
            is SettingsEvent.SaveApiKey            -> saveApiKey()
            is SettingsEvent.UpdateTheme           -> updateTheme(event.theme)
            is SettingsEvent.UpdateSessionDuration -> _uiState.update { it.copy(sessionDurationMinutes = event.minutes) }
            is SettingsEvent.UpdateDailyGoal       -> _uiState.update { it.copy(dailyGoalWords = event.words) }
            is SettingsEvent.ToggleReminder        -> _uiState.update { it.copy(reminderEnabled = event.enabled) }
            is SettingsEvent.UpdateReminderTime    -> _uiState.update { it.copy(reminderHour = event.hour, reminderMinute = event.minute) }
            is SettingsEvent.DismissMessages       -> _uiState.update { it.copy(successMessage = null, errorMessage = null) }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            runCatching {
                val apiKey = preferencesDataStore.getGeminiApiKey() ?: ""
                val theme  = preferencesDataStore.getThemeFlow().let {
                    var t = "system"
                    it.collect { v -> t = v; return@collect }
                    t
                }
                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        geminiApiKey = apiKey,
                    )
                }
            }.onFailure {
                _uiState.update { s -> s.copy(isLoading = false) }
            }
        }
    }

    private fun saveApiKey() {
        val key = _uiState.value.geminiApiKey.trim()
        if (key.isBlank()) {
            _uiState.update { it.copy(errorMessage = "API ключ не может быть пустым") }
            return
        }
        viewModelScope.launch {
            runCatching { preferencesDataStore.setGeminiApiKey(key) }
                .onSuccess { _uiState.update { it.copy(successMessage = "API ключ сохранён") } }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
        }
    }

    private fun updateTheme(theme: String) {
        viewModelScope.launch {
            preferencesDataStore.setTheme(theme)
            _uiState.update { it.copy(theme = theme) }
        }
    }
}
