// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/PhraseEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PhraseEntityTest {

    private fun createEntity(
        id: String = "ph_001",
        german: String = "Wie geht es Ihnen?",
        russian: String = "Как у вас дела?",
        category: String = "greetings",
        difficultyLevel: String = "A1",
        bookChapter: Int? = null,
        bookLesson: Int? = null,
        context: String = "",
        createdAt: Long = 7_000_000L,
    ) = PhraseEntity(
        id = id, german = german, russian = russian, category = category,
        difficultyLevel = difficultyLevel, bookChapter = bookChapter,
        bookLesson = bookLesson, context = context, createdAt = createdAt,
    )

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals("ph_001", entity.id)
        assertEquals("Wie geht es Ihnen?", entity.german)
        assertEquals("Как у вас дела?", entity.russian)
        assertEquals("greetings", entity.category)
        assertEquals("A1", entity.difficultyLevel)
        assertNull(entity.bookChapter)
        assertNull(entity.bookLesson)
        assertEquals("", entity.context)
        assertEquals(7_000_000L, entity.createdAt)
    }

    @Test
    fun creation_withBookRef_bookRefIsStored() {
        val entity = createEntity(bookChapter = 2, bookLesson = 3)
        assertEquals(2, entity.bookChapter)
        assertEquals(3, entity.bookLesson)
    }

    @Test fun creation_withContext_contextIsStored() = assertEquals("formal greeting", createEntity(context = "formal greeting").context)

    private fun minimal() = PhraseEntity(id = "ph_002", german = "Hallo", russian = "Привет", category = "greetings")

    @Test fun defaultDifficultyLevel_isA1() = assertEquals("A1", minimal().difficultyLevel)
    @Test fun defaultBookChapter_isNull() = assertNull(minimal().bookChapter)
    @Test fun defaultContext_isEmptyString() = assertEquals("", minimal().context)

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = PhraseEntity(id = "ph_003", german = "Tschüss", russian = "Пока", category = "farewell")
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentGerman_returnsFalse() = assertNotEquals(createEntity(german = "Hallo"), createEntity(german = "Tschüss"))
    @Test fun equals_differentCategory_returnsFalse() = assertNotEquals(createEntity(category = "greetings"), createEntity(category = "farewell"))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_withNewDifficultyLevel_onlyDifficultyChanges() {
        val original = createEntity(difficultyLevel = "A1")
        val copied = original.copy(difficultyLevel = "B2")
        assertEquals("B2", copied.difficultyLevel)
        assertEquals(original.german, copied.german)
        assertEquals(original.category, copied.category)
    }

    @Test
    fun copy_setBookChapter_bookChapterUpdated() {
        val copied = createEntity(bookChapter = null).copy(bookChapter = 4)
        assertEquals(4, copied.bookChapter)
    }

    @Test
    fun copy_withNewContext_contextUpdated() {
        val copied = createEntity(context = "").copy(context = "используется в официальной речи")
        assertEquals("используется в официальной речи", copied.context)
    }

    @Test fun difficultyLevel_b1_isAllowed() = assertEquals("B1", createEntity(difficultyLevel = "B1").difficultyLevel)
    @Test fun difficultyLevel_c2_isAllowed() = assertEquals("C2", createEntity(difficultyLevel = "C2").difficultyLevel)
}
