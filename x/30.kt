// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/user/ConfigureUserPreferencesUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.user

import com.voicedeutsch.master.domain.model.user.UserPreferences
import com.voicedeutsch.master.domain.model.user.UserProfile
import com.voicedeutsch.master.domain.model.user.VoiceSettings
import com.voicedeutsch.master.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConfigureUserPreferencesUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var useCase: ConfigureUserPreferencesUseCase

    private val preferences = mockk<UserPreferences>(relaxed = true)
    private val voiceSettings = mockk<VoiceSettings>(relaxed = true)

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeProfile(
        prefs: UserPreferences = preferences,
        voice: VoiceSettings = voiceSettings
    ): UserProfile = mockk<UserProfile>(relaxed = true).also {
        every { it.preferences }   returns prefs
        every { it.voiceSettings } returns voice
    }

    private fun makePreferences(
        preferredSessionDuration: Int = 30,
        dailyGoalWords: Int = 10,
        dailyGoalMinutes: Int = 20
    ): UserPreferences = mockk<UserPreferences>(relaxed = true).also {
        every { it.preferredSessionDuration } returns preferredSessionDuration
        every { it.dailyGoalWords }           returns dailyGoalWords
        every { it.dailyGoalMinutes }         returns dailyGoalMinutes
    }

    private fun makeVoiceSettings(germanVoiceSpeed: Float = 1.0f): VoiceSettings =
        mockk<VoiceSettings>(relaxed = true).also {
            every { it.germanVoiceSpeed } returns germanVoiceSpeed
        }

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        useCase = ConfigureUserPreferencesUseCase(userRepository)

        coEvery { userRepository.updateUserPreferences(any(), any()) } returns Unit
        coEvery { userRepository.updateVoiceSettings(any(), any()) }   returns Unit
        coEvery { userRepository.getUserProfile(any()) }               returns makeProfile()
    }

    // ── updatePreferences ─────────────────────────────────────────────────────

    @Test
    fun updatePreferences_delegatesToRepository() = runTest {
        val newPrefs = mockk<UserPreferences>(relaxed = true)

        useCase.updatePreferences("user1", newPrefs)

        coVerify(exactly = 1) { userRepository.updateUserPreferences("user1", newPrefs) }
    }

    @Test
    fun updatePreferences_passesExactPreferencesObject() = runTest {
        val newPrefs = mockk<UserPreferences>(relaxed = true)
        var captured: UserPreferences? = null
        coEvery { userRepository.updateUserPreferences(any(), any()) } answers {
            captured = secondArg(); Unit
        }

        useCase.updatePreferences("user1", newPrefs)

        assertEquals(newPrefs, captured)
    }

    // ── updateVoiceSettings ───────────────────────────────────────────────────

    @Test
    fun updateVoiceSettings_delegatesToRepository() = runTest {
        val newSettings = mockk<VoiceSettings>(relaxed = true)

        useCase.updateVoiceSettings("user1", newSettings)

        coVerify(exactly = 1) { userRepository.updateVoiceSettings("user1", newSettings) }
    }

    @Test
    fun updateVoiceSettings_passesExactSettingsObject() = runTest {
        val newSettings = mockk<VoiceSettings>(relaxed = true)
        var captured: VoiceSettings? = null
        coEvery { userRepository.updateVoiceSettings(any(), any()) } answers {
            captured = secondArg(); Unit
        }

        useCase.updateVoiceSettings("user1", newSettings)

        assertEquals(newSettings, captured)
    }

    // ── updatePreferredSessionDuration ────────────────────────────────────────

    @Test
    fun updatePreferredSessionDuration_userNotFound_returnsEarly() = runTest {
        coEvery { userRepository.getUserProfile("missing") } returns null

        useCase.updatePreferredSessionDuration("missing", 30)

        coVerify(exactly = 0) { userRepository.updateUserPreferences(any(), any()) }
    }

    @Test
    fun updatePreferredSessionDuration_validValue_updatesPreferences() = runTest {
        val prefs = makePreferences()
        val updatedPrefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.copy(preferredSessionDuration = 45) } returns updatedPrefs
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(prefs = prefs)

        useCase.updatePreferredSessionDuration("user1", 45)

        coVerify(exactly = 1) { userRepository.updateUserPreferences("user1", updatedPrefs) }
    }

    @Test
    fun updatePreferredSessionDuration_belowMin_clampedTo5() = runTest {
        val prefs = makePreferences()
        val clampedPrefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.copy(preferredSessionDuration = 5) } returns clampedPrefs
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(prefs = prefs)

        useCase.updatePreferredSessionDuration("user1", 1)

        coVerify { userRepository.updateUserPreferences("user1", clampedPrefs) }
    }

    @Test
    fun updatePreferredSessionDuration_aboveMax_clampedTo120() = runTest {
        val prefs = makePreferences()
        val clampedPrefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.copy(preferredSessionDuration = 120) } returns clampedPrefs
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(prefs = prefs)

        useCase.updatePreferredSessionDuration("user1", 999)

        coVerify { userRepository.updateUserPreferences("user1", clampedPrefs) }
    }

    @Test
    fun updatePreferredSessionDuration_atMinBoundary_5NotClamped() = runTest {
        val prefs = makePreferences()
        val updatedPrefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.copy(preferredSessionDuration = 5) } returns updatedPrefs
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(prefs = prefs)

        useCase.updatePreferredSessionDuration("user1", 5)

        coVerify { userRepository.updateUserPreferences("user1", updatedPrefs) }
    }

    @Test
    fun updatePreferredSessionDuration_atMaxBoundary_120NotClamped() = runTest {
        val prefs = makePreferences()
        val updatedPrefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.copy(preferredSessionDuration = 120) } returns updatedPrefs
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(prefs = prefs)

        useCase.updatePreferredSessionDuration("user1", 120)

        coVerify { userRepository.updateUserPreferences("user1", updatedPrefs) }
    }

    // ── updateDailyGoal ───────────────────────────────────────────────────────

    @Test
    fun updateDailyGoal_userNotFound_returnsEarly() = runTest {
        coEvery { userRepository.getUserProfile("missing") } returns null

        useCase.updateDailyGoal("missing", 10, 20)

        coVerify(exactly = 0) { userRepository.updateUserPreferences(any(), any()) }
    }

    @Test
    fun updateDailyGoal_validValues_updatesPreferences() = runTest {
        val prefs = makePreferences()
        val updatedPrefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.copy(dailyGoalWords = 15, dailyGoalMinutes = 30) } returns updatedPrefs
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(prefs = prefs)

        useCase.updateDailyGoal("user1", 15, 30)

        coVerify { userRepository.updateUserPreferences("user1", updatedPrefs) }
    }

    @Test
    fun updateDailyGoal_wordsBelowMin_clampedTo1() = runTest {
        val prefs = makePreferences()
        val updatedPrefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.copy(dailyGoalWords = 1, dailyGoalMinutes = 20) } returns updatedPrefs
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(prefs = prefs)

        useCase.updateDailyGoal("user1", 0, 20)

        coVerify { userRepository.updateUserPreferences("user1", updatedPrefs) }
    }

    @Test
    fun updateDailyGoal_wordsAboveMax_clampedTo100() = runTest {
        val prefs = makePreferences()
        val updatedPrefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.copy(dailyGoalWords = 100, dailyGoalMinutes = 20) } returns updatedPrefs
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(prefs = prefs)

        useCase.updateDailyGoal("user1", 500, 20)

        coVerify { userRepository.updateUserPreferences("user1", updatedPrefs) }
    }

    @Test
    fun updateDailyGoal_minutesBelowMin_clampedTo5() = runTest {
        val prefs = makePreferences()
        val updatedPrefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.copy(dailyGoalWords = 10, dailyGoalMinutes = 5) } returns updatedPrefs
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(prefs = prefs)

        useCase.updateDailyGoal("user1", 10, 1)

        coVerify { userRepository.updateUserPreferences("user1", updatedPrefs) }
    }

    @Test
    fun updateDailyGoal_minutesAboveMax_clampedTo180() = runTest {
        val prefs = makePreferences()
        val updatedPrefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.copy(dailyGoalWords = 10, dailyGoalMinutes = 180) } returns updatedPrefs
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(prefs = prefs)

        useCase.updateDailyGoal("user1", 10, 999)

        coVerify { userRepository.updateUserPreferences("user1", updatedPrefs) }
    }

    @Test
    fun updateDailyGoal_wordsAtMinBoundary_1NotClamped() = runTest {
        val prefs = makePreferences()
        val updatedPrefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.copy(dailyGoalWords = 1, dailyGoalMinutes = 10) } returns updatedPrefs
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(prefs = prefs)

        useCase.updateDailyGoal("user1", 1, 10)

        coVerify { userRepository.updateUserPreferences("user1", updatedPrefs) }
    }

    @Test
    fun updateDailyGoal_minutesAtMaxBoundary_180NotClamped() = runTest {
        val prefs = makePreferences()
        val updatedPrefs = mockk<UserPreferences>(relaxed = true)
        every { prefs.copy(dailyGoalWords = 10, dailyGoalMinutes = 180) } returns updatedPrefs
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(prefs = prefs)

        useCase.updateDailyGoal("user1", 10, 180)

        coVerify { userRepository.updateUserPreferences("user1", updatedPrefs) }
    }

    // ── updateGermanVoiceSpeed ────────────────────────────────────────────────

    @Test
    fun updateGermanVoiceSpeed_userNotFound_returnsEarly() = runTest {
        coEvery { userRepository.getUserProfile("missing") } returns null

        useCase.updateGermanVoiceSpeed("missing", 1.0f)

        coVerify(exactly = 0) { userRepository.updateVoiceSettings(any(), any()) }
    }

    @Test
    fun updateGermanVoiceSpeed_validValue_updatesVoiceSettings() = runTest {
        val voice = makeVoiceSettings()
        val updatedVoice = mockk<VoiceSettings>(relaxed = true)
        every { voice.copy(germanVoiceSpeed = 1.5f) } returns updatedVoice
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(voice = voice)

        useCase.updateGermanVoiceSpeed("user1", 1.5f)

        coVerify { userRepository.updateVoiceSettings("user1", updatedVoice) }
    }

    @Test
    fun updateGermanVoiceSpeed_belowMin_clampedTo03() = runTest {
        val voice = makeVoiceSettings()
        val updatedVoice = mockk<VoiceSettings>(relaxed = true)
        every { voice.copy(germanVoiceSpeed = 0.3f) } returns updatedVoice
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(voice = voice)

        useCase.updateGermanVoiceSpeed("user1", 0.1f)

        coVerify { userRepository.updateVoiceSettings("user1", updatedVoice) }
    }

    @Test
    fun updateGermanVoiceSpeed_aboveMax_clampedTo20() = runTest {
        val voice = makeVoiceSettings()
        val updatedVoice = mockk<VoiceSettings>(relaxed = true)
        every { voice.copy(germanVoiceSpeed = 2.0f) } returns updatedVoice
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(voice = voice)

        useCase.updateGermanVoiceSpeed("user1", 5.0f)

        coVerify { userRepository.updateVoiceSettings("user1", updatedVoice) }
    }

    @Test
    fun updateGermanVoiceSpeed_atMinBoundary_03NotClamped() = runTest {
        val voice = makeVoiceSettings()
        val updatedVoice = mockk<VoiceSettings>(relaxed = true)
        every { voice.copy(germanVoiceSpeed = 0.3f) } returns updatedVoice
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(voice = voice)

        useCase.updateGermanVoiceSpeed("user1", 0.3f)

        coVerify { userRepository.updateVoiceSettings("user1", updatedVoice) }
    }

    @Test
    fun updateGermanVoiceSpeed_atMaxBoundary_20NotClamped() = runTest {
        val voice = makeVoiceSettings()
        val updatedVoice = mockk<VoiceSettings>(relaxed = true)
        every { voice.copy(germanVoiceSpeed = 2.0f) } returns updatedVoice
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(voice = voice)

        useCase.updateGermanVoiceSpeed("user1", 2.0f)

        coVerify { userRepository.updateVoiceSettings("user1", updatedVoice) }
    }

    @Test
    fun updateGermanVoiceSpeed_exactlyOne_passedThrough() = runTest {
        val voice = makeVoiceSettings()
        val updatedVoice = mockk<VoiceSettings>(relaxed = true)
        every { voice.copy(germanVoiceSpeed = 1.0f) } returns updatedVoice
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(voice = voice)

        useCase.updateGermanVoiceSpeed("user1", 1.0f)

        coVerify { userRepository.updateVoiceSettings("user1", updatedVoice) }
    }
}
