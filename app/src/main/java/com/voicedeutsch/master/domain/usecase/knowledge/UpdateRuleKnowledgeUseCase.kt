package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.RuleKnowledge
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.util.Constants
import com.voicedeutsch.master.util.DateUtils
import com.voicedeutsch.master.util.generateUUID

/**
 * Updates user's knowledge of a specific grammar rule after practice.
 * Implements Modified SM-2 algorithm via [SrsCalculator].
 *
 * Called via Function Call: save_rule_knowledge()
 */
class UpdateRuleKnowledgeUseCase(
    private val knowledgeRepository: KnowledgeRepository
) {

    data class Params(
        val userId: String,
        val ruleId: String,
        val newLevel: Int,
        val quality: Int,
        val mistakeDescription: String? = null
    )

    suspend operator fun invoke(params: Params) {
        val existing = knowledgeRepository.getRuleKnowledge(params.userId, params.ruleId)

        val rule = knowledgeRepository.getGrammarRule(params.ruleId) ?: return

        val now = DateUtils.nowTimestamp()
        val quality = params.quality.coerceIn(0, 5)

        if (existing == null) {
            createNewRuleKnowledge(params, now, quality)
        } else {
            updateExistingRuleKnowledge(existing, params, now, quality)
        }
    }

    private suspend fun createNewRuleKnowledge(
        params: Params,
        now: Long,
        quality: Int
    ) {
        val defaultEF = Constants.SRS_DEFAULT_EASE_FACTOR
        val newEaseFactor = SrsCalculator.calculateEaseFactor(defaultEF, quality)
        val newInterval = SrsCalculator.calculateInterval(0, quality, newEaseFactor, 0f)
        val nextReview = SrsCalculator.calculateNextReview(now, 0, quality, newEaseFactor, 0f)

        val newKnowledge = RuleKnowledge(
            id = generateUUID(),
            userId = params.userId,
            ruleId = params.ruleId,
            knowledgeLevel = params.newLevel.coerceIn(0, 7),
            timesPracticed = 1,
            timesCorrect = if (quality >= 3) 1 else 0,
            timesIncorrect = if (quality < 3) 1 else 0,
            lastPracticed = now,
            nextReview = nextReview,
            srsIntervalDays = newInterval,
            srsEaseFactor = newEaseFactor,
            commonMistakes = listOfNotNull(params.mistakeDescription),
            createdAt = now,
            updatedAt = now
        )

        knowledgeRepository.upsertRuleKnowledge(newKnowledge)
    }

    private suspend fun updateExistingRuleKnowledge(
        existing: RuleKnowledge,
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

        val updatedMistakes = if (params.mistakeDescription != null) {
            (existing.commonMistakes + params.mistakeDescription).distinct().takeLast(15)
        } else {
            existing.commonMistakes
        }

        val updated = existing.copy(
            knowledgeLevel = adjustedLevel,
            timesPracticed = existing.timesPracticed + 1,
            timesCorrect = existing.timesCorrect + if (quality >= 3) 1 else 0,
            timesIncorrect = existing.timesIncorrect + if (quality < 3) 1 else 0,
            lastPracticed = now,
            nextReview = nextReview,
            srsIntervalDays = newInterval,
            srsEaseFactor = newEaseFactor,
            commonMistakes = updatedMistakes,
            updatedAt = now
        )

        knowledgeRepository.upsertRuleKnowledge(updated)
    }
}