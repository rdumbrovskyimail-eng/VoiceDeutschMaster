// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/speech/GetPronunciationTargetsUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.speech

import com.voicedeutsch.master.domain.model.speech.PhoneticTarget
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

class GetPronunciationTargetsUseCaseTest {

    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var analyzePronunciation: AnalyzePronunciationUseCase
    private lateinit var useCase: GetPronunciationTargetsUseCase

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeTarget(
        sound: String = "ü",
        currentScore: Float = 0.4f,
        inWords: List<String> = listOf("über", "Müller", "fühlen")
    ): PhoneticTarget = mockk<PhoneticTarget>(relaxed = true).also {
        every { it.sound }        returns sound
        every { it.currentScore } returns currentScore
        every { it.inWords }      returns inWords
        every { it.trend }        returns PronunciationTrend.STABLE
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        knowledgeRepository  = mockk()
        analyzePronunciation = mockk()
        useCase = GetPronunciationTargetsUseCase(knowledgeRepository, analyzePronunciation)

        coEvery { analyzePronunciation(any()) } returns emptyList()
    }

    // ── invoke — empty analysis ───────────────────────────────────────────────

    @Test
    fun invoke_noTargets_returnsEmptyPlan() = runTest {
        val result = useCase("user1")

        assertTrue(result.targets.isEmpty())
        assertTrue(result.practiceWords.isEmpty())
    }

    @Test
    fun invoke_analyzeCalledWithCorrectUserId() = runTest {
        useCase("user42")

        coVerify(exactly = 1) { analyzePronunciation("user42") }
    }

    // ── invoke — targets limited to 5 ────────────────────────────────────────

    @Test
    fun invoke_analyzeReturns7Targets_planContains5() = runTest {
        coEvery { analyzePronunciation(any()) } returns List(7) { i ->
            makeTarget(sound = "s$i")
        }

        val result = useCase("user1")

        assertEquals(5, result.targets.size)
    }

    @Test
    fun invoke_analyzeReturns3Targets_planContainsAll3() = runTest {
        val targets = List(3) { i -> makeTarget(sound = "s$i") }
        coEvery { analyzePronunciation(any()) } returns targets

        val result = useCase("user1")

        assertEquals(3, result.targets.size)
    }

    @Test
    fun invoke_analyzeReturnsExactly5Targets_allIncluded() = runTest {
        val targets = List(5) { i -> makeTarget(sound = "s$i") }
        coEvery { analyzePronunciation(any()) } returns targets

        val result = useCase("user1")

        assertEquals(5, result.targets.size)
    }

    @Test
    fun invoke_analyzeReturnsSingleTarget_planContains1() = runTest {
        coEvery { analyzePronunciation(any()) } returns listOf(makeTarget())

        val result = useCase("user1")

        assertEquals(1, result.targets.size)
    }

    // ── invoke — targets order preserved ─────────────────────────────────────

    @Test
    fun invoke_targetsPreserveOrderFromAnalyze() = runTest {
        val targets = listOf(
            makeTarget(sound = "ü"),
            makeTarget(sound = "ch"),
            makeTarget(sound = "z")
        )
        coEvery { analyzePronunciation(any()) } returns targets

        val result = useCase("user1")

        assertEquals("ü",  result.targets[0].sound)
        assertEquals("ch", result.targets[1].sound)
        assertEquals("z",  result.targets[2].sound)
    }

    // ── invoke — practice words generation ───────────────────────────────────

    @Test
    fun invoke_singleTargetWith3Words_3PracticeWordsGenerated() = runTest {
        coEvery { analyzePronunciation(any()) } returns listOf(
            makeTarget(sound = "ü", inWords = listOf("über", "Müller", "fühlen"))
        )

        val result = useCase("user1")

        assertEquals(3, result.practiceWords.size)
    }

    @Test
    fun invoke_singleTargetWithMoreThan3Words_only3PracticeWords() = runTest {
        coEvery { analyzePronunciation(any()) } returns listOf(
            makeTarget(sound = "ü", inWords = listOf("w1", "w2", "w3", "w4", "w5"))
        )

        val result = useCase("user1")

        assertEquals(3, result.practiceWords.size)
    }

