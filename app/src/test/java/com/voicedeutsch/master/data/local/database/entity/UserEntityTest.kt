// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/UserEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UserEntityTest {

    private fun createEntity(
        id: String = "user_001",
        name: String = "Алексей",
        nativeLanguage: String = "ru",
        targetLanguage: String = "de",
        cefrLevel: String = "A1",
        cefrSubLevel: Int = 1,
        totalSessions: Int = 5,
        totalMinutes: Int = 120,
        totalWordsLearned: Int = 50,
        totalRulesLearned: Int = 10,
        streakDays: Int = 7,
        lastSessionDate: Long? = 14_000_000L,
        age: Int? = 25,
        hobbies: String? = "музыка, спорт",
        learningGoals: String? = "переезд в Германию",
        createdAt: Long = 14_000_000L,
        updatedAt: Long = 14_000_000L,
        preferencesJson: String = "{}",
        voiceSettingsJson: String = "{}",
    ) = UserEntity(
        id = id, name = name, nativeLanguage = nativeLanguage, targetLanguage = targetLanguage,
        cefrLevel = cefrLevel, cefrSubLevel = cefrSubLevel, totalSessions = totalSessions,
        totalMinutes = totalMinutes, totalWordsLearned = totalWordsLearned,
        totalRulesLearned = totalRulesLearned, streakDays = streakDays,
        lastSessionDate = lastSessionDate, age = age, hobbies = hobbies,
        learningGoals = learningGoals, createdAt = createdAt, updatedAt = updatedAt,
        preferencesJson = preferencesJson, voiceSettingsJson = voiceSettingsJson,
    )

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals("user_001", entity.id)
        assertEquals("Алексей", entity.name)
        assertEquals("ru", entity.nativeLanguage)
        assertEquals("de", entity.targetLanguage)
        assertEquals("A1", entity.cefrLevel)
        assertEquals(1, entity.cefrSubLevel)
        assertEquals(5, entity.totalSessions)
        assertEquals(120, entity.totalMinutes)
        assertEquals(50, entity.totalWordsLearned)
        assertEquals(10, entity.totalRulesLearned)
        assertEquals(7, entity.streakDays)
        assertEquals(14_000_000L, entity.lastSessionDate)
        assertEquals(25, entity.age)
        assertEquals("музыка, спорт", entity.hobbies)
        assertEquals("переезд в Германию", entity.learningGoals)
        assertEquals("{}", entity.preferencesJson)
        assertEquals("{}", entity.voiceSettingsJson)
    }

    @Test
    fun creation_withNullOptionalFields_nullsAreNull() {
        val entity = createEntity(lastSessionDate = null, age = null, hobbies = null, learningGoals = null)
        assertNull(entity.lastSessionDate)
        assertNull(entity.age)
        assertNull(entity.hobbies)
        assertNull(entity.learningGoals)
    }

    private fun minimal() = UserEntity(id = "u2", name = "Test")

    @Test fun defaultNativeLanguage_isRu() = assertEquals("ru", minimal().nativeLanguage)
    @Test fun defaultTargetLanguage_isDe() = assertEquals("de", minimal().targetLanguage)
    @Test fun defaultCefrLevel_isA1() = assertEquals("A1", minimal().cefrLevel)
    @Test fun defaultCefrSubLevel_isOne() = assertEquals(1, minimal().cefrSubLevel)
    @Test fun defaultTotalSessions_isZero() = assertEquals(0, minimal().totalSessions)
    @Test fun defaultTotalMinutes_isZero() = assertEquals(0, minimal().totalMinutes)
    @Test fun defaultTotalWordsLearned_isZero() = assertEquals(0, minimal().totalWordsLearned)
    @Test fun defaultTotalRulesLearned_isZero() = assertEquals(0, minimal().totalRulesLearned)
    @Test fun defaultStreakDays_isZero() = assertEquals(0, minimal().streakDays)
    @Test fun defaultLastSessionDate_isNull() = assertNull(minimal().lastSessionDate)
    @Test fun defaultPreferencesJson_isEmptyObject() = assertEquals("{}", minimal().preferencesJson)
    @Test fun defaultVoiceSettingsJson_isEmptyObject() = assertEquals("{}", minimal().voiceSettingsJson)

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = UserEntity(id = "u3", name = "Test")
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test
    fun defaultUpdatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = UserEntity(id = "u3", name = "Test")
        val after = System.currentTimeMillis()
        assertTrue(entity.updatedAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentId_returnsFalse() = assertNotEquals(createEntity(id = "user_001"), createEntity(id = "user_002"))
    @Test fun equals_differentCefrLevel_returnsFalse() = assertNotEquals(createEntity(cefrLevel = "A1"), createEntity(cefrLevel = "B2"))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_withNewCefrLevel_onlyCefrLevelChanges() {
        val original = createEntity(cefrLevel = "A1")
        val copied = original.copy(cefrLevel = "A2")
        assertEquals("A2", copied.cefrLevel)
        assertEquals(original.id, copied.id)
        assertEquals(original.name, copied.name)
    }

    @Test
    fun copy_incrementStreakDays_valueUpdated() {
        val copied = createEntity(streakDays = 10).copy(streakDays = 11)
        assertEquals(11, copied.streakDays)
    }

    @Test
    fun copy_withNewPreferencesJson_jsonUpdated() {
        val newJson = """{"theme":"dark","notifications":true}"""
        val copied = createEntity(preferencesJson = "{}").copy(preferencesJson = newJson)
        assertEquals(newJson, copied.preferencesJson)
    }

    @Test
    fun copy_clearHobbies_becomesNull() {
        val copied = createEntity(hobbies = "музыка").copy(hobbies = null)
        assertNull(copied.hobbies)
    }

    @Test fun cefrLevel_b2_isAllowed() = assertEquals("B2", createEntity(cefrLevel = "B2").cefrLevel)
    @Test fun cefrLevel_c1_isAllowed() = assertEquals("C1", createEntity(cefrLevel = "C1").cefrLevel)
}
