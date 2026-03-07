// Path: src/test/java/com/voicedeutsch/master/domain/model/progress/VocabularyAndTopicProgressTest.kt
package com.voicedeutsch.master.domain.model.progress

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ════════════════════════════════════════════════════════════════════════════
// TopicProgress — computed property: percentage
// ════════════════════════════════════════════════════════════════════════════

class TopicProgressTest {

    // ── percentage ───────────────────────────────────────────────────────────

    @Test
    fun percentage_zeroTotal_returnsZero() {
        val tp = TopicProgress(known = 0, total = 0)
        assertEquals(0f, tp.percentage)
    }

    @Test
    fun percentage_zeroKnown_returnsZero() {
        val tp = TopicProgress(known = 0, total = 10)
        assertEquals(0f, tp.percentage)
    }

    @Test
    fun percentage_allKnown_returnsOne() {
        val tp = TopicProgress(known = 10, total = 10)
        assertEquals(1.0f, tp.percentage, 0.001f)
    }

    @Test
    fun percentage_halfKnown_returnsHalf() {
        val tp = TopicProgress(known = 5, total = 10)
        assertEquals(0.5f, tp.percentage, 0.001f)
    }

    @Test
    fun percentage_oneOfThree_returnsCorrect() {
        val tp = TopicProgress(known = 1, total = 3)
        assertEquals(1f / 3f, tp.percentage, 0.001f)
    }

    @Test
    fun percentage_knownGreaterThanTotal_returnsAboveOne() {
        // edge case — should not normally happen, but must not crash
        val tp = TopicProgress(known = 15, total = 10)
        assertEquals(1.5f, tp.percentage, 0.001f)
    }

    // ── data class basics ───────────────────────────────────────────────────

    @Test
    fun creation_withFields_setsValues() {
        val tp = TopicProgress(known = 3, total = 10)
        assertEquals(3, tp.known)
        assertEquals(10, tp.total)
    }

    @Test
    fun copy_changesKnown_restUnchanged() {
        val original = TopicProgress(known = 3, total = 10)
        val copy = original.copy(known = 7)
        assertEquals(7, copy.known)
        assertEquals(10, copy.total)
    }

    @Test
    fun copy_changesTotal() {
        val original = TopicProgress(known = 5, total = 10)
        val copy = original.copy(total = 20)
        assertEquals(20, copy.total)
        assertEquals(5, copy.known)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = TopicProgress(known = 5, total = 10)
        val b = TopicProgress(known = 5, total = 10)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentKnown_areNotEqual() {
        val a = TopicProgress(known = 5, total = 10)
        val b = TopicProgress(known = 6, total = 10)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentTotal_areNotEqual() {
        val a = TopicProgress(known = 5, total = 10)
        val b = TopicProgress(known = 5, total = 20)
        assertNotEquals(a, b)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// VocabularyProgress
// ════════════════════════════════════════════════════════════════════════════

class VocabularyProgressTest {

    private fun makeVocabularyProgress(
        totalWords: Int = 200,
        byLevel: Map<Int, Int> = mapOf(1 to 50, 3 to 30, 7 to 20),
        byTopic: Map<String, TopicProgress> = mapOf(
            "Animals" to TopicProgress(known = 5, total = 10),
        ),
        activeWords: Int = 50,
        passiveWords: Int = 80,
        wordsForReviewToday: Int = 10,
    ) = VocabularyProgress(
        totalWords = totalWords,
        byLevel = byLevel,
        byTopic = byTopic,
        activeWords = activeWords,
        passiveWords = passiveWords,
        wordsForReviewToday = wordsForReviewToday,
    )

    @Test
    fun creation_withAllFields_setsValues() {
        val vp = makeVocabularyProgress()
        assertEquals(200, vp.totalWords)
        assertEquals(50, vp.activeWords)
        assertEquals(80, vp.passiveWords)
        assertEquals(10, vp.wordsForReviewToday)
    }

    @Test
    fun creation_byLevelMap_isStored() {
        val vp = makeVocabularyProgress()
        assertEquals(3, vp.byLevel.size)
        assertEquals(50, vp.byLevel[1])
        assertEquals(20, vp.byLevel[7])
    }

    @Test
    fun creation_byTopicMap_isStored() {
        val vp = makeVocabularyProgress()
        assertEquals(1, vp.byTopic.size)
        assertNotNull(vp.byTopic["Animals"])
        assertEquals(5, vp.byTopic["Animals"]!!.known)
    }

    @Test
    fun creation_emptyByLevel_isValid() {
        val vp = makeVocabularyProgress(byLevel = emptyMap())
        assertTrue(vp.byLevel.isEmpty())
    }

    @Test
    fun creation_emptyByTopic_isValid() {
        val vp = makeVocabularyProgress(byTopic = emptyMap())
        assertTrue(vp.byTopic.isEmpty())
    }

    @Test
    fun creation_zeroTotals_isValid() {
        val vp = makeVocabularyProgress(
            totalWords = 0, activeWords = 0, passiveWords = 0, wordsForReviewToday = 0
        )
        assertEquals(0, vp.totalWords)
        assertEquals(0, vp.activeWords)
    }

    @Test
    fun copy_changesTotalWords_restUnchanged() {
        val original = makeVocabularyProgress(totalWords = 200)
        val copy = original.copy(totalWords = 300)
        assertEquals(300, copy.totalWords)
        assertEquals(50, copy.activeWords)
    }

    @Test
    fun copy_changesWordsForReviewToday() {
        val original = makeVocabularyProgress(wordsForReviewToday = 10)
        val copy = original.copy(wordsForReviewToday = 25)
        assertEquals(25, copy.wordsForReviewToday)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeVocabularyProgress()
        val b = makeVocabularyProgress()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentActiveWords_areNotEqual() {
        val a = makeVocabularyProgress(activeWords = 50)
        val b = makeVocabularyProgress(activeWords = 60)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentTotalWords_areNotEqual() {
        val a = makeVocabularyProgress(totalWords = 100)
        val b = makeVocabularyProgress(totalWords = 200)
        assertNotEquals(a, b)
    }
}
