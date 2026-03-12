// Путь: src/test/java/com/voicedeutsch/master/presentation/screen/settings/BookManagerViewModelTest.kt
package com.voicedeutsch.master.presentation.screen.settings

import app.cash.turbine.test
import com.voicedeutsch.master.data.local.database.dao.BookDao
import com.voicedeutsch.master.data.local.database.entity.BookChapterEntity
import com.voicedeutsch.master.data.local.database.entity.BookEntity
import com.voicedeutsch.master.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.Awaits
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class BookManagerViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bookDao: BookDao
    private lateinit var sut: BookManagerViewModel

    private val booksFlow = MutableStateFlow<List<BookEntity>>(emptyList())

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildBookEntity(
        id: Long = 1L,
        title: String = "Deutsch A1",
        description: String? = "Beginner book",
    ) = BookEntity(id = id, title = title, description = description)

    private fun buildChapterEntity(
        id: Long = 10L,
        bookId: Long = 1L,
        chapterNumber: Int = 1,
        title: String = "Kapitel 1",
        content: String = "Content here",
    ) = BookChapterEntity(
        id = id,
        bookId = bookId,
        chapterNumber = chapterNumber,
        title = title,
        content = content,
    )

    @BeforeEach
    fun setUp() {
        bookDao = mockk(relaxed = true)
        every { bookDao.getAllBooksFlow() } returns booksFlow
        coEvery { bookDao.insertBook(any()) } just Runs
        coEvery { bookDao.deleteBook(any()) } just Awaits
        coEvery { bookDao.insertChapter(any()) } just Runs
        coEvery { bookDao.updateChapter(any()) } just Awaits
        coEvery { bookDao.deleteChapter(any()) } just Runs
        coEvery { bookDao.getChapterCount(any()) } returns 0
        coEvery { bookDao.getChaptersFlow(any()) } returns flowOf(emptyList())
        coEvery { bookDao.getChapterById(any()) } returns null

        sut = BookManagerViewModel(bookDao)
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialState_booksIsEmpty() {
        assertEquals(emptyList<BookEntity>(), sut.uiState.value.books)
    }

    @Test
    fun initialState_chaptersIsEmpty() {
        assertEquals(emptyList<BookChapterEntity>(), sut.uiState.value.chapters)
    }

    @Test
    fun initialState_selectedBookIsNull() {
        assertNull(sut.uiState.value.selectedBook)
    }

    @Test
    fun initialState_errorMessageIsNull() {
        assertNull(sut.uiState.value.errorMessage)
    }

    // ── Books flow ────────────────────────────────────────────────────────────

    @Test
    fun init_collectsAllBooksFlow_updatesBooks() = runTest {
        val books = listOf(buildBookEntity(id = 1L), buildBookEntity(id = 2L))

        booksFlow.value = books
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, sut.uiState.value.books.size)
    }

    @Test
    fun init_booksFlowUpdates_reflectedInState() = runTest {
        booksFlow.value = listOf(buildBookEntity(id = 1L))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, sut.uiState.value.books.size)

        booksFlow.value = listOf(buildBookEntity(id = 1L), buildBookEntity(id = 2L))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, sut.uiState.value.books.size)
    }

    // ── CreateBook ────────────────────────────────────────────────────────────

    @Test
    fun createBook_callsDaoInsertWithTitle() = runTest {
        sut.onEvent(BookManagerEvent.CreateBook("Mein Buch", "Beschreibung"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookDao.insertBook(match { it.title == "Mein Buch" }) }
    }

    @Test
    fun createBook_withDescription_passesDescriptionToDao() = runTest {
        sut.onEvent(BookManagerEvent.CreateBook("Title", "Some description"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookDao.insertBook(match { it.description == "Some description" }) }
    }

    @Test
    fun createBook_blankDescription_passesNullToDao() = runTest {
        sut.onEvent(BookManagerEvent.CreateBook("Title", "   "))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookDao.insertBook(match { it.description == null }) }
    }

    @Test
    fun createBook_emptyDescription_passesNullToDao() = runTest {
        sut.onEvent(BookManagerEvent.CreateBook("Title", ""))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookDao.insertBook(match { it.description == null }) }
    }

    @Test
    fun createBook_daoThrows_setsErrorMessage() = runTest {
        coEvery { bookDao.insertBook(any()) } throws RuntimeException("DB error")

        sut.onEvent(BookManagerEvent.CreateBook("Title", "Desc"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("Ошибка создания книги"))
        assertTrue(sut.uiState.value.errorMessage!!.contains("DB error"))
    }

    @Test
    fun createBook_success_noErrorMessage() = runTest {
        sut.onEvent(BookManagerEvent.CreateBook("Title", "Desc"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sut.uiState.value.errorMessage)
    }

    // ── DeleteBook ────────────────────────────────────────────────────────────

    @Test
    fun deleteBook_callsDaoDelete() = runTest {
        val book = buildBookEntity()
        sut.onEvent(BookManagerEvent.DeleteBook(book))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookDao.deleteBook(book) }
    }

    @Test
    fun deleteBook_whenSelectedBookDeleted_clearsSelection() = runTest {
        val book = buildBookEntity(id = 1L)
        coEvery { bookDao.getChaptersFlow(1L) } returns flowOf(emptyList())

        sut.onEvent(BookManagerEvent.SelectBook(book))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(book, sut.uiState.value.selectedBook)

        sut.onEvent(BookManagerEvent.DeleteBook(book))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sut.uiState.value.selectedBook)
        assertEquals(emptyList<BookChapterEntity>(), sut.uiState.value.chapters)
    }

    @Test
    fun deleteBook_whenDifferentBookDeleted_keepsSelection() = runTest {
        val selected = buildBookEntity(id = 1L)
        val other = buildBookEntity(id = 2L)
        coEvery { bookDao.getChaptersFlow(1L) } returns flowOf(emptyList())

        sut.onEvent(BookManagerEvent.SelectBook(selected))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(BookManagerEvent.DeleteBook(other))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(selected, sut.uiState.value.selectedBook)
    }

    @Test
    fun deleteBook_daoThrows_setsErrorMessage() = runTest {
        coEvery { bookDao.deleteBook(any()) } throws RuntimeException("Delete failed")

        sut.onEvent(BookManagerEvent.DeleteBook(buildBookEntity()))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("Ошибка удаления"))
        assertTrue(sut.uiState.value.errorMessage!!.contains("Delete failed"))
    }

    // ── SelectBook ────────────────────────────────────────────────────────────

    @Test
    fun selectBook_setsSelectedBook() = runTest {
        val book = buildBookEntity()
        coEvery { bookDao.getChaptersFlow(book.id) } returns flowOf(emptyList())

        sut.onEvent(BookManagerEvent.SelectBook(book))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(book, sut.uiState.value.selectedBook)
    }

    @Test
    fun selectBook_loadsChaptersForBook() = runTest {
        val book = buildBookEntity(id = 5L)
        val chapters = listOf(buildChapterEntity(bookId = 5L))
        coEvery { bookDao.getChaptersFlow(5L) } returns flowOf(chapters)

        sut.onEvent(BookManagerEvent.SelectBook(book))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, sut.uiState.value.chapters.size)
        assertEquals(5L, sut.uiState.value.chapters[0].bookId)
    }

    @Test
    fun selectBook_chaptersFlowUpdates_reflectedInState() = runTest {
        val book = buildBookEntity(id = 3L)
        val chaptersFlow = MutableStateFlow<List<BookChapterEntity>>(emptyList())
        coEvery { bookDao.getChaptersFlow(3L) } returns chaptersFlow

        sut.onEvent(BookManagerEvent.SelectBook(book))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, sut.uiState.value.chapters.size)

        chaptersFlow.value = listOf(buildChapterEntity(bookId = 3L))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, sut.uiState.value.chapters.size)
    }

    // ── DeselectBook ──────────────────────────────────────────────────────────

    @Test
    fun deselectBook_clearsSelectedBook() = runTest {
        val book = buildBookEntity()
        coEvery { bookDao.getChaptersFlow(any()) } returns flowOf(emptyList())

        sut.onEvent(BookManagerEvent.SelectBook(book))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(BookManagerEvent.DeselectBook)

        assertNull(sut.uiState.value.selectedBook)
    }

    @Test
    fun deselectBook_clearsChapters() = runTest {
        val book = buildBookEntity(id = 1L)
        coEvery { bookDao.getChaptersFlow(1L) } returns flowOf(listOf(buildChapterEntity()))

        sut.onEvent(BookManagerEvent.SelectBook(book))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(BookManagerEvent.DeselectBook)

        assertEquals(emptyList<BookChapterEntity>(), sut.uiState.value.chapters)
    }

    @Test
    fun deselectBook_whenNothingSelected_doesNotCrash() = runTest {
        assertDoesNotThrow { sut.onEvent(BookManagerEvent.DeselectBook) }
    }

    // ── CreateChapter ─────────────────────────────────────────────────────────

    @Test
    fun createChapter_noSelectedBook_doesNotCallDao() = runTest {
        sut.onEvent(BookManagerEvent.CreateChapter("Chapter 1", "Content"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { bookDao.insertChapter(any()) }
    }

    @Test
    fun createChapter_withSelectedBook_callsDaoInsert() = runTest {
        val book = buildBookEntity(id = 2L)
        coEvery { bookDao.getChaptersFlow(2L) } returns flowOf(emptyList())
        coEvery { bookDao.getChapterCount(2L) } returns 0

        sut.onEvent(BookManagerEvent.SelectBook(book))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(BookManagerEvent.CreateChapter("Kapitel 1", "Inhalt"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookDao.insertChapter(match { it.title == "Kapitel 1" && it.bookId == 2L }) }
    }

    @Test
    fun createChapter_chapterNumberIsCountPlusOne() = runTest {
        val book = buildBookEntity(id = 2L)
        coEvery { bookDao.getChaptersFlow(2L) } returns flowOf(emptyList())
        coEvery { bookDao.getChapterCount(2L) } returns 3

        sut.onEvent(BookManagerEvent.SelectBook(book))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(BookManagerEvent.CreateChapter("Title", "Content"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookDao.insertChapter(match { it.chapterNumber == 4 }) }
    }

    @Test
    fun createChapter_daoThrows_setsErrorMessage() = runTest {
        val book = buildBookEntity(id = 2L)
        coEvery { bookDao.getChaptersFlow(2L) } returns flowOf(emptyList())
        coEvery { bookDao.getChapterCount(2L) } returns 0
        coEvery { bookDao.insertChapter(any()) } throws RuntimeException("Insert failed")

        sut.onEvent(BookManagerEvent.SelectBook(book))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(BookManagerEvent.CreateChapter("Title", "Content"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("Ошибка создания главы"))
        assertTrue(sut.uiState.value.errorMessage!!.contains("Insert failed"))
    }

    @Test
    fun createChapter_withContent_passesContentToDao() = runTest {
        val book = buildBookEntity(id = 1L)
        coEvery { bookDao.getChaptersFlow(1L) } returns flowOf(emptyList())
        coEvery { bookDao.getChapterCount(1L) } returns 0

        sut.onEvent(BookManagerEvent.SelectBook(book))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(BookManagerEvent.CreateChapter("T", "Detailed content here"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookDao.insertChapter(match { it.content == "Detailed content here" }) }
    }

    // ── UpdateChapter ─────────────────────────────────────────────────────────

    @Test
    fun updateChapter_chapterNotFound_doesNotCallUpdate() = runTest {
        coEvery { bookDao.getChapterById(99L) } returns null

        sut.onEvent(BookManagerEvent.UpdateChapter(99L, "New Title", "New Content"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { bookDao.updateChapter(any()) }
    }

    @Test
    fun updateChapter_chapterFound_callsDaoUpdateWithNewValues() = runTest {
        val existing = buildChapterEntity(id = 5L, title = "Old Title", content = "Old Content")
        coEvery { bookDao.getChapterById(5L) } returns existing

        sut.onEvent(BookManagerEvent.UpdateChapter(5L, "New Title", "New Content"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            bookDao.updateChapter(match {
                it.id == 5L && it.title == "New Title" && it.content == "New Content"
            })
        }
    }

    @Test
    fun updateChapter_preservesOtherFields() = runTest {
        val existing = buildChapterEntity(id = 5L, bookId = 3L, chapterNumber = 2)
        coEvery { bookDao.getChapterById(5L) } returns existing

        sut.onEvent(BookManagerEvent.UpdateChapter(5L, "New Title", "New Content"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            bookDao.updateChapter(match {
                it.bookId == 3L && it.chapterNumber == 2
            })
        }
    }

    @Test
    fun updateChapter_daoThrows_setsErrorMessage() = runTest {
        val existing = buildChapterEntity(id = 5L)
        coEvery { bookDao.getChapterById(5L) } returns existing
        coEvery { bookDao.updateChapter(any()) } throws RuntimeException("Update failed")

        sut.onEvent(BookManagerEvent.UpdateChapter(5L, "Title", "Content"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("Ошибка обновления главы"))
        assertTrue(sut.uiState.value.errorMessage!!.contains("Update failed"))
    }

    // ── DeleteChapter ─────────────────────────────────────────────────────────

    @Test
    fun deleteChapter_callsDaoDelete() = runTest {
        val chapter = buildChapterEntity()
        sut.onEvent(BookManagerEvent.DeleteChapter(chapter))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookDao.deleteChapter(chapter) }
    }

    @Test
    fun deleteChapter_daoThrows_setsErrorMessage() = runTest {
        coEvery { bookDao.deleteChapter(any()) } throws RuntimeException("Delete chapter failed")

        sut.onEvent(BookManagerEvent.DeleteChapter(buildChapterEntity()))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("Ошибка удаления главы"))
        assertTrue(sut.uiState.value.errorMessage!!.contains("Delete chapter failed"))
    }

    @Test
    fun deleteChapter_success_noErrorMessage() = runTest {
        sut.onEvent(BookManagerEvent.DeleteChapter(buildChapterEntity()))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sut.uiState.value.errorMessage)
    }

    // ── DismissError ──────────────────────────────────────────────────────────

    @Test
    fun dismissError_clearsErrorMessage() = runTest {
        coEvery { bookDao.insertBook(any()) } throws RuntimeException("error")

        sut.onEvent(BookManagerEvent.CreateBook("T", "D"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sut.uiState.value.errorMessage)

        sut.onEvent(BookManagerEvent.DismissError)

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun dismissError_whenNoError_doesNotCrash() = runTest {
        assertDoesNotThrow { sut.onEvent(BookManagerEvent.DismissError) }
        assertNull(sut.uiState.value.errorMessage)
    }

    // ── uiState flow ──────────────────────────────────────────────────────────

    @Test
    fun uiState_flow_emitsOnBooksChange() = runTest {
        sut.uiState.test {
            awaitItem() // initial empty

            booksFlow.value = listOf(buildBookEntity())
            val updated = awaitItem()
            assertEquals(1, updated.books.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_flow_emitsOnSelectBook() = runTest {
        val book = buildBookEntity()
        coEvery { bookDao.getChaptersFlow(any()) } returns flowOf(emptyList())

        sut.uiState.test {
            awaitItem() // initial

            sut.onEvent(BookManagerEvent.SelectBook(book))
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertEquals(book, updated.selectedBook)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── BookManagerUiState data class ─────────────────────────────────────────

    @Test
    fun bookManagerUiState_defaultValues_areCorrect() {
        val state = BookManagerUiState()
        assertEquals(emptyList<BookEntity>(), state.books)
        assertEquals(emptyList<BookChapterEntity>(), state.chapters)
        assertNull(state.selectedBook)
        assertNull(state.errorMessage)
    }

    @Test
    fun bookManagerUiState_copy_changesOneField() {
        val original = BookManagerUiState()
        val book = buildBookEntity()
        val copy = original.copy(selectedBook = book)
        assertEquals(book, copy.selectedBook)
        assertEquals(original.books, copy.books)
    }

    @Test
    fun bookManagerUiState_equals_twoIdenticalInstances() {
        val a = BookManagerUiState()
        val b = BookManagerUiState()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
