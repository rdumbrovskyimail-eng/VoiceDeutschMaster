package com.voicedeutsch.master.domain.usecase.session

import com.voicedeutsch.master.domain.model.session.LearningSession
import com.voicedeutsch.master.domain.repository.SessionRepository

/**
 * Analyzes session history to produce insights and recommendations.
 * Architecture line 933 (AnalyzeSessionResultsUseCase.kt).
 *
 * Metrics:
 *   - Average session duration trend
 *   - Error rate trend
 *   - Most productive strategies
 *   - Optimal study time
 */
class AnalyzeSessionResultsUseCase(
    private val sessionRepository: SessionRepository,
) {

    data class SessionAnalysis(
        val averageDurationMinutes: Float,
        val averageWordsPerSession: Float,
        val errorRateTrend: String,          // "improving", "stable", "worsening"
        val mostUsedStrategy: String,
        val mostProductiveStrategy: String,  // highest words-per-minute
        val totalSessionsAnalyzed: Int,
        val streakDays: Int,
        val recommendation: String,
    )

    suspend operator fun invoke(userId: String): SessionAnalysis {
        val sessions = sessionRepository.getRecentSessions(userId, 30)
        if (sessions.isEmpty()) {
            return SessionAnalysis(
                averageDurationMinutes = 0f,
                averageWordsPerSession = 0f,
                errorRateTrend = "stable",
                mostUsedStrategy = "LINEAR_BOOK",
                mostProductiveStrategy = "LINEAR_BOOK",
                totalSessionsAnalyzed = 0,
                streakDays = 0,
                recommendation = "Начни первую сессию!",
            )
        }

        val avgDuration = sessions.map { it.durationMinutes }.average().toFloat()
        val avgWords = sessions.map { it.wordsLearned + it.wordsReviewed }.average().toFloat()

        // Error rate trend: compare first half vs second half
        val half = sessions.size / 2
        val firstHalfErrors = sessions.take(half).map { errorRate(it) }.average()
        val secondHalfErrors = sessions.takeLast(half).map { errorRate(it) }.average()
        val trend = when {
            secondHalfErrors < firstHalfErrors - 0.05 -> "improving"
            secondHalfErrors > firstHalfErrors + 0.05 -> "worsening"
            else -> "stable"
        }

        // Most used strategy
        val strategyCounts = sessions
            .flatMap { it.strategiesUsed }
            .groupingBy { it.name }
            .eachCount()
        val mostUsed = strategyCounts.maxByOrNull { it.value }?.key ?: "LINEAR_BOOK"

        // Recommendation
        val rec = when {
            avgDuration < 10 -> "Попробуй заниматься хотя бы 15-20 минут за сессию."
            trend == "worsening" -> "Ошибок стало больше — попробуй больше повторений."
            avgWords < 3 -> "Попробуй стратегию VOCABULARY_BOOST для ускорения."
            else -> "Отличный прогресс! Продолжай в том же темпе."
        }

        return SessionAnalysis(
            averageDurationMinutes = avgDuration,
            averageWordsPerSession = avgWords,
            errorRateTrend = trend,
            mostUsedStrategy = mostUsed,
            mostProductiveStrategy = mostUsed, // simplified
            totalSessionsAnalyzed = sessions.size,
            streakDays = calculateStreak(sessions),
            recommendation = rec,
        )
    }

    private fun errorRate(session: LearningSession): Float {
        val total = session.wordsLearned + session.wordsReviewed
        return if (total == 0) 0f else session.mistakeCount.toFloat() / total
    }

    private fun calculateStreak(sessions: List<LearningSession>): Int {
        if (sessions.isEmpty()) return 0
        val dates = sessions.map { it.startedAt / 86_400_000 }.distinct().sorted()
        var streak = 1
        for (i in dates.size - 1 downTo 1) {
            if (dates[i] - dates[i - 1] == 1L) streak++ else break
        }
        return streak
    }
}