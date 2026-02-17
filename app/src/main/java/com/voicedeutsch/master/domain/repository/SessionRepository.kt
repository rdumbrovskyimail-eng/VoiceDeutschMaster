package com.voicedeutsch.master.domain.repository

import com.voicedeutsch.master.domain.model.progress.DailyProgress
import com.voicedeutsch.master.domain.model.session.LearningSession
import com.voicedeutsch.master.domain.model.session.SessionEvent
import kotlinx.coroutines.flow.Flow

interface SessionRepository {

    suspend fun createSession(session: LearningSession): String

    suspend fun updateSession(session: LearningSession)

    suspend fun getSession(sessionId: String): LearningSession?

    suspend fun getRecentSessions(userId: String, limit: Int): List<LearningSession>

    fun getSessionsFlow(userId: String): Flow<List<LearningSession>>

    suspend fun getSessionCount(userId: String): Int

    suspend fun getTotalMinutes(userId: String): Int

    // ==========================================
    // SESSION EVENTS
    // ==========================================

    suspend fun addSessionEvent(event: SessionEvent)

    suspend fun getSessionEvents(sessionId: String): List<SessionEvent>

    // ==========================================
    // DAILY STATISTICS
    // ==========================================

    suspend fun upsertDailyStatistics(
        userId: String,
        date: String,
        sessionsCount: Int,
        totalMinutes: Int,
        wordsLearned: Int,
        wordsReviewed: Int,
        exercisesCompleted: Int,
        exercisesCorrect: Int,
        averageScore: Float,
        streakMaintained: Boolean
    )

    suspend fun getDailyStatistics(userId: String, date: String): DailyProgress?

    suspend fun getDailyStatisticsRange(
        userId: String,
        startDate: String,
        endDate: String
    ): List<DailyProgress>

    suspend fun calculateStreak(userId: String): Int
}