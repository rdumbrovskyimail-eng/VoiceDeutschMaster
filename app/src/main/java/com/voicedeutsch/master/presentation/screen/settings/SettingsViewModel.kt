// app/src/main/java/com/voicedeutsch/master/presentation/screen/settings/SettingsViewModel.kt
package com.voicedeutsch.master.presentation.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.data.remote.sync.BackupManager
import com.voicedeutsch.master.data.remote.sync.BackupMetadata
import com.voicedeutsch.master.data.remote.sync.BackupResult
import com.voicedeutsch.master.domain.model.user.LearningPace
import com.voicedeutsch.master.domain.model.user.PronunciationStrictness
import com.voicedeutsch.master.domain.usecase.user.ConfigureUserPreferencesUseCase
import com.voicedeutsch.master.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,

    // Профиль пользователя
    val userName: String = "",
    val userAge: String = "",
    val userHobbies: String = "",
    val userLevel: String = "A1",
    val userNativeLanguage: String = "ru",
    val userLearningGoals: String = "",

    // Обучение
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

    // Голос
    val voiceSpeed: Float = 1.0f,
    val germanVoiceSpeed: Float = 0.8f,
    val showTranscription: Boolean = true,

    // Gemini
    val geminiTemperature: Float = 0.5f,
    val geminiTopP: Float = 0.95f,
    val geminiTopK: Int = 40,
    val geminiVoiceName: String = "Kore",
    val geminiModelName: String = "gemini-2.5-flash-native-audio-preview-12-2025",
    val geminiMaxContextTokens: Int = 131_072,
    val geminiInputTranscription: Boolean = false,
    val geminiOutputTranscription: Boolean = false,

    // Backup
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val isLoadingBackups: Boolean = false,
    val cloudBackups: List<BackupMetadata> = emptyList(),
    val showRestoreDialog: Boolean = false,

    val successMessage: String? = null,
    val errorMessage: String? = null,
)

sealed interface SettingsEvent {
    // Профиль
    data class UpdateUserName(val name: String) : SettingsEvent
    data class UpdateUserAge(val age: String) : SettingsEvent
    data class UpdateUserHobbies(val hobbies: String) : SettingsEvent
    data class UpdateUserLevel(val level: String) : SettingsEvent
    data class UpdateUserNativeLanguage(val language: String) : SettingsEvent
    data class UpdateUserLearningGoals(val goals: String) : SettingsEvent

    // Обучение
    data class UpdateSessionDuration(val minutes: Int) : SettingsEvent
    data class UpdateDailyGoal(val words: Int) : SettingsEvent
    data class UpdateLearningPace(val pace: String) : SettingsEvent
    data class ToggleSrs(val enabled: Boolean) : SettingsEvent
    data class UpdateMaxReviews(val count: Int) : SettingsEvent

    data class ToggleReminder(val enabled: Boolean) : SettingsEvent
    data class UpdateReminderTime(val hour: Int, val minute: Int) : SettingsEvent

    // Голос
    data class UpdateGermanSpeed(val speed: Float) : SettingsEvent
    data class UpdateVoiceSpeed(val speed: Float) : SettingsEvent
    data class ToggleTranscription(val enabled: Boolean) : SettingsEvent
    data class UpdateStrictness(val strictness: String) : SettingsEvent

    data class ToggleDataSaving(val enabled: Boolean) : SettingsEvent

    // Gemini
    data class UpdateGeminiTemperature(val value: Float) : SettingsEvent
    data class UpdateGeminiTopP(val value: Float) : SettingsEvent
    data class UpdateGeminiTopK(val value: Int) : SettingsEvent
    data class UpdateGeminiVoiceName(val name: String) : SettingsEvent
    data class UpdateGeminiMaxContext(val tokens: Int) : SettingsEvent
    data class ToggleGeminiInputTranscription(val enabled: Boolean) : SettingsEvent
    data class ToggleGeminiOutputTranscription(val enabled: Boolean) : SettingsEvent

