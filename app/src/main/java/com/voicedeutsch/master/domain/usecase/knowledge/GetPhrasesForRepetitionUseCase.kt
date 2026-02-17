package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.Phrase
import com.voicedeutsch.master.domain.model.knowledge.PhraseKnowledge
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.util.DateUtils

/**
 * Gets prioritized queue of phrases for SRS review.
 *
 * Priority levels:
 *   CRITICAL:   level <= 2 AND overdue > 3 days
 *   IMPORTANT:  level 3-4
 *   SUPPORTING: level 5-6
 *   MASTERY:    level 7
 *
 * Default limit: 5 phrases per session.
 */
class GetPhrasesForRepetitionUseCase(
    private val knowledgeRepository: KnowledgeRepository
) {

    data class PhraseReviewItem(
        val phrase: Phrase,
        val knowledge: PhraseKnowledge,
        val priority: ReviewPriority,
        val overdueDays: Long
    )

    enum class ReviewPriority { CRITICAL, IMPORTANT, SUPPORTING, MASTERY }

    companion object {
        private const val DEFAULT_PHRASES_LIMIT = 5
    }

    suspend operator fun invoke(
        userId: String,
        limit: Int = DEFAULT_PHRASES_LIMIT
    ): List<PhraseReviewItem> {
        val phrasesForReview = knowledgeRepository.getPhrasesForReview(userId, limit * 2)

        val prioritized = phrasesForReview.map { (phrase, knowledge) ->
            val overdue = knowledge.nextReview?.let { nr ->
                DateUtils.overdueDays(nr)
            } ?: 0L

            val priority = when {
                knowledge.knowledgeLevel <= 2 && overdue > 3 -> ReviewPriority.CRITICAL
                knowledge.knowledgeLevel in 3..4 -> ReviewPriority.IMPORTANT
                knowledge.knowledgeLevel in 5..6 -> ReviewPriority.SUPPORTING
                else -> ReviewPriority.MASTERY
            }

            PhraseReviewItem(phrase, knowledge, priority, overdue)
        }

        val sorted = prioritized.sortedWith(
            compareBy<PhraseReviewItem> { it.priority.ordinal }
                .thenByDescending { it.overdueDays }
        )

        return sorted.take(limit)
    }
}