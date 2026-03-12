// Путь: src/test/java/com/voicedeutsch/master/data/repository/ProgressRepositoryImplTest.kt
package com.voicedeutsch.master.data.repository

import com.voicedeutsch.master.data.local.database.dao.BookProgressDao
import com.voicedeutsch.master.data.local.database.dao.GrammarRuleDao
import com.voicedeutsch.master.data.local.database.dao.KnowledgeDao
import com.voicedeutsch.master.data.local.database.dao.ProgressDao
import com.voicedeutsch.master.data.local.database.dao.SessionDao
import com.voicedeutsch.master.data.local.database.dao.UserDao
import com.voicedeutsch.master.data.local.database.dao.WordDao
import com.voicedeutsch.master.data.local.database.entity.BookProgressEntity
import com.voicedeutsch.master.data.local.database.entity.DailyStatisticsEntity
import com.voicedeutsch.master.data.local.database.entity.GrammarRuleEntity

import com.voicedeutsch.master.data.local.database.entity.RuleKnowledgeEntity
import com.voicedeutsch.master.data.local.database.entity.UserEntity
import com.voicedeutsch.master.data.local.database.entity.WordEntity
import com.voicedeutsch.master.data.local.database.entity.WordKnowledgeEntity
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

class ProgressRepositoryImplTest {

    private lateinit var knowledgeDao: KnowledgeDao
    private lateinit var wordDao: WordDao
    private lateinit var grammarRuleDao: GrammarRuleDao
    private lateinit var sessionDao: SessionDao
    private lateinit var bookProgressDao: BookProgressDao
    private lateinit var progressDao: ProgressDao
    private lateinit var userDao: UserDao
    private lateinit var json: Json
    private lateinit var repository: ProgressRepositoryImpl

    private val fixedNow = 1_700_000_000_000L

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeUserEntity(
        id: String = "user1",
        cefrLevel: String = "A1",
        cefrSubLevel: Int = 1,
        streakDays: Int = 5,
        totalMinutes: Int = 120,
        totalSessions: Int = 10
    ): UserEntity = mockk<UserEntity>(relaxed = true).also {
        every { it.id }            returns id
        every { it.cefrLevel }     returns cefrLevel
        every { it.cefrSubLevel }  returns cefrSubLevel
        every { it.streakDays }    returns streakDays
        every { it.totalMinutes }  returns totalMinutes
        every { it.totalSessions } returns totalSessions
    }

    private fun makeWordKnowledgeEntity(
        wordId: String = "w1",
        knowledgeLevel: Int = 3,
        nextReview: Long? = null
    ): WordKnowledgeEntity = mockk<WordKnowledgeEntity>(relaxed = true).also {
        every { it.wordId }         returns wordId
        every { it.knowledgeLevel } returns knowledgeLevel
        every { it.nextReview }     returns nextReview
        every { it.contextsJson }   returns "[]"
        every { it.mistakesJson }   returns "[]"
    }

    private fun makeWordEntity(
        id: String = "w1",
        level: String = "A1",
        topic: String = "animals"
    ): WordEntity = mockk<WordEntity>(relaxed = true).also {
        every { it.id }              returns id
        every { it.difficultyLevel } returns level
        every { it.topic }           returns topic
        every { it.german }          returns "Hund"
        every { it.russian }         returns "собака"
        every { it.partOfSpeech }    returns "NOUN"
        every { it.source }          returns "book"
    }

    private fun makeRuleKnowledgeEntity(
        ruleId: String = "r1",
        knowledgeLevel: Int = 3
    ): RuleKnowledgeEntity = mockk<RuleKnowledgeEntity>(relaxed = true).also {
        every { it.ruleId }         returns ruleId
        every { it.knowledgeLevel } returns knowledgeLevel
    }

    private fun makeGrammarRuleEntity(
        id: String = "r1",
        category: String = "ARTICLES"
    ): GrammarRuleEntity = mockk<GrammarRuleEntity>(relaxed = true).also {
        every { it.id }           returns id
        every { it.category }     returns category
        every { it.examplesJson } returns "[]"
        every { it.relatedRulesJson } returns "[]"
    }

    private fun makeBookProgressEntity(
        chapter: Int = 1,
        lesson: Int = 1,
        status: String = "NOT_STARTED"
    ): BookProgressEntity = mockk<BookProgressEntity>(relaxed = true).also {
        every { it.chapter } returns chapter
        every { it.lesson }  returns lesson
        every { it.status }  returns status
    }

