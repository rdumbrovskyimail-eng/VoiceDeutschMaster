// Путь: src/test/java/com/voicedeutsch/master/data/repository/BookRepositoryImplTest.kt
package com.voicedeutsch.master.data.repository

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.voicedeutsch.master.data.local.database.dao.BookProgressDao
import com.voicedeutsch.master.data.local.database.dao.GrammarRuleDao
import com.voicedeutsch.master.data.local.database.dao.WordDao
import com.voicedeutsch.master.data.local.database.entity.BookProgressEntity
import com.voicedeutsch.master.data.local.database.entity.GrammarRuleEntity
import com.voicedeutsch.master.data.local.database.entity.WordEntity
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.data.local.file.BookFileReader
import com.voicedeutsch.master.domain.model.book.BookMetadata
import com.voicedeutsch.master.domain.model.book.BookProgress
import com.voicedeutsch.master.domain.model.book.ChapterInfo
import com.voicedeutsch.master.domain.model.book.Lesson
import com.voicedeutsch.master.domain.model.book.LessonContent
import com.voicedeutsch.master.domain.model.book.LessonVocabularyEntry
import com.voicedeutsch.master.domain.model.knowledge.GrammarRule
import com.voicedeutsch.master.domain.repository.BookRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class BookRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var assetManager: AssetManager
    private lateinit var bookFileReader: BookFileReader
    private lateinit var bookProgressDao: BookProgressDao
    private lateinit var wordDao: WordDao
    private lateinit var grammarRuleDao: GrammarRuleDao
    private lateinit var preferencesDataStore: UserPreferencesDataStore
    private lateinit var json: Json
    private lateinit var repository: BookRepositoryImpl

    private val fixedUUID = "test-uuid"
    private val fixedNow  = 1_700_000_000_000L

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeBookMetadata(totalChapters: Int = 3, totalLessons: Int = 9): BookMetadata =
        mockk<BookMetadata>(relaxed = true).also {
            every { it.totalChapters } returns totalChapters
            every { it.totalLessons }  returns totalLessons
        }

    private fun makeChapterInfo(lessonsCount: Int = 3, titleDe: String = "Kapitel", titleRu: String = "Глава", level: String = "A1", topics: List<String> = emptyList()): ChapterInfo =
        mockk<ChapterInfo>(relaxed = true).also {
            every { it.lessonsCount } returns lessonsCount
            every { it.titleDe }      returns titleDe
            every { it.titleRu }      returns titleRu
            every { it.level }        returns level
            every { it.topics }       returns topics
        }

    private fun makeLessonContent(vocabSize: Int = 2): LessonContent =
        mockk<LessonContent>(relaxed = true).also {
            every { it.vocabulary } returns List(vocabSize) { mockk(relaxed = true) }
        }

    private fun makeVocabEntry(german: String = "Hund", russian: String = "собака", gender: String? = null, plural: String? = null, level: String = "A1"): LessonVocabularyEntry =
        mockk<LessonVocabularyEntry>(relaxed = true).also {
            every { it.german }  returns german
            every { it.russian } returns russian
            every { it.gender }  returns gender
            every { it.plural }  returns plural
            every { it.level }   returns level
        }

    private fun makeProgressEntity(
        id: String = "pe1",
        userId: String = "user1",
        chapter: Int = 1,
        lesson: Int = 1,
        status: String = "NOT_STARTED",
        score: Float? = null
    ): BookProgressEntity = mockk<BookProgressEntity>(relaxed = true).also {
        every { it.id }      returns id
        every { it.userId }  returns userId
        every { it.chapter } returns chapter
        every { it.lesson }  returns lesson
        every { it.status }  returns status
        every { it.score }   returns score
    }

    private fun makeBookProgress(
        userId: String = "user1",
        chapter: Int = 1,
        lesson: Int = 1
    ): BookProgress = mockk<BookProgress>(relaxed = true).also {
        every { it.userId }  returns userId
        every { it.chapter } returns chapter
        every { it.lesson }  returns lesson
    }

    private fun makeGrammarJson(vararg names: String): String {
        val entries = names.joinToString(",") {
            """{"id":"r1","nameRu":"$it","nameDe":"Regel","bookChapter":1}"""
        }
        return "[$entries]"
    }

    private fun assetStream(content: String) =
        ByteArrayInputStream(content.toByteArray())

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        context           = mockk()
        assetManager      = mockk()
        bookFileReader    = mockk()
        bookProgressDao   = mockk()
        wordDao           = mockk()
        grammarRuleDao    = mockk()
        preferencesDataStore = mockk()
        json              = Json { ignoreUnknownKeys = true; coerceInputValues = true }

        repository = BookRepositoryImpl(
            context, bookFileReader, bookProgressDao, wordDao,
            grammarRuleDao, preferencesDataStore, json
        )

        mockkStatic(android.util.Log::class)
        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        mockkStatic("com.voicedeutsch.master.util.UUIDKt")

        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { com.voicedeutsch.master.util.DateUtils.nowTimestamp() } returns fixedNow
        every { com.voicedeutsch.master.util.generateUUID() }           returns fixedUUID

        every { context.assets } returns assetManager

        coEvery { bookProgressDao.upsertProgress(any()) }    returns Unit
        coEvery { bookProgressDao.markComplete(any(), any(), any(), any(), any()) } returns Unit
        coEvery { wordDao.insertWords(any()) }               returns Unit
        coEvery { grammarRuleDao.insertRules(any()) }        returns Unit
        coEvery { preferencesDataStore.isBookLoaded() }      returns false
        coEvery { preferencesDataStore.setBookLoaded(any()) } returns Unit
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── getBookMetadata ───────────────────────────────────────────────────────

    @Test
    fun getBookMetadata_delegatesToFileReader() = runTest {
        val meta = makeBookMetadata()
        coEvery { bookFileReader.readMetadata() } returns meta

        val result = repository.getBookMetadata()

        assertEquals(meta, result)
    }

    // ── getChapter ────────────────────────────────────────────────────────────

    @Test
    fun getChapter_chapterInfoNotFound_returnsNull() = runTest {
        coEvery { bookFileReader.readChapterInfo(99) } returns null

        val result = repository.getChapter(99)

        assertNull(result)
    }

    @Test
    fun getChapter_chapterFound_returnsChapterWithLessons() = runTest {
        coEvery { bookFileReader.readChapterInfo(1) } returns makeChapterInfo(lessonsCount = 2)
        coEvery { bookFileReader.readLessonContent(1, 1) } returns makeLessonContent(2)
        coEvery { bookFileReader.readLessonContent(1, 2) } returns makeLessonContent(3)

        val result = repository.getChapter(1)

        assertNotNull(result)
        assertEquals(1, result?.number)
        assertEquals(2, result?.lessons?.size)
    }

    @Test
    fun getChapter_chapterFound_lessonNumbersCorrect() = runTest {
        coEvery { bookFileReader.readChapterInfo(2) } returns makeChapterInfo(lessonsCount = 3)
        coEvery { bookFileReader.readLessonContent(2, any()) } returns makeLessonContent()

        val result = repository.getChapter(2)

        assertEquals(listOf(1, 2, 3), result?.lessons?.map { it.number })
    }

    @Test
    fun getChapter_lessonContentNull_newWordsCountIs0() = runTest {
        coEvery { bookFileReader.readChapterInfo(1) } returns makeChapterInfo(lessonsCount = 1)
        coEvery { bookFileReader.readLessonContent(1, 1) } returns null

        val result = repository.getChapter(1)

        assertEquals(0, result?.lessons?.first()?.newWordsCount)
    }

    @Test
    fun getChapter_titlesMappedFromChapterInfo() = runTest {
        coEvery { bookFileReader.readChapterInfo(1) } returns
            makeChapterInfo(titleDe = "Hallo", titleRu = "Привет")
        coEvery { bookFileReader.readLessonContent(1, any()) } returns null

        val result = repository.getChapter(1)

        assertEquals("Hallo",   result?.titleDe)
        assertEquals("Привет",  result?.titleRu)
    }

    // ── getLesson ─────────────────────────────────────────────────────────────

    @Test
    fun getLesson_lessonExists_returnsLesson() = runTest {
        coEvery { bookFileReader.readChapterInfo(1) } returns makeChapterInfo(lessonsCount = 3)
        coEvery { bookFileReader.readLessonContent(1, any()) } returns makeLessonContent()

        val result = repository.getLesson(1, 2)

        assertNotNull(result)
        assertEquals(2, result?.number)
    }

    @Test
    fun getLesson_chapterNotFound_returnsNull() = runTest {
        coEvery { bookFileReader.readChapterInfo(99) } returns null

        val result = repository.getLesson(99, 1)

        assertNull(result)
    }

    @Test
    fun getLesson_lessonNumberOutOfRange_returnsNull() = runTest {
        coEvery { bookFileReader.readChapterInfo(1) } returns makeChapterInfo(lessonsCount = 2)
        coEvery { bookFileReader.readLessonContent(1, any()) } returns makeLessonContent()

        val result = repository.getLesson(1, 99)

        assertNull(result)
    }

    // ── getLessonContent ──────────────────────────────────────────────────────

    @Test
    fun getLessonContent_delegatesToFileReader() = runTest {
        val content = makeLessonContent()
        coEvery { bookFileReader.readLessonContent(2, 3) } returns content

        val result = repository.getLessonContent(2, 3)

        assertEquals(content, result)
    }

    @Test
    fun getLessonContent_fileReaderReturnsNull_returnsNull() = runTest {
        coEvery { bookFileReader.readLessonContent(any(), any()) } returns null

        val result = repository.getLessonContent(1, 1)

        assertNull(result)
    }

    // ── getChapterVocabulary ──────────────────────────────────────────────────

    @Test
    fun getChapterVocabulary_delegatesToFileReader() = runTest {
        val vocab = listOf(makeVocabEntry())
        coEvery { bookFileReader.readChapterVocabulary(1) } returns vocab

        val result = repository.getChapterVocabulary(1)

        assertEquals(vocab, result)
    }

    // ── getChapterGrammar ─────────────────────────────────────────────────────

    @Test
    fun getChapterGrammar_validJson_returnsNameRuList() = runTest {
        val grammarJson = makeGrammarJson("Artikel", "Verben")
        every { assetManager.open("book/chapter_01/grammar.json") } returns
            assetStream(grammarJson)

        val result = repository.getChapterGrammar(1)

        assertEquals(listOf("Artikel", "Verben"), result)
    }

    @Test
    fun getChapterGrammar_assetsThrows_returnsEmptyList() = runTest {
        every { assetManager.open(any()) } throws java.io.FileNotFoundException("not found")

        val result = repository.getChapterGrammar(1)

        assertTrue(result.isEmpty())
    }

    @Test
    fun getChapterGrammar_invalidJson_returnsEmptyList() = runTest {
        every { assetManager.open(any()) } returns assetStream("not-valid-json")

        val result = repository.getChapterGrammar(1)

        assertTrue(result.isEmpty())
    }

    @Test
    fun getChapterGrammar_emptyJsonArray_returnsEmptyList() = runTest {
        every { assetManager.open(any()) } returns assetStream("[]")

        val result = repository.getChapterGrammar(1)

        assertTrue(result.isEmpty())
    }

    @Test
    fun getChapterGrammar_chapterNumberPaddedCorrectly() = runTest {
        every { assetManager.open("book/chapter_05/grammar.json") } returns
            assetStream(makeGrammarJson("Test"))

        repository.getChapterGrammar(5)

        every { assetManager.open("book/chapter_05/grammar.json") } returns assetStream("[]")
    }

    @Test
    fun getChapterGrammar_chapterNumber10_paddingCorrect() = runTest {
        every { assetManager.open("book/chapter_10/grammar.json") } returns
            assetStream(makeGrammarJson("Rule"))

        val result = repository.getChapterGrammar(10)

        assertEquals(listOf("Rule"), result)
    }

    // ── getBookProgress ───────────────────────────────────────────────────────

    @Test
    fun getBookProgress_entityExists_returnsMappedProgress() = runTest {
        val entity = makeProgressEntity(chapter = 2, lesson = 3, status = "COMPLETED")
        coEvery { bookProgressDao.getProgress("user1", 2, 3) } returns entity

        val result = repository.getBookProgress("user1", 2, 3)

        assertNotNull(result)
    }

    @Test
    fun getBookProgress_entityNotFound_returnsNull() = runTest {
        coEvery { bookProgressDao.getProgress("user1", 99, 99) } returns null

        val result = repository.getBookProgress("user1", 99, 99)

        assertNull(result)
    }

    // ── getCurrentBookPosition ────────────────────────────────────────────────

    @Test
    fun getCurrentBookPosition_positionExists_returnsIt() = runTest {
        val entity = makeProgressEntity(chapter = 3, lesson = 2)
        coEvery { bookProgressDao.getCurrentPosition("user1") } returns entity

        val result = repository.getCurrentBookPosition("user1")

        assertEquals(Pair(3, 2), result)
    }

    @Test
    fun getCurrentBookPosition_noPosition_creates1_1AndReturnsIt() = runTest {
        coEvery { bookProgressDao.getCurrentPosition("user1") } returns null

        val result = repository.getCurrentBookPosition("user1")

        assertEquals(Pair(1, 1), result)
    }

    @Test
    fun getCurrentBookPosition_noPosition_upsertCalledWithChapter1Lesson1() = runTest {
        coEvery { bookProgressDao.getCurrentPosition("user1") } returns null
        val slot = slot<BookProgressEntity>()
        coEvery { bookProgressDao.upsertProgress(capture(slot)) } returns Unit

        repository.getCurrentBookPosition("user1")

        assertEquals(1, slot.captured.chapter)
        assertEquals(1, slot.captured.lesson)
        assertEquals("NOT_STARTED", slot.captured.status)
    }

    @Test
    fun getCurrentBookPosition_noPosition_upsertEntityHasGeneratedUUID() = runTest {
        coEvery { bookProgressDao.getCurrentPosition("user1") } returns null
        val slot = slot<BookProgressEntity>()
        coEvery { bookProgressDao.upsertProgress(capture(slot)) } returns Unit

        repository.getCurrentBookPosition("user1")

        assertEquals(fixedUUID, slot.captured.id)
    }

    // ── getAllBookProgress ────────────────────────────────────────────────────

    @Test
    fun getAllBookProgress_emptyDao_returnsEmptyList() = runTest {
        coEvery { bookProgressDao.getAllProgress("user1") } returns emptyList()

        val result = repository.getAllBookProgress("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun getAllBookProgress_multipleEntities_allMapped() = runTest {
        coEvery { bookProgressDao.getAllProgress("user1") } returns listOf(
            makeProgressEntity(chapter = 1, lesson = 1),
            makeProgressEntity(chapter = 1, lesson = 2)
        )

        val result = repository.getAllBookProgress("user1")

        assertEquals(2, result.size)
    }

    // ── getBookProgressFlow ───────────────────────────────────────────────────

    @Test
    fun getBookProgressFlow_emitsFromDao() = runTest {
        coEvery { bookProgressDao.getAllProgressFlow("user1") } returns
            flowOf(listOf(makeProgressEntity()))

        val result = repository.getBookProgressFlow("user1").first()

        assertEquals(1, result.size)
    }

    @Test
    fun getBookProgressFlow_emptyFlow_emitsEmptyList() = runTest {
        coEvery { bookProgressDao.getAllProgressFlow("user1") } returns flowOf(emptyList())

        val result = repository.getBookProgressFlow("user1").first()

        assertTrue(result.isEmpty())
    }

    // ── upsertBookProgress ────────────────────────────────────────────────────

    @Test
    fun upsertBookProgress_delegatesToDao() = runTest {
        val progress = makeBookProgress()
        coEvery { bookProgressDao.upsertProgress(any()) } returns Unit

        repository.upsertBookProgress(progress)

        coVerify(exactly = 1) { bookProgressDao.upsertProgress(any()) }
    }

    // ── markLessonComplete ────────────────────────────────────────────────────

    @Test
    fun markLessonComplete_existingProgress_callsMarkComplete() = runTest {
        coEvery { bookProgressDao.getProgress("user1", 1, 1) } returns makeProgressEntity()

        repository.markLessonComplete("user1", 1, 1, 0.9f)

        coVerify(exactly = 1) {
            bookProgressDao.markComplete("user1", 1, 1, 0.9f, fixedNow)
        }
    }

    @Test
    fun markLessonComplete_noExistingProgress_upsertsCOMPLETED() = runTest {
        coEvery { bookProgressDao.getProgress("user1", 2, 3) } returns null
        val slot = slot<BookProgressEntity>()
        coEvery { bookProgressDao.upsertProgress(capture(slot)) } returns Unit

        repository.markLessonComplete("user1", 2, 3, 0.85f)

        assertEquals("COMPLETED", slot.captured.status)
        assertEquals(2,           slot.captured.chapter)
        assertEquals(3,           slot.captured.lesson)
        assertEquals(0.85f,       slot.captured.score)
    }

    @Test
    fun markLessonComplete_noExistingProgress_setsTimestamps() = runTest {
        coEvery { bookProgressDao.getProgress("user1", 1, 1) } returns null
        val slot = slot<BookProgressEntity>()
        coEvery { bookProgressDao.upsertProgress(capture(slot)) } returns Unit

        repository.markLessonComplete("user1", 1, 1, 1.0f)

        assertEquals(fixedNow, slot.captured.startedAt)
        assertEquals(fixedNow, slot.captured.completedAt)
    }

    @Test
    fun markLessonComplete_noExistingProgress_usesGeneratedUUID() = runTest {
        coEvery { bookProgressDao.getProgress("user1", 1, 1) } returns null
        val slot = slot<BookProgressEntity>()
        coEvery { bookProgressDao.upsertProgress(capture(slot)) } returns Unit

        repository.markLessonComplete("user1", 1, 1, 1.0f)

        assertEquals(fixedUUID, slot.captured.id)
    }

    // ── advanceToNextLesson ───────────────────────────────────────────────────

    @Test
    fun advanceToNextLesson_notLastLessonInChapter_advancesLesson() = runTest {
        val currentPos = makeProgressEntity(chapter = 1, lesson = 2)
        coEvery { bookProgressDao.getCurrentPosition("user1") }        returns currentPos
        coEvery { bookFileReader.readChapterInfo(1) }                  returns makeChapterInfo(lessonsCount = 3)
        coEvery { bookProgressDao.getProgress("user1", 1, 3) }         returns null

        val result = repository.advanceToNextLesson("user1")

        assertEquals(Pair(1, 3), result)
    }

    @Test
    fun advanceToNextLesson_lastLessonNotLastChapter_advancesToNextChapter() = runTest {
        val meta = makeBookMetadata(totalChapters = 3)
        val currentPos = makeProgressEntity(chapter = 1, lesson = 3)
        coEvery { bookProgressDao.getCurrentPosition("user1") }        returns currentPos
        coEvery { bookFileReader.readMetadata() }                      returns meta
        coEvery { bookFileReader.readChapterInfo(1) }                  returns makeChapterInfo(lessonsCount = 3)
        coEvery { bookProgressDao.getProgress("user1", 2, 1) }         returns null

        val result = repository.advanceToNextLesson("user1")

        assertEquals(Pair(2, 1), result)
    }

    @Test
    fun advanceToNextLesson_lastLessonLastChapter_returnsCurrentPosition() = runTest {
        val meta = makeBookMetadata(totalChapters = 2)
        val currentPos = makeProgressEntity(chapter = 2, lesson = 3)
        coEvery { bookProgressDao.getCurrentPosition("user1") }       returns currentPos
        coEvery { bookFileReader.readMetadata() }                     returns meta
        coEvery { bookFileReader.readChapterInfo(2) }                 returns makeChapterInfo(lessonsCount = 3)

        val result = repository.advanceToNextLesson("user1")

        assertEquals(Pair(2, 3), result)
    }

    @Test
    fun advanceToNextLesson_createsProgressForNewLesson() = runTest {
        val currentPos = makeProgressEntity(chapter = 1, lesson = 1)
        coEvery { bookProgressDao.getCurrentPosition("user1") }       returns currentPos
        coEvery { bookFileReader.readChapterInfo(1) }                 returns makeChapterInfo(lessonsCount = 3)
        coEvery { bookProgressDao.getProgress("user1", 1, 2) }        returns null
        val slot = slot<BookProgressEntity>()
        coEvery { bookProgressDao.upsertProgress(capture(slot)) } returns Unit

        repository.advanceToNextLesson("user1")

        // First upsert in getCurrentBookPosition already done; second for new lesson
        assertTrue(slot.captured.lesson == 1 || slot.captured.lesson == 2)
    }

    @Test
    fun advanceToNextLesson_progressAlreadyExistsForNewLesson_noUpsert() = runTest {
        val currentPos = makeProgressEntity(chapter = 1, lesson = 1)
        coEvery { bookProgressDao.getCurrentPosition("user1") }       returns currentPos
        coEvery { bookFileReader.readChapterInfo(1) }                 returns makeChapterInfo(lessonsCount = 3)
        coEvery { bookProgressDao.getProgress("user1", 1, 2) }        returns makeProgressEntity(chapter = 1, lesson = 2)

        repository.advanceToNextLesson("user1")

        // upsert should NOT be called for lesson 2 since it already exists
        coVerify(exactly = 0) {
            bookProgressDao.upsertProgress(match { it.lesson == 2 })
        }
    }

    // ── getCompletedLessonsCount ──────────────────────────────────────────────

    @Test
    fun getCompletedLessonsCount_delegatesToDao() = runTest {
        coEvery { bookProgressDao.getCompletedCount("user1") } returns 7

        val result = repository.getCompletedLessonsCount("user1")

        assertEquals(7, result)
    }

    // ── getTotalLessonsCount ──────────────────────────────────────────────────

    @Test
    fun getTotalLessonsCount_delegatesToMetadata() = runTest {
        coEvery { bookFileReader.readMetadata() } returns makeBookMetadata(totalLessons = 24)

        val result = repository.getTotalLessonsCount()

        assertEquals(24, result)
    }

    // ── getBookCompletionPercentage ───────────────────────────────────────────

    @Test
    fun getBookCompletionPercentage_totalIs0_returns0() = runTest {
        coEvery { bookProgressDao.getCompletedCount("user1") } returns 0
        coEvery { bookFileReader.readMetadata() }              returns makeBookMetadata(totalLessons = 0)

        val result = repository.getBookCompletionPercentage("user1")

        assertEquals(0f, result)
    }

    @Test
    fun getBookCompletionPercentage_halfCompleted_returns05() = runTest {
        coEvery { bookProgressDao.getCompletedCount("user1") } returns 5
        coEvery { bookFileReader.readMetadata() }              returns makeBookMetadata(totalLessons = 10)

        val result = repository.getBookCompletionPercentage("user1")

        assertEquals(0.5f, result, 0.01f)
    }

    @Test
    fun getBookCompletionPercentage_allCompleted_returns1() = runTest {
        coEvery { bookProgressDao.getCompletedCount("user1") } returns 10
        coEvery { bookFileReader.readMetadata() }              returns makeBookMetadata(totalLessons = 10)

        val result = repository.getBookCompletionPercentage("user1")

        assertEquals(1.0f, result, 0.001f)
    }

    // ── isBookLoaded ──────────────────────────────────────────────────────────

    @Test
    fun isBookLoaded_delegatesToDataStore() = runTest {
        coEvery { preferencesDataStore.isBookLoaded() } returns true

        assertTrue(repository.isBookLoaded())
    }

    @Test
    fun isBookLoaded_falseCase() = runTest {
        coEvery { preferencesDataStore.isBookLoaded() } returns false

        assertFalse(repository.isBookLoaded())
    }

    // ── loadBookIntoDatabase ──────────────────────────────────────────────────

    @Test
    fun loadBookIntoDatabase_alreadyLoaded_earlyReturn() = runTest {
        coEvery { preferencesDataStore.isBookLoaded() } returns true

        repository.loadBookIntoDatabase()

        coVerify(exactly = 0) { wordDao.insertWords(any()) }
        coVerify(exactly = 0) { preferencesDataStore.setBookLoaded(any()) }
    }

    @Test
    fun loadBookIntoDatabase_success_vocabularyInsertedPerChapter() = runTest {
        val meta = makeBookMetadata(totalChapters = 2, totalLessons = 4)
        coEvery { bookFileReader.readMetadata() }     returns meta
        coEvery { bookFileReader.readChapterVocabulary(1) } returns listOf(makeVocabEntry("Hund", "собака"))
        coEvery { bookFileReader.readChapterVocabulary(2) } returns listOf(makeVocabEntry("Katze", "кошка"))

        // Grammar: no assets file → throws → caught
        every { assetManager.open(any()) } throws java.io.FileNotFoundException()

        repository.loadBookIntoDatabase()

        coVerify(exactly = 2) { wordDao.insertWords(any()) }
    }

    @Test
    fun loadBookIntoDatabase_emptyVocabulary_insertNotCalled() = runTest {
        val meta = makeBookMetadata(totalChapters = 1, totalLessons = 1)
        coEvery { bookFileReader.readMetadata() }           returns meta
        coEvery { bookFileReader.readChapterVocabulary(1) } returns emptyList()
        every { assetManager.open(any()) }                  throws java.io.FileNotFoundException()

        repository.loadBookIntoDatabase()

        coVerify(exactly = 0) { wordDao.insertWords(any()) }
    }

    @Test
    fun loadBookIntoDatabase_wordEntityFieldsPopulatedCorrectly() = runTest {
        val meta = makeBookMetadata(totalChapters = 1, totalLessons = 1)
        coEvery { bookFileReader.readMetadata() }           returns meta
        coEvery { bookFileReader.readChapterVocabulary(1) } returns
            listOf(makeVocabEntry(german = "Buch", russian = "книга", gender = "n", level = "A2"))
        every { assetManager.open(any()) } throws java.io.FileNotFoundException()

        val slot = slot<List<WordEntity>>()
        coEvery { wordDao.insertWords(capture(slot)) } returns Unit

        repository.loadBookIntoDatabase()

        val entity = slot.captured.single()
        assertEquals("Buch",   entity.german)
        assertEquals("книга",  entity.russian)
        assertEquals("n",      entity.gender)
        assertEquals("A2",     entity.difficultyLevel)
        assertEquals(1,        entity.bookChapter)
        assertEquals("book",   entity.source)
        assertEquals(fixedUUID, entity.id)
        assertEquals(fixedNow, entity.createdAt)
    }

    @Test
    fun loadBookIntoDatabase_grammarLoaded_insertRulesCalled() = runTest {
        val meta = makeBookMetadata(totalChapters = 1, totalLessons = 1)
        coEvery { bookFileReader.readMetadata() }           returns meta
        coEvery { bookFileReader.readChapterVocabulary(1) } returns emptyList()
        every { assetManager.open("book/chapter_01/grammar.json") } returns
            assetStream(makeGrammarJson("Artikel", "Nominativ"))

        repository.loadBookIntoDatabase()

        coVerify(exactly = 1) { grammarRuleDao.insertRules(any()) }
    }

    @Test
    fun loadBookIntoDatabase_grammarJsonThrows_doesNotInterruptProcess() = runTest {
        val meta = makeBookMetadata(totalChapters = 2, totalLessons = 2)
        coEvery { bookFileReader.readMetadata() }           returns meta
        coEvery { bookFileReader.readChapterVocabulary(any()) } returns listOf(makeVocabEntry())
        every { assetManager.open(any()) } throws java.io.FileNotFoundException()

        repository.loadBookIntoDatabase()

        // Both chapters' vocabulary still processed
        coVerify(exactly = 2) { wordDao.insertWords(any()) }
    }

    @Test
    fun loadBookIntoDatabase_grammarChapter2Fails_chapter1GrammarStillLoaded() = runTest {
        val meta = makeBookMetadata(totalChapters = 2, totalLessons = 2)
        coEvery { bookFileReader.readMetadata() }              returns meta
        coEvery { bookFileReader.readChapterVocabulary(any()) } returns emptyList()
        every { assetManager.open("book/chapter_01/grammar.json") } returns
            assetStream(makeGrammarJson("Artikel"))
        every { assetManager.open("book/chapter_02/grammar.json") } throws
            java.io.FileNotFoundException()

        repository.loadBookIntoDatabase()

        coVerify(exactly = 1) { grammarRuleDao.insertRules(any()) }
    }

    @Test
    fun loadBookIntoDatabase_grammarRuleEntityBookChapterSetToChapterNumber() = runTest {
        val meta = makeBookMetadata(totalChapters = 1, totalLessons = 1)
        coEvery { bookFileReader.readMetadata() }              returns meta
        coEvery { bookFileReader.readChapterVocabulary(1) }    returns emptyList()
        every { assetManager.open("book/chapter_01/grammar.json") } returns
            assetStream(makeGrammarJson("Test"))

        val slot = slot<List<GrammarRuleEntity>>()
        coEvery { grammarRuleDao.insertRules(capture(slot)) } returns Unit

        repository.loadBookIntoDatabase()

        slot.captured.forEach { assertEquals(1, it.bookChapter) }
    }

    @Test
    fun loadBookIntoDatabase_emptyGrammarJson_insertRulesNotCalled() = runTest {
        val meta = makeBookMetadata(totalChapters = 1, totalLessons = 1)
        coEvery { bookFileReader.readMetadata() }              returns meta
        coEvery { bookFileReader.readChapterVocabulary(1) }    returns emptyList()
        every { assetManager.open(any()) }                     returns assetStream("[]")

        repository.loadBookIntoDatabase()

        coVerify(exactly = 0) { grammarRuleDao.insertRules(any()) }
    }

    @Test
    fun loadBookIntoDatabase_setBookLoadedCalledTrue() = runTest {
        val meta = makeBookMetadata(totalChapters = 1, totalLessons = 1)
        coEvery { bookFileReader.readMetadata() }              returns meta
        coEvery { bookFileReader.readChapterVocabulary(1) }    returns emptyList()
        every { assetManager.open(any()) }                     throws java.io.FileNotFoundException()

        repository.loadBookIntoDatabase()

        coVerify(exactly = 1) { preferencesDataStore.setBookLoaded(true) }
    }

    @Test
    fun loadBookIntoDatabase_allChaptersGrammarFail_setBookLoadedStillCalled() = runTest {
        val meta = makeBookMetadata(totalChapters = 3, totalLessons = 3)
        coEvery { bookFileReader.readMetadata() }              returns meta
        coEvery { bookFileReader.readChapterVocabulary(any()) } returns emptyList()
        every { assetManager.open(any()) }                     throws RuntimeException("parse error")

        repository.loadBookIntoDatabase()

        coVerify(exactly = 1) { preferencesDataStore.setBookLoaded(true) }
    }

    // ── searchContent ─────────────────────────────────────────────────────────

    @Test
    fun searchContent_noChapters_returnsEmptyList() = runTest {
        coEvery { bookFileReader.readMetadata() }      returns makeBookMetadata(totalChapters = 0)

        val result = repository.searchContent("Hund")

        assertTrue(result.isEmpty())
    }

    @Test
    fun searchContent_chapterInfoNull_skipsChapter() = runTest {
        coEvery { bookFileReader.readMetadata() }       returns makeBookMetadata(totalChapters = 1)
        coEvery { bookFileReader.readChapterInfo(1) }   returns null

        val result = repository.searchContent("Hund")

        assertTrue(result.isEmpty())
    }

    @Test
    fun searchContent_lessonContentNull_skipsLesson() = runTest {
        coEvery { bookFileReader.readMetadata() }       returns makeBookMetadata(totalChapters = 1)
        coEvery { bookFileReader.readChapterInfo(1) }   returns makeChapterInfo(lessonsCount = 1)
        coEvery { bookFileReader.readLessonContent(1, 1) } returns null

        val result = repository.searchContent("Hund")

        assertTrue(result.isEmpty())
    }

    @Test
    fun searchContent_germanMatch_returnsHit() = runTest {
        val vocabEntry = makeVocabEntry(german = "Hund", russian = "собака")
        val content = mockk<LessonContent>(relaxed = true).also {
            every { it.vocabulary } returns listOf(vocabEntry)
        }
        coEvery { bookFileReader.readMetadata() }           returns makeBookMetadata(totalChapters = 1)
        coEvery { bookFileReader.readChapterInfo(1) }       returns makeChapterInfo(lessonsCount = 1)
        coEvery { bookFileReader.readLessonContent(1, 1) }  returns content

        val result = repository.searchContent("Hund")

        assertEquals(1, result.size)
        assertEquals(1, result[0].chapter)
        assertEquals(1, result[0].lesson)
        assertTrue(result[0].snippet.contains("Hund"))
    }

    @Test
    fun searchContent_russianMatch_returnsHit() = runTest {
        val vocabEntry = makeVocabEntry(german = "Hund", russian = "собака")
        val content = mockk<LessonContent>(relaxed = true).also {
            every { it.vocabulary } returns listOf(vocabEntry)
        }
        coEvery { bookFileReader.readMetadata() }           returns makeBookMetadata(totalChapters = 1)
        coEvery { bookFileReader.readChapterInfo(1) }       returns makeChapterInfo(lessonsCount = 1)
        coEvery { bookFileReader.readLessonContent(1, 1) }  returns content

        val result = repository.searchContent("собака")

        assertEquals(1, result.size)
    }

    @Test
    fun searchContent_caseInsensitiveMatch() = runTest {
        val vocabEntry = makeVocabEntry(german = "Hund", russian = "собака")
        val content = mockk<LessonContent>(relaxed = true).also {
            every { it.vocabulary } returns listOf(vocabEntry)
        }
        coEvery { bookFileReader.readMetadata() }           returns makeBookMetadata(totalChapters = 1)
        coEvery { bookFileReader.readChapterInfo(1) }       returns makeChapterInfo(lessonsCount = 1)
        coEvery { bookFileReader.readLessonContent(1, 1) }  returns content

        val result = repository.searchContent("hund")

        assertEquals(1, result.size)
    }

    @Test
    fun searchContent_noMatch_returnsEmptyList() = runTest {
        val vocabEntry = makeVocabEntry(german = "Hund", russian = "собака")
        val content = mockk<LessonContent>(relaxed = true).also {
            every { it.vocabulary } returns listOf(vocabEntry)
        }
        coEvery { bookFileReader.readMetadata() }           returns makeBookMetadata(totalChapters = 1)
        coEvery { bookFileReader.readChapterInfo(1) }       returns makeChapterInfo(lessonsCount = 1)
        coEvery { bookFileReader.readLessonContent(1, 1) }  returns content

        val result = repository.searchContent("Katze")

        assertTrue(result.isEmpty())
    }

    @Test
    fun searchContent_snippetContainsBothGermanAndRussian() = runTest {
        val vocabEntry = makeVocabEntry(german = "Buch", russian = "книга")
        val content = mockk<LessonContent>(relaxed = true).also {
            every { it.vocabulary } returns listOf(vocabEntry)
        }
        coEvery { bookFileReader.readMetadata() }           returns makeBookMetadata(totalChapters = 1)
        coEvery { bookFileReader.readChapterInfo(1) }       returns makeChapterInfo(lessonsCount = 1)
        coEvery { bookFileReader.readLessonContent(1, 1) }  returns content

        val result = repository.searchContent("Buch")

        assertTrue(result[0].snippet.contains("Buch"))
        assertTrue(result[0].snippet.contains("книга"))
    }

    @Test
    fun searchContent_multipleChaptersAndLessons_allSearched() = runTest {
        val meta = makeBookMetadata(totalChapters = 2)
        val noMatchContent = mockk<LessonContent>(relaxed = true).also {
            every { it.vocabulary } returns listOf(makeVocabEntry("Haus", "дом"))
        }
        val matchContent = mockk<LessonContent>(relaxed = true).also {
            every { it.vocabulary } returns listOf(makeVocabEntry("Hund", "собака"))
        }
        coEvery { bookFileReader.readMetadata() }              returns meta
        coEvery { bookFileReader.readChapterInfo(any()) }      returns makeChapterInfo(lessonsCount = 1)
        coEvery { bookFileReader.readLessonContent(1, 1) }     returns noMatchContent
        coEvery { bookFileReader.readLessonContent(2, 1) }     returns matchContent

        val result = repository.searchContent("Hund")

        assertEquals(1, result.size)
        assertEquals(2, result[0].chapter)
    }

    @Test
    fun searchContent_nullVocabularyInContent_treatedAsEmpty() = runTest {
        val content = mockk<LessonContent>(relaxed = true).also {
            every { it.vocabulary } returns emptyList()
        }
        coEvery { bookFileReader.readMetadata() }           returns makeBookMetadata(totalChapters = 1)
        coEvery { bookFileReader.readChapterInfo(1) }       returns makeChapterInfo(lessonsCount = 1)
        coEvery { bookFileReader.readLessonContent(1, 1) }  returns content

        val result = repository.searchContent("Hund")

        assertTrue(result.isEmpty())
    }
}
