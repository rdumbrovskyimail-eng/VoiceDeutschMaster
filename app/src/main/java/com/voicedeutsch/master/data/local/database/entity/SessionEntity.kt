package com.voicedeutsch.master.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("user_id", "started_at")
    ]
)
data class SessionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "started_at")
    val startedAt: Long,
    @ColumnInfo(name = "ended_at")
    val endedAt: Long? = null,
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int = 0,
    @ColumnInfo(name = "strategies_used_json")
    val strategiesUsedJson: String = "[]",
    @ColumnInfo(name = "words_learned")
    val wordsLearned: Int = 0,
    @ColumnInfo(name = "words_reviewed")
    val wordsReviewed: Int = 0,
    @ColumnInfo(name = "rules_practiced")
    val rulesPracticed: Int = 0,
    @ColumnInfo(name = "exercises_completed")
    val exercisesCompleted: Int = 0,
    @ColumnInfo(name = "exercises_correct")
    val exercisesCorrect: Int = 0,
    @ColumnInfo(name = "average_pronunciation_score")
    val averagePronunciationScore: Float = 0f,
    @ColumnInfo(name = "book_chapter_start")
    val bookChapterStart: Int = 0,
    @ColumnInfo(name = "book_lesson_start")
    val bookLessonStart: Int = 0,
    @ColumnInfo(name = "book_chapter_end")
    val bookChapterEnd: Int = 0,
    @ColumnInfo(name = "book_lesson_end")
    val bookLessonEnd: Int = 0,
    @ColumnInfo(name = "session_summary")
    val sessionSummary: String = "",
    @ColumnInfo(name = "mood_estimate")
    val moodEstimate: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)