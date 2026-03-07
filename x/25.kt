// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/book/AdvanceBookProgressUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.book

import com.voicedeutsch.master.domain.model.book.BookProgress
import com.voicedeutsch.master.domain.model.book.LessonStatus
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.util.Constants
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

class AdvanceBookProgressUseCaseTest {

    private lateinit var bookRepository: BookRepository
    private lateinit var useCase: AdvanceBookProgressUseCase

    private val fixedNow  = 1_700_000_000_000L
    private val fixedUUID = "progress-uuid-1"

    private val threshold = Constants.BOOK_LESSON_COMPLETION_THRESHOLD  // 0.8f

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeBookProgress(
        startedAt: Long? = fixedNow - 1000L,
        timesPracticed: Int = 2,
        score: Float = 0.5f,
        status: LessonStatus = LessonStatus.IN_PROGRESS
    ): BookProgress = mockk<BookProgress>(relaxed = true).also { bp ->
        every { bp.startedAt }       returns startedAt
        every { bp.timesPracticed }  returns timesPracticed
        every { bp.score }           returns score
        every { bp.status }          returns status
        every { bp.copy(
            status = any(), score = any(), startedAt = any(), timesPracticed = any()
        ) } answers {
            val newStatus = firstArg<LessonStatus>()
            val newScore  = secondArg<Float>()
            val newStart  = thirdArg<Long?>()
            val newTimes  = arg<Int>(3)
            mockk<BookProgress>(relaxed = true).also { copy ->
                every { copy.status }          returns newStatus
                every { copy.score }           returns newScore
                every { copy.startedAt }       returns newStart
                every { copy.timesPracticed }  returns newTimes
            }
        }
    }

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        bookRepository = mockk()
        useCase = AdvanceBookProgressUseCase(bookRepository)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        mockkStatic("com.voicedeutsch.master.util.UUIDKt")

        every { com.voicedeutsch.master.util.DateUtils.nowTimestamp() } returns fixedNow
        every { com.voicedeutsch.master.util.generateUUID() }           returns fixedUUID

        coEvery { bookRepository.getCurrentBookPosition(any()) } returns Pair(2, 3)
        coEvery { bookRepository.markLessonComplete(any(), any(), any(), any()) } returns Unit
        coEvery { bookRepository.advanceToNextLesson(any()) } returns Pair(2, 4)
        coEvery { bookRepository.getBookProgress(any(), any(), any()) } returns null
        coEvery { bookRepository.upsertBookProgress(any()) } returns Unit
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── Score >= threshold → advance ──────────────────────────────────────────

    @Test
    fun invoke_scoreAtThreshold_marksLessonComplete() = runTest {
        useCase("user1", threshold)

        coVerify(exactly = 1) { bookRepository.markLessonComplete("user1", 2, 3, threshold) }
    }

    @Test
    fun invoke_scoreAboveThreshold_marksLessonComplete() = runTest {
        useCase("user1", 0.95f)

        coVerify(exactly = 1) { bookRepository.markLessonComplete("user1", 2, 3, 0.95f) }
    }

    @Test
    fun invoke_scoreAtThreshold_callsAdvanceToNextLesson() = runTest {
        useCase("user1", threshold)

        coVerify(exactly = 1) { bookRepository.advanceToNextLesson("user1") }
    }

    @Test
    fun invoke_scoreAtThreshold_previousChapterAndLessonCorrect() = runTest {
        val result = useCase("user1", threshold)

        assertEquals(2, result.previousChapter)
        assertEquals(3, result.previousLesson)
    }

    @Test
    fun invoke_scoreAtThreshold_newChapterAndLessonFromRepository() = runTest {
        coEvery { bookRepository.advanceToNextLesson("user1") } returns Pair(2, 4)

        val result = useCase("user1", threshold)

        assertEquals(2, result.newChapter)
        assertEquals(4, result.newLesson)
    }

    @Test
    fun invoke_newChapterSameAsOld_isChapterCompleteFalse() = runTest {
        coEvery { bookRepository.advanceToNextLesson("user1") } returns Pair(2, 4)

        val result = useCase("user1", threshold)

        assertFalse(result.isChapterComplete)
    }

