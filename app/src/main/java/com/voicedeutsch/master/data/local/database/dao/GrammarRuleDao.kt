package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.voicedeutsch.master.data.local.database.entity.GrammarRuleEntity

@Dao
interface GrammarRuleDao {

    @Query("SELECT * FROM grammar_rules WHERE id = :ruleId")
    suspend fun getRule(ruleId: String): GrammarRuleEntity?

    @Query("SELECT * FROM grammar_rules")
    suspend fun getAllRules(): List<GrammarRuleEntity>

    @Query("SELECT * FROM grammar_rules WHERE category = :category")
    suspend fun getRulesByCategory(category: String): List<GrammarRuleEntity>

    @Query("SELECT * FROM grammar_rules WHERE difficulty_level = :level")
    suspend fun getRulesByLevel(level: String): List<GrammarRuleEntity>

    @Query("SELECT * FROM grammar_rules WHERE book_chapter = :chapter")
    suspend fun getRulesByChapter(chapter: Int): List<GrammarRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: GrammarRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<GrammarRuleEntity>)

    @Query("SELECT COUNT(*) FROM grammar_rules")
    suspend fun getRuleCount(): Int
}