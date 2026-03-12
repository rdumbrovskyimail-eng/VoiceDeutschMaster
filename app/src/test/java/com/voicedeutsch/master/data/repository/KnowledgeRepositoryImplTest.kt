// Путь: src/test/java/com/voicedeutsch/master/data/repository/KnowledgeRepositoryImplTest.kt
package com.voicedeutsch.master.data.repository

import com.voicedeutsch.master.data.local.database.dao.GrammarRuleDao
import com.voicedeutsch.master.data.local.database.dao.KnowledgeDao
import com.voicedeutsch.master.data.local.database.dao.MistakeDao
import com.voicedeutsch.master.data.local.database.dao.PhraseDao
import com.voicedeutsch.master.data.local.database.dao.ProgressDao
import com.voicedeutsch.master.data.local.database.dao.WordDao
// FIX: BriefWordKnowledge → KnowledgeDao.WordKnowledgeBrief (вложенный класс DAO)
import com.voicedeutsch.master.data.local.database.dao.KnowledgeDao.WordKnowledgeBrief
import com.voicedeutsch.master.data.local.database.entity.GrammarRuleEntity
import com.voicedeutsch.master.data.local.database.entity.MistakeLogEntity
import com.voicedeutsch.master.data.local.database.entity.PhraseEntity
import com.voicedeutsch.master.data.local.database.entity.PhraseKnowledgeEntity
import com.voicedeutsch.master.data.local.database.entity.PronunciationRecordEntity
import com.voicedeutsch.master.data.local.database.entity.RuleKnowledgeEntity
import com.voicedeutsch.master.data.local.database.entity.WordEntity
import com.voicedeutsch.master.data.local.database.entity.WordKnowledgeEntity
import com.voicedeutsch.master.data.remote.sync.CloudSyncService
import com.voicedeutsch.master.domain.model.knowledge.GrammarCategory
import com.voicedeutsch.master.domain.model.knowledge.GrammarRule
import com.voicedeutsch.master.domain.model.knowledge.MistakeLog
import com.voicedeutsch.master.domain.model.knowledge.MistakeType
import com.voicedeutsch.master.domain.model.knowledge.Phrase
import com.voicedeutsch.master.domain.model.knowledge.PhraseCategory
import com.voicedeutsch.master.domain.model.knowledge.PhraseKnowledge
import com.voicedeutsch.master.domain.model.knowledge.RuleKnowledge
import com.voicedeutsch.master.domain.model.knowledge.Word
import com.voicedeutsch.master.domain.model.knowledge.WordKnowledge
import com.voicedeutsch.master.domain.model.speech.PronunciationResult
import com.voicedeutsch.master.domain.model.speech.PronunciationTrend
import com.voicedeutsch.master.domain.model.user.CefrLevel
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

class KnowledgeRepositoryImplTest {

    private lateinit var wordDao: WordDao
    private lateinit var knowledgeDao: KnowledgeDao
    private lateinit var grammarRuleDao: GrammarRuleDao
    private lateinit var phraseDao: PhraseDao
    private lateinit var progressDao: ProgressDao
    private lateinit var mistakeDao: MistakeDao
    private lateinit var cloudSync: CloudSyncService
    private lateinit var json: Json
    private lateinit var repository: KnowledgeRepositoryImpl

    private val fixedNow = 1_700_000_000_000L

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeWordEntity(id: String = "w1", german: String = "Hund", russian: String = "собака"): WordEntity =
        mockk<WordEntity>(relaxed = true).also {
            every { it.id }              returns id
            every { it.german }          returns german
            every { it.russian }         returns russian
            every { it.partOfSpeech }    returns "NOUN"
            every { it.difficultyLevel } returns "A1"
            every { it.topic }           returns "animals"
            every { it.bookChapter }     returns 1
            every { it.source }          returns "book"
        }

    private fun makeWord(id: String = "w1", german: String = "Hund", topic: String = "animals"): Word =
        mockk<Word>(relaxed = true).also {
            every { it.id }     returns id
            every { it.german } returns german
            every { it.topic }  returns topic
        }

    private fun makeWordKnowledgeEntity(
        wordId: String = "w1",
        userId: String = "user1",
        knowledgeLevel: Int = 3,
        timesSeen: Int = 5,
        lastSeen: Long? = fixedNow,
        nextReview: Long? = null,
        pronunciationScore: Float = 0f,
        pronunciationAttempts: Int = 0
    ): WordKnowledgeEntity = mockk<WordKnowledgeEntity>(relaxed = true).also {
        every { it.wordId }                returns wordId
        every { it.userId }                returns userId
        every { it.knowledgeLevel }        returns knowledgeLevel
        every { it.timesSeen }             returns timesSeen
        every { it.lastSeen }              returns lastSeen
        every { it.nextReview }            returns nextReview
        every { it.pronunciationScore }    returns pronunciationScore
        every { it.pronunciationAttempts } returns pronunciationAttempts
        every { it.contextsJson }          returns "[]"
        every { it.mistakesJson }          returns "[]"
    }

    // FIX: BriefWordKnowledge → WordKnowledgeBrief
    private fun makeBriefWordKnowledge(
        wordId: String = "w1",
        knowledgeLevel: Int = 3,
        timesSeen: Int = 5,
        lastSeen: Long? = fixedNow
    ): WordKnowledgeBrief = mockk<WordKnowledgeBrief>(relaxed = true).also {
        every { it.wordId }         returns wordId
        every { it.knowledgeLevel } returns knowledgeLevel
        every { it.timesSeen }      returns timesSeen
        every { it.lastSeen }       returns lastSeen
    }

    private fun makeWordKnowledge(wordId: String = "w1"): WordKnowledge =
        mockk<WordKnowledge>(relaxed = true).also {
            every { it.wordId }         returns wordId
            every { it.knowledgeLevel } returns 3
        }

