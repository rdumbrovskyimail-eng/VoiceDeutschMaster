// Путь: src/test/java/com/voicedeutsch/master/data/local/JsonFactoryTest.kt
package com.voicedeutsch.master.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonFactoryTest {

    // ── Singleton ─────────────────────────────────────────────────────────────

    @Test
    fun instance_isSameObjectOnMultipleCalls() {
        val a = JsonFactory.instance
        val b = JsonFactory.instance
        assertSame(a, b)
    }

    @Test
    fun instance_isNotNull() {
        assertNotNull(JsonFactory.instance)
    }

    // ── ignoreUnknownKeys ─────────────────────────────────────────────────────

    @Test
    fun ignoreUnknownKeys_extraFieldInJson_doesNotThrow() {
        @Serializable data class Simple(val name: String)
        val json = """{"name":"Max","unknownField":"ignored"}"""
        assertDoesNotThrow {
            JsonFactory.instance.decodeFromString<Simple>(json)
        }
    }

    @Test
    fun ignoreUnknownKeys_extraFieldInJson_parsesKnownField() {
        @Serializable data class Simple(val name: String)
        val json = """{"name":"Max","extra":123}"""
        val result = JsonFactory.instance.decodeFromString<Simple>(json)
        assertEquals("Max", result.name)
    }

    // ── isLenient ─────────────────────────────────────────────────────────────

    @Test
    fun isLenient_unquotedStringValue_parsedCorrectly() {
        @Serializable data class Simple(val name: String)
        // Lenient mode allows unquoted string values
        val json = """{"name":Max}"""
        assertDoesNotThrow {
            JsonFactory.instance.decodeFromString<Simple>(json)
        }
    }

    @Test
    fun isLenient_singleQuotedStrings_parsedCorrectly() {
        @Serializable data class Simple(val name: String)
        val json = """{'name':'Max'}"""
        assertDoesNotThrow {
            JsonFactory.instance.decodeFromString<Simple>(json)
        }
    }

    // ── encodeDefaults ────────────────────────────────────────────────────────

    @Test
    fun encodeDefaults_fieldWithDefaultValue_includedInJson() {
        @Serializable data class WithDefault(val name: String = "default", val count: Int = 0)
        val obj = WithDefault()
        val encoded = JsonFactory.instance.encodeToString(WithDefault.serializer(), obj)
        assertTrue(encoded.contains("name"))
        assertTrue(encoded.contains("count"))
    }

    @Test
    fun encodeDefaults_defaultValue_encodedWithCorrectValue() {
        @Serializable data class WithDefault(val level: String = "A1")
        val obj = WithDefault()
        val encoded = JsonFactory.instance.encodeToString(WithDefault.serializer(), obj)
        assertTrue(encoded.contains("A1"))
    }

    // ── coerceInputValues ─────────────────────────────────────────────────────

    @Test
    fun coerceInputValues_nullForNonNullableWithDefault_usesDefault() {
        @Serializable data class WithDefault(val count: Int = 42)
        val json = """{"count":null}"""
        val result = JsonFactory.instance.decodeFromString<WithDefault>(json)
        assertEquals(42, result.count)
    }

    @Test
    fun coerceInputValues_invalidEnumValue_usesDefault() {
        @Serializable
        enum class Status { ACTIVE, INACTIVE }

        @Serializable data class WithEnum(val status: Status = Status.INACTIVE)
        val json = """{"status":"UNKNOWN_VALUE"}"""
        assertDoesNotThrow {
            val result = JsonFactory.instance.decodeFromString<WithEnum>(json)
            assertEquals(Status.INACTIVE, result.status)
        }
    }

    // ── prettyPrint = false ───────────────────────────────────────────────────

    @Test
    fun prettyPrint_false_outputHasNoNewlines() {
        @Serializable data class Simple(val name: String, val level: Int)
        val encoded = JsonFactory.instance.encodeToString(
            Simple.serializer(),
            Simple("Max", 5)
        )
        assertFalse(encoded.contains("\n"))
    }

    @Test
    fun prettyPrint_false_outputHasNoLeadingSpaces() {
        @Serializable data class Simple(val a: String, val b: String)
        val encoded = JsonFactory.instance.encodeToString(
            Simple.serializer(),
            Simple("x", "y")
        )
        assertFalse(encoded.contains("  "))
    }

    // ── Combined behavior ─────────────────────────────────────────────────────

    @Test
    fun instance_roundtrip_simpleObject_preservesValues() {
        @Serializable data class User(val id: String, val level: Int, val active: Boolean = true)
        val original = User(id = "user_1", level = 3, active = false)
        val encoded = JsonFactory.instance.encodeToString(User.serializer(), original)
        val decoded = JsonFactory.instance.decodeFromString<User>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun instance_roundtrip_listOfStrings_preservesContent() {
        val original = listOf("Hund", "Katze", "Maus")
        val encoded = JsonFactory.instance.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer()),
            original
        )
        val decoded = JsonFactory.instance.decodeFromString<List<String>>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun instance_emptyList_encodedAsEmptyArray() {
        val encoded = JsonFactory.instance.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer()),
            emptyList<String>()
        )
        assertEquals("[]", encoded)
    }

    @Test
    fun instance_nestedObject_encodedAndDecodedCorrectly() {
        @Serializable data class Inner(val value: String)
        @Serializable data class Outer(val inner: Inner, val tag: String = "default")
        val original = Outer(inner = Inner("hello"), tag = "test")
        val encoded = JsonFactory.instance.encodeToString(Outer.serializer(), original)
        val decoded = JsonFactory.instance.decodeFromString<Outer>(encoded)
        assertEquals(original, decoded)
    }
}
