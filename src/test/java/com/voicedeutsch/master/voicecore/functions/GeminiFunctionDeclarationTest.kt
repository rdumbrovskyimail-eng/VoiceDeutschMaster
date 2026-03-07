// Path: src/test/java/com/voicedeutsch/master/voicecore/functions/GeminiFunctionDeclarationTest.kt
package com.voicedeutsch.master.voicecore.functions

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GeminiFunctionDeclarationTest {

    private fun makeDeclaration(
        name: String = "get_word",
        description: String = "Retrieve a word definition",
        parameters: GeminiParameters? = null,
    ) = GeminiFunctionDeclaration(
        name = name,
        description = description,
        parameters = parameters,
    )

    // ── creation ──────────────────────────────────────────────────────────────

    @Test
    fun creation_withRequiredFields_setsValues() {
        val decl = makeDeclaration()
        assertEquals("get_word", decl.name)
        assertEquals("Retrieve a word definition", decl.description)
    }

    @Test
    fun creation_defaultParameters_isNull() {
        val decl = makeDeclaration()
        assertNull(decl.parameters)
    }

    @Test
    fun creation_withParameters_setsParameters() {
        val params = GeminiParameters(
            type = "object",
            properties = mapOf("word" to GeminiProperty(type = "string", description = "The word")),
            required = listOf("word"),
        )
        val decl = makeDeclaration(parameters = params)
        assertNotNull(decl.parameters)
        assertTrue(decl.parameters!!.properties.containsKey("word"))
    }

    @Test
    fun creation_nullParameters_isParameterlessFunction() {
        val decl = makeDeclaration(parameters = null)
        assertNull(decl.parameters)
    }

    @Test
    fun creation_withEmptyParameters_isValid() {
        val params = GeminiParameters()
        val decl = makeDeclaration(parameters = params)
        assertNotNull(decl.parameters)
        assertTrue(decl.parameters!!.properties.isEmpty())
    }

    // ── copy ──────────────────────────────────────────────────────────────────

    @Test
    fun copy_changesName_restUnchanged() {
        val original = makeDeclaration(name = "get_word", description = "Desc")
        val copy = original.copy(name = "mark_lesson_complete")
        assertEquals("mark_lesson_complete", copy.name)
        assertEquals("Desc", copy.description)
        assertNull(copy.parameters)
    }

    @Test
    fun copy_changesDescription() {
        val original = makeDeclaration(description = "Old description")
        val copy = original.copy(description = "New description")
        assertEquals("New description", copy.description)
    }

    @Test
    fun copy_addsParameters() {
        val original = makeDeclaration(parameters = null)
        val params = GeminiParameters(properties = mapOf("n" to GeminiProperty(type = "integer")))
        val copy = original.copy(parameters = params)
        assertNotNull(copy.parameters)
    }

    @Test
    fun copy_removesParameters() {
        val params = GeminiParameters()
        val original = makeDeclaration(parameters = params)
        val copy = original.copy(parameters = null)
        assertNull(copy.parameters)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalNoParamInstances_areEqual() {
        val a = makeDeclaration()
        val b = makeDeclaration()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentName_areNotEqual() {
        val a = makeDeclaration(name = "func_a")
        val b = makeDeclaration(name = "func_b")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentDescription_areNotEqual() {
        val a = makeDeclaration(description = "Desc A")
        val b = makeDeclaration(description = "Desc B")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_nullVsNonNullParameters_areNotEqual() {
        val a = makeDeclaration(parameters = null)
        val b = makeDeclaration(parameters = GeminiParameters())
        assertNotEquals(a, b)
    }

    @Test
    fun equals_sameParameters_areEqual() {
        val params = GeminiParameters(
            type = "object",
            properties = mapOf("x" to GeminiProperty(type = "string")),
            required = listOf("x"),
        )
        val a = makeDeclaration(parameters = params)
        val b = makeDeclaration(parameters = params.copy())
        assertEquals(a, b)
    }

    // ── practical usage scenarios ─────────────────────────────────────────────

    @Test
    fun declaration_withMultipleProperties_storesAll() {
        val params = GeminiParameters(
            type = "object",
            properties = mapOf(
                "chapter" to GeminiProperty(type = "integer", description = "Chapter number"),
                "lesson" to GeminiProperty(type = "integer", description = "Lesson number"),
            ),
            required = listOf("chapter", "lesson"),
        )
        val decl = makeDeclaration(name = "advance_to_next_lesson", parameters = params)
        assertEquals(2, decl.parameters!!.properties.size)
        assertEquals(2, decl.parameters!!.required.size)
    }

    @Test
    fun declaration_name_isNotBlank() {
        val decl = makeDeclaration(name = "some_function")
        assertTrue(decl.name.isNotBlank())
    }

    @Test
    fun declaration_description_isNotBlank() {
        val decl = makeDeclaration(description = "Does something")
        assertTrue(decl.description.isNotBlank())
    }
}
