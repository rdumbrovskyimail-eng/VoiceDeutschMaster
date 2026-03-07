// Путь: src/test/java/com/voicedeutsch/master/data/repository/SessionRepositoryImplTest.kt
package com.voicedeutsch.master.data.repository

import com.voicedeutsch.master.data.local.database.dao.ProgressDao
import com.voicedeutsch.master.data.local.database.dao.SessionDao
import com.voicedeutsch.master.data.local.database.entity.DailyStatisticsEntity
import com.voicedeutsch.master.data.local.database.entity.SessionEntity
import com.voicedeutsch.master.data.local.database.entity.SessionEventEntity
import com.voicedeutsch.master.domain.model.session.LearningSession
import com.voicedeutsch.master.domain.model.session.SessionEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SessionRepositoryImplTest {

    private lateinit var sessionDao: SessionDao
    private lateinit var progressDao: ProgressDao
    private lateinit var json: Json
    private lateinit var repository: SessionRepositoryImpl

    private val fixedNow  = 1_700_000_000_000L
    private val fixedUUID = "test-uuid-1"
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeSession(id: String = "session1", userId: String = "user1"): LearningSession =
        mockk<LearningSession>(relaxed = true).also {
            every { it.id }     returns id
            every { it.userId } returns userId
        }

    private fun makeSessionEntity(
        id: String = "session1",
        userId: String = "user1",
        startedAt: Long = 1_000L
    ): SessionEntity = mockk<SessionEntity>(relaxed = true).also {
        every { it.id }        returns id
        every { it.userId }    returns userId
        every { it.startedAt } returns startedAt
        every { it.strategiesUsedJson } returns "[]"
    }

    private fun makeSessionEvent(sessionId: String = "session1"): SessionEvent =
        mockk<SessionEvent>(relaxed = true).also {
            every { it.sessionId } returns sessionId
        }

    private fun makeSessionEventEntity(sessionId: String = "session1"): SessionEventEntity =
        mockk<SessionEventEntity>(relaxed = true).also {
            every { it.sessionId } returns sessionId
        }

    private fun makeDailyStatsEntity(
        id: String = "ds1",
        userId: String = "user1",
        date: String = "2024-03-15",
        sessionsCount: Int = 1,
        totalMinutes: Int = 30,
        wordsLearned: Int = 5,
        streakMaintained: Boolean = true,
        createdAt: Long = fixedNow
    ): DailyStatisticsEntity = mockk<DailyStatisticsEntity>(relaxed = true).also {
        every { it.id }               returns id
        every { it.userId }           returns userId
        every { it.date }             returns date
        every { it.sessionsCount }    returns sessionsCount
        every { it.totalMinutes }     returns totalMinutes
        every { it.wordsLearned }     returns wordsLearned
        every { it.streakMaintained } returns streakMaintained
        every { it.createdAt }        returns createdAt
    }

    private fun today() = LocalDate.now().format(formatter)
    private fun daysAgo(n: Long) = LocalDate.now().minusDays(n).format(formatter)

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        sessionDao  = mockk()
        progressDao = mockk()
        json        = Json { ignoreUnknownKeys = true; coerceInputValues = true }

        repository = SessionRepositoryImpl(sessionDao, progressDao, json)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        mockkStatic("com.voicedeutsch.master.util.UUIDKt")

        every { com.voicedeutsch.master.util.DateUtils.nowTimestamp() } returns fixedNow
        every { com.voicedeutsch.master.util.generateUUID() }           returns fixedUUID

        coEvery { sessionDao.insertSession(any()) }      returns Unit
        coEvery { sessionDao.updateSession(any()) }      returns Unit
        coEvery { sessionDao.insertSessionEvent(any()) } returns Unit
        coEvery { progressDao.upsertDailyStats(any()) }  returns Unit
        coEvery { progressDao.getDailyStats(any(), any()) } returns null
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── createSession ─────────────────────────────────────────────────────────

    @Test
    fun createSession_insertsEntityAndReturnsId() = runTest {
        val session = makeSession(id = "s99")

        val result = repository.createSession(session)

        assertEquals("s99", result)
        coVerify(exactly = 1) { sessionDao.insertSession(any()) }
    }

    @Test
    fun createSession_differentIds_eachReturnedCorrectly() = runTest {
        val s1 = makeSession(id = "abc")
        val s2 = makeSession(id = "xyz")

        assertEquals("abc", repository.createSession(s1))
        assertEquals("xyz", repository.createSession(s2))
    }

    // ── updateSession ─────────────────────────────────────────────────────────

    @Test
    fun updateSession_delegatesToDao() = runTest {
        val session = makeSession()

        repository.updateSession(session)

        coVerify(exactly = 1) { sessionDao.updateSession(any()) }
    }

    // ── getSession ────────────────────────────────────────────────────────────

    @Test
    fun getSession_entityExists_returnsMappedSession() = runTest {
        coEvery { sessionDao.getSession("s1") } returns makeSessionEntity(id = "s1")

        assertNotNull(repository.getSession("s1"))
    }

    @Test
    fun getSession_entityNotFound_returnsNull() = runTest {
        coEvery { sessionDao.getSession("missing") } returns null

        assertNull(repository.getSession("missing"))
    }

    @Test
    fun getSession_passesCorrectId() = runTest {
        coEvery { sessionDao.getSession("s42") } returns null

        repository.getSession("s42")

        coVerify { sessionDao.getSession("s42") }
    }

    // ── getRecentSessions ─────────────────────────────────────────────────────

    @Test
    fun getRecentSessions_mapsAllEntities() = runTest {
        coEvery { sessionDao.getRecentSessions("user1", 5) } returns
            listOf(makeSessionEntity("s1"), makeSessionEntity("s2"))

        val result = repository.getRecentSessions("user1", 5)

        assertEquals(2, result.size)
    }

    @Test
    fun getRecentSessions_emptyDao_returnsEmptyList() = runTest {
        coEvery { sessionDao.getRecentSessions(any(), any()) } returns emptyList()

        assertTrue(repository.getRecentSessions("user1", 10).isEmpty())
    }

    @Test
    fun getRecentSessions_passesLimitCorrectly() = runTest {
        coEvery { sessionDao.getRecentSessions("user1", 20) } returns emptyList()

        repository.getRecentSessions("user1", 20)

        coVerify { sessionDao.getRecentSessions("user1", 20) }
    }

    // ── getSessionsFlow ───────────────────────────────────────────────────────

    @Test
    fun getSessionsFlow_emitsFromDao() = runTest {
        every { sessionDao.getSessionsFlow("user1") } returns
            flowOf(listOf(makeSessionEntity()))

        val result = repository.getSessionsFlow("user1").first()

        assertEquals(1, result.size)
    }

    @Test
    fun getSessionsFlow_emptyFlow_emitsEmptyList() = runTest {
        every { sessionDao.getSessionsFlow("user1") } returns flowOf(emptyList())

        val result = repository.getSessionsFlow("user1").first()

        assertTrue(result.isEmpty())
    }

    // ── getSessionCount / getTotalMinutes ─────────────────────────────────────

    @Test
    fun getSessionCount_delegatesCorrectly() = runTest {
        coEvery { sessionDao.getSessionCount("user1") } returns 42

        assertEquals(42, repository.getSessionCount("user1"))
    }

    @Test
    fun getTotalMinutes_delegatesCorrectly() = runTest {
        coEvery { sessionDao.getTotalMinutes("user1") } returns 300

        assertEquals(300, repository.getTotalMinutes("user1"))
    }

    // ── getSessionsSince ──────────────────────────────────────────────────────

    @Test
    fun getSessionsSince_filtersSessionsBeforeTimestamp() = runTest {
        coEvery { sessionDao.getRecentSessions("user1", Int.MAX_VALUE) } returns listOf(
            makeSessionEntity("s1", startedAt = 500L),
            makeSessionEntity("s2", startedAt = 1000L),
            makeSessionEntity("s3", startedAt = 2000L)
        )

        val result = repository.getSessionsSince("user1", 1000L)

        assertEquals(2, result.size)
    }

    @Test
    fun getSessionsSince_noSessionsAfterTimestamp_returnsEmpty() = runTest {
        coEvery { sessionDao.getRecentSessions("user1", Int.MAX_VALUE) } returns listOf(
            makeSessionEntity("s1", startedAt = 100L)
        )

        val result = repository.getSessionsSince("user1", 500L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun getSessionsSince_allSessionsAfterTimestamp_returnsAll() = runTest {
        coEvery { sessionDao.getRecentSessions("user1", Int.MAX_VALUE) } returns listOf(
            makeSessionEntity("s1", startedAt = 1000L),
            makeSessionEntity("s2", startedAt = 2000L)
        )

        val result = repository.getSessionsSince("user1", 500L)

        assertEquals(2, result.size)
    }

    @Test
    fun getSessionsSince_exactTimestampMatch_included() = runTest {
        coEvery { sessionDao.getRecentSessions("user1", Int.MAX_VALUE) } returns listOf(
            makeSessionEntity("s1", startedAt = 1000L)
        )

        val result = repository.getSessionsSince("user1", 1000L)

        assertEquals(1, result.size)
    }

    @Test
    fun getSessionsSince_callsDaoWithMaxValue() = runTest {
        coEvery { sessionDao.getRecentSessions("user1", Int.MAX_VALUE) } returns emptyList()

        repository.getSessionsSince("user1", 0L)

        coVerify { sessionDao.getRecentSessions("user1", Int.MAX_VALUE) }
    }

    // ── addSessionEvent ───────────────────────────────────────────────────────

    @Test
    fun addSessionEvent_delegatesToDao() = runTest {
        val event = makeSessionEvent()

        repository.addSessionEvent(event)

        coVerify(exactly = 1) { sessionDao.insertSessionEvent(any()) }
    }

    // ── getSessionEvents ──────────────────────────────────────────────────────

    @Test
    fun getSessionEvents_mapsAllEntities() = runTest {
        coEvery { sessionDao.getSessionEvents("s1") } returns listOf(
            makeSessionEventEntity("s1"),
            makeSessionEventEntity("s1")
        )

        val result = repository.getSessionEvents("s1")

        assertEquals(2, result.size)
    }

    @Test
    fun getSessionEvents_emptyDao_returnsEmptyList() = runTest {
        coEvery { sessionDao.getSessionEvents(any()) } returns emptyList()

        assertTrue(repository.getSessionEvents("s1").isEmpty())
    }

    @Test
    fun getSessionEvents_passesCorrectSessionId() = runTest {
        coEvery { sessionDao.getSessionEvents("s99") } returns emptyList()

        repository.getSessionEvents("s99")

        coVerify { sessionDao.getSessionEvents("s99") }
    }

    // ── upsertDailyStatistics — new record ────────────────────────────────────

    @Test
    fun upsertDailyStatistics_noExisting_usesGeneratedUUID() = runTest {
        coEvery { progressDao.getDailyStats("user1", "2024-03-15") } returns null
        val slot = slot<DailyStatisticsEntity>()
        coEvery { progressDao.upsertDailyStats(capture(slot)) } returns Unit

        repository.upsertDailyStatistics(
            "user1", "2024-03-15", 1, 30, 5, 3, 10, 8, 0.8f, true
        )

        assertEquals(fixedUUID, slot.captured.id)
    }

    @Test
    fun upsertDailyStatistics_noExisting_createdAtIsNow() = runTest {
        coEvery { progressDao.getDailyStats("user1", "2024-03-15") } returns null
        val slot = slot<DailyStatisticsEntity>()
        coEvery { progressDao.upsertDailyStats(capture(slot)) } returns Unit

        repository.upsertDailyStatistics(
            "user1", "2024-03-15", 1, 30, 5, 3, 10, 8, 0.8f, true
        )

        assertEquals(fixedNow, slot.captured.createdAt)
    }

    @Test
    fun upsertDailyStatistics_noExisting_allFieldsMappedCorrectly() = runTest {
        coEvery { progressDao.getDailyStats("user1", "2024-03-15") } returns null
        val slot = slot<DailyStatisticsEntity>()
        coEvery { progressDao.upsertDailyStats(capture(slot)) } returns Unit

        repository.upsertDailyStatistics(
            userId             = "user1",
            date               = "2024-03-15",
            sessionsCount      = 2,
            totalMinutes       = 45,
            wordsLearned       = 8,
            wordsReviewed      = 5,
            exercisesCompleted = 12,
            exercisesCorrect   = 10,
            averageScore       = 0.85f,
            streakMaintained   = true
        )

        assertEquals("user1",   slot.captured.userId)
        assertEquals("2024-03-15", slot.captured.date)
        assertEquals(2,          slot.captured.sessionsCount)
        assertEquals(45,         slot.captured.totalMinutes)
        assertEquals(8,          slot.captured.wordsLearned)
        assertEquals(5,          slot.captured.wordsReviewed)
        assertEquals(12,         slot.captured.exercisesCompleted)
        assertEquals(10,         slot.captured.exercisesCorrect)
        assertEquals(0.85f,      slot.captured.averageScore, 0.001f)
        assertTrue(slot.captured.streakMaintained)
    }

    // ── upsertDailyStatistics — existing record ───────────────────────────────

    @Test
    fun upsertDailyStatistics_existing_preservesOriginalId() = runTest {
        val existing = makeDailyStatsEntity(id = "original-id", createdAt = 500L)
        coEvery { progressDao.getDailyStats("user1", "2024-03-15") } returns existing
        val slot = slot<DailyStatisticsEntity>()
        coEvery { progressDao.upsertDailyStats(capture(slot)) } returns Unit

        repository.upsertDailyStatistics(
            "user1", "2024-03-15", 1, 30, 5, 3, 10, 8, 0.8f, true
        )

        assertEquals("original-id", slot.captured.id)
    }

    @Test
    fun upsertDailyStatistics_existing_preservesOriginalCreatedAt() = runTest {
        val existing = makeDailyStatsEntity(id = "orig-id", createdAt = 12345L)
        coEvery { progressDao.getDailyStats("user1", "2024-03-15") } returns existing
        val slot = slot<DailyStatisticsEntity>()
        coEvery { progressDao.upsertDailyStats(capture(slot)) } returns Unit

        repository.upsertDailyStatistics(
            "user1", "2024-03-15", 3, 60, 10, 5, 20, 18, 0.9f, false
        )

        assertEquals(12345L, slot.captured.createdAt)
    }

    @Test
    fun upsertDailyStatistics_existing_updatesOtherFields() = runTest {
        val existing = makeDailyStatsEntity(id = "orig-id", sessionsCount = 1, totalMinutes = 10)
        coEvery { progressDao.getDailyStats("user1", "2024-03-15") } returns existing
        val slot = slot<DailyStatisticsEntity>()
        coEvery { progressDao.upsertDailyStats(capture(slot)) } returns Unit

        repository.upsertDailyStatistics(
            "user1", "2024-03-15", 5, 90, 20, 15, 40, 35, 0.75f, true
        )

        assertEquals(5,   slot.captured.sessionsCount)
        assertEquals(90,  slot.captured.totalMinutes)
        assertEquals(20,  slot.captured.wordsLearned)
    }

    @Test
    fun upsertDailyStatistics_upsertCalledOnce() = runTest {
        repository.upsertDailyStatistics(
            "user1", "2024-03-15", 1, 30, 5, 3, 10, 8, 0.8f, true
        )

        coVerify(exactly = 1) { progressDao.upsertDailyStats(any()) }
    }

    // ── getDailyStatistics ────────────────────────────────────────────────────

    @Test
    fun getDailyStatistics_entityExists_returnsMapped() = runTest {
        coEvery { progressDao.getDailyStats("user1", "2024-03-15") } returns
            makeDailyStatsEntity()

        assertNotNull(repository.getDailyStatistics("user1", "2024-03-15"))
    }

    @Test
    fun getDailyStatistics_entityNotFound_returnsNull() = runTest {
        coEvery { progressDao.getDailyStats("user1", "2024-03-15") } returns null

        assertNull(repository.getDailyStatistics("user1", "2024-03-15"))
    }

    @Test
    fun getDailyStatistics_passesCorrectArguments() = runTest {
        coEvery { progressDao.getDailyStats("user42", "2024-01-01") } returns null

        repository.getDailyStatistics("user42", "2024-01-01")

        coVerify { progressDao.getDailyStats("user42", "2024-01-01") }
    }

    // ── getDailyStatisticsRange ───────────────────────────────────────────────

    @Test
    fun getDailyStatisticsRange_mapsAllEntities() = runTest {
        coEvery { progressDao.getDailyStatsRange("user1", "2024-03-01", "2024-03-15") } returns
            listOf(makeDailyStatsEntity(), makeDailyStatsEntity())

        val result = repository.getDailyStatisticsRange("user1", "2024-03-01", "2024-03-15")

        assertEquals(2, result.size)
    }

    @Test
    fun getDailyStatisticsRange_emptyResult_returnsEmptyList() = runTest {
        coEvery { progressDao.getDailyStatsRange(any(), any(), any()) } returns emptyList()

        assertTrue(repository.getDailyStatisticsRange("user1", "2024-01-01", "2024-01-31").isEmpty())
    }

    // ── calculateStreak ───────────────────────────────────────────────────────

    @Test
    fun calculateStreak_noStats_returns0() = runTest {
        coEvery { progressDao.getRecentDailyStats("user1", 365) } returns emptyList()

        assertEquals(0, repository.calculateStreak("user1"))
    }

    @Test
    fun calculateStreak_todayMaintained_streakIs1() = runTest {
        coEvery { progressDao.getRecentDailyStats("user1", 365) } returns listOf(
            makeDailyStatsEntity(date = today(), streakMaintained = true)
        )

        assertEquals(1, repository.calculateStreak("user1"))
    }

    @Test
    fun calculateStreak_todayNotMaintained_checkYesterdayInstead() = runTest {
        coEvery { progressDao.getRecentDailyStats("user1", 365) } returns listOf(
            makeDailyStatsEntity(date = today(),        streakMaintained = false),
            makeDailyStatsEntity(date = daysAgo(1), streakMaintained = true)
        )

        assertEquals(1, repository.calculateStreak("user1"))
    }

    @Test
    fun calculateStreak_todayNotPresent_checkYesterdayInstead() = runTest {
        coEvery { progressDao.getRecentDailyStats("user1", 365) } returns listOf(
            makeDailyStatsEntity(date = daysAgo(1), streakMaintained = true),
            makeDailyStatsEntity(date = daysAgo(2), streakMaintained = true)
        )

        assertEquals(2, repository.calculateStreak("user1"))
    }

    @Test
    fun calculateStreak_3ConsecutiveDaysIncludingToday_returns3() = runTest {
        coEvery { progressDao.getRecentDailyStats("user1", 365) } returns listOf(
            makeDailyStatsEntity(date = today(),        streakMaintained = true),
            makeDailyStatsEntity(date = daysAgo(1), streakMaintained = true),
            makeDailyStatsEntity(date = daysAgo(2), streakMaintained = true)
        )

        assertEquals(3, repository.calculateStreak("user1"))
    }

    @Test
    fun calculateStreak_gapInDays_streakBreaks() = runTest {
        coEvery { progressDao.getRecentDailyStats("user1", 365) } returns listOf(
            makeDailyStatsEntity(date = today(),        streakMaintained = true),
            makeDailyStatsEntity(date = daysAgo(1), streakMaintained = true),
            // gap: daysAgo(2) missing
            makeDailyStatsEntity(date = daysAgo(3), streakMaintained = true)
        )

        assertEquals(2, repository.calculateStreak("user1"))
    }

    @Test
    fun calculateStreak_streakNotMaintainedOnConsecutiveDay_breaks() = runTest {
        coEvery { progressDao.getRecentDailyStats("user1", 365) } returns listOf(
            makeDailyStatsEntity(date = today(),        streakMaintained = true),
            makeDailyStatsEntity(date = daysAgo(1), streakMaintained = false),  // not maintained
            makeDailyStatsEntity(date = daysAgo(2), streakMaintained = true)
        )

        assertEquals(1, repository.calculateStreak("user1"))
    }

    @Test
    fun calculateStreak_5ConsecutiveDays_returns5() = runTest {
        coEvery { progressDao.getRecentDailyStats("user1", 365) } returns
            (0L..4L).map { offset ->
                makeDailyStatsEntity(date = daysAgo(offset), streakMaintained = true)
            }

        assertEquals(5, repository.calculateStreak("user1"))
    }

    @Test
    fun calculateStreak_invalidDateFormat_skipped() = runTest {
        coEvery { progressDao.getRecentDailyStats("user1", 365) } returns listOf(
            makeDailyStatsEntity(date = today(),     streakMaintained = true),
            makeDailyStatsEntity(date = "INVALID",   streakMaintained = true), // skipped
            makeDailyStatsEntity(date = daysAgo(1), streakMaintained = true)
        )

        // "INVALID" is skipped; streak from today + daysAgo(1) = 2
        assertEquals(2, repository.calculateStreak("user1"))
    }

    @Test
    fun calculateStreak_statsOnlyFarInPast_returns0() = runTest {
        coEvery { progressDao.getRecentDailyStats("user1", 365) } returns listOf(
            makeDailyStatsEntity(date = daysAgo(30), streakMaintained = true)
        )
        // No today, no yesterday → expectedDate = yesterday → daysAgo(30) < yesterday → breaks immediately
        assertEquals(0, repository.calculateStreak("user1"))
    }

    @Test
    fun calculateStreak_callsDaoWith365Limit() = runTest {
        coEvery { progressDao.getRecentDailyStats("user1", 365) } returns emptyList()

        repository.calculateStreak("user1")

        coVerify { progressDao.getRecentDailyStats("user1", 365) }
    }

    @Test
    fun calculateStreak_sortedByDateDescending_correctOrder() = runTest {
        // Provide stats out of order; should still calculate streak correctly
        coEvery { progressDao.getRecentDailyStats("user1", 365) } returns listOf(
            makeDailyStatsEntity(date = daysAgo(2), streakMaintained = true),
            makeDailyStatsEntity(date = today(),        streakMaintained = true),
            makeDailyStatsEntity(date = daysAgo(1), streakMaintained = true)
        )

        assertEquals(3, repository.calculateStreak("user1"))
    }

    @Test
    fun calculateStreak_todayMaintainedFalseAndYesterdayMissing_returns0() = runTest {
        coEvery { progressDao.getRecentDailyStats("user1", 365) } returns listOf(
            makeDailyStatsEntity(date = today(), streakMaintained = false)
        )
        // No yesterday entry → streak loop finds nothing → 0
        assertEquals(0, repository.calculateStreak("user1"))
    }
}
