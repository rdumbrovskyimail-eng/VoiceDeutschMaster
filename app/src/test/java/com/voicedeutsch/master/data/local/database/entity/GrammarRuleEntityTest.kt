// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/GrammarRuleEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GrammarRuleEntityTest {

    private fun createEntity(
        id: String = "gr_001",
        nameRu: String = "Артикль определённый",
        nameDe: String = "Bestimmter Artikel",
        category: String = "articles",
        descriptionRu: String = "Слова der, die, das",
        descriptionDe: String = "Der, die, das Artikel",
        difficultyLevel: String = "A1",
        examplesJson: String = "[]",
        exceptionsJson: String = "[]",
        relatedRulesJson: String = "[]",
        bookChapter: Int? = null,
        bookLesson: Int? = null,
        createdAt: Long = 5_000_000L,
    ) = GrammarRuleEntity(
        id = id, nameRu = nameRu, nameDe = nameDe, category = category,
        descriptionRu = descriptionRu, descriptionDe = descriptionDe,
        difficultyLevel = difficultyLevel, examplesJson = examplesJson,
        exceptionsJson = exceptionsJson, relatedRulesJson = relatedRulesJson,
        bookChapter = bookChapter, bookLesson = bookLesson, createdAt = createdAt,
    )

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals("gr_001", entity.id)
        assertEquals("Артикль определённый", entity.nameRu)
        assertEquals("Bestimmter Artikel", entity.nameDe)
        assertEquals("articles", entity.category)
        assertEquals("Слова der, die, das", entity.descriptionRu)
        assertEquals("Der, die, das Artikel", entity.descriptionDe)
        assertEquals("A1", entity.difficultyLevel)
        assertEquals("[]", entity.examplesJson)
        assertEquals("[]", entity.exceptionsJson)
        assertEquals("[]", entity.relatedRulesJson)
        assertNull(entity.bookChapter)
        assertNull(entity.bookLesson)
        assertEquals(5_000_000L, entity.createdAt)
    }

    @Test
    fun creation_withBookChapterAndLesson_valuesAreStored() {
        val entity = createEntity(bookChapter = 3, bookLesson = 2)
        assertEquals(3, entity.bookChapter)
        assertEquals(2, entity.bookLesson)
    }

    private fun minimal() = GrammarRuleEntity(
        id = "gr_002", nameRu = "Тест", nameDe = "Test", category = "test", descriptionRu = "Описание"
    )

    @Test fun defaultDescriptionDe_isEmptyString() = assertEquals("", minimal().descriptionDe)
    @Test fun defaultDifficultyLevel_isA1() = assertEquals("A1", minimal().difficultyLevel)
    @Test fun defaultExamplesJson_isEmptyArray() = assertEquals("[]", minimal().examplesJson)
    @Test fun defaultBookChapter_isNull() = assertNull(minimal().bookChapter)

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = GrammarRuleEntity(id = "gr_003", nameRu = "Тест", nameDe = "Test", category = "test", descriptionRu = "Описание")
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentCategory_returnsFalse() = assertNotEquals(createEntity(category = "articles"), createEntity(category = "verbs"))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_withNewDifficultyLevel_onlyDifficultyChanges() {
        val original = createEntity(difficultyLevel = "A1")
        val copied = original.copy(difficultyLevel = "B1")
        assertEquals("B1", copied.difficultyLevel)
        assertEquals(original.id, copied.id)
        assertEquals(original.category, copied.category)
    }

    @Test
    fun copy_withExamplesJson_jsonUpdated() {
        val newJson = """[{"de":"Der Mann","ru":"Мужчина"}]"""
        val copied = createEntity(examplesJson = "[]").copy(examplesJson = newJson)
        assertEquals(newJson, copied.examplesJson)
    }

    @Test
    fun copy_setBookChapter_chapterUpdated() {
        val copied = createEntity(bookChapter = null).copy(bookChapter = 5)
        assertEquals(5, copied.bookChapter)
    }

    @Test
    fun examplesJson_complexJson_isStoredCorrectly() {
        val json = """[{"de":"Ich bin ein Mann","ru":"Я мужчина"},{"de":"Sie ist eine Frau","ru":"Она женщина"}]"""
        assertEquals(json, createEntity(examplesJson = json).examplesJson)
    }

    @Test
    fun relatedRulesJson_withValues_isStoredCorrectly() {
        val json = """["gr_002","gr_003"]"""
        assertEquals(json, createEntity(relatedRulesJson = json).relatedRulesJson)
    }
}
