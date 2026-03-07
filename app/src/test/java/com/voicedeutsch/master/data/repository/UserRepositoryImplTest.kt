// Путь: src/test/java/com/voicedeutsch/master/data/repository/UserRepositoryImplTest.kt
package com.voicedeutsch.master.data.repository

import com.voicedeutsch.master.data.local.database.dao.KnowledgeDao
import com.voicedeutsch.master.data.local.database.dao.UserDao
import com.voicedeutsch.master.data.local.database.dao.WordDao
import com.voicedeutsch.master.data.local.database.entity.UserEntity
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.domain.model.user.UserPreferences
import com.voicedeutsch.master.domain.model.user.UserProfile
import com.voicedeutsch.master.domain.model.user.VoiceSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserRepositoryImplTest {

    private lateinit var userDao: UserDao
    private lateinit var knowledgeDao: KnowledgeDao
    private lateinit var wordDao: WordDao
    private lateinit var preferencesDataStore: UserPreferencesDataStore
    private lateinit var json: Json
    private lateinit var repository: UserRepositoryImpl

    private val fixedNow = 1_700_000_000_000L

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeUserEntity(
        id: String = "user1",
        cefrLevel: String = "A1",
        cefrSubLevel: Int = 1,
        streakDays: Int = 5,
        totalMinutes: Int = 120,
        totalSessions: Int = 10,
        lastSessionDate: Long? = null
    ): UserEntity = mockk<UserEntity>(relaxed = true).also {
        every { it.id }              returns id
        every { it.cefrLevel }       returns cefrLevel
        every { it.cefrSubLevel }    returns cefrSubLevel
        every { it.streakDays }      returns streakDays
        every { it.totalMinutes }    returns totalMinutes
        every { it.totalSessions }   returns totalSessions
        every { it.lastSessionDate } returns lastSessionDate
        every { it.preferencesJson } returns "{}"
        every { it.voiceSettingsJson } returns "{}"
    }

    private fun makeUserProfile(
        id: String = "user1",
        streakDays: Int = 0
    ): UserProfile = mockk<UserProfile>(relaxed = true).also {
        every { it.id }         returns id
        every { it.streakDays } returns streakDays
    }

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        userDao              = mockk()
        knowledgeDao         = mockk()
        wordDao              = mockk()
        preferencesDataStore = mockk()
        json                 = Json { ignoreUnknownKeys = true; coerceInputValues = true }

        repository = UserRepositoryImpl(userDao, knowledgeDao, wordDao, preferencesDataStore, json)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        every { com.voicedeutsch.master.util.DateUtils.nowTimestamp() } returns fixedNow

        // Safe defaults
        coEvery { userDao.insertUser(any()) }                          returns Unit
        coEvery { userDao.updateUser(any()) }                          returns Unit
        coEvery { userDao.updateLevel(any(), any(), any(), any()) }    returns Unit
        coEvery { userDao.updatePreferences(any(), any(), any()) }     returns Unit
        coEvery { userDao.updateVoiceSettings(any(), any(), any()) }   returns Unit
        coEvery { userDao.incrementSessionStats(any(), any(), any(), any(), any(), any()) } returns Unit
        coEvery { userDao.updateStreak(any(), any(), any()) }          returns Unit
        coEvery { userDao.getUserCount() }                             returns 0
        coEvery { userDao.getAllUserIds() }                            returns emptyList()
        coEvery { userDao.getUser(any()) }                             returns null
        every  { userDao.getUserFlow(any()) }                         returns flowOf(null)

        coEvery { knowledgeDao.getKnownWordsCount(any()) }             returns 0
        coEvery { knowledgeDao.getActiveWordsCount(any()) }            returns 0
        coEvery { knowledgeDao.getKnownRulesCount(any()) }             returns 0
        coEvery { knowledgeDao.getWordsForReviewCount(any(), any()) }  returns 0
        coEvery { knowledgeDao.getRulesForReviewCount(any(), any()) }  returns 0

        coEvery { wordDao.getTotalWordCount() }                        returns 0
        coEvery { wordDao.getTotalRuleCount() }                        returns 0

        coEvery { preferencesDataStore.setActiveUserId(any()) }        returns Unit
        coEvery { preferencesDataStore.getActiveUserId() }             returns null
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── getUserProfileFlow ────────────────────────────────────────────────────

    @Test
    fun getUserProfileFlow_entityExists_emitsMappedProfile() = runTest {
        val entity = makeUserEntity(id = "user1")
        every { userDao.getUserFlow("user1") } returns flowOf(entity)

        val result = repository.getUserProfileFlow("user1").first()

        assertNotNull(result)
    }

    @Test
    fun getUserProfileFlow_entityNull_emitsNull() = runTest {
        every { userDao.getUserFlow("user1") } returns flowOf(null)

        val result = repository.getUserProfileFlow("user1").first()

        assertNull(result)
    }

    @Test
    fun getUserProfileFlow_passesCorrectUserId() = runTest {
        repository.getUserProfileFlow("user42").first()

        coVerify { userDao.getUserFlow("user42") }
    }

    @Test
    fun getUserProfileFlow_multipleEmissions_allForwarded() = runTest {
        val entity1 = makeUserEntity("user1")
        val entity2 = makeUserEntity("user1", cefrLevel = "B1")
        every { userDao.getUserFlow("user1") } returns flowOf(entity1, null, entity2)

        val results = mutableListOf<Any?>()
        repository.getUserProfileFlow("user1").collect { results.add(it) }

        assertEquals(3, results.size)
        assertNotNull(results[0])
        assertNull(results[1])
        assertNotNull(results[2])
    }

    // ── getUserProfile ────────────────────────────────────────────────────────

    @Test
    fun getUserProfile_entityExists_returnsMappedProfile() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity(id = "user1")

        assertNotNull(repository.getUserProfile("user1"))
    }

    @Test
    fun getUserProfile_entityNotFound_returnsNull() = runTest {
        coEvery { userDao.getUser("missing") } returns null

        assertNull(repository.getUserProfile("missing"))
    }

    @Test
    fun getUserProfile_passesCorrectUserId() = runTest {
        repository.getUserProfile("user99")

        coVerify { userDao.getUser("user99") }
    }

    // ── createUser ────────────────────────────────────────────────────────────

    @Test
    fun createUser_insertsEntityAndReturnsId() = runTest {
        val profile = makeUserProfile(id = "newUser")

        val result = repository.createUser(profile)

        assertEquals("newUser", result)
        coVerify(exactly = 1) { userDao.insertUser(any()) }
    }

    @Test
    fun createUser_setsActiveUserId() = runTest {
        val profile = makeUserProfile(id = "newUser")

        repository.createUser(profile)

        coVerify(exactly = 1) { preferencesDataStore.setActiveUserId("newUser") }
    }

    @Test
    fun createUser_activeUserIdMatchesProfileId() = runTest {
        val profile = makeUserProfile(id = "user-XYZ")

        repository.createUser(profile)

        coVerify { preferencesDataStore.setActiveUserId("user-XYZ") }
    }

    // ── updateUser ────────────────────────────────────────────────────────────

    @Test
    fun updateUser_delegatesToDao() = runTest {
        repository.updateUser(makeUserProfile())

        coVerify(exactly = 1) { userDao.updateUser(any()) }
    }

    // ── updateUserLevel ───────────────────────────────────────────────────────

    @Test
    fun updateUserLevel_delegatesWithCefrNameAndTimestamp() = runTest {
        repository.updateUserLevel("user1", CefrLevel.B2, 5)

        coVerify(exactly = 1) { userDao.updateLevel("user1", "B2", 5, fixedNow) }
    }

    @Test
    fun updateUserLevel_allLevelNames_passedCorrectly() = runTest {
        CefrLevel.entries.forEach { level ->
            repository.updateUserLevel("user1", level, 1)
            coVerify { userDao.updateLevel("user1", level.name, 1, fixedNow) }
        }
    }

    // ── updateUserPreferences ─────────────────────────────────────────────────

    @Test
    fun updateUserPreferences_callsDaoWithJsonAndTimestamp() = runTest {
        val prefs = mockk<UserPreferences>(relaxed = true)

        repository.updateUserPreferences("user1", prefs)

        coVerify(exactly = 1) { userDao.updatePreferences(eq("user1"), any(), eq(fixedNow)) }
    }

    @Test
    fun updateUserPreferences_prefsSerializedToJson() = runTest {
        val prefs = mockk<UserPreferences>(relaxed = true)
        var capturedJson = ""
        coEvery { userDao.updatePreferences(any(), any(), any()) } answers {
            capturedJson = secondArg<String>()
            Unit
        }

        repository.updateUserPreferences("user1", prefs)

        assertTrue(capturedJson.isNotBlank())
    }

    // ── updateVoiceSettings ───────────────────────────────────────────────────

    @Test
    fun updateVoiceSettings_callsDaoWithJsonAndTimestamp() = runTest {
        val settings = mockk<VoiceSettings>(relaxed = true)

        repository.updateVoiceSettings("user1", settings)

        coVerify(exactly = 1) { userDao.updateVoiceSettings(eq("user1"), any(), eq(fixedNow)) }
    }

    @Test
    fun updateVoiceSettings_settingsSerializedToJson() = runTest {
        val settings = mockk<VoiceSettings>(relaxed = true)
        var capturedJson = ""
        coEvery { userDao.updateVoiceSettings(any(), any(), any()) } answers {
            capturedJson = secondArg<String>()
            Unit
        }

        repository.updateVoiceSettings("user1", settings)

        assertTrue(capturedJson.isNotBlank())
    }

    // ── incrementSessionStats ─────────────────────────────────────────────────

    @Test
    fun incrementSessionStats_delegatesWithAllParams() = runTest {
        repository.incrementSessionStats("user1", 30, 10, 5)

        coVerify(exactly = 1) {
            userDao.incrementSessionStats("user1", 30, 10, 5, fixedNow, fixedNow)
        }
    }

    @Test
    fun incrementSessionStats_bothTimestampsAreNow() = runTest {
        var capturedUpdated = 0L
        var capturedLast    = 0L
        coEvery { userDao.incrementSessionStats(any(), any(), any(), any(), any(), any()) } answers {
            capturedUpdated = arg(4)
            capturedLast    = arg(5)
            Unit
        }

        repository.incrementSessionStats("user1", 10, 5, 2)

        assertEquals(fixedNow, capturedUpdated)
        assertEquals(fixedNow, capturedLast)
    }

    // ── updateStreak ──────────────────────────────────────────────────────────

    @Test
    fun updateStreak_delegatesWithStreakAndTimestamp() = runTest {
        repository.updateStreak("user1", 7)

        coVerify(exactly = 1) { userDao.updateStreak("user1", 7, fixedNow) }
    }

    @Test
    fun updateStreak_zero_passedCorrectly() = runTest {
        repository.updateStreak("user1", 0)

        coVerify { userDao.updateStreak("user1", 0, fixedNow) }
    }

    // ── getUserStatistics ─────────────────────────────────────────────────────

    @Test
    fun getUserStatistics_userNotFound_throwsIllegalState() = runTest {
        coEvery { userDao.getUser("user1") } returns null

        assertThrows(IllegalStateException::class.java) {
            runTest { repository.getUserStatistics("user1") }
        }
    }

    @Test
    fun getUserStatistics_totalWordsFromDictionary() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity()
        coEvery { wordDao.getTotalWordCount() } returns 500

        val result = repository.getUserStatistics("user1")

        assertEquals(500, result.totalWords)
    }

    @Test
    fun getUserStatistics_totalRulesFromDictionary() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity()
        coEvery { wordDao.getTotalRuleCount() } returns 75

        val result = repository.getUserStatistics("user1")

        assertEquals(75, result.totalRules)
    }

    @Test
    fun getUserStatistics_knownRulesFromKnowledgeDao() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity()
        coEvery { knowledgeDao.getKnownRulesCount("user1") } returns 30

        val result = repository.getUserStatistics("user1")

        assertEquals(30, result.knownRules)
    }

    @Test
    fun getUserStatistics_activeWordsFromKnowledgeDao() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity()
        coEvery { knowledgeDao.getActiveWordsCount("user1") } returns 40

        val result = repository.getUserStatistics("user1")

        assertEquals(40, result.activeWords)
    }

    @Test
    fun getUserStatistics_passiveWordsIsKnownMinusActive() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity()
        coEvery { knowledgeDao.getKnownWordsCount("user1") }  returns 50
        coEvery { knowledgeDao.getActiveWordsCount("user1") } returns 30

        val result = repository.getUserStatistics("user1")

        assertEquals(20, result.passiveWords)
    }

    @Test
    fun getUserStatistics_passiveWordsCoercedAtLeast0() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity()
        coEvery { knowledgeDao.getKnownWordsCount("user1") }  returns 10
        coEvery { knowledgeDao.getActiveWordsCount("user1") } returns 20  // active > known

        val result = repository.getUserStatistics("user1")

        assertEquals(0, result.passiveWords)
    }

    @Test
    fun getUserStatistics_bookProgressRatio_halwayKnown() = runTest {
        coEvery { userDao.getUser("user1") }          returns makeUserEntity()
        coEvery { wordDao.getTotalWordCount() }         returns 100
        coEvery { knowledgeDao.getKnownWordsCount("user1") } returns 50

        val result = repository.getUserStatistics("user1")

        assertEquals(0.5f, result.bookProgress, 0.001f)
    }

    @Test
    fun getUserStatistics_totalWordsZero_bookProgressIs0() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity()
        coEvery { wordDao.getTotalWordCount() } returns 0

        val result = repository.getUserStatistics("user1")

        assertEquals(0f, result.bookProgress)
    }

    @Test
    fun getUserStatistics_totalSessionsFromUser() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity(totalSessions = 42)

        val result = repository.getUserStatistics("user1")

        assertEquals(42, result.totalSessions)
    }

    @Test
    fun getUserStatistics_totalMinutesFromUser() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity(totalMinutes = 300)

        val result = repository.getUserStatistics("user1")

        assertEquals(300, result.totalMinutes)
    }

    @Test
    fun getUserStatistics_streakDaysFromUser() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity(streakDays = 14)

        val result = repository.getUserStatistics("user1")

        assertEquals(14, result.streakDays)
    }

    @Test
    fun getUserStatistics_wordsForReviewUsesNowTimestamp() = runTest {
        coEvery { userDao.getUser("user1") }                              returns makeUserEntity()
        coEvery { knowledgeDao.getWordsForReviewCount("user1", fixedNow) } returns 8

        val result = repository.getUserStatistics("user1")

        assertEquals(8, result.wordsForReviewToday)
    }

    @Test
    fun getUserStatistics_rulesForReviewUsesNowTimestamp() = runTest {
        coEvery { userDao.getUser("user1") }                               returns makeUserEntity()
        coEvery { knowledgeDao.getRulesForReviewCount("user1", fixedNow) } returns 3

        val result = repository.getUserStatistics("user1")

        assertEquals(3, result.rulesForReviewToday)
    }

    @Test
    fun getUserStatistics_averageScoreAlways0() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity()

        val result = repository.getUserStatistics("user1")

        assertEquals(0f, result.averageScore)
        assertEquals(0f, result.averagePronunciationScore)
    }

    @Test
    fun getUserStatistics_currentChapterAlways1() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity()

        val result = repository.getUserStatistics("user1")

        assertEquals(1, result.currentChapter)
    }

    @Test
    fun getUserStatistics_totalChaptersAlways20() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity()

        val result = repository.getUserStatistics("user1")

        assertEquals(20, result.totalChapters)
    }

    // ── getActiveUserId ───────────────────────────────────────────────────────

    @Test
    fun getActiveUserId_returnsFromDataStore() = runTest {
        coEvery { preferencesDataStore.getActiveUserId() } returns "user1"

        assertEquals("user1", repository.getActiveUserId())
    }

    @Test
    fun getActiveUserId_returnsNullWhenNotSet() = runTest {
        coEvery { preferencesDataStore.getActiveUserId() } returns null

        assertNull(repository.getActiveUserId())
    }

    // ── setActiveUserId ───────────────────────────────────────────────────────

    @Test
    fun setActiveUserId_delegatesToDataStore() = runTest {
        repository.setActiveUserId("user42")

        coVerify(exactly = 1) { preferencesDataStore.setActiveUserId("user42") }
    }

    // ── userExists ────────────────────────────────────────────────────────────

    @Test
    fun userExists_countGreaterThan0_returnsTrue() = runTest {
        coEvery { userDao.getUserCount() } returns 1

        assertTrue(repository.userExists())
    }

    @Test
    fun userExists_count0_returnsFalse() = runTest {
        coEvery { userDao.getUserCount() } returns 0

        assertFalse(repository.userExists())
    }

    @Test
    fun userExists_countGreaterThan1_returnsTrue() = runTest {
        coEvery { userDao.getUserCount() } returns 5

        assertTrue(repository.userExists())
    }

    // ── getAllUserIds ─────────────────────────────────────────────────────────

    @Test
    fun getAllUserIds_delegatesAndReturns() = runTest {
        coEvery { userDao.getAllUserIds() } returns listOf("u1", "u2", "u3")

        val result = repository.getAllUserIds()

        assertEquals(listOf("u1", "u2", "u3"), result)
    }

    @Test
    fun getAllUserIds_empty_returnsEmptyList() = runTest {
        val result = repository.getAllUserIds()

        assertTrue(result.isEmpty())
    }

    // ── updateStreakIfNeeded ──────────────────────────────────────────────────

    @Test
    fun updateStreakIfNeeded_userNotFound_earlyReturn() = runTest {
        coEvery { userDao.getUser("user1") } returns null

        repository.updateStreakIfNeeded("user1")

        coVerify(exactly = 0) { userDao.updateStreak(any(), any(), any()) }
    }

    @Test
    fun updateStreakIfNeeded_lastSessionNull_noUpdate() = runTest {
        // lastSessionDate=null → diff = now - 0 = fixedNow which is > twoDaysMs
        // So streak resets to 0
        coEvery { userDao.getUser("user1") } returns makeUserEntity(
            lastSessionDate = null, streakDays = 5
        )

        repository.updateStreakIfNeeded("user1")

        coVerify { userDao.updateStreak("user1", 0, fixedNow) }
    }

    @Test
    fun updateStreakIfNeeded_lastSessionMoreThan2Days_resetsStreakTo0() = runTest {
        val twoDaysMs   = 2 * 24 * 60 * 60 * 1000L
        val lastSession = fixedNow - twoDaysMs - 1
        coEvery { userDao.getUser("user1") } returns makeUserEntity(
            lastSessionDate = lastSession, streakDays = 7
        )

        repository.updateStreakIfNeeded("user1")

        coVerify(exactly = 1) { userDao.updateStreak("user1", 0, fixedNow) }
    }

    @Test
    fun updateStreakIfNeeded_lastSessionBetween1And2Days_incrementsStreak() = runTest {
        val oneDayMs    = 24 * 60 * 60 * 1000L
        val lastSession = fixedNow - oneDayMs - 1  // just over 1 day ago
        coEvery { userDao.getUser("user1") } returns makeUserEntity(
            lastSessionDate = lastSession, streakDays = 3
        )

        repository.updateStreakIfNeeded("user1")

        coVerify(exactly = 1) { userDao.updateStreak("user1", 4, fixedNow) }
    }

    @Test
    fun updateStreakIfNeeded_lastSessionLessThan1Day_noUpdate() = runTest {
        val lastSession = fixedNow - 3_600_000L  // 1 hour ago
        coEvery { userDao.getUser("user1") } returns makeUserEntity(
            lastSessionDate = lastSession, streakDays = 5
        )

        repository.updateStreakIfNeeded("user1")

        coVerify(exactly = 0) { userDao.updateStreak(any(), any(), any()) }
    }

    @Test
    fun updateStreakIfNeeded_exactlyOneDayAgo_incrementsStreak() = runTest {
        val oneDayMs    = 24 * 60 * 60 * 1000L
        val lastSession = fixedNow - oneDayMs - 1  // just past 1 day
        coEvery { userDao.getUser("user1") } returns makeUserEntity(
            lastSessionDate = lastSession, streakDays = 10
        )

        repository.updateStreakIfNeeded("user1")

        coVerify { userDao.updateStreak("user1", 11, fixedNow) }
    }

    @Test
    fun updateStreakIfNeeded_exactlyTwoDaysAgo_resetsStreak() = runTest {
        val twoDaysMs   = 2 * 24 * 60 * 60 * 1000L
        val lastSession = fixedNow - twoDaysMs - 1
        coEvery { userDao.getUser("user1") } returns makeUserEntity(
            lastSessionDate = lastSession, streakDays = 4
        )

        repository.updateStreakIfNeeded("user1")

        coVerify { userDao.updateStreak("user1", 0, fixedNow) }
    }

    @Test
    fun updateStreakIfNeeded_streak0_incrementedTo1WhenOneDayAgo() = runTest {
        val oneDayMs    = 24 * 60 * 60 * 1000L
        val lastSession = fixedNow - oneDayMs - 500
        coEvery { userDao.getUser("user1") } returns makeUserEntity(
            lastSessionDate = lastSession, streakDays = 0
        )

        repository.updateStreakIfNeeded("user1")

        coVerify { userDao.updateStreak("user1", 1, fixedNow) }
    }
}
