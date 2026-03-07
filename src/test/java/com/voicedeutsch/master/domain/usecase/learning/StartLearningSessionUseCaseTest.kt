// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/learning/StartLearningSessionUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.learning

import com.voicedeutsch.master.domain.model.session.LearningSession
import com.voicedeutsch.master.domain.model.session.SessionEvent
import com.voicedeutsch.master.domain.model.session.SessionEventType
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

class StartLearningSessionUseCaseTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var bookRepository: BookRepository
    private lateinit var useCase: StartLearningSessionUseCase

    private val fixedNow      = 1_700_000_000_000L
    private val fixedUUID1    = "session-uuid-1"
    private val fixedUUID2    = "event-uuid-2"
    private var uuidCallCount = 0

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeUserProfile(name: String = "Анна"): UserProfile =
        mockk<UserProfile>(relaxed = true).also { every { it.name } returns name }

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        sessionRepository = mockk()
        userRepository    = mockk()
        bookRepository    = mockk()
        useCase = StartLearningSessionUseCase(sessionRepository, userRepository, bookRepository)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        mockkStatic("com.voicedeutsch.master.util.UUIDKt")

        every { com.voicedeutsch.master.util.DateUtils.nowTimestamp() } returns fixedNow

        uuidCallCount = 0
        every { com.voicedeutsch.master.util.generateUUID() } answers {
            uuidCallCount++
            if (uuidCallCount == 1) fixedUUID1 else fixedUUID2
        }

        coEvery { userRepository.getUserProfile(any()) }             returns makeUserProfile()
        coEvery { bookRepository.getCurrentBookPosition(any()) }     returns Pair(2, 5)
        coEvery { sessionRepository.createSession(any()) }           returns Unit
        coEvery { sessionRepository.addSessionEvent(any()) }         returns Unit
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── invoke — session creation ─────────────────────────────────────────────

    @Test
    fun invoke_validUser_createSessionCalledOnce() = runTest {
        useCase("user1")

        coVerify(exactly = 1) { sessionRepository.createSession(any()) }
    }

    @Test
    fun invoke_validUser_sessionHasGeneratedUUID() = runTest {
        var capturedSession: LearningSession? = null
        coEvery { sessionRepository.createSession(any()) } answers {
            capturedSession = firstArg(); Unit
        }

        useCase("user1")

        assertEquals(fixedUUID1, capturedSession?.id)
    }

    @Test
    fun invoke_validUser_sessionUserIdMatchesParam() = runTest {
        var capturedSession: LearningSession? = null
        coEvery { sessionRepository.createSession(any()) } answers {
            capturedSession = firstArg(); Unit
        }

        useCase("user42")

        assertEquals("user42", capturedSession?.userId)
    }

    @Test
    fun invoke_validUser_sessionStartedAtIsNow() = runTest {
        var capturedSession: LearningSession? = null
        coEvery { sessionRepository.createSession(any()) } answers {
            capturedSession = firstArg(); Unit
        }

        useCase("user1")

        assertEquals(fixedNow, capturedSession?.startedAt)
        assertEquals(fixedNow, capturedSession?.createdAt)
    }

    @Test
    fun invoke_validUser_sessionEndedAtIsNull() = runTest {
        var capturedSession: LearningSession? = null
        coEvery { sessionRepository.createSession(any()) } answers {
            capturedSession = firstArg(); Unit
        }

        useCase("user1")

        assertNull(capturedSession?.endedAt)
    }

    @Test
    fun invoke_validUser_sessionDurationMinutesIsZero() = runTest {
        var capturedSession: LearningSession? = null
        coEvery { sessionRepository.createSession(any()) } answers {
            capturedSession = firstArg(); Unit
        }

        useCase("user1")

        assertEquals(0, capturedSession?.durationMinutes)
    }

    @Test
    fun invoke_validUser_sessionStrategiesUsedIsEmpty() = runTest {
        var capturedSession: LearningSession? = null
        coEvery { sessionRepository.createSession(any()) } answers {
            capturedSession = firstArg(); Unit
        }

        useCase("user1")

        assertTrue(capturedSession?.strategiesUsed?.isEmpty() == true)
    }

    @Test
    fun invoke_validUser_sessionCountersAreZero() = runTest {
        var capturedSession: LearningSession? = null
        coEvery { sessionRepository.createSession(any()) } answers {
            capturedSession = firstArg(); Unit
        }

        useCase("user1")

        assertEquals(0, capturedSession?.wordsLearned)
        assertEquals(0, capturedSession?.wordsReviewed)
        assertEquals(0, capturedSession?.rulesPracticed)
        assertEquals(0, capturedSession?.exercisesCompleted)
        assertEquals(0, capturedSession?.exercisesCorrect)
    }

    @Test
    fun invoke_validUser_sessionAveragePronunciationScoreIsZero() = runTest {
        var capturedSession: LearningSession? = null
        coEvery { sessionRepository.createSession(any()) } answers {
            capturedSession = firstArg(); Unit
        }

        useCase("user1")

        assertEquals(0f, capturedSession?.averagePronunciationScore)
    }

    @Test
    fun invoke_validUser_sessionBookPositionMatchesRepository() = runTest {
        coEvery { bookRepository.getCurrentBookPosition("user1") } returns Pair(3, 7)
        var capturedSession: LearningSession? = null
        coEvery { sessionRepository.createSession(any()) } answers {
            capturedSession = firstArg(); Unit
        }

        useCase("user1")

        assertEquals(3, capturedSession?.bookChapterStart)
        assertEquals(7, capturedSession?.bookLessonStart)
        assertEquals(3, capturedSession?.bookChapterEnd)
        assertEquals(7, capturedSession?.bookLessonEnd)
    }

    @Test
    fun invoke_validUser_sessionSummaryIsEmpty() = runTest {
        var capturedSession: LearningSession? = null
        coEvery { sessionRepository.createSession(any()) } answers {
            capturedSession = firstArg(); Unit
        }

        useCase("user1")

        assertEquals("", capturedSession?.sessionSummary)
    }

    @Test
    fun invoke_validUser_sessionMoodEstimateIsNull() = runTest {
        var capturedSession: LearningSession? = null
        coEvery { sessionRepository.createSession(any()) } answers {
            capturedSession = firstArg(); Unit
        }

        useCase("user1")

        assertNull(capturedSession?.moodEstimate)
    }

    // ── invoke — session start event ──────────────────────────────────────────

    @Test
    fun invoke_validUser_addSessionEventCalledOnce() = runTest {
        useCase("user1")

        coVerify(exactly = 1) { sessionRepository.addSessionEvent(any()) }
    }

    @Test
    fun invoke_validUser_startEventHasSecondGeneratedUUID() = runTest {
        var capturedEvent: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            capturedEvent = firstArg(); Unit
        }

        useCase("user1")

        assertEquals(fixedUUID2, capturedEvent?.id)
    }

    @Test
    fun invoke_validUser_startEventSessionIdMatchesSession() = runTest {
        var capturedEvent: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            capturedEvent = firstArg(); Unit
        }

        useCase("user1")

        assertEquals(fixedUUID1, capturedEvent?.sessionId)
    }

    @Test
    fun invoke_validUser_startEventTypeIsSessionStart() = runTest {
        var capturedEvent: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            capturedEvent = firstArg(); Unit
        }

        useCase("user1")

        assertEquals(SessionEventType.SESSION_START, capturedEvent?.eventType)
    }

    @Test
    fun invoke_validUser_startEventTimestampIsNow() = runTest {
        var capturedEvent: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            capturedEvent = firstArg(); Unit
        }

        useCase("user1")

        assertEquals(fixedNow, capturedEvent?.timestamp)
        assertEquals(fixedNow, capturedEvent?.createdAt)
    }

    @Test
    fun invoke_validUser_startEventDetailsJsonContainsUserId() = runTest {
        var capturedEvent: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            capturedEvent = firstArg(); Unit
        }

        useCase("user1")

        assertTrue(capturedEvent?.detailsJson?.contains("user1") == true)
    }

    @Test
    fun invoke_validUser_startEventDetailsJsonContainsChapterAndLesson() = runTest {
        coEvery { bookRepository.getCurrentBookPosition("user1") } returns Pair(4, 9)
        var capturedEvent: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            capturedEvent = firstArg(); Unit
        }

        useCase("user1")

        val json = capturedEvent?.detailsJson ?: ""
        assertTrue(json.contains("4"))
        assertTrue(json.contains("9"))
    }

    @Test
    fun invoke_createSessionCalledBeforeAddSessionEvent() = runTest {
        val callOrder = mutableListOf<String>()
        coEvery { sessionRepository.createSession(any()) } answers { callOrder.add("create"); Unit }
        coEvery { sessionRepository.addSessionEvent(any()) } answers { callOrder.add("event"); Unit }

        useCase("user1")

        assertEquals(listOf("create", "event"), callOrder)
    }

    // ── invoke — user name resolution ─────────────────────────────────────────

    @Test
    fun invoke_profileWithName_userNameInResult() = runTest {
        coEvery { userRepository.getUserProfile("user1") } returns makeUserProfile(name = "Мария")

        val result = useCase("user1")

        assertEquals("Мария", result.userName)
    }

    @Test
    fun invoke_profileIsNull_userNameIsDefault() = runTest {
        coEvery { userRepository.getUserProfile("user1") } returns null

        val result = useCase("user1")

        assertEquals("Пользователь", result.userName)
    }

    // ── invoke — returned SessionStartData ────────────────────────────────────

    @Test
    fun invoke_validUser_resultContainsCreatedSession() = runTest {
        val result = useCase("user1")

        assertEquals(fixedUUID1, result.session.id)
        assertEquals("user1",   result.session.userId)
    }

    @Test
    fun invoke_validUser_resultCurrentChapterFromRepository() = runTest {
        coEvery { bookRepository.getCurrentBookPosition("user1") } returns Pair(6, 3)

        val result = useCase("user1")

        assertEquals(6, result.currentChapter)
    }

    @Test
    fun invoke_validUser_resultCurrentLessonFromRepository() = runTest {
        coEvery { bookRepository.getCurrentBookPosition("user1") } returns Pair(6, 3)

        val result = useCase("user1")

        assertEquals(3, result.currentLesson)
    }

    @Test
    fun invoke_validUser_bookRepositoryCalledWithCorrectUserId() = runTest {
        useCase("user99")

        coVerify(exactly = 1) { bookRepository.getCurrentBookPosition("user99") }
    }

    @Test
    fun invoke_validUser_userRepositoryCalledWithCorrectUserId() = runTest {
        useCase("user99")

        coVerify(exactly = 1) { userRepository.getUserProfile("user99") }
    }

    // ── SessionStartData data class ───────────────────────────────────────────

    @Test
    fun sessionStartData_creation_storesAllFields() {
        val session = mockk<LearningSession>(relaxed = true)
        val data = StartLearningSessionUseCase.SessionStartData(
            session        = session,
            currentChapter = 2,
            currentLesson  = 5,
            userName       = "Иван"
        )

        assertEquals(session, data.session)
        assertEquals(2,       data.currentChapter)
        assertEquals(5,       data.currentLesson)
        assertEquals("Иван",  data.userName)
    }

    @Test
    fun sessionStartData_copy_changesOnlySpecifiedField() {
        val session = mockk<LearningSession>(relaxed = true)
        val original = StartLearningSessionUseCase.SessionStartData(session, 1, 1, "Имя")
        val copy     = original.copy(userName = "Другой")

        assertEquals("Другой", copy.userName)
        assertEquals(original.session,        copy.session)
        assertEquals(original.currentChapter, copy.currentChapter)
        assertEquals(original.currentLesson,  copy.currentLesson)
    }

    @Test
    fun sessionStartData_equals_twoIdenticalInstancesAreEqual() {
        val session = mockk<LearningSession>(relaxed = true)
        val a = StartLearningSessionUseCase.SessionStartData(session, 3, 4, "Ольга")
        val b = StartLearningSessionUseCase.SessionStartData(session, 3, 4, "Ольга")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun sessionStartData_equals_differentUserNameNotEqual() {
        val session = mockk<LearningSession>(relaxed = true)
        val a = StartLearningSessionUseCase.SessionStartData(session, 1, 1, "Алексей")
        val b = StartLearningSessionUseCase.SessionStartData(session, 1, 1, "Борис")

        assertNotEquals(a, b)
    }

    @Test
    fun sessionStartData_equals_differentChapterNotEqual() {
        val session = mockk<LearningSession>(relaxed = true)
        val a = StartLearningSessionUseCase.SessionStartData(session, 1, 1, "Имя")
        val b = StartLearningSessionUseCase.SessionStartData(session, 2, 1, "Имя")

        assertNotEquals(a, b)
    }
}
