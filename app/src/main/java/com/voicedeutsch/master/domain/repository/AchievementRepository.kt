package com.voicedeutsch.master.domain.repository

import com.voicedeutsch.master.domain.model.achievement.Achievement
import com.voicedeutsch.master.domain.model.achievement.UserAchievement
import kotlinx.coroutines.flow.Flow

interface AchievementRepository {

    suspend fun getAllAchievements(): List<Achievement>

    suspend fun getUserAchievements(userId: String): List<UserAchievement>

    fun observeUserAchievements(userId: String): Flow<List<UserAchievement>>

    suspend fun hasAchievement(userId: String, achievementId: String): Boolean

    suspend fun grantAchievement(userId: String, achievementId: String): UserAchievement?

    suspend fun getUnannouncedAchievements(userId: String): List<UserAchievement>

    suspend fun markAnnounced(userId: String, achievementId: String)

    suspend fun seedDefaultAchievements()
}