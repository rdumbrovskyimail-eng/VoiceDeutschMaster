// Путь: src/test/java/com/voicedeutsch/master/voicecore/strategy/GapFillingStrategyTest.kt
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

class GapFillingStrategyTest {

    private lateinit var strategy: GapFillingStrategy

    @BeforeEach
    fun setUp() {
        strategy = GapFillingStrategy()
    }

    private fun snapshotWithWeakPoints(weakPoints: List<String>) = KnowledgeSnapshot(
        vocabulary = VocabularySnapshot(50, emptyMap(), emptyMap(), emptyList(), emptyList(), 0),
        grammar = GrammarSnapshot(10, emptyMap(), emptyList(), emptyList(), 0),
        pronunciation = PronunciationSnapshot(0f, emptyList(), emptyList(), 0f, ""),
        bookProgress = BookProgressSnapshot(0, 0, 0, 0f, ""),
        sessionHistory = SessionHistorySnapshot("", "", "", 0, 0),
        weakPoints = weakPoints,
        recommendations = RecommendationsSnapshot("", "", emptyList(), "")
    )

    // ── strategy property ────────────────────────────────────────────────

    @Test
    fun strategy_returnsGapFilling() {
        assertEquals(LearningStrategy.GAP_FILLING, strategy.strategy)
    }

    // ── getStrategyContext ───────────────────────────────────────────────

    @Test
    fun getStrategyContext_returnsNonEmptyString() {
        val context = strategy.getStrategyContext(snapshotWithWeakPoints(emptyList()))
        assertTrue(context.isNotBlank())
    }

    @Test
    fun getStrategyContext_containsGapFillingMode() {
        val context = strategy.getStrategyContext(snapshotWithWeakPoints(emptyList()))
        assertTrue(context.contains("Заполнение пробелов"))
    }

    @Test
    fun getStrategyContext_containsGetWeakPointsReference() {
        val context = strategy.getStrategyContext(snapshotWithWeakPoints(emptyList()))
        assertTrue(context.contains("get_weak_points"))
    }

    @Test
    fun getStrategyContext_containsRecordMistakeReference() {
        val context = strategy.getStrategyContext(snapshotWithWeakPoints(emptyList()))
        assertTrue(context.contains("record_mistake"))
    }

    @Test
    fun getStrategyContext_fiveWeakPoints_allIncluded() {
        val weakPoints = listOf("Artikel", "Dativ", "Akkusativ", "Plusquamperfekt", "Konjunktiv")
        val context = strategy.getStrategyContext(snapshotWithWeakPoints(weakPoints))
        weakPoints.forEach { point ->
            assertTrue(context.contains(point), "Context should contain weak point: $point")
        }
    }

    @Test
    fun getStrategyContext_moreThanFiveWeakPoints_onlyFirstFiveIncluded() {
        val weakPoints = listOf("A", "B", "C", "D", "E", "F", "G")
        val context = strategy.getStrategyContext(snapshotWithWeakPoints(weakPoints))
        assertTrue(context.contains("A"))
        assertTrue(context.contains("E"))
        assertFalse(context.contains("F"), "Context should not contain 6th weak point")
        assertFalse(context.contains("G"), "Context should not contain 7th weak point")
    }

    @Test
    fun getStrategyContext_emptyWeakPoints_returnsNonEmpty() {
        val context = strategy.getStrategyContext(snapshotWithWeakPoints(emptyList()))
        assertTrue(context.isNotBlank())
    }

    @Test
    fun getStrategyContext_oneWeakPoint_includedInContext() {
        val context = strategy.getStrategyContext(snapshotWithWeakPoints(listOf("Genitiv")))
        assertTrue(context.contains("Genitiv"))
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