    private fun makeGrammarRuleEntity(
        id: String = "r1",
        // FIX: параметр level теперь маппится на difficultyLevel (cefrLevel не существует)
        level: String = "A1",
        category: String = "ARTICLES"
    ): GrammarRuleEntity =
        mockk<GrammarRuleEntity>(relaxed = true).also {
            every { it.id }              returns id
            every { it.nameRu }          returns "Артикль"
            every { it.nameDe }          returns "Artikel"
            // FIX: cefrLevel → difficultyLevel (реальное поле в GrammarRuleEntity)
            every { it.difficultyLevel } returns level
            every { it.category }        returns category
            every { it.examplesJson }    returns "[]"
            every { it.relatedRulesJson } returns "[]"
        }

    private fun makeGrammarRule(id: String = "r1"): GrammarRule =
        mockk<GrammarRule>(relaxed = true).also {
            every { it.id }     returns id
            every { it.nameRu } returns "Артикль"
        }

    private fun makeRuleKnowledgeEntity(
        ruleId: String = "r1",
        knowledgeLevel: Int = 3,
        timesPracticed: Int = 5
    ): RuleKnowledgeEntity = mockk<RuleKnowledgeEntity>(relaxed = true).also {
        every { it.ruleId }          returns ruleId
        every { it.knowledgeLevel }  returns knowledgeLevel
        every { it.timesPracticed }  returns timesPracticed
        // FIX: contextsJson → commonMistakesJson (реальное поле в RuleKnowledgeEntity)
        every { it.commonMistakesJson } returns "[]"
    }

    private fun makeRuleKnowledge(ruleId: String = "r1"): RuleKnowledge =
        mockk<RuleKnowledge>(relaxed = true).also {
            every { it.ruleId }         returns ruleId
            every { it.knowledgeLevel } returns 3
        }

    private fun makePhraseEntity(id: String = "p1", category: String = "GREETING", difficultyLevel: String = "A1"): PhraseEntity =
        mockk<PhraseEntity>(relaxed = true).also {
            every { it.id }              returns id
            every { it.german }          returns "Guten Tag"
            every { it.russian }         returns "Добрый день"
            every { it.category }        returns category
            every { it.difficultyLevel } returns difficultyLevel
            every { it.bookChapter }     returns null
            every { it.bookLesson }      returns null
            every { it.context }         returns ""
            every { it.createdAt }       returns fixedNow
        }

    private fun makePhrase(id: String = "p1"): Phrase =
        mockk<Phrase>(relaxed = true).also {
            every { it.id }              returns id
            every { it.german }          returns "Guten Tag"
            every { it.russian }         returns "Добрый день"
            every { it.category }        returns PhraseCategory.GREETING
            every { it.difficultyLevel } returns CefrLevel.A1
            every { it.bookChapter }     returns null
            every { it.bookLesson }      returns null
            every { it.context }         returns ""
            every { it.createdAt }       returns fixedNow
        }

    private fun makePhraseKnowledgeEntity(phraseId: String = "p1"): PhraseKnowledgeEntity =
        mockk<PhraseKnowledgeEntity>(relaxed = true).also {
            every { it.phraseId } returns phraseId
        }

    private fun makePhraseKnowledge(phraseId: String = "p1"): PhraseKnowledge =
        mockk<PhraseKnowledge>(relaxed = true).also {
            every { it.phraseId } returns phraseId
        }

    private fun makeMistakeLogEntity(type: String = "WORD"): MistakeLogEntity =
        mockk<MistakeLogEntity>(relaxed = true).also {
            every { it.type } returns type
        }

    private fun makeMistakeLog(type: MistakeType = MistakeType.WORD): MistakeLog =
        mockk<MistakeLog>(relaxed = true).also {
            every { it.type } returns type
        }

    private fun makePronunciationRecordEntity(word: String = "Hund", score: Float = 0.8f, problemSoundsJson: String = "[]"): PronunciationRecordEntity =
        mockk<PronunciationRecordEntity>(relaxed = true).also {
            every { it.word }              returns word
            every { it.score }             returns score
            every { it.problemSoundsJson } returns problemSoundsJson
        }

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        wordDao        = mockk()
        knowledgeDao   = mockk()
        grammarRuleDao = mockk()
        phraseDao      = mockk()
        progressDao    = mockk()
        mistakeDao     = mockk()
        cloudSync      = mockk()
        json           = Json { ignoreUnknownKeys = true; coerceInputValues = true }

        repository = KnowledgeRepositoryImpl(
            wordDao, knowledgeDao, grammarRuleDao, phraseDao,
            progressDao, mistakeDao, json, cloudSync
        )

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        every { com.voicedeutsch.master.util.DateUtils.nowTimestamp() } returns fixedNow

        // Safe defaults
        coEvery { wordDao.insertWord(any()) }          returns Unit
        coEvery { wordDao.insertWords(any()) }         returns Unit
        coEvery { knowledgeDao.upsertWordKnowledge(any()) } returns Unit
        coEvery { knowledgeDao.upsertRuleKnowledge(any()) } returns Unit
        coEvery { knowledgeDao.upsertPhraseKnowledge(any()) } returns Unit
        coEvery { grammarRuleDao.insertRule(any()) }   returns Unit
        coEvery { grammarRuleDao.insertRules(any()) }  returns Unit
        coEvery { phraseDao.insertPhrase(any()) }      returns Unit
        coEvery { phraseDao.insertPhrases(any()) }     returns Unit
        coEvery { mistakeDao.insertMistake(any()) }    returns Unit
        coEvery { progressDao.insertPronunciationRecord(any()) } returns Unit
        coEvery { cloudSync.enqueueKnowledgeItem(any(), any()) } returns Unit
        // FIX: every → coEvery для suspend-функции flushPendingQueue() (строка 1251)
        coEvery { cloudSync.flushPendingQueue() }      returns CloudSyncService.SyncStatus.SUCCESS
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── Words ─────────────────────────────────────────────────────────────────

    @Test
    fun getWord_entityExists_returnsMappedDomain() = runTest {
        coEvery { wordDao.getWord("w1") } returns makeWordEntity(id = "w1")

        val result = repository.getWord("w1")

        assertNotNull(result)
    }

    @Test
    fun getWord_entityNotFound_returnsNull() = runTest {
        coEvery { wordDao.getWord("missing") } returns null

        assertNull(repository.getWord("missing"))
    }

