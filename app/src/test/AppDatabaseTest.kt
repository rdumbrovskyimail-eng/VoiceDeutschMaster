package com.voicedeutsch.master.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voicedeutsch.master.data.local.database.entity.UserEntity
import com.voicedeutsch.master.data.local.database.entity.WordEntity
import com.voicedeutsch.master.data.local.database.entity.WordKnowledgeEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for Room DB.
 * Verifies all tables can be written/read correctly.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndReadUser() = runBlocking {
        val user = UserEntity(
            id = "test-user-1",
            name = "Руслан",
            nativeLanguage = "ru",
            targetLanguage = "de",
            cefrLevel = "A1",
            cefrSubLevel = 3,
            totalSessions = 5,
            totalMinutes = 150,
            totalWordsLearned = 80,
            totalRulesLearned = 10,
            streakDays = 7,
            lastSessionDate = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.userDao().insertUser(user)
        val loaded = db.userDao().getUser("test-user-1")
        assertNotNull(loaded)
        assertEquals("Руслан", loaded!!.name)
        assertEquals("A1", loaded.cefrLevel)
    }

    @Test
    fun insertAndReadWord() = runBlocking {
        val word = WordEntity(
            id = "word-1",
            german = "Haus",
            russian = "дом",
            partOfSpeech = "noun",
            gender = "das",
            plural = "Häuser",
            exampleSentenceDe = "Das Haus ist groß.",
            exampleSentenceRu = "Дом большой.",
            difficultyLevel = "A1",
            topic = "Wohnen",
            source = "book"
        )
        db.wordDao().insertWord(word)
        val loaded = db.wordDao().getWord("word-1")
        assertNotNull(loaded)
        assertEquals("Haus", loaded!!.german)
        assertEquals("das", loaded.gender)
    }

    @Test
    fun insertAndReadWordKnowledge() = runBlocking {
        // Insert user and word first (FK constraints)
        val user = UserEntity(
            id = "u1", name = "Test", nativeLanguage = "ru", targetLanguage = "de",
            cefrLevel = "A1", cefrSubLevel = 1, totalSessions = 0, totalMinutes = 0,
            totalWordsLearned = 0, totalRulesLearned = 0, streakDays = 0,
            lastSessionDate = 0, createdAt = 0, updatedAt = 0
        )
        db.userDao().insertUser(user)
        db.wordDao().insertWord(
            WordEntity(id = "w1", german = "Buch", russian = "книга", partOfSpeech = "noun")
        )

        val knowledge = WordKnowledgeEntity(
            id = "wk1",
            userId = "u1",
            wordId = "w1",
            knowledgeLevel = 3,
            timesSeen = 10,
            timesCorrect = 7,
            timesIncorrect = 3,
            lastSeen = System.currentTimeMillis(),
            nextReview = System.currentTimeMillis() + 86400000,
            srsIntervalDays = 3f,
            srsEaseFactor = 2.5f,
            pronunciationScore = 0.75f,
            pronunciationAttempts = 5,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.knowledgeDao().insertWordKnowledge(knowledge)
        val loaded = db.knowledgeDao().getWordKnowledge("u1", "w1")
        assertNotNull(loaded)
        assertEquals(3, loaded!!.knowledgeLevel)
    }

    @Test
    fun wordCountIsCorrect() = runBlocking {
        db.wordDao().insertWords(listOf(
            WordEntity(id = "w1", german = "eins", russian = "один", partOfSpeech = "numeral"),
            WordEntity(id = "w2", german = "zwei", russian = "два", partOfSpeech = "numeral"),
            WordEntity(id = "w3", german = "drei", russian = "три", partOfSpeech = "numeral"),
        ))
        assertEquals(3, db.wordDao().getWordCount())
    }
}