// Path: src/test/java/com/voicedeutsch/master/voicecore/session/ConnectionAndAudioStateTest.kt
package com.voicedeutsch.master.voicecore.session

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ════════════════════════════════════════════════════════════════════════════
// ConnectionState
// ════════════════════════════════════════════════════════════════════════════

class ConnectionStateTest {

    @Test
    fun entries_size_equals5() {
        assertEquals(5, ConnectionState.entries.size)
    }

    @Test
    fun entries_containsDisconnected() {
        assertTrue(ConnectionState.entries.contains(ConnectionState.DISCONNECTED))
    }

    @Test
    fun entries_containsConnecting() {
        assertTrue(ConnectionState.entries.contains(ConnectionState.CONNECTING))
    }

    @Test
    fun entries_containsConnected() {
        assertTrue(ConnectionState.entries.contains(ConnectionState.CONNECTED))
    }

    @Test
    fun entries_containsReconnecting() {
        assertTrue(ConnectionState.entries.contains(ConnectionState.RECONNECTING))
    }

    @Test
    fun entries_containsFailed() {
        assertTrue(ConnectionState.entries.contains(ConnectionState.FAILED))
    }

    @Test
    fun valueOf_disconnected_returnsCorrect() {
        assertEquals(ConnectionState.DISCONNECTED, ConnectionState.valueOf("DISCONNECTED"))
    }

    @Test
    fun valueOf_connected_returnsCorrect() {
        assertEquals(ConnectionState.CONNECTED, ConnectionState.valueOf("CONNECTED"))
    }

    @Test
    fun valueOf_failed_returnsCorrect() {
        assertEquals(ConnectionState.FAILED, ConnectionState.valueOf("FAILED"))
    }

    @Test
    fun valueOf_unknown_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            ConnectionState.valueOf("UNKNOWN_STATE")
        }
    }

    @Test
    fun allEntries_haveUniqueName() {
        val names = ConnectionState.entries.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun allEntries_haveUniqueOrdinal() {
        val ordinals = ConnectionState.entries.map { it.ordinal }
        assertEquals(ordinals.size, ordinals.toSet().size)
    }

    @Test
    fun disconnected_ordinal_isZero() {
        assertEquals(0, ConnectionState.DISCONNECTED.ordinal)
    }

    @Test
    fun terminalStates_includeDisconnectedAndFailed() {
        val terminal = setOf(ConnectionState.DISCONNECTED, ConnectionState.FAILED)
        val nonTerminal = setOf(ConnectionState.CONNECTING, ConnectionState.RECONNECTING, ConnectionState.CONNECTED)
        assertTrue(terminal.intersect(nonTerminal).isEmpty())
    }

    @Test
    fun allExpectedValues_arePresent() {
        val expected = setOf(
            ConnectionState.DISCONNECTED,
            ConnectionState.CONNECTING,
            ConnectionState.CONNECTED,
            ConnectionState.RECONNECTING,
            ConnectionState.FAILED,
        )
        assertEquals(expected, ConnectionState.entries.toSet())
    }
}

// ════════════════════════════════════════════════════════════════════════════
// AudioState
// ════════════════════════════════════════════════════════════════════════════

class AudioStateTest {

    @Test
    fun entries_size_equals4() {
        assertEquals(4, AudioState.entries.size)
    }

    @Test
    fun entries_containsIdle() {
        assertTrue(AudioState.entries.contains(AudioState.IDLE))
    }

    @Test
    fun entries_containsRecording() {
        assertTrue(AudioState.entries.contains(AudioState.RECORDING))
    }

    @Test
    fun entries_containsPlaying() {
        assertTrue(AudioState.entries.contains(AudioState.PLAYING))
    }

    @Test
    fun entries_containsPaused() {
        assertTrue(AudioState.entries.contains(AudioState.PAUSED))
    }

    @Test
    fun valueOf_idle_returnsCorrect() {
        assertEquals(AudioState.IDLE, AudioState.valueOf("IDLE"))
    }

    @Test
    fun valueOf_recording_returnsCorrect() {
        assertEquals(AudioState.RECORDING, AudioState.valueOf("RECORDING"))
    }

    @Test
    fun valueOf_playing_returnsCorrect() {
        assertEquals(AudioState.PLAYING, AudioState.valueOf("PLAYING"))
    }

    @Test
    fun valueOf_paused_returnsCorrect() {
        assertEquals(AudioState.PAUSED, AudioState.valueOf("PAUSED"))
    }

    @Test
    fun valueOf_unknown_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            AudioState.valueOf("UNKNOWN_AUDIO")
        }
    }

    @Test
    fun allEntries_haveUniqueName() {
        val names = AudioState.entries.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun allEntries_haveUniqueOrdinal() {
        val ordinals = AudioState.entries.map { it.ordinal }
        assertEquals(ordinals.size, ordinals.toSet().size)
    }

    @Test
    fun idle_ordinal_isZero() {
        assertEquals(0, AudioState.IDLE.ordinal)
    }

    @Test
    fun activeAudioStates_doNotContainIdle() {
        val active = setOf(AudioState.RECORDING, AudioState.PLAYING)
        assertFalse(active.contains(AudioState.IDLE))
    }

    @Test
    fun allExpectedValues_arePresent() {
        val expected = setOf(
            AudioState.IDLE,
            AudioState.RECORDING,
            AudioState.PLAYING,
            AudioState.PAUSED,
        )
        assertEquals(expected, AudioState.entries.toSet())
    }
}
