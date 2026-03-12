// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/knowledge/GetUserKnowledgeUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.GrammarCategory
import com.voicedeutsch.master.domain.model.knowledge.GrammarRule
import com.voicedeutsch.master.domain.model.knowledge.PhraseKnowledge
import com.voicedeutsch.master.domain.model.knowledge.RuleKnowledge
import com.voicedeutsch.master.domain.model.knowledge.Word
import com.voicedeutsch.master.domain.model.knowledge.WordKnowledge
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetUserKnowledgeUseCaseTest {

    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var useCase: GetUserKnowledgeUseCase

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeWordKnowledge(
        wordId: String = "w1",
        knowledgeLevel: Int = 0,
        lastSeen: Long? = null
    ): WordKnowledge = mockk<WordKnowledge>().also {
        every { it.wordId } returns wordId
        every { it.knowledgeLevel } returns knowledgeLevel
        every { it.lastSeen } returns lastSeen
    }

    private fun makeWord(
        id: String = "w1",
        german: String = "Haus",
        russian: String = "дом",
        topic: String = "Home"
    ): Word = mockk<Word>().also {
        every { it.id } returns id
        every { it.german } returns german
        every { it.russian } returns russian
        every { it.topic } returns topic
    }

    private fun makeRuleKnowledge(
        ruleId: String = "r1",
        knowledgeLevel: Int = 0
    ): RuleKnowledge = mockk<RuleKnowledge>().also {
        every { it.ruleId } returns ruleId
        every { it.knowledgeLevel } returns knowledgeLevel
    }

    private fun makePhraseKnowledge(
        knowledgeLevel: Int = 0
    ): PhraseKnowledge = mockk<PhraseKnowledge>().also {
        every { it.knowledgeLevel } returns knowledgeLevel
    }

    private fun makeGrammarRule(
        id: String = "r1",
        categoryName: String = "ARTICLES"
    ): GrammarRule = mockk<GrammarRule>().also {
        val category = mockk<GrammarCategory>()
        every { category.name } returns categoryName
        every { it.id } returns id
        every { it.category } returns category
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        knowledgeRepository = mockk()
        useCase = GetUserKnowledgeUseCase(knowledgeRepository)

        coEvery { knowledgeRepository.getAllWordKnowledge(any()) } returns emptyList()
        coEvery { knowledgeRepository.getAllWords() } returns emptyList()
        coEvery { knowledgeRepository.getWordsForReviewCount(any()) } returns 0
        coEvery { knowledgeRepository.getAllRuleKnowledge(any()) } returns emptyList()
        coEvery { knowledgeRepository.getRulesForReviewCount(any()) } returns 0
        coEvery { knowledgeRepository.getAllPhraseKnowledge(any()) } returns emptyList()
        coEvery { knowledgeRepository.getPhrasesForReviewCount(any()) } returns 0
        coEvery { knowledgeRepository.getAveragePronunciationScore(any()) } returns 0f
        coEvery { knowledgeRepository.getAllGrammarRules() } returns emptyList()
    }

    // ── invoke — all sources empty ────────────────────────────────────────────

    @Test
    fun invoke_allSourcesEmpty_returnsZeroedOverview() = runTest {
        val result = useCase("user1")

        assertEquals(0, result.totalWordsEncountered)
        assertEquals(0, result.wordsKnown)
        assertEquals(0, result.wordsActive)
        assertEquals(0, result.wordsMastered)
        assertEquals(0, result.wordsForReviewToday)
        assertEquals(0, result.totalGrammarRules)
        assertEquals(0, result.rulesKnown)
        assertEquals(0, result.rulesForReviewToday)
        assertEquals(0, result.totalPhrases)
        assertEquals(0, result.phrasesKnown)
        assertEquals(0, result.phrasesForReviewToday)
        assertEquals(0f, result.averagePronunciationScore)
        assertTrue(result.topicDistribution.isEmpty())
        assertTrue(result.recentActivity.isEmpty())
        assertTrue(result.grammarByCategory.isEmpty())
    }

    @Test
    fun invoke_allRepositoryMethodsCalledOnce() = runTest {
        useCase("user42")

        coVerify(exactly = 1) { knowledgeRepository.getAllWordKnowledge("user42") }
        coVerify(exactly = 1) { knowledgeRepository.getAllWords() }
        coVerify(exactly = 1) { knowledgeRepository.getWordsForReviewCount("user42") }
        coVerify(exactly = 1) { knowledgeRepository.getAllRuleKnowledge("user42") }
        coVerify(exactly = 1) { knowledgeRepository.getRulesForReviewCount("user42") }
        coVerify(exactly = 1) { knowledgeRepository.getAllPhraseKnowledge("user42") }
        coVerify(exactly = 1) { knowledgeRepository.getPhrasesForReviewCount("user42") }
        coVerify(exactly = 1) { knowledgeRepository.getAveragePronunciationScore("user42") }
        coVerify(exactly = 1) { knowledgeRepository.getAllGrammarRules() }
    }

    // ── Word counters ─────────────────────────────────────────────────────────

    @Test
    fun invoke_wordsWithLevelAbove0_countedAsTotalEncountered() = runTest {
        coEvery { knowledgeRepository.getAllWordKnowledge("u") } returns listOf(
            makeWordKnowledge(knowledgeLevel = 0),
            makeWordKnowledge(knowledgeLevel = 1),
            makeWordKnowledge(knowledgeLevel = 3)
        )

        val result = useCase("u")

        assertEquals(2, result.totalWordsEncountered)
    }

    @Test
    fun invoke_wordsWithLevel0_notCountedAsEncountered() = runTest {
        coEvery { knowledgeRepository.getAllWordKnowledge("u") } returns listOf(
            makeWordKnowledge(knowledgeLevel = 0),
            makeWordKnowledge(knowledgeLevel = 0)
        )

        val result = useCase("u")

        assertEquals(0, result.totalWordsEncountered)
    }

    @Test
    fun invoke_wordsKnown_levelAtLeast4() = runTest {
        coEvery { knowledgeRepository.getAllWordKnowledge("u") } returns listOf(
            makeWordKnowledge(knowledgeLevel = 3),
            makeWordKnowledge(knowledgeLevel = 4),
            makeWordKnowledge(knowledgeLevel = 7)
        )

        val result = useCase("u")

        assertEquals(2, result.wordsKnown)
    }

    @Test
    fun invoke_wordsActive_levelAtLeast5() = runTest {
        coEvery { knowledgeRepository.getAllWordKnowledge("u") } returns listOf(
            makeWordKnowledge(knowledgeLevel = 4),
            makeWordKnowledge(knowledgeLevel = 5),
            makeWordKnowledge(knowledgeLevel = 6)
        )

        val result = useCase("u")

        assertEquals(2, result.wordsActive)
    }

    @Test
    fun invoke_wordsMastered_levelExactly7() = runTest {
        coEvery { knowledgeRepository.getAllWordKnowledge("u") } returns listOf(
            makeWordKnowledge(knowledgeLevel = 6),
            makeWordKnowledge(knowledgeLevel = 7),
            makeWordKnowledge(knowledgeLevel = 7)
        )

        val result = useCase("u")

        assertEquals(2, result.wordsMastered)
    }

    @Test
    fun invoke_wordsForReviewToday_delegatesToRepository() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount("u") } returns 13

        val result = useCase("u")

        assertEquals(13, result.wordsForReviewToday)
    }

    // ── Grammar counters ──────────────────────────────────────────────────────

    @Test
    fun invoke_grammarRulesWithLevelAbove0_countedAsTotal() = runTest {
        coEvery { knowledgeRepository.getAllRuleKnowledge("u") } returns listOf(
            makeRuleKnowledge(knowledgeLevel = 0),
            makeRuleKnowledge(knowledgeLevel = 1),
            makeRuleKnowledge(knowledgeLevel = 5)
        )

        val result = useCase("u")

        assertEquals(2, result.totalGrammarRules)
    }

    @Test
    fun invoke_rulesKnown_levelAtLeast4() = runTest {
        coEvery { knowledgeRepository.getAllRuleKnowledge("u") } returns listOf(
            makeRuleKnowledge(knowledgeLevel = 3),
            makeRuleKnowledge(knowledgeLevel = 4),
            makeRuleKnowledge(knowledgeLevel = 6)
        )

        val result = useCase("u")

        assertEquals(2, result.rulesKnown)
    }

    @Test
    fun invoke_rulesForReviewToday_delegatesToRepository() = runTest {
        coEvery { knowledgeRepository.getRulesForReviewCount("u") } returns 7

        val result = useCase("u")

        assertEquals(7, result.rulesForReviewToday)
    }

    // ── Phrase counters ───────────────────────────────────────────────────────

    @Test
    fun invoke_phrasesWithLevelAbove0_countedAsTotal() = runTest {
        coEvery { knowledgeRepository.getAllPhraseKnowledge("u") } returns listOf(
            makePhraseKnowledge(knowledgeLevel = 0),
            makePhraseKnowledge(knowledgeLevel = 2),
            makePhraseKnowledge(knowledgeLevel = 5)
        )

        val result = useCase("u")

        assertEquals(2, result.totalPhrases)
    }

    @Test
    fun invoke_phrasesKnown_levelAtLeast4() = runTest {
        coEvery { knowledgeRepository.getAllPhraseKnowledge("u") } returns listOf(
            makePhraseKnowledge(knowledgeLevel = 3),
            makePhraseKnowledge(knowledgeLevel = 4),
            makePhraseKnowledge(knowledgeLevel = 7)
        )

        val result = useCase("u")

        assertEquals(2, result.phrasesKnown)
    }

    @Test
    fun invoke_phrasesForReviewToday_delegatesToRepository() = runTest {
        coEvery { knowledgeRepository.getPhrasesForReviewCount("u") } returns 5

        val result = useCase("u")

        assertEquals(5, result.phrasesForReviewToday)
    }

    // ── Pronunciation score ───────────────────────────────────────────────────

    @Test
    fun invoke_averagePronunciationScore_delegatesToRepository() = runTest {
        coEvery { knowledgeRepository.getAveragePronunciationScore("u") } returns 0.82f

        val result = useCase("u")

        assertEquals(0.82f, result.averagePronunciationScore, 0.001f)
    }

    @Test
    fun invoke_zeroPronunciationScore_returnedAsIs() = runTest {
        coEvery { knowledgeRepository.getAveragePronunciationScore("u") } returns 0f

        val result = useCase("u")

        assertEquals(0f, result.averagePronunciationScore)
    }

    // ── Topic distribution ────────────────────────────────────────────────────

    @Test
    fun invoke_knownWordsWithTopics_accumulatesTopicDistribution() = runTest {
        val wk1 = makeWordKnowledge(wordId = "w1", knowledgeLevel = 4)
        val wk2 = makeWordKnowledge(wordId = "w2", knowledgeLevel = 5)
        val wk3 = makeWordKnowledge(wordId = "w3", knowledgeLevel = 6)
        coEvery { knowledgeRepository.getAllWordKnowledge("u") } returns listOf(wk1, wk2, wk3)
        coEvery { knowledgeRepository.getAllWords() } returns listOf(
            makeWord(id = "w1", topic = "Home"),
            makeWord(id = "w2", topic = "Home"),
            makeWord(id = "w3", topic = "Travel")
        )

        val result = useCase("u")

        assertEquals(2, result.topicDistribution["Home"])
        assertEquals(1, result.topicDistribution["Travel"])
    }

    @Test
    fun invoke_wordBelowKnownLevel_notIncludedInTopicDistribution() = runTest {
        val wk = makeWordKnowledge(wordId = "w1", knowledgeLevel = 3)
        coEvery { knowledgeRepository.getAllWordKnowledge("u") } returns listOf(wk)
        coEvery { knowledgeRepository.getAllWords() } returns listOf(makeWord(id = "w1", topic = "Home"))

        val result = useCase("u")

        assertTrue(result.topicDistribution.isEmpty())
    }

    @Test
    fun invoke_knownWordWithNoMatchingWordEntry_skippedInTopicDistribution() = runTest {
        val wk = makeWordKnowledge(wordId = "missing", knowledgeLevel = 5)
        coEvery { knowledgeRepository.getAllWordKnowledge("u") } returns listOf(wk)
        coEvery { knowledgeRepository.getAllWords() } returns emptyList()

        val result = useCase("u")

        assertTrue(result.topicDistribution.isEmpty())
    }

    @Test
    fun invoke_multipleTopics_eachCountedIndependently() = runTest {
        val wordKnowledgeList = listOf(
            makeWordKnowledge(wordId = "w1", knowledgeLevel = 4),
            makeWordKnowledge(wordId = "w2", knowledgeLevel = 4),
            makeWordKnowledge(wordId = "w3", knowledgeLevel = 4)
        )
        coEvery { knowledgeRepository.getAllWordKnowledge("u") } returns wordKnowledgeList
        coEvery { knowledgeRepository.getAllWords() } returns listOf(
            makeWord(id = "w1", topic = "A"),
            makeWord(id = "w2", topic = "B"),
            makeWord(id = "w3", topic = "C")
        )

        val result = useCase("u")

        assertEquals(3, result.topicDistribution.size)
        result.topicDistribution.values.forEach { assertEquals(1, it) }
    }

    // ── Grammar by category ───────────────────────────────────────────────────

    @Test
    fun invoke_allRulesInCategoryKnown_categoryRatioIs1() = runTest {
        val rules = listOf(makeGrammarRule(id = "r1", categoryName = "VERBS"))
        val ruleKnowledge = listOf(makeRuleKnowledge(ruleId = "r1", knowledgeLevel = 4))
        coEvery { knowledgeRepository.getAllGrammarRules() } returns rules
        coEvery { knowledgeRepository.getAllRuleKnowledge("u") } returns ruleKnowledge

        val result = useCase("u")

        assertEquals(1f, result.grammarByCategory["VERBS"]!!, 0.001f)
    }

    @Test
    fun invoke_noRulesInCategoryKnown_categoryRatioIs0() = runTest {
        val rules = listOf(makeGrammarRule(id = "r1", categoryName = "VERBS"))
        val ruleKnowledge = listOf(makeRuleKnowledge(ruleId = "r1", knowledgeLevel = 3))
        coEvery { knowledgeRepository.getAllGrammarRules() } returns rules
        coEvery { knowledgeRepository.getAllRuleKnowledge("u") } returns ruleKnowledge

        val result = useCase("u")

        assertEquals(0f, result.grammarByCategory["VERBS"]!!, 0.001f)
    }

    @Test
    fun invoke_halfRulesInCategoryKnown_categoryRatioIsHalf() = runTest {
        val rules = listOf(
            makeGrammarRule(id = "r1", categoryName = "NOUNS"),
            makeGrammarRule(id = "r2", categoryName = "NOUNS")
        )
        val ruleKnowledge = listOf(
            makeRuleKnowledge(ruleId = "r1", knowledgeLevel = 4),
            makeRuleKnowledge(ruleId = "r2", knowledgeLevel = 1)
        )
        coEvery { knowledgeRepository.getAllGrammarRules() } returns rules
        coEvery { knowledgeRepository.getAllRuleKnowledge("u") } returns ruleKnowledge

        val result = useCase("u")

        assertEquals(0.5f, result.grammarByCategory["NOUNS"]!!, 0.001f)
    }

    @Test
    fun invoke_multipleCategories_calculatedIndependently() = runTest {
        val rules = listOf(
            makeGrammarRule(id = "r1", categoryName = "VERBS"),
            makeGrammarRule(id = "r2", categoryName = "ARTICLES")
        )
        val ruleKnowledge = listOf(
            makeRuleKnowledge(ruleId = "r1", knowledgeLevel = 5),
            makeRuleKnowledge(ruleId = "r2", knowledgeLevel = 2)
        )
        coEvery { knowledgeRepository.getAllGrammarRules() } returns rules
        coEvery { knowledgeRepository.getAllRuleKnowledge("u") } returns ruleKnowledge

        val result = useCase("u")

        assertEquals(1f, result.grammarByCategory["VERBS"]!!, 0.001f)
        assertEquals(0f, result.grammarByCategory["ARTICLES"]!!, 0.001f)
    }

    @Test
    fun invoke_ruleNotInUserKnowledge_countedAsUnknownInCategory() = runTest {
        val rules = listOf(
            makeGrammarRule(id = "r1", categoryName = "VERBS"),
            makeGrammarRule(id = "r2", categoryName = "VERBS")
        )
        coEvery { knowledgeRepository.getAllGrammarRules() } returns rules
        coEvery { knowledgeRepository.getAllRuleKnowledge("u") } returns emptyList()

        val result = useCase("u")

        assertEquals(0f, result.grammarByCategory["VERBS"]!!, 0.001f)
    }

    // ── Recent activity ───────────────────────────────────────────────────────

    @Test
    fun invoke_wordsWithLastSeen_includedInRecentActivity() = runTest {
        val wk = makeWordKnowledge(wordId = "w1", knowledgeLevel = 3, lastSeen = 1000L)
        coEvery { knowledgeRepository.getAllWordKnowledge("u") } returns listOf(wk)
        coEvery { knowledgeRepository.getAllWords() } returns listOf(
            makeWord(id = "w1", german = "Katze", russian = "кошка")
        )

        val result = useCase("u")

        assertEquals(1, result.recentActivity.size)
        assertEquals("Katze", result.recentActivity[0].wordGerman)
        assertEquals("кошка", result.recentActivity[0].wordRussian)
        assertEquals(3, result.recentActivity[0].knowledgeLevel)
        assertEquals(1000L, result.recentActivity[0].lastSeen)
    }

    @Test
    fun invoke_wordsWithNullLastSeen_excludedFromRecentActivity() = runTest {
        val wk = makeWordKnowledge(wordId = "w1", knowledgeLevel = 3, lastSeen = null)
        coEvery { knowledgeRepository.getAllWordKnowledge("u") } returns listOf(wk)
        coEvery { knowledgeRepository.getAllWords() } returns listOf(makeWord(id = "w1"))

        val result = useCase("u")

        assertTrue(result.recentActivity.isEmpty())
    }

    @Test
    fun invoke_recentActivitySortedByLastSeenDescending() = runTest {
        val wk1 = makeWordKnowledge(wordId = "w1", lastSeen = 100L)
        val wk2 = makeWordKnowledge(wordId = "w2", lastSeen = 500L)
        val wk3 = makeWordKnowledge(wordId = "w3", lastSeen = 300L)
        coEvery { knowledgeRepository.getAllWordKnowledge("u") } returns listOf(wk1, wk2, wk3)
        coEvery { knowledgeRepository.getAllWords() } returns listOf(
            makeWord(id = "w1", german = "A"),
            makeWord(id = "w2", german = "B"),
            makeWord(id = "w3", german = "C")
        )

        val result = useCase("u")

        assertEquals("B", result.recentActivity[0].wordGerman)
        assertEquals("C", result.recentActivity[1].wordGerman)
        assertEquals("A", result.recentActivity[2].wordGerman)
    }

    @Test
    fun invoke_recentActivityCappedAt20() = runTest {
        val wordKnowledgeList = List(30) { i ->
            makeWordKnowledge(wordId = "w$i", lastSeen = i.toLong())
        }
        val words = List(30) { i -> makeWord(id = "w$i") }
        coEvery { knowledgeRepository.getAllWordKnowledge("u") } returns wordKnowledgeList
        coEvery { knowledgeRepository.getAllWords() } returns words

        val result = useCase("u")

        assertEquals(20, result.recentActivity.size)
    }

    @Test
    fun invoke_wordInRecentActivityWithNoWordEntry_skipped() = runTest {
        val wk = makeWordKnowledge(wordId = "missing", lastSeen = 999L)
        coEvery { knowledgeRepository.getAllWordKnowledge("u") } returns listOf(wk)
        coEvery { knowledgeRepository.getAllWords() } returns emptyList()

        val result = useCase("u")

        assertTrue(result.recentActivity.isEmpty())
    }

    @Test
    fun invoke_recentActivityLastSeenNullFallsBackTo0InMapping() = runTest {
        // lastSeen is non-null (filter passes), but edge: verify lastSeen stored correctly
        val wk = makeWordKnowledge(wordId = "w1", lastSeen = 42L)
        coEvery { knowledgeRepository.getAllWordKnowledge("u") } returns listOf(wk)
        coEvery { knowledgeRepository.getAllWords() } returns listOf(makeWord(id = "w1"))

        val result = useCase("u")

        assertEquals(42L, result.recentActivity[0].lastSeen)
    }

    // ── UserKnowledgeOverview data class ──────────────────────────────────────

    @Test
    fun userKnowledgeOverview_creation_storesAllFields() {
        val overview = GetUserKnowledgeUseCase.UserKnowledgeOverview(
            totalWordsEncountered = 100,
            wordsKnown = 50,
            wordsActive = 30,
            wordsMastered = 10,
            wordsForReviewToday = 5,
            totalGrammarRules = 20,
            rulesKnown = 10,
            rulesForReviewToday = 3,
            totalPhrases = 15,
            phrasesKnown = 8,
            phrasesForReviewToday = 2,
            averagePronunciationScore = 0.75f,
            topicDistribution = mapOf("Home" to 5),
            recentActivity = emptyList(),
            grammarByCategory = mapOf("VERBS" to 0.8f)
        )

        assertEquals(100, overview.totalWordsEncountered)
        assertEquals(50, overview.wordsKnown)
        assertEquals(30, overview.wordsActive)
        assertEquals(10, overview.wordsMastered)
        assertEquals(5, overview.wordsForReviewToday)
        assertEquals(20, overview.totalGrammarRules)
        assertEquals(10, overview.rulesKnown)
        assertEquals(3, overview.rulesForReviewToday)
        assertEquals(15, overview.totalPhrases)
        assertEquals(8, overview.phrasesKnown)
        assertEquals(2, overview.phrasesForReviewToday)
        assertEquals(0.75f, overview.averagePronunciationScore)
        assertEquals(mapOf("Home" to 5), overview.topicDistribution)
        assertEquals(mapOf("VERBS" to 0.8f), overview.grammarByCategory)
    }

    @Test
    fun userKnowledgeOverview_defaultGrammarByCategory_isEmpty() {
        val overview = GetUserKnowledgeUseCase.UserKnowledgeOverview(
            totalWordsEncountered = 0, wordsKnown = 0, wordsActive = 0,
            wordsMastered = 0, wordsForReviewToday = 0, totalGrammarRules = 0,
            rulesKnown = 0, rulesForReviewToday = 0, totalPhrases = 0,
            phrasesKnown = 0, phrasesForReviewToday = 0,
            averagePronunciationScore = 0f,
            topicDistribution = emptyMap(),
            recentActivity = emptyList()
        )

        assertTrue(overview.grammarByCategory.isEmpty())
    }

    @Test
    fun userKnowledgeOverview_copy_changesOnlySpecifiedField() {
        val original = GetUserKnowledgeUseCase.UserKnowledgeOverview(
            totalWordsEncountered = 10, wordsKnown = 5, wordsActive = 3,
            wordsMastered = 1, wordsForReviewToday = 2, totalGrammarRules = 4,
            rulesKnown = 2, rulesForReviewToday = 1, totalPhrases = 6,
            phrasesKnown = 3, phrasesForReviewToday = 1,
            averagePronunciationScore = 0.5f,
            topicDistribution = emptyMap(),
            recentActivity = emptyList()
        )
        val copy = original.copy(wordsKnown = 99)

        assertEquals(99, copy.wordsKnown)
        assertEquals(original.totalWordsEncountered, copy.totalWordsEncountered)
        assertEquals(original.averagePronunciationScore, copy.averagePronunciationScore)
    }

    @Test
    fun userKnowledgeOverview_equals_twoIdenticalInstancesAreEqual() {
        val a = GetUserKnowledgeUseCase.UserKnowledgeOverview(
            totalWordsEncountered = 1, wordsKnown = 1, wordsActive = 1,
            wordsMastered = 1, wordsForReviewToday = 1, totalGrammarRules = 1,
            rulesKnown = 1, rulesForReviewToday = 1, totalPhrases = 1,
            phrasesKnown = 1, phrasesForReviewToday = 1,
            averagePronunciationScore = 1f,
            topicDistribution = emptyMap(),
            recentActivity = emptyList()
        )
        val b = a.copy()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ── RecentWordActivity data class ─────────────────────────────────────────

    @Test
    fun recentWordActivity_creation_storesAllFields() {
        val activity = GetUserKnowledgeUseCase.RecentWordActivity(
            wordGerman = "Apfel",
            wordRussian = "яблоко",
            knowledgeLevel = 3,
            lastSeen = 123456789L
        )

        assertEquals("Apfel", activity.wordGerman)
        assertEquals("яблоко", activity.wordRussian)
        assertEquals(3, activity.knowledgeLevel)
        assertEquals(123456789L, activity.lastSeen)
    }

    @Test
    fun recentWordActivity_copy_changesOnlySpecifiedField() {
        val original = GetUserKnowledgeUseCase.RecentWordActivity("A", "Б", 2, 100L)
        val copy = original.copy(knowledgeLevel = 5)

        assertEquals(5, copy.knowledgeLevel)
        assertEquals("A", copy.wordGerman)
        assertEquals("Б", copy.wordRussian)
        assertEquals(100L, copy.lastSeen)
    }

    @Test
    fun recentWordActivity_equals_twoIdenticalInstancesAreEqual() {
        val a = GetUserKnowledgeUseCase.RecentWordActivity("W", "С", 4, 1000L)
        val b = GetUserKnowledgeUseCase.RecentWordActivity("W", "С", 4, 1000L)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun recentWordActivity_equals_differentLastSeenNotEqual() {
        val a = GetUserKnowledgeUseCase.RecentWordActivity("W", "С", 4, 1000L)
        val b = GetUserKnowledgeUseCase.RecentWordActivity("W", "С", 4, 2000L)

        assertNotEquals(a, b)
    }
}
