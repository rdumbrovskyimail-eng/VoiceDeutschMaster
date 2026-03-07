// Путь: src/test/java/com/voicedeutsch/master/voicecore/functions/GeminiFunctionDeclarationTest.kt
package com.voicedeutsch.master.voicecore.functions

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GeminiFunctionDeclarationTest {

    // ── GeminiFunctionDeclaration — creation ─────────────────────────────

    @Test
    fun declaration_minimalConstruction_parametersIsNull() {
        val decl = GeminiFunctionDeclaration(name = "test_func", description = "desc")
        assertNull(decl.parameters)
    }

    @Test
    fun declaration_withParameters_parametersIsNotNull() {
        val params = GeminiParameters(
            properties = mapOf("key" to GeminiProperty(type = "string")),
            required = listOf("key"),
        )
        val decl = GeminiFunctionDeclaration(
            name = "test_func",
            description = "desc",
            parameters = params,
        )
        assertNotNull(decl.parameters)
    }

    @Test
    fun declaration_nameStoredCorrectly() {
        val decl = GeminiFunctionDeclaration(name = "my_function", description = "d")
        assertEquals("my_function", decl.name)
    }

    @Test
    fun declaration_descriptionStoredCorrectly() {
        val decl = GeminiFunctionDeclaration(name = "f", description = "my description")
        assertEquals("my description", decl.description)
    }

    @Test
    fun declaration_equals_twoIdenticalNullParams() {
        val a = GeminiFunctionDeclaration(name = "f", description = "d")
        val b = GeminiFunctionDeclaration(name = "f", description = "d")
        assertEquals(a, b)
    }

    @Test
    fun declaration_equals_twoIdenticalWithParams() {
        val params = GeminiParameters(required = listOf("x"))
        val a = GeminiFunctionDeclaration(name = "f", description = "d", parameters = params)
        val b = GeminiFunctionDeclaration(name = "f", description = "d", parameters = params.copy())
        assertEquals(a, b)
    }

    @Test
    fun declaration_notEquals_differentName() {
        val a = GeminiFunctionDeclaration(name = "func_a", description = "d")
        val b = GeminiFunctionDeclaration(name = "func_b", description = "d")
        assertNotEquals(a, b)
    }

    @Test
    fun declaration_notEquals_oneNullParamsOneNot() {
        val a = GeminiFunctionDeclaration(name = "f", description = "d", parameters = null)
        val b = GeminiFunctionDeclaration(
            name = "f",
            description = "d",
            parameters = GeminiParameters(),
        )
        assertNotEquals(a, b)
    }

    @Test
    fun declaration_copy_changesOnlySpecifiedField() {
        val original = GeminiFunctionDeclaration(name = "original", description = "desc")
        val copied = original.copy(name = "updated")
        assertEquals("updated", copied.name)
        assertEquals("desc", copied.description)
        assertNull(copied.parameters)
    }

    @Test
    fun declaration_hashCode_equalObjectsHaveSameHash() {
        val a = GeminiFunctionDeclaration(name = "f", description = "d")
        val b = GeminiFunctionDeclaration(name = "f", description = "d")
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ── GeminiParameters — creation ──────────────────────────────────────

    @Test
    fun parameters_defaultType_isObject() {
        assertEquals("object", GeminiParameters().type)
    }

    @Test
    fun parameters_defaultProperties_isEmpty() {
        assertTrue(GeminiParameters().properties.isEmpty())
    }

    @Test
    fun parameters_defaultRequired_isEmpty() {
        assertTrue(GeminiParameters().required.isEmpty())
    }

    @Test
    fun parameters_withProperties_storesCorrectly() {
        val props = mapOf(
            "score" to GeminiProperty(type = "number", description = "Score 0-1"),
        )
        val params = GeminiParameters(properties = props)
        assertEquals(1, params.properties.size)
        assertEquals("number", params.properties["score"]?.type)
    }

    @Test
    fun parameters_withRequired_storesCorrectly() {
        val params = GeminiParameters(required = listOf("score", "index"))
        assertEquals(listOf("score", "index"), params.required)
    }

    @Test
    fun parameters_equals_twoIdentical() {
        val a = GeminiParameters(required = listOf("x"))
        val b = GeminiParameters(required = listOf("x"))
        assertEquals(a, b)
    }

    @Test
    fun parameters_notEquals_differentRequired() {
        val a = GeminiParameters(required = listOf("x"))
        val b = GeminiParameters(required = listOf("y"))
        assertNotEquals(a, b)
    }

    @Test
    fun parameters_copy_changesOnlySpecifiedField() {
        val original = GeminiParameters(type = "object", required = listOf("a"))
        val copied = original.copy(required = listOf("b"))
        assertEquals("object", copied.type)
        assertEquals(listOf("b"), copied.required)
    }

    @Test
    fun parameters_hashCode_equalObjectsHaveSameHash() {
        val a = GeminiParameters(required = listOf("x"))
        val b = GeminiParameters(required = listOf("x"))
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ── GeminiProperty — creation ────────────────────────────────────────

    @Test
    fun property_typeStoredCorrectly() {
        val prop = GeminiProperty(type = "string")
        assertEquals("string", prop.type)
    }

    @Test
    fun property_defaultDescription_isEmpty() {
        assertEquals("", GeminiProperty(type = "integer").description)
    }

    @Test
    fun property_defaultEnum_isNull() {
        assertNull(GeminiProperty(type = "string").enum)
    }

    @Test
    fun property_withEnum_storesCorrectly() {
        val prop = GeminiProperty(type = "string", enum = listOf("A1", "A2", "B1"))
        assertEquals(listOf("A1", "A2", "B1"), prop.enum)
    }

    @Test
    fun property_withDescription_storesCorrectly() {
        val prop = GeminiProperty(type = "number", description = "Score 0.0-1.0")
        assertEquals("Score 0.0-1.0", prop.description)
    }

    @Test
    fun property_equals_twoIdentical() {
        val a = GeminiProperty(type = "string", description = "desc", enum = listOf("x"))
        val b = GeminiProperty(type = "string", description = "desc", enum = listOf("x"))
        assertEquals(a, b)
    }

    @Test
    fun property_notEquals_differentType() {
        val a = GeminiProperty(type = "string")
        val b = GeminiProperty(type = "integer")
        assertNotEquals(a, b)
    }

    @Test
    fun property_notEquals_nullEnumVsNonNull() {
        val a = GeminiProperty(type = "string", enum = null)
        val b = GeminiProperty(type = "string", enum = listOf("x"))
        assertNotEquals(a, b)
    }

    @Test
    fun property_copy_changesOnlySpecifiedField() {
        val original = GeminiProperty(type = "string", description = "original")
        val copied = original.copy(description = "updated")
        assertEquals("string", copied.type)
        assertEquals("updated", copied.description)
        assertNull(copied.enum)
    }

    @Test
    fun property_hashCode_equalObjectsHaveSameHash() {
        val a = GeminiProperty(type = "number", description = "d")
        val b = GeminiProperty(type = "number", description = "d")
        assertEquals(a.hashCode(), b.hashCode())
    }
}
