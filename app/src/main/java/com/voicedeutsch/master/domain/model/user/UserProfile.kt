package com.voicedeutsch.master.domain.model.user

import kotlinx.serialization.Serializable

/**
 * Domain model of the user profile.
 *
 * Contains the user's name, current CEFR level, cumulative statistics,
 * learning preferences, and voice interaction settings.
 *
 * This is a pure data class with no framework dependencies.
 *
 * **Mappings:**
 * - Maps from/to `UserEntity` (Data layer)
 * - Used by `UserRepository`, `GetUserProfileUseCase`
 * - Passed to `ContextBuilder` for Gemini context formation
 * - Displayed on `DashboardScreen`
 */
@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val nativeLanguage: String = "ru",
    val targetLanguage: String = "de",
    val cefrLevel: CefrLevel = CefrLevel.A1,
    val cefrSubLevel: Int = 1, // 1-10
    val totalSessions: Int = 0,
    val totalMinutes: Int = 0,
    val totalWordsLearned: Int = 0,
    val totalRulesLearned: Int = 0,
    val streakDays: Int = 0,
    val lastSessionDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val preferences: UserPreferences = UserPreferences(),
    val voiceSettings: VoiceSettings = VoiceSettings()
) {
    /**
     * Human-readable CEFR display string, e.g. "A1.3".
     */
    val cefrDisplay: String get() = "${cefrLevel.name}.${cefrSubLevel}"

    /**
     * Returns `true` when the user has never completed a session.
     */
    val isNewUser: Boolean get() = totalSessions == 0

    /**
     * Total learning time expressed in fractional hours.
     */
    val totalHours: Float get() = totalMinutes / 60f
}