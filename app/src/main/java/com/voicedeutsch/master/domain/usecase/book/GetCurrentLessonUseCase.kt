package com.voicedeutsch.master.domain.usecase.book

import com.voicedeutsch.master.domain.model.book.BookProgress
import com.voicedeutsch.master.domain.model.book.Chapter
import com.voicedeutsch.master.domain.model.book.Lesson
import com.voicedeutsch.master.domain.model.book.LessonContent
import com.voicedeutsch.master.domain.model.book.LessonVocabularyEntry
import com.voicedeutsch.master.domain.repository.BookRepository

/**
 * Retrieves the current lesson with full content, chapter data, and vocabulary.
 * Used by ContextBuilder and BookScreen ViewModel.
 */
class GetCurrentLessonUseCase(
    private val bookRepository: BookRepository
) {

    data class CurrentLessonData(
        val chapter: Chapter,
        val lesson: Lesson,
        val content: LessonContent?,
        val progress: BookProgress?,
        val vocabulary: List<LessonVocabularyEntry>,
        val chapterNumber: Int,
        val lessonNumber: Int
    )

    suspend operator fun invoke(userId: String): CurrentLessonData? {
        val (chapterNumber, lessonNumber) = bookRepository.getCurrentBookPosition(userId)

        val chapter = bookRepository.getChapter(chapterNumber) ?: return null
        val lesson = bookRepository.getLesson(chapterNumber, lessonNumber) ?: return null
        val content = bookRepository.getLessonContent(chapterNumber, lessonNumber)
        val progress = bookRepository.getBookProgress(userId, chapterNumber, lessonNumber)
        val vocabulary = bookRepository.getChapterVocabulary(chapterNumber)

        return CurrentLessonData(
            chapter = chapter,
            lesson = lesson,
            content = content,
            progress = progress,
            vocabulary = vocabulary,
            chapterNumber = chapterNumber,
            lessonNumber = lessonNumber
        )
    }
}