// Путь: src/test/java/com/voicedeutsch/master/voicecore/strategy/StrategyHandlerTest.kt
package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StrategyHandlerTest {

    private lateinit var handler: StrategyHandler

    @BeforeEach
    fun setUp() {
        handler = object : StrategyHandler {
            override val strategy = LearningStrategy.LINEAR_BOOK
            override fun getStrategyContext(snapshot: KnowledgeSnapshot) = "test"
        }
    }

    // ── evaluateMidSession — error rate branch ───────────────────────────

    @Test
    fun evaluateMidSession_highErrorRateAndEnoughItems_returnsFreePractice() {
        val result = handler.evaluateMidSession(
            elapsedMinutes = 0,
            errorRate = 0.61f,
            itemsCompleted = 6,
        )
        assertEquals(LearningStrategy.FREE_PRACTICE, result)
    }

    @Test
    fun evaluateMidSession_errorRateExactly06_itemsAbove5_returnsNull() {
        val result = handler.evaluateMidSession(
            elapsedMinutes = 0,
            errorRate = 0.6f,
            itemsCompleted = 6,
        )
        assertNull(result)
    }

    @Test
    fun evaluateMidSession_highErrorRateButExactly5Items_returnsNull() {
        val result = handler.evaluateMidSession(
            elapsedMinutes = 0,
            errorRate = 0.9f,
            itemsCompleted = 5,
        )
        assertNull(result)
    }

    @Test
    fun evaluateMidSession_highErrorRateButZeroItems_returnsNull() {
        val result = handler.evaluateMidSession(
            elapsedMinutes = 0,
            errorRate = 1f,
            itemsCompleted = 0,
        )
        assertNull(result)
    }

    @Test
    fun evaluateMidSession_lowErrorRateAndEnoughItems_returnsNull() {
        val result = handler.evaluateMidSession(
            elapsedMinutes = 0,
            errorRate = 0.3f,
            itemsCompleted = 10,
        )
        assertNull(result)
    }

    // ── evaluateMidSession — elapsed minutes branch ──────────────────────

    @Test
    fun evaluateMidSession_elapsedMinutesAbove25_returnsFreePractice() {
        val result = handler.evaluateMidSession(
            elapsedMinutes = 26,
            errorRate = 0f,
            itemsCompleted = 0,
        )
        assertEquals(LearningStrategy.FREE_PRACTICE, result)
    }

    @Test
    fun evaluateMidSession_elapsedMinutesExactly25_returnsNull() {
        val result = handler.evaluateMidSession(
            elapsedMinutes = 25,
            errorRate = 0f,
            itemsCompleted = 0,
        )
        assertNull(result)
    }

    @Test
    fun evaluateMidSession_elapsedMinutesBelow25_returnsNull() {
        val result = handler.evaluateMidSession(
            elapsedMinutes = 10,
            errorRate = 0f,
            itemsCompleted = 3,
        )
        assertNull(result)
    }

    // ── evaluateMidSession — no condition met ────────────────────────────

    @Test
    fun evaluateMidSession_zeroValues_returnsNull() {
        val result = handler.evaluateMidSession(
            elapsedMinutes = 0,
            errorRate = 0f,
            itemsCompleted = 0,
        )
        assertNull(result)
    }

    @Test
    fun evaluateMidSession_errorRateTriggerTakesPrecedenceOverMinutes() {
        val result = handler.evaluateMidSession(
            elapsedMinutes = 26,
            errorRate = 0.9f,
            itemsCompleted = 10,
        )
        assertEquals(LearningStrategy.FREE_PRACTICE, result)
    }
}
