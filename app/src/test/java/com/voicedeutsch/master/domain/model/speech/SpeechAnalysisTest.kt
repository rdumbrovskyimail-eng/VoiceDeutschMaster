// Path: src/test/java/com/voicedeutsch/master/domain/model/speech/SpeechAnalysisTest.kt
package com.voicedeutsch.master.domain.model.speech

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SpeechAnalysisTest {

    private fun makeSpeechAnalysis(
        intelligibility: Float = 0f,
        segmentalAccuracy: Float = 0f,
        stressCorrect: Boolean = true,
        intonation: Float = 0f,
        fluency: Float = 0f,
    ) = SpeechAnalysis(
        intelligibility = intelligibility,
        segmentalAccuracy = segmentalAccuracy,
        stressCorrect = stressCorrect,
        intonation = intonation,
        fluency = fluency,
    )

    // ── overallScore — computed property ─────────────────────────────────────

    @Test
    fun overallScore_allZeroStressCorrect_returnsStressContribution() {
        // 0.3*0 + 0.3*0 + 0.15*1 + 0.15*0 + 0.1*0 = 0.15
        val sa = makeSpeechAnalysis(stressCorrect = true)
        assertEquals(0.15f, sa.overallScore, 0.001f)
    }

    @Test
    fun overallScore_allZeroStressIncorrect_returnsZero() {
        val sa = makeSpeechAnalysis(stressCorrect = false)
        assertEquals(0.0f, sa.overallScore, 0.001f)
    }

    @Test
    fun overallScore_perfectAllFields_returnsOne() {
        val sa = makeSpeechAnalysis(
            intelligibility = 1f,
            segmentalAccuracy = 1f,
            stressCorrect = true,
            intonation = 1f,
            fluency = 1f,
        )
        // 0.3 + 0.3 + 0.15 + 0.15 + 0.1 = 1.0
        assertEquals(1.0f, sa.overallScore, 0.001f)
    }

    @Test
    fun overallScore_perfectWithWrongStress_returnsPoint85() {
        val sa = makeSpeechAnalysis(
            intelligibility = 1f,
            segmentalAccuracy = 1f,
            stressCorrect = false,
            intonation = 1f,
            fluency = 1f,
        )
        // 0.3 + 0.3 + 0.0 + 0.15 + 0.1 = 0.85
        assertEquals(0.85f, sa.overallScore, 0.001f)
    }

    @Test
    fun overallScore_intelligibilityOnly_returnsThirtyPercent() {
        val sa = makeSpeechAnalysis(
            intelligibility = 1f,
            stressCorrect = false,
        )
        assertEquals(0.3f, sa.overallScore, 0.001f)
    }

    @Test
    fun overallScore_segmentalAccuracyOnly_returnsThirtyPercent() {
        val sa = makeSpeechAnalysis(
            segmentalAccuracy = 1f,
            stressCorrect = false,
        )
        assertEquals(0.3f, sa.overallScore, 0.001f)
    }

    @Test
    fun overallScore_intonationOnly_returnsFifteenPercent() {
        val sa = makeSpeechAnalysis(
            intonation = 1f,
            stressCorrect = false,
        )
        assertEquals(0.15f, sa.overallScore, 0.001f)
    }

    @Test
    fun overallScore_fluencyOnly_returnsTenPercent() {
        val sa = makeSpeechAnalysis(
            fluency = 1f,
            stressCorrect = false,
        )
        assertEquals(0.1f, sa.overallScore, 0.001f)
    }

    @Test
    fun overallScore_halfAllFields_returnsHalf() {
        val sa = makeSpeechAnalysis(
            intelligibility = 0.5f,
            segmentalAccuracy = 0.5f,
            stressCorrect = true,
            intonation = 0.5f,
            fluency = 0.5f,
        )
        // 0.15 + 0.15 + 0.15 + 0.075 + 0.05 = 0.575
        val expected = 0.3f * 0.5f + 0.3f * 0.5f + 0.15f * 1f + 0.15f * 0.5f + 0.1f * 0.5f
        assertEquals(expected, sa.overallScore, 0.001f)
    }

    @Test
    fun overallScore_customWeightedValues_computedCorrectly() {
        val intel = 0.8f
        val seg = 0.6f
        val inton = 0.7f
        val flu = 0.9f
        val sa = makeSpeechAnalysis(
            intelligibility = intel,
            segmentalAccuracy = seg,
            stressCorrect = true,
            intonation = inton,
            fluency = flu,
        )
        val expected = 0.3f * intel + 0.3f * seg + 0.15f * 1f + 0.15f * inton + 0.1f * flu
        assertEquals(expected, sa.overallScore, 0.001f)
    }

    // ── defaults ─────────────────────────────────────────────────────────────

    @Test
    fun creation_defaultIntelligibility_isZero() {
        val sa = SpeechAnalysis()
        assertEquals(0f, sa.intelligibility)
    }

    @Test
    fun creation_defaultSegmentalAccuracy_isZero() {
        val sa = SpeechAnalysis()
        assertEquals(0f, sa.segmentalAccuracy)
    }

    @Test
    fun creation_defaultStressCorrect_isTrue() {
        val sa = SpeechAnalysis()
        assertTrue(sa.stressCorrect)
    }

    @Test
    fun creation_defaultIntonation_isZero() {
        val sa = SpeechAnalysis()
        assertEquals(0f, sa.intonation)
    }

    @Test
    fun creation_defaultFluency_isZero() {
        val sa = SpeechAnalysis()
        assertEquals(0f, sa.fluency)
    }

    // ── data class basics ─────────────────────────────────────────────────────

    @Test
    fun copy_changesStressCorrect_overallScoreChanges() {
        val original = makeSpeechAnalysis(stressCorrect = true, intelligibility = 0f)
        val copy = original.copy(stressCorrect = false)
        assertTrue(original.overallScore > copy.overallScore)
    }

    @Test
    fun copy_changesIntelligibility_restUnchanged() {
        val original = makeSpeechAnalysis(intelligibility = 0.5f)
        val copy = original.copy(intelligibility = 1.0f)
        assertEquals(1.0f, copy.intelligibility)
        assertEquals(original.fluency, copy.fluency)
        assertEquals(original.stressCorrect, copy.stressCorrect)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeSpeechAnalysis(intelligibility = 0.8f, stressCorrect = true)
        val b = makeSpeechAnalysis(intelligibility = 0.8f, stressCorrect = true)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentStressCorrect_areNotEqual() {
        val a = makeSpeechAnalysis(stressCorrect = true)
        val b = makeSpeechAnalysis(stressCorrect = false)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentFluency_areNotEqual() {
        val a = makeSpeechAnalysis(fluency = 0.5f)
        val b = makeSpeechAnalysis(fluency = 0.9f)
        assertNotEquals(a, b)
    }
}
