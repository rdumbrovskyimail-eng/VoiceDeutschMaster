// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/UserAchievementEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UserAchievementEntityTest {

    private fun createEntity(
        id: String = "ua_001",
        userId: String = "user_001",
        achievementId: String = "ach_001",
        earnedAt: Long = 13_000_000L,
        announced: Boolean = false,
        createdAt: Long = 13_000_100L,
    ) = UserAchievementEntity(
        id = id, userId = userId, achievementId = achievementId,
        earnedAt = earnedAt, announced = announced, createdAt = createdAt,
    )

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals("ua_001", entity.id)
        assertEquals("user_001", entity.userId)
        assertEquals("ach_001", entity.achievementId)
        assertEquals(13_000_000L, entity.earnedAt)
        assertFalse(entity.announced)
        assertEquals(13_000_100L, entity.createdAt)
    }

    @Test fun creation_announcedTrue_isStored() = assertTrue(createEntity(announced = true).announced)
    @Test fun creation_differentAchievement_achievementIdIsCorrect() = assertEquals("ach_streak_30", createEntity(achievementId = "ach_streak_30").achievementId)

    @Test
    fun defaultAnnounced_isFalse() {
        assertFalse(UserAchievementEntity(id = "ua_002", userId = "u1", achievementId = "ach_001", earnedAt = 100L).announced)
    }

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = UserAchievementEntity(id = "ua_003", userId = "u1", achievementId = "ach_001", earnedAt = 100L)
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentUserId_returnsFalse() = assertNotEquals(createEntity(userId = "user_001"), createEntity(userId = "user_002"))
    @Test fun equals_differentAchievementId_returnsFalse() = assertNotEquals(createEntity(achievementId = "ach_001"), createEntity(achievementId = "ach_002"))
    @Test fun equals_differentAnnounced_returnsFalse() = assertNotEquals(createEntity(announced = false), createEntity(announced = true))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_markAsAnnounced_announcedBecomesTrue() {
        val original = createEntity(announced = false)
        val copied = original.copy(announced = true)
        assertTrue(copied.announced)
        assertEquals(original.id, copied.id)
        assertEquals(original.achievementId, copied.achievementId)
    }

    @Test
    fun copy_withNewEarnedAt_onlyEarnedAtChanges() {
        val original = createEntity(earnedAt = 1000L)
        val copied = original.copy(earnedAt = 9999L)
        assertEquals(9999L, copied.earnedAt)
        assertEquals(original.userId, copied.userId)
        assertEquals(original.announced, copied.announced)
    }

    @Test
    fun copy_withNewAchievementId_achievementIdUpdated() {
        val copied = createEntity(achievementId = "ach_001").copy(achievementId = "ach_streak_100")
        assertEquals("ach_streak_100", copied.achievementId)
    }

    @Test
    fun earnedAt_exactTimestamp_isPreserved() {
        val ts = 1_741_999_999_999L
        assertEquals(ts, createEntity(earnedAt = ts).earnedAt)
    }

    @Test
    fun earnedAt_canBeBeforeCreatedAt() {
        val entity = createEntity(earnedAt = 100L, createdAt = 200L)
        assertTrue(entity.earnedAt < entity.createdAt)
    }
}
