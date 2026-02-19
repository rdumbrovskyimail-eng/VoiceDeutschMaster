package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.util.Constants

/**
 * Selects the optimal learning strategy from [KnowledgeSnapshot] data.
 *
 * Architecture lines 683-730 (StrategySelector algorithm).
 *
 * Priority order (first match wins):
 *   1. SRS queue > [Constants.STRATEGY_SRS_QUEUE_THRESHOLD]          → REPETITION
 *   2. Weak points > [Constants.STRATEGY_WEAK_POINTS_THRESHOLD]      → GAP_FILLING
 *   3. Vocab << Grammar (skill gap)                                   → VOCABULARY_BOOST
 *   4. Grammar << Vocab (skill gap)                                   → GRAMMAR_DRILL
 *   5. Problem sounds > threshold                                     → PRONUNCIATION
 *   6. Default                                                        → LINEAR_BOOK
 *
 * Mid-session switching:
 *   - > 25 min on same strategy → suggest change
 *   - error rate > 60 %         → suggest lighter strategy
 *   - REPETITION queue drained  → switch to LINEAR_BOOK
 */
class StrategySelector {

    // ── Initial selection (before session start) ──────────────────────────────

    /**
     * Selects the recommended strategy based on the current knowledge snapshot.
     *
     * @param snapshot  Full knowledge state from [BuildKnowledgeSummaryUseCase].
     * @return          The best-fitting [LearningStrategy].
     */
    fun selectStrategy(snapshot: KnowledgeSnapshot): LearningStrategy {
        // 1. SRS queue overflowing → clear repetition debt first
        val totalSrsQueue = snapshot.vocabulary.wordsForReviewToday +
                snapshot.grammar.rulesForReviewToday
        if (totalSrsQueue > Constants.STRATEGY_SRS_QUEUE_THRESHOLD) {
            return LearningStrategy.REPETITION
        }

        // 2. Weak points accumulated → targeted gap-filling
        if (snapshot.weakPoints.size > Constants.STRATEGY_WEAK_POINTS_THRESHOLD) {
            return LearningStrategy.GAP_FILLING
        }

        // 3. Skill gap between vocabulary and grammar
        val vocabTotal   = snapshot.vocabulary.totalWords.coerceAtLeast(1)
        val grammarTotal = snapshot.grammar.totalRules.coerceAtLeast(1)
        val skillRatio   = vocabTotal.toFloat() / grammarTotal.toFloat()

        if (skillRatio > VOCAB_LEAD_RATIO) {
            // Vocabulary is far ahead → grammar needs boosting
            return LearningStrategy.GRAMMAR_DRILL
        }
        if (skillRatio < GRAMMAR_LEAD_RATIO) {
            // Grammar is far ahead → vocabulary needs boosting
            return LearningStrategy.VOCABULARY_BOOST
        }

        // 4. Pronunciation neglected or deteriorating
        val pronunciationProblems = snapshot.pronunciation.problemSounds.size
        if (pronunciationProblems > PRONUNCIATION_PROBLEM_THRESHOLD) {
            return LearningStrategy.PRONUNCIATION
        }

        // 5. Default: continue the book
        return LearningStrategy.LINEAR_BOOK
    }

    /**
     * Returns a [StrategyRecommendation] with a human-readable reason.
     * Used by ContextBuilder to annotate the session context.
     */
    fun recommend(snapshot: KnowledgeSnapshot): StrategyRecommendation {
        val primary = selectStrategy(snapshot)
        val secondary = when (primary) {
            LearningStrategy.REPETITION      -> LearningStrategy.LINEAR_BOOK
            LearningStrategy.GAP_FILLING     -> LearningStrategy.REPETITION
            LearningStrategy.GRAMMAR_DRILL   -> LearningStrategy.LINEAR_BOOK
            LearningStrategy.VOCABULARY_BOOST -> LearningStrategy.LINEAR_BOOK
            LearningStrategy.PRONUNCIATION   -> LearningStrategy.FREE_PRACTICE
            else                             -> LearningStrategy.REPETITION
        }
        val reason = buildReason(primary, snapshot)
        return StrategyRecommendation(primary = primary, secondary = secondary, reason = reason)
    }

    // ── Mid-session switching ─────────────────────────────────────────────────

    /**
     * Determines whether a mid-session strategy switch is warranted.
     *
     * @param currentStrategy           Active strategy.
     * @param timeOnStrategyMinutes     How long the current strategy has been running.
     * @param recentErrorRate           Error rate in the last ~10 exercises (0.0–1.0).
     * @param isRepetitionQueueDrained  True when all due SRS items have been reviewed.
     */
    fun shouldSwitchStrategy(
        currentStrategy:          LearningStrategy,
        timeOnStrategyMinutes:    Int,
        recentErrorRate:          Float,
        isRepetitionQueueDrained: Boolean,
    ): Boolean {
        // Too long on a single strategy → variety to maintain engagement
        if (timeOnStrategyMinutes >= Constants.STRATEGY_CHANGE_TIME_THRESHOLD_MIN) return true

        // User is struggling → ease off, let them breathe
        if (recentErrorRate > Constants.STRATEGY_ERROR_RATE_THRESHOLD) return true

        // Repetition queue is empty → nothing left to repeat
        if (currentStrategy == LearningStrategy.REPETITION && isRepetitionQueueDrained) return true

        return false
    }

