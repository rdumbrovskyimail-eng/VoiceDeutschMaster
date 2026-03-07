// Путь: src/test/java/com/voicedeutsch/master/presentation/screen/session/SessionViewModelTest.kt
package com.voicedeutsch.master.presentation.screen.session

import android.content.Context
import app.cash.turbine.test
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.testutil.MainDispatcherRule
import com.voicedeutsch.master.voicecore.engine.GeminiConfig
import com.voicedeutsch.master.voicecore.engine.VoiceCoreEngine
import com.voicedeutsch.master.voicecore.session.VoiceSessionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.Runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SessionViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var voiceCoreEngine: VoiceCoreEngine
    private lateinit var userRepository: UserRepository
    private lateinit var preferencesDataStore: UserPreferencesDataStore
    private lateinit var context: Context
    private lateinit var sut: SessionViewModel

    private val sessionStateFlow = MutableStateFlow(VoiceSessionState())
    private val amplitudeSharedFlow = MutableSharedFlow<Float>(extraBufferCapacity = 64)

    private fun buildGeminiConfig() = GeminiConfig(
        modelName = GeminiConfig.MODEL_GEMINI_LIVE,
        temperature = GeminiConfig.DEFAULT_TEMPERATURE,
        topP = GeminiConfig.DEFAULT_TOP_P,
        topK = GeminiConfig.DEFAULT_TOP_K,
        voiceName = GeminiConfig.DEFAULT_VOICE,
        maxContextTokens = GeminiConfig.MAX_CONTEXT_TOKENS,
        transcriptionConfig = GeminiConfig.TranscriptionConfig(
            inputTranscriptionEnabled = false,
            outputTranscriptionEnabled = false,
        ),
    )

    @BeforeEach
    fun setUp() {
        voiceCoreEngine = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        preferencesDataStore = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { voiceCoreEngine.sessionState } returns sessionStateFlow
        every { voiceCoreEngine.amplitudeFlow } returns amplitudeSharedFlow

        coEvery { userRepository.getActiveUserId() } returns "user_1"
        coEvery { preferencesDataStore.loadGeminiConfig() } returns buildGeminiConfig()
        coEvery { voiceCoreEngine.initialize(any()) } just Runs
        coEvery { voiceCoreEngine.startSession(any()) } just Runs
        coEvery { voiceCoreEngine.endSession() } returns mockk(relaxed = true)
        coEvery { voiceCoreEngine.sendTextMessage(any()) } just Runs

        sut = SessionViewModel(
            voiceCoreEngine = voiceCoreEngine,
            userRepository = userRepository,
            preferencesDataStore = preferencesDataStore,
            context = context,
        )
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialState_isLoadingFalse() {
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun initialState_isSessionActiveFalse() {
        assertFalse(sut.uiState.value.isSessionActive)
    }

    @Test
    fun initialState_errorMessageNull() {
        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun initialState_sessionResultNull() {
        assertNull(sut.uiState.value.sessionResult)
    }

    @Test
    fun initialState_snackbarMessageNull() {
        assertNull(sut.uiState.value.snackbarMessage)
    }

    @Test
    fun initialState_currentAmplitude_isZero() {
        assertEquals(0f, sut.currentAmplitude.floatValue, 0.001f)
    }

    @Test
    fun voiceState_exposesEngineSessionState() {
        assertSame(sessionStateFlow, sut.voiceState)
    }

    // ── StartSession ──────────────────────────────────────────────────────────

    @Test
    fun startSession_success_isSessionActiveTrue() = runTest {
        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(sut.uiState.value.isSessionActive)
    }

    @Test
    fun startSession_success_isLoadingFalse() = runTest {
        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun startSession_success_noErrorMessage() = runTest {
        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun startSession_success_showHintFalse() = runTest {
        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.showHint)
    }

    @Test
    fun startSession_callsInitializeWithGeminiConfig() = runTest {
        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { voiceCoreEngine.initialize(any<GeminiConfig>()) }
    }

    @Test
    fun startSession_callsStartSessionWithUserId() = runTest {
        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { voiceCoreEngine.startSession("user_1") }
    }

    @Test
    fun startSession_noActiveUser_setsErrorMessage() = runTest {
        coEvery { userRepository.getActiveUserId() } returns null

        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("onboarding"))
        assertFalse(sut.uiState.value.isLoading)
        assertFalse(sut.uiState.value.isSessionActive)
    }

    @Test
    fun startSession_engineThrows_setsErrorMessage() = runTest {
        coEvery { voiceCoreEngine.startSession(any()) } throws RuntimeException("Connection error")

        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("Connection error"))
        assertFalse(sut.uiState.value.isLoading)
        assertFalse(sut.uiState.value.isSessionActive)
    }

    @Test
    fun startSession_initializeThrows_setsErrorMessage() = runTest {
        coEvery { voiceCoreEngine.initialize(any()) } throws RuntimeException("Init failed")

        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertFalse(sut.uiState.value.isSessionActive)
    }

    @Test
    fun startSession_engineThrowsWithNullMessage_setsUnknownErrorMessage() = runTest {
        coEvery { voiceCoreEngine.startSession(any()) } throws RuntimeException(null as String?)

        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("Unknown error"))
    }

    @Test
    fun startSession_amplitudeFlow_updatesCurrentAmplitude() = runTest {
        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        amplitudeSharedFlow.emit(0.75f)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0.75f, sut.currentAmplitude.floatValue, 0.001f)
    }

    @Test
    fun startSession_amplitudeFlow_multipleValues_usesLatest() = runTest {
        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        amplitudeSharedFlow.emit(0.2f)
        amplitudeSharedFlow.emit(0.5f)
        amplitudeSharedFlow.emit(0.9f)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0.9f, sut.currentAmplitude.floatValue, 0.001f)
    }

    // ── EndSession ────────────────────────────────────────────────────────────

    @Test
    fun endSession_isSessionActiveFalse() = runTest {
        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SessionEvent.EndSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.isSessionActive)
    }

    @Test
    fun endSession_isLoadingFalse() = runTest {
        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SessionEvent.EndSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun endSession_callsEndSession() = runTest {
        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SessionEvent.EndSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { voiceCoreEngine.endSession() }
    }

    @Test
    fun endSession_resetsCurrentAmplitudeToZero() = runTest {
        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        amplitudeSharedFlow.emit(0.8f)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SessionEvent.EndSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0f, sut.currentAmplitude.floatValue, 0.001f)
    }

    @Test
    fun endSession_afterEnd_amplitudeFlowNoLongerUpdatesAmplitude() = runTest {
        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SessionEvent.EndSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        amplitudeSharedFlow.emit(0.99f)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0f, sut.currentAmplitude.floatValue, 0.001f)
    }

    @Test
    fun endSession_engineThrows_sessionResultIsNull() = runTest {
        coEvery { voiceCoreEngine.endSession() } throws RuntimeException("End failed")

        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SessionEvent.EndSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sut.uiState.value.sessionResult)
        assertFalse(sut.uiState.value.isSessionActive)
    }

    @Test
    fun endSession_withoutStarting_doesNotCrash() = runTest {
        assertDoesNotThrow {
            sut.onEvent(SessionEvent.EndSession)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        }
    }

    // ── PauseResume (TogglePause) ─────────────────────────────────────────────

    @Test
    fun togglePause_whenListening_callsStopListening() = runTest {
        sessionStateFlow.value = VoiceSessionState(isListening = true, isSessionActive = true)

        sut.onEvent(SessionEvent.PauseResume)

        verify { voiceCoreEngine.stopListening() }
    }

    @Test
    fun togglePause_whenSessionActiveNotListening_callsStartListening() = runTest {
        sessionStateFlow.value = VoiceSessionState(isListening = false, isSessionActive = true)

        sut.onEvent(SessionEvent.PauseResume)

        verify { voiceCoreEngine.startListening() }
    }

    @Test
    fun togglePause_whenSessionInactive_neitherStartNorStop() = runTest {
        sessionStateFlow.value = VoiceSessionState(isListening = false, isSessionActive = false)

        sut.onEvent(SessionEvent.PauseResume)

        verify(exactly = 0) { voiceCoreEngine.startListening() }
        verify(exactly = 0) { voiceCoreEngine.stopListening() }
    }

    // ── SendTextMessage ───────────────────────────────────────────────────────

    @Test
    fun sendText_nonBlankText_callsEngineWithText() = runTest {
        sut.onEvent(SessionEvent.SendTextMessage("Hallo"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { voiceCoreEngine.sendTextMessage("Hallo") }
    }

    @Test
    fun sendText_blankText_doesNotCallEngine() = runTest {
        sut.onEvent(SessionEvent.SendTextMessage("   "))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { voiceCoreEngine.sendTextMessage(any()) }
    }

    @Test
    fun sendText_emptyText_doesNotCallEngine() = runTest {
        sut.onEvent(SessionEvent.SendTextMessage(""))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { voiceCoreEngine.sendTextMessage(any()) }
    }

    @Test
    fun sendText_engineThrows_setsSnackbarMessage() = runTest {
        coEvery { voiceCoreEngine.sendTextMessage(any()) } throws RuntimeException("Send error")

        sut.onEvent(SessionEvent.SendTextMessage("Test"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.snackbarMessage)
        assertTrue(sut.uiState.value.snackbarMessage!!.contains("Send error"))
    }

    @Test
    fun sendText_engineThrowsNullMessage_setsDefaultSnackbar() = runTest {
        coEvery { voiceCoreEngine.sendTextMessage(any()) } throws RuntimeException(null as String?)

        sut.onEvent(SessionEvent.SendTextMessage("Test"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.snackbarMessage)
        assertTrue(sut.uiState.value.snackbarMessage!!.contains("Failed"))
    }

    // ── DismissError ──────────────────────────────────────────────────────────

    @Test
    fun dismissError_clearsErrorMessage() = runTest {
        coEvery { voiceCoreEngine.startSession(any()) } throws RuntimeException("some error")

        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sut.uiState.value.errorMessage)

        sut.onEvent(SessionEvent.DismissError)

        assertNull(sut.uiState.value.errorMessage)
    }

    // ── DismissResult ─────────────────────────────────────────────────────────

    @Test
    fun dismissResult_clearsSessionResult() = runTest {
        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SessionEvent.EndSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(SessionEvent.DismissResult)

        assertNull(sut.uiState.value.sessionResult)
    }

    // ── ToggleTextInput ───────────────────────────────────────────────────────

    @Test
    fun toggleTextInput_whenFalse_becomesTrue() = runTest {
        assertFalse(sut.uiState.value.showTextInput)

        sut.onEvent(SessionEvent.ToggleTextInput)

        assertTrue(sut.uiState.value.showTextInput)
    }

    @Test
    fun toggleTextInput_whenTrue_becomesFalse() = runTest {
        sut.onEvent(SessionEvent.ToggleTextInput)
        assertTrue(sut.uiState.value.showTextInput)

        sut.onEvent(SessionEvent.ToggleTextInput)

        assertFalse(sut.uiState.value.showTextInput)
    }

    // ── DismissHint ───────────────────────────────────────────────────────────

    @Test
    fun dismissHint_setsShowHintFalse() = runTest {
        sut.onEvent(SessionEvent.DismissHint)

        assertFalse(sut.uiState.value.showHint)
    }

    // ── ConsumeSnackbar ───────────────────────────────────────────────────────

    @Test
    fun consumeSnackbar_clearsSnackbarMessage() = runTest {
        coEvery { voiceCoreEngine.sendTextMessage(any()) } throws RuntimeException("err")

        sut.onEvent(SessionEvent.SendTextMessage("test"))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sut.uiState.value.snackbarMessage)

        sut.onEvent(SessionEvent.ConsumeSnackbar)

        assertNull(sut.uiState.value.snackbarMessage)
    }

    // ── PermissionDenied ──────────────────────────────────────────────────────

    @Test
    fun permissionDenied_setsErrorMessageWithMicText() = runTest {
        sut.onEvent(SessionEvent.PermissionDenied)

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("микрофон"))
        assertFalse(sut.uiState.value.isLoading)
    }

    // ── uiState Flow ──────────────────────────────────────────────────────────

    @Test
    fun uiState_flow_emitsOnStartSession() = runTest {
        sut.uiState.test {
            awaitItem() // initial

            sut.onEvent(SessionEvent.StartSession)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            // At least one more emission
            val states = mutableListOf<SessionUiState>()
            while (true) {
                val item = awaitItem()
                states.add(item)
                if (!item.isLoading) break
            }

            assertTrue(states.last().isSessionActive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_flow_emitsOnEndSession() = runTest {
        sut.onEvent(SessionEvent.StartSession)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.test {
            awaitItem() // session active

            sut.onEvent(SessionEvent.EndSession)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            val states = mutableListOf<SessionUiState>()
            while (true) {
                val item = awaitItem()
                states.add(item)
                if (!item.isLoading) break
            }

            assertFalse(states.last().isSessionActive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── SettingsUiState data class ────────────────────────────────────────────

    @Test
    fun sessionUiState_defaultValues_areCorrect() {
        val state = SessionUiState()
        assertFalse(state.isLoading)
        assertFalse(state.isSessionActive)
        assertNull(state.errorMessage)
        assertNull(state.sessionResult)
        assertNull(state.snackbarMessage)
        assertFalse(state.showTextInput)
    }

    @Test
    fun sessionUiState_copy_changesOneField() {
        val original = SessionUiState()
        val copy = original.copy(isLoading = true)
        assertTrue(copy.isLoading)
        assertFalse(copy.isSessionActive)
    }

    @Test
    fun sessionUiState_equals_twoIdenticalInstances() {
        val a = SessionUiState()
        val b = SessionUiState()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
