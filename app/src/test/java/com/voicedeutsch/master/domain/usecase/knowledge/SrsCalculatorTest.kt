// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/knowledge/SrsCalculatorTest.kt
package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.util.Constants
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SrsCalculatorTest {

    // ── calculateInterval ────────────────────────────────────────────────

    @Test
    fun calculateInterval_qualityBelow3_returnsFailedInterval() {
        val result = SrsCalculator.calculateInterval(
            repetition = 2,
            quality = 2,
            easeFactor = 2.5f,
            previousInterval = 10f
        )
        assertEquals(Constants.SRS_FAILED_INTERVAL_DAYS, result)
    }

    @Test
    fun calculateInterval_qualityZero_returnsFailedInterval() {
        val result = SrsCalculator.calculateInterval(
            repetition = 5,
            quality = 0,
            easeFactor = 2.5f,
            previousInterval = 20f
        )
        assertEquals(Constants.SRS_FAILED_INTERVAL_DAYS, result)
    }

    @Test
    fun calculateInterval_qualityExactly2_returnsFailedInterval() {
        val result = SrsCalculator.calculateInterval(
            repetition = 1,
            quality = 2,
            easeFactor = 2.5f,
            previousInterval = 5f
        )
        assertEquals(Constants.SRS_FAILED_INTERVAL_DAYS, result)
    }

    @Test
    fun calculateInterval_quality3Rep0_returnsInitialInterval() {
        val result = SrsCalculator.calculateInterval(
            repetition = 0,
            quality = 3,
            easeFactor = 2.5f,
            previousInterval = 0f
        )
        assertEquals(Constants.SRS_INITIAL_INTERVAL_DAYS, result)
    }

    @Test
    fun calculateInterval_quality5Rep0_returnsInitialInterval() {
        val result = SrsCalculator.calculateInterval(
            repetition = 0,
            quality = 5,
            easeFactor = 2.5f,
            previousInterval = 0f
        )
        assertEquals(Constants.SRS_INITIAL_INTERVAL_DAYS, result)
    }

    @Test
    fun calculateInterval_quality3Rep1_returnsSecondInterval() {
        val result = SrsCalculator.calculateInterval(
            repetition = 1,
            quality = 3,
            easeFactor = 2.5f,
            previousInterval = 1f
        )
        assertEquals(Constants.SRS_SECOND_INTERVAL_DAYS, result)
    }

    @Test
    fun calculateInterval_quality5Rep1_returnsSecondInterval() {
        val result = SrsCalculator.calculateInterval(
            repetition = 1,
            quality = 5,
            easeFactor = 2.5f,
            previousInterval = 1f
        )
        assertEquals(Constants.SRS_SECOND_INTERVAL_DAYS, result)
    }

    @Test
    fun calculateInterval_quality3Rep2_returnsPreviousIntervalTimesEaseFactor() {
        val previousInterval = 6f
        val easeFactor = 2.5f
        val result = SrsCalculator.calculateInterval(
            repetition = 2,
            quality = 3,
            easeFactor = easeFactor,
            previousInterval = previousInterval
        )
        assertEquals(previousInterval * easeFactor, result, 0.001f)
    }

    @Test
    fun calculateInterval_quality5RepMany_returnsPreviousIntervalTimesEaseFactor() {
        val previousInterval = 15f
        val easeFactor = 2.1f
        val result = SrsCalculator.calculateInterval(
            repetition = 10,
            quality = 5,
            easeFactor = easeFactor,
            previousInterval = previousInterval
        )
        assertEquals(previousInterval * easeFactor, result, 0.001f)
    }

    @Test
    fun calculateInterval_qualityExactly3_isNotFailed() {
        val result = SrsCalculator.calculateInterval(
            repetition = 0,
            quality = 3,
            easeFactor = 2.5f,
            previousInterval = 0f
        )
        assertNotEquals(Constants.SRS_FAILED_INTERVAL_DAYS, result)
    }

    // ── calculateEaseFactor ──────────────────────────────────────────────

    @Test
    fun calculateEaseFactor_quality5_increasesEaseFactor() {
        val currentEF = 2.5f
        val result = SrsCalculator.calculateEaseFactor(currentEF, quality = 5)
        assertTrue(result > currentEF)
    }

    @Test
    fun calculateEaseFactor_quality3_slightlyDecreasesEaseFactor() {
        val currentEF = 2.5f
        val result = SrsCalculator.calculateEaseFactor(currentEF, quality = 3)
        // q=3: 0.1 - (5-3)*(0.08 + (5-3)*0.02) = 0.1 - 2*(0.12) = 0.1 - 0.24 = -0.14
        val expected = currentEF - 0.14f
        assertEquals(expected, result, 0.001f)
    }

    @Test
    fun calculateEaseFactor_quality0_doesNotGoBelowMinimum() {
        val result = SrsCalculator.calculateEaseFactor(1.3f, quality = 0)
        assertEquals(Constants.SRS_MIN_EASE_FACTOR, result)
    }

    @Test
    fun calculateEaseFactor_qualityLow_clampedToMinimum() {
        val result = SrsCalculator.calculateEaseFactor(1.4f, quality = 0)
        assertTrue(result >= Constants.SRS_MIN_EASE_FACTOR)
    }

    @Test
    fun calculateEaseFactor_quality5_correctFormula() {
        val currentEF = 2.5f
        // q=5: 0.1 - (5-5)*(0.08+(5-5)*0.02) = 0.1 - 0 = 0.1
        val expected = currentEF + 0.1f
        val result = SrsCalculator.calculateEaseFactor(currentEF, quality = 5)
        assertEquals(expected, result, 0.001f)
    }

    @Test
    fun calculateEaseFactor_quality4_correctFormula() {
        val currentEF = 2.5f
        // q=4: 0.1 - (1)*(0.08 + (1)*0.02) = 0.1 - 0.10 = 0.0
        val expected = currentEF + 0.0f
        val result = SrsCalculator.calculateEaseFactor(currentEF, quality = 4)
        assertEquals(expected, result, 0.001f)
    }

    @Test
    fun calculateEaseFactor_qualityAbove5_clampedTo5() {
        val currentEF = 2.5f
        val resultClamped = SrsCalculator.calculateEaseFactor(currentEF, quality = 10)
        val resultAt5 = SrsCalculator.calculateEaseFactor(currentEF, quality = 5)
        assertEquals(resultAt5, resultClamped, 0.001f)
    }

    @Test
    fun calculateEaseFactor_qualityBelow0_clampedTo0() {
        val resultClamped = SrsCalculator.calculateEaseFactor(2.5f, quality = -5)
        val resultAt0 = SrsCalculator.calculateEaseFactor(2.5f, quality = 0)
        assertEquals(resultAt0, resultClamped, 0.001f)
    }

    // ── calculateNextReview ──────────────────────────────────────────────

    @Test
    fun calculateNextReview_quality5Rep2_returnsFutureTimestamp() {
        val now = System.currentTimeMillis()
        val result = SrsCalculator.calculateNextReview(
            now = now,
            repetition = 2,
            quality = 5,
            easeFactor = 2.5f,
            previousInterval = 6f
        )
        assertTrue(result > now)
    }

    @Test
    fun calculateNextReview_qualityFailed_returnsShortFutureTimestamp() {
        val now = System.currentTimeMillis()
        val resultFailed = SrsCalculator.calculateNextReview(
            now = now,
            repetition = 5,
            quality = 1,
            easeFactor = 2.5f,
            previousInterval = 30f
        )
        val resultSuccess = SrsCalculator.calculateNextReview(
            now = now,
            repetition = 5,
            quality = 5,
            easeFactor = 2.5f,
            previousInterval = 30f
        )
        assertTrue(resultFailed < resultSuccess)
    }

    @Test
    fun calculateNextReview_rep0Quality5_usesInitialInterval() {
        val now = 1_000_000L
        val result = SrsCalculator.calculateNextReview(
            now = now,
            repetition = 0,
            quality = 5,
            easeFactor = 2.5f,
            previousInterval = 0f
        )
        assertTrue(result > now)
    }

    // ── calculateKnowledgeLevel ──────────────────────────────────────────

    @Test
    fun calculateKnowledgeLevel_quality5_incrementsByOne() {
        val result = SrsCalculator.calculateKnowledgeLevel(
            currentLevel = 3,
            quality = 5,
            suggestedLevel = 3
        )
        assertEquals(4, result)
    }

    @Test
    fun calculateKnowledgeLevel_quality5_doesNotExceedMax() {
        val result = SrsCalculator.calculateKnowledgeLevel(
            currentLevel = 7,
            quality = 5,
            suggestedLevel = 7
        )
        assertEquals(7, result)
    }

    @Test
    fun calculateKnowledgeLevel_quality5SuggestedHigher_usesMaxThenIncrements() {
        val result = SrsCalculator.calculateKnowledgeLevel(
            currentLevel = 3,
            quality = 5,
            suggestedLevel = 5
        )
        assertEquals(6, result)
    }

    @Test
    fun calculateKnowledgeLevel_quality4SuggestedHigher_returnsSuggestedLevel() {
        val result = SrsCalculator.calculateKnowledgeLevel(
            currentLevel = 2,
            quality = 4,
            suggestedLevel = 5
        )
        assertEquals(5, result)
    }

    @Test
    fun calculateKnowledgeLevel_quality4CurrentHigher_returnsCurrentLevel() {
        val result = SrsCalculator.calculateKnowledgeLevel(
            currentLevel = 6,
            quality = 4,
            suggestedLevel = 3
        )
        assertEquals(6, result)
    }

    @Test
    fun calculateKnowledgeLevel_quality4_doesNotExceedMax() {
        val result = SrsCalculator.calculateKnowledgeLevel(
            currentLevel = 7,
            quality = 4,
            suggestedLevel = 7
        )
        assertEquals(7, result)
    }

    @Test
    fun calculateKnowledgeLevel_quality3_returnsCurrentLevel() {
        val result = SrsCalculator.calculateKnowledgeLevel(
            currentLevel = 4,
            quality = 3,
            suggestedLevel = 6
        )
        assertEquals(4, result)
    }

    @Test
    fun calculateKnowledgeLevel_quality2_decrementsBy1() {
        val result = SrsCalculator.calculateKnowledgeLevel(
            currentLevel = 4,
            quality = 2,
            suggestedLevel = 4
        )
        assertEquals(3, result)
    }

    @Test
    fun calculateKnowledgeLevel_quality1_decrementsBy1() {
        val result = SrsCalculator.calculateKnowledgeLevel(
            currentLevel = 2,
            quality = 1,
            suggestedLevel = 2
        )
        assertEquals(1, result)
    }

    @Test
    fun calculateKnowledgeLevel_quality0_decrementsBy1() {
        val result = SrsCalculator.calculateKnowledgeLevel(
            currentLevel = 1,
            quality = 0,
            suggestedLevel = 1
        )
        assertEquals(0, result)
    }

    @Test
    fun calculateKnowledgeLevel_qualityBelow3_doesNotGoBelowZero() {
        val result = SrsCalculator.calculateKnowledgeLevel(
            currentLevel = 0,
            quality = 0,
            suggestedLevel = 0
        )
        assertEquals(0, result)
    }

    // ── calculateRepetitionNumber ────────────────────────────────────────

    @Test
    fun calculateRepetitionNumber_quality3_incrementsRepetition() {
        val result = SrsCalculator.calculateRepetitionNumber(
            previousCorrectCount = 4,
            quality = 3
        )
        assertEquals(5, result)
    }

    @Test
    fun calculateRepetitionNumber_quality5_incrementsRepetition() {
        val result = SrsCalculator.calculateRepetitionNumber(
            previousCorrectCount = 0,
            quality = 5
        )
        assertEquals(1, result)
    }

    @Test
    fun calculateRepetitionNumber_quality2_resetsToZero() {
        val result = SrsCalculator.calculateRepetitionNumber(
            previousCorrectCount = 10,
            quality = 2
        )
        assertEquals(0, result)
    }

    @Test
    fun calculateRepetitionNumber_quality0_resetsToZero() {
        val result = SrsCalculator.calculateRepetitionNumber(
            previousCorrectCount = 5,
            quality = 0
        )
        assertEquals(0, result)
    }

    @Test
    fun calculateRepetitionNumber_qualityExactly3_incrementsNotResets() {
        val result = SrsCalculator.calculateRepetitionNumber(
            previousCorrectCount = 3,
            quality = 3
        )
        assertEquals(4, result)
    }

    // ── applyStreakBonus ─────────────────────────────────────────────────

    @Test
    fun applyStreakBonus_consecutivePerfectExactly3_appliesMultiplier() {
        val interval = 10f
        val result = SrsCalculator.applyStreakBonus(consecutivePerfect = 3, interval = interval)
        assertEquals(interval * 1.5f, result, 0.001f)
    }

    @Test
    fun applyStreakBonus_consecutivePerfectAbove3_appliesMultiplier() {
        val interval = 8f
        val result = SrsCalculator.applyStreakBonus(consecutivePerfect = 7, interval = interval)
        assertEquals(interval * 1.5f, result, 0.001f)
    }

    @Test
    fun applyStreakBonus_consecutivePerfectBelow3_returnsOriginalInterval() {
        val interval = 10f
        val result = SrsCalculator.applyStreakBonus(consecutivePerfect = 2, interval = interval)
        assertEquals(interval, result, 0.001f)
    }

    @Test
    fun applyStreakBonus_consecutivePerfectZero_returnsOriginalInterval() {
        val interval = 5f
        val result = SrsCalculator.applyStreakBonus(consecutivePerfect = 0, interval = interval)
        assertEquals(interval, result, 0.001f)
    }

    @Test
    fun applyStreakBonus_consecutivePerfectExactly2_noBonus() {
        val interval = 6f
        val result = SrsCalculator.applyStreakBonus(consecutivePerfect = 2, interval = interval)
        assertEquals(interval, result, 0.001f)
    }
}
