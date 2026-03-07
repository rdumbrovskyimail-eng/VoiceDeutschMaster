// Путь: src/test/java/com/voicedeutsch/master/voicecore/functions/ProgressFunctionsTest.kt
package com.voicedeutsch.master.voicecore.functions

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProgressFunctionsTest {

    private val declarations = ProgressFunctions.declarations

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
    fun declarations_firstIsUpdateUserLevel() {
        assertEquals("update_user_level", declarations[0].name)
    }

    @Test
    fun declarations_secondIsGetUserStatistics() {
        assertEquals("get_user_statistics", declarations[1].name)
    }

    // ── update_user_level ─────────────────────────────────────────────────

    @Test
    fun updateUserLevel_parametersIsNull() {
        assertNull(declarations[0].parameters)
    }

    @Test
    fun updateUserLevel_descriptionIsNotBlank() {
        assertTrue(declarations[0].description.isNotBlank())
    }

    @Test
    fun updateUserLevel_descriptionMentionsCefr() {
        assertTrue(declarations[0].description.contains("CEFR"))
    }

    // ── get_user_statistics ───────────────────────────────────────────────

    @Test
    fun getUserStatistics_parametersIsNull() {
        assertNull(declarations[1].parameters)
    }

    @Test
    fun getUserStatistics_descriptionIsNotBlank() {
        assertTrue(declarations[1].description.isNotBlank())
    }
}
