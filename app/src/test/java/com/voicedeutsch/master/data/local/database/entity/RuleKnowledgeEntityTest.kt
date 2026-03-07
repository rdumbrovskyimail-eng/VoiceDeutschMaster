// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/RuleKnowledgeEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RuleKnowledgeEntityTest {

    private fun createEntity(
        id: String = "rk_001",
        userId: String = "user_001",
        ruleId: String = "gr_001",
        knowledgeLevel: Int = 0,
        timesPracticed: Int = 0,
        timesCorrect: Int = 0,
        timesIncorrect: Int = 0,
        lastPracticed: Long? = null,
        nextReview: Long? = null,
        srsIntervalDays: Float = 0f,
        srsEaseFactor: Float = 2.5f,
        commonMistakesJson: String = "[]",
        createdAt: Long = 10_000_000L,
        updatedAt: Long = 10_000_000L,
    ) = RuleKnowledgeEntity(
        id = id, userId = userId, ruleId = ruleId, knowledgeLevel = knowledgeLevel,
        timesPracticed = timesPracticed, timesCorrect = timesCorrect,
        timesIncorrect = timesIncorrect, lastPracticed = lastPracticed,
        nextReview = nextReview, srsIntervalDays = srsIntervalDays,
        srsEaseFactor = srsEaseFactor, commonMistakesJson = commonMistakesJson,
        createdAt = createdAt, updatedAt = updatedAt,
    )

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals("rk_001", entity.id)
        assertEquals("user_001", entity.userId)
        assertEquals("gr_001", entity.ruleId)
        assertEquals(0, entity.knowledgeLevel)
        assertEquals(0, entity.timesPracticed)
        assertEquals(0, entity.timesCorrect)
        assertEquals(0, entity.timesIncorrect)
        assertNull(entity.lastPracticed)
        assertNull(entity.nextReview)
        assertEquals(0f, entity.srsIntervalDays, 0.001f)
        assertEquals(2.5f, entity.srsEaseFactor, 0.001f)
        assertEquals("[]", entity.commonMistakesJson)
        assertEquals(10_000_000L, entity.createdAt)
        assertEquals(10_000_000L, entity.updatedAt)
    }

    @Test
    fun creation_withTimestamps_timestampsAreStored() {
        val entity = createEntity(lastPracticed = 1000L, nextReview = 5000L)
        assertEquals(1000L, entity.lastPracticed)
        assertEquals(5000L, entity.nextReview)
    }

    private fun minimal() = RuleKnowledgeEntity(id = "rk_002", userId = "u1", ruleId = "gr_002")

    @Test fun defaultKnowledgeLevel_isZero() = assertEquals(0, minimal().knowledgeLevel)
    @Test fun defaultSrsEaseFactor_is2_5() = assertEquals(2.5f, minimal().srsEaseFactor, 0.001f)
    @Test fun defaultCommonMistakesJson_isEmptyArray() = assertEquals("[]", minimal().commonMistakesJson)
    @Test fun defaultTimesIncorrect_isZero() = assertEquals(0, minimal().timesIncorrect)

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = RuleKnowledgeEntity(id = "rk_003", userId = "u1", ruleId = "gr_003")
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test
    fun defaultUpdatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = RuleKnowledgeEntity(id = "rk_003", userId = "u1", ruleId = "gr_003")
        val after = System.currentTimeMillis()
        assertTrue(entity.updatedAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentRuleId_returnsFalse() = assertNotEquals(createEntity(ruleId = "gr_001"), createEntity(ruleId = "gr_002"))
    @Test fun equals_differentKnowledgeLevel_returnsFalse() = assertNotEquals(createEntity(knowledgeLevel = 0), createEntity(knowledgeLevel = 5))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_withNewKnowledgeLevel_onlyKnowledgeLevelChanges() {
        val copied = createEntity(knowledgeLevel = 2).copy(knowledgeLevel = 5)
        assertEquals(5, copied.knowledgeLevel)
    }

    @Test
    fun copy_withNewCommonMistakesJson_jsonUpdated() {
        val json = """[{"mistake":"wrong case","count":3}]"""
        val copied = createEntity(commonMistakesJson = "[]").copy(commonMistakesJson = json)
        assertEquals(json, copied.commonMistakesJson)
    }

    @Test
    fun copy_withNewUpdatedAt_updatedAtChanges() {
        val original = createEntity(updatedAt = 1000L)
        val copied = original.copy(updatedAt = 9999L)
        assertEquals(9999L, copied.updatedAt)
        assertEquals(original.createdAt, copied.createdAt)
    }

    @Test
    fun timesCorrectAndIncorrect_canBeTrackedIndependently() {
        val entity = createEntity(timesCorrect = 7, timesIncorrect = 3, timesPracticed = 10)
        assertEquals(7, entity.timesCorrect)
        assertEquals(3, entity.timesIncorrect)
        assertEquals(10, entity.timesPracticed)
    }

    @Test fun srsIntervalDays_largeDuration_isStoredCorrectly() = assertEquals(30.0f, createEntity(srsIntervalDays = 30.0f).srsIntervalDays, 0.001f)
}
