// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/SessionEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SessionEntityTest {

    private fun createEntity(
        id: String = "sess_001",
        userId: String = "user_001",
        startedAt: Long = 11_000_000L,
        endedAt: Long? = 11_060_000L,
        durationMinutes: Int = 30,
        strategiesUsedJson: String = """["vocabulary"]""",
        wordsLearned: Int = 5,
        wordsReviewed: Int = 10,
        rulesPracticed: Int = 2,
        exercisesCompleted: Int = 8,
        exercisesCorrect: Int = 7,
        averagePronunciationScore: Float = 0.8f,
        bookChapterStart: Int = 1,
        bookLessonStart: Int = 1,
        bookChapterEnd: Int = 1,
        bookLessonEnd: Int = 2,
        sessionSummary: String = "Good session",
        moodEstimate: String? = "good",
        createdAt: Long = 11_000_000L,
    ) = SessionEntity(
        id = id, userId = userId, startedAt = startedAt, endedAt = endedAt,
        durationMinutes = durationMinutes, strategiesUsedJson = strategiesUsedJson,
        wordsLearned = wordsLearned, wordsReviewed = wordsReviewed,
        rulesPracticed = rulesPracticed, exercisesCompleted = exercisesCompleted,
        exercisesCorrect = exercisesCorrect, averagePronunciationScore = averagePronunciationScore,
        bookChapterStart = bookChapterStart, bookLessonStart = bookLessonStart,
        bookChapterEnd = bookChapterEnd, bookLessonEnd = bookLessonEnd,
        sessionSummary = sessionSummary, moodEstimate = moodEstimate, createdAt = createdAt,
    )

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals("sess_001", entity.id)
        assertEquals("user_001", entity.userId)
        assertEquals(11_000_000L, entity.startedAt)
        assertEquals(11_060_000L, entity.endedAt)
        assertEquals(30, entity.durationMinutes)
        assertEquals(5, entity.wordsLearned)
        assertEquals(10, entity.wordsReviewed)
        assertEquals(2, entity.rulesPracticed)
        assertEquals(8, entity.exercisesCompleted)
        assertEquals(7, entity.exercisesCorrect)
        assertEquals(0.8f, entity.averagePronunciationScore, 0.001f)
        assertEquals(1, entity.bookChapterStart)
        assertEquals(1, entity.bookLessonStart)
        assertEquals(1, entity.bookChapterEnd)
        assertEquals(2, entity.bookLessonEnd)
        assertEquals("Good session", entity.sessionSummary)
        assertEquals("good", entity.moodEstimate)
    }

    @Test fun creation_withNullEndedAt_sessionIsOngoing() = assertNull(createEntity(endedAt = null).endedAt)
    @Test fun creation_withNullMoodEstimate_moodIsNull() = assertNull(createEntity(moodEstimate = null).moodEstimate)

    private fun minimal() = SessionEntity(id = "sess_002", userId = "u1", startedAt = 100L)

    @Test fun defaultEndedAt_isNull() = assertNull(minimal().endedAt)
    @Test fun defaultDurationMinutes_isZero() = assertEquals(0, minimal().durationMinutes)
    @Test fun defaultStrategiesUsedJson_isEmptyArray() = assertEquals("[]", minimal().strategiesUsedJson)
    @Test fun defaultWordsLearned_isZero() = assertEquals(0, minimal().wordsLearned)
    @Test fun defaultSessionSummary_isEmptyString() = assertEquals("", minimal().sessionSummary)
    @Test fun defaultMoodEstimate_isNull() = assertNull(minimal().moodEstimate)
    @Test fun defaultBookChapterStart_isZero() = assertEquals(0, minimal().bookChapterStart)

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = SessionEntity(id = "sess_003", userId = "u1", startedAt = 100L)
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentId_returnsFalse() = assertNotEquals(createEntity(id = "sess_001"), createEntity(id = "sess_002"))
    @Test fun equals_nullVsNonNullEndedAt_returnsFalse() = assertNotEquals(createEntity(endedAt = null), createEntity(endedAt = 999L))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_setEndedAt_sessionBecomesFinished() {
        val original = createEntity(endedAt = null)
        val copied = original.copy(endedAt = 12_000_000L)
        assertEquals(12_000_000L, copied.endedAt)
        assertEquals(original.startedAt, copied.startedAt)
    }

    @Test
    fun copy_withNewDuration_durationUpdated() {
        val copied = createEntity(durationMinutes = 15).copy(durationMinutes = 45)
        assertEquals(45, copied.durationMinutes)
    }

    @Test fun copy_setMoodEstimate_moodUpdated() = assertEquals("neutral", createEntity(moodEstimate = null).copy(moodEstimate = "neutral").moodEstimate)

    @Test
    fun strategiesUsedJson_multipleStrategies_isStoredCorrectly() {
        val json = """["vocabulary","grammar","pronunciation"]"""
        assertEquals(json, createEntity(strategiesUsedJson = json).strategiesUsedJson)
    }

    @Test
    fun bookChapterProgress_startLessThanEnd_isConsistent() {
        val entity = createEntity(bookChapterStart = 1, bookChapterEnd = 2)
        assertTrue(entity.bookChapterEnd >= entity.bookChapterStart)
    }
}
