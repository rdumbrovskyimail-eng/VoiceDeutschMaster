package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.Word
import com.voicedeutsch.master.domain.model.knowledge.WordKnowledge
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.util.Constants
import com.voicedeutsch.master.util.DateUtils

/**
 * Gets prioritized queue of words for SRS review.
 *
 * Priority levels:
 *   CRITICAL:   level <= 2 AND overdue > 3 days
 *   IMPORTANT:  level 3-4
 *   SUPPORTING: level 5-6
 *   MASTERY:    level 7
 *
 * Sorted by priority ASC, overdue DESC.
 * Default limit: 15 words per session.
 */
class GetWordsForRepetitionUseCase(
    private val knowledgeRepository: KnowledgeRepository
) {

    data class ReviewItem(
        val word: Word,
        val knowledge: WordKnowledge,
        val priority: ReviewPriority,
        val overdueDays: Long
    )

    enum class ReviewPriority { CRITICAL, IMPORTANT, SUPPORTING, MASTERY }

    suspend operator fun invoke(
        userId: String,
        limit: Int = Constants.SRS_MAX_REVIEWS_PER_SESSION
    ): List<ReviewItem> {
        val wordsForReview = knowledgeRepository.getWordsForReview(userId, limit * 2)

        val prioritized = wordsForReview.map { (word, knowledge) ->
            val overdue = knowledge.nextReview?.let { nr ->
                DateUtils.overdueDays(nr)
            } ?: 0L

            val priority = when {
                knowledge.knowledgeLevel <= 2 && overdue > 3 -> ReviewPriority.CRITICAL
                knowledge.knowledgeLevel in 3..4 -> ReviewPriority.IMPORTANT
                knowledge.knowledgeLevel in 5..6 -> ReviewPriority.SUPPORTING
                else -> ReviewPriority.MASTERY
            }

            ReviewItem(word, knowledge, priority, overdue)
        }

        val sorted = prioritized.sortedWith(
            compareBy<ReviewItem> { it.priority.ordinal }
                .thenByDescending { it.overdueDays }
        )

        return sorted.take(limit)
    }
}