    // Backup
    data object CreateCloudBackup : SettingsEvent
    data object LoadCloudBackups : SettingsEvent
    data object ShowRestoreDialog : SettingsEvent
    data object HideRestoreDialog : SettingsEvent
    data class RestoreFromCloud(val metadata: BackupMetadata) : SettingsEvent

    data object SaveAll : SettingsEvent
    data object DismissMessages : SettingsEvent
}

class SettingsViewModel(
    private val configureUserPreferences: ConfigureUserPreferencesUseCase,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val userRepository: UserRepository,
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { loadSettings() }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            // Профиль
            is SettingsEvent.UpdateUserName           -> _uiState.update { it.copy(userName = event.name) }
            is SettingsEvent.UpdateUserAge            -> _uiState.update { it.copy(userAge = event.age) }
            is SettingsEvent.UpdateUserHobbies        -> _uiState.update { it.copy(userHobbies = event.hobbies) }
            is SettingsEvent.UpdateUserLevel          -> _uiState.update { it.copy(userLevel = event.level) }
            is SettingsEvent.UpdateUserNativeLanguage -> _uiState.update { it.copy(userNativeLanguage = event.language) }
            is SettingsEvent.UpdateUserLearningGoals  -> _uiState.update { it.copy(userLearningGoals = event.goals) }
            // Обучение
            is SettingsEvent.UpdateSessionDuration    -> _uiState.update { it.copy(sessionDurationMinutes = event.minutes) }
            is SettingsEvent.UpdateDailyGoal          -> _uiState.update { it.copy(dailyGoalWords = event.words) }
            is SettingsEvent.UpdateLearningPace       -> _uiState.update { it.copy(learningPace = event.pace) }
            is SettingsEvent.ToggleSrs                -> _uiState.update { it.copy(srsEnabled = event.enabled) }
            is SettingsEvent.UpdateMaxReviews         -> _uiState.update { it.copy(maxReviewsPerSession = event.count) }
            is SettingsEvent.ToggleReminder           -> _uiState.update { it.copy(reminderEnabled = event.enabled) }
            is SettingsEvent.UpdateReminderTime       -> _uiState.update { it.copy(reminderHour = event.hour, reminderMinute = event.minute) }
            // Голос
            is SettingsEvent.UpdateGermanSpeed        -> _uiState.update { it.copy(germanVoiceSpeed = event.speed) }
            is SettingsEvent.UpdateVoiceSpeed         -> _uiState.update { it.copy(voiceSpeed = event.speed) }
            is SettingsEvent.ToggleTranscription      -> _uiState.update { it.copy(showTranscription = event.enabled) }
            is SettingsEvent.UpdateStrictness         -> _uiState.update { it.copy(pronunciationStrictness = event.strictness) }
            is SettingsEvent.ToggleDataSaving         -> _uiState.update { it.copy(dataSavingMode = event.enabled) }
            // Gemini
            is SettingsEvent.UpdateGeminiTemperature        -> _uiState.update { it.copy(geminiTemperature = event.value) }
            is SettingsEvent.UpdateGeminiTopP               -> _uiState.update { it.copy(geminiTopP = event.value) }
            is SettingsEvent.UpdateGeminiTopK               -> _uiState.update { it.copy(geminiTopK = event.value) }
            is SettingsEvent.UpdateGeminiVoiceName          -> _uiState.update { it.copy(geminiVoiceName = event.name) }
            is SettingsEvent.UpdateGeminiMaxContext         -> _uiState.update { it.copy(geminiMaxContextTokens = event.tokens) }
            is SettingsEvent.ToggleGeminiInputTranscription -> _uiState.update { it.copy(geminiInputTranscription = event.enabled) }
            is SettingsEvent.ToggleGeminiOutputTranscription -> _uiState.update { it.copy(geminiOutputTranscription = event.enabled) }
            // Backup
            is SettingsEvent.CreateCloudBackup        -> createCloudBackup()
            is SettingsEvent.LoadCloudBackups         -> loadCloudBackups()
            is SettingsEvent.ShowRestoreDialog        -> { loadCloudBackups(); _uiState.update { it.copy(showRestoreDialog = true) } }
            is SettingsEvent.HideRestoreDialog        -> _uiState.update { it.copy(showRestoreDialog = false) }
            is SettingsEvent.RestoreFromCloud         -> restoreFromCloud(event.metadata)
            is SettingsEvent.SaveAll                  -> saveAll()
            is SettingsEvent.DismissMessages          -> _uiState.update { it.copy(successMessage = null, errorMessage = null) }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            runCatching {
                val userId = userRepository.getActiveUserId()
                val profile = userId?.let { userRepository.getUserProfile(it) }
                val prefs = profile?.preferences
                val voice = profile?.voiceSettings

                val duration = preferencesDataStore.getSessionDuration()
                    ?: prefs?.preferredSessionDuration
                    ?: 30
                val goal = preferencesDataStore.getDailyGoal()
                    ?: prefs?.dailyGoalWords
                    ?: 10

                val geminiConfig = preferencesDataStore.loadGeminiConfig()

                _uiState.update {
                    it.copy(
                        isLoading               = false,
                        // Профиль
                        userName                = profile?.name ?: "",
                        userAge                 = profile?.age?.toString() ?: "",
                        userHobbies             = profile?.hobbies ?: "",
                        userLevel               = profile?.cefrLevel?.name ?: "A1",
                        userNativeLanguage      = profile?.nativeLanguage ?: "ru",
                        userLearningGoals       = profile?.learningGoals ?: "",
                        // Обучение
                        sessionDurationMinutes  = duration,
                        dailyGoalWords          = goal,
                        learningPace            = prefs?.learningPace?.name ?: "NORMAL",
                        srsEnabled              = prefs?.srsEnabled ?: true,
                        maxReviewsPerSession    = prefs?.maxReviewsPerSession ?: 30,
                        reminderEnabled         = prefs?.reminderEnabled ?: false,
                        reminderHour            = prefs?.reminderHour ?: 19,
                        reminderMinute          = prefs?.reminderMinute ?: 0,
                        pronunciationStrictness = prefs?.pronunciationStrictness?.name ?: "MODERATE",
                        dataSavingMode          = prefs?.dataSavingMode ?: false,
                        voiceSpeed              = voice?.voiceSpeed ?: 1.0f,
                        germanVoiceSpeed        = voice?.germanVoiceSpeed ?: 0.8f,
                        showTranscription       = voice?.showTranscription ?: true,
                        // Gemini
                        geminiTemperature       = geminiConfig.temperature,
                        geminiTopP              = geminiConfig.topP,
                        geminiTopK              = geminiConfig.topK,
                        geminiVoiceName         = geminiConfig.voiceName,
                        geminiModelName         = geminiConfig.modelName,
                        geminiMaxContextTokens  = geminiConfig.maxContextTokens,
                        geminiInputTranscription  = geminiConfig.transcriptionConfig.inputTranscriptionEnabled,
                        geminiOutputTranscription = geminiConfig.transcriptionConfig.outputTranscriptionEnabled,
                    )
                }
            }.onFailure {
                _uiState.update { s -> s.copy(isLoading = false) }
            }
        }
    }

    private fun saveAll() {
        viewModelScope.launch {
            runCatching {
                val s = _uiState.value
                val userId = userRepository.getActiveUserId() ?: error("Пользователь не найден")
                val profile = userRepository.getUserProfile(userId) ?: return@launch

                // Обновляем профиль пользователя
                val updatedProfile = profile.copy(
                    name = s.userName.ifBlank { profile.name },
                    age = s.userAge.toIntOrNull(),
                    hobbies = s.userHobbies.ifBlank { null },
                    learningGoals = s.userLearningGoals.ifBlank { null },
                    cefrLevel = com.voicedeutsch.master.domain.model.user.CefrLevel.fromString(s.userLevel),
                    nativeLanguage = s.userNativeLanguage,
                    updatedAt = System.currentTimeMillis(),
                )

                val updatedPrefs = profile.preferences.copy(
                    preferredSessionDuration = s.sessionDurationMinutes,
                    dailyGoalWords = s.dailyGoalWords,
                    dailyGoalMinutes = s.sessionDurationMinutes,
                    learningPace = LearningPace.valueOf(s.learningPace),
                    srsEnabled = s.srsEnabled,
                    maxReviewsPerSession = s.maxReviewsPerSession,
                    reminderEnabled = s.reminderEnabled,
                    reminderHour = s.reminderHour,
                    reminderMinute = s.reminderMinute,
                    pronunciationStrictness = PronunciationStrictness.valueOf(s.pronunciationStrictness),
                    dataSavingMode = s.dataSavingMode,
                )

                val updatedVoice = profile.voiceSettings.copy(
                    voiceSpeed = s.voiceSpeed,
                    germanVoiceSpeed = s.germanVoiceSpeed,
                    showTranscription = s.showTranscription,
                )

                userRepository.updateUser(updatedProfile)
                configureUserPreferences.updatePreferences(userId, updatedPrefs)
                configureUserPreferences.updateVoiceSettings(userId, updatedVoice)

                preferencesDataStore.setSessionDuration(s.sessionDurationMinutes)
                preferencesDataStore.setDailyGoal(s.dailyGoalWords)

                // Сохраняем Gemini config в DataStore
                val geminiConfig = com.voicedeutsch.master.voicecore.engine.GeminiConfig(
                    temperature = s.geminiTemperature,
                    topP = s.geminiTopP,
                    topK = s.geminiTopK,
                    voiceName = s.geminiVoiceName,
                    modelName = s.geminiModelName,
                    maxContextTokens = s.geminiMaxContextTokens,
                    transcriptionConfig = com.voicedeutsch.master.voicecore.engine.GeminiConfig.TranscriptionConfig(
                        inputTranscriptionEnabled = s.geminiInputTranscription,
                        outputTranscriptionEnabled = s.geminiOutputTranscription,
                    ),
                )
                preferencesDataStore.saveGeminiConfig(geminiConfig)
            }
            .onSuccess { _uiState.update { it.copy(successMessage = "Все настройки сохранены") } }
            .onFailure { e -> _uiState.update { it.copy(errorMessage = "Ошибка: ${e.message}") } }
        }
    }

    private fun createCloudBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, errorMessage = null) }
            when (val result = backupManager.createCloudBackup()) {
                is BackupResult.Success -> _uiState.update {
                    it.copy(
                        isBackingUp    = false,
                        successMessage = "✅ Бекап создан (${result.sizeBytes / 1024} КБ)",
                    )
                }
                is BackupResult.Error -> _uiState.update {
                    it.copy(
                        isBackingUp  = false,
                        errorMessage = "❌ Ошибка бекапа: ${result.message}",
                    )
                }
            }
        }
    }

    private fun loadCloudBackups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBackups = true) }
            val backups = backupManager.listCloudBackups()
            _uiState.update { it.copy(isLoadingBackups = false, cloudBackups = backups) }
        }
    }

    private fun restoreFromCloud(metadata: BackupMetadata) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, showRestoreDialog = false, errorMessage = null) }
            val success = backupManager.restoreFromCloudBackup(metadata.storagePath)
            _uiState.update {
                if (success) {
                    it.copy(
                        isRestoring    = false,
                        successMessage = "✅ Восстановлено. Перезапустите приложение для применения.",
                    )
                } else {
                    it.copy(
                        isRestoring  = false,
                        errorMessage = "❌ Не удалось восстановить бекап",
                    )
                }
            }
        }
    }
}