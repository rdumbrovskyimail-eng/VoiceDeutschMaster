// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/knowledge/GetWeakPointsUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.GrammarRule
import com.voicedeutsch.master.domain.model.knowledge.Mistake
import com.voicedeutsch.master.domain.model.knowledge.MistakeType
import com.voicedeutsch.master.domain.model.knowledge.RuleKnowledge
import com.voicedeutsch.master.domain.model.knowledge.Word
import com.voicedeutsch.master.domain.model.knowledge.WordKnowledge
import com.voicedeutsch.master.domain.model.speech.ProblemSound
import com.voicedeutsch.master.domain.model.speech.PronunciationTrend
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetWeakPointsUseCaseTest {

    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var useCase: GetWeakPointsUseCase

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeWord(german: String = "Haus", russian: String = "дом"): Word =
        mockk<Word>().also {
            every { it.german } returns german
            every { it.russian } returns russian
        }

    private fun makeWordKnowledge(
        accuracy: Float = 0.2f,
        timesCorrect: Int = 2,
        timesIncorrect: Int = 8
    ): WordKnowledge = mockk<WordKnowledge>().also {
        every { it.accuracy } returns accuracy
        every { it.timesCorrect } returns timesCorrect
        every { it.timesIncorrect } returns timesIncorrect
    }

    private fun makeRuleKnowledge(
        ruleId: String = "rule_1",
        knowledgeLevel: Int = 1,
        timesPracticed: Int = 5,
        accuracy: Float = 0.3f
    ): RuleKnowledge = mockk<RuleKnowledge>().also {
        every { it.ruleId } returns ruleId
        every { it.knowledgeLevel } returns knowledgeLevel
        every { it.timesPracticed } returns timesPracticed
        every { it.accuracy } returns accuracy
    }

    private fun makeGrammarRule(nameRu: String = "Артикль"): GrammarRule =
        mockk<GrammarRule>().also { every { it.nameRu } returns nameRu }

    private fun makeProblemSound(
        sound: String = "ü",
        ipa: String = "y",
        currentScore: Float = 0.3f,
        totalAttempts: Int = 10,
        trend: PronunciationTrend = PronunciationTrend.STABLE
    ): ProblemSound = mockk<ProblemSound>().also {
        every { it.sound } returns sound
        every { it.ipa } returns ipa
        every { it.currentScore } returns currentScore
        every { it.totalAttempts } returns totalAttempts
        every { it.trend } returns trend
    }

    private fun makeMistake(type: MistakeType, item: String): Mistake =
        mockk<Mistake>().also {
            every { it.type } returns type
            every { it.item } returns item
        }

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        knowledgeRepository = mockk()
        useCase = GetWeakPointsUseCase(knowledgeRepository)

        coEvery { knowledgeRepository.getProblemWords(any(), any()) } returns emptyList()
        coEvery { knowledgeRepository.getAllRuleKnowledge(any()) } returns emptyList()
        coEvery { knowledgeRepository.getProblemSounds(any()) } returns emptyList()
        coEvery { knowledgeRepository.getMistakes(any(), any()) } returns emptyList()
        coEvery { knowledgeRepository.getGrammarRule(any()) } returns null
    }

    // ── invoke — general ──────────────────────────────────────────────────────

    @Test
    fun invoke_allSourcesEmpty_returnsEmptyList() = runTest {
        val result = useCase("user1")
        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_resultSortedBySeverityDescending() = runTest {
        val words = listOf(
            makeWord("A", "а") to makeWordKnowledge(accuracy = 0.8f),
            makeWord("B", "б") to makeWordKnowledge(accuracy = 0.1f),
            makeWord("C", "в") to makeWordKnowledge(accuracy = 0.5f)
        )
        coEvery { knowledgeRepository.getProblemWords("u", 10) } returns words

        val result = useCase("u")

        for (i in 0 until result.size - 1) {
            assertTrue(result[i].severity >= result[i + 1].severity)
        }
    }

    @Test
    fun invoke_defaultLimit_returnsAtMost10Items() = runTest {
        val words = List(15) { i ->
            makeWord("W$i", "с$i") to makeWordKnowledge(accuracy = 0.1f)
        }
        coEvery { knowledgeRepository.getProblemWords("u", 10) } returns words

        val result = useCase("u")

        assertTrue(result.size <= 10)
    }

    @Test
    fun invoke_customLimit_returnsAtMostLimitItems() = runTest {
        val words = List(20) { i ->
            makeWord("W$i", "с$i") to makeWordKnowledge(accuracy = 0.1f)
        }
        coEvery { knowledgeRepository.getProblemWords("u", 10) } returns words

        val result = useCase("u", limit = 3)

        assertEquals(3, result.size)
    }

    @Test
    fun invoke_allRepositoryMethodsCalledOnce() = runTest {
        useCase("user42")

        coVerify(exactly = 1) { knowledgeRepository.getProblemWords("user42", 10) }
        coVerify(exactly = 1) { knowledgeRepository.getAllRuleKnowledge("user42") }
        coVerify(exactly = 1) { knowledgeRepository.getProblemSounds("user42") }
        coVerify(exactly = 1) { knowledgeRepository.getMistakes("user42", 50) }
    }

    // ── Vocabulary weak points ────────────────────────────────────────────────

    @Test
    fun invoke_vocabularyAccuracyPositive_severityIsOneMinusAccuracy() = runTest {
        coEvery { knowledgeRepository.getProblemWords("u", 10) } returns
            listOf(makeWord() to makeWordKnowledge(accuracy = 0.4f))

        val result = useCase("u")

        val wp = result.single { it.category == "vocabulary" }
        assertEquals(0.6f, wp.severity, 0.001f)
    }

    @Test
    fun invoke_vocabularyAccuracyZero_severityIsOne() = runTest {
        coEvery { knowledgeRepository.getProblemWords("u", 10) } returns
            listOf(makeWord() to makeWordKnowledge(accuracy = 0f))

        val result = useCase("u")

        val wp = result.single { it.category == "vocabulary" }
        assertEquals(1f, wp.severity, 0.001f)
    }

    @Test
    fun invoke_vocabularySeverityClampedToMax1() = runTest {
        coEvery { knowledgeRepository.getProblemWords("u", 10) } returns
            listOf(makeWord() to makeWordKnowledge(accuracy = -2f))

        val result = useCase("u")

        val wp = result.single { it.category == "vocabulary" }
        assertTrue(wp.severity <= 1f)
    }

    @Test
    fun invoke_vocabularyDescriptionContainsGermanAndRussian() = runTest {
        coEvery { knowledgeRepository.getProblemWords("u", 10) } returns
            listOf(makeWord("Hund", "собака") to makeWordKnowledge(timesCorrect = 3, timesIncorrect = 7))

        val result = useCase("u")

        val wp = result.single { it.category == "vocabulary" }
        assertTrue(wp.description.contains("Hund"))
        assertTrue(wp.description.contains("собака"))
        assertTrue(wp.description.contains("7"))
        assertTrue(wp.description.contains("3"))
    }

    @Test
    fun invoke_vocabularyCategory_isVocabulary() = runTest {
        coEvery { knowledgeRepository.getProblemWords("u", 10) } returns
            listOf(makeWord() to makeWordKnowledge())

        val result = useCase("u")

        assertEquals("vocabulary", result.single { it.category == "vocabulary" }.category)
    }

    // ── Grammar weak points ───────────────────────────────────────────────────

    @Test
    fun invoke_grammarLevelAtMost2AndPracticed3Plus_addsGrammarPoint() = runTest {
        coEvery { knowledgeRepository.getAllRuleKnowledge("u") } returns
            listOf(makeRuleKnowledge(knowledgeLevel = 2, timesPracticed = 3))
        coEvery { knowledgeRepository.getGrammarRule("rule_1") } returns makeGrammarRule("Nominativ")

        val result = useCase("u")

        val wp = result.single { it.category == "grammar" }
        assertTrue(wp.description.contains("Nominativ"))
        assertTrue(wp.description.contains("2/7"))
    }

    @Test
    fun invoke_grammarLevelExactly2_included() = runTest {
        coEvery { knowledgeRepository.getAllRuleKnowledge("u") } returns
            listOf(makeRuleKnowledge(knowledgeLevel = 2, timesPracticed = 3))
        coEvery { knowledgeRepository.getGrammarRule("rule_1") } returns makeGrammarRule()

        val result = useCase("u")

        assertTrue(result.any { it.category == "grammar" })
    }

    @Test
    fun invoke_grammarLevelAbove2_notIncluded() = runTest {
        coEvery { knowledgeRepository.getAllRuleKnowledge("u") } returns
            listOf(makeRuleKnowledge(knowledgeLevel = 3, timesPracticed = 5))

        val result = useCase("u")

        assertTrue(result.none { it.category == "grammar" })
    }

    @Test
    fun invoke_grammarPracticedBelow3_notIncluded() = runTest {
        coEvery { knowledgeRepository.getAllRuleKnowledge("u") } returns
            listOf(makeRuleKnowledge(knowledgeLevel = 1, timesPracticed = 2))

        val result = useCase("u")

        assertTrue(result.none { it.category == "grammar" })
    }

    @Test
    fun invoke_grammarRuleNotFoundInRepo_skipped() = runTest {
        coEvery { knowledgeRepository.getAllRuleKnowledge("u") } returns
            listOf(makeRuleKnowledge(knowledgeLevel = 1, timesPracticed = 5))
        coEvery { knowledgeRepository.getGrammarRule("rule_1") } returns null

        val result = useCase("u")

        assertTrue(result.none { it.category == "grammar" })
    }

    @Test
    fun invoke_grammarSeverityCalculatedFromLevel() = runTest {
        coEvery { knowledgeRepository.getAllRuleKnowledge("u") } returns
            listOf(makeRuleKnowledge(knowledgeLevel = 1, timesPracticed = 4))
        coEvery { knowledgeRepository.getGrammarRule("rule_1") } returns makeGrammarRule()

        val result = useCase("u")

        val wp = result.single { it.category == "grammar" }
        assertEquals(1f - (1f / 7f), wp.severity, 0.001f)
    }

    @Test
    fun invoke_grammarMoreThan5WeakRules_takesOnly5() = runTest {
        val rules = List(8) { i ->
            makeRuleKnowledge(ruleId = "rule_$i", knowledgeLevel = 1, timesPracticed = 5)
        }
        coEvery { knowledgeRepository.getAllRuleKnowledge("u") } returns rules
        repeat(8) { i ->
            coEvery { knowledgeRepository.getGrammarRule("rule_$i") } returns makeGrammarRule("R$i")
        }

        val result = useCase("u", limit = 100)

        assertTrue(result.count { it.category == "grammar" } <= 5)
    }

    @Test
    fun invoke_grammarRulesSortedByAccuracyAscending_lowestAccuracyFirst() = runTest {
        val rkLow = makeRuleKnowledge(ruleId = "low", knowledgeLevel = 1, timesPracticed = 5, accuracy = 0.1f)
        val rkHigh = makeRuleKnowledge(ruleId = "high", knowledgeLevel = 2, timesPracticed = 3, accuracy = 0.9f)
        coEvery { knowledgeRepository.getAllRuleKnowledge("u") } returns listOf(rkHigh, rkLow)
        coEvery { knowledgeRepository.getGrammarRule("low") } returns makeGrammarRule("Low")
        coEvery { knowledgeRepository.getGrammarRule("high") } returns makeGrammarRule("High")

        val result = useCase("u", limit = 100)

        val grammarPoints = result.filter { it.category == "grammar" }
        assertEquals(2, grammarPoints.size)
        // lowest knowledge_level → highest severity first after overall sort
        assertTrue(grammarPoints.first().severity >= grammarPoints.last().severity)
    }

    // ── Pronunciation weak points ─────────────────────────────────────────────

    @Test
    fun invoke_soundLowScoreStableTrend_addsPronunciationPoint() = runTest {
        coEvery { knowledgeRepository.getProblemSounds("u") } returns
            listOf(makeProblemSound(currentScore = 0.3f, totalAttempts = 8, trend = PronunciationTrend.STABLE))

        val result = useCase("u")

        assertNotNull(result.find { it.category == "pronunciation" })
    }

    @Test
    fun invoke_soundLowScoreDecliningTrend_addsPronunciationPoint() = runTest {
        coEvery { knowledgeRepository.getProblemSounds("u") } returns
            listOf(makeProblemSound(currentScore = 0.4f, totalAttempts = 6, trend = PronunciationTrend.DECLINING))

        val result = useCase("u")

        assertNotNull(result.find { it.category == "pronunciation" })
    }

    @Test
    fun invoke_soundScoreAtOrAbove05_notIncluded() = runTest {
        coEvery { knowledgeRepository.getProblemSounds("u") } returns
            listOf(makeProblemSound(currentScore = 0.5f, totalAttempts = 10, trend = PronunciationTrend.STABLE))

        val result = useCase("u")

        assertTrue(result.none { it.category == "pronunciation" })
    }

    @Test
    fun invoke_soundAttemptsExactly5_notIncluded() = runTest {
        coEvery { knowledgeRepository.getProblemSounds("u") } returns
            listOf(makeProblemSound(currentScore = 0.3f, totalAttempts = 5, trend = PronunciationTrend.STABLE))

        val result = useCase("u")

        assertTrue(result.none { it.category == "pronunciation" })
    }

    @Test
    fun invoke_soundImprovingTrend_notIncluded() = runTest {
        coEvery { knowledgeRepository.getProblemSounds("u") } returns
            listOf(makeProblemSound(currentScore = 0.3f, totalAttempts = 10, trend = PronunciationTrend.IMPROVING))

        val result = useCase("u")

        assertTrue(result.none { it.category == "pronunciation" })
    }

    @Test
    fun invoke_pronunciationSeverityIsOneMinusScore() = runTest {
        coEvery { knowledgeRepository.getProblemSounds("u") } returns
            listOf(makeProblemSound(currentScore = 0.2f, totalAttempts = 8, trend = PronunciationTrend.STABLE))

        val result = useCase("u")

        val wp = result.single { it.category == "pronunciation" }
        assertEquals(0.8f, wp.severity, 0.001f)
    }

    @Test
    fun invoke_pronunciationDescriptionContainsSoundIpaAndPercent() = runTest {
        coEvery { knowledgeRepository.getProblemSounds("u") } returns
            listOf(makeProblemSound(sound = "ö", ipa = "ø", currentScore = 0.35f, totalAttempts = 8))

        val result = useCase("u")

        val wp = result.single { it.category == "pronunciation" }
        assertTrue(wp.description.contains("ö"))
        assertTrue(wp.description.contains("ø"))
        assertTrue(wp.description.contains("35"))
    }

    // ── Mistake pattern weak points ───────────────────────────────────────────

    @Test
    fun invoke_grammarMistakePattern3Times_addsGrammarPatternPoint() = runTest {
        coEvery { knowledgeRepository.getMistakes("u", 50) } returns
            List(3) { makeMistake(MistakeType.GRAMMAR, "Akkusativ") }

        val result = useCase("u")

        val wp = result.single { it.category == "grammar_pattern" }
        assertTrue(wp.description.contains("Akkusativ"))
        assertTrue(wp.description.contains("3"))
    }

    @Test
    fun invoke_wordMistakePattern3Times_addsVocabularyPatternPoint() = runTest {
        coEvery { knowledgeRepository.getMistakes("u", 50) } returns
            List(3) { makeMistake(MistakeType.WORD, "laufen") }

        val result = useCase("u")

        assertNotNull(result.find { it.category == "vocabulary_pattern" })
    }

    @Test
    fun invoke_pronunciationMistakePattern3Times_addsPronunciationPatternPoint() = runTest {
        coEvery { knowledgeRepository.getMistakes("u", 50) } returns
            List(3) { makeMistake(MistakeType.PRONUNCIATION, "ch") }

        val result = useCase("u")

        assertNotNull(result.find { it.category == "pronunciation_pattern" })
    }

    @Test
    fun invoke_phraseMistakePattern3Times_addsPhrasePatternPoint() = runTest {
        coEvery { knowledgeRepository.getMistakes("u", 50) } returns
            List(3) { makeMistake(MistakeType.PHRASE, "Guten Morgen") }

        val result = useCase("u")

        assertNotNull(result.find { it.category == "phrase_pattern" })
    }

    @Test
    fun invoke_mistakePatternLessThan3_notIncluded() = runTest {
        coEvery { knowledgeRepository.getMistakes("u", 50) } returns
            List(2) { makeMistake(MistakeType.GRAMMAR, "Dativ") }

        val result = useCase("u")

        assertTrue(result.none { it.category == "grammar_pattern" })
    }

    @Test
    fun invoke_mistakePatternExactly3_included() = runTest {
        coEvery { knowledgeRepository.getMistakes("u", 50) } returns
            List(3) { makeMistake(MistakeType.GRAMMAR, "Dativ") }

        val result = useCase("u")

        assertTrue(result.any { it.category == "grammar_pattern" })
    }

    @Test
    fun invoke_mistakePatternSeverityIsCountDividedBy10() = runTest {
        coEvery { knowledgeRepository.getMistakes("u", 50) } returns
            List(5) { makeMistake(MistakeType.GRAMMAR, "haben") }

        val result = useCase("u")

        val wp = result.single { it.category == "grammar_pattern" }
        assertEquals(0.5f, wp.severity, 0.001f)
    }

    @Test
    fun invoke_mistakePatternSeverityClampedToMax1() = runTest {
        coEvery { knowledgeRepository.getMistakes("u", 50) } returns
            List(15) { makeMistake(MistakeType.WORD, "sein") }

        val result = useCase("u")

        val wp = result.single { it.category == "vocabulary_pattern" }
        assertEquals(1f, wp.severity, 0.001f)
    }

    @Test
    fun invoke_twoDistinctPatternsForSameType_eachGetsOwnWeakPoint() = runTest {
        coEvery { knowledgeRepository.getMistakes("u", 50) } returns
            List(3) { makeMistake(MistakeType.GRAMMAR, "Dativ") } +
            List(3) { makeMistake(MistakeType.GRAMMAR, "Genitiv") }

        val result = useCase("u", limit = 100)

        assertEquals(2, result.count { it.category == "grammar_pattern" })
    }

    @Test
    fun invoke_mistakesGroupedByTypeIndependently_noCrossContamination() = runTest {
        coEvery { knowledgeRepository.getMistakes("u", 50) } returns
            List(2) { makeMistake(MistakeType.GRAMMAR, "item") } +
            List(2) { makeMistake(MistakeType.WORD, "item") }

        val result = useCase("u")

        assertTrue(result.none { it.category == "grammar_pattern" || it.category == "vocabulary_pattern" })
    }

    // ── WeakPoint data class ──────────────────────────────────────────────────

    @Test
    fun weakPoint_creation_allFieldsStored() {
        val wp = GetWeakPointsUseCase.WeakPoint(
            description = "test desc",
            category = "vocabulary",
            severity = 0.75f
        )
        assertEquals("test desc", wp.description)
        assertEquals("vocabulary", wp.category)
        assertEquals(0.75f, wp.severity)
    }

    @Test
    fun weakPoint_copy_onlyChangedFieldDiffers() {
        val original = GetWeakPointsUseCase.WeakPoint("desc", "grammar", 0.5f)
        val copy = original.copy(severity = 0.9f)
        assertEquals(original.description, copy.description)
        assertEquals(original.category, copy.category)
        assertEquals(0.9f, copy.severity)
    }

    @Test
    fun weakPoint_equals_twoIdenticalInstancesAreEqual() {
        val a = GetWeakPointsUseCase.WeakPoint("d", "vocabulary", 0.3f)
        val b = GetWeakPointsUseCase.WeakPoint("d", "vocabulary", 0.3f)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun weakPoint_equals_differentSeverityNotEqual() {
        val a = GetWeakPointsUseCase.WeakPoint("d", "vocabulary", 0.3f)
        val b = GetWeakPointsUseCase.WeakPoint("d", "vocabulary", 0.4f)
        assertNotEquals(a, b)
    }
}
