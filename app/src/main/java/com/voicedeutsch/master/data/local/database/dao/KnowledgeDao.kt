package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.voicedeutsch.master.data.local.database.entity.PhraseKnowledgeEntity
import com.voicedeutsch.master.data.local.database.entity.RuleKnowledgeEntity
import com.voicedeutsch.master.data.local.database.entity.WordKnowledgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeDao {

    // ==========================================
    // WORD KNOWLEDGE
    // ==========================================

    @Query("SELECT * FROM word_knowledge WHERE user_id = :userId AND word_id = :wordId")
    suspend fun getWordKnowledge(userId: String, wordId: String): WordKnowledgeEntity?

    @Query(
        """
        SELECT wk.* FROM word_knowledge wk 
        INNER JOIN words w ON wk.word_id = w.id
        WHERE wk.user_id = :userId AND w.german = :german
        LIMIT 1
        """
    )
    suspend fun getWordKnowledgeByGerman(userId: String, german: String): WordKnowledgeEntity?

    @Query("SELECT * FROM word_knowledge WHERE user_id = :userId")
    suspend fun getAllWordKnowledge(userId: String): List<WordKnowledgeEntity>

    @Query("SELECT * FROM word_knowledge WHERE user_id = :userId")
    fun getWordKnowledgeFlow(userId: String): Flow<List<WordKnowledgeEntity>>

    @Query(
        """
        SELECT * FROM word_knowledge 
        WHERE user_id = :userId AND next_review <= :now AND next_review IS NOT NULL
        ORDER BY next_review ASC
        LIMIT :limit
        """
    )
    suspend fun getWordsForReview(userId: String, now: Long, limit: Int): List<WordKnowledgeEntity>

    @Query(
        """
        SELECT COUNT(*) FROM word_knowledge 
        WHERE user_id = :userId AND next_review <= :now AND next_review IS NOT NULL
        """
    )
    suspend fun getWordsForReviewCount(userId: String, now: Long): Int

    @Query("SELECT COUNT(*) FROM word_knowledge WHERE user_id = :userId AND knowledge_level >= 4")
    suspend fun getKnownWordsCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM word_knowledge WHERE user_id = :userId AND knowledge_level >= 5")
    suspend fun getActiveWordsCount(userId: String): Int

    @Query(
        """
        SELECT * FROM word_knowledge 
        WHERE user_id = :userId AND knowledge_level = :level
        """
    )
    suspend fun getWordKnowledgeByLevel(userId: String, level: Int): List<WordKnowledgeEntity>

    @Query(
        """
        SELECT * FROM word_knowledge 
        WHERE user_id = :userId AND times_incorrect > times_correct AND times_seen >= 3
        ORDER BY (times_incorrect - times_correct) DESC
        LIMIT :limit
        """
    )
    suspend fun getProblemWords(userId: String, limit: Int): List<WordKnowledgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWordKnowledge(knowledge: WordKnowledgeEntity)

    // ==========================================
    // RULE KNOWLEDGE
    // ==========================================

    @Query("SELECT * FROM rule_knowledge WHERE user_id = :userId AND rule_id = :ruleId")
    suspend fun getRuleKnowledge(userId: String, ruleId: String): RuleKnowledgeEntity?

    @Query("SELECT * FROM rule_knowledge WHERE user_id = :userId")
    suspend fun getAllRuleKnowledge(userId: String): List<RuleKnowledgeEntity>

    @Query(
        """
        SELECT * FROM rule_knowledge 
        WHERE user_id = :userId AND next_review <= :now AND next_review IS NOT NULL
        ORDER BY next_review ASC
        LIMIT :limit
        """
    )
    suspend fun getRulesForReview(userId: String, now: Long, limit: Int): List<RuleKnowledgeEntity>

    @Query(
        """
        SELECT COUNT(*) FROM rule_knowledge 
        WHERE user_id = :userId AND next_review <= :now AND next_review IS NOT NULL
        """
    )
    suspend fun getRulesForReviewCount(userId: String, now: Long): Int

    @Query("SELECT COUNT(*) FROM rule_knowledge WHERE user_id = :userId AND knowledge_level >= 4")
    suspend fun getKnownRulesCount(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRuleKnowledge(knowledge: RuleKnowledgeEntity)

    // ==========================================
    // PHRASE KNOWLEDGE
    // ==========================================

    @Query("SELECT * FROM phrase_knowledge WHERE user_id = :userId AND phrase_id = :phraseId")
    suspend fun getPhraseKnowledge(userId: String, phraseId: String): PhraseKnowledgeEntity?

    @Query(
        """
        SELECT * FROM phrase_knowledge 
        WHERE user_id = :userId AND next_review <= :now AND next_review IS NOT NULL
        ORDER BY next_review ASC
        LIMIT :limit
        """
    )
    suspend fun getPhrasesForReview(userId: String, now: Long, limit: Int): List<PhraseKnowledgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPhraseKnowledge(knowledge: PhraseKnowledgeEntity)

    @Query("SELECT * FROM phrase_knowledge WHERE user_id = :userId")
    suspend fun getAllPhraseKnowledge(userId: String): List<PhraseKnowledgeEntity>

    @Query(
        """
        SELECT COUNT(*) FROM phrase_knowledge 
        WHERE user_id = :userId AND next_review <= :now AND next_review IS NOT NULL
        """
    )
    suspend fun getPhrasesForReviewCount(userId: String, now: Long): Int
}