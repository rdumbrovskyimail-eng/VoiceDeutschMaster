// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/AchievementEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AchievementEntityTest {

    private fun createEntity(
        id: String = "ach_001",
        nameRu: String = "Первые шаги",
        nameDe: String = "Erste Schritte",
        descriptionRu: String = "Выучи первые 10 слов",
        icon: String = "ic_star",
        conditionJson: String = "{\"words\":10}",
        category: String = "vocabulary",
        createdAt: Long = 1_000_000L,
    ) = AchievementEntity(
        id = id,
        nameRu = nameRu,
        nameDe = nameDe,
        descriptionRu = descriptionRu,
        icon = icon,
        conditionJson = conditionJson,
        category = category,
        createdAt = createdAt,
    )

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals("ach_001", entity.id)
        assertEquals("Первые шаги", entity.nameRu)
        assertEquals("Erste Schritte", entity.nameDe)
        assertEquals("Выучи первые 10 слов", entity.descriptionRu)
        assertEquals("ic_star", entity.icon)
        assertEquals("{\"words\":10}", entity.conditionJson)
        assertEquals("vocabulary", entity.category)
        assertEquals(1_000_000L, entity.createdAt)
    }

    @Test fun creation_categoryGrammar_categoryIsGrammar() = assertEquals("grammar", createEntity(category = "grammar").category)
    @Test fun creation_categoryPronunciation_categoryIsPronunciation() = assertEquals("pronunciation", createEntity(category = "pronunciation").category)
    @Test fun creation_categoryStreak_categoryIsStreak() = assertEquals("streak", createEntity(category = "streak").category)
    @Test fun creation_categorySession_categoryIsSession() = assertEquals("session", createEntity(category = "session").category)
    @Test fun creation_categoryBook_categoryIsBook() = assertEquals("book", createEntity(category = "book").category)

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = AchievementEntity(
            id = "ach_002", nameRu = "Тест", nameDe = "Test",
            descriptionRu = "Описание", icon = "ic_default",
            conditionJson = "{}", category = "vocabulary",
        )
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentId_returnsFalse() = assertNotEquals(createEntity(id = "ach_001"), createEntity(id = "ach_002"))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_withNewCategory_onlyCategoryChanges() {
        val original = createEntity(category = "vocabulary")
        val copied = original.copy(category = "grammar")
        assertEquals("grammar", copied.category)
        assertEquals(original.id, copied.id)
        assertEquals(original.nameRu, copied.nameRu)
        assertEquals(original.icon, copied.icon)
    }

    @Test
    fun copy_withNewIcon_onlyIconChanges() {
        val original = createEntity(icon = "ic_star")
        val copied = original.copy(icon = "ic_trophy")
        assertEquals("ic_trophy", copied.icon)
        assertEquals(original.category, copied.category)
    }

    @Test
    fun copy_withNewConditionJson_onlyConditionJsonChanges() {
        val original = createEntity(conditionJson = "{\"words\":10}")
        val copied = original.copy(conditionJson = "{\"words\":50}")
        assertEquals("{\"words\":50}", copied.conditionJson)
        assertEquals(original.id, copied.id)
    }

    @Test fun conditionJson_emptyObject_isStoredAsIs() = assertEquals("{}", createEntity(conditionJson = "{}").conditionJson)

    @Test
    fun conditionJson_complexJson_isStoredAsIs() {
        val json = """{"type":"streak","days":30,"bonus":true}"""
        assertEquals(json, createEntity(conditionJson = json).conditionJson)
    }
}