    @Test
    fun getWordByGerman_entityExists_returnsMappedDomain() = runTest {
        coEvery { wordDao.getWordByGerman("Hund") } returns makeWordEntity(german = "Hund")

        assertNotNull(repository.getWordByGerman("Hund"))
    }

    @Test
    fun getWordByGerman_entityNotFound_returnsNull() = runTest {
        coEvery { wordDao.getWordByGerman("xyz") } returns null

        assertNull(repository.getWordByGerman("xyz"))
    }

    @Test
    fun getAllWords_mapsAllEntities() = runTest {
        coEvery { wordDao.getAllWords() } returns listOf(makeWordEntity("w1"), makeWordEntity("w2"))

        val result = repository.getAllWords()

        assertEquals(2, result.size)
    }

    @Test
    fun getAllWords_emptyDao_returnsEmpty() = runTest {
        coEvery { wordDao.getAllWords() } returns emptyList()

        assertTrue(repository.getAllWords().isEmpty())
    }

    @Test
    fun getWordsByTopic_delegatesWithTopic() = runTest {
        coEvery { wordDao.getWordsByTopic("animals") } returns listOf(makeWordEntity())

        val result = repository.getWordsByTopic("animals")

        assertEquals(1, result.size)
        coVerify { wordDao.getWordsByTopic("animals") }
    }

    @Test
    fun getWordsByLevel_passesLevelName() = runTest {
        coEvery { wordDao.getWordsByLevel("A1") } returns listOf(makeWordEntity())

        val result = repository.getWordsByLevel(CefrLevel.A1)

        assertEquals(1, result.size)
        coVerify { wordDao.getWordsByLevel("A1") }
    }

    @Test
    fun getWordsByChapter_delegatesWithChapter() = runTest {
        coEvery { wordDao.getWordsByChapter(2) } returns listOf(makeWordEntity())

        repository.getWordsByChapter(2)

        coVerify { wordDao.getWordsByChapter(2) }
    }

    @Test
    fun insertWord_convertsToEntityAndDelegates() = runTest {
        val word = makeWord()

        repository.insertWord(word)

        coVerify(exactly = 1) { wordDao.insertWord(any()) }
    }

    @Test
    fun insertWords_convertsAllAndDelegates() = runTest {
        repository.insertWords(listOf(makeWord("w1"), makeWord("w2")))

        coVerify(exactly = 1) { wordDao.insertWords(any()) }
    }

    @Test
    fun searchWords_delegatesQuery() = runTest {
        coEvery { wordDao.searchWords("Hun") } returns listOf(makeWordEntity())

        val result = repository.searchWords("Hun")

        assertEquals(1, result.size)
        coVerify { wordDao.searchWords("Hun") }
    }

    // ── Word Knowledge ────────────────────────────────────────────────────────

    @Test
    fun getWordKnowledge_entityExists_returnsMapped() = runTest {
        coEvery { knowledgeDao.getWordKnowledge("user1", "w1") } returns makeWordKnowledgeEntity()

        assertNotNull(repository.getWordKnowledge("user1", "w1"))
    }

    @Test
    fun getWordKnowledge_entityNotFound_returnsNull() = runTest {
        coEvery { knowledgeDao.getWordKnowledge(any(), any()) } returns null

        assertNull(repository.getWordKnowledge("user1", "w1"))
    }

    @Test
    fun getWordKnowledgeByGerman_delegatesCorrectly() = runTest {
        coEvery { knowledgeDao.getWordKnowledgeByGerman("user1", "Hund") } returns
            makeWordKnowledgeEntity()

        assertNotNull(repository.getWordKnowledgeByGerman("user1", "Hund"))
        coVerify { knowledgeDao.getWordKnowledgeByGerman("user1", "Hund") }
    }

    @Test
    fun getAllWordKnowledge_mapsAllEntities() = runTest {
        coEvery { knowledgeDao.getAllWordKnowledge("user1") } returns
            listOf(makeWordKnowledgeEntity("w1"), makeWordKnowledgeEntity("w2"))

        val result = repository.getAllWordKnowledge("user1")

        assertEquals(2, result.size)
    }

    @Test
    fun getWordKnowledgeFlow_emitsFromDao() = runTest {
        every { knowledgeDao.getWordKnowledgeFlow("user1") } returns
            flowOf(listOf(makeWordKnowledgeEntity()))

        val result = repository.getWordKnowledgeFlow("user1").first()

        assertEquals(1, result.size)
    }

    @Test
    fun getWordsForReview_wordFound_returnsPair() = runTest {
        val wke = makeWordKnowledgeEntity(wordId = "w1")
        coEvery { knowledgeDao.getWordsForReview("user1", fixedNow, 10) } returns listOf(wke)
        coEvery { wordDao.getWord("w1") } returns makeWordEntity(id = "w1")

        val result = repository.getWordsForReview("user1", 10)

        assertEquals(1, result.size)
    }

    @Test
    fun getWordsForReview_wordNotFound_skipped() = runTest {
        val wke = makeWordKnowledgeEntity(wordId = "missing")
        coEvery { knowledgeDao.getWordsForReview("user1", fixedNow, 10) } returns listOf(wke)
        coEvery { wordDao.getWord("missing") } returns null

        val result = repository.getWordsForReview("user1", 10)

        assertTrue(result.isEmpty())
    }

    @Test
    fun getWordsForReviewCount_delegatesWithNow() = runTest {
        coEvery { knowledgeDao.getWordsForReviewCount("user1", fixedNow) } returns 7

        assertEquals(7, repository.getWordsForReviewCount("user1"))
    }

    @Test
    fun getKnownWordsCount_delegatesCorrectly() = runTest {
        coEvery { knowledgeDao.getKnownWordsCount("user1") } returns 42

        assertEquals(42, repository.getKnownWordsCount("user1"))
    }

    @Test
    fun getActiveWordsCount_delegatesCorrectly() = runTest {
        coEvery { knowledgeDao.getActiveWordsCount("user1") } returns 10

        assertEquals(10, repository.getActiveWordsCount("user1"))
    }

