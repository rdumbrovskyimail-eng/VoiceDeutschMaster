// Путь: src/test/java/com/voicedeutsch/master/domain/model/knowledge/MistakeLogTest.kt
package com.voicedeutsch.master.domain.model.knowledge

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MistakeLogTest {

    private fun createMistakeLog(
        id: String = "mistake_001",
        userId: String = "user_1",
        sessionId: String? = "session_1",
        type: MistakeType = MistakeType.GRAMMAR,
        item: String = "ich bin",
        expected: String = "ich habe",
        actual: String = "ich bin",
        context: String = "",
        explanation: String = "",
        timestamp: Long = 1_000L,
        createdAt: Long = 1_000L,
    ) = MistakeLog(
        id = id,
        userId = userId,
        sessionId = sessionId,
        type = type,
        item = item,
        expected = expected,
        actual = actual,
        context = context,
        explanation = explanation,
        timestamp = timestamp,
        createdAt = createdAt,
    )

    // ── MistakeLog — creation ─────────────────────────────────────────────

    @Test
    fun creation_requiredFields_storedCorrectly() {
        val log = createMistakeLog(id = "m1", userId = "u1")
        assertEquals("m1", log.id)
        assertEquals("u1", log.userId)
    }

    @Test
    fun creation_sessionId_nullable_storedCorrectly() {
        val withSession = createMistakeLog(sessionId = "s1")
        val withoutSession = createMistakeLog(sessionId = null)
        assertEquals("s1", withSession.sessionId)
        assertNull(withoutSession.sessionId)
    }

    @Test
    fun creation_type_storedCorrectly() {
        assertEquals(MistakeType.PRONUNCIATION, createMistakeLog(type = MistakeType.PRONUNCIATION).type)
    }

    @Test
    fun creation_itemExpectedActual_storedCorrectly() {
        val log = createMistakeLog(item = "Hund", expected = "der Hund", actual = "die Hund")
        assertEquals("Hund", log.item)
        assertEquals("der Hund", log.expected)
        assertEquals("die Hund", log.actual)
    }

    @Test
    fun creation_defaultContext_isEmpty() {
        assertEquals("", createMistakeLog().context)
    }

    @Test
    fun creation_defaultExplanation_isEmpty() {
        assertEquals("", createMistakeLog().explanation)
    }

    @Test
    fun creation_withContext_storedCorrectly() {
        val log = createMistakeLog(context = "In einem Dialog")
        assertEquals("In einem Dialog", log.context)
    }

    @Test
    fun creation_withExplanation_storedCorrectly() {
        val log = createMistakeLog(explanation = "Verwende 'haben' mit Partizip II")
        assertEquals("Verwende 'haben' mit Partizip II", log.explanation)
    }

    @Test
    fun creation_timestamp_storedCorrectly() {
        assertEquals(5_000L, createMistakeLog(timestamp = 5_000L).timestamp)
    }

    @Test
    fun creation_createdAt_storedCorrectly() {
        assertEquals(9_000L, createMistakeLog(createdAt = 9_000L).createdAt)
    }

    @Test
    fun creation_defaultTimestamp_isPositive() {
        assertTrue(MistakeLog(
            id = "x", userId = "u", sessionId = null,
            type = MistakeType.WORD, item = "W",
            expected = "E", actual = "A"
        ).timestamp > 0L)
    }

    // ── equals / hashCode / copy ──────────────────────────────────────────

    @Test
    fun equals_twoIdentical_returnsTrue() {
        assertEquals(createMistakeLog(), createMistakeLog())
    }

    @Test
    fun equals_differentId_returnsFalse() {
        assertNotEquals(createMistakeLog(id = "a"), createMistakeLog(id = "b"))
    }

    @Test
    fun equals_differentType_returnsFalse() {
        assertNotEquals(
            createMistakeLog(type = MistakeType.GRAMMAR),
            createMistakeLog(type = MistakeType.WORD),
        )
    }

    @Test
    fun equals_nullVsNonNullSessionId_returnsFalse() {
        assertNotEquals(
            createMistakeLog(sessionId = "s1"),
            createMistakeLog(sessionId = null),
        )
    }

    @Test
    fun hashCode_equalLogs_sameHash() {
        assertEquals(createMistakeLog().hashCode(), createMistakeLog().hashCode())
    }

    @Test
    fun copy_changesOnlySpecifiedField() {
        val original = createMistakeLog(type = MistakeType.GRAMMAR)
        val copied = original.copy(type = MistakeType.PRONUNCIATION)
        assertEquals(MistakeType.PRONUNCIATION, copied.type)
        assertEquals(original.id, copied.id)
        assertEquals(original.userId, copied.userId)
    }

    @Test
    fun copy_nullifySessionId() {
        val original = createMistakeLog(sessionId = "s1")
        val copied = original.copy(sessionId = null)
        assertNull(copied.sessionId)
    }

    // ── MistakeType enum ──────────────────────────────────────────────────

    @Test
    fun mistakeType_entryCount_equals4() {
        assertEquals(4, MistakeType.entries.size)
    }

    @Test
    fun mistakeType_containsAllExpectedValues() {
        val expected = setOf(
            MistakeType.WORD,
            MistakeType.GRAMMAR,
            MistakeType.PRONUNCIATION,
            MistakeType.PHRASE,
        )
        assertEquals(expected, MistakeType.entries.toSet())
    }

    @Test
    fun mistakeType_valueOf_word() {
        assertEquals(MistakeType.WORD, MistakeType.valueOf("WORD"))
    }

    @Test
    fun mistakeType_valueOf_grammar() {
        assertEquals(MistakeType.GRAMMAR, MistakeType.valueOf("GRAMMAR"))
    }

    @Test
    fun mistakeType_valueOf_pronunciation() {
        assertEquals(MistakeType.PRONUNCIATION, MistakeType.valueOf("PRONUNCIATION"))
    }

    @Test
    fun mistakeType_valueOf_phrase() {
        assertEquals(MistakeType.PHRASE, MistakeType.valueOf("PHRASE"))
    }

    @Test
    fun mistakeType_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            MistakeType.valueOf("SPELLING")
        }
    }

    @Test
    fun mistakeType_ordinalsAreUnique() {
        val ordinals = MistakeType.entries.map { it.ordinal }
        assertEquals(ordinals.size, ordinals.toSet().size)
    }
}
