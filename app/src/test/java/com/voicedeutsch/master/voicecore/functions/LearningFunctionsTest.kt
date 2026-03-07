// Путь: src/test/java/com/voicedeutsch/master/voicecore/functions/LearningFunctionsTest.kt
package com.voicedeutsch.master.voicecore.functions

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LearningFunctionsTest {

    private val declarations = LearningFunctions.declarations

    // ── list integrity ────────────────────────────────────────────────────

    @Test
    fun declarations_containsExactly2Functions() {
        assertEquals(2, declarations.size)
    }

    @Test
    fun declarations_namesAreUnique() {
        val names = declarations.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun declarations_noBlankNames() {
        declarations.forEach { assertTrue(it.name.isNotBlank()) }
    }

    @Test
    fun declarations_noBlankDescriptions() {
        declarations.forEach { assertTrue(it.description.isNotBlank()) }
    }

    // ── declaration order ─────────────────────────────────────────────────

    @Test
    fun declarations_firstIsSavePronunciationResult() {
        assertEquals("save_pronunciation_result", declarations[0].name)
    }

    @Test
    fun declarations_secondIsGetPronunciationTargets() {
        assertEquals("get_pronunciation_targets", declarations[1].name)
    }

    // ── save_pronunciation_result ─────────────────────────────────────────

    @Test
    fun savePronunciationResult_parametersNotNull() {
        assertNotNull(declarations[0].parameters)
    }

    @Test
    fun savePronunciationResult_hasWordParam_typeString() {
        val params = declarations[0].parameters!!
        assertEquals("string", params.properties["word"]?.type)
    }

    @Test
    fun savePronunciationResult_hasScoreParam_typeNumber() {
        val params = declarations[0].parameters!!
        assertEquals("number", params.properties["score"]?.type)
    }

    @Test
    fun savePronunciationResult_hasProblemSoundsParam_typeArray() {
        val params = declarations[0].parameters!!
        assertEquals("array", params.properties["problem_sounds"]?.type)
    }

    @Test
    fun savePronunciationResult_hasAttemptNumberParam_typeInteger() {
        val params = declarations[0].parameters!!
        assertEquals("integer", params.properties["attempt_number"]?.type)
    }

    @Test
    fun savePronunciationResult_requiredContainsWordAndScore() {
        val required = declarations[0].parameters!!.required
        assertTrue(required.contains("word"))
        assertTrue(required.contains("score"))
    }

    @Test
    fun savePronunciationResult_problemSoundsAndAttemptNotRequired() {
        val required = declarations[0].parameters!!.required
        assertFalse(required.contains("problem_sounds"))
        assertFalse(required.contains("attempt_number"))
    }

    @Test
    fun savePronunciationResult_problemSoundsDescriptionMentionsExamples() {
        val description = declarations[0].parameters!!.properties["problem_sounds"]?.description ?: ""
        assertTrue(description.isNotBlank())
    }

    @Test
    fun savePronunciationResult_parametersTypeIsObject() {
        assertEquals("object", declarations[0].parameters!!.type)
    }

    // ── get_pronunciation_targets ─────────────────────────────────────────

    @Test
    fun getPronunciationTargets_parametersIsNull() {
        assertNull(declarations[1].parameters)
    }

    @Test
    fun getPronunciationTargets_descriptionIsNotBlank() {
        assertTrue(declarations[1].description.isNotBlank())
    }
}
