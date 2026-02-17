package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.voicedeutsch.master.data.local.database.entity.PhraseEntity

@Dao
interface PhraseDao {

    @Query("SELECT * FROM phrases WHERE id = :phraseId")
    suspend fun getPhrase(phraseId: String): PhraseEntity?

    @Query("SELECT * FROM phrases")
    suspend fun getAllPhrases(): List<PhraseEntity>

    @Query("SELECT * FROM phrases WHERE category = :category")
    suspend fun getPhrasesByCategory(category: String): List<PhraseEntity>

    @Query("SELECT * FROM phrases WHERE difficulty_level = :level")
    suspend fun getPhrasesByLevel(level: String): List<PhraseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: PhraseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrases(phrases: List<PhraseEntity>)

    @Query("SELECT COUNT(*) FROM phrases")
    suspend fun getPhraseCount(): Int
}