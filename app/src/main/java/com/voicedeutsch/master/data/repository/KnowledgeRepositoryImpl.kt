package com.voicedeutsch.master.data.repository

import com.voicedeutsch.master.data.local.database.dao.GrammarRuleDao
import com.voicedeutsch.master.data.local.database.dao.KnowledgeDao
import com.voicedeutsch.master.data.local.database.dao.MistakeDao
import com.voicedeutsch.master.data.local.database.dao.PhraseDao
import com.voicedeutsch.master.data.local.database.dao.ProgressDao
import com.voicedeutsch.master.data.local.database.dao.WordDao
import com.voicedeutsch.master.data.local.database.entity.PhraseEntity
import com.voicedeutsch.master.data.mapper.KnowledgeMapper.toDomain
import com.voicedeutsch.master.data.mapper.KnowledgeMapper.toEntity
import com.voicedeutsch.master.data.mapper.ProgressMapper.toDomain
import com.voicedeutsch.master.data.mapper.ProgressMapper.toEntity
import com.voicedeutsch.master.data.mapper.RuleMapper.toDomain
import com.voicedeutsch.master.data.mapper.RuleMapper.toEntity
import com.voicedeutsch.master.data.mapper.WordMapper.toDomain
import com.voicedeutsch.master.data.mapper.WordMapper.toEntity
import com.voicedeutsch.master.domain.model.knowledge.GrammarCategory
import com.voicedeutsch.master.domain.model.knowledge.GrammarRule
import com.voicedeutsch.master.domain.model.knowledge.GrammarSnapshot
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.domain.model.knowledge.KnownRuleInfo
import com.voicedeutsch.master.domain.model.knowledge.MistakeLog
import com.voicedeutsch.master.domain.model.knowledge.MistakeType
import com.voicedeutsch.master.domain.model.knowledge.Phrase
import com.voicedeutsch.master.domain.model.knowledge.PhraseCategory
import com.voicedeutsch.master.domain.model.knowledge.PhraseKnowledge
import com.voicedeutsch.master.domain.model.knowledge.ProblemWordInfo
import com.voicedeutsch.master.domain.model.knowledge.PronunciationSnapshot
import com.voicedeutsch.master.domain.model.knowledge.RecommendationsSnapshot
import com.voicedeutsch.master.domain.model.knowledge.RuleKnowledge
import com.voicedeutsch.master.domain.model.knowledge.SessionHistorySnapshot
import com.voicedeutsch.master.domain.model.knowledge.TopicStats
import com.voicedeutsch.master.domain.model.knowledge.VocabularySnapshot
import com.voicedeutsch.master.domain.model.knowledge.Word
import com.voicedeutsch.master.domain.model.knowledge.WordKnowledge
import com.voicedeutsch.master.domain.model.knowledge.BookProgressSnapshot
import com.voicedeutsch.master.domain.model.speech.PhoneticTarget
import com.voicedeutsch.master.domain.model.speech.PronunciationResult
import com.voicedeutsch.master.domain.model.speech.PronunciationTrend
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class KnowledgeRepositoryImpl(
    private val wordDao: WordDao,
    private val knowledgeDao: KnowledgeDao,
    private val grammarRuleDao: GrammarRuleDao,
    private val phraseDao: PhraseDao,
    private val progressDao: ProgressDao,
    private val mistakeDao: MistakeDao,
    private val json: Json
) : KnowledgeRepository {

    // ==========================================
    // WORDS
    // ==========================================

    override suspend fun getWord(wordId: String): Word? =
        wordDao.getWord(wordId)?.toDomain()

    override suspend fun getWordByGerman(german: String): Word? =
        wordDao.getWordByGerman(german)?.toDomain()

    override suspend fun getAllWords(): List<Word> =
        wordDao.getAllWords().map { it.toDomain() }

    override suspend fun getWordsByTopic(topic: String): List<Word> =
        wordDao.getWordsByTopic(topic).map { it.toDomain() }

    override suspend fun getWordsByLevel(level: CefrLevel): List<Word> =
        wordDao.getWordsByLevel(level.name).map { it.toDomain() }

    override suspend fun getWordsByChapter(chapter: Int): List<Word> =
        wordDao.getWordsByChapter(chapter).map { it.toDomain() }

    override suspend fun insertWord(word: Word) =
        wordDao.insertWord(word.toEntity())

    override suspend fun insertWords(words: List<Word>) =
        wordDao.insertWords(words.map { it.toEntity() })

    override suspend fun searchWords(query: String): List<Word> =
        wordDao.searchWords(query).map { it.toDomain() }

    // ==========================================
    // WORD KNOWLEDGE
    // ==========================================

    override suspend fun getWordKnowledge(userId: String, wordId: String): WordKnowledge? =
        knowledgeDao.getWordKnowledge(userId, wordId)?.toDomain(json)

    override suspend fun getWordKnowledgeByGerman(userId: String, german: String): WordKnowledge? =
        knowledgeDao.getWordKnowledgeByGerman(userId, german)?.toDomain(json)

    override suspend fun getAllWordKnowledge(userId: String): List<WordKnowledge> =
        knowledgeDao.getAllWordKnowledge(userId).map { it.toDomain(json) }

    override fun getWordKnowledgeFlow(userId: String): Flow<List<WordKnowledge>> =
        knowledgeDao.getWordKnowledgeFlow(userId).map { list ->
            list.map { it.toDomain(json) }
        }

    override suspend fun getWordsForReview(userId: String, limit: Int): List<Pair<Word, WordKnowledge>> {
        val now = DateUtils.nowTimestamp()
        val wkEntities = knowledgeDao.getWordsForReview(userId, now, limit)
        return wkEntities.mapNotNull { wke ->
            val word = wordDao.getWord(wke.wordId)?.toDomain() ?: return@mapNotNull null
            Pair(word, wke.toDomain(json))
        }
    }

    override suspend fun getWordsForReviewCount(userId: String): Int =
        knowledgeDao.getWordsForReviewCount(userId, DateUtils.nowTimestamp())

    override suspend fun getKnownWordsCount(userId: String): Int =
        knowledgeDao.getKnownWordsCount(userId)

    override suspend fun getActiveWordsCount(userId: String): Int =
        knowledgeDao.getActiveWordsCount(userId)

    override suspend fun getWordsByKnowledgeLevel(
        userId: String,
        level: Int
    ): List<Pair<Word, WordKnowledge>> {
        val allWK = knowledgeDao.getWordKnowledgeByLevel(userId, level)
        return allWK.mapNotNull { wke ->
            val word = wordDao.getWord(wke.wordId)?.toDomain() ?: return@mapNotNull null
            Pair(word, wke.toDomain(json))
        }
    }

    override suspend fun getProblemWords(userId: String, limit: Int): List<Pair<Word, WordKnowledge>> {
        val problemEntities = knowledgeDao.getProblemWords(userId, limit)
        return problemEntities.mapNotNull { wke ->
            val word = wordDao.getWord(wke.wordId)?.toDomain() ?: return@mapNotNull null
            Pair(word, wke.toDomain(json))
        }
    }

    override suspend fun upsertWordKnowledge(knowledge: WordKnowledge) =
        knowledgeDao.upsertWordKnowledge(knowledge.toEntity(json))

    override suspend fun getWordKnowledgeByTopic(
        userId: String,
        topic: String
    ): Map<Word, WordKnowledge?> {
        val words = wordDao.getWordsByTopic(topic).map { it.toDomain() }
        return words.associateWith { word ->
            knowledgeDao.getWordKnowledge(userId, word.id)?.toDomain(json)
        }
    }

    // ==========================================
    // GRAMMAR RULES
    // ==========================================

    override suspend fun getGrammarRule(ruleId: String): GrammarRule? =
        grammarRuleDao.getRule(ruleId)?.toDomain(json)

    override suspend fun getAllGrammarRules(): List<GrammarRule> =
        grammarRuleDao.getAllRules().map { it.toDomain(json) }

    override suspend fun getGrammarRulesByCategory(category: GrammarCategory): List<GrammarRule> =
        grammarRuleDao.getRulesByCategory(category.name).map { it.toDomain(json) }

    override suspend fun getGrammarRulesByLevel(level: CefrLevel): List<GrammarRule> =
        grammarRuleDao.getRulesByLevel(level.name).map { it.toDomain(json) }

    override suspend fun getGrammarRulesByChapter(chapter: Int): List<GrammarRule> =
        grammarRuleDao.getRulesByChapter(chapter).map { it.toDomain(json) }

    override suspend fun insertGrammarRule(rule: GrammarRule) =
        grammarRuleDao.insertRule(rule.toEntity(json))

    override suspend fun insertGrammarRules(rules: List<GrammarRule>) =
        grammarRuleDao.insertRules(rules.map { it.toEntity(json) })

    // ==========================================
    // RULE KNOWLEDGE
    // ==========================================

    override suspend fun getRuleKnowledge(userId: String, ruleId: String): RuleKnowledge? =
        knowledgeDao.getRuleKnowledge(userId, ruleId)?.toDomain(json)

    override suspend fun getAllRuleKnowledge(userId: String): List<RuleKnowledge> =
        knowledgeDao.getAllRuleKnowledge(userId).map { it.toDomain(json) }

    override suspend fun getRulesForReview(
        userId: String,
        limit: Int
    ): List<Pair<GrammarRule, RuleKnowledge>> {
        val now = DateUtils.nowTimestamp()
        val rkEntities = knowledgeDao.getRulesForReview(userId, now, limit)
        return rkEntities.mapNotNull { rke ->
            val rule = grammarRuleDao.getRule(rke.ruleId)?.toDomain(json) ?: return@mapNotNull null
            Pair(rule, rke.toDomain(json))
        }
    }

    override suspend fun getRulesForReviewCount(userId: String): Int =
        knowledgeDao.getRulesForReviewCount(userId, DateUtils.nowTimestamp())

    override suspend fun getKnownRulesCount(userId: String): Int =
        knowledgeDao.getKnownRulesCount(userId)

    override suspend fun upsertRuleKnowledge(knowledge: RuleKnowledge) =
        knowledgeDao.upsertRuleKnowledge(knowledge.toEntity(json))

    // ==========================================
    // PHRASES
    // ==========================================

    override suspend fun getPhrase(phraseId: String): Phrase? =
        phraseDao.getPhrase(phraseId)?.let { entity ->
            Phrase(
                id = entity.id,
                german = entity.german,
                russian = entity.russian,
                category = try {
                    PhraseCategory.valueOf(entity.category)
                } catch (e: Exception) {
                    PhraseCategory.OTHER
                },
                difficultyLevel = CefrLevel.fromString(entity.difficultyLevel),
                bookChapter = entity.bookChapter,
                bookLesson = entity.bookLesson,
                context = entity.context,
                createdAt = entity.createdAt
            )
        }

    override suspend fun getAllPhrases(): List<Phrase> =
        phraseDao.getAllPhrases().map { entity ->
            Phrase(
                id = entity.id,
                german = entity.german,
                russian = entity.russian,
                category = try {
                    PhraseCategory.valueOf(entity.category)
                } catch (e: Exception) {
                    PhraseCategory.OTHER
                },
                difficultyLevel = CefrLevel.fromString(entity.difficultyLevel),
                bookChapter = entity.bookChapter,
                bookLesson = entity.bookLesson,
                context = entity.context,
                createdAt = entity.createdAt
            )
        }

    override suspend fun insertPhrase(phrase: Phrase) =
        phraseDao.insertPhrase(
            PhraseEntity(
                id = phrase.id,
                german = phrase.german,
                russian = phrase.russian,
                category = phrase.category.name,
                difficultyLevel = phrase.difficultyLevel.name,
                bookChapter = phrase.bookChapter,
                bookLesson = phrase.bookLesson,
                context = phrase.context,
                createdAt = phrase.createdAt
            )
        )

    override suspend fun insertPhrases(phrases: List<Phrase>) =
        phraseDao.insertPhrases(phrases.map { phrase ->
            PhraseEntity(
                id = phrase.id,
                german = phrase.german,
                russian = phrase.russian,
                category = phrase.category.name,
                difficultyLevel = phrase.difficultyLevel.name,
                bookChapter = phrase.bookChapter,
                bookLesson = phrase.bookLesson,
                context = phrase.context,
                createdAt = phrase.createdAt
            )
        })

    // ==========================================
    // PHRASE KNOWLEDGE
    // ==========================================

    override suspend fun getPhraseKnowledge(userId: String, phraseId: String): PhraseKnowledge? =
        knowledgeDao.getPhraseKnowledge(userId, phraseId)?.toDomain()

    override suspend fun getAllPhraseKnowledge(userId: String): List<PhraseKnowledge> =
        knowledgeDao.getAllPhraseKnowledge(userId).map { it.toDomain() }

    override suspend fun getPhrasesForReview(
        userId: String,
        limit: Int
    ): List<Pair<Phrase, PhraseKnowledge>> {
        val now = DateUtils.nowTimestamp()
        val pkEntities = knowledgeDao.getPhrasesForReview(userId, now, limit)
        return pkEntities.mapNotNull { pke ->
            val phrase = getPhrase(pke.phraseId) ?: return@mapNotNull null
            Pair(phrase, pke.toDomain())
        }
    }

    override suspend fun getPhrasesForReviewCount(userId: String): Int =
        knowledgeDao.getPhrasesForReviewCount(userId, DateUtils.nowTimestamp())

    override suspend fun upsertPhraseKnowledge(knowledge: PhraseKnowledge) =
        knowledgeDao.upsertPhraseKnowledge(knowledge.toEntity())

    // ==========================================
    // MISTAKES
    // ==========================================

    override suspend fun logMistake(mistake: MistakeLog) =
        mistakeDao.insertMistake(mistake.toEntity())

    override suspend fun getMistakes(userId: String, limit: Int): List<MistakeLog> =
        mistakeDao.getMistakes(userId, limit).map { it.toDomain() }

    override suspend fun getMistakesByType(userId: String, type: MistakeType): List<MistakeLog> =
        mistakeDao.getMistakesByType(userId, type.name).map { it.toDomain() }

    // ==========================================
    // PRONUNCIATION
    // ==========================================

    override suspend fun savePronunciationResult(result: PronunciationResult) =
        progressDao.insertPronunciationRecord(result.toEntity(json))

    override suspend fun getPronunciationResults(
        userId: String,
        word: String
    ): List<PronunciationResult> =
        progressDao.getPronunciationRecords(userId, word).map { it.toDomain(json) }

    override suspend fun getAveragePronunciationScore(userId: String): Float =
        progressDao.getAveragePronunciationScore(userId)

    override suspend fun getProblemSounds(userId: String): List<PhoneticTarget> {
        val problemWords = progressDao.getProblemWordsForPronunciation(userId)
        val soundMap = mutableMapOf<String, MutableList<Float>>()
        val soundWordsMap = mutableMapOf<String, MutableSet<String>>()

        for (pw in problemWords) {
            val records = progressDao.getPronunciationRecords(userId, pw.word)
            for (record in records) {
                val sounds = try {
                    json.decodeFromString<List<String>>(record.problemSoundsJson)
                } catch (e: Exception) {
                    emptyList()
                }
                for (sound in sounds) {
                    soundMap.getOrPut(sound) { mutableListOf() }.add(record.score)
                    soundWordsMap.getOrPut(sound) { mutableSetOf() }.add(pw.word)
                }
            }
        }

        return soundMap.map { (sound, scores) ->
            val avgScore = if (scores.isNotEmpty()) scores.average().toFloat() else 0f
            val trend = if (scores.size >= 3) {
                val recent = scores.takeLast(3).average()
                val earlier = scores.take(3).average()
                when {
                    recent > earlier + 0.1 -> PronunciationTrend.IMPROVING
                    recent < earlier - 0.1 -> PronunciationTrend.DECLINING
                    else -> PronunciationTrend.STABLE
                }
            } else {
                PronunciationTrend.STABLE
            }

            PhoneticTarget(
                sound = sound,
                ipa = sound,
                detectionDate = DateUtils.nowTimestamp(),
                totalAttempts = scores.size,
                successfulAttempts = scores.count { it >= 0.7f },
                currentScore = avgScore,
                trend = trend,
                lastPracticed = DateUtils.nowTimestamp(),
                inWords = soundWordsMap[sound]?.toList()?.take(5) ?: emptyList()
            )
        }.sortedBy { it.currentScore }
    }

    override suspend fun getPerfectPronunciationCount(userId: String): Int =
        knowledgeDao.getPerfectPronunciationCount(userId)

    override suspend fun recalculateOverdueItems(userId: String) {
        val now = DateUtils.nowTimestamp()
        // Words overdue — already handled by getWordsForReview query
        // This method exists as a hook for future SRS interval recalculation
        // Currently a no-op: SRS intervals are recalculated on-demand when
        // items are reviewed via upsertWordKnowledge / upsertRuleKnowledge
    }

    // ==========================================
    // KNOWLEDGE SNAPSHOT
    // ==========================================

    override suspend fun buildKnowledgeSnapshot(userId: String): KnowledgeSnapshot {
        val allWK = knowledgeDao.getAllWordKnowledge(userId)
        val allWords = wordDao.getAllWords()
        val allRK = knowledgeDao.getAllRuleKnowledge(userId)
        val allRules = grammarRuleDao.getAllRules()
        val now = DateUtils.nowTimestamp()

        // --- Vocabulary Snapshot ---
        val wordsByLevel = allWK.groupBy { it.knowledgeLevel }
            .mapValues { it.value.size }

        val byTopic = allWords.groupBy { it.topic }.mapValues { (_, words) ->
            val knownInTopic = words.count { w ->
                allWK.any { wk -> wk.wordId == w.id && wk.knowledgeLevel >= 4 }
            }
            TopicStats(known = knownInTopic, total = words.size)
        }

        val recentWords = allWK
            .filter { it.lastSeen != null }
            .sortedByDescending { it.lastSeen }
            .take(10)
            .mapNotNull { wk -> allWords.find { it.id == wk.wordId }?.german }

        val problemWordsList = allWK
            .filter { it.knowledgeLevel <= 2 && it.timesSeen >= 3 }
            .sortedBy { it.knowledgeLevel }
            .take(10)
            .map { wk ->
                val word = allWords.find { it.id == wk.wordId }?.german ?: "?"
                ProblemWordInfo(
                    word = word,
                    level = wk.knowledgeLevel,
                    attempts = wk.timesSeen
                )
            }

        val vocabularySnapshot = VocabularySnapshot(
            totalWords = allWK.count { it.knowledgeLevel > 0 },
            byLevel = wordsByLevel,
            byTopic = byTopic,
            recentNewWords = recentWords,
            problemWords = problemWordsList,
            wordsForReviewToday = knowledgeDao.getWordsForReviewCount(userId, now)
        )

        // --- Grammar Snapshot ---
        val rulesByLevel = allRK.groupBy { it.knowledgeLevel }
            .mapValues { it.value.size }

        val knownRulesList = allRK
            .filter { it.knowledgeLevel >= 4 }
            .mapNotNull { rk ->
                val rule = allRules.find { it.id == rk.ruleId }
                rule?.let { KnownRuleInfo(name = it.nameRu, level = rk.knowledgeLevel) }
            }

        val problemRulesList = allRK
            .filter { it.knowledgeLevel <= 2 && it.timesPracticed >= 3 }
            .mapNotNull { rk -> allRules.find { it.id == rk.ruleId }?.nameRu }

        val grammarSnapshot = GrammarSnapshot(
            totalRules = allRules.size,
            byLevel = rulesByLevel,
            knownRules = knownRulesList,
            problemRules = problemRulesList,
            rulesForReviewToday = knowledgeDao.getRulesForReviewCount(userId, now)
        )

        // --- Pronunciation Snapshot ---
        val avgPron = progressDao.getAveragePronunciationScore(userId)
        val problemSoundsResult = getProblemSounds(userId)

        val pronunciationSnapshot = PronunciationSnapshot(
            overallScore = avgPron,
            problemSounds = problemSoundsResult.map { it.sound },
            goodSounds = emptyList(),
            averageWordScore = avgPron,
            trend = if (problemSoundsResult.any { it.trend == PronunciationTrend.IMPROVING }) {
                "improving"
            } else {
                "stable"
            }
        )

        // --- Book Progress Snapshot ---
        val bookProgressSnapshot = BookProgressSnapshot(
            currentChapter = 1,
            currentLesson = 1,
            totalChapters = 20,
            completionPercentage = 0f,
            currentTopic = ""
        )

        // --- Session History Snapshot ---
        val sessionHistorySnapshot = SessionHistorySnapshot(
            lastSession = "",
            lastSessionSummary = "",
            averageSessionDuration = "",
            streak = 0,
            totalSessions = 0
        )

        // --- Weak Points ---
        val weakPointDescriptions = mutableListOf<String>()
        problemWordsList.forEach { pw ->
            weakPointDescriptions.add(
                "Слово '${pw.word}' (уровень ${pw.level}, попыток: ${pw.attempts})"
            )
        }
        problemRulesList.forEach { rule ->
            weakPointDescriptions.add("Грамматика: $rule")
        }
        problemSoundsResult.take(3).forEach { pt ->
            weakPointDescriptions.add(
                "Произношение звука '${pt.sound}' (${(pt.currentScore * 100).toInt()}%)"
            )
        }

        // --- Recommendations ---
        val srsCount = vocabularySnapshot.wordsForReviewToday + grammarSnapshot.rulesForReviewToday
        val primaryStrategy = when {
            srsCount > 10 -> "REPETITION"
            weakPointDescriptions.size > 5 -> "GAP_FILLING"
            else -> "LINEAR_BOOK"
        }

        val recommendationsSnapshot = RecommendationsSnapshot(
            primaryStrategy = primaryStrategy,
            secondaryStrategy = "LINEAR_BOOK",
            focusAreas = weakPointDescriptions.take(3),
            suggestedSessionDuration = "30 мин"
        )

        return KnowledgeSnapshot(
            vocabulary = vocabularySnapshot,
            grammar = grammarSnapshot,
            pronunciation = pronunciationSnapshot,
            bookProgress = bookProgressSnapshot,
            sessionHistory = sessionHistorySnapshot,
            weakPoints = weakPointDescriptions,
            recommendations = recommendationsSnapshot
        )
    }
}
