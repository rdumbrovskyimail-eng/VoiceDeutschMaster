// Путь: src/test/java/com/voicedeutsch/master/voicecore/strategy/LinearBookStrategyTest.kt
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

class LinearBookStrategyTest {

    private lateinit var strategy: LinearBookStrategy

    @BeforeEach
    fun setUp() {
        strategy = LinearBookStrategy()
    }

    private fun snapshotWithProgress(currentChapter: Int, currentLesson: Int) = KnowledgeSnapshot(
        vocabulary = VocabularySnapshot(0, emptyMap(), emptyMap(), emptyList(), emptyList(), 0),
        grammar = GrammarSnapshot(0, emptyMap(), emptyList(), emptyList(), 0),
        pronunciation = PronunciationSnapshot(0f, emptyList(), emptyList(), 0f, ""),
        bookProgress = BookProgressSnapshot(currentChapter, currentLesson, 0, 0f, ""),
        sessionHistory = SessionHistorySnapshot("", "", "", 0, 0),
        weakPoints = emptyList(),
        recommendations = RecommendationsSnapshot("", "", emptyList(), "")
    )

    // ── strategy property ────────────────────────────────────────────────

    @Test
    fun strategy_returnsLinearBook() {
        assertEquals(LearningStrategy.LINEAR_BOOK, strategy.strategy)
    }

    // ── getStrategyContext ───────────────────────────────────────────────

    @Test
    fun getStrategyContext_returnsNonEmptyString() {
        val context = strategy.getStrategyContext(snapshotWithProgress(1, 1))
        assertTrue(context.isNotBlank())
    }

    @Test
    fun getStrategyContext_containsLinearBookMode() {
        val context = strategy.getStrategyContext(snapshotWithProgress(1, 1))
        assertTrue(context.contains("Линейное прохождение книги"))
    }

    @Test
    fun getStrategyContext_containsMarkLessonCompleteReference() {
        val context = strategy.getStrategyContext(snapshotWithProgress(1, 1))
        assertTrue(context.contains("mark_lesson_complete"))
    }

    @Test
    fun getStrategyContext_containsCurrentChapterValue() {
        val context = strategy.getStrategyContext(snapshotWithProgress(currentChapter = 3, currentLesson = 1))
        assertTrue(context.contains("3"), "Context should contain currentChapter value")
    }

    @Test
    fun getStrategyContext_containsCurrentLessonValue() {
        val context = strategy.getStrategyContext(snapshotWithProgress(currentChapter = 1, currentLesson = 7))
        assertTrue(context.contains("7"), "Context should contain currentLesson value")
    }

    @Test
    fun getStrategyContext_bothProgressValues_presentInContext() {
        val context = strategy.getStrategyContext(snapshotWithProgress(currentChapter = 5, currentLesson = 12))
        assertTrue(context.contains("5"))
        assertTrue(context.contains("12"))
    }

    @Test
    fun getStrategyContext_zeroProgress_returnsNonEmpty() {
        val context = strategy.getStrategyContext(snapshotWithProgress(currentChapter = 0, currentLesson = 0))
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
