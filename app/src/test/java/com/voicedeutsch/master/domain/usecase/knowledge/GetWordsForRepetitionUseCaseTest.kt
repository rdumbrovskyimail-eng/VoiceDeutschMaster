// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/knowledge/GetWordsForRepetitionUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.Word
import com.voicedeutsch.master.domain.model.knowledge.WordKnowledge
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

class GetWordsForRepetitionUseCaseTest {

    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var useCase: GetWordsForRepetitionUseCase

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeWord(id: String = "w1"): Word = mockk<Word>(relaxed = true).also {
        every { it.id } returns id
    }

    private fun makeWordKnowledge(
        knowledgeLevel: Int = 3,
        nextReview: Long? = System.currentTimeMillis() - 86_400_000L
    ): WordKnowledge = mockk<WordKnowledge>(relaxed = true).also {
        every { it.knowledgeLevel } returns knowledgeLevel
        every { it.nextReview } returns nextReview
    }

    private fun makeReviewPair(
        id: String = "w1",
        knowledgeLevel: Int = 3,
        nextReview: Long? = System.currentTimeMillis() - 86_400_000L
    ): Pair<Word, WordKnowledge> = makeWord(id) to makeWordKnowledge(knowledgeLevel, nextReview)

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        knowledgeRepository = mockk()
        useCase = GetWordsForRepetitionUseCase(knowledgeRepository)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 1L

        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns emptyList()
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
        useCase("user1", limit = 10)

