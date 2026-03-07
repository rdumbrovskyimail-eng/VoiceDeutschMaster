// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/knowledge/GetRulesForRepetitionUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.GrammarRule
import com.voicedeutsch.master.domain.model.knowledge.RuleKnowledge
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetRulesForRepetitionUseCaseTest {

    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var useCase: GetRulesForRepetitionUseCase

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeGrammarRule(id: String = "r1"): GrammarRule =
        mockk<GrammarRule>(relaxed = true).also { every { it.id } returns id }

    private fun makeRuleKnowledge(
        knowledgeLevel: Int = 3,
        nextReview: Long? = 1000L
    ): RuleKnowledge = mockk<RuleKnowledge>(relaxed = true).also {
        every { it.knowledgeLevel } returns knowledgeLevel
        every { it.nextReview }     returns nextReview
    }

    private fun makeReviewPair(
        id: String = "r1",
        knowledgeLevel: Int = 3,
        nextReview: Long? = 1000L
    ): Pair<GrammarRule, RuleKnowledge> =
        makeGrammarRule(id) to makeRuleKnowledge(knowledgeLevel, nextReview)

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        knowledgeRepository = mockk()
        useCase = GetRulesForRepetitionUseCase(knowledgeRepository)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 1L

        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns emptyList()
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── invoke — empty source ─────────────────────────────────────────────────

    @Test
    fun invoke_emptyRepository_returnsEmptyList() = runTest {
        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_repositoryCalledWithLimitTimesTwo() = runTest {
        useCase("user1", limit = 5)

        coVerify(exactly = 1) { knowledgeRepository.getRulesForReview("user1", 10) }
    }

    @Test
    fun invoke_defaultLimit_repositoryCalledWith20() = runTest {
        // DEFAULT_RULES_LIMIT = 10 → limit * 2 = 20
        useCase("user1")

        coVerify(exactly = 1) { knowledgeRepository.getRulesForReview("user1", 20) }
    }

    // ── Priority assignment — CRITICAL ────────────────────────────────────────

    @Test
    fun invoke_levelAtMost2AndOverdueAbove3_priorityCritical() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 4L
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 2))

        val result = useCase("user1")

        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.CRITICAL, result.single().priority)
    }

    @Test
    fun invoke_level0AndOverdueAbove3_priorityCritical() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 10L
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 0))

        val result = useCase("user1")

        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.CRITICAL, result.single().priority)
    }

    @Test
    fun invoke_level1AndOverdueAbove3_priorityCritical() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 5L
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 1))

        val result = useCase("user1")

        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.CRITICAL, result.single().priority)
    }

    @Test
    fun invoke_levelAtMost2ButOverdueExactly3_notCritical() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 3L
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 2))

        val result = useCase("user1")

        assertNotEquals(GetRulesForRepetitionUseCase.ReviewPriority.CRITICAL, result.single().priority)
    }

    @Test
    fun invoke_level3AndOverdueAbove3_notCritical() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 10L
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 3))

        val result = useCase("user1")

        assertNotEquals(GetRulesForRepetitionUseCase.ReviewPriority.CRITICAL, result.single().priority)
    }

    // ── Priority assignment — IMPORTANT ───────────────────────────────────────

    @Test
    fun invoke_level3_priorityImportant() = runTest {
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 3))

        val result = useCase("user1")

        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.IMPORTANT, result.single().priority)
    }

    @Test
    fun invoke_level4_priorityImportant() = runTest {
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 4))

        val result = useCase("user1")

        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.IMPORTANT, result.single().priority)
    }

    // ── Priority assignment — SUPPORTING ──────────────────────────────────────

    @Test
    fun invoke_level5_prioritySupporting() = runTest {
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 5))

        val result = useCase("user1")

        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.SUPPORTING, result.single().priority)
    }

    @Test
    fun invoke_level6_prioritySupporting() = runTest {
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 6))

        val result = useCase("user1")

        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.SUPPORTING, result.single().priority)
    }

    // ── Priority assignment — MASTERY ─────────────────────────────────────────

    @Test
    fun invoke_level7_priorityMastery() = runTest {
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 7))

        val result = useCase("user1")

        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.MASTERY, result.single().priority)
    }

    @Test
    fun invoke_level2OverdueExactly3_priorityMastery() = runTest {
        // level <= 2 but overdue NOT > 3 → else → MASTERY
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 3L
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 2))

        val result = useCase("user1")

        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.MASTERY, result.single().priority)
    }

    @Test
    fun invoke_level2OverdueBelow3_priorityMastery() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 1L
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 2))

        val result = useCase("user1")

        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.MASTERY, result.single().priority)
    }

    // ── Overdue days ──────────────────────────────────────────────────────────

    @Test
    fun invoke_nextReviewPresent_overdueDaysFromDateUtils() = runTest {
        val ts = 99_999L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(ts) } returns 8L
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns
            listOf(makeGrammarRule() to makeRuleKnowledge(knowledgeLevel = 7, nextReview = ts))

        val result = useCase("user1")

        assertEquals(8L, result.single().overdueDays)
    }

    @Test
    fun invoke_nextReviewNull_overdueDaysIs0() = runTest {
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns
            listOf(makeGrammarRule() to makeRuleKnowledge(knowledgeLevel = 5, nextReview = null))

        val result = useCase("user1")

        assertEquals(0L, result.single().overdueDays)
    }

    @Test
    fun invoke_nextReviewNull_dateUtilsNotCalled() = runTest {
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns
            listOf(makeGrammarRule() to makeRuleKnowledge(nextReview = null))

        useCase("user1")

        io.mockk.verify(exactly = 0) { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) }
    }

    // ── Sorting ───────────────────────────────────────────────────────────────

    @Test
    fun invoke_sortedByPriorityOrdinalAscending() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 1L
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns listOf(
            makeReviewPair(id = "mastery",    knowledgeLevel = 7),
            makeReviewPair(id = "supporting", knowledgeLevel = 5),
            makeReviewPair(id = "important",  knowledgeLevel = 3)
        )

        val result = useCase("user1", limit = 10)

        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.IMPORTANT,  result[0].priority)
        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.SUPPORTING, result[1].priority)
        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.MASTERY,    result[2].priority)
    }

    @Test
    fun invoke_samePriority_sortedByOverdueDaysDescending() = runTest {
        val ts1 = 111L; val ts2 = 222L; val ts3 = 333L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(ts1) } returns 1L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(ts2) } returns 9L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(ts3) } returns 4L

        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns listOf(
            makeGrammarRule("a") to makeRuleKnowledge(3, ts1),
            makeGrammarRule("b") to makeRuleKnowledge(3, ts2),
            makeGrammarRule("c") to makeRuleKnowledge(3, ts3)
        )

        val result = useCase("user1", limit = 10)

        assertEquals(9L, result[0].overdueDays)
        assertEquals(4L, result[1].overdueDays)
        assertEquals(1L, result[2].overdueDays)
    }

    @Test
    fun invoke_criticalBeforeImportantBeforeSupporting() = runTest {
        val tsOverdue4 = 10L
        val tsOverdue1 = 20L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(tsOverdue4) } returns 4L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(tsOverdue1) } returns 1L

        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns listOf(
            makeGrammarRule("sup")  to makeRuleKnowledge(5, tsOverdue1),
            makeGrammarRule("imp")  to makeRuleKnowledge(3, tsOverdue1),
            makeGrammarRule("crit") to makeRuleKnowledge(1, tsOverdue4)
        )

        val result = useCase("user1", limit = 10)

        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.CRITICAL,   result[0].priority)
        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.IMPORTANT,  result[1].priority)
        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.SUPPORTING, result[2].priority)
    }

    // ── Limit enforcement ─────────────────────────────────────────────────────

    @Test
    fun invoke_moreThanLimitItems_returnsExactlyLimit() = runTest {
        val pairs = List(20) { i -> makeReviewPair(id = "r$i", knowledgeLevel = 3) }
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns pairs

        val result = useCase("user1", limit = 4)

        assertEquals(4, result.size)
    }

    @Test
    fun invoke_fewerThanLimitItems_returnsAll() = runTest {
        val pairs = List(3) { i -> makeReviewPair(id = "r$i", knowledgeLevel = 4) }
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns pairs

        val result = useCase("user1", limit = 10)

        assertEquals(3, result.size)
    }

    @Test
    fun invoke_exactlyLimitItems_returnsAll() = runTest {
        val pairs = List(10) { i -> makeReviewPair(id = "r$i", knowledgeLevel = 4) }
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns pairs

        val result = useCase("user1", limit = 10)

        assertEquals(10, result.size)
    }

    // ── RuleReviewItem contents ───────────────────────────────────────────────

    @Test
    fun invoke_reviewItemContainsCorrectRuleAndKnowledge() = runTest {
        val rule = makeGrammarRule("rX")
        val rk   = makeRuleKnowledge(knowledgeLevel = 4)
        coEvery { knowledgeRepository.getRulesForReview(any(), any()) } returns listOf(rule to rk)

        val result = useCase("user1")

        assertEquals(rule, result.single().rule)
        assertEquals(rk,   result.single().knowledge)
    }

    // ── ReviewPriority enum ───────────────────────────────────────────────────

    @Test
    fun reviewPriority_hasExactly4Values() {
        assertEquals(4, GetRulesForRepetitionUseCase.ReviewPriority.entries.size)
    }

    @Test
    fun reviewPriority_ordinalOrderCriticalFirst() {
        val entries = GetRulesForRepetitionUseCase.ReviewPriority.entries
        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.CRITICAL,   entries[0])
        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.IMPORTANT,  entries[1])
        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.SUPPORTING, entries[2])
        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.MASTERY,    entries[3])
    }

    // ── RuleReviewItem data class ─────────────────────────────────────────────

    @Test
    fun ruleReviewItem_creation_storesAllFields() {
        val rule = makeGrammarRule("r1")
        val rk   = makeRuleKnowledge()
        val item = GetRulesForRepetitionUseCase.RuleReviewItem(
            rule        = rule,
            knowledge   = rk,
            priority    = GetRulesForRepetitionUseCase.ReviewPriority.IMPORTANT,
            overdueDays = 5L
        )

        assertEquals(rule, item.rule)
        assertEquals(rk,   item.knowledge)
        assertEquals(GetRulesForRepetitionUseCase.ReviewPriority.IMPORTANT, item.priority)
        assertEquals(5L,   item.overdueDays)
    }

    @Test
    fun ruleReviewItem_copy_changesOnlySpecifiedField() {
        val rule = makeGrammarRule()
        val rk   = makeRuleKnowledge()
        val original = GetRulesForRepetitionUseCase.RuleReviewItem(
            rule        = rule,
            knowledge   = rk,
            priority    = GetRulesForRepetitionUseCase.ReviewPriority.MASTERY,
            overdueDays = 2L
        )
        val copy = original.copy(overdueDays = 99L)

        assertEquals(99L,  copy.overdueDays)
        assertEquals(original.rule,      copy.rule)
        assertEquals(original.knowledge, copy.knowledge)
        assertEquals(original.priority,  copy.priority)
    }

    @Test
    fun ruleReviewItem_equals_twoIdenticalInstancesAreEqual() {
        val rule = makeGrammarRule()
        val rk   = makeRuleKnowledge()
        val a = GetRulesForRepetitionUseCase.RuleReviewItem(
            rule, rk, GetRulesForRepetitionUseCase.ReviewPriority.CRITICAL, 7L
        )
        val b = GetRulesForRepetitionUseCase.RuleReviewItem(
            rule, rk, GetRulesForRepetitionUseCase.ReviewPriority.CRITICAL, 7L
        )

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun ruleReviewItem_equals_differentOverdueDaysNotEqual() {
        val rule = makeGrammarRule()
        val rk   = makeRuleKnowledge()
        val a = GetRulesForRepetitionUseCase.RuleReviewItem(
            rule, rk, GetRulesForRepetitionUseCase.ReviewPriority.MASTERY, 1L
        )
        val b = GetRulesForRepetitionUseCase.RuleReviewItem(
            rule, rk, GetRulesForRepetitionUseCase.ReviewPriority.MASTERY, 2L
        )

        assertNotEquals(a, b)
    }

    @Test
    fun ruleReviewItem_equals_differentPriorityNotEqual() {
        val rule = makeGrammarRule()
        val rk   = makeRuleKnowledge()
        val a = GetRulesForRepetitionUseCase.RuleReviewItem(
            rule, rk, GetRulesForRepetitionUseCase.ReviewPriority.CRITICAL, 5L
        )
        val b = GetRulesForRepetitionUseCase.RuleReviewItem(
            rule, rk, GetRulesForRepetitionUseCase.ReviewPriority.MASTERY, 5L
        )

        assertNotEquals(a, b)
    }
}
