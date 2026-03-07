// Путь: src/androidTest/java/com/voicedeutsch/master/data/local/database/dao/BookDaoTest.kt
package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.voicedeutsch.master.data.local.database.VoiceDeutschDatabase
import com.voicedeutsch.master.data.local.database.entity.BookChapterEntity
import com.voicedeutsch.master.data.local.database.entity.BookEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class BookDaoTest {

    private lateinit var db: VoiceDeutschDatabase
    private lateinit var bookDao: BookDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VoiceDeutschDatabase::class.java
        ).allowMainThreadQueries().build()
        bookDao = db.bookDao()
    }

    @After
    fun tearDown() = db.close()

    private fun makeBook(title: String = "Menschen A1") = BookEntity(title = title)
    private fun makeChapter(bookId: Long, num: Int = 1, title: String = "Kapitel $num") =
        BookChapterEntity(bookId = bookId, chapterNumber = num, title = title, content = "Content $num")

    @Test
    fun getAllBooks_empty_returnsEmptyList() = runTest {
        assertTrue(bookDao.getAllBooks().isEmpty())
    }

    @Test
    fun getAllBooksFlow_emitsInsertedBooks() = runTest {
        bookDao.insertBook(makeBook("Book A"))
        val result = bookDao.getAllBooksFlow().first()
        assertEquals(1, result.size)
        assertEquals("Book A", result[0].title)
    }

    @Test
    fun getAllBooks_returnsOrderedByCreatedAtDesc() = runTest {
        bookDao.insertBook(makeBook("Old Book").copy(createdAt = 1000L))
        bookDao.insertBook(makeBook("New Book").copy(createdAt = 2000L))
        val result = bookDao.getAllBooks()
        assertEquals("New Book", result[0].title)
        assertEquals("Old Book", result[1].title)
    }

    @Test
    fun getBookById_existing_returnsBook() = runTest {
        val id = bookDao.insertBook(makeBook("Test Book"))
        val result = bookDao.getBookById(id)
        assertNotNull(result)
        assertEquals("Test Book", result!!.title)
    }

    @Test
    fun getBookById_nonExisting_returnsNull() = runTest {
        assertNull(bookDao.getBookById(999L))
    }

    @Test
    fun insertBook_returnsGeneratedId() = runTest {
        assertTrue(bookDao.insertBook(makeBook()) > 0)
    }

    @Test
    fun insertBook_twoBooks_differentIds() = runTest {
        val id1 = bookDao.insertBook(makeBook("Book 1"))
        val id2 = bookDao.insertBook(makeBook("Book 2"))
        assertNotEquals(id1, id2)
    }

    @Test
    fun updateBook_changesTitle() = runTest {
        val id = bookDao.insertBook(makeBook("Original"))
        val inserted = bookDao.getBookById(id)!!
        bookDao.updateBook(inserted.copy(title = "Updated"))
        assertEquals("Updated", bookDao.getBookById(id)!!.title)
    }

    @Test
    fun deleteBook_removesFromDatabase() = runTest {
        val id = bookDao.insertBook(makeBook())
        bookDao.deleteBook(bookDao.getBookById(id)!!)
        assertNull(bookDao.getBookById(id))
    }

    @Test
    fun deleteBook_cascadesChapters() = runTest {
        val bookId = bookDao.insertBook(makeBook())
        bookDao.insertChapter(makeChapter(bookId, 1))
        bookDao.deleteBook(bookDao.getBookById(bookId)!!)
        assertTrue(bookDao.getChapters(bookId).isEmpty())
    }

    @Test
    fun getChapters_empty_returnsEmptyList() = runTest {
        val bookId = bookDao.insertBook(makeBook())
        assertTrue(bookDao.getChapters(bookId).isEmpty())
    }

    @Test
    fun getChapters_orderedByChapterNumber() = runTest {
        val bookId = bookDao.insertBook(makeBook())
        bookDao.insertChapter(makeChapter(bookId, 3))
        bookDao.insertChapter(makeChapter(bookId, 1))
        bookDao.insertChapter(makeChapter(bookId, 2))
        assertEquals(listOf(1, 2, 3), bookDao.getChapters(bookId).map { it.chapterNumber })
    }

    @Test
    fun getChaptersFlow_emitsCurrentChapters() = runTest {
        val bookId = bookDao.insertBook(makeBook())
        bookDao.insertChapter(makeChapter(bookId, 1))
        assertEquals(1, bookDao.getChaptersFlow(bookId).first().size)
    }

    @Test
    fun getChapterById_existing_returnsChapter() = runTest {
        val bookId = bookDao.insertBook(makeBook())
        val chapterId = bookDao.insertChapter(makeChapter(bookId, 1))
        val result = bookDao.getChapterById(chapterId)
        assertNotNull(result)
        assertEquals(1, result!!.chapterNumber)
    }

    @Test
    fun getChapterById_nonExisting_returnsNull() = runTest {
        assertNull(bookDao.getChapterById(999L))
    }

    @Test
    fun getChapterByNumber_existing_returnsCorrectChapter() = runTest {
        val bookId = bookDao.insertBook(makeBook())
        bookDao.insertChapter(makeChapter(bookId, 2, "Chapter Two"))
        val result = bookDao.getChapterByNumber(bookId, 2)
        assertNotNull(result)
        assertEquals("Chapter Two", result!!.title)
    }

    @Test
    fun getChapterByNumber_wrongBook_returnsNull() = runTest {
        val bookId1 = bookDao.insertBook(makeBook("Book 1"))
        val bookId2 = bookDao.insertBook(makeBook("Book 2"))
        bookDao.insertChapter(makeChapter(bookId1, 1))
        assertNull(bookDao.getChapterByNumber(bookId2, 1))
    }

    @Test
    fun insertChapter_returnsPositiveId() = runTest {
        val bookId = bookDao.insertBook(makeBook())
        assertTrue(bookDao.insertChapter(makeChapter(bookId)) > 0)
    }

    @Test
    fun updateChapter_changesContent() = runTest {
        val bookId = bookDao.insertBook(makeBook())
        val chapterId = bookDao.insertChapter(makeChapter(bookId))
        bookDao.updateChapter(bookDao.getChapterById(chapterId)!!.copy(content = "New content"))
        assertEquals("New content", bookDao.getChapterById(chapterId)!!.content)
    }

    @Test
    fun deleteChapter_removesChapter() = runTest {
        val bookId = bookDao.insertBook(makeBook())
        val chapterId = bookDao.insertChapter(makeChapter(bookId))
        bookDao.deleteChapter(bookDao.getChapterById(chapterId)!!)
        assertNull(bookDao.getChapterById(chapterId))
    }

    @Test
    fun getChapterCount_noChapters_returnsZero() = runTest {
        val bookId = bookDao.insertBook(makeBook())
        assertEquals(0, bookDao.getChapterCount(bookId))
    }

    @Test
    fun getChapterCount_afterInserting3_returns3() = runTest {
        val bookId = bookDao.insertBook(makeBook())
        repeat(3) { i -> bookDao.insertChapter(makeChapter(bookId, i + 1)) }
        assertEquals(3, bookDao.getChapterCount(bookId))
    }
}
