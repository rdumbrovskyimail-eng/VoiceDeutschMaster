package com.voicedeutsch.master.voicecore.context

import com.voicedeutsch.master.domain.repository.BookRepository

/**
 * Provides the current book chapter/lesson text for the Gemini context.
 * Architecture lines 610-620 (Book Context block, ~5K tokens).
 */
class BookContextProvider(private val bookRepository: BookRepository) {

    suspend fun buildBookContext(chapter: Int, lesson: Int): String {
        val lessonContent = bookRepository.getLessonContent(chapter, lesson)
        val vocabulary = bookRepository.getChapterVocabulary(chapter)

        if (lessonContent == null) {
            return "=== BOOK CONTEXT ===\nКнига ещё не загружена.\n=== END BOOK CONTEXT ==="
        }

        return buildString {
            appendLine("=== BOOK CONTEXT ===")
            appendLine("Глава $chapter, Урок $lesson")
            appendLine()
            appendLine("ТЕКСТ УРОКА:")
            appendLine(lessonContent.text)

            if (lessonContent.exercises.isNotEmpty()) {
                appendLine()
                appendLine("УПРАЖНЕНИЯ:")
                lessonContent.exercises.forEach { exercise ->
                    appendLine("- ${exercise.instruction}: ${exercise.content}")
                }
            }

            if (vocabulary.isNotEmpty()) {
                appendLine()
                appendLine("СЛОВАРЬ ГЛАВЫ (${vocabulary.size} слов):")
                vocabulary.take(MAX_VOCABULARY_ENTRIES).forEach { entry ->
                    appendLine("${entry.german} — ${entry.russian}")
                }
            }
            appendLine("=== END BOOK CONTEXT ===")
        }
    }

    companion object {
        private const val MAX_VOCABULARY_ENTRIES = 50
    }
}