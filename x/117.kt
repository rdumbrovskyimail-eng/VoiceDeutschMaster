// Путь: src/test/java/com/voicedeutsch/master/domain/model/user/UserProfileTest.kt
package com.voicedeutsch.master.domain.model.user

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UserProfileTest {

    private fun createUserProfile(
        id: String = "user_1",
        name: String = "Anna",
        nativeLanguage: String = "ru",
        targetLanguage: String = "de",
        cefrLevel: CefrLevel = CefrLevel.A1,
        cefrSubLevel: Int = 1,
        totalSessions: Int = 0,
        totalMinutes: Int = 0,
        totalWordsLearned: Int = 0,
        totalRulesLearned: Int = 0,
        age: Int? = null,
        hobbies: String? = null,
        learningGoals: String? = null,
        streakDays: Int = 0,
        lastSessionDate: Long? = null,
        preferences: UserPreferences = UserPreferences(),
        voiceSettings: VoiceSettings = VoiceSettings()
    ) = UserProfile(
        id = id,
        name = name,
        nativeLanguage = nativeLanguage,
        targetLanguage = targetLanguage,
        cefrLevel = cefrLevel,
        cefrSubLevel = cefrSubLevel,
        totalSessions = totalSessions,
        totalMinutes = totalMinutes,
        totalWordsLearned = totalWordsLearned,
        totalRulesLearned = totalRulesLearned,
        age = age,
        hobbies = hobbies,
        learningGoals = learningGoals,
        streakDays = streakDays,
        lastSessionDate = lastSessionDate,
        preferences = preferences,
        voiceSettings = voiceSettings
    )

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val profile = UserProfile(id = "u1", name = "Max")
        assertEquals("ru", profile.nativeLanguage)
        assertEquals("de", profile.targetLanguage)
        assertEquals(CefrLevel.A1, profile.cefrLevel)
        assertEquals(1, profile.cefrSubLevel)
        assertEquals(0, profile.totalSessions)
        assertEquals(0, profile.totalMinutes)
        assertEquals(0, profile.totalWordsLearned)
        assertEquals(0, profile.totalRulesLearned)
        assertNull(profile.age)
        assertNull(profile.hobbies)
        assertNull(profile.learningGoals)
        assertEquals(0, profile.streakDays)
        assertNull(profile.lastSessionDate)
        assertEquals(UserPreferences(), profile.preferences)
        assertEquals(VoiceSettings(), profile.voiceSettings)
    }

    @Test
    fun constructor_requiredFields_storedCorrectly() {
        val profile = UserProfile(id = "abc", name = "Hans")
        assertEquals("abc", profile.id)
        assertEquals("Hans", profile.name)
    }

    @Test
    fun constructor_customValues_storedCorrectly() {
        val prefs = UserPreferences(dailyGoalWords = 20)
        val profile = createUserProfile(
            id = "u2",
            name = "Lena",
            cefrLevel = CefrLevel.B1,
            cefrSubLevel = 5,
            totalSessions = 10,
            totalMinutes = 300,
            totalWordsLearned = 150,
            totalRulesLearned = 20,
            age = 25,
            hobbies = "reading",
            learningGoals = "fluency",
            streakDays = 7,
            lastSessionDate = 1000L,
            preferences = prefs
        )
        assertEquals("u2", profile.id)
        assertEquals("Lena", profile.name)
        assertEquals(CefrLevel.B1, profile.cefrLevel)
        assertEquals(5, profile.cefrSubLevel)
        assertEquals(10, profile.totalSessions)
        assertEquals(300, profile.totalMinutes)
        assertEquals(150, profile.totalWordsLearned)
        assertEquals(20, profile.totalRulesLearned)
        assertEquals(25, profile.age)
        assertEquals("reading", profile.hobbies)
        assertEquals("fluency", profile.learningGoals)
        assertEquals(7, profile.streakDays)
        assertEquals(1000L, profile.lastSessionDate)
        assertEquals(prefs, profile.preferences)
    }

    // ── cefrDisplay ───────────────────────────────────────────────────────

    @Test
    fun cefrDisplay_a1SubLevel1_returnsA1dot1() {
        val profile = createUserProfile(cefrLevel = CefrLevel.A1, cefrSubLevel = 1)
        assertEquals("A1.1", profile.cefrDisplay)
    }

    @Test
    fun cefrDisplay_b2SubLevel7_returnsB2dot7() {
        val profile = createUserProfile(cefrLevel = CefrLevel.B2, cefrSubLevel = 7)
        assertEquals("B2.7", profile.cefrDisplay)
    }

    @Test
    fun cefrDisplay_c1SubLevel10_returnsC1dot10() {
        val profile = createUserProfile(cefrLevel = CefrLevel.C1, cefrSubLevel = 10)
        assertEquals("C1.10", profile.cefrDisplay)
    }

    @Test
    fun cefrDisplay_a2SubLevel3_returnsA2dot3() {
        val profile = createUserProfile(cefrLevel = CefrLevel.A2, cefrSubLevel = 3)
        assertEquals("A2.3", profile.cefrDisplay)
    }

    // ── isNewUser ─────────────────────────────────────────────────────────

    @Test
    fun isNewUser_zeroSessions_returnsTrue() {
        val profile = createUserProfile(totalSessions = 0)
        assertTrue(profile.isNewUser)
    }

    @Test
    fun isNewUser_oneSession_returnsFalse() {
        val profile = createUserProfile(totalSessions = 1)
        assertFalse(profile.isNewUser)
    }

    @Test
    fun isNewUser_manySessions_returnsFalse() {
        val profile = createUserProfile(totalSessions = 100)
        assertFalse(profile.isNewUser)
    }

    // ── totalHours ────────────────────────────────────────────────────────

    @Test
    fun totalHours_zeroMinutes_returnsZero() {
        val profile = createUserProfile(totalMinutes = 0)
        assertEquals(0f, profile.totalHours, 0.001f)
    }

    @Test
    fun totalHours_60Minutes_returnsOne() {
        val profile = createUserProfile(totalMinutes = 60)
        assertEquals(1f, profile.totalHours, 0.001f)
    }

    @Test
    fun totalHours_90Minutes_returnsOnePointFive() {
        val profile = createUserProfile(totalMinutes = 90)
        assertEquals(1.5f, profile.totalHours, 0.001f)
    }

    @Test
    fun totalHours_30Minutes_returnsHalf() {
        val profile = createUserProfile(totalMinutes = 30)
        assertEquals(0.5f, profile.totalHours, 0.001f)
    }

    @Test
    fun totalHours_1Minute_returnsFractionalHour() {
        val profile = createUserProfile(totalMinutes = 1)
        assertEquals(1f / 60f, profile.totalHours, 0.001f)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_changeName_onlyNameChanges() {
        val original = createUserProfile(name = "Anna")
        val modified = original.copy(name = "Klaus")
        assertEquals("Klaus", modified.name)
        assertEquals(original.id, modified.id)
        assertEquals(original.cefrLevel, modified.cefrLevel)
        assertEquals(original.totalSessions, modified.totalSessions)
    }

    @Test
    fun copy_incrementStreak_streakUpdated() {
        val original = createUserProfile(streakDays = 5)
        val modified = original.copy(streakDays = 6)
        assertEquals(6, modified.streakDays)
        assertEquals(5, original.streakDays)
    }

    @Test
    fun copy_setCefrLevel_levelUpdated() {
        val original = createUserProfile(cefrLevel = CefrLevel.A1)
        val modified = original.copy(cefrLevel = CefrLevel.B1)
        assertEquals(CefrLevel.B1, modified.cefrLevel)
        assertEquals(CefrLevel.A1, original.cefrLevel)
    }

    @Test
    fun copy_setAge_ageUpdated() {
        val original = createUserProfile(age = null)
        val modified = original.copy(age = 30)
        assertEquals(30, modified.age)
        assertNull(original.age)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val ts = System.currentTimeMillis()
        val p1 = UserProfile(id = "u1", name = "Anna", createdAt = ts, updatedAt = ts)
        val p2 = UserProfile(id = "u1", name = "Anna", createdAt = ts, updatedAt = ts)
        assertEquals(p1, p2)
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        val ts = System.currentTimeMillis()
        val p1 = UserProfile(id = "u1", name = "Anna", createdAt = ts, updatedAt = ts)
        val p2 = UserProfile(id = "u1", name = "Anna", createdAt = ts, updatedAt = ts)
        assertEquals(p1.hashCode(), p2.hashCode())
    }

    @Test
    fun equals_differentId_notEqual() {
        val ts = System.currentTimeMillis()
        val p1 = UserProfile(id = "u1", name = "Anna", createdAt = ts, updatedAt = ts)
        val p2 = UserProfile(id = "u2", name = "Anna", createdAt = ts, updatedAt = ts)
        assertNotEquals(p1, p2)
    }

    @Test
    fun equals_differentCefrLevel_notEqual() {
        val ts = System.currentTimeMillis()
        val p1 = UserProfile(id = "u1", name = "Anna", cefrLevel = CefrLevel.A1, createdAt = ts, updatedAt = ts)
        val p2 = UserProfile(id = "u1", name = "Anna", cefrLevel = CefrLevel.B2, createdAt = ts, updatedAt = ts)
        assertNotEquals(p1, p2)
    }

    // ── Timestamps ────────────────────────────────────────────────────────

    @Test
    fun createdAt_defaultValue_isPositive() {
        val profile = UserProfile(id = "u1", name = "Max")
        assertTrue(profile.createdAt > 0)
    }

    @Test
    fun updatedAt_defaultValue_isPositive() {
        val profile = UserProfile(id = "u1", name = "Max")
        assertTrue(profile.updatedAt > 0)
    }
}
