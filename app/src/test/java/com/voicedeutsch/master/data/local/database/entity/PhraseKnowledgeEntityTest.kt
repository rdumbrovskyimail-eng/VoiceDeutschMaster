// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/PhraseKnowledgeEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PhraseKnowledgeEntityTest {

    private fun createEntity(
        id: String = "pk_001",
        userId: String = "user_001",
        phraseId: String = "ph_001",
        knowledgeLevel: Int = 0,
        timesPracticed: Int = 0,
        timesCorrect: Int = 0,
        lastPracticed: Long? = null,
        nextReview: Long? = null,
        srsIntervalDays: Float = 0f,
        srsEaseFactor: Float = 2.5f,
        pronunciationScore: Float = 0f,
        createdAt: Long = 8_000_000L,
        updatedAt: Long = 8_000_000L,
    ) = PhraseKnowledgeEntity(
        id = id, userId = userId, phraseId = phraseId,
        knowledgeLevel = knowledgeLevel, timesPracticed = timesPracticed,
        timesCorrect = timesCorrect, lastPracticed = lastPracticed,
        nextReview = nextReview, srsIntervalDays = srsIntervalDays,
        srsEaseFactor = srsEaseFactor, pronunciationScore = pronunciationScore,
        createdAt = createdAt, updatedAt = updatedAt,
    )

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals("pk_001", entity.id)
        assertEquals("user_001", entity.userId)
        assertEquals("ph_001", entity.phraseId)
        assertEquals(0, entity.knowledgeLevel)
        assertEquals(0, entity.timesPracticed)
        assertEquals(0, entity.timesCorrect)
        assertNull(entity.lastPracticed)
        assertNull(entity.nextReview)
        assertEquals(0f, entity.srsIntervalDays, 0.001f)
        assertEquals(2.5f, entity.srsEaseFactor, 0.001f)
        assertEquals(0f, entity.pronunciationScore, 0.001f)
    }

    @Test
    fun creation_withTimestamps_timestampsAreStored() {
        val entity = createEntity(lastPracticed = 1000L, nextReview = 5000L)
        assertEquals(1000L, entity.lastPracticed)
        assertEquals(5000L, entity.nextReview)
    }

    private fun minimal() = PhraseKnowledgeEntity(id = "pk_002", userId = "u1", phraseId = "ph_002")

    @Test fun defaultKnowledgeLevel_isZero() = assertEquals(0, minimal().knowledgeLevel)
    @Test fun defaultSrsEaseFactor_is2_5() = assertEquals(2.5f, minimal().srsEaseFactor, 0.001f)
    @Test fun defaultSrsIntervalDays_isZero() = assertEquals(0f, minimal().srsIntervalDays, 0.001f)
    @Test fun defaultPronunciationScore_isZero() = assertEquals(0f, minimal().pronunciationScore, 0.001f)
    @Test fun defaultLastPracticed_isNull() = assertNull(minimal().lastPracticed)
    @Test fun defaultNextReview_isNull() = assertNull(minimal().nextReview)

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = PhraseKnowledgeEntity(id = "pk_003", userId = "u1", phraseId = "ph_003")
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentPhraseId_returnsFalse() = assertNotEquals(createEntity(phraseId = "ph_001"), createEntity(phraseId = "ph_002"))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_withNewKnowledgeLevel_onlyKnowledgeLevelChanges() {
        val original = createEntity(knowledgeLevel = 0)
        val copied = original.copy(knowledgeLevel = 3)
        assertEquals(3, copied.knowledgeLevel)
        assertEquals(original.id, copied.id)
        assertEquals(original.phraseId, copied.phraseId)
    }

    @Test
    fun copy_withNewSrsEaseFactor_easeFactorUpdated() {
        val copied = createEntity(srsEaseFactor = 2.5f).copy(srsEaseFactor = 3.0f)
        assertEquals(3.0f, copied.srsEaseFactor, 0.001f)
    }

    @Test
    fun copy_setPronunciationScore_scoreUpdated() {
        val copied = createEntity(pronunciationScore = 0f).copy(pronunciationScore = 0.92f)
        assertEquals(0.92f, copied.pronunciationScore, 0.001f)
    }

    @Test fun srsIntervalDays_fractional_isStoredCorrectly() = assertEquals(1.5f, createEntity(srsIntervalDays = 1.5f).srsIntervalDays, 0.001f)
    @Test fun knowledgeLevel_maxValue_isStored() = assertEquals(Int.MAX_VALUE, createEntity(knowledgeLevel = Int.MAX_VALUE).knowledgeLevel)
}
