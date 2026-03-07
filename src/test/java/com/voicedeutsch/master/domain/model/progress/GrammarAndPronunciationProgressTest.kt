// Path: src/test/java/com/voicedeutsch/master/domain/model/progress/GrammarAndPronunciationProgressTest.kt
package com.voicedeutsch.master.domain.model.progress

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ════════════════════════════════════════════════════════════════════════════
// GrammarProgress
// ════════════════════════════════════════════════════════════════════════════

class GrammarProgressTest {

    private fun makeGrammarProgress(
        totalRules: Int = 50,
        knownRules: Int = 20,
        byCategory: Map<String, Int> = mapOf("Artikel" to 5, "Verb" to 10),
        rulesForReviewToday: Int = 4,
    ) = GrammarProgress(
        totalRules = totalRules,
        knownRules = knownRules,
        byCategory = byCategory,
        rulesForReviewToday = rulesForReviewToday,
    )

    @Test
    fun creation_withAllFields_setsValues() {
        val gp = makeGrammarProgress()
        assertEquals(50, gp.totalRules)
        assertEquals(20, gp.knownRules)
        assertEquals(4, gp.rulesForReviewToday)
    }

    @Test
    fun creation_byCategoryMap_isStored() {
        val gp = makeGrammarProgress()
        assertEquals(2, gp.byCategory.size)
        assertEquals(5, gp.byCategory["Artikel"])
        assertEquals(10, gp.byCategory["Verb"])
    }

    @Test
    fun creation_emptyByCategory_isValid() {
        val gp = makeGrammarProgress(byCategory = emptyMap())
        assertTrue(gp.byCategory.isEmpty())
    }

    @Test
    fun creation_zeroKnownRules_isValid() {
        val gp = makeGrammarProgress(knownRules = 0)
        assertEquals(0, gp.knownRules)
    }

    @Test
    fun creation_knownEqualsTotal_isValid() {
        val gp = makeGrammarProgress(totalRules = 50, knownRules = 50)
        assertEquals(50, gp.totalRules)
        assertEquals(50, gp.knownRules)
    }

    @Test
    fun copy_changesKnownRules_restUnchanged() {
        val original = makeGrammarProgress(knownRules = 20)
        val copy = original.copy(knownRules = 35)
        assertEquals(35, copy.knownRules)
        assertEquals(50, copy.totalRules)
        assertEquals(4, copy.rulesForReviewToday)
    }

    @Test
    fun copy_changesTotalRules() {
        val original = makeGrammarProgress(totalRules = 50)
        val copy = original.copy(totalRules = 80)
        assertEquals(80, copy.totalRules)
    }

    @Test
    fun copy_changesRulesForReviewToday() {
        val original = makeGrammarProgress(rulesForReviewToday = 4)
        val copy = original.copy(rulesForReviewToday = 0)
        assertEquals(0, copy.rulesForReviewToday)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeGrammarProgress()
        val b = makeGrammarProgress()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentKnownRules_areNotEqual() {
        val a = makeGrammarProgress(knownRules = 20)
        val b = makeGrammarProgress(knownRules = 21)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentTotalRules_areNotEqual() {
        val a = makeGrammarProgress(totalRules = 50)
        val b = makeGrammarProgress(totalRules = 60)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentByCategory_areNotEqual() {
        val a = makeGrammarProgress(byCategory = mapOf("Artikel" to 5))
        val b = makeGrammarProgress(byCategory = mapOf("Artikel" to 6))
        assertNotEquals(a, b)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// PronunciationProgress
// ════════════════════════════════════════════════════════════════════════════

class PronunciationProgressTest {

    private fun makePronunciationProgress(
        overallScore: Float = 0.72f,
        problemSounds: List<String> = listOf("ü", "ö"),
        goodSounds: List<String> = listOf("a", "i", "e"),
        trend: String = "stable",
    ) = PronunciationProgress(
        overallScore = overallScore,
        problemSounds = problemSounds,
        goodSounds = goodSounds,
        trend = trend,
    )

    @Test
    fun creation_withAllFields_setsValues() {
        val pp = makePronunciationProgress()
        assertEquals(0.72f, pp.overallScore)
        assertEquals("stable", pp.trend)
    }

    @Test
    fun creation_problemSounds_areStored() {
        val pp = makePronunciationProgress()
        assertEquals(2, pp.problemSounds.size)
        assertTrue(pp.problemSounds.contains("ü"))
        assertTrue(pp.problemSounds.contains("ö"))
    }

    @Test
    fun creation_goodSounds_areStored() {
        val pp = makePronunciationProgress()
        assertEquals(3, pp.goodSounds.size)
        assertTrue(pp.goodSounds.contains("a"))
    }

    @Test
    fun creation_emptyProblemSounds_isValid() {
        val pp = makePronunciationProgress(problemSounds = emptyList())
        assertTrue(pp.problemSounds.isEmpty())
    }

    @Test
    fun creation_emptyGoodSounds_isValid() {
        val pp = makePronunciationProgress(goodSounds = emptyList())
        assertTrue(pp.goodSounds.isEmpty())
    }

    @Test
    fun creation_trendImproving_isValid() {
        val pp = makePronunciationProgress(trend = "improving")
        assertEquals("improving", pp.trend)
    }

    @Test
    fun creation_trendDeclining_isValid() {
        val pp = makePronunciationProgress(trend = "declining")
        assertEquals("declining", pp.trend)
    }

    @Test
    fun creation_perfectScore_isOneFloat() {
        val pp = makePronunciationProgress(overallScore = 1.0f)
        assertEquals(1.0f, pp.overallScore)
    }

    @Test
    fun creation_zeroScore_isValid() {
        val pp = makePronunciationProgress(overallScore = 0.0f)
        assertEquals(0.0f, pp.overallScore)
    }

    @Test
    fun copy_changesOverallScore_restUnchanged() {
        val original = makePronunciationProgress(overallScore = 0.5f)
        val copy = original.copy(overallScore = 0.9f)
        assertEquals(0.9f, copy.overallScore)
        assertEquals("stable", copy.trend)
    }

    @Test
    fun copy_changesTrend() {
        val original = makePronunciationProgress(trend = "stable")
        val copy = original.copy(trend = "improving")
        assertEquals("improving", copy.trend)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makePronunciationProgress()
        val b = makePronunciationProgress()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentOverallScore_areNotEqual() {
        val a = makePronunciationProgress(overallScore = 0.7f)
        val b = makePronunciationProgress(overallScore = 0.8f)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentTrend_areNotEqual() {
        val a = makePronunciationProgress(trend = "stable")
        val b = makePronunciationProgress(trend = "improving")
        assertNotEquals(a, b)
    }
}