    @Test
    fun invoke_newChapterGreaterThanOld_isChapterCompleteTrue() = runTest {
        coEvery { bookRepository.advanceToNextLesson("user1") } returns Pair(3, 1)

        val result = useCase("user1", threshold)

        assertTrue(result.isChapterComplete)
    }

    @Test
    fun invoke_newPositionSameAsOld_isBookCompleteTrue() = runTest {
        // newChapter == currentChapter && newLesson == currentLesson
        coEvery { bookRepository.advanceToNextLesson("user1") } returns Pair(2, 3)

        val result = useCase("user1", threshold)

        assertTrue(result.isBookComplete)
    }

    @Test
    fun invoke_newPositionDiffersFromOld_isBookCompleteFalse() = runTest {
        coEvery { bookRepository.advanceToNextLesson("user1") } returns Pair(2, 4)

        val result = useCase("user1", threshold)

        assertFalse(result.isBookComplete)
    }

    @Test
    fun invoke_chapterAdvances_isBookCompleteFalse() = runTest {
        coEvery { bookRepository.advanceToNextLesson("user1") } returns Pair(3, 1)

        val result = useCase("user1", threshold)

        assertFalse(result.isBookComplete)
    }

    @Test
    fun invoke_scoreAboveThreshold_noUpsertProgressCalled() = runTest {
        useCase("user1", threshold)

        coVerify(exactly = 0) { bookRepository.upsertBookProgress(any()) }
    }

    @Test
    fun invoke_scoreAboveThreshold_doesNotCallGetBookProgress() = runTest {
        useCase("user1", threshold)

        coVerify(exactly = 0) { bookRepository.getBookProgress(any(), any(), any()) }
    }

    // ── Score < threshold → update progress ───────────────────────────────────

    @Test
    fun invoke_scoreBelowThreshold_doesNotMarkLessonComplete() = runTest {
        useCase("user1", 0.5f)

        coVerify(exactly = 0) { bookRepository.markLessonComplete(any(), any(), any(), any()) }
    }

    @Test
    fun invoke_scoreBelowThreshold_doesNotAdvanceLesson() = runTest {
        useCase("user1", 0.5f)

        coVerify(exactly = 0) { bookRepository.advanceToNextLesson(any()) }
    }

    @Test
    fun invoke_scoreBelowThreshold_positionUnchangedInResult() = runTest {
        val result = useCase("user1", 0.5f)

        assertEquals(2, result.previousChapter)
        assertEquals(3, result.previousLesson)
        assertEquals(2, result.newChapter)
        assertEquals(3, result.newLesson)
    }

    @Test
    fun invoke_scoreBelowThreshold_isChapterCompleteFalse() = runTest {
        val result = useCase("user1", 0.5f)

        assertFalse(result.isChapterComplete)
    }

    @Test
    fun invoke_scoreBelowThreshold_isBookCompleteFalse() = runTest {
        val result = useCase("user1", 0.5f)

        assertFalse(result.isBookComplete)
    }

    @Test
    fun invoke_scoreBelowThreshold_getsExistingProgress() = runTest {
        useCase("user1", 0.5f)

        coVerify(exactly = 1) { bookRepository.getBookProgress("user1", 2, 3) }
    }

    // ── Score < threshold, no existing progress → create new ──────────────────

    @Test
    fun invoke_scoreBelowThresholdNoExisting_upsertsNewProgress() = runTest {
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns null

        useCase("user1", 0.5f)

        coVerify(exactly = 1) { bookRepository.upsertBookProgress(any()) }
    }

