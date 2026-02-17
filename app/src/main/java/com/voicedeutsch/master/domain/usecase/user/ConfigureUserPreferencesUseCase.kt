package com.voicedeutsch.master.domain.usecase.user

import com.voicedeutsch.master.domain.model.user.UserPreferences
import com.voicedeutsch.master.domain.model.user.VoiceSettings
import com.voicedeutsch.master.domain.repository.UserRepository

/**
 * Updates user preferences and voice settings.
 * Simple wrapper for repository calls, ensuring uniform API access.
 */
class ConfigureUserPreferencesUseCase(
    private val userRepository: UserRepository
) {

    suspend fun updatePreferences(userId: String, preferences: UserPreferences) {
        userRepository.updateUserPreferences(userId, preferences)
    }

    suspend fun updateVoiceSettings(userId: String, settings: VoiceSettings) {
        userRepository.updateVoiceSettings(userId, settings)
    }

    suspend fun updatePreferredSessionDuration(userId: String, durationMinutes: Int) {
        val profile = userRepository.getUserProfile(userId) ?: return
        val updatedPreferences = profile.preferences.copy(
            preferredSessionDuration = durationMinutes.coerceIn(5, 120)
        )
        userRepository.updateUserPreferences(userId, updatedPreferences)
    }

    suspend fun updateDailyGoal(userId: String, words: Int, minutes: Int) {
        val profile = userRepository.getUserProfile(userId) ?: return
        val updatedPreferences = profile.preferences.copy(
            dailyGoalWords = words.coerceIn(1, 100),
            dailyGoalMinutes = minutes.coerceIn(5, 180)
        )
        userRepository.updateUserPreferences(userId, updatedPreferences)
    }

    suspend fun updateGermanVoiceSpeed(userId: String, speed: Float) {
        val profile = userRepository.getUserProfile(userId) ?: return
        val updatedSettings = profile.voiceSettings.copy(
            germanVoiceSpeed = speed.coerceIn(0.3f, 2.0f)
        )
        userRepository.updateVoiceSettings(userId, updatedSettings)
    }
}