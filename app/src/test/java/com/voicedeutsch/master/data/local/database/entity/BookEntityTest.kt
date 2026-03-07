// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/BookEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BookEntityTest {

    private fun createEntity(
        id: Long = 0L,
        title: String = "Menschen A1",
        description: String? = "Учебник немецкого языка",
        createdAt: Long = 3_000_000L,
    ) = BookEntity(id = id, title = title, description = description, createdAt = createdAt)

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals(0L, entity.id)
        assertEquals("Menschen A1", entity.title)
        assertEquals("Учебник немецкого языка", entity.description)
        assertEquals(3_000_000L, entity.createdAt)
    }

    @Test fun creation_withNullDescription_descriptionIsNull() = assertNull(createEntity(description = null).description)
    @Test fun creation_withNonZeroId_idIsCorrect() = assertEquals(5L, createEntity(id = 5L).id)
    @Test fun defaultId_isZero() = assertEquals(0L, BookEntity(title = "Test").id)
    @Test fun defaultDescription_isNull() = assertNull(BookEntity(title = "Test").description)

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = BookEntity(title = "Test")
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentTitle_returnsFalse() = assertNotEquals(createEntity(title = "Menschen A1"), createEntity(title = "Menschen A2"))
    @Test fun equals_nullVsNonNullDescription_returnsFalse() = assertNotEquals(createEntity(description = null), createEntity(description = "Some desc"))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_withNewTitle_onlyTitleChanges() {
        val original = createEntity(title = "Menschen A1")
        val copied = original.copy(title = "Menschen A2")
        assertEquals("Menschen A2", copied.title)
        assertEquals(original.id, copied.id)
        assertEquals(original.description, copied.description)
    }

    @Test
    fun copy_clearDescription_descriptionBecomesNull() {
        val original = createEntity(description = "Some description")
        val copied = original.copy(description = null)
        assertNull(copied.description)
        assertEquals(original.title, copied.title)
    }

    @Test
    fun copy_setDescription_descriptionUpdated() {
        val original = createEntity(description = null)
        val copied = original.copy(description = "New description")
        assertEquals("New description", copied.description)
    }

    @Test fun title_emptyString_isAllowed() = assertEquals("", createEntity(title = "").title)

    @Test
    fun title_specialChars_isStoredCorrectly() {
        val title = "Deutsch für Anfänger — Übungen"
        assertEquals(title, createEntity(title = title).title)
    }
}
