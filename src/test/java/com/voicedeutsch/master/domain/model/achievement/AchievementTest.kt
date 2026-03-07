// Path: src/test/java/com/voicedeutsch/master/domain/model/achievement/AchievementTest.kt
package com.voicedeutsch.master.domain.model.achievement

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ════════════════════════════════════════════════════════════════════════════
// AchievementCategory
// ════════════════════════════════════════════════════════════════════════════

class AchievementCategoryTest {

    @Test
    fun entries_size_equals9() {
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
            AchievementCategory.SPECIAL,
        )
        assertEquals(expected, AchievementCategory.entries.toSet())
    }

    @Test
    fun valueOf_vocabulary_returnsCorrectEnum() {
        assertEquals(AchievementCategory.VOCABULARY, AchievementCategory.valueOf("VOCABULARY"))
    }

    @Test
    fun valueOf_special_returnsCorrectEnum() {
        assertEquals(AchievementCategory.SPECIAL, AchievementCategory.valueOf("SPECIAL"))
    }

    @Test
    fun valueOf_unknownName_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            AchievementCategory.valueOf("UNKNOWN")
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// AchievementCondition
// ════════════════════════════════════════════════════════════════════════════

class AchievementConditionTest {

    @Test
    fun creation_withRequiredFields_setsValues() {
        val condition = AchievementCondition(type = "word_count", threshold = 100)
        assertEquals("word_count", condition.type)
        assertEquals(100, condition.threshold)
    }

    @Test
    fun creation_defaultExtra_isEmptyString() {
        val condition = AchievementCondition(type = "streak_days", threshold = 7)
        assertEquals("", condition.extra)
    }

    @Test
    fun creation_withCustomExtra_setsValue() {
        val condition = AchievementCondition(
            type = "cefr_level",
            threshold = 3,
            extra = """{"level":"B1"}""",
        )
        assertEquals("""{"level":"B1"}""", condition.extra)
    }

    @Test
    fun copy_changesThreshold_otherFieldsUnchanged() {
        val original = AchievementCondition(type = "word_count", threshold = 50)
        val copy = original.copy(threshold = 200)
        assertEquals(200, copy.threshold)
        assertEquals("word_count", copy.type)
        assertEquals("", copy.extra)
    }

