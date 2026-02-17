package com.voicedeutsch.master.domain.model.session

import kotlinx.serialization.Serializable

/**
 * A single learning session — one "visit" of the user.
 *
 * Records duration, strategies used, items learned/reviewed,
 * exercises completed, pronunciation scores, book coordinates, and a summary.
 */
@Serializable
data class LearningSession(
    val id: String,
    val userId: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationMinutes: Int = 0,
    val strategiesUsed: List<String> = emptyList(),
    val wordsLearned: Int = 0,
    val wordsReviewed: Int = 0,
    val rulesPracticed: Int = 0,
    val exercisesCompleted: Int = 0,
    val exercisesCorrect: Int = 0,
    val averagePronunciationScore: Float = 0f,
    val bookChapterStart: Int = 0,
    val bookLessonStart: Int = 0,
    val bookChapterEnd: Int = 0,
    val bookLessonEnd: Int = 0,
    val sessionSummary: String = "",
    val moodEstimate: MoodEstimate? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Exercise accuracy for the session. Returns 0 when no exercises were attempted. */
    val accuracy: Float get() = if (exercisesCompleted == 0) 0f
        else exercisesCorrect.toFloat() / exercisesCompleted
}

/**
 * Estimated user mood during the session (inferred by Gemini).
 */
@Serializable
enum class MoodEstimate { MOTIVATED, NEUTRAL, TIRED }