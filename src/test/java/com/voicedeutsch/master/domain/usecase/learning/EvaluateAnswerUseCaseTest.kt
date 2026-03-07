// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/learning/EvaluateAnswerUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.learning

import com.voicedeutsch.master.domain.model.knowledge.MistakeType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EvaluateAnswerUseCaseTest {

    private lateinit var useCase: EvaluateAnswerUseCase

    @BeforeEach
    fun setUp() {
        useCase = EvaluateAnswerUseCase()
    }

    // ── Exact match ───────────────────────────────────────────────────────────

    @Test
    fun invoke_exactMatch_isCorrectTrue() {
        val result = useCase(makeParams("Hund", "Hund"))
        assertTrue(result.isCorrect)
    }

    @Test
    fun invoke_exactMatch_qualityIs5() {
        val result = useCase(makeParams("Hund", "Hund"))
        assertEquals(5, result.quality)
    }

    @Test
    fun invoke_exactMatch_mistakeTypeIsNull() {
        val result = useCase(makeParams("Hund", "Hund"))
        assertNull(result.mistakeType)
    }

    @Test
    fun invoke_exactMatch_feedbackContainsRichtig() {
        val result = useCase(makeParams("Hund", "Hund"))
        assertTrue(result.feedback.contains("Richtig"))
    }

    @Test
    fun invoke_exactMatchCaseInsensitive_isCorrectTrue() {
        val result = useCase(makeParams("HUND", "hund"))
        assertTrue(result.isCorrect)
        assertEquals(5, result.quality)
    }

    @Test
    fun invoke_exactMatchWithLeadingTrailingSpaces_isCorrectTrue() {
        val result = useCase(makeParams("  Hund  ", "Hund"))
        assertTrue(result.isCorrect)
        assertEquals(5, result.quality)
    }

    @Test
    fun invoke_exactMatchBothWithSpaces_isCorrectTrue() {
        val result = useCase(makeParams("  der Hund  ", "  der Hund  "))
        assertTrue(result.isCorrect)
        assertEquals(5, result.quality)
    }

    @Test
    fun invoke_exactMatchMixedCase_isCorrectTrue() {
        val result = useCase(makeParams("Der Hund", "der Hund"))
        assertTrue(result.isCorrect)
        assertEquals(5, result.quality)
    }

    // ── Close match (levenshtein ≤ 2) → quality 4 ────────────────────────────

    @Test
    fun invoke_distanceExactly1_isCorrectTrueQuality4() {
        // "Hunde" vs "Hund" → distance 1
        val result = useCase(makeParams("Hunde", "Hund"))
        assertTrue(result.isCorrect)
        assertEquals(4, result.quality)
    }

    @Test
    fun invoke_distanceExactly2_isCorrectTrueQuality4() {
        // "Hundes" vs "Hund" → distance 2
        val result = useCase(makeParams("Hundes", "Hund"))
        assertTrue(result.isCorrect)
        assertEquals(4, result.quality)
    }

    @Test
    fun invoke_closeMatch_mistakeTypeIsNull() {
        val result = useCase(makeParams("Hunde", "Hund"))
        assertNull(result.mistakeType)
    }

    @Test
    fun invoke_closeMatch_feedbackContainsFastRichtig() {
        val result = useCase(makeParams("Hunde", "Hund"))
        assertTrue(result.feedback.contains("Fast richtig"))
    }

    @Test
    fun invoke_closeMatch_feedbackContainsExpectedAnswer() {
        val result = useCase(makeParams("Hunde", "Hund"))
        assertTrue(result.feedback.contains("Hund"))
    }

    @Test
    fun invoke_closeMatchCaseNormalized_isCorrectTrue() {
        // "HUNDE" normalized to "hunde", expected "HUND" → "hund" → distance 1
        val result = useCase(makeParams("HUNDE", "HUND"))
        assertTrue(result.isCorrect)
        assertEquals(4, result.quality)
    }

    @Test
    fun invoke_oneDeletion_distanceIs1_quality4() {
        // "katze" vs "katz" → distance 1
        val result = useCase(makeParams("katz", "katze"))
        assertTrue(result.isCorrect)
        assertEquals(4, result.quality)
    }

    @Test
    fun invoke_oneSubstitution_distanceIs1_quality4() {
        // "Bund" vs "Hund" → distance 1
        val result = useCase(makeParams("Bund", "Hund"))
        assertTrue(result.isCorrect)
        assertEquals(4, result.quality)
    }

    @Test
    fun invoke_twoSubstitutions_distanceIs2_quality4() {
        // "Bond" vs "Hund" → distance 2
        val result = useCase(makeParams("Bond", "Hund"))
        assertTrue(result.isCorrect)
        assertEquals(4, result.quality)
    }

    // ── Wrong answer (levenshtein > 2) ────────────────────────────────────────

    @Test
    fun invoke_distanceAbove2_isCorrectFalse() {
        // "abc" vs "xyz" → distance 3
        val result = useCase(makeParams("abc", "xyz"))
        assertFalse(result.isCorrect)
    }

    @Test
    fun invoke_distanceAbove2_qualityIs1() {
        val result = useCase(makeParams("abc", "xyz"))
        assertEquals(1, result.quality)
    }

    @Test
    fun invoke_distanceAbove2_mistakeTypeIsWord() {
        val result = useCase(makeParams("abc", "xyz"))
        assertEquals(MistakeType.WORD, result.mistakeType)
    }

    @Test
    fun invoke_distanceAbove2_feedbackContainsNichtGanz() {
        val result = useCase(makeParams("abc", "xyz"))
        assertTrue(result.feedback.contains("Nicht ganz"))
    }

    @Test
    fun invoke_distanceAbove2_feedbackContainsExpectedAnswer() {
        val result = useCase(makeParams("completely wrong", "Hund"))
        assertTrue(result.feedback.contains("Hund"))
    }

    @Test
    fun invoke_emptyUserAnswer_wrongAnswerResult() {
        val result = useCase(makeParams("", "Hund"))
        assertFalse(result.isCorrect)
        assertEquals(1, result.quality)
        assertEquals(MistakeType.WORD, result.mistakeType)
    }

    @Test
    fun invoke_emptyBothAnswers_exactMatch() {
        val result = useCase(makeParams("", ""))
        assertTrue(result.isCorrect)
        assertEquals(5, result.quality)
    }

    @Test
    fun invoke_distanceExactly3_isCorrectFalse() {
        // "Hunden" vs "Hund" → distance 2... use "abcHund" vs "Hund" → distance 3
        val result = useCase(makeParams("Hundx1x", "Hund"))
        assertFalse(result.isCorrect)
    }

    // ── Feedback uses original expectedAnswer (not normalized) ────────────────

    @Test
    fun invoke_closeMatch_feedbackContainsOriginalCasedExpected() {
        val result = useCase(makeParams("Katze", "KATZE"))
        // isClose because both normalize to same → actually exact match
        // Let's test with slight difference
        val result2 = useCase(makeParams("katza", "KATZE"))
        assertTrue(result2.feedback.contains("KATZE"))
    }

    @Test
    fun invoke_wrongAnswer_feedbackContainsOriginalCasedExpected() {
        val result = useCase(makeParams("hallo", "Auf Wiedersehen"))
        assertTrue(result.feedback.contains("Auf Wiedersehen"))
    }

    // ── Levenshtein — boundary conditions ────────────────────────────────────

    @Test
    fun invoke_singleCharCorrect_exactMatch() {
        val result = useCase(makeParams("a", "a"))
        assertTrue(result.isCorrect)
        assertEquals(5, result.quality)
    }

    @Test
    fun invoke_singleCharWrong_distanceIs1_closeMatch() {
        val result = useCase(makeParams("a", "b"))
        assertTrue(result.isCorrect)
        assertEquals(4, result.quality)
    }

    @Test
    fun invoke_longStringsCompletelyDifferent_wrongAnswer() {
        val result = useCase(makeParams("абвгдежзийклмн", "nopqrstuvwxyz"))
        assertFalse(result.isCorrect)
        assertEquals(1, result.quality)
    }

    @Test
    fun invoke_userAnswerWithOnlySpaces_normalizedToEmpty_wrongAnswer() {
        val result = useCase(makeParams("   ", "Hund"))
        assertFalse(result.isCorrect)
        assertEquals(1, result.quality)
    }

    // ── Params data class ─────────────────────────────────────────────────────

    @Test
    fun params_defaultExerciseType_isTranslate() {
        val params = EvaluateAnswerUseCase.Params(
            userAnswer     = "Hund",
            expectedAnswer = "Hund"
        )
        assertEquals("translate", params.exerciseType)
    }

    @Test
    fun params_creation_storesAllFields() {
        val params = EvaluateAnswerUseCase.Params(
            userAnswer     = "answer",
            expectedAnswer = "expected",
            exerciseType   = "fill_blank"
        )
        assertEquals("answer",     params.userAnswer)
        assertEquals("expected",   params.expectedAnswer)
        assertEquals("fill_blank", params.exerciseType)
    }

    @Test
    fun params_copy_changesOnlySpecifiedField() {
        val original = EvaluateAnswerUseCase.Params("a", "b", "translate")
        val copy     = original.copy(userAnswer = "c")

        assertEquals("c",          copy.userAnswer)
        assertEquals("b",          copy.expectedAnswer)
        assertEquals("translate",  copy.exerciseType)
    }

    @Test
    fun params_equals_twoIdenticalInstancesAreEqual() {
        val a = EvaluateAnswerUseCase.Params("a", "b", "translate")
        val b = EvaluateAnswerUseCase.Params("a", "b", "translate")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun params_equals_differentUserAnswerNotEqual() {
        val a = EvaluateAnswerUseCase.Params("a", "b")
        val b = EvaluateAnswerUseCase.Params("c", "b")

        assertNotEquals(a, b)
    }

    // ── Result data class ─────────────────────────────────────────────────────

    @Test
    fun result_creation_storesAllFields() {
        val result = EvaluateAnswerUseCase.Result(
            isCorrect   = true,
            quality     = 5,
            feedback    = "Richtig! ✓",
            mistakeType = null
        )
        assertTrue(result.isCorrect)
        assertEquals(5,          result.quality)
        assertEquals("Richtig! ✓", result.feedback)
        assertNull(result.mistakeType)
    }

    @Test
    fun result_copy_changesOnlySpecifiedField() {
        val original = EvaluateAnswerUseCase.Result(true, 5, "ok", null)
        val copy     = original.copy(quality = 4)

        assertEquals(4,    copy.quality)
        assertEquals(true, copy.isCorrect)
        assertEquals("ok", copy.feedback)
        assertNull(copy.mistakeType)
    }

    @Test
    fun result_equals_twoIdenticalInstancesAreEqual() {
        val a = EvaluateAnswerUseCase.Result(false, 1, "wrong", MistakeType.WORD)
        val b = EvaluateAnswerUseCase.Result(false, 1, "wrong", MistakeType.WORD)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun result_equals_differentMistakeTypeNotEqual() {
        val a = EvaluateAnswerUseCase.Result(false, 1, "f", MistakeType.WORD)
        val b = EvaluateAnswerUseCase.Result(false, 1, "f", MistakeType.GRAMMAR)

        assertNotEquals(a, b)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeParams(
        userAnswer     : String = "",
        expectedAnswer : String = "",
        exerciseType   : String = "translate"
    ) = EvaluateAnswerUseCase.Params(
        userAnswer     = userAnswer,
        expectedAnswer = expectedAnswer,
        exerciseType   = exerciseType
    )
}
