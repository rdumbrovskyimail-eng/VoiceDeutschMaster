package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.PhraseKnowledge
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.util.Constants
import com.voicedeutsch.master.util.DateUtils
import com.voicedeutsch.master.util.generateUUID

/**
 * Updates user's knowledge of a specific phrase after practice.
 * Implements Modified SM-2 algorithm via [SrsCalculator].
 *
 * Called via Function Call: save_phrase_knowledge()
 */
class UpdatePhraseKnowledgeUseCase(
    private val knowledgeRepository: KnowledgeRepository
) {

    data class Params(
        val userId: String,
        val phraseId: String,
        val newLevel: Int,
        val quality: Int,
        val pronunciationScore: Float? = null
    )

    suspend operator fun invoke(params: Params) {
        val existing = knowledgeRepository.getPhraseKnowledge(params.userId, params.phraseId)

        val phrase = knowledgeRepository.getPhrase(params.phraseId) ?: return

        val now = DateUtils.nowTimestamp()
        val quality = params.quality.coerceIn(0, 5)

        if (existing == null) {
            createNewPhraseKnowledge(params, now, quality)
        } else {
            updateExistingPhraseKnowledge(existing, params, now, quality)
        }
    }

    private suspend fun createNewPhraseKnowledge(
        params: Params,
        now: Long,
        quality: Int
    ) {
        val defaultEF = Constants.SRS_DEFAULT_EASE_FACTOR
        val newEaseFactor = SrsCalculator.calculateEaseFactor(defaultEF, quality)
        val newInterval = SrsCalculator.calculateInterval(0, quality, newEaseFactor, 0f)
        val nextReview = SrsCalculator.calculateNextReview(now, 0, quality, newEaseFactor, 0f)

        val newKnowledge = PhraseKnowledge(
            id = generateUUID(),
            userId = params.userId,
            phraseId = params.phraseId,
            knowledgeLevel = params.newLevel.coerceIn(0, 7),
            timesPracticed = 1,
            timesCorrect = if (quality >= 3) 1 else 0,
            lastPracticed = now,
            nextReview = nextReview,
            srsIntervalDays = newInterval,
            srsEaseFactor = newEaseFactor,
            pronunciationScore = params.pronunciationScore ?: 0f,
            createdAt = now,
            updatedAt = now
        )

        knowledgeRepository.upsertPhraseKnowledge(newKnowledge)
    }

    private suspend fun updateExistingPhraseKnowledge(
        existing: PhraseKnowledge,
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
            val prevAttempts = existing.timesPracticed
            val totalAttempts = prevAttempts + 1
            val prevTotal = existing.pronunciationScore * prevAttempts
            if (totalAttempts > 0) (prevTotal + params.pronunciationScore) / totalAttempts else params.pronunciationScore
        } else {
            existing.pronunciationScore
        }

        val updated = existing.copy(
            knowledgeLevel = adjustedLevel,
            timesPracticed = existing.timesPracticed + 1,
            timesCorrect = existing.timesCorrect + if (quality >= 3) 1 else 0,
            lastPracticed = now,
            nextReview = nextReview,
            srsIntervalDays = newInterval,
            srsEaseFactor = newEaseFactor,
            pronunciationScore = newPronScore,
            updatedAt = now
        )

        knowledgeRepository.upsertPhraseKnowledge(updated)
    }
}