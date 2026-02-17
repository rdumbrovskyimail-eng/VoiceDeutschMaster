package com.voicedeutsch.master.data.mapper

import com.voicedeutsch.master.data.local.database.entity.SessionEntity
import com.voicedeutsch.master.data.local.database.entity.SessionEventEntity
import com.voicedeutsch.master.domain.model.session.LearningSession
import com.voicedeutsch.master.domain.model.session.MoodEstimate
import com.voicedeutsch.master.domain.model.session.SessionEvent
import com.voicedeutsch.master.domain.model.session.SessionEventType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SessionMapper {

    fun SessionEntity.toDomain(json: Json): LearningSession {
        val strategies = try {
            json.decodeFromString<List<String>>(strategiesUsedJson)
        } catch (e: Exception) {
            emptyList()
        }
        return LearningSession(
            id = id,
            userId = userId,
            startedAt = startedAt,
            endedAt = endedAt,
            durationMinutes = durationMinutes,
            strategiesUsed = strategies,
            wordsLearned = wordsLearned,
            wordsReviewed = wordsReviewed,
            rulesPracticed = rulesPracticed,
            exercisesCompleted = exercisesCompleted,
            exercisesCorrect = exercisesCorrect,
            averagePronunciationScore = averagePronunciationScore,
            bookChapterStart = bookChapterStart,
            bookLessonStart = bookLessonStart,
            bookChapterEnd = bookChapterEnd,
            bookLessonEnd = bookLessonEnd,
            sessionSummary = sessionSummary,
            moodEstimate = moodEstimate?.let {
                try {
                    MoodEstimate.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            },
            createdAt = createdAt
        )
    }

    fun LearningSession.toEntity(json: Json): SessionEntity = SessionEntity(
        id = id,
        userId = userId,
        startedAt = startedAt,
        endedAt = endedAt,
        durationMinutes = durationMinutes,
        strategiesUsedJson = json.encodeToString(strategiesUsed),
        wordsLearned = wordsLearned,
        wordsReviewed = wordsReviewed,
        rulesPracticed = rulesPracticed,
        exercisesCompleted = exercisesCompleted,
        exercisesCorrect = exercisesCorrect,
        averagePronunciationScore = averagePronunciationScore,
        bookChapterStart = bookChapterStart,
        bookLessonStart = bookLessonStart,
        bookChapterEnd = bookChapterEnd,
        bookLessonEnd = bookLessonEnd,
        sessionSummary = sessionSummary,
        moodEstimate = moodEstimate?.name,
        createdAt = createdAt
    )

    fun SessionEventEntity.toDomain(): SessionEvent = SessionEvent(
        id = id,
        sessionId = sessionId,
        eventType = try {
            SessionEventType.valueOf(eventType)
        } catch (e: Exception) {
            SessionEventType.USER_REQUEST
        },
        timestamp = timestamp,
        detailsJson = detailsJson,
        createdAt = createdAt
    )

    fun SessionEvent.toEntity(): SessionEventEntity = SessionEventEntity(
        id = id,
        sessionId = sessionId,
        eventType = eventType.name,
        timestamp = timestamp,
        detailsJson = detailsJson,
        createdAt = createdAt
    )
}