    @Test
    fun invoke_singleTargetWith1Word_1PracticeWord() = runTest {
        coEvery { analyzePronunciation(any()) } returns listOf(
            makeTarget(sound = "ü", inWords = listOf("über"))
        )

        val result = useCase("user1")

        assertEquals(1, result.practiceWords.size)
    }

    @Test
    fun invoke_singleTargetNoWords_noPracticeWords() = runTest {
        coEvery { analyzePronunciation(any()) } returns listOf(
            makeTarget(sound = "ü", inWords = emptyList())
        )

        val result = useCase("user1")

        assertTrue(result.practiceWords.isEmpty())
    }

    @Test
    fun invoke_2TargetsWith3WordsEach_6PracticeWordsTotal() = runTest {
        coEvery { analyzePronunciation(any()) } returns listOf(
            makeTarget(sound = "ü",  inWords = listOf("über", "Müller", "fühlen")),
            makeTarget(sound = "ch", inWords = listOf("Bach", "Nacht", "Dach"))
        )

        val result = useCase("user1")

        assertEquals(6, result.practiceWords.size)
    }

    // ── invoke — practice word fields ─────────────────────────────────────────

    @Test
    fun invoke_practiceWord_germanMatchesWord() = runTest {
        coEvery { analyzePronunciation(any()) } returns listOf(
            makeTarget(sound = "ü", currentScore = 0.35f, inWords = listOf("über"))
        )

        val result = useCase("user1")

        assertEquals("über", result.practiceWords.single().german)
    }

    @Test
    fun invoke_practiceWord_targetSoundMatchesTargetSound() = runTest {
        coEvery { analyzePronunciation(any()) } returns listOf(
            makeTarget(sound = "sch", currentScore = 0.4f, inWords = listOf("Schule"))
        )

        val result = useCase("user1")

        assertEquals("sch", result.practiceWords.single().targetSound)
    }

    @Test
    fun invoke_practiceWord_currentScoreMatchesTargetScore() = runTest {
        coEvery { analyzePronunciation(any()) } returns listOf(
            makeTarget(sound = "ü", currentScore = 0.28f, inWords = listOf("über"))
        )

        val result = useCase("user1")

        assertEquals(0.28f, result.practiceWords.single().currentScore, 0.001f)
    }

    @Test
    fun invoke_multipleWordsForTarget_allShareSameSoundAndScore() = runTest {
        coEvery { analyzePronunciation(any()) } returns listOf(
            makeTarget(sound = "z", currentScore = 0.5f, inWords = listOf("Zeit", "Zucker", "Zug"))
        )

        val result = useCase("user1")

        result.practiceWords.forEach { pw ->
            assertEquals("z",   pw.targetSound)
            assertEquals(0.5f,  pw.currentScore, 0.001f)
        }
    }

    @Test
    fun invoke_wordsPreserveOrder() = runTest {
        coEvery { analyzePronunciation(any()) } returns listOf(
            makeTarget(sound = "ü", inWords = listOf("über", "Müller", "fühlen"))
        )

        val result = useCase("user1")

        assertEquals("über",   result.practiceWords[0].german)
        assertEquals("Müller", result.practiceWords[1].german)
        assertEquals("fühlen", result.practiceWords[2].german)
    }

    @Test
    fun invoke_mixedTargets_practiceWordsSoundMappedCorrectly() = runTest {
        coEvery { analyzePronunciation(any()) } returns listOf(
            makeTarget(sound = "ü",  inWords = listOf("über")),
            makeTarget(sound = "ch", inWords = listOf("Bach"))
        )

        val result = useCase("user1")

        val uWord  = result.practiceWords.first { it.german == "über" }
        val chWord = result.practiceWords.first { it.german == "Bach" }
        assertEquals("ü",  uWord.targetSound)
        assertEquals("ch", chWord.targetSound)
    }

