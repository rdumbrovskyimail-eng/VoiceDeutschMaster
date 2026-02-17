package com.voicedeutsch.master.domain.usecase.book

import com.voicedeutsch.master.domain.model.book.BookProgress
import com.voicedeutsch.master.domain.model.book.LessonStatus
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.util.Constants
import com.voicedeutsch.master.util.DateUtils
import com.voicedeutsch.master.util.generateUUID

/**
 * Completes the current lesson (if score is sufficient) and advances to the next one.
 * Called via Function Call: advance_to_next_lesson() from Gemini.
 *
 * Book completion threshold: score >= 0.8 (BOOK_LESSON_COMPLETION_THRESHOLD).
 */
class AdvanceBookProgressUseCase(
    private val bookRepository: BookRepository
) {

    data class BookAdvanceResult(
        val previousChapter: Int,
        val previousLesson: Int,
        val newChapter: Int,
        val newLesson: Int,
        val isChapterComplete: Boolean,
        val isBookComplete: Boolean
    )

    suspend operator fun invoke(userId: String, score: Float): BookAdvanceResult {
        val (currentChapter, currentLesson) = bookRepository.getCurrentBookPosition(userId)

        if (score >= Constants.BOOK_LESSON_COMPLETION_THRESHOLD) {
            bookRepository.markLessonComplete(userId, currentChapter, currentLesson, score)

            val (newChapter, newLesson) = bookRepository.advanceToNextLesson(userId)

            val isChapterComplete = newChapter > currentChapter
            val isBookComplete = newChapter == currentChapter && newLesson == currentLesson

            return BookAdvanceResult(
                previousChapter = currentChapter,
                previousLesson = currentLesson,
                newChapter = newChapter,
                newLesson = newLesson,
                isChapterComplete = isChapterComplete,
                isBookComplete = isBookComplete
            )
        } else {
            val now = DateUtils.nowTimestamp()
            val existingProgress = bookRepository.getBookProgress(
                userId, currentChapter, currentLesson
            )

            if (existingProgress != null) {
                val updated = existingProgress.copy(
                    status = LessonStatus.IN_PROGRESS,
                    score = score,
                    startedAt = existingProgress.startedAt ?: now,
                    timesPracticed = existingProgress.timesPracticed + 1
                )
                bookRepository.upsertBookProgress(updated)
            } else {
                val newProgress = BookProgress(
                    id = generateUUID(),
                    userId = userId,
                    chapter = currentChapter,
                    lesson = currentLesson,
                    status = LessonStatus.IN_PROGRESS,
                    score = score,
                    startedAt = now,
                    completedAt = null,
                    timesPracticed = 1,
                    notes = null
                )
                bookRepository.upsertBookProgress(newProgress)
            }

            return BookAdvanceResult(
                previousChapter = currentChapter,
                previousLesson = currentLesson,
                newChapter = currentChapter,
                newLesson = currentLesson,
                isChapterComplete = false,
                isBookComplete = false
            )
        }
    }
}