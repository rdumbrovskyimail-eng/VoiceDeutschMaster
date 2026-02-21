package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for StrategySelector.
 * Architecture lines 1742-1788 (Strategy selection algorithm).
 */
class StrategySelectorTest {

    @Test
    fun `returns REPETITION when SRS queue is large and overdue`() {
        val strategy = StrategySelector.selectStrategy(
            srsQueueSize = 15,
            hoursSinceLastRepetition = 25,
            weakPointsCount = 0,
            hasGrammarGap = false,
            hasVocabularyGap = false,
            pronunciationProblems = 0,
            daysSincePronunciationSession = 0,
            hasUncompletedLesson = false,
            sessionDurationMinutes = 0,
            isEvening = false
        )
        assertEquals(LearningStrategy.REPETITION, strategy)
    }

    @Test
    fun `returns LINEAR_BOOK as default`() {
        val strategy = StrategySelector.selectStrategy(
            srsQueueSize = 2,
            hoursSinceLastRepetition = 5,
            weakPointsCount = 0,
            hasGrammarGap = false,
            hasVocabularyGap = false,
            pronunciationProblems = 0,
            daysSincePronunciationSession = 0,
            hasUncompletedLesson = false,
            sessionDurationMinutes = 10,
            isEvening = false
        )
        assertEquals(LearningStrategy.LINEAR_BOOK, strategy)
    }

    @Test
    fun `returns FREE_PRACTICE after 30 minutes`() {
        val strategy = StrategySelector.selectStrategy(
            srsQueueSize = 0,
            hoursSinceLastRepetition = 0,
            weakPointsCount = 0,
            hasGrammarGap = false,
            hasVocabularyGap = false,
            pronunciationProblems = 0,
            daysSincePronunciationSession = 0,
            hasUncompletedLesson = false,
            sessionDurationMinutes = 35,
            isEvening = false
        )
        assertEquals(LearningStrategy.FREE_PRACTICE, strategy)
    }
}