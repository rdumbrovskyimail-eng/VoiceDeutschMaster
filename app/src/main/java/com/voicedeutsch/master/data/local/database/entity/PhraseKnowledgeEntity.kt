package com.voicedeutsch.master.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "phrase_knowledge",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PhraseEntity::class,
            parentColumns = ["id"],
            childColumns = ["phrase_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id", "phrase_id"], unique = true),
        Index("phrase_id")
    ]
)
data class PhraseKnowledgeEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "phrase_id")
    val phraseId: String,
    @ColumnInfo(name = "knowledge_level")
    val knowledgeLevel: Int = 0,
    @ColumnInfo(name = "times_practiced")
    val timesPracticed: Int = 0,
    @ColumnInfo(name = "times_correct")
    val timesCorrect: Int = 0,
    @ColumnInfo(name = "last_practiced")
    val lastPracticed: Long? = null,
    @ColumnInfo(name = "next_review")
    val nextReview: Long? = null,
    @ColumnInfo(name = "srs_interval_days")
    val srsIntervalDays: Float = 0f,
    @ColumnInfo(name = "srs_ease_factor")
    val srsEaseFactor: Float = 2.5f,
    @ColumnInfo(name = "pronunciation_score")
    val pronunciationScore: Float = 0f,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)