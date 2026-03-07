// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/WordKnowledgeEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WordKnowledgeEntityTest {

    private fun createEntity(
        id: String = "wk_001",
        userId: String = "user_001",
        wordId: String = "word_001",
        knowledgeLevel: Int = 0,
        timesSeen: Int = 0,
        timesCorrect: Int = 0,
        timesIncorrect: Int = 0,
        lastSeen: Long? = null,
        lastCorrect: Long? = null,
        lastIncorrect: Long? = null,
        nextReview: Long? = null,
        srsIntervalDays: Float = 0f,
        srsEaseFactor: Float = 2.5f,
        pronunciationScore: Float = 0f,
        pronunciationAttempts: Int = 0,
        contextsJson: String = "[]",
        mistakesJson: String = "[]",
        createdAt: Long = 16_000_000L,
        updatedAt: Long = 16_000_000L,
    ) = WordKnowledgeEntity(
        id = id, userId = userId, wordId = wordId, knowledgeLevel = knowledgeLevel,
        timesSeen = timesSeen, timesCorrect = timesCorrect, timesIncorrect = timesIncorrect,
        lastSeen = lastSeen, lastCorrect = lastCorrect, lastIncorrect = lastIncorrect,
        nextReview = nextReview, srsIntervalDays = srsIntervalDays, srsEaseFactor = srsEaseFactor,
        pronunciationScore = pronunciationScore, pronunciationAttempts = pronunciationAttempts,
        contextsJson = contextsJson, mistakesJson = mistakesJson,
        createdAt = createdAt, updatedAt = updatedAt,
    )

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals("wk_001", entity.id)
        assertEquals("user_001", entity.userId)
        assertEquals("word_001", entity.wordId)
        assertEquals(0, entity.knowledgeLevel)
        assertEquals(0, entity.timesSeen)
        assertEquals(0, entity.timesCorrect)
        assertEquals(0, entity.timesIncorrect)
        assertNull(entity.lastSeen)
        assertNull(entity.lastCorrect)
        assertNull(entity.lastIncorrect)
        assertNull(entity.nextReview)
        assertEquals(0f, entity.srsIntervalDays, 0.001f)
        assertEquals(2.5f, entity.srsEaseFactor, 0.001f)
        assertEquals(0f, entity.pronunciationScore, 0.001f)
        assertEquals(0, entity.pronunciationAttempts)
        assertEquals("[]", entity.contextsJson)
        assertEquals("[]", entity.mistakesJson)
        assertEquals(16_000_000L, entity.createdAt)
        assertEquals(16_000_000L, entity.updatedAt)
    }

    @Test
    fun creation_withTimestamps_allTimestampsAreStored() {
        val entity = createEntity(lastSeen = 1000L, lastCorrect = 2000L, lastIncorrect = 500L, nextReview = 5000L)
        assertEquals(1000L, entity.lastSeen)
        assertEquals(2000L, entity.lastCorrect)
        assertEquals(500L, entity.lastIncorrect)
        assertEquals(5000L, entity.nextReview)
    }

    private fun minimal() = WordKnowledgeEntity(id = "wk_002", userId = "u1", wordId = "w1")

    @Test fun defaultKnowledgeLevel_isZero() = assertEquals(0, minimal().knowledgeLevel)
    @Test fun defaultSrsEaseFactor_is2_5() = assertEquals(2.5f, minimal().srsEaseFactor, 0.001f)
    @Test fun defaultSrsIntervalDays_isZero() = assertEquals(0f, minimal().srsIntervalDays, 0.001f)
    @Test fun defaultPronunciationScore_isZero() = assertEquals(0f, minimal().pronunciationScore, 0.001f)
    @Test fun defaultPronunciationAttempts_isZero() = assertEquals(0, minimal().pronunciationAttempts)
    @Test fun defaultContextsJson_isEmptyArray() = assertEquals("[]", minimal().contextsJson)
    @Test fun defaultMistakesJson_isEmptyArray() = assertEquals("[]", minimal().mistakesJson)
    @Test fun defaultLastSeen_isNull() = assertNull(minimal().lastSeen)
    @Test fun defaultNextReview_isNull() = assertNull(minimal().nextReview)

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = WordKnowledgeEntity(id = "wk_003", userId = "u1", wordId = "w1")
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test
    fun defaultUpdatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = WordKnowledgeEntity(id = "wk_003", userId = "u1", wordId = "w1")
        val after = System.currentTimeMillis()
        assertTrue(entity.updatedAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentWordId_returnsFalse() = assertNotEquals(createEntity(wordId = "word_001"), createEntity(wordId = "word_002"))
    @Test fun equals_differentKnowledgeLevel_returnsFalse() = assertNotEquals(createEntity(knowledgeLevel = 0), createEntity(knowledgeLevel = 5))
    @Test fun equals_differentSrsEaseFactor_returnsFalse() = assertNotEquals(createEntity(srsEaseFactor = 2.5f), createEntity(srsEaseFactor = 1.3f))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_withNewKnowledgeLevel_onlyKnowledgeLevelChanges() {
        val original = createEntity(knowledgeLevel = 0)
        val copied = original.copy(knowledgeLevel = 4)
        assertEquals(4, copied.knowledgeLevel)
        assertEquals(original.wordId, copied.wordId)
        assertEquals(original.userId, copied.userId)
    }

    @Test
    fun copy_withNewSrsEaseFactor_easeFactorUpdated() {
        val copied = createEntity(srsEaseFactor = 2.5f).copy(srsEaseFactor = 1.8f)
        assertEquals(1.8f, copied.srsEaseFactor, 0.001f)
    }

    @Test
    fun copy_incrementTimesSeen_valueUpdated() {
        val original = createEntity(timesSeen = 5)
        val copied = original.copy(timesSeen = 6)
        assertEquals(6, copied.timesSeen)
        assertEquals(original.timesCorrect, copied.timesCorrect)
    }

    @Test
    fun copy_withNewMistakesJson_jsonUpdated() {
        val json = """[{"expected":"der","actual":"die","count":2}]"""
        val copied = createEntity(mistakesJson = "[]").copy(mistakesJson = json)
        assertEquals(json, copied.mistakesJson)
    }

    @Test
    fun copy_setNextReview_nextReviewUpdated() {
        val copied = createEntity(nextReview = null).copy(nextReview = 99_000_000L)
        assertEquals(99_000_000L, copied.nextReview)
    }

    @Test fun srsEaseFactor_minimumValue_isStoredCorrectly() = assertEquals(1.3f, createEntity(srsEaseFactor = 1.3f).srsEaseFactor, 0.001f)
    @Test fun srsIntervalDays_halfDay_isStoredCorrectly() = assertEquals(0.5f, createEntity(srsIntervalDays = 0.5f).srsIntervalDays, 0.001f)
    @Test fun pronunciationScore_highScore_isStoredCorrectly() = assertEquals(0.97f, createEntity(pronunciationScore = 0.97f).pronunciationScore, 0.001f)

    @Test
    fun contextsJson_withData_isStoredCorrectly() {
        val json = """[{"sentence":"Der Hund bellt.","translation":"Собака лает."}]"""
        assertEquals(json, createEntity(contextsJson = json).contextsJson)
    }
}
