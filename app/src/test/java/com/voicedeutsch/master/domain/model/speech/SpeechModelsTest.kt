// Путь: src/test/java/com/voicedeutsch/master/domain/model/speech/SpeechModelsTest.kt
package com.voicedeutsch.master.domain.model.speech

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ═══════════════════════════════════════════════════════════════════════════
// PronunciationResult
// ═══════════════════════════════════════════════════════════════════════════

class PronunciationResultTest {

    private fun createPronunciationResult(
        id: String = "pr_1",
        userId: String = "user_1",
        word: String = "Schule",
        score: Float = 0.8f,
        problemSounds: List<String> = emptyList(),
        attemptNumber: Int = 1,
        sessionId: String? = null
    ) = PronunciationResult(
        id = id,
        userId = userId,
        word = word,
        score = score,
        problemSounds = problemSounds,
        attemptNumber = attemptNumber,
        sessionId = sessionId
    )

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val result = PronunciationResult(id = "pr_1", userId = "u1", word = "Haus", score = 0.7f)
        assertTrue(result.problemSounds.isEmpty())
        assertEquals(1, result.attemptNumber)
        assertNull(result.sessionId)
        assertTrue(result.timestamp > 0)
        assertTrue(result.createdAt > 0)
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val result = createPronunciationResult(
            id = "pr_42",
            userId = "u99",
            word = "Straße",
            score = 0.65f,
            problemSounds = listOf("ß", "r"),
            attemptNumber = 3,
            sessionId = "session_7"
        )
        assertEquals("pr_42", result.id)
        assertEquals("u99", result.userId)
        assertEquals("Straße", result.word)
        assertEquals(0.65f, result.score, 0.001f)
        assertEquals(listOf("ß", "r"), result.problemSounds)
        assertEquals(3, result.attemptNumber)
        assertEquals("session_7", result.sessionId)
    }

    @Test
    fun constructor_scorePerfect_storedCorrectly() {
        val result = createPronunciationResult(score = 1.0f)
        assertEquals(1.0f, result.score, 0.001f)
    }

    @Test
    fun constructor_scoreZero_storedCorrectly() {
        val result = createPronunciationResult(score = 0.0f)
        assertEquals(0.0f, result.score, 0.001f)
    }

    @Test
    fun constructor_withSessionId_sessionIdStored() {
        val result = createPronunciationResult(sessionId = "s_123")
        assertEquals("s_123", result.sessionId)
    }

    @Test
    fun constructor_nullSessionId_isNull() {
        val result = createPronunciationResult(sessionId = null)
        assertNull(result.sessionId)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_incrementAttemptNumber_onlyAttemptChanges() {
        val original = createPronunciationResult(attemptNumber = 1)
        val modified = original.copy(attemptNumber = 2)
        assertEquals(2, modified.attemptNumber)
        assertEquals(original.word, modified.word)
        assertEquals(original.score, modified.score, 0.001f)
    }

    @Test
    fun copy_updateProblemSounds_soundsUpdated() {
        val original = createPronunciationResult(problemSounds = emptyList())
        val modified = original.copy(problemSounds = listOf("ü", "ch"))
        assertEquals(listOf("ü", "ch"), modified.problemSounds)
        assertTrue(original.problemSounds.isEmpty())
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val ts = 1_000_000L
        val r1 = PronunciationResult(id = "pr_1", userId = "u1", word = "Haus", score = 0.8f, timestamp = ts, createdAt = ts)
        val r2 = PronunciationResult(id = "pr_1", userId = "u1", word = "Haus", score = 0.8f, timestamp = ts, createdAt = ts)
        assertEquals(r1, r2)
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        val ts = 1_000_000L
        val r1 = PronunciationResult(id = "pr_1", userId = "u1", word = "Haus", score = 0.8f, timestamp = ts, createdAt = ts)
        val r2 = PronunciationResult(id = "pr_1", userId = "u1", word = "Haus", score = 0.8f, timestamp = ts, createdAt = ts)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun equals_differentScore_notEqual() {
        val r1 = createPronunciationResult(score = 0.5f)
        val r2 = createPronunciationResult(score = 0.9f)
        assertNotEquals(r1, r2)
    }

    @Test
    fun equals_differentWord_notEqual() {
        val r1 = createPronunciationResult(word = "Haus")
        val r2 = createPronunciationResult(word = "Maus")
        assertNotEquals(r1, r2)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SpeechAnalysis
// ═══════════════════════════════════════════════════════════════════════════

class SpeechAnalysisTest {

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val analysis = SpeechAnalysis()
        assertEquals(0f, analysis.intelligibility, 0.001f)
        assertEquals(0f, analysis.segmentalAccuracy, 0.001f)
        assertTrue(analysis.stressCorrect)
        assertEquals(0f, analysis.intonation, 0.001f)
        assertEquals(0f, analysis.fluency, 0.001f)
    }

    // ── overallScore ──────────────────────────────────────────────────────

    @Test
    fun overallScore_allZerosStressCorrect_returns15Percent() {
        val analysis = SpeechAnalysis(
            intelligibility = 0f,
            segmentalAccuracy = 0f,
            stressCorrect = true,
            intonation = 0f,
            fluency = 0f
        )
        assertEquals(0.15f, analysis.overallScore, 0.001f)
    }

    @Test
    fun overallScore_allZerosStressIncorrect_returnsZero() {
        val analysis = SpeechAnalysis(
            intelligibility = 0f,
            segmentalAccuracy = 0f,
            stressCorrect = false,
            intonation = 0f,
            fluency = 0f
        )
        assertEquals(0f, analysis.overallScore, 0.001f)
    }

    @Test
    fun overallScore_allOnesStressCorrect_returnsOne() {
        val analysis = SpeechAnalysis(
            intelligibility = 1f,
            segmentalAccuracy = 1f,
            stressCorrect = true,
            intonation = 1f,
            fluency = 1f
        )
        assertEquals(1.0f, analysis.overallScore, 0.001f)
    }

    @Test
    fun overallScore_allOnesStressIncorrect_returns85Percent() {
        val analysis = SpeechAnalysis(
            intelligibility = 1f,
            segmentalAccuracy = 1f,
            stressCorrect = false,
            intonation = 1f,
            fluency = 1f
        )
        assertEquals(0.85f, analysis.overallScore, 0.001f)
    }

    @Test
    fun overallScore_weightedFormula_calculatedCorrectly() {
        val analysis = SpeechAnalysis(
            intelligibility = 0.8f,
            segmentalAccuracy = 0.6f,
            stressCorrect = true,
            intonation = 0.4f,
            fluency = 0.2f
        )
        val expected = 0.3f * 0.8f + 0.3f * 0.6f + 0.15f * 1f + 0.15f * 0.4f + 0.1f * 0.2f
        assertEquals(expected, analysis.overallScore, 0.001f)
    }

    @Test
    fun overallScore_onlyIntelligibility_contributes30Percent() {
        val analysis = SpeechAnalysis(
            intelligibility = 1f,
            segmentalAccuracy = 0f,
            stressCorrect = false,
            intonation = 0f,
            fluency = 0f
        )
        assertEquals(0.3f, analysis.overallScore, 0.001f)
    }

    @Test
    fun overallScore_onlySegmentalAccuracy_contributes30Percent() {
        val analysis = SpeechAnalysis(
            intelligibility = 0f,
            segmentalAccuracy = 1f,
            stressCorrect = false,
            intonation = 0f,
            fluency = 0f
        )
        assertEquals(0.3f, analysis.overallScore, 0.001f)
    }

    @Test
    fun overallScore_onlyIntonation_contributes15Percent() {
        val analysis = SpeechAnalysis(
            intelligibility = 0f,
            segmentalAccuracy = 0f,
            stressCorrect = false,
            intonation = 1f,
            fluency = 0f
        )
        assertEquals(0.15f, analysis.overallScore, 0.001f)
    }

    @Test
    fun overallScore_onlyFluency_contributes10Percent() {
        val analysis = SpeechAnalysis(
            intelligibility = 0f,
            segmentalAccuracy = 0f,
            stressCorrect = false,
            intonation = 0f,
            fluency = 1f
        )
        assertEquals(0.1f, analysis.overallScore, 0.001f)
    }

    @Test
    fun overallScore_halfAllScoresStressCorrect_calculatedCorrectly() {
        val analysis = SpeechAnalysis(
            intelligibility = 0.5f,
            segmentalAccuracy = 0.5f,
            stressCorrect = true,
            intonation = 0.5f,
            fluency = 0.5f
        )
        val expected = 0.3f * 0.5f + 0.3f * 0.5f + 0.15f * 1f + 0.15f * 0.5f + 0.1f * 0.5f
        assertEquals(expected, analysis.overallScore, 0.001f)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_changeStressCorrect_overallScoreChanges() {
        val withStress = SpeechAnalysis(stressCorrect = true)
        val withoutStress = withStress.copy(stressCorrect = false)
        assertTrue(withStress.overallScore > withoutStress.overallScore)
    }

    @Test
    fun copy_updateFluency_fluencyUpdated() {
        val original = SpeechAnalysis(fluency = 0.2f)
        val modified = original.copy(fluency = 0.9f)
        assertEquals(0.9f, modified.fluency, 0.001f)
        assertEquals(0.2f, original.fluency, 0.001f)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a1 = SpeechAnalysis(intelligibility = 0.8f, stressCorrect = true)
        val a2 = SpeechAnalysis(intelligibility = 0.8f, stressCorrect = true)
        assertEquals(a1, a2)
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        val a1 = SpeechAnalysis(intelligibility = 0.8f)
        val a2 = SpeechAnalysis(intelligibility = 0.8f)
        assertEquals(a1.hashCode(), a2.hashCode())
    }

    @Test
    fun equals_differentStressCorrect_notEqual() {
        assertNotEquals(
            SpeechAnalysis(stressCorrect = true),
            SpeechAnalysis(stressCorrect = false)
        )
    }

    @Test
    fun equals_differentFluency_notEqual() {
        assertNotEquals(SpeechAnalysis(fluency = 0.3f), SpeechAnalysis(fluency = 0.9f))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PhoneticTarget
// ═══════════════════════════════════════════════════════════════════════════

class PhoneticTargetTest {

    private fun createPhoneticTarget(
        sound: String = "ü",
        ipa: String = "[y]",
        detectionDate: Long = 1_000_000L,
        totalAttempts: Int = 0,
        successfulAttempts: Int = 0,
        currentScore: Float = 0f,
        trend: PronunciationTrend = PronunciationTrend.STABLE,
        lastPracticed: Long? = null,
        inWords: List<String> = emptyList()
    ) = PhoneticTarget(
        sound = sound,
        ipa = ipa,
        detectionDate = detectionDate,
        totalAttempts = totalAttempts,
        successfulAttempts = successfulAttempts,
        currentScore = currentScore,
        trend = trend,
        lastPracticed = lastPracticed,
        inWords = inWords
    )

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val target = PhoneticTarget(sound = "ö", ipa = "[ø]", detectionDate = 1_000L)
        assertEquals(0, target.totalAttempts)
        assertEquals(0, target.successfulAttempts)
        assertEquals(0f, target.currentScore, 0.001f)
        assertEquals(PronunciationTrend.STABLE, target.trend)
        assertNull(target.lastPracticed)
        assertTrue(target.inWords.isEmpty())
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val target = createPhoneticTarget(
            sound = "ch",
            ipa = "[x]",
            detectionDate = 2_000_000L,
            totalAttempts = 10,
            successfulAttempts = 6,
            currentScore = 0.6f,
            trend = PronunciationTrend.IMPROVING,
            lastPracticed = 3_000_000L,
            inWords = listOf("machen", "lachen")
        )
        assertEquals("ch", target.sound)
        assertEquals("[x]", target.ipa)
        assertEquals(2_000_000L, target.detectionDate)
        assertEquals(10, target.totalAttempts)
        assertEquals(6, target.successfulAttempts)
        assertEquals(0.6f, target.currentScore, 0.001f)
        assertEquals(PronunciationTrend.IMPROVING, target.trend)
        assertEquals(3_000_000L, target.lastPracticed)
        assertEquals(listOf("machen", "lachen"), target.inWords)
    }

    @Test
    fun constructor_decliningTrend_storedCorrectly() {
        val target = createPhoneticTarget(trend = PronunciationTrend.DECLINING)
        assertEquals(PronunciationTrend.DECLINING, target.trend)
    }

    @Test
    fun constructor_nullLastPracticed_isNull() {
        val target = createPhoneticTarget(lastPracticed = null)
        assertNull(target.lastPracticed)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_incrementTotalAttempts_onlyAttemptsChange() {
        val original = createPhoneticTarget(totalAttempts = 5)
        val modified = original.copy(totalAttempts = 6)
        assertEquals(6, modified.totalAttempts)
        assertEquals(original.sound, modified.sound)
        assertEquals(original.currentScore, modified.currentScore, 0.001f)
    }

    @Test
    fun copy_changeTrendToImproving_trendUpdated() {
        val original = createPhoneticTarget(trend = PronunciationTrend.STABLE)
        val modified = original.copy(trend = PronunciationTrend.IMPROVING)
        assertEquals(PronunciationTrend.IMPROVING, modified.trend)
        assertEquals(PronunciationTrend.STABLE, original.trend)
    }

    @Test
    fun copy_setLastPracticed_lastPracticedUpdated() {
        val original = createPhoneticTarget(lastPracticed = null)
        val modified = original.copy(lastPracticed = 9_999_999L)
        assertEquals(9_999_999L, modified.lastPracticed)
        assertNull(original.lastPracticed)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createPhoneticTarget(), createPhoneticTarget())
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(createPhoneticTarget().hashCode(), createPhoneticTarget().hashCode())
    }

    @Test
    fun equals_differentSound_notEqual() {
        assertNotEquals(createPhoneticTarget(sound = "ü"), createPhoneticTarget(sound = "ö"))
    }

    @Test
    fun equals_differentTrend_notEqual() {
        assertNotEquals(
            createPhoneticTarget(trend = PronunciationTrend.IMPROVING),
            createPhoneticTarget(trend = PronunciationTrend.DECLINING)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PronunciationTrend
// ═══════════════════════════════════════════════════════════════════════════

class PronunciationTrendTest {

    @Test
    fun entries_size_isThree() {
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
    fun valueOf_improving_returnsImproving() {
        assertEquals(PronunciationTrend.IMPROVING, PronunciationTrend.valueOf("IMPROVING"))
    }

    @Test
    fun valueOf_stable_returnsStable() {
        assertEquals(PronunciationTrend.STABLE, PronunciationTrend.valueOf("STABLE"))
    }

    @Test
    fun valueOf_declining_returnsDeclining() {
        assertEquals(PronunciationTrend.DECLINING, PronunciationTrend.valueOf("DECLINING"))
    }

    @Test
    fun valueOf_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            PronunciationTrend.valueOf("UNKNOWN")
        }
    }

    @Test
    fun ordinal_improving_isZero() {
        assertEquals(0, PronunciationTrend.IMPROVING.ordinal)
    }

    @Test
    fun ordinal_stable_isOne() {
        assertEquals(1, PronunciationTrend.STABLE.ordinal)
    }

    @Test
    fun ordinal_declining_isTwo() {
        assertEquals(2, PronunciationTrend.DECLINING.ordinal)
    }

    @Test
    fun defaultPhoneticTarget_usesStableTrend() {
        val target = PhoneticTarget(sound = "r", ipa = "[ʁ]", detectionDate = 1_000L)
        assertEquals(PronunciationTrend.STABLE, target.trend)
    }
}
