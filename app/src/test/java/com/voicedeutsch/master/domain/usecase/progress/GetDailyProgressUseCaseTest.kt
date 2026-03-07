// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/progress/GetDailyProgressUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.progress

import com.voicedeutsch.master.domain.model.progress.DailyProgress
import com.voicedeutsch.master.domain.repository.ProgressRepository
import com.voicedeutsch.master.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetDailyProgressUseCaseTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var progressRepository: ProgressRepository
    private lateinit var useCase: GetDailyProgressUseCase

    private val fixedToday = "2024-03-15"

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeDailyProgress(
        sessionsCount: Int = 2,
        totalMinutes: Int = 30,
        wordsLearned: Int = 8,
        wordsReviewed: Int = 5,
        exercisesCompleted: Int = 12,
        averageScore: Float = 0.75f
    ): DailyProgress = mockk<DailyProgress>(relaxed = true).also {
        every { it.sessionsCount }       returns sessionsCount
        every { it.totalMinutes }        returns totalMinutes
        every { it.wordsLearned }        returns wordsLearned
        every { it.wordsReviewed }       returns wordsReviewed
        every { it.exercisesCompleted }  returns exercisesCompleted
        every { it.averageScore }        returns averageScore
    }

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        sessionRepository  = mockk()
        progressRepository = mockk()
        useCase = GetDailyProgressUseCase(sessionRepository, progressRepository)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        every { com.voicedeutsch.master.util.DateUtils.todayDateString() } returns fixedToday

        coEvery { sessionRepository.getDailyStatistics(any(), any()) } returns null
        coEvery { sessionRepository.calculateStreak(any()) }           returns 0
        coEvery { progressRepository.getWeeklyProgress(any()) }        returns emptyList()
        coEvery { progressRepository.getMonthlyProgress(any()) }       returns emptyList()
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── getToday ──────────────────────────────────────────────────────────────

    @Test
    fun getToday_repositoryCalledWithTodayDate() = runTest {
        useCase.getToday("user1")

        coVerify(exactly = 1) { sessionRepository.getDailyStatistics("user1", fixedToday) }
    }

    @Test
    fun getToday_statisticsExist_returnsProgress() = runTest {
        val dp = makeDailyProgress()
        coEvery { sessionRepository.getDailyStatistics("user1", fixedToday) } returns dp

        val result = useCase.getToday("user1")

        assertEquals(dp, result)
    }

    @Test
    fun getToday_noStatistics_returnsNull() = runTest {
        coEvery { sessionRepository.getDailyStatistics(any(), any()) } returns null

        val result = useCase.getToday("user1")

        assertNull(result)
    }

    @Test
    fun getToday_correctUserIdPassed() = runTest {
        useCase.getToday("user99")

        coVerify { sessionRepository.getDailyStatistics("user99", any()) }
    }

    // ── getWeekly ─────────────────────────────────────────────────────────────

    @Test
    fun getWeekly_returnsWeeklyProgressFromRepository() = runTest {
        val weekly = List(7) { makeDailyProgress() }
        coEvery { progressRepository.getWeeklyProgress("user1") } returns weekly

        val result = useCase.getWeekly("user1")

        assertEquals(weekly, result)
    }

    @Test
    fun getWeekly_repositoryCalledWithCorrectUserId() = runTest {
        useCase.getWeekly("user42")

        coVerify(exactly = 1) { progressRepository.getWeeklyProgress("user42") }
    }

    @Test
    fun getWeekly_emptyRepository_returnsEmptyList() = runTest {
        coEvery { progressRepository.getWeeklyProgress(any()) } returns emptyList()

        val result = useCase.getWeekly("user1")

        assertTrue(result.isEmpty())
    }

    // ── getMonthly ────────────────────────────────────────────────────────────

    @Test
    fun getMonthly_returnsMonthlyProgressFromRepository() = runTest {
        val monthly = List(30) { makeDailyProgress() }
        coEvery { progressRepository.getMonthlyProgress("user1") } returns monthly

        val result = useCase.getMonthly("user1")

        assertEquals(monthly, result)
    }

    @Test
    fun getMonthly_repositoryCalledWithCorrectUserId() = runTest {
        useCase.getMonthly("user77")

        coVerify(exactly = 1) { progressRepository.getMonthlyProgress("user77") }
    }

    @Test
    fun getMonthly_emptyRepository_returnsEmptyList() = runTest {
        coEvery { progressRepository.getMonthlyProgress(any()) } returns emptyList()

        val result = useCase.getMonthly("user1")

        assertTrue(result.isEmpty())
    }

    // ── getStreak ─────────────────────────────────────────────────────────────

    @Test
    fun getStreak_returnsStreakFromRepository() = runTest {
        coEvery { sessionRepository.calculateStreak("user1") } returns 7

        val result = useCase.getStreak("user1")

        assertEquals(7, result)
    }

    @Test
    fun getStreak_repositoryCalledWithCorrectUserId() = runTest {
        useCase.getStreak("user55")

        coVerify(exactly = 1) { sessionRepository.calculateStreak("user55") }
    }

    @Test
    fun getStreak_noStreak_returns0() = runTest {
        coEvery { sessionRepository.calculateStreak(any()) } returns 0

        val result = useCase.getStreak("user1")

        assertEquals(0, result)
    }

    // ── getTodaySummary — with data ───────────────────────────────────────────

    @Test
    fun getTodaySummary_todayDataExists_allFieldsPopulated() = runTest {
        val dp = makeDailyProgress(
            sessionsCount      = 3,
            totalMinutes       = 45,
            wordsLearned       = 10,
            wordsReviewed      = 7,
            exercisesCompleted = 20,
            averageScore       = 0.85f
        )
        coEvery { sessionRepository.getDailyStatistics("user1", fixedToday) } returns dp
        coEvery { sessionRepository.calculateStreak("user1") }                returns 5

        val result = useCase.getTodaySummary("user1")

        assertEquals(3,     result.sessionsToday)
        assertEquals(45,    result.minutesToday)
        assertEquals(10,    result.wordsLearnedToday)
        assertEquals(7,     result.wordsReviewedToday)
        assertEquals(20,    result.exercisesCompletedToday)
        assertEquals(0.85f, result.averageScoreToday, 0.001f)
        assertEquals(5,     result.currentStreak)
    }

    @Test
    fun getTodaySummary_noTodayData_allCountersAreZero() = runTest {
        coEvery { sessionRepository.getDailyStatistics(any(), any()) } returns null
        coEvery { sessionRepository.calculateStreak(any()) }           returns 0

        val result = useCase.getTodaySummary("user1")

        assertEquals(0,   result.sessionsToday)
        assertEquals(0,   result.minutesToday)
        assertEquals(0,   result.wordsLearnedToday)
        assertEquals(0,   result.wordsReviewedToday)
        assertEquals(0,   result.exercisesCompletedToday)
        assertEquals(0f,  result.averageScoreToday)
        assertEquals(0,   result.currentStreak)
    }

    @Test
    fun getTodaySummary_noTodayData_streakStillPopulated() = runTest {
        coEvery { sessionRepository.getDailyStatistics(any(), any()) } returns null
        coEvery { sessionRepository.calculateStreak("user1") }         returns 12

        val result = useCase.getTodaySummary("user1")

        assertEquals(12, result.currentStreak)
        assertEquals(0,  result.sessionsToday)
    }

    @Test
    fun getTodaySummary_callsDailyStatisticsWithTodayDate() = runTest {
        useCase.getTodaySummary("user1")

        coVerify { sessionRepository.getDailyStatistics("user1", fixedToday) }
    }

    @Test
    fun getTodaySummary_callsCalculateStreak() = runTest {
        useCase.getTodaySummary("user1")

        coVerify(exactly = 1) { sessionRepository.calculateStreak("user1") }
    }

    @Test
    fun getTodaySummary_userIdPassedToAllRepositoryCalls() = runTest {
        useCase.getTodaySummary("user88")

        coVerify { sessionRepository.getDailyStatistics("user88", any()) }
        coVerify { sessionRepository.calculateStreak("user88") }
    }

    // ── DailySummary data class ───────────────────────────────────────────────

    @Test
    fun dailySummary_creation_storesAllFields() {
        val summary = GetDailyProgressUseCase.DailySummary(
            sessionsToday            = 2,
            minutesToday             = 30,
            wordsLearnedToday        = 8,
            wordsReviewedToday       = 5,
            exercisesCompletedToday  = 12,
            averageScoreToday        = 0.75f,
            currentStreak            = 4
        )

        assertEquals(2,     summary.sessionsToday)
        assertEquals(30,    summary.minutesToday)
        assertEquals(8,     summary.wordsLearnedToday)
        assertEquals(5,     summary.wordsReviewedToday)
        assertEquals(12,    summary.exercisesCompletedToday)
        assertEquals(0.75f, summary.averageScoreToday, 0.001f)
        assertEquals(4,     summary.currentStreak)
    }

    @Test
    fun dailySummary_copy_changesOnlySpecifiedField() {
        val original = GetDailyProgressUseCase.DailySummary(1, 10, 2, 3, 5, 0.6f, 2)
        val copy     = original.copy(currentStreak = 9)

        assertEquals(9,                         copy.currentStreak)
        assertEquals(original.sessionsToday,    copy.sessionsToday)
        assertEquals(original.minutesToday,     copy.minutesToday)
        assertEquals(original.averageScoreToday, copy.averageScoreToday)
    }

    @Test
    fun dailySummary_equals_twoIdenticalInstancesAreEqual() {
        val a = GetDailyProgressUseCase.DailySummary(1, 10, 2, 3, 5, 0.6f, 2)
        val b = GetDailyProgressUseCase.DailySummary(1, 10, 2, 3, 5, 0.6f, 2)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun dailySummary_equals_differentStreakNotEqual() {
        val a = GetDailyProgressUseCase.DailySummary(1, 10, 2, 3, 5, 0.6f, 2)
        val b = GetDailyProgressUseCase.DailySummary(1, 10, 2, 3, 5, 0.6f, 9)

        assertNotEquals(a, b)
    }

    @Test
    fun dailySummary_equals_differentAverageScoreNotEqual() {
        val a = GetDailyProgressUseCase.DailySummary(1, 10, 2, 3, 5, 0.6f, 2)
        val b = GetDailyProgressUseCase.DailySummary(1, 10, 2, 3, 5, 0.9f, 2)

        assertNotEquals(a, b)
    }

    @Test
    fun dailySummary_allZeros_validInstance() {
        val summary = GetDailyProgressUseCase.DailySummary(0, 0, 0, 0, 0, 0f, 0)

        assertEquals(0,  summary.sessionsToday)
        assertEquals(0f, summary.averageScoreToday)
        assertEquals(0,  summary.currentStreak)
    }
}
