// Path: src/test/java/com/voicedeutsch/master/domain/model/book/LessonVocabularyEntryAndFocusTest.kt
package com.voicedeutsch.master.domain.model.book

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ════════════════════════════════════════════════════════════════════════════
// LessonFocus
// ════════════════════════════════════════════════════════════════════════════

class LessonFocusTest {

    @Test
    fun entries_size_equals7() {
        assertEquals(7, LessonFocus.entries.size)
    }

    @Test
    fun entries_containsVocabulary() {
        assertTrue(LessonFocus.entries.contains(LessonFocus.VOCABULARY))
    }

    @Test
    fun entries_containsGrammar() {
        assertTrue(LessonFocus.entries.contains(LessonFocus.GRAMMAR))
    }

    @Test
    fun entries_containsPronunciation() {
        assertTrue(LessonFocus.entries.contains(LessonFocus.PRONUNCIATION))
    }

    @Test
    fun entries_containsListening() {
        assertTrue(LessonFocus.entries.contains(LessonFocus.LISTENING))
    }

    @Test
    fun entries_containsReading() {
        assertTrue(LessonFocus.entries.contains(LessonFocus.READING))
    }

    @Test
    fun entries_containsSpeaking() {
        assertTrue(LessonFocus.entries.contains(LessonFocus.SPEAKING))
    }

    @Test
    fun entries_containsMixed() {
        assertTrue(LessonFocus.entries.contains(LessonFocus.MIXED))
    }

    @Test
    fun valueOf_vocabulary_returnsCorrect() {
        assertEquals(LessonFocus.VOCABULARY, LessonFocus.valueOf("VOCABULARY"))
    }

    @Test
    fun valueOf_mixed_returnsCorrect() {
        assertEquals(LessonFocus.MIXED, LessonFocus.valueOf("MIXED"))
    }

    @Test
    fun valueOf_unknown_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            LessonFocus.valueOf("UNKNOWN_FOCUS")
        }
    }

    @Test
    fun allEntries_haveUniqueName() {
        val names = LessonFocus.entries.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// LessonVocabularyEntry
// ════════════════════════════════════════════════════════════════════════════

class LessonVocabularyEntryTest {

    private fun makeEntry(
        german: String = "Haus",
        russian: String = "дом",
        gender: String? = null,
        plural: String? = null,
        level: String = "A1",
    ) = LessonVocabularyEntry(
        german = german,
        russian = russian,
        gender = gender,
        plural = plural,
        level = level,
    )

    @Test
    fun creation_withRequiredFields_setsValues() {
        val entry = makeEntry()
        assertEquals("Haus", entry.german)
        assertEquals("дом", entry.russian)
    }

    @Test
    fun creation_defaultGender_isNull() {
        val entry = makeEntry()
        assertNull(entry.gender)
    }

    @Test
    fun creation_defaultPlural_isNull() {
        val entry = makeEntry()
        assertNull(entry.plural)
    }

    @Test
    fun creation_defaultLevel_isA1() {
        val entry = makeEntry()
        assertEquals("A1", entry.level)
    }

    @Test
    fun creation_withGender_setsGender() {
        val entry = makeEntry(gender = "n")
        assertEquals("n", entry.gender)
    }

    @Test
    fun creation_withPlural_setsPlural() {
        val entry = makeEntry(plural = "Häuser")
        assertEquals("Häuser", entry.plural)
    }

    @Test
    fun creation_withLevel_setsLevel() {
        val entry = makeEntry(level = "B1")
        assertEquals("B1", entry.level)
    }

    @Test
    fun copy_changesGerman_restUnchanged() {
        val original = makeEntry(german = "Haus", russian = "дом")
        val copy = original.copy(german = "Auto")
        assertEquals("Auto", copy.german)
        assertEquals("дом", copy.russian)
        assertEquals("A1", copy.level)
    }

    @Test
    fun copy_changesGender() {
        val original = makeEntry(gender = null)
        val copy = original.copy(gender = "m")
        assertEquals("m", copy.gender)
    }

    @Test
    fun copy_changesLevel() {
        val original = makeEntry(level = "A1")
        val copy = original.copy(level = "A2")
        assertEquals("A2", copy.level)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeEntry(german = "Haus", russian = "дом", gender = "n")
        val b = makeEntry(german = "Haus", russian = "дом", gender = "n")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentGerman_areNotEqual() {
        val a = makeEntry(german = "Haus")
        val b = makeEntry(german = "Auto")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentLevel_areNotEqual() {
        val a = makeEntry(level = "A1")
        val b = makeEntry(level = "B2")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_nullVsNonNullGender_areNotEqual() {
        val a = makeEntry(gender = null)
        val b = makeEntry(gender = "m")
        assertNotEquals(a, b)
    }

    @Test
    fun creation_withAllFemaleNoun_setsAll() {
        val entry = LessonVocabularyEntry(
            german = "Frau",
            russian = "женщина",
            gender = "f",
            plural = "Frauen",
            level = "A1",
        )
        assertEquals("Frau", entry.german)
        assertEquals("женщина", entry.russian)
        assertEquals("f", entry.gender)
        assertEquals("Frauen", entry.plural)
        assertEquals("A1", entry.level)
    }
}
