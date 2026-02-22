package com.voicedeutsch.master.data.repository

import com.voicedeutsch.master.data.local.database.dao.ProgressDao
import com.voicedeutsch.master.data.local.database.dao.SessionDao
import com.voicedeutsch.master.data.local.database.entity.DailyStatisticsEntity
import com.voicedeutsch.master.data.mapper.ProgressMapper.toDomain
import com.voicedeutsch.master.data.mapper.SessionMapper.toDomain
import com.voicedeutsch.master.data.mapper.SessionMapper.toEntity
import com.voicedeutsch.master.domain.model.progress.DailyProgress
import com.voicedeutsch.master.domain.model.session.LearningSession
import com.voicedeutsch.master.domain.model.session.SessionEvent
import com.voicedeutsch.master.domain.repository.SessionRepository
import com.voicedeutsch.master.util.DateUtils
import com.voicedeutsch.master.util.generateUUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SessionRepositoryImpl(
    private val sessionDao: SessionDao,
    private val progressDao: ProgressDao,
    private val json: Json
) : SessionRepository {

    override suspend fun createSession(session: LearningSession): String {
        sessionDao.insertSession(session.toEntity(json))
        return session.id
    }

    override suspend fun updateSession(session: LearningSession) =
        sessionDao.updateSession(session.toEntity(json))

    override suspend fun getSession(sessionId: String): LearningSession? =
        sessionDao.getSession(sessionId)?.toDomain(json)

    override suspend fun getRecentSessions(userId: String, limit: Int): List<LearningSession> =
        sessionDao.getRecentSessions(userId, limit).map { it.toDomain(json) }

    override fun getSessionsFlow(userId: String): Flow<List<LearningSession>> =
        sessionDao.getSessionsFlow(userId).map { list ->
            list.map { it.toDomain(json) }
        }

    override suspend fun getSessionCount(userId: String): Int =
        sessionDao.getSessionCount(userId)

    override suspend fun getTotalMinutes(userId: String): Int =
        sessionDao.getTotalMinutes(userId)

    override suspend fun getSessionsSince(userId: String, fromTimestamp: Long): List<LearningSession> =
        sessionDao.getRecentSessions(userId, Int.MAX_VALUE)
            .filter { it.startedAt >= fromTimestamp }
            .map { it.toDomain(json) }

    override suspend fun addSessionEvent(event: SessionEvent) =
        sessionDao.insertSessionEvent(event.toEntity())

    override suspend fun getSessionEvents(sessionId: String): List<SessionEvent> =
        sessionDao.getSessionEvents(sessionId).map { it.toDomain() }

    override suspend fun upsertDailyStatistics(
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
    ) {
        val existing = progressDao.getDailyStats(userId, date)
        val entity = DailyStatisticsEntity(
            id = existing?.id ?: generateUUID(),
            userId = userId,
            date = date,
            sessionsCount = sessionsCount,
            totalMinutes = totalMinutes,
            wordsLearned = wordsLearned,
            wordsReviewed = wordsReviewed,
            exercisesCompleted = exercisesCompleted,
            exercisesCorrect = exercisesCorrect,
            averageScore = averageScore,
            streakMaintained = streakMaintained,
            createdAt = existing?.createdAt ?: DateUtils.nowTimestamp()
        )
        progressDao.upsertDailyStats(entity)
    }

    override suspend fun getDailyStatistics(userId: String, date: String): DailyProgress? =
        progressDao.getDailyStats(userId, date)?.toDomain()

    override suspend fun getDailyStatisticsRange(
        userId: String,
        startDate: String,
        endDate: String
    ): List<DailyProgress> =
        progressDao.getDailyStatsRange(userId, startDate, endDate).map { it.toDomain() }

    override suspend fun calculateStreak(userId: String): Int {
        val recentStats = progressDao.getRecentDailyStats(userId, 365)
        if (recentStats.isEmpty()) return 0

        var streak = 0
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        var expectedDate = LocalDate.now()

        val todayStr = expectedDate.format(formatter)
        val hasTodaySession = recentStats.any { it.date == todayStr && it.streakMaintained }
        if (!hasTodaySession) {
            expectedDate = expectedDate.minusDays(1)
        }

        for (stat in recentStats.sortedByDescending { it.date }) {
            val statDate = try {
                LocalDate.parse(stat.date, formatter)
            } catch (e: Exception) {
                continue
            }

            if (statDate == expectedDate && stat.streakMaintained) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else if (statDate < expectedDate) {
                break
            }
        }

        return streak
    }
}