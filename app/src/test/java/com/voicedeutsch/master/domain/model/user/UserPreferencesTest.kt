// Путь: src/test/java/com/voicedeutsch/master/domain/model/user/UserPreferencesTest.kt
package com.voicedeutsch.master.domain.model.user

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UserPreferencesTest {

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_allDefaultsAppliedCorrectly() {
        val prefs = UserPreferences()
        assertEquals(30, prefs.preferredSessionDuration)
        assertEquals(10, prefs.dailyGoalWords)
        assertEquals(30, prefs.dailyGoalMinutes)
        assertEquals(LearningPace.NORMAL, prefs.learningPace)
        assertTrue(prefs.srsEnabled)
        assertEquals(30, prefs.maxReviewsPerSession)
        assertTrue(prefs.reminderEnabled)
        assertEquals(19, prefs.reminderHour)
        assertEquals(0, prefs.reminderMinute)
        assertEquals(PronunciationStrictness.MODERATE, prefs.pronunciationStrictness)
        assertTrue(prefs.topicsOfInterest.isEmpty())
        assertNull(prefs.professionalDomain)
        assertFalse(prefs.offlineModeEnabled)
        assertFalse(prefs.dataSavingMode)
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_customValues_allFieldsStoredCorrectly() {
        val prefs = UserPreferences(
            preferredSessionDuration = 45,
            dailyGoalWords = 20,
            dailyGoalMinutes = 60,
            learningPace = LearningPace.FAST,
            srsEnabled = false,
            maxReviewsPerSession = 50,
            reminderEnabled = false,
            reminderHour = 8,
            reminderMinute = 30,
            pronunciationStrictness = PronunciationStrictness.STRICT,
            topicsOfInterest = listOf("travel", "food"),
            professionalDomain = "engineering",
            offlineModeEnabled = true,
            dataSavingMode = true
        )
        assertEquals(45, prefs.preferredSessionDuration)
        assertEquals(20, prefs.dailyGoalWords)
        assertEquals(60, prefs.dailyGoalMinutes)
        assertEquals(LearningPace.FAST, prefs.learningPace)
        assertFalse(prefs.srsEnabled)
        assertEquals(50, prefs.maxReviewsPerSession)
        assertFalse(prefs.reminderEnabled)
        assertEquals(8, prefs.reminderHour)
        assertEquals(30, prefs.reminderMinute)
        assertEquals(PronunciationStrictness.STRICT, prefs.pronunciationStrictness)
        assertEquals(listOf("travel", "food"), prefs.topicsOfInterest)
        assertEquals("engineering", prefs.professionalDomain)
        assertTrue(prefs.offlineModeEnabled)
        assertTrue(prefs.dataSavingMode)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_changeLearningPace_onlyPaceChanges() {
        val original = UserPreferences()
        val modified = original.copy(learningPace = LearningPace.SLOW)
        assertEquals(LearningPace.SLOW, modified.learningPace)
        assertEquals(original.preferredSessionDuration, modified.preferredSessionDuration)
        assertEquals(original.dailyGoalWords, modified.dailyGoalWords)
        assertEquals(original.srsEnabled, modified.srsEnabled)
    }

    @Test
    fun copy_changeProfessionalDomain_onlyDomainChanges() {
        val original = UserPreferences()
        val modified = original.copy(professionalDomain = "medicine")
        assertEquals("medicine", modified.professionalDomain)
        assertEquals(original.learningPace, modified.learningPace)
    }

    @Test
    fun copy_disableSrs_srsEnabledIsFalse() {
        val original = UserPreferences(srsEnabled = true)
        val modified = original.copy(srsEnabled = false)
        assertFalse(modified.srsEnabled)
        assertTrue(original.srsEnabled)
    }

    @Test
    fun copy_updateTopicsOfInterest_topicsUpdated() {
        val original = UserPreferences()
        val topics = listOf("tech", "sports", "music")
        val modified = original.copy(topicsOfInterest = topics)
        assertEquals(topics, modified.topicsOfInterest)
        assertTrue(original.topicsOfInterest.isEmpty())
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val prefs1 = UserPreferences(dailyGoalWords = 15, learningPace = LearningPace.FAST)
        val prefs2 = UserPreferences(dailyGoalWords = 15, learningPace = LearningPace.FAST)
        assertEquals(prefs1, prefs2)
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        val prefs1 = UserPreferences(dailyGoalWords = 15, learningPace = LearningPace.FAST)
        val prefs2 = UserPreferences(dailyGoalWords = 15, learningPace = LearningPace.FAST)
        assertEquals(prefs1.hashCode(), prefs2.hashCode())
    }

    @Test
    fun equals_differentDailyGoalWords_notEqual() {
        val prefs1 = UserPreferences(dailyGoalWords = 10)
        val prefs2 = UserPreferences(dailyGoalWords = 20)
        assertNotEquals(prefs1, prefs2)
    }

    @Test
    fun equals_defaultVsCustomProfessionalDomain_notEqual() {
        val prefs1 = UserPreferences()
        val prefs2 = UserPreferences(professionalDomain = "law")
        assertNotEquals(prefs1, prefs2)
    }
}

class LearningPaceTest {

    @Test
    fun entries_size_isThree() {
        assertEquals(3, LearningPace.entries.size)
    }

    @Test
    fun entries_containsSlow() {
        assertTrue(LearningPace.entries.contains(LearningPace.SLOW))
    }

    @Test
    fun entries_containsNormal() {
        assertTrue(LearningPace.entries.contains(LearningPace.NORMAL))
    }

    @Test
    fun entries_containsFast() {
        assertTrue(LearningPace.entries.contains(LearningPace.FAST))
    }

    @Test
    fun valueOf_slow_returnsSlow() {
        assertEquals(LearningPace.SLOW, LearningPace.valueOf("SLOW"))
    }

    @Test
    fun valueOf_normal_returnsNormal() {
        assertEquals(LearningPace.NORMAL, LearningPace.valueOf("NORMAL"))
    }

    @Test
    fun valueOf_fast_returnsFast() {
        assertEquals(LearningPace.FAST, LearningPace.valueOf("FAST"))
    }

    @Test
    fun valueOf_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            LearningPace.valueOf("ULTRA")
        }
    }

    @Test
    fun ordinal_slow_isZero() {
        assertEquals(0, LearningPace.SLOW.ordinal)
    }

    @Test
    fun ordinal_normal_isOne() {
        assertEquals(1, LearningPace.NORMAL.ordinal)
    }

    @Test
    fun ordinal_fast_isTwo() {
        assertEquals(2, LearningPace.FAST.ordinal)
    }
}

