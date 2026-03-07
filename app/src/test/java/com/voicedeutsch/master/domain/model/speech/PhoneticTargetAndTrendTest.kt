// Path: src/test/java/com/voicedeutsch/master/domain/model/speech/PhoneticTargetAndTrendTest.kt
package com.voicedeutsch.master.domain.model.speech

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ════════════════════════════════════════════════════════════════════════════
// PronunciationTrend
// ════════════════════════════════════════════════════════════════════════════

class PronunciationTrendTest {

    @Test
    fun entries_size_equals3() {
        assertEquals(3, PronunciationTrend.entries.size)
    }

    @Test
    fun entries_containsImproving() {
        assertTrue(PronunciationTrend.entries.contains(PronunciationTrend.IMPROVING))
    }

    @Test
    fun entries_containsStable() {
        assertTrue(PronunciationTrend.entries.contains(PronunciationTrend.STABLE))
    }

    @Test
    fun entries_containsDeclining() {
        assertTrue(PronunciationTrend.entries.contains(PronunciationTrend.DECLINING))
    }

    @Test
    fun valueOf_improving_returnsCorrect() {
        assertEquals(PronunciationTrend.IMPROVING, PronunciationTrend.valueOf("IMPROVING"))
    }

    @Test
    fun valueOf_stable_returnsCorrect() {
        assertEquals(PronunciationTrend.STABLE, PronunciationTrend.valueOf("STABLE"))
    }

    @Test
    fun valueOf_declining_returnsCorrect() {
        assertEquals(PronunciationTrend.DECLINING, PronunciationTrend.valueOf("DECLINING"))
    }

    @Test
    fun valueOf_unknown_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            PronunciationTrend.valueOf("UNKNOWN_TREND")
        }
    }

    @Test
    fun allEntries_haveUniqueOrdinal() {
        val ordinals = PronunciationTrend.entries.map { it.ordinal }
        assertEquals(ordinals.size, ordinals.toSet().size)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// PhoneticTarget
// ════════════════════════════════════════════════════════════════════════════

class PhoneticTargetTest {

    private fun makePhoneticTarget(
        sound: String = "ü",
        ipa: String = "[y]",
        detectionDate: Long = 1_000_000L,
        totalAttempts: Int = 0,
        successfulAttempts: Int = 0,
        currentScore: Float = 0f,
        trend: PronunciationTrend = PronunciationTrend.STABLE,
        lastPracticed: Long? = null,
        inWords: List<String> = emptyList(),
    ) = PhoneticTarget(
        sound = sound,
        ipa = ipa,
        detectionDate = detectionDate,
        totalAttempts = totalAttempts,
        successfulAttempts = successfulAttempts,
        currentScore = currentScore,
        trend = trend,
        lastPracticed = lastPracticed,
        inWords = inWords,
    )

    @Test
    fun creation_withRequiredFields_setsValues() {
        val pt = makePhoneticTarget()
        assertEquals("ü", pt.sound)
        assertEquals("[y]", pt.ipa)
        assertEquals(1_000_000L, pt.detectionDate)
    }

    @Test
    fun creation_defaultTotalAttempts_isZero() {
        val pt = makePhoneticTarget()
        assertEquals(0, pt.totalAttempts)
    }

    @Test
    fun creation_defaultSuccessfulAttempts_isZero() {
        val pt = makePhoneticTarget()
        assertEquals(0, pt.successfulAttempts)
    }

    @Test
    fun creation_defaultCurrentScore_isZero() {
        val pt = makePhoneticTarget()
        assertEquals(0f, pt.currentScore)
    }

    @Test
    fun creation_defaultTrend_isStable() {
        val pt = makePhoneticTarget()
        assertEquals(PronunciationTrend.STABLE, pt.trend)
    }

    @Test
    fun creation_defaultLastPracticed_isNull() {
        val pt = makePhoneticTarget()
        assertNull(pt.lastPracticed)
    }

    @Test
    fun creation_defaultInWords_isEmpty() {
        val pt = makePhoneticTarget()
        assertTrue(pt.inWords.isEmpty())
    }

    @Test
    fun creation_withInWords_setsWords() {
        val pt = makePhoneticTarget(inWords = listOf("über", "müde", "für"))
        assertEquals(3, pt.inWords.size)
        assertTrue(pt.inWords.contains("über"))
    }

    @Test
    fun creation_withLastPracticed_setsValue() {
        val pt = makePhoneticTarget(lastPracticed = 9_999_999L)
        assertEquals(9_999_999L, pt.lastPracticed)
    }

    @Test
    fun creation_trendImproving_isValid() {
        val pt = makePhoneticTarget(trend = PronunciationTrend.IMPROVING)
        assertEquals(PronunciationTrend.IMPROVING, pt.trend)
    }

    @Test
    fun creation_trendDeclining_isValid() {
        val pt = makePhoneticTarget(trend = PronunciationTrend.DECLINING)
        assertEquals(PronunciationTrend.DECLINING, pt.trend)
    }

    @Test
    fun copy_changesTrend_restUnchanged() {
        val original = makePhoneticTarget(trend = PronunciationTrend.STABLE)
        val copy = original.copy(trend = PronunciationTrend.IMPROVING)
        assertEquals(PronunciationTrend.IMPROVING, copy.trend)
        assertEquals("ü", copy.sound)
        assertEquals(0f, copy.currentScore)
    }

    @Test
    fun copy_changesCurrentScore() {
        val original = makePhoneticTarget(currentScore = 0.3f)
        val copy = original.copy(currentScore = 0.75f)
        assertEquals(0.75f, copy.currentScore)
    }

    @Test
    fun copy_changesTotalAttempts() {
        val original = makePhoneticTarget(totalAttempts = 5)
        val copy = original.copy(totalAttempts = 10)
        assertEquals(10, copy.totalAttempts)
    }

    @Test
    fun copy_addsLastPracticed() {
        val original = makePhoneticTarget(lastPracticed = null)
        val copy = original.copy(lastPracticed = 5_000L)
        assertEquals(5_000L, copy.lastPracticed)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makePhoneticTarget()
        val b = makePhoneticTarget()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentSound_areNotEqual() {
        val a = makePhoneticTarget(sound = "ü")
        val b = makePhoneticTarget(sound = "ö")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentTrend_areNotEqual() {
        val a = makePhoneticTarget(trend = PronunciationTrend.STABLE)
        val b = makePhoneticTarget(trend = PronunciationTrend.DECLINING)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentTotalAttempts_areNotEqual() {
        val a = makePhoneticTarget(totalAttempts = 5)
        val b = makePhoneticTarget(totalAttempts = 6)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_nullVsNonNullLastPracticed_areNotEqual() {
        val a = makePhoneticTarget(lastPracticed = null)
        val b = makePhoneticTarget(lastPracticed = 1000L)
        assertNotEquals(a, b)
    }
}
