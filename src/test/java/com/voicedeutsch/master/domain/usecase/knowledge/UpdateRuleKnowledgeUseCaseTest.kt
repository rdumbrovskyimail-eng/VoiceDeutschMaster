// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/knowledge/UpdateRuleKnowledgeUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.GrammarRule
import com.voicedeutsch.master.domain.model.knowledge.RuleKnowledge
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.util.Constants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdateRuleKnowledgeUseCaseTest {

    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var useCase: UpdateRuleKnowledgeUseCase

    private val fixedNow = 1_700_000_000_000L
    private val fixedUUID = "test-uuid-1234"

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeParams(
        userId: String = "user1",
        ruleId: String = "rule1",
        newLevel: Int = 3,
        quality: Int = 4,
        mistakeDescription: String? = null
    ) = UpdateRuleKnowledgeUseCase.Params(
        userId = userId,
        ruleId = ruleId,
        newLevel = newLevel,
        quality = quality,
        mistakeDescription = mistakeDescription
    )

    private fun makeExistingRuleKnowledge(
        knowledgeLevel: Int = 2,
        timesPracticed: Int = 3,
        timesCorrect: Int = 2,
        timesIncorrect: Int = 1,
        srsEaseFactor: Float = 2.5f,
        srsIntervalDays: Float = 1f,
        commonMistakes: List<String> = emptyList()
    ): RuleKnowledge = mockk<RuleKnowledge>(relaxed = true).also {
        every { it.knowledgeLevel } returns knowledgeLevel
        every { it.timesPracticed } returns timesPracticed
        every { it.timesCorrect } returns timesCorrect
        every { it.timesIncorrect } returns timesIncorrect
        every { it.srsEaseFactor } returns srsEaseFactor
        every { it.srsIntervalDays } returns srsIntervalDays
        every { it.commonMistakes } returns commonMistakes
        every { it.copy(
            knowledgeLevel = any(), timesPracticed = any(), timesCorrect = any(),
            timesIncorrect = any(), lastPracticed = any(), nextReview = any(),
            srsIntervalDays = any(), srsEaseFactor = any(), commonMistakes = any(),
            updatedAt = any()
        ) } answers {
            val level = firstArg<Int>()
            val tp = secondArg<Int>()
            val tc = thirdArg<Int>()
            val ti = arg<Int>(3)
            val lp = arg<Long>(4)
            val nr = arg<Long>(5)
            val si = arg<Float>(6)
            val ef = arg<Float>(7)
            val cm = arg<List<String>>(8)
            val ua = arg<Long>(9)
            mockk<RuleKnowledge>(relaxed = true).also { copy ->
                every { copy.knowledgeLevel } returns level
                every { copy.timesPracticed } returns tp
                every { copy.timesCorrect } returns tc
                every { copy.timesIncorrect } returns ti
                every { copy.lastPracticed } returns lp
                every { copy.nextReview } returns nr
                every { copy.srsIntervalDays } returns si
                every { copy.srsEaseFactor } returns ef
                every { copy.commonMistakes } returns cm
                every { copy.updatedAt } returns ua
            }
        }
    }

    private fun makeGrammarRule(): GrammarRule = mockk(relaxed = true)

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        knowledgeRepository = mockk()
        useCase = UpdateRuleKnowledgeUseCase(knowledgeRepository)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        mockkStatic("com.voicedeutsch.master.util.UUIDKt")
        mockkObject(SrsCalculator)

        every { com.voicedeutsch.master.util.DateUtils.nowTimestamp() } returns fixedNow
        every { com.voicedeutsch.master.util.generateUUID() } returns fixedUUID

        every { SrsCalculator.calculateEaseFactor(any(), any()) } returns 2.5f
        every { SrsCalculator.calculateInterval(any(), any(), any(), any()) } returns 1f
        every { SrsCalculator.calculateNextReview(any(), any(), any(), any(), any()) } returns fixedNow + 86_400_000L
        every { SrsCalculator.calculateRepetitionNumber(any(), any()) } returns 1
        every { SrsCalculator.calculateKnowledgeLevel(any(), any(), any()) } returns 3

        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } returns Unit
        coEvery { knowledgeRepository.getRuleKnowledge(any(), any()) } returns null
        coEvery { knowledgeRepository.getGrammarRule(any()) } returns makeGrammarRule()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // ── invoke — grammar rule not found ───────────────────────────────────────

    @Test
    fun invoke_grammarRuleNotFound_returnsEarlyWithoutUpserting() = runTest {
        coEvery { knowledgeRepository.getGrammarRule("rule1") } returns null

        useCase(makeParams())

        coVerify(exactly = 0) { knowledgeRepository.upsertRuleKnowledge(any()) }
    }

    @Test
    fun invoke_grammarRuleNotFound_doesNotCheckExistingKnowledge() = runTest {
        coEvery { knowledgeRepository.getGrammarRule("rule1") } returns null

        useCase(makeParams())

        // getRuleKnowledge is called before getGrammarRule in some impls,
        // but the upsert must definitely not happen
        coVerify(exactly = 0) { knowledgeRepository.upsertRuleKnowledge(any()) }
    }

    // ── invoke — create new knowledge (existing == null) ──────────────────────

    @Test
    fun invoke_noExistingKnowledge_upsertsNewRuleKnowledge() = runTest {
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null

        useCase(makeParams())

        coVerify(exactly = 1) { knowledgeRepository.upsertRuleKnowledge(any()) }
    }

    @Test
    fun invoke_noExistingKnowledge_newKnowledgeHasGeneratedUUID() = runTest {
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null
        var capturedKnowledge: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(capture(mutableListOf<RuleKnowledge>().also {
            coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
                capturedKnowledge = firstArg()
                Unit
            }
        })) } returns Unit

        useCase(makeParams())

        assertNotNull(capturedKnowledge)
        assertEquals(fixedUUID, capturedKnowledge?.id)
    }

    @Test
    fun invoke_noExistingKnowledge_newKnowledgeHasCorrectUserAndRuleId() = runTest {
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(userId = "user1", ruleId = "rule1"))

        assertEquals("user1", captured?.userId)
        assertEquals("rule1", captured?.ruleId)
    }

    @Test
    fun invoke_noExistingKnowledge_timesPracticedIs1() = runTest {
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(1, captured?.timesPracticed)
    }

    @Test
    fun invoke_noExistingKnowledge_qualityAtLeast3_timesCorrectIs1() = runTest {
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(quality = 3))

        assertEquals(1, captured?.timesCorrect)
        assertEquals(0, captured?.timesIncorrect)
    }

    @Test
    fun invoke_noExistingKnowledge_qualityBelow3_timesIncorrectIs1() = runTest {
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(quality = 2))

        assertEquals(0, captured?.timesCorrect)
        assertEquals(1, captured?.timesIncorrect)
    }

    @Test
    fun invoke_noExistingKnowledge_newLevelClampedTo0Min() = runTest {
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(newLevel = -5))

        assertEquals(0, captured?.knowledgeLevel)
    }

    @Test
    fun invoke_noExistingKnowledge_newLevelClampedTo7Max() = runTest {
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(newLevel = 99))

        assertEquals(7, captured?.knowledgeLevel)
    }

    @Test
    fun invoke_noExistingKnowledge_timestampsSetToNow() = runTest {
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(fixedNow, captured?.lastPracticed)
        assertEquals(fixedNow, captured?.createdAt)
        assertEquals(fixedNow, captured?.updatedAt)
    }

    @Test
    fun invoke_noExistingKnowledge_withMistakeDescription_commonMistakesContainsIt() = runTest {
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(mistakeDescription = "Forgot article"))

        assertEquals(listOf("Forgot article"), captured?.commonMistakes)
    }

    @Test
    fun invoke_noExistingKnowledge_withNullMistake_commonMistakesIsEmpty() = runTest {
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(mistakeDescription = null))

        assertTrue(captured?.commonMistakes?.isEmpty() == true)
    }

    @Test
    fun invoke_noExistingKnowledge_srsCalculatorCalledWithDefaultEaseFactor() = runTest {
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null

        useCase(makeParams(quality = 4))

        io.mockk.verify {
            SrsCalculator.calculateEaseFactor(Constants.SRS_DEFAULT_EASE_FACTOR, 4)
        }
    }

    @Test
    fun invoke_noExistingKnowledge_qualityClamped0To5() = runTest {
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null

        useCase(makeParams(quality = 10))

        io.mockk.verify {
            SrsCalculator.calculateEaseFactor(any(), 5)
        }
    }

    @Test
    fun invoke_noExistingKnowledge_qualityClampedBelow0() = runTest {
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null

        useCase(makeParams(quality = -3))

        io.mockk.verify {
            SrsCalculator.calculateEaseFactor(any(), 0)
        }
    }

    @Test
    fun invoke_noExistingKnowledge_nextReviewSetFromSrsCalculator() = runTest {
        val expectedNextReview = fixedNow + 999_999L
        every {
            SrsCalculator.calculateNextReview(fixedNow, 0, any(), any(), 0f)
        } returns expectedNextReview
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(expectedNextReview, captured?.nextReview)
    }

    @Test
    fun invoke_noExistingKnowledge_srsIntervalSetFromSrsCalculator() = runTest {
        every { SrsCalculator.calculateInterval(0, any(), any(), 0f) } returns 6f
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns null
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(6f, captured?.srsIntervalDays)
    }

    // ── invoke — update existing knowledge ───────────────────────────────────

    @Test
    fun invoke_existingKnowledge_upsertsUpdatedKnowledge() = runTest {
        val existing = makeExistingRuleKnowledge()
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns existing

        useCase(makeParams())

        coVerify(exactly = 1) { knowledgeRepository.upsertRuleKnowledge(any()) }
    }

    @Test
    fun invoke_existingKnowledge_timesPracticedIncrementedBy1() = runTest {
        val existing = makeExistingRuleKnowledge(timesPracticed = 5)
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns existing
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(6, captured?.timesPracticed)
    }

    @Test
    fun invoke_existingKnowledge_qualityAtLeast3_timesCorrectIncremented() = runTest {
        val existing = makeExistingRuleKnowledge(timesCorrect = 4, timesIncorrect = 1)
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns existing
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(quality = 3))

        assertEquals(5, captured?.timesCorrect)
        assertEquals(1, captured?.timesIncorrect)
    }

    @Test
    fun invoke_existingKnowledge_qualityBelow3_timesIncorrectIncremented() = runTest {
        val existing = makeExistingRuleKnowledge(timesCorrect = 4, timesIncorrect = 1)
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns existing
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(quality = 2))

        assertEquals(4, captured?.timesCorrect)
        assertEquals(2, captured?.timesIncorrect)
    }

    @Test
    fun invoke_existingKnowledge_knowledgeLevelFromSrsCalculator() = runTest {
        every { SrsCalculator.calculateKnowledgeLevel(2, 4, 3) } returns 4
        val existing = makeExistingRuleKnowledge(knowledgeLevel = 2)
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns existing
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(quality = 4, newLevel = 3))

        assertEquals(4, captured?.knowledgeLevel)
    }

    @Test
    fun invoke_existingKnowledge_easeFactorFromExistingPassedToSrsCalculator() = runTest {
        val existing = makeExistingRuleKnowledge(srsEaseFactor = 2.1f)
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns existing

        useCase(makeParams(quality = 4))

        io.mockk.verify { SrsCalculator.calculateEaseFactor(2.1f, 4) }
    }

    @Test
    fun invoke_existingKnowledge_intervalFromExistingPassedToSrsCalculator() = runTest {
        val existing = makeExistingRuleKnowledge(srsIntervalDays = 6f)
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns existing

        useCase(makeParams(quality = 4))

        io.mockk.verify { SrsCalculator.calculateInterval(any(), 4, any(), 6f) }
    }

    @Test
    fun invoke_existingKnowledge_repetitionNumberFromSrsCalculator() = runTest {
        every { SrsCalculator.calculateRepetitionNumber(2, 4) } returns 3
        val existing = makeExistingRuleKnowledge(timesCorrect = 2)
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns existing

        useCase(makeParams(quality = 4))

        io.mockk.verify { SrsCalculator.calculateRepetitionNumber(2, 4) }
    }

    @Test
    fun invoke_existingKnowledge_lastPracticedSetToNow() = runTest {
        val existing = makeExistingRuleKnowledge()
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns existing
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(fixedNow, captured?.lastPracticed)
        assertEquals(fixedNow, captured?.updatedAt)
    }

    @Test
    fun invoke_existingKnowledge_withNewMistake_appendedToCommonMistakes() = runTest {
        val existing = makeExistingRuleKnowledge(commonMistakes = listOf("Old mistake"))
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns existing
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(mistakeDescription = "New mistake"))

        val mistakes = captured?.commonMistakes ?: emptyList()
        assertTrue(mistakes.contains("Old mistake"))
        assertTrue(mistakes.contains("New mistake"))
    }

    @Test
    fun invoke_existingKnowledge_duplicateMistake_deduplicatedInCommonMistakes() = runTest {
        val existing = makeExistingRuleKnowledge(commonMistakes = listOf("Same mistake"))
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns existing
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(mistakeDescription = "Same mistake"))

        assertEquals(1, captured?.commonMistakes?.count { it == "Same mistake" })
    }

    @Test
    fun invoke_existingKnowledge_noMistakeDescription_commonMistakesUnchanged() = runTest {
        val existing = makeExistingRuleKnowledge(commonMistakes = listOf("Existing"))
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns existing
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(mistakeDescription = null))

        assertEquals(listOf("Existing"), captured?.commonMistakes)
    }

    @Test
    fun invoke_existingKnowledge_commonMistakesCappedAt15() = runTest {
        val manyMistakes = List(20) { "Mistake $it" }
        val existing = makeExistingRuleKnowledge(commonMistakes = manyMistakes)
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns existing
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(mistakeDescription = "Extra mistake"))

        assertTrue((captured?.commonMistakes?.size ?: 0) <= 15)
    }

    @Test
    fun invoke_existingKnowledge_commonMistakesTakesLast15AfterAppend() = runTest {
        val existing = makeExistingRuleKnowledge(
            commonMistakes = List(15) { "Old $it" }
        )
        coEvery { knowledgeRepository.getRuleKnowledge("user1", "rule1") } returns existing
        var captured: RuleKnowledge? = null
        coEvery { knowledgeRepository.upsertRuleKnowledge(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(mistakeDescription = "Brand new"))

        val mistakes = captured?.commonMistakes ?: emptyList()
        assertEquals(15, mistakes.size)
        assertTrue(mistakes.contains("Brand new"))
    }

    // ── Params data class ─────────────────────────────────────────────────────

    @Test
    fun params_creation_storesAllFields() {
        val params = UpdateRuleKnowledgeUseCase.Params(
            userId = "u1",
            ruleId = "r1",
            newLevel = 3,
            quality = 4,
            mistakeDescription = "test"
        )

        assertEquals("u1", params.userId)
        assertEquals("r1", params.ruleId)
        assertEquals(3, params.newLevel)
        assertEquals(4, params.quality)
        assertEquals("test", params.mistakeDescription)
    }

    @Test
    fun params_defaultMistakeDescription_isNull() {
        val params = UpdateRuleKnowledgeUseCase.Params(
            userId = "u1",
            ruleId = "r1",
            newLevel = 2,
            quality = 3
        )

        assertNull(params.mistakeDescription)
    }

    @Test
    fun params_copy_changesOnlySpecifiedField() {
        val original = UpdateRuleKnowledgeUseCase.Params("u", "r", 2, 3, "m")
        val copy = original.copy(quality = 5)

        assertEquals(5, copy.quality)
        assertEquals("u", copy.userId)
        assertEquals("r", copy.ruleId)
        assertEquals(2, copy.newLevel)
        assertEquals("m", copy.mistakeDescription)
    }

    @Test
    fun params_equals_twoIdenticalInstancesAreEqual() {
        val a = UpdateRuleKnowledgeUseCase.Params("u", "r", 2, 3, "m")
        val b = UpdateRuleKnowledgeUseCase.Params("u", "r", 2, 3, "m")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun params_equals_differentQualityNotEqual() {
        val a = UpdateRuleKnowledgeUseCase.Params("u", "r", 2, 3)
        val b = UpdateRuleKnowledgeUseCase.Params("u", "r", 2, 5)

        assertNotEquals(a, b)
    }
}
