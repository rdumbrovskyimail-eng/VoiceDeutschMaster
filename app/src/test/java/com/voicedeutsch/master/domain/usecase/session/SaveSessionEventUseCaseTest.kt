// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/session/SaveSessionEventUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.session

import com.voicedeutsch.master.domain.model.session.SessionEvent
import com.voicedeutsch.master.domain.model.session.SessionEventType
import com.voicedeutsch.master.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SaveSessionEventUseCaseTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var useCase: SaveSessionEventUseCase

    private val fixedNow  = 1_700_000_000_000L
    private val fixedUUID = "event-uuid-1"

    @BeforeEach
    fun setUp() {
        sessionRepository = io.mockk.mockk()
        useCase = SaveSessionEventUseCase(sessionRepository)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        mockkStatic("com.voicedeutsch.master.util.UUIDKt")

        every { com.voicedeutsch.master.util.DateUtils.nowTimestamp() } returns fixedNow
        every { com.voicedeutsch.master.util.generateUUID() }           returns fixedUUID

        coJustRun { sessionRepository.addSessionEvent(any()) }
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── invoke — repository call ──────────────────────────────────────────────

    @Test
    fun invoke_addSessionEventCalledOnce() = runTest {
        useCase("session1", SessionEventType.WORD_LEARNED)

        coVerify(exactly = 1) { sessionRepository.addSessionEvent(any()) }
    }

    @Test
    fun invoke_eventHasGeneratedUUID() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("session1", SessionEventType.WORD_LEARNED)

        assertEquals(fixedUUID, captured?.id)
    }

    @Test
    fun invoke_eventSessionIdMatchesParam() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("session-xyz", SessionEventType.WORD_LEARNED)

        assertEquals("session-xyz", captured?.sessionId)
    }

    @Test
    fun invoke_eventTypeMatchesParam() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.RULE_PRACTICED)

        assertEquals(SessionEventType.RULE_PRACTICED, captured?.eventType)
    }

    @Test
    fun invoke_eventTimestampIsNow() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED)

        assertEquals(fixedNow, captured?.timestamp)
        assertEquals(fixedNow, captured?.createdAt)
    }

    // ── invoke — empty details ────────────────────────────────────────────────

    @Test
    fun invoke_emptyDetails_detailsJsonIsEmptyObject() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED, emptyMap())

        assertEquals("{}", captured?.detailsJson)
    }

    @Test
    fun invoke_defaultDetails_detailsJsonIsEmptyObject() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED)

        assertEquals("{}", captured?.detailsJson)
    }

    // ── invoke — details JSON building: strings ───────────────────────────────

    @Test
    fun invoke_stringValue_encodedWithQuotes() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED, mapOf("word" to "Hund"))

        val json = captured?.detailsJson ?: ""
        assertTrue(json.contains("\"word\""))
        assertTrue(json.contains("\"Hund\""))
    }

    @Test
    fun invoke_stringValueWithDoubleQuotes_escapedCorrectly() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED, mapOf("msg" to "say \"hello\""))

        val json = captured?.detailsJson ?: ""
        assertTrue(json.contains("""\"hello\""""))
    }

    @Test
    fun invoke_stringValueWithBackslash_escapedCorrectly() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED, mapOf("path" to "a\\b"))

        val json = captured?.detailsJson ?: ""
        assertTrue(json.contains("\\\\"))
    }

    @Test
    fun invoke_stringValueWithNewline_escapedCorrectly() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED, mapOf("text" to "line1\nline2"))

        val json = captured?.detailsJson ?: ""
        assertTrue(json.contains("\\n"))
    }

    @Test
    fun invoke_stringValueWithCarriageReturn_escapedCorrectly() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED, mapOf("text" to "line1\rline2"))

        val json = captured?.detailsJson ?: ""
        assertTrue(json.contains("\\r"))
    }

    @Test
    fun invoke_stringValueWithTab_escapedCorrectly() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED, mapOf("text" to "col1\tcol2"))

        val json = captured?.detailsJson ?: ""
        assertTrue(json.contains("\\t"))
    }

    // ── invoke — details JSON building: booleans ──────────────────────────────

    @Test
    fun invoke_booleanTrueValue_encodedWithoutQuotes() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED, mapOf("correct" to true))

        val json = captured?.detailsJson ?: ""
        assertTrue(json.contains("\"correct\":true"))
    }

    @Test
    fun invoke_booleanFalseValue_encodedWithoutQuotes() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED, mapOf("correct" to false))

        val json = captured?.detailsJson ?: ""
        assertTrue(json.contains("\"correct\":false"))
    }

    // ── invoke — details JSON building: numbers ───────────────────────────────

    @Test
    fun invoke_intValue_encodedWithoutQuotes() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED, mapOf("count" to 42))

        val json = captured?.detailsJson ?: ""
        assertTrue(json.contains("\"count\":42"))
    }

    @Test
    fun invoke_floatValue_encodedWithoutQuotes() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED, mapOf("score" to 0.85f))

        val json = captured?.detailsJson ?: ""
        assertTrue(json.contains("\"score\":"))
        assertFalse(json.contains("\"0.85"))  // not quoted
    }

    @Test
    fun invoke_longValue_encodedWithoutQuotes() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED, mapOf("ts" to 1_700_000_000_000L))

        val json = captured?.detailsJson ?: ""
        assertTrue(json.contains("\"ts\":1700000000000"))
    }

    // ── invoke — details JSON building: other types ───────────────────────────

    @Test
    fun invoke_unknownTypeValue_encodedAsQuotedString() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        val obj = object { override fun toString() = "custom" }
        useCase("s1", SessionEventType.WORD_LEARNED, mapOf("obj" to obj))

        val json = captured?.detailsJson ?: ""
        assertTrue(json.contains("\"custom\""))
    }

    // ── invoke — details JSON structure ──────────────────────────────────────

    @Test
    fun invoke_nonEmptyDetails_jsonWrappedInBraces() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED, mapOf("k" to "v"))

        val json = captured?.detailsJson ?: ""
        assertTrue(json.startsWith("{"))
        assertTrue(json.endsWith("}"))
    }

    @Test
    fun invoke_multipleDetails_allKeysPresent() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED, mapOf(
            "word"    to "Hund",
            "correct" to true,
            "quality" to 5
        ))

        val json = captured?.detailsJson ?: ""
        assertTrue(json.contains("\"word\""))
        assertTrue(json.contains("\"correct\""))
        assertTrue(json.contains("\"quality\""))
    }

    @Test
    fun invoke_keyWithSpecialChars_keyEscaped() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_LEARNED, mapOf("ke\"y" to "val"))

        val json = captured?.detailsJson ?: ""
        assertTrue(json.contains("ke\\\"y"))
    }

    // ── invoke — all event types ──────────────────────────────────────────────

    @Test
    fun invoke_allEventTypes_eachPersistedCorrectly() = runTest {
        SessionEventType.entries.forEach { type ->
            var captured: SessionEvent? = null
            coEvery { sessionRepository.addSessionEvent(any()) } answers {
                captured = firstArg(); Unit
            }

            useCase("s1", type)

            assertEquals(type, captured?.eventType)
        }
    }

    // ── invoke — single-entry details ────────────────────────────────────────

    @Test
    fun invoke_singleStringEntry_validJson() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.STRATEGY_CHANGE, mapOf("strategy" to "REPETITION"))

        assertEquals("""{"strategy":"REPETITION"}""", captured?.detailsJson)
    }

    @Test
    fun invoke_singleBooleanEntry_validJson() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.WORD_REVIEWED, mapOf("correct" to true))

        assertEquals("""{"correct":true}""", captured?.detailsJson)
    }

    @Test
    fun invoke_singleIntEntry_validJson() = runTest {
        var captured: SessionEvent? = null
        coEvery { sessionRepository.addSessionEvent(any()) } answers {
            captured = firstArg(); Unit
        }

        useCase("s1", SessionEventType.RULE_PRACTICED, mapOf("quality" to 4))

        assertEquals("""{"quality":4}""", captured?.detailsJson)
    }
}
