// Path: src/test/java/com/voicedeutsch/master/voicecore/functions/GeminiParametersAndPropertyTest.kt
package com.voicedeutsch.master.voicecore.functions

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ════════════════════════════════════════════════════════════════════════════
// GeminiProperty
// ════════════════════════════════════════════════════════════════════════════

class GeminiPropertyTest {

    private fun makeProperty(
        type: String = "string",
        description: String = "",
        enum: List<String>? = null,
    ) = GeminiProperty(type = type, description = description, enum = enum)

    @Test
    fun creation_withType_setsType() {
        val prop = makeProperty(type = "string")
        assertEquals("string", prop.type)
    }

    @Test
    fun creation_defaultDescription_isEmpty() {
        val prop = makeProperty()
        assertEquals("", prop.description)
    }

    @Test
    fun creation_defaultEnum_isNull() {
        val prop = makeProperty()
        assertNull(prop.enum)
    }

    @Test
    fun creation_withDescription_setsDescription() {
        val prop = makeProperty(description = "The chapter number")
        assertEquals("The chapter number", prop.description)
    }

    @Test
    fun creation_withEnumValues_setsEnum() {
        val prop = makeProperty(enum = listOf("A1", "A2", "B1", "B2"))
        assertNotNull(prop.enum)
        assertEquals(4, prop.enum!!.size)
        assertTrue(prop.enum!!.contains("B1"))
    }

    @Test
    fun creation_integerType_isValid() {
        val prop = makeProperty(type = "integer")
        assertEquals("integer", prop.type)
    }

    @Test
    fun creation_booleanType_isValid() {
        val prop = makeProperty(type = "boolean")
        assertEquals("boolean", prop.type)
    }

    @Test
    fun copy_changesType_restUnchanged() {
        val original = makeProperty(type = "string", description = "Desc")
        val copy = original.copy(type = "integer")
        assertEquals("integer", copy.type)
        assertEquals("Desc", copy.description)
        assertNull(copy.enum)
    }

    @Test
    fun copy_addsEnum() {
        val original = makeProperty(enum = null)
        val copy = original.copy(enum = listOf("yes", "no"))
        assertEquals(2, copy.enum!!.size)
    }

    @Test
    fun copy_removesEnum() {
        val original = makeProperty(enum = listOf("a", "b"))
        val copy = original.copy(enum = null)
        assertNull(copy.enum)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeProperty(type = "string", description = "D", enum = listOf("x"))
        val b = makeProperty(type = "string", description = "D", enum = listOf("x"))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentType_areNotEqual() {
        val a = makeProperty(type = "string")
        val b = makeProperty(type = "integer")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_nullVsNonNullEnum_areNotEqual() {
        val a = makeProperty(enum = null)
        val b = makeProperty(enum = listOf("A1"))
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentEnumContent_areNotEqual() {
        val a = makeProperty(enum = listOf("A1", "A2"))
        val b = makeProperty(enum = listOf("B1", "B2"))
        assertNotEquals(a, b)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// GeminiParameters
// ════════════════════════════════════════════════════════════════════════════

class GeminiParametersTest {

    private fun makeParameters(
        type: String = "object",
        properties: Map<String, GeminiProperty> = emptyMap(),
        required: List<String> = emptyList(),
    ) = GeminiParameters(type = type, properties = properties, required = required)

    @Test
    fun creation_defaultType_isObject() {
        val params = GeminiParameters()
        assertEquals("object", params.type)
    }

    @Test
    fun creation_defaultProperties_isEmpty() {
        val params = GeminiParameters()
        assertTrue(params.properties.isEmpty())
    }

    @Test
    fun creation_defaultRequired_isEmpty() {
        val params = GeminiParameters()
        assertTrue(params.required.isEmpty())
    }

    @Test
    fun creation_withProperties_setsProperties() {
        val props = mapOf(
            "word" to GeminiProperty(type = "string", description = "German word"),
        )
        val params = makeParameters(properties = props)
        assertEquals(1, params.properties.size)
        assertEquals("string", params.properties["word"]!!.type)
    }

    @Test
    fun creation_withRequired_setsRequired() {
        val params = makeParameters(required = listOf("word", "score"))
        assertEquals(2, params.required.size)
        assertTrue(params.required.contains("word"))
    }

    @Test
    fun creation_withMultipleProperties_storesAll() {
        val params = makeParameters(
            properties = mapOf(
                "chapter" to GeminiProperty(type = "integer"),
                "lesson" to GeminiProperty(type = "integer"),
                "score" to GeminiProperty(type = "number"),
            ),
        )
        assertEquals(3, params.properties.size)
        assertTrue(params.properties.containsKey("chapter"))
        assertTrue(params.properties.containsKey("lesson"))
        assertTrue(params.properties.containsKey("score"))
    }

    @Test
    fun copy_changesRequired_restUnchanged() {
        val original = makeParameters(type = "object", required = listOf("word"))
        val copy = original.copy(required = listOf("word", "score"))
        assertEquals(2, copy.required.size)
        assertEquals("object", copy.type)
    }

    @Test
    fun copy_changesType() {
        val original = makeParameters(type = "object")
        val copy = original.copy(type = "array")
        assertEquals("array", copy.type)
    }

    @Test
    fun copy_addsProperties() {
        val original = makeParameters(properties = emptyMap())
        val copy = original.copy(
            properties = mapOf("x" to GeminiProperty(type = "string")),
        )
        assertEquals(1, copy.properties.size)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeParameters(type = "object", required = listOf("w"))
        val b = makeParameters(type = "object", required = listOf("w"))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentType_areNotEqual() {
        val a = makeParameters(type = "object")
        val b = makeParameters(type = "array")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentRequired_areNotEqual() {
        val a = makeParameters(required = listOf("word"))
        val b = makeParameters(required = listOf("score"))
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentProperties_areNotEqual() {
        val a = makeParameters(properties = mapOf("x" to GeminiProperty(type = "string")))
        val b = makeParameters(properties = mapOf("y" to GeminiProperty(type = "integer")))
        assertNotEquals(a, b)
    }

    @Test
    fun propertyWithEnum_isStoredCorrectly() {
        val params = makeParameters(
            properties = mapOf(
                "level" to GeminiProperty(
                    type = "string",
                    description = "CEFR level",
                    enum = listOf("A1", "A2", "B1", "B2", "C1"),
                ),
            ),
        )
        val levelProp = params.properties["level"]!!
        assertNotNull(levelProp.enum)
        assertEquals(5, levelProp.enum!!.size)
    }
}
