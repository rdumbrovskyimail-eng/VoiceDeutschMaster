// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/BookChapterEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BookChapterEntityTest {

    private fun createEntity(
        id: Long = 0L,
        bookId: Long = 1L,
        chapterNumber: Int = 1,
        title: String = "Kapitel 1",
        content: String = "Willkommen!",
        createdAt: Long = 2_000_000L,
    ) = BookChapterEntity(
        id = id, bookId = bookId, chapterNumber = chapterNumber,
        title = title, content = content, createdAt = createdAt,
    )

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals(0L, entity.id)
        assertEquals(1L, entity.bookId)
        assertEquals(1, entity.chapterNumber)
        assertEquals("Kapitel 1", entity.title)
        assertEquals("Willkommen!", entity.content)
        assertEquals(2_000_000L, entity.createdAt)
    }

    @Test fun creation_differentBookId_bookIdIsCorrect() = assertEquals(42L, createEntity(bookId = 42L).bookId)
    @Test fun creation_chapterNumberZero_isAllowed() = assertEquals(0, createEntity(chapterNumber = 0).chapterNumber)
    @Test fun creation_largeChapterNumber_isStored() = assertEquals(999, createEntity(chapterNumber = 999).chapterNumber)
    @Test fun defaultId_isZero() = assertEquals(0L, createEntity().id)

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = BookChapterEntity(bookId = 1L, chapterNumber = 1, title = "Test", content = "Content")
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentBookId_returnsFalse() = assertNotEquals(createEntity(bookId = 1L), createEntity(bookId = 2L))
    @Test fun equals_differentChapterNumber_returnsFalse() = assertNotEquals(createEntity(chapterNumber = 1), createEntity(chapterNumber = 2))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_withNewTitle_onlyTitleChanges() {
        val original = createEntity(title = "Kapitel 1")
        val copied = original.copy(title = "Kapitel 2")
        assertEquals("Kapitel 2", copied.title)
        assertEquals(original.bookId, copied.bookId)
        assertEquals(original.chapterNumber, copied.chapterNumber)
    }

    @Test
    fun copy_withNewContent_onlyContentChanges() {
        val original = createEntity(content = "Willkommen!")
        val copied = original.copy(content = "Auf Wiedersehen!")
        assertEquals("Auf Wiedersehen!", copied.content)
        assertEquals(original.title, copied.title)
    }

    @Test
    fun copy_withNewId_onlyIdChanges() {
        val original = createEntity(id = 0L)
        val copied = original.copy(id = 10L)
        assertEquals(10L, copied.id)
        assertEquals(original.bookId, copied.bookId)
    }

    @Test fun content_emptyString_isAllowed() = assertEquals("", createEntity(content = "").content)

    @Test
    fun content_longText_isStoredCorrectly() {
        val longText = "A".repeat(10_000)
        assertEquals(longText.length, createEntity(content = longText).content.length)
    }
}