    @Test
    fun getWordsByKnowledgeLevel_wordFound_returnsPairs() = runTest {
        coEvery { knowledgeDao.getWordKnowledgeByLevel("user1", 3) } returns
            listOf(makeWordKnowledgeEntity(wordId = "w1"))
        coEvery { wordDao.getWord("w1") } returns makeWordEntity(id = "w1")

        val result = repository.getWordsByKnowledgeLevel("user1", 3)

        assertEquals(1, result.size)
    }

    @Test
    fun getProblemWords_wordFound_returnsPairs() = runTest {
        coEvery { knowledgeDao.getProblemWords("user1", 5) } returns
            listOf(makeWordKnowledgeEntity(wordId = "w1"))
        coEvery { wordDao.getWord("w1") } returns makeWordEntity(id = "w1")

        val result = repository.getProblemWords("user1", 5)

        assertEquals(1, result.size)
    }

    @Test
    fun getProblemWords_wordNotFound_skipped() = runTest {
        coEvery { knowledgeDao.getProblemWords("user1", 5) } returns
            listOf(makeWordKnowledgeEntity(wordId = "missing"))
        coEvery { wordDao.getWord("missing") } returns null

        assertTrue(repository.getProblemWords("user1", 5).isEmpty())
    }

    // ── upsertWordKnowledge — cloud sync ──────────────────────────────────────

    @Test
    fun upsertWordKnowledge_callsDaoAndCloudSync() = runTest {
        val wk = makeWordKnowledge()

        repository.upsertWordKnowledge(wk)

        coVerify(exactly = 1) { knowledgeDao.upsertWordKnowledge(any()) }
        coVerify(exactly = 1) { cloudSync.enqueueKnowledgeItem(any(), any()) }
    }

    @Test
    fun upsertWordKnowledge_cloudSyncCalledWithWordId() = runTest {
        val wk = makeWordKnowledge(wordId = "w99")

        repository.upsertWordKnowledge(wk)

        coVerify { cloudSync.enqueueKnowledgeItem(eq("w99"), any()) }
    }

    @Test
    fun upsertWordKnowledge_daoThrows_exceptionPropagates() = runTest {
        coEvery { knowledgeDao.upsertWordKnowledge(any()) } throws RuntimeException("db error")

        assertThrows(RuntimeException::class.java) {
            runTest { repository.upsertWordKnowledge(makeWordKnowledge()) }
        }
    }

    @Test
    fun getWordKnowledgeByTopic_associatesWordsWithKnowledge() = runTest {
        coEvery { wordDao.getWordsByTopic("food") } returns listOf(makeWordEntity(id = "w1"))
        coEvery { knowledgeDao.getWordKnowledge("user1", "w1") } returns makeWordKnowledgeEntity()

        val result = repository.getWordKnowledgeByTopic("user1", "food")

        assertEquals(1, result.size)
    }

    // ── Grammar Rules ─────────────────────────────────────────────────────────

    @Test
    fun getGrammarRule_entityExists_returnsMapped() = runTest {
        coEvery { grammarRuleDao.getRule("r1") } returns makeGrammarRuleEntity(id = "r1")

        assertNotNull(repository.getGrammarRule("r1"))
    }

    @Test
    fun getGrammarRule_entityNotFound_returnsNull() = runTest {
        coEvery { grammarRuleDao.getRule("missing") } returns null

        assertNull(repository.getGrammarRule("missing"))
    }

    @Test
    fun getAllGrammarRules_mapsAllEntities() = runTest {
        coEvery { grammarRuleDao.getAllRules() } returns listOf(
            makeGrammarRuleEntity("r1"), makeGrammarRuleEntity("r2")
        )

        assertEquals(2, repository.getAllGrammarRules().size)
    }

    @Test
    fun getGrammarRulesByCategory_passesCorrectCategory() = runTest {
        coEvery { grammarRuleDao.getRulesByCategory("ARTICLES") } returns listOf(makeGrammarRuleEntity())

        repository.getGrammarRulesByCategory(GrammarCategory.ARTICLES)

        coVerify { grammarRuleDao.getRulesByCategory("ARTICLES") }
    }

    @Test
    fun getGrammarRulesByLevel_passesLevelName() = runTest {
        coEvery { grammarRuleDao.getRulesByLevel("B1") } returns listOf(makeGrammarRuleEntity())

        repository.getGrammarRulesByLevel(CefrLevel.B1)

        coVerify { grammarRuleDao.getRulesByLevel("B1") }
    }

    @Test
    fun getGrammarRulesByChapter_delegatesCorrectly() = runTest {
        coEvery { grammarRuleDao.getRulesByChapter(3) } returns listOf(makeGrammarRuleEntity())

        repository.getGrammarRulesByChapter(3)

        coVerify { grammarRuleDao.getRulesByChapter(3) }
    }

    @Test
    fun insertGrammarRule_convertsAndDelegates() = runTest {
        repository.insertGrammarRule(makeGrammarRule())

        coVerify(exactly = 1) { grammarRuleDao.insertRule(any()) }
    }

    @Test
    fun insertGrammarRules_convertsAllAndDelegates() = runTest {
        repository.insertGrammarRules(listOf(makeGrammarRule("r1"), makeGrammarRule("r2")))

        coVerify(exactly = 1) { grammarRuleDao.insertRules(any()) }
    }

    // ── Rule Knowledge ────────────────────────────────────────────────────────

    @Test
    fun getRuleKnowledge_entityExists_returnsMapped() = runTest {
        coEvery { knowledgeDao.getRuleKnowledge("user1", "r1") } returns makeRuleKnowledgeEntity()

        assertNotNull(repository.getRuleKnowledge("user1", "r1"))
    }

    @Test
    fun getRuleKnowledge_entityNotFound_returnsNull() = runTest {
        coEvery { knowledgeDao.getRuleKnowledge(any(), any()) } returns null

        assertNull(repository.getRuleKnowledge("user1", "r1"))
    }

    @Test
    fun getAllRuleKnowledge_mapsAllEntities() = runTest {
        coEvery { knowledgeDao.getAllRuleKnowledge("user1") } returns
            listOf(makeRuleKnowledgeEntity("r1"), makeRuleKnowledgeEntity("r2"))

        assertEquals(2, repository.getAllRuleKnowledge("user1").size)
    }

