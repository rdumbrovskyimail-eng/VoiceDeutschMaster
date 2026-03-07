// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/book/GetCurrentLessonUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.book

import com.voicedeutsch.master.domain.model.book.BookProgress
import com.voicedeutsch.master.domain.model.book.Chapter
import com.voicedeutsch.master.domain.model.book.Lesson
import com.voicedeutsch.master.domain.model.book.LessonContent
import com.voicedeutsch.master.domain.model.book.LessonVocabularyEntry
import com.voicedeutsch.master.domain.repository.BookRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetCurrentLessonUseCaseTest {

    private lateinit var bookRepository: BookRepository
    private lateinit var useCase: GetCurrentLessonUseCase

    private val chapter  = mockk<Chapter>(relaxed = true)
    private val lesson   = mockk<Lesson>(relaxed = true)
    private val content  = mockk<LessonContent>(relaxed = true)
    private val progress = mockk<BookProgress>(relaxed = true)
    private val vocab    = listOf(mockk<LessonVocabularyEntry>(relaxed = true))

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        bookRepository = mockk()
        useCase = GetCurrentLessonUseCase(bookRepository)

        coEvery { bookRepository.getCurrentBookPosition(any()) } returns Pair(2, 5)
        coEvery { bookRepository.getChapter(any()) }             returns chapter
        coEvery { bookRepository.getLesson(any(), any()) }       returns lesson
        coEvery { bookRepository.getLessonContent(any(), any()) } returns content
        coEvery { bookRepository.getBookProgress(any(), any(), any()) } returns progress
        coEvery { bookRepository.getChapterVocabulary(any()) }   returns vocab
    }

    // ── invoke — happy path ───────────────────────────────────────────────────

    @Test
    fun invoke_allDataAvailable_returnsCurrentLessonData() = runTest {
        val result = useCase("user1")

        assertNotNull(result)
    }

    @Test
    fun invoke_allDataAvailable_chapterFromRepository() = runTest {
        val result = useCase("user1")

        assertEquals(chapter, result?.chapter)
    }

    @Test
    fun invoke_allDataAvailable_lessonFromRepository() = runTest {
        val result = useCase("user1")

        assertEquals(lesson, result?.lesson)
    }

    @Test
    fun invoke_allDataAvailable_contentFromRepository() = runTest {
        val result = useCase("user1")

        assertEquals(content, result?.content)
    }

    @Test
    fun invoke_allDataAvailable_progressFromRepository() = runTest {
        val result = useCase("user1")

        assertEquals(progress, result?.progress)
    }

    @Test
    fun invoke_allDataAvailable_vocabularyFromRepository() = runTest {
        val result = useCase("user1")

        assertEquals(vocab, result?.vocabulary)
    }

    @Test
    fun invoke_allDataAvailable_chapterNumberFromPosition() = runTest {
        coEvery { bookRepository.getCurrentBookPosition("user1") } returns Pair(3, 7)

        val result = useCase("user1")

        assertEquals(3, result?.chapterNumber)
    }

    @Test
    fun invoke_allDataAvailable_lessonNumberFromPosition() = runTest {
        coEvery { bookRepository.getCurrentBookPosition("user1") } returns Pair(3, 7)

        val result = useCase("user1")

        assertEquals(7, result?.lessonNumber)
    }

    @Test
    fun invoke_allDataAvailable_repositoryCalledWithCorrectChapterAndLesson() = runTest {
        coEvery { bookRepository.getCurrentBookPosition("user1") } returns Pair(4, 9)

        useCase("user1")

        coVerify { bookRepository.getChapter(4) }
        coVerify { bookRepository.getLesson(4, 9) }
        coVerify { bookRepository.getLessonContent(4, 9) }
        coVerify { bookRepository.getBookProgress("user1", 4, 9) }
        coVerify { bookRepository.getChapterVocabulary(4) }
    }

    // ── invoke — chapter not found ────────────────────────────────────────────

    @Test
    fun invoke_chapterNotFound_returnsNull() = runTest {
        coEvery { bookRepository.getChapter(any()) } returns null

        val result = useCase("user1")

        assertNull(result)
    }

    @Test
    fun invoke_chapterNotFound_lessonNotRequested() = runTest {
        coEvery { bookRepository.getChapter(any()) } returns null

        useCase("user1")

        coVerify(exactly = 0) { bookRepository.getLesson(any(), any()) }
    }

    @Test
    fun invoke_chapterNotFound_progressNotRequested() = runTest {
        coEvery { bookRepository.getChapter(any()) } returns null

        useCase("user1")

        coVerify(exactly = 0) { bookRepository.getBookProgress(any(), any(), any()) }
    }

    // ── invoke — lesson not found ─────────────────────────────────────────────

    @Test
    fun invoke_lessonNotFound_returnsNull() = runTest {
        coEvery { bookRepository.getLesson(any(), any()) } returns null

        val result = useCase("user1")

        assertNull(result)
    }

    @Test
    fun invoke_lessonNotFound_contentNotRequested() = runTest {
        coEvery { bookRepository.getLesson(any(), any()) } returns null

        useCase("user1")

        coVerify(exactly = 0) { bookRepository.getLessonContent(any(), any()) }
    }

    @Test
    fun invoke_lessonNotFound_vocabularyNotRequested() = runTest {
        coEvery { bookRepository.getLesson(any(), any()) } returns null

        useCase("user1")

        coVerify(exactly = 0) { bookRepository.getChapterVocabulary(any()) }
    }

    // ── invoke — nullable optional fields ────────────────────────────────────

    @Test
    fun invoke_contentIsNull_resultContentIsNull() = runTest {
        coEvery { bookRepository.getLessonContent(any(), any()) } returns null

        val result = useCase("user1")

        assertNull(result?.content)
        assertNotNull(result)
    }

    @Test
    fun invoke_progressIsNull_resultProgressIsNull() = runTest {
        coEvery { bookRepository.getBookProgress(any(), any(), any()) } returns null

        val result = useCase("user1")

        assertNull(result?.progress)
        assertNotNull(result)
    }

    @Test
    fun invoke_vocabularyIsEmpty_resultVocabularyIsEmpty() = runTest {
        coEvery { bookRepository.getChapterVocabulary(any()) } returns emptyList()

        val result = useCase("user1")

        assertTrue(result?.vocabulary?.isEmpty() == true)
    }

    @Test
    fun invoke_contentAndProgressBothNull_stillReturnsData() = runTest {
        coEvery { bookRepository.getLessonContent(any(), any()) } returns null
        coEvery { bookRepository.getBookProgress(any(), any(), any()) } returns null

        val result = useCase("user1")

        assertNotNull(result)
        assertNull(result?.content)
        assertNull(result?.progress)
    }

    // ── invoke — userId passed through ────────────────────────────────────────

    @Test
    fun invoke_positionFetchedWithCorrectUserId() = runTest {
        useCase("userXYZ")

        coVerify(exactly = 1) { bookRepository.getCurrentBookPosition("userXYZ") }
    }

    @Test
    fun invoke_progressFetchedWithCorrectUserId() = runTest {
        useCase("userXYZ")

        coVerify(exactly = 1) { bookRepository.getBookProgress("userXYZ", any(), any()) }
    }

    // ── CurrentLessonData data class ──────────────────────────────────────────

    @Test
    fun currentLessonData_creation_storesAllFields() {
        val data = GetCurrentLessonUseCase.CurrentLessonData(
            chapter       = chapter,
            lesson        = lesson,
            content       = content,
            progress      = progress,
            vocabulary    = vocab,
            chapterNumber = 2,
            lessonNumber  = 5
        )

        assertEquals(chapter,  data.chapter)
        assertEquals(lesson,   data.lesson)
        assertEquals(content,  data.content)
        assertEquals(progress, data.progress)
        assertEquals(vocab,    data.vocabulary)
        assertEquals(2,        data.chapterNumber)
        assertEquals(5,        data.lessonNumber)
    }

    @Test
    fun currentLessonData_copy_changesOnlySpecifiedField() {
        val original = GetCurrentLessonUseCase.CurrentLessonData(
            chapter, lesson, content, progress, vocab, 2, 5
        )
        val copy = original.copy(lessonNumber = 6)

        assertEquals(6,              copy.lessonNumber)
        assertEquals(original.chapter,        copy.chapter)
        assertEquals(original.lesson,         copy.lesson)
        assertEquals(original.chapterNumber,  copy.chapterNumber)
    }

    @Test
    fun currentLessonData_equals_twoIdenticalInstancesAreEqual() {
        val a = GetCurrentLessonUseCase.CurrentLessonData(
            chapter, lesson, content, progress, vocab, 1, 1
        )
        val b = GetCurrentLessonUseCase.CurrentLessonData(
            chapter, lesson, content, progress, vocab, 1, 1
        )

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun currentLessonData_equals_differentChapterNumberNotEqual() {
        val a = GetCurrentLessonUseCase.CurrentLessonData(
            chapter, lesson, content, progress, vocab, 1, 1
        )
        val b = GetCurrentLessonUseCase.CurrentLessonData(
            chapter, lesson, content, progress, vocab, 2, 1
        )

        assertNotEquals(a, b)
    }

    @Test
    fun currentLessonData_nullContentAndProgress_allowedInDataClass() {
        val data = GetCurrentLessonUseCase.CurrentLessonData(
            chapter       = chapter,
            lesson        = lesson,
            content       = null,
            progress      = null,
            vocabulary    = emptyList(),
            chapterNumber = 1,
            lessonNumber  = 1
        )

        assertNull(data.content)
        assertNull(data.progress)
        assertTrue(data.vocabulary.isEmpty())
    }
}
