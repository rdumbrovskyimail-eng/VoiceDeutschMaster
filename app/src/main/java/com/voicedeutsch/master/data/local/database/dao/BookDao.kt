package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.voicedeutsch.master.data.local.database.entity.BookChapterEntity
import com.voicedeutsch.master.data.local.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    // ── Books ─────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM books ORDER BY created_at DESC")
    fun getAllBooksFlow(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY created_at DESC")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    // ── Chapters ──────────────────────────────────────────────────────────────

    @Query("SELECT * FROM book_chapters WHERE book_id = :bookId ORDER BY chapter_number ASC")
    fun getChaptersFlow(bookId: Long): Flow<List<BookChapterEntity>>

    @Query("SELECT * FROM book_chapters WHERE book_id = :bookId ORDER BY chapter_number ASC")
    suspend fun getChapters(bookId: Long): List<BookChapterEntity>

    @Query("SELECT * FROM book_chapters WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: Long): BookChapterEntity?

    @Query("SELECT * FROM book_chapters WHERE book_id = :bookId AND chapter_number = :chapterNumber")
    suspend fun getChapterByNumber(bookId: Long, chapterNumber: Int): BookChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: BookChapterEntity): Long

    @Update
    suspend fun updateChapter(chapter: BookChapterEntity)

    @Delete
    suspend fun deleteChapter(chapter: BookChapterEntity)

    @Query("SELECT COUNT(*) FROM book_chapters WHERE book_id = :bookId")
    suspend fun getChapterCount(bookId: Long): Int
}
