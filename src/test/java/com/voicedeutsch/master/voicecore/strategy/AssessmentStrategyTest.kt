// Путь: src/test/java/com/voicedeutsch/master/voicecore/strategy/AssessmentStrategyTest.kt
package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AssessmentStrategyTest {

    private lateinit var strategy: AssessmentStrategy
    private lateinit var snapshot: KnowledgeSnapshot

    @BeforeEach
    fun setUp() {
        strategy = AssessmentStrategy()
        snapshot = KnowledgeSnapshot(
            vocabulary = KnowledgeSnapshot.VocabularyStats(totalWords = 42),
            grammar = KnowledgeSnapshot.GrammarStats(totalRules = 7),
        )
    }

    // ── strategy property ────────────────────────────────────────────────

    @Test
    fun strategy_returnsAssessment() {
        assertEquals(LearningStrategy.ASSESSMENT, strategy.strategy)
    }

    // ── getStrategyContext ───────────────────────────────────────────────

    @Test
    fun getStrategyContext_returnsNonEmptyString() {
        val context = strategy.getStrategyContext(snapshot)
        assertTrue(context.isNotBlank())
    }

    @Test
    fun getStrategyContext_containsTotalWords() {
        val context = strategy.getStrategyContext(snapshot)
        assertTrue(context.contains("42"), "Context should contain totalWords value")
    }

    @Test
    fun getStrategyContext_containsTotalRules() {
        val context = strategy.getStrategyContext(snapshot)
        assertTrue(context.contains("7"), "Context should contain totalRules value")
    }

    @Test
    fun getStrategyContext_containsAllFiveInstructions() {
        val context = strategy.getStrategyContext(snapshot)
        assertTrue(context.contains("1."))
        assertTrue(context.contains("2."))
        assertTrue(context.contains("3."))
        assertTrue(context.contains("4."))
        assertTrue(context.contains("5."))
    }

    @Test
    fun getStrategyContext_containsRecordMistakeReference() {
        val context = strategy.getStrategyContext(snapshot)
        assertTrue(context.contains("record_mistake"))
    }

    @Test
    fun getStrategyContext_containsUpdateUserLevelReference() {
        val context = strategy.getStrategyContext(snapshot)
        assertTrue(context.contains("update_user_level"))
    }

    // ── evaluateMidSession ───────────────────────────────────────────────

    @Test
    fun evaluateMidSession_itemsCompleted15_returnsLinearBook() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 0,
            errorRate = 0f,
            itemsCompleted = 15,
        )
        assertEquals(LearningStrategy.LINEAR_BOOK, result)
    }

    @Test
    fun evaluateMidSession_itemsCompletedAbove15_returnsLinearBook() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 0,
            errorRate = 0f,
            itemsCompleted = 20,
        )
        assertEquals(LearningStrategy.LINEAR_BOOK, result)
    }

    @Test
    fun evaluateMidSession_elapsedMinutes10_returnsLinearBook() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 10,
            errorRate = 0f,
            itemsCompleted = 0,
        )
        assertEquals(LearningStrategy.LINEAR_BOOK, result)
    }

    @Test
    fun evaluateMidSession_elapsedMinutesAbove10_returnsLinearBook() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 15,
            errorRate = 0.5f,
            itemsCompleted = 5,
        )
        assertEquals(LearningStrategy.LINEAR_BOOK, result)
    }

    @Test
    fun evaluateMidSession_bothConditionsMet_returnsLinearBook() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 10,
            errorRate = 1f,
            itemsCompleted = 15,
        )
        assertEquals(LearningStrategy.LINEAR_BOOK, result)
    }

    @Test
    fun evaluateMidSession_belowThresholds_returnsNull() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 9,
            errorRate = 0.3f,
            itemsCompleted = 14,
        )
        assertNull(result)
    }

    @Test
    fun evaluateMidSession_zeroItemsZeroMinutes_returnsNull() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 0,
            errorRate = 0f,
            itemsCompleted = 0,
        )
        assertNull(result)
    }

    @Test
    fun evaluateMidSession_exactlyOneBelowBothThresholds_returnsNull() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 9,
            errorRate = 0f,
            itemsCompleted = 14,
        )
        assertNull(result)
    }
}
