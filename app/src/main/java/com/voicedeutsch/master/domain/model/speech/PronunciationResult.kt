package com.voicedeutsch.master.domain.model.speech

import kotlinx.serialization.Serializable

/**
 * The result of evaluating a user's pronunciation of a word or phrase.
 */
@Serializable
data class PronunciationResult(
    val id: String,
    val userId: String,
    val word: String,
    val score: Float,                        // 0.0 - 1.0
    val problemSounds: List<String> = emptyList(),
    val attemptNumber: Int = 1,
    val sessionId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Detailed speech analysis with sub-scores.
 *
 * **Weighted score formula:**
 * `0.3 × intelligibility + 0.3 × segmental + 0.15 × stress + 0.15 × intonation + 0.1 × fluency`
 */
@Serializable
data class SpeechAnalysis(
    val intelligibility: Float = 0f,          // 0.0 - 1.0
    val segmentalAccuracy: Float = 0f,        // 0.0 - 1.0
    val stressCorrect: Boolean = true,
    val intonation: Float = 0f,               // 0.0 - 1.0
    val fluency: Float = 0f                   // 0.0 - 1.0
) {
    /**
     * Weighted overall pronunciation score.
     *
     * Weights:
     * - intelligibility: 30 %
     * - segmental accuracy: 30 %
     * - stress correctness: 15 % (binary: 1.0 or 0.0)
     * - intonation: 15 %
     * - fluency: 10 %
     */
    val overallScore: Float get() =
        0.3f * intelligibility +
        0.3f * segmentalAccuracy +
        0.15f * (if (stressCorrect) 1f else 0f) +
        0.15f * intonation +
        0.1f * fluency
}

/**
 * A specific phonetic sound that the user struggles with.
 *
 * Tracked over time to measure improvement or regression.
 *
 * **Weak point detection:**
 * - score < 0.5 after > 5 attempts, trend STABLE or DECLINING
 */
@Serializable
data class PhoneticTarget(
    val sound: String,                        // e.g., "ü"
    val ipa: String,                          // e.g., "[y]"
    val detectionDate: Long,
    val totalAttempts: Int = 0,
    val successfulAttempts: Int = 0,
    val currentScore: Float = 0f,
    val trend: PronunciationTrend = PronunciationTrend.STABLE,
    val lastPracticed: Long? = null,
    val inWords: List<String> = emptyList()
)

/**
 * Direction of a pronunciation metric over recent attempts.
 */
@Serializable
enum class PronunciationTrend { IMPROVING, STABLE, DECLINING }