    @Test
    fun invoke_scoreBelowThresholdNoExisting_newProgressHasGeneratedUUID() = runTest {
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns null
        var captured: BookProgress? = null
        coEvery { bookRepository.upsertBookProgress(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("user1", 0.5f)

        assertEquals(fixedUUID, captured?.id)
    }

    @Test
    fun invoke_scoreBelowThresholdNoExisting_newProgressUserIdCorrect() = runTest {
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns null
        var captured: BookProgress? = null
        coEvery { bookRepository.upsertBookProgress(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("user1", 0.5f)

        assertEquals("user1", captured?.userId)
    }

    @Test
    fun invoke_scoreBelowThresholdNoExisting_newProgressChapterAndLessonCorrect() = runTest {
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns null
        var captured: BookProgress? = null
        coEvery { bookRepository.upsertBookProgress(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("user1", 0.5f)

        assertEquals(2, captured?.chapter)
        assertEquals(3, captured?.lesson)
    }

    @Test
    fun invoke_scoreBelowThresholdNoExisting_newProgressStatusIsInProgress() = runTest {
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns null
        var captured: BookProgress? = null
        coEvery { bookRepository.upsertBookProgress(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("user1", 0.5f)

        assertEquals(LessonStatus.IN_PROGRESS, captured?.status)
    }

    @Test
    fun invoke_scoreBelowThresholdNoExisting_newProgressScoreMatchesParam() = runTest {
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns null
        var captured: BookProgress? = null
        coEvery { bookRepository.upsertBookProgress(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("user1", 0.65f)

        assertEquals(0.65f, captured?.score ?: -1f, 0.001f)
    }

    @Test
    fun invoke_scoreBelowThresholdNoExisting_startedAtIsNow() = runTest {
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns null
        var captured: BookProgress? = null
        coEvery { bookRepository.upsertBookProgress(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("user1", 0.5f)

        assertEquals(fixedNow, captured?.startedAt)
    }

    @Test
    fun invoke_scoreBelowThresholdNoExisting_timesPracticedIs1() = runTest {
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns null
        var captured: BookProgress? = null
        coEvery { bookRepository.upsertBookProgress(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("user1", 0.5f)

        assertEquals(1, captured?.timesPracticed)
    }

    @Test
    fun invoke_scoreBelowThresholdNoExisting_completedAtIsNull() = runTest {
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns null
        var captured: BookProgress? = null
        coEvery { bookRepository.upsertBookProgress(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("user1", 0.5f)

        assertNull(captured?.completedAt)
    }

    @Test
    fun invoke_scoreBelowThresholdNoExisting_notesIsNull() = runTest {
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns null
        var captured: BookProgress? = null
        coEvery { bookRepository.upsertBookProgress(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("user1", 0.5f)

        assertNull(captured?.notes)
    }

    // ── Score < threshold, existing progress → update ────────────────────────

    @Test
    fun invoke_scoreBelowThresholdWithExisting_upsertsUpdatedProgress() = runTest {
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns makeBookProgress()

        useCase("user1", 0.6f)

        coVerify(exactly = 1) { bookRepository.upsertBookProgress(any()) }
    }

    @Test
    fun invoke_scoreBelowThresholdWithExisting_statusSetToInProgress() = runTest {
        val existing = makeBookProgress(status = LessonStatus.NOT_STARTED)
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns existing
        var captured: BookProgress? = null
        coEvery { bookRepository.upsertBookProgress(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("user1", 0.6f)

        assertEquals(LessonStatus.IN_PROGRESS, captured?.status)
    }

    @Test
    fun invoke_scoreBelowThresholdWithExisting_scoreUpdated() = runTest {
        val existing = makeBookProgress(score = 0.4f)
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns existing
        var captured: BookProgress? = null
        coEvery { bookRepository.upsertBookProgress(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("user1", 0.6f)

        assertEquals(0.6f, captured?.score ?: -1f, 0.001f)
    }

    @Test
    fun invoke_scoreBelowThresholdWithExisting_timesPracticedIncremented() = runTest {
        val existing = makeBookProgress(timesPracticed = 3)
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns existing
        var captured: BookProgress? = null
        coEvery { bookRepository.upsertBookProgress(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("user1", 0.6f)

        assertEquals(4, captured?.timesPracticed)
    }

    @Test
    fun invoke_scoreBelowThresholdWithExistingStartedAtNotNull_startedAtPreserved() = runTest {
        val existingStart = fixedNow - 5000L
        val existing = makeBookProgress(startedAt = existingStart)
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns existing
        var captured: BookProgress? = null
        coEvery { bookRepository.upsertBookProgress(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("user1", 0.6f)

        assertEquals(existingStart, captured?.startedAt)
    }

    @Test
    fun invoke_scoreBelowThresholdWithExistingStartedAtNull_startedAtSetToNow() = runTest {
        val existing = makeBookProgress(startedAt = null)
        coEvery { bookRepository.getBookProgress("user1", 2, 3) } returns existing
        var captured: BookProgress? = null
        coEvery { bookRepository.upsertBookProgress(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("user1", 0.6f)

        assertEquals(fixedNow, captured?.startedAt)
    }

    // ── Score exactly at boundary ─────────────────────────────────────────────

    @Test
    fun invoke_scoreJustBelowThreshold_doesNotAdvance() = runTest {
        val justBelow = threshold - 0.001f
        useCase("user1", justBelow)

        coVerify(exactly = 0) { bookRepository.markLessonComplete(any(), any(), any(), any()) }
        coVerify(exactly = 0) { bookRepository.advanceToNextLesson(any()) }
    }

    @Test
    fun invoke_scoreJustAboveThreshold_advances() = runTest {
        val justAbove = threshold + 0.001f
        useCase("user1", justAbove)

        coVerify(exactly = 1) { bookRepository.markLessonComplete(any(), any(), any(), any()) }
        coVerify(exactly = 1) { bookRepository.advanceToNextLesson(any()) }
    }

    // ── BookAdvanceResult data class ──────────────────────────────────────────

    @Test
    fun bookAdvanceResult_creation_storesAllFields() {
        val result = AdvanceBookProgressUseCase.BookAdvanceResult(
            previousChapter   = 1,
            previousLesson    = 2,
            newChapter        = 1,
            newLesson         = 3,
            isChapterComplete = false,
            isBookComplete    = false
        )

        assertEquals(1,     result.previousChapter)
        assertEquals(2,     result.previousLesson)
        assertEquals(1,     result.newChapter)
        assertEquals(3,     result.newLesson)
        assertFalse(result.isChapterComplete)
        assertFalse(result.isBookComplete)
    }

    @Test
    fun bookAdvanceResult_copy_changesOnlySpecifiedField() {
        val original = AdvanceBookProgressUseCase.BookAdvanceResult(1, 2, 2, 1, true, false)
        val copy     = original.copy(isBookComplete = true)

        assertTrue(copy.isBookComplete)
        assertEquals(original.previousChapter,   copy.previousChapter)
        assertEquals(original.previousLesson,    copy.previousLesson)
        assertEquals(original.newChapter,        copy.newChapter)
        assertEquals(original.newLesson,         copy.newLesson)
        assertEquals(original.isChapterComplete, copy.isChapterComplete)
    }

    @Test
    fun bookAdvanceResult_equals_twoIdenticalInstancesAreEqual() {
        val a = AdvanceBookProgressUseCase.BookAdvanceResult(1, 2, 1, 3, false, false)
        val b = AdvanceBookProgressUseCase.BookAdvanceResult(1, 2, 1, 3, false, false)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun bookAdvanceResult_equals_differentNewLessonNotEqual() {
        val a = AdvanceBookProgressUseCase.BookAdvanceResult(1, 2, 1, 3, false, false)
        val b = AdvanceBookProgressUseCase.BookAdvanceResult(1, 2, 1, 4, false, false)

        assertNotEquals(a, b)
    }

    @Test
    fun bookAdvanceResult_equals_differentIsChapterCompleteNotEqual() {
        val a = AdvanceBookProgressUseCase.BookAdvanceResult(1, 1, 2, 1, true,  false)
        val b = AdvanceBookProgressUseCase.BookAdvanceResult(1, 1, 2, 1, false, false)

        assertNotEquals(a, b)
    }
}
