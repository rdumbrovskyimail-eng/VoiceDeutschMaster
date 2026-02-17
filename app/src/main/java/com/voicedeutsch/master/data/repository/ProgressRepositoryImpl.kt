package com.voicedeutsch.master.data.repository

import com.voicedeutsch.master.data.local.database.dao.BookProgressDao
import com.voicedeutsch.master.data.local.database.dao.GrammarRuleDao
import com.voicedeutsch.master.data.local.database.dao.KnowledgeDao
import com.voicedeutsch.master.data.local.database.dao.ProgressDao
import com.voicedeutsch.master.data.local.database.dao.SessionDao
import com.voicedeutsch.master.data.local.database.dao.UserDao
import com.voicedeutsch.master.data.local.database.dao.WordDao
import com.voicedeutsch.master.data.mapper.ProgressMapper.toDomain
import com.voicedeutsch.master.domain.model.progress.BookOverallProgress
import com.voicedeutsch.master.domain.model.progress.DailyProgress
import com.voicedeutsch.master.domain.model.progress.GrammarProgress
import com.voicedeutsch.master.domain.model.progress.OverallProgress
import com.voicedeutsch.master.domain.model.progress.PronunciationProgress
import com.voicedeutsch.master.domain.model.progress.SkillProgress
import com.voicedeutsch.master.domain.model.progress.TopicProgress
import com.voicedeutsch.master.domain.model.progress.VocabularyProgress
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.domain.repository.ProgressRepository
import com.voicedeutsch.master.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ProgressRepositoryImpl(
    private val knowledgeDao: KnowledgeDao,
    private val wordDao: WordDao,
    private val grammarRuleDao: GrammarRuleDao,
    private val sessionDao: SessionDao,
    private val bookProgressDao: BookProgressDao,
    private val progressDao: ProgressDao,
    private val userDao: UserDao,
    private val json: Json
) : ProgressRepository {

    override suspend fun calculateOverallProgress(userId: String): OverallProgress {
        val user = userDao.getUser(userId)
            ?: throw IllegalStateException("User not found: $userId")
        val vocabProgress = getVocabularyProgress(userId)
        val grammarProg = getGrammarProgress(userId)
        val pronProgress = getPronunciationProgress(userId)

        val currentPosition = bookProgressDao.getCurrentPosition(userId)
        val currentCh = currentPosition?.chapter ?: 1
        val currentLes = currentPosition?.lesson ?: 1
        val completedLessons = bookProgressDao.getCompletedCount(userId)
        val totalLessons = bookProgressDao.getAllProgress(userId).size.coerceAtLeast(1)

        return OverallProgress(
            currentCefrLevel = CefrLevel.fromString(user.cefrLevel),
            currentSubLevel = user.cefrSubLevel,
            vocabularyProgress = vocabProgress,
            grammarProgress = grammarProg,
            pronunciationProgress = pronProgress,
            bookProgress = BookOverallProgress(
                currentChapter = currentCh,
                currentLesson = currentLes,
                totalChapters = 20,
                totalLessons = totalLessons,
                completionPercentage = if (totalLessons > 0) {
                    completedLessons.toFloat() / totalLessons
                } else {
                    0f
                },
                currentTopic = ""
            ),
            streakDays = user.streakDays,
            totalHours = user.totalMinutes / 60f,
            totalSessions = user.totalSessions
        )
    }

    override fun getOverallProgressFlow(userId: String): Flow<OverallProgress> =
        userDao.getUserFlow(userId).map { userEntity ->
            if (userEntity == null) throw IllegalStateException("User not found")
            calculateOverallProgress(userEntity.id)
        }

    override suspend fun calculateCefrLevel(userId: String): Pair<CefrLevel, Int> {
        var confirmedLevel = CefrLevel.A1
        var subLevel = 1

        val allWK = knowledgeDao.getAllWordKnowledge(userId)
        val allRK = knowledgeDao.getAllRuleKnowledge(userId)

        for (level in CefrLevel.entries) {
            val wordsAtLevel = wordDao.getWordsByLevel(level.name)
            val totalWordsAtLevel = wordsAtLevel.size
            if (totalWordsAtLevel == 0) continue

            val wordIdsAtLevel = wordsAtLevel.map { it.id }.toSet()
            val knownAtLevel = allWK.count { wk ->
                wk.wordId in wordIdsAtLevel && wk.knowledgeLevel >= 5
            }
            val vocabScore = knownAtLevel.toFloat() / totalWordsAtLevel

            val rulesAtLevel = grammarRuleDao.getRulesByLevel(level.name)
            val totalRulesAtLevel = rulesAtLevel.size
            val grammarScore = if (totalRulesAtLevel > 0) {
                val ruleIdsAtLevel = rulesAtLevel.map { it.id }.toSet()
                val knownRulesAtLevel = allRK.count { rk ->
                    rk.ruleId in ruleIdsAtLevel && rk.knowledgeLevel >= 4
                }
                knownRulesAtLevel.toFloat() / totalRulesAtLevel
            } else {
                0f
            }

            if (vocabScore >= 0.7f && grammarScore >= 0.6f) {
                confirmedLevel = level
            }
        }

        val nextLevel = CefrLevel.entries.getOrNull(confirmedLevel.ordinal + 1)
        subLevel = if (nextLevel != null) {
            val wordsAtNext = wordDao.getWordsByLevel(nextLevel.name)
            val totalWordsNext = wordsAtNext.size
            if (totalWordsNext > 0) {
                val wordIdsNext = wordsAtNext.map { it.id }.toSet()
                val allWKSnapshot = knowledgeDao.getAllWordKnowledge(userId)
                val knownNext = allWKSnapshot.count { it.wordId in wordIdsNext && it.knowledgeLevel >= 5 }
                val progress = knownNext.toFloat() / totalWordsNext / 0.7f * 10
                progress.toInt().coerceIn(1, 10)
            } else {
                1
            }
        } else {
            10
        }

        return Pair(confirmedLevel, subLevel)
    }

    override suspend fun getVocabularyProgress(userId: String): VocabularyProgress {
        val allWK = knowledgeDao.getAllWordKnowledge(userId)
        val allWords = wordDao.getAllWords()
        val byLevel = allWK.groupBy { it.knowledgeLevel }.mapValues { it.value.size }
        val byTopic = allWords.groupBy { it.topic }.mapValues { (_, words) ->
            val knownInTopic = words.count { w ->
                allWK.any { wk -> wk.wordId == w.id && wk.knowledgeLevel >= 4 }
            }
            TopicProgress(known = knownInTopic, total = words.size)
        }
        val now = DateUtils.nowTimestamp()
        val activeWords = knowledgeDao.getActiveWordsCount(userId)
        val knownWords = knowledgeDao.getKnownWordsCount(userId)

        return VocabularyProgress(
            totalWords = allWK.count { it.knowledgeLevel > 0 },
            byLevel = byLevel,
            byTopic = byTopic,
            activeWords = activeWords,
            passiveWords = (knownWords - activeWords).coerceAtLeast(0),
            wordsForReviewToday = knowledgeDao.getWordsForReviewCount(userId, now)
        )
    }

    override suspend fun getGrammarProgress(userId: String): GrammarProgress {
        val allRules = grammarRuleDao.getAllRules()
        val allRK = knowledgeDao.getAllRuleKnowledge(userId)
        val byCategory = allRules.groupBy { it.category }.mapValues { (_, rules) ->
            rules.count { r ->
                allRK.any { rk -> rk.ruleId == r.id && rk.knowledgeLevel >= 4 }
            }
        }
        val now = DateUtils.nowTimestamp()

        return GrammarProgress(
            totalRules = allRules.size,
            knownRules = knowledgeDao.getKnownRulesCount(userId),
            byCategory = byCategory,
            rulesForReviewToday = knowledgeDao.getRulesForReviewCount(userId, now)
        )
    }

    override suspend fun getPronunciationProgress(userId: String): PronunciationProgress {
        val avgScore = progressDao.getAveragePronunciationScore(userId)
        val problemWords = progressDao.getProblemWordsForPronunciation(userId)

        return PronunciationProgress(
            overallScore = avgScore,
            problemSounds = problemWords.map { it.word },
            goodSounds = emptyList(),
            trend = "stable"
        )
    }

    override suspend fun getSkillProgress(userId: String): SkillProgress {
        val vocab = getVocabularyProgress(userId)
        val grammar = getGrammarProgress(userId)
        val pron = getPronunciationProgress(userId)
        val totalKnown = vocab.activeWords + vocab.passiveWords
        val totalWords = wordDao.getWordCount().coerceAtLeast(1)

        return SkillProgress(
            vocabulary = totalKnown.toFloat() / totalWords,
            grammar = if (grammar.totalRules > 0) {
                grammar.knownRules.toFloat() / grammar.totalRules
            } else {
                0f
            },
            pronunciation = pron.overallScore,
            listening = 0f,
            speaking = 0f
        )
    }

    override suspend fun getWeeklyProgress(userId: String): List<DailyProgress> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val endDate = LocalDate.now().format(formatter)
        val startDate = LocalDate.now().minusDays(6).format(formatter)
        return progressDao.getDailyStatsRange(userId, startDate, endDate).map { it.toDomain() }
    }

    override suspend fun getMonthlyProgress(userId: String): List<DailyProgress> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val endDate = LocalDate.now().format(formatter)
        val startDate = LocalDate.now().minusDays(29).format(formatter)
        return progressDao.getDailyStatsRange(userId, startDate, endDate).map { it.toDomain() }
    }
}