        coVerify(exactly = 1) { knowledgeRepository.getWordsForReview("user1", 20) }
    }

    @Test
    fun invoke_defaultLimit_usesConstantSrsMaxReviews() = runTest {
        useCase("user1")

        coVerify(exactly = 1) {
            knowledgeRepository.getWordsForReview("user1", Constants.SRS_MAX_REVIEWS_PER_SESSION * 2)
        }
    }

    // ── Priority assignment — CRITICAL ────────────────────────────────────────

    @Test
    fun invoke_levelAtMost2AndOverdueAbove3_priorityCritical() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 4L
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 2))

        val result = useCase("user1")

        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.CRITICAL, result.single().priority)
    }

    @Test
    fun invoke_levelIs0AndOverdueAbove3_priorityCritical() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 5L
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 0))

        val result = useCase("user1")

        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.CRITICAL, result.single().priority)
    }

    @Test
    fun invoke_levelAtMost2ButOverdueExactly3_notCritical() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 3L
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 2))

        val result = useCase("user1")

        assertNotEquals(GetWordsForRepetitionUseCase.ReviewPriority.CRITICAL, result.single().priority)
    }

    @Test
    fun invoke_level3AndOverdueAbove3_notCritical() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 10L
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 3))

        val result = useCase("user1")

        assertNotEquals(GetWordsForRepetitionUseCase.ReviewPriority.CRITICAL, result.single().priority)
    }

    // ── Priority assignment — IMPORTANT ───────────────────────────────────────

    @Test
    fun invoke_level3_priorityImportant() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 1L
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 3))

        val result = useCase("user1")

        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.IMPORTANT, result.single().priority)
    }

    @Test
    fun invoke_level4_priorityImportant() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 1L
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 4))

        val result = useCase("user1")

        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.IMPORTANT, result.single().priority)
    }

    // ── Priority assignment — SUPPORTING ──────────────────────────────────────

    @Test
    fun invoke_level5_prioritySupporting() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 1L
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 5))

        val result = useCase("user1")

        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.SUPPORTING, result.single().priority)
    }

    @Test
    fun invoke_level6_prioritySupporting() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 1L
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 6))

        val result = useCase("user1")

        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.SUPPORTING, result.single().priority)
    }

    // ── Priority assignment — MASTERY ─────────────────────────────────────────

    @Test
    fun invoke_level7_priorityMastery() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 1L
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 7))

        val result = useCase("user1")

        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.MASTERY, result.single().priority)
    }

    @Test
    fun invoke_level2OverdueExactly3_priorityMastery() = runTest {
        // level <= 2 but overdue NOT > 3 → falls to else → MASTERY
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 3L
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 2))

        val result = useCase("user1")

        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.MASTERY, result.single().priority)
    }

    // ── Overdue days calculation ───────────────────────────────────────────────

    @Test
    fun invoke_nextReviewPresent_overdueDaysFromDateUtils() = runTest {
        val nextReviewTs = 12345L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(nextReviewTs) } returns 7L
        val wk = makeWordKnowledge(knowledgeLevel = 7, nextReview = nextReviewTs)
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns
            listOf(makeWord() to wk)

        val result = useCase("user1")

        assertEquals(7L, result.single().overdueDays)
    }

    @Test
    fun invoke_nextReviewNull_overdueDaysIs0() = runTest {
        val wk = makeWordKnowledge(knowledgeLevel = 5, nextReview = null)
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns
            listOf(makeWord() to wk)

        val result = useCase("user1")

        assertEquals(0L, result.single().overdueDays)
    }

    // ── Sorting ───────────────────────────────────────────────────────────────

    @Test
    fun invoke_sortedByPriorityOrdinalAscending() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 1L
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns listOf(
            makeReviewPair(id = "mastery",    knowledgeLevel = 7),
            makeReviewPair(id = "supporting", knowledgeLevel = 5),
            makeReviewPair(id = "important",  knowledgeLevel = 3)
        )

        val result = useCase("user1", limit = 10)

        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.IMPORTANT,  result[0].priority)
        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.SUPPORTING, result[1].priority)
        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.MASTERY,    result[2].priority)
    }

    @Test
    fun invoke_samePriority_sortedByOverdueDaysDescending() = runTest {
        val ts1 = 1000L
        val ts2 = 2000L
        val ts3 = 3000L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(ts1) } returns 1L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(ts2) } returns 5L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(ts3) } returns 3L

        val wk1 = makeWordKnowledge(knowledgeLevel = 3, nextReview = ts1)
        val wk2 = makeWordKnowledge(knowledgeLevel = 3, nextReview = ts2)
        val wk3 = makeWordKnowledge(knowledgeLevel = 3, nextReview = ts3)
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns listOf(
            makeWord("a") to wk1,
            makeWord("b") to wk2,
            makeWord("c") to wk3
        )

        val result = useCase("user1", limit = 10)

        assertEquals(5L, result[0].overdueDays)
        assertEquals(3L, result[1].overdueDays)
        assertEquals(1L, result[2].overdueDays)
    }

    @Test
    fun invoke_criticalBeforeImportantBeforeSupporting() = runTest {
        val tsOverdue4 = 100L
        val tsOverdue1 = 200L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(tsOverdue4) } returns 4L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(tsOverdue1) } returns 1L

        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns listOf(
            makeWord("sup")  to makeWordKnowledge(5, tsOverdue1),
            makeWord("imp")  to makeWordKnowledge(3, tsOverdue1),
            makeWord("crit") to makeWordKnowledge(1, tsOverdue4)
        )

        val result = useCase("user1", limit = 10)

        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.CRITICAL,   result[0].priority)
        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.IMPORTANT,  result[1].priority)
        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.SUPPORTING, result[2].priority)
    }

    // ── Limit enforcement ─────────────────────────────────────────────────────

    @Test
    fun invoke_moreThanLimitItems_returnsExactlyLimit() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 1L
        val pairs = List(20) { i -> makeReviewPair(id = "w$i", knowledgeLevel = 3) }
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns pairs

        val result = useCase("user1", limit = 7)

        assertEquals(7, result.size)
    }

    @Test
    fun invoke_fewerThanLimitItems_returnsAll() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 1L
        val pairs = List(3) { i -> makeReviewPair(id = "w$i", knowledgeLevel = 4) }
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns pairs

        val result = useCase("user1", limit = 15)

        assertEquals(3, result.size)
    }

    // ── ReviewItem contents ───────────────────────────────────────────────────

    @Test
    fun invoke_reviewItemContainsCorrectWordAndKnowledge() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 2L
        val word = makeWord("wX")
        val wk   = makeWordKnowledge(knowledgeLevel = 4)
        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns listOf(word to wk)

        val result = useCase("user1")

        assertEquals(word, result.single().word)
        assertEquals(wk, result.single().knowledge)
    }

    // ── ReviewPriority enum ───────────────────────────────────────────────────

    @Test
    fun reviewPriority_hasExactly4Values() {
        assertEquals(4, GetWordsForRepetitionUseCase.ReviewPriority.entries.size)
    }

    @Test
    fun reviewPriority_ordinalOrderCriticalFirst() {
        val entries = GetWordsForRepetitionUseCase.ReviewPriority.entries
        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.CRITICAL,   entries[0])
        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.IMPORTANT,  entries[1])
        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.SUPPORTING, entries[2])
        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.MASTERY,    entries[3])
    }

    // ── ReviewItem data class ─────────────────────────────────────────────────

    @Test
    fun reviewItem_creation_storesAllFields() {
        val word = makeWord("w1")
        val wk   = makeWordKnowledge()
        val item = GetWordsForRepetitionUseCase.ReviewItem(
            word        = word,
            knowledge   = wk,
            priority    = GetWordsForRepetitionUseCase.ReviewPriority.IMPORTANT,
            overdueDays = 3L
        )

        assertEquals(word, item.word)
        assertEquals(wk, item.knowledge)
        assertEquals(GetWordsForRepetitionUseCase.ReviewPriority.IMPORTANT, item.priority)
        assertEquals(3L, item.overdueDays)
    }

    @Test
    fun reviewItem_copy_changesOnlySpecifiedField() {
        val word = makeWord()
        val wk   = makeWordKnowledge()
        val original = GetWordsForRepetitionUseCase.ReviewItem(
            word        = word,
            knowledge   = wk,
            priority    = GetWordsForRepetitionUseCase.ReviewPriority.MASTERY,
            overdueDays = 2L
        )
        val copy = original.copy(overdueDays = 10L)

        assertEquals(10L, copy.overdueDays)
        assertEquals(original.word, copy.word)
        assertEquals(original.knowledge, copy.knowledge)
        assertEquals(original.priority, copy.priority)
    }

    @Test
    fun reviewItem_equals_twoIdenticalInstancesAreEqual() {
        val word = makeWord()
        val wk   = makeWordKnowledge()
        val a = GetWordsForRepetitionUseCase.ReviewItem(word, wk, GetWordsForRepetitionUseCase.ReviewPriority.CRITICAL, 5L)
        val b = GetWordsForRepetitionUseCase.ReviewItem(word, wk, GetWordsForRepetitionUseCase.ReviewPriority.CRITICAL, 5L)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun reviewItem_equals_differentPriorityNotEqual() {
        val word = makeWord()
        val wk   = makeWordKnowledge()
        val a = GetWordsForRepetitionUseCase.ReviewItem(word, wk, GetWordsForRepetitionUseCase.ReviewPriority.CRITICAL,  5L)
        val b = GetWordsForRepetitionUseCase.ReviewItem(word, wk, GetWordsForRepetitionUseCase.ReviewPriority.IMPORTANT, 5L)

        assertNotEquals(a, b)
    }
}
