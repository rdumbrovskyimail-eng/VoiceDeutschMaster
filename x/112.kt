// Путь: src/test/java/com/voicedeutsch/master/domain/model/knowledge/RuleKnowledgeTest.kt
package com.voicedeutsch.master.domain.model.knowledge

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RuleKnowledgeTest {

    private fun createRuleKnowledge(
        id: String = "rk_001",
        userId: String = "user_1",
        ruleId: String = "rule_dativ",
        knowledgeLevel: Int = 0,
        timesPracticed: Int = 0,
        timesCorrect: Int = 0,
        timesIncorrect: Int = 0,
        lastPracticed: Long? = null,
        nextReview: Long? = null,
        srsIntervalDays: Float = 0f,
        srsEaseFactor: Float = 2.5f,
        commonMistakes: List<String> = emptyList(),
        createdAt: Long = 1_000L,
        updatedAt: Long = 1_000L,
    ) = RuleKnowledge(
        id = id,
        userId = userId,
        ruleId = ruleId,
        knowledgeLevel = knowledgeLevel,
        timesPracticed = timesPracticed,
        timesCorrect = timesCorrect,
        timesIncorrect = timesIncorrect,
        lastPracticed = lastPracticed,
        nextReview = nextReview,
        srsIntervalDays = srsIntervalDays,
        srsEaseFactor = srsEaseFactor,
        commonMistakes = commonMistakes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    // ── creation ──────────────────────────────────────────────────────────

    @Test
    fun creation_requiredFields_storedCorrectly() {
        val rk = createRuleKnowledge(id = "rk1", userId = "u1", ruleId = "r1")
        assertEquals("rk1", rk.id)
        assertEquals("u1", rk.userId)
        assertEquals("r1", rk.ruleId)
    }

    @Test
    fun creation_defaultKnowledgeLevel_isZero() {
        assertEquals(0, createRuleKnowledge().knowledgeLevel)
    }

    @Test
    fun creation_defaultTimesPracticed_isZero() {
        assertEquals(0, createRuleKnowledge().timesPracticed)
    }

    @Test
    fun creation_defaultTimesCorrect_isZero() {
        assertEquals(0, createRuleKnowledge().timesCorrect)
    }

    @Test
    fun creation_defaultTimesIncorrect_isZero() {
        assertEquals(0, createRuleKnowledge().timesIncorrect)
    }

    @Test
    fun creation_defaultLastPracticed_isNull() {
        assertNull(createRuleKnowledge().lastPracticed)
    }

    @Test
    fun creation_defaultNextReview_isNull() {
        assertNull(createRuleKnowledge().nextReview)
    }

    @Test
    fun creation_defaultSrsIntervalDays_isZero() {
        assertEquals(0f, createRuleKnowledge().srsIntervalDays)
    }

    @Test
    fun creation_defaultSrsEaseFactor_is2point5() {
        assertEquals(2.5f, createRuleKnowledge().srsEaseFactor)
    }

    @Test
    fun creation_defaultCommonMistakes_isEmpty() {
        assertTrue(createRuleKnowledge().commonMistakes.isEmpty())
    }

    @Test
    fun creation_withCommonMistakes_storedCorrectly() {
        val mistakes = listOf("Falsche Endung", "Falscher Artikel")
        assertEquals(mistakes, createRuleKnowledge(commonMistakes = mistakes).commonMistakes)
    }

    // ── isKnown ───────────────────────────────────────────────────────────

    @Test
    fun isKnown_levelEquals4_returnsTrue() {
        assertTrue(createRuleKnowledge(knowledgeLevel = 4).isKnown)
    }

    @Test
    fun isKnown_levelAbove4_returnsTrue() {
        assertTrue(createRuleKnowledge(knowledgeLevel = 7).isKnown)
    }

    @Test
    fun isKnown_levelBelow4_returnsFalse() {
        assertFalse(createRuleKnowledge(knowledgeLevel = 3).isKnown)
    }

    @Test
    fun isKnown_levelZero_returnsFalse() {
        assertFalse(createRuleKnowledge(knowledgeLevel = 0).isKnown)
    }

    @Test
    fun isKnown_levelExactly4_boundary_returnsTrue() {
        assertTrue(createRuleKnowledge(knowledgeLevel = 4).isKnown)
    }

    // ── needsReview ───────────────────────────────────────────────────────

    @Test
    fun needsReview_pastTimestamp_returnsTrue() {
        val rk = createRuleKnowledge(nextReview = System.currentTimeMillis() - 1_000L)
        assertTrue(rk.needsReview)
    }

    @Test
    fun needsReview_futureTimestamp_returnsFalse() {
        val rk = createRuleKnowledge(nextReview = System.currentTimeMillis() + 100_000L)
        assertFalse(rk.needsReview)
    }

    @Test
    fun needsReview_nullNextReview_returnsFalse() {
        assertFalse(createRuleKnowledge(nextReview = null).needsReview)
    }

    // ── accuracy ──────────────────────────────────────────────────────────

    @Test
    fun accuracy_zeroPracticed_returnsZero() {
        assertEquals(0f, createRuleKnowledge(timesPracticed = 0, timesCorrect = 0).accuracy)
    }

    @Test
    fun accuracy_allCorrect_returnsOne() {
        assertEquals(1f, createRuleKnowledge(timesPracticed = 10, timesCorrect = 10).accuracy, 0.001f)
    }

    @Test
    fun accuracy_halfCorrect_returns0point5() {
        assertEquals(0.5f, createRuleKnowledge(timesPracticed = 10, timesCorrect = 5).accuracy, 0.001f)
    }

    @Test
    fun accuracy_noneCorrect_returnsZero() {
        assertEquals(0f, createRuleKnowledge(timesPracticed = 5, timesCorrect = 0).accuracy, 0.001f)
    }

    @Test
    fun accuracy_oneOutOfThree_returnsCorrectRatio() {
        assertEquals(1f / 3f, createRuleKnowledge(timesPracticed = 3, timesCorrect = 1).accuracy, 0.001f)
    }

    // ── equals / hashCode / copy ──────────────────────────────────────────

    @Test
    fun equals_twoIdentical_returnsTrue() {
        assertEquals(createRuleKnowledge(), createRuleKnowledge())
    }

    @Test
    fun equals_differentId_returnsFalse() {
        assertNotEquals(createRuleKnowledge(id = "a"), createRuleKnowledge(id = "b"))
    }

    @Test
    fun equals_differentKnowledgeLevel_returnsFalse() {
        assertNotEquals(createRuleKnowledge(knowledgeLevel = 1), createRuleKnowledge(knowledgeLevel = 5))
    }

    @Test
    fun hashCode_equalObjects_sameHash() {
        assertEquals(createRuleKnowledge().hashCode(), createRuleKnowledge().hashCode())
    }

    @Test
    fun copy_changesOnlyKnowledgeLevel() {
        val original = createRuleKnowledge(knowledgeLevel = 1)
        val copied = original.copy(knowledgeLevel = 6)
        assertEquals(6, copied.knowledgeLevel)
        assertEquals(original.id, copied.id)
        assertEquals(original.ruleId, copied.ruleId)
    }

    @Test
    fun copy_nullifyNextReview() {
        val original = createRuleKnowledge(nextReview = 9_000L)
        val copied = original.copy(nextReview = null)
        assertNull(copied.nextReview)
    }
}
