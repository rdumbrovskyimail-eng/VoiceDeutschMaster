// Путь: src/test/java/com/voicedeutsch/master/domain/model/achievement/AchievementModelsTest.kt
package com.voicedeutsch.master.domain.model.achievement

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ═══════════════════════════════════════════════════════════════════════════
// AchievementCondition
// ═══════════════════════════════════════════════════════════════════════════

class AchievementConditionTest {

    private fun createCondition(
        type: String = "word_count",
        threshold: Int = 100,
        extra: String = ""
    ) = AchievementCondition(type = type, threshold = threshold, extra = extra)

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultExtra_isEmptyString() {
        val condition = AchievementCondition(type = "streak_days", threshold = 7)
        assertEquals("", condition.extra)
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val condition = createCondition(
            type = "total_hours",
            threshold = 50,
            extra = "{\"category\":\"vocabulary\"}"
        )
        assertEquals("total_hours", condition.type)
        assertEquals(50, condition.threshold)
        assertEquals("{\"category\":\"vocabulary\"}", condition.extra)
    }

    @Test
    fun constructor_thresholdZero_storedCorrectly() {
        val condition = createCondition(threshold = 0)
        assertEquals(0, condition.threshold)
    }

    @Test
    fun constructor_largeThreshold_storedCorrectly() {
        val condition = createCondition(threshold = 10_000)
        assertEquals(10_000, condition.threshold)
    }

    @Test
    fun constructor_emptyType_storedCorrectly() {
        val condition = createCondition(type = "")
        assertEquals("", condition.type)
    }

    @Test
    fun constructor_emptyExtra_storedCorrectly() {
        val condition = createCondition(extra = "")
        assertEquals("", condition.extra)
    }

    @Test
    fun constructor_nonEmptyExtra_storedCorrectly() {
        val condition = createCondition(extra = "level=B1")
        assertEquals("level=B1", condition.extra)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_changeThreshold_onlyThresholdChanges() {
        val original = createCondition(threshold = 10)
        val modified = original.copy(threshold = 50)
        assertEquals(50, modified.threshold)
        assertEquals(original.type, modified.type)
        assertEquals(original.extra, modified.extra)
    }

    @Test
    fun copy_changeType_typeUpdated() {
        val original = createCondition(type = "word_count")
        val modified = original.copy(type = "streak_days")
        assertEquals("streak_days", modified.type)
        assertEquals(original.threshold, modified.threshold)
    }

    @Test
    fun copy_setExtra_extraUpdated() {
        val original = createCondition(extra = "")
        val modified = original.copy(extra = "param=value")
        assertEquals("param=value", modified.extra)
        assertEquals("", original.extra)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createCondition(), createCondition())
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(createCondition().hashCode(), createCondition().hashCode())
    }

    @Test
    fun equals_differentType_notEqual() {
        assertNotEquals(createCondition(type = "word_count"), createCondition(type = "streak_days"))
    }

    @Test
    fun equals_differentThreshold_notEqual() {
        assertNotEquals(createCondition(threshold = 10), createCondition(threshold = 100))
    }

