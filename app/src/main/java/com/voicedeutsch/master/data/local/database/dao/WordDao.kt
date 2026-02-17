package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.voicedeutsch.master.data.local.database.entity.WordEntity

@Dao
interface WordDao {

    @Query("SELECT * FROM words WHERE id = :wordId")
    suspend fun getWord(wordId: String): WordEntity?

    @Query("SELECT * FROM words WHERE german = :german LIMIT 1")
    suspend fun getWordByGerman(german: String): WordEntity?

    @Query("SELECT * FROM words")
    suspend fun getAllWords(): List<WordEntity>

    @Query("SELECT * FROM words WHERE topic = :topic")
    suspend fun getWordsByTopic(topic: String): List<WordEntity>

    @Query("SELECT * FROM words WHERE difficulty_level = :level")
    suspend fun getWordsByLevel(level: String): List<WordEntity>

    @Query("SELECT * FROM words WHERE book_chapter = :chapter")
    suspend fun getWordsByChapter(chapter: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE german LIKE '%' || :query || '%' OR russian LIKE '%' || :query || '%'")
    suspend fun searchWords(query: String): List<WordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int
}