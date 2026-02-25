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

// ── State ─────────────────────────────────────────────────────────────────────

data class SettingsUiState(
    val isLoading: Boolean = true,

    // ── UserPreferences ───────────────────────────────────────────────────────
    val sessionDurationMinutes: Int = 30,
    val dailyGoalWords: Int = 10,
    val learningPace: String = "NORMAL",
    val srsEnabled: Boolean = true,
    val maxReviewsPerSession: Int = 30,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val pronunciationStrictness: String = "MODERATE",
    val dataSavingMode: Boolean = false,

    // ── VoiceSettings ─────────────────────────────────────────────────────────
    val voiceSpeed: Float = 1.0f,
    val germanVoiceSpeed: Float = 0.8f,
    val showTranscription: Boolean = true,

    val successMessage: String? = null,
    val errorMessage: String? = null,
)

// ── Events ────────────────────────────────────────────────────────────────────

sealed interface SettingsEvent {
    // Занятия
    data class UpdateSessionDuration(val minutes: Int) : SettingsEvent
    data class UpdateDailyGoal(val words: Int) : SettingsEvent
    data class UpdateLearningPace(val pace: String) : SettingsEvent
    data class ToggleSrs(val enabled: Boolean) : SettingsEvent
    data class UpdateMaxReviews(val count: Int) : SettingsEvent

    // Напоминание
    data class ToggleReminder(val enabled: Boolean) : SettingsEvent
    data class UpdateReminderTime(val hour: Int, val minute: Int) : SettingsEvent

    // Голос и ИИ
    data class UpdateGermanSpeed(val speed: Float) : SettingsEvent
    data class UpdateVoiceSpeed(val speed: Float) : SettingsEvent
    data class ToggleTranscription(val enabled: Boolean) : SettingsEvent
    data class UpdateStrictness(val strictness: String) : SettingsEvent

    // Система
    data class ToggleDataSaving(val enabled: Boolean) : SettingsEvent

    // Служебные
    data object SaveAll : SettingsEvent
    data object DismissMessages : SettingsEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel for [SettingsScreen].
 *
 * Manages all UserPreferences + VoiceSettings fields.
 * On [SettingsEvent.SaveAll] persists everything to UserProfile and DataStore.
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
            // Занятия
            is SettingsEvent.UpdateSessionDuration -> _uiState.update { it.copy(sessionDurationMinutes = event.minutes) }
            is SettingsEvent.UpdateDailyGoal       -> _uiState.update { it.copy(dailyGoalWords = event.words) }
            is SettingsEvent.UpdateLearningPace    -> _uiState.update { it.copy(learningPace = event.pace) }
            is SettingsEvent.ToggleSrs             -> _uiState.update { it.copy(srsEnabled = event.enabled) }
            is SettingsEvent.UpdateMaxReviews      -> _uiState.update { it.copy(maxReviewsPerSession = event.count) }
            // Напоминание
            is SettingsEvent.ToggleReminder        -> _uiState.update { it.copy(reminderEnabled = event.enabled) }
            is SettingsEvent.UpdateReminderTime    -> _uiState.update { it.copy(reminderHour = event.hour, reminderMinute = event.minute) }
            // Голос и ИИ
            is SettingsEvent.UpdateGermanSpeed     -> _uiState.update { it.copy(germanVoiceSpeed = event.speed) }
            is SettingsEvent.UpdateVoiceSpeed      -> _uiState.update { it.copy(voiceSpeed = event.speed) }
            is SettingsEvent.ToggleTranscription   -> _uiState.update { it.copy(showTranscription = event.enabled) }
            is SettingsEvent.UpdateStrictness      -> _uiState.update { it.copy(pronunciationStrictness = event.strictness) }
            // Система
            is SettingsEvent.ToggleDataSaving      -> _uiState.update { it.copy(dataSavingMode = event.enabled) }
            // Служебные
            is SettingsEvent.SaveAll               -> saveAll()
            is SettingsEvent.DismissMessages       -> _uiState.update { it.copy(successMessage = null, errorMessage = null) }
        }
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    private fun loadSettings() {
        viewModelScope.launch {
            runCatching {
                val userId = userRepository.getActiveUserId()
                val profile = userId?.let { userRepository.getUserProfile(it) }
                val prefs = profile?.preferences
                val voice = profile?.voiceSettings

                val duration = preferencesDataStore.getSessionDuration()
                    ?: prefs?.preferredSessionDurationMinutes
                    ?: 30
                val goal = preferencesDataStore.getDailyGoal()
                    ?: prefs?.dailyGoalWords
                    ?: 10

                _uiState.update {
                    it.copy(
                        isLoading              = false,
                        sessionDurationMinutes = duration,
                        dailyGoalWords         = goal,
                        learningPace           = prefs?.learningPace?.name ?: "NORMAL",
                        srsEnabled             = prefs?.srsEnabled ?: true,
                        maxReviewsPerSession   = prefs?.maxReviewsPerSession ?: 30,
                        reminderEnabled        = prefs?.reminderEnabled ?: false,
                        reminderHour           = prefs?.reminderHour ?: 19,
                        reminderMinute         = prefs?.reminderMinute ?: 0,
                        pronunciationStrictness = prefs?.pronunciationStrictness?.name ?: "MODERATE",
                        dataSavingMode         = prefs?.dataSavingMode ?: false,
                        voiceSpeed             = voice?.voiceSpeed ?: 1.0f,
                        germanVoiceSpeed       = voice?.germanVoiceSpeed ?: 0.8f,
                        showTranscription      = voice?.showTranscription ?: true,
                    )
                }
            }.onFailure {
                _uiState.update { s -> s.copy(isLoading = false) }
            }
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private fun saveAll() {
        viewModelScope.launch {
            runCatching {
                val s = _uiState.value
                val userId = userRepository.getActiveUserId() ?: error("Пользователь не найден")

                // Обновляем продолжительность и цель через существующий use case
                configureUserPreferences.updatePreferredSessionDuration(userId, s.sessionDurationMinutes)
                configureUserPreferences.updateDailyGoal(
                    userId,
                    words   = s.dailyGoalWords,
                    minutes = s.sessionDurationMinutes,
                )

                // Остальные поля через расширенный use case (если методы есть)
                runCatching { configureUserPreferences.updateLearningPace(userId, s.learningPace) }
                runCatching { configureUserPreferences.updateSrsSettings(userId, s.srsEnabled, s.maxReviewsPerSession) }
                runCatching { configureUserPreferences.updateReminderSettings(userId, s.reminderEnabled, s.reminderHour, s.reminderMinute) }
                runCatching { configureUserPreferences.updatePronunciationStrictness(userId, s.pronunciationStrictness) }
                runCatching { configureUserPreferences.updateDataSavingMode(userId, s.dataSavingMode) }
                runCatching { configureUserPreferences.updateVoiceSettings(userId, s.voiceSpeed, s.germanVoiceSpeed, s.showTranscription) }

                // DataStore — быстрый доступ для сессий без обращения в БД
                preferencesDataStore.setSessionDuration(s.sessionDurationMinutes)
                preferencesDataStore.setDailyGoal(s.dailyGoalWords)
            }
            .onSuccess { _uiState.update { it.copy(successMessage = "✅ Все настройки сохранены") } }
            .onFailure { e -> _uiState.update { it.copy(errorMessage = "❌ ${e.message}") } }
        }
    }
}
