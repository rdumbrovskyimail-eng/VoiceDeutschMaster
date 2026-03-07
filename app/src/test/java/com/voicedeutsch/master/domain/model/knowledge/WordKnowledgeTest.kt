// Путь: src/test/java/com/voicedeutsch/master/domain/model/knowledge/WordKnowledgeTest.kt
package com.voicedeutsch.master.domain.model.knowledge

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WordKnowledgeTest {

    private fun createWordKnowledge(
        id: String = "wk_001",
        userId: String = "user_1",
        wordId: String = "word_1",
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
        contexts: List<String> = emptyList(),
        mistakes: List<MistakeRecord> = emptyList(),
        createdAt: Long = 1_000L,
        updatedAt: Long = 1_000L,
    ) = WordKnowledge(
        id = id,
        userId = userId,
        wordId = wordId,
        knowledgeLevel = knowledgeLevel,
        timesSeen = timesSeen,
        timesCorrect = timesCorrect,
        timesIncorrect = timesIncorrect,
        lastSeen = lastSeen,
        lastCorrect = lastCorrect,
        lastIncorrect = lastIncorrect,
        nextReview = nextReview,
        srsIntervalDays = srsIntervalDays,
        srsEaseFactor = srsEaseFactor,
        pronunciationScore = pronunciationScore,
        pronunciationAttempts = pronunciationAttempts,
        contexts = contexts,
        mistakes = mistakes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    // ── creation ──────────────────────────────────────────────────────────

    @Test
    fun creation_requiredFields_storedCorrectly() {
        val wk = createWordKnowledge(id = "wk1", userId = "u1", wordId = "w1")
        assertEquals("wk1", wk.id)
        assertEquals("u1", wk.userId)
        assertEquals("w1", wk.wordId)
    }

    @Test
    fun creation_defaultKnowledgeLevel_isZero() {
        assertEquals(0, createWordKnowledge().knowledgeLevel)
    }

    @Test
    fun creation_defaultTimesSeen_isZero() {
        assertEquals(0, createWordKnowledge().timesSeen)
    }

    @Test
    fun creation_defaultTimesCorrect_isZero() {
        assertEquals(0, createWordKnowledge().timesCorrect)
    }

    @Test
    fun creation_defaultTimesIncorrect_isZero() {
        assertEquals(0, createWordKnowledge().timesIncorrect)
    }

    @Test
    fun creation_defaultLastSeen_isNull() {
        assertNull(createWordKnowledge().lastSeen)
    }

    @Test
    fun creation_defaultLastCorrect_isNull() {
        assertNull(createWordKnowledge().lastCorrect)
    }

    @Test
    fun creation_defaultLastIncorrect_isNull() {
        assertNull(createWordKnowledge().lastIncorrect)
    }

    @Test
    fun creation_defaultNextReview_isNull() {
        assertNull(createWordKnowledge().nextReview)
    }

    @Test
    fun creation_defaultSrsIntervalDays_isZero() {
        assertEquals(0f, createWordKnowledge().srsIntervalDays)
    }

    @Test
    fun creation_defaultSrsEaseFactor_is2point5() {
        assertEquals(2.5f, createWordKnowledge().srsEaseFactor)
    }

    @Test
    fun creation_defaultPronunciationScore_isZero() {
        assertEquals(0f, createWordKnowledge().pronunciationScore)
    }

    @Test
    fun creation_defaultPronunciationAttempts_isZero() {
        assertEquals(0, createWordKnowledge().pronunciationAttempts)
    }

    @Test
    fun creation_defaultContexts_isEmpty() {
        assertTrue(createWordKnowledge().contexts.isEmpty())
    }

    @Test
    fun creation_defaultMistakes_isEmpty() {
        assertTrue(createWordKnowledge().mistakes.isEmpty())
    }

    // ── isKnown ───────────────────────────────────────────────────────────

    @Test
    fun isKnown_levelEquals4_returnsTrue() {
        assertTrue(createWordKnowledge(knowledgeLevel = 4).isKnown)
    }

    @Test
    fun isKnown_levelAbove4_returnsTrue() {
        assertTrue(createWordKnowledge(knowledgeLevel = 7).isKnown)
    }

    @Test
    fun isKnown_levelBelow4_returnsFalse() {
        assertFalse(createWordKnowledge(knowledgeLevel = 3).isKnown)
    }

    @Test
    fun isKnown_levelZero_returnsFalse() {
        assertFalse(createWordKnowledge(knowledgeLevel = 0).isKnown)
    }

    // ── isActive ──────────────────────────────────────────────────────────

    @Test
    fun isActive_levelEquals5_returnsTrue() {
        assertTrue(createWordKnowledge(knowledgeLevel = 5).isActive)
    }

    @Test
    fun isActive_levelAbove5_returnsTrue() {
        assertTrue(createWordKnowledge(knowledgeLevel = 6).isActive)
    }

    @Test
    fun isActive_levelBelow5_returnsFalse() {
        assertFalse(createWordKnowledge(knowledgeLevel = 4).isActive)
    }

    @Test
    fun isActive_levelZero_returnsFalse() {
        assertFalse(createWordKnowledge(knowledgeLevel = 0).isActive)
    }

    // ── isMastered ────────────────────────────────────────────────────────

    @Test
    fun isMastered_levelEquals7_returnsTrue() {
        assertTrue(createWordKnowledge(knowledgeLevel = 7).isMastered)
    }

    @Test
    fun isMastered_levelAbove7_returnsTrue() {
        assertTrue(createWordKnowledge(knowledgeLevel = 10).isMastered)
    }

    @Test
    fun isMastered_levelBelow7_returnsFalse() {
        assertFalse(createWordKnowledge(knowledgeLevel = 6).isMastered)
    }

    @Test
    fun isMastered_levelZero_returnsFalse() {
        assertFalse(createWordKnowledge(knowledgeLevel = 0).isMastered)
    }

    // ── needsReview ───────────────────────────────────────────────────────

    @Test
    fun needsReview_pastTimestamp_returnsTrue() {
        val wk = createWordKnowledge(nextReview = System.currentTimeMillis() - 1_000L)
        assertTrue(wk.needsReview)
    }

    @Test
    fun needsReview_futureTimestamp_returnsFalse() {
        val wk = createWordKnowledge(nextReview = System.currentTimeMillis() + 100_000L)
        assertFalse(wk.needsReview)
    }

    @Test
    fun needsReview_nullNextReview_returnsFalse() {
        assertFalse(createWordKnowledge(nextReview = null).needsReview)
    }

    // ── accuracy ──────────────────────────────────────────────────────────

    @Test
    fun accuracy_zeroTimesSeen_returnsZero() {
        assertEquals(0f, createWordKnowledge(timesSeen = 0, timesCorrect = 0).accuracy)
    }

    @Test
    fun accuracy_allCorrect_returnsOne() {
        assertEquals(1f, createWordKnowledge(timesSeen = 10, timesCorrect = 10).accuracy, 0.001f)
    }

    @Test
    fun accuracy_halfCorrect_returns0point5() {
        assertEquals(0.5f, createWordKnowledge(timesSeen = 10, timesCorrect = 5).accuracy, 0.001f)
    }

    @Test
    fun accuracy_noneCorrect_returnsZero() {
        assertEquals(0f, createWordKnowledge(timesSeen = 5, timesCorrect = 0).accuracy, 0.001f)
    }

    @Test
    fun accuracy_oneOutOfThree_returnsCorrectRatio() {
        assertEquals(1f / 3f, createWordKnowledge(timesSeen = 3, timesCorrect = 1).accuracy, 0.001f)
    }

    // ── isProblemWord ─────────────────────────────────────────────────────

    @Test
    fun isProblemWord_moreIncorrectAndEnoughSeen_returnsTrue() {
        val wk = createWordKnowledge(timesIncorrect = 5, timesCorrect = 3, timesSeen = 8)
        assertTrue(wk.isProblemWord)
    }

    @Test
    fun isProblemWord_lessIncorrectThanCorrect_returnsFalse() {
        val wk = createWordKnowledge(timesIncorrect = 2, timesCorrect = 5, timesSeen = 7)
        assertFalse(wk.isProblemWord)
    }

    @Test
    fun isProblemWord_equalIncorrectAndCorrect_returnsFalse() {
        val wk = createWordKnowledge(timesIncorrect = 3, timesCorrect = 3, timesSeen = 6)
        assertFalse(wk.isProblemWord)
    }

    @Test
    fun isProblemWord_tooFewSeen_returnsFalse() {
        val wk = createWordKnowledge(timesIncorrect = 5, timesCorrect = 1, timesSeen = 2)
        assertFalse(wk.isProblemWord)
    }

    @Test
    fun isProblemWord_exactlyThreeSeen_moreIncorrect_returnsTrue() {
        val wk = createWordKnowledge(timesIncorrect = 2, timesCorrect = 1, timesSeen = 3)
        assertTrue(wk.isProblemWord)
    }

    @Test
    fun isProblemWord_zeroSeen_returnsFalse() {
        val wk = createWordKnowledge(timesIncorrect = 0, timesCorrect = 0, timesSeen = 0)
        assertFalse(wk.isProblemWord)
    }

    // ── equals / hashCode / copy ──────────────────────────────────────────

    @Test
    fun equals_twoIdentical_returnsTrue() {
        assertEquals(createWordKnowledge(), createWordKnowledge())
    }

    @Test
    fun equals_differentId_returnsFalse() {
        assertNotEquals(createWordKnowledge(id = "a"), createWordKnowledge(id = "b"))
    }

    @Test
    fun equals_differentKnowledgeLevel_returnsFalse() {
        assertNotEquals(
            createWordKnowledge(knowledgeLevel = 1),
            createWordKnowledge(knowledgeLevel = 5),
        )
    }

    @Test
    fun hashCode_equalObjects_sameHash() {
        assertEquals(createWordKnowledge().hashCode(), createWordKnowledge().hashCode())
    }

    @Test
    fun copy_changesOnlyKnowledgeLevel() {
        val original = createWordKnowledge(knowledgeLevel = 2)
        val copied = original.copy(knowledgeLevel = 6)
        assertEquals(6, copied.knowledgeLevel)
        assertEquals(original.id, copied.id)
        assertEquals(original.wordId, copied.wordId)
    }

    @Test
    fun copy_nullifyNextReview() {
        val original = createWordKnowledge(nextReview = 9_000L)
        val copied = original.copy(nextReview = null)
        assertNull(copied.nextReview)
    }

    // ── MistakeRecord — creation ──────────────────────────────────────────

    @Test
    fun mistakeRecord_fieldsStoredCorrectly() {
        val record = MistakeRecord(
            expected = "der Hund",
            actual = "die Hund",
            timestamp = 5_000L,
            context = "In einem Satz",
        )
        assertEquals("der Hund", record.expected)
        assertEquals("die Hund", record.actual)
        assertEquals(5_000L, record.timestamp)
        assertEquals("In einem Satz", record.context)
    }

    @Test
    fun mistakeRecord_defaultContext_isEmpty() {
        val record = MistakeRecord("E", "A", 1_000L)
        assertEquals("", record.context)
    }

    @Test
    fun mistakeRecord_equals_twoIdentical() {
        val a = MistakeRecord("E", "A", 1_000L, "ctx")
        val b = MistakeRecord("E", "A", 1_000L, "ctx")
        assertEquals(a, b)
    }

    @Test
    fun mistakeRecord_notEquals_differentExpected() {
        assertNotEquals(MistakeRecord("E1", "A", 1_000L), MistakeRecord("E2", "A", 1_000L))
    }

    @Test
    fun mistakeRecord_copy_changesOnlyContext() {
        val original = MistakeRecord("E", "A", 1_000L, "old")
        val copied = original.copy(context = "new")
        assertEquals("E", copied.expected)
        assertEquals("new", copied.context)
    }

    @Test
    fun mistakeRecord_hashCode_equalObjects_sameHash() {
        val a = MistakeRecord("E", "A", 1_000L)
        val b = MistakeRecord("E", "A", 1_000L)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
