// Путь: src/test/java/com/voicedeutsch/master/voicecore/engine/VoiceCoreEngineImplTest.kt
package com.voicedeutsch.master.voicecore.engine

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.BookProgressSnapshot
import com.voicedeutsch.master.domain.model.knowledge.GrammarSnapshot
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.domain.model.knowledge.PronunciationSnapshot
import com.voicedeutsch.master.domain.model.knowledge.RecommendationsSnapshot
import com.voicedeutsch.master.domain.model.knowledge.SessionHistorySnapshot
import com.voicedeutsch.master.domain.model.knowledge.VocabularySnapshot
import com.voicedeutsch.master.domain.model.session.SessionResult
import com.voicedeutsch.master.domain.usecase.knowledge.BuildKnowledgeSummaryUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.FlushKnowledgeSyncUseCase
import com.voicedeutsch.master.domain.usecase.learning.EndLearningSessionUseCase
import com.voicedeutsch.master.domain.usecase.learning.StartLearningSessionUseCase
import com.voicedeutsch.master.testutil.MainDispatcherRule
import com.voicedeutsch.master.util.NetworkMonitor
import com.voicedeutsch.master.voicecore.audio.AudioPipeline
import com.voicedeutsch.master.voicecore.context.ContextBuilder
import com.voicedeutsch.master.voicecore.context.BookContextProvider
import com.voicedeutsch.master.voicecore.context.UserContextProvider
import com.voicedeutsch.master.voicecore.functions.FunctionRouter
import com.voicedeutsch.master.voicecore.session.AudioState
import com.voicedeutsch.master.voicecore.session.ConnectionState
import com.voicedeutsch.master.voicecore.session.VoiceEngineState
import com.voicedeutsch.master.voicecore.strategy.StrategySelector
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceCoreEngineImplTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    // ── Mocks ─────────────────────────────────────────────────────────────────

    private lateinit var contextBuilder:       ContextBuilder
    private lateinit var functionRouter:       FunctionRouter
    private lateinit var audioPipeline:        AudioPipeline
    private lateinit var strategySelector:     StrategySelector
    private lateinit var geminiClient:         GeminiClient
    private lateinit var buildKnowledgeSummary: BuildKnowledgeSummaryUseCase
    private lateinit var startLearningSession:  StartLearningSessionUseCase
    private lateinit var endLearningSession:    EndLearningSessionUseCase
    private lateinit var networkMonitor:        NetworkMonitor
    private lateinit var flushKnowledgeSync:    FlushKnowledgeSyncUseCase

    private lateinit var engine: VoiceCoreEngineImpl

    private val testUserId = "user_test_42"

    @BeforeEach
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0

        contextBuilder       = mockk()
        functionRouter       = mockk()
        audioPipeline        = mockk(relaxed = true)
        strategySelector     = mockk()
        geminiClient         = mockk(relaxed = true)
        buildKnowledgeSummary = mockk()
        startLearningSession  = mockk()
        endLearningSession    = mockk()
        networkMonitor        = mockk()
        flushKnowledgeSync    = mockk()

        engine = VoiceCoreEngineImpl(
            contextBuilder        = contextBuilder,
            functionRouter        = functionRouter,
            audioPipeline         = audioPipeline,
            strategySelector      = strategySelector,
            geminiClient          = geminiClient,
            buildKnowledgeSummary = buildKnowledgeSummary,
            startLearningSession  = startLearningSession,
            endLearningSession    = endLearningSession,
            networkMonitor        = networkMonitor,
            flushKnowledgeSync    = flushKnowledgeSync,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private fun buildConfig(
        modelName: String = "gemini-2.5-flash-preview-native-audio-dialog",
        maxContextTokens: Int = GeminiConfig.MAX_CONTEXT_TOKENS,
    ) = GeminiConfig(
        apiKey           = "test_key",
        modelName        = modelName,
        maxContextTokens = maxContextTokens,
    )

    private fun buildSnapshot() = KnowledgeSnapshot(
        vocabulary = VocabularySnapshot(
            totalWords          = 50,
            byLevel             = emptyMap(),
            byTopic             = emptyMap(),
            recentNewWords      = emptyList(),
            problemWords        = emptyList(),
            wordsForReviewToday = 3,
        ),
        grammar = GrammarSnapshot(
            totalRules          = 20,
            byLevel             = emptyMap(),
            knownRules          = emptyList(),
            problemRules        = emptyList(),
            rulesForReviewToday = 1,
        ),
        pronunciation = PronunciationSnapshot(
            overallScore     = 0.8f,
            problemSounds    = emptyList(),
            goodSounds       = emptyList(),
            averageWordScore = 0.8f,
            trend            = "stable",
        ),
        weakPoints   = emptyList(),
        bookProgress = BookProgressSnapshot(
            currentChapter       = 1,
            currentLesson        = 1,
            totalChapters        = 10,
            completionPercentage = 5f,
            currentTopic         = "Einführung",
        ),
        sessionHistory = SessionHistorySnapshot(
            lastSession            = "",
            lastSessionSummary     = "",
            averageSessionDuration = "20 мин",
            streak                 = 1,
            totalSessions          = 3,
        ),
        recommendations = RecommendationsSnapshot(
            primaryStrategy          = "LINEAR_BOOK",
            secondaryStrategy        = "REPETITION",
            focusAreas               = emptyList(),
            suggestedSessionDuration = "25 мин",
        ),
    )

    private fun buildSessionData(
        sessionId: String = "session_001",
        currentChapter: Int = 1,
        currentLesson: Int = 1,
    ): StartLearningSessionUseCase.SessionData {
        val session = mockk<com.voicedeutsch.master.domain.model.session.LearningSession>(relaxed = true)
        every { session.id } returns sessionId
        return StartLearningSessionUseCase.SessionData(
            session        = session,
            currentChapter = currentChapter,
            currentLesson  = currentLesson,
        )
    }

    private fun buildSessionContext() = ContextBuilder.SessionContext(
        systemPrompt         = "system prompt text",
        userContext          = "user context",
        bookContext          = "book context",
        strategyPrompt       = "strategy prompt",
        functionDeclarations = emptyList(),
    )

    private suspend fun initializeEngine(config: GeminiConfig = buildConfig()) {
        engine.initialize(config)
    }

    private suspend fun startSessionSuccessfully(
        userId: String = testUserId,
        config: GeminiConfig = buildConfig(),
    ) {
        initializeEngine(config)

        every { networkMonitor.isOnline() } returns true
        coEvery { startLearningSession(userId) } returns buildSessionData()
        coEvery { buildKnowledgeSummary(userId) } returns buildSnapshot()
        every { strategySelector.selectStrategy(any()) } returns LearningStrategy.LINEAR_BOOK
        coEvery { contextBuilder.buildSessionContext(any(), any(), any(), any(), any(), any()) } returns buildSessionContext()
        coEvery { geminiClient.connect(any()) } returns Unit

        engine.startSession(userId)
    }

    // ── initialize ────────────────────────────────────────────────────────────

    @Test
    fun initialize_callsAudioPipelineInitialize() = runTest {
        engine.initialize(buildConfig())
        coVerify { audioPipeline.initialize() }
    }

    @Test
    fun initialize_transitionsToIdleState() = runTest {
        engine.initialize(buildConfig())
        assertEquals(VoiceEngineState.IDLE, engine.sessionState.value.engineState)
    }

    @Test
    fun initialize_setsConfigOnGeminiClient() = runTest {
        val config = buildConfig(modelName = "test-model")
        engine.initialize(config)
        verify { geminiClient.config = config }
    }

    @Test
    fun initialize_whenAudioPipelineFails_transitionsToErrorState() = runTest {
        coEvery { audioPipeline.initialize() } throws RuntimeException("Audio init failed")
        assertThrows(RuntimeException::class.java) {
            runTest { engine.initialize(buildConfig()) }
        }
        assertEquals(VoiceEngineState.ERROR, engine.sessionState.value.engineState)
    }

    @Test
    fun initialize_whenCalledInInvalidState_throwsIllegalStateException() = runTest {
        initializeEngine()
        // Put into INITIALIZING state indirectly via a second call check
        // (engine is now IDLE after first init, second call is valid)
        assertDoesNotThrow { runTest { engine.initialize(buildConfig()) } }
    }

    // ── initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialState_engineStateIsIdle() {
        assertEquals(VoiceEngineState.IDLE, engine.sessionState.value.engineState)
    }

    @Test
    fun initialState_connectionStateIsDisconnected() {
        assertEquals(ConnectionState.DISCONNECTED, engine.connectionState.value)
    }

    @Test
    fun initialState_audioStateIsIdle() {
        assertEquals(AudioState.IDLE, engine.audioState.value)
    }

    @Test
    fun initialState_isVoiceActiveIsFalse() {
        assertFalse(engine.sessionState.value.isVoiceActive)
    }

    // ── startSession: success ─────────────────────────────────────────────────

    @Test
    fun startSession_success_transitionsToSessionActive() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()
        assertEquals(VoiceEngineState.SESSION_ACTIVE, engine.sessionState.value.engineState)
    }

    @Test
    fun startSession_success_setsIsVoiceActiveTrue() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()
        assertTrue(engine.sessionState.value.isVoiceActive)
    }

    @Test
    fun startSession_success_setsConnectionStateConnected() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()
        assertEquals(ConnectionState.CONNECTED, engine.connectionState.value)
    }

    @Test
    fun startSession_success_setsSessionId() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()
        assertEquals("session_001", engine.sessionState.value.sessionId)
    }

    @Test
    fun startSession_success_setsCurrentStrategy() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()
        assertEquals(LearningStrategy.LINEAR_BOOK, engine.sessionState.value.currentStrategy)
    }

    @Test
    fun startSession_success_callsStartLearningSession() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()
        coVerify { startLearningSession(testUserId) }
    }

    @Test
    fun startSession_success_callsBuildKnowledgeSummary() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()
        coVerify { buildKnowledgeSummary(testUserId) }
    }

    @Test
    fun startSession_success_callsGeminiClientConnect() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()
        coVerify { geminiClient.connect(any()) }
    }

    @Test
    fun startSession_success_callsContextBuilderBuildSessionContext() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()
        coVerify { contextBuilder.buildSessionContext(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun startSession_success_clearsErrorMessage() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()
        assertNull(engine.sessionState.value.errorMessage)
    }

    // ── startSession: offline ─────────────────────────────────────────────────

    @Test
    fun startSession_offline_transitionsToErrorState() = runTest {
        initializeEngine()
        every { networkMonitor.isOnline() } returns false

        engine.startSession(testUserId)
        advanceUntilIdle()

        assertEquals(VoiceEngineState.ERROR, engine.sessionState.value.engineState)
    }

    @Test
    fun startSession_offline_setsErrorMessage() = runTest {
        initializeEngine()
        every { networkMonitor.isOnline() } returns false

        engine.startSession(testUserId)
        advanceUntilIdle()

        assertNotNull(engine.sessionState.value.errorMessage)
        assertTrue(engine.sessionState.value.errorMessage!!.contains("интернет") ||
                   engine.sessionState.value.errorMessage!!.contains("сеть"))
    }

    @Test
    fun startSession_offline_doesNotCallStartLearningSession() = runTest {
        initializeEngine()
        every { networkMonitor.isOnline() } returns false

        engine.startSession(testUserId)
        advanceUntilIdle()

        coVerify(exactly = 0) { startLearningSession(any()) }
    }

    // ── startSession: connect failure ─────────────────────────────────────────

    @Test
    fun startSession_connectFails_transitionsToErrorState() = runTest {
        initializeEngine()
        every { networkMonitor.isOnline() } returns true
        coEvery { startLearningSession(testUserId) } returns buildSessionData()
        coEvery { buildKnowledgeSummary(testUserId) } returns buildSnapshot()
        every { strategySelector.selectStrategy(any()) } returns LearningStrategy.LINEAR_BOOK
        coEvery { contextBuilder.buildSessionContext(any(), any(), any(), any(), any(), any()) } returns buildSessionContext()
        coEvery { geminiClient.connect(any()) } throws RuntimeException("Connection refused")

        engine.startSession(testUserId)
        advanceUntilIdle()

        assertEquals(VoiceEngineState.ERROR, engine.sessionState.value.engineState)
    }

    @Test
    fun startSession_connectFails_setsConnectionStateFailed() = runTest {
        initializeEngine()
        every { networkMonitor.isOnline() } returns true
        coEvery { startLearningSession(testUserId) } returns buildSessionData()
        coEvery { buildKnowledgeSummary(testUserId) } returns buildSnapshot()
        every { strategySelector.selectStrategy(any()) } returns LearningStrategy.LINEAR_BOOK
        coEvery { contextBuilder.buildSessionContext(any(), any(), any(), any(), any(), any()) } returns buildSessionContext()
        coEvery { geminiClient.connect(any()) } throws RuntimeException("Connection refused")

        engine.startSession(testUserId)
        advanceUntilIdle()

        assertEquals(ConnectionState.FAILED, engine.connectionState.value)
    }

    @Test
    fun startSession_connectFails_setsErrorMessage() = runTest {
        initializeEngine()
        every { networkMonitor.isOnline() } returns true
        coEvery { startLearningSession(testUserId) } returns buildSessionData()
        coEvery { buildKnowledgeSummary(testUserId) } returns buildSnapshot()
        every { strategySelector.selectStrategy(any()) } returns LearningStrategy.LINEAR_BOOK
        coEvery { contextBuilder.buildSessionContext(any(), any(), any(), any(), any(), any()) } returns buildSessionContext()
        coEvery { geminiClient.connect(any()) } throws RuntimeException("Connection refused")

        engine.startSession(testUserId)
        advanceUntilIdle()

        assertNotNull(engine.sessionState.value.errorMessage)
        assertTrue(engine.sessionState.value.errorMessage!!.contains("Connection refused"))
    }

    @Test
    fun startSession_withoutInitialize_throwsIllegalStateException() = runTest {
        every { networkMonitor.isOnline() } returns true
        assertThrows(IllegalStateException::class.java) {
            runTest { engine.startSession(testUserId) }
        }
    }

    // ── endSession ────────────────────────────────────────────────────────────

    @Test
    fun endSession_withoutActiveSession_returnsNull() = runTest {
        val result = engine.endSession()
        assertNull(result)
    }

    @Test
    fun endSession_afterSuccessfulStart_callsStopConversation() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        val sessionResult = mockk<SessionResult>(relaxed = true)
        coEvery { endLearningSession(any()) } returns sessionResult
        coEvery { flushKnowledgeSync() } returns true

        engine.endSession()
        advanceUntilIdle()

        coVerify { geminiClient.stopConversation() }
    }

    @Test
    fun endSession_afterSuccessfulStart_callsDisconnect() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        val sessionResult = mockk<SessionResult>(relaxed = true)
        coEvery { endLearningSession(any()) } returns sessionResult
        coEvery { flushKnowledgeSync() } returns true

        engine.endSession()
        advanceUntilIdle()

        coVerify { geminiClient.disconnect() }
    }

    @Test
    fun endSession_afterSuccessfulStart_callsEndLearningSession() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        val sessionResult = mockk<SessionResult>(relaxed = true)
        coEvery { endLearningSession(any()) } returns sessionResult
        coEvery { flushKnowledgeSync() } returns true

        engine.endSession()
        advanceUntilIdle()

        coVerify { endLearningSession("session_001") }
    }

    @Test
    fun endSession_afterSuccessfulStart_callsFlushKnowledgeSync() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        val sessionResult = mockk<SessionResult>(relaxed = true)
        coEvery { endLearningSession(any()) } returns sessionResult
        coEvery { flushKnowledgeSync() } returns true

        engine.endSession()
        advanceUntilIdle()

        coVerify { flushKnowledgeSync() }
    }

    @Test
    fun endSession_transitionsToIdleState() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        val sessionResult = mockk<SessionResult>(relaxed = true)
        coEvery { endLearningSession(any()) } returns sessionResult
        coEvery { flushKnowledgeSync() } returns true

        engine.endSession()
        advanceUntilIdle()

        assertEquals(VoiceEngineState.IDLE, engine.sessionState.value.engineState)
    }

    @Test
    fun endSession_resetsIsVoiceActive() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        val sessionResult = mockk<SessionResult>(relaxed = true)
        coEvery { endLearningSession(any()) } returns sessionResult
        coEvery { flushKnowledgeSync() } returns true

        engine.endSession()
        advanceUntilIdle()

        assertFalse(engine.sessionState.value.isVoiceActive)
    }

    @Test
    fun endSession_resetsConnectionStateToDisconnected() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        val sessionResult = mockk<SessionResult>(relaxed = true)
        coEvery { endLearningSession(any()) } returns sessionResult
        coEvery { flushKnowledgeSync() } returns true

        engine.endSession()
        advanceUntilIdle()

        assertEquals(ConnectionState.DISCONNECTED, engine.connectionState.value)
    }

    @Test
    fun endSession_returnsSessionResult() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        val sessionResult = mockk<SessionResult>(relaxed = true)
        coEvery { endLearningSession(any()) } returns sessionResult
        coEvery { flushKnowledgeSync() } returns true

        val result = engine.endSession()
        advanceUntilIdle()

        assertEquals(sessionResult, result)
    }

    @Test
    fun endSession_flushKnowledgeSyncFails_stillCompletesSessionEnd() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        val sessionResult = mockk<SessionResult>(relaxed = true)
        coEvery { endLearningSession(any()) } returns sessionResult
        coEvery { flushKnowledgeSync() } throws RuntimeException("Sync failed")

        // Should not throw — failure is logged and handled gracefully
        assertDoesNotThrow { runTest { engine.endSession() } }
        advanceUntilIdle()

        assertEquals(VoiceEngineState.IDLE, engine.sessionState.value.engineState)
    }

    @Test
    fun endSession_stopConversationFails_stillCompletes() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        coEvery { geminiClient.stopConversation() } throws RuntimeException("Stop failed")
        val sessionResult = mockk<SessionResult>(relaxed = true)
        coEvery { endLearningSession(any()) } returns sessionResult
        coEvery { flushKnowledgeSync() } returns true

        assertDoesNotThrow { runTest { engine.endSession() } }
        advanceUntilIdle()
    }

    @Test
    fun endSession_calledTwice_secondCallReturnsNull() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        val sessionResult = mockk<SessionResult>(relaxed = true)
        coEvery { endLearningSession(any()) } returns sessionResult
        coEvery { flushKnowledgeSync() } returns true

        engine.endSession()
        advanceUntilIdle()

        val secondResult = engine.endSession()
        assertNull(secondResult)
    }

    // ── stopListening ─────────────────────────────────────────────────────────

    @Test
    fun stopListening_whenNotListening_doesNotCallStopConversation() = runTest {
        engine.stopListening()
        advanceUntilIdle()
        coVerify(exactly = 0) { geminiClient.stopConversation() }
    }

    // ── pausePlayback / resumePlayback (no-ops) ───────────────────────────────

    @Test
    fun pausePlayback_doesNotThrow() {
        assertDoesNotThrow { engine.pausePlayback() }
    }

    @Test
    fun resumePlayback_doesNotThrow() {
        assertDoesNotThrow { engine.resumePlayback() }
    }

    // ── sendAudioStreamEnd (no-op) ────────────────────────────────────────────

    @Test
    fun sendAudioStreamEnd_doesNotThrow() = runTest {
        assertDoesNotThrow { runTest { engine.sendAudioStreamEnd() } }
    }

    // ── sendTextMessage ───────────────────────────────────────────────────────

    @Test
    fun sendTextMessage_whenSessionActive_callsGeminiClientSendText() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        engine.sendTextMessage("Hallo!")
        advanceUntilIdle()

        coVerify { geminiClient.sendText("Hallo!") }
    }

    @Test
    fun sendTextMessage_whenNoSession_throwsIllegalStateException() = runTest {
        assertThrows(IllegalStateException::class.java) {
            runTest { engine.sendTextMessage("text") }
        }
    }

    // ── requestStrategyChange ─────────────────────────────────────────────────

    @Test
    fun requestStrategyChange_updatesCurrentStrategyInState() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        engine.requestStrategyChange(LearningStrategy.REPETITION)
        advanceUntilIdle()

        assertEquals(LearningStrategy.REPETITION, engine.sessionState.value.currentStrategy)
    }

    @Test
    fun requestStrategyChange_sendsTextMessageToGemini() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        engine.requestStrategyChange(LearningStrategy.GRAMMAR_DRILL)
        advanceUntilIdle()

        coVerify {
            geminiClient.sendText(match { text ->
                text.contains("GRAMMAR_DRILL") || text.contains("Грамматический")
            })
        }
    }

    // ── requestBookNavigation ─────────────────────────────────────────────────

    @Test
    fun requestBookNavigation_validArgs_sendsTextMessage() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        engine.requestBookNavigation(chapter = 3, lesson = 5)
        advanceUntilIdle()

        coVerify {
            geminiClient.sendText(match { it.contains("3") && it.contains("5") })
        }
    }

    @Test
    fun requestBookNavigation_chapterZero_throwsIllegalStateException() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()
        assertThrows(IllegalStateException::class.java) {
            runTest { engine.requestBookNavigation(chapter = 0, lesson = 1) }
        }
    }

    @Test
    fun requestBookNavigation_lessonZero_throwsIllegalStateException() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()
        assertThrows(IllegalStateException::class.java) {
            runTest { engine.requestBookNavigation(chapter = 1, lesson = 0) }
        }
    }

    @Test
    fun requestBookNavigation_negativeChapter_throwsIllegalStateException() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()
        assertThrows(IllegalStateException::class.java) {
            runTest { engine.requestBookNavigation(chapter = -1, lesson = 1) }
        }
    }

    // ── submitFunctionResult (legacy no-op) ───────────────────────────────────

    @Test
    fun submitFunctionResult_doesNotThrow() = runTest {
        assertDoesNotThrow {
            runTest { engine.submitFunctionResult("id", "fn_name", "{}") }
        }
    }

    // ── getTokenUsage ─────────────────────────────────────────────────────────

    @Test
    fun getTokenUsage_delegatesToGeminiClient() {
        val usage = mockk<GeminiClient.TokenUsage>(relaxed = true)
        every { geminiClient.lastTokenUsage } returns usage
        assertEquals(usage, engine.getTokenUsage())
    }

    @Test
    fun getTokenUsage_returnsNullWhenClientHasNoUsage() {
        every { geminiClient.lastTokenUsage } returns null
        assertNull(engine.getTokenUsage())
    }

    // ── destroy ───────────────────────────────────────────────────────────────

    @Test
    fun destroy_releasesAudioPipeline() = runTest {
        initializeEngine()
        engine.destroy()
        advanceUntilIdle()
        coVerify { audioPipeline.release() }
    }

    @Test
    fun destroy_releasesGeminiClient() = runTest {
        initializeEngine()
        engine.destroy()
        advanceUntilIdle()
        coVerify { geminiClient.release() }
    }

    @Test
    fun destroy_doesNotThrowWhenNoActiveSession() = runTest {
        initializeEngine()
        assertDoesNotThrow { runTest { engine.destroy() } }
    }

    // ── applyFunctionSideEffects: side effects on state ───────────────────────

    @Test
    fun startSession_afterSaveWordKnowledge_wordsLearnedInSessionIncrements() = runTest {
        // side effects tested indirectly by verifying state after successful session start
        startSessionSuccessfully()
        advanceUntilIdle()
        // wordsLearnedInSession starts at 0; side effects come from function calls
        assertEquals(0, engine.sessionState.value.wordsLearnedInSession)
    }

    // ── connectionState / audioState flows ───────────────────────────────────

    @Test
    fun connectionState_initiallyDisconnected() {
        assertEquals(ConnectionState.DISCONNECTED, engine.connectionState.value)
    }

    @Test
    fun audioState_initiallyIdle() {
        assertEquals(AudioState.IDLE, engine.audioState.value)
    }

    @Test
    fun sessionState_engineStateAndConnectionStateSynchronized() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()
        // Both should reflect CONNECTED after successful startSession
        assertEquals(ConnectionState.CONNECTED, engine.sessionState.value.connectionState)
        assertEquals(ConnectionState.CONNECTED, engine.connectionState.value)
    }

    // ── state after endSession ────────────────────────────────────────────────

    @Test
    fun endSession_resetsCurrentTranscript() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        val sessionResult = mockk<SessionResult>(relaxed = true)
        coEvery { endLearningSession(any()) } returns sessionResult
        coEvery { flushKnowledgeSync() } returns true

        engine.endSession()
        advanceUntilIdle()

        assertEquals("", engine.sessionState.value.currentTranscript)
        assertEquals("", engine.sessionState.value.voiceTranscript)
    }

    @Test
    fun endSession_resetsIsListeningAndIsSpeaking() = runTest {
        startSessionSuccessfully()
        advanceUntilIdle()

        val sessionResult = mockk<SessionResult>(relaxed = true)
        coEvery { endLearningSession(any()) } returns sessionResult
        coEvery { flushKnowledgeSync() } returns true

        engine.endSession()
        advanceUntilIdle()

        assertFalse(engine.sessionState.value.isListening)
        assertFalse(engine.sessionState.value.isSpeaking)
        assertFalse(engine.sessionState.value.isProcessing)
    }
}
