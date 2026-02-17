package com.voicedeutsch.master.data.local.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.voicedeutsch.master.data.local.database.entity.DailyStatisticsEntity
import com.voicedeutsch.master.data.local.database.entity.PronunciationRecordEntity

@Dao
interface ProgressDao {

    // --- DAILY STATISTICS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyStats(stats: DailyStatisticsEntity)

    @Query("SELECT * FROM daily_statistics WHERE user_id = :userId AND date = :date")
    suspend fun getDailyStats(userId: String, date: String): DailyStatisticsEntity?

    @Query(
        """
        SELECT * FROM daily_statistics 
        WHERE user_id = :userId AND date >= :startDate AND date <= :endDate 
        ORDER BY date ASC
        """
    )
    suspend fun getDailyStatsRange(
        userId: String,
        startDate: String,
        endDate: String
    ): List<DailyStatisticsEntity>

    @Query("SELECT * FROM daily_statistics WHERE user_id = :userId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentDailyStats(userId: String, limit: Int): List<DailyStatisticsEntity>

    // --- PRONUNCIATION RECORDS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPronunciationRecord(record: PronunciationRecordEntity)

    @Query("SELECT * FROM pronunciation_records WHERE user_id = :userId AND word = :word ORDER BY timestamp DESC")
    suspend fun getPronunciationRecords(userId: String, word: String): List<PronunciationRecordEntity>

    @Query("SELECT COALESCE(AVG(score), 0.0) FROM pronunciation_records WHERE user_id = :userId")
    suspend fun getAveragePronunciationScore(userId: String): Float

    @Query(
        """
        SELECT word, AVG(score) as avg_score, COUNT(*) as attempts 
        FROM pronunciation_records 
        WHERE user_id = :userId 
        GROUP BY word 
        HAVING avg_score < 0.7 AND attempts >= 3
        ORDER BY avg_score ASC
        """
    )
    suspend fun getProblemWordsForPronunciation(userId: String): List<PronunciationWordStat>

    @Query("SELECT * FROM pronunciation_records WHERE user_id = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentRecords(userId: String, limit: Int): List<PronunciationRecordEntity>
}

data class PronunciationWordStat(
    val word: String,
    @ColumnInfo(name = "avg_score") val avgScore: Float,
    val attempts: Int
)