package com.voicedeutsch.master.domain.repository

import com.voicedeutsch.master.domain.model.knowledge.GrammarCategory
import com.voicedeutsch.master.domain.model.knowledge.GrammarRule
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.domain.model.knowledge.MistakeLog
import com.voicedeutsch.master.domain.model.knowledge.MistakeType
import com.voicedeutsch.master.domain.model.knowledge.Phrase
import com.voicedeutsch.master.domain.model.knowledge.PhraseKnowledge
import com.voicedeutsch.master.domain.model.knowledge.RuleKnowledge
import com.voicedeutsch.master.domain.model.knowledge.Word
import com.voicedeutsch.master.domain.model.knowledge.WordKnowledge
import com.voicedeutsch.master.domain.model.speech.PhoneticTarget
import com.voicedeutsch.master.domain.model.speech.PronunciationResult
import com.voicedeutsch.master.domain.model.user.CefrLevel
import kotlinx.coroutines.flow.Flow

interface KnowledgeRepository {

    // ==========================================
    // WORDS
    // ==========================================

    suspend fun getWord(wordId: String): Word?

    suspend fun getWordByGerman(german: String): Word?

    suspend fun getAllWords(): List<Word>

    suspend fun getWordsByTopic(topic: String): List<Word>

    suspend fun getWordsByLevel(level: CefrLevel): List<Word>

    suspend fun getWordsByChapter(chapter: Int): List<Word>

    suspend fun insertWord(word: Word)

    suspend fun insertWords(words: List<Word>)

    suspend fun searchWords(query: String): List<Word>

    // ==========================================
    // WORD KNOWLEDGE
    // ==========================================

    suspend fun getWordKnowledge(userId: String, wordId: String): WordKnowledge?

    suspend fun getWordKnowledgeByGerman(userId: String, german: String): WordKnowledge?

    suspend fun getAllWordKnowledge(userId: String): List<WordKnowledge>

    fun getWordKnowledgeFlow(userId: String): Flow<List<WordKnowledge>>

    suspend fun getWordsForReview(userId: String, limit: Int): List<Pair<Word, WordKnowledge>>

    suspend fun getWordsForReviewCount(userId: String): Int

    suspend fun getKnownWordsCount(userId: String): Int

    suspend fun getActiveWordsCount(userId: String): Int

    suspend fun getWordsByKnowledgeLevel(userId: String, level: Int): List<Pair<Word, WordKnowledge>>

    suspend fun getProblemWords(userId: String, limit: Int): List<Pair<Word, WordKnowledge>>

    suspend fun upsertWordKnowledge(knowledge: WordKnowledge)

    suspend fun getWordKnowledgeByTopic(userId: String, topic: String): Map<Word, WordKnowledge?>

    // ==========================================
    // GRAMMAR RULES
    // ==========================================

    suspend fun getGrammarRule(ruleId: String): GrammarRule?

    suspend fun getAllGrammarRules(): List<GrammarRule>

    suspend fun getGrammarRulesByCategory(category: GrammarCategory): List<GrammarRule>

    suspend fun getGrammarRulesByLevel(level: CefrLevel): List<GrammarRule>

    suspend fun getGrammarRulesByChapter(chapter: Int): List<GrammarRule>

    suspend fun insertGrammarRule(rule: GrammarRule)

    suspend fun insertGrammarRules(rules: List<GrammarRule>)

    // ==========================================
    // RULE KNOWLEDGE
    // ==========================================

    suspend fun getRuleKnowledge(userId: String, ruleId: String): RuleKnowledge?

    suspend fun getAllRuleKnowledge(userId: String): List<RuleKnowledge>

    suspend fun getRulesForReview(userId: String, limit: Int): List<Pair<GrammarRule, RuleKnowledge>>

    suspend fun getRulesForReviewCount(userId: String): Int

    suspend fun getKnownRulesCount(userId: String): Int

    suspend fun upsertRuleKnowledge(knowledge: RuleKnowledge)

    // ==========================================
    // PHRASES
    // ==========================================

    suspend fun getPhrase(phraseId: String): Phrase?

    suspend fun getAllPhrases(): List<Phrase>

    suspend fun insertPhrase(phrase: Phrase)

    suspend fun insertPhrases(phrases: List<Phrase>)

    // ==========================================
    // PHRASE KNOWLEDGE
    // ==========================================

    suspend fun getPhraseKnowledge(userId: String, phraseId: String): PhraseKnowledge?

    suspend fun getAllPhraseKnowledge(userId: String): List<PhraseKnowledge>

    suspend fun getPhrasesForReview(userId: String, limit: Int): List<Pair<Phrase, PhraseKnowledge>>

    suspend fun getPhrasesForReviewCount(userId: String): Int

    suspend fun upsertPhraseKnowledge(knowledge: PhraseKnowledge)

    // ==========================================
    // MISTAKES
    // ==========================================

    suspend fun logMistake(mistake: MistakeLog)

    suspend fun getMistakes(userId: String, limit: Int): List<MistakeLog>

    suspend fun getMistakesByType(userId: String, type: MistakeType): List<MistakeLog>

    // ==========================================
    // PRONUNCIATION
    // ==========================================

    suspend fun savePronunciationResult(result: PronunciationResult)

    suspend fun getPronunciationResults(userId: String, word: String): List<PronunciationResult>

    suspend fun getAveragePronunciationScore(userId: String): Float

    suspend fun getProblemSounds(userId: String): List<PhoneticTarget>

    suspend fun getPerfectPronunciationCount(userId: String): Int

    suspend fun getRecentPronunciationRecords(userId: String, limit: Int): List<PronunciationResult>

    // ==========================================
    // KNOWLEDGE SNAPSHOT
    // ==========================================

    suspend fun buildKnowledgeSnapshot(userId: String): KnowledgeSnapshot

    suspend fun recalculateOverdueItems(userId: String)

    // ==========================================
    // SYNC
    // ==========================================

    /**
     * Сбрасывает очередь батча накопленных за сессию изменений в Firestore.
     *
     * Вызывается ОДИН РАЗ в конце сессии через [FlushKnowledgeSyncUseCase].
     * 50 слов SRS = было 50 Firestore-записей → стало 1 batch-commit.
     *
     * Возвращает true если синхронизация прошла успешно (или очередь была пуста).
     * false означает ошибку сети — данные остались в очереди и будут
     * отправлены при следующем вызове (at-least-once семантика).
     *
     * Реализация в domain-слое возвращает Boolean чтобы не протащить
     * [CloudSyncService.SyncStatus] (data-тип) в domain-интерфейс.
     */
    suspend fun flushSync(): Boolean
}
