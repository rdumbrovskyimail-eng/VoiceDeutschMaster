package com.voicedeutsch.master.domain.usecase.learning

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.domain.repository.SessionRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.domain.usecase.knowledge.GetWeakPointsUseCase
import com.voicedeutsch.master.util.Constants
import com.voicedeutsch.master.util.DateUtils
import kotlin.math.abs
import kotlin.math.max

/**
 * Selects the optimal learning strategy based on user data.
 *
 * Priority checks (first match wins):
 *   1. SRS queue > threshold → REPETITION
 *   2. Weak points > threshold → GAP_FILLING
 *   3. Skill gap (vocab vs grammar) > threshold → VOCABULARY_BOOST or GRAMMAR_DRILL
 *   4. Days since pronunciation session > threshold → PRONUNCIATION
 *   5. Default → LINEAR_BOOK
 */
class SelectStrategyUseCase(
    private val knowledgeRepository: KnowledgeRepository,
    private val bookRepository: BookRepository,
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val getWeakPointsUseCase: GetWeakPointsUseCase
) {

    data class StrategyRecommendation(
        val primary: LearningStrategy,
        val secondary: LearningStrategy,
        val reason: String
    )

    companion object {
        private const val SRS_QUEUE_THRESHOLD = 10
        private const val WEAK_POINTS_THRESHOLD = 5
        private const val SKILL_GAP_THRESHOLD = 2
        private const val PRONUNCIATION_GAP_DAYS = 3
    }

    suspend operator fun invoke(userId: String): StrategyRecommendation {
        val wordsForReview = knowledgeRepository.getWordsForReviewCount(userId)
        val rulesForReview = knowledgeRepository.getRulesForReviewCount(userId)
        val totalForReview = wordsForReview + rulesForReview

        if (totalForReview > SRS_QUEUE_THRESHOLD) {
            return StrategyRecommendation(
                primary = LearningStrategy.REPETITION,
                secondary = LearningStrategy.LINEAR_BOOK,
                reason = "Накопилось $totalForReview элементов для повторения"
            )
        }

        val weakPoints = getWeakPointsUseCase(userId)

        if (weakPoints.size > WEAK_POINTS_THRESHOLD) {
            return StrategyRecommendation(
                primary = LearningStrategy.GAP_FILLING,
                secondary = LearningStrategy.LINEAR_BOOK,
                reason = "Обнаружено ${weakPoints.size} слабых мест"
            )
        }

        val skillGapResult = checkSkillGap(userId)
        if (skillGapResult != null) {
            return skillGapResult
        }

        val pronunciationResult = checkPronunciationGap(userId)
        if (pronunciationResult != null) {
            return pronunciationResult
        }

        return StrategyRecommendation(
            primary = LearningStrategy.LINEAR_BOOK,
            secondary = LearningStrategy.REPETITION,
            reason = "Продолжение прохождения книги"
        )
    }

    private suspend fun checkSkillGap(userId: String): StrategyRecommendation? {
        val allWords = knowledgeRepository.getAllWords()
        val knownWordsCount = knowledgeRepository.getKnownWordsCount(userId)
        val totalWordsCount = max(1, allWords.size)

        val allRules = knowledgeRepository.getAllGrammarRules()
        val knownRulesCount = knowledgeRepository.getKnownRulesCount(userId)
        val totalRulesCount = max(1, allRules.size)

        val vocabScore = knownWordsCount.toFloat() / totalWordsCount
        val grammarScore = knownRulesCount.toFloat() / totalRulesCount

        val vocabSubLevel = (vocabScore * 60).toInt()
        val grammarSubLevel = (grammarScore * 60).toInt()
        val gap = abs(vocabSubLevel - grammarSubLevel)

        if (gap > SKILL_GAP_THRESHOLD) {
            return if (vocabSubLevel < grammarSubLevel) {
                StrategyRecommendation(
                    primary = LearningStrategy.VOCABULARY_BOOST,
                    secondary = LearningStrategy.LINEAR_BOOK,
                    reason = "Словарный запас отстаёт от грамматики (разрыв: $gap уровней)"
                )
            } else {
                StrategyRecommendation(
                    primary = LearningStrategy.GRAMMAR_DRILL,
                    secondary = LearningStrategy.LINEAR_BOOK,
                    reason = "Грамматика отстаёт от словарного запаса (разрыв: $gap уровней)"
                )
            }
        }

        return null
    }

    private suspend fun checkPronunciationGap(userId: String): StrategyRecommendation? {
        val now = DateUtils.nowTimestamp()
        val recentSessions = sessionRepository.getRecentSessions(userId, 10)

        val lastPronSession = recentSessions.firstOrNull { session ->
            session.strategiesUsed.any { strategy ->
                strategy.equals("PRONUNCIATION", ignoreCase = true)
            }
        }

        val daysSinceLastPron = if (lastPronSession != null) {
            DateUtils.daysBetween(lastPronSession.startedAt, now)
        } else {
            999L
        }

        if (daysSinceLastPron > PRONUNCIATION_GAP_DAYS) {
            return StrategyRecommendation(
                primary = LearningStrategy.PRONUNCIATION,
                secondary = LearningStrategy.LINEAR_BOOK,
                reason = "Произношение не тренировалось $daysSinceLastPron дней"
            )
        }

        return null
    }
}