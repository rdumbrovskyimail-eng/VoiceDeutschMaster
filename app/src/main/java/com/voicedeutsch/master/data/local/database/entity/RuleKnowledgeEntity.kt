package com.voicedeutsch.master.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rule_knowledge",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GrammarRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["rule_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("user_id", "next_review"),
        Index(value = ["user_id", "rule_id"], unique = true)
    ]
)
data class RuleKnowledgeEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "rule_id")
    val ruleId: String,
    @ColumnInfo(name = "knowledge_level")
    val knowledgeLevel: Int = 0,
    @ColumnInfo(name = "times_practiced")
    val timesPracticed: Int = 0,
    @ColumnInfo(name = "times_correct")
    val timesCorrect: Int = 0,
    @ColumnInfo(name = "times_incorrect")
    val timesIncorrect: Int = 0,
    @ColumnInfo(name = "last_practiced")
    val lastPracticed: Long? = null,
    @ColumnInfo(name = "next_review")
    val nextReview: Long? = null,
    @ColumnInfo(name = "srs_interval_days")
    val srsIntervalDays: Float = 0f,
    @ColumnInfo(name = "srs_ease_factor")
    val srsEaseFactor: Float = 2.5f,
    @ColumnInfo(name = "common_mistakes_json")
    val commonMistakesJson: String = "[]",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)