package com.voicedeutsch.master.data.mapper

import com.voicedeutsch.master.data.local.database.entity.BookProgressEntity
import com.voicedeutsch.master.data.local.database.entity.DailyStatisticsEntity
import com.voicedeutsch.master.data.local.database.entity.MistakeLogEntity
import com.voicedeutsch.master.data.local.database.entity.PronunciationRecordEntity
import com.voicedeutsch.master.domain.model.book.BookProgress
import com.voicedeutsch.master.domain.model.book.LessonStatus
import com.voicedeutsch.master.domain.model.knowledge.MistakeLog
import com.voicedeutsch.master.domain.model.knowledge.MistakeType
import com.voicedeutsch.master.domain.model.progress.DailyProgress
import com.voicedeutsch.master.domain.model.speech.PronunciationResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ProgressMapper {

    // === DailyStatistics ↔ DailyProgress ===

    fun DailyStatisticsEntity.toDomain(): DailyProgress = DailyProgress(
        id = id,
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
        createdAt = createdAt
    )

    fun DailyProgress.toEntity(): DailyStatisticsEntity = DailyStatisticsEntity(
        id = id,
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
        createdAt = createdAt
    )

    // === BookProgress ↔ BookProgress ===

    fun BookProgressEntity.toDomain(): BookProgress = BookProgress(
        id = id,
        userId = userId,
        chapter = chapter,
        lesson = lesson,
        status = try {
            LessonStatus.valueOf(status)
        } catch (e: Exception) {
            LessonStatus.NOT_STARTED
        },
        score = score,
        startedAt = startedAt,
        completedAt = completedAt,
        timesPracticed = timesPracticed,
        notes = notes
    )

    fun BookProgress.toEntity(): BookProgressEntity = BookProgressEntity(
        id = id,
        userId = userId,
        chapter = chapter,
        lesson = lesson,
        status = status.name,
        score = score,
        startedAt = startedAt,
        completedAt = completedAt,
        timesPracticed = timesPracticed,
        notes = notes
    )

    // === PronunciationRecord ↔ PronunciationResult ===

    fun PronunciationRecordEntity.toDomain(json: Json): PronunciationResult {
        val sounds = try {
            json.decodeFromString<List<String>>(problemSoundsJson)
        } catch (e: Exception) {
            emptyList()
        }
        return PronunciationResult(
            id = id,
            userId = userId,
            word = word,
            score = score,
            problemSounds = sounds,
            attemptNumber = attemptNumber,
            sessionId = sessionId,
            timestamp = timestamp,
            createdAt = createdAt
        )
    }

    fun PronunciationResult.toEntity(json: Json): PronunciationRecordEntity =
        PronunciationRecordEntity(
            id = id,
            userId = userId,
            word = word,
            score = score,
            problemSoundsJson = json.encodeToString(problemSounds),
            attemptNumber = attemptNumber,
            sessionId = sessionId,
            timestamp = timestamp,
            createdAt = createdAt
        )

    // === MistakeLog ↔ MistakeLog ===

    fun MistakeLogEntity.toDomain(): MistakeLog = MistakeLog(
        id = id,
        userId = userId,
        sessionId = sessionId,
        type = try {
            MistakeType.valueOf(type.uppercase())
        } catch (e: Exception) {
            MistakeType.WORD
        },
        item = item,
        expected = expected,
        actual = actual,
        context = context,
        explanation = explanation,
        timestamp = timestamp,
        createdAt = createdAt
    )

    fun MistakeLog.toEntity(): MistakeLogEntity = MistakeLogEntity(
        id = id,
        userId = userId,
        sessionId = sessionId,
        type = type.name,
        item = item,
        expected = expected,
        actual = actual,
        context = context,
        explanation = explanation,
        timestamp = timestamp,
        createdAt = createdAt
    )
}