package com.voicedeutsch.master.voicecore.engine

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.session.SessionResult
import com.voicedeutsch.master.domain.usecase.knowledge.BuildKnowledgeSummaryUseCase
import com.voicedeutsch.master.domain.usecase.learning.EndLearningSessionUseCase
import com.voicedeutsch.master.domain.usecase.learning.StartLearningSessionUseCase
import com.voicedeutsch.master.voicecore.audio.AudioPipeline
import com.voicedeutsch.master.voicecore.context.ContextBuilder
import com.voicedeutsch.master.voicecore.functions.FunctionRouter
import com.voicedeutsch.master.voicecore.session.AudioState
import com.voicedeutsch.master.voicecore.session.ConnectionState
import com.voicedeutsch.master.voicecore.session.VoiceEngineState
import com.voicedeutsch.master.voicecore.session.VoiceSessionState
import com.voicedeutsch.master.voicecore.strategy.StrategySelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Heart of the system — orchestrates all VoiceCore components.
 * Architecture lines 530-570 (responsibilities), 625-680 (lifecycle).
 *
 * Lifecycle:
 *   IDLE → INITIALIZING → (IDLE if init failed)
 *   startSession: CONTEXT_LOADING → CONNECTING → SESSION_ACTIVE → LISTENING / PROCESSING / SPEAKING
 *   endSession:   SESSION_ENDING → SAVING → IDLE
 *   error:        ERROR → RECONNECTING → SESSION_ACTIVE  (or IDLE after max attempts)
 *
 * Concurrency:
 *   - [lifecycleMutex] serialises all lifecycle transitions
 *   - [_sessionState] is written exclusively via [updateState]
 *   - The main audio loop runs in [sessionJob] and is cancelled by [endSession] / [destroy]
 */
