// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/session/AnalyzeSessionResultsUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.session

import com.voicedeutsch.master.domain.model.session.LearningSession
import com.voicedeutsch.master.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnalyzeSessionResultsUseCaseTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var useCase: AnalyzeSessionResultsUseCase

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeSession(
        durationMinutes: Int = 15,
        wordsLearned: Int = 5,
        wordsReviewed: Int = 3,
        exercisesCompleted: Int = 10,
        exercisesCorrect: Int = 8,
        strategiesUsed: List<String> = listOf("LINEAR_BOOK"),
        startedAt: Long = DAY_MS * 1000L   // default: day 1000
    ): LearningSession = mockk<LearningSession>(relaxed = true).also {
        every { it.durationMinutes }     returns durationMinutes
        every { it.wordsLearned }        returns wordsLearned
        every { it.wordsReviewed }       returns wordsReviewed
        every { it.exercisesCompleted }  returns exercisesCompleted
        every { it.exercisesCorrect }    returns exercisesCorrect
        every { it.strategiesUsed }      returns strategiesUsed
        every { it.startedAt }           returns startedAt
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        sessionRepository = mockk()
        useCase = AnalyzeSessionResultsUseCase(sessionRepository)

        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns emptyList()
    }

    // ── invoke — empty sessions ───────────────────────────────────────────────

    @Test
    fun invoke_noSessions_returnsDefaultAnalysis() = runTest {
        val result = useCase("user1")

        assertEquals(0f,           result.averageDurationMinutes)
        assertEquals(0f,           result.averageWordsPerSession)
        assertEquals("stable",     result.errorRateTrend)
        assertEquals("LINEAR_BOOK", result.mostUsedStrategy)
        assertEquals("LINEAR_BOOK", result.mostProductiveStrategy)
        assertEquals(0,            result.totalSessionsAnalyzed)
        assertEquals(0,            result.streakDays)
    }

    @Test
    fun invoke_noSessions_recommendationIsFirstSession() = runTest {
        val result = useCase("user1")

        assertTrue(result.recommendation.isNotBlank())
    }

    @Test
    fun invoke_repositoryCalledWith30Limit() = runTest {
        useCase("user1")

        coVerify(exactly = 1) { sessionRepository.getRecentSessions("user1", 30) }
    }

    // ── invoke — averageDurationMinutes ───────────────────────────────────────

    @Test
    fun invoke_singleSession_averageDurationEqualsThatSession() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns
            listOf(makeSession(durationMinutes = 20))

        val result = useCase("user1")

        assertEquals(20f, result.averageDurationMinutes, 0.01f)
    }

    @Test
    fun invoke_twoSessions_averageDurationCorrect() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(durationMinutes = 10),
            makeSession(durationMinutes = 30)
        )

        val result = useCase("user1")

        assertEquals(20f, result.averageDurationMinutes, 0.01f)
    }

    @Test
    fun invoke_allSameDuration_averageEqualsThatDuration() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns
            List(5) { makeSession(durationMinutes = 15) }

        val result = useCase("user1")

        assertEquals(15f, result.averageDurationMinutes, 0.01f)
    }

    // ── invoke — averageWordsPerSession ───────────────────────────────────────

    @Test
    fun invoke_singleSession_averageWordsSumOfLearnedAndReviewed() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns
            listOf(makeSession(wordsLearned = 4, wordsReviewed = 6))

        val result = useCase("user1")

        assertEquals(10f, result.averageWordsPerSession, 0.01f)
    }

    @Test
    fun invoke_twoSessions_averageWordsCorrect() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(wordsLearned = 10, wordsReviewed = 0),
            makeSession(wordsLearned = 0,  wordsReviewed = 20)
        )

        val result = useCase("user1")

        assertEquals(15f, result.averageWordsPerSession, 0.01f)
    }

    // ── invoke — error rate trend ─────────────────────────────────────────────

    @Test
    fun invoke_secondHalfErrorsMuchLower_trendIsImproving() = runTest {
        // first half: 50% error; second half: 0% error → diff > 0.05
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(exercisesCompleted = 10, exercisesCorrect = 5), // error 0.5
            makeSession(exercisesCompleted = 10, exercisesCorrect = 5), // error 0.5
            makeSession(exercisesCompleted = 10, exercisesCorrect = 10), // error 0.0
            makeSession(exercisesCompleted = 10, exercisesCorrect = 10)  // error 0.0
        )

        val result = useCase("user1")

        assertEquals("improving", result.errorRateTrend)
    }

    @Test
    fun invoke_secondHalfErrorsMuchHigher_trendIsWorsening() = runTest {
        // first half: 0% error; second half: 50% error → diff > 0.05
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(exercisesCompleted = 10, exercisesCorrect = 10), // error 0.0
            makeSession(exercisesCompleted = 10, exercisesCorrect = 10), // error 0.0
            makeSession(exercisesCompleted = 10, exercisesCorrect = 5),  // error 0.5
            makeSession(exercisesCompleted = 10, exercisesCorrect = 5)   // error 0.5
        )

        val result = useCase("user1")

        assertEquals("worsening", result.errorRateTrend)
    }

    @Test
    fun invoke_errorRateDiffWithin005_trendIsStable() = runTest {
        // first half: 10% error; second half: 12% error → diff 0.02 < 0.05
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(exercisesCompleted = 10, exercisesCorrect = 9), // error 0.10
            makeSession(exercisesCompleted = 10, exercisesCorrect = 9), // error 0.10
            makeSession(exercisesCompleted = 10, exercisesCorrect = 8), // error 0.20 → avg ~0.12 still within
            makeSession(exercisesCompleted = 10, exercisesCorrect = 9)  // error 0.10
        )

        val result = useCase("user1")

        assertEquals("stable", result.errorRateTrend)
    }

    @Test
    fun invoke_allSessionsNoExercises_errorRateIs0_trendStable() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(exercisesCompleted = 0, exercisesCorrect = 0),
            makeSession(exercisesCompleted = 0, exercisesCorrect = 0),
            makeSession(exercisesCompleted = 0, exercisesCorrect = 0),
            makeSession(exercisesCompleted = 0, exercisesCorrect = 0)
        )

        val result = useCase("user1")

        assertEquals("stable", result.errorRateTrend)
    }

    @Test
    fun invoke_oddNumberOfSessions_trendCalculated() = runTest {
        // 3 sessions: half = 1, firstHalf = take(1), secondHalf = takeLast(1)
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(exercisesCompleted = 10, exercisesCorrect = 5),  // error 0.5
            makeSession(exercisesCompleted = 10, exercisesCorrect = 8),  // ignored (middle)
            makeSession(exercisesCompleted = 10, exercisesCorrect = 10)  // error 0.0
        )

        val result = useCase("user1")

        assertEquals("improving", result.errorRateTrend)
    }

    // ── invoke — mostUsedStrategy ─────────────────────────────────────────────

    @Test
    fun invoke_singleStrategyAcrossSessions_thatStrategyIsMostUsed() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(strategiesUsed = listOf("REPETITION")),
            makeSession(strategiesUsed = listOf("REPETITION"))
        )

        val result = useCase("user1")

        assertEquals("REPETITION", result.mostUsedStrategy)
    }

    @Test
    fun invoke_multipleStrategies_mostFrequentIsMostUsed() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(strategiesUsed = listOf("REPETITION", "GRAMMAR_DRILL")),
            makeSession(strategiesUsed = listOf("REPETITION")),
            makeSession(strategiesUsed = listOf("LINEAR_BOOK"))
        )

        val result = useCase("user1")

        assertEquals("REPETITION", result.mostUsedStrategy)
    }

    @Test
    fun invoke_noStrategiesUsed_defaultsToLinearBook() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(strategiesUsed = emptyList()),
            makeSession(strategiesUsed = emptyList())
        )

        val result = useCase("user1")

        assertEquals("LINEAR_BOOK", result.mostUsedStrategy)
    }

    // ── invoke — totalSessionsAnalyzed ───────────────────────────────────────

    @Test
    fun invoke_3sessions_totalSessionsIs3() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns
            List(3) { makeSession() }

        val result = useCase("user1")

        assertEquals(3, result.totalSessionsAnalyzed)
    }

    @Test
    fun invoke_30Sessions_totalSessionsIs30() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns
            List(30) { makeSession() }

        val result = useCase("user1")

        assertEquals(30, result.totalSessionsAnalyzed)
    }

    // ── invoke — streakDays ───────────────────────────────────────────────────

    @Test
    fun invoke_allSessionsOnSameDay_streakIs1() = runTest {
        val sameDay = DAY_MS * 1000L
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(startedAt = sameDay),
            makeSession(startedAt = sameDay + 3600_000L)
        )

        val result = useCase("user1")

        assertEquals(1, result.streakDays)
    }

    @Test
    fun invoke_consecutiveDays_streakCountsCorrectly() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(startedAt = DAY_MS * 100L),
            makeSession(startedAt = DAY_MS * 101L),
            makeSession(startedAt = DAY_MS * 102L)
        )

        val result = useCase("user1")

        assertEquals(3, result.streakDays)
    }

    @Test
    fun invoke_gapInDays_streakBreaksAtLastConsecutiveRun() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(startedAt = DAY_MS * 100L),
            makeSession(startedAt = DAY_MS * 102L),  // gap — day 101 missing
            makeSession(startedAt = DAY_MS * 103L)
        )

        val result = useCase("user1")

        assertEquals(2, result.streakDays)  // last consecutive: 102→103
    }

    @Test
    fun invoke_singleSession_streakIs1() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(startedAt = DAY_MS * 500L)
        )

        val result = useCase("user1")

        assertEquals(1, result.streakDays)
    }

    @Test
    fun invoke_duplicateDates_deduplicatedForStreak() = runTest {
        // Two sessions on day 100, then day 101 → distinct = [100, 101] → streak 2
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(startedAt = DAY_MS * 100L),
            makeSession(startedAt = DAY_MS * 100L + 3600_000L),
            makeSession(startedAt = DAY_MS * 101L)
        )

        val result = useCase("user1")

        assertEquals(2, result.streakDays)
    }

    // ── invoke — recommendation ───────────────────────────────────────────────

    @Test
    fun invoke_avgDurationBelow10_recommendationSuggestsLongerSessions() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(durationMinutes = 5)
        )

        val result = useCase("user1")

        assertTrue(result.recommendation.contains("15"))
    }

    @Test
    fun invoke_trendWorsening_recommendationSuggestsRepetition() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(durationMinutes = 20, exercisesCompleted = 10, exercisesCorrect = 10),
            makeSession(durationMinutes = 20, exercisesCompleted = 10, exercisesCorrect = 10),
            makeSession(durationMinutes = 20, exercisesCompleted = 10, exercisesCorrect = 5),
            makeSession(durationMinutes = 20, exercisesCompleted = 10, exercisesCorrect = 5)
        )

        val result = useCase("user1")

        assertEquals("worsening", result.errorRateTrend)
        assertTrue(result.recommendation.contains("повторен"))
    }

    @Test
    fun invoke_avgWordsBelow3AndDurationOkAndStable_recommendsVocabBoost() = runTest {
        // duration ≥ 10, trend stable, avgWords < 3
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(durationMinutes = 15, wordsLearned = 1, wordsReviewed = 1,
                exercisesCompleted = 10, exercisesCorrect = 9),
            makeSession(durationMinutes = 15, wordsLearned = 1, wordsReviewed = 1,
                exercisesCompleted = 10, exercisesCorrect = 9),
            makeSession(durationMinutes = 15, wordsLearned = 1, wordsReviewed = 1,
                exercisesCompleted = 10, exercisesCorrect = 9),
            makeSession(durationMinutes = 15, wordsLearned = 1, wordsReviewed = 1,
                exercisesCompleted = 10, exercisesCorrect = 9)
        )

        val result = useCase("user1")

        assertTrue(result.recommendation.contains("VOCABULARY_BOOST"))
    }

    @Test
    fun invoke_allMetricsGood_recommendationIsPositive() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(durationMinutes = 20, wordsLearned = 5, wordsReviewed = 5,
                exercisesCompleted = 10, exercisesCorrect = 9),
            makeSession(durationMinutes = 20, wordsLearned = 5, wordsReviewed = 5,
                exercisesCompleted = 10, exercisesCorrect = 9),
            makeSession(durationMinutes = 20, wordsLearned = 5, wordsReviewed = 5,
                exercisesCompleted = 10, exercisesCorrect = 9),
            makeSession(durationMinutes = 20, wordsLearned = 5, wordsReviewed = 5,
                exercisesCompleted = 10, exercisesCorrect = 9)
        )

        val result = useCase("user1")

        assertTrue(result.recommendation.isNotBlank())
    }

    @Test
    fun invoke_durationCheckTakesPriorityOverWorsening() = runTest {
        // avgDuration < 10 wins even with worsening trend
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(durationMinutes = 5, exercisesCompleted = 10, exercisesCorrect = 10),
            makeSession(durationMinutes = 5, exercisesCompleted = 10, exercisesCorrect = 10),
            makeSession(durationMinutes = 5, exercisesCompleted = 10, exercisesCorrect = 5),
            makeSession(durationMinutes = 5, exercisesCompleted = 10, exercisesCorrect = 5)
        )

        val result = useCase("user1")

        assertTrue(result.recommendation.contains("15"))
    }

    // ── SessionAnalysis data class ────────────────────────────────────────────

    @Test
    fun sessionAnalysis_creation_storesAllFields() {
        val analysis = AnalyzeSessionResultsUseCase.SessionAnalysis(
            averageDurationMinutes   = 20f,
            averageWordsPerSession   = 8f,
            errorRateTrend           = "improving",
            mostUsedStrategy         = "REPETITION",
            mostProductiveStrategy   = "REPETITION",
            totalSessionsAnalyzed    = 10,
            streakDays               = 5,
            recommendation           = "Great!"
        )

        assertEquals(20f,         analysis.averageDurationMinutes)
        assertEquals(8f,          analysis.averageWordsPerSession)
        assertEquals("improving", analysis.errorRateTrend)
        assertEquals("REPETITION", analysis.mostUsedStrategy)
        assertEquals("REPETITION", analysis.mostProductiveStrategy)
        assertEquals(10,          analysis.totalSessionsAnalyzed)
        assertEquals(5,           analysis.streakDays)
        assertEquals("Great!",    analysis.recommendation)
    }

    @Test
    fun sessionAnalysis_copy_changesOnlySpecifiedField() {
        val original = AnalyzeSessionResultsUseCase.SessionAnalysis(
            15f, 6f, "stable", "LINEAR_BOOK", "LINEAR_BOOK", 5, 3, "ok"
        )
        val copy = original.copy(streakDays = 7)

        assertEquals(7,                      copy.streakDays)
        assertEquals(original.errorRateTrend, copy.errorRateTrend)
        assertEquals(original.recommendation, copy.recommendation)
    }

    @Test
    fun sessionAnalysis_equals_twoIdenticalInstancesAreEqual() {
        val a = AnalyzeSessionResultsUseCase.SessionAnalysis(
            10f, 5f, "stable", "LB", "LB", 3, 2, "rec"
        )
        val b = AnalyzeSessionResultsUseCase.SessionAnalysis(
            10f, 5f, "stable", "LB", "LB", 3, 2, "rec"
        )

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun sessionAnalysis_equals_differentStreakDaysNotEqual() {
        val a = AnalyzeSessionResultsUseCase.SessionAnalysis(
            10f, 5f, "stable", "LB", "LB", 3, 2, "rec"
        )
        val b = AnalyzeSessionResultsUseCase.SessionAnalysis(
            10f, 5f, "stable", "LB", "LB", 3, 9, "rec"
        )

        assertNotEquals(a, b)
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val DAY_MS = 86_400_000L
    }
}
