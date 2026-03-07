// Путь: src/test/java/com/voicedeutsch/master/voicecore/strategy/FreePracticeStrategyTest.kt
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

class FreePracticeStrategyTest {

    private lateinit var strategy: FreePracticeStrategy
    private lateinit var snapshot: KnowledgeSnapshot

    @BeforeEach
    fun setUp() {
        strategy = FreePracticeStrategy()
        snapshot = KnowledgeSnapshot(
            vocabulary = VocabularySnapshot(totalWords = 100, byLevel = emptyMap(), byTopic = emptyMap(), recentNewWords = emptyList(), problemWords = emptyList(), wordsForReviewToday = 0),
            grammar = GrammarSnapshot(totalRules = 15, byLevel = emptyMap(), knownRules = emptyList(), problemRules = emptyList(), rulesForReviewToday = 0),
            pronunciation = PronunciationSnapshot(0f, emptyList(), emptyList(), 0f, ""),
            bookProgress = BookProgressSnapshot(0, 0, 0, 0f, ""),
            sessionHistory = SessionHistorySnapshot("", "", "", 0, 0),
            weakPoints = emptyList(),
            recommendations = RecommendationsSnapshot("", "", emptyList(), "")
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
            vocabulary = VocabularySnapshot(0, emptyMap(), emptyMap(), emptyList(), emptyList(), 0),
            grammar = GrammarSnapshot(0, emptyMap(), emptyList(), emptyList(), 0),
            pronunciation = PronunciationSnapshot(0f, emptyList(), emptyList(), 0f, ""),
            bookProgress = BookProgressSnapshot(0, 0, 0, 0f, ""),
            sessionHistory = SessionHistorySnapshot("", "", "", 0, 0),
            weakPoints = emptyList(),
            recommendations = RecommendationsSnapshot("", "", emptyList(), "")
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
