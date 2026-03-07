// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/learning/GetNextExerciseUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.learning

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.book.ExerciseType
import com.voicedeutsch.master.domain.model.knowledge.Word
import com.voicedeutsch.master.domain.model.knowledge.WordKnowledge
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetNextExerciseUseCaseTest {

    private lateinit var bookRepository: BookRepository
    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var useCase: GetNextExerciseUseCase

    private val fixedUUID = "exercise-uuid-1"

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeWord(
        id: String = "w1",
        german: String = "Hund",
        russian: String = "собака"
    ): Word = mockk<Word>(relaxed = true).also {
        every { it.id }      returns id
        every { it.german }  returns german
        every { it.russian } returns russian
    }

    private fun makeWordKnowledge(): WordKnowledge = mockk(relaxed = true)

    private fun makeParams(
        userId: String = "user1",
        strategy: LearningStrategy = LearningStrategy.REPETITION,
        excludeIds: List<String> = emptyList()
    ) = GetNextExerciseUseCase.Params(
        userId     = userId,
        strategy   = strategy,
        excludeIds = excludeIds
    )

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        bookRepository      = mockk()
        knowledgeRepository = mockk()
        useCase = GetNextExerciseUseCase(bookRepository, knowledgeRepository)

        mockkStatic("com.voicedeutsch.master.util.UUIDKt")
        every { com.voicedeutsch.master.util.generateUUID() } returns fixedUUID

        coEvery { knowledgeRepository.getWordsForReview(any(), any()) } returns emptyList()
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── REPETITION strategy ───────────────────────────────────────────────────

    @Test
    fun invoke_repetition_dueWordExists_returnsExercise() = runTest {
        val word = makeWord()
        coEvery { knowledgeRepository.getWordsForReview("user1", 1) } returns
            listOf(word to makeWordKnowledge())

        val result = useCase(makeParams(strategy = LearningStrategy.REPETITION))

        assertNotNull(result)
    }

    @Test
    fun invoke_repetition_dueWordExists_exerciseHasGeneratedUUID() = runTest {
        val word = makeWord()
        coEvery { knowledgeRepository.getWordsForReview("user1", 1) } returns
            listOf(word to makeWordKnowledge())

        val result = useCase(makeParams(strategy = LearningStrategy.REPETITION))

        assertEquals(fixedUUID, result?.id)
    }

    @Test
    fun invoke_repetition_dueWordExists_exerciseTypeIsTranslateToDe() = runTest {
        val word = makeWord()
        coEvery { knowledgeRepository.getWordsForReview("user1", 1) } returns
            listOf(word to makeWordKnowledge())

        val result = useCase(makeParams(strategy = LearningStrategy.REPETITION))

        assertEquals(ExerciseType.TRANSLATE_TO_DE, result?.type)
    }

    @Test
    fun invoke_repetition_dueWordExists_promptIsRussian() = runTest {
        val word = makeWord(russian = "собака")
        coEvery { knowledgeRepository.getWordsForReview("user1", 1) } returns
            listOf(word to makeWordKnowledge())

        val result = useCase(makeParams(strategy = LearningStrategy.REPETITION))

        assertEquals("собака", result?.prompt)
    }

    @Test
    fun invoke_repetition_dueWordExists_expectedAnswerIsGerman() = runTest {
        val word = makeWord(german = "Hund")
        coEvery { knowledgeRepository.getWordsForReview("user1", 1) } returns
            listOf(word to makeWordKnowledge())

        val result = useCase(makeParams(strategy = LearningStrategy.REPETITION))

        assertEquals("Hund", result?.expectedAnswer)
    }

    @Test
    fun invoke_repetition_dueWordExists_relatedWordIdsContainsWordId() = runTest {
        val word = makeWord(id = "word-abc")
        coEvery { knowledgeRepository.getWordsForReview("user1", 1) } returns
            listOf(word to makeWordKnowledge())

        val result = useCase(makeParams(strategy = LearningStrategy.REPETITION))

        assertEquals(listOf("word-abc"), result?.relatedWordIds)
    }

    @Test
    fun invoke_repetition_noDueWords_returnsNull() = runTest {
        coEvery { knowledgeRepository.getWordsForReview("user1", 1) } returns emptyList()

        val result = useCase(makeParams(strategy = LearningStrategy.REPETITION))

        assertNull(result)
    }

    @Test
    fun invoke_repetition_repositoryCalledWithLimit1() = runTest {
        useCase(makeParams(strategy = LearningStrategy.REPETITION))

        coVerify(exactly = 1) { knowledgeRepository.getWordsForReview("user1", 1) }
    }

    @Test
    fun invoke_repetition_multipleWordsDue_usesFirstOnly() = runTest {
        val word1 = makeWord(id = "w1", german = "Hund",  russian = "собака")
        val word2 = makeWord(id = "w2", german = "Katze", russian = "кошка")
        coEvery { knowledgeRepository.getWordsForReview("user1", 1) } returns listOf(
            word1 to makeWordKnowledge(),
            word2 to makeWordKnowledge()
        )

        val result = useCase(makeParams(strategy = LearningStrategy.REPETITION))

        assertEquals("собака", result?.prompt)
        assertEquals("Hund",   result?.expectedAnswer)
    }

    // ── LINEAR_BOOK strategy ──────────────────────────────────────────────────

    @Test
    fun invoke_linearBook_returnsNull() = runTest {
        val result = useCase(makeParams(strategy = LearningStrategy.LINEAR_BOOK))

        assertNull(result)
    }

    @Test
    fun invoke_linearBook_knowledgeRepositoryNotCalled() = runTest {
        useCase(makeParams(strategy = LearningStrategy.LINEAR_BOOK))

        coVerify(exactly = 0) { knowledgeRepository.getWordsForReview(any(), any()) }
    }

    // ── Other / else strategies ───────────────────────────────────────────────

    @Test
    fun invoke_gapFilling_returnsNull() = runTest {
        val result = useCase(makeParams(strategy = LearningStrategy.GAP_FILLING))

        assertNull(result)
    }

    @Test
    fun invoke_vocabularyBoost_returnsNull() = runTest {
        val result = useCase(makeParams(strategy = LearningStrategy.VOCABULARY_BOOST))

        assertNull(result)
    }

    @Test
    fun invoke_grammarDrill_returnsNull() = runTest {
        val result = useCase(makeParams(strategy = LearningStrategy.GRAMMAR_DRILL))

        assertNull(result)
    }

    @Test
    fun invoke_pronunciation_returnsNull() = runTest {
        val result = useCase(makeParams(strategy = LearningStrategy.PRONUNCIATION))

        assertNull(result)
    }

    @Test
    fun invoke_elseStrategy_knowledgeRepositoryNotCalled() = runTest {
        useCase(makeParams(strategy = LearningStrategy.GAP_FILLING))

        coVerify(exactly = 0) { knowledgeRepository.getWordsForReview(any(), any()) }
    }

    // ── Params data class ─────────────────────────────────────────────────────

    @Test
    fun params_defaultExcludeIds_isEmpty() {
        val params = GetNextExerciseUseCase.Params(
            userId   = "u",
            strategy = LearningStrategy.REPETITION
        )
        assertTrue(params.excludeIds.isEmpty())
    }

    @Test
    fun params_creation_storesAllFields() {
        val params = GetNextExerciseUseCase.Params(
            userId     = "user1",
            strategy   = LearningStrategy.LINEAR_BOOK,
            excludeIds = listOf("e1", "e2")
        )
        assertEquals("user1",                        params.userId)
        assertEquals(LearningStrategy.LINEAR_BOOK,   params.strategy)
        assertEquals(listOf("e1", "e2"),             params.excludeIds)
    }

    @Test
    fun params_copy_changesOnlySpecifiedField() {
        val original = GetNextExerciseUseCase.Params("u", LearningStrategy.REPETITION, listOf("e1"))
        val copy     = original.copy(strategy = LearningStrategy.GAP_FILLING)

        assertEquals(LearningStrategy.GAP_FILLING, copy.strategy)
        assertEquals("u",             copy.userId)
        assertEquals(listOf("e1"),    copy.excludeIds)
    }

    @Test
    fun params_equals_twoIdenticalInstancesAreEqual() {
        val a = GetNextExerciseUseCase.Params("u", LearningStrategy.REPETITION, listOf("e1"))
        val b = GetNextExerciseUseCase.Params("u", LearningStrategy.REPETITION, listOf("e1"))

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun params_equals_differentStrategyNotEqual() {
        val a = GetNextExerciseUseCase.Params("u", LearningStrategy.REPETITION)
        val b = GetNextExerciseUseCase.Params("u", LearningStrategy.LINEAR_BOOK)

        assertNotEquals(a, b)
    }

    @Test
    fun params_equals_differentExcludeIdsNotEqual() {
        val a = GetNextExerciseUseCase.Params("u", LearningStrategy.REPETITION, listOf("e1"))
        val b = GetNextExerciseUseCase.Params("u", LearningStrategy.REPETITION, listOf("e2"))

        assertNotEquals(a, b)
    }
}
