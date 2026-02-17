package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.voicedeutsch.master.data.local.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserFlow(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUser(userId: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getFirstUser(): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query(
        """
        UPDATE users SET 
            cefr_level = :cefrLevel, 
            cefr_sub_level = :subLevel,
            updated_at = :updatedAt 
        WHERE id = :userId
        """
    )
    suspend fun updateLevel(userId: String, cefrLevel: String, subLevel: Int, updatedAt: Long)

    @Query(
        """
        UPDATE users SET 
            total_sessions = total_sessions + 1,
            total_minutes = total_minutes + :durationMinutes,
            total_words_learned = total_words_learned + :wordsLearned,
            total_rules_learned = total_rules_learned + :rulesLearned,
            last_session_date = :sessionDate,
            updated_at = :updatedAt
        WHERE id = :userId
        """
    )
    suspend fun incrementSessionStats(
        userId: String,
        durationMinutes: Int,
        wordsLearned: Int,
        rulesLearned: Int,
        sessionDate: Long,
        updatedAt: Long
    )

    @Query("UPDATE users SET streak_days = :streakDays, updated_at = :updatedAt WHERE id = :userId")
    suspend fun updateStreak(userId: String, streakDays: Int, updatedAt: Long)

    @Query("UPDATE users SET preferences_json = :prefsJson, updated_at = :updatedAt WHERE id = :userId")
    suspend fun updatePreferences(userId: String, prefsJson: String, updatedAt: Long)

    @Query("UPDATE users SET voice_settings_json = :settingsJson, updated_at = :updatedAt WHERE id = :userId")
    suspend fun updateVoiceSettings(userId: String, settingsJson: String, updatedAt: Long)
}