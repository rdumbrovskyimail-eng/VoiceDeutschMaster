// Path: src/test/java/com/voicedeutsch/master/domain/model/progress/BookOverallAndSkillProgressTest.kt
package com.voicedeutsch.master.domain.model.progress

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ════════════════════════════════════════════════════════════════════════════
// BookOverallProgress
// ════════════════════════════════════════════════════════════════════════════

class BookOverallProgressTest {

    private fun makeBookOverallProgress(
        currentChapter: Int = 2,
        currentLesson: Int = 3,
        totalChapters: Int = 20,
        totalLessons: Int = 120,
        completionPercentage: Float = 0.10f,
        currentTopic: String = "Greetings",
    ) = BookOverallProgress(
        currentChapter = currentChapter,
        currentLesson = currentLesson,
        totalChapters = totalChapters,
        totalLessons = totalLessons,
        completionPercentage = completionPercentage,
        currentTopic = currentTopic,
    )

    @Test
    fun creation_withAllFields_setsValues() {
        val bp = makeBookOverallProgress()
        assertEquals(2, bp.currentChapter)
        assertEquals(3, bp.currentLesson)
        assertEquals(20, bp.totalChapters)
        assertEquals(120, bp.totalLessons)
        assertEquals(0.10f, bp.completionPercentage)
        assertEquals("Greetings", bp.currentTopic)
    }

    @Test
    fun creation_zeroCompletion_isValid() {
        val bp = makeBookOverallProgress(completionPercentage = 0.0f)
        assertEquals(0.0f, bp.completionPercentage)
    }

    @Test
    fun creation_fullCompletion_isValid() {
        val bp = makeBookOverallProgress(completionPercentage = 1.0f)
        assertEquals(1.0f, bp.completionPercentage)
    }

    @Test
    fun copy_changesCurrentChapter_restUnchanged() {
        val original = makeBookOverallProgress(currentChapter = 2)
        val copy = original.copy(currentChapter = 5)
        assertEquals(5, copy.currentChapter)
        assertEquals(3, copy.currentLesson)
        assertEquals(20, copy.totalChapters)
    }

    @Test
    fun copy_changesCurrentTopic() {
        val original = makeBookOverallProgress(currentTopic = "Greetings")
        val copy = original.copy(currentTopic = "Numbers")
        assertEquals("Numbers", copy.currentTopic)
    }

    @Test
    fun copy_changesCompletionPercentage() {
        val original = makeBookOverallProgress(completionPercentage = 0.10f)
        val copy = original.copy(completionPercentage = 0.50f)
        assertEquals(0.50f, copy.completionPercentage)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeBookOverallProgress()
        val b = makeBookOverallProgress()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentCurrentLesson_areNotEqual() {
        val a = makeBookOverallProgress(currentLesson = 1)
        val b = makeBookOverallProgress(currentLesson = 2)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentCompletionPercentage_areNotEqual() {
        val a = makeBookOverallProgress(completionPercentage = 0.1f)
        val b = makeBookOverallProgress(completionPercentage = 0.5f)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentCurrentTopic_areNotEqual() {
        val a = makeBookOverallProgress(currentTopic = "A")
        val b = makeBookOverallProgress(currentTopic = "B")
        assertNotEquals(a, b)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SkillProgress
// ════════════════════════════════════════════════════════════════════════════

class SkillProgressTest {

    private fun makeSkillProgress(
        vocabulary: Float = 0.7f,
        grammar: Float = 0.6f,
        pronunciation: Float = 0.5f,
        listening: Float = 0.4f,
        speaking: Float = 0.3f,
    ) = SkillProgress(
        vocabulary = vocabulary,
        grammar = grammar,
        pronunciation = pronunciation,
        listening = listening,
        speaking = speaking,
    )

    @Test
    fun creation_withAllFields_setsValues() {
        val sp = makeSkillProgress()
        assertEquals(0.7f, sp.vocabulary)
        assertEquals(0.6f, sp.grammar)
        assertEquals(0.5f, sp.pronunciation)
        assertEquals(0.4f, sp.listening)
        assertEquals(0.3f, sp.speaking)
    }

    @Test
    fun creation_zeroes_isValid() {
        val sp = makeSkillProgress(0f, 0f, 0f, 0f, 0f)
        assertEquals(0f, sp.vocabulary)
        assertEquals(0f, sp.speaking)
    }

    @Test
    fun creation_ones_isValid() {
        val sp = makeSkillProgress(1f, 1f, 1f, 1f, 1f)
        assertEquals(1f, sp.vocabulary)
        assertEquals(1f, sp.speaking)
    }

    @Test
    fun copy_changesVocabulary_restUnchanged() {
        val original = makeSkillProgress(vocabulary = 0.5f)
        val copy = original.copy(vocabulary = 0.9f)
        assertEquals(0.9f, copy.vocabulary)
        assertEquals(0.6f, copy.grammar)
        assertEquals(0.3f, copy.speaking)
    }

    @Test
    fun copy_changesSpeaking() {
        val original = makeSkillProgress(speaking = 0.3f)
        val copy = original.copy(speaking = 0.8f)
        assertEquals(0.8f, copy.speaking)
    }

    @Test
    fun copy_changesListening() {
        val original = makeSkillProgress(listening = 0.4f)
        val copy = original.copy(listening = 0.75f)
        assertEquals(0.75f, copy.listening)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeSkillProgress()
        val b = makeSkillProgress()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentGrammar_areNotEqual() {
        val a = makeSkillProgress(grammar = 0.5f)
        val b = makeSkillProgress(grammar = 0.6f)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentPronunciation_areNotEqual() {
        val a = makeSkillProgress(pronunciation = 0.5f)
        val b = makeSkillProgress(pronunciation = 0.9f)
        assertNotEquals(a, b)
    }

    @Test
    fun allSkills_areIndependent() {
        val sp = SkillProgress(
            vocabulary = 0.1f,
            grammar = 0.2f,
            pronunciation = 0.3f,
            listening = 0.4f,
            speaking = 0.5f,
        )
        assertEquals(0.1f, sp.vocabulary, 0.001f)
        assertEquals(0.2f, sp.grammar, 0.001f)
        assertEquals(0.3f, sp.pronunciation, 0.001f)
        assertEquals(0.4f, sp.listening, 0.001f)
        assertEquals(0.5f, sp.speaking, 0.001f)
    }
}
