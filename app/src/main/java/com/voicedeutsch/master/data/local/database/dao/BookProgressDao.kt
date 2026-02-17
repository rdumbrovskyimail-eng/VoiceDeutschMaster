package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.voicedeutsch.master.data.local.database.entity.BookProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookProgressDao {

    @Query("SELECT * FROM book_progress WHERE user_id = :userId AND chapter = :chapter AND lesson = :lesson")
    suspend fun getProgress(userId: String, chapter: Int, lesson: Int): BookProgressEntity?

    @Query(
        """
        SELECT * FROM book_progress WHERE user_id = :userId 
        ORDER BY 
            CASE status WHEN 'IN_PROGRESS' THEN 0 WHEN 'NOT_STARTED' THEN 1 ELSE 2 END,
            chapter ASC, lesson ASC
        LIMIT 1
        """
    )
    suspend fun getCurrentPosition(userId: String): BookProgressEntity?

    @Query("SELECT * FROM book_progress WHERE user_id = :userId ORDER BY chapter ASC, lesson ASC")
    suspend fun getAllProgress(userId: String): List<BookProgressEntity>

    @Query("SELECT * FROM book_progress WHERE user_id = :userId ORDER BY chapter ASC, lesson ASC")
    fun getAllProgressFlow(userId: String): Flow<List<BookProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: BookProgressEntity)

    @Query("SELECT COUNT(*) FROM book_progress WHERE user_id = :userId AND status = 'COMPLETED'")
    suspend fun getCompletedCount(userId: String): Int

    @Query(
        """
        UPDATE book_progress SET 
            status = 'COMPLETED', 
            score = :score, 
            completed_at = :completedAt 
        WHERE user_id = :userId AND chapter = :chapter AND lesson = :lesson
        """
    )
    suspend fun markComplete(userId: String, chapter: Int, lesson: Int, score: Float, completedAt: Long)
}