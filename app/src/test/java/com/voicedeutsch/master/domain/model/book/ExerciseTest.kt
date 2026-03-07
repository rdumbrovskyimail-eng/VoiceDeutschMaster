// Путь: src/test/java/com/voicedeutsch/master/domain/model/book/ExerciseTest.kt
package com.voicedeutsch.master.domain.model.book

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ═══════════════════════════════════════════════════════════════════════════
// Exercise
// ═══════════════════════════════════════════════════════════════════════════

class ExerciseTest {

    private fun createExercise(
        id: String = "ex_1",
        type: ExerciseType = ExerciseType.FILL_IN_BLANK,
        prompt: String = "Ich ___ ein Student.",
        expectedAnswer: String = "",
        hints: List<String> = emptyList(),
        difficulty: Int = 1,
        relatedWordIds: List<String> = emptyList(),
        relatedRuleIds: List<String> = emptyList()
    ) = Exercise(
        id = id,
        type = type,
        prompt = prompt,
        expectedAnswer = expectedAnswer,
        hints = hints,
        difficulty = difficulty,
        relatedWordIds = relatedWordIds,
        relatedRuleIds = relatedRuleIds
    )

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val exercise = Exercise(id = "ex_1", type = ExerciseType.CONJUGATE, prompt = "spielen")
        assertEquals("", exercise.expectedAnswer)
        assertTrue(exercise.hints.isEmpty())
        assertEquals(1, exercise.difficulty)
        assertTrue(exercise.relatedWordIds.isEmpty())
        assertTrue(exercise.relatedRuleIds.isEmpty())
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val hints = listOf("Tipp 1", "Tipp 2")
        val wordIds = listOf("w_1", "w_2")
        val ruleIds = listOf("r_1")
        val exercise = createExercise(
            id = "ex_99",
            type = ExerciseType.TRANSLATE_TO_DE,
            prompt = "Переведи: дом",
            expectedAnswer = "das Haus",
            hints = hints,
            difficulty = 3,
            relatedWordIds = wordIds,
            relatedRuleIds = ruleIds
        )
        assertEquals("ex_99", exercise.id)
        assertEquals(ExerciseType.TRANSLATE_TO_DE, exercise.type)
        assertEquals("Переведи: дом", exercise.prompt)
        assertEquals("das Haus", exercise.expectedAnswer)
        assertEquals(hints, exercise.hints)
        assertEquals(3, exercise.difficulty)
        assertEquals(wordIds, exercise.relatedWordIds)
        assertEquals(ruleIds, exercise.relatedRuleIds)
    }

    @Test
    fun constructor_difficultyAtMinimum_storedCorrectly() {
        val exercise = createExercise(difficulty = 1)
        assertEquals(1, exercise.difficulty)
    }

    @Test
    fun constructor_difficultyAtMaximum_storedCorrectly() {
        val exercise = createExercise(difficulty = 5)
        assertEquals(5, exercise.difficulty)
    }

    @Test
    fun constructor_emptyExpectedAnswer_storedCorrectly() {
        val exercise = createExercise(expectedAnswer = "")
        assertEquals("", exercise.expectedAnswer)
    }

    @Test
    fun constructor_allExerciseTypes_storedCorrectly() {
        ExerciseType.entries.forEach { type ->
            val exercise = createExercise(type = type)
            assertEquals(type, exercise.type)
        }
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_changeType_onlyTypeChanges() {
        val original = createExercise(type = ExerciseType.FILL_IN_BLANK)
        val modified = original.copy(type = ExerciseType.WORD_ORDER)
        assertEquals(ExerciseType.WORD_ORDER, modified.type)
        assertEquals(original.id, modified.id)
        assertEquals(original.prompt, modified.prompt)
        assertEquals(original.difficulty, modified.difficulty)
    }

    @Test
    fun copy_increaseDifficulty_difficultyUpdated() {
        val original = createExercise(difficulty = 2)
        val modified = original.copy(difficulty = 4)
        assertEquals(4, modified.difficulty)
        assertEquals(2, original.difficulty)
    }

    @Test
    fun copy_addHints_hintsUpdated() {
        val original = createExercise(hints = emptyList())
        val modified = original.copy(hints = listOf("Clue 1", "Clue 2"))
        assertEquals(listOf("Clue 1", "Clue 2"), modified.hints)
        assertTrue(original.hints.isEmpty())
    }

    @Test
    fun copy_setExpectedAnswer_answerUpdated() {
        val original = createExercise(expectedAnswer = "")
        val modified = original.copy(expectedAnswer = "der Hund")
        assertEquals("der Hund", modified.expectedAnswer)
        assertEquals("", original.expectedAnswer)
    }

    @Test
    fun copy_addRelatedWordIds_wordIdsUpdated() {
        val original = createExercise(relatedWordIds = emptyList())
        val modified = original.copy(relatedWordIds = listOf("w_10", "w_11"))
        assertEquals(listOf("w_10", "w_11"), modified.relatedWordIds)
        assertTrue(original.relatedWordIds.isEmpty())
    }

    @Test
    fun copy_addRelatedRuleIds_ruleIdsUpdated() {
        val original = createExercise(relatedRuleIds = emptyList())
        val modified = original.copy(relatedRuleIds = listOf("r_5"))
        assertEquals(listOf("r_5"), modified.relatedRuleIds)
        assertTrue(original.relatedRuleIds.isEmpty())
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createExercise(), createExercise())
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(createExercise().hashCode(), createExercise().hashCode())
    }

    @Test
    fun equals_differentId_notEqual() {
        assertNotEquals(createExercise(id = "ex_1"), createExercise(id = "ex_2"))
    }

    @Test
    fun equals_differentType_notEqual() {
        assertNotEquals(
            createExercise(type = ExerciseType.CONJUGATE),
            createExercise(type = ExerciseType.CHOOSE_ARTICLE)
        )
    }

    @Test
    fun equals_differentDifficulty_notEqual() {
        assertNotEquals(createExercise(difficulty = 1), createExercise(difficulty = 5))
    }

    @Test
    fun equals_differentExpectedAnswer_notEqual() {
        assertNotEquals(
            createExercise(expectedAnswer = "der Hund"),
            createExercise(expectedAnswer = "die Katze")
        )
    }

    @Test
    fun equals_differentHints_notEqual() {
        assertNotEquals(
            createExercise(hints = listOf("hint A")),
            createExercise(hints = listOf("hint B"))
        )
    }

    @Test
    fun equals_differentRelatedWordIds_notEqual() {
        assertNotEquals(
            createExercise(relatedWordIds = listOf("w_1")),
            createExercise(relatedWordIds = listOf("w_2"))
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ExerciseType
// ═══════════════════════════════════════════════════════════════════════════

class ExerciseTypeTest {

    @Test
    fun entries_size_isEight() {
        assertEquals(8, ExerciseType.entries.size)
    }

    @Test
    fun entries_containsAllExpectedValues() {
        val expected = setOf(
            ExerciseType.FILL_IN_BLANK,
            ExerciseType.TRANSLATE_TO_DE,
            ExerciseType.TRANSLATE_TO_RU,
            ExerciseType.CONJUGATE,
            ExerciseType.CHOOSE_ARTICLE,
            ExerciseType.WORD_ORDER,
            ExerciseType.LISTEN_AND_REPEAT,
            ExerciseType.FREE_RESPONSE
        )
        assertEquals(expected, ExerciseType.entries.toSet())
    }

    @Test
    fun ordinal_fillInBlank_isZero() {
        assertEquals(0, ExerciseType.FILL_IN_BLANK.ordinal)
    }

    @Test
    fun ordinal_translateToDe_isOne() {
        assertEquals(1, ExerciseType.TRANSLATE_TO_DE.ordinal)
    }

    @Test
    fun ordinal_translateToRu_isTwo() {
        assertEquals(2, ExerciseType.TRANSLATE_TO_RU.ordinal)
    }

    @Test
    fun ordinal_conjugate_isThree() {
        assertEquals(3, ExerciseType.CONJUGATE.ordinal)
    }

    @Test
    fun ordinal_chooseArticle_isFour() {
        assertEquals(4, ExerciseType.CHOOSE_ARTICLE.ordinal)
    }

    @Test
    fun ordinal_wordOrder_isFive() {
        assertEquals(5, ExerciseType.WORD_ORDER.ordinal)
    }

    @Test
    fun ordinal_listenAndRepeat_isSix() {
        assertEquals(6, ExerciseType.LISTEN_AND_REPEAT.ordinal)
    }

    @Test
    fun ordinal_freeResponse_isSeven() {
        assertEquals(7, ExerciseType.FREE_RESPONSE.ordinal)
    }

    @Test
    fun valueOf_fillInBlank_returnsFillInBlank() {
        assertEquals(ExerciseType.FILL_IN_BLANK, ExerciseType.valueOf("FILL_IN_BLANK"))
    }

    @Test
    fun valueOf_translateToDe_returnsTranslateToDe() {
        assertEquals(ExerciseType.TRANSLATE_TO_DE, ExerciseType.valueOf("TRANSLATE_TO_DE"))
    }

    @Test
    fun valueOf_freeResponse_returnsFreeResponse() {
        assertEquals(ExerciseType.FREE_RESPONSE, ExerciseType.valueOf("FREE_RESPONSE"))
    }

    @Test
    fun valueOf_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            ExerciseType.valueOf("UNKNOWN_TYPE")
        }
    }

    @Test
    fun valueOf_lowercaseValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            ExerciseType.valueOf("fill_in_blank")
        }
    }

    @Test
    fun defaultExercise_usesDifficultyOne() {
        val exercise = Exercise(
            id = "ex_1",
            type = ExerciseType.FREE_RESPONSE,
            prompt = "Erzähl über dich."
        )
        assertEquals(1, exercise.difficulty)
    }
}
