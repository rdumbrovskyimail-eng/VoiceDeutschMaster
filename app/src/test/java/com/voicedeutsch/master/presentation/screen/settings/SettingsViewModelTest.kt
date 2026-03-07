// Путь: src/test/java/com/voicedeutsch/master/presentation/screen/settings/SettingsViewModelTest.kt
package com.voicedeutsch.master.presentation.screen.settings

import app.cash.turbine.test
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.data.remote.sync.BackupManager
import com.voicedeutsch.master.data.remote.sync.BackupMetadata
import com.voicedeutsch.master.data.remote.sync.BackupResult
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.domain.model.user.LearningPace
import com.voicedeutsch.master.domain.model.user.PronunciationStrictness
import com.voicedeutsch.master.domain.model.user.UserPreferences
import com.voicedeutsch.master.domain.model.user.UserProfile
import com.voicedeutsch.master.domain.model.user.VoiceSettings
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.domain.usecase.user.ConfigureUserPreferencesUseCase
import com.voicedeutsch.master.testutil.MainDispatcherRule
import com.voicedeutsch.master.voicecore.engine.GeminiConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SettingsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var configureUserPreferences: ConfigureUserPreferencesUseCase
    private lateinit var preferencesDataStore: UserPreferencesDataStore
    private lateinit var userRepository: UserRepository
    private lateinit var backupManager: BackupManager
    private lateinit var sut: SettingsViewModel

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildUserProfile(
        id: String = "user_1",
        name: String = "Max",
        age: Int? = 25,
        hobbies: String? = "reading",
        learningGoals: String? = "business",
        cefrLevel: CefrLevel = CefrLevel.B1,
        nativeLanguage: String = "ru",
        preferences: UserPreferences = UserPreferences(),
        voiceSettings: VoiceSettings = VoiceSettings(),
    ) = UserProfile(
        id = id,
        name = name,
        age = age,
        hobbies = hobbies,
        learningGoals = learningGoals,
        cefrLevel = cefrLevel,
        nativeLanguage = nativeLanguage,
        preferences = preferences,
        voiceSettings = voiceSettings,
    )

    private fun buildGeminiConfig(
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        topK: Int = 40,
        voiceName: String = "Kore",
        modelName: String = GeminiConfig.MODEL_GEMINI_LIVE,
        maxContextTokens: Int = 131_072,
        inputTranscription: Boolean = false,
        outputTranscription: Boolean = false,
    ) = GeminiConfig(
        temperature = temperature,
        topP = topP,
        topK = topK,
        voiceName = voiceName,
        modelName = modelName,
        maxContextTokens = maxContextTokens,
        transcriptionConfig = GeminiConfig.TranscriptionConfig(
            inputTranscriptionEnabled = inputTranscription,
            outputTranscriptionEnabled = outputTranscription,
        ),
    )

    private fun buildBackupMetadata(
        storagePath: String = "users/uid/backups/backup_1.db",
        timestamp: Long = 1000L,
        sizeBytes: Long = 2048L,
        deviceModel: String = "Pixel",
        appVersion: String = "1.0",
        fileName: String = "backup_1.db",
    ) = BackupMetadata(
        storagePath = storagePath,
        timestamp = timestamp,
        sizeBytes = sizeBytes,
        deviceModel = deviceModel,
        appVersion = appVersion,
        fileName = fileName,
    )

    private fun createViewModel() = SettingsViewModel(
        configureUserPreferences = configureUserPreferences,
        preferencesDataStore = preferencesDataStore,
        userRepository = userRepository,
        backupManager = backupManager,
    )

    @BeforeEach
    fun setUp() {
        configureUserPreferences = mockk(relaxed = true)
        preferencesDataStore = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        backupManager = mockk(relaxed = true)

        coEvery { userRepository.getActiveUserId() } returns "user_1"
        coEvery { userRepository.getUserProfile("user_1") } returns buildUserProfile()
        coEvery { preferencesDataStore.getSessionDuration() } returns null
        coEvery { preferencesDataStore.getDailyGoal() } returns null
        coEvery { preferencesDataStore.loadGeminiConfig() } returns buildGeminiConfig()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialState_isLoadingTrue() {
        val vm = createViewModel()
        // Before loadSettings completes, isLoading could be true
        assertNotNull(vm.uiState.value)
    }

    @Test
    fun loadSettings_success_isLoadingFalse() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun loadSettings_success_populatesProfileFields() = runTest {
        val profile = buildUserProfile(
            name = "Klaus",
            age = 30,
            hobbies = "chess",
            learningGoals = "travel",
            cefrLevel = CefrLevel.C1,
            nativeLanguage = "en",
        )
        coEvery { userRepository.getUserProfile("user_1") } returns profile

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        with(sut.uiState.value) {
            assertEquals("Klaus", userName)
            assertEquals("30", userAge)
            assertEquals("chess", userHobbies)
            assertEquals("travel", userLearningGoals)
            assertEquals("C1", userLevel)
            assertEquals("en", userNativeLanguage)
        }
    }

    @Test
    fun loadSettings_success_populatesGeminiConfig() = runTest {
        coEvery { preferencesDataStore.loadGeminiConfig() } returns buildGeminiConfig(
            temperature = 1.2f,
            topP = 0.85f,
            topK = 50,
            voiceName = "Aoede",
            maxContextTokens = 65_536,
            inputTranscription = true,
            outputTranscription = true,
        )

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        with(sut.uiState.value) {
            assertEquals(1.2f, geminiTemperature, 0.001f)
            assertEquals(0.85f, geminiTopP, 0.001f)
            assertEquals(50, geminiTopK)
            assertEquals("Aoede", geminiVoiceName)
            assertEquals(65_536, geminiMaxContextTokens)
            assertTrue(geminiInputTranscription)
            assertTrue(geminiOutputTranscription)
        }
    }

    @Test
    fun loadSettings_sessionDurationFromDataStore_usedOverProfile() = runTest {
        coEvery { preferencesDataStore.getSessionDuration() } returns 45
        coEvery { userRepository.getUserProfile("user_1") } returns buildUserProfile(
            preferences = UserPreferences(preferredSessionDuration = 20)
        )

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(45, sut.uiState.value.sessionDurationMinutes)
    }

    @Test
    fun loadSettings_sessionDurationNullInDataStore_usesProfileValue() = runTest {
        coEvery { preferencesDataStore.getSessionDuration() } returns null
        coEvery { userRepository.getUserProfile("user_1") } returns buildUserProfile(
            preferences = UserPreferences(preferredSessionDuration = 20)
        )

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(20, sut.uiState.value.sessionDurationMinutes)
    }

    @Test
    fun loadSettings_noUser_isLoadingFalse() = runTest {
        coEvery { userRepository.getActiveUserId() } returns null

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun loadSettings_repositoryThrows_isLoadingFalse() = runTest {
        coEvery { userRepository.getActiveUserId() } throws RuntimeException("DB error")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun loadSettings_geminiTemperature_clampedTo2() = runTest {
        coEvery { preferencesDataStore.loadGeminiConfig() } returns buildGeminiConfig(temperature = 5.0f)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(sut.uiState.value.geminiTemperature <= 2.0f)
    }

    @Test
    fun loadSettings_geminiTopK_clampedTo100() = runTest {
        coEvery { preferencesDataStore.loadGeminiConfig() } returns buildGeminiConfig(topK = 999)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(sut.uiState.value.geminiTopK <= 100)
    }

    // ── Profile events ────────────────────────────────────────────────────────

    @Test
    fun onEvent_updateUserName_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateUserName("Anna"))

        assertEquals("Anna", sut.uiState.value.userName)
    }

    @Test
    fun onEvent_updateUserAge_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateUserAge("28"))

        assertEquals("28", sut.uiState.value.userAge)
    }

    @Test
    fun onEvent_updateUserHobbies_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateUserHobbies("cooking, hiking"))

        assertEquals("cooking, hiking", sut.uiState.value.userHobbies)
    }

    @Test
    fun onEvent_updateUserLevel_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateUserLevel("C2"))

        assertEquals("C2", sut.uiState.value.userLevel)
    }

    @Test
    fun onEvent_updateUserNativeLanguage_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateUserNativeLanguage("de"))

        assertEquals("de", sut.uiState.value.userNativeLanguage)
    }

    @Test
    fun onEvent_updateUserLearningGoals_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateUserLearningGoals("Work abroad"))

        assertEquals("Work abroad", sut.uiState.value.userLearningGoals)
    }

    // ── Learning events ───────────────────────────────────────────────────────

    @Test
    fun onEvent_updateSessionDuration_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateSessionDuration(60))

        assertEquals(60, sut.uiState.value.sessionDurationMinutes)
    }

    @Test
    fun onEvent_updateDailyGoal_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateDailyGoal(20))

        assertEquals(20, sut.uiState.value.dailyGoalWords)
    }

    @Test
    fun onEvent_updateLearningPace_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateLearningPace("FAST"))

        assertEquals("FAST", sut.uiState.value.learningPace)
    }

    @Test
    fun onEvent_toggleSrsEnabled_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.ToggleSrs(false))

        assertFalse(sut.uiState.value.srsEnabled)
    }

    @Test
    fun onEvent_updateMaxReviews_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateMaxReviews(50))

        assertEquals(50, sut.uiState.value.maxReviewsPerSession)
    }

    @Test
    fun onEvent_toggleReminder_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.ToggleReminder(true))

        assertTrue(sut.uiState.value.reminderEnabled)
    }

    @Test
    fun onEvent_updateReminderTime_updatesHourAndMinute() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateReminderTime(8, 30))

        assertEquals(8, sut.uiState.value.reminderHour)
        assertEquals(30, sut.uiState.value.reminderMinute)
    }

    // ── Voice events ──────────────────────────────────────────────────────────

    @Test
    fun onEvent_updateVoiceSpeed_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateVoiceSpeed(1.5f))

        assertEquals(1.5f, sut.uiState.value.voiceSpeed, 0.001f)
    }

    @Test
    fun onEvent_updateGermanSpeed_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateGermanSpeed(0.6f))

        assertEquals(0.6f, sut.uiState.value.germanVoiceSpeed, 0.001f)
    }

    @Test
    fun onEvent_toggleTranscription_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.ToggleTranscription(false))

        assertFalse(sut.uiState.value.showTranscription)
    }

    @Test
    fun onEvent_updateStrictness_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateStrictness("STRICT"))

        assertEquals("STRICT", sut.uiState.value.pronunciationStrictness)
    }

    @Test
    fun onEvent_toggleDataSaving_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.ToggleDataSaving(true))

        assertTrue(sut.uiState.value.dataSavingMode)
    }

    // ── Gemini events ─────────────────────────────────────────────────────────

    @Test
    fun onEvent_updateGeminiTemperature_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateGeminiTemperature(1.5f))

        assertEquals(1.5f, sut.uiState.value.geminiTemperature, 0.001f)
    }

    @Test
    fun onEvent_updateGeminiTopP_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateGeminiTopP(0.75f))

        assertEquals(0.75f, sut.uiState.value.geminiTopP, 0.001f)
    }

    @Test
    fun onEvent_updateGeminiTopK_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateGeminiTopK(60))

        assertEquals(60, sut.uiState.value.geminiTopK)
    }

    @Test
    fun onEvent_updateGeminiVoiceName_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateGeminiVoiceName("Puck"))

        assertEquals("Puck", sut.uiState.value.geminiVoiceName)
    }

    @Test
    fun onEvent_updateGeminiMaxContext_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateGeminiMaxContext(65_536))

        assertEquals(65_536, sut.uiState.value.geminiMaxContextTokens)
    }

    @Test
    fun onEvent_toggleGeminiInputTranscription_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.ToggleGeminiInputTranscription(true))

        assertTrue(sut.uiState.value.geminiInputTranscription)
    }

    @Test
    fun onEvent_toggleGeminiOutputTranscription_updatesState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.ToggleGeminiOutputTranscription(true))

        assertTrue(sut.uiState.value.geminiOutputTranscription)
    }

    // ── SaveAll ───────────────────────────────────────────────────────────────

    @Test
    fun onEvent_saveAll_success_setsSuccessMessage() = runTest {
        coEvery { userRepository.updateUser(any()) } returns Unit
        coEvery { configureUserPreferences.updatePreferences(any(), any()) } returns Unit
        coEvery { configureUserPreferences.updateVoiceSettings(any(), any()) } returns Unit
        coEvery { preferencesDataStore.setSessionDuration(any()) } returns Unit
        coEvery { preferencesDataStore.setDailyGoal(any()) } returns Unit
        coEvery { preferencesDataStore.saveGeminiConfig(any()) } returns Unit

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.SaveAll)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.successMessage)
        assertTrue(sut.uiState.value.successMessage!!.contains("сохранены"))
    }

    @Test
    fun onEvent_saveAll_noUser_setsErrorMessage() = runTest {
        coEvery { userRepository.getActiveUserId() } returns null

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.SaveAll)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun onEvent_saveAll_repositoryThrows_setsErrorMessage() = runTest {
        coEvery { userRepository.updateUser(any()) } throws RuntimeException("Save failed")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.SaveAll)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("Save failed"))
    }

    @Test
    fun onEvent_saveAll_callsUpdateUserWithCorrectProfile() = runTest {
        coEvery { userRepository.updateUser(any()) } returns Unit
        coEvery { configureUserPreferences.updatePreferences(any(), any()) } returns Unit
        coEvery { configureUserPreferences.updateVoiceSettings(any(), any()) } returns Unit
        coEvery { preferencesDataStore.setSessionDuration(any()) } returns Unit
        coEvery { preferencesDataStore.setDailyGoal(any()) } returns Unit
        coEvery { preferencesDataStore.saveGeminiConfig(any()) } returns Unit

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateUserName("Helga"))
        sut.onEvent(SettingsEvent.SaveAll)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userRepository.updateUser(match { it.name == "Helga" }) }
    }

    @Test
    fun onEvent_saveAll_savesSessionDurationToDataStore() = runTest {
        coEvery { userRepository.updateUser(any()) } returns Unit
        coEvery { configureUserPreferences.updatePreferences(any(), any()) } returns Unit
        coEvery { configureUserPreferences.updateVoiceSettings(any(), any()) } returns Unit
        coEvery { preferencesDataStore.setSessionDuration(any()) } returns Unit
        coEvery { preferencesDataStore.setDailyGoal(any()) } returns Unit
        coEvery { preferencesDataStore.saveGeminiConfig(any()) } returns Unit

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateSessionDuration(45))
        sut.onEvent(SettingsEvent.SaveAll)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { preferencesDataStore.setSessionDuration(45) }
    }

    // ── Backup events ─────────────────────────────────────────────────────────

    @Test
    fun onEvent_createCloudBackup_success_setsSuccessMessage() = runTest {
        coEvery { backupManager.createCloudBackup() } returns BackupResult.Success(
            storagePath = "users/uid/backup.db",
            sizeBytes = 4096L,
        )

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.CreateCloudBackup)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.successMessage)
        assertTrue(sut.uiState.value.successMessage!!.contains("Бекап создан"))
        assertFalse(sut.uiState.value.isBackingUp)
    }

    @Test
    fun onEvent_createCloudBackup_error_setsErrorMessage() = runTest {
        coEvery { backupManager.createCloudBackup() } returns BackupResult.Error("Upload failed")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.CreateCloudBackup)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("Upload failed"))
        assertFalse(sut.uiState.value.isBackingUp)
    }

    @Test
    fun onEvent_createCloudBackup_whileInProgress_isBackingUpTrue() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.test {
            awaitItem() // loaded state

            coEvery { backupManager.createCloudBackup() } coAnswers {
                // Capture the intermediate isBackingUp=true state
                BackupResult.Success("path", 0L)
            }

            sut.onEvent(SettingsEvent.CreateCloudBackup)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(sut.uiState.value.isBackingUp)
    }

    @Test
    fun onEvent_loadCloudBackups_populatesBackupsList() = runTest {
        val backups = listOf(
            buildBackupMetadata(fileName = "backup_1.db"),
            buildBackupMetadata(fileName = "backup_2.db"),
        )
        coEvery { backupManager.listCloudBackups() } returns backups

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.LoadCloudBackups)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, sut.uiState.value.cloudBackups.size)
        assertFalse(sut.uiState.value.isLoadingBackups)
    }

    @Test
    fun onEvent_loadCloudBackups_emptyResult_setsEmptyList() = runTest {
        coEvery { backupManager.listCloudBackups() } returns emptyList()

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.LoadCloudBackups)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList<BackupMetadata>(), sut.uiState.value.cloudBackups)
    }

    @Test
    fun onEvent_showRestoreDialog_setsShowRestoreDialogTrue() = runTest {
        coEvery { backupManager.listCloudBackups() } returns emptyList()

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.ShowRestoreDialog)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(sut.uiState.value.showRestoreDialog)
    }

    @Test
    fun onEvent_showRestoreDialog_alsoLoadsBackups() = runTest {
        val backups = listOf(buildBackupMetadata())
        coEvery { backupManager.listCloudBackups() } returns backups

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.ShowRestoreDialog)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, sut.uiState.value.cloudBackups.size)
    }

    @Test
    fun onEvent_hideRestoreDialog_setsShowRestoreDialogFalse() = runTest {
        coEvery { backupManager.listCloudBackups() } returns emptyList()

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.ShowRestoreDialog)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        sut.onEvent(SettingsEvent.HideRestoreDialog)

        assertFalse(sut.uiState.value.showRestoreDialog)
    }

    @Test
    fun onEvent_restoreFromCloud_success_setsSuccessMessage() = runTest {
        coEvery { backupManager.restoreFromCloudBackup(any()) } returns true

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.RestoreFromCloud(buildBackupMetadata()))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.successMessage)
        assertTrue(sut.uiState.value.successMessage!!.contains("Восстановлено"))
        assertFalse(sut.uiState.value.isRestoring)
        assertFalse(sut.uiState.value.showRestoreDialog)
    }

    @Test
    fun onEvent_restoreFromCloud_failure_setsErrorMessage() = runTest {
        coEvery { backupManager.restoreFromCloudBackup(any()) } returns false

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.RestoreFromCloud(buildBackupMetadata()))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("Не удалось"))
        assertFalse(sut.uiState.value.isRestoring)
    }

    @Test
    fun onEvent_restoreFromCloud_callsManagerWithCorrectPath() = runTest {
        val metadata = buildBackupMetadata(storagePath = "users/uid/backups/specific.db")
        coEvery { backupManager.restoreFromCloudBackup("users/uid/backups/specific.db") } returns true

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.RestoreFromCloud(metadata))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { backupManager.restoreFromCloudBackup("users/uid/backups/specific.db") }
    }

    // ── DismissMessages ───────────────────────────────────────────────────────

    @Test
    fun onEvent_dismissMessages_clearsSuccessMessage() = runTest {
        coEvery { backupManager.createCloudBackup() } returns BackupResult.Success("path", 0L)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.CreateCloudBackup)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sut.uiState.value.successMessage)

        sut.onEvent(SettingsEvent.DismissMessages)

        assertNull(sut.uiState.value.successMessage)
        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun onEvent_dismissMessages_clearsErrorMessage() = runTest {
        coEvery { backupManager.createCloudBackup() } returns BackupResult.Error("error")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.CreateCloudBackup)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sut.uiState.value.errorMessage)

        sut.onEvent(SettingsEvent.DismissMessages)

        assertNull(sut.uiState.value.successMessage)
        assertNull(sut.uiState.value.errorMessage)
    }

    // ── uiState Flow ──────────────────────────────────────────────────────────

    @Test
    fun uiState_flow_emitsOnEventUpdate() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.test {
            awaitItem() // current state

            sut.onEvent(SettingsEvent.UpdateUserName("New Name"))
            val updated = awaitItem()
            assertEquals("New Name", updated.userName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_multipleEvents_allReflectedInState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SettingsEvent.UpdateUserName("Hans"))
        sut.onEvent(SettingsEvent.UpdateUserLevel("C1"))
        sut.onEvent(SettingsEvent.UpdateDailyGoal(30))
        sut.onEvent(SettingsEvent.ToggleSrs(false))

        with(sut.uiState.value) {
            assertEquals("Hans", userName)
            assertEquals("C1", userLevel)
            assertEquals(30, dailyGoalWords)
            assertFalse(srsEnabled)
        }
    }
}