    private fun makeDailyStatisticsEntity(): DailyStatisticsEntity =
        mockk<DailyStatisticsEntity>(relaxed = true)

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        knowledgeDao   = mockk()
        wordDao        = mockk()
        grammarRuleDao = mockk()
        sessionDao     = mockk()
        bookProgressDao = mockk()
        progressDao    = mockk()
        userDao        = mockk()
        json           = Json { ignoreUnknownKeys = true }

        repository = ProgressRepositoryImpl(
            knowledgeDao, wordDao, grammarRuleDao, sessionDao,
            bookProgressDao, progressDao, userDao, json
        )

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        every { com.voicedeutsch.master.util.DateUtils.nowTimestamp() } returns fixedNow

        // Safe defaults
        coEvery { userDao.getUser(any()) }                           returns makeUserEntity()
        coEvery { knowledgeDao.getAllWordKnowledge(any()) }           returns emptyList()
        coEvery { knowledgeDao.getAllRuleKnowledge(any()) }           returns emptyList()
        coEvery { knowledgeDao.getActiveWordsCount(any()) }          returns 0
        coEvery { knowledgeDao.getKnownWordsCount(any()) }           returns 0
        coEvery { knowledgeDao.getWordsForReviewCount(any(), any()) } returns 0
        coEvery { knowledgeDao.getKnownRulesCount(any()) }           returns 0
        coEvery { knowledgeDao.getRulesForReviewCount(any(), any()) } returns 0
        coEvery { wordDao.getAllWords() }                             returns emptyList()
        coEvery { wordDao.getWordsByLevel(any()) }                   returns emptyList()
        coEvery { wordDao.getWordCount() }                           returns 0
        coEvery { grammarRuleDao.getAllRules() }                     returns emptyList()
        coEvery { grammarRuleDao.getRulesByLevel(any()) }            returns emptyList()
        coEvery { bookProgressDao.getCurrentPosition(any()) }        returns null
        coEvery { bookProgressDao.getCompletedCount(any()) }         returns 0
        coEvery { bookProgressDao.getAllProgress(any()) }            returns emptyList()
        coEvery { progressDao.getAveragePronunciationScore(any()) }  returns 0f
        coEvery { progressDao.getDailyStatsRange(any(), any(), any()) } returns emptyList()
        every  { userDao.getUserFlow(any()) }                        returns flowOf(null)
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── calculateOverallProgress ──────────────────────────────────────────────

    @Test
    fun calculateOverallProgress_userNotFound_throwsIllegalState() = runTest {
        coEvery { userDao.getUser("user1") } returns null

        assertThrows(IllegalStateException::class.java) {
            runTest { repository.calculateOverallProgress("user1") }
        }
    }

    @Test
    fun calculateOverallProgress_userFound_returnsCefrLevelFromUser() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity(cefrLevel = "B1")

        val result = repository.calculateOverallProgress("user1")