    /**
     * Picks the next strategy when a mid-session switch is triggered.
     *
     * @param currentStrategy  Strategy being abandoned.
     * @param recentErrorRate  Current error rate; high rate leads to a lighter strategy.
     * @param snapshot         Fresh knowledge state (may be null if unavailable).
     */
    fun nextStrategy(
        currentStrategy:  LearningStrategy,
        recentErrorRate:  Float,
        snapshot:         KnowledgeSnapshot?,
    ): LearningStrategy {
        // High error rate → free practice to reduce pressure
        if (recentErrorRate > Constants.STRATEGY_ERROR_RATE_THRESHOLD) {
            return LearningStrategy.FREE_PRACTICE
        }

        // Use fresh snapshot if available
        if (snapshot != null) return selectStrategy(snapshot)

        // Fallback rotation
        return when (currentStrategy) {
            LearningStrategy.REPETITION      -> LearningStrategy.LINEAR_BOOK
            LearningStrategy.LINEAR_BOOK     -> LearningStrategy.FREE_PRACTICE
            LearningStrategy.GAP_FILLING     -> LearningStrategy.LINEAR_BOOK
            LearningStrategy.GRAMMAR_DRILL   -> LearningStrategy.FREE_PRACTICE
            LearningStrategy.VOCABULARY_BOOST -> LearningStrategy.REPETITION
            LearningStrategy.PRONUNCIATION   -> LearningStrategy.FREE_PRACTICE
            LearningStrategy.FREE_PRACTICE   -> LearningStrategy.LINEAR_BOOK
            LearningStrategy.LISTENING       -> LearningStrategy.FREE_PRACTICE
            LearningStrategy.ASSESSMENT      -> LearningStrategy.LINEAR_BOOK
        }
    }

    // ── Data classes ──────────────────────────────────────────────────────────

    /**
     * A strategy recommendation with a reason phrase (shown in session context).
     */
    data class StrategyRecommendation(
        val primary:   LearningStrategy,
        val secondary: LearningStrategy,
        val reason:    String,
    )

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * G5 FIX: Every strategy now has its own specific reason message instead
     * of a generic "Продолжение" for the else branch. This gives Gemini and
     * the UI meaningful context for why each strategy was selected.
     */
    private fun buildReason(strategy: LearningStrategy, snapshot: KnowledgeSnapshot): String =
        when (strategy) {
            LearningStrategy.REPETITION ->
                "Накопилось ${snapshot.vocabulary.wordsForReviewToday + snapshot.grammar.rulesForReviewToday} элементов для повторения"
            LearningStrategy.GAP_FILLING ->
                "Обнаружено ${snapshot.weakPoints.size} слабых мест"
            LearningStrategy.GRAMMAR_DRILL ->
                "Словарный запас (${snapshot.vocabulary.totalWords} слов) опережает грамматику (${snapshot.grammar.totalRules} правил)"
            LearningStrategy.VOCABULARY_BOOST ->
                "Грамматика (${snapshot.grammar.totalRules} правил) опережает словарный запас (${snapshot.vocabulary.totalWords} слов)"
            LearningStrategy.PRONUNCIATION ->
                "Проблемы с произношением: ${snapshot.pronunciation.problemSounds.take(3).joinToString(", ")}"
            LearningStrategy.LINEAR_BOOK ->
                "Последовательное прохождение книги — глава ${snapshot.bookProgress.currentChapter}, урок ${snapshot.bookProgress.currentLesson}"
            LearningStrategy.FREE_PRACTICE ->
                "Свободная разговорная практика для закрепления пройденного материала"
            LearningStrategy.LISTENING ->
                "Тренировка восприятия на слух — развитие навыка аудирования"
            LearningStrategy.ASSESSMENT ->
                "Оценка текущего уровня знаний для корректировки программы"
        }

    companion object {
        /** Vocabulary-to-grammar ratio threshold above which GRAMMAR_DRILL is triggered. */
        private const val VOCAB_LEAD_RATIO   = 3.5f
        /** Vocabulary-to-grammar ratio threshold below which VOCABULARY_BOOST is triggered. */
        private const val GRAMMAR_LEAD_RATIO = 0.5f
        /** Number of problem sounds above which PRONUNCIATION is preferred. */
        private const val PRONUNCIATION_PROBLEM_THRESHOLD = 3
    }
}
