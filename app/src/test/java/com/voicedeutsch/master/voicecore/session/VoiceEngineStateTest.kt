// Path: src/test/java/com/voicedeutsch/master/voicecore/session/VoiceEngineStateTest.kt
package com.voicedeutsch.master.voicecore.session

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VoiceEngineStateTest {

    @Test
    fun entries_size_equals14() {
        assertEquals(14, VoiceEngineState.entries.size)
    }

    @Test
    fun entries_containsIdle() {
        assertTrue(VoiceEngineState.entries.contains(VoiceEngineState.IDLE))
    }

    @Test
    fun entries_containsInitializing() {
        assertTrue(VoiceEngineState.entries.contains(VoiceEngineState.INITIALIZING))
    }

    @Test
    fun entries_containsContextLoading() {
        assertTrue(VoiceEngineState.entries.contains(VoiceEngineState.CONTEXT_LOADING))
    }

    @Test
    fun entries_containsConnecting() {
        assertTrue(VoiceEngineState.entries.contains(VoiceEngineState.CONNECTING))
    }

    @Test
    fun entries_containsConnected() {
        assertTrue(VoiceEngineState.entries.contains(VoiceEngineState.CONNECTED))
    }

    @Test
    fun entries_containsSessionActive() {
        assertTrue(VoiceEngineState.entries.contains(VoiceEngineState.SESSION_ACTIVE))
    }

    @Test
    fun entries_containsListening() {
        assertTrue(VoiceEngineState.entries.contains(VoiceEngineState.LISTENING))
    }

    @Test
    fun entries_containsProcessing() {
        assertTrue(VoiceEngineState.entries.contains(VoiceEngineState.PROCESSING))
    }

    @Test
    fun entries_containsSpeaking() {
        assertTrue(VoiceEngineState.entries.contains(VoiceEngineState.SPEAKING))
    }

    @Test
    fun entries_containsWaiting() {
        assertTrue(VoiceEngineState.entries.contains(VoiceEngineState.WAITING))
    }

    @Test
    fun entries_containsSessionEnding() {
        assertTrue(VoiceEngineState.entries.contains(VoiceEngineState.SESSION_ENDING))
    }

    @Test
    fun entries_containsSaving() {
        assertTrue(VoiceEngineState.entries.contains(VoiceEngineState.SAVING))
    }

    @Test
    fun entries_containsError() {
        assertTrue(VoiceEngineState.entries.contains(VoiceEngineState.ERROR))
    }

    @Test
    fun entries_containsReconnecting() {
        assertTrue(VoiceEngineState.entries.contains(VoiceEngineState.RECONNECTING))
    }

    @Test
    fun valueOf_idle_returnsCorrect() {
        assertEquals(VoiceEngineState.IDLE, VoiceEngineState.valueOf("IDLE"))
    }

    @Test
    fun valueOf_sessionActive_returnsCorrect() {
        assertEquals(VoiceEngineState.SESSION_ACTIVE, VoiceEngineState.valueOf("SESSION_ACTIVE"))
    }

    @Test
    fun valueOf_contextLoading_returnsCorrect() {
        assertEquals(VoiceEngineState.CONTEXT_LOADING, VoiceEngineState.valueOf("CONTEXT_LOADING"))
    }

    @Test
    fun valueOf_error_returnsCorrect() {
        assertEquals(VoiceEngineState.ERROR, VoiceEngineState.valueOf("ERROR"))
    }

    @Test
    fun valueOf_unknown_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            VoiceEngineState.valueOf("NOT_A_STATE")
        }
    }

    @Test
    fun allEntries_haveUniqueName() {
        val names = VoiceEngineState.entries.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun allEntries_haveUniqueOrdinal() {
        val ordinals = VoiceEngineState.entries.map { it.ordinal }
        assertEquals(ordinals.size, ordinals.toSet().size)
    }

    @Test
    fun idle_ordinal_isZero() {
        assertEquals(0, VoiceEngineState.IDLE.ordinal)
    }

    @Test
    fun activeStates_areDistinctFromInactiveStates() {
        val activeStates = setOf(
            VoiceEngineState.SESSION_ACTIVE,
            VoiceEngineState.LISTENING,
            VoiceEngineState.PROCESSING,
            VoiceEngineState.SPEAKING,
            VoiceEngineState.WAITING,
        )
        val inactiveStates = setOf(
            VoiceEngineState.IDLE,
            VoiceEngineState.ERROR,
            VoiceEngineState.SAVING,
        )
        assertTrue(activeStates.intersect(inactiveStates).isEmpty())
    }
}
