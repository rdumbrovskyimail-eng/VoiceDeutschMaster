// Путь: src/test/java/com/voicedeutsch/master/voicecore/prompt/PromptTemplatesTest.kt
package com.voicedeutsch.master.voicecore.prompt

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.BookProgressSnapshot
import com.voicedeutsch.master.domain.model.knowledge.GrammarSnapshot
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.domain.model.knowledge.PronunciationSnapshot
import com.voicedeutsch.master.domain.model.knowledge.RecommendationsSnapshot
import com.voicedeutsch.master.domain.model.knowledge.SessionHistorySnapshot
import com.voicedeutsch.master.domain.model.knowledge.VocabularySnapshot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PromptTemplatesTest {

    // ── Test Fixtures ─────────────────────────────────────────────────────────

    private fun buildSnapshot(
        wordsForReviewToday: Int = 5,
        rulesForReviewToday: Int = 2,
        totalWords: Int = 100,
        totalRules: Int = 30,
        problemRules: List<String> = listOf("Dativ", "Akkusativ", "Genitiv"),
        problemSounds: List<String> = listOf("ü", "ö", "ä"),
        overallScore: Float = 0.75f,
        trend: String = "stable",
        weakPoints: List<String> = listOf("Artikel", "Kasus", "Verben"),
        currentChapter: Int = 3,
        currentLesson: Int = 7,
        completionPercentage: Float = 42.5f,
        currentTopic: String = "Familie und Wohnung",
        byLevel: Map<Int, Int> = mapOf(0 to 5, 1 to 10, 2 to 8, 3 to 20, 4 to 30, 5 to 27),
    ) = KnowledgeSnapshot(
        vocabulary = VocabularySnapshot(
            totalWords          = totalWords,
            byLevel             = byLevel,
            byTopic             = emptyMap(),
            recentNewWords      = emptyList(),
            problemWords        = emptyList(),
            wordsForReviewToday = wordsForReviewToday,
        ),
        grammar = GrammarSnapshot(
            totalRules          = totalRules,
            byLevel             = emptyMap(),
            knownRules          = emptyList(),
            problemRules        = problemRules,
            rulesForReviewToday = rulesForReviewToday,
        ),
        pronunciation = PronunciationSnapshot(
            overallScore      = overallScore,
            problemSounds     = problemSounds,
            goodSounds        = emptyList(),
            averageWordScore  = overallScore,
            trend             = trend,
        ),
        weakPoints = weakPoints,
        bookProgress = BookProgressSnapshot(
            currentChapter      = currentChapter,
            currentLesson       = currentLesson,
            totalChapters       = 10,
            completionPercentage = completionPercentage,
            currentTopic        = currentTopic,
        ),
        sessionHistory = SessionHistorySnapshot(
            lastSession             = "",
            lastSessionSummary      = "",
            averageSessionDuration  = "25 мин",
            streak                  = 0,
            totalSessions           = 0,
        ),
        recommendations = RecommendationsSnapshot(
            primaryStrategy         = "LINEAR_BOOK",
            secondaryStrategy       = "FREE_PRACTICE",
            focusAreas              = emptyList(),
            suggestedSessionDuration = "30 мин",
        ),
    )

    // ── getStrategyPrompt: routing ────────────────────────────────────────────

    @Test
    fun getStrategyPrompt_repetition_containsRepetitionHeader() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.REPETITION, buildSnapshot())
        assertTrue(result.contains("REPETITION"))
    }

    @Test
    fun getStrategyPrompt_linearBook_containsLinearBookHeader() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.LINEAR_BOOK, buildSnapshot())
        assertTrue(result.contains("LINEAR_BOOK"))
    }

    @Test
    fun getStrategyPrompt_freePractice_containsFreePracticeHeader() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.FREE_PRACTICE, buildSnapshot())
        assertTrue(result.contains("FREE_PRACTICE"))
    }

    @Test
    fun getStrategyPrompt_pronunciation_containsPronunciationHeader() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.PRONUNCIATION, buildSnapshot())
        assertTrue(result.contains("PRONUNCIATION"))
    }

    @Test
    fun getStrategyPrompt_gapFilling_containsGapFillingHeader() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.GAP_FILLING, buildSnapshot())
        assertTrue(result.contains("GAP_FILLING"))
    }

    @Test
    fun getStrategyPrompt_grammarDrill_containsGrammarDrillHeader() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.GRAMMAR_DRILL, buildSnapshot())
        assertTrue(result.contains("GRAMMAR_DRILL"))
    }

    @Test
    fun getStrategyPrompt_vocabularyBoost_containsVocabularyBoostHeader() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.VOCABULARY_BOOST, buildSnapshot())
        assertTrue(result.contains("VOCABULARY_BOOST"))
    }

    @Test
    fun getStrategyPrompt_listening_containsListeningHeader() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.LISTENING, buildSnapshot())
        assertTrue(result.contains("LISTENING"))
    }

    @Test
    fun getStrategyPrompt_assessment_containsAssessmentHeader() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.ASSESSMENT, buildSnapshot())
        assertTrue(result.contains("ASSESSMENT"))
    }

    @Test
    fun getStrategyPrompt_allStrategies_returnNonBlankString() {
        val snapshot = buildSnapshot()
        LearningStrategy.entries.forEach { strategy ->
            val result = PromptTemplates.getStrategyPrompt(strategy, snapshot)
            assertTrue(result.isNotBlank(), "Prompt for $strategy must not be blank")
        }
    }

    // ── REPETITION prompt ─────────────────────────────────────────────────────

    @Test
    fun getStrategyPrompt_repetition_containsWordsForReviewCount() {
        val snapshot = buildSnapshot(wordsForReviewToday = 17, rulesForReviewToday = 4)
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.REPETITION, snapshot)
        assertTrue(result.contains("17"))
        assertTrue(result.contains("4"))
    }

    @Test
    fun getStrategyPrompt_repetition_mentionsGetWordsForRepetition() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.REPETITION, buildSnapshot())
        assertTrue(result.contains("get_words_for_repetition"))
    }

    @Test
    fun getStrategyPrompt_repetition_mentionsSaveWordKnowledge() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.REPETITION, buildSnapshot())
        assertTrue(result.contains("save_word_knowledge"))
    }

    @Test
    fun getStrategyPrompt_repetition_mentionsQualityScale() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.REPETITION, buildSnapshot())
        assertTrue(result.contains("0-5"))
    }

    // ── LINEAR_BOOK prompt ────────────────────────────────────────────────────

    @Test
    fun getStrategyPrompt_linearBook_containsCurrentChapterAndLesson() {
        val snapshot = buildSnapshot(currentChapter = 5, currentLesson = 12)
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.LINEAR_BOOK, snapshot)
        assertTrue(result.contains("5"))
        assertTrue(result.contains("12"))
    }

    @Test
    fun getStrategyPrompt_linearBook_containsCompletionPercentage() {
        val snapshot = buildSnapshot(completionPercentage = 67.8f)
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.LINEAR_BOOK, snapshot)
        assertTrue(result.contains("67"))
    }

    @Test
    fun getStrategyPrompt_linearBook_containsCurrentTopic() {
        val snapshot = buildSnapshot(currentTopic = "Im Restaurant")
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.LINEAR_BOOK, snapshot)
        assertTrue(result.contains("Im Restaurant"))
    }

    @Test
    fun getStrategyPrompt_linearBook_mentionsGetCurrentLesson() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.LINEAR_BOOK, buildSnapshot())
        assertTrue(result.contains("get_current_lesson"))
    }

    @Test
    fun getStrategyPrompt_linearBook_mentionsMarkLessonComplete() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.LINEAR_BOOK, buildSnapshot())
        assertTrue(result.contains("mark_lesson_complete"))
    }

    @Test
    fun getStrategyPrompt_linearBook_mentionsAdvanceToNextLesson() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.LINEAR_BOOK, buildSnapshot())
        assertTrue(result.contains("advance_to_next_lesson"))
    }

    // ── FREE_PRACTICE prompt ──────────────────────────────────────────────────

    @Test
    fun getStrategyPrompt_freePractice_containsCurrentTopic() {
        val snapshot = buildSnapshot(currentTopic = "Reise und Urlaub")
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.FREE_PRACTICE, snapshot)
        assertTrue(result.contains("Reise und Urlaub"))
    }

    @Test
    fun getStrategyPrompt_freePractice_mentionsSaveWordKnowledge() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.FREE_PRACTICE, buildSnapshot())
        assertTrue(result.contains("save_word_knowledge"))
    }

    @Test
    fun getStrategyPrompt_freePractice_containsExampleQuestionsForLevels() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.FREE_PRACTICE, buildSnapshot())
        // Sample questions for A2-B2 are present
        assertTrue(result.contains("A2") || result.contains("B1") || result.contains("B2"))
    }

    // ── PRONUNCIATION prompt ──────────────────────────────────────────────────

    @Test
    fun getStrategyPrompt_pronunciation_containsProblemSounds() {
        val snapshot = buildSnapshot(problemSounds = listOf("ü", "ch", "r"))
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.PRONUNCIATION, snapshot)
        assertTrue(result.contains("ü"))
        assertTrue(result.contains("ch"))
    }

    @Test
    fun getStrategyPrompt_pronunciation_limitsToFiveProblemSounds() {
        val snapshot = buildSnapshot(
            problemSounds = listOf("ü", "ö", "ä", "ch", "r", "w", "z")
        )
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.PRONUNCIATION, snapshot)
        // 6th and 7th sounds should not appear (take(5))
        assertFalse(result.contains("w") && result.contains("z") && result.indexOf("w") > 0)
    }

    @Test
    fun getStrategyPrompt_pronunciation_containsOverallScore() {
        val snapshot = buildSnapshot(overallScore = 0.65f)
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.PRONUNCIATION, snapshot)
        assertTrue(result.contains("0.65"))
    }

    @Test
    fun getStrategyPrompt_pronunciation_containsTrend() {
        val snapshot = buildSnapshot(trend = "improving")
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.PRONUNCIATION, snapshot)
        assertTrue(result.contains("improving"))
    }

    @Test
    fun getStrategyPrompt_pronunciation_mentionsSavePronunciationResult() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.PRONUNCIATION, buildSnapshot())
        assertTrue(result.contains("save_pronunciation_result"))
    }

    @Test
    fun getStrategyPrompt_pronunciation_emptyProblemSounds_noException() {
        val snapshot = buildSnapshot(problemSounds = emptyList())
        assertDoesNotThrow {
            PromptTemplates.getStrategyPrompt(LearningStrategy.PRONUNCIATION, snapshot)
        }
    }

    // ── GAP_FILLING prompt ────────────────────────────────────────────────────

    @Test
    fun getStrategyPrompt_gapFilling_containsWeakPoints() {
        val snapshot = buildSnapshot(weakPoints = listOf("Dativ", "Akkusativ", "Plural"))
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.GAP_FILLING, snapshot)
        assertTrue(result.contains("Dativ"))
        assertTrue(result.contains("Akkusativ"))
    }

    @Test
    fun getStrategyPrompt_gapFilling_limitsToFiveWeakPoints() {
        val snapshot = buildSnapshot(
            weakPoints = listOf("P1", "P2", "P3", "P4", "P5", "P6", "P7")
        )
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.GAP_FILLING, snapshot)
        // Only first 5 via take(5)
        assertTrue(result.contains("P5"))
        assertFalse(result.contains("P6"))
        assertFalse(result.contains("P7"))
    }

    @Test
    fun getStrategyPrompt_gapFilling_mentionsGetWeakPoints() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.GAP_FILLING, buildSnapshot())
        assertTrue(result.contains("get_weak_points"))
    }

    @Test
    fun getStrategyPrompt_gapFilling_emptyWeakPoints_noException() {
        val snapshot = buildSnapshot(weakPoints = emptyList())
        assertDoesNotThrow {
            PromptTemplates.getStrategyPrompt(LearningStrategy.GAP_FILLING, snapshot)
        }
    }

    // ── GRAMMAR_DRILL prompt ──────────────────────────────────────────────────

    @Test
    fun getStrategyPrompt_grammarDrill_containsProblemRules() {
        val snapshot = buildSnapshot(problemRules = listOf("Konjunktiv", "Passiv"))
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.GRAMMAR_DRILL, snapshot)
        assertTrue(result.contains("Konjunktiv"))
        assertTrue(result.contains("Passiv"))
    }

    @Test
    fun getStrategyPrompt_grammarDrill_limitsToThreeProblemRules() {
        val snapshot = buildSnapshot(
            problemRules = listOf("R1", "R2", "R3", "R4", "R5")
        )
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.GRAMMAR_DRILL, snapshot)
        assertTrue(result.contains("R3"))
        assertFalse(result.contains("R4"))
        assertFalse(result.contains("R5"))
    }

    @Test
    fun getStrategyPrompt_grammarDrill_containsTotalRulesCount() {
        val snapshot = buildSnapshot(totalRules = 45)
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.GRAMMAR_DRILL, snapshot)
        assertTrue(result.contains("45"))
    }

    @Test
    fun getStrategyPrompt_grammarDrill_containsProblemRulesCount() {
        val snapshot = buildSnapshot(problemRules = List(7) { "Rule_$it" })
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.GRAMMAR_DRILL, snapshot)
        assertTrue(result.contains("7"))
    }

    @Test
    fun getStrategyPrompt_grammarDrill_mentionsSaveRuleKnowledge() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.GRAMMAR_DRILL, buildSnapshot())
        assertTrue(result.contains("save_rule_knowledge"))
    }

    @Test
    fun getStrategyPrompt_grammarDrill_emptyProblemRules_noException() {
        val snapshot = buildSnapshot(problemRules = emptyList())
        assertDoesNotThrow {
            PromptTemplates.getStrategyPrompt(LearningStrategy.GRAMMAR_DRILL, snapshot)
        }
    }

    // ── VOCABULARY_BOOST prompt ───────────────────────────────────────────────

    @Test
    fun getStrategyPrompt_vocabularyBoost_containsCurrentTopic() {
        val snapshot = buildSnapshot(currentTopic = "Arbeit und Beruf")
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.VOCABULARY_BOOST, snapshot)
        assertTrue(result.contains("Arbeit und Beruf"))
    }

    @Test
    fun getStrategyPrompt_vocabularyBoost_containsTotalWordsCount() {
        val snapshot = buildSnapshot(totalWords = 237)
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.VOCABULARY_BOOST, snapshot)
        assertTrue(result.contains("237"))
    }

    @Test
    fun getStrategyPrompt_vocabularyBoost_containsLowLevelWordCount() {
        val snapshot = buildSnapshot(byLevel = mapOf(0 to 3, 1 to 7, 2 to 12))
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.VOCABULARY_BOOST, snapshot)
        // 3 + 7 + 12 = 22
        assertTrue(result.contains("22"))
    }

    @Test
    fun getStrategyPrompt_vocabularyBoost_mentionsSaveWordKnowledge() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.VOCABULARY_BOOST, buildSnapshot())
        assertTrue(result.contains("save_word_knowledge"))
    }

    @Test
    fun getStrategyPrompt_vocabularyBoost_missingByLevelKeys_defaultsToZero() {
        val snapshot = buildSnapshot(byLevel = emptyMap())
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.VOCABULARY_BOOST, snapshot)
        // Should not throw; levels 0+1+2 defaulted to 0+0+0 = 0
        assertTrue(result.contains("0"))
    }

    // ── LISTENING prompt ──────────────────────────────────────────────────────

    @Test
    fun getStrategyPrompt_listening_isNonBlank() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.LISTENING, buildSnapshot())
        assertTrue(result.isNotBlank())
    }

    @Test
    fun getStrategyPrompt_listening_mentionsLevelGuidelines() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.LISTENING, buildSnapshot())
        assertTrue(result.contains("A2") || result.contains("B1"))
    }

    @Test
    fun getStrategyPrompt_listening_containsExampleText() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.LISTENING, buildSnapshot())
        assertTrue(result.contains("Anna") || result.contains("Supermarkt"))
    }

    // ── ASSESSMENT prompt ─────────────────────────────────────────────────────

    @Test
    fun getStrategyPrompt_assessment_isNonBlank() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.ASSESSMENT, buildSnapshot())
        assertTrue(result.isNotBlank())
    }

    @Test
    fun getStrategyPrompt_assessment_mentionsUpdateUserLevel() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.ASSESSMENT, buildSnapshot())
        assertTrue(result.contains("update_user_level"))
    }

    @Test
    fun getStrategyPrompt_assessment_mentionsCefrLevels() {
        val result = PromptTemplates.getStrategyPrompt(LearningStrategy.ASSESSMENT, buildSnapshot())
        assertTrue(result.contains("A1") || result.contains("B2"))
    }

    // ── general quality guarantees ────────────────────────────────────────────

    @Test
    fun getStrategyPrompt_allStrategies_doesNotStartOrEndWithBlankLines() {
        val snapshot = buildSnapshot()
        LearningStrategy.entries.forEach { strategy ->
            val result = PromptTemplates.getStrategyPrompt(strategy, snapshot)
            assertEquals(result.trimIndent(), result,
                "Prompt for $strategy should be trimmed (no leading/trailing whitespace)")
        }
    }

    @Test
    fun getStrategyPrompt_differentSnapshotValues_reflectedInOutput() {
        val snapshot1 = buildSnapshot(wordsForReviewToday = 3)
        val snapshot2 = buildSnapshot(wordsForReviewToday = 99)
        val result1 = PromptTemplates.getStrategyPrompt(LearningStrategy.REPETITION, snapshot1)
        val result2 = PromptTemplates.getStrategyPrompt(LearningStrategy.REPETITION, snapshot2)
        assertNotEquals(result1, result2)
    }
}