    // ── invoke — targets > 5: only first 5 contribute practice words ──────────

    @Test
    fun invoke_7TargetsWith3WordsEach_max15PracticeWords() = runTest {
        coEvery { analyzePronunciation(any()) } returns List(7) { i ->
            makeTarget(sound = "s$i", inWords = listOf("w${i}a", "w${i}b", "w${i}c"))
        }

        val result = useCase("user1")

        // Only first 5 targets used → 5 * 3 = 15
        assertEquals(15, result.practiceWords.size)
    }

    // ── PronunciationPlan data class ──────────────────────────────────────────

    @Test
    fun pronunciationPlan_creation_storesAllFields() {
        val targets = listOf(makeTarget())
        val words   = listOf(
            GetPronunciationTargetsUseCase.PracticeWord("über", "ü", 0.4f)
        )
        val plan = GetPronunciationTargetsUseCase.PronunciationPlan(
            targets       = targets,
            practiceWords = words
        )

        assertEquals(targets, plan.targets)
        assertEquals(words,   plan.practiceWords)
    }

    @Test
    fun pronunciationPlan_copy_changesOnlySpecifiedField() {
        val original = GetPronunciationTargetsUseCase.PronunciationPlan(emptyList(), emptyList())
        val words    = listOf(GetPronunciationTargetsUseCase.PracticeWord("Hund", "u", 0.5f))
        val copy     = original.copy(practiceWords = words)

        assertEquals(words,            copy.practiceWords)
        assertEquals(original.targets, copy.targets)
    }

    @Test
    fun pronunciationPlan_equals_twoEmptyPlansAreEqual() {
        val a = GetPronunciationTargetsUseCase.PronunciationPlan(emptyList(), emptyList())
        val b = GetPronunciationTargetsUseCase.PronunciationPlan(emptyList(), emptyList())

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ── PracticeWord data class ───────────────────────────────────────────────

    @Test
    fun practiceWord_creation_storesAllFields() {
        val pw = GetPronunciationTargetsUseCase.PracticeWord(
            german      = "Schule",
            targetSound = "sch",
            currentScore = 0.55f
        )

        assertEquals("Schule", pw.german)
        assertEquals("sch",    pw.targetSound)
        assertEquals(0.55f,    pw.currentScore, 0.001f)
    }

    @Test
    fun practiceWord_copy_changesOnlySpecifiedField() {
        val original = GetPronunciationTargetsUseCase.PracticeWord("Hund", "u", 0.3f)
        val copy     = original.copy(currentScore = 0.6f)

        assertEquals(0.6f,             copy.currentScore, 0.001f)
        assertEquals(original.german,      copy.german)
        assertEquals(original.targetSound, copy.targetSound)
    }

    @Test
    fun practiceWord_equals_twoIdenticalInstancesAreEqual() {
        val a = GetPronunciationTargetsUseCase.PracticeWord("über", "ü", 0.4f)
        val b = GetPronunciationTargetsUseCase.PracticeWord("über", "ü", 0.4f)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun practiceWord_equals_differentGermanNotEqual() {
        val a = GetPronunciationTargetsUseCase.PracticeWord("über", "ü", 0.4f)
        val b = GetPronunciationTargetsUseCase.PracticeWord("müde", "ü", 0.4f)

        assertNotEquals(a, b)
    }

    @Test
    fun practiceWord_equals_differentSoundNotEqual() {
        val a = GetPronunciationTargetsUseCase.PracticeWord("Bach", "ch", 0.4f)
        val b = GetPronunciationTargetsUseCase.PracticeWord("Bach", "a",  0.4f)

        assertNotEquals(a, b)
    }

    @Test
    fun practiceWord_equals_differentScoreNotEqual() {
        val a = GetPronunciationTargetsUseCase.PracticeWord("Bach", "ch", 0.3f)
        val b = GetPronunciationTargetsUseCase.PracticeWord("Bach", "ch", 0.5f)

        assertNotEquals(a, b)
    }
}
