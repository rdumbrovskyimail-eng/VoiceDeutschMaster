// Путь: src/test/java/com/voicedeutsch/master/voicecore/session/SessionHistoryTest.kt
package com.voicedeutsch.master.voicecore.session

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SessionHistoryTest {

    private lateinit var history: SessionHistory

    @BeforeEach
    fun setUp() {
        history = SessionHistory()
    }

    // ── Initial state ────────────────────────────────────────────────────

    @Test
    fun initialState_turnsIsEmpty() {
        assertTrue(history.turns.value.isEmpty())
    }

    // ── addUserTurn ──────────────────────────────────────────────────────

    @Test
    fun addUserTurn_appendsTurnWithUserRole() {
        history.addUserTurn("Hallo")
        val turn = history.turns.value.single()
        assertEquals("user", turn.role)
        assertEquals("Hallo", turn.text)
    }

    @Test
    fun addUserTurn_multipleCalls_appendsInOrder() {
        history.addUserTurn("first")
        history.addUserTurn("second")
        val turns = history.turns.value
        assertEquals(2, turns.size)
        assertEquals("first", turns[0].text)
        assertEquals("second", turns[1].text)
    }

    @Test
    fun addUserTurn_emitsNewStateFlow() = runTest {
        history.turns.test {
            awaitItem() // initial empty
            history.addUserTurn("Guten Tag")
            val updated = awaitItem()
            assertEquals(1, updated.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── addModelTurn ─────────────────────────────────────────────────────

    @Test
    fun addModelTurn_appendsTurnWithModelRole() {
        history.addModelTurn("Wie geht es Ihnen?")
        val turn = history.turns.value.single()
        assertEquals("model", turn.role)
        assertEquals("Wie geht es Ihnen?", turn.text)
    }

    @Test
    fun addModelTurn_emitsNewStateFlow() = runTest {
        history.turns.test {
            awaitItem() // initial empty
            history.addModelTurn("Antwort")
            val updated = awaitItem()
            assertEquals(1, updated.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── interleaved turns ────────────────────────────────────────────────

    @Test
    fun addMixedTurns_preservesRolesAndOrder() {
        history.addUserTurn("Frage")
        history.addModelTurn("Antwort")
        history.addUserTurn("Folgefrage")
        val turns = history.turns.value
        assertEquals(3, turns.size)
        assertEquals("user",  turns[0].role)
        assertEquals("model", turns[1].role)
        assertEquals("user",  turns[2].role)
    }

    // ── clear ────────────────────────────────────────────────────────────

    @Test
    fun clear_removesAllTurns() {
        history.addUserTurn("text")
        history.addModelTurn("response")
        history.clear()
        assertTrue(history.turns.value.isEmpty())
    }

    @Test
    fun clear_onEmptyHistory_remainsEmpty() {
        history.clear()
        assertTrue(history.turns.value.isEmpty())
    }

    @Test
    fun clear_emitsEmptyList() = runTest {
        history.addUserTurn("text")
        history.turns.test {
            awaitItem() // current non-empty state
            history.clear()
            val cleared = awaitItem()
            assertTrue(cleared.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── trimIfNeeded ─────────────────────────────────────────────────────

    @Test
    fun addTurns_exceedsMaxTurns_keepsOnlyLastMaxTurns() {
        repeat(SessionHistory.MAX_TURNS + 10) { i ->
            history.addUserTurn("turn_$i")
        }
        assertEquals(SessionHistory.MAX_TURNS, history.turns.value.size)
    }

    @Test
    fun addTurns_exceedsMaxTurns_retainsLastTurns() {
        repeat(SessionHistory.MAX_TURNS + 5) { i ->
            history.addUserTurn("turn_$i")
        }
        val turns = history.turns.value
        assertEquals("turn_${SessionHistory.MAX_TURNS + 4}", turns.last().text)
        assertEquals("turn_5", turns.first().text)
    }

    @Test
    fun addTurns_exactlyMaxTurns_noTrimOccurs() {
        repeat(SessionHistory.MAX_TURNS) { i ->
            history.addUserTurn("turn_$i")
        }
        assertEquals(SessionHistory.MAX_TURNS, history.turns.value.size)
        assertEquals("turn_0", history.turns.value.first().text)
    }

    @Test
    fun addTurns_oneBelowMaxTurns_noTrimOccurs() {
        repeat(SessionHistory.MAX_TURNS - 1) { i ->
            history.addUserTurn("turn_$i")
        }
        assertEquals(SessionHistory.MAX_TURNS - 1, history.turns.value.size)
    }

    // ── toPromptText ─────────────────────────────────────────────────────

    @Test
    fun toPromptText_emptyHistory_returnsEmptyString() {
        assertEquals("", history.toPromptText())
    }

    @Test
    fun toPromptText_singleUserTurn_formatsCorrectly() {
        history.addUserTurn("Hallo")
        val text = history.toPromptText()
        assertTrue(text.contains("USER: Hallo"))
    }

    @Test
    fun toPromptText_singleModelTurn_formatsCorrectly() {
        history.addModelTurn("Guten Tag")
        val text = history.toPromptText()
        assertTrue(text.contains("MODEL: Guten Tag"))
    }

    @Test
    fun toPromptText_multipleTurns_joinedWithNewline() {
        history.addUserTurn("Frage")
        history.addModelTurn("Antwort")
        val text = history.toPromptText()
        val lines = text.lines()
        assertEquals(2, lines.size)
        assertEquals("USER: Frage", lines[0])
        assertEquals("MODEL: Antwort", lines[1])
    }

    @Test
    fun toPromptText_roleIsUppercased() {
        history.addUserTurn("test")
        val text = history.toPromptText()
        assertTrue(text.startsWith("USER:"))
    }

    // ── Turn data class ──────────────────────────────────────────────────

    @Test
    fun turn_defaultTimestamp_isPositive() {
        history.addUserTurn("text")
        val turn = history.turns.value.single()
        assertTrue(turn.timestamp > 0L)
    }

    @Test
    fun turn_equalsAndHashCode() {
        val t1 = SessionHistory.Turn(role = "user", text = "hi", timestamp = 1000L)
        val t2 = SessionHistory.Turn(role = "user", text = "hi", timestamp = 1000L)
        assertEquals(t1, t2)
        assertEquals(t1.hashCode(), t2.hashCode())
    }

    @Test
    fun turn_copy_changesOnlySpecifiedField() {
        val original = SessionHistory.Turn(role = "user", text = "original", timestamp = 999L)
        val copied = original.copy(text = "updated")
        assertEquals("user", copied.role)
        assertEquals("updated", copied.text)
        assertEquals(999L, copied.timestamp)
    }

    // ── MAX_TURNS constant ───────────────────────────────────────────────

    @Test
    fun maxTurns_equals50() {
        assertEquals(50, SessionHistory.MAX_TURNS)
    }
}
