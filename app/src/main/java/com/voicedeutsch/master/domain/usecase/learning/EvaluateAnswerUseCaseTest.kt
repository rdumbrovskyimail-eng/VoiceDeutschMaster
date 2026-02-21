package com.voicedeutsch.master.domain.usecase.learning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EvaluateAnswerUseCaseTest {

    private val useCase = EvaluateAnswerUseCase()

    @Test
    fun `exact match returns quality 5`() {
        val result = useCase(EvaluateAnswerUseCase.Params("Haus", "Haus"))
        assertTrue(result.isCorrect)
        assertEquals(5, result.quality)
        assertNull(result.mistakeType)
    }

    @Test
    fun `case-insensitive match is correct`() {
        val result = useCase(EvaluateAnswerUseCase.Params("haus", "Haus"))
        assertTrue(result.isCorrect)
    }

    @Test
    fun `close match returns quality 4`() {
        val result = useCase(EvaluateAnswerUseCase.Params("Hauss", "Haus"))
        assertTrue(result.isCorrect)
        assertEquals(4, result.quality)
    }

    @Test
    fun `wrong answer returns quality 1`() {
        val result = useCase(EvaluateAnswerUseCase.Params("Katze", "Haus"))
        assertFalse(result.isCorrect)
        assertEquals(1, result.quality)
    }
}