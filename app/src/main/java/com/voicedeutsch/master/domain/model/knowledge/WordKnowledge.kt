package com.voicedeutsch.master.domain.model.knowledge

import kotlinx.serialization.Serializable

/**
 * Tracks a user's knowledge of a specific word — SRS state, scores, and error history.
 *
 * This is the primary model of the SRS system. It stores the knowledge level (0–7),
 * view/correct/incorrect counts, SRS interval, ease factor, next review timestamp,
 * pronunciation score, usage contexts, and mistake records.
 *
 * This model is continuously updated during learning sessions.
 *
 * **Mappings:**
 * - Maps from/to `WordKnowledgeEntity`
 * - Linked to [Word] via [wordId]
 * - Linked to user via [userId]
 * - Updated through `UpdateWordKnowledgeUseCase`
 * - Included in [KnowledgeSnapshot] for Gemini context
 *
 * **SRS SM-2 algorithm parameters:**
 * - quality 0–5
 * - ease_factor: max(1.3, old_ef + (0.1 − (5−q) × (0.08 + (5−q) × 0.02)))
 * - interval: q<3 → 0.5 day, q≥3 ∧ n=1 → 1 day, n=2 → 3 days, n>2 → old × ef
 * - boost: 3× consecutive quality=5 → interval × 1.5
 */
@Serializable
data class WordKnowledge(
    val id: String,
    val userId: String,
    val wordId: String,
    val knowledgeLevel: Int = 0,           // 0-7
    val timesSeen: Int = 0,
    val timesCorrect: Int = 0,
    val timesIncorrect: Int = 0,
    val lastSeen: Long? = null,
    val lastCorrect: Long? = null,
    val lastIncorrect: Long? = null,
    val nextReview: Long? = null,          // SRS next review timestamp
    val srsIntervalDays: Float = 0f,       // Current SRS interval
    val srsEaseFactor: Float = 2.5f,       // SM-2 ease factor
    val pronunciationScore: Float = 0f,     // 0.0 - 1.0
    val pronunciationAttempts: Int = 0,
    val contexts: List<String> = emptyList(), // Usage contexts
    val mistakes: List<MistakeRecord> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** The word is considered "known" at level 4 or above. */
    val isKnown: Boolean get() = knowledgeLevel >= 4

    /** The word is "active" (can be used freely) at level 5+. */
    val isActive: Boolean get() = knowledgeLevel >= 5

    /** The word is fully mastered at level 7. */
    val isMastered: Boolean get() = knowledgeLevel >= 7

    /** Returns `true` when the word is due for SRS review. */
    val needsReview: Boolean get() = nextReview != null && nextReview <= System.currentTimeMillis()

    /** Accuracy ratio (correct / seen). Returns 0 when never seen. */
    val accuracy: Float get() = if (timesSeen == 0) 0f else timesCorrect.toFloat() / timesSeen

    /** A "problem word" has more errors than successes after at least 3 encounters. */
    val isProblemWord: Boolean get() = timesIncorrect > timesCorrect && timesSeen >= 3
}

/**
 * A single recorded mistake for a word.
 */
@Serializable
data class MistakeRecord(
    val expected: String,
    val actual: String,
    val timestamp: Long,
    val context: String = ""
)