    @Test
    fun equals_differentExtra_notEqual() {
        assertNotEquals(createCondition(extra = "a=1"), createCondition(extra = "a=2"))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Achievement
// ═══════════════════════════════════════════════════════════════════════════

class AchievementTest {

    private fun createCondition() = AchievementCondition(type = "word_count", threshold = 100)

    private fun createAchievement(
        id: String = "ach_1",
        nameRu: String = "Первые слова",
        nameDe: String = "Erste Wörter",
        descriptionRu: String = "Выучи 100 слов",
        icon: String = "star",
        condition: AchievementCondition = createCondition(),
        category: AchievementCategory = AchievementCategory.VOCABULARY
    ) = Achievement(
        id = id,
        nameRu = nameRu,
        nameDe = nameDe,
        descriptionRu = descriptionRu,
        icon = icon,
        condition = condition,
        category = category
    )

    // ── Construction ──────────────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val condition = createCondition()
        val achievement = createAchievement(
            id = "ach_42",
            nameRu = "Марафонец",
            nameDe = "Marathonläufer",
            descriptionRu = "Занимайся 30 дней подряд",
            icon = "trophy",
            condition = condition,
            category = AchievementCategory.STREAK
        )
        assertEquals("ach_42", achievement.id)
        assertEquals("Марафонец", achievement.nameRu)
        assertEquals("Marathonläufer", achievement.nameDe)
        assertEquals("Занимайся 30 дней подряд", achievement.descriptionRu)
        assertEquals("trophy", achievement.icon)
        assertEquals(condition, achievement.condition)
        assertEquals(AchievementCategory.STREAK, achievement.category)
    }

    @Test
    fun constructor_allCategories_storedCorrectly() {
        AchievementCategory.entries.forEach { category ->
            val achievement = createAchievement(category = category)
            assertEquals(category, achievement.category)
        }
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_changeCategory_onlyCategoryChanges() {
        val original = createAchievement(category = AchievementCategory.VOCABULARY)
        val modified = original.copy(category = AchievementCategory.GRAMMAR)
        assertEquals(AchievementCategory.GRAMMAR, modified.category)
        assertEquals(original.id, modified.id)
        assertEquals(original.nameRu, modified.nameRu)
    }

    @Test
    fun copy_updateCondition_conditionUpdated() {
        val original = createAchievement(condition = createCondition())
        val newCondition = AchievementCondition(type = "streak_days", threshold = 30)
        val modified = original.copy(condition = newCondition)
        assertEquals(newCondition, modified.condition)
        assertEquals(createCondition(), original.condition)
    }

    @Test
    fun copy_changeIcon_iconUpdated() {
        val original = createAchievement(icon = "star")
        val modified = original.copy(icon = "diamond")
        assertEquals("diamond", modified.icon)
        assertEquals("star", original.icon)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createAchievement(), createAchievement())
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(createAchievement().hashCode(), createAchievement().hashCode())
    }

    @Test
    fun equals_differentId_notEqual() {
        assertNotEquals(createAchievement(id = "ach_1"), createAchievement(id = "ach_2"))
    }

    @Test
    fun equals_differentCategory_notEqual() {
        assertNotEquals(
            createAchievement(category = AchievementCategory.STREAK),
            createAchievement(category = AchievementCategory.TIME)
        )
    }

    @Test
    fun equals_differentNameRu_notEqual() {
        assertNotEquals(
            createAchievement(nameRu = "Первые слова"),
            createAchievement(nameRu = "Марафонец")
        )
    }

    @Test
    fun equals_differentConditionThreshold_notEqual() {
        val c1 = AchievementCondition(type = "word_count", threshold = 100)
        val c2 = AchievementCondition(type = "word_count", threshold = 500)
        assertNotEquals(createAchievement(condition = c1), createAchievement(condition = c2))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// UserAchievement
// ═══════════════════════════════════════════════════════════════════════════

class UserAchievementTest {

    private fun createAchievement() = Achievement(
        id = "ach_1",
        nameRu = "Первые слова",
        nameDe = "Erste Wörter",
        descriptionRu = "Выучи 100 слов",
        icon = "star",
        condition = AchievementCondition(type = "word_count", threshold = 100),
        category = AchievementCategory.VOCABULARY
    )

    private fun createUserAchievement(
        id: String = "ua_1",
        userId: String = "user_1",
        achievementId: String = "ach_1",
        earnedAt: Long = 1_000_000L,
        announced: Boolean = false,
        achievement: Achievement? = null
    ) = UserAchievement(
        id = id,
        userId = userId,
        achievementId = achievementId,
        earnedAt = earnedAt,
        announced = announced,
        achievement = achievement
    )

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultAchievement_isNull() {
        val ua = UserAchievement(
            id = "ua_1",
            userId = "u1",
            achievementId = "ach_1",
            earnedAt = 1_000L,
            announced = false
        )
        assertNull(ua.achievement)
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val ach = createAchievement()
        val ua = createUserAchievement(
            id = "ua_99",
            userId = "u42",
            achievementId = "ach_7",
            earnedAt = 9_999_999L,
            announced = true,
            achievement = ach
        )
        assertEquals("ua_99", ua.id)
        assertEquals("u42", ua.userId)
        assertEquals("ach_7", ua.achievementId)
        assertEquals(9_999_999L, ua.earnedAt)
        assertTrue(ua.announced)
        assertEquals(ach, ua.achievement)
    }

    @Test
    fun constructor_nullAchievement_isNull() {
        val ua = createUserAchievement(achievement = null)
        assertNull(ua.achievement)
    }

    @Test
    fun constructor_withAchievement_achievementStored() {
        val ach = createAchievement()
        val ua = createUserAchievement(achievement = ach)
        assertNotNull(ua.achievement)
        assertEquals(ach, ua.achievement)
    }

    @Test
    fun constructor_announcedTrue_storedCorrectly() {
        val ua = createUserAchievement(announced = true)
        assertTrue(ua.announced)
    }

    @Test
    fun constructor_announcedFalse_storedCorrectly() {
        val ua = createUserAchievement(announced = false)
        assertFalse(ua.announced)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_markAsAnnounced_onlyAnnouncedChanges() {
        val original = createUserAchievement(announced = false)
        val modified = original.copy(announced = true)
        assertTrue(modified.announced)
        assertFalse(original.announced)
        assertEquals(original.id, modified.id)
        assertEquals(original.userId, modified.userId)
    }

    @Test
    fun copy_populateAchievement_achievementUpdated() {
        val original = createUserAchievement(achievement = null)
        val ach = createAchievement()
        val modified = original.copy(achievement = ach)
        assertEquals(ach, modified.achievement)
        assertNull(original.achievement)
    }

    @Test
    fun copy_clearAchievement_achievementBecomesNull() {
        val original = createUserAchievement(achievement = createAchievement())
        val modified = original.copy(achievement = null)
        assertNull(modified.achievement)
        assertNotNull(original.achievement)
    }

    @Test
    fun copy_changeEarnedAt_timestampUpdated() {
        val original = createUserAchievement(earnedAt = 1_000L)
        val modified = original.copy(earnedAt = 2_000L)
        assertEquals(2_000L, modified.earnedAt)
        assertEquals(1_000L, original.earnedAt)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createUserAchievement(), createUserAchievement())
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(createUserAchievement().hashCode(), createUserAchievement().hashCode())
    }

    @Test
    fun equals_differentId_notEqual() {
        assertNotEquals(createUserAchievement(id = "ua_1"), createUserAchievement(id = "ua_2"))
    }

    @Test
    fun equals_differentUserId_notEqual() {
        assertNotEquals(createUserAchievement(userId = "u1"), createUserAchievement(userId = "u2"))
    }

    @Test
    fun equals_differentAnnounced_notEqual() {
        assertNotEquals(
            createUserAchievement(announced = false),
            createUserAchievement(announced = true)
        )
    }

    @Test
    fun equals_nullVsNonNullAchievement_notEqual() {
        assertNotEquals(
            createUserAchievement(achievement = null),
            createUserAchievement(achievement = createAchievement())
        )
    }

    @Test
    fun equals_differentEarnedAt_notEqual() {
        assertNotEquals(
            createUserAchievement(earnedAt = 1_000L),
            createUserAchievement(earnedAt = 2_000L)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// AchievementCategory
// ═══════════════════════════════════════════════════════════════════════════

class AchievementCategoryTest {

    @Test
    fun entries_size_isNine() {
        assertEquals(9, AchievementCategory.entries.size)
    }

    @Test
    fun entries_containsAllExpectedValues() {
        val expected = setOf(
            AchievementCategory.VOCABULARY,
            AchievementCategory.GRAMMAR,
            AchievementCategory.PRONUNCIATION,
            AchievementCategory.STREAK,
            AchievementCategory.SESSION,
            AchievementCategory.BOOK,
            AchievementCategory.TIME,
            AchievementCategory.CEFR,
            AchievementCategory.SPECIAL
        )
        assertEquals(expected, AchievementCategory.entries.toSet())
    }

    @Test
    fun ordinal_vocabulary_isZero() {
        assertEquals(0, AchievementCategory.VOCABULARY.ordinal)
    }

    @Test
    fun ordinal_grammar_isOne() {
        assertEquals(1, AchievementCategory.GRAMMAR.ordinal)
    }

    @Test
    fun ordinal_pronunciation_isTwo() {
        assertEquals(2, AchievementCategory.PRONUNCIATION.ordinal)
    }

    @Test
    fun ordinal_streak_isThree() {
        assertEquals(3, AchievementCategory.STREAK.ordinal)
    }

    @Test
    fun ordinal_session_isFour() {
        assertEquals(4, AchievementCategory.SESSION.ordinal)
    }

    @Test
    fun ordinal_book_isFive() {
        assertEquals(5, AchievementCategory.BOOK.ordinal)
    }

    @Test
    fun ordinal_time_isSix() {
        assertEquals(6, AchievementCategory.TIME.ordinal)
    }

    @Test
    fun ordinal_cefr_isSeven() {
        assertEquals(7, AchievementCategory.CEFR.ordinal)
    }

    @Test
    fun ordinal_special_isEight() {
        assertEquals(8, AchievementCategory.SPECIAL.ordinal)
    }

    @Test
    fun valueOf_vocabulary_returnsVocabulary() {
        assertEquals(AchievementCategory.VOCABULARY, AchievementCategory.valueOf("VOCABULARY"))
    }

    @Test
    fun valueOf_cefr_returnsCefr() {
        assertEquals(AchievementCategory.CEFR, AchievementCategory.valueOf("CEFR"))
    }

    @Test
    fun valueOf_special_returnsSpecial() {
        assertEquals(AchievementCategory.SPECIAL, AchievementCategory.valueOf("SPECIAL"))
    }

    @Test
    fun valueOf_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            AchievementCategory.valueOf("UNKNOWN")
        }
    }

    @Test
    fun valueOf_lowercaseValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            AchievementCategory.valueOf("streak")
        }
    }

    @Test
    fun allEntries_names_areUnique() {
        val names = AchievementCategory.entries.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }
}
