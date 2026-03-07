// Путь: src/androidTest/java/com/voicedeutsch/master/data/local/database/dao/KnowledgeDaoTest.kt
package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.voicedeutsch.master.data.local.database.VoiceDeutschDatabase
import com.voicedeutsch.master.data.local.database.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class KnowledgeDaoTest {

    private lateinit var db: VoiceDeutschDatabase
    private lateinit var knowledgeDao: KnowledgeDao
    private lateinit var userDao: UserDao
    private lateinit var wordDao: WordDao
    private lateinit var grammarRuleDao: GrammarRuleDao
    private lateinit var phraseDao: PhraseDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VoiceDeutschDatabase::class.java
        ).allowMainThreadQueries().build()
        knowledgeDao = db.knowledgeDao()
        userDao = db.userDao()
        wordDao = db.wordDao()
        grammarRuleDao = db.grammarRuleDao()
        phraseDao = db.phraseDao()
    }

    @After
    fun tearDown() = db.close()

    private fun makeUser(id: String = "user_1") = UserEntity(id = id, name = "Test")
    private fun makeWord(id: String = "word_1") = WordEntity(id = id, german = "Hund $id", russian = "собака $id", partOfSpeech = "noun")
    private fun makeRule(id: String = "rule_1") = GrammarRuleEntity(id = id, nameRu = "Rule $id", nameDe = "Regel $id", category = "test", descriptionRu = "desc")
    private fun makePhrase(id: String = "phrase_1") = PhraseEntity(id = id, german = "Guten Tag $id", russian = "Добрый день $id", category = "greetings")

    private fun makeWordKnowledge(id: String = "wk_1", userId: String = "user_1", wordId: String = "word_1",
        level: Int = 0, timesSeen: Int = 0, timesCorrect: Int = 0, timesIncorrect: Int = 0, nextReview: Long? = null) =
        WordKnowledgeEntity(id = id, userId = userId, wordId = wordId, knowledgeLevel = level,
            timesSeen = timesSeen, timesCorrect = timesCorrect, timesIncorrect = timesIncorrect, nextReview = nextReview)

    private fun makeRuleKnowledge(id: String = "rk_1", userId: String = "user_1", ruleId: String = "rule_1",
        level: Int = 0, nextReview: Long? = null) =
        RuleKnowledgeEntity(id = id, userId = userId, ruleId = ruleId, knowledgeLevel = level, nextReview = nextReview)

    private fun makePhraseKnowledge(id: String = "pk_1", userId: String = "user_1", phraseId: String = "phrase_1", nextReview: Long? = null) =
        PhraseKnowledgeEntity(id = id, userId = userId, phraseId = phraseId, nextReview = nextReview)

    // ── WordKnowledge ─────────────────────────────────────────────────────

    @Test
    fun getWordKnowledge_nonExisting_returnsNull() = runTest {
        assertNull(knowledgeDao.getWordKnowledge("user_1", "word_1"))
    }

    @Test
    fun getWordKnowledge_existing_returnsEntity() = runTest {
        userDao.insertUser(makeUser()); wordDao.insertWord(makeWord())
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge())
        val result = knowledgeDao.getWordKnowledge("user_1", "word_1")
        assertNotNull(result); assertEquals("wk_1", result!!.id)
    }

    @Test
    fun getWordKnowledgeByGerman_existing_returnsEntity() = runTest {
        userDao.insertUser(makeUser())
        wordDao.insertWord(WordEntity(id = "w_dog", german = "der Hund", russian = "собака", partOfSpeech = "noun"))
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge(wordId = "w_dog"))
        assertNotNull(knowledgeDao.getWordKnowledgeByGerman("user_1", "der Hund"))
    }

    @Test
    fun getWordKnowledgeByGerman_noWord_returnsNull() = runTest {
        assertNull(knowledgeDao.getWordKnowledgeByGerman("user_1", "nonexistent"))
    }

    @Test
    fun getAllWordKnowledge_empty_returnsEmptyList() = runTest {
        assertTrue(knowledgeDao.getAllWordKnowledge("user_1").isEmpty())
    }

    @Test
    fun getAllWordKnowledge_returnsOnlyUserEntries() = runTest {
        userDao.insertUser(makeUser("user_1")); userDao.insertUser(makeUser("user_2"))
        wordDao.insertWords(listOf(makeWord("w1"), makeWord("w2")))
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_1", userId = "user_1", wordId = "w1"))
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_2", userId = "user_2", wordId = "w2"))
        assertEquals(1, knowledgeDao.getAllWordKnowledge("user_1").size)
    }

    @Test
    fun getWordKnowledgeFlow_emitsCurrentList() = runTest {
        userDao.insertUser(makeUser()); wordDao.insertWord(makeWord())
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge())
        assertEquals(1, knowledgeDao.getWordKnowledgeFlow("user_1").first().size)
    }

    @Test
    fun getWordsForReview_withPastNextReview_returnsItems() = runTest {
        userDao.insertUser(makeUser()); wordDao.insertWord(makeWord())
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge(nextReview = System.currentTimeMillis() - 10_000L))
        assertEquals(1, knowledgeDao.getWordsForReview("user_1", System.currentTimeMillis(), 10).size)
    }

    @Test
    fun getWordsForReview_withFutureNextReview_returnsEmpty() = runTest {
        userDao.insertUser(makeUser()); wordDao.insertWord(makeWord())
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge(nextReview = System.currentTimeMillis() + 100_000L))
        assertTrue(knowledgeDao.getWordsForReview("user_1", System.currentTimeMillis(), 10).isEmpty())
    }

    @Test
    fun getWordsForReview_withNullNextReview_returnsEmpty() = runTest {
        userDao.insertUser(makeUser()); wordDao.insertWord(makeWord())
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge(nextReview = null))
        assertTrue(knowledgeDao.getWordsForReview("user_1", System.currentTimeMillis(), 10).isEmpty())
    }

    @Test
    fun getWordsForReview_respectsLimit() = runTest {
        userDao.insertUser(makeUser())
        val past = System.currentTimeMillis() - 10_000L
        repeat(5) { i -> wordDao.insertWord(makeWord("w_$i")); knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_$i", wordId = "w_$i", nextReview = past)) }
        assertEquals(3, knowledgeDao.getWordsForReview("user_1", System.currentTimeMillis(), 3).size)
    }

    @Test
    fun getWordsForReviewCount_withDueItems_returnsCorrectCount() = runTest {
        userDao.insertUser(makeUser())
        wordDao.insertWords(listOf(makeWord("w1"), makeWord("w2"), makeWord("w3")))
        val past = System.currentTimeMillis() - 1000L
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_1", wordId = "w1", nextReview = past))
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_2", wordId = "w2", nextReview = past))
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_3", wordId = "w3", nextReview = null))
        assertEquals(2, knowledgeDao.getWordsForReviewCount("user_1", System.currentTimeMillis()))
    }

    @Test
    fun getKnownWordsCount_level4AndAbove_returnsCounted() = runTest {
        userDao.insertUser(makeUser())
        wordDao.insertWords(listOf(makeWord("w1"), makeWord("w2"), makeWord("w3"), makeWord("w4")))
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_1", wordId = "w1", level = 3))
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_2", wordId = "w2", level = 4))
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_3", wordId = "w3", level = 5))
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_4", wordId = "w4", level = 7))
        assertEquals(3, knowledgeDao.getKnownWordsCount("user_1"))
    }

    @Test
    fun getKnownWordsCount_noKnownWords_returnsZero() = runTest {
        userDao.insertUser(makeUser()); wordDao.insertWord(makeWord())
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge(level = 2))
        assertEquals(0, knowledgeDao.getKnownWordsCount("user_1"))
    }

    @Test
    fun getActiveWordsCount_level5AndAbove_returnsCounted() = runTest {
        userDao.insertUser(makeUser())
        wordDao.insertWords(listOf(makeWord("w1"), makeWord("w2")))
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_1", wordId = "w1", level = 4))
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_2", wordId = "w2", level = 5))
        assertEquals(1, knowledgeDao.getActiveWordsCount("user_1"))
    }

    @Test
    fun getWordKnowledgeByLevel_returnsCorrectLevel() = runTest {
        userDao.insertUser(makeUser())
        wordDao.insertWords(listOf(makeWord("w1"), makeWord("w2"), makeWord("w3")))
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_1", wordId = "w1", level = 2))
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_2", wordId = "w2", level = 2))
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_3", wordId = "w3", level = 4))
        val result = knowledgeDao.getWordKnowledgeByLevel("user_1", 2)
        assertEquals(2, result.size); assertTrue(result.all { it.knowledgeLevel == 2 })
    }

    @Test
    fun getProblemWords_moreIncorrectThanCorrect_returnsItem() = runTest {
        userDao.insertUser(makeUser()); wordDao.insertWord(makeWord())
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge(timesCorrect = 1, timesIncorrect = 4, timesSeen = 5))
        assertEquals(1, knowledgeDao.getProblemWords("user_1", 10).size)
    }

    @Test
    fun getProblemWords_tooFewSeen_returnsEmpty() = runTest {
        userDao.insertUser(makeUser()); wordDao.insertWord(makeWord())
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge(timesCorrect = 0, timesIncorrect = 2, timesSeen = 2))
        assertTrue(knowledgeDao.getProblemWords("user_1", 10).isEmpty())
    }

    // ── RuleKnowledge ─────────────────────────────────────────────────────

    @Test
    fun getRuleKnowledge_nonExisting_returnsNull() = runTest {
        assertNull(knowledgeDao.getRuleKnowledge("user_1", "rule_1"))
    }

    @Test
    fun getRuleKnowledge_existing_returnsEntity() = runTest {
        userDao.insertUser(makeUser()); grammarRuleDao.insertRule(makeRule())
        knowledgeDao.upsertRuleKnowledge(makeRuleKnowledge())
        assertNotNull(knowledgeDao.getRuleKnowledge("user_1", "rule_1"))
    }

    @Test
    fun getAllRuleKnowledge_returnsOnlyUserEntries() = runTest {
        userDao.insertUser(makeUser("u1")); userDao.insertUser(makeUser("u2"))
        grammarRuleDao.insertRules(listOf(makeRule("r1"), makeRule("r2")))
        knowledgeDao.upsertRuleKnowledge(makeRuleKnowledge("rk_1", userId = "u1", ruleId = "r1"))
        knowledgeDao.upsertRuleKnowledge(makeRuleKnowledge("rk_2", userId = "u2", ruleId = "r2"))
        assertEquals(1, knowledgeDao.getAllRuleKnowledge("u1").size)
    }

    @Test
    fun getRulesForReview_pastNextReview_returnsDue() = runTest {
        userDao.insertUser(makeUser()); grammarRuleDao.insertRule(makeRule())
        knowledgeDao.upsertRuleKnowledge(makeRuleKnowledge(nextReview = System.currentTimeMillis() - 5000L))
        assertEquals(1, knowledgeDao.getRulesForReview("user_1", System.currentTimeMillis(), 10).size)
    }

    @Test
    fun getKnownRulesCount_level4AndAbove_returnsCounted() = runTest {
        userDao.insertUser(makeUser())
        grammarRuleDao.insertRules(listOf(makeRule("r1"), makeRule("r2")))
        knowledgeDao.upsertRuleKnowledge(makeRuleKnowledge("rk_1", ruleId = "r1", level = 4))
        knowledgeDao.upsertRuleKnowledge(makeRuleKnowledge("rk_2", ruleId = "r2", level = 3))
        assertEquals(1, knowledgeDao.getKnownRulesCount("user_1"))
    }

    // ── PhraseKnowledge ───────────────────────────────────────────────────

    @Test
    fun getPhraseKnowledge_nonExisting_returnsNull() = runTest {
        assertNull(knowledgeDao.getPhraseKnowledge("user_1", "phrase_1"))
    }

    @Test
    fun getPhraseKnowledge_existing_returnsEntity() = runTest {
        userDao.insertUser(makeUser()); phraseDao.insertPhrase(makePhrase())
        knowledgeDao.upsertPhraseKnowledge(makePhraseKnowledge())
        assertNotNull(knowledgeDao.getPhraseKnowledge("user_1", "phrase_1"))
    }

    @Test
    fun getPhrasesForReview_pastNextReview_returnsDue() = runTest {
        userDao.insertUser(makeUser()); phraseDao.insertPhrase(makePhrase())
        knowledgeDao.upsertPhraseKnowledge(makePhraseKnowledge(nextReview = System.currentTimeMillis() - 5000L))
        assertEquals(1, knowledgeDao.getPhrasesForReview("user_1", System.currentTimeMillis(), 10).size)
    }

    @Test
    fun getPhrasesForReviewCount_pastNextReview_returnsCorrectCount() = runTest {
        userDao.insertUser(makeUser())
        phraseDao.insertPhrases(listOf(makePhrase("p1"), makePhrase("p2")))
        val past = System.currentTimeMillis() - 5000L
        knowledgeDao.upsertPhraseKnowledge(makePhraseKnowledge("pk_1", phraseId = "p1", nextReview = past))
        knowledgeDao.upsertPhraseKnowledge(makePhraseKnowledge("pk_2", phraseId = "p2", nextReview = null))
        assertEquals(1, knowledgeDao.getPhrasesForReviewCount("user_1", System.currentTimeMillis()))
    }

    // ── PronunciationRecords ──────────────────────────────────────────────

    @Test
    fun insertPronunciationRecord_andGetRecords_returnsInserted() = runTest {
        userDao.insertUser(makeUser())
        val record = PronunciationRecordEntity(id = "pr_1", userId = "user_1", word = "Hund", score = 0.8f, timestamp = System.currentTimeMillis())
        knowledgeDao.insertPronunciationRecord(record)
        val result = knowledgeDao.getPronunciationRecords("user_1", "Hund")
        assertEquals(1, result.size); assertEquals(0.8f, result[0].score, 0.001f)
    }

    @Test
    fun getAveragePronunciationScore_noRecords_returnsNull() = runTest {
        assertNull(knowledgeDao.getAveragePronunciationScore("user_1"))
    }

    @Test
    fun getAveragePronunciationScore_withRecords_returnsAverage() = runTest {
        userDao.insertUser(makeUser())
        val ts = System.currentTimeMillis()
        knowledgeDao.insertPronunciationRecord(PronunciationRecordEntity(id = "pr_1", userId = "user_1", word = "Hund", score = 0.6f, timestamp = ts))
        knowledgeDao.insertPronunciationRecord(PronunciationRecordEntity(id = "pr_2", userId = "user_1", word = "Katze", score = 1.0f, timestamp = ts))
        assertEquals(0.8f, knowledgeDao.getAveragePronunciationScore("user_1")!!, 0.05f)
    }

    @Test
    fun getPerfectPronunciationCount_highScore_returnsCounted() = runTest {
        userDao.insertUser(makeUser())
        val ts = System.currentTimeMillis()
        knowledgeDao.insertPronunciationRecord(PronunciationRecordEntity(id = "pr_1", userId = "user_1", word = "w1", score = 0.95f, timestamp = ts))
        knowledgeDao.insertPronunciationRecord(PronunciationRecordEntity(id = "pr_2", userId = "user_1", word = "w2", score = 0.7f, timestamp = ts))
        assertEquals(1, knowledgeDao.getPerfectPronunciationCount("user_1"))
    }

    // ── getBriefWordKnowledge ─────────────────────────────────────────────

    @Test
    fun getBriefWordKnowledge_returnsProjectedFields() = runTest {
        userDao.insertUser(makeUser()); wordDao.insertWord(makeWord("w1"))
        knowledgeDao.upsertWordKnowledge(makeWordKnowledge("wk_1", wordId = "w1", level = 3))
        val result = knowledgeDao.getBriefWordKnowledge("user_1")
        assertEquals(1, result.size); assertEquals("w1", result[0].wordId); assertEquals(3, result[0].knowledgeLevel)
    }

    @Test
    fun getBriefWordKnowledge_empty_returnsEmptyList() = runTest {
        assertTrue(knowledgeDao.getBriefWordKnowledge("user_1").isEmpty())
    }
}