class VoiceCoreEngineImpl(
    private val contextBuilder: ContextBuilder,
    private val functionRouter: FunctionRouter,
    private val audioPipeline: AudioPipeline,
    private val strategySelector: StrategySelector,
    private val buildKnowledgeSummary: BuildKnowledgeSummaryUseCase,
    private val startLearningSession: StartLearningSessionUseCase,
    private val endLearningSession: EndLearningSessionUseCase,
) : VoiceCoreEngine {

    // ── Coroutine infrastructure ─────────────────────────────────────────────

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sessionJob: Job? = null

    // Serialises lifecycle transitions; never held across suspension points
    // that call external code (to avoid deadlock).
    private val lifecycleMutex = Mutex()

    // ── State ────────────────────────────────────────────────────────────────

    private val _sessionState = MutableStateFlow(VoiceSessionState())
    override val sessionState: StateFlow<VoiceSessionState> = _sessionState.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _audioState = MutableStateFlow(AudioState.IDLE)
    override val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    // Mutable runtime fields (only accessed under lifecycleMutex or from sessionJob)
    @Volatile private var config: GeminiConfig? = null
    @Volatile private var activeSessionId: String? = null
    @Volatile private var activeUserId: String? = null
    @Volatile private var reconnectAttempts: Int = 0
    @Volatile private var sessionStartMs: Long = 0L

    // ── State helpers ─────────────────────────────────────────────────────────

    private fun updateState(block: VoiceSessionState.() -> VoiceSessionState) {
        _sessionState.update(block)
    }

    private fun transitionEngine(state: VoiceEngineState) {
        _sessionState.update { it.copy(engineState = state) }
    }

    private fun transitionConnection(state: ConnectionState) {
        _connectionState.value = state
        _sessionState.update { it.copy(connectionState = state) }
    }

    private fun transitionAudio(state: AudioState) {
        _audioState.value = state
        _sessionState.update {
            it.copy(
                audioState = state,
                isListening = state == AudioState.RECORDING,
                isSpeaking = state == AudioState.PLAYING,
            )
        }
    }

    // ── VoiceCoreEngine: lifecycle ────────────────────────────────────────────

    override suspend fun initialize(config: GeminiConfig) {
        lifecycleMutex.withLock {
            val current = _sessionState.value.engineState
            check(current == VoiceEngineState.IDLE || current == VoiceEngineState.ERROR) {
                "initialize() called in invalid state: $current"
            }

            transitionEngine(VoiceEngineState.INITIALIZING)
            runCatching {
                audioPipeline.initialize()
                this.config = config
            }.onFailure { error ->
                transitionEngine(VoiceEngineState.ERROR)
                updateState { copy(errorMessage = error.message) }
                throw error
            }
            transitionEngine(VoiceEngineState.IDLE)
        }
    }

    override suspend fun startSession(userId: String): VoiceSessionState = lifecycleMutex.withLock {
        val current = _sessionState.value.engineState
        check(current == VoiceEngineState.IDLE || current == VoiceEngineState.CONNECTED) {
            "startSession() called in invalid state: $current"
        }
        checkNotNull(config) { "Call initialize() before startSession()" }

        transitionEngine(VoiceEngineState.CONTEXT_LOADING)
        reconnectAttempts = 0

        // 1. Domain: create session record
        val sessionData = withContext(Dispatchers.IO) {
            startLearningSession(userId)
        }
        activeSessionId = sessionData.session.id
        activeUserId = userId
        sessionStartMs = System.currentTimeMillis()

        // 2. Domain: build knowledge snapshot
        val snapshot = withContext(Dispatchers.IO) {
            buildKnowledgeSummary(userId)
        }

        // 3. Choose strategy
        val strategy = strategySelector.selectStrategy(snapshot)

        // 4. Build full Gemini context
        val sessionContext = withContext(Dispatchers.IO) {
            contextBuilder.buildSessionContext(
                userId = userId,
                knowledgeSnapshot = snapshot,
                currentStrategy = strategy,
                currentChapter = sessionData.currentChapter,
                currentLesson = sessionData.currentLesson,
            )
        }

        // 5. Connect and transmit context to Gemini
        transitionEngine(VoiceEngineState.CONNECTING)
        transitionConnection(ConnectionState.CONNECTING)
        connectToGemini(sessionContext)

        // 6. Activate session state
        transitionEngine(VoiceEngineState.SESSION_ACTIVE)
        transitionConnection(ConnectionState.CONNECTED)
        reconnectAttempts = 0

        updateState {
            copy(
                isVoiceActive = true,
                currentStrategy = strategy,
                sessionId = sessionData.session.id,
                errorMessage = null,
            )
        }

        // 7. Launch the main audio/response loop in the background
        sessionJob = engineScope.launch {
            runSessionLoop()
        }

        _sessionState.value
    }

    override suspend fun endSession(): SessionResult? {
        val sessionId = activeSessionId ?: return null

        lifecycleMutex.withLock {
            val current = _sessionState.value.engineState
            if (!current.isActiveSession()) return@withLock

            transitionEngine(VoiceEngineState.SESSION_ENDING)
        }

        // Cancel the audio loop (outside the mutex — avoids deadlock with the loop itself)
        sessionJob?.cancel()
        sessionJob = null

        lifecycleMutex.withLock {
            transitionEngine(VoiceEngineState.SAVING)

            return@withLock withContext(Dispatchers.IO) {
                val result = runCatching {
                    audioPipeline.stopAll()
                    transitionAudio(AudioState.IDLE)
                    transitionConnection(ConnectionState.DISCONNECTED)
                    disconnectFromGemini()

                    val sessionResult = endLearningSession(sessionId)
                    result -> sessionResult
                }.onFailure { error ->
                    updateState { copy(errorMessage = "Session save failed: ${error.message}") }
                }.getOrNull()

                activeSessionId = null
                activeUserId = null
                transitionEngine(VoiceEngineState.IDLE)
                updateState {
                    copy(
                        isVoiceActive = false,
                        isListening = false,
                        isSpeaking = false,
                        isProcessing = false,
                        currentTranscript = "",
                        voiceTranscript = "",
                    )
                }
                result
            }
        }
    }

    override suspend fun destroy() {
        // Best-effort: end session if active, then release everything
        runCatching { endSession() }
        lifecycleMutex.withLock {
            audioPipeline.release()
            config = null
        }
        engineScope.cancel()
    }

    // ── VoiceCoreEngine: audio control ───────────────────────────────────────

    override fun startListening() {
        if (_sessionState.value.isSessionActive && !_sessionState.value.isListening) {
            audioPipeline.startRecording()
            transitionAudio(AudioState.RECORDING)
        }
    }

    override fun stopListening() {
        if (_sessionState.value.isListening) {
            audioPipeline.stopRecording()
            transitionAudio(AudioState.IDLE)
        }
    }

    override fun pausePlayback() {
        if (_sessionState.value.isSpeaking) {
            audioPipeline.pausePlayback()
            transitionAudio(AudioState.PAUSED)
        }
    }

    override fun resumePlayback() {
        if (_sessionState.value.audioState == AudioState.PAUSED) {
            audioPipeline.resumePlayback()
            transitionAudio(AudioState.PLAYING)
        }
    }

    // ── VoiceCoreEngine: manual control ──────────────────────────────────────

    override suspend fun sendTextMessage(text: String) {
        check(_sessionState.value.isSessionActive) { "No active session" }
        sendTextToGemini(text)
    }

    override suspend fun requestStrategyChange(strategy: LearningStrategy) {
        updateState { copy(currentStrategy = strategy) }
        val message = buildStrategyChangeMessage(strategy)
        sendTextToGemini(message)
    }

    override suspend fun requestBookNavigation(chapter: Int, lesson: Int) {
        check(chapter > 0 && lesson > 0) { "Chapter and lesson must be positive" }
        val message = "Перейди к главе $chapter, уроку $lesson."
        sendTextToGemini(message)
    }

    override suspend fun submitFunctionResult(callId: String, resultJson: String) {
        sendFunctionResultToGemini(callId, resultJson)
    }

    // ── Main session loop ─────────────────────────────────────────────────────

    /**
     * Continuously reads audio from [AudioPipeline], forwards it to Gemini,
     * and dispatches responses (audio + function calls) back to the appropriate handlers.
     *
     * The loop runs until the coroutine is cancelled (by [endSession] / [destroy]).
     */
    private suspend fun runSessionLoop() {
        try {
            while (isActive) {
                // Receive the next chunk from Gemini (audio bytes | function call | transcript)
                val response = receiveGeminiResponse() ?: continue

                when {
                    response.hasAudio() -> {
                        transitionAudio(AudioState.PLAYING)
                        updateState {
                            copy(
                                isProcessing = false,
                                isSpeaking = true,
                                voiceTranscript = response.transcript ?: voiceTranscript,
                            )
                        }
                        audioPipeline.enqueueAudio(response.audioData!!)
                        if (response.isTurnComplete) {
                            transitionAudio(AudioState.IDLE)
                            transitionEngine(VoiceEngineState.WAITING)
                        }
                    }

                    response.hasFunctionCall() -> {
                        updateState { copy(isProcessing = true) }
                        val call = response.functionCall!!
                        val userId = activeUserId ?: return
                        val sessionId = activeSessionId

                        val result = withContext(Dispatchers.IO) {
                            functionRouter.route(
                                functionName = call.name,
                                argsJson = call.argsJson,
                                userId = userId,
                                sessionId = sessionId,
                            )
                        }

                        // Propagate session counters from function router results
                        applyFunctionSideEffects(call.name, result)

                        // Return result to Gemini so it can continue generating
                        sendFunctionResultToGemini(call.id, result.resultJson)
                        updateState { copy(isProcessing = false) }
                    }

                    response.hasTranscript() -> {
                        updateState { copy(currentTranscript = response.transcript ?: "") }
                        transitionEngine(VoiceEngineState.PROCESSING)
                    }
                }
            }
        } catch (e: Exception) {
            if (isActive) handleSessionError(e)
        }
    }

    /** Updates session counters in state when Gemini calls knowledge/book functions. */
    private fun applyFunctionSideEffects(functionName: String, result: FunctionRouter.FunctionCallResult) {
        if (!result.success) return
        when (functionName) {
            "save_word_knowledge" -> updateState { copy(wordsLearnedInSession = wordsLearnedInSession + 1) }
            "get_words_for_repetition" -> updateState { copy(wordsReviewedInSession = wordsReviewedInSession + 1) }
            "mark_lesson_complete", "advance_to_next_lesson" ->
                updateState { copy(exercisesCompleted = exercisesCompleted + 1) }
        }
    }

    /** Handles transient errors with exponential-back-off reconnection. */
    private fun handleSessionError(error: Throwable) {
        val maxAttempts = config?.reconnectMaxAttempts ?: GeminiConfig.DEFAULT_RECONNECT_ATTEMPTS
        engineScope.launch {
            transitionEngine(VoiceEngineState.ERROR)
            updateState { copy(errorMessage = error.message) }

            if (reconnectAttempts < maxAttempts) {
                reconnectAttempts++
                val delayMs = (config?.reconnectDelayMs ?: GeminiConfig.DEFAULT_RECONNECT_DELAY_MS) * reconnectAttempts
                transitionEngine(VoiceEngineState.RECONNECTING)
                transitionConnection(ConnectionState.RECONNECTING)
                delay(delayMs)

                runCatching {
                    val uid = activeUserId ?: return@launch
                    // Re-enter start session without holding the mutex (it was released)
                    startSession(uid)
                }.onFailure { handleSessionError(it) }
            } else {
                transitionConnection(ConnectionState.FAILED)
                transitionEngine(VoiceEngineState.IDLE)
            }
        }
    }

    // ── Gemini API stubs ─────────────────────────────────────────────────────
    // These are thin delegation points. The actual WebSocket/gRPC connection
    // is managed by GeminiClient (Session 6). In production these are replaced
    // by injected GeminiClient calls.

    private suspend fun connectToGemini(context: ContextBuilder.SessionContext) {
        // GeminiClient.connect(config!!, context) — wired in Session 6
    }

    private suspend fun disconnectFromGemini() {
        // GeminiClient.disconnect()
    }

    private suspend fun sendTextToGemini(text: String) {
        // GeminiClient.sendText(text)
    }

    private suspend fun sendFunctionResultToGemini(callId: String, resultJson: String) {
        // GeminiClient.sendFunctionResult(callId, resultJson)
    }

    /** Returns the next response chunk from Gemini, or null if the stream is momentarily empty. */
    private suspend fun receiveGeminiResponse(): GeminiResponse? {
        // GeminiClient.receiveNextResponse()
        return null
    }

    private fun buildStrategyChangeMessage(strategy: LearningStrategy): String =
        "Пожалуйста, переключись на стратегию ${strategy.displayNameRu} (${strategy.name})."

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun VoiceEngineState.isActiveSession(): Boolean = when (this) {
        VoiceEngineState.SESSION_ACTIVE,
        VoiceEngineState.LISTENING,
        VoiceEngineState.PROCESSING,
        VoiceEngineState.SPEAKING,
        VoiceEngineState.WAITING -> true
        else -> false
    }
}