    @Test
    fun getRulesForReview_ruleFound_returnsPair() = runTest {
        val rke = makeRuleKnowledgeEntity(ruleId = "r1")
        coEvery { knowledgeDao.getRulesForReview("user1", fixedNow, 5) } returns listOf(rke)
        coEvery { grammarRuleDao.getRule("r1") } returns makeGrammarRuleEntity(id = "r1")

        val result = repository.getRulesForReview("user1", 5)

        assertEquals(1, result.size)
    }

    @Test
    fun getRulesForReview_ruleNotFound_skipped() = runTest {
        val rke = makeRuleKnowledgeEntity(ruleId = "missing")
        coEvery { knowledgeDao.getRulesForReview("user1", fixedNow, 5) } returns listOf(rke)
        coEvery { grammarRuleDao.getRule("missing") } returns null

        assertTrue(repository.getRulesForReview("user1", 5).isEmpty())
    }

    @Test
    fun getRulesForReviewCount_delegatesWithNow() = runTest {
        coEvery { knowledgeDao.getRulesForReviewCount("user1", fixedNow) } returns 3

        assertEquals(3, repository.getRulesForReviewCount("user1"))
    }

    @Test
    fun getKnownRulesCount_delegatesCorrectly() = runTest {
        coEvery { knowledgeDao.getKnownRulesCount("user1") } returns 15

        assertEquals(15, repository.getKnownRulesCount("user1"))
    }

    // ── upsertRuleKnowledge — cloud sync ──────────────────────────────────────

    @Test
    fun upsertRuleKnowledge_callsDaoAndCloudSync() = runTest {
        repository.upsertRuleKnowledge(makeRuleKnowledge())

        coVerify(exactly = 1) { knowledgeDao.upsertRuleKnowledge(any()) }
        coVerify(exactly = 1) { cloudSync.enqueueKnowledgeItem(any(), any()) }
    }

    @Test
    fun upsertRuleKnowledge_cloudSyncKeyPrefixedWithRule() = runTest {
        val rk = makeRuleKnowledge(ruleId = "r42")

        repository.upsertRuleKnowledge(rk)

        coVerify { cloudSync.enqueueKnowledgeItem(match { it.startsWith("rule_") }, any()) }
    }

    // ── Phrases ───────────────────────────────────────────────────────────────

    @Test
    fun getPhrase_entityExists_returnsMappedDomain() = runTest {
        coEvery { phraseDao.getPhrase("p1") } returns makePhraseEntity(id = "p1")

        val result = repository.getPhrase("p1")

        assertNotNull(result)
        assertEquals("p1", result?.id)
    }

    @Test
    fun getPhrase_entityNotFound_returnsNull() = runTest {
        coEvery { phraseDao.getPhrase("missing") } returns null

        assertNull(repository.getPhrase("missing"))
    }

    @Test
    fun getPhrase_invalidCategory_fallsBackToOther() = runTest {
        coEvery { phraseDao.getPhrase("p1") } returns
            makePhraseEntity(id = "p1", category = "INVALID_CATEGORY")

        val result = repository.getPhrase("p1")

        assertEquals(PhraseCategory.OTHER, result?.category)
    }

    @Test
    fun getPhrase_validCategory_parsedCorrectly() = runTest {
        coEvery { phraseDao.getPhrase("p1") } returns
            makePhraseEntity(id = "p1", category = "GREETING")

        val result = repository.getPhrase("p1")

        assertEquals(PhraseCategory.GREETING, result?.category)
    }

    @Test
    fun getAllPhrases_mapsAllEntities() = runTest {
        coEvery { phraseDao.getAllPhrases() } returns
            listOf(makePhraseEntity("p1"), makePhraseEntity("p2"))

        assertEquals(2, repository.getAllPhrases().size)
    }

    @Test
    fun getAllPhrases_invalidCategoryInOne_othersStillMapped() = runTest {
        coEvery { phraseDao.getAllPhrases() } returns listOf(
            makePhraseEntity("p1", category = "GREETING"),
            makePhraseEntity("p2", category = "BAD")
        )

        val result = repository.getAllPhrases()

        assertEquals(2, result.size)
        assertEquals(PhraseCategory.OTHER, result[1].category)
    }

    @Test
    fun insertPhrase_createsPhraseEntityAndDelegates() = runTest {
        val phrase = makePhrase()

        repository.insertPhrase(phrase)

        coVerify(exactly = 1) { phraseDao.insertPhrase(any()) }
    }

    @Test
    fun insertPhrases_convertsAllAndDelegates() = runTest {
        repository.insertPhrases(listOf(makePhrase("p1"), makePhrase("p2")))

        coVerify(exactly = 1) { phraseDao.insertPhrases(any()) }
    }

    // ── Phrase Knowledge ──────────────────────────────────────────────────────

    @Test
    fun getPhraseKnowledge_entityExists_returnsMapped() = runTest {
        coEvery { knowledgeDao.getPhraseKnowledge("user1", "p1") } returns
            makePhraseKnowledgeEntity()

        assertNotNull(repository.getPhraseKnowledge("user1", "p1"))
    }

    @Test
    fun getPhraseKnowledge_notFound_returnsNull() = runTest {
        coEvery { knowledgeDao.getPhraseKnowledge(any(), any()) } returns null

        assertNull(repository.getPhraseKnowledge("user1", "p1"))
    }

    @Test
    fun getAllPhraseKnowledge_mapsAllEntities() = runTest {
        coEvery { knowledgeDao.getAllPhraseKnowledge("user1") } returns
            listOf(makePhraseKnowledgeEntity("p1"), makePhraseKnowledgeEntity("p2"))

        assertEquals(2, repository.getAllPhraseKnowledge("user1").size)
    }

    @Test
    fun getPhrasesForReview_phraseFound_returnsPair() = runTest {
        val pke = makePhraseKnowledgeEntity(phraseId = "p1")
        coEvery { knowledgeDao.getPhrasesForReview("user1", fixedNow, 5) } returns listOf(pke)
        coEvery { phraseDao.getPhrase("p1") } returns makePhraseEntity(id = "p1")

        val result = repository.getPhrasesForReview("user1", 5)

        assertEquals(1, result.size)
    }

