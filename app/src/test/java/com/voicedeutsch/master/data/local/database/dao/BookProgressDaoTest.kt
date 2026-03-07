// Путь: src/androidTest/java/com/voicedeutsch/master/data/local/database/dao/BookProgressDaoTest.kt
package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.voicedeutsch.master.data.local.database.VoiceDeutschDatabase
import com.voicedeutsch.master.data.local.database.entity.BookProgressEntity
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
class BookProgressDaoTest {

    private lateinit var db: VoiceDeutschDatabase
    private lateinit var progressDao: BookProgressDao
    private lateinit var userDao: UserDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VoiceDeutschDatabase::class.java
        ).allowMainThreadQueries().build()
        progressDao = db.bookProgressDao()
        userDao = db.userDao()
    }

    @After
    fun tearDown() = db.close()

    private fun makeUser(id: String = "user_1") = UserEntity(id = id, name = "Test")

    private fun makeProgress(
        id: String, userId: String = "user_1",
        chapter: Int = 1, lesson: Int = 1, status: String = "NOT_STARTED",
    ) = BookProgressEntity(id = id, userId = userId, chapter = chapter, lesson = lesson, status = status)

    @Test
    fun getProgress_nonExisting_returnsNull() = runTest {
        userDao.insertUser(makeUser())
        assertNull(progressDao.getProgress("user_1", 1, 1))
    }

    @Test
    fun getProgress_existing_returnsCorrectEntry() = runTest {
        userDao.insertUser(makeUser())
        progressDao.upsertProgress(makeProgress("bp_1", chapter = 1, lesson = 1))
        val result = progressDao.getProgress("user_1", 1, 1)
        assertNotNull(result)
        assertEquals("bp_1", result!!.id)
    }

    @Test
    fun getProgress_wrongChapter_returnsNull() = runTest {
        userDao.insertUser(makeUser())
        progressDao.upsertProgress(makeProgress("bp_1", chapter = 1, lesson = 1))
        assertNull(progressDao.getProgress("user_1", 2, 1))
    }

    @Test
    fun getCurrentPosition_noProgress_returnsNull() = runTest {
        userDao.insertUser(makeUser())
        assertNull(progressDao.getCurrentPosition("user_1"))
    }

    @Test
    fun getCurrentPosition_prefersInProgress() = runTest {
        userDao.insertUser(makeUser())
        progressDao.upsertProgress(makeProgress("bp_1", chapter = 1, lesson = 1, status = "NOT_STARTED"))
        progressDao.upsertProgress(makeProgress("bp_2", chapter = 2, lesson = 1, status = "IN_PROGRESS"))
        assertEquals("IN_PROGRESS", progressDao.getCurrentPosition("user_1")!!.status)
    }

    @Test
    fun getCurrentPosition_allNotStarted_returnsFirstChapter() = runTest {
        userDao.insertUser(makeUser())
        progressDao.upsertProgress(makeProgress("bp_2", chapter = 2, lesson = 1, status = "NOT_STARTED"))
        progressDao.upsertProgress(makeProgress("bp_1", chapter = 1, lesson = 1, status = "NOT_STARTED"))
        assertEquals(1, progressDao.getCurrentPosition("user_1")!!.chapter)
    }

    @Test
    fun getAllProgress_empty_returnsEmptyList() = runTest {
        userDao.insertUser(makeUser())
        assertTrue(progressDao.getAllProgress("user_1").isEmpty())
    }

    @Test
    fun getAllProgress_orderedByChapterAndLesson() = runTest {
        userDao.insertUser(makeUser())
        progressDao.upsertProgress(makeProgress("bp_3", chapter = 2, lesson = 1))
        progressDao.upsertProgress(makeProgress("bp_1", chapter = 1, lesson = 1))
        progressDao.upsertProgress(makeProgress("bp_2", chapter = 1, lesson = 2))
        val result = progressDao.getAllProgress("user_1")
        assertEquals(listOf(1 to 1, 1 to 2, 2 to 1), result.map { it.chapter to it.lesson })
    }

    @Test
    fun getAllProgressFlow_emitsCurrentData() = runTest {
        userDao.insertUser(makeUser())
        progressDao.upsertProgress(makeProgress("bp_1"))
        assertEquals(1, progressDao.getAllProgressFlow("user_1").first().size)
    }

    @Test
    fun upsertProgress_insert_createsNewEntry() = runTest {
        userDao.insertUser(makeUser())
        progressDao.upsertProgress(makeProgress("bp_1", chapter = 3, lesson = 2))
        assertNotNull(progressDao.getProgress("user_1", 3, 2))
    }

    @Test
    fun upsertProgress_update_replacesExistingEntry() = runTest {
        userDao.insertUser(makeUser())
        progressDao.upsertProgress(makeProgress("bp_1", status = "NOT_STARTED"))
        progressDao.upsertProgress(makeProgress("bp_1", status = "IN_PROGRESS"))
        assertEquals("IN_PROGRESS", progressDao.getProgress("user_1", 1, 1)!!.status)
    }

    @Test
    fun getCompletedCount_noCompleted_returnsZero() = runTest {
        userDao.insertUser(makeUser())
        progressDao.upsertProgress(makeProgress("bp_1", status = "IN_PROGRESS"))
        assertEquals(0, progressDao.getCompletedCount("user_1"))
    }

    @Test
    fun getCompletedCount_twoCompleted_returnsTwo() = runTest {
        userDao.insertUser(makeUser())
        progressDao.upsertProgress(makeProgress("bp_1", chapter = 1, lesson = 1, status = "COMPLETED"))
        progressDao.upsertProgress(makeProgress("bp_2", chapter = 1, lesson = 2, status = "COMPLETED"))
        progressDao.upsertProgress(makeProgress("bp_3", chapter = 2, lesson = 1, status = "NOT_STARTED"))
        assertEquals(2, progressDao.getCompletedCount("user_1"))
    }

    @Test
    fun markComplete_updatesStatusAndScore() = runTest {
        userDao.insertUser(makeUser())
        progressDao.upsertProgress(makeProgress("bp_1", chapter = 1, lesson = 1, status = "IN_PROGRESS"))
        progressDao.markComplete("user_1", 1, 1, 0.9f, System.currentTimeMillis())
        val result = progressDao.getProgress("user_1", 1, 1)
        assertEquals("COMPLETED", result!!.status)
        assertEquals(0.9f, result.score!!, 0.001f)
        assertNotNull(result.completedAt)
    }

    @Test
    fun markComplete_nonExistingEntry_doesNotCrash() = runTest {
        progressDao.markComplete("ghost_user", 99, 99, 1f, System.currentTimeMillis())
    }

    @Test
    fun userDeletion_cascadesProgressDeletion() = runTest {
        userDao.insertUser(makeUser("user_1"))
        progressDao.upsertProgress(makeProgress("bp_1", userId = "user_1"))
        db.query("DELETE FROM users WHERE id = 'user_1'", null)
        assertTrue(progressDao.getAllProgress("user_1").isEmpty())
    }
}
