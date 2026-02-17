package com.voicedeutsch.master.domain.usecase.learning

import com.voicedeutsch.master.domain.model.session.LearningSession
import com.voicedeutsch.master.domain.model.session.SessionEvent
import com.voicedeutsch.master.domain.model.session.SessionEventType
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.SessionRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.util.DateUtils
import com.voicedeutsch.master.util.generateUUID

/**
 * Starts a new learning session.
 * Creates a LearningSession record, records SESSION_START event,
 * and returns initial session data needed for context building.
 */
class StartLearningSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val bookRepository: BookRepository
) {

    data class SessionStartData(
        val session: LearningSession,
        val currentChapter: Int,
        val currentLesson: Int,
        val userName: String
    )

    suspend operator fun invoke(userId: String): SessionStartData {
        val profile = userRepository.getUserProfile(userId)
        val userName = profile?.name ?: "Пользователь"

        val (currentChapter, currentLesson) = bookRepository.getCurrentBookPosition(userId)

        val now = DateUtils.nowTimestamp()

        val session = LearningSession(
            id = generateUUID(),
            userId = userId,
            startedAt = now,
            endedAt = null,
            durationMinutes = 0,
            strategiesUsed = emptyList(),
            wordsLearned = 0,
            wordsReviewed = 0,
            rulesPracticed = 0,
            exercisesCompleted = 0,
            exercisesCorrect = 0,
            averagePronunciationScore = 0f,
            bookChapterStart = currentChapter,
            bookLessonStart = currentLesson,
            bookChapterEnd = currentChapter,
            bookLessonEnd = currentLesson,
            sessionSummary = "",
            moodEstimate = null,
            createdAt = now
        )

        sessionRepository.createSession(session)

        val startEvent = SessionEvent(
            id = generateUUID(),
            sessionId = session.id,
            eventType = SessionEventType.SESSION_START,
            timestamp = now,
            detailsJson = """{"userId":"$userId","chapter":$currentChapter,"lesson":$currentLesson}""",
            createdAt = now
        )
        sessionRepository.addSessionEvent(startEvent)

        return SessionStartData(
            session = session,
            currentChapter = currentChapter,
            currentLesson = currentLesson,
            userName = userName
        )
    }
}