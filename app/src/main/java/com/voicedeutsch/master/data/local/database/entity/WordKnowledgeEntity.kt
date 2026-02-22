package com.voicedeutsch.master.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "word_knowledge",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("user_id", "next_review"),
        Index("user_id", "knowledge_level"),
        Index(value = ["user_id", "word_id"], unique = true),
        Index("word_id")
    ]
)
data class WordKnowledgeEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "word_id")
    val wordId: String,
    @ColumnInfo(name = "knowledge_level")
    val knowledgeLevel: Int = 0,
    @ColumnInfo(name = "times_seen")
    val timesSeen: Int = 0,
    @ColumnInfo(name = "times_correct")
    val timesCorrect: Int = 0,
    @ColumnInfo(name = "times_incorrect")
    val timesIncorrect: Int = 0,
    @ColumnInfo(name = "last_seen")
    val lastSeen: Long? = null,
    @ColumnInfo(name = "last_correct")
    val lastCorrect: Long? = null,
    @ColumnInfo(name = "last_incorrect")
    val lastIncorrect: Long? = null,
    @ColumnInfo(name = "next_review")
    val nextReview: Long? = null,
    @ColumnInfo(name = "srs_interval_days")
    val srsIntervalDays: Float = 0f,
    @ColumnInfo(name = "srs_ease_factor")
    val srsEaseFactor: Float = 2.5f,
    @ColumnInfo(name = "pronunciation_score")
    val pronunciationScore: Float = 0f,
    @ColumnInfo(name = "pronunciation_attempts")
    val pronunciationAttempts: Int = 0,
    @ColumnInfo(name = "contexts_json")
    val contextsJson: String = "[]",
    @ColumnInfo(name = "mistakes_json")
    val mistakesJson: String = "[]",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)