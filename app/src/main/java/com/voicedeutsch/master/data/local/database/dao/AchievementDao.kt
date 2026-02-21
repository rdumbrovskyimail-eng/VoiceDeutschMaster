package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.voicedeutsch.master.data.local.database.entity.AchievementEntity
import com.voicedeutsch.master.data.local.database.entity.UserAchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    // ── Achievements catalog ──────────────────────────────────────────────────

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievements(): List<AchievementEntity>

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievement(id: String): AchievementEntity?

    @Query("SELECT * FROM achievements WHERE category = :category")
    suspend fun getAchievementsByCategory(category: String): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    // ── User achievements ─────────────────────────────────────────────────────

    @Query("SELECT * FROM user_achievements WHERE user_id = :userId")
    suspend fun getUserAchievements(userId: String): List<UserAchievementEntity>

    @Query("SELECT * FROM user_achievements WHERE user_id = :userId")
    fun observeUserAchievements(userId: String): Flow<List<UserAchievementEntity>>

    @Query(
        """SELECT * FROM user_achievements 
           WHERE user_id = :userId AND announced = 0"""
    )
    suspend fun getUnannounced(userId: String): List<UserAchievementEntity>

    @Query(
        """SELECT COUNT(*) FROM user_achievements 
           WHERE user_id = :userId AND achievement_id = :achievementId"""
    )
    suspend fun hasAchievement(userId: String, achievementId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserAchievement(ua: UserAchievementEntity)

    @Query(
        """UPDATE user_achievements SET announced = 1 
           WHERE user_id = :userId AND achievement_id = :achievementId"""
    )
    suspend fun markAnnounced(userId: String, achievementId: String)

    @Query("SELECT COUNT(*) FROM user_achievements WHERE user_id = :userId")
    suspend fun countUserAchievements(userId: String): Int
}