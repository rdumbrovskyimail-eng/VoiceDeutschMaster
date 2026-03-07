// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/speech/AnalyzePronunciationUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.speech

import com.voicedeutsch.master.domain.model.speech.PhoneticTarget
import com.voicedeutsch.master.domain.model.speech.PronunciationRecord
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

class AnalyzePronunciationUseCaseTest {

    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var useCase: AnalyzePronunciationUseCase

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeRecord(
        word: String = "Hund",
        score: Float = 0.4f,
        problemSounds: List<String> = listOf("ü"),
        timestamp: Long = 1_000L
    ): PronunciationRecord = mockk<PronunciationRecord>(relaxed = true).also {
        every { it.word }          returns word
        every { it.score }         returns score
        every { it.problemSounds } returns problemSounds
        every { it.timestamp }     returns timestamp
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        knowledgeRepository = mockk()
        useCase = AnalyzePronunciationUseCase(knowledgeRepository)

        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns emptyList()
    }

    // ── invoke — empty records ────────────────────────────────────────────────

    @Test
    fun invoke_noRecords_returnsEmptyList() = runTest {
        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_repositoryCalledWith200Limit() = runTest {
        useCase("user1")

        coVerify(exactly = 1) { knowledgeRepository.getRecentPronunciationRecords("user1", 200) }
    }

    @Test
    fun invoke_repositoryCalledWithCorrectUserId() = runTest {
        useCase("user99")

        coVerify { knowledgeRepository.getRecentPronunciationRecords("user99", any()) }
    }

    // ── invoke — filtering by score < 0.7 ────────────────────────────────────

    @Test
    fun invoke_avgScoreAbove07_soundFilteredOut() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns
            listOf(makeRecord(score = 0.8f, problemSounds = listOf("ü")))

        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_avgScoreExactly07_soundFilteredOut() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns
            listOf(makeRecord(score = 0.7f, problemSounds = listOf("ü")))

        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_avgScoreBelow07_soundIncluded() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns
            listOf(makeRecord(score = 0.4f, problemSounds = listOf("ü")))

        val result = useCase("user1")

        assertEquals(1, result.size)
    }

