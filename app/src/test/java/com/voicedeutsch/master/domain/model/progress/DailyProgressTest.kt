// Path: src/test/java/com/voicedeutsch/master/domain/model/progress/DailyProgressTest.kt
package com.voicedeutsch.master.domain.model.progress

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DailyProgressTest {

    private fun makeDailyProgress(
        id: String = "dp_1",
        userId: String = "user_1",
        date: String = "2024-11-01",
        sessionsCount: Int = 2,
        totalMinutes: Int = 30,
        wordsLearned: Int = 10,
        wordsReviewed: Int = 20,
        exercisesCompleted: Int = 15,
        exercisesCorrect: Int = 12,
        averageScore: Float = 0.8f,
        streakMaintained: Boolean = true,
        createdAt: Long = 1_000_000L,
    ) = DailyProgress(
        id = id,
        userId = userId,
        date = date,
        sessionsCount = sessionsCount,
        totalMinutes = totalMinutes,
        wordsLearned = wordsLearned,
        wordsReviewed = wordsReviewed,
        exercisesCompleted = exercisesCompleted,
        exercisesCorrect = exercisesCorrect,
        averageScore = averageScore,
        streakMaintained = streakMaintained,
        createdAt = createdAt,
    )

    @Test
    fun creation_withAllFields_setsValues() {
        val dp = makeDailyProgress()
        assertEquals("dp_1", dp.id)
        assertEquals("user_1", dp.userId)
        assertEquals("2024-11-01", dp.date)
        assertEquals(2, dp.sessionsCount)
        assertEquals(30, dp.totalMinutes)
        assertEquals(10, dp.wordsLearned)
        assertEquals(20, dp.wordsReviewed)
        assertEquals(15, dp.exercisesCompleted)
        assertEquals(12, dp.exercisesCorrect)
        assertEquals(0.8f, dp.averageScore)
        assertTrue(dp.streakMaintained)
    }

    @Test
    fun creation_defaultSessionsCount_isZero() {
        val dp = DailyProgress(id = "dp_1", userId = "user_1", date = "2024-01-01")
        assertEquals(0, dp.sessionsCount)
    }

    @Test
    fun creation_defaultTotalMinutes_isZero() {
        val dp = DailyProgress(id = "dp_1", userId = "user_1", date = "2024-01-01")
        assertEquals(0, dp.totalMinutes)
    }

    @Test
    fun creation_defaultWordsLearned_isZero() {
        val dp = DailyProgress(id = "dp_1", userId = "user_1", date = "2024-01-01")
        assertEquals(0, dp.wordsLearned)
    }

    @Test
    fun creation_defaultWordsReviewed_isZero() {
        val dp = DailyProgress(id = "dp_1", userId = "user_1", date = "2024-01-01")
        assertEquals(0, dp.wordsReviewed)
    }

    @Test
    fun creation_defaultExercisesCompleted_isZero() {
        val dp = DailyProgress(id = "dp_1", userId = "user_1", date = "2024-01-01")
        assertEquals(0, dp.exercisesCompleted)
    }

    @Test
    fun creation_defaultExercisesCorrect_isZero() {
        val dp = DailyProgress(id = "dp_1", userId = "user_1", date = "2024-01-01")
        assertEquals(0, dp.exercisesCorrect)
    }

    @Test
    fun creation_defaultAverageScore_isZero() {
        val dp = DailyProgress(id = "dp_1", userId = "user_1", date = "2024-01-01")
        assertEquals(0f, dp.averageScore)
    }

    @Test
    fun creation_defaultStreakMaintained_isFalse() {
        val dp = DailyProgress(id = "dp_1", userId = "user_1", date = "2024-01-01")
        assertFalse(dp.streakMaintained)
    }

    @Test
    fun creation_streakNotMaintained_isFalse() {
        val dp = makeDailyProgress(streakMaintained = false)
        assertFalse(dp.streakMaintained)
    }

    @Test
    fun copy_changesWordsLearned_restUnchanged() {
        val original = makeDailyProgress(wordsLearned = 10)
        val copy = original.copy(wordsLearned = 25)
        assertEquals(25, copy.wordsLearned)
        assertEquals("dp_1", copy.id)
        assertEquals("2024-11-01", copy.date)
    }

    @Test
    fun copy_changesAverageScore() {
        val original = makeDailyProgress(averageScore = 0.8f)
        val copy = original.copy(averageScore = 0.95f)
        assertEquals(0.95f, copy.averageScore)
    }

    @Test
    fun copy_changesStreakMaintained() {
        val original = makeDailyProgress(streakMaintained = false)
        val copy = original.copy(streakMaintained = true)
        assertTrue(copy.streakMaintained)
    }

    @Test
    fun copy_changesDate() {
        val original = makeDailyProgress(date = "2024-01-01")
        val copy = original.copy(date = "2024-12-31")
        assertEquals("2024-12-31", copy.date)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeDailyProgress()
        val b = makeDailyProgress()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentDate_areNotEqual() {
        val a = makeDailyProgress(date = "2024-01-01")
        val b = makeDailyProgress(date = "2024-01-02")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentWordsLearned_areNotEqual() {
        val a = makeDailyProgress(wordsLearned = 5)
        val b = makeDailyProgress(wordsLearned = 10)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentStreakMaintained_areNotEqual() {
        val a = makeDailyProgress(streakMaintained = true)
        val b = makeDailyProgress(streakMaintained = false)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentUserId_areNotEqual() {
        val a = makeDailyProgress(userId = "user_1")
        val b = makeDailyProgress(userId = "user_2")
        assertNotEquals(a, b)
    }
}
