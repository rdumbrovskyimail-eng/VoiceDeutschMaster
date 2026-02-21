package com.voicedeutsch.master.voicecore.session

import com.voicedeutsch.master.domain.model.LearningStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VoiceSessionManagerTest {

    private val manager = VoiceSessionManager()

    @Test
    fun `start session creates valid state`() {
        val sessionId = manager.startSession("user1", LearningStrategy.LINEAR_BOOK)
        assertTrue(sessionId.isNotBlank())
        assertTrue(manager.state.value.isActive)
        assertEquals("user1", manager.state.value.userId)
    }

    @Test
    fun `switch strategy is tracked`() {
        manager.startSession("user1", LearningStrategy.LINEAR_BOOK)
        manager.switchStrategy(LearningStrategy.REPETITION)
        val state = manager.state.value
        assertEquals(LearningStrategy.REPETITION, state.currentStrategy)
        assertTrue(state.strategiesUsed.contains(LearningStrategy.LINEAR_BOOK))
        assertTrue(state.strategiesUsed.contains(LearningStrategy.REPETITION))
    }

    @Test
    fun `counters increment correctly`() {
        manager.startSession("user1", LearningStrategy.LINEAR_BOOK)
        manager.recordWordLearned()
        manager.recordWordLearned()
        manager.recordWordReviewed()
        manager.recordMistake()
        manager.recordCorrect()
        manager.recordCorrect()

        val state = manager.state.value
        assertEquals(2, state.wordsLearned)
        assertEquals(1, state.wordsReviewed)
        assertEquals(1, state.mistakeCount)
        assertEquals(2, state.correctCount)
    }

    @Test
    fun `end session returns result and resets`() {
        manager.startSession("user1", LearningStrategy.LINEAR_BOOK)
        manager.recordWordLearned()
        manager.recordWordLearned()
        manager.recordWordLearned()
        manager.recordCorrect()
        manager.recordCorrect()
        manager.recordMistake()

        val result = manager.endSession()
        assertEquals(3, result.wordsLearned)
        assertEquals(2, result.exercisesCorrect)
        assertEquals(3, result.exercisesCompleted)
        assertFalse(manager.state.value.isActive)
    }
}