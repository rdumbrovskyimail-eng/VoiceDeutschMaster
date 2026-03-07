// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/knowledge/UpdatePhraseKnowledgeUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.Phrase
import com.voicedeutsch.master.domain.model.knowledge.PhraseKnowledge
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.util.Constants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdatePhraseKnowledgeUseCaseTest {

    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var useCase: UpdatePhraseKnowledgeUseCase

    private val fixedNow = 1_700_000_000_000L
    private val fixedUUID = "test-phrase-uuid"
    private val fixedNextReview = fixedNow + 86_400_000L

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeParams(
        userId: String = "user1",
        phraseId: String = "phrase1",
        newLevel: Int = 3,
        quality: Int = 4,
        pronunciationScore: Float? = null
    ) = UpdatePhraseKnowledgeUseCase.Params(
        userId = userId,
        phraseId = phraseId,
        newLevel = newLevel,
        quality = quality,
        pronunciationScore = pronunciationScore
    )

    private fun makeExistingPhraseKnowledge(
        knowledgeLevel: Int = 2,
        timesPracticed: Int = 3,
        timesCorrect: Int = 2,
        srsEaseFactor: Float = 2.5f,
        srsIntervalDays: Float = 1f,
        pronunciationScore: Float = 0.6f
    ): PhraseKnowledge = mockk<PhraseKnowledge>(relaxed = true).also { pk ->
        every { pk.knowledgeLevel } returns knowledgeLevel
        every { pk.timesPracticed } returns timesPracticed
        every { pk.timesCorrect } returns timesCorrect
        every { pk.srsEaseFactor } returns srsEaseFactor
        every { pk.srsIntervalDays } returns srsIntervalDays
        every { pk.pronunciationScore } returns pronunciationScore
        every { pk.copy(
            knowledgeLevel = any(), timesPracticed = any(), timesCorrect = any(),
            lastPracticed = any(), nextReview = any(), srsIntervalDays = any(),
            srsEaseFactor = any(), pronunciationScore = any(), updatedAt = any()
        ) } answers {
            val level = firstArg<Int>()
            val tp    = secondArg<Int>()
            val tc    = thirdArg<Int>()
            val lp    = arg<Long>(3)
            val nr    = arg<Long>(4)
            val si    = arg<Float>(5)
            val ef    = arg<Float>(6)
            val ps    = arg<Float>(7)
            val ua    = arg<Long>(8)
            mockk<PhraseKnowledge>(relaxed = true).also { copy ->
                every { copy.knowledgeLevel }    returns level
                every { copy.timesPracticed }    returns tp
                every { copy.timesCorrect }      returns tc
                every { copy.lastPracticed }     returns lp
                every { copy.nextReview }        returns nr
                every { copy.srsIntervalDays }   returns si
                every { copy.srsEaseFactor }     returns ef
                every { copy.pronunciationScore } returns ps
                every { copy.updatedAt }         returns ua
            }
        }
    }

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        knowledgeRepository = mockk()
        useCase = UpdatePhraseKnowledgeUseCase(knowledgeRepository)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        mockkStatic("com.voicedeutsch.master.util.UUIDKt")
        mockkObject(SrsCalculator)

        every { com.voicedeutsch.master.util.DateUtils.nowTimestamp() } returns fixedNow
        every { com.voicedeutsch.master.util.generateUUID() } returns fixedUUID

        every { SrsCalculator.calculateEaseFactor(any(), any()) }           returns 2.5f
        every { SrsCalculator.calculateInterval(any(), any(), any(), any()) } returns 1f
        every { SrsCalculator.calculateNextReview(any(), any(), any(), any(), any()) } returns fixedNextReview
        every { SrsCalculator.calculateRepetitionNumber(any(), any()) }     returns 1
        every { SrsCalculator.calculateKnowledgeLevel(any(), any(), any()) } returns 3

        coEvery { knowledgeRepository.getPhraseKnowledge(any(), any()) } returns null
        coEvery { knowledgeRepository.getPhrase(any()) }                 returns mockk(relaxed = true)
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) }     returns Unit
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── invoke — phrase not found (early return) ──────────────────────────────

    @Test
    fun invoke_phraseNotFound_returnsEarlyWithoutUpserting() = runTest {
        coEvery { knowledgeRepository.getPhrase("phrase1") } returns null

        useCase(makeParams())

        coVerify(exactly = 0) { knowledgeRepository.upsertPhraseKnowledge(any()) }
    }

    // ── invoke — create new phrase knowledge ──────────────────────────────────

    @Test
    fun invoke_noExistingKnowledge_upsertsOnce() = runTest {
        useCase(makeParams())

        coVerify(exactly = 1) { knowledgeRepository.upsertPhraseKnowledge(any()) }
    }

    @Test
    fun invoke_noExistingKnowledge_newKnowledgeHasGeneratedUUID() = runTest {
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(fixedUUID, captured?.id)
    }

    @Test
    fun invoke_noExistingKnowledge_userIdAndPhraseIdCorrect() = runTest {
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(userId = "userX", phraseId = "phraseY"))

        assertEquals("userX", captured?.userId)
        assertEquals("phraseY", captured?.phraseId)
    }

    @Test
    fun invoke_noExistingKnowledge_timesPracticedIs1() = runTest {
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(1, captured?.timesPracticed)
    }

    @Test
    fun invoke_noExistingKnowledge_qualityAtLeast3_timesCorrectIs1() = runTest {
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(quality = 3))

        assertEquals(1, captured?.timesCorrect)
    }

    @Test
    fun invoke_noExistingKnowledge_qualityBelow3_timesCorrectIs0() = runTest {
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(quality = 2))

        assertEquals(0, captured?.timesCorrect)
    }

    @Test
    fun invoke_noExistingKnowledge_newLevelClampedTo0Min() = runTest {
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(newLevel = -10))

        assertEquals(0, captured?.knowledgeLevel)
    }

    @Test
    fun invoke_noExistingKnowledge_newLevelClampedTo7Max() = runTest {
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(newLevel = 100))

        assertEquals(7, captured?.knowledgeLevel)
    }

    @Test
    fun invoke_noExistingKnowledge_timestampsSetToNow() = runTest {
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(fixedNow, captured?.lastPracticed)
        assertEquals(fixedNow, captured?.createdAt)
        assertEquals(fixedNow, captured?.updatedAt)
    }

    @Test
    fun invoke_noExistingKnowledge_nextReviewFromSrsCalculator() = runTest {
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(fixedNextReview, captured?.nextReview)
    }

    @Test
    fun invoke_noExistingKnowledge_srsIntervalFromSrsCalculator() = runTest {
        every { SrsCalculator.calculateInterval(0, any(), any(), 0f) } returns 4f
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(4f, captured?.srsIntervalDays)
    }

    @Test
    fun invoke_noExistingKnowledge_withPronunciationScore_storedDirectly() = runTest {
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(pronunciationScore = 0.85f))

        assertEquals(0.85f, captured?.pronunciationScore ?: -1f, 0.001f)
    }

    @Test
    fun invoke_noExistingKnowledge_nullPronunciationScore_storedAs0() = runTest {
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(pronunciationScore = null))

        assertEquals(0f, captured?.pronunciationScore ?: -1f, 0.001f)
    }

    @Test
    fun invoke_noExistingKnowledge_srsCalculatorCalledWithDefaultEaseFactor() = runTest {
        useCase(makeParams(quality = 4))

        verify { SrsCalculator.calculateEaseFactor(Constants.SRS_DEFAULT_EASE_FACTOR, 4) }
    }

    @Test
    fun invoke_noExistingKnowledge_qualityClampedTo5Max() = runTest {
        useCase(makeParams(quality = 99))

        verify { SrsCalculator.calculateEaseFactor(any(), 5) }
    }

    @Test
    fun invoke_noExistingKnowledge_qualityClampedTo0Min() = runTest {
        useCase(makeParams(quality = -5))

        verify { SrsCalculator.calculateEaseFactor(any(), 0) }
    }

    @Test
    fun invoke_noExistingKnowledge_srsIntervalCalledWithZeroRepetitionAndZeroInterval() = runTest {
        useCase(makeParams(quality = 4))

        verify { SrsCalculator.calculateInterval(0, 4, any(), 0f) }
    }

    @Test
    fun invoke_noExistingKnowledge_nextReviewCalledWithZeroRepetitionAndZeroInterval() = runTest {
        useCase(makeParams(quality = 4))

        verify { SrsCalculator.calculateNextReview(fixedNow, 0, 4, any(), 0f) }
    }

    // ── invoke — update existing phrase knowledge ─────────────────────────────

    @Test
    fun invoke_existingKnowledge_upsertsOnce() = runTest {
        coEvery { knowledgeRepository.getPhraseKnowledge("user1", "phrase1") } returns
            makeExistingPhraseKnowledge()

        useCase(makeParams())

        coVerify(exactly = 1) { knowledgeRepository.upsertPhraseKnowledge(any()) }
    }

    @Test
    fun invoke_existingKnowledge_timesPracticedIncrementedBy1() = runTest {
        val existing = makeExistingPhraseKnowledge(timesPracticed = 7)
        coEvery { knowledgeRepository.getPhraseKnowledge("user1", "phrase1") } returns existing
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(8, captured?.timesPracticed)
    }

    @Test
    fun invoke_existingKnowledge_qualityAtLeast3_timesCorrectIncremented() = runTest {
        val existing = makeExistingPhraseKnowledge(timesCorrect = 5)
        coEvery { knowledgeRepository.getPhraseKnowledge("user1", "phrase1") } returns existing
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(quality = 3))

        assertEquals(6, captured?.timesCorrect)
    }

    @Test
    fun invoke_existingKnowledge_qualityBelow3_timesCorrectUnchanged() = runTest {
        val existing = makeExistingPhraseKnowledge(timesCorrect = 5)
        coEvery { knowledgeRepository.getPhraseKnowledge("user1", "phrase1") } returns existing
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(quality = 2))

        assertEquals(5, captured?.timesCorrect)
    }

    @Test
    fun invoke_existingKnowledge_knowledgeLevelFromSrsCalculator() = runTest {
        every { SrsCalculator.calculateKnowledgeLevel(2, 4, 3) } returns 4
        val existing = makeExistingPhraseKnowledge(knowledgeLevel = 2)
        coEvery { knowledgeRepository.getPhraseKnowledge("user1", "phrase1") } returns existing
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(quality = 4, newLevel = 3))

        assertEquals(4, captured?.knowledgeLevel)
    }

    @Test
    fun invoke_existingKnowledge_easeFactorFromExistingPassedToSrsCalculator() = runTest {
        val existing = makeExistingPhraseKnowledge(srsEaseFactor = 1.8f)
        coEvery { knowledgeRepository.getPhraseKnowledge("user1", "phrase1") } returns existing

        useCase(makeParams(quality = 4))

        verify { SrsCalculator.calculateEaseFactor(1.8f, 4) }
    }

    @Test
    fun invoke_existingKnowledge_intervalFromExistingPassedToSrsCalculator() = runTest {
        val existing = makeExistingPhraseKnowledge(srsIntervalDays = 10f)
        coEvery { knowledgeRepository.getPhraseKnowledge("user1", "phrase1") } returns existing

        useCase(makeParams(quality = 4))

        verify { SrsCalculator.calculateInterval(any(), 4, any(), 10f) }
    }

    @Test
    fun invoke_existingKnowledge_repetitionNumberFromTimesCorrect() = runTest {
        val existing = makeExistingPhraseKnowledge(timesCorrect = 3)
        coEvery { knowledgeRepository.getPhraseKnowledge("user1", "phrase1") } returns existing

        useCase(makeParams(quality = 4))

        verify { SrsCalculator.calculateRepetitionNumber(3, 4) }
    }

    @Test
    fun invoke_existingKnowledge_lastPracticedAndUpdatedAtSetToNow() = runTest {
        val existing = makeExistingPhraseKnowledge()
        coEvery { knowledgeRepository.getPhraseKnowledge("user1", "phrase1") } returns existing
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(fixedNow, captured?.lastPracticed)
        assertEquals(fixedNow, captured?.updatedAt)
    }

    // ── Pronunciation score averaging (update path) ───────────────────────────

    @Test
    fun invoke_existingKnowledge_withNewPronScore_calculatesRunningAverage() = runTest {
        // prevAttempts=4, prevScore=0.5 → prevTotal=2.0; newScore=1.0; avg=(2.0+1.0)/5=0.6
        val existing = makeExistingPhraseKnowledge(
            timesPracticed = 4,
            pronunciationScore = 0.5f
        )
        coEvery { knowledgeRepository.getPhraseKnowledge("user1", "phrase1") } returns existing
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(pronunciationScore = 1.0f))

        assertEquals(0.6f, captured?.pronunciationScore ?: -1f, 0.001f)
    }

    @Test
    fun invoke_existingKnowledge_withNewPronScore_singlePrevAttempt_averageCorrect() = runTest {
        // prevAttempts=1, prevScore=0.4 → prevTotal=0.4; newScore=0.8; avg=(0.4+0.8)/2=0.6
        val existing = makeExistingPhraseKnowledge(
            timesPracticed = 1,
            pronunciationScore = 0.4f
        )
        coEvery { knowledgeRepository.getPhraseKnowledge("user1", "phrase1") } returns existing
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(pronunciationScore = 0.8f))

        assertEquals(0.6f, captured?.pronunciationScore ?: -1f, 0.001f)
    }

    @Test
    fun invoke_existingKnowledge_nullPronScore_pronunciationScoreUnchanged() = runTest {
        val existing = makeExistingPhraseKnowledge(pronunciationScore = 0.75f)
        coEvery { knowledgeRepository.getPhraseKnowledge("user1", "phrase1") } returns existing
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(pronunciationScore = null))

        assertEquals(0.75f, captured?.pronunciationScore ?: -1f, 0.001f)
    }

    @Test
    fun invoke_existingKnowledge_zeroPronScore_averagedCorrectly() = runTest {
        // prevAttempts=2, prevScore=0.6 → prevTotal=1.2; newScore=0.0; avg=1.2/3=0.4
        val existing = makeExistingPhraseKnowledge(
            timesPracticed = 2,
            pronunciationScore = 0.6f
        )
        coEvery { knowledgeRepository.getPhraseKnowledge("user1", "phrase1") } returns existing
        var captured: PhraseKnowledge? = null
        coEvery { knowledgeRepository.upsertPhraseKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(pronunciationScore = 0.0f))

        assertEquals(0.4f, captured?.pronunciationScore ?: -1f, 0.001f)
    }

    // ── Params data class ─────────────────────────────────────────────────────

    @Test
    fun params_creation_storesAllFields() {
        val params = UpdatePhraseKnowledgeUseCase.Params(
            userId = "u1",
            phraseId = "p1",
            newLevel = 4,
            quality = 5,
            pronunciationScore = 0.9f
        )

        assertEquals("u1", params.userId)
        assertEquals("p1", params.phraseId)
        assertEquals(4, params.newLevel)
        assertEquals(5, params.quality)
        assertEquals(0.9f, params.pronunciationScore)
    }

    @Test
    fun params_defaultPronunciationScore_isNull() {
        val params = UpdatePhraseKnowledgeUseCase.Params(
            userId = "u", phraseId = "p", newLevel = 2, quality = 3
        )

        assertNull(params.pronunciationScore)
    }

    @Test
    fun params_copy_changesOnlySpecifiedField() {
        val original = UpdatePhraseKnowledgeUseCase.Params("u", "p", 2, 3, 0.5f)
        val copy = original.copy(quality = 5)

        assertEquals(5, copy.quality)
        assertEquals("u", copy.userId)
        assertEquals("p", copy.phraseId)
        assertEquals(2, copy.newLevel)
        assertEquals(0.5f, copy.pronunciationScore)
    }

    @Test
    fun params_equals_twoIdenticalInstancesAreEqual() {
        val a = UpdatePhraseKnowledgeUseCase.Params("u", "p", 2, 3, 0.5f)
        val b = UpdatePhraseKnowledgeUseCase.Params("u", "p", 2, 3, 0.5f)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun params_equals_differentPronunciationScoreNotEqual() {
        val a = UpdatePhraseKnowledgeUseCase.Params("u", "p", 2, 3, 0.5f)
        val b = UpdatePhraseKnowledgeUseCase.Params("u", "p", 2, 3, 0.9f)

        assertNotEquals(a, b)
    }

    @Test
    fun params_equals_nullVsNonNullPronunciationScoreNotEqual() {
        val a = UpdatePhraseKnowledgeUseCase.Params("u", "p", 2, 3, null)
        val b = UpdatePhraseKnowledgeUseCase.Params("u", "p", 2, 3, 0.5f)

        assertNotEquals(a, b)
    }
}
