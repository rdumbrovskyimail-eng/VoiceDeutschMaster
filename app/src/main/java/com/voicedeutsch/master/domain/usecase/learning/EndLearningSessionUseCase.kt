package com.voicedeutsch.master.domain.usecase.learning

import com.voicedeutsch.master.domain.model.session.SessionEvent
import com.voicedeutsch.master.domain.model.session.SessionEventType
import com.voicedeutsch.master.domain.model.session.SessionResult
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.SessionRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.util.DateUtils
import com.voicedeutsch.master.util.generateUUID
import com.voicedeutsch.master.util.safeDivide

/**
 * Ends a learning session, computes final statistics,
 * updates user profile stats, recalculates streak,
 * and persists daily progress.
 */
class EndLearningSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val bookRepository: BookRepository
) {

    suspend operator fun invoke(sessionId: String, summary: String = ""): SessionResult {
        val session = sessionRepository.getSession(sessionId)
            ?: throw IllegalArgumentException("Session $sessionId not found")

        val now = DateUtils.nowTimestamp()
        val durationMinutes = ((now - session.startedAt) / 60_000).toInt().coerceAtLeast(1)

        val events = sessionRepository.getSessionEvents(sessionId)

        val wordsLearned = events.count { it.eventType == SessionEventType.WORD_LEARNED }
        val wordsReviewed = events.count { it.eventType == SessionEventType.WORD_REVIEWED }
        val rulesPracticed = events.count { it.eventType == SessionEventType.RULE_PRACTICED }

        val exerciseEventTypes = setOf(
            SessionEventType.WORD_LEARNED,
            SessionEventType.WORD_REVIEWED,
            SessionEventType.RULE_PRACTICED,
            SessionEventType.PRONUNCIATION_ATTEMPT
        )
        val exercisesCompleted = events.count { it.eventType in exerciseEventTypes }

        val exercisesCorrect = events.count { event ->
            event.eventType in exerciseEventTypes &&
                event.detailsJson.contains("\"correct\":true", ignoreCase = true)
        }

        val averageScore = safeDivide(exercisesCorrect, exercisesCompleted)

        val strategiesUsed = events
            .filter { it.eventType == SessionEventType.STRATEGY_CHANGE }
            .mapNotNull { event ->
                extractStrategyFromJson(event.detailsJson)
            }
            .distinct()

        val pronunciationEvents = events.filter {
            it.eventType == SessionEventType.PRONUNCIATION_ATTEMPT
        }
        val averagePronunciationScore = if (pronunciationEvents.isNotEmpty()) {
            val scores = pronunciationEvents.mapNotNull { event ->
                extractScoreFromJson(event.detailsJson)
            }
            if (scores.isNotEmpty()) scores.average().toFloat() else 0f
        } else {
            0f
        }

        val (bookChapterEnd, bookLessonEnd) = bookRepository.getCurrentBookPosition(session.userId)

        val updatedSession = session.copy(
            endedAt = now,
            durationMinutes = durationMinutes,
            strategiesUsed = strategiesUsed,
            wordsLearned = wordsLearned,
            wordsReviewed = wordsReviewed,
            rulesPracticed = rulesPracticed,
            exercisesCompleted = exercisesCompleted,
            exercisesCorrect = exercisesCorrect,
            averagePronunciationScore = averagePronunciationScore,
            bookChapterEnd = bookChapterEnd,
            bookLessonEnd = bookLessonEnd,
            sessionSummary = summary
        )
        sessionRepository.updateSession(updatedSession)

        val endEvent = SessionEvent(
            id = generateUUID(),
            sessionId = sessionId,
            eventType = SessionEventType.SESSION_END,
            timestamp = now,
            detailsJson = """{"duration":$durationMinutes,"wordsLearned":$wordsLearned,"wordsReviewed":$wordsReviewed}""",
            createdAt = now
        )
        sessionRepository.addSessionEvent(endEvent)

        userRepository.incrementSessionStats(
            userId = session.userId,
            durationMinutes = durationMinutes,
            wordsLearned = wordsLearned,
            rulesLearned = rulesPracticed
        )

        updateStreak(session.userId, now)

        val today = DateUtils.todayDateString()
        val existingDaily = sessionRepository.getDailyStatistics(session.userId, today)
        val prevCompleted = existingDaily?.exercisesCompleted ?: 0
        val prevScore = existingDaily?.averageScore ?: 0f
        val combinedScore = if (prevCompleted + exercisesCompleted > 0)
            (prevScore * prevCompleted + averageScore * exercisesCompleted) /
            (prevCompleted + exercisesCompleted)
        else 0f
        sessionRepository.upsertDailyStatistics(
            userId = session.userId,
            date = today,
            sessionsCount = (existingDaily?.sessionsCount ?: 0) + 1,
            totalMinutes = (existingDaily?.totalMinutes ?: 0) + durationMinutes,
            wordsLearned = (existingDaily?.wordsLearned ?: 0) + wordsLearned,
            wordsReviewed = (existingDaily?.wordsReviewed ?: 0) + wordsReviewed,
            exercisesCompleted = (existingDaily?.exercisesCompleted ?: 0) + exercisesCompleted,
            exercisesCorrect = (existingDaily?.exercisesCorrect ?: 0) + exercisesCorrect,
            averageScore = combinedScore,
            streakMaintained = true
        )

        return SessionResult(
            sessionId = sessionId,
            durationMinutes = durationMinutes,
            wordsLearned = wordsLearned,
            wordsReviewed = wordsReviewed,
            rulesPracticed = rulesPracticed,
            exercisesCompleted = exercisesCompleted,
            exercisesCorrect = exercisesCorrect,
            averageScore = averageScore,
            averagePronunciationScore = averagePronunciationScore,
            strategiesUsed = strategiesUsed,
            summary = summary
        )
    }

    private suspend fun updateStreak(userId: String, now: Long) {
        val profile = userRepository.getUserProfile(userId) ?: return
        val lastSessionDate = profile.lastSessionDate

        val newStreak = when {
            lastSessionDate == null -> 1
            DateUtils.isSameDay(lastSessionDate, now) -> profile.streakDays
            DateUtils.daysBetween(lastSessionDate, now) == 1L -> profile.streakDays + 1
            else -> 1
        }

        userRepository.updateStreak(userId, newStreak)
    }

    private fun extractStrategyFromJson(json: String): String? {
        val regex = """"strategy"\s*:\s*"([^"]+)"""".toRegex()
        return regex.find(json)?.groupValues?.getOrNull(1)
    }

    private fun extractScoreFromJson(json: String): Float? {
        val regex = """"score"\s*:\s*([0-9.]+)""".toRegex()
        return regex.find(json)?.groupValues?.getOrNull(1)?.toFloatOrNull()
    }
}