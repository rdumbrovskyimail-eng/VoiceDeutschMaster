package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.util.Constants
import com.voicedeutsch.master.util.DateUtils
import kotlin.math.max
import kotlin.math.min

/**
 * Shared SM-2 Spaced Repetition calculator.
 * Implements the Modified SM-2 algorithm.
 *
 * Quality scale:
 *   0 = complete failure, no recall
 *   1 = incorrect, but recognized after seeing answer
 *   2 = incorrect, but answer felt familiar
 *   3 = correct with significant difficulty
 *   4 = correct with minor hesitation
 *   5 = instant perfect recall
 *
 * Interval progression:
 *   quality < 3  → reset to 0.5 days (failed)
 *   quality >= 3, rep 0 → 1 day
 *   quality >= 3, rep 1 → 3 days
 *   quality >= 3, rep > 1 → previous_interval * ease_factor
 *
 * Ease factor:
 *   new_ef = old_ef + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
 *   minimum = 1.3
 *
 * Knowledge level:
 *   q = 5 → +1 (max 7)
 *   q = 4 → max(current, suggested) (max 7)
 *   q = 3 → no change
 *   q <= 2 → -1 (min 0)
 */
object SrsCalculator {

    /**
     * Calculates the next review interval in days.
     *
     * @param repetition The number of consecutive correct answers (0-based).
     *                   Reset to 0 on failure.
     * @param quality SM-2 quality rating (0-5).
     * @param easeFactor Current ease factor (>= 1.3).
     * @param previousInterval Previous interval in days.
     * @return New interval in days.
     */
    fun calculateInterval(
        repetition: Int,
        quality: Int,
        easeFactor: Float,
        previousInterval: Float
    ): Float {
        if (quality < 3) {
            return Constants.SRS_FAILED_INTERVAL_DAYS
        }
        return when (repetition) {
            0 -> Constants.SRS_INITIAL_INTERVAL_DAYS
            1 -> Constants.SRS_SECOND_INTERVAL_DAYS
            else -> previousInterval * easeFactor
        }
    }

    /**
     * Calculates the new ease factor based on quality.
     *
     * @param currentEF Current ease factor.
     * @param quality SM-2 quality rating (0-5).
     * @return New ease factor (>= 1.3).
     */
    fun calculateEaseFactor(currentEF: Float, quality: Int): Float {
        val q = quality.coerceIn(0, 5)
        val newEF = currentEF + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        return max(Constants.SRS_MIN_EASE_FACTOR, newEF)
    }

    /**
     * Calculates the timestamp for the next review.
     *
     * @param now Current timestamp in milliseconds.
     * @param repetition Consecutive correct answers count.
     * @param quality SM-2 quality rating (0-5).
     * @param easeFactor Current ease factor.
     * @param previousInterval Previous interval in days.
     * @return Timestamp in milliseconds for next review.
     */
    fun calculateNextReview(
        now: Long,
        repetition: Int,
        quality: Int,
        easeFactor: Float,
        previousInterval: Float
    ): Long {
        val interval = calculateInterval(repetition, quality, easeFactor, previousInterval)
        return DateUtils.addDaysToTimestamp(now, interval)
    }

    /**
     * Adjusts knowledge level based on quality and suggested level.
     *
     * @param currentLevel Current knowledge level (0-7).
     * @param quality SM-2 quality rating (0-5).
     * @param suggestedLevel Level suggested by Gemini evaluation.
     * @return New knowledge level (0-7).
     */
    fun calculateKnowledgeLevel(
        currentLevel: Int,
        quality: Int,
        suggestedLevel: Int
    ): Int {
        return when {
            quality == 5 -> min(max(currentLevel, suggestedLevel) + 1, 7)
            quality == 4 -> min(max(currentLevel, suggestedLevel), 7)
            quality == 3 -> currentLevel
            quality <= 2 -> max(currentLevel - 1, 0)
            else -> currentLevel
        }
    }

    /**
     * Determines the repetition number after an answer.
     * Resets to 0 on failure (quality < 3).
     *
     * @param previousCorrectCount Number of previous consecutive correct answers.
     * @param quality SM-2 quality rating (0-5).
     * @return New repetition number.
     */
    fun calculateRepetitionNumber(
        previousCorrectCount: Int,
        quality: Int
    ): Int {
        return if (quality >= 3) {
            previousCorrectCount + 1
        } else {
            0
        }
    }

    /**
     * Checks if a streak bonus should be applied.
     * Bonus: if 3+ consecutive quality=5 answers, interval * 1.5
     *
     * @param consecutivePerfect Number of consecutive quality=5 answers.
     * @param interval Calculated interval.
     * @return Adjusted interval with streak bonus if applicable.
     */
    fun applyStreakBonus(
        consecutivePerfect: Int,
        interval: Float
    ): Float {
        return if (consecutivePerfect >= 3) {
            interval * 1.5f
        } else {
            interval
        }
    }
}