    @Test
    fun copy_changesType_otherFieldsUnchanged() {
        val original = AchievementCondition(type = "word_count", threshold = 50, extra = "x")
        val copy = original.copy(type = "streak_days")
        assertEquals("streak_days", copy.type)
        assertEquals(50, copy.threshold)
        assertEquals("x", copy.extra)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = AchievementCondition(type = "word_count", threshold = 100, extra = "x")
        val b = AchievementCondition(type = "word_count", threshold = 100, extra = "x")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentThreshold_areNotEqual() {
        val a = AchievementCondition(type = "word_count", threshold = 100)
        val b = AchievementCondition(type = "word_count", threshold = 200)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentType_areNotEqual() {
        val a = AchievementCondition(type = "word_count", threshold = 100)
        val b = AchievementCondition(type = "streak_days", threshold = 100)
        assertNotEquals(a, b)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Achievement
// ════════════════════════════════════════════════════════════════════════════

class AchievementTest {

    private fun makeCondition() = AchievementCondition(type = "word_count", threshold = 100)

    private fun makeAchievement(
        id: String = "ach_1",
        nameRu: String = "Словарник",
        nameDe: String = "Wortschatz",
        descriptionRu: String = "Изучи 100 слов",
        icon: String = "📚",
        condition: AchievementCondition = makeCondition(),
        category: AchievementCategory = AchievementCategory.VOCABULARY,
    ) = Achievement(
        id = id,
        nameRu = nameRu,
        nameDe = nameDe,
        descriptionRu = descriptionRu,
        icon = icon,
        condition = condition,
        category = category,
    )

    @Test
    fun creation_withAllFields_setsCorrectly() {
        val ach = makeAchievement()
        assertEquals("ach_1", ach.id)
        assertEquals("Словарник", ach.nameRu)
        assertEquals("Wortschatz", ach.nameDe)
        assertEquals("Изучи 100 слов", ach.descriptionRu)
        assertEquals("📚", ach.icon)
        assertEquals(AchievementCategory.VOCABULARY, ach.category)
    }

    @Test
    fun creation_conditionIsStored() {
        val cond = makeCondition()
        val ach = makeAchievement(condition = cond)
        assertEquals(cond, ach.condition)
    }

    @Test
    fun copy_changesCategory_restUnchanged() {
        val original = makeAchievement()
        val copy = original.copy(category = AchievementCategory.SPECIAL)
        assertEquals(AchievementCategory.SPECIAL, copy.category)
        assertEquals("ach_1", copy.id)
        assertEquals("Словарник", copy.nameRu)
    }

    @Test
    fun copy_changesId_restUnchanged() {
        val original = makeAchievement()
        val copy = original.copy(id = "ach_99")
        assertEquals("ach_99", copy.id)
        assertEquals(AchievementCategory.VOCABULARY, copy.category)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeAchievement()
        val b = makeAchievement()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentId_areNotEqual() {
        val a = makeAchievement(id = "ach_1")
        val b = makeAchievement(id = "ach_2")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentCategory_areNotEqual() {
        val a = makeAchievement(category = AchievementCategory.VOCABULARY)
        val b = makeAchievement(category = AchievementCategory.GRAMMAR)
        assertNotEquals(a, b)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// UserAchievement
// ════════════════════════════════════════════════════════════════════════════

class UserAchievementTest {

    private fun makeUserAchievement(
        id: String = "ua_1",
        userId: String = "user_1",
        achievementId: String = "ach_1",
        earnedAt: Long = 1_000_000L,
        announced: Boolean = false,
        achievement: Achievement? = null,
    ) = UserAchievement(
        id = id,
        userId = userId,
        achievementId = achievementId,
        earnedAt = earnedAt,
        announced = announced,
        achievement = achievement,
    )

    @Test
    fun creation_withRequiredFields_setsValues() {
        val ua = makeUserAchievement()
        assertEquals("ua_1", ua.id)
        assertEquals("user_1", ua.userId)
        assertEquals("ach_1", ua.achievementId)
        assertEquals(1_000_000L, ua.earnedAt)
        assertFalse(ua.announced)
    }

    @Test
    fun creation_defaultAchievement_isNull() {
        val ua = makeUserAchievement()
        assertNull(ua.achievement)
    }

    @Test
    fun creation_withAchievement_setsValue() {
        val ach = Achievement(
            id = "ach_1", nameRu = "Test", nameDe = "Test", descriptionRu = "Test",
            icon = "🏆",
            condition = AchievementCondition("word_count", 100),
            category = AchievementCategory.VOCABULARY,
        )
        val ua = makeUserAchievement(achievement = ach)
        assertNotNull(ua.achievement)
        assertEquals("ach_1", ua.achievement!!.id)
    }

    @Test
    fun copy_changesAnnounced_restUnchanged() {
        val original = makeUserAchievement(announced = false)
        val copy = original.copy(announced = true)
        assertTrue(copy.announced)
        assertEquals("ua_1", copy.id)
        assertEquals("user_1", copy.userId)
    }

    @Test
    fun copy_changesEarnedAt() {
        val original = makeUserAchievement(earnedAt = 1000L)
        val copy = original.copy(earnedAt = 9999L)
        assertEquals(9999L, copy.earnedAt)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeUserAchievement()
        val b = makeUserAchievement()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentUserId_areNotEqual() {
        val a = makeUserAchievement(userId = "user_1")
        val b = makeUserAchievement(userId = "user_2")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentAnnounced_areNotEqual() {
        val a = makeUserAchievement(announced = false)
        val b = makeUserAchievement(announced = true)
        assertNotEquals(a, b)
    }
}
