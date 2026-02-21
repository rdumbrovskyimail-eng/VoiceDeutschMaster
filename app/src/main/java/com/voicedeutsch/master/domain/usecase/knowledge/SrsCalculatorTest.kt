package com.voicedeutsch.master.domain.usecase.knowledge

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SrsCalculatorTest {

    @Test
    fun `quality 5 first time gives interval 1 day`() {
        val interval = SrsCalculator.calculateInterval(0, 5, 2.5f, 0f)
        assertEquals(1f, interval, 0.01f)
    }

    @Test
    fun `quality 5 second time gives interval 3 days`() {
        val interval = SrsCalculator.calculateInterval(1, 5, 2.5f, 1f)
        assertEquals(3f, interval, 0.01f)
    }

    @Test
    fun `quality 5 third time uses ease factor`() {
        val ef = SrsCalculator.calculateEaseFactor(2.5f, 5)
        val interval = SrsCalculator.calculateInterval(2, 5, ef, 3f)
        assertEquals(3f * ef, interval, 0.1f)
    }

    @Test
    fun `quality 0 gives short interval`() {
        val interval = SrsCalculator.calculateInterval(5, 0, 2.5f, 30f)
        assertEquals(0.5f, interval, 0.01f)
    }

    @Test
    fun `ease factor never drops below 1_3`() {
        val ef = SrsCalculator.calculateEaseFactor(1.3f, 0)
        assertTrue(ef >= 1.3f)
    }

    @Test
    fun `quality 0 resets repetition`() {
        val rep = SrsCalculator.calculateRepetitionNumber(5, 0)
        assertEquals(0, rep)
    }

    @Test
    fun `quality 4 increments repetition`() {
        val rep = SrsCalculator.calculateRepetitionNumber(1, 4)
        assertEquals(2, rep)
    }
}