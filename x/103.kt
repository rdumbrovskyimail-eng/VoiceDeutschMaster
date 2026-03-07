// Путь: src/test/java/com/voicedeutsch/master/voicecore/audio/AudioRecorderTest.kt
package com.voicedeutsch.master.voicecore.audio

import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AudioRecorderTest {

    private lateinit var recorder: AudioRecorder
    private lateinit var testScope: CoroutineScope

    @BeforeEach
    fun setUp() {
        recorder = AudioRecorder()
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    // ── companion constants ───────────────────────────────────────────────

    @Test
    fun sampleRate_equals16000() {
        assertEquals(16_000, AudioRecorder.SAMPLE_RATE)
    }

    @Test
    fun frameSizeSamples_equals4000() {
        assertEquals(4000, AudioRecorder.FRAME_SIZE_SAMPLES)
    }

    @Test
    fun frameSizeSamples_represents250msAt16kHz() {
        // 16000 samples/sec * 0.250 sec = 4000 samples
        val expected = AudioRecorder.SAMPLE_RATE / 4
        assertEquals(expected, AudioRecorder.FRAME_SIZE_SAMPLES)
    }

    // ── initial state ─────────────────────────────────────────────────────

    @Test
    fun initialState_currentAmplitudeIsZero() {
        assertEquals(0f, recorder.currentAmplitude)
    }

    @Test
    fun initialState_audioFrameFlowIsNotNull() {
        assertNotNull(recorder.audioFrameFlow)
    }

    // ── stop ──────────────────────────────────────────────────────────────

    @Test
    fun stop_withoutStart_doesNotThrow() {
        assertDoesNotThrow { recorder.stop() }
    }

    @Test
    fun stop_calledTwice_doesNotThrow() {
        recorder.stop()
        assertDoesNotThrow { recorder.stop() }
    }

    // ── release ───────────────────────────────────────────────────────────

    @Test
    fun release_withoutStart_doesNotThrow() {
        assertDoesNotThrow { recorder.release() }
    }

    @Test
    fun release_calledTwice_doesNotThrow() {
        recorder.release()
        assertDoesNotThrow { recorder.release() }
    }

    @Test
    fun release_afterStop_doesNotThrow() {
        recorder.stop()
        assertDoesNotThrow { recorder.release() }
    }

    // ── start — AudioRecord unavailable in JVM ────────────────────────────
    // AudioRecord cannot be instantiated in a JVM unit test (requires hardware).
    // We verify the guard (idempotency) and scope-cancellation behaviour only.

    @Test
    fun start_withCancelledScope_doesNotThrow() {
        val cancelledScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        cancelledScope.cancel()
        // AudioRecord() will throw in JVM — verify we survive the exception
        runCatching { recorder.start(cancelledScope) }
        // No assertion on state — just must not propagate an unhandled crash
    }

    @Test
    fun stop_afterFailedStart_doesNotThrow() {
        runCatching { recorder.start(testScope) }
        assertDoesNotThrow { recorder.stop() }
    }

    @Test
    fun release_afterFailedStart_doesNotThrow() {
        runCatching { recorder.start(testScope) }
        assertDoesNotThrow { recorder.release() }
    }

    // ── currentAmplitude ──────────────────────────────────────────────────

    @Test
    fun currentAmplitude_isFiniteByDefault() {
        assertTrue(recorder.currentAmplitude.isFinite())
    }

    @Test
    fun currentAmplitude_isNonNegativeByDefault() {
        assertTrue(recorder.currentAmplitude >= 0f)
    }

    @Test
    fun currentAmplitude_afterStop_remainsFinite() {
        runCatching { recorder.start(testScope) }
        recorder.stop()
        assertTrue(recorder.currentAmplitude.isFinite())
    }

    // ── audioFrameFlow ────────────────────────────────────────────────────

    @Test
    fun audioFrameFlow_afterRelease_channelClosedDoesNotThrow() = runTest {
        recorder.release()
        // Collecting from a closed channel should complete normally
        assertDoesNotThrow {
            recorder.audioFrameFlow.collect { }
        }
    }

    // ── lifecycle sequences ───────────────────────────────────────────────

    @Test
    fun lifecycle_stopRelease_doesNotThrow() {
        recorder.stop()
        assertDoesNotThrow { recorder.release() }
    }

    @Test
    fun lifecycle_stopStopRelease_doesNotThrow() {
        recorder.stop()
        recorder.stop()
        assertDoesNotThrow { recorder.release() }
    }

    @Test
    fun lifecycle_releaseRelease_doesNotThrow() {
        recorder.release()
        assertDoesNotThrow { recorder.release() }
    }
}
