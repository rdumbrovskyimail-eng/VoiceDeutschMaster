// Путь: src/test/java/com/voicedeutsch/master/voicecore/strategy/ListeningStrategyTest.kt
package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ListeningStrategyTest {

    private lateinit var strategy: ListeningStrategy
    private lateinit var snapshot: KnowledgeSnapshot

    @BeforeEach
    fun setUp() {
        strategy = ListeningStrategy()
        snapshot = KnowledgeSnapshot(
            vocabulary = KnowledgeSnapshot.VocabularyStats(totalWords = 80),
            grammar = KnowledgeSnapshot.GrammarStats(totalRules = 12),
        )
    }

    // ── strategy property ────────────────────────────────────────────────

    @Test
    fun strategy_returnsListening() {
        assertEquals(LearningStrategy.LISTENING, strategy.strategy)
    }

    // ── getStrategyContext ───────────────────────────────────────────────

    @Test
    fun getStrategyContext_returnsNonEmptyString() {
        val context = strategy.getStrategyContext(snapshot)
        assertTrue(context.isNotBlank())
    }

    @Test
    fun getStrategyContext_containsListeningMode() {
        val context = strategy.getStrategyContext(snapshot)
        assertTrue(context.contains("Аудирование"))
    }

    @Test
    fun getStrategyContext_containsRepeatSlowerInstruction() {
        val context = strategy.getStrategyContext(snapshot)
        assertTrue(context.contains("повтори медленнее"))
    }

    @Test
    fun getStrategyContext_containsRussianFallbackInstruction() {
        val context = strategy.getStrategyContext(snapshot)
        assertTrue(context.contains("по-русски"))
    }

    @Test
    fun getStrategyContext_containsComprehensionCheckInstruction() {
        val context = strategy.getStrategyContext(snapshot)
        assertTrue(context.contains("Проверяй понимание"))
    }

    @Test
    fun getStrategyContext_differentSnapshot_returnsNonEmpty() {
        val emptySnapshot = KnowledgeSnapshot(
            vocabulary = KnowledgeSnapshot.VocabularyStats(totalWords = 0),
            grammar = KnowledgeSnapshot.GrammarStats(totalRules = 0),
        )
        val context = strategy.getStrategyContext(emptySnapshot)
        assertTrue(context.isNotBlank())
    }

    // ── evaluateMidSession (default) ─────────────────────────────────────

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
    fun evaluateMidSession_highValues_returnsNull() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 999,
            errorRate = 1f,
            itemsCompleted = 999,
        )
        assertNull(result)
    }
}