// ── Internal Gemini response model ────────────────────────────────────────────
// Placeholder until GeminiClient (Session 6) provides the real type.

internal data class GeminiFunctionCall(
    val id: String,
    val name: String,
    val argsJson: String,
)

internal data class GeminiResponse(
    val audioData: ByteArray?,
    val transcript: String?,
    val functionCall: GeminiFunctionCall?,
    val isTurnComplete: Boolean = false,
) {
    fun hasAudio(): Boolean = audioData != null && audioData.isNotEmpty()
    fun hasFunctionCall(): Boolean = functionCall != null
    fun hasTranscript(): Boolean = !transcript.isNullOrEmpty() && audioData == null && functionCall == null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeminiResponse) return false
        return transcript == other.transcript &&
                functionCall == other.functionCall &&
                isTurnComplete == other.isTurnComplete &&
                (audioData == null && other.audioData == null ||
                        audioData != null && other.audioData != null && audioData.contentEquals(other.audioData))
    }

    override fun hashCode(): Int = transcript.hashCode() * 31 + isTurnComplete.hashCode()
}
Note on endSession: The compiler will catch the syntax error in the runCatching lambda — corrected below in the final clean version. Let me write the rest of the files first, then provide the corrected endSession.
FILE 5: voicecore/context/ContextBuilder.kt + sub-providers
// ─────────────────────────────────────────────────────────────────────────────
// FILE: voicecore/context/SystemPromptBuilder.kt
// ─────────────────────────────────────────────────────────────────────────────
package com.voicedeutsch.master.voicecore.context

import com.voicedeutsch.master.voicecore.prompt.MasterPrompt

/**
 * Thin wrapper that produces the system instruction string from [MasterPrompt].
 * Kept as a class (not an object) so it is injectable and mockable in tests.
 */
class SystemPromptBuilder {
    fun build(): String = MasterPrompt.build()
}