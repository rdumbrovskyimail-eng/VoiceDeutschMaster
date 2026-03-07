// Путь: src/androidTest/java/com/voicedeutsch/master/data/local/database/dao/PhraseDaoTest.kt
package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.voicedeutsch.master.data.local.database.VoiceDeutschDatabase
import com.voicedeutsch.master.data.local.database.entity.PhraseEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PhraseDaoTest {

    private lateinit var db: VoiceDeutschDatabase
    private lateinit var phraseDao: PhraseDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VoiceDeutschDatabase::class.java
        ).allowMainThreadQueries().build()
        phraseDao = db.phraseDao()
    }

    @After
    fun tearDown() = db.close()

    private fun makePhrase(id: String, category: String = "greetings", level: String = "A1") =
        PhraseEntity(id = id, german = "Guten Tag $id", russian = "Добрый день $id",
            category = category, difficultyLevel = level)

    @Test
    fun getPhrase_nonExisting_returnsNull() = runTest {
        assertNull(phraseDao.getPhrase("ph_missing"))
    }

    @Test
    fun getPhrase_existing_returnsEntity() = runTest {
        phraseDao.insertPhrase(makePhrase("ph_1"))
        val result = phraseDao.getPhrase("ph_1")
        assertNotNull(result); assertEquals("ph_1", result!!.id)
    }

    @Test
    fun getAllPhrases_empty_returnsEmptyList() = runTest {
        assertTrue(phraseDao.getAllPhrases().isEmpty())
    }

    @Test
    fun getAllPhrases_afterBulkInsert_returnsAll() = runTest {
        phraseDao.insertPhrases(listOf(makePhrase("ph_1"), makePhrase("ph_2"), makePhrase("ph_3")))
        assertEquals(3, phraseDao.getAllPhrases().size)
    }

    @Test
    fun getPhrasesByCategory_matchingCategory_returnsMatches() = runTest {
        phraseDao.insertPhrases(listOf(
            makePhrase("ph_1", category = "greetings"),
            makePhrase("ph_2", category = "greetings"),
            makePhrase("ph_3", category = "farewell")))
        val result = phraseDao.getPhrasesByCategory("greetings")
        assertEquals(2, result.size); assertTrue(result.all { it.category == "greetings" })
    }

    @Test
    fun getPhrasesByCategory_noMatch_returnsEmpty() = runTest {
        phraseDao.insertPhrase(makePhrase("ph_1", category = "greetings"))
        assertTrue(phraseDao.getPhrasesByCategory("questions").isEmpty())
    }

    @Test
    fun getPhrasesByLevel_matchingLevel_returnsMatches() = runTest {
        phraseDao.insertPhrases(listOf(
            makePhrase("ph_1", level = "A1"), makePhrase("ph_2", level = "A1"), makePhrase("ph_3", level = "B2")))
        val result = phraseDao.getPhrasesByLevel("A1")
        assertEquals(2, result.size); assertTrue(result.all { it.difficultyLevel == "A1" })
    }

    @Test
    fun getPhrasesByLevel_noMatch_returnsEmpty() = runTest {
        phraseDao.insertPhrase(makePhrase("ph_1", level = "A1"))
        assertTrue(phraseDao.getPhrasesByLevel("C1").isEmpty())
    }

    @Test
    fun insertPhrase_replace_updatesExisting() = runTest {
        phraseDao.insertPhrase(makePhrase("ph_1").copy(category = "old"))
        phraseDao.insertPhrase(makePhrase("ph_1").copy(category = "new"))
        assertEquals("new", phraseDao.getPhrase("ph_1")!!.category)
    }

    @Test
    fun insertPhrases_empty_doesNotCrash() = runTest {
        phraseDao.insertPhrases(emptyList())
        assertEquals(0, phraseDao.getPhraseCount())
    }

    @Test
    fun insertPhrases_multipleEntries_allInserted() = runTest {
        phraseDao.insertPhrases((1..5).map { makePhrase("ph_$it") })
        assertEquals(5, phraseDao.getPhraseCount())
    }

    @Test
    fun getPhraseCount_noPhrases_returnsZero() = runTest {
        assertEquals(0, phraseDao.getPhraseCount())
    }

    @Test
    fun getPhraseCount_afterInserts_returnsCorrectCount() = runTest {
        phraseDao.insertPhrases((1..7).map { makePhrase("ph_$it") })
        assertEquals(7, phraseDao.getPhraseCount())
    }

    @Test
    fun insertedPhrase_allFieldsPreserved() = runTest {
        val phrase = PhraseEntity(id = "ph_full", german = "Wie geht es Ihnen?", russian = "Как у вас дела?",
            category = "greetings", difficultyLevel = "A1", bookChapter = 2, bookLesson = 3, context = "formal")
        phraseDao.insertPhrase(phrase)
        val result = phraseDao.getPhrase("ph_full")!!
        assertEquals("Wie geht es Ihnen?", result.german)
        assertEquals(2, result.bookChapter)
        assertEquals("formal", result.context)
    }
}