    @Test
    fun invoke_mixedScores_onlyBelowThresholdReturned() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ü")),   // weak
            makeRecord(score = 0.9f, problemSounds = listOf("ch")),  // strong
            makeRecord(score = 0.5f, problemSounds = listOf("z"))    // weak
        )

        val result = useCase("user1")

        assertEquals(2, result.size)
        assertTrue(result.none { it.sound == "ch" })
    }

    // ── invoke — sound accumulation ───────────────────────────────────────────

    @Test
    fun invoke_multipleRecordsSameSound_attemptsAccumulated() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ü"), timestamp = 100L),
            makeRecord(score = 0.4f, problemSounds = listOf("ü"), timestamp = 200L),
            makeRecord(score = 0.5f, problemSounds = listOf("ü"), timestamp = 300L)
        )

        val result = useCase("user1")

        assertEquals(3, result.single().totalAttempts)
    }

    @Test
    fun invoke_multipleRecordsSameSound_averageScoreCalculated() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.2f, problemSounds = listOf("ü")),
            makeRecord(score = 0.4f, problemSounds = listOf("ü")),
            makeRecord(score = 0.6f, problemSounds = listOf("ü"))
        )

        val result = useCase("user1")

        assertEquals(0.4f, result.single().currentScore, 0.01f)
    }

    @Test
    fun invoke_multipleRecordsSameSound_lastPracticedIsMaxTimestamp() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ü"), timestamp = 100L),
            makeRecord(score = 0.3f, problemSounds = listOf("ü"), timestamp = 500L),
            makeRecord(score = 0.3f, problemSounds = listOf("ü"), timestamp = 300L)
        )

        val result = useCase("user1")

        assertEquals(500L, result.single().lastPracticed)
        assertEquals(500L, result.single().detectionDate)
    }

    @Test
    fun invoke_differentSoundsInSameRecord_eachSoundAccumulatedSeparately() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ü", "ch"))
        )

        val result = useCase("user1")

        assertEquals(2, result.size)
        assertTrue(result.any { it.sound == "ü" })
        assertTrue(result.any { it.sound == "ch" })
    }

    // ── invoke — successfulAttempts (score >= 0.7) ────────────────────────────

    @Test
    fun invoke_scoresAbove07_countedAsSuccessful() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.8f, problemSounds = listOf("z")),  // successful but avg = 0.8, filtered?
            makeRecord(score = 0.2f, problemSounds = listOf("z")),  // avg = 0.5 < 0.7 → passes
            makeRecord(score = 0.2f, problemSounds = listOf("z"))
        )
        // avg = (0.8 + 0.2 + 0.2) / 3 = 0.4 < 0.7 → included

        val result = useCase("user1")

        assertEquals(1, result.single().successfulAttempts)
    }

    @Test
    fun invoke_noScoresAbove07_successfulAttemptsIs0() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ü")),
            makeRecord(score = 0.4f, problemSounds = listOf("ü"))
        )

        val result = useCase("user1")

        assertEquals(0, result.single().successfulAttempts)
    }

    @Test
    fun invoke_scoreExactly07_countedAsSuccessful() = runTest {
        // avg = (0.7 + 0.3) / 2 = 0.5 < 0.7 → included
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.7f, problemSounds = listOf("ü")),
            makeRecord(score = 0.3f, problemSounds = listOf("ü"))
        )

        val result = useCase("user1")

        assertEquals(1, result.single().successfulAttempts)
    }

    // ── invoke — words accumulation (distinct, max 10) ────────────────────────

    @Test
    fun invoke_wordsForSound_distinctUpTo10() = runTest {
        val records = List(15) { i ->
            makeRecord(score = 0.3f, problemSounds = listOf("ü"), word = "word$i")
        }
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns records

        val result = useCase("user1")

        assertTrue(result.single().inWords.size <= 10)
    }

    @Test
    fun invoke_duplicateWordsForSound_deduplicatedInInWords() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ü"), word = "Hund"),
            makeRecord(score = 0.3f, problemSounds = listOf("ü"), word = "Hund"),
            makeRecord(score = 0.3f, problemSounds = listOf("ü"), word = "Mund")
        )

        val result = useCase("user1")

        assertEquals(2, result.single().inWords.size)
        assertEquals(1, result.single().inWords.count { it == "Hund" })
    }

    @Test
    fun invoke_singleWord_inWordsContainsThatWord() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ü"), word = "Müller")
        )

        val result = useCase("user1")

        assertTrue(result.single().inWords.contains("Müller"))
    }

    // ── invoke — trend calculation ────────────────────────────────────────────

    @Test
    fun invoke_fewerThan3RecentScores_trendIsStable() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ü")),
            makeRecord(score = 0.3f, problemSounds = listOf("ü"))
        )

        val result = useCase("user1")

        assertEquals(PronunciationTrend.STABLE, result.single().trend)
    }

    @Test
    fun invoke_recentScoresMuchHigherThanEarlier_trendIsImproving() = runTest {
        // Need >= 5 scores so recentScores.size >= 3
        // takeLast(3).avg > take(3).avg + 0.1
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.2f, problemSounds = listOf("ü")), // early
            makeRecord(score = 0.2f, problemSounds = listOf("ü")), // early
            makeRecord(score = 0.2f, problemSounds = listOf("ü")), // early → take(3) avg = 0.2
            makeRecord(score = 0.5f, problemSounds = listOf("ü")), // recent
            makeRecord(score = 0.5f, problemSounds = listOf("ü")), // recent
            makeRecord(score = 0.5f, problemSounds = listOf("ü"))  // recent → takeLast(3) avg = 0.5
            // diff = 0.5 - 0.2 = 0.3 > 0.1 → IMPROVING; overall avg = (0.2*3+0.5*3)/6 ≈ 0.35 < 0.7
        )

        val result = useCase("user1")

        assertEquals(PronunciationTrend.IMPROVING, result.single().trend)
    }

    @Test
    fun invoke_recentScoresMuchLowerThanEarlier_trendIsDeclining() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.5f, problemSounds = listOf("ü")), // early → take(3) avg = 0.5
            makeRecord(score = 0.5f, problemSounds = listOf("ü")),
            makeRecord(score = 0.5f, problemSounds = listOf("ü")),
            makeRecord(score = 0.2f, problemSounds = listOf("ü")), // recent → takeLast(3) avg = 0.2
            makeRecord(score = 0.2f, problemSounds = listOf("ü")),
            makeRecord(score = 0.2f, problemSounds = listOf("ü"))
            // diff = 0.2 - 0.5 = -0.3 < -0.1 → DECLINING; avg ≈ 0.35 < 0.7
        )

        val result = useCase("user1")

        assertEquals(PronunciationTrend.DECLINING, result.single().trend)
    }

    @Test
    fun invoke_recentScoresDiffWithin01_trendIsStable() = runTest {
        // all scores same → diff = 0 → STABLE
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns
            List(6) { makeRecord(score = 0.3f, problemSounds = listOf("ü")) }

        val result = useCase("user1")

        assertEquals(PronunciationTrend.STABLE, result.single().trend)
    }

    @Test
    fun invoke_exactly3recentScores_trendCalcUsesThose3() = runTest {
        // 3 scores: recentScores.size == 3 >= 3, takeLast(3) = all, take(3) = all → same avg → STABLE
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ü")),
            makeRecord(score = 0.3f, problemSounds = listOf("ü")),
            makeRecord(score = 0.3f, problemSounds = listOf("ü"))
        )

        val result = useCase("user1")

        assertEquals(PronunciationTrend.STABLE, result.single().trend)
    }

    // ── invoke — sorting by currentScore ascending ────────────────────────────

    @Test
    fun invoke_multipleSounds_sortedByCurrentScoreAscending() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.6f, problemSounds = listOf("ch")),
            makeRecord(score = 0.2f, problemSounds = listOf("ü")),
            makeRecord(score = 0.4f, problemSounds = listOf("z"))
        )

        val result = useCase("user1")

        assertEquals("ü",  result[0].sound)
        assertEquals("z",  result[1].sound)
        assertEquals("ch", result[2].sound)
    }

    // ── invoke — sound field ──────────────────────────────────────────────────

    @Test
    fun invoke_soundFieldMatchesKey() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("sch"))
        )

        val result = useCase("user1")

        assertEquals("sch", result.single().sound)
    }

    // ── invoke — IPA mapping ──────────────────────────────────────────────────

    @Test
    fun invoke_sound_ü_ipaIsYLong() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ü"))
        )
        val result = useCase("user1")
        assertEquals("[yː]", result.single().ipa)
    }

    @Test
    fun invoke_sound_ö_ipaIsOeLong() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ö"))
        )
        val result = useCase("user1")
        assertEquals("[øː]", result.single().ipa)
    }

    @Test
    fun invoke_sound_ä_ipaIsELong() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ä"))
        )
        val result = useCase("user1")
        assertEquals("[ɛː]", result.single().ipa)
    }

    @Test
    fun invoke_sound_sch_ipaIsShSound() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("sch"))
        )
        val result = useCase("user1")
        assertEquals("[ʃ]", result.single().ipa)
    }

    @Test
    fun invoke_sound_ch_ipaIsBothVariants() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ch"))
        )
        val result = useCase("user1")
        assertEquals("[ç]/[x]", result.single().ipa)
    }

    @Test
    fun invoke_sound_z_ipaIsTs() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("z"))
        )
        val result = useCase("user1")
        assertEquals("[ts]", result.single().ipa)
    }

    @Test
    fun invoke_sound_r_ipaIsUvularR() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("r"))
        )
        val result = useCase("user1")
        assertEquals("[ʁ]", result.single().ipa)
    }

    @Test
    fun invoke_sound_w_ipaIsV() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("w"))
        )
        val result = useCase("user1")
        assertEquals("[v]", result.single().ipa)
    }

    @Test
    fun invoke_sound_ei_ipaIsDiphthong() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ei"))
        )
        val result = useCase("user1")
        assertEquals("[aɪ]", result.single().ipa)
    }

    @Test
    fun invoke_sound_eu_ipaIsOyDiphthong() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("eu"))
        )
        val result = useCase("user1")
        assertEquals("[ɔʏ]", result.single().ipa)
    }

    @Test
    fun invoke_sound_äu_ipaIsOyDiphthong() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("äu"))
        )
        val result = useCase("user1")
        assertEquals("[ɔʏ]", result.single().ipa)
    }

    @Test
    fun invoke_sound_ss_ipaIsS() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ß"))
        )
        val result = useCase("user1")
        assertEquals("[s]", result.single().ipa)
    }

    @Test
    fun invoke_unknownSound_ipaIsBracketedSound() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("xyz"))
        )
        val result = useCase("user1")
        assertEquals("[xyz]", result.single().ipa)
    }

    @Test
    fun invoke_soundIpaMapping_caseInsensitive() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("Ü"))
        )
        val result = useCase("user1")
        assertEquals("[yː]", result.single().ipa)
    }

    // ── invoke — record with no problem sounds ────────────────────────────────

    @Test
    fun invoke_recordWithNoProblemSounds_noPhoneticTargets() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(problemSounds = emptyList())
        )

        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    // ── invoke — PhoneticTarget result fields ─────────────────────────────────

    @Test
    fun invoke_singleRecord_phoneticTargetFieldsCorrect() = runTest {
        coEvery { knowledgeRepository.getRecentPronunciationRecords(any(), any()) } returns listOf(
            makeRecord(score = 0.3f, problemSounds = listOf("ü"), word = "über", timestamp = 777L)
        )

        val target = useCase("user1").single()

        assertEquals("ü",          target.sound)
        assertEquals("[yː]",       target.ipa)
        assertEquals(777L,         target.lastPracticed)
        assertEquals(777L,         target.detectionDate)
        assertEquals(1,            target.totalAttempts)
        assertEquals(0,            target.successfulAttempts)
        assertEquals(0.3f,         target.currentScore, 0.01f)
        assertEquals(PronunciationTrend.STABLE, target.trend)
        assertTrue(target.inWords.contains("über"))
    }
}
