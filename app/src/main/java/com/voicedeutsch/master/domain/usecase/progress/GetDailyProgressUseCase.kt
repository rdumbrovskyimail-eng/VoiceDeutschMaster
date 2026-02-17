package com.voicedeutsch.master.domain.usecase.progress

import com.voicedeutsch.master.domain.model.progress.DailyProgress
import com.voicedeutsch.master.domain.repository.ProgressRepository
import com.voicedeutsch.master.domain.repository.SessionRepository
import com.voicedeutsch.master.util.DateUtils

/**
 * Retrieves daily progress — today, weekly, monthly, and streak data.
 */
class GetDailyProgressUseCase(
    private val sessionRepository: SessionRepository,
    private val progressRepository: ProgressRepository
) {

    suspend fun getToday(userId: String): DailyProgress? {
        val today = DateUtils.todayDateString()
        return sessionRepository.getDailyStatistics(userId, today)
    }

    suspend fun getWeekly(userId: String): List<DailyProgress> {
        return progressRepository.getWeeklyProgress(userId)
    }

    suspend fun getMonthly(userId: String): List<DailyProgress> {
        return progressRepository.getMonthlyProgress(userId)
    }

    suspend fun getStreak(userId: String): Int {
        return sessionRepository.calculateStreak(userId)
    }

    suspend fun getTodaySummary(userId: String): DailySummary {
        val today = getToday(userId)
        val streak = getStreak(userId)

        return DailySummary(
            sessionsToday = today?.sessionsCount ?: 0,
            minutesToday = today?.totalMinutes ?: 0,
            wordsLearnedToday = today?.wordsLearned ?: 0,
            wordsReviewedToday = today?.wordsReviewed ?: 0,
            exercisesCompletedToday = today?.exercisesCompleted ?: 0,
            averageScoreToday = today?.averageScore ?: 0f,
            currentStreak = streak
        )
    }

    data class DailySummary(
        val sessionsToday: Int,
        val minutesToday: Int,
        val wordsLearnedToday: Int,
        val wordsReviewedToday: Int,
        val exercisesCompletedToday: Int,
        val averageScoreToday: Float,
        val currentStreak: Int
    )
}