package com.voicedeutsch.master.domain.model.knowledge

import com.voicedeutsch.master.domain.model.user.CefrLevel
import kotlinx.serialization.Serializable

/**
 * Domain model of a German phrase or fixed expression.
 */
@Serializable
data class Phrase(
    val id: String,
    val german: String,
    val russian: String,
    val category: PhraseCategory,
    val difficultyLevel: CefrLevel,
    val bookChapter: Int? = null,
    val bookLesson: Int? = null,
    val context: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Thematic categories for phrases.
 */
@Serializable
enum class PhraseCategory {
    GREETING, FAREWELL, TRAVEL, WORK, DAILY, SHOPPING,
    RESTAURANT, HEALTH, EMERGENCY, POLITE, OPINION, OTHER
}

/**
 * Tracks a user's knowledge of a specific phrase — SRS state and pronunciation score.
 */
@Serializable
data class PhraseKnowledge(
    val id: String,
    val userId: String,
    val phraseId: String,
    val knowledgeLevel: Int = 0,
    val timesPracticed: Int = 0,
    val timesCorrect: Int = 0,
    val lastPracticed: Long? = null,
    val nextReview: Long? = null,
    val srsIntervalDays: Float = 0f,
    val srsEaseFactor: Float = 2.5f,
    val pronunciationScore: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Returns `true` when the phrase is due for SRS review. */
    val needsReview: Boolean get() = nextReview != null && nextReview <= System.currentTimeMillis()
}