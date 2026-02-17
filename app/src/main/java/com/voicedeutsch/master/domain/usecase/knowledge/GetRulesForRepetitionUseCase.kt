package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.GrammarRule
import com.voicedeutsch.master.domain.model.knowledge.RuleKnowledge
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.util.DateUtils

/**
 * Gets prioritized queue of grammar rules for SRS review.
 *
 * Priority levels mirror [GetWordsForRepetitionUseCase.ReviewPriority]:
 *   CRITICAL:   level <= 2 AND overdue > 3 days
 *   IMPORTANT:  level 3-4
 *   SUPPORTING: level 5-6
 *   MASTERY:    level 7
 *
 * Default limit: 10 rules per session.
 */
class GetRulesForRepetitionUseCase(
    private val knowledgeRepository: KnowledgeRepository
) {

    data class RuleReviewItem(
        val rule: GrammarRule,
        val knowledge: RuleKnowledge,
        val priority: ReviewPriority,
        val overdueDays: Long
    )

    enum class ReviewPriority { CRITICAL, IMPORTANT, SUPPORTING, MASTERY }

    companion object {
        private const val DEFAULT_RULES_LIMIT = 10
    }

    suspend operator fun invoke(
        userId: String,
        limit: Int = DEFAULT_RULES_LIMIT
    ): List<RuleReviewItem> {
        val rulesForReview = knowledgeRepository.getRulesForReview(userId, limit * 2)

        val prioritized = rulesForReview.map { (rule, knowledge) ->
            val overdue = knowledge.nextReview?.let { nr ->
                DateUtils.overdueDays(nr)
            } ?: 0L

            val priority = when {
                knowledge.knowledgeLevel <= 2 && overdue > 3 -> ReviewPriority.CRITICAL
                knowledge.knowledgeLevel in 3..4 -> ReviewPriority.IMPORTANT
                knowledge.knowledgeLevel in 5..6 -> ReviewPriority.SUPPORTING
                else -> ReviewPriority.MASTERY
            }

            RuleReviewItem(rule, knowledge, priority, overdue)
        }

        val sorted = prioritized.sortedWith(
            compareBy<RuleReviewItem> { it.priority.ordinal }
                .thenByDescending { it.overdueDays }
        )

        return sorted.take(limit)
    }
}