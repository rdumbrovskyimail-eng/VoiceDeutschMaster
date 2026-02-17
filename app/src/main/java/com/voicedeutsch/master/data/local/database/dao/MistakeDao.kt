package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.voicedeutsch.master.data.local.database.entity.MistakeLogEntity

@Dao
interface MistakeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistake(mistake: MistakeLogEntity)

    @Query("SELECT * FROM mistakes_log WHERE user_id = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getMistakes(userId: String, limit: Int): List<MistakeLogEntity>

    @Query(
        """
        SELECT * FROM mistakes_log 
        WHERE user_id = :userId AND type = :type 
        ORDER BY timestamp DESC 
        LIMIT :limit
        """
    )
    suspend fun getMistakesByType(userId: String, type: String, limit: Int = 50): List<MistakeLogEntity>

    @Query("SELECT COUNT(*) FROM mistakes_log WHERE user_id = :userId")
    suspend fun getMistakeCount(userId: String): Int

    @Query(
        """
        SELECT * FROM mistakes_log 
        WHERE user_id = :userId AND session_id = :sessionId 
        ORDER BY timestamp ASC
        """
    )
    suspend fun getMistakesBySession(userId: String, sessionId: String): List<MistakeLogEntity>

    @Query(
        """
        SELECT item, type, COUNT(*) as count 
        FROM mistakes_log 
        WHERE user_id = :userId 
        GROUP BY item, type 
        HAVING count >= 3 
        ORDER BY count DESC 
        LIMIT :limit
        """
    )
    suspend fun getFrequentMistakes(userId: String, limit: Int = 20): List<FrequentMistakeStat>
}

data class FrequentMistakeStat(
    val item: String,
    val type: String,
    val count: Int
)