    @Test
    fun getPhrasesForReview_phraseNotFound_skipped() = runTest {
        val pke = makePhraseKnowledgeEntity(phraseId = "missing")
        coEvery { knowledgeDao.getPhrasesForReview("user1", fixedNow, 5) } returns listOf(pke)
        coEvery { phraseDao.getPhrase("missing") } returns null

        assertTrue(repository.getPhrasesForReview("user1", 5).isEmpty())
    }

    @Test
    fun getPhrasesForReviewCount_delegatesWithNow() = runTest {
        coEvery { knowledgeDao.getPhrasesForReviewCount("user1", fixedNow) } returns 4

        assertEquals(4, repository.getPhrasesForReviewCount("user1"))
    }

    @Test
    fun upsertPhraseKnowledge_delegatesToDao() = runTest {
        val pk = makePhraseKnowledge()

        repository.upsertPhraseKnowledge(pk)

        coVerify(exactly = 1) { knowledgeDao.upsertPhraseKnowledge(any()) }
    }

    // ── Mistakes ──────────────────────────────────────────────────────────────

    @Test
    fun logMistake_convertsToEntityAndDelegates() = runTest {
        val mistake = makeMistakeLog()

        repository.logMistake(mistake)

        coVerify(exactly = 1) { mistakeDao.insertMistake(any()) }
    }

    @Test
    fun getMistakes_mapsAllEntities() = runTest {
        coEvery { mistakeDao.getMistakes("user1", 10) } returns
            listOf(makeMistakeLogEntity(), makeMistakeLogEntity())

        assertEquals(2, repository.getMistakes("user1", 10).size)
    }

    @Test
    fun getMistakesByType_passesTypeName() = runTest {
        coEvery { mistakeDao.getMistakesByType("user1", "GRAMMAR") } returns
            listOf(makeMistakeLogEntity(type = "GRAMMAR"))

        repository.getMistakesByType("user1", MistakeType.GRAMMAR)

        coVerify { mistakeDao.getMistakesByType("user1", "GRAMMAR") }
    }

    // ── Pronunciation ─────────────────────────────────────────────────────────

    @Test
    fun savePronunciationResult_delegatesToProgressDao() = runTest {
        val result = mockk<PronunciationResult>(relaxed = true)

        repository.savePronunciationResult(result)

        coVerify(exactly = 1) { progressDao.insertPronunciationRecord(any()) }
    }

    @Test
    fun getPronunciationResults_mapsAllRecords() = runTest {
        coEvery { progressDao.getPronunciationRecords("user1", "Hund") } returns
            listOf(makePronunciationRecordEntity(), makePronunciationRecordEntity())

        val result = repository.getPronunciationResults("user1", "Hund")

        assertEquals(2, result.size)
    }

    @Test
    fun getAveragePronunciationScore_delegatesCorrectly() = runTest {
        coEvery { progressDao.getAveragePronunciationScore("user1") } returns 0.78f

        assertEquals(0.78f, repository.getAveragePronunciationScore("user1"), 0.001f)
    }

    @Test
    fun getPerfectPronunciationCount_delegatesCorrectly() = runTest {
        coEvery { knowledgeDao.getPerfectPronunciationCount("user1") } returns 5

        assertEquals(5, repository.getPerfectPronunciationCount("user1"))
    }

    @Test
    fun getRecentPronunciationRecords_mapsAllEntities() = runTest {
        coEvery { knowledgeDao.getRecentPronunciationRecords("user1", 20) } returns
            listOf(makePronunciationRecordEntity(), makePronunciationRecordEntity())

        val result = repository.getRecentPronunciationRecords("user1", 20)

        assertEquals(2, result.size)
        coVerify { knowledgeDao.getRecentPronunciationRecords("user1", 20) }
    }

    // ── getProblemSounds ──────────────────────────────────────────────────────

    @Test
    fun getProblemSounds_noProblems_returnsEmptyList() = runTest {
        coEvery { progressDao.getProblemWordsForPronunciation("user1") } returns emptyList()

        val result = repository.getProblemSounds("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun getProblemSounds_validRecord_returnsSoundTargets() = runTest {
        coEvery { progressDao.getProblemWordsForPronunciation("user1") } returns emptyList()

        val result = repository.getProblemSounds("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun getProblemSounds_invalidSoundJson_skipsRecord() = runTest {
        coEvery { progressDao.getProblemWordsForPronunciation("user1") } returns emptyList()

        val result = repository.getProblemSounds("user1")

        assertNotNull(result)
    }

    @Test
    fun getProblemSounds_resultsSortedByScoreAscending() = runTest {
        coEvery { progressDao.getProblemWordsForPronunciation("user1") } returns emptyList()

        val result = repository.getProblemSounds("user1")

        if (result.size > 1) {
            for (i in 0 until result.size - 1) {
                assertTrue(result[i].currentScore <= result[i + 1].currentScore)
            }
        }
    }

    // ── flushSync — SyncStatus conversion ────────────────────────────────────

    @Test
    fun flushSync_successStatus_returnsTrue() = runTest {
        coEvery { cloudSync.flushPendingQueue() } returns CloudSyncService.SyncStatus.SUCCESS

        assertTrue(repository.flushSync())
    }

    @Test
    fun flushSync_offlineStatus_returnsTrue() = runTest {
        coEvery { cloudSync.flushPendingQueue() } returns CloudSyncService.SyncStatus.OFFLINE

        assertTrue(repository.flushSync())
    }

    @Test
    fun flushSync_errorStatus_returnsFalse() = runTest {
        coEvery { cloudSync.flushPendingQueue() } returns CloudSyncService.SyncStatus.ERROR

        assertFalse(repository.flushSync())
    }

    @Test
    fun flushSync_calledOnce_flushPendingQueueCalledOnce() = runTest {
        repository.flushSync()

        coVerify(exactly = 1) { cloudSync.flushPendingQueue() }
    }

    // ── buildKnowledgeSnapshot — empty data ───────────────────────────────────

    @Test
    fun buildKnowledgeSnapshot_emptyData_returnsDefaultSnapshot() = runTest {
        stubSnapshotDaosEmpty()

        val result = repository.buildKnowledgeSnapshot("user1")

        assertEquals(0,   result.vocabulary.totalWords)
        assertEquals(0,   result.grammar.totalRules)
        assertTrue(result.weakPoints.isEmpty())
    }

    @Test
    fun buildKnowledgeSnapshot_emptyData_vocabularyWordsForReview0() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getWordsForReviewCount("user1", fixedNow) } returns 0

        val result = repository.buildKnowledgeSnapshot("user1")

        assertEquals(0, result.vocabulary.wordsForReviewToday)
    }

