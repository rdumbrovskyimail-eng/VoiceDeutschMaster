// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/MistakeLogEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MistakeLogEntityTest {

    private fun createEntity(
        id: String = "ml_001",
        userId: String = "user_001",
        sessionId: String? = "sess_001",
        type: String = "vocabulary",
        item: String = "der Hund",
        expected: String = "собака",
        actual: String = "кошка",
        context: String = "Was ist das?",
        explanation: String = "Hund = собака, не кошка",
        timestamp: Long = 6_000_000L,
        createdAt: Long = 6_000_100L,
    ) = MistakeLogEntity(
        id = id, userId = userId, sessionId = sessionId, type = type,
        item = item, expected = expected, actual = actual,
        context = context, explanation = explanation,
        timestamp = timestamp, createdAt = createdAt,
    )

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals("ml_001", entity.id)
        assertEquals("user_001", entity.userId)
        assertEquals("sess_001", entity.sessionId)
        assertEquals("vocabulary", entity.type)
        assertEquals("der Hund", entity.item)
        assertEquals("собака", entity.expected)
        assertEquals("кошка", entity.actual)
        assertEquals("Was ist das?", entity.context)
        assertEquals("Hund = собака, не кошка", entity.explanation)
        assertEquals(6_000_000L, entity.timestamp)
        assertEquals(6_000_100L, entity.createdAt)
    }

    @Test fun creation_withNullSessionId_sessionIdIsNull() = assertNull(createEntity(sessionId = null).sessionId)
    @Test fun creation_typeGrammar_isStored() = assertEquals("grammar", createEntity(type = "grammar").type)
    @Test fun creation_typePronunciation_isStored() = assertEquals("pronunciation", createEntity(type = "pronunciation").type)

    private fun minimal() = MistakeLogEntity(
        id = "ml_002", userId = "u1", type = "vocabulary",
        item = "Hund", expected = "dog", actual = "cat", timestamp = 100L,
    )

    @Test fun defaultContext_isEmptyString() = assertEquals("", minimal().context)
    @Test fun defaultExplanation_isEmptyString() = assertEquals("", minimal().explanation)
    @Test fun defaultSessionId_isNull() = assertNull(minimal().sessionId)

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = MistakeLogEntity(id = "ml_003", userId = "u1", type = "vocabulary", item = "Hund", expected = "dog", actual = "cat", timestamp = 100L)
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentItem_returnsFalse() = assertNotEquals(createEntity(item = "der Hund"), createEntity(item = "die Katze"))
    @Test fun equals_differentType_returnsFalse() = assertNotEquals(createEntity(type = "vocabulary"), createEntity(type = "grammar"))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_withNewExplanation_onlyExplanationChanges() {
        val original = createEntity(explanation = "old explanation")
        val copied = original.copy(explanation = "new explanation")
        assertEquals("new explanation", copied.explanation)
        assertEquals(original.id, copied.id)
        assertEquals(original.type, copied.type)
    }

    @Test fun copy_setSessionId_sessionIdUpdated() = assertEquals("sess_new", createEntity(sessionId = null).copy(sessionId = "sess_new").sessionId)
    @Test fun copy_clearSessionId_becomesNull() = assertNull(createEntity(sessionId = "sess_001").copy(sessionId = null).sessionId)

    @Test
    fun timestamp_isStoredExactly() {
        val ts = 1_741_234_567_890L
        assertEquals(ts, createEntity(timestamp = ts).timestamp)
    }

    @Test
    fun createdAt_canDifferFromTimestamp() {
        val entity = createEntity(timestamp = 100L, createdAt = 200L)
        assertNotEquals(entity.timestamp, entity.createdAt)
    }
}
