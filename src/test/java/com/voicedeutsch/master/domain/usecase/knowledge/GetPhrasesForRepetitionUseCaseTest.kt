// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/knowledge/GetPhrasesForRepetitionUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.Phrase
import com.voicedeutsch.master.domain.model.knowledge.PhraseKnowledge
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetPhrasesForRepetitionUseCaseTest {

    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var useCase: GetPhrasesForRepetitionUseCase

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makePhrase(id: String = "p1"): Phrase =
        mockk<Phrase>(relaxed = true).also { every { it.id } returns id }

    private fun makePhraseKnowledge(
        knowledgeLevel: Int = 3,
        nextReview: Long? = 1000L
    ): PhraseKnowledge = mockk<PhraseKnowledge>(relaxed = true).also {
        every { it.knowledgeLevel } returns knowledgeLevel
        every { it.nextReview }     returns nextReview
    }

    private fun makeReviewPair(
        id: String = "p1",
        knowledgeLevel: Int = 3,
        nextReview: Long? = 1000L
    ): Pair<Phrase, PhraseKnowledge> =
        makePhrase(id) to makePhraseKnowledge(knowledgeLevel, nextReview)

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        knowledgeRepository = mockk()
        useCase = GetPhrasesForRepetitionUseCase(knowledgeRepository)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 1L

        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns emptyList()
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
        useCase("user1", limit = 4)

        coVerify(exactly = 1) { knowledgeRepository.getPhrasesForReview("user1", 8) }
    }

    @Test
    fun invoke_defaultLimit_repositoryCalledWith10() = runTest {
        // DEFAULT_PHRASES_LIMIT = 5 → limit * 2 = 10
        useCase("user1")

        coVerify(exactly = 1) { knowledgeRepository.getPhrasesForReview("user1", 10) }
    }

    // ── Priority assignment — CRITICAL ────────────────────────────────────────

    @Test
    fun invoke_level2AndOverdueAbove3_priorityCritical() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 4L
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 2))

        val result = useCase("user1")

        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.CRITICAL, result.single().priority)
    }

    @Test
    fun invoke_level0AndOverdueAbove3_priorityCritical() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 5L
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 0))

        val result = useCase("user1")

        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.CRITICAL, result.single().priority)
    }

    @Test
    fun invoke_level1AndOverdueAbove3_priorityCritical() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 7L
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 1))

        val result = useCase("user1")

        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.CRITICAL, result.single().priority)
    }

    @Test
    fun invoke_levelAtMost2ButOverdueExactly3_notCritical() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 3L
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 2))

        val result = useCase("user1")

        assertNotEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.CRITICAL, result.single().priority)
    }

    @Test
    fun invoke_level3AndOverdueAbove3_notCritical() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 10L
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 3))

        val result = useCase("user1")

        assertNotEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.CRITICAL, result.single().priority)
    }

    // ── Priority assignment — IMPORTANT ───────────────────────────────────────

    @Test
    fun invoke_level3_priorityImportant() = runTest {
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 3))

        val result = useCase("user1")

        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.IMPORTANT, result.single().priority)
    }

    @Test
    fun invoke_level4_priorityImportant() = runTest {
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 4))

        val result = useCase("user1")

        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.IMPORTANT, result.single().priority)
    }

    // ── Priority assignment — SUPPORTING ──────────────────────────────────────

    @Test
    fun invoke_level5_prioritySupporting() = runTest {
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 5))

        val result = useCase("user1")

        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.SUPPORTING, result.single().priority)
    }

    @Test
    fun invoke_level6_prioritySupporting() = runTest {
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 6))

        val result = useCase("user1")

        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.SUPPORTING, result.single().priority)
    }

    // ── Priority assignment — MASTERY ─────────────────────────────────────────

    @Test
    fun invoke_level7_priorityMastery() = runTest {
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 7))

        val result = useCase("user1")

        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.MASTERY, result.single().priority)
    }

    @Test
    fun invoke_level2OverdueExactly3_priorityMastery() = runTest {
        // level <= 2 but NOT > 3 → else → MASTERY
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 3L
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 2))

        val result = useCase("user1")

        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.MASTERY, result.single().priority)
    }

    @Test
    fun invoke_level2OverdueBelow3_priorityMastery() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 1L
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns
            listOf(makeReviewPair(knowledgeLevel = 2))

        val result = useCase("user1")

        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.MASTERY, result.single().priority)
    }

    // ── Overdue days ──────────────────────────────────────────────────────────

    @Test
    fun invoke_nextReviewPresent_overdueDaysFromDateUtils() = runTest {
        val ts = 77_777L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(ts) } returns 6L
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns
            listOf(makePhrase() to makePhraseKnowledge(knowledgeLevel = 7, nextReview = ts))

        val result = useCase("user1")

        assertEquals(6L, result.single().overdueDays)
    }

    @Test
    fun invoke_nextReviewNull_overdueDaysIs0() = runTest {
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns
            listOf(makePhrase() to makePhraseKnowledge(knowledgeLevel = 5, nextReview = null))

        val result = useCase("user1")

        assertEquals(0L, result.single().overdueDays)
    }

    @Test
    fun invoke_nextReviewNull_dateUtilsNotCalled() = runTest {
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns
            listOf(makePhrase() to makePhraseKnowledge(nextReview = null))

        useCase("user1")

        verify(exactly = 0) { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) }
    }

    // ── Sorting ───────────────────────────────────────────────────────────────

    @Test
    fun invoke_sortedByPriorityOrdinalAscending() = runTest {
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(any()) } returns 1L
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns listOf(
            makeReviewPair(id = "mastery",    knowledgeLevel = 7),
            makeReviewPair(id = "supporting", knowledgeLevel = 5),
            makeReviewPair(id = "important",  knowledgeLevel = 3)
        )

        val result = useCase("user1", limit = 10)

        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.IMPORTANT,  result[0].priority)
        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.SUPPORTING, result[1].priority)
        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.MASTERY,    result[2].priority)
    }

    @Test
    fun invoke_samePriority_sortedByOverdueDaysDescending() = runTest {
        val ts1 = 111L; val ts2 = 222L; val ts3 = 333L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(ts1) } returns 2L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(ts2) } returns 8L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(ts3) } returns 5L

        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns listOf(
            makePhrase("a") to makePhraseKnowledge(4, ts1),
            makePhrase("b") to makePhraseKnowledge(4, ts2),
            makePhrase("c") to makePhraseKnowledge(4, ts3)
        )

        val result = useCase("user1", limit = 10)

        assertEquals(8L, result[0].overdueDays)
        assertEquals(5L, result[1].overdueDays)
        assertEquals(2L, result[2].overdueDays)
    }

    @Test
    fun invoke_criticalBeforeImportantBeforeSupporting() = runTest {
        val tsOverdue4 = 10L
        val tsOverdue1 = 20L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(tsOverdue4) } returns 4L
        every { com.voicedeutsch.master.util.DateUtils.overdueDays(tsOverdue1) } returns 1L

        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns listOf(
            makePhrase("sup")  to makePhraseKnowledge(5, tsOverdue1),
            makePhrase("imp")  to makePhraseKnowledge(3, tsOverdue1),
            makePhrase("crit") to makePhraseKnowledge(1, tsOverdue4)
        )

        val result = useCase("user1", limit = 10)

        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.CRITICAL,   result[0].priority)
        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.IMPORTANT,  result[1].priority)
        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.SUPPORTING, result[2].priority)
    }

    // ── Limit enforcement ─────────────────────────────────────────────────────

    @Test
    fun invoke_moreThanLimitItems_returnsExactlyLimit() = runTest {
        val pairs = List(10) { i -> makeReviewPair(id = "p$i", knowledgeLevel = 3) }
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns pairs

        val result = useCase("user1", limit = 3)

        assertEquals(3, result.size)
    }

    @Test
    fun invoke_fewerThanLimitItems_returnsAll() = runTest {
        val pairs = List(2) { i -> makeReviewPair(id = "p$i", knowledgeLevel = 4) }
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns pairs

        val result = useCase("user1", limit = 5)

        assertEquals(2, result.size)
    }

    @Test
    fun invoke_exactlyLimitItems_returnsAll() = runTest {
        val pairs = List(5) { i -> makeReviewPair(id = "p$i", knowledgeLevel = 4) }
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns pairs

        val result = useCase("user1", limit = 5)

        assertEquals(5, result.size)
    }

    // ── PhraseReviewItem contents ─────────────────────────────────────────────

    @Test
    fun invoke_reviewItemContainsCorrectPhraseAndKnowledge() = runTest {
        val phrase = makePhrase("pX")
        val pk     = makePhraseKnowledge(knowledgeLevel = 4)
        coEvery { knowledgeRepository.getPhrasesForReview(any(), any()) } returns listOf(phrase to pk)

        val result = useCase("user1")

        assertEquals(phrase, result.single().phrase)
        assertEquals(pk,     result.single().knowledge)
    }

    // ── ReviewPriority enum ───────────────────────────────────────────────────

    @Test
    fun reviewPriority_hasExactly4Values() {
        assertEquals(4, GetPhrasesForRepetitionUseCase.ReviewPriority.entries.size)
    }

    @Test
    fun reviewPriority_ordinalOrderCriticalFirst() {
        val entries = GetPhrasesForRepetitionUseCase.ReviewPriority.entries
        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.CRITICAL,   entries[0])
        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.IMPORTANT,  entries[1])
        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.SUPPORTING, entries[2])
        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.MASTERY,    entries[3])
    }

    // ── PhraseReviewItem data class ───────────────────────────────────────────

    @Test
    fun phraseReviewItem_creation_storesAllFields() {
        val phrase = makePhrase("p1")
        val pk     = makePhraseKnowledge()
        val item   = GetPhrasesForRepetitionUseCase.PhraseReviewItem(
            phrase      = phrase,
            knowledge   = pk,
            priority    = GetPhrasesForRepetitionUseCase.ReviewPriority.SUPPORTING,
            overdueDays = 4L
        )

        assertEquals(phrase, item.phrase)
        assertEquals(pk,     item.knowledge)
        assertEquals(GetPhrasesForRepetitionUseCase.ReviewPriority.SUPPORTING, item.priority)
        assertEquals(4L,     item.overdueDays)
    }

    @Test
    fun phraseReviewItem_copy_changesOnlySpecifiedField() {
        val phrase   = makePhrase()
        val pk       = makePhraseKnowledge()
        val original = GetPhrasesForRepetitionUseCase.PhraseReviewItem(
            phrase      = phrase,
            knowledge   = pk,
            priority    = GetPhrasesForRepetitionUseCase.ReviewPriority.MASTERY,
            overdueDays = 1L
        )
        val copy = original.copy(overdueDays = 99L)

        assertEquals(99L,             copy.overdueDays)
        assertEquals(original.phrase,    copy.phrase)
        assertEquals(original.knowledge, copy.knowledge)
        assertEquals(original.priority,  copy.priority)
    }

    @Test
    fun phraseReviewItem_equals_twoIdenticalInstancesAreEqual() {
        val phrase = makePhrase()
        val pk     = makePhraseKnowledge()
        val a = GetPhrasesForRepetitionUseCase.PhraseReviewItem(
            phrase, pk, GetPhrasesForRepetitionUseCase.ReviewPriority.CRITICAL, 3L
        )
        val b = GetPhrasesForRepetitionUseCase.PhraseReviewItem(
            phrase, pk, GetPhrasesForRepetitionUseCase.ReviewPriority.CRITICAL, 3L
        )

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun phraseReviewItem_equals_differentOverdueDaysNotEqual() {
        val phrase = makePhrase()
        val pk     = makePhraseKnowledge()
        val a = GetPhrasesForRepetitionUseCase.PhraseReviewItem(
            phrase, pk, GetPhrasesForRepetitionUseCase.ReviewPriority.MASTERY, 1L
        )
        val b = GetPhrasesForRepetitionUseCase.PhraseReviewItem(
            phrase, pk, GetPhrasesForRepetitionUseCase.ReviewPriority.MASTERY, 9L
        )

        assertNotEquals(a, b)
    }

    @Test
    fun phraseReviewItem_equals_differentPriorityNotEqual() {
        val phrase = makePhrase()
        val pk     = makePhraseKnowledge()
        val a = GetPhrasesForRepetitionUseCase.PhraseReviewItem(
            phrase, pk, GetPhrasesForRepetitionUseCase.ReviewPriority.IMPORTANT, 2L
        )
        val b = GetPhrasesForRepetitionUseCase.PhraseReviewItem(
            phrase, pk, GetPhrasesForRepetitionUseCase.ReviewPriority.MASTERY,   2L
        )

        assertNotEquals(a, b)
    }
}
