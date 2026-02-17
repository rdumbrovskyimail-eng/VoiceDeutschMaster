package com.voicedeutsch.master.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pronunciation_records",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("user_id", "word", "timestamp")
    ]
)
data class PronunciationRecordEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val word: String,
    val score: Float = 0f,
    @ColumnInfo(name = "problem_sounds_json")
    val problemSoundsJson: String = "[]",
    @ColumnInfo(name = "attempt_number")
    val attemptNumber: Int = 1,
    @ColumnInfo(name = "session_id")
    val sessionId: String? = null,
    val timestamp: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)