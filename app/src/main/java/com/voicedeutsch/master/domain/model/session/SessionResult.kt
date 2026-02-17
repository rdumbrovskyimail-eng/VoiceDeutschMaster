package com.voicedeutsch.master.domain.model.session

/**
 * Summarised result of a completed learning session.
 *
 * Assembled at session end and displayed on the session-result screen.
 * Not serializable — used only as a transient view model.
 */
data class SessionResult(
    val sessionId: String,
    val durationMinutes: Int,
    val wordsLearned: Int,
    val wordsReviewed: Int,
    val rulesPracticed: Int,
    val exercisesCompleted: Int,
    val exercisesCorrect: Int,
    val averageScore: Float,
    val averagePronunciationScore: Float,
    val strategiesUsed: List<String>,
    val summary: String
)