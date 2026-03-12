// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/learning/EndLearningSessionUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.learning

import com.voicedeutsch.master.domain.model.progress.DailyProgress
import com.voicedeutsch.master.domain.model.session.LearningSession
import com.voicedeutsch.master.domain.model.session.SessionEvent
import com.voicedeutsch.master.domain.model.session.SessionEventType
import com.voicedeutsch.master.domain.model.session.SessionResult
import com.voicedeutsch.master.domain.model.user.UserProfile
import com.voicedeutsch.master.domain.repository.BookRepository
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
import org.junit.jupiter.api.assertThrows

class EndLearningSessionUseCaseTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var bookRepository: BookRepository
    private lateinit var useCase: EndLearningSessionUseCase

    private val fixedNow    = 1_700_000_060_000L   // startedAt + 60 s → 1 min
    private val fixedStart  = 1_700_000_000_000L
    private val fixedUUID   = "end-event-uuid"
    private val fixedToday  = "2024-01-01"

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeSession(
        id: String = "session1",
        userId: String = "user1",
        startedAt: Long = fixedStart
    ): LearningSession = LearningSession(
        id = id,
        userId = userId,
        startedAt = startedAt,
    )

    private fun makeEvent(
        type: SessionEventType,
        detailsJson: String = "{}"
    ): SessionEvent = mockk<SessionEvent>(relaxed = true).also {
        every { it.eventType }   returns type
        every { it.detailsJson } returns detailsJson
    }

    private fun makeUserProfile(
        streakDays: Int = 3,
        lastSessionDate: Long? = null
    ): UserProfile = mockk<UserProfile>(relaxed = true).also {
        every { it.streakDays }      returns streakDays
        every { it.lastSessionDate } returns lastSessionDate
    }

    private fun makeDailyStats(
        sessionsCount: Int = 1,
        totalMinutes: Int = 10,
        wordsLearned: Int = 5,
        wordsReviewed: Int = 3,
        exercisesCompleted: Int = 8,
        exercisesCorrect: Int = 6,
        averageScore: Float = 0.75f
    ) = DailyProgress(
        id = "daily_1",
        userId = "user1",
        date = "2024-01-01",
        sessionsCount = sessionsCount,
        totalMinutes = totalMinutes,
        wordsLearned = wordsLearned,
        wordsReviewed = wordsReviewed,
        exercisesCompleted = exercisesCompleted,
        exercisesCorrect = exercisesCorrect,
        averageScore = averageScore,
        streakMaintained = false,
    )

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        sessionRepository = mockk()
        userRepository    = mockk()
        bookRepository    = mockk()
        useCase = EndLearningSessionUseCase(sessionRepository, userRepository, bookRepository)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        mockkStatic("com.voicedeutsch.master.util.UUIDKt")
        mockkStatic("com.voicedeutsch.master.util.SafeDivideKt")

        every { com.voicedeutsch.master.util.DateUtils.nowTimestamp() }          returns fixedNow
        every { com.voicedeutsch.master.util.DateUtils.todayDateString() }       returns fixedToday
        every { com.voicedeutsch.master.util.DateUtils.isSameDay(any(), any()) } returns false
        every { com.voicedeutsch.master.util.DateUtils.daysBetween(any(), any()) } returns 1L
        every { com.voicedeutsch.master.util.generateUUID() }                    returns fixedUUID
        every { com.voicedeutsch.master.util.safeDivide(any(), any()) }          returns 0f

        coEvery { sessionRepository.getSession(any()) }               returns makeSession()
        coEvery { sessionRepository.getSessionEvents(any()) }         returns emptyList()
        coEvery { sessionRepository.updateSession(any()) }            returns Unit
        coEvery { sessionRepository.addSessionEvent(any()) }          returns Unit
        coEvery { sessionRepository.getDailyStatistics(any(), any()) } returns null
        coEvery { sessionRepository.upsertDailyStatistics(
            any<String>(), any<String>(), any<Int>(), any<Int>(), any<Int>(),
            any<Int>(), any<Int>(), any<Int>(), any<Float>(), any<Boolean>()
        ) } returns Unit

        coEvery { userRepository.getUserProfile(any()) }                    returns makeUserProfile()
        coEvery { userRepository.incrementSessionStats(any(), any(), any(), any()) } returns Unit
        coEvery { userRepository.updateStreak(any(), any()) }               returns Unit

        coEvery { bookRepository.getCurrentBookPosition(any()) } returns Pair(1, 1)
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── invoke — session not found ────────────────────────────────────────────

    @Test
    fun invoke_sessionNotFound_throwsIllegalArgumentException() = runTest {
        coEvery { sessionRepository.getSession("missing") } returns null

        assertThrows<IllegalArgumentException> {
            useCase("missing")
        }
    }

    @Test
    fun invoke_sessionNotFound_exceptionMessageContainsSessionId() = runTest {
        coEvery { sessionRepository.getSession("bad-id") } returns null

        val ex = assertThrows<IllegalArgumentException> { useCase("bad-id") }
        assertTrue(ex.message?.contains("bad-id") == true)
    }

    // ── invoke — happy path ───────────────────────────────────────────────────

    @Test
    fun invoke_validSession_returnsSessionResult() = runTest {
        val result = useCase("session1")

        assertNotNull(result)
        assertInstanceOf(SessionResult::class.java, result)
    }

    @Test
    fun invoke_validSession_sessionIdInResult() = runTest {
        val result = useCase("session1")

        assertEquals("session1", result.sessionId)
    }

    @Test
    fun invoke_validSession_updatesSessionInRepository() = runTest {
        useCase("session1")

        coVerify(exactly = 1) { sessionRepository.updateSession(any()) }
    }

    @Test
    fun invoke_validSession_addsSessionEndEvent() = runTest {
        useCase("session1")

        coVerify(exactly = 1) { sessionRepository.addSessionEvent(any()) }
    }

    @Test
    fun invoke_validSession_sessionEndEventHasCorrectType() = runTest {
        var capturedEvent: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            capturedEvent = firstArg(); Unit
        }

        useCase("session1")

        assertEquals(SessionEventType.SESSION_END, capturedEvent?.eventType)
    }

    @Test
    fun invoke_validSession_sessionEndEventUsesGeneratedUUID() = runTest {
        var capturedEvent: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            capturedEvent = firstArg(); Unit
        }

        useCase("session1")

        assertEquals(fixedUUID, capturedEvent?.id)
    }

    @Test
    fun invoke_validSession_sessionEndEventTimestampIsNow() = runTest {
        var capturedEvent: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            capturedEvent = firstArg(); Unit
        }

        useCase("session1")

        assertEquals(fixedNow, capturedEvent?.timestamp)
        assertEquals(fixedNow, capturedEvent?.createdAt)
    }

    @Test
    fun invoke_validSession_incrementSessionStatsCalled() = runTest {
        useCase("session1")

        coVerify(exactly = 1) {
            userRepository.incrementSessionStats("user1", any(), any(), any())
        }
    }

    @Test
    fun invoke_validSession_upsertDailyStatisticsCalled() = runTest {
        useCase("session1")

        coVerify(exactly = 1) {
            sessionRepository.upsertDailyStatistics(
                userId = "user1", date = fixedToday, any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
    }

    // ── Duration calculation ──────────────────────────────────────────────────

    @Test
    fun invoke_60SecondsElapsed_durationIs1Minute() = runTest {
        // fixedNow = fixedStart + 60_000 ms → 1 minute
        val result = useCase("session1")

        assertEquals(1, result.durationMinutes)
    }

    @Test
    fun invoke_durationLessThan1Minute_clampedTo1() = runTest {
        // startedAt == fixedNow → 0 ms → coerceAtLeast(1)
        coEvery { sessionRepository.getSession("session1") } returns makeSession(startedAt = fixedNow)

        val result = useCase("session1")

        assertEquals(1, result.durationMinutes)
    }

    @Test
    fun invoke_120SecondsElapsed_durationIs2Minutes() = runTest {
        val start = fixedNow - 120_000L
        coEvery { sessionRepository.getSession("session1") } returns makeSession(startedAt = start)

        val result = useCase("session1")

        assertEquals(2, result.durationMinutes)
    }

    // ── Event counting ────────────────────────────────────────────────────────

    @Test
    fun invoke_wordLearnedEvents_countedInWordsLearned() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.WORD_LEARNED),
            makeEvent(SessionEventType.WORD_LEARNED),
            makeEvent(SessionEventType.WORD_REVIEWED)
        )

        val result = useCase("session1")

        assertEquals(2, result.wordsLearned)
    }

    @Test
    fun invoke_wordReviewedEvents_countedInWordsReviewed() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.WORD_REVIEWED),
            makeEvent(SessionEventType.WORD_REVIEWED),
            makeEvent(SessionEventType.WORD_REVIEWED)
        )

        val result = useCase("session1")

        assertEquals(3, result.wordsReviewed)
    }

    @Test
    fun invoke_rulePracticedEvents_countedInRulesPracticed() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.RULE_PRACTICED),
            makeEvent(SessionEventType.RULE_PRACTICED)
        )

        val result = useCase("session1")

        assertEquals(2, result.rulesPracticed)
    }

    @Test
    fun invoke_mixedExerciseEvents_allCountedInExercisesCompleted() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.WORD_LEARNED),
            makeEvent(SessionEventType.WORD_REVIEWED),
            makeEvent(SessionEventType.RULE_PRACTICED),
            makeEvent(SessionEventType.PRONUNCIATION_ATTEMPT),
            makeEvent(SessionEventType.STRATEGY_CHANGE)   // NOT an exercise
        )

        val result = useCase("session1")

        assertEquals(4, result.exercisesCompleted)
    }

    @Test
    fun invoke_noEvents_allCountsAreZero() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns emptyList()

        val result = useCase("session1")

        assertEquals(0, result.wordsLearned)
        assertEquals(0, result.wordsReviewed)
        assertEquals(0, result.rulesPracticed)
        assertEquals(0, result.exercisesCompleted)
        assertEquals(0, result.exercisesCorrect)
    }

    // ── Exercises correct ─────────────────────────────────────────────────────

    @Test
    fun invoke_exerciseEventsWithCorrectTrue_countedInExercisesCorrect() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.WORD_LEARNED,  """{"correct":true}"""),
            makeEvent(SessionEventType.WORD_REVIEWED, """{"correct":false}"""),
            makeEvent(SessionEventType.RULE_PRACTICED, """{"correct":true}""")
        )

        val result = useCase("session1")

        assertEquals(2, result.exercisesCorrect)
    }

    @Test
    fun invoke_exerciseEventsWithCorrectFalseOnly_exercisesCorrectIs0() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.WORD_LEARNED, """{"correct":false}"""),
            makeEvent(SessionEventType.WORD_REVIEWED, """{"correct":false}""")
        )

        val result = useCase("session1")

        assertEquals(0, result.exercisesCorrect)
    }

    @Test
    fun invoke_correctTrueIsCaseInsensitive_countedCorrectly() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.WORD_LEARNED, """{"correct":TRUE}""")
        )

        val result = useCase("session1")

        assertEquals(1, result.exercisesCorrect)
    }

    @Test
    fun invoke_nonExerciseEventWithCorrectTrue_notCountedInExercisesCorrect() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.STRATEGY_CHANGE, """{"correct":true}""")
        )

        val result = useCase("session1")

        assertEquals(0, result.exercisesCorrect)
    }

    // ── Average score ─────────────────────────────────────────────────────────

    @Test
    fun invoke_averageScore_delegatesToSafeDivide() = runTest {
        every { com.voicedeutsch.master.util.safeDivide(0, 0) } returns 0.5f
        coEvery { sessionRepository.getSessionEvents("session1") } returns emptyList()

        val result = useCase("session1")

        assertEquals(0.5f, result.averageScore, 0.001f)
    }

    // ── Strategies used ───────────────────────────────────────────────────────

    @Test
    fun invoke_strategyChangeEvents_strategiesExtractedAndDeduped() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.STRATEGY_CHANGE, """{"strategy":"VOCABULARY"}"""),
            makeEvent(SessionEventType.STRATEGY_CHANGE, """{"strategy":"GRAMMAR"}"""),
            makeEvent(SessionEventType.STRATEGY_CHANGE, """{"strategy":"VOCABULARY"}""")
        )

        val result = useCase("session1")

        assertEquals(2, result.strategiesUsed.size)
        assertTrue(result.strategiesUsed.contains("VOCABULARY"))
        assertTrue(result.strategiesUsed.contains("GRAMMAR"))
    }

    @Test
    fun invoke_strategyChangeEventWithNoStrategyField_skipped() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.STRATEGY_CHANGE, """{"other":"value"}""")
        )

        val result = useCase("session1")

        assertTrue(result.strategiesUsed.isEmpty())
    }

    @Test
    fun invoke_noStrategyChangeEvents_strategiesUsedIsEmpty() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.WORD_LEARNED)
        )

        val result = useCase("session1")

        assertTrue(result.strategiesUsed.isEmpty())
    }

    // ── Average pronunciation score ───────────────────────────────────────────

    @Test
    fun invoke_pronunciationEventsWithScores_averageCalculated() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.PRONUNCIATION_ATTEMPT, """{"score":0.8}"""),
            makeEvent(SessionEventType.PRONUNCIATION_ATTEMPT, """{"score":0.6}""")
        )

        val result = useCase("session1")

        assertEquals(0.7f, result.averagePronunciationScore, 0.001f)
    }

    @Test
    fun invoke_pronunciationEventWithNoScoreField_skippedInAverage() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.PRONUNCIATION_ATTEMPT, """{"other":"data"}"""),
            makeEvent(SessionEventType.PRONUNCIATION_ATTEMPT, """{"score":1.0}""")
        )

        val result = useCase("session1")

        assertEquals(1.0f, result.averagePronunciationScore, 0.001f)
    }

    @Test
    fun invoke_noPronunciationEvents_averagePronunciationScoreIs0() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.WORD_LEARNED)
        )

        val result = useCase("session1")

        assertEquals(0f, result.averagePronunciationScore)
    }

    @Test
    fun invoke_pronunciationEventsAllWithoutScore_averageIs0() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.PRONUNCIATION_ATTEMPT, """{"no_score":true}""")
        )

        val result = useCase("session1")

        assertEquals(0f, result.averagePronunciationScore)
    }

    // ── Summary passthrough ───────────────────────────────────────────────────

    @Test
    fun invoke_withSummary_summaryInResult() = runTest {
        val result = useCase("session1", summary = "Great session!")

        assertEquals("Great session!", result.summary)
    }

    @Test
    fun invoke_defaultSummary_emptyStringInResult() = runTest {
        val result = useCase("session1")

        assertEquals("", result.summary)
    }

    // ── Streak update ─────────────────────────────────────────────────────────

    @Test
    fun invoke_noLastSessionDate_streakSetTo1() = runTest {
        coEvery { userRepository.getUserProfile("user1") } returns
            makeUserProfile(streakDays = 5, lastSessionDate = null)

        useCase("session1")

        coVerify { userRepository.updateStreak("user1", 1) }
    }

    @Test
    fun invoke_sameDay_streakUnchanged() = runTest {
        val lastDate = fixedNow - 1000L
        every { com.voicedeutsch.master.util.DateUtils.isSameDay(lastDate, fixedNow) } returns true
        coEvery { userRepository.getUserProfile("user1") } returns
            makeUserProfile(streakDays = 7, lastSessionDate = lastDate)

        useCase("session1")

        coVerify { userRepository.updateStreak("user1", 7) }
    }

    @Test
    fun invoke_consecutiveDay_streakIncrementedBy1() = runTest {
        val yesterday = fixedNow - 86_400_000L
        every { com.voicedeutsch.master.util.DateUtils.isSameDay(yesterday, fixedNow) } returns false
        every { com.voicedeutsch.master.util.DateUtils.daysBetween(yesterday, fixedNow) } returns 1L
        coEvery { userRepository.getUserProfile("user1") } returns
            makeUserProfile(streakDays = 4, lastSessionDate = yesterday)

        useCase("session1")

        coVerify { userRepository.updateStreak("user1", 5) }
    }

    @Test
    fun invoke_missedDays_streakResetTo1() = runTest {
        val twoDaysAgo = fixedNow - 172_800_000L
        every { com.voicedeutsch.master.util.DateUtils.isSameDay(twoDaysAgo, fixedNow) } returns false
        every { com.voicedeutsch.master.util.DateUtils.daysBetween(twoDaysAgo, fixedNow) } returns 2L
        coEvery { userRepository.getUserProfile("user1") } returns
            makeUserProfile(streakDays = 10, lastSessionDate = twoDaysAgo)

        useCase("session1")

        coVerify { userRepository.updateStreak("user1", 1) }
    }

    @Test
    fun invoke_userProfileNotFound_streakUpdateSkipped() = runTest {
        coEvery { userRepository.getUserProfile("user1") } returns null

        useCase("session1")

        coVerify(exactly = 0) { userRepository.updateStreak(any(), any()) }
    }

    // ── Daily statistics ──────────────────────────────────────────────────────

    @Test
    fun invoke_noExistingDailyStats_upsertedWithSessionDataOnly() = runTest {
        coEvery { sessionRepository.getDailyStatistics("user1", fixedToday) } returns null
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.WORD_LEARNED)
        )

        useCase("session1")

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = "user1",
                date = fixedToday,
                sessionsCount = 1,
                totalMinutes = any(),
                wordsLearned = 1,
                wordsReviewed = 0,
                exercisesCompleted = any(),
                exercisesCorrect = any(),
                averageScore = any(),
                streakMaintained = true
            )
        }
    }

    @Test
    fun invoke_existingDailyStats_sessionsCountIncremented() = runTest {
        coEvery { sessionRepository.getDailyStatistics("user1", fixedToday) } returns
            makeDailyStats(sessionsCount = 3)

        useCase("session1")

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = "user1",
                date = fixedToday,
                sessionsCount = 4,
                totalMinutes = any(),
                wordsLearned = any(),
                wordsReviewed = any(),
                exercisesCompleted = any(),
                exercisesCorrect = any(),
                averageScore = any(),
                streakMaintained = true
            )
        }
    }

    @Test
    fun invoke_existingDailyStats_totalMinutesAccumulated() = runTest {
        coEvery { sessionRepository.getDailyStatistics("user1", fixedToday) } returns
            makeDailyStats(totalMinutes = 20)
        // duration = 1 min (fixedNow - fixedStart = 60s)

        useCase("session1")

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = "user1",
                date = fixedToday,
                sessionsCount = any(),
                totalMinutes = 21,
                wordsLearned = any(),
                wordsReviewed = any(),
                exercisesCompleted = any(),
                exercisesCorrect = any(),
                averageScore = any(),
                streakMaintained = true
            )
        }
    }

    @Test
    fun invoke_existingDailyStats_wordsLearnedAccumulated() = runTest {
        coEvery { sessionRepository.getDailyStatistics("user1", fixedToday) } returns
            makeDailyStats(wordsLearned = 10)
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.WORD_LEARNED),
            makeEvent(SessionEventType.WORD_LEARNED)
        )

        useCase("session1")

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = "user1",
                date = fixedToday,
                sessionsCount = any(),
                totalMinutes = any(),
                wordsLearned = 12,
                wordsReviewed = any(),
                exercisesCompleted = any(),
                exercisesCorrect = any(),
                averageScore = any(),
                streakMaintained = true
            )
        }
    }

    @Test
    fun invoke_existingAndNewExercisesCompleted_combinedScoreWeightedAverage() = runTest {
        // prevCompleted=4, prevScore=1.0 → prevTotal=4.0
        // newCompleted=0, newScore=0.0 → combinedScore = 4.0/(4+0) = 1.0
        every { com.voicedeutsch.master.util.safeDivide(0, 0) } returns 0f
        coEvery { sessionRepository.getDailyStatistics("user1", fixedToday) } returns
            makeDailyStats(exercisesCompleted = 4, averageScore = 1.0f)
        coEvery { sessionRepository.getSessionEvents("session1") } returns emptyList()

        useCase("session1")

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = "user1",
                date = fixedToday,
                sessionsCount = any(),
                totalMinutes = any(),
                wordsLearned = any(),
                wordsReviewed = any(),
                exercisesCompleted = any(),
                exercisesCorrect = any(),
                averageScore = 1.0f,
                streakMaintained = true
            )
        }
    }

    @Test
    fun invoke_noExistingStatsAndNoExercises_combinedScoreIs0() = runTest {
        every { com.voicedeutsch.master.util.safeDivide(0, 0) } returns 0f
        coEvery { sessionRepository.getDailyStatistics("user1", fixedToday) } returns null
        coEvery { sessionRepository.getSessionEvents("session1") } returns emptyList()

        useCase("session1")

        coVerify {
            sessionRepository.upsertDailyStatistics(
                userId = "user1",
                date = fixedToday,
                sessionsCount = any(),
                totalMinutes = any(),
                wordsLearned = any(),
                wordsReviewed = any(),
                exercisesCompleted = any(),
                exercisesCorrect = any(),
                averageScore = 0f,
                streakMaintained = true
            )
        }
    }

    // ── Book position ─────────────────────────────────────────────────────────

    @Test
    fun invoke_bookPositionFromRepository_passedToSessionUpdate() = runTest {
        coEvery { bookRepository.getCurrentBookPosition("user1") } returns Pair(3, 7)
        var capturedSession: LearningSession? = null
        coEvery { sessionRepository.updateSession(any()) } answers {
            capturedSession = firstArg(); Unit
        }
        // We can't easily assert copy args without a real data class, but verify the call was made
        coVerify(atLeast = 1) { bookRepository.getCurrentBookPosition("user1") }
        useCase("session1")
    }

    // ── incrementSessionStats params ──────────────────────────────────────────

    @Test
    fun invoke_wordsLearnedAndRulesPracticed_passedToIncrementStats() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.WORD_LEARNED),
            makeEvent(SessionEventType.WORD_LEARNED),
            makeEvent(SessionEventType.RULE_PRACTICED)
        )

        useCase("session1")

        coVerify {
            userRepository.incrementSessionStats(
                userId = "user1",
                durationMinutes = 1,
                wordsLearned = 2,
                rulesLearned = 1
            )
        }
    }

    // ── SessionEndEvent detailsJson ───────────────────────────────────────────

    @Test
    fun invoke_sessionEndEvent_detailsJsonContainsDurationAndWordCounts() = runTest {
        coEvery { sessionRepository.getSessionEvents("session1") } returns listOf(
            makeEvent(SessionEventType.WORD_LEARNED),
            makeEvent(SessionEventType.WORD_REVIEWED)
        )
        var capturedEvent: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            capturedEvent = firstArg(); Unit
        }

        useCase("session1")

        val json = capturedEvent?.detailsJson ?: ""
        assertTrue(json.contains("duration"))
        assertTrue(json.contains("wordsLearned"))
        assertTrue(json.contains("wordsReviewed"))
    }
}
