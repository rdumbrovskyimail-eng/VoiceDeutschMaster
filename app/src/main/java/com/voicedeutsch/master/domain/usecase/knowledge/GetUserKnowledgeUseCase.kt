package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.repository.KnowledgeRepository

/**
 * Retrieves aggregated user knowledge overview.
 * Used by Dashboard ViewModel and ContextBuilder.
 */
class GetUserKnowledgeUseCase(
    private val knowledgeRepository: KnowledgeRepository
) {

    data class UserKnowledgeOverview(
        val totalWordsEncountered: Int,
        val wordsKnown: Int,
        val wordsActive: Int,
        val wordsMastered: Int,
        val wordsForReviewToday: Int,
        val totalGrammarRules: Int,
        val rulesKnown: Int,
        val rulesForReviewToday: Int,
        val totalPhrases: Int,
        val phrasesKnown: Int,
        val phrasesForReviewToday: Int,
        val averagePronunciationScore: Float,
        val topicDistribution: Map<String, Int>,
        val recentActivity: List<RecentWordActivity>
    )

    data class RecentWordActivity(
        val wordGerman: String,
        val wordRussian: String,
        val knowledgeLevel: Int,
        val lastSeen: Long
    )

    suspend operator fun invoke(userId: String): UserKnowledgeOverview {
        val allWordKnowledge = knowledgeRepository.getAllWordKnowledge(userId)
        val allWords = knowledgeRepository.getAllWords()
        val wordMap = allWords.associateBy { it.id }

        val totalWordsEncountered = allWordKnowledge.count { it.knowledgeLevel > 0 }
        val wordsKnown = allWordKnowledge.count { it.knowledgeLevel >= 4 }
        val wordsActive = allWordKnowledge.count { it.knowledgeLevel >= 5 }
        val wordsMastered = allWordKnowledge.count { it.knowledgeLevel == 7 }
        val wordsForReviewToday = knowledgeRepository.getWordsForReviewCount(userId)

        val allRuleKnowledge = knowledgeRepository.getAllRuleKnowledge(userId)
        val totalGrammarRules = allRuleKnowledge.count { it.knowledgeLevel > 0 }
        val rulesKnown = allRuleKnowledge.count { it.knowledgeLevel >= 4 }
        val rulesForReviewToday = knowledgeRepository.getRulesForReviewCount(userId)

        val allPhraseKnowledge = knowledgeRepository.getAllPhraseKnowledge(userId)
        val totalPhrases = allPhraseKnowledge.count { it.knowledgeLevel > 0 }
        val phrasesKnown = allPhraseKnowledge.count { it.knowledgeLevel >= 4 }
        val phrasesForReviewToday = knowledgeRepository.getPhrasesForReviewCount(userId)

        val averagePronunciationScore = knowledgeRepository.getAveragePronunciationScore(userId)

        val topicDistribution = mutableMapOf<String, Int>()
        for (wk in allWordKnowledge) {
            if (wk.knowledgeLevel >= 4) {
                val word = wordMap[wk.wordId] ?: continue
                topicDistribution[word.topic] = (topicDistribution[word.topic] ?: 0) + 1
            }
        }

        val recentActivity = allWordKnowledge
            .filter { it.lastSeen != null }
            .sortedByDescending { it.lastSeen }
            .take(20)
            .mapNotNull { wk ->
                val word = wordMap[wk.wordId] ?: return@mapNotNull null
                RecentWordActivity(
                    wordGerman = word.german,
                    wordRussian = word.russian,
                    knowledgeLevel = wk.knowledgeLevel,
                    lastSeen = wk.lastSeen ?: 0L
                )
            }

        return UserKnowledgeOverview(
            totalWordsEncountered = totalWordsEncountered,
            wordsKnown = wordsKnown,
            wordsActive = wordsActive,
            wordsMastered = wordsMastered,
            wordsForReviewToday = wordsForReviewToday,
            totalGrammarRules = totalGrammarRules,
            rulesKnown = rulesKnown,
            rulesForReviewToday = rulesForReviewToday,
            totalPhrases = totalPhrases,
            phrasesKnown = phrasesKnown,
            phrasesForReviewToday = phrasesForReviewToday,
            averagePronunciationScore = averagePronunciationScore,
            topicDistribution = topicDistribution,
            recentActivity = recentActivity
        )
    }
}