// Путь: src/test/java/com/voicedeutsch/master/voicecore/session/SessionEnumsTest.kt
package com.voicedeutsch.master.voicecore.session

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SessionEnumsTest {

    // ── VoiceEngineState ─────────────────────────────────────────────────

    @Test
    fun voiceEngineState_entryCount_equals14() {
        assertEquals(14, VoiceEngineState.entries.size)
    }

    @Test
    fun voiceEngineState_containsAllExpectedValues() {
        val expected = setOf(
            VoiceEngineState.IDLE,
            VoiceEngineState.INITIALIZING,
            VoiceEngineState.CONTEXT_LOADING,
            VoiceEngineState.CONNECTING,
            VoiceEngineState.CONNECTED,
            VoiceEngineState.SESSION_ACTIVE,
            VoiceEngineState.LISTENING,
            VoiceEngineState.PROCESSING,
            VoiceEngineState.SPEAKING,
            VoiceEngineState.WAITING,
            VoiceEngineState.SESSION_ENDING,
            VoiceEngineState.SAVING,
            VoiceEngineState.ERROR,
            VoiceEngineState.RECONNECTING,
        )
        assertEquals(expected, VoiceEngineState.entries.toSet())
    }

    @Test
    fun voiceEngineState_nameResolution_idle() {
        assertEquals(VoiceEngineState.IDLE, VoiceEngineState.valueOf("IDLE"))
    }

    @Test
    fun voiceEngineState_nameResolution_error() {
        assertEquals(VoiceEngineState.ERROR, VoiceEngineState.valueOf("ERROR"))
    }

    @Test
    fun voiceEngineState_nameResolution_sessionActive() {
        assertEquals(VoiceEngineState.SESSION_ACTIVE, VoiceEngineState.valueOf("SESSION_ACTIVE"))
    }

    @Test
    fun voiceEngineState_nameResolution_reconnecting() {
        assertEquals(VoiceEngineState.RECONNECTING, VoiceEngineState.valueOf("RECONNECTING"))
    }

    @Test
    fun voiceEngineState_unknownName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            VoiceEngineState.valueOf("UNKNOWN")
        }
    }

    @Test
    fun voiceEngineState_ordinals_areUnique() {
        val ordinals = VoiceEngineState.entries.map { it.ordinal }
        assertEquals(ordinals.size, ordinals.toSet().size)
    }

    // ── ConnectionState ──────────────────────────────────────────────────

    @Test
    fun connectionState_entryCount_equals5() {
        assertEquals(5, ConnectionState.entries.size)
    }

    @Test
    fun connectionState_containsAllExpectedValues() {
        val expected = setOf(
            ConnectionState.DISCONNECTED,
            ConnectionState.CONNECTING,
            ConnectionState.CONNECTED,
            ConnectionState.RECONNECTING,
            ConnectionState.FAILED,
        )
        assertEquals(expected, ConnectionState.entries.toSet())
    }

    @Test
    fun connectionState_nameResolution_disconnected() {
        assertEquals(ConnectionState.DISCONNECTED, ConnectionState.valueOf("DISCONNECTED"))
    }

    @Test
    fun connectionState_nameResolution_failed() {
        assertEquals(ConnectionState.FAILED, ConnectionState.valueOf("FAILED"))
    }

    @Test
    fun connectionState_nameResolution_reconnecting() {
        assertEquals(ConnectionState.RECONNECTING, ConnectionState.valueOf("RECONNECTING"))
    }

    @Test
    fun connectionState_unknownName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            ConnectionState.valueOf("OFFLINE")
        }
    }

    @Test
    fun connectionState_ordinals_areUnique() {
        val ordinals = ConnectionState.entries.map { it.ordinal }
        assertEquals(ordinals.size, ordinals.toSet().size)
    }

    // ── AudioState ───────────────────────────────────────────────────────

    @Test
    fun audioState_entryCount_equals4() {
        assertEquals(4, AudioState.entries.size)
    }

    @Test
    fun audioState_containsAllExpectedValues() {
        val expected = setOf(
            AudioState.IDLE,
            AudioState.RECORDING,
            AudioState.PLAYING,
            AudioState.PAUSED,
        )
        assertEquals(expected, AudioState.entries.toSet())
    }

    @Test
    fun audioState_nameResolution_idle() {
        assertEquals(AudioState.IDLE, AudioState.valueOf("IDLE"))
    }

    @Test
    fun audioState_nameResolution_recording() {
        assertEquals(AudioState.RECORDING, AudioState.valueOf("RECORDING"))
    }

    @Test
    fun audioState_nameResolution_playing() {
        assertEquals(AudioState.PLAYING, AudioState.valueOf("PLAYING"))
    }

    @Test
    fun audioState_nameResolution_paused() {
        assertEquals(AudioState.PAUSED, AudioState.valueOf("PAUSED"))
    }

    @Test
    fun audioState_unknownName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            AudioState.valueOf("MUTED")
        }
    }

    @Test
    fun audioState_ordinals_areUnique() {
        val ordinals = AudioState.entries.map { it.ordinal }
        assertEquals(ordinals.size, ordinals.toSet().size)
    }
}