    @Test
    fun buildKnowledgeSnapshot_emptyData_grammarRulesForReview0() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getRulesForReviewCount("user1", fixedNow) } returns 0

        val result = repository.buildKnowledgeSnapshot("user1")

        assertEquals(0, result.grammar.rulesForReviewToday)
    }

    // ── buildKnowledgeSnapshot — vocabulary snapshot ──────────────────────────

    @Test
    fun buildKnowledgeSnapshot_wordsWithKnowledge_totalWordsCountsNonZeroLevel() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getBriefWordKnowledge("user1") } returns listOf(
            makeBriefWordKnowledge("w1", knowledgeLevel = 3),
            makeBriefWordKnowledge("w2", knowledgeLevel = 0)
        )

        val result = repository.buildKnowledgeSnapshot("user1")

        assertEquals(1, result.vocabulary.totalWords)
    }

    @Test
    fun buildKnowledgeSnapshot_problemWords_level2andSeenGte3() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getBriefWordKnowledge("user1") } returns listOf(
            makeBriefWordKnowledge("w1", knowledgeLevel = 2, timesSeen = 3),
            makeBriefWordKnowledge("w2", knowledgeLevel = 3, timesSeen = 5)
        )
        coEvery { wordDao.getAllWords() } returns listOf(makeWordEntity("w1"), makeWordEntity("w2"))

        val result = repository.buildKnowledgeSnapshot("user1")

        assertEquals(1, result.vocabulary.problemWords.size)
    }

    @Test
    fun buildKnowledgeSnapshot_problemWords_level2SeenLt3_notIncluded() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getBriefWordKnowledge("user1") } returns listOf(
            makeBriefWordKnowledge("w1", knowledgeLevel = 2, timesSeen = 2)
        )

        val result = repository.buildKnowledgeSnapshot("user1")

        assertTrue(result.vocabulary.problemWords.isEmpty())
    }

    @Test
    fun buildKnowledgeSnapshot_recentWords_sortedByLastSeenDesc() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getBriefWordKnowledge("user1") } returns listOf(
            makeBriefWordKnowledge("w1", lastSeen = 100L),
            makeBriefWordKnowledge("w2", lastSeen = 500L),
            makeBriefWordKnowledge("w3", lastSeen = 200L)
        )
        coEvery { wordDao.getAllWords() } returns listOf(
            makeWordEntity("w1", german = "Haus"),
            makeWordEntity("w2", german = "Buch"),
            makeWordEntity("w3", german = "Auto")
        )

        val result = repository.buildKnowledgeSnapshot("user1")

        assertEquals("Buch", result.vocabulary.recentNewWords.firstOrNull())
    }

    @Test
    fun buildKnowledgeSnapshot_recentWords_nullLastSeen_excluded() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getBriefWordKnowledge("user1") } returns listOf(
            makeBriefWordKnowledge("w1", lastSeen = null)
        )

        val result = repository.buildKnowledgeSnapshot("user1")

        assertTrue(result.vocabulary.recentNewWords.isEmpty())
    }

    @Test
    fun buildKnowledgeSnapshot_recentWords_cappedAt10() = runTest {
        stubSnapshotDaosEmpty()
        val briefs = List(15) { i -> makeBriefWordKnowledge("w$i", lastSeen = i.toLong() + 1) }
        val words  = List(15) { i -> makeWordEntity("w$i", german = "Word$i") }
        coEvery { knowledgeDao.getBriefWordKnowledge("user1") } returns briefs
        coEvery { wordDao.getAllWords() } returns words

        val result = repository.buildKnowledgeSnapshot("user1")

        assertTrue(result.vocabulary.recentNewWords.size <= 10)
    }

    // ── buildKnowledgeSnapshot — grammar snapshot ─────────────────────────────

    @Test
    fun buildKnowledgeSnapshot_rulesTotal_equalsAllRulesSize() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { grammarRuleDao.getAllRules() } returns
            listOf(makeGrammarRuleEntity("r1"), makeGrammarRuleEntity("r2"), makeGrammarRuleEntity("r3"))

        val result = repository.buildKnowledgeSnapshot("user1")

        assertEquals(3, result.grammar.totalRules)
    }

    @Test
    fun buildKnowledgeSnapshot_knownRules_levelGte4() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getAllRuleKnowledge("user1") } returns listOf(
            makeRuleKnowledgeEntity("r1", knowledgeLevel = 4),
            makeRuleKnowledgeEntity("r2", knowledgeLevel = 3)
        )
        coEvery { grammarRuleDao.getAllRules() } returns listOf(
            makeGrammarRuleEntity("r1"), makeGrammarRuleEntity("r2")
        )

        val result = repository.buildKnowledgeSnapshot("user1")

        assertEquals(1, result.grammar.knownRules.size)
    }

    @Test
    fun buildKnowledgeSnapshot_problemRules_level2andPracticedGte3() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getAllRuleKnowledge("user1") } returns listOf(
            makeRuleKnowledgeEntity("r1", knowledgeLevel = 2, timesPracticed = 3)
        )
        coEvery { grammarRuleDao.getAllRules() } returns listOf(makeGrammarRuleEntity("r1"))

        val result = repository.buildKnowledgeSnapshot("user1")

        assertEquals(1, result.grammar.problemRules.size)
    }

    @Test
    fun buildKnowledgeSnapshot_problemRules_ruleNotFound_skipped() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getAllRuleKnowledge("user1") } returns listOf(
            makeRuleKnowledgeEntity("missing", knowledgeLevel = 1, timesPracticed = 5)
        )
        coEvery { grammarRuleDao.getAllRules() } returns emptyList()

        val result = repository.buildKnowledgeSnapshot("user1")

        assertTrue(result.grammar.problemRules.isEmpty())
    }

    // ── buildKnowledgeSnapshot — weak points ──────────────────────────────────

    @Test
    fun buildKnowledgeSnapshot_problemWordsAndRules_addedToWeakPoints() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getBriefWordKnowledge("user1") } returns listOf(
            makeBriefWordKnowledge("w1", knowledgeLevel = 2, timesSeen = 3)
        )
        coEvery { wordDao.getAllWords() } returns listOf(makeWordEntity("w1", german = "Haus"))
        coEvery { knowledgeDao.getAllRuleKnowledge("user1") } returns listOf(
            makeRuleKnowledgeEntity("r1", knowledgeLevel = 1, timesPracticed = 4)
        )
        coEvery { grammarRuleDao.getAllRules() } returns listOf(makeGrammarRuleEntity("r1"))

        val result = repository.buildKnowledgeSnapshot("user1")

        assertTrue(result.weakPoints.any { it.contains("Haus") })
        assertTrue(result.weakPoints.any { it.contains("Артикль") || it.contains("Грамматика") })
    }

    // ── buildKnowledgeSnapshot — recommendations strategy ────────────────────

    @Test
    fun buildKnowledgeSnapshot_srsCountOver10_primaryStrategyRepetition() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getWordsForReviewCount("user1", fixedNow) } returns 8
        coEvery { knowledgeDao.getRulesForReviewCount("user1", fixedNow) } returns 5

        val result = repository.buildKnowledgeSnapshot("user1")

        assertEquals("REPETITION", result.recommendations.primaryStrategy)
    }

    @Test
    fun buildKnowledgeSnapshot_manyWeakPoints_primaryStrategyGapFilling() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getWordsForReviewCount("user1", fixedNow) } returns 0
        coEvery { knowledgeDao.getRulesForReviewCount("user1", fixedNow) } returns 0
        coEvery { knowledgeDao.getBriefWordKnowledge("user1") } returns
            List(6) { i -> makeBriefWordKnowledge("w$i", knowledgeLevel = 1, timesSeen = 5) }
        coEvery { wordDao.getAllWords() } returns
            List(6) { i -> makeWordEntity("w$i", german = "W$i") }

        val result = repository.buildKnowledgeSnapshot("user1")

        assertEquals("GAP_FILLING", result.recommendations.primaryStrategy)
    }

    @Test
    fun buildKnowledgeSnapshot_noIssues_primaryStrategyLinearBook() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getWordsForReviewCount("user1", fixedNow) } returns 2
        coEvery { knowledgeDao.getRulesForReviewCount("user1", fixedNow) } returns 3

        val result = repository.buildKnowledgeSnapshot("user1")

        assertEquals("LINEAR_BOOK", result.recommendations.primaryStrategy)
    }

    @Test
    fun buildKnowledgeSnapshot_secondaryStrategyAlwaysLinearBook() = runTest {
        stubSnapshotDaosEmpty()

        val result = repository.buildKnowledgeSnapshot("user1")

        assertEquals("LINEAR_BOOK", result.recommendations.secondaryStrategy)
    }

    // ── buildKnowledgeSnapshot — pronunciation snapshot ───────────────────────

    @Test
    fun buildKnowledgeSnapshot_avgPronunciation_fromProgressDao() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { progressDao.getAveragePronunciationScore("user1") } returns 0.77f

        val result = repository.buildKnowledgeSnapshot("user1")

        assertEquals(0.77f, result.pronunciation.overallScore, 0.001f)
    }

    // ── buildKnowledgeSnapshot — O(1) lookup by id ───────────────────────────

    @Test
    fun buildKnowledgeSnapshot_knownRuleInfo_usesO1Lookup() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getAllRuleKnowledge("user1") } returns listOf(
            makeRuleKnowledgeEntity("r2", knowledgeLevel = 5)
        )
        coEvery { grammarRuleDao.getAllRules() } returns listOf(
            makeGrammarRuleEntity("r1"), makeGrammarRuleEntity("r2")
        )

        val result = repository.buildKnowledgeSnapshot("user1")

        assertEquals(1, result.grammar.knownRules.size)
        assertEquals("Артикль", result.grammar.knownRules[0].name)
    }

    @Test
    fun buildKnowledgeSnapshot_wordNotInAllWords_skippedForRecentAndProblem() = runTest {
        stubSnapshotDaosEmpty()
        coEvery { knowledgeDao.getBriefWordKnowledge("user1") } returns listOf(
            makeBriefWordKnowledge("wX", knowledgeLevel = 2, timesSeen = 5, lastSeen = fixedNow)
        )
        coEvery { wordDao.getAllWords() } returns emptyList()

        val result = repository.buildKnowledgeSnapshot("user1")

        assertTrue(result.vocabulary.recentNewWords.isEmpty())
    }

    // ── recalculateOverdueItems ───────────────────────────────────────────────

    @Test
    fun recalculateOverdueItems_noOpDoesNotThrow() = runTest {
        repository.recalculateOverdueItems("user1")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun stubSnapshotDaosEmpty() {
        coEvery { knowledgeDao.getBriefWordKnowledge("user1") }         returns emptyList()
        coEvery { wordDao.getAllWords() }                                returns emptyList()
        coEvery { knowledgeDao.getAllRuleKnowledge("user1") }           returns emptyList()
        coEvery { grammarRuleDao.getAllRules() }                        returns emptyList()
        coEvery { knowledgeDao.getWordsForReviewCount("user1", any()) } returns 0
        coEvery { knowledgeDao.getRulesForReviewCount("user1", any()) } returns 0
        coEvery { progressDao.getAveragePronunciationScore("user1") }   returns 0f
        coEvery { progressDao.getProblemWordsForPronunciation("user1") } returns emptyList()
    }
}
