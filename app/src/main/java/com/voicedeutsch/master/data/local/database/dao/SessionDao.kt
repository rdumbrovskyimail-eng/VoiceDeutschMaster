package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.voicedeutsch.master.data.local.database.entity.SessionEntity
import com.voicedeutsch.master.data.local.database.entity.SessionEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    // --- SESSIONS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE user_id = :userId ORDER BY started_at DESC LIMIT :limit")
    suspend fun getRecentSessions(userId: String, limit: Int): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE user_id = :userId ORDER BY started_at DESC")
    fun getSessionsFlow(userId: String): Flow<List<SessionEntity>>

    @Query("SELECT COUNT(*) FROM sessions WHERE user_id = :userId")
    suspend fun getSessionCount(userId: String): Int

    @Query("SELECT COALESCE(SUM(duration_minutes), 0) FROM sessions WHERE user_id = :userId")
    suspend fun getTotalMinutes(userId: String): Int

    // --- SESSION EVENTS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionEvent(event: SessionEventEntity)

    @Query("SELECT * FROM session_events WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getSessionEvents(sessionId: String): List<SessionEventEntity>
}