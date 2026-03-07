// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/session/GetSessionHistoryUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.session

import com.voicedeutsch.master.domain.model.session.LearningSession
import com.voicedeutsch.master.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetSessionHistoryUseCaseTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var useCase: GetSessionHistoryUseCase

    private val session1 = mockk<LearningSession>(relaxed = true)
    private val session2 = mockk<LearningSession>(relaxed = true)

    @BeforeEach
    fun setUp() {
        sessionRepository = mockk()
        useCase = GetSessionHistoryUseCase(sessionRepository)

        coEvery { sessionRepository.getRecentSessions(any(), any()) }  returns emptyList()
        coEvery { sessionRepository.getSessionsSince(any(), any()) }   returns emptyList()
    }

    // ── invoke — fromDate null → getRecentSessions ────────────────────────────

    @Test
    fun invoke_fromDateNull_callsGetRecentSessions() = runTest {
        useCase(makeParams(fromDate = null))

        coVerify(exactly = 1) { sessionRepository.getRecentSessions(any(), any()) }
        coVerify(exactly = 0) { sessionRepository.getSessionsSince(any(), any()) }
    }

    @Test
    fun invoke_fromDateNull_passesUserIdToRepository() = runTest {
        useCase(makeParams(userId = "user99", fromDate = null))

        coVerify { sessionRepository.getRecentSessions("user99", any()) }
    }

    @Test
    fun invoke_fromDateNull_passesLimitToRepository() = runTest {
        useCase(makeParams(limit = 15, fromDate = null))

        coVerify { sessionRepository.getRecentSessions(any(), 15) }
    }

    @Test
    fun invoke_fromDateNull_defaultLimit30() = runTest {
        useCase(GetSessionHistoryUseCase.Params(userId = "user1"))

        coVerify { sessionRepository.getRecentSessions("user1", 30) }
    }

    @Test
    fun invoke_fromDateNull_returnsSessionsFromRepository() = runTest {
        coEvery { sessionRepository.getRecentSessions("user1", 30) } returns
            listOf(session1, session2)

        val result = useCase(makeParams())

        assertEquals(listOf(session1, session2), result)
    }

    @Test
    fun invoke_fromDateNull_emptyRepository_returnsEmptyList() = runTest {
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns emptyList()

        val result = useCase(makeParams())

        assertTrue(result.isEmpty())
    }

    // ── invoke — fromDate present → getSessionsSince ──────────────────────────

    @Test
    fun invoke_fromDatePresent_callsGetSessionsSince() = runTest {
        useCase(makeParams(fromDate = 1_700_000_000_000L))

        coVerify(exactly = 1) { sessionRepository.getSessionsSince(any(), any()) }
        coVerify(exactly = 0) { sessionRepository.getRecentSessions(any(), any()) }
    }

    @Test
    fun invoke_fromDatePresent_passesUserIdToRepository() = runTest {
        useCase(makeParams(userId = "user42", fromDate = 1_000L))

        coVerify { sessionRepository.getSessionsSince("user42", any()) }
    }

    @Test
    fun invoke_fromDatePresent_passesFromDateToRepository() = runTest {
        val ts = 1_700_000_000_000L
        useCase(makeParams(fromDate = ts))

        coVerify { sessionRepository.getSessionsSince(any(), ts) }
    }

    @Test
    fun invoke_fromDatePresent_returnsSessionsFromRepository() = runTest {
        coEvery { sessionRepository.getSessionsSince("user1", any()) } returns
            listOf(session1, session2)

        val result = useCase(makeParams(fromDate = 1_000L))

        assertEquals(listOf(session1, session2), result)
    }

    @Test
    fun invoke_fromDatePresent_emptyRepository_returnsEmptyList() = runTest {
        coEvery { sessionRepository.getSessionsSince(any(), any()) } returns emptyList()

        val result = useCase(makeParams(fromDate = 1_000L))

        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_fromDatePresent_limitIgnored() = runTest {
        // limit should have no effect when fromDate is provided
        useCase(makeParams(limit = 5, fromDate = 1_000L))

        coVerify(exactly = 0) { sessionRepository.getRecentSessions(any(), any()) }
        coVerify(exactly = 1) { sessionRepository.getSessionsSince(any(), any()) }
    }

    // ── Params data class ─────────────────────────────────────────────────────

    @Test
    fun params_defaultLimit_is30() {
        val params = GetSessionHistoryUseCase.Params(userId = "u")
        assertEquals(30, params.limit)
    }

    @Test
    fun params_defaultFromDate_isNull() {
        val params = GetSessionHistoryUseCase.Params(userId = "u")
        assertNull(params.fromDate)
    }

    @Test
    fun params_creation_storesAllFields() {
        val params = GetSessionHistoryUseCase.Params(
            userId   = "user1",
            limit    = 10,
            fromDate = 999L
        )
        assertEquals("user1", params.userId)
        assertEquals(10,      params.limit)
        assertEquals(999L,    params.fromDate)
    }

    @Test
    fun params_copy_changesOnlySpecifiedField() {
        val original = GetSessionHistoryUseCase.Params("u", 20, 500L)
        val copy     = original.copy(limit = 5)

        assertEquals(5,        copy.limit)
        assertEquals("u",      copy.userId)
        assertEquals(500L,     copy.fromDate)
    }

    @Test
    fun params_equals_twoIdenticalInstancesAreEqual() {
        val a = GetSessionHistoryUseCase.Params("u", 10, 100L)
        val b = GetSessionHistoryUseCase.Params("u", 10, 100L)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun params_equals_differentFromDateNotEqual() {
        val a = GetSessionHistoryUseCase.Params("u", 10, 100L)
        val b = GetSessionHistoryUseCase.Params("u", 10, 200L)

        assertNotEquals(a, b)
    }

    @Test
    fun params_equals_differentLimitNotEqual() {
        val a = GetSessionHistoryUseCase.Params("u", 10, null)
        val b = GetSessionHistoryUseCase.Params("u", 20, null)

        assertNotEquals(a, b)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeParams(
        userId   : String  = "user1",
        limit    : Int     = 30,
        fromDate : Long?   = null
    ) = GetSessionHistoryUseCase.Params(
        userId   = userId,
        limit    = limit,
        fromDate = fromDate
    )
}