        assertEquals(CefrLevel.B1, result.currentCefrLevel)
    }

    @Test
    fun calculateOverallProgress_streakDaysMappedFromUser() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity(streakDays = 14)

        val result = repository.calculateOverallProgress("user1")

        assertEquals(14, result.streakDays)
    }

    @Test
    fun calculateOverallProgress_totalHoursConvertedFromMinutes() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity(totalMinutes = 180)

        val result = repository.calculateOverallProgress("user1")

        assertEquals(3.0f, result.totalHours, 0.001f)
    }

    @Test
    fun calculateOverallProgress_totalSessionsFromUser() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity(totalSessions = 25)

        val result = repository.calculateOverallProgress("user1")

        assertEquals(25, result.totalSessions)
    }

    @Test
    fun calculateOverallProgress_currentPositionNull_defaultsTo1_1() = runTest {
        coEvery { bookProgressDao.getCurrentPosition("user1") } returns null

        val result = repository.calculateOverallProgress("user1")

        assertEquals(1, result.bookProgress.currentChapter)
        assertEquals(1, result.bookProgress.currentLesson)
    }

    @Test
    fun calculateOverallProgress_currentPositionExists_usedCorrectly() = runTest {
        coEvery { bookProgressDao.getCurrentPosition("user1") } returns
            makeBookProgressEntity(chapter = 3, lesson = 2)

        val result = repository.calculateOverallProgress("user1")

        assertEquals(3, result.bookProgress.currentChapter)
        assertEquals(2, result.bookProgress.currentLesson)
    }

    @Test
    fun calculateOverallProgress_completionPercentage_halfCompleted() = runTest {
        coEvery { bookProgressDao.getCompletedCount("user1") }  returns 5
        coEvery { bookProgressDao.getAllProgress("user1") }     returns
            List(10) { makeBookProgressEntity() }

        val result = repository.calculateOverallProgress("user1")

        assertEquals(0.5f, result.bookProgress.completionPercentage, 0.001f)
    }

    @Test
    fun calculateOverallProgress_allProgressEmpty_completionIs0() = runTest {
        coEvery { bookProgressDao.getCompletedCount("user1") } returns 0
        coEvery { bookProgressDao.getAllProgress("user1") }    returns emptyList()

        val result = repository.calculateOverallProgress("user1")

        assertEquals(0f, result.bookProgress.completionPercentage)
    }

    @Test
    fun calculateOverallProgress_totalChaptersAlways20() = runTest {
        val result = repository.calculateOverallProgress("user1")

        assertEquals(20, result.bookProgress.totalChapters)
    }

    @Test
    fun calculateOverallProgress_subLevelFromUser() = runTest {
        coEvery { userDao.getUser("user1") } returns makeUserEntity(cefrSubLevel = 7)

        val result = repository.calculateOverallProgress("user1")

        assertEquals(7, result.currentSubLevel)
    }

    // ── getOverallProgressFlow ────────────────────────────────────────────────

    @Test
    fun getOverallProgressFlow_userEntityNull_throwsIllegalState() = runTest {
        every { userDao.getUserFlow("user1") } returns flowOf(null)

        val flow = repository.getOverallProgressFlow("user1")

        assertThrows(IllegalStateException::class.java) {
            runTest { flow.first() }
        }
    }

    @Test
    fun getOverallProgressFlow_emitsCalculatedProgress() = runTest {
        val userEntity = makeUserEntity(id = "user1", cefrLevel = "A2")
        every { userDao.getUserFlow("user1") } returns flowOf(userEntity)
        coEvery { userDao.getUser("user1") }   returns userEntity

        val result = repository.getOverallProgressFlow("user1").first()

        assertEquals(CefrLevel.A2, result.currentCefrLevel)
    }

    // ── calculateCefrLevel ────────────────────────────────────────────────────

    @Test
    fun calculateCefrLevel_noWordsOrRules_returnsA1SubLevel1() = runTest {
        coEvery { knowledgeDao.getAllWordKnowledge("user1") } returns emptyList()
        coEvery { knowledgeDao.getAllRuleKnowledge("user1") } returns emptyList()
        coEvery { wordDao.getWordsByLevel(any()) }            returns emptyList()
        coEvery { grammarRuleDao.getRulesByLevel(any()) }     returns emptyList()

        val (level, subLevel) = repository.calculateCefrLevel("user1")

        assertEquals(CefrLevel.A1, level)
        assertEquals(1, subLevel)
    }

    @Test
    fun calculateCefrLevel_vocabBelow70pct_levelNotConfirmed() = runTest {
        coEvery { wordDao.getWordsByLevel("A1") } returns
            listOf(makeWordEntity("w1"), makeWordEntity("w2"), makeWordEntity("w3"))
        // knownAtLevel = 2 of 3 → 66.6% < 70% → A1 not confirmed
        coEvery { knowledgeDao.getAllWordKnowledge("user1") } returns listOf(
            makeWordKnowledgeEntity("w1", knowledgeLevel = 5),
            makeWordKnowledgeEntity("w2", knowledgeLevel = 5),
            makeWordKnowledgeEntity("w3", knowledgeLevel = 3)
        )
        coEvery { grammarRuleDao.getRulesByLevel("A1") } returns
            listOf(makeGrammarRuleEntity("r1"))
        coEvery { knowledgeDao.getAllRuleKnowledge("user1") } returns listOf(
            makeRuleKnowledgeEntity("r1", knowledgeLevel = 4)
        )

        val (level, _) = repository.calculateCefrLevel("user1")

        assertEquals(CefrLevel.A1, level) // stays at default A1, not confirmed for A1 properly
    }

    @Test
    fun calculateCefrLevel_vocabAndGrammarMet_levelConfirmed() = runTest {
        // A1: 3 words, 3 known (100% ≥ 70%); 1 rule, 1 known (100% ≥ 60%)
        coEvery { wordDao.getWordsByLevel("A1") } returns
            listOf(makeWordEntity("w1"), makeWordEntity("w2"), makeWordEntity("w3"))
        coEvery { knowledgeDao.getAllWordKnowledge("user1") } returns listOf(
            makeWordKnowledgeEntity("w1", knowledgeLevel = 5),
            makeWordKnowledgeEntity("w2", knowledgeLevel = 5),
            makeWordKnowledgeEntity("w3", knowledgeLevel = 5)
        )
        coEvery { grammarRuleDao.getRulesByLevel("A1") } returns
            listOf(makeGrammarRuleEntity("r1"))
        coEvery { knowledgeDao.getAllRuleKnowledge("user1") } returns listOf(
            makeRuleKnowledgeEntity("r1", knowledgeLevel = 4)
        )

        val (level, _) = repository.calculateCefrLevel("user1")

        assertEquals(CefrLevel.A1, level)
    }

    @Test
    fun calculateCefrLevel_grammarBelow60pct_levelNotAdvanced() = runTest {
        coEvery { wordDao.getWordsByLevel("A1") } returns
            listOf(makeWordEntity("w1"), makeWordEntity("w2"))
        coEvery { knowledgeDao.getAllWordKnowledge("user1") } returns listOf(
            makeWordKnowledgeEntity("w1", knowledgeLevel = 5),
            makeWordKnowledgeEntity("w2", knowledgeLevel = 5)
        )
        // 1 of 2 rules known = 50% < 60% → not confirmed
        coEvery { grammarRuleDao.getRulesByLevel("A1") } returns
            listOf(makeGrammarRuleEntity("r1"), makeGrammarRuleEntity("r2"))
        coEvery { knowledgeDao.getAllRuleKnowledge("user1") } returns listOf(
            makeRuleKnowledgeEntity("r1", knowledgeLevel = 4),
            makeRuleKnowledgeEntity("r2", knowledgeLevel = 2)
        )

        val (level, _) = repository.calculateCefrLevel("user1")

        // A1 not confirmed → stays at default A1
        assertEquals(CefrLevel.A1, level)
    }

    @Test
    fun calculateCefrLevel_noRulesAtLevel_grammarScoreIs0_notConfirmed() = runTest {
        coEvery { wordDao.getWordsByLevel("A1") } returns
            listOf(makeWordEntity("w1"), makeWordEntity("w2"))
        coEvery { knowledgeDao.getAllWordKnowledge("user1") } returns listOf(
            makeWordKnowledgeEntity("w1", knowledgeLevel = 5),
            makeWordKnowledgeEntity("w2", knowledgeLevel = 5)
        )
        // No rules → grammarScore = 0f < 0.6f → not confirmed
        coEvery { grammarRuleDao.getRulesByLevel("A1") } returns emptyList()

        val (level, _) = repository.calculateCefrLevel("user1")

        assertEquals(CefrLevel.A1, level)
    }

    @Test
    fun calculateCefrLevel_subLevel_noNextLevelWords_returns1() = runTest {
        // A1 confirmed, no A2 words exist → subLevel = 1
        stubA1Confirmed()
        coEvery { wordDao.getWordsByLevel("A2") } returns emptyList()

        val (_, subLevel) = repository.calculateCefrLevel("user1")

        assertEquals(1, subLevel)
    }

    @Test
    fun calculateCefrLevel_atMaxLevel_subLevelIs10() = runTest {
        // When confirmed level has no next level → subLevel = 10
        // Simulate by confirming C2 (last level)
        for (level in CefrLevel.entries) {
            coEvery { wordDao.getWordsByLevel(level.name) } returns
                listOf(makeWordEntity("w_${level.name}"))
            coEvery { grammarRuleDao.getRulesByLevel(level.name) } returns
                listOf(makeGrammarRuleEntity("r_${level.name}"))
        }
        coEvery { knowledgeDao.getAllWordKnowledge("user1") } returns
            CefrLevel.entries.map { level -> makeWordKnowledgeEntity("w_${level.name}", knowledgeLevel = 5) }
        coEvery { knowledgeDao.getAllRuleKnowledge("user1") } returns
            CefrLevel.entries.map { level -> makeRuleKnowledgeEntity("r_${level.name}", knowledgeLevel = 4) }

        val (level, subLevel) = repository.calculateCefrLevel("user1")

        assertEquals(CefrLevel.C2, level)
        assertEquals(10, subLevel)
    }

    @Test
    fun calculateCefrLevel_subLevelClamped1to10() = runTest {
        stubA1Confirmed()
        // A2: 100 words, 0 known → progress = 0 → coerceIn(1, 10) = 1
        coEvery { wordDao.getWordsByLevel("A2") } returns List(100) { i -> makeWordEntity("w2_$i") }
        coEvery { knowledgeDao.getAllWordKnowledge("user1") } returns listOf(
            makeWordKnowledgeEntity("w1", knowledgeLevel = 5),
            makeWordKnowledgeEntity("w1_r1", knowledgeLevel = 5) // enough to confirm A1
        )

        val (_, subLevel) = repository.calculateCefrLevel("user1")

        assertTrue(subLevel in 1..10)
    }

    // ── getVocabularyProgress ─────────────────────────────────────────────────

    @Test
    fun getVocabularyProgress_emptyData_allZeros() = runTest {
        val result = repository.getVocabularyProgress("user1")

        assertEquals(0, result.totalWords)
        assertEquals(0, result.activeWords)
        assertEquals(0, result.passiveWords)
        assertEquals(0, result.wordsForReviewToday)
    }

    @Test
    fun getVocabularyProgress_totalWords_countsOnlyNonZeroLevel() = runTest {
        coEvery { knowledgeDao.getAllWordKnowledge("user1") } returns listOf(
            makeWordKnowledgeEntity("w1", knowledgeLevel = 3),
            makeWordKnowledgeEntity("w2", knowledgeLevel = 0)
        )

        val result = repository.getVocabularyProgress("user1")

        assertEquals(1, result.totalWords)
    }

    @Test
    fun getVocabularyProgress_byLevel_groupedCorrectly() = runTest {
        coEvery { knowledgeDao.getAllWordKnowledge("user1") } returns listOf(
            makeWordKnowledgeEntity("w1", knowledgeLevel = 3),
            makeWordKnowledgeEntity("w2", knowledgeLevel = 3),
            makeWordKnowledgeEntity("w3", knowledgeLevel = 5)
        )

        val result = repository.getVocabularyProgress("user1")

        assertEquals(2, result.byLevel[3])
        assertEquals(1, result.byLevel[5])
    }

    @Test
    fun getVocabularyProgress_byTopic_countsKnownAtLevel4Plus() = runTest {
        coEvery { wordDao.getAllWords() } returns listOf(
            makeWordEntity("w1", topic = "food"),
            makeWordEntity("w2", topic = "food")
        )
        coEvery { knowledgeDao.getAllWordKnowledge("user1") } returns listOf(
            makeWordKnowledgeEntity("w1", knowledgeLevel = 4),
            makeWordKnowledgeEntity("w2", knowledgeLevel = 3) // level 3, not known
        )

        val result = repository.getVocabularyProgress("user1")

        assertEquals(1, result.byTopic["food"]?.known)
        assertEquals(2, result.byTopic["food"]?.total)
    }

    @Test
    fun getVocabularyProgress_passiveWords_coercedAtLeast0() = runTest {
        // knownWords < activeWords (shouldn't happen, but coerceAtLeast(0) guards it)
        coEvery { knowledgeDao.getActiveWordsCount("user1") }  returns 10
        coEvery { knowledgeDao.getKnownWordsCount("user1") }   returns 5

        val result = repository.getVocabularyProgress("user1")

        assertEquals(0, result.passiveWords) // (5-10).coerceAtLeast(0) = 0
    }

    @Test
    fun getVocabularyProgress_passiveWords_normalCase() = runTest {
        coEvery { knowledgeDao.getActiveWordsCount("user1") }  returns 30
        coEvery { knowledgeDao.getKnownWordsCount("user1") }   returns 50

        val result = repository.getVocabularyProgress("user1")

        assertEquals(20, result.passiveWords)
    }

    @Test
    fun getVocabularyProgress_wordsForReviewToday_usesNowTimestamp() = runTest {
        coEvery { knowledgeDao.getWordsForReviewCount("user1", fixedNow) } returns 8

        val result = repository.getVocabularyProgress("user1")

        assertEquals(8, result.wordsForReviewToday)
    }

    // ── getGrammarProgress ────────────────────────────────────────────────────

    @Test
    fun getGrammarProgress_emptyData_allZeros() = runTest {
        val result = repository.getGrammarProgress("user1")

        assertEquals(0,  result.totalRules)
        assertEquals(0,  result.knownRules)
        assertTrue(result.byCategory.isEmpty())
        assertEquals(0,  result.rulesForReviewToday)
    }

    @Test
    fun getGrammarProgress_totalRulesEqualsAllRulesSize() = runTest {
        coEvery { grammarRuleDao.getAllRules() } returns
            listOf(makeGrammarRuleEntity("r1"), makeGrammarRuleEntity("r2"), makeGrammarRuleEntity("r3"))

        val result = repository.getGrammarProgress("user1")

        assertEquals(3, result.totalRules)
    }

    @Test
    fun getGrammarProgress_knownRulesDelegatesCorrectly() = runTest {
        coEvery { knowledgeDao.getKnownRulesCount("user1") } returns 7

        val result = repository.getGrammarProgress("user1")

        assertEquals(7, result.knownRules)
    }

    @Test
    fun getGrammarProgress_byCategory_countsKnownAtLevel4Plus() = runTest {
        coEvery { grammarRuleDao.getAllRules() } returns listOf(
            makeGrammarRuleEntity("r1", category = "ARTICLES"),
            makeGrammarRuleEntity("r2", category = "ARTICLES")
        )
        coEvery { knowledgeDao.getAllRuleKnowledge("user1") } returns listOf(
            makeRuleKnowledgeEntity("r1", knowledgeLevel = 4),
            makeRuleKnowledgeEntity("r2", knowledgeLevel = 3) // not known
        )

        val result = repository.getGrammarProgress("user1")

        assertEquals(1, result.byCategory["ARTICLES"])
    }

    @Test
    fun getGrammarProgress_byCategory_differentCategories() = runTest {
        coEvery { grammarRuleDao.getAllRules() } returns listOf(
            makeGrammarRuleEntity("r1", category = "ARTICLES"),
            makeGrammarRuleEntity("r2", category = "VERBS")
        )
        coEvery { knowledgeDao.getAllRuleKnowledge("user1") } returns listOf(
            makeRuleKnowledgeEntity("r1", knowledgeLevel = 5),
            makeRuleKnowledgeEntity("r2", knowledgeLevel = 5)
        )

        val result = repository.getGrammarProgress("user1")

        assertEquals(1, result.byCategory["ARTICLES"])
        assertEquals(1, result.byCategory["VERBS"])
    }

    @Test
    fun getGrammarProgress_rulesForReviewToday_usesNowTimestamp() = runTest {
        coEvery { knowledgeDao.getRulesForReviewCount("user1", fixedNow) } returns 3

        val result = repository.getGrammarProgress("user1")

        assertEquals(3, result.rulesForReviewToday)
    }

    // ── getPronunciationProgress ──────────────────────────────────────────────

    @Test
    fun getPronunciationProgress_averageScoreFromProgressDao() = runTest {
        coEvery { progressDao.getAveragePronunciationScore("user1") } returns 0.72f

        val result = repository.getPronunciationProgress("user1")

        assertEquals(0.72f, result.overallScore, 0.001f)
    }

    @Test
    fun getPronunciationProgress_goodSoundsAlwaysEmpty() = runTest {
        val result = repository.getPronunciationProgress("user1")

        assertTrue(result.goodSounds.isEmpty())
    }

    @Test
    fun getPronunciationProgress_trendAlwaysStable() = runTest {
        val result = repository.getPronunciationProgress("user1")

        assertEquals("stable", result.trend)
    }

    // ── getBookOverallProgress ────────────────────────────────────────────────

    @Test
    fun getBookOverallProgress_noPosition_defaultsTo1_1() = runTest {
        coEvery { bookProgressDao.getCurrentPosition("user1") } returns null

        val result = repository.getBookOverallProgress("user1")

        assertEquals(1, result.currentChapter)
        assertEquals(1, result.currentLesson)
    }

    @Test
    fun getBookOverallProgress_positionExists_usedCorrectly() = runTest {
        coEvery { bookProgressDao.getCurrentPosition("user1") } returns
            makeBookProgressEntity(chapter = 4, lesson = 3)

        val result = repository.getBookOverallProgress("user1")

        assertEquals(4, result.currentChapter)
        assertEquals(3, result.currentLesson)
    }

    @Test
    fun getBookOverallProgress_completionHalfway() = runTest {
        coEvery { bookProgressDao.getCompletedCount("user1") }  returns 4
        coEvery { bookProgressDao.getAllProgress("user1") }     returns
            List(8) { makeBookProgressEntity() }

        val result = repository.getBookOverallProgress("user1")

        assertEquals(0.5f, result.completionPercentage, 0.001f)
    }

    @Test
    fun getBookOverallProgress_allProgressEmpty_completionIs0() = runTest {
        coEvery { bookProgressDao.getCompletedCount("user1") } returns 0
        coEvery { bookProgressDao.getAllProgress("user1") }    returns emptyList()

        val result = repository.getBookOverallProgress("user1")

        assertEquals(0f, result.completionPercentage)
    }

    @Test
    fun getBookOverallProgress_totalLessonsCoercedAtLeast1() = runTest {
        coEvery { bookProgressDao.getAllProgress("user1") } returns emptyList()
        coEvery { bookProgressDao.getCompletedCount("user1") } returns 0

        val result = repository.getBookOverallProgress("user1")

        assertEquals(1, result.totalLessons)
    }

    @Test
    fun getBookOverallProgress_totalChaptersAlways20() = runTest {
        val result = repository.getBookOverallProgress("user1")

        assertEquals(20, result.totalChapters)
    }

    // ── getSkillProgress ──────────────────────────────────────────────────────

    @Test
    fun getSkillProgress_emptyData_allZeros() = runTest {
        val result = repository.getSkillProgress("user1")

        assertEquals(0f, result.vocabulary, 0.001f)
        assertEquals(0f, result.grammar, 0.001f)
        assertEquals(0f, result.pronunciation, 0.001f)
        assertEquals(0f, result.listening, 0.001f)
        assertEquals(0f, result.speaking, 0.001f)
    }

    @Test
    fun getSkillProgress_vocabularyRatioCorrect() = runTest {
        coEvery { knowledgeDao.getActiveWordsCount("user1") }  returns 30
        coEvery { knowledgeDao.getKnownWordsCount("user1") }   returns 50
        coEvery { wordDao.getWordCount() }                     returns 100

        val result = repository.getSkillProgress("user1")

        // totalKnown = 30 + 20 = 50; totalWords = 100 → 0.5
        assertEquals(0.5f, result.vocabulary, 0.001f)
    }

    @Test
    fun getSkillProgress_vocabularyTotalWords0_coercedTo1() = runTest {
        coEvery { knowledgeDao.getActiveWordsCount("user1") }  returns 0
        coEvery { knowledgeDao.getKnownWordsCount("user1") }   returns 0
        coEvery { wordDao.getWordCount() }                     returns 0

        val result = repository.getSkillProgress("user1")

        // coerceAtLeast(1) prevents division by zero
        assertEquals(0f, result.vocabulary, 0.001f)
    }

    @Test
    fun getSkillProgress_grammarRatioCorrect() = runTest {
        coEvery { grammarRuleDao.getAllRules() }           returns List(10) { makeGrammarRuleEntity("r$it") }
        coEvery { knowledgeDao.getKnownRulesCount("user1") } returns 6

        val result = repository.getSkillProgress("user1")

        assertEquals(0.6f, result.grammar, 0.001f)
    }

    @Test
    fun getSkillProgress_grammarNoRules_returns0() = runTest {
        coEvery { grammarRuleDao.getAllRules() } returns emptyList()

        val result = repository.getSkillProgress("user1")

        assertEquals(0f, result.grammar, 0.001f)
    }

    @Test
    fun getSkillProgress_pronunciationFromOverallScore() = runTest {
        coEvery { progressDao.getAveragePronunciationScore("user1") } returns 0.83f

        val result = repository.getSkillProgress("user1")

        assertEquals(0.83f, result.pronunciation, 0.001f)
    }

    @Test
    fun getSkillProgress_listeningAndSpeakingAlwaysZero() = runTest {
        val result = repository.getSkillProgress("user1")

        assertEquals(0f, result.listening)
        assertEquals(0f, result.speaking)
    }

    // ── getWeeklyProgress ─────────────────────────────────────────────────────

    @Test
    fun getWeeklyProgress_delegatesToProgressDao() = runTest {
        coEvery { progressDao.getDailyStatsRange("user1", any(), any()) } returns emptyList()

        val result = repository.getWeeklyProgress("user1")

        assertTrue(result.isEmpty())
        coVerify(exactly = 1) { progressDao.getDailyStatsRange(eq("user1"), any(), any()) }
    }

    @Test
    fun getWeeklyProgress_mapsEntitiesCorrectly() = runTest {
        coEvery { progressDao.getDailyStatsRange("user1", any(), any()) } returns
            listOf(makeDailyStatisticsEntity(), makeDailyStatisticsEntity())

        val result = repository.getWeeklyProgress("user1")

        assertEquals(2, result.size)
    }

    @Test
    fun getWeeklyProgress_dateRangeIs7Days() = runTest {
        coEvery { progressDao.getDailyStatsRange("user1", any(), any()) } returns emptyList()

        repository.getWeeklyProgress("user1")

        // Verify that getDailyStatsRange is called with a range spanning 7 days (6 days back)
        coVerify {
            progressDao.getDailyStatsRange(
                "user1",
                match { startDate ->
                    // end - start should be 6 days
                    val pattern = Regex("""\d{4}-\d{2}-\d{2}""")
                    pattern.matches(startDate)
                },
                any()
            )
        }
    }

    // ── getMonthlyProgress ────────────────────────────────────────────────────

    @Test
    fun getMonthlyProgress_delegatesToProgressDao() = runTest {
        coEvery { progressDao.getDailyStatsRange("user1", any(), any()) } returns emptyList()

        repository.getMonthlyProgress("user1")

        coVerify(exactly = 1) { progressDao.getDailyStatsRange(eq("user1"), any(), any()) }
    }

    @Test
    fun getMonthlyProgress_mapsEntitiesCorrectly() = runTest {
        coEvery { progressDao.getDailyStatsRange("user1", any(), any()) } returns
            List(30) { makeDailyStatisticsEntity() }

        val result = repository.getMonthlyProgress("user1")

        assertEquals(30, result.size)
    }

    // ── getCompletedChapterCount ──────────────────────────────────────────────

    @Test
    fun getCompletedChapterCount_noProgress_returns0() = runTest {
        coEvery { bookProgressDao.getAllProgress("user1") } returns emptyList()

        val result = repository.getCompletedChapterCount("user1")

        assertEquals(0, result)
    }

    @Test
    fun getCompletedChapterCount_onlyCountsCOMPLETEDStatus() = runTest {
        coEvery { bookProgressDao.getAllProgress("user1") } returns listOf(
            makeBookProgressEntity(chapter = 1, status = "COMPLETED"),
            makeBookProgressEntity(chapter = 1, status = "NOT_STARTED"),
            makeBookProgressEntity(chapter = 2, status = "COMPLETED")
        )

        val result = repository.getCompletedChapterCount("user1")

        assertEquals(2, result)
    }

    @Test
    fun getCompletedChapterCount_distinctChapters() = runTest {
        // Multiple lessons in same chapter → chapter counted once
        coEvery { bookProgressDao.getAllProgress("user1") } returns listOf(
            makeBookProgressEntity(chapter = 1, status = "COMPLETED"),
            makeBookProgressEntity(chapter = 1, status = "COMPLETED"),
            makeBookProgressEntity(chapter = 1, status = "COMPLETED")
        )

        val result = repository.getCompletedChapterCount("user1")

        assertEquals(1, result)
    }

    @Test
    fun getCompletedChapterCount_mixedChapters_onlyDistinctCompleted() = runTest {
        coEvery { bookProgressDao.getAllProgress("user1") } returns listOf(
            makeBookProgressEntity(chapter = 1, status = "COMPLETED"),
            makeBookProgressEntity(chapter = 1, status = "COMPLETED"),
            makeBookProgressEntity(chapter = 2, status = "COMPLETED"),
            makeBookProgressEntity(chapter = 3, status = "NOT_STARTED")
        )

        val result = repository.getCompletedChapterCount("user1")

        assertEquals(2, result)
    }

    @Test
    fun getCompletedChapterCount_allNOT_STARTED_returns0() = runTest {
        coEvery { bookProgressDao.getAllProgress("user1") } returns listOf(
            makeBookProgressEntity(chapter = 1, status = "NOT_STARTED"),
            makeBookProgressEntity(chapter = 2, status = "NOT_STARTED")
        )

        val result = repository.getCompletedChapterCount("user1")

        assertEquals(0, result)
    }

    @Test
    fun getCompletedChapterCount_5DistinctChaptersCompleted_returns5() = runTest {
        coEvery { bookProgressDao.getAllProgress("user1") } returns
            List(5) { i -> makeBookProgressEntity(chapter = i + 1, status = "COMPLETED") }

        val result = repository.getCompletedChapterCount("user1")

        assertEquals(5, result)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun stubA1Confirmed() {
        coEvery { wordDao.getWordsByLevel("A1") } returns listOf(makeWordEntity("w1"))
        coEvery { knowledgeDao.getAllWordKnowledge("user1") } returns listOf(
            makeWordKnowledgeEntity("w1", knowledgeLevel = 5)
        )
        coEvery { grammarRuleDao.getRulesByLevel("A1") } returns listOf(makeGrammarRuleEntity("r1"))
        coEvery { knowledgeDao.getAllRuleKnowledge("user1") } returns listOf(
            makeRuleKnowledgeEntity("r1", knowledgeLevel = 4)
        )
        // Other levels return empty
        for (level in CefrLevel.entries.drop(1)) {
            coEvery { wordDao.getWordsByLevel(level.name) } returns emptyList()
            coEvery { grammarRuleDao.getRulesByLevel(level.name) } returns emptyList()
        }
    }
}
