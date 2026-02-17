package com.voicedeutsch.master.domain.repository

import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.domain.model.user.UserPreferences
import com.voicedeutsch.master.domain.model.user.UserProfile
import com.voicedeutsch.master.domain.model.user.UserStatistics
import com.voicedeutsch.master.domain.model.user.VoiceSettings
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    fun getUserProfileFlow(userId: String): Flow<UserProfile?>

    suspend fun getUserProfile(userId: String): UserProfile?

    suspend fun createUser(profile: UserProfile): String

    suspend fun updateUser(profile: UserProfile)

    suspend fun updateUserLevel(userId: String, cefrLevel: CefrLevel, subLevel: Int)

    suspend fun updateUserPreferences(userId: String, preferences: UserPreferences)

    suspend fun updateVoiceSettings(userId: String, settings: VoiceSettings)

    suspend fun incrementSessionStats(
        userId: String,
        durationMinutes: Int,
        wordsLearned: Int,
        rulesLearned: Int
    )

    suspend fun updateStreak(userId: String, streakDays: Int)

    suspend fun getUserStatistics(userId: String): UserStatistics

    suspend fun getActiveUserId(): String?

    suspend fun setActiveUserId(userId: String)

    suspend fun userExists(): Boolean
}