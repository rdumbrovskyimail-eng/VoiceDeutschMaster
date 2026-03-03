package com.voicedeutsch.master.voicecore.context

import com.voicedeutsch.master.data.local.database.dao.BookDao
import com.voicedeutsch.master.domain.repository.BookRepository

/**
 * Provides the current book chapter/lesson text for the Gemini context.
 *
 * Загружает контент из двух источников:
 * 1. Встроенная книга (assets) через BookRepository
 * 2. Пользовательские книги (Room) через BookDao
 */
class BookContextProvider(
    private val bookRepository: BookRepository,
    private val bookDao: BookDao,
) {

    suspend fun buildBookContext(chapter: Int, lesson: Int): String {
        val lessonContent = bookRepository.getLessonContent(chapter, lesson)
        val vocabulary = bookRepository.getChapterVocabulary(chapter)

        val userBooksContext = buildUserBooksContext()

        return buildString {
            appendLine("=== BOOK CONTEXT ===")

            if (lessonContent != null) {
                appendLine("Глава $chapter, Урок $lesson")
                appendLine()
                appendLine("ТЕКСТ УРОКА:")
                appendLine(lessonContent.mainContent)

                if (lessonContent.exerciseMarkers.isNotEmpty()) {
                    appendLine()
                    appendLine("УПРАЖНЕНИЯ:")
                    lessonContent.exerciseMarkers.forEach { marker ->
                        appendLine("- $marker")
                    }
                }

                if (vocabulary.isNotEmpty()) {
                    appendLine()
                    appendLine("СЛОВАРЬ ГЛАВЫ (${vocabulary.size} слов):")
                    vocabulary.take(MAX_VOCABULARY_ENTRIES).forEach { entry ->
                        appendLine("${entry.german} — ${entry.russian}")
                    }
                }
            } else {
                appendLine("Встроенная книга не загружена.")
            }

            if (userBooksContext.isNotEmpty()) {
                appendLine()
                appendLine("--- ПОЛЬЗОВАТЕЛЬСКИЕ КНИГИ ---")
                appendLine(userBooksContext)
            }

            appendLine("=== END BOOK CONTEXT ===")
        }
    }

    private suspend fun buildUserBooksContext(): String {
        val books = bookDao.getAllBooks()
        if (books.isEmpty()) return ""

        return buildString {
            for (book in books.take(MAX_USER_BOOKS)) {
                val chapters = bookDao.getChapters(book.id)
                if (chapters.isEmpty()) continue

                appendLine("Книга: ${book.title}")
                for (ch in chapters.take(MAX_CHAPTERS_PER_BOOK)) {
                    val preview = ch.content.take(MAX_CHAPTER_PREVIEW_CHARS)
                    appendLine("  Глава ${ch.chapterNumber}: ${ch.title}")
                    appendLine("  $preview")
                    if (ch.content.length > MAX_CHAPTER_PREVIEW_CHARS) {
                        appendLine("  ... (ещё ${ch.content.length - MAX_CHAPTER_PREVIEW_CHARS} символов)")
                    }
                }
            }
        }
    }

    companion object {
        private const val MAX_VOCABULARY_ENTRIES = 50
        private const val MAX_USER_BOOKS = 3
        private const val MAX_CHAPTERS_PER_BOOK = 5
        private const val MAX_CHAPTER_PREVIEW_CHARS = 2_000
    }
}
