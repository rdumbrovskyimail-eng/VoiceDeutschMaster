// Путь: src/test/java/com/voicedeutsch/master/voicecore/audio/AudioPlayerTest.kt
package com.voicedeutsch.master.voicecore.audio

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AudioPlayerTest {

    private lateinit var player: AudioPlayer

    @BeforeEach
    fun setUp() {
        player = AudioPlayer()
    }

    // ── companion constants ───────────────────────────────────────────────

    @Test
    fun sampleRate_equals24000() {
        assertEquals(24_000, AudioPlayer.SAMPLE_RATE)
    }

    // ── initial state ─────────────────────────────────────────────────────

    @Test
    fun initialState_isPlayingIsFalse() {
        assertFalse(player.isPlaying)
    }

    // ── start ─────────────────────────────────────────────────────────────

    @Test
    fun start_doesNotThrow() {
        assertDoesNotThrow { player.start() }
    }

    @Test
    fun start_setsIsPlayingTrue() {
        player.start()
        assertTrue(player.isPlaying)
    }

    @Test
    fun start_calledTwice_doesNotThrow() {
        player.start()
        assertDoesNotThrow { player.start() }
    }

    @Test
    fun start_calledTwice_isPlayingRemainsTrue() {
        player.start()
        player.start()
        assertTrue(player.isPlaying)
    }

    @Test
    fun start_isIdempotent() {
        player.start()
        val firstCall = player.isPlaying
        player.start()
        val secondCall = player.isPlaying
        assertEquals(firstCall, secondCall)
    }

    // ── stop ──────────────────────────────────────────────────────────────

    @Test
    fun stop_withoutStart_doesNotThrow() {
        assertDoesNotThrow { player.stop() }
    }

    @Test
    fun stop_afterStart_setsIsPlayingFalse() {
        player.start()
        player.stop()
        assertFalse(player.isPlaying)
    }

    @Test
    fun stop_calledTwice_doesNotThrow() {
        player.start()
        player.stop()
        assertDoesNotThrow { player.stop() }
    }

    @Test
    fun stop_calledTwice_isPlayingRemainsIsFalse() {
        player.start()
        player.stop()
        player.stop()
        assertFalse(player.isPlaying)
    }

    // ── pause ─────────────────────────────────────────────────────────────

    @Test
    fun pause_withoutStart_doesNotThrow() {
        assertDoesNotThrow { player.pause() }
    }

    @Test
    fun pause_afterStart_doesNotThrow() {
        player.start()
        assertDoesNotThrow { player.pause() }
    }

    @Test
    fun pause_calledTwice_doesNotThrow() {
        player.start()
        player.pause()
        assertDoesNotThrow { player.pause() }
    }

    // ── resume ────────────────────────────────────────────────────────────

    @Test
    fun resume_withoutPause_doesNotThrow() {
        player.start()
        assertDoesNotThrow { player.resume() }
    }

    @Test
    fun resume_afterPause_doesNotThrow() {
        player.start()
        player.pause()
        assertDoesNotThrow { player.resume() }
    }

    @Test
    fun resume_withoutStart_doesNotThrow() {
        assertDoesNotThrow { player.resume() }
    }

    // ── flush ─────────────────────────────────────────────────────────────

    @Test
    fun flush_withoutStart_doesNotThrow() {
        assertDoesNotThrow { player.flush() }
    }

    @Test
    fun flush_afterStart_doesNotThrow() {
        player.start()
        assertDoesNotThrow { player.flush() }
    }

    @Test
    fun flush_afterStop_doesNotThrow() {
        player.start()
        player.stop()
        assertDoesNotThrow { player.flush() }
    }

    @Test
    fun flush_calledMultipleTimes_doesNotThrow() {
        player.start()
        repeat(3) { assertDoesNotThrow { player.flush() } }
    }

    // ── release ───────────────────────────────────────────────────────────

    @Test
    fun release_withoutStart_doesNotThrow() {
        assertDoesNotThrow { player.release() }
    }

    @Test
    fun release_afterStart_setsIsPlayingFalse() {
        player.start()
        player.release()
        assertFalse(player.isPlaying)
    }

    @Test
    fun release_calledTwice_doesNotThrow() {
        player.start()
        player.release()
        assertDoesNotThrow { player.release() }
    }

    // ── setVolume ─────────────────────────────────────────────────────────

    @Test
    fun setVolume_withoutStart_doesNotThrow() {
        assertDoesNotThrow { player.setVolume(0.5f) }
    }

    @Test
    fun setVolume_afterStart_doesNotThrow() {
        player.start()
        assertDoesNotThrow { player.setVolume(0.8f) }
    }

    @Test
    fun setVolume_zero_doesNotThrow() {
        player.start()
        assertDoesNotThrow { player.setVolume(0f) }
    }

    @Test
    fun setVolume_one_doesNotThrow() {
        player.start()
        assertDoesNotThrow { player.setVolume(1f) }
    }

    @Test
    fun setVolume_belowZero_coercedDoesNotThrow() {
        player.start()
        assertDoesNotThrow { player.setVolume(-1f) }
    }

    @Test
    fun setVolume_aboveOne_coercedDoesNotThrow() {
        player.start()
        assertDoesNotThrow { player.setVolume(2f) }
    }

    // ── write ─────────────────────────────────────────────────────────────

    @Test
    fun write_withoutStart_doesNotThrow() = runTest {
        assertDoesNotThrow { player.write(ByteArray(640)) }
    }

    @Test
    fun write_emptyArray_doesNotThrow() = runTest {
        player.start()
        assertDoesNotThrow { player.write(ByteArray(0)) }
    }

    @Test
    fun write_afterRelease_doesNotThrow() = runTest {
        player.start()
        player.release()
        assertDoesNotThrow { player.write(ByteArray(640)) }
    }

    @Test
    fun write_afterPause_doesNotThrow() = runTest {
        player.start()
        player.pause()
        assertDoesNotThrow { player.write(ByteArray(640)) }
    }

    // ── lifecycle sequences ───────────────────────────────────────────────

    @Test
    fun lifecycle_startStopStart_isPlayingTrue() {
        player.start()
        player.stop()
        player.start()
        assertTrue(player.isPlaying)
    }

    @Test
    fun lifecycle_startPauseResumeStop_doesNotThrow() {
        assertDoesNotThrow {
            player.start()
            player.pause()
            player.resume()
            player.stop()
        }
    }

    @Test
    fun lifecycle_fullCycle_doesNotThrow() = runTest {
        assertDoesNotThrow {
            player.start()
            player.write(ByteArray(640))
            player.pause()
            player.flush()
            player.resume()
            player.stop()
            player.release()
        }
    }

    @Test
    fun lifecycle_releaseWithoutStop_isPlayingFalse() {
        player.start()
        player.release()
        assertFalse(player.isPlaying)
    }
}
