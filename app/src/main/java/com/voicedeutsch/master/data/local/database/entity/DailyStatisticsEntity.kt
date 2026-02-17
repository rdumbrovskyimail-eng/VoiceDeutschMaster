package com.voicedeutsch.master.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_statistics",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id", "date"], unique = true)
    ]
)
data class DailyStatisticsEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val date: String,
    @ColumnInfo(name = "sessions_count")
    val sessionsCount: Int = 0,
    @ColumnInfo(name = "total_minutes")
    val totalMinutes: Int = 0,
    @ColumnInfo(name = "words_learned")
    val wordsLearned: Int = 0,
    @ColumnInfo(name = "words_reviewed")
    val wordsReviewed: Int = 0,
    @ColumnInfo(name = "exercises_completed")
    val exercisesCompleted: Int = 0,
    @ColumnInfo(name = "exercises_correct")
    val exercisesCorrect: Int = 0,
    @ColumnInfo(name = "average_score")
    val averageScore: Float = 0f,
    @ColumnInfo(name = "streak_maintained")
    val streakMaintained: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)