package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.BookProgressSnapshot
import com.voicedeutsch.master.domain.model.knowledge.GrammarSnapshot
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.domain.model.knowledge.KnownRuleInfo
import com.voicedeutsch.master.domain.model.knowledge.ProblemWordInfo
import com.voicedeutsch.master.domain.model.knowledge.PronunciationSnapshot
import com.voicedeutsch.master.domain.model.knowledge.RecommendationsSnapshot
import com.voicedeutsch.master.domain.model.knowledge.SessionHistorySnapshot
import com.voicedeutsch.master.domain.model.knowledge.TopicStats
import com.voicedeutsch.master.domain.model.knowledge.VocabularySnapshot
import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.speech.PronunciationTrend
import com.voicedeutsch.master.domain.model.speech.PhoneticTarget
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.domain.repository.ProgressRepository
import com.voicedeutsch.master.domain.repository.SessionRepository
import com.voicedeutsch.master.util.DateUtils

/**
 * Builds complete knowledge snapshot for Gemini context.
 * This is the "brain" of the system — the resulting KnowledgeSnapshot
 * is serialized to JSON and included in the Gemini system prompt,
 * allowing it to act as a knowledgeable, personalized teacher.
 */
class BuildKnowledgeSummaryUseCase(
    private val knowledgeRepository: KnowledgeRepository,
    private val sessionRepository: SessionRepository,
    private val bookRepository: BookRepository,
    private val progressRepository: ProgressRepository,
    private val getWeakPointsUseCase: GetWeakPointsUseCase
) {

    suspend operator fun invoke(userId: String): KnowledgeSnapshot {
        val vocabularySnapshot = buildVocabularySnapshot(userId)
        val grammarSnapshot = buildGrammarSnapshot(userId)
        val pronunciationSnapshot = buildPronunciationSnapshot(userId)
        val bookProgressSnapshot = buildBookProgressSnapshot(userId)
        val sessionHistorySnapshot = buildSessionHistorySnapshot(userId)
        val weakPoints = buildWeakPoints(userId)
        val recommendations = buildRecommendations(
            vocabularySnapshot, grammarSnapshot, pronunciationSnapshot, bookProgressSnapshot
        )

        return KnowledgeSnapshot(
            vocabulary = vocabularySnapshot,
            grammar = grammarSnapshot,
            pronunciation = pronunciationSnapshot,
            bookProgress = bookProgressSnapshot,
            sessionHistory = sessionHistorySnapshot,
            weakPoints = weakPoints,
            recommendations = recommendations
        )
    }

    private suspend fun buildVocabularySnapshot(userId: String): VocabularySnapshot {
        val allWordKnowledge = knowledgeRepository.getAllWordKnowledge(userId)
        val allWords = knowledgeRepository.getAllWords()
        val wordMap = allWords.associateBy { it.id }

        val wordsByLevel = allWordKnowledge
            .groupBy { it.knowledgeLevel }
            .mapValues { it.value.size }

        val byTopic = mutableMapOf<String, TopicStats>()

        for (wk in allWordKnowledge) {
            val word = wordMap[wk.wordId] ?: continue
            val current = byTopic[word.topic] ?: TopicStats(0, 0)
            byTopic[word.topic] = TopicStats(
                known = current.known + if (wk.knowledgeLevel >= 4) 1 else 0,
                total = current.total + 1
            )
        }

        for (word in allWords) {
            if (allWordKnowledge.none { it.wordId == word.id }) {
                val current = byTopic[word.topic] ?: TopicStats(0, 0)
                byTopic[word.topic] = TopicStats(current.known, current.total + 1)
            }
        }

        val recentNewWords = allWordKnowledge
            .filter { it.knowledgeLevel == 1 }
            .sortedByDescending { it.createdAt }
            .take(10)
            .mapNotNull { wk -> wordMap[wk.wordId]?.german }

        // ✅ FIX (Баг #4): ограничиваем проблемные слова до 5 самых критичных.
        // ИИ не нужен полный словарь — только топ ошибок для фокуса в сессии.
        val problemWordsList = knowledgeRepository.getProblemWords(userId, 5)
        val problemWords = problemWordsList.map { (word, knowledge) ->
            ProblemWordInfo(
                word = word.german,
                level = knowledge.knowledgeLevel,
                attempts = knowledge.timesSeen
            )
        }

        return VocabularySnapshot(
            totalWords = allWordKnowledge.count { it.knowledgeLevel > 0 },
            byLevel = wordsByLevel,
            byTopic = byTopic,
            recentNewWords = recentNewWords,
            problemWords = problemWords,
            wordsForReviewToday = knowledgeRepository.getWordsForReviewCount(userId)
        )
    }

    private suspend fun buildGrammarSnapshot(userId: String): GrammarSnapshot {
        val allRuleKnowledge = knowledgeRepository.getAllRuleKnowledge(userId)
        val allRules = knowledgeRepository.getAllGrammarRules()
        val ruleMap = allRules.associateBy { it.id }

        val rulesByLevel = allRuleKnowledge
            .groupBy { it.knowledgeLevel }
            .mapValues { it.value.size }

        // ✅ FIX (Баг #4): ограничиваем известные правила до 10 последних свежих.
        // ИИ понимает уровень пользователя по срезу, но не тонет в полном списке.
        val knownRules = allRuleKnowledge
            .filter { it.knowledgeLevel > 0 }
            .sortedByDescending { it.lastPracticed } // берём только свежие
            .take(10)
            .mapNotNull { rk ->
                val rule = ruleMap[rk.ruleId]
                rule?.let { KnownRuleInfo(it.nameRu, rk.knowledgeLevel) }
            }

        val problemRules = allRuleKnowledge
            .filter { it.knowledgeLevel <= 2 && it.timesPracticed >= 3 }
            .mapNotNull { rk ->
                ruleMap[rk.ruleId]?.nameRu
            }

        return GrammarSnapshot(
            totalRules = allRuleKnowledge.count { it.knowledgeLevel > 0 },
            byLevel = rulesByLevel,
            knownRules = knownRules,
            problemRules = problemRules,
            rulesForReviewToday = knowledgeRepository.getRulesForReviewCount(userId)
        )
    }

    private suspend fun buildPronunciationSnapshot(userId: String): PronunciationSnapshot {
        val avgScore = knowledgeRepository.getAveragePronunciationScore(userId)
        val phoneticTargets = knowledgeRepository.getProblemSounds(userId)

        return PronunciationSnapshot(
            overallScore = avgScore,
            problemSounds = phoneticTargets
                .filter { it.currentScore < 0.7f }
                .map { it.sound },
            goodSounds = phoneticTargets
                .filter { it.currentScore >= 0.7f }
                .map { it.sound },
            averageWordScore = avgScore,
            trend = determinePronunciationTrend(phoneticTargets)
        )
    }

    private suspend fun buildBookProgressSnapshot(userId: String): BookProgressSnapshot {
        val (currentChapter, currentLesson) = bookRepository.getCurrentBookPosition(userId)
        val metadata = bookRepository.getBookMetadata()
        val completionPct = bookRepository.getBookCompletionPercentage(userId)
        val chapter = bookRepository.getChapter(currentChapter)

        return BookProgressSnapshot(
            currentChapter = currentChapter,
            currentLesson = currentLesson,
            totalChapters = metadata.totalChapters,
            completionPercentage = completionPct,
            currentTopic = chapter?.titleRu ?: ""
        )
    }

    private suspend fun buildSessionHistorySnapshot(userId: String): SessionHistorySnapshot {
        val recentSessions = sessionRepository.getRecentSessions(userId, 5)
        val lastSession = recentSessions.firstOrNull()

        val averageDuration = if (recentSessions.isNotEmpty()) {
            val avg = recentSessions.map { it.durationMinutes }.average()
            "${avg.toInt()} минут"
        } else {
            "0 минут"
        }

        return SessionHistorySnapshot(
            lastSession = lastSession?.let {
                DateUtils.formatRelativeTime(it.startedAt)
            } ?: "никогда",
            lastSessionSummary = lastSession?.sessionSummary ?: "",
            averageSessionDuration = averageDuration,
            streak = sessionRepository.calculateStreak(userId),
            totalSessions = sessionRepository.getSessionCount(userId)
        )
    }

    private suspend fun buildWeakPoints(userId: String): List<String> {
        return getWeakPointsUseCase(userId).map { it.description }
    }

    private fun buildRecommendations(
        vocab: VocabularySnapshot,
        grammar: GrammarSnapshot,
        pronunciation: PronunciationSnapshot,
        book: BookProgressSnapshot
    ): RecommendationsSnapshot {
        val primaryStrategy = determineStrategy(vocab, grammar, pronunciation, book)

        val secondaryStrategy = if (primaryStrategy != LearningStrategy.LINEAR_BOOK) {
            LearningStrategy.LINEAR_BOOK
        } else {
            LearningStrategy.REPETITION
        }

        val focusAreas = mutableListOf<String>()
        if (vocab.wordsForReviewToday > 0) {
            focusAreas.add("Повторение ${vocab.wordsForReviewToday} слов")
        }
        if (grammar.problemRules.isNotEmpty()) {
            focusAreas.add("Проблемные правила: ${grammar.problemRules.take(3).joinToString(", ")}")
        }
        if (pronunciation.problemSounds.isNotEmpty()) {
            focusAreas.add("Звуки: ${pronunciation.problemSounds.take(3).joinToString(", ")}")
        }

        return RecommendationsSnapshot(
            primaryStrategy = primaryStrategy.name,
            secondaryStrategy = secondaryStrategy.name,
            focusAreas = focusAreas.take(3),
            suggestedSessionDuration = "30 минут"
        )
    }

    private fun determineStrategy(
        vocab: VocabularySnapshot,
        grammar: GrammarSnapshot,
        pronunciation: PronunciationSnapshot,
        book: BookProgressSnapshot
    ): LearningStrategy = when {
        vocab.wordsForReviewToday > 10 -> LearningStrategy.REPETITION
        grammar.problemRules.size > 5 -> LearningStrategy.GAP_FILLING
        pronunciation.problemSounds.size > 3 -> LearningStrategy.PRONUNCIATION
        else -> LearningStrategy.LINEAR_BOOK
    }

    private fun determinePronunciationTrend(targets: List<PhoneticTarget>): String {
        if (targets.isEmpty()) return "stable"
        val improving = targets.count { it.trend == PronunciationTrend.IMPROVING }
        val declining = targets.count { it.trend == PronunciationTrend.DECLINING }
        return when {
            improving > declining -> "improving"
            declining > improving -> "declining"
            else -> "stable"
        }
    }
}
