// Путь: src/test/java/com/voicedeutsch/master/domain/model/progress/ProgressModelsTest.kt
package com.voicedeutsch.master.domain.model.progress

import com.voicedeutsch.master.domain.model.user.CefrLevel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ═══════════════════════════════════════════════════════════════════════════
// TopicProgress
// ═══════════════════════════════════════════════════════════════════════════

class TopicProgressTest {

    // ── Construction ──────────────────────────────────────────────────────

    @Test
    fun constructor_fields_storedCorrectly() {
        val progress = TopicProgress(known = 7, total = 10)
        assertEquals(7, progress.known)
        assertEquals(10, progress.total)
    }

    // ── percentage ────────────────────────────────────────────────────────

    @Test
    fun percentage_zeroTotal_returnsZero() {
        val progress = TopicProgress(known = 0, total = 0)
        assertEquals(0f, progress.percentage, 0.001f)
    }

    @Test
    fun percentage_halfKnown_returnsHalf() {
        val progress = TopicProgress(known = 5, total = 10)
        assertEquals(0.5f, progress.percentage, 0.001f)
    }

    @Test
    fun percentage_allKnown_returnsOne() {
        val progress = TopicProgress(known = 10, total = 10)
        assertEquals(1.0f, progress.percentage, 0.001f)
    }

    @Test
    fun percentage_noneKnown_returnsZero() {
        val progress = TopicProgress(known = 0, total = 10)
        assertEquals(0f, progress.percentage, 0.001f)
    }

    @Test
    fun percentage_oneKnownOfLarge_returnsFractional() {
        val progress = TopicProgress(known = 1, total = 4)
        assertEquals(0.25f, progress.percentage, 0.001f)
    }

