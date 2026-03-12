// Путь: src/test/java/com/voicedeutsch/master/voicecore/functions/BookFunctionsTest.kt
package com.voicedeutsch.master.voicecore.functions

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BookFunctionsTest {

    private val declarations = BookFunctions.declarations

    // ── declarations list ────────────────────────────────────────────────

    @Test
    fun declarations_containsExactly4Functions() {
        assertEquals(4, declarations.size)
    }

    @Test
    fun declarations_namesAreUnique() {
        val names = declarations.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    // ── get_current_lesson ───────────────────────────────────────────────

    @Test
    fun getCurrentLesson_declarationExists() {
        assertNotNull(declarations.find { it.name == "get_current_lesson" })
    }

    @Test
    fun getCurrentLesson_descriptionIsNotBlank() {
        val decl = declarations.first { it.name == "get_current_lesson" }
        assertTrue(decl.description.isNotBlank())
    }

    @Test
    fun getCurrentLesson_hasNoRequiredParams() {
        val decl = declarations.first { it.name == "get_current_lesson" }
        assertNull(decl.parameters)
    }

    // ── advance_to_next_lesson ───────────────────────────────────────────

    @Test
    fun advanceToNextLesson_declarationExists() {
        assertNotNull(declarations.find { it.name == "advance_to_next_lesson" })
    }

    @Test
    fun advanceToNextLesson_descriptionIsNotBlank() {
        val decl = declarations.first { it.name == "advance_to_next_lesson" }
        assertTrue(decl.description.isNotBlank())
    }

    @Test
    fun advanceToNextLesson_hasScoreParam() {
        val decl = declarations.first { it.name == "advance_to_next_lesson" }
        assertNotNull(decl.parameters?.properties?.get("score"))
    }

    @Test
    fun advanceToNextLesson_scoreParamTypeIsNumber() {
        val decl = declarations.first { it.name == "advance_to_next_lesson" }
        assertEquals("number", decl.parameters?.properties?.get("score")?.type)
    }

    @Test
    fun advanceToNextLesson_scoreIsRequired() {
        val decl = declarations.first { it.name == "advance_to_next_lesson" }
        assertTrue((decl.parameters?.required ?: emptyList()).contains("score"))
    }

    // ── mark_lesson_complete ─────────────────────────────────────────────

    @Test
    fun markLessonComplete_declarationExists() {
        assertNotNull(declarations.find { it.name == "mark_lesson_complete" })
    }

    @Test
    fun markLessonComplete_descriptionIsNotBlank() {
        val decl = declarations.first { it.name == "mark_lesson_complete" }
        assertTrue(decl.description.isNotBlank())
    }

    @Test
    fun markLessonComplete_hasScoreParam() {
        val decl = declarations.first { it.name == "mark_lesson_complete" }
        assertNotNull(decl.parameters?.properties?.get("score"))
    }

    @Test
    fun markLessonComplete_scoreParamTypeIsNumber() {
        val decl = declarations.first { it.name == "mark_lesson_complete" }
        assertEquals("number", decl.parameters?.properties?.get("score")?.type)
    }

    // ── read_lesson_paragraph ────────────────────────────────────────────

    @Test
    fun readLessonParagraph_declarationExists() {
        assertNotNull(declarations.find { it.name == "read_lesson_paragraph" })
    }

    @Test
    fun readLessonParagraph_descriptionIsNotBlank() {
        val decl = declarations.first { it.name == "read_lesson_paragraph" }
        assertTrue(decl.description.isNotBlank())
    }

    @Test
    fun readLessonParagraph_descriptionMentionsIndexZeroBased() {
        val decl = declarations.first { it.name == "read_lesson_paragraph" }
        assertTrue(decl.description.contains("0"))
    }

    @Test
    fun readLessonParagraph_descriptionMentionsHasNext() {
        val decl = declarations.first { it.name == "read_lesson_paragraph" }
        assertTrue(decl.description.contains("has_next"))
    }

    @Test
    fun readLessonParagraph_hasIndexParam() {
        val decl = declarations.first { it.name == "read_lesson_paragraph" }
        assertNotNull(decl.parameters?.properties?.get("index"))
    }

    @Test
    fun readLessonParagraph_indexParamTypeIsInteger() {
        val decl = declarations.first { it.name == "read_lesson_paragraph" }
        assertEquals("integer", decl.parameters?.properties?.get("index")?.type)
    }

    @Test
    fun readLessonParagraph_indexIsRequired() {
        val decl = declarations.first { it.name == "read_lesson_paragraph" }
        assertTrue((decl.parameters?.required ?: emptyList()).contains("index"))
    }

    // ── declaration order ────────────────────────────────────────────────

    @Test
    fun declarations_firstIsGetCurrentLesson() {
        assertEquals("get_current_lesson", declarations[0].name)
    }

    @Test
    fun declarations_secondIsAdvanceToNextLesson() {
        assertEquals("advance_to_next_lesson", declarations[1].name)
    }

    @Test
    fun declarations_thirdIsMarkLessonComplete() {
        assertEquals("mark_lesson_complete", declarations[2].name)
    }

    @Test
    fun declarations_fourthIsReadLessonParagraph() {
        assertEquals("read_lesson_paragraph", declarations[3].name)
    }
}
