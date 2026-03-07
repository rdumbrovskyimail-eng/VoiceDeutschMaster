// Путь: src/test/java/com/voicedeutsch/master/voicecore/strategy/ListeningStrategyTest.kt
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

class ListeningStrategyTest {

    private lateinit var strategy: ListeningStrategy
    private lateinit var snapshot: KnowledgeSnapshot

    @BeforeEach
    fun setUp() {
        strategy = ListeningStrategy()
        snapshot = KnowledgeSnapshot(
            vocabulary = VocabularySnapshot(
                totalWords = 80,
                byLevel = emptyMap(),
                byTopic = emptyMap(),
                recentNewWords = emptyList(),
                problemWords = emptyList(),
                wordsForReviewToday = 0
            ),
            grammar = GrammarSnapshot(
                totalRules = 12,
                byLevel = emptyMap(),
                knownRules = emptyList(),
                problemRules = emptyList(),
                rulesForReviewToday = 0
            ),
            pronunciation = PronunciationSnapshot(0f, emptyList(), emptyList(), 0f, ""),
            bookProgress = BookProgressSnapshot(0, 0, 0, 0f, ""),
            sessionHistory = SessionHistorySnapshot("", "", "", 0, 0),
            weakPoints = emptyList(),
            recommendations = RecommendationsSnapshot("", "", emptyList(), "")
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
