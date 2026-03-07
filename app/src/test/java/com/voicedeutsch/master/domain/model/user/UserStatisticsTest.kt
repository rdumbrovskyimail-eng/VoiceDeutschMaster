// Путь: src/test/java/com/voicedeutsch/master/domain/model/user/UserStatisticsTest.kt
package com.voicedeutsch.master.domain.model.user

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UserStatisticsTest {

    private fun createUserStatistics(
        totalWords: Int = 100,
        activeWords: Int = 40,
        passiveWords: Int = 30,
        totalRules: Int = 50,
        knownRules: Int = 20,
        totalSessions: Int = 10,
        totalMinutes: Int = 300,
        streakDays: Int = 7,
        averageScore: Float = 0.8f,
        averagePronunciationScore: Float = 0.75f,
        wordsForReviewToday: Int = 5,
        rulesForReviewToday: Int = 3,
        bookProgress: Float = 0.5f,
        currentChapter: Int = 3,
        totalChapters: Int = 6
    ) = UserStatistics(
        totalWords = totalWords,
        activeWords = activeWords,
        passiveWords = passiveWords,
        totalRules = totalRules,
        knownRules = knownRules,
        totalSessions = totalSessions,
        totalMinutes = totalMinutes,
        streakDays = streakDays,
        averageScore = averageScore,
        averagePronunciationScore = averagePronunciationScore,
        wordsForReviewToday = wordsForReviewToday,
        rulesForReviewToday = rulesForReviewToday,
        bookProgress = bookProgress,
        currentChapter = currentChapter,
        totalChapters = totalChapters
    )

    // ── Construction ──────────────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val stats = createUserStatistics(
            totalWords = 200,
            activeWords = 80,
            passiveWords = 60,
            totalRules = 100,
            knownRules = 40,
            totalSessions = 20,
            totalMinutes = 600,
            streakDays = 14,
            averageScore = 0.9f,
            averagePronunciationScore = 0.85f,
            wordsForReviewToday = 10,
            rulesForReviewToday = 6,
            bookProgress = 0.75f,
            currentChapter = 6,
            totalChapters = 8
        )
        assertEquals(200, stats.totalWords)
        assertEquals(80, stats.activeWords)
        assertEquals(60, stats.passiveWords)
        assertEquals(100, stats.totalRules)
        assertEquals(40, stats.knownRules)
        assertEquals(20, stats.totalSessions)
        assertEquals(600, stats.totalMinutes)
        assertEquals(14, stats.streakDays)
        assertEquals(0.9f, stats.averageScore, 0.001f)
        assertEquals(0.85f, stats.averagePronunciationScore, 0.001f)
        assertEquals(10, stats.wordsForReviewToday)
        assertEquals(6, stats.rulesForReviewToday)
        assertEquals(0.75f, stats.bookProgress, 0.001f)
        assertEquals(6, stats.currentChapter)
        assertEquals(8, stats.totalChapters)
    }

    // ── totalHours ────────────────────────────────────────────────────────

    @Test
    fun totalHours_zeroMinutes_returnsZero() {
        val stats = createUserStatistics(totalMinutes = 0)
        assertEquals(0f, stats.totalHours, 0.001f)
    }

    @Test
    fun totalHours_60Minutes_returnsOne() {
        val stats = createUserStatistics(totalMinutes = 60)
        assertEquals(1f, stats.totalHours, 0.001f)
    }

    @Test
    fun totalHours_90Minutes_returnsOnePointFive() {
        val stats = createUserStatistics(totalMinutes = 90)
        assertEquals(1.5f, stats.totalHours, 0.001f)
    }

    @Test
    fun totalHours_30Minutes_returnsHalf() {
        val stats = createUserStatistics(totalMinutes = 30)
        assertEquals(0.5f, stats.totalHours, 0.001f)
    }

    @Test
    fun totalHours_1Minute_returnsFractionalHour() {
        val stats = createUserStatistics(totalMinutes = 1)
        assertEquals(1f / 60f, stats.totalHours, 0.001f)
    }

    @Test
    fun totalHours_largeValue_calculatedCorrectly() {
        val stats = createUserStatistics(totalMinutes = 1200)
        assertEquals(20f, stats.totalHours, 0.001f)
    }

    // ── wordsPerDay ───────────────────────────────────────────────────────

    @Test
    fun wordsPerDay_zeroSessions_returnsZero() {
        val stats = createUserStatistics(totalSessions = 0, totalWords = 50, streakDays = 5)
        assertEquals(0f, stats.wordsPerDay, 0.001f)
    }

    @Test
    fun wordsPerDay_nonZeroSessionsAndStreak_returnsCorrectRatio() {
        val stats = createUserStatistics(totalSessions = 5, totalWords = 70, streakDays = 7)
        assertEquals(70f / 7f, stats.wordsPerDay, 0.001f)
    }

    @Test
    fun wordsPerDay_zeroStreakDays_usesMaxOf1ToAvoidDivisionByZero() {
        val stats = createUserStatistics(totalSessions = 3, totalWords = 30, streakDays = 0)
        assertEquals(30f / 1f, stats.wordsPerDay, 0.001f)
    }

    @Test
    fun wordsPerDay_oneStreakDay_returnsTotalWords() {
        val stats = createUserStatistics(totalSessions = 1, totalWords = 10, streakDays = 1)
        assertEquals(10f, stats.wordsPerDay, 0.001f)
    }

    @Test
    fun wordsPerDay_zeroTotalWords_returnsZero() {
        val stats = createUserStatistics(totalSessions = 5, totalWords = 0, streakDays = 5)
        assertEquals(0f, stats.wordsPerDay, 0.001f)
    }

    @Test
    fun wordsPerDay_largeValues_calculatedCorrectly() {
        val stats = createUserStatistics(totalSessions = 100, totalWords = 1000, streakDays = 50)
        assertEquals(1000f / 50f, stats.wordsPerDay, 0.001f)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_changeStreakDays_onlyStreakChanges() {
        val original = createUserStatistics(streakDays = 3)
        val modified = original.copy(streakDays = 10)
        assertEquals(10, modified.streakDays)
        assertEquals(original.totalWords, modified.totalWords)
        assertEquals(original.totalSessions, modified.totalSessions)
    }

    @Test
    fun copy_changeBookProgress_onlyProgressChanges() {
        val original = createUserStatistics(bookProgress = 0.25f)
        val modified = original.copy(bookProgress = 0.75f)
        assertEquals(0.75f, modified.bookProgress, 0.001f)
        assertEquals(original.bookProgress, 0.25f, 0.001f)
    }

    @Test
    fun copy_incrementTotalWords_wordsPerDayRecalculated() {
        val original = createUserStatistics(totalSessions = 5, totalWords = 50, streakDays = 5)
        val modified = original.copy(totalWords = 100)
        assertEquals(100f / 5f, modified.wordsPerDay, 0.001f)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val s1 = createUserStatistics(totalWords = 100, streakDays = 7)
        val s2 = createUserStatistics(totalWords = 100, streakDays = 7)
        assertEquals(s1, s2)
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        val s1 = createUserStatistics(totalWords = 100, streakDays = 7)
        val s2 = createUserStatistics(totalWords = 100, streakDays = 7)
        assertEquals(s1.hashCode(), s2.hashCode())
    }

    @Test
    fun equals_differentTotalWords_notEqual() {
        val s1 = createUserStatistics(totalWords = 100)
        val s2 = createUserStatistics(totalWords = 200)
        assertNotEquals(s1, s2)
    }

    @Test
    fun equals_differentAverageScore_notEqual() {
        val s1 = createUserStatistics(averageScore = 0.5f)
        val s2 = createUserStatistics(averageScore = 0.9f)
        assertNotEquals(s1, s2)
    }

    // ── bookProgress boundary values ──────────────────────────────────────

    @Test
    fun bookProgress_zero_storedCorrectly() {
        val stats = createUserStatistics(bookProgress = 0.0f)
        assertEquals(0.0f, stats.bookProgress, 0.001f)
    }

    @Test
    fun bookProgress_one_storedCorrectly() {
        val stats = createUserStatistics(bookProgress = 1.0f)
        assertEquals(1.0f, stats.bookProgress, 0.001f)
    }
}
