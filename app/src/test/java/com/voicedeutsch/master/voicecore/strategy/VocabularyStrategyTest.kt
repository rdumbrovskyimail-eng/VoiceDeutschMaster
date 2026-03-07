// Путь: src/test/java/com/voicedeutsch/master/voicecore/strategy/VocabularyStrategyTest.kt
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

class VocabularyStrategyTest {

    private lateinit var strategy: VocabularyStrategy

    @BeforeEach
    fun setUp() {
        strategy = VocabularyStrategy()
    }

    private fun snapshotWithVocab(totalWords: Int) = KnowledgeSnapshot(
        vocabulary = VocabularySnapshot(
            totalWords = totalWords,
            byLevel = emptyMap(),
            byTopic = emptyMap(),
            recentNewWords = emptyList(),
            problemWords = emptyList(),
            wordsForReviewToday = 0
        ),
        grammar = GrammarSnapshot(
            totalRules = 0,
            byLevel = emptyMap(),
            knownRules = emptyList(),
            problemRules = emptyList(),
            rulesForReviewToday = 0
        ),
        pronunciation = PronunciationSnapshot(
            overallScore = 0f,
            problemSounds = emptyList(),
            goodSounds = emptyList(),
            averageWordScore = 0f,
            trend = ""
        ),
        bookProgress = BookProgressSnapshot(
            currentChapter = 0,
            currentLesson = 0,
            totalChapters = 0,
            completionPercentage = 0f,
            currentTopic = ""
        ),
        sessionHistory = SessionHistorySnapshot(
            lastSession = "",
            lastSessionSummary = "",
            averageSessionDuration = "",
            streak = 0,
            totalSessions = 0
        ),
        weakPoints = emptyList(),
        recommendations = RecommendationsSnapshot(
            primaryStrategy = "",
            secondaryStrategy = "",
            focusAreas = emptyList(),
            suggestedSessionDuration = ""
        )
    )

    // ── strategy property ────────────────────────────────────────────────

    @Test
    fun strategy_returnsVocabularyBoost() {
        assertEquals(LearningStrategy.VOCABULARY_BOOST, strategy.strategy)
    }

    // ── getStrategyContext ───────────────────────────────────────────────

    @Test
    fun getStrategyContext_returnsNonEmptyString() {
        val context = strategy.getStrategyContext(snapshotWithVocab(50))
        assertTrue(context.isNotBlank())
    }

    @Test
    fun getStrategyContext_containsVocabularyMode() {
        val context = strategy.getStrategyContext(snapshotWithVocab(50))
        assertTrue(context.contains("Расширение словарного запаса"))
    }

    @Test
    fun getStrategyContext_containsTotalWordsValue() {
        val context = strategy.getStrategyContext(snapshotWithVocab(123))
        assertTrue(context.contains("123"), "Context should contain totalWords value")
    }

    @Test
    fun getStrategyContext_containsSaveWordKnowledgeReference() {
        val context = strategy.getStrategyContext(snapshotWithVocab(50))
        assertTrue(context.contains("save_word_knowledge"))
    }

    @Test
    fun getStrategyContext_containsInitialLevelAndQuality() {
        val context = strategy.getStrategyContext(snapshotWithVocab(50))
        assertTrue(context.contains("level=1"))
        assertTrue(context.contains("quality=3"))
    }

    @Test
    fun getStrategyContext_containsGroupSizeInstruction() {
        val context = strategy.getStrategyContext(snapshotWithVocab(50))
        assertTrue(context.contains("5-7"))
    }

    @Test
    fun getStrategyContext_containsMnemonicInstruction() {
        val context = strategy.getStrategyContext(snapshotWithVocab(50))
        assertTrue(context.contains("мнемоническ"))
    }

    @Test
    fun getStrategyContext_zeroWords_returnsNonEmpty() {
        val context = strategy.getStrategyContext(snapshotWithVocab(0))
        assertTrue(context.isNotBlank())
        assertTrue(context.contains("0"))
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
