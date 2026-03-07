// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/knowledge/BuildKnowledgeSummaryUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.ProblemWordInfo
import com.voicedeutsch.master.domain.model.speech.PhoneticTarget
import com.voicedeutsch.master.domain.model.speech.PronunciationTrend
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.domain.repository.ProgressRepository
import com.voicedeutsch.master.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BuildKnowledgeSummaryUseCaseTest {

    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var sessionRepository:   SessionRepository
    private lateinit var bookRepository:      BookRepository
    private lateinit var progressRepository:  ProgressRepository
    private lateinit var getWeakPointsUseCase: GetWeakPointsUseCase

    private lateinit var useCase: BuildKnowledgeSummaryUseCase

    private val userId = "user_build_test"

    // ── Domain model helpers ──────────────────────────────────────────────────

    /** Minimal word entity */
    private fun makeWord(
        id: String = "w1",
        german: String = "Hund",
        topic: String = "Tiere",
    ) = mockk<com.voicedeutsch.master.domain.model.knowledge.Word>(relaxed = true).also { w ->
        every { w.id }     returns id
        every { w.german } returns german
        every { w.topic }  returns topic
    }

    /** Minimal word-knowledge entity */
    private fun makeWordKnowledge(
        wordId: String        = "w1",
        knowledgeLevel: Int   = 1,
        createdAt: Long       = 1_000L,
        timesSeen: Int        = 3,
    ) = mockk<com.voicedeutsch.master.domain.model.knowledge.WordKnowledge>(relaxed = true).also { wk ->
        every { wk.wordId }         returns wordId
        every { wk.knowledgeLevel } returns knowledgeLevel
        every { wk.createdAt }      returns createdAt
        every { wk.timesSeen }      returns timesSeen
    }

    /** Minimal grammar-rule entity */
    private fun makeRule(
        id: String     = "r1",
        nameRu: String = "Akkusativ",
    ) = mockk<com.voicedeutsch.master.domain.model.knowledge.GrammarRule>(relaxed = true).also { r ->
        every { r.id }     returns id
        every { r.nameRu } returns nameRu
    }

    /** Minimal rule-knowledge entity */
    private fun makeRuleKnowledge(
        ruleId: String        = "r1",
        knowledgeLevel: Int   = 1,
        lastPracticed: Long   = 1_000L,
        timesPracticed: Int   = 5,
    ) = mockk<com.voicedeutsch.master.domain.model.knowledge.RuleKnowledge>(relaxed = true).also { rk ->
        every { rk.ruleId }         returns ruleId
        every { rk.knowledgeLevel } returns knowledgeLevel
        every { rk.lastPracticed }  returns lastPracticed
        every { rk.timesPracticed } returns timesPracticed
    }

    private fun makePhoneticTarget(
        sound: String              = "ü",
        currentScore: Float        = 0.5f,
        trend: PronunciationTrend  = PronunciationTrend.STABLE,
    ) = mockk<PhoneticTarget>(relaxed = true).also { t ->
        every { t.sound }        returns sound
        every { t.currentScore } returns currentScore
        every { t.trend }        returns trend
    }

    private fun makeLearningSession(
        durationMinutes: Int  = 25,
        startedAt: Long       = System.currentTimeMillis(),
        sessionSummary: String = "Learned 5 words",
    ) = mockk<com.voicedeutsch.master.domain.model.session.LearningSession>(relaxed = true).also { s ->
        every { s.durationMinutes } returns durationMinutes
        every { s.startedAt }       returns startedAt
        every { s.sessionSummary }  returns sessionSummary
    }

    private fun makeBookMetadata(totalChapters: Int = 10) =
        mockk<com.voicedeutsch.master.domain.model.book.BookMetadata>(relaxed = true).also { m ->
            every { m.totalChapters } returns totalChapters
        }

    private fun makeChapter(titleRu: String = "Familie") =
        mockk<com.voicedeutsch.master.domain.model.book.Chapter>(relaxed = true).also { c ->
            every { c.titleRu } returns titleRu
        }

    private fun makeProblemWordPair(
        german: String        = "Tisch",
        knowledgeLevel: Int   = 1,
        timesSeen: Int        = 4,
    ): Pair<com.voicedeutsch.master.domain.model.knowledge.Word, com.voicedeutsch.master.domain.model.knowledge.WordKnowledge> {
        val word = makeWord(german = german)
        val wk   = makeWordKnowledge(knowledgeLevel = knowledgeLevel, timesSeen = timesSeen)
        return Pair(word, wk)
    }

    // ── setUp: default happy-path stubs ───────────────────────────────────────

    @BeforeEach
    fun setUp() {
        knowledgeRepository = mockk()
        sessionRepository   = mockk()
        bookRepository      = mockk()
        progressRepository  = mockk()
        getWeakPointsUseCase = mockk()

        useCase = BuildKnowledgeSummaryUseCase(
            knowledgeRepository  = knowledgeRepository,
            sessionRepository    = sessionRepository,
            bookRepository       = bookRepository,
            progressRepository   = progressRepository,
            getWeakPointsUseCase = getWeakPointsUseCase,
        )

        // ── vocabulary defaults ──
        coEvery { knowledgeRepository.getAllWordKnowledge(userId) } returns listOf(
            makeWordKnowledge("w1", knowledgeLevel = 3, createdAt = 200L),
            makeWordKnowledge("w2", knowledgeLevel = 1, createdAt = 100L),
        )
        coEvery { knowledgeRepository.getAllWords() } returns listOf(
            makeWord("w1", "Hund", "Tiere"),
            makeWord("w2", "Katze", "Tiere"),
        )
        coEvery { knowledgeRepository.getProblemWords(userId, 5) } returns emptyList()
        coEvery { knowledgeRepository.getWordsForReviewCount(userId) } returns 3

        // ── grammar defaults ──
        coEvery { knowledgeRepository.getAllRuleKnowledge(userId) } returns listOf(
            makeRuleKnowledge("r1", knowledgeLevel = 2, lastPracticed = 500L, timesPracticed = 4),
        )
        coEvery { knowledgeRepository.getAllGrammarRules() } returns listOf(
            makeRule("r1", "Akkusativ"),
        )
        coEvery { knowledgeRepository.getRulesForReviewCount(userId) } returns 1

        // ── pronunciation defaults ──
        coEvery { knowledgeRepository.getAveragePronunciationScore(userId) } returns 0.75f
        coEvery { knowledgeRepository.getProblemSounds(userId) } returns emptyList()

        // ── book defaults ──
        coEvery { bookRepository.getCurrentBookPosition(userId) } returns Pair(2, 5)
        coEvery { bookRepository.getBookMetadata() } returns makeBookMetadata(totalChapters = 10)
        coEvery { bookRepository.getBookCompletionPercentage(userId) } returns 30f
        coEvery { bookRepository.getChapter(2) } returns makeChapter("Familie")

        // ── session defaults ──
        coEvery { sessionRepository.getRecentSessions(userId, 5) } returns listOf(
            makeLearningSession(durationMinutes = 20),
            makeLearningSession(durationMinutes = 30),
        )
        coEvery { sessionRepository.calculateStreak(userId) } returns 5
        coEvery { sessionRepository.getSessionCount(userId) } returns 12

        // ── weak points default ──
        coEvery { getWeakPointsUseCase(userId) } returns emptyList()
    }

    // ── Basic contract ────────────────────────────────────────────────────────

    @Test
    fun invoke_happyPath_returnsNonNullSnapshot() = runTest {
        val result = useCase(userId)
        assertNotNull(result)
    }

    @Test
    fun invoke_happyPath_vocabularyIsPopulated() = runTest {
        val result = useCase(userId)
        assertNotNull(result.vocabulary)
    }

    @Test
    fun invoke_happyPath_grammarIsPopulated() = runTest {
        val result = useCase(userId)
        assertNotNull(result.grammar)
    }

    @Test
    fun invoke_happyPath_pronunciationIsPopulated() = runTest {
        val result = useCase(userId)
        assertNotNull(result.pronunciation)
    }

    @Test
    fun invoke_happyPath_bookProgressIsPopulated() = runTest {
        val result = useCase(userId)
        assertNotNull(result.bookProgress)
    }

    @Test
    fun invoke_happyPath_sessionHistoryIsPopulated() = runTest {
        val result = useCase(userId)
        assertNotNull(result.sessionHistory)
    }

    @Test
    fun invoke_happyPath_recommendationsIsPopulated() = runTest {
        val result = useCase(userId)
        assertNotNull(result.recommendations)
    }

    // ── Vocabulary snapshot ───────────────────────────────────────────────────

    @Test
    fun invoke_vocabulary_totalWordsCountsKnowledgeLevelAbove0() = runTest {
        // wk levels: 3 and 1 — both > 0 → totalWords = 2
        val result = useCase(userId)
        assertEquals(2, result.vocabulary.totalWords)
    }

    @Test
    fun invoke_vocabulary_wordsWithLevel0NotCountedInTotal() = runTest {
        coEvery { knowledgeRepository.getAllWordKnowledge(userId) } returns listOf(
            makeWordKnowledge("w1", knowledgeLevel = 0),
            makeWordKnowledge("w2", knowledgeLevel = 3),
        )
        val result = useCase(userId)
        assertEquals(1, result.vocabulary.totalWords)
    }

    @Test
    fun invoke_vocabulary_byLevelGroupsCorrectly() = runTest {
        coEvery { knowledgeRepository.getAllWordKnowledge(userId) } returns listOf(
            makeWordKnowledge("w1", knowledgeLevel = 2),
            makeWordKnowledge("w2", knowledgeLevel = 2),
            makeWordKnowledge("w3", knowledgeLevel = 5),
        )
        val result = useCase(userId)
        assertEquals(2, result.vocabulary.byLevel[2])
        assertEquals(1, result.vocabulary.byLevel[5])
    }

    @Test
    fun invoke_vocabulary_byTopicCountsCorrectly() = runTest {
        coEvery { knowledgeRepository.getAllWordKnowledge(userId) } returns listOf(
            makeWordKnowledge("w1", knowledgeLevel = 4), // known (>= 4) in Tiere
            makeWordKnowledge("w2", knowledgeLevel = 2), // not known in Tiere
        )
        coEvery { knowledgeRepository.getAllWords() } returns listOf(
            makeWord("w1", "Hund", "Tiere"),
            makeWord("w2", "Katze", "Tiere"),
        )
        val result = useCase(userId)
        val tiereStats = result.vocabulary.byTopic["Tiere"]
        assertNotNull(tiereStats)
        assertEquals(1, tiereStats!!.known)
        assertEquals(2, tiereStats.total)
    }

    @Test
    fun invoke_vocabulary_byTopicIncludesWordsWithNoKnowledge() = runTest {
        // w2 has no entry in allWordKnowledge → still counted in topic total
        coEvery { knowledgeRepository.getAllWordKnowledge(userId) } returns listOf(
            makeWordKnowledge("w1", knowledgeLevel = 3),
        )
        coEvery { knowledgeRepository.getAllWords() } returns listOf(
            makeWord("w1", "Hund", "Tiere"),
            makeWord("w2", "Maus", "Tiere"), // not in knowledge list
        )
        val result = useCase(userId)
        val tiereStats = result.vocabulary.byTopic["Tiere"]
        assertEquals(2, tiereStats!!.total)
    }

    @Test
    fun invoke_vocabulary_recentNewWordsAreLevel1SortedByCreatedAtDesc() = runTest {
        coEvery { knowledgeRepository.getAllWordKnowledge(userId) } returns listOf(
            makeWordKnowledge("w1", knowledgeLevel = 1, createdAt = 100L),
            makeWordKnowledge("w2", knowledgeLevel = 1, createdAt = 300L),
            makeWordKnowledge("w3", knowledgeLevel = 3, createdAt = 500L), // not level 1
        )
        coEvery { knowledgeRepository.getAllWords() } returns listOf(
            makeWord("w1", "Hund", "Tiere"),
            makeWord("w2", "Katze", "Tiere"),
            makeWord("w3", "Maus", "Tiere"),
        )
        val result = useCase(userId)
        // w2 (createdAt=300) should come before w1 (createdAt=100)
        assertEquals(listOf("Katze", "Hund"), result.vocabulary.recentNewWords)
    }

    @Test
    fun invoke_vocabulary_recentNewWordsLimitedTo10() = runTest {
        val knowledgeList = (1..15).map { i ->
            makeWordKnowledge("w$i", knowledgeLevel = 1, createdAt = i.toLong())
        }
        val wordList = (1..15).map { i -> makeWord("w$i", "Word$i") }
        coEvery { knowledgeRepository.getAllWordKnowledge(userId) } returns knowledgeList
        coEvery { knowledgeRepository.getAllWords() } returns wordList
        val result = useCase(userId)
        assertTrue(result.vocabulary.recentNewWords.size <= 10)
    }

    @Test
    fun invoke_vocabulary_wordsForReviewTodayFromRepository() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount(userId) } returns 17
        val result = useCase(userId)
        assertEquals(17, result.vocabulary.wordsForReviewToday)
    }

    // ── FIX Баг #4: problemWords limited to 5 ────────────────────────────────

    @Test
    fun invoke_vocabulary_problemWordsLimitedTo5() = runTest {
        val pairs = (1..8).map { i -> makeProblemWordPair("Word$i") }
        coEvery { knowledgeRepository.getProblemWords(userId, 5) } returns pairs.take(5)
        val result = useCase(userId)
        assertTrue(result.vocabulary.problemWords.size <= 5)
    }

    @Test
    fun invoke_vocabulary_problemWordsCallsRepositoryWithLimit5() = runTest {
        useCase(userId)
        coVerify { knowledgeRepository.getProblemWords(userId, 5) }
    }

    @Test
    fun invoke_vocabulary_problemWordsMappedCorrectly() = runTest {
        val pair = makeProblemWordPair(german = "Schmetterling", knowledgeLevel = 1, timesSeen = 6)
        coEvery { knowledgeRepository.getProblemWords(userId, 5) } returns listOf(pair)
        val result = useCase(userId)
        val pw = result.vocabulary.problemWords.first()
        assertEquals("Schmetterling", pw.word)
        assertEquals(1, pw.level)
        assertEquals(6, pw.attempts)
    }

    // ── Grammar snapshot ──────────────────────────────────────────────────────

    @Test
    fun invoke_grammar_totalRulesCountsKnowledgeLevelAbove0() = runTest {
        coEvery { knowledgeRepository.getAllRuleKnowledge(userId) } returns listOf(
            makeRuleKnowledge("r1", knowledgeLevel = 1),
            makeRuleKnowledge("r2", knowledgeLevel = 0), // not counted
        )
        val result = useCase(userId)
        assertEquals(1, result.grammar.totalRules)
    }

    @Test
    fun invoke_grammar_byLevelGroupsCorrectly() = runTest {
        coEvery { knowledgeRepository.getAllRuleKnowledge(userId) } returns listOf(
            makeRuleKnowledge("r1", knowledgeLevel = 3),
            makeRuleKnowledge("r2", knowledgeLevel = 3),
            makeRuleKnowledge("r3", knowledgeLevel = 5),
        )
        val result = useCase(userId)
        assertEquals(2, result.grammar.byLevel[3])
        assertEquals(1, result.grammar.byLevel[5])
    }

    // ── FIX Баг #4: knownRules limited to 10 ─────────────────────────────────

    @Test
    fun invoke_grammar_knownRulesLimitedTo10() = runTest {
        val ruleKnowledgeList = (1..15).map { i ->
            makeRuleKnowledge("r$i", knowledgeLevel = 3, lastPracticed = i.toLong())
        }
        val ruleList = (1..15).map { i -> makeRule("r$i", "Rule$i") }
        coEvery { knowledgeRepository.getAllRuleKnowledge(userId) } returns ruleKnowledgeList
        coEvery { knowledgeRepository.getAllGrammarRules() } returns ruleList
        val result = useCase(userId)
        assertTrue(result.grammar.knownRules.size <= 10)
    }

    @Test
    fun invoke_grammar_knownRulesSortedByLastPracticedDesc() = runTest {
        coEvery { knowledgeRepository.getAllRuleKnowledge(userId) } returns listOf(
            makeRuleKnowledge("r1", knowledgeLevel = 2, lastPracticed = 100L),
            makeRuleKnowledge("r2", knowledgeLevel = 2, lastPracticed = 500L),
        )
        coEvery { knowledgeRepository.getAllGrammarRules() } returns listOf(
            makeRule("r1", "Dativ"),
            makeRule("r2", "Akkusativ"),
        )
        val result = useCase(userId)
        // r2 (lastPracticed=500) should be first
        assertEquals("Akkusativ", result.grammar.knownRules.first().name)
    }

    @Test
    fun invoke_grammar_problemRulesFilteredByLevelAndAttempts() = runTest {
        // level <= 2 AND timesPracticed >= 3 → problem
        coEvery { knowledgeRepository.getAllRuleKnowledge(userId) } returns listOf(
            makeRuleKnowledge("r1", knowledgeLevel = 2, timesPracticed = 5), // problem
            makeRuleKnowledge("r2", knowledgeLevel = 3, timesPracticed = 5), // not problem (level ok)
            makeRuleKnowledge("r3", knowledgeLevel = 1, timesPracticed = 2), // not problem (< 3 times)
        )
        coEvery { knowledgeRepository.getAllGrammarRules() } returns listOf(
            makeRule("r1", "Genitiv"),
            makeRule("r2", "Dativ"),
            makeRule("r3", "Nominativ"),
        )
        val result = useCase(userId)
        assertEquals(listOf("Genitiv"), result.grammar.problemRules)
    }

    @Test
    fun invoke_grammar_rulesForReviewTodayFromRepository() = runTest {
        coEvery { knowledgeRepository.getRulesForReviewCount(userId) } returns 7
        val result = useCase(userId)
        assertEquals(7, result.grammar.rulesForReviewToday)
    }

    // ── Pronunciation snapshot ────────────────────────────────────────────────

    @Test
    fun invoke_pronunciation_overallScoreFromRepository() = runTest {
        coEvery { knowledgeRepository.getAveragePronunciationScore(userId) } returns 0.62f
        val result = useCase(userId)
        assertEquals(0.62f, result.pronunciation.overallScore, 0.001f)
    }

    @Test
    fun invoke_pronunciation_problemSoundsHaveScoreBelow0_7() = runTest {
        coEvery { knowledgeRepository.getProblemSounds(userId) } returns listOf(
            makePhoneticTarget("ü", currentScore = 0.5f),   // problem
            makePhoneticTarget("ö", currentScore = 0.8f),   // good
            makePhoneticTarget("ch", currentScore = 0.65f), // problem
        )
        val result = useCase(userId)
        assertTrue(result.pronunciation.problemSounds.contains("ü"))
        assertTrue(result.pronunciation.problemSounds.contains("ch"))
        assertFalse(result.pronunciation.problemSounds.contains("ö"))
    }

    @Test
    fun invoke_pronunciation_goodSoundsHaveScoreAtOrAbove0_7() = runTest {
        coEvery { knowledgeRepository.getProblemSounds(userId) } returns listOf(
            makePhoneticTarget("ü", currentScore = 0.5f),
            makePhoneticTarget("a", currentScore = 0.9f),
        )
        val result = useCase(userId)
        assertTrue(result.pronunciation.goodSounds.contains("a"))
        assertFalse(result.pronunciation.goodSounds.contains("ü"))
    }

    // ── determinePronunciationTrend ───────────────────────────────────────────

    @Test
    fun invoke_pronunciation_trendIsStableWhenNoTargets() = runTest {
        coEvery { knowledgeRepository.getProblemSounds(userId) } returns emptyList()
        val result = useCase(userId)
        assertEquals("stable", result.pronunciation.trend)
    }

    @Test
    fun invoke_pronunciation_trendIsImprovingWhenMoreImproving() = runTest {
        coEvery { knowledgeRepository.getProblemSounds(userId) } returns listOf(
            makePhoneticTarget("ü", trend = PronunciationTrend.IMPROVING),
            makePhoneticTarget("ö", trend = PronunciationTrend.IMPROVING),
            makePhoneticTarget("ch", trend = PronunciationTrend.DECLINING),
        )
        val result = useCase(userId)
        assertEquals("improving", result.pronunciation.trend)
    }

    @Test
    fun invoke_pronunciation_trendIsDecliningWhenMoreDeclining() = runTest {
        coEvery { knowledgeRepository.getProblemSounds(userId) } returns listOf(
            makePhoneticTarget("ü", trend = PronunciationTrend.DECLINING),
            makePhoneticTarget("ö", trend = PronunciationTrend.DECLINING),
            makePhoneticTarget("ch", trend = PronunciationTrend.IMPROVING),
        )
        val result = useCase(userId)
        assertEquals("declining", result.pronunciation.trend)
    }

    @Test
    fun invoke_pronunciation_trendIsStableWhenEqual() = runTest {
        coEvery { knowledgeRepository.getProblemSounds(userId) } returns listOf(
            makePhoneticTarget("ü", trend = PronunciationTrend.IMPROVING),
            makePhoneticTarget("ö", trend = PronunciationTrend.DECLINING),
        )
        val result = useCase(userId)
        assertEquals("stable", result.pronunciation.trend)
    }

    // ── Book progress snapshot ────────────────────────────────────────────────

    @Test
    fun invoke_bookProgress_currentChapterAndLessonFromRepository() = runTest {
        coEvery { bookRepository.getCurrentBookPosition(userId) } returns Pair(4, 7)
        val result = useCase(userId)
        assertEquals(4, result.bookProgress.currentChapter)
        assertEquals(7, result.bookProgress.currentLesson)
    }

    @Test
    fun invoke_bookProgress_totalChaptersFromMetadata() = runTest {
        coEvery { bookRepository.getBookMetadata() } returns makeBookMetadata(totalChapters = 12)
        val result = useCase(userId)
        assertEquals(12, result.bookProgress.totalChapters)
    }

    @Test
    fun invoke_bookProgress_completionPercentageFromRepository() = runTest {
        coEvery { bookRepository.getBookCompletionPercentage(userId) } returns 55.5f
        val result = useCase(userId)
        assertEquals(55.5f, result.bookProgress.completionPercentage, 0.01f)
    }

    @Test
    fun invoke_bookProgress_currentTopicFromChapterTitleRu() = runTest {
        coEvery { bookRepository.getChapter(2) } returns makeChapter("Reise und Urlaub")
        val result = useCase(userId)
        assertEquals("Reise und Urlaub", result.bookProgress.currentTopic)
    }

    @Test
    fun invoke_bookProgress_nullChapter_currentTopicIsEmpty() = runTest {
        coEvery { bookRepository.getChapter(any()) } returns null
        val result = useCase(userId)
        assertEquals("", result.bookProgress.currentTopic)
    }

    // ── Session history snapshot ──────────────────────────────────────────────

    @Test
    fun invoke_sessionHistory_streakFromRepository() = runTest {
        coEvery { sessionRepository.calculateStreak(userId) } returns 14
        val result = useCase(userId)
        assertEquals(14, result.sessionHistory.streak)
    }

    @Test
    fun invoke_sessionHistory_totalSessionsFromRepository() = runTest {
        coEvery { sessionRepository.getSessionCount(userId) } returns 42
        val result = useCase(userId)
        assertEquals(42, result.sessionHistory.totalSessions)
    }

    @Test
    fun invoke_sessionHistory_averageDurationCalculatedCorrectly() = runTest {
        coEvery { sessionRepository.getRecentSessions(userId, 5) } returns listOf(
            makeLearningSession(durationMinutes = 10),
            makeLearningSession(durationMinutes = 30),
        )
        val result = useCase(userId)
        // avg = (10 + 30) / 2 = 20
        assertTrue(result.sessionHistory.averageSessionDuration.contains("20"))
    }

    @Test
    fun invoke_sessionHistory_noSessions_averageDurationIsZero() = runTest {
        coEvery { sessionRepository.getRecentSessions(userId, 5) } returns emptyList()
        val result = useCase(userId)
        assertEquals("0 минут", result.sessionHistory.averageSessionDuration)
    }

    @Test
    fun invoke_sessionHistory_noSessions_lastSessionIsNever() = runTest {
        coEvery { sessionRepository.getRecentSessions(userId, 5) } returns emptyList()
        val result = useCase(userId)
        assertEquals("никогда", result.sessionHistory.lastSession)
    }

    @Test
    fun invoke_sessionHistory_noSessions_lastSessionSummaryIsEmpty() = runTest {
        coEvery { sessionRepository.getRecentSessions(userId, 5) } returns emptyList()
        val result = useCase(userId)
        assertEquals("", result.sessionHistory.lastSessionSummary)
    }

    @Test
    fun invoke_sessionHistory_withSessions_lastSessionSummaryFromFirstSession() = runTest {
        coEvery { sessionRepository.getRecentSessions(userId, 5) } returns listOf(
            makeLearningSession(sessionSummary = "Studied 12 words"),
        )
        val result = useCase(userId)
        assertEquals("Studied 12 words", result.sessionHistory.lastSessionSummary)
    }

    // ── Weak points ───────────────────────────────────────────────────────────

    @Test
    fun invoke_weakPoints_delegatesToGetWeakPointsUseCase() = runTest {
        val weakPoint = mockk<GetWeakPointsUseCase.WeakPoint>(relaxed = true)
        every { weakPoint.description } returns "Проблема с Dativ"
        coEvery { getWeakPointsUseCase(userId) } returns listOf(weakPoint)
        val result = useCase(userId)
        assertTrue(result.weakPoints.contains("Проблема с Dativ"))
    }

    @Test
    fun invoke_weakPoints_emptyWhenUseCaseReturnsEmpty() = runTest {
        coEvery { getWeakPointsUseCase(userId) } returns emptyList()
        val result = useCase(userId)
        assertTrue(result.weakPoints.isEmpty())
    }

    // ── Recommendations: determineStrategy ───────────────────────────────────

    @Test
    fun invoke_recommendations_primaryStrategyIsRepetitionWhenMoreThan10WordsForReview() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount(userId) } returns 11
        val result = useCase(userId)
        assertEquals(LearningStrategy.REPETITION.name, result.recommendations.primaryStrategy)
    }

    @Test
    fun invoke_recommendations_primaryStrategyIsGapFillingWhenMoreThan5ProblemRules() = runTest {
        // wordsForReviewToday = 0 (no REPETITION trigger)
        coEvery { knowledgeRepository.getWordsForReviewCount(userId) } returns 0
        val ruleKnowledgeList = (1..7).map { i ->
            makeRuleKnowledge("r$i", knowledgeLevel = 1, timesPracticed = 5)
        }
        val ruleList = (1..7).map { i -> makeRule("r$i", "Rule$i") }
        coEvery { knowledgeRepository.getAllRuleKnowledge(userId) } returns ruleKnowledgeList
        coEvery { knowledgeRepository.getAllGrammarRules() } returns ruleList
        val result = useCase(userId)
        assertEquals(LearningStrategy.GAP_FILLING.name, result.recommendations.primaryStrategy)
    }

    @Test
    fun invoke_recommendations_primaryStrategyIsPronunciationWhenMoreThan3ProblemSounds() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount(userId) } returns 0
        coEvery { knowledgeRepository.getProblemSounds(userId) } returns listOf(
            makePhoneticTarget("ü", currentScore = 0.4f),
            makePhoneticTarget("ö", currentScore = 0.5f),
            makePhoneticTarget("ä", currentScore = 0.6f),
            makePhoneticTarget("ch", currentScore = 0.3f),
        )
        val result = useCase(userId)
        assertEquals(LearningStrategy.PRONUNCIATION.name, result.recommendations.primaryStrategy)
    }

    @Test
    fun invoke_recommendations_primaryStrategyIsLinearBookByDefault() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount(userId) } returns 5
        coEvery { knowledgeRepository.getProblemSounds(userId) } returns emptyList()
        val result = useCase(userId)
        assertEquals(LearningStrategy.LINEAR_BOOK.name, result.recommendations.primaryStrategy)
    }

    @Test
    fun invoke_recommendations_repetitionThresholdIsExactly10_doesNotTrigger() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount(userId) } returns 10
        val result = useCase(userId)
        assertNotEquals(LearningStrategy.REPETITION.name, result.recommendations.primaryStrategy)
    }

    @Test
    fun invoke_recommendations_secondaryStrategyIsLinearBookWhenPrimaryIsNot() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount(userId) } returns 11
        val result = useCase(userId)
        // primary = REPETITION → secondary = LINEAR_BOOK
        assertEquals(LearningStrategy.LINEAR_BOOK.name, result.recommendations.secondaryStrategy)
    }

    @Test
    fun invoke_recommendations_secondaryStrategyIsRepetitionWhenPrimaryIsLinearBook() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount(userId) } returns 0
        val result = useCase(userId)
        assertEquals(LearningStrategy.REPETITION.name, result.recommendations.secondaryStrategy)
    }

    // ── Recommendations: focusAreas ───────────────────────────────────────────

    @Test
    fun invoke_recommendations_focusAreasContainsWordsForReview() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount(userId) } returns 8
        val result = useCase(userId)
        assertTrue(result.recommendations.focusAreas.any { it.contains("8") })
    }

    @Test
    fun invoke_recommendations_focusAreasContainsProblemRules() = runTest {
        coEvery { knowledgeRepository.getAllRuleKnowledge(userId) } returns listOf(
            makeRuleKnowledge("r1", knowledgeLevel = 1, timesPracticed = 5),
        )
        coEvery { knowledgeRepository.getAllGrammarRules() } returns listOf(
            makeRule("r1", "Akkusativ"),
        )
        val result = useCase(userId)
        assertTrue(result.recommendations.focusAreas.any { it.contains("Akkusativ") })
    }

    @Test
    fun invoke_recommendations_focusAreasContainsPronunciationSounds() = runTest {
        coEvery { knowledgeRepository.getProblemSounds(userId) } returns listOf(
            makePhoneticTarget("ü", currentScore = 0.4f),
        )
        val result = useCase(userId)
        assertTrue(result.recommendations.focusAreas.any { it.contains("ü") })
    }

    @Test
    fun invoke_recommendations_focusAreasLimitedTo3() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount(userId) } returns 5
        coEvery { knowledgeRepository.getAllRuleKnowledge(userId) } returns listOf(
            makeRuleKnowledge("r1", knowledgeLevel = 1, timesPracticed = 5),
        )
        coEvery { knowledgeRepository.getAllGrammarRules() } returns listOf(makeRule("r1", "Dativ"))
        coEvery { knowledgeRepository.getProblemSounds(userId) } returns listOf(
            makePhoneticTarget("ü", currentScore = 0.4f),
        )
        val result = useCase(userId)
        assertTrue(result.recommendations.focusAreas.size <= 3)
    }

    @Test
    fun invoke_recommendations_suggestedSessionDurationIs30Minutes() = runTest {
        val result = useCase(userId)
        assertEquals("30 минут", result.recommendations.suggestedSessionDuration)
    }

    // ── Empty repositories ────────────────────────────────────────────────────

    @Test
    fun invoke_emptyRepositories_doesNotThrow() = runTest {
        coEvery { knowledgeRepository.getAllWordKnowledge(userId) } returns emptyList()
        coEvery { knowledgeRepository.getAllWords() } returns emptyList()
        coEvery { knowledgeRepository.getProblemWords(userId, 5) } returns emptyList()
        coEvery { knowledgeRepository.getWordsForReviewCount(userId) } returns 0
        coEvery { knowledgeRepository.getAllRuleKnowledge(userId) } returns emptyList()
        coEvery { knowledgeRepository.getAllGrammarRules() } returns emptyList()
        coEvery { knowledgeRepository.getRulesForReviewCount(userId) } returns 0
        coEvery { knowledgeRepository.getProblemSounds(userId) } returns emptyList()
        coEvery { knowledgeRepository.getAveragePronunciationScore(userId) } returns 0f
        coEvery { sessionRepository.getRecentSessions(userId, 5) } returns emptyList()
        coEvery { sessionRepository.calculateStreak(userId) } returns 0
        coEvery { sessionRepository.getSessionCount(userId) } returns 0
        coEvery { getWeakPointsUseCase(userId) } returns emptyList()

        assertDoesNotThrow { runTest { useCase(userId) } }
    }

    @Test
    fun invoke_emptyRepositories_totalWordsIsZero() = runTest {
        coEvery { knowledgeRepository.getAllWordKnowledge(userId) } returns emptyList()
        coEvery { knowledgeRepository.getAllWords() } returns emptyList()
        val result = useCase(userId)
        assertEquals(0, result.vocabulary.totalWords)
    }

    @Test
    fun invoke_emptyRepositories_problemWordsIsEmpty() = runTest {
        coEvery { knowledgeRepository.getProblemWords(userId, 5) } returns emptyList()
        val result = useCase(userId)
        assertTrue(result.vocabulary.problemWords.isEmpty())
    }
}
