// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/book/GetChapterContentUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.book

import com.voicedeutsch.master.domain.model.book.Chapter
import com.voicedeutsch.master.domain.repository.BookRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetChapterContentUseCaseTest {

    private lateinit var bookRepository: BookRepository
    private lateinit var useCase: GetChapterContentUseCase

    private val chapter = mockk<Chapter>(relaxed = true)

    @BeforeEach
    fun setUp() {
        bookRepository = mockk()
        useCase = GetChapterContentUseCase(bookRepository)

        coEvery { bookRepository.getChapter(any()) } returns chapter
    }

    // ── invoke — delegates to repository ─────────────────────────────────────

    @Test
    fun invoke_chapterExists_returnsChapter() = runTest {
        val result = useCase(makeParams(3))

        assertEquals(chapter, result)
    }

    @Test
    fun invoke_chapterNotFound_returnsNull() = runTest {
        coEvery { bookRepository.getChapter(99) } returns null

        val result = useCase(makeParams(99))

        assertNull(result)
    }

    @Test
    fun invoke_chapterNumberPassedToRepository() = runTest {
        useCase(makeParams(5))

        coVerify(exactly = 1) { bookRepository.getChapter(5) }
    }

    @Test
    fun invoke_calledMultipleTimes_repositoryCalledEachTime() = runTest {
        useCase(makeParams(1))
        useCase(makeParams(2))
        useCase(makeParams(3))

        coVerify(exactly = 1) { bookRepository.getChapter(1) }
        coVerify(exactly = 1) { bookRepository.getChapter(2) }
        coVerify(exactly = 1) { bookRepository.getChapter(3) }
    }

    @Test
    fun invoke_chapterNumber0_passedToRepository() = runTest {
        useCase(makeParams(0))

        coVerify(exactly = 1) { bookRepository.getChapter(0) }
    }

    // ── Params data class ─────────────────────────────────────────────────────

    @Test
    fun params_creation_storesChapterNumber() {
        val params = GetChapterContentUseCase.Params(chapterNumber = 7)

        assertEquals(7, params.chapterNumber)
    }

    @Test
    fun params_copy_changesChapterNumber() {
        val original = GetChapterContentUseCase.Params(chapterNumber = 2)
        val copy     = original.copy(chapterNumber = 5)

        assertEquals(5, copy.chapterNumber)
    }

    @Test
    fun params_equals_twoIdenticalInstancesAreEqual() {
        val a = GetChapterContentUseCase.Params(chapterNumber = 4)
        val b = GetChapterContentUseCase.Params(chapterNumber = 4)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun params_equals_differentChapterNumberNotEqual() {
        val a = GetChapterContentUseCase.Params(chapterNumber = 1)
        val b = GetChapterContentUseCase.Params(chapterNumber = 2)

        assertNotEquals(a, b)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeParams(chapterNumber: Int) =
        GetChapterContentUseCase.Params(chapterNumber = chapterNumber)
}
