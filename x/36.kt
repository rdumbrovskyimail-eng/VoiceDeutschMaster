// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/session/SaveSessionUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.session

import com.voicedeutsch.master.domain.model.session.LearningSession
import com.voicedeutsch.master.domain.model.session.SessionResult
import com.voicedeutsch.master.domain.repository.SessionRepository
import com.voicedeutsch.master.domain.repository.UserRepository
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

class SaveSessionUseCaseTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var useCase: SaveSessionUseCase

    private val fixedToday = "2024-01-15"

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeSession(userId: String = "user1"): LearningSession =
        mockk<LearningSession>(relaxed = true).also {
            every { it.userId } returns userId
        }

    private fun makeResult(
        durationMinutes: Int = 20,
        wordsLearned: Int = 5,
        wordsReviewed: Int = 3,
        rulesPracticed: Int = 2,
        exercisesCompleted: Int = 10,
        exercisesCorrect: Int = 8,
        averageScore: Float = 0.8f
    ): SessionResult = mockk<SessionResult>(relaxed = true).also {
        every { it.durationMinutes }    returns durationMinutes
        every { it.wordsLearned }       returns wordsLearned
        every { it.wordsReviewed }      returns wordsReviewed
        every { it.rulesPracticed }     returns rulesPracticed
        every { it.exercisesCompleted } returns exercisesCompleted
        every { it.exercisesCorrect }   returns exercisesCorrect
        every { it.averageScore }       returns averageScore
    }

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        sessionRepository = mockk()
        userRepository    = mockk()
        useCase = SaveSessionUseCase(sessionRepository, userRepository)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        every { com.voicedeutsch.master.util.DateUtils.todayDateString() } returns fixedToday

        coEvery { sessionRepository.updateSession(any()) }              returns Unit
        coEvery { userRepository.incrementSessionStats(any(), any(), any(), any()) } returns Unit
        coEvery { sessionRepository.upsertDailyStatistics(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        ) } returns Unit
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── Step 1: updateSession ─────────────────────────────────────────────────

    @Test
    fun invoke_updateSessionCalledOnce() = runTest {
        val session = makeSession()
        useCase(session, makeResult())

        coVerify(exactly = 1) { sessionRepository.updateSession(session) }
    }

    @Test
    fun invoke_updateSessionCalledWithCorrectSession() = runTest {
        val session = makeSession(userId = "user42")

        useCase(session, makeResult())

        coVerify { sessionRepository.updateSession(session) }
    }

    // ── Step 2: incrementSessionStats ────────────────────────────────────────

    @Test
    fun invoke_incrementSessionStatsCalledOnce() = runTest {
        useCase(makeSession(), makeResult())

        coVerify(exactly = 1) {
            userRepository.incrementSessionStats(any(), any(), any(), any())
        }
    }

    @Test
    fun invoke_incrementSessionStats_userIdFromSession() = runTest {
        useCase(makeSession(userId = "user99"), makeResult())

        coVerify { userRepository.incrementSessionStats("user99", any(), any(), any()) }
    }

    @Test
    fun invoke_incrementSessionStats_durationFromResult() = runTest {
        useCase(makeSession(), makeResult(durationMinutes = 35))

        coVerify {
            userRepository.incrementSessionStats(any(), durationMinutes = 35, any(), any())
        }
    }

    @Test
    fun invoke_incrementSessionStats_wordsLearnedFromResult() = runTest {
        useCase(makeSession(), makeResult(wordsLearned = 7))

        coVerify {
            userRepository.incrementSessionStats(any(), any(), wordsLearned = 7, any())
        }
    }

    @Test
    fun invoke_incrementSessionStats_rulesLearnedFromRulesPracticed() = runTest {
        useCase(makeSession(), makeResult(rulesPracticed = 4))

        coVerify {
            userRepository.incrementSessionStats(any(), any(), any(), rulesLearned = 4)
        }
    }

    // ── Step 3: upsertDailyStatistics ────────────────────────────────────────

    @Test
    fun invoke_upsertDailyStatisticsCalledOnce() = runTest {
        useCase(makeSession(), makeResult())

        coVerify(exactly = 1) {
            sessionRepository.upsertDailyStatistics(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun invoke_upsertDailyStatistics_userIdFromSession() = runTest {
        useCase(makeSession(userId = "user77"), makeResult())

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = "user77", date = any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun invoke_upsertDailyStatistics_dateFromDateUtils() = runTest {
        useCase(makeSession(), makeResult())

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = any(), date = fixedToday, any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun invoke_upsertDailyStatistics_sessionsCountIs1() = runTest {
        useCase(makeSession(), makeResult())

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = any(), date = any(), sessionsCount = 1,
                any(), any(), any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun invoke_upsertDailyStatistics_totalMinutesFromResult() = runTest {
        useCase(makeSession(), makeResult(durationMinutes = 25))

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = any(), date = any(), sessionsCount = any(),
                totalMinutes = 25, any(), any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun invoke_upsertDailyStatistics_wordsLearnedFromResult() = runTest {
        useCase(makeSession(), makeResult(wordsLearned = 9))

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = any(), date = any(), sessionsCount = any(), totalMinutes = any(),
                wordsLearned = 9, any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun invoke_upsertDailyStatistics_wordsReviewedFromResult() = runTest {
        useCase(makeSession(), makeResult(wordsReviewed = 6))

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = any(), date = any(), sessionsCount = any(), totalMinutes = any(),
                wordsLearned = any(), wordsReviewed = 6, any(), any(), any(), any()
            )
        }
    }

    @Test
    fun invoke_upsertDailyStatistics_exercisesCompletedFromResult() = runTest {
        useCase(makeSession(), makeResult(exercisesCompleted = 12))

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = any(), date = any(), sessionsCount = any(), totalMinutes = any(),
                wordsLearned = any(), wordsReviewed = any(), exercisesCompleted = 12, any(), any(), any()
            )
        }
    }

    @Test
    fun invoke_upsertDailyStatistics_exercisesCorrectFromResult() = runTest {
        useCase(makeSession(), makeResult(exercisesCorrect = 11))

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = any(), date = any(), sessionsCount = any(), totalMinutes = any(),
                wordsLearned = any(), wordsReviewed = any(), exercisesCompleted = any(),
                exercisesCorrect = 11, any(), any()
            )
        }
    }

    @Test
    fun invoke_upsertDailyStatistics_averageScoreFromResult() = runTest {
        useCase(makeSession(), makeResult(averageScore = 0.9f))

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = any(), date = any(), sessionsCount = any(), totalMinutes = any(),
                wordsLearned = any(), wordsReviewed = any(), exercisesCompleted = any(),
                exercisesCorrect = any(), averageScore = 0.9f, any()
            )
        }
    }

    @Test
    fun invoke_upsertDailyStatistics_streakMaintainedIsTrue() = runTest {
        useCase(makeSession(), makeResult())

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = any(), date = any(), sessionsCount = any(), totalMinutes = any(),
                wordsLearned = any(), wordsReviewed = any(), exercisesCompleted = any(),
                exercisesCorrect = any(), averageScore = any(), streakMaintained = true
            )
        }
    }

    // ── Call ordering ─────────────────────────────────────────────────────────

    @Test
    fun invoke_stepsCalledInOrder() = runTest {
        val callOrder = mutableListOf<String>()
        coEvery { sessionRepository.updateSession(any()) } answers { callOrder.add("update"); Unit }
        coEvery { userRepository.incrementSessionStats(any(), any(), any(), any()) } answers {
            callOrder.add("increment"); Unit
        }
        coEvery { sessionRepository.upsertDailyStatistics(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        ) } answers { callOrder.add("upsert"); Unit }

        useCase(makeSession(), makeResult())

        assertEquals(listOf("update", "increment", "upsert"), callOrder)
    }

    // ── Zero-value result ─────────────────────────────────────────────────────

    @Test
    fun invoke_zeroValueResult_allZerosPassedThrough() = runTest {
        useCase(makeSession(), makeResult(
            durationMinutes    = 0,
            wordsLearned       = 0,
            wordsReviewed      = 0,
            rulesPracticed     = 0,
            exercisesCompleted = 0,
            exercisesCorrect   = 0,
            averageScore       = 0f
        ))

        coVerify {
            userRepository.incrementSessionStats(any(), 0, 0, 0)
        }
        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = any(), date = any(), sessionsCount = any(),
                totalMinutes = 0, wordsLearned = 0, wordsReviewed = 0,
                exercisesCompleted = 0, exercisesCorrect = 0, averageScore = 0f,
                streakMaintained = true
            )
        }
    }
}
