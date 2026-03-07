// Путь: src/androidTest/java/com/voicedeutsch/master/data/local/database/dao/ProgressDaoTest.kt
package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.voicedeutsch.master.data.local.database.VoiceDeutschDatabase
import com.voicedeutsch.master.data.local.database.entity.DailyStatisticsEntity
import com.voicedeutsch.master.data.local.database.entity.PronunciationRecordEntity
import com.voicedeutsch.master.data.local.database.entity.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ProgressDaoTest {

    private lateinit var db: VoiceDeutschDatabase
    private lateinit var progressDao: ProgressDao
    private lateinit var userDao: UserDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VoiceDeutschDatabase::class.java
        ).allowMainThreadQueries().build()
        progressDao = db.progressDao()
        userDao = db.userDao()
    }

    @After
    fun tearDown() = db.close()

    private fun makeUser(id: String = "user_1") = UserEntity(id = id, name = "Test")

    private fun makeStats(id: String, userId: String = "user_1", date: String,
        wordsLearned: Int = 0, totalMinutes: Int = 0, averageScore: Float = 0f) =
        DailyStatisticsEntity(id = id, userId = userId, date = date,
            wordsLearned = wordsLearned, totalMinutes = totalMinutes, averageScore = averageScore)

    private fun makePronRecord(id: String, userId: String = "user_1", word: String = "Hund",
        score: Float = 0.8f, timestamp: Long = System.currentTimeMillis()) =
        PronunciationRecordEntity(id = id, userId = userId, word = word, score = score, timestamp = timestamp)

    @Test
    fun upsertDailyStats_insert_canBeRetrieved() = runTest {
        userDao.insertUser(makeUser())
        progressDao.upsertDailyStats(makeStats("ds_1", date = "2026-03-01"))
        assertNotNull(progressDao.getDailyStats("user_1", "2026-03-01"))
    }

    @Test
    fun upsertDailyStats_update_replacesExisting() = runTest {
        userDao.insertUser(makeUser())
        progressDao.upsertDailyStats(makeStats("ds_1", date = "2026-03-01", wordsLearned = 5))
        progressDao.upsertDailyStats(makeStats("ds_1", date = "2026-03-01", wordsLearned = 20))
        assertEquals(20, progressDao.getDailyStats("user_1", "2026-03-01")!!.wordsLearned)
    }

    @Test
    fun getDailyStats_nonExisting_returnsNull() = runTest {
        assertNull(progressDao.getDailyStats("user_1", "2026-01-01"))
    }

    @Test
    fun getDailyStats_wrongDate_returnsNull() = runTest {
        userDao.insertUser(makeUser())
        progressDao.upsertDailyStats(makeStats("ds_1", date = "2026-03-01"))
        assertNull(progressDao.getDailyStats("user_1", "2026-03-02"))
    }

    @Test
    fun getDailyStats_wrongUser_returnsNull() = runTest {
        userDao.insertUser(makeUser("user_1"))
        progressDao.upsertDailyStats(makeStats("ds_1", userId = "user_1", date = "2026-03-01"))
        assertNull(progressDao.getDailyStats("user_2", "2026-03-01"))
    }

    @Test
    fun getDailyStatsRange_withinRange_returnsOrderedByDate() = runTest {
        userDao.insertUser(makeUser())
        listOf("2026-03-01", "2026-03-03", "2026-03-05", "2026-03-07").forEachIndexed { i, d ->
            progressDao.upsertDailyStats(makeStats("d$i", date = d))
        }
        val result = progressDao.getDailyStatsRange("user_1", "2026-03-01", "2026-03-05")
        assertEquals(3, result.size)
        assertEquals("2026-03-01", result[0].date)
        assertEquals("2026-03-05", result[2].date)
    }

    @Test
    fun getDailyStatsRange_noMatchInRange_returnsEmpty() = runTest {
        userDao.insertUser(makeUser())
        progressDao.upsertDailyStats(makeStats("d1", date = "2026-01-01"))
        assertTrue(progressDao.getDailyStatsRange("user_1", "2026-03-01", "2026-03-31").isEmpty())
    }

    @Test
    fun getRecentDailyStats_orderedByDateDesc() = runTest {
        userDao.insertUser(makeUser())
        listOf("2026-03-01", "2026-03-05", "2026-03-03").forEachIndexed { i, d ->
            progressDao.upsertDailyStats(makeStats("d$i", date = d))
        }
        assertEquals("2026-03-05", progressDao.getRecentDailyStats("user_1", 10)[0].date)
    }

    @Test
    fun getRecentDailyStats_respectsLimit() = runTest {
        userDao.insertUser(makeUser())
        repeat(5) { i -> progressDao.upsertDailyStats(makeStats("d$i", date = "2026-03-0${i + 1}")) }
        assertEquals(3, progressDao.getRecentDailyStats("user_1", 3).size)
    }

    @Test
    fun insertPronunciationRecord_canBeRetrieved() = runTest {
        userDao.insertUser(makeUser())
        progressDao.insertPronunciationRecord(makePronRecord("pr_1", word = "Straße", score = 0.75f))
        val result = progressDao.getPronunciationRecords("user_1", "Straße")
        assertEquals(1, result.size); assertEquals(0.75f, result[0].score, 0.001f)
    }

    @Test
    fun getPronunciationRecords_onlyForCorrectWord() = runTest {
        userDao.insertUser(makeUser())
        progressDao.insertPronunciationRecord(makePronRecord("pr_1", word = "Hund"))
        progressDao.insertPronunciationRecord(makePronRecord("pr_2", word = "Katze"))
        val result = progressDao.getPronunciationRecords("user_1", "Hund")
        assertEquals(1, result.size); assertEquals("Hund", result[0].word)
    }

    @Test
    fun getPronunciationRecords_empty_returnsEmptyList() = runTest {
        assertTrue(progressDao.getPronunciationRecords("user_1", "Hund").isEmpty())
    }

    @Test
    fun getAveragePronunciationScore_noRecords_returnsZero() = runTest {
        assertEquals(0f, progressDao.getAveragePronunciationScore("user_1"), 0.001f)
    }

    @Test
    fun getAveragePronunciationScore_withRecords_returnsAverage() = runTest {
        userDao.insertUser(makeUser())
        val ts = System.currentTimeMillis()
        progressDao.insertPronunciationRecord(makePronRecord("pr_1", score = 0.6f, timestamp = ts))
        progressDao.insertPronunciationRecord(makePronRecord("pr_2", word = "Katze", score = 1.0f, timestamp = ts))
        assertEquals(0.8f, progressDao.getAveragePronunciationScore("user_1"), 0.05f)
    }

    @Test
    fun getProblemWordsForPronunciation_lowScoreWithEnoughAttempts_included() = runTest {
        userDao.insertUser(makeUser())
        val ts = System.currentTimeMillis()
        repeat(3) { i -> progressDao.insertPronunciationRecord(makePronRecord("pr_$i", word = "Straße", score = 0.5f, timestamp = ts + i)) }
        val result = progressDao.getProblemWordsForPronunciation("user_1")
        assertEquals(1, result.size); assertEquals("Straße", result[0].word); assertTrue(result[0].avgScore < 0.7f)
    }

    @Test
    fun getProblemWordsForPronunciation_highScore_notIncluded() = runTest {
        userDao.insertUser(makeUser())
        val ts = System.currentTimeMillis()
        repeat(3) { i -> progressDao.insertPronunciationRecord(makePronRecord("pr_$i", word = "Hund", score = 0.95f, timestamp = ts + i)) }
        assertTrue(progressDao.getProblemWordsForPronunciation("user_1").isEmpty())
    }

    @Test
    fun getProblemWordsForPronunciation_tooFewAttempts_notIncluded() = runTest {
        userDao.insertUser(makeUser())
        val ts = System.currentTimeMillis()
        repeat(2) { i -> progressDao.insertPronunciationRecord(makePronRecord("pr_$i", word = "Katze", score = 0.4f, timestamp = ts + i)) }
        assertTrue(progressDao.getProblemWordsForPronunciation("user_1").isEmpty())
    }

    @Test
    fun getRecentRecords_orderedByTimestampDesc() = runTest {
        userDao.insertUser(makeUser())
        progressDao.insertPronunciationRecord(makePronRecord("pr_1", timestamp = 1000L))
        progressDao.insertPronunciationRecord(makePronRecord("pr_2", timestamp = 3000L))
        progressDao.insertPronunciationRecord(makePronRecord("pr_3", timestamp = 2000L))
        assertEquals(3000L, progressDao.getRecentRecords("user_1", 10)[0].timestamp)
    }

    @Test
    fun getRecentRecords_respectsLimit() = runTest {
        userDao.insertUser(makeUser())
        repeat(5) { i -> progressDao.insertPronunciationRecord(makePronRecord("pr_$i", timestamp = i.toLong())) }
        assertEquals(2, progressDao.getRecentRecords("user_1", 2).size)
    }
}