    @Test
    fun percentage_totalIsOne_returnsOne() {
        val progress = TopicProgress(known = 1, total = 1)
        assertEquals(1.0f, progress.percentage, 0.001f)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_changeKnown_percentageRecalculated() {
        val original = TopicProgress(known = 3, total = 10)
        val modified = original.copy(known = 5)
        assertEquals(0.5f, modified.percentage, 0.001f)
        assertEquals(0.3f, original.percentage, 0.001f)
    }

    @Test
    fun copy_changeTotal_onlyTotalChanges() {
        val original = TopicProgress(known = 5, total = 10)
        val modified = original.copy(total = 20)
        assertEquals(5, modified.known)
        assertEquals(20, modified.total)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(TopicProgress(known = 3, total = 10), TopicProgress(known = 3, total = 10))
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(
            TopicProgress(known = 3, total = 10).hashCode(),
            TopicProgress(known = 3, total = 10).hashCode()
        )
    }

    @Test
    fun equals_differentKnown_notEqual() {
        assertNotEquals(TopicProgress(known = 3, total = 10), TopicProgress(known = 4, total = 10))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// VocabularyProgress
// ═══════════════════════════════════════════════════════════════════════════

class VocabularyProgressTest {

    private fun createVocabularyProgress(
        totalWords: Int = 100,
        byLevel: Map<Int, Int> = mapOf(1 to 10, 2 to 20),
        byTopic: Map<String, TopicProgress> = mapOf("food" to TopicProgress(5, 10)),
        activeWords: Int = 40,
        passiveWords: Int = 30,
        wordsForReviewToday: Int = 5
    ) = VocabularyProgress(
        totalWords = totalWords,
        byLevel = byLevel,
        byTopic = byTopic,
        activeWords = activeWords,
        passiveWords = passiveWords,
        wordsForReviewToday = wordsForReviewToday
    )

    @Test
    fun constructor_allFields_storedCorrectly() {
        val byLevel = mapOf(1 to 10, 5 to 40)
        val byTopic = mapOf("travel" to TopicProgress(3, 6))
        val progress = createVocabularyProgress(
            totalWords = 200,
            byLevel = byLevel,
            byTopic = byTopic,
            activeWords = 80,
            passiveWords = 60,
            wordsForReviewToday = 12
        )
        assertEquals(200, progress.totalWords)
        assertEquals(byLevel, progress.byLevel)
        assertEquals(byTopic, progress.byTopic)
        assertEquals(80, progress.activeWords)
        assertEquals(60, progress.passiveWords)
        assertEquals(12, progress.wordsForReviewToday)
    }

    @Test
    fun constructor_emptyMaps_storedCorrectly() {
        val progress = createVocabularyProgress(byLevel = emptyMap(), byTopic = emptyMap())
        assertTrue(progress.byLevel.isEmpty())
        assertTrue(progress.byTopic.isEmpty())
    }

    @Test
    fun copy_changeTotalWords_onlyTotalWordsChanges() {
        val original = createVocabularyProgress(totalWords = 100)
        val modified = original.copy(totalWords = 200)
        assertEquals(200, modified.totalWords)
        assertEquals(original.activeWords, modified.activeWords)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val p1 = createVocabularyProgress(totalWords = 50)
        val p2 = createVocabularyProgress(totalWords = 50)
        assertEquals(p1, p2)
    }

    @Test
    fun equals_differentActiveWords_notEqual() {
        val p1 = createVocabularyProgress(activeWords = 10)
        val p2 = createVocabularyProgress(activeWords = 20)
        assertNotEquals(p1, p2)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GrammarProgress
// ═══════════════════════════════════════════════════════════════════════════

class GrammarProgressTest {

    private fun createGrammarProgress(
        totalRules: Int = 50,
        knownRules: Int = 20,
        byCategory: Map<String, Int> = mapOf("articles" to 5, "verbs" to 10),
        rulesForReviewToday: Int = 3
    ) = GrammarProgress(
        totalRules = totalRules,
        knownRules = knownRules,
        byCategory = byCategory,
        rulesForReviewToday = rulesForReviewToday
    )

    @Test
    fun constructor_allFields_storedCorrectly() {
        val byCategory = mapOf("cases" to 4, "tenses" to 8)
        val progress = createGrammarProgress(
            totalRules = 100,
            knownRules = 45,
            byCategory = byCategory,
            rulesForReviewToday = 7
        )
        assertEquals(100, progress.totalRules)
        assertEquals(45, progress.knownRules)
        assertEquals(byCategory, progress.byCategory)
        assertEquals(7, progress.rulesForReviewToday)
    }

    @Test
    fun constructor_emptyByCategory_storedCorrectly() {
        val progress = createGrammarProgress(byCategory = emptyMap())
        assertTrue(progress.byCategory.isEmpty())
    }

    @Test
    fun copy_changeKnownRules_onlyKnownRulesChanges() {
        val original = createGrammarProgress(knownRules = 10)
        val modified = original.copy(knownRules = 25)
        assertEquals(25, modified.knownRules)
        assertEquals(original.totalRules, modified.totalRules)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createGrammarProgress(knownRules = 10), createGrammarProgress(knownRules = 10))
    }

    @Test
    fun equals_differentTotalRules_notEqual() {
        assertNotEquals(createGrammarProgress(totalRules = 50), createGrammarProgress(totalRules = 100))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PronunciationProgress
// ═══════════════════════════════════════════════════════════════════════════

class PronunciationProgressTest {

    private fun createPronunciationProgress(
        overallScore: Float = 0.75f,
        problemSounds: List<String> = listOf("ü", "ö"),
        goodSounds: List<String> = listOf("a", "e"),
        trend: String = "improving"
    ) = PronunciationProgress(
        overallScore = overallScore,
        problemSounds = problemSounds,
        goodSounds = goodSounds,
        trend = trend
    )

    @Test
    fun constructor_allFields_storedCorrectly() {
        val progress = createPronunciationProgress(
            overallScore = 0.9f,
            problemSounds = listOf("ch", "r"),
            goodSounds = listOf("a", "i", "u"),
            trend = "stable"
        )
        assertEquals(0.9f, progress.overallScore, 0.001f)
        assertEquals(listOf("ch", "r"), progress.problemSounds)
        assertEquals(listOf("a", "i", "u"), progress.goodSounds)
        assertEquals("stable", progress.trend)
    }

    @Test
    fun constructor_emptyLists_storedCorrectly() {
        val progress = createPronunciationProgress(problemSounds = emptyList(), goodSounds = emptyList())
        assertTrue(progress.problemSounds.isEmpty())
        assertTrue(progress.goodSounds.isEmpty())
    }

    @Test
    fun constructor_trendDeclining_storedCorrectly() {
        val progress = createPronunciationProgress(trend = "declining")
        assertEquals("declining", progress.trend)
    }

    @Test
    fun copy_changeTrend_onlyTrendChanges() {
        val original = createPronunciationProgress(trend = "improving")
        val modified = original.copy(trend = "declining")
        assertEquals("declining", modified.trend)
        assertEquals(original.overallScore, modified.overallScore, 0.001f)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createPronunciationProgress(), createPronunciationProgress())
    }

    @Test
    fun equals_differentOverallScore_notEqual() {
        assertNotEquals(
            createPronunciationProgress(overallScore = 0.5f),
            createPronunciationProgress(overallScore = 0.9f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BookOverallProgress
// ═══════════════════════════════════════════════════════════════════════════

class BookOverallProgressTest {

    private fun createBookOverallProgress(
        currentChapter: Int = 3,
        currentLesson: Int = 2,
        totalChapters: Int = 10,
        totalLessons: Int = 50,
        completionPercentage: Float = 0.3f,
        currentTopic: String = "Accusative case"
    ) = BookOverallProgress(
        currentChapter = currentChapter,
        currentLesson = currentLesson,
        totalChapters = totalChapters,
        totalLessons = totalLessons,
        completionPercentage = completionPercentage,
        currentTopic = currentTopic
    )

    @Test
    fun constructor_allFields_storedCorrectly() {
        val progress = createBookOverallProgress(
            currentChapter = 5,
            currentLesson = 10,
            totalChapters = 12,
            totalLessons = 60,
            completionPercentage = 0.65f,
            currentTopic = "Dative"
        )
        assertEquals(5, progress.currentChapter)
        assertEquals(10, progress.currentLesson)
        assertEquals(12, progress.totalChapters)
        assertEquals(60, progress.totalLessons)
        assertEquals(0.65f, progress.completionPercentage, 0.001f)
        assertEquals("Dative", progress.currentTopic)
    }

    @Test
    fun constructor_completionAtZero_storedCorrectly() {
        val progress = createBookOverallProgress(completionPercentage = 0.0f)
        assertEquals(0.0f, progress.completionPercentage, 0.001f)
    }

    @Test
    fun constructor_completionAtOne_storedCorrectly() {
        val progress = createBookOverallProgress(completionPercentage = 1.0f)
        assertEquals(1.0f, progress.completionPercentage, 0.001f)
    }

    @Test
    fun copy_advanceChapter_chapterUpdated() {
        val original = createBookOverallProgress(currentChapter = 3)
        val modified = original.copy(currentChapter = 4)
        assertEquals(4, modified.currentChapter)
        assertEquals(original.totalChapters, modified.totalChapters)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createBookOverallProgress(), createBookOverallProgress())
    }

    @Test
    fun equals_differentCurrentChapter_notEqual() {
        assertNotEquals(
            createBookOverallProgress(currentChapter = 1),
            createBookOverallProgress(currentChapter = 2)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DailyProgress
// ═══════════════════════════════════════════════════════════════════════════

class DailyProgressTest {

    private fun createDailyProgress(
        id: String = "dp_1",
        userId: String = "user_1",
        date: String = "2025-01-15",
        sessionsCount: Int = 0,
        totalMinutes: Int = 0,
        wordsLearned: Int = 0,
        wordsReviewed: Int = 0,
        exercisesCompleted: Int = 0,
        exercisesCorrect: Int = 0,
        averageScore: Float = 0f,
        streakMaintained: Boolean = false
    ) = DailyProgress(
        id = id,
        userId = userId,
        date = date,
        sessionsCount = sessionsCount,
        totalMinutes = totalMinutes,
        wordsLearned = wordsLearned,
        wordsReviewed = wordsReviewed,
        exercisesCompleted = exercisesCompleted,
        exercisesCorrect = exercisesCorrect,
        averageScore = averageScore,
        streakMaintained = streakMaintained
    )

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val dp = DailyProgress(id = "dp_1", userId = "u1", date = "2025-01-15")
        assertEquals(0, dp.sessionsCount)
        assertEquals(0, dp.totalMinutes)
        assertEquals(0, dp.wordsLearned)
        assertEquals(0, dp.wordsReviewed)
        assertEquals(0, dp.exercisesCompleted)
        assertEquals(0, dp.exercisesCorrect)
        assertEquals(0f, dp.averageScore, 0.001f)
        assertFalse(dp.streakMaintained)
        assertTrue(dp.createdAt > 0)
    }

    @Test
    fun constructor_requiredFields_storedCorrectly() {
        val dp = DailyProgress(id = "dp_42", userId = "u99", date = "2025-06-01")
        assertEquals("dp_42", dp.id)
        assertEquals("u99", dp.userId)
        assertEquals("2025-06-01", dp.date)
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_customValues_storedCorrectly() {
        val dp = createDailyProgress(
            sessionsCount = 3,
            totalMinutes = 45,
            wordsLearned = 15,
            wordsReviewed = 20,
            exercisesCompleted = 10,
            exercisesCorrect = 8,
            averageScore = 0.8f,
            streakMaintained = true
        )
        assertEquals(3, dp.sessionsCount)
        assertEquals(45, dp.totalMinutes)
        assertEquals(15, dp.wordsLearned)
        assertEquals(20, dp.wordsReviewed)
        assertEquals(10, dp.exercisesCompleted)
        assertEquals(8, dp.exercisesCorrect)
        assertEquals(0.8f, dp.averageScore, 0.001f)
        assertTrue(dp.streakMaintained)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_maintainStreak_onlyStreakChanges() {
        val original = createDailyProgress(streakMaintained = false)
        val modified = original.copy(streakMaintained = true)
        assertTrue(modified.streakMaintained)
        assertFalse(original.streakMaintained)
        assertEquals(original.sessionsCount, modified.sessionsCount)
    }

    @Test
    fun copy_incrementWordsLearned_wordsUpdated() {
        val original = createDailyProgress(wordsLearned = 5)
        val modified = original.copy(wordsLearned = 10)
        assertEquals(10, modified.wordsLearned)
        assertEquals(5, original.wordsLearned)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val ts = 1_000_000L
        val dp1 = DailyProgress(id = "dp_1", userId = "u1", date = "2025-01-15", createdAt = ts)
        val dp2 = DailyProgress(id = "dp_1", userId = "u1", date = "2025-01-15", createdAt = ts)
        assertEquals(dp1, dp2)
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        val ts = 1_000_000L
        val dp1 = DailyProgress(id = "dp_1", userId = "u1", date = "2025-01-15", createdAt = ts)
        val dp2 = DailyProgress(id = "dp_1", userId = "u1", date = "2025-01-15", createdAt = ts)
        assertEquals(dp1.hashCode(), dp2.hashCode())
    }

    @Test
    fun equals_differentDate_notEqual() {
        val dp1 = createDailyProgress(date = "2025-01-15")
        val dp2 = createDailyProgress(date = "2025-01-16")
        assertNotEquals(dp1, dp2)
    }

    @Test
    fun equals_differentUserId_notEqual() {
        val dp1 = createDailyProgress(userId = "u1")
        val dp2 = createDailyProgress(userId = "u2")
        assertNotEquals(dp1, dp2)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SkillProgress
// ═══════════════════════════════════════════════════════════════════════════

class SkillProgressTest {

    private fun createSkillProgress(
        vocabulary: Float = 0.5f,
        grammar: Float = 0.6f,
        pronunciation: Float = 0.7f,
        listening: Float = 0.4f,
        speaking: Float = 0.3f
    ) = SkillProgress(
        vocabulary = vocabulary,
        grammar = grammar,
        pronunciation = pronunciation,
        listening = listening,
        speaking = speaking
    )

    @Test
    fun constructor_allFields_storedCorrectly() {
        val progress = createSkillProgress(
            vocabulary = 0.8f,
            grammar = 0.75f,
            pronunciation = 0.6f,
            listening = 0.5f,
            speaking = 0.9f
        )
        assertEquals(0.8f, progress.vocabulary, 0.001f)
        assertEquals(0.75f, progress.grammar, 0.001f)
        assertEquals(0.6f, progress.pronunciation, 0.001f)
        assertEquals(0.5f, progress.listening, 0.001f)
        assertEquals(0.9f, progress.speaking, 0.001f)
    }

    @Test
    fun constructor_allZeros_storedCorrectly() {
        val progress = createSkillProgress(0f, 0f, 0f, 0f, 0f)
        assertEquals(0f, progress.vocabulary, 0.001f)
        assertEquals(0f, progress.grammar, 0.001f)
        assertEquals(0f, progress.pronunciation, 0.001f)
        assertEquals(0f, progress.listening, 0.001f)
        assertEquals(0f, progress.speaking, 0.001f)
    }

    @Test
    fun constructor_allOnes_storedCorrectly() {
        val progress = createSkillProgress(1f, 1f, 1f, 1f, 1f)
        assertEquals(1f, progress.vocabulary, 0.001f)
        assertEquals(1f, progress.grammar, 0.001f)
        assertEquals(1f, progress.pronunciation, 0.001f)
        assertEquals(1f, progress.listening, 0.001f)
        assertEquals(1f, progress.speaking, 0.001f)
    }

    @Test
    fun copy_improveSpeaking_onlySpeakingChanges() {
        val original = createSkillProgress(speaking = 0.3f)
        val modified = original.copy(speaking = 0.8f)
        assertEquals(0.8f, modified.speaking, 0.001f)
        assertEquals(original.vocabulary, modified.vocabulary, 0.001f)
        assertEquals(original.grammar, modified.grammar, 0.001f)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createSkillProgress(), createSkillProgress())
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(createSkillProgress().hashCode(), createSkillProgress().hashCode())
    }

    @Test
    fun equals_differentVocabulary_notEqual() {
        assertNotEquals(createSkillProgress(vocabulary = 0.1f), createSkillProgress(vocabulary = 0.9f))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// OverallProgress
// ═══════════════════════════════════════════════════════════════════════════

class OverallProgressTest {

    private fun createOverallProgress(
        currentCefrLevel: CefrLevel = CefrLevel.A1,
        currentSubLevel: Int = 1,
        streakDays: Int = 5,
        totalHours: Float = 10f,
        totalSessions: Int = 20
    ) = OverallProgress(
        currentCefrLevel = currentCefrLevel,
        currentSubLevel = currentSubLevel,
        vocabularyProgress = VocabularyProgress(
            totalWords = 100,
            byLevel = emptyMap(),
            byTopic = emptyMap(),
            activeWords = 40,
            passiveWords = 30,
            wordsForReviewToday = 5
        ),
        grammarProgress = GrammarProgress(
            totalRules = 50,
            knownRules = 20,
            byCategory = emptyMap(),
            rulesForReviewToday = 3
        ),
        pronunciationProgress = PronunciationProgress(
            overallScore = 0.75f,
            problemSounds = emptyList(),
            goodSounds = emptyList(),
            trend = "stable"
        ),
        bookProgress = BookOverallProgress(
            currentChapter = 2,
            currentLesson = 4,
            totalChapters = 10,
            totalLessons = 50,
            completionPercentage = 0.2f,
            currentTopic = "Nominative"
        ),
        streakDays = streakDays,
        totalHours = totalHours,
        totalSessions = totalSessions
    )

    @Test
    fun constructor_allFields_storedCorrectly() {
        val progress = createOverallProgress(
            currentCefrLevel = CefrLevel.B1,
            currentSubLevel = 4,
            streakDays = 14,
            totalHours = 50f,
            totalSessions = 100
        )
        assertEquals(CefrLevel.B1, progress.currentCefrLevel)
        assertEquals(4, progress.currentSubLevel)
        assertEquals(14, progress.streakDays)
        assertEquals(50f, progress.totalHours, 0.001f)
        assertEquals(100, progress.totalSessions)
        assertNotNull(progress.vocabularyProgress)
        assertNotNull(progress.grammarProgress)
        assertNotNull(progress.pronunciationProgress)
        assertNotNull(progress.bookProgress)
    }

    @Test
    fun copy_advanceCefrLevel_levelUpdated() {
        val original = createOverallProgress(currentCefrLevel = CefrLevel.A1)
        val modified = original.copy(currentCefrLevel = CefrLevel.A2)
        assertEquals(CefrLevel.A2, modified.currentCefrLevel)
        assertEquals(original.streakDays, modified.streakDays)
    }

    @Test
    fun copy_incrementStreakDays_streakUpdated() {
        val original = createOverallProgress(streakDays = 10)
        val modified = original.copy(streakDays = 11)
        assertEquals(11, modified.streakDays)
        assertEquals(10, original.streakDays)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createOverallProgress(), createOverallProgress())
    }

    @Test
    fun equals_differentCefrLevel_notEqual() {
        assertNotEquals(
            createOverallProgress(currentCefrLevel = CefrLevel.A1),
            createOverallProgress(currentCefrLevel = CefrLevel.B2)
        )
    }

    @Test
    fun equals_differentTotalSessions_notEqual() {
        assertNotEquals(
            createOverallProgress(totalSessions = 10),
            createOverallProgress(totalSessions = 20)
        )
    }
}
