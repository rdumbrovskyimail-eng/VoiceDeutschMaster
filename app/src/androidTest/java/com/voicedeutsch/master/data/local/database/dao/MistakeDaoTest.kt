// Путь: src/androidTest/java/com/voicedeutsch/master/data/local/database/dao/MistakeDaoTest.kt
package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.voicedeutsch.master.data.local.database.VoiceDeutschDatabase
import com.voicedeutsch.master.data.local.database.entity.MistakeLogEntity
import com.voicedeutsch.master.data.local.database.entity.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MistakeDaoTest {

    private lateinit var db: VoiceDeutschDatabase
    private lateinit var mistakeDao: MistakeDao
    private lateinit var userDao: UserDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VoiceDeutschDatabase::class.java
        ).allowMainThreadQueries().build()
        mistakeDao = db.mistakeDao()
        userDao = db.userDao()
    }

    @After
    fun tearDown() = db.close()

    private fun makeUser(id: String = "user_1") = UserEntity(id = id, name = "Test")

    private fun makeMistake(id: String, userId: String = "user_1", type: String = "vocabulary",
        sessionId: String? = null, timestamp: Long = System.currentTimeMillis()) =
        MistakeLogEntity(id = id, userId = userId, type = type,
            item = "der Hund", expected = "собака", actual = "кошка",
            sessionId = sessionId, timestamp = timestamp)

    @Test
    fun insertMistake_canBeRetrievedByUser() = runTest {
        userDao.insertUser(makeUser())
        mistakeDao.insertMistake(makeMistake("ml_1"))
        val result = mistakeDao.getMistakes("user_1", 10)
        assertEquals(1, result.size); assertEquals("ml_1", result[0].id)
    }

    @Test
    fun insertMistake_replace_updatesExisting() = runTest {
        userDao.insertUser(makeUser())
        mistakeDao.insertMistake(makeMistake("ml_1").copy(item = "original"))
        mistakeDao.insertMistake(makeMistake("ml_1").copy(item = "updated"))
        val result = mistakeDao.getMistakes("user_1", 10)
        assertEquals(1, result.size); assertEquals("updated", result[0].item)
    }

    @Test
    fun getMistakes_empty_returnsEmptyList() = runTest {
        userDao.insertUser(makeUser())
        assertTrue(mistakeDao.getMistakes("user_1", 10).isEmpty())
    }

    @Test
    fun getMistakes_orderedByTimestampDesc() = runTest {
        userDao.insertUser(makeUser())
        mistakeDao.insertMistake(makeMistake("ml_1", timestamp = 1000L))
        mistakeDao.insertMistake(makeMistake("ml_2", timestamp = 3000L))
        mistakeDao.insertMistake(makeMistake("ml_3", timestamp = 2000L))
        assertEquals(listOf(3000L, 2000L, 1000L), mistakeDao.getMistakes("user_1", 10).map { it.timestamp })
    }

    @Test
    fun getMistakes_respectsLimit() = runTest {
        userDao.insertUser(makeUser())
        repeat(5) { i -> mistakeDao.insertMistake(makeMistake("ml_$i", timestamp = i.toLong())) }
        assertEquals(3, mistakeDao.getMistakes("user_1", 3).size)
    }

    @Test
    fun getMistakes_onlyForCorrectUser() = runTest {
        userDao.insertUser(makeUser("u1")); userDao.insertUser(makeUser("u2"))
        mistakeDao.insertMistake(makeMistake("ml_1", userId = "u1"))
        mistakeDao.insertMistake(makeMistake("ml_2", userId = "u2"))
        assertEquals(1, mistakeDao.getMistakes("u1", 10).size)
    }

    @Test
    fun getMistakesByType_matchingType_returnsMatches() = runTest {
        userDao.insertUser(makeUser())
        mistakeDao.insertMistake(makeMistake("ml_1", type = "vocabulary"))
        mistakeDao.insertMistake(makeMistake("ml_2", type = "vocabulary"))
        mistakeDao.insertMistake(makeMistake("ml_3", type = "grammar"))
        val result = mistakeDao.getMistakesByType("user_1", "vocabulary")
        assertEquals(2, result.size); assertTrue(result.all { it.type == "vocabulary" })
    }

    @Test
    fun getMistakesByType_noMatch_returnsEmpty() = runTest {
        userDao.insertUser(makeUser())
        mistakeDao.insertMistake(makeMistake("ml_1", type = "vocabulary"))
        assertTrue(mistakeDao.getMistakesByType("user_1", "pronunciation").isEmpty())
    }

    @Test
    fun getMistakeCount_noMistakes_returnsZero() = runTest {
        userDao.insertUser(makeUser())
        assertEquals(0, mistakeDao.getMistakeCount("user_1"))
    }

    @Test
    fun getMistakeCount_afterInserts_returnsCorrectCount() = runTest {
        userDao.insertUser(makeUser())
        repeat(4) { i -> mistakeDao.insertMistake(makeMistake("ml_$i")) }
        assertEquals(4, mistakeDao.getMistakeCount("user_1"))
    }

    @Test
    fun getMistakesBySession_returnsOnlySessionMistakes() = runTest {
        userDao.insertUser(makeUser())
        mistakeDao.insertMistake(makeMistake("ml_1", sessionId = "sess_1", timestamp = 100L))
        mistakeDao.insertMistake(makeMistake("ml_2", sessionId = "sess_2", timestamp = 200L))
        mistakeDao.insertMistake(makeMistake("ml_3", sessionId = "sess_1", timestamp = 300L))
        val result = mistakeDao.getMistakesBySession("user_1", "sess_1")
        assertEquals(2, result.size); assertTrue(result.all { it.sessionId == "sess_1" })
    }

    @Test
    fun getMistakesBySession_orderedByTimestampAsc() = runTest {
        userDao.insertUser(makeUser())
        mistakeDao.insertMistake(makeMistake("ml_1", sessionId = "sess_1", timestamp = 300L))
        mistakeDao.insertMistake(makeMistake("ml_2", sessionId = "sess_1", timestamp = 100L))
        val result = mistakeDao.getMistakesBySession("user_1", "sess_1")
        assertEquals(100L, result[0].timestamp); assertEquals(300L, result[1].timestamp)
    }

    @Test
    fun getFrequentMistakes_itemOccurs3Times_isIncluded() = runTest {
        userDao.insertUser(makeUser())
        repeat(3) { i -> mistakeDao.insertMistake(makeMistake("ml_v$i").copy(item = "der Hund", type = "vocabulary")) }
        val result = mistakeDao.getFrequentMistakes("user_1", 10)
        assertEquals(1, result.size); assertEquals("der Hund", result[0].item); assertEquals(3, result[0].count)
    }

    @Test
    fun getFrequentMistakes_itemOccursTwice_isNotIncluded() = runTest {
        userDao.insertUser(makeUser())
        repeat(2) { i -> mistakeDao.insertMistake(makeMistake("ml_r$i").copy(item = "die Katze", type = "vocabulary")) }
        assertTrue(mistakeDao.getFrequentMistakes("user_1", 10).isEmpty())
    }

    @Test
    fun getFrequentMistakes_respectsLimit() = runTest {
        userDao.insertUser(makeUser())
        listOf("Hund", "Katze", "Haus").forEachIndexed { idx, item ->
            repeat(3) { i -> mistakeDao.insertMistake(makeMistake("ml_${idx}_$i", timestamp = (idx * 10 + i).toLong()).copy(item = item, type = "vocabulary")) }
        }
        assertEquals(2, mistakeDao.getFrequentMistakes("user_1", 2).size)
    }

    @Test
    fun userDeletion_cascadesMistakeDeletion() = runTest {
        userDao.insertUser(makeUser())
        mistakeDao.insertMistake(makeMistake("ml_1"))
        db.query("DELETE FROM users WHERE id = 'user_1'", null)
        assertEquals(0, mistakeDao.getMistakeCount("user_1"))
    }
}
