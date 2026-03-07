// Путь: src/test/java/com/voicedeutsch/master/voicecore/strategy/RepetitionStrategyTest.kt
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

class RepetitionStrategyTest {

    private lateinit var strategy: RepetitionStrategy

    @BeforeEach
    fun setUp() {
        strategy = RepetitionStrategy()
    }

    private fun snapshotWithReview(
        wordsForReviewToday: Int,
        rulesForReviewToday: Int,
    ) = KnowledgeSnapshot(
        vocabulary = VocabularySnapshot(
            totalWords = 0,
            byLevel = emptyMap(),
            byTopic = emptyMap(),
            recentNewWords = emptyList(),
            problemWords = emptyList(),
            wordsForReviewToday = wordsForReviewToday
        ),
        grammar = GrammarSnapshot(
            totalRules = 0,
            byLevel = emptyMap(),
            knownRules = emptyList(),
            problemRules = emptyList(),
            rulesForReviewToday = rulesForReviewToday
        ),
        pronunciation = PronunciationSnapshot(0f, emptyList(), emptyList(), 0f, ""),
        bookProgress = BookProgressSnapshot(0, 0, 0, 0f, ""),
        sessionHistory = SessionHistorySnapshot("", "", "", 0, 0),
        weakPoints = emptyList(),
        recommendations = RecommendationsSnapshot("", "", emptyList(), "")
    )

    // ── strategy property ────────────────────────────────────────────────

    @Test
    fun strategy_returnsRepetition() {
        assertEquals(LearningStrategy.REPETITION, strategy.strategy)
    }

    // ── getStrategyContext ───────────────────────────────────────────────

    @Test
    fun getStrategyContext_returnsNonEmptyString() {
        val context = strategy.getStrategyContext(snapshotWithReview(10, 5))
        assertTrue(context.isNotBlank())
    }

    @Test
    fun getStrategyContext_containsSrsMode() {
        val context = strategy.getStrategyContext(snapshotWithReview(10, 5))
        assertTrue(context.contains("SRS"))
    }

    @Test
    fun getStrategyContext_containsWordsForReviewTodayValue() {
        val context = strategy.getStrategyContext(snapshotWithReview(wordsForReviewToday = 17, rulesForReviewToday = 0))
        assertTrue(context.contains("17"), "Context should contain wordsForReviewToday value")
    }

    @Test
    fun getStrategyContext_containsRulesForReviewTodayValue() {
        val context = strategy.getStrategyContext(snapshotWithReview(wordsForReviewToday = 0, rulesForReviewToday = 4))
        assertTrue(context.contains("4"), "Context should contain rulesForReviewToday value")
    }

    @Test
    fun getStrategyContext_containsGetWordsForRepetitionReference() {
        val context = strategy.getStrategyContext(snapshotWithReview(10, 5))
        assertTrue(context.contains("get_words_for_repetition"))
    }

    @Test
    fun getStrategyContext_containsSaveWordKnowledgeReference() {
        val context = strategy.getStrategyContext(snapshotWithReview(10, 5))
        assertTrue(context.contains("save_word_knowledge"))
    }

    @Test
    fun getStrategyContext_containsQualityScaleReference() {
        val context = strategy.getStrategyContext(snapshotWithReview(10, 5))
        assertTrue(context.contains("0-5"))
    }

    @Test
    fun getStrategyContext_containsNoNewMaterialInstruction() {
        val context = strategy.getStrategyContext(snapshotWithReview(10, 5))
        assertTrue(context.contains("новый материал"))
    }

    @Test
    fun getStrategyContext_zeroReviews_returnsNonEmpty() {
        val context = strategy.getStrategyContext(snapshotWithReview(0, 0))
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
    fun evaluateMidSession_highValues_returnsNull() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 999,
            errorRate = 1f,
            itemsCompleted = 999,
        )
        assertNull(result)
    }

    @Test
    fun evaluateMidSession_delegatesToSuperDefault_returnsNull() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 15,
            errorRate = 0.5f,
            itemsCompleted = 20,
        )
        assertNull(result)
    }
}
