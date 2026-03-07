// Путь: src/androidTest/java/com/voicedeutsch/master/data/local/database/dao/SessionDaoTest.kt
package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.voicedeutsch.master.data.local.database.VoiceDeutschDatabase
import com.voicedeutsch.master.data.local.database.entity.SessionEntity
import com.voicedeutsch.master.data.local.database.entity.SessionEventEntity
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
class SessionDaoTest {

    private lateinit var db: VoiceDeutschDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var userDao: UserDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VoiceDeutschDatabase::class.java
        ).allowMainThreadQueries().build()
        sessionDao = db.sessionDao()
        userDao = db.userDao()
    }

    @After
    fun tearDown() = db.close()

    private fun makeUser(id: String = "user_1") = UserEntity(id = id, name = "Test")

    private fun makeSession(id: String, userId: String = "user_1",
        startedAt: Long = System.currentTimeMillis(), durationMinutes: Int = 30) =
        SessionEntity(id = id, userId = userId, startedAt = startedAt, durationMinutes = durationMinutes)

    private fun makeEvent(id: String, sessionId: String, eventType: String = "word_learned",
        timestamp: Long = System.currentTimeMillis()) =
        SessionEventEntity(id = id, sessionId = sessionId, eventType = eventType, timestamp = timestamp)

    @Test
    fun getSession_nonExisting_returnsNull() = runTest {
        assertNull(sessionDao.getSession("missing"))
    }

    @Test
    fun insertSession_canBeRetrievedById() = runTest {
        userDao.insertUser(makeUser())
        sessionDao.insertSession(makeSession("sess_1"))
        val result = sessionDao.getSession("sess_1")
        assertNotNull(result); assertEquals("sess_1", result!!.id)
    }

    @Test
    fun insertSession_replace_updatesExisting() = runTest {
        userDao.insertUser(makeUser())
        sessionDao.insertSession(makeSession("sess_1", durationMinutes = 10))
        sessionDao.insertSession(makeSession("sess_1", durationMinutes = 45))
        assertEquals(45, sessionDao.getSession("sess_1")!!.durationMinutes)
    }

    @Test
    fun updateSession_changesEndedAt() = runTest {
        userDao.insertUser(makeUser())
        sessionDao.insertSession(makeSession("sess_1"))
        val endTime = System.currentTimeMillis()
        sessionDao.updateSession(sessionDao.getSession("sess_1")!!.copy(endedAt = endTime))
        assertEquals(endTime, sessionDao.getSession("sess_1")!!.endedAt)
    }

    @Test
    fun getRecentSessions_empty_returnsEmptyList() = runTest {
        userDao.insertUser(makeUser())
        assertTrue(sessionDao.getRecentSessions("user_1", 10).isEmpty())
    }

    @Test
    fun getRecentSessions_orderedByStartedAtDesc() = runTest {
        userDao.insertUser(makeUser())
        sessionDao.insertSession(makeSession("s1", startedAt = 1000L))
        sessionDao.insertSession(makeSession("s2", startedAt = 3000L))
        sessionDao.insertSession(makeSession("s3", startedAt = 2000L))
        assertEquals(listOf(3000L, 2000L, 1000L), sessionDao.getRecentSessions("user_1", 10).map { it.startedAt })
    }

    @Test
    fun getRecentSessions_respectsLimit() = runTest {
        userDao.insertUser(makeUser())
        repeat(5) { i -> sessionDao.insertSession(makeSession("s_$i", startedAt = i.toLong())) }
        assertEquals(3, sessionDao.getRecentSessions("user_1", 3).size)
    }

    @Test
    fun getRecentSessions_onlyForCorrectUser() = runTest {
        userDao.insertUser(makeUser("u1")); userDao.insertUser(makeUser("u2"))
        sessionDao.insertSession(makeSession("s1", userId = "u1"))
        sessionDao.insertSession(makeSession("s2", userId = "u2"))
        assertEquals(1, sessionDao.getRecentSessions("u1", 10).size)
    }

    @Test
    fun getSessionsFlow_emitsCurrentSessions() = runTest {
        userDao.insertUser(makeUser())
        sessionDao.insertSession(makeSession("s1"))
        assertEquals(1, sessionDao.getSessionsFlow("user_1").first().size)
    }

    @Test
    fun getSessionCount_noSessions_returnsZero() = runTest {
        userDao.insertUser(makeUser())
        assertEquals(0, sessionDao.getSessionCount("user_1"))
    }

    @Test
    fun getSessionCount_afterInserts_returnsCorrectCount() = runTest {
        userDao.insertUser(makeUser())
        repeat(4) { i -> sessionDao.insertSession(makeSession("s_$i")) }
        assertEquals(4, sessionDao.getSessionCount("user_1"))
    }

    @Test
    fun getTotalMinutes_noSessions_returnsZero() = runTest {
        assertEquals(0, sessionDao.getTotalMinutes("user_1"))
    }

    @Test
    fun getTotalMinutes_sumsDurationCorrectly() = runTest {
        userDao.insertUser(makeUser())
        sessionDao.insertSession(makeSession("s1", durationMinutes = 30))
        sessionDao.insertSession(makeSession("s2", durationMinutes = 45))
        sessionDao.insertSession(makeSession("s3", durationMinutes = 15))
        assertEquals(90, sessionDao.getTotalMinutes("user_1"))
    }

    @Test
    fun getTotalMinutes_onlyForCorrectUser() = runTest {
        userDao.insertUser(makeUser("u1")); userDao.insertUser(makeUser("u2"))
        sessionDao.insertSession(makeSession("s1", userId = "u1", durationMinutes = 60))
        sessionDao.insertSession(makeSession("s2", userId = "u2", durationMinutes = 200))
        assertEquals(60, sessionDao.getTotalMinutes("u1"))
    }

    @Test
    fun insertSessionEvent_canBeRetrieved() = runTest {
        userDao.insertUser(makeUser())
        sessionDao.insertSession(makeSession("sess_1"))
        sessionDao.insertSessionEvent(makeEvent("ev_1", sessionId = "sess_1"))
        val result = sessionDao.getSessionEvents("sess_1")
        assertEquals(1, result.size); assertEquals("ev_1", result[0].id)
    }

    @Test
    fun getSessionEvents_empty_returnsEmptyList() = runTest {
        userDao.insertUser(makeUser())
        sessionDao.insertSession(makeSession("sess_1"))
        assertTrue(sessionDao.getSessionEvents("sess_1").isEmpty())
    }

    @Test
    fun getSessionEvents_orderedByTimestampAsc() = runTest {
        userDao.insertUser(makeUser())
        sessionDao.insertSession(makeSession("sess_1"))
        sessionDao.insertSessionEvent(makeEvent("ev_3", "sess_1", timestamp = 3000L))
        sessionDao.insertSessionEvent(makeEvent("ev_1", "sess_1", timestamp = 1000L))
        sessionDao.insertSessionEvent(makeEvent("ev_2", "sess_1", timestamp = 2000L))
        assertEquals(listOf(1000L, 2000L, 3000L), sessionDao.getSessionEvents("sess_1").map { it.timestamp })
    }

    @Test
    fun getSessionEvents_onlyForCorrectSession() = runTest {
        userDao.insertUser(makeUser())
        sessionDao.insertSession(makeSession("sess_1"))
        sessionDao.insertSession(makeSession("sess_2"))
        sessionDao.insertSessionEvent(makeEvent("ev_1", "sess_1"))
        sessionDao.insertSessionEvent(makeEvent("ev_2", "sess_2"))
        val result = sessionDao.getSessionEvents("sess_1")
        assertEquals(1, result.size); assertEquals("ev_1", result[0].id)
    }

    @Test
    fun sessionDeletion_cascadesEvents() = runTest {
        userDao.insertUser(makeUser())
        sessionDao.insertSession(makeSession("sess_1"))
        sessionDao.insertSessionEvent(makeEvent("ev_1", "sess_1"))
        db.query("DELETE FROM sessions WHERE id = 'sess_1'", null)
        assertTrue(sessionDao.getSessionEvents("sess_1").isEmpty())
    }

    @Test
    fun userDeletion_cascadesSessions() = runTest {
        userDao.insertUser(makeUser())
        sessionDao.insertSession(makeSession("sess_1"))
        db.query("DELETE FROM users WHERE id = 'user_1'", null)
        assertNull(sessionDao.getSession("sess_1"))
    }
}
