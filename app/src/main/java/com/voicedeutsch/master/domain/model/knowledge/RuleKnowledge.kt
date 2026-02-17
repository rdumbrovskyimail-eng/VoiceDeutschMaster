package com.voicedeutsch.master.domain.model.knowledge

import kotlinx.serialization.Serializable

/**
 * Tracks a user's knowledge of a specific grammar rule — SRS state and mistake history.
 *
 * Parallels [WordKnowledge] but for grammar rules.
 *
 * **Weak point detection criteria:**
 * - knowledge_level ≤ 2 AND times_practiced ≥ 3
 */
@Serializable
data class RuleKnowledge(
    val id: String,
    val userId: String,
    val ruleId: String,
    val knowledgeLevel: Int = 0,           // 0-7
    val timesPracticed: Int = 0,
    val timesCorrect: Int = 0,
    val timesIncorrect: Int = 0,
    val lastPracticed: Long? = null,
    val nextReview: Long? = null,
    val srsIntervalDays: Float = 0f,
    val srsEaseFactor: Float = 2.5f,
    val commonMistakes: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** The rule is considered "known" at level 4 or above. */
    val isKnown: Boolean get() = knowledgeLevel >= 4

    /** Returns `true` when the rule is due for SRS review. */
    val needsReview: Boolean get() = nextReview != null && nextReview <= System.currentTimeMillis()

    /** Accuracy ratio (correct / practiced). Returns 0 when never practiced. */
    val accuracy: Float get() = if (timesPracticed == 0) 0f else timesCorrect.toFloat() / timesPracticed
}