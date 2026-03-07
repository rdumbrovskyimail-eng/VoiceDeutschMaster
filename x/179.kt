// Path: src/test/java/com/voicedeutsch/master/domain/model/progress/OverallProgressTest.kt
package com.voicedeutsch.master.domain.model.progress

import com.voicedeutsch.master.domain.model.user.CefrLevel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OverallProgressTest {

    private fun makeVocabularyProgress(wordsForReview: Int = 0) = VocabularyProgress(
        totalWords = 100,
        byLevel = mapOf(1 to 20, 5 to 30),
        byTopic = emptyMap(),
        activeWords = 30,
        passiveWords = 50,
        wordsForReviewToday = wordsForReview,
    )

    private fun makeGrammarProgress() = GrammarProgress(
        totalRules = 40,
        knownRules = 15,
        byCategory = mapOf("Artikel" to 5),
        rulesForReviewToday = 3,
    )

    private fun makePronunciationProgress() = PronunciationProgress(
        overallScore = 0.75f,
        problemSounds = listOf("ü", "ö"),
        goodSounds = listOf("a", "e"),
        trend = "improving",
    )

    private fun makeBookOverallProgress() = BookOverallProgress(
        currentChapter = 3,
        currentLesson = 2,
        totalChapters = 20,
        totalLessons = 120,
        completionPercentage = 0.15f,
        currentTopic = "Greetings",
    )

    private fun makeOverallProgress(
        level: CefrLevel = CefrLevel.A1,
        subLevel: Int = 1,
        streakDays: Int = 5,
        totalHours: Float = 10.0f,
        totalSessions: Int = 20,
    ) = OverallProgress(
        currentCefrLevel = level,
        currentSubLevel = subLevel,
        vocabularyProgress = makeVocabularyProgress(),
        grammarProgress = makeGrammarProgress(),
        pronunciationProgress = makePronunciationProgress(),
        bookProgress = makeBookOverallProgress(),
        streakDays = streakDays,
        totalHours = totalHours,
        totalSessions = totalSessions,
    )

    @Test
    fun creation_withAllFields_setsValues() {
        val progress = makeOverallProgress()
        assertEquals(CefrLevel.A1, progress.currentCefrLevel)
        assertEquals(1, progress.currentSubLevel)
        assertEquals(5, progress.streakDays)
        assertEquals(10.0f, progress.totalHours)
        assertEquals(20, progress.totalSessions)
    }

    @Test
    fun creation_vocabularyProgressIsStored() {
        val progress = makeOverallProgress()
        assertEquals(100, progress.vocabularyProgress.totalWords)
        assertEquals(30, progress.vocabularyProgress.activeWords)
    }

    @Test
    fun creation_grammarProgressIsStored() {
        val progress = makeOverallProgress()
        assertEquals(40, progress.grammarProgress.totalRules)
        assertEquals(15, progress.grammarProgress.knownRules)
    }

    @Test
    fun creation_pronunciationProgressIsStored() {
        val progress = makeOverallProgress()
        assertEquals(0.75f, progress.pronunciationProgress.overallScore)
        assertEquals("improving", progress.pronunciationProgress.trend)
    }

    @Test
    fun creation_bookProgressIsStored() {
        val progress = makeOverallProgress()
        assertEquals(3, progress.bookProgress.currentChapter)
        assertEquals(0.15f, progress.bookProgress.completionPercentage)
    }

    @Test
    fun copy_changesStreakDays_restUnchanged() {
        val original = makeOverallProgress(streakDays = 5)
        val copy = original.copy(streakDays = 10)
        assertEquals(10, copy.streakDays)
        assertEquals(CefrLevel.A1, copy.currentCefrLevel)
        assertEquals(10.0f, copy.totalHours)
    }

    @Test
    fun copy_changesCefrLevel() {
        val original = makeOverallProgress(level = CefrLevel.A1)
        val copy = original.copy(currentCefrLevel = CefrLevel.B1)
        assertEquals(CefrLevel.B1, copy.currentCefrLevel)
    }

    @Test
    fun copy_changesTotalSessions() {
        val original = makeOverallProgress(totalSessions = 20)
        val copy = original.copy(totalSessions = 50)
        assertEquals(50, copy.totalSessions)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeOverallProgress()
        val b = makeOverallProgress()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentSubLevel_areNotEqual() {
        val a = makeOverallProgress(subLevel = 1)
        val b = makeOverallProgress(subLevel = 2)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentTotalHours_areNotEqual() {
        val a = makeOverallProgress(totalHours = 10.0f)
        val b = makeOverallProgress(totalHours = 20.0f)
        assertNotEquals(a, b)
    }

    @Test
    fun creation_zeroStreakAndSessions_isValid() {
        val progress = makeOverallProgress(streakDays = 0, totalSessions = 0)
        assertEquals(0, progress.streakDays)
        assertEquals(0, progress.totalSessions)
    }
}
