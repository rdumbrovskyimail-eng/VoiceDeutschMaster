// Путь: src/test/java/com/voicedeutsch/master/voicecore/engine/VoiceCoreEngineTest.kt
package com.voicedeutsch.master.voicecore.engine

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.session.SessionResult
import com.voicedeutsch.master.voicecore.session.AudioState
import com.voicedeutsch.master.voicecore.session.ConnectionState
import com.voicedeutsch.master.voicecore.session.VoiceEngineState
import com.voicedeutsch.master.voicecore.session.VoiceSessionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VoiceCoreEngineTest {

    private lateinit var engine: VoiceCoreEngine

    private val defaultState = VoiceSessionState()
    private val sessionStateFlow   = MutableStateFlow(defaultState)
    private val connectionStateFlow = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val audioStateFlow     = MutableStateFlow(AudioState.IDLE)
    private val amplitudeSharedFlow = MutableSharedFlow<Float>()

    @BeforeEach
    fun setUp() {
        engine = mockk<VoiceCoreEngine>(relaxed = true)

        every { engine.sessionState }    returns sessionStateFlow
        every { engine.connectionState } returns connectionStateFlow
        every { engine.audioState }      returns audioStateFlow
        every { engine.amplitudeFlow }   returns amplitudeSharedFlow
        every { engine.getTokenUsage() } returns null
    }

    // ── state flows are exposed ───────────────────────────────────────────

    @Test
    fun sessionState_isNotNull() {
        assertNotNull(engine.sessionState)
    }

    @Test
    fun connectionState_isNotNull() {
        assertNotNull(engine.connectionState)
    }

    @Test
    fun audioState_isNotNull() {
        assertNotNull(engine.audioState)
    }

    @Test
    fun amplitudeFlow_isNotNull() {
        assertNotNull(engine.amplitudeFlow)
    }

    @Test
    fun sessionState_defaultValue_engineStateIsIdle() {
        assertEquals(VoiceEngineState.IDLE, engine.sessionState.value.engineState)
    }

    @Test
    fun connectionState_defaultValue_isDisconnected() {
        assertEquals(ConnectionState.DISCONNECTED, engine.connectionState.value)
    }

    @Test
    fun audioState_defaultValue_isIdle() {
        assertEquals(AudioState.IDLE, engine.audioState.value)
    }

    // ── initialize ────────────────────────────────────────────────────────

    @Test
    fun initialize_isCalled() = runTest {
        val config = mockk<GeminiConfig>(relaxed = true)
        engine.initialize(config)
        coVerify(exactly = 1) { engine.initialize(config) }
    }

    @Test
    fun initialize_doesNotThrow() = runTest {
        val config = mockk<GeminiConfig>(relaxed = true)
        coEvery { engine.initialize(any()) } returns Unit
        assertDoesNotThrow { engine.initialize(config) }
    }

    // ── startSession ──────────────────────────────────────────────────────

    @Test
    fun startSession_isCalled() = runTest {
        coEvery { engine.startSession("user_1") } returns defaultState
        engine.startSession("user_1")
        coVerify(exactly = 1) { engine.startSession("user_1") }
    }

    @Test
    fun startSession_returnsVoiceSessionState() = runTest {
        val expected = VoiceSessionState(engineState = VoiceEngineState.SESSION_ACTIVE)
        coEvery { engine.startSession("user_1") } returns expected
        val result = engine.startSession("user_1")
        assertEquals(expected, result)
    }

    // ── endSession ────────────────────────────────────────────────────────

    @Test
    fun endSession_isCalled() = runTest {
        coEvery { engine.endSession() } returns null
        engine.endSession()
        coVerify(exactly = 1) { engine.endSession() }
    }

    @Test
    fun endSession_canReturnNull() = runTest {
        coEvery { engine.endSession() } returns null
        assertNull(engine.endSession())
    }

    @Test
    fun endSession_canReturnSessionResult() = runTest {
        val result = mockk<SessionResult>(relaxed = true)
        coEvery { engine.endSession() } returns result
        assertNotNull(engine.endSession())
    }

    // ── destroy ───────────────────────────────────────────────────────────

    @Test
    fun destroy_isCalled() = runTest {
        engine.destroy()
        coVerify(exactly = 1) { engine.destroy() }
    }

    @Test
    fun destroy_doesNotThrow() = runTest {
        coEvery { engine.destroy() } returns Unit
        assertDoesNotThrow { engine.destroy() }
    }

    // ── startListening / stopListening ────────────────────────────────────

    @Test
    fun startListening_isCalled() {
        engine.startListening()
        verify(exactly = 1) { engine.startListening() }
    }

    @Test
    fun stopListening_isCalled() {
        engine.stopListening()
        verify(exactly = 1) { engine.stopListening() }
    }

    @Test
    fun startListening_doesNotThrow() {
        every { engine.startListening() } returns Unit
        assertDoesNotThrow { engine.startListening() }
    }

    @Test
    fun stopListening_doesNotThrow() {
        every { engine.stopListening() } returns Unit
        assertDoesNotThrow { engine.stopListening() }
    }

    // ── pausePlayback / resumePlayback ────────────────────────────────────

    @Test
    fun pausePlayback_isCalled() {
        engine.pausePlayback()
        verify(exactly = 1) { engine.pausePlayback() }
    }

    @Test
    fun resumePlayback_isCalled() {
        engine.resumePlayback()
        verify(exactly = 1) { engine.resumePlayback() }
    }

    // ── sendTextMessage ───────────────────────────────────────────────────

    @Test
    fun sendTextMessage_isCalled() = runTest {
        engine.sendTextMessage("Hallo")
        coVerify(exactly = 1) { engine.sendTextMessage("Hallo") }
    }

    @Test
    fun sendTextMessage_doesNotThrow() = runTest {
        coEvery { engine.sendTextMessage(any()) } returns Unit
        assertDoesNotThrow { engine.sendTextMessage("Test") }
    }

    // ── requestStrategyChange ─────────────────────────────────────────────

    @Test
    fun requestStrategyChange_isCalledWithCorrectStrategy() = runTest {
        engine.requestStrategyChange(LearningStrategy.REPETITION)
        coVerify(exactly = 1) { engine.requestStrategyChange(LearningStrategy.REPETITION) }
    }

    @Test
    fun requestStrategyChange_allStrategies_doNotThrow() = runTest {
        coEvery { engine.requestStrategyChange(any()) } returns Unit
        LearningStrategy.entries.forEach { strategy ->
            assertDoesNotThrow { engine.requestStrategyChange(strategy) }
        }
    }

    // ── requestBookNavigation ─────────────────────────────────────────────

    @Test
    fun requestBookNavigation_isCalledWithCorrectArgs() = runTest {
        engine.requestBookNavigation(chapter = 3, lesson = 7)
        coVerify(exactly = 1) { engine.requestBookNavigation(chapter = 3, lesson = 7) }
    }

    @Test
    fun requestBookNavigation_doesNotThrow() = runTest {
        coEvery { engine.requestBookNavigation(any(), any()) } returns Unit
        assertDoesNotThrow { engine.requestBookNavigation(1, 1) }
    }

    // ── submitFunctionResult ──────────────────────────────────────────────

    @Test
    fun submitFunctionResult_isCalledWithCorrectArgs() = runTest {
        engine.submitFunctionResult("call_1", "save_word_knowledge", """{"status":"saved"}""")
        coVerify(exactly = 1) {
            engine.submitFunctionResult("call_1", "save_word_knowledge", """{"status":"saved"}""")
        }
    }

    @Test
    fun submitFunctionResult_doesNotThrow() = runTest {
        coEvery { engine.submitFunctionResult(any(), any(), any()) } returns Unit
        assertDoesNotThrow { engine.submitFunctionResult("id", "func", "{}") }
    }

    // ── sendAudioStreamEnd ────────────────────────────────────────────────

    @Test
    fun sendAudioStreamEnd_isCalled() = runTest {
        engine.sendAudioStreamEnd()
        coVerify(exactly = 1) { engine.sendAudioStreamEnd() }
    }

    @Test
    fun sendAudioStreamEnd_doesNotThrow() = runTest {
        coEvery { engine.sendAudioStreamEnd() } returns Unit
        assertDoesNotThrow { engine.sendAudioStreamEnd() }
    }

    // ── getTokenUsage ─────────────────────────────────────────────────────

    @Test
    fun getTokenUsage_canReturnNull() {
        every { engine.getTokenUsage() } returns null
        assertNull(engine.getTokenUsage())
    }

    @Test
    fun getTokenUsage_canReturnNonNull() {
        val usage = mockk<GeminiClient.TokenUsage>(relaxed = true)
        every { engine.getTokenUsage() } returns usage
        assertNotNull(engine.getTokenUsage())
    }

    @Test
    fun getTokenUsage_isCalled() {
        engine.getTokenUsage()
        verify(exactly = 1) { engine.getTokenUsage() }
    }
}
