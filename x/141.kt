// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/SessionEventEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SessionEventEntityTest {

    private fun createEntity(
        id: String = "se_001",
        sessionId: String = "sess_001",
        eventType: String = "word_learned",
        timestamp: Long = 12_000_000L,
        detailsJson: String = "{}",
        createdAt: Long = 12_000_100L,
    ) = SessionEventEntity(
        id = id, sessionId = sessionId, eventType = eventType,
        timestamp = timestamp, detailsJson = detailsJson, createdAt = createdAt,
    )

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals("se_001", entity.id)
        assertEquals("sess_001", entity.sessionId)
        assertEquals("word_learned", entity.eventType)
        assertEquals(12_000_000L, entity.timestamp)
        assertEquals("{}", entity.detailsJson)
        assertEquals(12_000_100L, entity.createdAt)
    }

    @Test fun creation_eventTypeRuleExplained_isStored() = assertEquals("rule_explained", createEntity(eventType = "rule_explained").eventType)
    @Test fun creation_eventTypeExercise_isStored() = assertEquals("exercise_completed", createEntity(eventType = "exercise_completed").eventType)
    @Test fun creation_eventTypePronunciation_isStored() = assertEquals("pronunciation_evaluated", createEntity(eventType = "pronunciation_evaluated").eventType)

    @Test
    fun defaultDetailsJson_isEmptyObject() {
        val entity = SessionEventEntity(id = "se_002", sessionId = "sess_001", eventType = "word_learned", timestamp = 100L)
        assertEquals("{}", entity.detailsJson)
    }

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = SessionEventEntity(id = "se_003", sessionId = "sess_001", eventType = "word_learned", timestamp = 100L)
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentEventType_returnsFalse() = assertNotEquals(createEntity(eventType = "word_learned"), createEntity(eventType = "rule_explained"))
    @Test fun equals_differentSessionId_returnsFalse() = assertNotEquals(createEntity(sessionId = "sess_001"), createEntity(sessionId = "sess_002"))
    @Test fun equals_differentTimestamp_returnsFalse() = assertNotEquals(createEntity(timestamp = 1000L), createEntity(timestamp = 2000L))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_withNewEventType_onlyEventTypeChanges() {
        val original = createEntity(eventType = "word_learned")
        val copied = original.copy(eventType = "mistake_made")
        assertEquals("mistake_made", copied.eventType)
        assertEquals(original.id, copied.id)
        assertEquals(original.sessionId, copied.sessionId)
    }

    @Test
    fun copy_withNewDetailsJson_jsonUpdated() {
        val newJson = """{"wordId":"w_001","score":0.9}"""
        val copied = createEntity(detailsJson = "{}").copy(detailsJson = newJson)
        assertEquals(newJson, copied.detailsJson)
    }

    @Test
    fun copy_withNewTimestamp_timestampUpdated() {
        val copied = createEntity(timestamp = 1000L).copy(timestamp = 9999L)
        assertEquals(9999L, copied.timestamp)
    }

    @Test
    fun detailsJson_complexPayload_isStoredCorrectly() {
        val json = """{"wordId":"w_42","isCorrect":true,"pronunciationScore":0.87,"attempts":3}"""
        assertEquals(json, createEntity(detailsJson = json).detailsJson)
    }

    @Test
    fun timestamp_exactValue_isPreserved() {
        val ts = 1_741_234_567_890L
        assertEquals(ts, createEntity(timestamp = ts).timestamp)
    }
}
