// Путь: src/androidTest/java/com/voicedeutsch/master/data/local/database/dao/AchievementDaoTest.kt
package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.voicedeutsch.master.data.local.database.VoiceDeutschDatabase
import com.voicedeutsch.master.data.local.database.entity.AchievementEntity
import com.voicedeutsch.master.data.local.database.entity.UserAchievementEntity
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
class AchievementDaoTest {

    private lateinit var db: VoiceDeutschDatabase
    private lateinit var achievementDao: AchievementDao
    private lateinit var userDao: UserDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VoiceDeutschDatabase::class.java
        ).allowMainThreadQueries().build()
        achievementDao = db.achievementDao()
        userDao = db.userDao()
    }

    @After
    fun tearDown() { db.close() }

    private fun makeAchievement(id: String = "ach_1", category: String = "vocabulary") =
        AchievementEntity(id = id, nameRu = "Тест $id", nameDe = "Test $id",
            descriptionRu = "Описание", icon = "ic_test", conditionJson = "{}", category = category)

    private fun makeUser(id: String = "user_1") = UserEntity(id = id, name = "Test User")

    private fun makeUserAchievement(
        id: String = "ua_1", userId: String = "user_1",
        achievementId: String = "ach_1", announced: Boolean = false,
    ) = UserAchievementEntity(id = id, userId = userId, achievementId = achievementId,
        earnedAt = System.currentTimeMillis(), announced = announced)

    @Test
    fun getAllAchievements_empty_returnsEmptyList() = runTest {
        assertTrue(achievementDao.getAllAchievements().isEmpty())
    }

    @Test
    fun getAllAchievements_afterInsert_returnsAll() = runTest {
        achievementDao.insertAchievements(listOf(makeAchievement("a1"), makeAchievement("a2")))
        assertEquals(2, achievementDao.getAllAchievements().size)
    }

    @Test
    fun getAchievement_existingId_returnsEntity() = runTest {
        achievementDao.insertAchievements(listOf(makeAchievement("ach_x")))
        val result = achievementDao.getAchievement("ach_x")
        assertNotNull(result)
        assertEquals("ach_x", result!!.id)
    }

    @Test
    fun getAchievement_nonExistingId_returnsNull() = runTest {
        assertNull(achievementDao.getAchievement("missing_id"))
    }

    @Test
    fun getAchievementsByCategory_matchingCategory_returnsMatches() = runTest {
        achievementDao.insertAchievements(listOf(
            makeAchievement("a1", "vocabulary"), makeAchievement("a2", "vocabulary"), makeAchievement("a3", "grammar")))
        val result = achievementDao.getAchievementsByCategory("vocabulary")
        assertEquals(2, result.size)
        assertTrue(result.all { it.category == "vocabulary" })
    }

    @Test
    fun getAchievementsByCategory_noMatch_returnsEmpty() = runTest {
        achievementDao.insertAchievements(listOf(makeAchievement("a1", "vocabulary")))
        assertTrue(achievementDao.getAchievementsByCategory("streak").isEmpty())
    }

    @Test
    fun insertAchievements_duplicateId_replacesExisting() = runTest {
        achievementDao.insertAchievements(listOf(makeAchievement("ach_1").copy(nameRu = "Оригинал")))
        achievementDao.insertAchievements(listOf(makeAchievement("ach_1").copy(nameRu = "Обновлённый")))
        assertEquals("Обновлённый", achievementDao.getAchievement("ach_1")!!.nameRu)
    }

    @Test
    fun getUserAchievements_empty_returnsEmptyList() = runTest {
        userDao.insertUser(makeUser("user_1"))
        assertTrue(achievementDao.getUserAchievements("user_1").isEmpty())
    }

    @Test
    fun getUserAchievements_afterInsert_returnsUserAchievements() = runTest {
        userDao.insertUser(makeUser("user_1"))
        achievementDao.insertAchievements(listOf(makeAchievement("ach_1")))
        achievementDao.insertUserAchievement(makeUserAchievement())
        val result = achievementDao.getUserAchievements("user_1")
        assertEquals(1, result.size)
        assertEquals("ach_1", result[0].achievementId)
    }

    @Test
    fun getUserAchievements_differentUser_returnsEmpty() = runTest {
        userDao.insertUser(makeUser("user_1")); userDao.insertUser(makeUser("user_2"))
        achievementDao.insertAchievements(listOf(makeAchievement("ach_1")))
        achievementDao.insertUserAchievement(makeUserAchievement(userId = "user_1"))
        assertTrue(achievementDao.getUserAchievements("user_2").isEmpty())
    }

    @Test
    fun observeUserAchievements_emitsCurrentList() = runTest {
        userDao.insertUser(makeUser("user_1"))
        achievementDao.insertAchievements(listOf(makeAchievement("ach_1")))
        achievementDao.insertUserAchievement(makeUserAchievement())
        assertEquals(1, achievementDao.observeUserAchievements("user_1").first().size)
    }

    @Test
    fun getUnannounced_hasUnannounced_returnsThem() = runTest {
        userDao.insertUser(makeUser("user_1"))
        achievementDao.insertAchievements(listOf(makeAchievement("ach_1"), makeAchievement("ach_2")))
        achievementDao.insertUserAchievement(makeUserAchievement("ua_1", achievementId = "ach_1", announced = false))
        achievementDao.insertUserAchievement(makeUserAchievement("ua_2", achievementId = "ach_2", announced = true))
        val result = achievementDao.getUnannounced("user_1")
        assertEquals(1, result.size)
        assertEquals("ach_1", result[0].achievementId)
    }

    @Test
    fun getUnannounced_allAnnounced_returnsEmpty() = runTest {
        userDao.insertUser(makeUser("user_1"))
        achievementDao.insertAchievements(listOf(makeAchievement("ach_1")))
        achievementDao.insertUserAchievement(makeUserAchievement(announced = true))
        assertTrue(achievementDao.getUnannounced("user_1").isEmpty())
    }

    @Test
    fun hasAchievement_exists_returnsOne() = runTest {
        userDao.insertUser(makeUser("user_1"))
        achievementDao.insertAchievements(listOf(makeAchievement("ach_1")))
        achievementDao.insertUserAchievement(makeUserAchievement())
        assertEquals(1, achievementDao.hasAchievement("user_1", "ach_1"))
    }

    @Test
    fun hasAchievement_notExists_returnsZero() = runTest {
        userDao.insertUser(makeUser("user_1"))
        assertEquals(0, achievementDao.hasAchievement("user_1", "ach_missing"))
    }

    @Test
    fun markAnnounced_setsAnnouncedToTrue() = runTest {
        userDao.insertUser(makeUser("user_1"))
        achievementDao.insertAchievements(listOf(makeAchievement("ach_1")))
        achievementDao.insertUserAchievement(makeUserAchievement(announced = false))
        achievementDao.markAnnounced("user_1", "ach_1")
        assertTrue(achievementDao.getUserAchievements("user_1")[0].announced)
    }

    @Test
    fun markAnnounced_nonExistingRecord_doesNotCrash() = runTest {
        achievementDao.markAnnounced("user_ghost", "ach_ghost")
    }

    @Test
    fun countUserAchievements_noAchievements_returnsZero() = runTest {
        userDao.insertUser(makeUser("user_1"))
        assertEquals(0, achievementDao.countUserAchievements("user_1"))
    }

    @Test
    fun countUserAchievements_afterMultipleInserts_returnsCorrectCount() = runTest {
        userDao.insertUser(makeUser("user_1"))
        achievementDao.insertAchievements(listOf(makeAchievement("ach_1"), makeAchievement("ach_2")))
        achievementDao.insertUserAchievement(makeUserAchievement("ua_1", achievementId = "ach_1"))
        achievementDao.insertUserAchievement(makeUserAchievement("ua_2", achievementId = "ach_2"))
        assertEquals(2, achievementDao.countUserAchievements("user_1"))
    }

    @Test
    fun insertUserAchievement_duplicateIgnored() = runTest {
        userDao.insertUser(makeUser("user_1"))
        achievementDao.insertAchievements(listOf(makeAchievement("ach_1")))
        achievementDao.insertUserAchievement(makeUserAchievement("ua_1"))
        achievementDao.insertUserAchievement(makeUserAchievement("ua_1"))
        assertEquals(1, achievementDao.countUserAchievements("user_1"))
    }

    @Test
    fun userAchievement_cascadeDeletedWhenUserDeleted() = runTest {
        userDao.insertUser(makeUser("user_1"))
        achievementDao.insertAchievements(listOf(makeAchievement("ach_1")))
        achievementDao.insertUserAchievement(makeUserAchievement())
        db.query("DELETE FROM users WHERE id = 'user_1'", null)
        assertTrue(achievementDao.getUserAchievements("user_1").isEmpty())
    }
}
