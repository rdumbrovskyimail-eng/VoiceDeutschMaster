// Путь: src/test/java/com/voicedeutsch/master/voicecore/strategy/StrategySelectorTest.kt
package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.util.Constants
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StrategySelectorTest {

    private lateinit var selector: StrategySelector

    @BeforeEach
    fun setUp() {
        selector = StrategySelector()
    }

    // ── Snapshot builder helpers ─────────────────────────────────────────

    private fun snapshot(
        wordsForReviewToday: Int = 0,
        rulesForReviewToday: Int = 0,
        totalWords: Int = 10,
        totalRules: Int = 10,
        weakPoints: List<String> = emptyList(),
        problemSounds: List<String> = emptyList(),
        overallScore: Float = 0.8f,
        currentChapter: Int = 1,
        currentLesson: Int = 1,
    ) = KnowledgeSnapshot(
        vocabulary = KnowledgeSnapshot.VocabularyStats(
            totalWords = totalWords,
            wordsForReviewToday = wordsForReviewToday,
        ),
        grammar = KnowledgeSnapshot.GrammarStats(
            totalRules = totalRules,
            rulesForReviewToday = rulesForReviewToday,
        ),
        weakPoints = weakPoints,
        pronunciation = KnowledgeSnapshot.PronunciationStats(
            problemSounds = problemSounds,
            overallScore = overallScore,
        ),
        bookProgress = KnowledgeSnapshot.BookProgress(
            currentChapter = currentChapter,
            currentLesson = currentLesson,
        ),
    )

    // ── selectStrategy — priority 1: SRS queue ───────────────────────────

    @Test
    fun selectStrategy_srsQueueAboveThreshold_returnsRepetition() {
        val result = selector.selectStrategy(
            snapshot(wordsForReviewToday = Constants.STRATEGY_SRS_QUEUE_THRESHOLD + 1)
        )
        assertEquals(LearningStrategy.REPETITION, result)
    }

    @Test
    fun selectStrategy_srsQueueSplitAboveThreshold_returnsRepetition() {
        val half = Constants.STRATEGY_SRS_QUEUE_THRESHOLD / 2 + 1
        val result = selector.selectStrategy(
            snapshot(wordsForReviewToday = half, rulesForReviewToday = half)
        )
        assertEquals(LearningStrategy.REPETITION, result)
    }

    @Test
    fun selectStrategy_srsQueueExactlyAtThreshold_doesNotReturnRepetition() {
        val result = selector.selectStrategy(
            snapshot(wordsForReviewToday = Constants.STRATEGY_SRS_QUEUE_THRESHOLD)
        )
        assertNotEquals(LearningStrategy.REPETITION, result)
    }

    // ── selectStrategy — priority 2: weak points ─────────────────────────

    @Test
    fun selectStrategy_weakPointsAboveThreshold_returnsGapFilling() {
        val weakPoints = List(Constants.STRATEGY_WEAK_POINTS_THRESHOLD + 1) { "point_$it" }
        val result = selector.selectStrategy(snapshot(weakPoints = weakPoints))
        assertEquals(LearningStrategy.GAP_FILLING, result)
    }

    @Test
    fun selectStrategy_weakPointsExactlyAtThreshold_doesNotReturnGapFilling() {
        val weakPoints = List(Constants.STRATEGY_WEAK_POINTS_THRESHOLD) { "point_$it" }
        val result = selector.selectStrategy(snapshot(weakPoints = weakPoints))
        assertNotEquals(LearningStrategy.GAP_FILLING, result)
    }

    @Test
    fun selectStrategy_srsQueueTakesPriorityOverWeakPoints() {
        val weakPoints = List(Constants.STRATEGY_WEAK_POINTS_THRESHOLD + 1) { "point_$it" }
        val result = selector.selectStrategy(
            snapshot(
                wordsForReviewToday = Constants.STRATEGY_SRS_QUEUE_THRESHOLD + 1,
                weakPoints = weakPoints,
            )
        )
        assertEquals(LearningStrategy.REPETITION, result)
    }

    // ── selectStrategy — priority 3: vocab leads grammar ─────────────────

    @Test
    fun selectStrategy_vocabFarAheadOfGrammar_returnsGrammarDrill() {
        // ratio = 350/100 = 3.5 → not > 3.5, use 400/100 = 4.0
        val result = selector.selectStrategy(snapshot(totalWords = 400, totalRules = 100))
        assertEquals(LearningStrategy.GRAMMAR_DRILL, result)
    }

    @Test
    fun selectStrategy_vocabRatioExactly35_doesNotReturnGrammarDrill() {
        // ratio = 350/100 = 3.5 → not strictly > 3.5
        val result = selector.selectStrategy(snapshot(totalWords = 350, totalRules = 100))
        assertNotEquals(LearningStrategy.GRAMMAR_DRILL, result)
    }

    // ── selectStrategy — priority 4: grammar leads vocab ─────────────────

    @Test
    fun selectStrategy_grammarFarAheadOfVocab_returnsVocabularyBoost() {
        // ratio = 40/100 = 0.4 → < 0.5
        val result = selector.selectStrategy(snapshot(totalWords = 40, totalRules = 100))
        assertEquals(LearningStrategy.VOCABULARY_BOOST, result)
    }

    @Test
    fun selectStrategy_grammarRatioExactly05_doesNotReturnVocabularyBoost() {
        // ratio = 50/100 = 0.5 → not strictly < 0.5
        val result = selector.selectStrategy(snapshot(totalWords = 50, totalRules = 100))
        assertNotEquals(LearningStrategy.VOCABULARY_BOOST, result)
    }

    // ── selectStrategy — priority 5: pronunciation ────────────────────────

    @Test
    fun selectStrategy_pronunciationProblemsAboveThreshold_returnsPronunciation() {
        val problems = listOf("ü", "ö", "ch", "r") // 4 > threshold of 3
        val result = selector.selectStrategy(snapshot(problemSounds = problems))
        assertEquals(LearningStrategy.PRONUNCIATION, result)
    }

    @Test
    fun selectStrategy_pronunciationProblemsExactlyAtThreshold_doesNotReturnPronunciation() {
        val problems = listOf("ü", "ö", "ch") // exactly 3
        val result = selector.selectStrategy(snapshot(problemSounds = problems))
        assertNotEquals(LearningStrategy.PRONUNCIATION, result)
    }

    // ── selectStrategy — priority 6: default ─────────────────────────────

    @Test
    fun selectStrategy_noConditionsMet_returnsLinearBook() {
        val result = selector.selectStrategy(snapshot())
        assertEquals(LearningStrategy.LINEAR_BOOK, result)
    }

    @Test
    fun selectStrategy_zeroWordsAndRules_coercedToOne_returnsLinearBook() {
        val result = selector.selectStrategy(snapshot(totalWords = 0, totalRules = 0))
        assertEquals(LearningStrategy.LINEAR_BOOK, result)
    }

    // ── recommend ─────────────────────────────────────────────────────────

    @Test
    fun recommend_repetitionPrimary_secondaryIsLinearBook() {
        val rec = selector.recommend(
            snapshot(wordsForReviewToday = Constants.STRATEGY_SRS_QUEUE_THRESHOLD + 1)
        )
        assertEquals(LearningStrategy.REPETITION, rec.primary)
        assertEquals(LearningStrategy.LINEAR_BOOK, rec.secondary)
    }

    @Test
    fun recommend_gapFillingPrimary_secondaryIsRepetition() {
        val weakPoints = List(Constants.STRATEGY_WEAK_POINTS_THRESHOLD + 1) { "p$it" }
        val rec = selector.recommend(snapshot(weakPoints = weakPoints))
        assertEquals(LearningStrategy.GAP_FILLING, rec.primary)
        assertEquals(LearningStrategy.REPETITION, rec.secondary)
    }

    @Test
    fun recommend_grammarDrillPrimary_secondaryIsLinearBook() {
        val rec = selector.recommend(snapshot(totalWords = 400, totalRules = 100))
        assertEquals(LearningStrategy.GRAMMAR_DRILL, rec.primary)
        assertEquals(LearningStrategy.LINEAR_BOOK, rec.secondary)
    }

    @Test
    fun recommend_vocabularyBoostPrimary_secondaryIsLinearBook() {
        val rec = selector.recommend(snapshot(totalWords = 40, totalRules = 100))
        assertEquals(LearningStrategy.VOCABULARY_BOOST, rec.primary)
        assertEquals(LearningStrategy.LINEAR_BOOK, rec.secondary)
    }

    @Test
    fun recommend_pronunciationPrimary_secondaryIsFreePractice() {
        val rec = selector.recommend(snapshot(problemSounds = listOf("ü", "ö", "ch", "r")))
        assertEquals(LearningStrategy.PRONUNCIATION, rec.primary)
        assertEquals(LearningStrategy.FREE_PRACTICE, rec.secondary)
    }

    @Test
    fun recommend_linearBookPrimary_secondaryIsRepetition() {
        val rec = selector.recommend(snapshot())
        assertEquals(LearningStrategy.LINEAR_BOOK, rec.primary)
        assertEquals(LearningStrategy.REPETITION, rec.secondary)
    }

    @Test
    fun recommend_reasonIsNotBlank() {
        val rec = selector.recommend(snapshot())
        assertTrue(rec.reason.isNotBlank())
    }

    @Test
    fun recommend_repetitionReason_containsQueueCount() {
        val snap = snapshot(wordsForReviewToday = 8, rulesForReviewToday = 5)
        // total = 13, must exceed threshold
        val threshold = Constants.STRATEGY_SRS_QUEUE_THRESHOLD
        val totalQueue = 8 + 5
        if (totalQueue > threshold) {
            val rec = selector.recommend(snap)
            assertTrue(rec.reason.contains("13"))
        }
    }

    // ── shouldSwitchStrategy ─────────────────────────────────────────────

    @Test
    fun shouldSwitchStrategy_timeAboveThreshold_returnsTrue() {
        val result = selector.shouldSwitchStrategy(
            currentStrategy = LearningStrategy.LINEAR_BOOK,
            timeOnStrategyMinutes = Constants.STRATEGY_CHANGE_TIME_THRESHOLD_MIN,
            recentErrorRate = 0f,
            isRepetitionQueueDrained = false,
        )
        assertTrue(result)
    }

    @Test
    fun shouldSwitchStrategy_timeBelowThreshold_returnsFalse() {
        val result = selector.shouldSwitchStrategy(
            currentStrategy = LearningStrategy.LINEAR_BOOK,
            timeOnStrategyMinutes = Constants.STRATEGY_CHANGE_TIME_THRESHOLD_MIN - 1,
            recentErrorRate = 0f,
            isRepetitionQueueDrained = false,
        )
        assertFalse(result)
    }

    @Test
    fun shouldSwitchStrategy_highErrorRate_returnsTrue() {
        val result = selector.shouldSwitchStrategy(
            currentStrategy = LearningStrategy.LINEAR_BOOK,
            timeOnStrategyMinutes = 0,
            recentErrorRate = Constants.STRATEGY_ERROR_RATE_THRESHOLD + 0.01f,
            isRepetitionQueueDrained = false,
        )
        assertTrue(result)
    }

    @Test
    fun shouldSwitchStrategy_errorRateExactlyAtThreshold_returnsFalse() {
        val result = selector.shouldSwitchStrategy(
            currentStrategy = LearningStrategy.LINEAR_BOOK,
            timeOnStrategyMinutes = 0,
            recentErrorRate = Constants.STRATEGY_ERROR_RATE_THRESHOLD,
            isRepetitionQueueDrained = false,
        )
        assertFalse(result)
    }

    @Test
    fun shouldSwitchStrategy_repetitionQueueDrained_returnsTrue() {
        val result = selector.shouldSwitchStrategy(
            currentStrategy = LearningStrategy.REPETITION,
            timeOnStrategyMinutes = 0,
            recentErrorRate = 0f,
            isRepetitionQueueDrained = true,
        )
        assertTrue(result)
    }

    @Test
    fun shouldSwitchStrategy_queueDrainedButNotRepetitionStrategy_returnsFalse() {
        val result = selector.shouldSwitchStrategy(
            currentStrategy = LearningStrategy.LINEAR_BOOK,
            timeOnStrategyMinutes = 0,
            recentErrorRate = 0f,
            isRepetitionQueueDrained = true,
        )
        assertFalse(result)
    }

    @Test
    fun shouldSwitchStrategy_noConditionMet_returnsFalse() {
        val result = selector.shouldSwitchStrategy(
            currentStrategy = LearningStrategy.LINEAR_BOOK,
            timeOnStrategyMinutes = 0,
            recentErrorRate = 0f,
            isRepetitionQueueDrained = false,
        )
        assertFalse(result)
    }

    // ── nextStrategy — high error rate ───────────────────────────────────

    @Test
    fun nextStrategy_highErrorRate_returnsFreePractice() {
        val result = selector.nextStrategy(
            currentStrategy = LearningStrategy.LINEAR_BOOK,
            recentErrorRate = Constants.STRATEGY_ERROR_RATE_THRESHOLD + 0.01f,
            snapshot = null,
        )
        assertEquals(LearningStrategy.FREE_PRACTICE, result)
    }

    @Test
    fun nextStrategy_highErrorRateWithSnapshot_stillReturnsFreePractice() {
        val result = selector.nextStrategy(
            currentStrategy = LearningStrategy.LINEAR_BOOK,
            recentErrorRate = Constants.STRATEGY_ERROR_RATE_THRESHOLD + 0.01f,
            snapshot = snapshot(),
        )
        assertEquals(LearningStrategy.FREE_PRACTICE, result)
    }

    // ── nextStrategy — snapshot available ────────────────────────────────

    @Test
    fun nextStrategy_lowErrorRateWithSnapshot_delegatesToSelectStrategy() {
        val snap = snapshot()
        val expected = selector.selectStrategy(snap)
        val result = selector.nextStrategy(
            currentStrategy = LearningStrategy.GRAMMAR_DRILL,
            recentErrorRate = 0f,
            snapshot = snap,
        )
        assertEquals(expected, result)
    }

    // ── nextStrategy — fallback rotation (snapshot = null) ───────────────

    @Test
    fun nextStrategy_repetitionNoSnapshot_returnsLinearBook() {
        val result = selector.nextStrategy(LearningStrategy.REPETITION, 0f, null)
        assertEquals(LearningStrategy.LINEAR_BOOK, result)
    }

    @Test
    fun nextStrategy_linearBookNoSnapshot_returnsFreePractice() {
        val result = selector.nextStrategy(LearningStrategy.LINEAR_BOOK, 0f, null)
        assertEquals(LearningStrategy.FREE_PRACTICE, result)
    }

    @Test
    fun nextStrategy_gapFillingNoSnapshot_returnsLinearBook() {
        val result = selector.nextStrategy(LearningStrategy.GAP_FILLING, 0f, null)
        assertEquals(LearningStrategy.LINEAR_BOOK, result)
    }

    @Test
    fun nextStrategy_grammarDrillNoSnapshot_returnsFreePractice() {
        val result = selector.nextStrategy(LearningStrategy.GRAMMAR_DRILL, 0f, null)
        assertEquals(LearningStrategy.FREE_PRACTICE, result)
    }

    @Test
    fun nextStrategy_vocabularyBoostNoSnapshot_returnsRepetition() {
        val result = selector.nextStrategy(LearningStrategy.VOCABULARY_BOOST, 0f, null)
        assertEquals(LearningStrategy.REPETITION, result)
    }

    @Test
    fun nextStrategy_pronunciationNoSnapshot_returnsFreePractice() {
        val result = selector.nextStrategy(LearningStrategy.PRONUNCIATION, 0f, null)
        assertEquals(LearningStrategy.FREE_PRACTICE, result)
    }

    @Test
    fun nextStrategy_freePracticeNoSnapshot_returnsLinearBook() {
        val result = selector.nextStrategy(LearningStrategy.FREE_PRACTICE, 0f, null)
        assertEquals(LearningStrategy.LINEAR_BOOK, result)
    }

    @Test
    fun nextStrategy_listeningNoSnapshot_returnsFreePractice() {
        val result = selector.nextStrategy(LearningStrategy.LISTENING, 0f, null)
        assertEquals(LearningStrategy.FREE_PRACTICE, result)
    }

    @Test
    fun nextStrategy_assessmentNoSnapshot_returnsLinearBook() {
        val result = selector.nextStrategy(LearningStrategy.ASSESSMENT, 0f, null)
        assertEquals(LearningStrategy.LINEAR_BOOK, result)
    }

    // ── StrategyRecommendation data class ────────────────────────────────

    @Test
    fun strategyRecommendation_equalsAndHashCode() {
        val r1 = StrategySelector.StrategyRecommendation(
            primary = LearningStrategy.LINEAR_BOOK,
            secondary = LearningStrategy.REPETITION,
            reason = "test reason",
        )
        val r2 = StrategySelector.StrategyRecommendation(
            primary = LearningStrategy.LINEAR_BOOK,
            secondary = LearningStrategy.REPETITION,
            reason = "test reason",
        )
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun strategyRecommendation_copy_changesOnlySpecifiedField() {
        val original = StrategySelector.StrategyRecommendation(
            primary = LearningStrategy.LINEAR_BOOK,
            secondary = LearningStrategy.REPETITION,
            reason = "original",
        )
        val copied = original.copy(reason = "updated")
        assertEquals(LearningStrategy.LINEAR_BOOK, copied.primary)
        assertEquals(LearningStrategy.REPETITION, copied.secondary)
        assertEquals("updated", copied.reason)
    }
}
