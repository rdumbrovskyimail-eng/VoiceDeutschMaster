// Путь: src/androidTest/java/com/voicedeutsch/master/data/local/database/dao/WordDaoTest.kt
package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.voicedeutsch.master.data.local.database.VoiceDeutschDatabase
import com.voicedeutsch.master.data.local.database.entity.GrammarRuleEntity
import com.voicedeutsch.master.data.local.database.entity.WordEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class WordDaoTest {

    private lateinit var db: VoiceDeutschDatabase
    private lateinit var wordDao: WordDao
    private lateinit var grammarRuleDao: GrammarRuleDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VoiceDeutschDatabase::class.java
        ).allowMainThreadQueries().build()
        wordDao = db.wordDao()
        grammarRuleDao = db.grammarRuleDao()
    }

    @After
    fun tearDown() = db.close()

    private fun makeWord(id: String, german: String = "Wort_$id", russian: String = "слово_$id",
        topic: String = "general", level: String = "A1", chapter: Int? = null) =
        WordEntity(id = id, german = german, russian = russian, partOfSpeech = "noun",
            topic = topic, difficultyLevel = level, bookChapter = chapter)

    private fun makeRule(id: String) = GrammarRuleEntity(id = id, nameRu = "Rule $id",
        nameDe = "Regel $id", category = "test", descriptionRu = "desc")

    @Test
    fun getWord_nonExisting_returnsNull() = runTest {
        assertNull(wordDao.getWord("missing"))
    }

    @Test
    fun getWord_existing_returnsWord() = runTest {
        wordDao.insertWord(makeWord("w1"))
        val result = wordDao.getWord("w1")
        assertNotNull(result); assertEquals("w1", result!!.id)
    }

    @Test
    fun getWordByGerman_existing_returnsWord() = runTest {
        wordDao.insertWord(makeWord("w1", german = "der Hund"))
        val result = wordDao.getWordByGerman("der Hund")
        assertNotNull(result); assertEquals("w1", result!!.id)
    }

    @Test
    fun getWordByGerman_nonExisting_returnsNull() = runTest {
        assertNull(wordDao.getWordByGerman("die Katze"))
    }

    @Test
    fun getWordByGerman_duplicateGerman_returnsFirst() = runTest {
        wordDao.insertWord(makeWord("w1", german = "Hund"))
        wordDao.insertWord(makeWord("w2", german = "Hund"))
        assertNotNull(wordDao.getWordByGerman("Hund"))
    }

    @Test
    fun getAllWords_empty_returnsEmptyList() = runTest {
        assertTrue(wordDao.getAllWords().isEmpty())
    }

    @Test
    fun getAllWords_afterBulkInsert_returnsAll() = runTest {
        wordDao.insertWords(listOf(makeWord("w1"), makeWord("w2"), makeWord("w3")))
        assertEquals(3, wordDao.getAllWords().size)
    }

    @Test
    fun getWordsByTopic_matchingTopic_returnsMatches() = runTest {
        wordDao.insertWords(listOf(makeWord("w1", topic = "animals"), makeWord("w2", topic = "animals"), makeWord("w3", topic = "food")))
        val result = wordDao.getWordsByTopic("animals")
        assertEquals(2, result.size); assertTrue(result.all { it.topic == "animals" })
    }

    @Test
    fun getWordsByTopic_noMatch_returnsEmpty() = runTest {
        wordDao.insertWord(makeWord("w1", topic = "animals"))
        assertTrue(wordDao.getWordsByTopic("travel").isEmpty())
    }

    @Test
    fun getWordsByLevel_matchingLevel_returnsMatches() = runTest {
        wordDao.insertWords(listOf(makeWord("w1", level = "A1"), makeWord("w2", level = "A1"), makeWord("w3", level = "B2")))
        assertEquals(2, wordDao.getWordsByLevel("A1").size)
    }

    @Test
    fun getWordsByLevel_noMatch_returnsEmpty() = runTest {
        wordDao.insertWord(makeWord("w1", level = "A1"))
        assertTrue(wordDao.getWordsByLevel("C2").isEmpty())
    }

    @Test
    fun getWordsByChapter_matchingChapter_returnsMatches() = runTest {
        wordDao.insertWords(listOf(makeWord("w1", chapter = 3), makeWord("w2", chapter = 3), makeWord("w3", chapter = 5)))
        val result = wordDao.getWordsByChapter(3)
        assertEquals(2, result.size); assertTrue(result.all { it.bookChapter == 3 })
    }

    @Test
    fun getWordsByChapter_noMatch_returnsEmpty() = runTest {
        wordDao.insertWord(makeWord("w1", chapter = 1))
        assertTrue(wordDao.getWordsByChapter(99).isEmpty())
    }

    @Test
    fun searchWords_byGerman_returnsMatchingWords() = runTest {
        wordDao.insertWords(listOf(
            makeWord("w1", german = "der Hund", russian = "собака"),
            makeWord("w2", german = "die Katze", russian = "кошка"),
            makeWord("w3", german = "das Haus", russian = "дом")))
        val result = wordDao.searchWords("Hund")
        assertEquals(1, result.size); assertEquals("w1", result[0].id)
    }

    @Test
    fun searchWords_byRussian_returnsMatchingWords() = runTest {
        wordDao.insertWords(listOf(
            makeWord("w1", german = "der Hund", russian = "собака"),
            makeWord("w2", german = "die Katze", russian = "кошка")))
        val result = wordDao.searchWords("собака")
        assertEquals(1, result.size); assertEquals("w1", result[0].id)
    }

    @Test
    fun searchWords_partialMatch_returnsResults() = runTest {
        wordDao.insertWord(makeWord("w1", german = "der Hund"))
        assertEquals(1, wordDao.searchWords("und").size)
    }

    @Test
    fun searchWords_noMatch_returnsEmpty() = runTest {
        wordDao.insertWord(makeWord("w1", german = "der Hund"))
        assertTrue(wordDao.searchWords("xyz_no_match").isEmpty())
    }

    @Test
    fun searchWords_emptyQuery_returnsAll() = runTest {
        wordDao.insertWords(listOf(makeWord("w1"), makeWord("w2")))
        assertEquals(2, wordDao.searchWords("").size)
    }

    @Test
    fun insertWord_replace_updatesExisting() = runTest {
        wordDao.insertWord(makeWord("w1", topic = "animals"))
        wordDao.insertWord(makeWord("w1", topic = "food"))
        assertEquals("food", wordDao.getWord("w1")!!.topic)
    }

    @Test
    fun insertWords_empty_doesNotCrash() = runTest {
        wordDao.insertWords(emptyList())
        assertEquals(0, wordDao.getWordCount())
    }

    @Test
    fun insertWords_bulkInsert_allInserted() = runTest {
        wordDao.insertWords((1..10).map { makeWord("w_$it") })
        assertEquals(10, wordDao.getWordCount())
    }

    @Test
    fun getWordCount_noWords_returnsZero() = runTest {
        assertEquals(0, wordDao.getWordCount())
    }

    @Test
    fun getWordCount_afterInserts_returnsCorrectCount() = runTest {
        wordDao.insertWords((1..5).map { makeWord("w_$it") })
        assertEquals(5, wordDao.getWordCount())
    }

    @Test
    fun getTotalWordCount_equalsGetWordCount() = runTest {
        wordDao.insertWords((1..3).map { makeWord("w_$it") })
        assertEquals(wordDao.getWordCount(), wordDao.getTotalWordCount())
    }

    @Test
    fun getTotalRuleCount_noRules_returnsZero() = runTest {
        assertEquals(0, wordDao.getTotalRuleCount())
    }

    @Test
    fun getTotalRuleCount_afterInserts_returnsCorrectCount() = runTest {
        grammarRuleDao.insertRules((1..4).map { makeRule("gr_$it") })
        assertEquals(4, wordDao.getTotalRuleCount())
    }

    @Test
    fun getTotalRuleCount_independentFromWordCount() = runTest {
        wordDao.insertWords((1..5).map { makeWord("w_$it") })
        grammarRuleDao.insertRules((1..3).map { makeRule("gr_$it") })
        assertEquals(3, wordDao.getTotalRuleCount()); assertEquals(5, wordDao.getWordCount())
    }

    @Test
    fun insertWord_allFieldsPreserved() = runTest {
        val word = WordEntity(id = "w_full", german = "der Hund", russian = "собака",
            partOfSpeech = "noun", gender = "m", plural = "die Hunde",
            exampleSentenceDe = "Der Hund bellt.", exampleSentenceRu = "Собака лает.",
            phoneticTranscription = "hʊnt", difficultyLevel = "A1",
            topic = "animals", bookChapter = 1, bookLesson = 2, source = "book")
        wordDao.insertWord(word)
        val result = wordDao.getWord("w_full")!!
        assertEquals("m", result.gender); assertEquals("die Hunde", result.plural)
        assertEquals("hʊnt", result.phoneticTranscription)
        assertEquals(1, result.bookChapter); assertEquals(2, result.bookLesson)
    }
}
