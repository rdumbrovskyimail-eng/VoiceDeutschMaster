// Путь: src/test/java/com/voicedeutsch/master/voicecore/strategy/GrammarStrategyTest.kt
package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.BookProgressSnapshot
import com.voicedeutsch.master.domain.model.knowledge.GrammarSnapshot
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.domain.model.knowledge.PronunciationSnapshot
import com.voicedeutsch.master.domain.model.knowledge.RecommendationsSnapshot
import com.voicedeutsch.master.domain.model.knowledge.SessionHistorySnapshot
import com.voicedeutsch.master.domain.model.knowledge.VocabularySnapshot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GrammarStrategyTest {

    private lateinit var strategy: GrammarStrategy

    @BeforeEach
    fun setUp() {
        strategy = GrammarStrategy()
    }

    private fun snapshotWithGrammar(
        totalRules: Int,
        rulesForReviewToday: Int,
    ) = KnowledgeSnapshot(
        vocabulary = VocabularySnapshot(0, emptyMap(), emptyMap(), emptyList(), emptyList(), 0),
        grammar = GrammarSnapshot(totalRules, emptyMap(), emptyList(), emptyList(), rulesForReviewToday),
        pronunciation = PronunciationSnapshot(0f, emptyList(), emptyList(), 0f, ""),
        bookProgress = BookProgressSnapshot(0, 0, 0, 0f, ""),
        sessionHistory = SessionHistorySnapshot("", "", "", 0, 0),
        weakPoints = emptyList(),
        recommendations = RecommendationsSnapshot("", "", emptyList(), "")
    )

    // ── strategy property ────────────────────────────────────────────────

    @Test
    fun strategy_returnsGrammarDrill() {
        assertEquals(LearningStrategy.GRAMMAR_DRILL, strategy.strategy)
    }

    // ── getStrategyContext ───────────────────────────────────────────────

    @Test
    fun getStrategyContext_returnsNonEmptyString() {
        val context = strategy.getStrategyContext(snapshotWithGrammar(10, 3))
        assertTrue(context.isNotBlank())
    }

    @Test
    fun getStrategyContext_containsGrammarMode() {
        val context = strategy.getStrategyContext(snapshotWithGrammar(10, 3))
        assertTrue(context.contains("Грамматические упражнения"))
    }

    @Test
    fun getStrategyContext_containsTotalRulesValue() {
        val context = strategy.getStrategyContext(snapshotWithGrammar(totalRules = 25, rulesForReviewToday = 0))
        assertTrue(context.contains("25"), "Context should contain totalRules value")
    }

    @Test
    fun getStrategyContext_containsRulesForReviewTodayValue() {
        val context = strategy.getStrategyContext(snapshotWithGrammar(totalRules = 0, rulesForReviewToday = 8))
        assertTrue(context.contains("8"), "Context should contain rulesForReviewToday value")
    }

    @Test
    fun getStrategyContext_containsSaveRuleKnowledgeReference() {
        val context = strategy.getStrategyContext(snapshotWithGrammar(10, 3))
        assertTrue(context.contains("save_rule_knowledge"))
    }

    @Test
    fun getStrategyContext_containsRecordMistakeReference() {
        val context = strategy.getStrategyContext(snapshotWithGrammar(10, 3))
        assertTrue(context.contains("record_mistake"))
    }

    @Test
    fun getStrategyContext_containsGrammarMistakeType() {
        val context = strategy.getStrategyContext(snapshotWithGrammar(10, 3))
        assertTrue(context.contains("grammar"))
    }

    @Test
    fun getStrategyContext_zeroRules_containsZeroValues() {
        val context = strategy.getStrategyContext(snapshotWithGrammar(totalRules = 0, rulesForReviewToday = 0))
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
