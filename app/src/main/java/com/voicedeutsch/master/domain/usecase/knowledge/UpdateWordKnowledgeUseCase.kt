package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.MistakeRecord
import com.voicedeutsch.master.domain.model.knowledge.WordKnowledge
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.util.Constants
import com.voicedeutsch.master.util.DateUtils
import com.voicedeutsch.master.util.generateUUID

/**
 * Updates user's knowledge of a specific word after an exercise.
 * Implements Modified SM-2 algorithm via [SrsCalculator].
 *
 * Called via Function Call: save_word_knowledge()
 */
class UpdateWordKnowledgeUseCase(
    private val knowledgeRepository: KnowledgeRepository
) {

    data class Params(
        val userId: String,
        val wordGerman: String,
        val translation: String,
        val newLevel: Int,
        val quality: Int,
        val pronunciationScore: Float? = null,
        val context: String? = null,
        val mistakeExpected: String? = null,
        val mistakeActual: String? = null
    )

    suspend operator fun invoke(params: Params) {
        val existing = knowledgeRepository.getWordKnowledgeByGerman(
            params.userId, params.wordGerman
        )

        val word = knowledgeRepository.getWordByGerman(params.wordGerman)
        val wordId = word?.id ?: return

        val now = DateUtils.nowTimestamp()
        val quality = params.quality.coerceIn(0, 5)

        if (existing == null) {
            createNewWordKnowledge(params, wordId, now, quality)
        } else {
            updateExistingWordKnowledge(existing, params, now, quality)
        }
    }

    private suspend fun createNewWordKnowledge(
        params: Params,
        wordId: String,
        now: Long,
        quality: Int
    ) {
        val defaultEF = Constants.SRS_DEFAULT_EASE_FACTOR
        val newEaseFactor = SrsCalculator.calculateEaseFactor(defaultEF, quality)
        val newInterval = SrsCalculator.calculateInterval(0, quality, newEaseFactor, 0f)
        val nextReview = SrsCalculator.calculateNextReview(now, 0, quality, newEaseFactor, 0f)

        val newKnowledge = WordKnowledge(
            id = generateUUID(),
            userId = params.userId,
            wordId = wordId,
            knowledgeLevel = params.newLevel.coerceIn(0, 7),
            timesSeen = 1,
            timesCorrect = if (quality >= 3) 1 else 0,
            timesIncorrect = if (quality < 3) 1 else 0,
            lastSeen = now,
            lastCorrect = if (quality >= 3) now else null,
            lastIncorrect = if (quality < 3) now else null,
            nextReview = nextReview,
            srsIntervalDays = newInterval,
            srsEaseFactor = newEaseFactor,
            pronunciationScore = params.pronunciationScore ?: 0f,
            pronunciationAttempts = if (params.pronunciationScore != null) 1 else 0,
            contexts = listOfNotNull(params.context),
            mistakes = buildMistakesList(
                emptyList(), params.mistakeExpected, params.mistakeActual, now
            ),
            createdAt = now,
            updatedAt = now
        )

        knowledgeRepository.upsertWordKnowledge(newKnowledge)
    }

    private suspend fun updateExistingWordKnowledge(
        existing: WordKnowledge,
        params: Params,
        now: Long,
        quality: Int
    ) {
        val repetitionNumber = SrsCalculator.calculateRepetitionNumber(
            existing.timesCorrect, quality
        )

        val newEaseFactor = SrsCalculator.calculateEaseFactor(existing.srsEaseFactor, quality)
        val newInterval = SrsCalculator.calculateInterval(
            repetitionNumber, quality, newEaseFactor, existing.srsIntervalDays
        )
        val nextReview = SrsCalculator.calculateNextReview(
            now, repetitionNumber, quality, newEaseFactor, existing.srsIntervalDays
        )

        val adjustedLevel = SrsCalculator.calculateKnowledgeLevel(
            existing.knowledgeLevel, quality, params.newLevel
        )

        val newPronScore = if (params.pronunciationScore != null) {
            val totalAttempts = existing.pronunciationAttempts + 1
            val prevTotal = existing.pronunciationScore * existing.pronunciationAttempts
            (prevTotal + params.pronunciationScore) / totalAttempts
        } else {
            existing.pronunciationScore
        }

        val newPronAttempts = existing.pronunciationAttempts +
            if (params.pronunciationScore != null) 1 else 0

        val updatedContexts = (existing.contexts + listOfNotNull(params.context))
            .distinct()
            .takeLast(10)

        val updatedMistakes = buildMistakesList(
            existing.mistakes, params.mistakeExpected, params.mistakeActual, now
        )

        val updated = existing.copy(
            knowledgeLevel = adjustedLevel,
            timesSeen = existing.timesSeen + 1,
            timesCorrect = existing.timesCorrect + if (quality >= 3) 1 else 0,
            timesIncorrect = existing.timesIncorrect + if (quality < 3) 1 else 0,
            lastSeen = now,
            lastCorrect = if (quality >= 3) now else existing.lastCorrect,
            lastIncorrect = if (quality < 3) now else existing.lastIncorrect,
            nextReview = nextReview,
            srsIntervalDays = newInterval,
            srsEaseFactor = newEaseFactor,
            pronunciationScore = newPronScore,
            pronunciationAttempts = newPronAttempts,
            contexts = updatedContexts,
            mistakes = updatedMistakes,
            updatedAt = now
        )

        knowledgeRepository.upsertWordKnowledge(updated)
    }

    private fun buildMistakesList(
        existing: List<MistakeRecord>,
        expected: String?,
        actual: String?,
        timestamp: Long
    ): List<MistakeRecord> {
        if (expected == null || actual == null) return existing
        val newMistake = MistakeRecord(
            expected = expected,
            actual = actual,
            timestamp = timestamp
        )
        return (existing + newMistake).takeLast(20)
    }
}