package com.voicedeutsch.master.domain.usecase.user

import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import kotlin.math.min

/**
 * Recalculates and updates the user's CEFR level based on knowledge data.
 *
 * CEFR Level Determination:
 *   For each level (A1→C2):
 *     vocabulary_score = count(words WHERE knowledge_level >= 5) / total_words_for_that_CEFR
 *     grammar_score = count(rules WHERE knowledge_level >= 4) / total_rules_for_that_CEFR
 *     Level confirmed if: vocabulary_score >= 0.7 AND grammar_score >= 0.6
 *
 * Sub-level: interpolation 1-10 based on progress toward next level.
 *
 * Called via Function Call: update_user_level() or automatically after session.
 */
class UpdateUserLevelUseCase(
    private val userRepository: UserRepository,
    private val knowledgeRepository: KnowledgeRepository
) {

    data class LevelUpdateResult(
        val previousLevel: CefrLevel,
        val previousSubLevel: Int,
        val newLevel: CefrLevel,
        val newSubLevel: Int,
        val levelChanged: Boolean,
        val reason: String
    )

    companion object {
        private const val CEFR_VOCAB_THRESHOLD = 0.7f
        private const val CEFR_GRAMMAR_THRESHOLD = 0.6f
    }

    suspend operator fun invoke(userId: String): LevelUpdateResult {
        val profile = userRepository.getUserProfile(userId)
            ?: throw IllegalArgumentException("User $userId not found")

        val previousLevel = profile.cefrLevel
        val previousSubLevel = profile.cefrSubLevel

        var highestConfirmedLevel = CefrLevel.A1
        var vocabScoreForNext = 0f
        var grammarScoreForNext = 0f

        val cefrLevels = CefrLevel.values()

        for (level in cefrLevels) {
            val vocabScore = calculateVocabScore(userId, level)
            val grammarScore = calculateGrammarScore(userId, level)

            if (vocabScore >= CEFR_VOCAB_THRESHOLD && grammarScore >= CEFR_GRAMMAR_THRESHOLD) {
                highestConfirmedLevel = level
            } else {
                vocabScoreForNext = vocabScore
                grammarScoreForNext = grammarScore
                break
            }
        }

        val progressToNext = min(vocabScoreForNext / CEFR_VOCAB_THRESHOLD, grammarScoreForNext / CEFR_GRAMMAR_THRESHOLD)
        val newSubLevel = (progressToNext * 10).toInt().coerceIn(1, 10)

        val levelChanged = highestConfirmedLevel != previousLevel || newSubLevel != previousSubLevel

        if (levelChanged) {
            userRepository.updateUserLevel(userId, highestConfirmedLevel, newSubLevel)
        }

        val reason = when {
            highestConfirmedLevel.ordinal > previousLevel.ordinal ->
                "Уровень повышен! Словарный запас и грамматика подтверждают ${highestConfirmedLevel.name}"
            highestConfirmedLevel.ordinal < previousLevel.ordinal ->
                "Уровень скорректирован на основе текущих знаний"
            newSubLevel > previousSubLevel ->
                "Прогресс внутри уровня ${highestConfirmedLevel.name}: подуровень $newSubLevel/10"
            newSubLevel < previousSubLevel ->
                "Подуровень скорректирован на основе пересчёта"
            else ->
                "Уровень подтверждён: ${highestConfirmedLevel.name} ($newSubLevel/10)"
        }

        return LevelUpdateResult(
            previousLevel = previousLevel,
            previousSubLevel = previousSubLevel,
            newLevel = highestConfirmedLevel,
            newSubLevel = newSubLevel,
            levelChanged = levelChanged,
            reason = reason
        )
    }

    private suspend fun calculateVocabScore(userId: String, level: CefrLevel): Float {
        val wordsForLevel = knowledgeRepository.getWordsByLevel(level)
        if (wordsForLevel.isEmpty()) return 0f

        var activeCount = 0
        for (word in wordsForLevel) {
            val knowledge = knowledgeRepository.getWordKnowledge(userId, word.id)
            if (knowledge != null && knowledge.knowledgeLevel >= 5) {
                activeCount++
            }
        }

        return activeCount.toFloat() / wordsForLevel.size
    }

    private suspend fun calculateGrammarScore(userId: String, level: CefrLevel): Float {
        val rulesForLevel = knowledgeRepository.getGrammarRulesByLevel(level)
        if (rulesForLevel.isEmpty()) return 0f

        var knownCount = 0
        for (rule in rulesForLevel) {
            val knowledge = knowledgeRepository.getRuleKnowledge(userId, rule.id)
            if (knowledge != null && knowledge.knowledgeLevel >= 4) {
                knownCount++
            }
        }

        return knownCount.toFloat() / rulesForLevel.size
    }
}