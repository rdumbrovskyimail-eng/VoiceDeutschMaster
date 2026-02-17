package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.MistakeType
import com.voicedeutsch.master.domain.model.speech.PronunciationTrend
import com.voicedeutsch.master.domain.repository.KnowledgeRepository

/**
 * Detects user's weak points across vocabulary, grammar, and pronunciation.
 *
 * Weak point categories:
 *   - Vocabulary: knowledge_level <= 2 AND times_seen >= 5, OR error_rate > 0.5
 *   - Grammar: knowledge_level <= 2 AND times_practiced >= 3
 *   - Pronunciation: score < 0.5 after > 5 attempts, trend STABLE/DECLINING
 *   - Patterns: groups of mistakes on same item >= 3 times
 */
class GetWeakPointsUseCase(
    private val knowledgeRepository: KnowledgeRepository
) {

    data class WeakPoint(
        val description: String,
        val category: String,
        val severity: Float
    )

    suspend operator fun invoke(userId: String, limit: Int = 10): List<WeakPoint> {
        val weakPoints = mutableListOf<WeakPoint>()

        addVocabularyWeakPoints(userId, weakPoints)
        addGrammarWeakPoints(userId, weakPoints)
        addPronunciationWeakPoints(userId, weakPoints)
        addMistakePatternWeakPoints(userId, weakPoints)

        return weakPoints
            .sortedByDescending { it.severity }
            .take(limit)
    }

    private suspend fun addVocabularyWeakPoints(
        userId: String,
        weakPoints: MutableList<WeakPoint>
    ) {
        val problemWords = knowledgeRepository.getProblemWords(userId, 10)
        for ((word, knowledge) in problemWords) {
            val severity = if (knowledge.accuracy > 0f) {
                1f - knowledge.accuracy
            } else {
                1f
            }
            weakPoints.add(
                WeakPoint(
                    description = "Слово '${word.german}' (${word.russian}) — " +
                        "ошибок: ${knowledge.timesIncorrect}, правильных: ${knowledge.timesCorrect}",
                    category = "vocabulary",
                    severity = severity.coerceIn(0f, 1f)
                )
            )
        }
    }

    private suspend fun addGrammarWeakPoints(
        userId: String,
        weakPoints: MutableList<WeakPoint>
    ) {
        val allRuleKnowledge = knowledgeRepository.getAllRuleKnowledge(userId)
        val weakRules = allRuleKnowledge
            .filter { it.knowledgeLevel <= 2 && it.timesPracticed >= 3 }
            .sortedBy { it.accuracy }

        for (rk in weakRules.take(5)) {
            val rule = knowledgeRepository.getGrammarRule(rk.ruleId) ?: continue
            weakPoints.add(
                WeakPoint(
                    description = "Правило '${rule.nameRu}' — уровень ${rk.knowledgeLevel}/7",
                    category = "grammar",
                    severity = (1f - (rk.knowledgeLevel / 7f)).coerceIn(0f, 1f)
                )
            )
        }
    }

    private suspend fun addPronunciationWeakPoints(
        userId: String,
        weakPoints: MutableList<WeakPoint>
    ) {
        val problemSounds = knowledgeRepository.getProblemSounds(userId)
        for (sound in problemSounds) {
            if (sound.currentScore < 0.5f && sound.totalAttempts > 5 &&
                sound.trend != PronunciationTrend.IMPROVING
            ) {
                weakPoints.add(
                    WeakPoint(
                        description = "Звук '${sound.sound}' [${sound.ipa}] — " +
                            "оценка: ${(sound.currentScore * 100).toInt()}%",
                        category = "pronunciation",
                        severity = (1f - sound.currentScore).coerceIn(0f, 1f)
                    )
                )
            }
        }
    }

    private suspend fun addMistakePatternWeakPoints(
        userId: String,
        weakPoints: MutableList<WeakPoint>
    ) {
        val recentMistakes = knowledgeRepository.getMistakes(userId, 50)

        val mistakesByType = recentMistakes.groupBy { it.type }

        for ((type, mistakes) in mistakesByType) {
            val commonPatterns = mistakes
                .groupBy { it.item }
                .filter { it.value.size >= 3 }

            for ((pattern, patternMistakes) in commonPatterns) {
                val categoryName = when (type) {
                    MistakeType.GRAMMAR -> "grammar_pattern"
                    MistakeType.WORD -> "vocabulary_pattern"
                    MistakeType.PRONUNCIATION -> "pronunciation_pattern"
                    MistakeType.PHRASE -> "phrase_pattern"
                }
                weakPoints.add(
                    WeakPoint(
                        description = "Паттерн ошибки: $pattern (${patternMistakes.size} раз)",
                        category = categoryName,
                        severity = (patternMistakes.size / 10f).coerceAtMost(1f)
                    )
                )
            }
        }
    }
}