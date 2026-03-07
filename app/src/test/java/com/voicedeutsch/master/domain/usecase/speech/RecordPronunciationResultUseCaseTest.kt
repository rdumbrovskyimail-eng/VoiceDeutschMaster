// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/speech/RecordPronunciationResultUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.speech

import com.voicedeutsch.master.domain.model.knowledge.WordKnowledge
import com.voicedeutsch.master.domain.model.speech.PronunciationResult
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

class RecordPronunciationResultUseCaseTest {

    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var useCase: RecordPronunciationResultUseCase

    private val fixedNow  = 1_700_000_000_000L
    private val fixedUUID = "pronunciation-uuid-1"

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeWordKnowledge(
        pronunciationScore: Float = 0.5f,
        pronunciationAttempts: Int = 2
    ): WordKnowledge = mockk<WordKnowledge>(relaxed = true).also { wk ->
        every { wk.pronunciationScore }    returns pronunciationScore
        every { wk.pronunciationAttempts } returns pronunciationAttempts
        every { wk.copy(
            pronunciationScore = any(),
            pronunciationAttempts = any(),
            updatedAt = any()
        ) } answers {
            val newScore    = firstArg<Float>()
            val newAttempts = secondArg<Int>()
            val newUpdated  = thirdArg<Long>()
            mockk<WordKnowledge>(relaxed = true).also { copy ->
                every { copy.pronunciationScore }    returns newScore
                every { copy.pronunciationAttempts } returns newAttempts
                every { copy.updatedAt }             returns newUpdated
            }
        }
    }

    private fun makeParams(
        userId: String = "user1",
        word: String = "Hund",
        score: Float = 0.8f,
        problemSounds: List<String> = emptyList(),
        sessionId: String? = null,
        attemptNumber: Int = 1
    ) = RecordPronunciationResultUseCase.Params(
        userId        = userId,
        word          = word,
        score         = score,
        problemSounds = problemSounds,
        sessionId     = sessionId,
        attemptNumber = attemptNumber
    )

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        knowledgeRepository = mockk()
        useCase = RecordPronunciationResultUseCase(knowledgeRepository)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        mockkStatic("com.voicedeutsch.master.util.UUIDKt")

        every { com.voicedeutsch.master.util.DateUtils.nowTimestamp() } returns fixedNow
        every { com.voicedeutsch.master.util.generateUUID() }           returns fixedUUID

