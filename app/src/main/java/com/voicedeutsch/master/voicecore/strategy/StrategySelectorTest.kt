package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.BookProgressSnapshot
import com.voicedeutsch.master.domain.model.GrammarSnapshot
import com.voicedeutsch.master.domain.model.KnowledgeSnapshot
import com.voicedeutsch.master.domain.model.PronunciationSnapshot
import com.voicedeutsch.master.domain.model.RecommendationsSnapshot
import com.voicedeutsch.master.domain.model.VocabularySnapshot
import com.voicedeutsch.master.domain.model.WeakPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for StrategySelector.
 * Architecture lines 1742-1788 (Strategy selection algorithm).
 */
class StrategySelectorTest {

    private fun mockSnapshot(
        srsQueue: Int = 0,
        weakPoints: Int = 0,
    ): KnowledgeSnapshot = KnowledgeSnapshot(
        vocabulary = VocabularySnapshot(
            totalWords = 100, byLevel = emptyMap(),
            wordsForReviewToday = srsQueue, problemWords = emptyList()
        ),
        grammar = GrammarSnapshot(
            totalRules = 20, byLevel = emptyMap(),
            rulesForReviewToday = 0, problemRules = emptyList()
        ),
        pronunciation = PronunciationSnapshot(
            overallScore = 0.7f, problemSounds = emptyList(),
            goodSounds = emptyList(), trend = "stable"
        ),
        weakPoints = List(weakPoints) { WeakPoint("", "", 1) },
        bookProgress = BookProgressSnapshot(1, 1, "", 0f),
        recommendations = RecommendationsSnapshot(LearningStrategy.LINEAR_BOOK, "")
    )

    @Test
    fun `returns REPETITION when SRS queue is large`() {
        val strategy = StrategySelector().selectStrategy(mockSnapshot(srsQueue = 15))
        assertEquals(LearningStrategy.REPETITION, strategy)
    }

    @Test
    fun `returns LINEAR_BOOK as default`() {
        val strategy = StrategySelector().selectStrategy(mockSnapshot())
        assertEquals(LearningStrategy.LINEAR_BOOK, strategy)
    }
}