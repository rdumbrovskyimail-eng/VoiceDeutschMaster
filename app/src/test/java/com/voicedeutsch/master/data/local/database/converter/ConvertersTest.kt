// Путь: src/test/java/com/voicedeutsch/master/data/local/database/converter/ConvertersTest.kt
package com.voicedeutsch.master.data.local.database.converter

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConvertersTest {

    private lateinit var sut: Converters

    @BeforeEach
    fun setUp() {
        sut = Converters()
    }

    // ── fromStringList ────────────────────────────────────────────────────────

    @Test
    fun fromStringList_emptyList_producesEmptyJsonArray() {
        assertEquals("[]", sut.fromStringList(emptyList()))
    }

    @Test
    fun fromStringList_singleElement_producesJsonArray() {
        val result = sut.fromStringList(listOf("hello"))
        assertEquals("[\"hello\"]", result)
    }

    @Test
    fun fromStringList_multipleElements_producesJsonArray() {
        val result = sut.fromStringList(listOf("a", "b", "c"))
        assertEquals("[\"a\",\"b\",\"c\"]", result)
    }

    @Test
    fun fromStringList_elementWithSpecialChars_encodedCorrectly() {
        val result = sut.fromStringList(listOf("hello world", "foo/bar"))
        assertTrue(result.contains("hello world"))
        assertTrue(result.contains("foo/bar"))
    }

    @Test
    fun fromStringList_elementWithQuotes_escapedCorrectly() {
        val result = sut.fromStringList(listOf("say \"hi\""))
        assertTrue(result.isNotBlank())
        assertDoesNotThrow { sut.toStringList(result) }
    }

    @Test
    fun fromStringList_germanUmlauts_encodedAndDecodable() {
        val input = listOf("Schüler", "Größe", "Straße")
        val encoded = sut.fromStringList(input)
        val decoded = sut.toStringList(encoded)
        assertEquals(input, decoded)
    }

    // ── toStringList ──────────────────────────────────────────────────────────

    @Test
    fun toStringList_emptyJsonArray_returnsEmptyList() {
        assertEquals(emptyList<String>(), sut.toStringList("[]"))
    }

    @Test
    fun toStringList_validJsonArray_returnsCorrectList() {
        val result = sut.toStringList("[\"a\",\"b\",\"c\"]")
        assertEquals(listOf("a", "b", "c"), result)
    }

    @Test
    fun toStringList_singleElementArray_returnsListWithOneElement() {
        val result = sut.toStringList("[\"hello\"]")
        assertEquals(listOf("hello"), result)
    }

    @Test
    fun toStringList_invalidJson_returnsEmptyList() {
        assertEquals(emptyList<String>(), sut.toStringList("{not_valid}"))
    }

    @Test
    fun toStringList_emptyString_returnsEmptyList() {
        assertEquals(emptyList<String>(), sut.toStringList(""))
    }

    @Test
    fun toStringList_blankString_returnsEmptyList() {
        assertEquals(emptyList<String>(), sut.toStringList("   "))
    }

    @Test
    fun toStringList_nullLiteralString_returnsEmptyList() {
        assertEquals(emptyList<String>(), sut.toStringList("null"))
    }

    @Test
    fun toStringList_malformedArray_returnsEmptyList() {
        assertEquals(emptyList<String>(), sut.toStringList("[\"a\","))
    }

    // ── Roundtrip ─────────────────────────────────────────────────────────────

    @Test
    fun roundtrip_emptyList_preserved() {
        val original = emptyList<String>()
        assertEquals(original, sut.toStringList(sut.fromStringList(original)))
    }

    @Test
    fun roundtrip_singleElement_preserved() {
        val original = listOf("Hund")
        assertEquals(original, sut.toStringList(sut.fromStringList(original)))
    }

    @Test
    fun roundtrip_multipleElements_preserved() {
        val original = listOf("ctx1", "ctx2", "ctx3")
        assertEquals(original, sut.toStringList(sut.fromStringList(original)))
    }

    @Test
    fun roundtrip_elementWithSpaces_preserved() {
        val original = listOf("guten morgen", "wie geht es dir")
        assertEquals(original, sut.toStringList(sut.fromStringList(original)))
    }

    @Test
    fun roundtrip_largeList_preserved() {
        val original = (1..100).map { "item_$it" }
        assertEquals(original, sut.toStringList(sut.fromStringList(original)))
    }

    @Test
    fun roundtrip_duplicateElements_preserved() {
        val original = listOf("word", "word", "word")
        assertEquals(original, sut.toStringList(sut.fromStringList(original)))
    }

    @Test
    fun roundtrip_unicodeContent_preserved() {
        val original = listOf("Привет", "日本語", "العربية")
        assertEquals(original, sut.toStringList(sut.fromStringList(original)))
    }
}
