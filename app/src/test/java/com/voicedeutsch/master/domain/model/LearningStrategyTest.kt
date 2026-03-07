// Путь: src/test/java/com/voicedeutsch/master/domain/model/LearningStrategyTest.kt
package com.voicedeutsch.master.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LearningStrategyTest {

    // ── entries ───────────────────────────────────────────────────────────

    @Test
    fun entries_size_isNine() {
        assertEquals(9, LearningStrategy.entries.size)
    }

    @Test
    fun entries_containsLinearBook() {
        assertTrue(LearningStrategy.entries.contains(LearningStrategy.LINEAR_BOOK))
    }

    @Test
    fun entries_containsGapFilling() {
        assertTrue(LearningStrategy.entries.contains(LearningStrategy.GAP_FILLING))
    }

    @Test
    fun entries_containsRepetition() {
        assertTrue(LearningStrategy.entries.contains(LearningStrategy.REPETITION))
    }

    @Test
    fun entries_containsFreePractice() {
        assertTrue(LearningStrategy.entries.contains(LearningStrategy.FREE_PRACTICE))
    }

    @Test
    fun entries_containsPronunciation() {
        assertTrue(LearningStrategy.entries.contains(LearningStrategy.PRONUNCIATION))
    }

    @Test
    fun entries_containsGrammarDrill() {
        assertTrue(LearningStrategy.entries.contains(LearningStrategy.GRAMMAR_DRILL))
    }

    @Test
    fun entries_containsVocabularyBoost() {
        assertTrue(LearningStrategy.entries.contains(LearningStrategy.VOCABULARY_BOOST))
    }

    @Test
    fun entries_containsListening() {
        assertTrue(LearningStrategy.entries.contains(LearningStrategy.LISTENING))
    }

    @Test
    fun entries_containsAssessment() {
        assertTrue(LearningStrategy.entries.contains(LearningStrategy.ASSESSMENT))
    }

    // ── ordinals ──────────────────────────────────────────────────────────

    @Test
    fun ordinal_linearBook_isZero() {
        assertEquals(0, LearningStrategy.LINEAR_BOOK.ordinal)
    }

    @Test
    fun ordinal_gapFilling_isOne() {
        assertEquals(1, LearningStrategy.GAP_FILLING.ordinal)
    }

    @Test
    fun ordinal_repetition_isTwo() {
        assertEquals(2, LearningStrategy.REPETITION.ordinal)
    }

    @Test
    fun ordinal_freePractice_isThree() {
        assertEquals(3, LearningStrategy.FREE_PRACTICE.ordinal)
    }

    @Test
    fun ordinal_pronunciation_isFour() {
        assertEquals(4, LearningStrategy.PRONUNCIATION.ordinal)
    }

    @Test
    fun ordinal_grammarDrill_isFive() {
        assertEquals(5, LearningStrategy.GRAMMAR_DRILL.ordinal)
    }

    @Test
    fun ordinal_vocabularyBoost_isSix() {
        assertEquals(6, LearningStrategy.VOCABULARY_BOOST.ordinal)
    }

    @Test
    fun ordinal_listening_isSeven() {
        assertEquals(7, LearningStrategy.LISTENING.ordinal)
    }

    @Test
    fun ordinal_assessment_isEight() {
        assertEquals(8, LearningStrategy.ASSESSMENT.ordinal)
    }

    // ── displayNameRu ─────────────────────────────────────────────────────

    @Test
    fun displayNameRu_linearBook_isCorrect() {
        assertEquals("Прохождение книги", LearningStrategy.LINEAR_BOOK.displayNameRu)
    }

    @Test
    fun displayNameRu_gapFilling_isCorrect() {
        assertEquals("Заполнение пробелов", LearningStrategy.GAP_FILLING.displayNameRu)
    }

    @Test
    fun displayNameRu_repetition_isCorrect() {
        assertEquals("Повторение", LearningStrategy.REPETITION.displayNameRu)
    }

    @Test
    fun displayNameRu_freePractice_isCorrect() {
        assertEquals("Свободная практика", LearningStrategy.FREE_PRACTICE.displayNameRu)
    }

    @Test
    fun displayNameRu_pronunciation_isCorrect() {
        assertEquals("Произношение", LearningStrategy.PRONUNCIATION.displayNameRu)
    }

    @Test
    fun displayNameRu_grammarDrill_isCorrect() {
        assertEquals("Грамматический штурм", LearningStrategy.GRAMMAR_DRILL.displayNameRu)
    }

    @Test
    fun displayNameRu_vocabularyBoost_isCorrect() {
        assertEquals("Словарный бросок", LearningStrategy.VOCABULARY_BOOST.displayNameRu)
    }

    @Test
    fun displayNameRu_listening_isCorrect() {
        assertEquals("Аудирование", LearningStrategy.LISTENING.displayNameRu)
    }

    @Test
    fun displayNameRu_assessment_isCorrect() {
        assertEquals("Оценка уровня", LearningStrategy.ASSESSMENT.displayNameRu)
    }

    // ── description ───────────────────────────────────────────────────────

    @Test
    fun description_allEntries_nonEmpty() {
        LearningStrategy.entries.forEach { strategy ->
            assertTrue(
                strategy.description.isNotBlank(),
                "description for ${strategy.name} must not be blank"
            )
        }
    }

    @Test
    fun description_linearBook_isCorrect() {
        assertEquals(
            "Последовательное прохождение материала книги",
            LearningStrategy.LINEAR_BOOK.description
        )
    }

    @Test
    fun description_repetition_isCorrect() {
        assertEquals(
            "Интервальное повторение накопленного материала",
            LearningStrategy.REPETITION.description
        )
    }

    @Test
    fun description_assessment_isCorrect() {
        assertEquals(
            "Определение текущего уровня пользователя",
            LearningStrategy.ASSESSMENT.description
        )
    }

    // ── valueOf ───────────────────────────────────────────────────────────

    @Test
    fun valueOf_linearBook_returnsCorrectEntry() {
        assertEquals(LearningStrategy.LINEAR_BOOK, LearningStrategy.valueOf("LINEAR_BOOK"))
    }

    @Test
    fun valueOf_assessment_returnsCorrectEntry() {
        assertEquals(LearningStrategy.ASSESSMENT, LearningStrategy.valueOf("ASSESSMENT"))
    }

    @Test
    fun valueOf_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            LearningStrategy.valueOf("UNKNOWN")
        }
    }

    // ── fromString ────────────────────────────────────────────────────────

    @Test
    fun fromString_validName_returnsMatchingStrategy() {
        assertEquals(LearningStrategy.REPETITION, LearningStrategy.fromString("REPETITION"))
    }

    @Test
    fun fromString_linearBook_returnsLinearBook() {
        assertEquals(LearningStrategy.LINEAR_BOOK, LearningStrategy.fromString("LINEAR_BOOK"))
    }

    @Test
    fun fromString_gapFilling_returnsGapFilling() {
        assertEquals(LearningStrategy.GAP_FILLING, LearningStrategy.fromString("GAP_FILLING"))
    }

    @Test
    fun fromString_freePractice_returnsFreePractice() {
        assertEquals(LearningStrategy.FREE_PRACTICE, LearningStrategy.fromString("FREE_PRACTICE"))
    }

    @Test
    fun fromString_pronunciation_returnsPronunciation() {
        assertEquals(LearningStrategy.PRONUNCIATION, LearningStrategy.fromString("PRONUNCIATION"))
    }

    @Test
    fun fromString_grammarDrill_returnsGrammarDrill() {
        assertEquals(LearningStrategy.GRAMMAR_DRILL, LearningStrategy.fromString("GRAMMAR_DRILL"))
    }

    @Test
    fun fromString_vocabularyBoost_returnsVocabularyBoost() {
        assertEquals(LearningStrategy.VOCABULARY_BOOST, LearningStrategy.fromString("VOCABULARY_BOOST"))
    }

    @Test
    fun fromString_listening_returnsListening() {
        assertEquals(LearningStrategy.LISTENING, LearningStrategy.fromString("LISTENING"))
    }

    @Test
    fun fromString_assessment_returnsAssessment() {
        assertEquals(LearningStrategy.ASSESSMENT, LearningStrategy.fromString("ASSESSMENT"))
    }

    @Test
    fun fromString_unknownValue_defaultsToLinearBook() {
        assertEquals(LearningStrategy.LINEAR_BOOK, LearningStrategy.fromString("UNKNOWN"))
    }

    @Test
    fun fromString_emptyString_defaultsToLinearBook() {
        assertEquals(LearningStrategy.LINEAR_BOOK, LearningStrategy.fromString(""))
    }

    @Test
    fun fromString_lowercaseName_defaultsToLinearBook() {
        assertEquals(LearningStrategy.LINEAR_BOOK, LearningStrategy.fromString("repetition"))
    }

    @Test
    fun fromString_mixedCaseName_defaultsToLinearBook() {
        assertEquals(LearningStrategy.LINEAR_BOOK, LearningStrategy.fromString("Repetition"))
    }

    @Test
    fun fromString_nameWithSpaces_defaultsToLinearBook() {
        assertEquals(LearningStrategy.LINEAR_BOOK, LearningStrategy.fromString("LINEAR BOOK"))
    }

    // ── displayNameRu uniqueness ──────────────────────────────────────────

    @Test
    fun displayNameRu_allEntries_areUnique() {
        val names = LearningStrategy.entries.map { it.displayNameRu }
        assertEquals(names.size, names.toSet().size)
    }

    // ── description uniqueness ────────────────────────────────────────────

    @Test
    fun description_allEntries_areUnique() {
        val descriptions = LearningStrategy.entries.map { it.description }
        assertEquals(descriptions.size, descriptions.toSet().size)
    }
}
