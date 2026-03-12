// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/knowledge/UpdateWordKnowledgeUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.MistakeRecord
import com.voicedeutsch.master.domain.model.knowledge.PartOfSpeech
import com.voicedeutsch.master.domain.model.knowledge.Word
import com.voicedeutsch.master.domain.model.knowledge.WordKnowledge
import com.voicedeutsch.master.domain.model.knowledge.WordSource
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.util.Constants
import com.voicedeutsch.master.util.DateUtils
import com.voicedeutsch.master.util.generateUUID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdateWordKnowledgeUseCaseTest {

    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var useCase: UpdateWordKnowledgeUseCase

    private val userId     = "user_update_test"
    private val fakeNow    = 1_700_000_000_000L
    private val fakeUUID   = "fake-uuid-1234"

    @BeforeEach
    fun setUp() {
        knowledgeRepository = io.mockk.mockk()

        mockkStatic("com.voicedeutsch.master.util.DateUtilsKt")
        every { DateUtils.nowTimestamp() } returns fakeNow

        mockkStatic("com.voicedeutsch.master.util.UuidKt")
        every { generateUUID() } returns fakeUUID

        useCase = UpdateWordKnowledgeUseCase(knowledgeRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private fun baseParams(
        wordGerman: String        = "Tisch",
        translation: String       = "table",
        newLevel: Int             = 3,
        quality: Int              = 4,
        pronunciationScore: Float? = null,
        context: String?          = null,
        mistakeExpected: String?  = null,
        mistakeActual: String?    = null,
    ) = UpdateWordKnowledgeUseCase.Params(
        userId             = userId,
        wordGerman         = wordGerman,
        translation        = translation,
        newLevel           = newLevel,
        quality            = quality,
        pronunciationScore = pronunciationScore,
        context            = context,
        mistakeExpected    = mistakeExpected,
        mistakeActual      = mistakeActual,
    )

    private fun makeWord(
        id: String     = "word_001",
        german: String = "Tisch",
        source: WordSource = WordSource.BOOK,
    ) = Word(
        id                = id,
        german            = german,
        russian           = "стол",
        partOfSpeech      = PartOfSpeech.NOUN,
        exampleSentenceDe = "Der Tisch ist groß.",
        exampleSentenceRu = "Стол большой.",
        difficultyLevel   = CefrLevel.A1,
        topic             = "Möbel",
        source            = source,
    )

    private fun makeWordKnowledge(
        wordId: String            = "word_001",
        knowledgeLevel: Int       = 2,
        timesSeen: Int            = 5,
        timesCorrect: Int         = 3,
        timesIncorrect: Int       = 2,
        srsEaseFactor: Float      = 2.5f,
        srsIntervalDays: Float    = 6f,
        nextReview: Long          = fakeNow + 86_400_000L,
        pronunciationScore: Float = 0.7f,
        pronunciationAttempts: Int = 2,
        contexts: List<String>    = listOf("Im Büro"),
        mistakes: List<MistakeRecord> = emptyList(),
        lastCorrect: Long?        = null,
        lastIncorrect: Long?      = null,
    ) = WordKnowledge(
        id                    = "wk_001",
        userId                = userId,
        wordId                = wordId,
        knowledgeLevel        = knowledgeLevel,
        timesSeen             = timesSeen,
        timesCorrect          = timesCorrect,
        timesIncorrect        = timesIncorrect,
        lastSeen              = fakeNow - 86_400_000L,
        lastCorrect           = lastCorrect,
        lastIncorrect         = lastIncorrect,
        nextReview            = nextReview,
        srsIntervalDays       = srsIntervalDays,
        srsEaseFactor         = srsEaseFactor,
        pronunciationScore    = pronunciationScore,
        pronunciationAttempts = pronunciationAttempts,
        contexts              = contexts,
        mistakes              = mistakes,
        createdAt             = fakeNow - 1_000_000L,
        updatedAt             = fakeNow - 100L,
    )

    // ── FIX Баг "Слов-призраков": word not in DB → create on the fly ──────────

    @Test
    fun invoke_wordNotInDb_createsNewWordWithConversationSource() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Schmetterling") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Schmetterling") } returns null
        coEvery { knowledgeRepository.insertWord(any()) } returns Unit
        coEvery { knowledgeRepository.upsertWordKnowledge(any()) } returns Unit

        useCase(baseParams(wordGerman = "Schmetterling", translation = "butterfly"))

        val wordSlot = slot<Word>()
        coVerify { knowledgeRepository.insertWord(capture(wordSlot)) }
        assertEquals("Schmetterling", wordSlot.captured.german)
        assertEquals("butterfly", wordSlot.captured.russian)
        assertEquals(WordSource.CONVERSATION, wordSlot.captured.source)
        assertEquals("Conversation", wordSlot.captured.topic)
    }

    @Test
    fun invoke_wordNotInDb_contextStoredInExampleSentence() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Hund") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Hund") } returns null
        coEvery { knowledgeRepository.insertWord(any()) } returns Unit
        coEvery { knowledgeRepository.upsertWordKnowledge(any()) } returns Unit

        useCase(baseParams(wordGerman = "Hund", context = "Der Hund bellt laut."))

        val wordSlot = slot<Word>()
        coVerify { knowledgeRepository.insertWord(capture(wordSlot)) }
        assertEquals("Der Hund bellt laut.", wordSlot.captured.exampleSentenceDe)
    }

    @Test
    fun invoke_wordNotInDb_nullContext_exampleSentenceIsEmpty() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Apfel") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Apfel") } returns null
        coEvery { knowledgeRepository.insertWord(any()) } returns Unit
        coEvery { knowledgeRepository.upsertWordKnowledge(any()) } returns Unit

        useCase(baseParams(wordGerman = "Apfel", context = null))

        val wordSlot = slot<Word>()
        coVerify { knowledgeRepository.insertWord(capture(wordSlot)) }
        assertEquals("", wordSlot.captured.exampleSentenceDe)
    }

    @Test
    fun invoke_wordExistsInDb_doesNotInsertNewWord() = runTest {
        val existingWord = makeWord()
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns existingWord
        coEvery { knowledgeRepository.upsertWordKnowledge(any()) } returns Unit

        useCase(baseParams())

        coVerify(exactly = 0) { knowledgeRepository.insertWord(any()) }
    }

    // ── New word knowledge (existing == null) ─────────────────────────────────

    @Test
    fun invoke_newKnowledge_callsUpsertWordKnowledge() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        coEvery { knowledgeRepository.upsertWordKnowledge(any()) } returns Unit

        useCase(baseParams())

        coVerify { knowledgeRepository.upsertWordKnowledge(any()) }
    }

    @Test
    fun invoke_newKnowledge_setsTimesSeenTo1() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams())

        assertEquals(1, slot.captured.timesSeen)
    }

    @Test
    fun invoke_newKnowledge_quality4_timesCorrectIs1() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(quality = 4))

        assertEquals(1, slot.captured.timesCorrect)
        assertEquals(0, slot.captured.timesIncorrect)
    }

    @Test
    fun invoke_newKnowledge_quality2_timesIncorrectIs1() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(quality = 2))

        assertEquals(0, slot.captured.timesCorrect)
        assertEquals(1, slot.captured.timesIncorrect)
    }

    @Test
    fun invoke_newKnowledge_quality4_lastCorrectIsSet() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(quality = 4))

        assertEquals(fakeNow, slot.captured.lastCorrect)
        assertNull(slot.captured.lastIncorrect)
    }

    @Test
    fun invoke_newKnowledge_quality2_lastIncorrectIsSet() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(quality = 2))

        assertEquals(fakeNow, slot.captured.lastIncorrect)
        assertNull(slot.captured.lastCorrect)
    }

    @Test
    fun invoke_newKnowledge_knowledgeLevelClamped0to7() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(newLevel = 10))

        assertEquals(7, slot.captured.knowledgeLevel)
    }

    @Test
    fun invoke_newKnowledge_knowledgeLevelNegative_clampedTo0() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(newLevel = -3))

        assertEquals(0, slot.captured.knowledgeLevel)
    }

    @Test
    fun invoke_newKnowledge_lastSeenIsNow() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams())

        assertEquals(fakeNow, slot.captured.lastSeen)
        assertEquals(fakeNow, slot.captured.createdAt)
        assertEquals(fakeNow, slot.captured.updatedAt)
    }

    @Test
    fun invoke_newKnowledge_withPronunciationScore_setsAttempts1() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(pronunciationScore = 0.85f))

        assertEquals(0.85f, slot.captured.pronunciationScore, 0.001f)
        assertEquals(1, slot.captured.pronunciationAttempts)
    }

    @Test
    fun invoke_newKnowledge_withoutPronunciationScore_scoreIs0andAttempts0() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(pronunciationScore = null))

        assertEquals(0f, slot.captured.pronunciationScore, 0.001f)
        assertEquals(0, slot.captured.pronunciationAttempts)
    }

    @Test
    fun invoke_newKnowledge_withContext_contextsListContainsIt() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(context = "Der Tisch ist rund."))

        assertTrue(slot.captured.contexts.contains("Der Tisch ist rund."))
    }

    @Test
    fun invoke_newKnowledge_withoutContext_contextsListIsEmpty() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(context = null))

        assertTrue(slot.captured.contexts.isEmpty())
    }

    @Test
    fun invoke_newKnowledge_withMistake_mistakesListHasOneEntry() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(mistakeExpected = "der Tisch", mistakeActual = "die Tisch"))

        assertEquals(1, slot.captured.mistakes.size)
        assertEquals("der Tisch", slot.captured.mistakes.first().expected)
        assertEquals("die Tisch", slot.captured.mistakes.first().actual)
    }

    @Test
    fun invoke_newKnowledge_withoutMistake_mistakesListIsEmpty() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(mistakeExpected = null, mistakeActual = null))

        assertTrue(slot.captured.mistakes.isEmpty())
    }

    @Test
    fun invoke_newKnowledge_onlyExpectedWithoutActual_mistakesListIsEmpty() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        // Both must be non-null for a mistake to be recorded
        useCase(baseParams(mistakeExpected = "expected", mistakeActual = null))

        assertTrue(slot.captured.mistakes.isEmpty())
    }

    // ── quality clamping ──────────────────────────────────────────────────────

    @Test
    fun invoke_qualityAbove5_clampedTo5() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        // quality=10 → clamped to 5 → timesCorrect=1
        useCase(baseParams(quality = 10))

        assertEquals(1, slot.captured.timesCorrect)
    }

    @Test
    fun invoke_qualityNegative_clampedTo0() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns null
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        // quality=-1 → clamped to 0 → timesIncorrect=1
        useCase(baseParams(quality = -1))

        assertEquals(1, slot.captured.timesIncorrect)
    }

    // ── Existing word knowledge ───────────────────────────────────────────────

    @Test
    fun invoke_existingKnowledge_callsUpsertWordKnowledge() = runTest {
        val existing = makeWordKnowledge()
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        coEvery { knowledgeRepository.upsertWordKnowledge(any()) } returns Unit

        useCase(baseParams())

        coVerify { knowledgeRepository.upsertWordKnowledge(any()) }
    }

    @Test
    fun invoke_existingKnowledge_incrementsTimesSeen() = runTest {
        val existing = makeWordKnowledge(timesSeen = 7)
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams())

        assertEquals(8, slot.captured.timesSeen)
    }

    @Test
    fun invoke_existingKnowledge_quality4_incrementsTimesCorrect() = runTest {
        val existing = makeWordKnowledge(timesCorrect = 5, timesIncorrect = 2)
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(quality = 4))

        assertEquals(6, slot.captured.timesCorrect)
        assertEquals(2, slot.captured.timesIncorrect)
    }

    @Test
    fun invoke_existingKnowledge_quality2_incrementsTimesIncorrect() = runTest {
        val existing = makeWordKnowledge(timesCorrect = 5, timesIncorrect = 2)
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(quality = 2))

        assertEquals(5, slot.captured.timesCorrect)
        assertEquals(3, slot.captured.timesIncorrect)
    }

    @Test
    fun invoke_existingKnowledge_quality4_updatesLastCorrect() = runTest {
        val existing = makeWordKnowledge(lastCorrect = null)
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(quality = 4))

        assertEquals(fakeNow, slot.captured.lastCorrect)
    }

    @Test
    fun invoke_existingKnowledge_quality2_lastCorrectUnchanged() = runTest {
        val prevCorrect = fakeNow - 500_000L
        val existing = makeWordKnowledge(lastCorrect = prevCorrect)
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(quality = 2))

        assertEquals(prevCorrect, slot.captured.lastCorrect)
        assertEquals(fakeNow, slot.captured.lastIncorrect)
    }

    @Test
    fun invoke_existingKnowledge_updatesLastSeenAndUpdatedAt() = runTest {
        val existing = makeWordKnowledge()
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams())

        assertEquals(fakeNow, slot.captured.lastSeen)
        assertEquals(fakeNow, slot.captured.updatedAt)
    }

    @Test
    fun invoke_existingKnowledge_srsFieldsAreRecalculated() = runTest {
        val existing = makeWordKnowledge(srsEaseFactor = 2.5f, srsIntervalDays = 6f, timesCorrect = 3)
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(quality = 5))

        // srsEaseFactor should increase for quality=5
        assertTrue(slot.captured.srsEaseFactor > 0f)
        assertTrue(slot.captured.nextReview!! > fakeNow)
    }

    // ── Pronunciation score averaging ─────────────────────────────────────────

    @Test
    fun invoke_existingKnowledge_pronunciationScoreAveraged() = runTest {
        // existing: score=0.6, attempts=2 → total = 0.6*2 = 1.2
        // new: 0.9 → (1.2 + 0.9) / 3 = 0.7
        val existing = makeWordKnowledge(pronunciationScore = 0.6f, pronunciationAttempts = 2)
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(pronunciationScore = 0.9f))

        assertEquals(0.7f, slot.captured.pronunciationScore, 0.001f)
        assertEquals(3, slot.captured.pronunciationAttempts)
    }

    @Test
    fun invoke_existingKnowledge_noPronunciationScoreProvided_scoreUnchanged() = runTest {
        val existing = makeWordKnowledge(pronunciationScore = 0.75f, pronunciationAttempts = 4)
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(pronunciationScore = null))

        assertEquals(0.75f, slot.captured.pronunciationScore, 0.001f)
        assertEquals(4, slot.captured.pronunciationAttempts)
    }

    // ── Contexts deduplication and limit ─────────────────────────────────────

    @Test
    fun invoke_existingKnowledge_newContextAddedToList() = runTest {
        val existing = makeWordKnowledge(contexts = listOf("Im Büro"))
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(context = "In der Schule"))

        assertTrue(slot.captured.contexts.contains("Im Büro"))
        assertTrue(slot.captured.contexts.contains("In der Schule"))
    }

    @Test
    fun invoke_existingKnowledge_duplicateContextNotAdded() = runTest {
        val existing = makeWordKnowledge(contexts = listOf("Im Büro", "Zuhause"))
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(context = "Im Büro")) // duplicate

        val occurrences = slot.captured.contexts.count { it == "Im Büro" }
        assertEquals(1, occurrences)
    }

    @Test
    fun invoke_existingKnowledge_contextsLimitedToLast10() = runTest {
        val existing = makeWordKnowledge(contexts = (1..10).map { "Context$it" })
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(context = "NewContext"))

        assertTrue(slot.captured.contexts.size <= 10)
        assertTrue(slot.captured.contexts.contains("NewContext"))
    }

    // ── Mistakes list limit ───────────────────────────────────────────────────

    @Test
    fun invoke_existingKnowledge_newMistakeAppended() = runTest {
        val existing = makeWordKnowledge(
            mistakes = listOf(MistakeRecord("old_expected", "old_actual", fakeNow - 1000L))
        )
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(mistakeExpected = "der Tisch", mistakeActual = "die Tisch"))

        assertEquals(2, slot.captured.mistakes.size)
        assertEquals("der Tisch", slot.captured.mistakes.last().expected)
    }

    @Test
    fun invoke_existingKnowledge_mistakesLimitedToLast20() = runTest {
        val oldMistakes = (1..20).map { i ->
            MistakeRecord("expected$i", "actual$i", fakeNow - i * 1000L)
        }
        val existing = makeWordKnowledge(mistakes = oldMistakes)
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(mistakeExpected = "new_expected", mistakeActual = "new_actual"))

        assertEquals(20, slot.captured.mistakes.size)
        // Newest mistake should be last (takeLast(20) keeps newest)
        assertEquals("new_expected", slot.captured.mistakes.last().expected)
    }

    @Test
    fun invoke_existingKnowledge_noMistakeParams_mistakesListUnchanged() = runTest {
        val existingMistakes = listOf(MistakeRecord("e", "a", fakeNow - 1000L))
        val existing = makeWordKnowledge(mistakes = existingMistakes)
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(userId, "Tisch") } returns existing
        coEvery { knowledgeRepository.getWordByGerman("Tisch") } returns makeWord()
        val slot = slot<WordKnowledge>()
        coEvery { knowledgeRepository.upsertWordKnowledge(capture(slot)) } returns Unit

        useCase(baseParams(mistakeExpected = null, mistakeActual = null))

        assertEquals(existingMistakes, slot.captured.mistakes)
    }

    // ── Params data class ─────────────────────────────────────────────────────

    @Test
    fun params_equals_sameValues() {
        val p1 = UpdateWordKnowledgeUseCase.Params(
            userId = "u1", wordGerman = "Hund", translation = "dog",
            newLevel = 3, quality = 4,
        )
        val p2 = UpdateWordKnowledgeUseCase.Params(
            userId = "u1", wordGerman = "Hund", translation = "dog",
            newLevel = 3, quality = 4,
        )
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
    }

    @Test
    fun params_copy_changesOnlySpecifiedField() {
        val original = UpdateWordKnowledgeUseCase.Params(
            userId = "u1", wordGerman = "Hund", translation = "dog",
            newLevel = 3, quality = 4,
        )
        val copy = original.copy(quality = 5)
        assertEquals(5, copy.quality)
        assertEquals("Hund", copy.wordGerman)
        assertEquals("u1", copy.userId)
    }

    @Test
    fun params_defaultValues_areNull() {
        val p = UpdateWordKnowledgeUseCase.Params(
            userId = "u", wordGerman = "w", translation = "t",
            newLevel = 1, quality = 3,
        )
        assertNull(p.pronunciationScore)
        assertNull(p.context)
        assertNull(p.mistakeExpected)
        assertNull(p.mistakeActual)
    }
}
