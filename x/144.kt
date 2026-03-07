// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/PronunciationRecordEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PronunciationRecordEntityTest {

    private fun createEntity(
        id: String = "pr_001",
        userId: String = "user_001",
        word: String = "Straße",
        score: Float = 0.75f,
        problemSoundsJson: String = "[]",
        attemptNumber: Int = 1,
        sessionId: String? = "sess_001",
        timestamp: Long = 9_000_000L,
        createdAt: Long = 9_000_100L,
    ) = PronunciationRecordEntity(
        id = id, userId = userId, word = word, score = score,
        problemSoundsJson = problemSoundsJson, attemptNumber = attemptNumber,
        sessionId = sessionId, timestamp = timestamp, createdAt = createdAt,
    )

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals("pr_001", entity.id)
        assertEquals("user_001", entity.userId)
        assertEquals("Straße", entity.word)
        assertEquals(0.75f, entity.score, 0.001f)
        assertEquals("[]", entity.problemSoundsJson)
        assertEquals(1, entity.attemptNumber)
        assertEquals("sess_001", entity.sessionId)
        assertEquals(9_000_000L, entity.timestamp)
        assertEquals(9_000_100L, entity.createdAt)
    }

    @Test fun creation_withNullSessionId_sessionIdIsNull() = assertNull(createEntity(sessionId = null).sessionId)

    @Test
    fun creation_withProblemSounds_jsonIsStored() {
        val json = """["ß","ü","ö"]"""
        assertEquals(json, createEntity(problemSoundsJson = json).problemSoundsJson)
    }

    private fun minimal() = PronunciationRecordEntity(id = "pr_002", userId = "u1", word = "Haus", timestamp = 100L)

    @Test fun defaultScore_isZero() = assertEquals(0f, minimal().score, 0.001f)
    @Test fun defaultProblemSoundsJson_isEmptyArray() = assertEquals("[]", minimal().problemSoundsJson)
    @Test fun defaultAttemptNumber_isOne() = assertEquals(1, minimal().attemptNumber)
    @Test fun defaultSessionId_isNull() = assertNull(minimal().sessionId)

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = PronunciationRecordEntity(id = "pr_003", userId = "u1", word = "Haus", timestamp = 100L)
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentWord_returnsFalse() = assertNotEquals(createEntity(word = "Straße"), createEntity(word = "Haus"))
    @Test fun equals_differentScore_returnsFalse() = assertNotEquals(createEntity(score = 0.5f), createEntity(score = 0.9f))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_withNewScore_onlyScoreChanges() {
        val original = createEntity(score = 0.5f)
        val copied = original.copy(score = 0.95f)
        assertEquals(0.95f, copied.score, 0.001f)
        assertEquals(original.word, copied.word)
        assertEquals(original.userId, copied.userId)
    }

    @Test
    fun copy_incrementAttemptNumber_valueUpdated() {
        val copied = createEntity(attemptNumber = 1).copy(attemptNumber = 2)
        assertEquals(2, copied.attemptNumber)
    }

    @Test
    fun copy_withNewProblemSounds_jsonUpdated() {
        val copied = createEntity(problemSoundsJson = "[]").copy(problemSoundsJson = """["ß"]""")
        assertEquals("""["ß"]""", copied.problemSoundsJson)
    }

    @Test fun score_perfectScore_isOne() = assertEquals(1.0f, createEntity(score = 1.0f).score, 0.001f)
    @Test fun score_zeroScore_isZero() = assertEquals(0f, createEntity(score = 0f).score, 0.001f)
    @Test fun attemptNumber_multipleAttempts_isStoredCorrectly() = assertEquals(10, createEntity(attemptNumber = 10).attemptNumber)
}
