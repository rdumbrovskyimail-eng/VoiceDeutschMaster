// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/DailyStatisticsEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DailyStatisticsEntityTest {

    private fun createEntity(
        id: String = "ds_001",
        userId: String = "user_001",
        date: String = "2026-03-06",
        sessionsCount: Int = 2,
        totalMinutes: Int = 30,
        wordsLearned: Int = 15,
        wordsReviewed: Int = 20,
        exercisesCompleted: Int = 10,
        exercisesCorrect: Int = 8,
        averageScore: Float = 0.8f,
        streakMaintained: Boolean = true,
        createdAt: Long = 4_000_000L,
    ) = DailyStatisticsEntity(
        id = id, userId = userId, date = date, sessionsCount = sessionsCount,
        totalMinutes = totalMinutes, wordsLearned = wordsLearned, wordsReviewed = wordsReviewed,
        exercisesCompleted = exercisesCompleted, exercisesCorrect = exercisesCorrect,
        averageScore = averageScore, streakMaintained = streakMaintained, createdAt = createdAt,
    )

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals("ds_001", entity.id)
        assertEquals("user_001", entity.userId)
        assertEquals("2026-03-06", entity.date)
        assertEquals(2, entity.sessionsCount)
        assertEquals(30, entity.totalMinutes)
        assertEquals(15, entity.wordsLearned)
        assertEquals(20, entity.wordsReviewed)
        assertEquals(10, entity.exercisesCompleted)
        assertEquals(8, entity.exercisesCorrect)
        assertEquals(0.8f, entity.averageScore, 0.001f)
        assertTrue(entity.streakMaintained)
        assertEquals(4_000_000L, entity.createdAt)
    }

    @Test fun creation_streakNotMaintained_isFalse() = assertFalse(createEntity(streakMaintained = false).streakMaintained)

    private fun minimal() = DailyStatisticsEntity(id = "ds_002", userId = "u1", date = "2026-03-07")

    @Test fun defaultSessionsCount_isZero() = assertEquals(0, minimal().sessionsCount)
    @Test fun defaultTotalMinutes_isZero() = assertEquals(0, minimal().totalMinutes)
    @Test fun defaultWordsLearned_isZero() = assertEquals(0, minimal().wordsLearned)
    @Test fun defaultWordsReviewed_isZero() = assertEquals(0, minimal().wordsReviewed)
    @Test fun defaultExercisesCompleted_isZero() = assertEquals(0, minimal().exercisesCompleted)
    @Test fun defaultExercisesCorrect_isZero() = assertEquals(0, minimal().exercisesCorrect)
    @Test fun defaultAverageScore_isZero() = assertEquals(0f, minimal().averageScore, 0.001f)
    @Test fun defaultStreakMaintained_isFalse() = assertFalse(minimal().streakMaintained)

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = DailyStatisticsEntity(id = "ds_003", userId = "u1", date = "2026-03-07")
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentDate_returnsFalse() = assertNotEquals(createEntity(date = "2026-03-06"), createEntity(date = "2026-03-07"))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_withNewWordsLearned_onlyWordsLearnedChanges() {
        val original = createEntity(wordsLearned = 5)
        val copied = original.copy(wordsLearned = 25)
        assertEquals(25, copied.wordsLearned)
        assertEquals(original.id, copied.id)
        assertEquals(original.date, copied.date)
    }

    @Test
    fun copy_activateStreak_streakBecomesTrue() {
        val original = createEntity(streakMaintained = false)
        assertTrue(original.copy(streakMaintained = true).streakMaintained)
    }

    @Test
    fun copy_newAverageScore_scoreUpdated() {
        val original = createEntity(averageScore = 0.5f)
        val copied = original.copy(averageScore = 0.95f)
        assertEquals(0.95f, copied.averageScore, 0.001f)
        assertEquals(original.userId, copied.userId)
    }

    @Test fun averageScore_perfectScore_isOne() = assertEquals(1.0f, createEntity(averageScore = 1.0f).averageScore, 0.001f)
    @Test fun averageScore_zeroScore_isZero() = assertEquals(0f, createEntity(averageScore = 0f).averageScore, 0.001f)
}
