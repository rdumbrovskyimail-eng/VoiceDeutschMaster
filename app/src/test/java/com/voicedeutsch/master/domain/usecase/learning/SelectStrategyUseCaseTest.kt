// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/learning/SelectStrategyUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.learning

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.GrammarRule
import com.voicedeutsch.master.domain.model.knowledge.Word
import com.voicedeutsch.master.domain.model.session.Session
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.domain.repository.SessionRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.domain.usecase.knowledge.GetWeakPointsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SelectStrategyUseCaseTest {

    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var bookRepository: BookRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var getWeakPointsUseCase: GetWeakPointsUseCase
    private lateinit var useCase: SelectStrategyUseCase

    private val fixedNow = 1_700_000_000_000L

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeWeakPoint(category: String = "vocabulary", severity: Float = 0.8f) =
        GetWeakPointsUseCase.WeakPoint(
            description = "test weak point",
            category    = category,
            severity    = severity
        )

    private fun makeSession(
        strategiesUsed: List<String> = emptyList(),
        startedAt: Long = fixedNow - 86_400_000L
    ): Session = mockk<Session>(relaxed = true).also {
        every { it.strategiesUsed } returns strategiesUsed
        every { it.startedAt }      returns startedAt
    }

    private fun makeWord(): Word = mockk(relaxed = true)
    private fun makeRule(): GrammarRule = mockk(relaxed = true)

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        knowledgeRepository = mockk()
        bookRepository      = mockk()
        sessionRepository   = mockk()
        userRepository      = mockk()
        getWeakPointsUseCase = mockk()
        useCase = SelectStrategyUseCase(
            knowledgeRepository, bookRepository, sessionRepository,
            userRepository, getWeakPointsUseCase
        )

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        every { com.voicedeutsch.master.util.DateUtils.nowTimestamp() }              returns fixedNow
        every { com.voicedeutsch.master.util.DateUtils.daysBetween(any(), any()) }   returns 1L

        // Default: SRS queue below threshold (≤ 10)
        coEvery { knowledgeRepository.getWordsForReviewCount(any()) } returns 3
        coEvery { knowledgeRepository.getRulesForReviewCount(any()) } returns 3

        // Default: no weak points
        coEvery { getWeakPointsUseCase(any()) } returns emptyList()

        // Default: no skill gap (equal vocab and grammar)
        coEvery { knowledgeRepository.getAllWords() }         returns List(10) { makeWord() }
        coEvery { knowledgeRepository.getAllGrammarRules() }  returns List(10) { makeRule() }
        coEvery { knowledgeRepository.getKnownWordsCount(any()) }  returns 5
        coEvery { knowledgeRepository.getKnownRulesCount(any()) }  returns 5

        // Default: pronunciation session within last 3 days
        coEvery { sessionRepository.getRecentSessions(any(), any()) } returns listOf(
            makeSession(strategiesUsed = listOf("PRONUNCIATION"), startedAt = fixedNow - 86_400_000L)
        )
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── Priority 1: SRS queue > 10 → REPETITION ───────────────────────────────

    @Test
    fun invoke_totalForReviewAbove10_returnsRepetition() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount("u") } returns 8
        coEvery { knowledgeRepository.getRulesForReviewCount("u") } returns 5

        val result = useCase("u")

        assertEquals(LearningStrategy.REPETITION, result.primary)
    }

    @Test
    fun invoke_totalForReviewExactly11_returnsRepetition() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount("u") } returns 6
        coEvery { knowledgeRepository.getRulesForReviewCount("u") } returns 5

        val result = useCase("u")

        assertEquals(LearningStrategy.REPETITION, result.primary)
    }

    @Test
    fun invoke_totalForReviewExactly10_notRepetition() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount("u") } returns 5
        coEvery { knowledgeRepository.getRulesForReviewCount("u") } returns 5

        val result = useCase("u")

        assertNotEquals(LearningStrategy.REPETITION, result.primary)
    }

    @Test
    fun invoke_repetitionPrimary_secondaryIsLinearBook() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount("u") } returns 11
        coEvery { knowledgeRepository.getRulesForReviewCount("u") } returns 0

        val result = useCase("u")

        assertEquals(LearningStrategy.LINEAR_BOOK, result.secondary)
    }

    @Test
    fun invoke_repetitionPrimary_reasonContainsCount() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount("u") } returns 8
        coEvery { knowledgeRepository.getRulesForReviewCount("u") } returns 5

        val result = useCase("u")

        assertTrue(result.reason.contains("13"))
    }

    @Test
    fun invoke_srsQueueAboveThreshold_weakPointsNotChecked() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount("u") } returns 11
        coEvery { knowledgeRepository.getRulesForReviewCount("u") } returns 0

        useCase("u")

        // getWeakPointsUseCase must NOT be called when SRS threshold triggers
        io.mockk.coVerify(exactly = 0) { getWeakPointsUseCase(any()) }
    }

    // ── Priority 2: Weak points > 5 → GAP_FILLING ────────────────────────────

    @Test
    fun invoke_weakPointsAbove5_returnsGapFilling() = runTest {
        coEvery { getWeakPointsUseCase("u") } returns List(6) { makeWeakPoint() }

        val result = useCase("u")

        assertEquals(LearningStrategy.GAP_FILLING, result.primary)
    }

    @Test
    fun invoke_weakPointsExactly6_returnsGapFilling() = runTest {
        coEvery { getWeakPointsUseCase("u") } returns List(6) { makeWeakPoint() }

        val result = useCase("u")

        assertEquals(LearningStrategy.GAP_FILLING, result.primary)
    }

    @Test
    fun invoke_weakPointsExactly5_notGapFilling() = runTest {
        coEvery { getWeakPointsUseCase("u") } returns List(5) { makeWeakPoint() }

        val result = useCase("u")

        assertNotEquals(LearningStrategy.GAP_FILLING, result.primary)
    }

    @Test
    fun invoke_gapFillingPrimary_secondaryIsLinearBook() = runTest {
        coEvery { getWeakPointsUseCase("u") } returns List(6) { makeWeakPoint() }

        val result = useCase("u")

        assertEquals(LearningStrategy.LINEAR_BOOK, result.secondary)
    }

    @Test
    fun invoke_gapFillingPrimary_reasonContainsWeakPointCount() = runTest {
        coEvery { getWeakPointsUseCase("u") } returns List(8) { makeWeakPoint() }

        val result = useCase("u")

        assertTrue(result.reason.contains("8"))
    }

    // ── Priority 3: Skill gap > 2 → VOCABULARY_BOOST or GRAMMAR_DRILL ─────────

    @Test
    fun invoke_vocabBehindGrammar_returnsVocabularyBoost() = runTest {
        // vocabScore = 0/100 = 0 → subLevel = 0
        // grammarScore = 100/100 = 1 → subLevel = 60
        // gap = 60 > 2
        coEvery { knowledgeRepository.getAllWords() }        returns List(100) { makeWord() }
        coEvery { knowledgeRepository.getAllGrammarRules() } returns List(100) { makeRule() }
        coEvery { knowledgeRepository.getKnownWordsCount("u") }  returns 0
        coEvery { knowledgeRepository.getKnownRulesCount("u") }  returns 100

        val result = useCase("u")

        assertEquals(LearningStrategy.VOCABULARY_BOOST, result.primary)
    }

    @Test
    fun invoke_grammarBehindVocab_returnsGrammarDrill() = runTest {
        // vocabScore = 100/100 = 1 → subLevel = 60
        // grammarScore = 0/100 = 0 → subLevel = 0
        // gap = 60 > 2 and vocabSubLevel > grammarSubLevel
        coEvery { knowledgeRepository.getAllWords() }        returns List(100) { makeWord() }
        coEvery { knowledgeRepository.getAllGrammarRules() } returns List(100) { makeRule() }
        coEvery { knowledgeRepository.getKnownWordsCount("u") }  returns 100
        coEvery { knowledgeRepository.getKnownRulesCount("u") }  returns 0

        val result = useCase("u")

        assertEquals(LearningStrategy.GRAMMAR_DRILL, result.primary)
    }

    @Test
    fun invoke_skillGapExactly2_notSkillGapResult() = runTest {
        // gap of exactly 2 → NOT > 2, so returns null from checkSkillGap
        // vocabSubLevel=30, grammarSubLevel=32 → gap = 2
        coEvery { knowledgeRepository.getAllWords() }        returns List(60) { makeWord() }
        coEvery { knowledgeRepository.getAllGrammarRules() } returns List(60) { makeRule() }
        coEvery { knowledgeRepository.getKnownWordsCount("u") }  returns 30  // 30/60 * 60 = 30
        coEvery { knowledgeRepository.getKnownRulesCount("u") }  returns 32  // 32/60 * 60 = 32

        val result = useCase("u")

        assertNotEquals(LearningStrategy.VOCABULARY_BOOST, result.primary)
        assertNotEquals(LearningStrategy.GRAMMAR_DRILL, result.primary)
    }

    @Test
    fun invoke_vocabularyBoostPrimary_secondaryIsLinearBook() = runTest {
        coEvery { knowledgeRepository.getAllWords() }        returns List(100) { makeWord() }
        coEvery { knowledgeRepository.getAllGrammarRules() } returns List(100) { makeRule() }
        coEvery { knowledgeRepository.getKnownWordsCount("u") }  returns 0
        coEvery { knowledgeRepository.getKnownRulesCount("u") }  returns 100

        val result = useCase("u")

        assertEquals(LearningStrategy.LINEAR_BOOK, result.secondary)
    }

    @Test
    fun invoke_skillGap_reasonContainsGapValue() = runTest {
        coEvery { knowledgeRepository.getAllWords() }        returns List(100) { makeWord() }
        coEvery { knowledgeRepository.getAllGrammarRules() } returns List(100) { makeRule() }
        coEvery { knowledgeRepository.getKnownWordsCount("u") }  returns 0
        coEvery { knowledgeRepository.getKnownRulesCount("u") }  returns 100

        val result = useCase("u")

        assertTrue(result.reason.contains("60"))
    }

    @Test
    fun invoke_allWordsEmpty_totalWordsCountClampedTo1() = runTest {
        // allWords empty → max(1, 0) = 1 → vocabScore = knownWords/1
        coEvery { knowledgeRepository.getAllWords() }        returns emptyList()
        coEvery { knowledgeRepository.getAllGrammarRules() } returns List(100) { makeRule() }
        coEvery { knowledgeRepository.getKnownWordsCount("u") }  returns 0
        coEvery { knowledgeRepository.getKnownRulesCount("u") }  returns 100

        // Should not throw; result could be VOCABULARY_BOOST
        val result = useCase("u")
        assertNotNull(result)
    }

    @Test
    fun invoke_allRulesEmpty_totalRulesCountClampedTo1() = runTest {
        coEvery { knowledgeRepository.getAllWords() }        returns List(100) { makeWord() }
        coEvery { knowledgeRepository.getAllGrammarRules() } returns emptyList()
        coEvery { knowledgeRepository.getKnownWordsCount("u") }  returns 100
        coEvery { knowledgeRepository.getKnownRulesCount("u") }  returns 0

        val result = useCase("u")
        assertNotNull(result)
    }

    // ── Priority 4: No pronunciation session in > 3 days → PRONUNCIATION ──────

    @Test
    fun invoke_lastPronunciationOver3DaysAgo_returnsPronunciation() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.daysBetween(any(), fixedNow) } returns 4L
        coEvery { sessionRepository.getRecentSessions("u", 10) } returns listOf(
            makeSession(strategiesUsed = listOf("PRONUNCIATION"))
        )

        val result = useCase("u")

        assertEquals(LearningStrategy.PRONUNCIATION, result.primary)
    }

    @Test
    fun invoke_lastPronunciationExactly3DaysAgo_notPronunciation() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.daysBetween(any(), fixedNow) } returns 3L
        coEvery { sessionRepository.getRecentSessions("u", 10) } returns listOf(
            makeSession(strategiesUsed = listOf("PRONUNCIATION"))
        )

        val result = useCase("u")

        assertNotEquals(LearningStrategy.PRONUNCIATION, result.primary)
    }

    @Test
    fun invoke_noPronunciationSessionAtAll_daysSinceIs999_returnsPronunciation() = runTest {
        coEvery { sessionRepository.getRecentSessions("u", 10) } returns listOf(
            makeSession(strategiesUsed = listOf("GRAMMAR"))
        )

        val result = useCase("u")

        assertEquals(LearningStrategy.PRONUNCIATION, result.primary)
    }

    @Test
    fun invoke_noRecentSessionsAtAll_daysSinceIs999_returnsPronunciation() = runTest {
        coEvery { sessionRepository.getRecentSessions("u", 10) } returns emptyList()

        val result = useCase("u")

        assertEquals(LearningStrategy.PRONUNCIATION, result.primary)
    }

    @Test
    fun invoke_pronunciationStrategyMatchesCaseInsensitive() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.daysBetween(any(), fixedNow) } returns 1L
        coEvery { sessionRepository.getRecentSessions("u", 10) } returns listOf(
            makeSession(strategiesUsed = listOf("pronunciation"))
        )

        val result = useCase("u")

        // Within 3 days → should NOT select PRONUNCIATION
        assertNotEquals(LearningStrategy.PRONUNCIATION, result.primary)
    }

    @Test
    fun invoke_pronunciationPrimary_secondaryIsLinearBook() = runTest {
        coEvery { sessionRepository.getRecentSessions("u", 10) } returns emptyList()

        val result = useCase("u")

        assertEquals(LearningStrategy.LINEAR_BOOK, result.secondary)
    }

    @Test
    fun invoke_pronunciationPrimary_reasonContainsDayCount() = runTest {
        coEvery { sessionRepository.getRecentSessions("u", 10) } returns emptyList()

        val result = useCase("u")

        assertTrue(result.reason.contains("999"))
    }

    // ── Priority 5: Default → LINEAR_BOOK ────────────────────────────────────

    @Test
    fun invoke_noThresholdsMet_returnsLinearBook() = runTest {
        // SRS ≤ 10, weak points ≤ 5, no skill gap, pronunciation recent
        every { com.voicedeutsch.master.util.DateUtils.daysBetween(any(), fixedNow) } returns 1L
        coEvery { sessionRepository.getRecentSessions("u", 10) } returns listOf(
            makeSession(strategiesUsed = listOf("PRONUNCIATION"))
        )

        val result = useCase("u")

        assertEquals(LearningStrategy.LINEAR_BOOK, result.primary)
    }

    @Test
    fun invoke_defaultPrimary_secondaryIsRepetition() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.daysBetween(any(), fixedNow) } returns 1L
        coEvery { sessionRepository.getRecentSessions("u", 10) } returns listOf(
            makeSession(strategiesUsed = listOf("PRONUNCIATION"))
        )

        val result = useCase("u")

        assertEquals(LearningStrategy.REPETITION, result.secondary)
    }

    @Test
    fun invoke_defaultPrimary_reasonIsNotEmpty() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.daysBetween(any(), fixedNow) } returns 1L
        coEvery { sessionRepository.getRecentSessions("u", 10) } returns listOf(
            makeSession(strategiesUsed = listOf("PRONUNCIATION"))
        )

        val result = useCase("u")

        assertTrue(result.reason.isNotBlank())
    }

    // ── Priority ordering ─────────────────────────────────────────────────────

    @Test
    fun invoke_srsAboveThresholdAndWeakPoints_srsWins() = runTest {
        coEvery { knowledgeRepository.getWordsForReviewCount("u") } returns 11
        coEvery { knowledgeRepository.getRulesForReviewCount("u") } returns 0
        coEvery { getWeakPointsUseCase("u") }                       returns List(8) { makeWeakPoint() }

        val result = useCase("u")

        assertEquals(LearningStrategy.REPETITION, result.primary)
    }

    @Test
    fun invoke_weakPointsAboveThresholdAndSkillGap_weakPointsWin() = runTest {
        coEvery { getWeakPointsUseCase("u") } returns List(6) { makeWeakPoint() }
        coEvery { knowledgeRepository.getAllWords() }        returns List(100) { makeWord() }
        coEvery { knowledgeRepository.getAllGrammarRules() } returns List(100) { makeRule() }
        coEvery { knowledgeRepository.getKnownWordsCount("u") }  returns 0
        coEvery { knowledgeRepository.getKnownRulesCount("u") }  returns 100

        val result = useCase("u")

        assertEquals(LearningStrategy.GAP_FILLING, result.primary)
    }

    // ── StrategyRecommendation data class ─────────────────────────────────────

    @Test
    fun strategyRecommendation_creation_storesAllFields() {
        val rec = SelectStrategyUseCase.StrategyRecommendation(
            primary   = LearningStrategy.REPETITION,
            secondary = LearningStrategy.LINEAR_BOOK,
            reason    = "test reason"
        )

        assertEquals(LearningStrategy.REPETITION,   rec.primary)
        assertEquals(LearningStrategy.LINEAR_BOOK,  rec.secondary)
        assertEquals("test reason",                 rec.reason)
    }

    @Test
    fun strategyRecommendation_copy_changesOnlySpecifiedField() {
        val original = SelectStrategyUseCase.StrategyRecommendation(
            primary   = LearningStrategy.PRONUNCIATION,
            secondary = LearningStrategy.REPETITION,
            reason    = "reason"
        )
        val copy = original.copy(reason = "new reason")

        assertEquals("new reason",                 copy.reason)
        assertEquals(original.primary,             copy.primary)
        assertEquals(original.secondary,           copy.secondary)
    }

    @Test
    fun strategyRecommendation_equals_twoIdenticalInstancesAreEqual() {
        val a = SelectStrategyUseCase.StrategyRecommendation(
            LearningStrategy.GAP_FILLING, LearningStrategy.LINEAR_BOOK, "r"
        )
        val b = SelectStrategyUseCase.StrategyRecommendation(
            LearningStrategy.GAP_FILLING, LearningStrategy.LINEAR_BOOK, "r"
        )

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun strategyRecommendation_equals_differentPrimaryNotEqual() {
        val a = SelectStrategyUseCase.StrategyRecommendation(
            LearningStrategy.REPETITION, LearningStrategy.LINEAR_BOOK, "r"
        )
        val b = SelectStrategyUseCase.StrategyRecommendation(
            LearningStrategy.GRAMMAR_DRILL, LearningStrategy.LINEAR_BOOK, "r"
        )

        assertNotEquals(a, b)
    }
}
