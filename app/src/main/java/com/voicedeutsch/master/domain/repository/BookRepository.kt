package com.voicedeutsch.master.domain.repository

import com.voicedeutsch.master.domain.model.book.BookMetadata
import com.voicedeutsch.master.domain.model.book.BookProgress
import com.voicedeutsch.master.domain.model.book.Chapter
import com.voicedeutsch.master.domain.model.book.Lesson
import com.voicedeutsch.master.domain.model.book.LessonContent
import com.voicedeutsch.master.domain.model.book.LessonVocabularyEntry
import kotlinx.coroutines.flow.Flow

interface BookRepository {

    suspend fun getBookMetadata(): BookMetadata

    suspend fun getChapter(chapterNumber: Int): Chapter?

    suspend fun getLesson(chapterNumber: Int, lessonNumber: Int): Lesson?

    suspend fun getLessonContent(chapterNumber: Int, lessonNumber: Int): LessonContent?

    suspend fun getChapterVocabulary(chapterNumber: Int): List<LessonVocabularyEntry>

    suspend fun getChapterGrammar(chapterNumber: Int): List<String>

    // ==========================================
    // BOOK PROGRESS
    // ==========================================

    suspend fun getBookProgress(userId: String, chapter: Int, lesson: Int): BookProgress?

    suspend fun getCurrentBookPosition(userId: String): Pair<Int, Int>

    suspend fun getAllBookProgress(userId: String): List<BookProgress>

    fun getBookProgressFlow(userId: String): Flow<List<BookProgress>>

    suspend fun upsertBookProgress(progress: BookProgress)

    suspend fun markLessonComplete(userId: String, chapter: Int, lesson: Int, score: Float)

    suspend fun advanceToNextLesson(userId: String): Pair<Int, Int>

    suspend fun getCompletedLessonsCount(userId: String): Int

    suspend fun getTotalLessonsCount(): Int

    suspend fun getBookCompletionPercentage(userId: String): Float

    // ==========================================
    // INITIALIZATION
    // ==========================================

    suspend fun isBookLoaded(): Boolean

    suspend fun loadBookIntoDatabase()
}