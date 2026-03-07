// Путь: src/test/java/com/voicedeutsch/master/voicecore/context/BookContextProviderTest.kt
package com.voicedeutsch.master.voicecore.context

import com.voicedeutsch.master.data.local.database.dao.BookDao
import com.voicedeutsch.master.domain.repository.BookRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BookContextProviderTest {

    private lateinit var provider: BookContextProvider
    private val bookRepository = mockk<BookRepository>(relaxed = true)
    private val bookDao        = mockk<BookDao>(relaxed = true)

    @BeforeEach
    fun setUp() {
        provider = BookContextProvider(bookRepository, bookDao)
        coEvery { bookDao.getAllBooks() } returns emptyList()
    }

    // ── buildBookContext — structure ──────────────────────────────────────

    @Test
    fun buildBookContext_alwaysContainsHeader() = runTest {
        coEvery { bookRepository.getLessonContent(any(), any()) } returns null
        coEvery { bookRepository.getChapterVocabulary(any()) } returns emptyList()
        val result = provider.buildBookContext(1, 1)
        assertTrue(result.contains("=== BOOK CONTEXT ==="))
    }

    @Test
    fun buildBookContext_alwaysContainsFooter() = runTest {
        coEvery { bookRepository.getLessonContent(any(), any()) } returns null
        coEvery { bookRepository.getChapterVocabulary(any()) } returns emptyList()
        val result = provider.buildBookContext(1, 1)
        assertTrue(result.contains("=== END BOOK CONTEXT ==="))
    }

    @Test
    fun buildBookContext_invokesLessonContentWithCorrectArgs() = runTest {
        coEvery { bookRepository.getLessonContent(3, 7) } returns null
        coEvery { bookRepository.getChapterVocabulary(3) } returns emptyList()
        provider.buildBookContext(3, 7)
        coVerify(exactly = 1) { bookRepository.getLessonContent(3, 7) }
    }

    @Test
    fun buildBookContext_invokesChapterVocabularyWithCorrectChapter() = runTest {
        coEvery { bookRepository.getLessonContent(2, 4) } returns null
        coEvery { bookRepository.getChapterVocabulary(2) } returns emptyList()
        provider.buildBookContext(2, 4)
        coVerify(exactly = 1) { bookRepository.getChapterVocabulary(2) }
    }

    // ── buildBookContext — null lesson content ────────────────────────────

    @Test
    fun buildBookContext_nullLessonContent_containsFallbackMessage() = runTest {
        coEvery { bookRepository.getLessonContent(any(), any()) } returns null
        coEvery { bookRepository.getChapterVocabulary(any()) } returns emptyList()
        val result = provider.buildBookContext(1, 1)
        assertTrue(result.contains("Встроенная книга не загружена"))
    }

    @Test
    fun buildBookContext_nullLessonContent_doesNotContainLessonTextLabel() = runTest {
        coEvery { bookRepository.getLessonContent(any(), any()) } returns null
        coEvery { bookRepository.getChapterVocabulary(any()) } returns emptyList()
        val result = provider.buildBookContext(1, 1)
        assertFalse(result.contains("ТЕКСТ УРОКА"))
    }

    // ── buildBookContext — with lesson content ────────────────────────────

    @Test
    fun buildBookContext_withLessonContent_containsChapterAndLesson() = runTest {
        val content = buildLessonContent(mainContent = "Some text", exerciseMarkers = emptyList())
        coEvery { bookRepository.getLessonContent(2, 3) } returns content
        coEvery { bookRepository.getChapterVocabulary(2) } returns emptyList()
        val result = provider.buildBookContext(2, 3)
        assertTrue(result.contains("Глава 2"))
        assertTrue(result.contains("Урок 3"))
    }

    @Test
    fun buildBookContext_withLessonContent_containsMainContent() = runTest {
        val content = buildLessonContent(mainContent = "Der Hund ist groß.", exerciseMarkers = emptyList())
        coEvery { bookRepository.getLessonContent(1, 1) } returns content
        coEvery { bookRepository.getChapterVocabulary(1) } returns emptyList()
        val result = provider.buildBookContext(1, 1)
        assertTrue(result.contains("Der Hund ist groß."))
    }

    @Test
    fun buildBookContext_withLessonContent_containsLessonTextLabel() = runTest {
        val content = buildLessonContent(mainContent = "text", exerciseMarkers = emptyList())
        coEvery { bookRepository.getLessonContent(1, 1) } returns content
        coEvery { bookRepository.getChapterVocabulary(1) } returns emptyList()
        val result = provider.buildBookContext(1, 1)
        assertTrue(result.contains("ТЕКСТ УРОКА"))
    }

    // ── buildBookContext — exercise markers ───────────────────────────────

    @Test
    fun buildBookContext_withExerciseMarkers_containsExercisesLabel() = runTest {
        val content = buildLessonContent(
            mainContent = "text",
            exerciseMarkers = listOf("EX_1", "EX_2"),
        )
        coEvery { bookRepository.getLessonContent(1, 1) } returns content
        coEvery { bookRepository.getChapterVocabulary(1) } returns emptyList()
        val result = provider.buildBookContext(1, 1)
        assertTrue(result.contains("УПРАЖНЕНИЯ"))
        assertTrue(result.contains("EX_1"))
        assertTrue(result.contains("EX_2"))
    }

    @Test
    fun buildBookContext_emptyExerciseMarkers_doesNotContainExercisesLabel() = runTest {
        val content = buildLessonContent(mainContent = "text", exerciseMarkers = emptyList())
        coEvery { bookRepository.getLessonContent(1, 1) } returns content
        coEvery { bookRepository.getChapterVocabulary(1) } returns emptyList()
        val result = provider.buildBookContext(1, 1)
        assertFalse(result.contains("УПРАЖНЕНИЯ"))
    }

    // ── buildBookContext — vocabulary ─────────────────────────────────────

    @Test
    fun buildBookContext_withVocabulary_containsVocabularyLabel() = runTest {
        val content = buildLessonContent(mainContent = "text", exerciseMarkers = emptyList())
        val vocab = listOf(buildVocabEntry("Haus", "дом"), buildVocabEntry("Hund", "собака"))
        coEvery { bookRepository.getLessonContent(1, 1) } returns content
        coEvery { bookRepository.getChapterVocabulary(1) } returns vocab
        val result = provider.buildBookContext(1, 1)
        assertTrue(result.contains("СЛОВАРЬ ГЛАВЫ"))
        assertTrue(result.contains("Haus"))
        assertTrue(result.contains("дом"))
    }

    @Test
    fun buildBookContext_emptyVocabulary_doesNotContainVocabularyLabel() = runTest {
        val content = buildLessonContent(mainContent = "text", exerciseMarkers = emptyList())
        coEvery { bookRepository.getLessonContent(1, 1) } returns content
        coEvery { bookRepository.getChapterVocabulary(1) } returns emptyList()
        val result = provider.buildBookContext(1, 1)
        assertFalse(result.contains("СЛОВАРЬ ГЛАВЫ"))
    }

    @Test
    fun buildBookContext_vocabularyExceeds50_onlyFirst50Shown() = runTest {
        val content = buildLessonContent(mainContent = "text", exerciseMarkers = emptyList())
        val vocab = (1..60).map { buildVocabEntry("Wort_$it", "слово_$it") }
        coEvery { bookRepository.getLessonContent(1, 1) } returns content
        coEvery { bookRepository.getChapterVocabulary(1) } returns vocab
        val result = provider.buildBookContext(1, 1)
        assertTrue(result.contains("Wort_50"))
        assertFalse(result.contains("Wort_51"))
    }

    @Test
    fun buildBookContext_vocabularyCountShownInLabel() = runTest {
        val content = buildLessonContent(mainContent = "text", exerciseMarkers = emptyList())
        val vocab = listOf(buildVocabEntry("A", "a"), buildVocabEntry("B", "b"))
        coEvery { bookRepository.getLessonContent(1, 1) } returns content
        coEvery { bookRepository.getChapterVocabulary(1) } returns vocab
        val result = provider.buildBookContext(1, 1)
        assertTrue(result.contains("2 слов"))
    }

    // ── buildUserBooksContext — no books ──────────────────────────────────

    @Test
    fun buildBookContext_noUserBooks_doesNotContainUserBooksSection() = runTest {
        coEvery { bookRepository.getLessonContent(any(), any()) } returns null
        coEvery { bookRepository.getChapterVocabulary(any()) } returns emptyList()
        coEvery { bookDao.getAllBooks() } returns emptyList()
        val result = provider.buildBookContext(1, 1)
        assertFalse(result.contains("ПОЛЬЗОВАТЕЛЬСКИЕ КНИГИ"))
    }

    // ── buildUserBooksContext — with books ────────────────────────────────

    @Test
    fun buildBookContext_withUserBooks_containsUserBooksSectionHeader() = runTest {
        coEvery { bookRepository.getLessonContent(any(), any()) } returns null
        coEvery { bookRepository.getChapterVocabulary(any()) } returns emptyList()
        val book = buildUserBook(id = "b1", title = "Mein Buch")
        coEvery { bookDao.getAllBooks() } returns listOf(book)
        coEvery { bookDao.getChapters("b1") } returns listOf(
            buildUserChapter(chapterNumber = 1, title = "Kapitel 1", content = "Content here")
        )
        val result = provider.buildBookContext(1, 1)
        assertTrue(result.contains("ПОЛЬЗОВАТЕЛЬСКИЕ КНИГИ"))
        assertTrue(result.contains("Mein Buch"))
    }

    @Test
    fun buildBookContext_userBookChaptersExceed5_onlyFirst5Shown() = runTest {
        coEvery { bookRepository.getLessonContent(any(), any()) } returns null
        coEvery { bookRepository.getChapterVocabulary(any()) } returns emptyList()
        val book = buildUserBook(id = "b1", title = "Buch")
        coEvery { bookDao.getAllBooks() } returns listOf(book)
        val chapters = (1..7).map { buildUserChapter(it, "Kapitel $it", "text $it") }
        coEvery { bookDao.getChapters("b1") } returns chapters
        val result = provider.buildBookContext(1, 1)
        assertTrue(result.contains("Kapitel 5"))
        assertFalse(result.contains("Kapitel 6"))
    }

    @Test
    fun buildBookContext_userBooksExceed3_onlyFirst3Processed() = runTest {
        coEvery { bookRepository.getLessonContent(any(), any()) } returns null
        coEvery { bookRepository.getChapterVocabulary(any()) } returns emptyList()
        val books = (1..5).map { buildUserBook("b$it", "Buch $it") }
        coEvery { bookDao.getAllBooks() } returns books
        books.forEach { b ->
            coEvery { bookDao.getChapters(b.id) } returns listOf(
                buildUserChapter(1, "Ch", "text")
            )
        }
        val result = provider.buildBookContext(1, 1)
        assertTrue(result.contains("Buch 3"))
        assertFalse(result.contains("Buch 4"))
    }

    @Test
    fun buildBookContext_chapterContentExceedsLimit_showsTruncationHint() = runTest {
        coEvery { bookRepository.getLessonContent(any(), any()) } returns null
        coEvery { bookRepository.getChapterVocabulary(any()) } returns emptyList()
        val book = buildUserBook("b1", "Buch")
        coEvery { bookDao.getAllBooks() } returns listOf(book)
        val longContent = "X".repeat(3_000)
        coEvery { bookDao.getChapters("b1") } returns listOf(
            buildUserChapter(1, "Ch", longContent)
        )
        val result = provider.buildBookContext(1, 1)
        assertTrue(result.contains("ещё") && result.contains("символов"))
    }

    @Test
    fun buildBookContext_chapterContentWithinLimit_noTruncationHint() = runTest {
        coEvery { bookRepository.getLessonContent(any(), any()) } returns null
        coEvery { bookRepository.getChapterVocabulary(any()) } returns emptyList()
        val book = buildUserBook("b1", "Buch")
        coEvery { bookDao.getAllBooks() } returns listOf(book)
        val shortContent = "Short content"
        coEvery { bookDao.getChapters("b1") } returns listOf(
            buildUserChapter(1, "Ch", shortContent)
        )
        val result = provider.buildBookContext(1, 1)
        assertFalse(result.contains("символов"))
    }

    @Test
    fun buildBookContext_bookWithNoChapters_skippedSilently() = runTest {
        coEvery { bookRepository.getLessonContent(any(), any()) } returns null
        coEvery { bookRepository.getChapterVocabulary(any()) } returns emptyList()
        val book = buildUserBook("b1", "Пустая книга")
        coEvery { bookDao.getAllBooks() } returns listOf(book)
        coEvery { bookDao.getChapters("b1") } returns emptyList()
        val result = provider.buildBookContext(1, 1)
        assertFalse(result.contains("Пустая книга"))
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private fun buildLessonContent(
        mainContent: String,
        exerciseMarkers: List<String>,
    ) = mockk<com.voicedeutsch.master.domain.model.book.LessonContent>(relaxed = true) {
        io.mockk.every { this@mockk.mainContent } returns mainContent
        io.mockk.every { this@mockk.exerciseMarkers } returns exerciseMarkers
    }

    private fun buildVocabEntry(german: String, russian: String) =
        mockk<com.voicedeutsch.master.domain.model.book.VocabularyEntry>(relaxed = true) {
            io.mockk.every { this@mockk.german } returns german
            io.mockk.every { this@mockk.russian } returns russian
        }

    private fun buildUserBook(id: String, title: String) =
        mockk<com.voicedeutsch.master.data.local.database.entity.BookEntity>(relaxed = true) {
            io.mockk.every { this@mockk.id } returns id
            io.mockk.every { this@mockk.title } returns title
        }

    private fun buildUserChapter(chapterNumber: Int, title: String, content: String) =
        mockk<com.voicedeutsch.master.data.local.database.entity.ChapterEntity>(relaxed = true) {
            io.mockk.every { this@mockk.chapterNumber } returns chapterNumber
            io.mockk.every { this@mockk.title } returns title
            io.mockk.every { this@mockk.content } returns content
        }
}
