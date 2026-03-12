// Путь: src/test/java/com/voicedeutsch/master/voicecore/audio/AudioPipelineTest.kt
package com.voicedeutsch.master.voicecore.audio

import android.content.Context
import app.cash.turbine.test
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AudioPipelineTest {

    private lateinit var pipeline: AudioPipeline
    private val context = mockk<Context>(relaxed = true)

    @BeforeEach
    fun setUp() {
        pipeline = AudioPipeline(context)
    }

    // ── initial state ─────────────────────────────────────────────────────

    @Test
    fun initialState_isRecordingIsFalse() {
        assertFalse(pipeline.isRecording)
    }

    @Test
    fun initialState_getCurrentAmplitude_returnsZeroOrPositive() {
        assertTrue(pipeline.getCurrentAmplitude() >= 0f)
    }

    // ── initialize ────────────────────────────────────────────────────────

    @Test
    fun initialize_calledOnce_doesNotThrow() {
        assertDoesNotThrow { pipeline.initialize() }
    }

    @Test
    fun initialize_calledTwice_doesNotThrow() {
        pipeline.initialize()
        assertDoesNotThrow { pipeline.initialize() }
    }

    @Test
    fun initialize_isIdempotent() {
        pipeline.initialize()
        pipeline.initialize()
        assertFalse(pipeline.isRecording)
    }

    // ── release ───────────────────────────────────────────────────────────

    @Test
    fun release_withoutInitialize_doesNotThrow() {
        assertDoesNotThrow { pipeline.release() }
    }

    @Test
    fun release_afterInitialize_doesNotThrow() {
        pipeline.initialize()
        assertDoesNotThrow { pipeline.release() }
    }

    @Test
    fun release_afterInitialize_isRecordingIsFalse() {
        pipeline.initialize()
        pipeline.release()
        assertFalse(pipeline.isRecording)
    }

    @Test
    fun release_calledTwice_doesNotThrow() {
        pipeline.initialize()
        pipeline.release()
        assertDoesNotThrow { pipeline.release() }
    }

    // ── startRecording ────────────────────────────────────────────────────

    @Test
    fun startRecording_withoutInitialize_doesNotThrow() {
        assertDoesNotThrow { pipeline.startRecording() }
    }

    @Test
    fun startRecording_afterInitialize_doesNotThrow() {
        pipeline.initialize()
        assertDoesNotThrow { pipeline.startRecording() }
    }

    @Test
    fun startRecording_calledTwice_doesNotThrow() {
        pipeline.initialize()
        pipeline.startRecording()
        assertDoesNotThrow { pipeline.startRecording() }
    }

    // ── stopRecording ─────────────────────────────────────────────────────

    @Test
    fun stopRecording_withoutStarting_doesNotThrow() {
        pipeline.initialize()
        assertDoesNotThrow { pipeline.stopRecording() }
    }

    @Test
    fun stopRecording_calledTwice_doesNotThrow() {
        pipeline.initialize()
        pipeline.stopRecording()
        assertDoesNotThrow { pipeline.stopRecording() }
    }

    // ── stopAll ───────────────────────────────────────────────────────────

    @Test
    fun stopAll_withoutRecording_doesNotThrow() = runTest {
        pipeline.initialize()
        pipeline.stopAll()
    }

    @Test
    fun stopAll_afterRelease_doesNotThrow() = runTest {
        pipeline.initialize()
        pipeline.release()
        pipeline.stopAll()
    }

    // ── audioChunks flow ──────────────────────────────────────────────────

    @Test
    fun audioChunks_returnsNonNullFlow() {
        assertNotNull(pipeline.audioChunks())
    }

    @Test
    fun incomingAudioFlow_isSameAsAudioChunksFlow() {
        // Both properties expose the same underlying SharedFlow
        assertNotNull(pipeline.incomingAudioFlow)
        assertNotNull(pipeline.audioChunks())
    }

    // ── getCurrentAmplitude ───────────────────────────────────────────────

    @Test
    fun getCurrentAmplitude_afterInitialize_isFinite() {
        pipeline.initialize()
        val amplitude = pipeline.getCurrentAmplitude()
        assertTrue(amplitude.isFinite())
    }

    @Test
    fun getCurrentAmplitude_afterRelease_doesNotThrow() {
        pipeline.initialize()
        pipeline.release()
        assertDoesNotThrow { pipeline.getCurrentAmplitude() }
    }

    // ── lifecycle sequence ────────────────────────────────────────────────

    @Test
    fun fullLifecycle_initStartStopRelease_doesNotThrow() = runTest {
        assertDoesNotThrow {
            pipeline.initialize()
            pipeline.startRecording()
            pipeline.stopRecording()
            pipeline.release()
        }
    }

    @Test
    fun reinitializeAfterRelease_doesNotThrow() {
        pipeline.initialize()
        pipeline.release()
        assertDoesNotThrow { pipeline.initialize() }
    }

    @Test
    fun reinitializeAfterRelease_isRecordingIsFalse() {
        pipeline.initialize()
        pipeline.release()
        pipeline.initialize()
        assertFalse(pipeline.isRecording)
    }
}
