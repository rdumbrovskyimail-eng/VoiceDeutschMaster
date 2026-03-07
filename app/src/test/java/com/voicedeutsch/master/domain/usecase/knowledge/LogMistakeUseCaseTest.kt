// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/knowledge/LogMistakeUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.MistakeLog
import com.voicedeutsch.master.domain.model.knowledge.MistakeType
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

class LogMistakeUseCaseTest {

    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var useCase: LogMistakeUseCase

    private val fixedNow  = 1_700_000_000_000L
    private val fixedUUID = "test-mistake-uuid"

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        knowledgeRepository = mockk()
        useCase = LogMistakeUseCase(knowledgeRepository)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        mockkStatic("com.voicedeutsch.master.util.UUIDKt")

        every { com.voicedeutsch.master.util.DateUtils.nowTimestamp() } returns fixedNow
        every { com.voicedeutsch.master.util.generateUUID() }           returns fixedUUID

        coEvery { knowledgeRepository.logMistake(any()) } returns Unit
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── invoke — happy path ───────────────────────────────────────────────────

    @Test
    fun invoke_validParams_logMistakeCalledOnce() = runTest {
        useCase(makeParams())

        coVerify(exactly = 1) { knowledgeRepository.logMistake(any()) }
    }

    @Test
    fun invoke_validParams_mistakeLogHasGeneratedUUID() = runTest {
        var captured: MistakeLog? = null
        coEvery { knowledgeRepository.logMistake(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(fixedUUID, captured?.id)
    }

    @Test
    fun invoke_validParams_mistakeLogTimestampSetToNow() = runTest {
        var captured: MistakeLog? = null
        coEvery { knowledgeRepository.logMistake(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams())

        assertEquals(fixedNow, captured?.timestamp)
        assertEquals(fixedNow, captured?.createdAt)
    }

    @Test
    fun invoke_validParams_mistakeLogUserIdMatchesParams() = runTest {
        var captured: MistakeLog? = null
        coEvery { knowledgeRepository.logMistake(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(userId = "userABC"))

        assertEquals("userABC", captured?.userId)
    }

    @Test
    fun invoke_validParams_mistakeLogSessionIdMatchesParams() = runTest {
        var captured: MistakeLog? = null
        coEvery { knowledgeRepository.logMistake(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(sessionId = "session-99"))

        assertEquals("session-99", captured?.sessionId)
    }

    @Test
    fun invoke_nullSessionId_mistakeLogSessionIdIsNull() = runTest {
        var captured: MistakeLog? = null
        coEvery { knowledgeRepository.logMistake(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(sessionId = null))

        assertNull(captured?.sessionId)
    }

    @Test
    fun invoke_validParams_mistakeLogTypeMatchesParams() = runTest {
        var captured: MistakeLog? = null
        coEvery { knowledgeRepository.logMistake(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(type = MistakeType.GRAMMAR))

        assertEquals(MistakeType.GRAMMAR, captured?.type)
    }

    @Test
    fun invoke_wordMistakeType_mistakeLogTypeIsWord() = runTest {
        var captured: MistakeLog? = null
        coEvery { knowledgeRepository.logMistake(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(type = MistakeType.WORD))

        assertEquals(MistakeType.WORD, captured?.type)
    }

    @Test
    fun invoke_pronunciationMistakeType_mistakeLogTypeIsPronunciation() = runTest {
        var captured: MistakeLog? = null
        coEvery { knowledgeRepository.logMistake(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(type = MistakeType.PRONUNCIATION))

        assertEquals(MistakeType.PRONUNCIATION, captured?.type)
    }

    @Test
    fun invoke_phraseMistakeType_mistakeLogTypeIsPhrase() = runTest {
        var captured: MistakeLog? = null
        coEvery { knowledgeRepository.logMistake(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(type = MistakeType.PHRASE))

        assertEquals(MistakeType.PHRASE, captured?.type)
    }

    @Test
    fun invoke_validParams_mistakeLogItemMatchesParams() = runTest {
        var captured: MistakeLog? = null
        coEvery { knowledgeRepository.logMistake(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(item = "der Tisch"))

        assertEquals("der Tisch", captured?.item)
    }

    @Test
    fun invoke_validParams_mistakeLogExpectedMatchesParams() = runTest {
        var captured: MistakeLog? = null
        coEvery { knowledgeRepository.logMistake(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(expected = "des Tisches"))

        assertEquals("des Tisches", captured?.expected)
    }

    @Test
    fun invoke_validParams_mistakeLogActualMatchesParams() = runTest {
        var captured: MistakeLog? = null
        coEvery { knowledgeRepository.logMistake(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(actual = "dem Tisch"))

        assertEquals("dem Tisch", captured?.actual)
    }

    @Test
    fun invoke_validParams_mistakeLogContextMatchesParams() = runTest {
        var captured: MistakeLog? = null
        coEvery { knowledgeRepository.logMistake(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(context = "Genitiv exercise"))

        assertEquals("Genitiv exercise", captured?.context)
    }

    @Test
    fun invoke_validParams_mistakeLogExplanationMatchesParams() = runTest {
        var captured: MistakeLog? = null
        coEvery { knowledgeRepository.logMistake(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(makeParams(explanation = "Confused Genitiv with Dativ"))

        assertEquals("Confused Genitiv with Dativ", captured?.explanation)
    }

    @Test
    fun invoke_defaultContextAndExplanation_storedAsEmptyStrings() = runTest {
        var captured: MistakeLog? = null
        coEvery { knowledgeRepository.logMistake(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase(
            LogMistakeUseCase.Params(
                userId    = "u",
                sessionId = "s",
                type      = MistakeType.WORD,
                item      = "Hund",
                expected  = "Hunde",
                actual    = "Hunds"
            )
        )

        assertEquals("", captured?.context)
        assertEquals("", captured?.explanation)
    }

    @Test
    fun invoke_allMistakeTypeValues_eachLoggedCorrectly() = runTest {
        MistakeType.entries.forEach { type ->
            var captured: MistakeLog? = null
            coEvery { knowledgeRepository.logMistake(any()) } answers {
                captured = firstArg(); Unit
            }

            useCase(makeParams(type = type))

            assertEquals(type, captured?.type)
        }
    }

    // ── Params data class ─────────────────────────────────────────────────────

    @Test
    fun params_creation_storesAllFields() {
        val params = LogMistakeUseCase.Params(
            userId      = "u1",
            sessionId   = "s1",
            type        = MistakeType.GRAMMAR,
            item        = "Artikel",
            expected    = "der",
            actual      = "die",
            context     = "noun exercise",
            explanation = "wrong gender"
        )

        assertEquals("u1",          params.userId)
        assertEquals("s1",          params.sessionId)
        assertEquals(MistakeType.GRAMMAR, params.type)
        assertEquals("Artikel",     params.item)
        assertEquals("der",         params.expected)
        assertEquals("die",         params.actual)
        assertEquals("noun exercise", params.context)
        assertEquals("wrong gender", params.explanation)
    }

    @Test
    fun params_defaultContextAndExplanation_areEmptyStrings() {
        val params = LogMistakeUseCase.Params(
            userId    = "u",
            sessionId = null,
            type      = MistakeType.WORD,
            item      = "laufen",
            expected  = "lief",
            actual    = "laufte"
        )

        assertEquals("", params.context)
        assertEquals("", params.explanation)
    }

    @Test
    fun params_defaultSessionId_isNull() {
        val params = LogMistakeUseCase.Params(
            userId   = "u",
            sessionId = null,
            type     = MistakeType.WORD,
            item     = "x",
            expected = "y",
            actual   = "z"
        )

        assertNull(params.sessionId)
    }

    @Test
    fun params_copy_changesOnlySpecifiedField() {
        val original = makeParams()
        val copy     = original.copy(item = "neues Wort")

        assertEquals("neues Wort", copy.item)
        assertEquals(original.userId,      copy.userId)
        assertEquals(original.sessionId,   copy.sessionId)
        assertEquals(original.type,        copy.type)
        assertEquals(original.expected,    copy.expected)
        assertEquals(original.actual,      copy.actual)
        assertEquals(original.context,     copy.context)
        assertEquals(original.explanation, copy.explanation)
    }

    @Test
    fun params_equals_twoIdenticalInstancesAreEqual() {
        val a = makeParams()
        val b = makeParams()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun params_equals_differentItemNotEqual() {
        val a = makeParams(item = "Hund")
        val b = makeParams(item = "Katze")

        assertNotEquals(a, b)
    }

    @Test
    fun params_equals_differentTypeNotEqual() {
        val a = makeParams(type = MistakeType.GRAMMAR)
        val b = makeParams(type = MistakeType.WORD)

        assertNotEquals(a, b)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeParams(
        userId      : String      = "user1",
        sessionId   : String?     = "session1",
        type        : MistakeType = MistakeType.GRAMMAR,
        item        : String      = "der Artikel",
        expected    : String      = "des Artikels",
        actual      : String      = "dem Artikel",
        context     : String      = "Genitiv test",
        explanation : String      = "Confused cases"
    ) = LogMistakeUseCase.Params(
        userId      = userId,
        sessionId   = sessionId,
        type        = type,
        item        = item,
        expected    = expected,
        actual      = actual,
        context     = context,
        explanation = explanation
    )
}