class PronunciationStrictnessTest {

    @Test
    fun entries_size_isThree() {
        assertEquals(3, PronunciationStrictness.entries.size)
    }

    @Test
    fun entries_containsLenient() {
        assertTrue(PronunciationStrictness.entries.contains(PronunciationStrictness.LENIENT))
    }

    @Test
    fun entries_containsModerate() {
        assertTrue(PronunciationStrictness.entries.contains(PronunciationStrictness.MODERATE))
    }

    @Test
    fun entries_containsStrict() {
        assertTrue(PronunciationStrictness.entries.contains(PronunciationStrictness.STRICT))
    }

    @Test
    fun valueOf_lenient_returnsLenient() {
        assertEquals(PronunciationStrictness.LENIENT, PronunciationStrictness.valueOf("LENIENT"))
    }

    @Test
    fun valueOf_moderate_returnsModerate() {
        assertEquals(PronunciationStrictness.MODERATE, PronunciationStrictness.valueOf("MODERATE"))
    }

    @Test
    fun valueOf_strict_returnsStrict() {
        assertEquals(PronunciationStrictness.STRICT, PronunciationStrictness.valueOf("STRICT"))
    }

    @Test
    fun valueOf_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            PronunciationStrictness.valueOf("VERY_STRICT")
        }
    }

    @Test
    fun ordinal_lenient_isZero() {
        assertEquals(0, PronunciationStrictness.LENIENT.ordinal)
    }

    @Test
    fun ordinal_moderate_isOne() {
        assertEquals(1, PronunciationStrictness.MODERATE.ordinal)
    }

    @Test
    fun ordinal_strict_isTwo() {
        assertEquals(2, PronunciationStrictness.STRICT.ordinal)
    }

    @Test
    fun defaultPreferences_usesModerateStrictness() {
        val prefs = UserPreferences()
        assertEquals(PronunciationStrictness.MODERATE, prefs.pronunciationStrictness)
    }
}
