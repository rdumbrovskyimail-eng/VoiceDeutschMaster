// Путь: src/androidTest/java/com/voicedeutsch/master/data/local/database/dao/UserDaoTest.kt
package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.voicedeutsch.master.data.local.database.VoiceDeutschDatabase
import com.voicedeutsch.master.data.local.database.entity.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class UserDaoTest {

    private lateinit var db: VoiceDeutschDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VoiceDeutschDatabase::class.java
        ).allowMainThreadQueries().build()
        userDao = db.userDao()
    }

    @After
    fun tearDown() = db.close()

    private fun makeUser(id: String = "user_1", name: String = "Алексей", cefrLevel: String = "A1",
        streakDays: Int = 0, totalSessions: Int = 0, totalMinutes: Int = 0,
        totalWordsLearned: Int = 0, totalRulesLearned: Int = 0) =
        UserEntity(id = id, name = name, cefrLevel = cefrLevel, streakDays = streakDays,
            totalSessions = totalSessions, totalMinutes = totalMinutes,
            totalWordsLearned = totalWordsLearned, totalRulesLearned = totalRulesLearned)

    @Test
    fun getUser_nonExisting_returnsNull() = runTest {
        assertNull(userDao.getUser("missing_id"))
    }

    @Test
    fun insertUser_canBeRetrievedById() = runTest {
        userDao.insertUser(makeUser("user_1", name = "Иван"))
        val result = userDao.getUser("user_1")
        assertNotNull(result); assertEquals("Иван", result!!.name)
    }

    @Test
    fun insertUser_replace_updatesExisting() = runTest {
        userDao.insertUser(makeUser("user_1", name = "Оригинал"))
        userDao.insertUser(makeUser("user_1", name = "Обновлённый"))
        assertEquals("Обновлённый", userDao.getUser("user_1")!!.name)
    }

    @Test
    fun getUserFlow_existingUser_emitsUser() = runTest {
        userDao.insertUser(makeUser("user_1"))
        val result = userDao.getUserFlow("user_1").first()
        assertNotNull(result); assertEquals("user_1", result!!.id)
    }

    @Test
    fun getUserFlow_nonExistingUser_emitsNull() = runTest {
        assertNull(userDao.getUserFlow("missing").first())
    }

    @Test
    fun getFirstUser_noUsers_returnsNull() = runTest {
        assertNull(userDao.getFirstUser())
    }

    @Test
    fun getFirstUser_withOneUser_returnsThatUser() = runTest {
        userDao.insertUser(makeUser("user_1"))
        assertNotNull(userDao.getFirstUser())
    }

    @Test
    fun getFirstUser_withMultipleUsers_returnsOne() = runTest {
        userDao.insertUser(makeUser("user_1")); userDao.insertUser(makeUser("user_2"))
        assertNotNull(userDao.getFirstUser())
    }

    @Test
    fun getUserCount_noUsers_returnsZero() = runTest {
        assertEquals(0, userDao.getUserCount())
    }

    @Test
    fun getUserCount_afterInserts_returnsCorrectCount() = runTest {
        userDao.insertUser(makeUser("user_1")); userDao.insertUser(makeUser("user_2"))
        assertEquals(2, userDao.getUserCount())
    }

    @Test
    fun updateUser_changesName() = runTest {
        userDao.insertUser(makeUser("user_1", name = "Старое имя"))
        userDao.updateUser(userDao.getUser("user_1")!!.copy(name = "Новое имя"))
        assertEquals("Новое имя", userDao.getUser("user_1")!!.name)
    }

    @Test
    fun updateLevel_updatesCefrLevelAndSubLevel() = runTest {
        userDao.insertUser(makeUser("user_1", cefrLevel = "A1"))
        userDao.updateLevel("user_1", "B1", 2, System.currentTimeMillis())
        val result = userDao.getUser("user_1")
        assertEquals("B1", result!!.cefrLevel); assertEquals(2, result.cefrSubLevel)
    }

    @Test
    fun updateLevel_nonExistingUser_doesNotCrash() = runTest {
        userDao.updateLevel("ghost", "C1", 1, System.currentTimeMillis())
    }

    @Test
    fun incrementSessionStats_correctlyIncrements() = runTest {
        userDao.insertUser(makeUser("user_1", totalSessions = 2, totalMinutes = 60, totalWordsLearned = 10, totalRulesLearned = 5))
        val now = System.currentTimeMillis()
        userDao.incrementSessionStats("user_1", 30, 5, 3, now, now)
        val result = userDao.getUser("user_1")!!
        assertEquals(3, result.totalSessions)
        assertEquals(90, result.totalMinutes)
        assertEquals(15, result.totalWordsLearned)
        assertEquals(8, result.totalRulesLearned)
    }

    @Test
    fun incrementSessionStats_updatesLastSessionDate() = runTest {
        userDao.insertUser(makeUser("user_1"))
        val sessionDate = 99_999_999L
        userDao.incrementSessionStats("user_1", 0, 0, 0, sessionDate, System.currentTimeMillis())
        assertEquals(sessionDate, userDao.getUser("user_1")!!.lastSessionDate)
    }

    @Test
    fun incrementSessionStats_zeroValues_incrementsByZero() = runTest {
        userDao.insertUser(makeUser("user_1", totalSessions = 5, totalMinutes = 100))
        userDao.incrementSessionStats("user_1", 0, 0, 0, System.currentTimeMillis(), System.currentTimeMillis())
        val result = userDao.getUser("user_1")!!
        assertEquals(6, result.totalSessions); assertEquals(100, result.totalMinutes)
    }

    @Test
    fun updateStreak_changesStreakDays() = runTest {
        userDao.insertUser(makeUser("user_1", streakDays = 5))
        userDao.updateStreak("user_1", 15, System.currentTimeMillis())
        assertEquals(15, userDao.getUser("user_1")!!.streakDays)
    }

    @Test
    fun updateStreak_resetToZero_streakIsZero() = runTest {
        userDao.insertUser(makeUser("user_1", streakDays = 30))
        userDao.updateStreak("user_1", 0, System.currentTimeMillis())
        assertEquals(0, userDao.getUser("user_1")!!.streakDays)
    }

    @Test
    fun updatePreferences_changesPreferencesJson() = runTest {
        userDao.insertUser(makeUser("user_1"))
        val newPrefs = """{"theme":"dark","lang":"ru"}"""
        userDao.updatePreferences("user_1", newPrefs, System.currentTimeMillis())
        assertEquals(newPrefs, userDao.getUser("user_1")!!.preferencesJson)
    }

    @Test
    fun updateVoiceSettings_changesVoiceSettingsJson() = runTest {
        userDao.insertUser(makeUser("user_1"))
        val newSettings = """{"voice":"female","speed":1.0}"""
        userDao.updateVoiceSettings("user_1", newSettings, System.currentTimeMillis())
        assertEquals(newSettings, userDao.getUser("user_1")!!.voiceSettingsJson)
    }

    @Test
    fun getAllUserIds_noUsers_returnsEmptyList() = runTest {
        assertTrue(userDao.getAllUserIds().isEmpty())
    }

    @Test
    fun getAllUserIds_multipleUsers_returnsAllIds() = runTest {
        userDao.insertUser(makeUser("u1")); userDao.insertUser(makeUser("u2")); userDao.insertUser(makeUser("u3"))
        val ids = userDao.getAllUserIds()
        assertEquals(3, ids.size); assertTrue(ids.containsAll(listOf("u1", "u2", "u3")))
    }
}
