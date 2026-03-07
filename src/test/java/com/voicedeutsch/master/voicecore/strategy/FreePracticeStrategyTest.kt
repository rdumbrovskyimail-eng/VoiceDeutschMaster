// Путь: src/test/java/com/voicedeutsch/master/voicecore/strategy/FreePracticeStrategyTest.kt
package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FreePracticeStrategyTest {

    private lateinit var strategy: FreePracticeStrategy
    private lateinit var snapshot: KnowledgeSnapshot

    @BeforeEach
    fun setUp() {
        strategy = FreePracticeStrategy()
        snapshot = KnowledgeSnapshot(
            vocabulary = KnowledgeSnapshot.VocabularyStats(totalWords = 100),
            grammar = KnowledgeSnapshot.GrammarStats(totalRules = 15),
        )
    }

    // ── strategy property ────────────────────────────────────────────────

    @Test
    fun strategy_returnsFreePractice() {
        assertEquals(LearningStrategy.FREE_PRACTICE, strategy.strategy)
    }

    // ── getStrategyContext ───────────────────────────────────────────────

    @Test
    fun getStrategyContext_returnsNonEmptyString() {
        val context = strategy.getStrategyContext(snapshot)
        assertTrue(context.isNotBlank())
    }

    @Test
    fun getStrategyContext_containsFreePracticeMode() {
        val context = strategy.getStrategyContext(snapshot)
        assertTrue(context.contains("Свободная практика"))
    }

    @Test
    fun getStrategyContext_containsSaveWordKnowledgeReference() {
        val context = strategy.getStrategyContext(snapshot)
        assertTrue(context.contains("save_word_knowledge"))
    }

    @Test
    fun getStrategyContext_containsErrorCorrectionInstruction() {
        val context = strategy.getStrategyContext(snapshot)
        assertTrue(context.contains("Корректируй ошибки"))
    }

    @Test
    fun getStrategyContext_differentSnapshot_stillReturnsNonEmpty() {
        val emptySnapshot = KnowledgeSnapshot(
            vocabulary = KnowledgeSnapshot.VocabularyStats(totalWords = 0),
            grammar = KnowledgeSnapshot.GrammarStats(totalRules = 0),
        )
        val context = strategy.getStrategyContext(emptySnapshot)
        assertTrue(context.isNotBlank())
    }

    // ── evaluateMidSession ───────────────────────────────────────────────

    @Test
    fun evaluateMidSession_zeroValues_returnsNull() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 0,
            errorRate = 0f,
            itemsCompleted = 0,
        )
        assertNull(result)
    }

    @Test
    fun evaluateMidSession_highItemsCompleted_returnsNull() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 0,
            errorRate = 0f,
            itemsCompleted = 100,
        )
        assertNull(result)
    }

    @Test
    fun evaluateMidSession_highElapsedMinutes_returnsNull() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 60,
            errorRate = 0f,
            itemsCompleted = 0,
        )
        assertNull(result)
    }

    @Test
    fun evaluateMidSession_highErrorRate_returnsNull() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 5,
            errorRate = 1f,
            itemsCompleted = 20,
        )
        assertNull(result)
    }

    @Test
    fun evaluateMidSession_allThresholdsExceeded_returnsNull() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 999,
            errorRate = 1f,
            itemsCompleted = 999,
        )
        assertNull(result)
    }
}
