// Путь: src/test/java/com/voicedeutsch/master/domain/model/session/SessionResultTest.kt
package com.voicedeutsch.master.domain.model.session

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SessionResultTest {

    private fun createSessionResult(
        sessionId: String = "session_1",
        durationMinutes: Int = 30,
        wordsLearned: Int = 10,
        wordsReviewed: Int = 20,
        rulesPracticed: Int = 5,
        exercisesCompleted: Int = 15,
        exercisesCorrect: Int = 12,
        averageScore: Float = 0.8f,
        averagePronunciationScore: Float = 0.75f,
        strategiesUsed: List<String> = listOf("LINEAR_BOOK"),
        summary: String = "Good session"
    ) = SessionResult(
        sessionId = sessionId,
        durationMinutes = durationMinutes,
        wordsLearned = wordsLearned,
        wordsReviewed = wordsReviewed,
        rulesPracticed = rulesPracticed,
        exercisesCompleted = exercisesCompleted,
        exercisesCorrect = exercisesCorrect,
        averageScore = averageScore,
        averagePronunciationScore = averagePronunciationScore,
        strategiesUsed = strategiesUsed,
        summary = summary
    )

    // ── Construction ──────────────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val strategies = listOf("REPETITION", "GAP_FILLING")
        val result = createSessionResult(
            sessionId = "s_99",
            durationMinutes = 45,
            wordsLearned = 20,
            wordsReviewed = 40,
            rulesPracticed = 8,
            exercisesCompleted = 25,
            exercisesCorrect = 23,
            averageScore = 0.92f,
            averagePronunciationScore = 0.88f,
            strategiesUsed = strategies,
            summary = "Excellent progress!"
        )
        assertEquals("s_99", result.sessionId)
        assertEquals(45, result.durationMinutes)
        assertEquals(20, result.wordsLearned)
        assertEquals(40, result.wordsReviewed)
        assertEquals(8, result.rulesPracticed)
        assertEquals(25, result.exercisesCompleted)
        assertEquals(23, result.exercisesCorrect)
        assertEquals(0.92f, result.averageScore, 0.001f)
        assertEquals(0.88f, result.averagePronunciationScore, 0.001f)
        assertEquals(strategies, result.strategiesUsed)
        assertEquals("Excellent progress!", result.summary)
    }

    @Test
    fun constructor_zeroCounts_storedCorrectly() {
        val result = createSessionResult(
            wordsLearned = 0,
            wordsReviewed = 0,
            rulesPracticed = 0,
            exercisesCompleted = 0,
            exercisesCorrect = 0
        )
        assertEquals(0, result.wordsLearned)
        assertEquals(0, result.wordsReviewed)
        assertEquals(0, result.rulesPracticed)
        assertEquals(0, result.exercisesCompleted)
        assertEquals(0, result.exercisesCorrect)
    }

    @Test
    fun constructor_perfectScores_storedCorrectly() {
        val result = createSessionResult(averageScore = 1.0f, averagePronunciationScore = 1.0f)
        assertEquals(1.0f, result.averageScore, 0.001f)
        assertEquals(1.0f, result.averagePronunciationScore, 0.001f)
    }

    @Test
    fun constructor_zeroScores_storedCorrectly() {
        val result = createSessionResult(averageScore = 0f, averagePronunciationScore = 0f)
        assertEquals(0f, result.averageScore, 0.001f)
        assertEquals(0f, result.averagePronunciationScore, 0.001f)
    }

    @Test
    fun constructor_emptyStrategiesUsed_storedCorrectly() {
        val result = createSessionResult(strategiesUsed = emptyList())
        assertTrue(result.strategiesUsed.isEmpty())
    }

    @Test
    fun constructor_multipleStrategies_storedCorrectly() {
        val strategies = listOf("LINEAR_BOOK", "REPETITION", "PRONUNCIATION")
        val result = createSessionResult(strategiesUsed = strategies)
        assertEquals(3, result.strategiesUsed.size)
        assertEquals(strategies, result.strategiesUsed)
    }

    @Test
    fun constructor_emptySummary_storedCorrectly() {
        val result = createSessionResult(summary = "")
        assertEquals("", result.summary)
    }

    @Test
    fun constructor_longSummary_storedCorrectly() {
        val summary = "Today you learned 20 new words and practiced pronunciation. Great job!"
        val result = createSessionResult(summary = summary)
        assertEquals(summary, result.summary)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_changeDurationMinutes_onlyDurationChanges() {
        val original = createSessionResult(durationMinutes = 30)
        val modified = original.copy(durationMinutes = 60)
        assertEquals(60, modified.durationMinutes)
        assertEquals(original.sessionId, modified.sessionId)
        assertEquals(original.wordsLearned, modified.wordsLearned)
        assertEquals(original.averageScore, modified.averageScore, 0.001f)
    }

    @Test
    fun copy_updateSummary_summaryUpdated() {
        val original = createSessionResult(summary = "Good session")
        val modified = original.copy(summary = "Amazing session!")
        assertEquals("Amazing session!", modified.summary)
        assertEquals("Good session", original.summary)
    }

    @Test
    fun copy_updateStrategiesUsed_strategiesUpdated() {
        val original = createSessionResult(strategiesUsed = listOf("LINEAR_BOOK"))
        val modified = original.copy(strategiesUsed = listOf("REPETITION", "GAP_FILLING"))
        assertEquals(listOf("REPETITION", "GAP_FILLING"), modified.strategiesUsed)
        assertEquals(listOf("LINEAR_BOOK"), original.strategiesUsed)
    }

    @Test
    fun copy_increaseWordsLearned_wordsUpdated() {
        val original = createSessionResult(wordsLearned = 10)
        val modified = original.copy(wordsLearned = 25)
        assertEquals(25, modified.wordsLearned)
        assertEquals(10, original.wordsLearned)
    }

    @Test
    fun copy_updateAverageScore_scoreUpdated() {
        val original = createSessionResult(averageScore = 0.5f)
        val modified = original.copy(averageScore = 0.95f)
        assertEquals(0.95f, modified.averageScore, 0.001f)
        assertEquals(0.5f, original.averageScore, 0.001f)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createSessionResult(), createSessionResult())
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(createSessionResult().hashCode(), createSessionResult().hashCode())
    }

    @Test
    fun equals_differentSessionId_notEqual() {
        assertNotEquals(
            createSessionResult(sessionId = "s_1"),
            createSessionResult(sessionId = "s_2")
        )
    }

    @Test
    fun equals_differentDurationMinutes_notEqual() {
        assertNotEquals(
            createSessionResult(durationMinutes = 30),
            createSessionResult(durationMinutes = 60)
        )
    }

    @Test
    fun equals_differentWordsLearned_notEqual() {
        assertNotEquals(
            createSessionResult(wordsLearned = 5),
            createSessionResult(wordsLearned = 15)
        )
    }

    @Test
    fun equals_differentExercisesCorrect_notEqual() {
        assertNotEquals(
            createSessionResult(exercisesCorrect = 10),
            createSessionResult(exercisesCorrect = 12)
        )
    }

    @Test
    fun equals_differentAverageScore_notEqual() {
        assertNotEquals(
            createSessionResult(averageScore = 0.5f),
            createSessionResult(averageScore = 0.9f)
        )
    }

    @Test
    fun equals_differentAveragePronunciationScore_notEqual() {
        assertNotEquals(
            createSessionResult(averagePronunciationScore = 0.6f),
            createSessionResult(averagePronunciationScore = 0.9f)
        )
    }

    @Test
    fun equals_differentStrategiesUsed_notEqual() {
        assertNotEquals(
            createSessionResult(strategiesUsed = listOf("LINEAR_BOOK")),
            createSessionResult(strategiesUsed = listOf("REPETITION"))
        )
    }

    @Test
    fun equals_differentSummary_notEqual() {
        assertNotEquals(
            createSessionResult(summary = "Good"),
            createSessionResult(summary = "Bad")
        )
    }

    @Test
    fun equals_emptyVsNonEmptyStrategies_notEqual() {
        assertNotEquals(
            createSessionResult(strategiesUsed = emptyList()),
            createSessionResult(strategiesUsed = listOf("LINEAR_BOOK"))
        )
    }
}