        coEvery { knowledgeRepository.savePronunciationResult(any()) }      returns Unit
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(any(), any()) } returns null
        coEvery { knowledgeRepository.upsertWordKnowledge(any()) }          returns Unit
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── invoke — savePronunciationResult ─────────────────────────────────────

    @Test
    fun invoke_savePronunciationResultCalledOnce() = runTest {
        useCase(makeParams())

        coVerify(exactly = 1) { knowledgeRepository.savePronunciationResult(any()) }
    }

    @Test
    fun invoke_resultHasGeneratedUUID() = runTest {
        var captured: PronunciationResult? = null
        coEvery { knowledgeRepository.savePronunciationResult(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(fixedUUID, captured?.id)
    }

    @Test
    fun invoke_resultUserIdMatchesParams() = runTest {
        var captured: PronunciationResult? = null
        coEvery { knowledgeRepository.savePronunciationResult(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(userId = "user42"))

        assertEquals("user42", captured?.userId)
    }

    @Test
    fun invoke_resultWordMatchesParams() = runTest {
        var captured: PronunciationResult? = null
        coEvery { knowledgeRepository.savePronunciationResult(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(word = "Katze"))

        assertEquals("Katze", captured?.word)
    }

    @Test
    fun invoke_resultScoreMatchesParams() = runTest {
        var captured: PronunciationResult? = null
        coEvery { knowledgeRepository.savePronunciationResult(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(score = 0.75f))

        assertEquals(0.75f, captured?.score ?: -1f, 0.001f)
    }

    @Test
    fun invoke_resultTimestampIsNow() = runTest {
        var captured: PronunciationResult? = null
        coEvery { knowledgeRepository.savePronunciationResult(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(fixedNow, captured?.timestamp)
        assertEquals(fixedNow, captured?.createdAt)
    }

    @Test
    fun invoke_resultProblemSoundsMatchesParams() = runTest {
        var captured: PronunciationResult? = null
        coEvery { knowledgeRepository.savePronunciationResult(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(problemSounds = listOf("ü", "ch")))

        assertEquals(listOf("ü", "ch"), captured?.problemSounds)
    }

    @Test
    fun invoke_resultAttemptNumberMatchesParams() = runTest {
        var captured: PronunciationResult? = null
        coEvery { knowledgeRepository.savePronunciationResult(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(attemptNumber = 3))

        assertEquals(3, captured?.attemptNumber)
    }

    @Test
    fun invoke_resultSessionIdMatchesParams() = runTest {
        var captured: PronunciationResult? = null
        coEvery { knowledgeRepository.savePronunciationResult(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(sessionId = "session-99"))

        assertEquals("session-99", captured?.sessionId)
    }

    @Test
    fun invoke_sessionIdNull_resultSessionIdIsNull() = runTest {
        var captured: PronunciationResult? = null
        coEvery { knowledgeRepository.savePronunciationResult(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(sessionId = null))

        assertNull(captured?.sessionId)
    }

    // ── invoke — score clamping ───────────────────────────────────────────────

    @Test
    fun invoke_scoreAbove1_clampedTo1() = runTest {
        var captured: PronunciationResult? = null
        coEvery { knowledgeRepository.savePronunciationResult(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(score = 1.5f))

        assertEquals(1.0f, captured?.score ?: -1f, 0.001f)
    }

    @Test
    fun invoke_scoreBelow0_clampedTo0() = runTest {
        var captured: PronunciationResult? = null
        coEvery { knowledgeRepository.savePronunciationResult(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(score = -0.3f))

        assertEquals(0.0f, captured?.score ?: -1f, 0.001f)
    }

    @Test
    fun invoke_scoreExactly0_notClamped() = runTest {
        var captured: PronunciationResult? = null
        coEvery { knowledgeRepository.savePronunciationResult(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(score = 0.0f))

        assertEquals(0.0f, captured?.score ?: -1f, 0.001f)
    }

    @Test
    fun invoke_scoreExactly1_notClamped() = runTest {
        var captured: PronunciationResult? = null
        coEvery { knowledgeRepository.savePronunciationResult(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(score = 1.0f))

        assertEquals(1.0f, captured?.score ?: -1f, 0.001f)
    }

    // ── invoke — updateWordPronunciationScore: no existing knowledge ──────────

    @Test
    fun invoke_noExistingWordKnowledge_upsertNotCalled() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman("user1", "Hund") } returns null

        useCase(makeParams())

        coVerify(exactly = 0) { knowledgeRepository.upsertWordKnowledge(any()) }
    }

    @Test
    fun invoke_noExistingWordKnowledge_getWordKnowledgeCalled() = runTest {
        useCase(makeParams(userId = "user1", word = "Hund"))

        coVerify(exactly = 1) { knowledgeRepository.getWordKnowledgeByGerman("user1", "Hund") }
    }

    // ── invoke — updateWordPronunciationScore: existing knowledge ─────────────

    @Test
    fun invoke_existingWordKnowledge_upsertCalledOnce() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman("user1", "Hund") } returns
            makeWordKnowledge(pronunciationScore = 0.5f, pronunciationAttempts = 2)

        useCase(makeParams(score = 0.8f))

        coVerify(exactly = 1) { knowledgeRepository.upsertWordKnowledge(any()) }
    }

    @Test
    fun invoke_existingWordKnowledge_attemptsIncrementedBy1() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(any(), any()) } returns
            makeWordKnowledge(pronunciationScore = 0.5f, pronunciationAttempts = 4)
        var capturedWK: WordKnowledge? = null
        coEvery { knowledgeRepository.upsertWordKnowledge(any()) } answers {
            capturedWK = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(5, capturedWK?.pronunciationAttempts)
    }

    @Test
    fun invoke_existingWordKnowledge_rollingAverageCalculatedCorrectly() = runTest {
        // prevScore=0.5, prevAttempts=2 → prevTotal=1.0; newScore=1.0
        // totalAttempts=3, updatedScore=(1.0+1.0)/3 = 0.666...
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(any(), any()) } returns
            makeWordKnowledge(pronunciationScore = 0.5f, pronunciationAttempts = 2)
        var capturedWK: WordKnowledge? = null
        coEvery { knowledgeRepository.upsertWordKnowledge(any()) } answers {
            capturedWK = firstArg(); Unit
        }

        useCase(makeParams(score = 1.0f))

        assertEquals(0.666f, capturedWK?.pronunciationScore ?: -1f, 0.01f)
    }

    @Test
    fun invoke_existingKnowledge0Attempts_scoreSetDirectlyToNewScore() = runTest {
        // totalAttempts = 0+1 = 1 > 0; prevTotal = 0*score = 0; updated = (0+newScore)/1 = newScore
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(any(), any()) } returns
            makeWordKnowledge(pronunciationScore = 0f, pronunciationAttempts = 0)
        var capturedWK: WordKnowledge? = null
        coEvery { knowledgeRepository.upsertWordKnowledge(any()) } answers {
            capturedWK = firstArg(); Unit
        }

        useCase(makeParams(score = 0.9f))

        assertEquals(0.9f, capturedWK?.pronunciationScore ?: -1f, 0.001f)
    }

    @Test
    fun invoke_existingWordKnowledge_updatedAtSetToNow() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(any(), any()) } returns
            makeWordKnowledge()
        var capturedWK: WordKnowledge? = null
        coEvery { knowledgeRepository.upsertWordKnowledge(any()) } answers {
            capturedWK = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(fixedNow, capturedWK?.updatedAt)
    }

    @Test
    fun invoke_existingKnowledge_savePronunciationCalledBeforeUpsert() = runTest {
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(any(), any()) } returns
            makeWordKnowledge()
        val callOrder = mutableListOf<String>()
        coEvery { knowledgeRepository.savePronunciationResult(any()) } answers {
            callOrder.add("save"); Unit
        }
        coEvery { knowledgeRepository.upsertWordKnowledge(any()) } answers {
            callOrder.add("upsert"); Unit
        }

        useCase(makeParams())

        assertEquals(listOf("save", "upsert"), callOrder)
    }

    @Test
    fun invoke_multipleCallsSameWord_rollingAverageAccumulates() = runTest {
        // First call: prevScore=0.0, prevAttempts=0 → updatedScore = 0.6
        // Second call: prevScore=0.6, prevAttempts=1 → updatedScore = (0.6+0.4)/2 = 0.5
        val wk1 = makeWordKnowledge(pronunciationScore = 0.0f, pronunciationAttempts = 0)
        val wk2 = makeWordKnowledge(pronunciationScore = 0.6f, pronunciationAttempts = 1)
        coEvery { knowledgeRepository.getWordKnowledgeByGerman(any(), any()) }
            .returnsMany(wk1, wk2)

        var lastCapturedWK: WordKnowledge? = null
        coEvery { knowledgeRepository.upsertWordKnowledge(any()) } answers {
            lastCapturedWK = firstArg(); Unit
        }

        useCase(makeParams(score = 0.6f))
        useCase(makeParams(score = 0.4f))

        assertEquals(0.5f, lastCapturedWK?.pronunciationScore ?: -1f, 0.01f)
    }

    // ── Params data class ─────────────────────────────────────────────────────

    @Test
    fun params_defaultProblemSounds_isEmpty() {
        val params = RecordPronunciationResultUseCase.Params(
            userId = "u", word = "w", score = 0.5f
        )
        assertTrue(params.problemSounds.isEmpty())
    }

    @Test
    fun params_defaultSessionId_isNull() {
        val params = RecordPronunciationResultUseCase.Params(
            userId = "u", word = "w", score = 0.5f
        )
        assertNull(params.sessionId)
    }

    @Test
    fun params_defaultAttemptNumber_is1() {
        val params = RecordPronunciationResultUseCase.Params(
            userId = "u", word = "w", score = 0.5f
        )
        assertEquals(1, params.attemptNumber)
    }

    @Test
    fun params_creation_storesAllFields() {
        val params = RecordPronunciationResultUseCase.Params(
            userId        = "user1",
            word          = "Hund",
            score         = 0.75f,
            problemSounds = listOf("ü", "ch"),
            sessionId     = "s1",
            attemptNumber = 2
        )

        assertEquals("user1",         params.userId)
        assertEquals("Hund",          params.word)
        assertEquals(0.75f,           params.score, 0.001f)
        assertEquals(listOf("ü", "ch"), params.problemSounds)
        assertEquals("s1",            params.sessionId)
        assertEquals(2,               params.attemptNumber)
    }

    @Test
    fun params_copy_changesOnlySpecifiedField() {
        val original = makeParams()
        val copy     = original.copy(score = 0.1f)

        assertEquals(0.1f,            copy.score, 0.001f)
        assertEquals(original.userId, copy.userId)
        assertEquals(original.word,   copy.word)
    }

    @Test
    fun params_equals_twoIdenticalInstancesAreEqual() {
        val a = makeParams()
        val b = makeParams()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun params_equals_differentScoreNotEqual() {
        val a = makeParams(score = 0.3f)
        val b = makeParams(score = 0.7f)

        assertNotEquals(a, b)
    }

    @Test
    fun params_equals_differentProblemSoundsNotEqual() {
        val a = makeParams(problemSounds = listOf("ü"))
        val b = makeParams(problemSounds = listOf("ch"))

        assertNotEquals(a, b)
    }

    @Test
    fun params_equals_differentWordNotEqual() {
        val a = makeParams(word = "Hund")
        val b = makeParams(word = "Katze")

        assertNotEquals(a, b)
    }
}
