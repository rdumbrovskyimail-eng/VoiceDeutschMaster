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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

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
 *   - [audioForwardJob] forwards AudioPipeline chunks to GeminiClient in parallel
 */
class VoiceCoreEngineImpl(
    private val contextBuilder: ContextBuilder,
    private val functionRouter: FunctionRouter,
    private val audioPipeline: AudioPipeline,
    private val strategySelector: StrategySelector,
    private val buildKnowledgeSummary: BuildKnowledgeSummaryUseCase,
    private val startLearningSession: StartLearningSessionUseCase,
    private val endLearningSession: EndLearningSessionUseCase,
    private val httpClient: io.ktor.client.HttpClient,
    private val json: kotlinx.serialization.json.Json,
    private val networkMonitor: com.voicedeutsch.master.util.NetworkMonitor,
    private val ephemeralTokenService: com.voicedeutsch.master.data.remote.gemini.EphemeralTokenService,
) : VoiceCoreEngine {

    companion object {
        /**
         * Максимальное время выполнения функции (мс).
         * Gemini разрывает сессию если toolResponse не приходит ~15 секунд.
         * 12 секунд — безопасный буфер: успеваем ответить даже при зависании Room.
         */
        private const val FUNCTION_CALL_TIMEOUT_MS = 12_000L
    }

    // ── Coroutine infrastructure ──────────────────────────────────────────────

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sessionJob: Job? = null
    private var audioForwardJob: Job? = null
    private val lifecycleMutex = Mutex()

    // ── State ─────────────────────────────────────────────────────────────────

    private val _sessionState = MutableStateFlow(VoiceSessionState())
    override val sessionState: StateFlow<VoiceSessionState> = _sessionState.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _audioState = MutableStateFlow(AudioState.IDLE)
    override val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    @Volatile private var config: GeminiConfig? = null
    @Volatile private var geminiClient: GeminiClient? = null
    @Volatile private var activeSessionId: String? = null
    @Volatile private var activeUserId: String? = null
    private val reconnectAttempts = java.util.concurrent.atomic.AtomicInteger(0)
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
                this.geminiClient?.release()
                this.geminiClient = GeminiClient(config, httpClient, json)
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
        val cfg = checkNotNull(config) { "Call initialize() before startSession()" }

        if (!networkMonitor.isOnline()) {
            transitionEngine(VoiceEngineState.ERROR)
            updateState { copy(errorMessage = "Нет подключения к интернету. Проверьте сеть.") }
            return@withLock _sessionState.value
        }

        transitionEngine(VoiceEngineState.CONTEXT_LOADING)
        reconnectAttempts.set(0)

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

        // 5. Получить эфемерный токен и открыть WebSocket
        transitionEngine(VoiceEngineState.CONNECTING)
        transitionConnection(ConnectionState.CONNECTING)
        val token = withContext(Dispatchers.IO) {
            ephemeralTokenService.fetchToken(userId)
        }
        withContext(Dispatchers.IO) {
            requireNotNull(geminiClient).connect(cfg, sessionContext, token)
        }

        // 6. Activate session state
        transitionEngine(VoiceEngineState.SESSION_ACTIVE)
        transitionConnection(ConnectionState.CONNECTED)
        reconnectAttempts.set(0)

        updateState {
            copy(
                isVoiceActive = true,
                currentStrategy = strategy,
                sessionId = sessionData.session.id,
                errorMessage = null,
            )
        }

        // 7. Запустить основной цикл обработки ответов Gemini
        sessionJob = engineScope.launch {
            runSessionLoop()
        }
        startListening()

        _sessionState.value
    }

    override suspend fun endSession(): SessionResult? {
        val sessionId = activeSessionId ?: return null

        lifecycleMutex.withLock {
            val current = _sessionState.value.engineState
            if (!current.isActiveSession()) return@withLock
            transitionEngine(VoiceEngineState.SESSION_ENDING)
        }

        audioForwardJob?.cancel()
        audioForwardJob = null
        sessionJob?.cancel()
        sessionJob = null

        return lifecycleMutex.withLock {
            transitionEngine(VoiceEngineState.SAVING)

            val sessionResult: SessionResult? = withContext(Dispatchers.IO) {
                runCatching {
                    audioPipeline.stopAll()
                    transitionAudio(AudioState.IDLE)
                    transitionConnection(ConnectionState.DISCONNECTED)
                    geminiClient?.disconnect()
                    endLearningSession(sessionId)
                }.onFailure { error ->
                    updateState { copy(errorMessage = "Session save failed: ${error.message}") }
                }.getOrNull()
            }

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

            sessionResult
        }
    }

    override suspend fun destroy() {
        runCatching { endSession() }
        lifecycleMutex.withLock {
            geminiClient?.release()
            geminiClient = null
            audioPipeline.release()
            config = null
        }
        engineScope.cancel()
    }

    // ── VoiceCoreEngine: audio control ────────────────────────────────────────

    override fun startListening() {
        if (_sessionState.value.isSessionActive && !_sessionState.value.isListening) {
            runCatching {
                audioPipeline.startRecording()
            }.onFailure { e ->
                android.util.Log.e("VoiceCoreEngine", "startRecording failed", e)
                updateState { copy(errorMessage = "Не удалось запустить микрофон: ${e.message}") }
                return
            }
            transitionAudio(AudioState.RECORDING)
            audioForwardJob = engineScope.launch(Dispatchers.IO) {
                audioPipeline.audioChunks()
                    .catch { e ->
                        android.util.Log.e("VoiceCoreEngine", "Audio stream error", e)
                        transitionAudio(AudioState.IDLE)
                        handleSessionError(e)
                    }
                    .collect { pcmChunk ->
                        runCatching {
                            geminiClient?.sendAudioChunk(pcmChunk)
                        }.onFailure { e ->
                            android.util.Log.w("VoiceCoreEngine", "sendAudioChunk failed: ${e.message}")
                        }
                    }
            }
        }
    }

    override fun stopListening() {
        if (_sessionState.value.isListening) {
            audioForwardJob?.cancel()
            audioForwardJob = null
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
        requireNotNull(geminiClient).sendText(text)
    }

    override suspend fun requestStrategyChange(strategy: LearningStrategy) {
        updateState { copy(currentStrategy = strategy) }
        requireNotNull(geminiClient).sendText(buildStrategyChangeMessage(strategy))
    }

    override suspend fun requestBookNavigation(chapter: Int, lesson: Int) {
        check(chapter > 0 && lesson > 0) { "Chapter and lesson must be positive" }
        requireNotNull(geminiClient).sendText("Перейди к главе $chapter, уроку $lesson.")
    }

    override suspend fun submitFunctionResult(callId: String, name: String, resultJson: String) {
        requireNotNull(geminiClient).sendFunctionResult(callId, name, resultJson)
    }

    // ── Main session loop ─────────────────────────────────────────────────────

    private suspend fun runSessionLoop() {
        try {
            while (currentCoroutineContext().isActive) {
                val response = requireNotNull(geminiClient).receiveNextResponse()

                if (response == null) {
                    if (currentCoroutineContext().isActive) {
                        handleSessionError(IllegalStateException("Gemini connection closed unexpectedly"))
                    }
                    return
                }

                when {
                    response.isInterrupted -> {
                        android.util.Log.d("VoiceCoreEngine", "ИИ перебит пользователем! Очистка аудио-очереди.")
                        audioPipeline.flushPlayback()
                        transitionAudio(AudioState.IDLE)
                        transitionEngine(VoiceEngineState.LISTENING)
                        updateState { copy(isSpeaking = false, isProcessing = false) }
                    }

                    response.hasAudio() -> {
                        transitionAudio(AudioState.PLAYING)
                        updateState {
                            copy(
                                isProcessing = false,
                                isSpeaking = true,
                                voiceTranscript = response.transcript ?: voiceTranscript,
                            )
                        }
                        val audioData = response.audioData
                        if (audioData == null) {
                            android.util.Log.e("VoiceCoreEngine", "hasAudio() true but audioData is null")
                        } else {
                            audioPipeline.enqueueAudio(audioData)
                            if (response.isTurnComplete) {
                                transitionAudio(AudioState.IDLE)
                                transitionEngine(VoiceEngineState.WAITING)
                            }
                        }
                    }

                    response.hasFunctionCall() -> {
                        updateState { copy(isProcessing = true) }
                        val call = response.functionCall!!

                        // ✅ FIX: Защита от null userId — отвечаем Gemini даже в аномальном случае,
                        // чтобы не получить таймаут сессии (~15 сек без toolResponse).
                        val userId = activeUserId
                        val sessionId = activeSessionId

                        val result = if (userId == null) {
                            android.util.Log.e("VoiceCoreEngine", "hasFunctionCall: activeUserId is null, returning error to Gemini")
                            FunctionRouter.FunctionCallResult(
                                functionName = call.name,
                                success = false,
                                resultJson = """{"error":"no active user session"}""",
                            )
                        } else {
                            // ✅ FIX: Таймаут 12 секунд — буфер до ~15-секундного таймаута Gemini.
                            // Если Room завис или UseCase завис — Gemini всё равно получит ответ.
                            try {
                                withTimeout(FUNCTION_CALL_TIMEOUT_MS) {
                                    withContext(Dispatchers.IO) {
                                        functionRouter.route(
                                            functionName = call.name,
                                            argsJson = call.argsJson,
                                            userId = userId,
                                            sessionId = sessionId,
                                        )
                                    }
                                }
                            } catch (e: TimeoutCancellationException) {
                                android.util.Log.e(
                                    "VoiceCoreEngine",
                                    "Function '${call.name}' timed out after ${FUNCTION_CALL_TIMEOUT_MS}ms",
                                )
                                FunctionRouter.FunctionCallResult(
                                    functionName = call.name,
                                    success = false,
                                    resultJson = """{"error":"function execution timed out"}""",
                                )
                            }
                        }

                        // ✅ toolResponse уходит в WebSocket в любом случае (success или error)
                        applyFunctionSideEffects(call.name, result)
                        requireNotNull(geminiClient).sendFunctionResult(
                            callId = call.id,
                            name = result.functionName,
                            resultJson = result.resultJson,
                        )
                        updateState { copy(isProcessing = false) }
                    }

                    response.hasTranscript() -> {
                        updateState { copy(currentTranscript = response.transcript ?: "") }
                        transitionEngine(VoiceEngineState.PROCESSING)
                    }
                }
            }
        } catch (e: Exception) {
            if (currentCoroutineContext().isActive) handleSessionError(e)
        }
    }

    private fun applyFunctionSideEffects(
        functionName: String,
        result: FunctionRouter.FunctionCallResult,
    ) {
        if (!result.success) return
        when (functionName) {
            "save_word_knowledge" ->
                updateState { copy(wordsLearnedInSession = wordsLearnedInSession + 1) }
            "get_words_for_repetition" ->
                updateState { copy(wordsReviewedInSession = wordsReviewedInSession + 1) }
            "mark_lesson_complete", "advance_to_next_lesson" ->
                updateState { copy(exercisesCompleted = exercisesCompleted + 1) }
        }
    }

    private fun handleSessionError(error: Throwable) {
        val maxAttempts = config?.reconnectMaxAttempts ?: GeminiConfig.DEFAULT_RECONNECT_ATTEMPTS
        engineScope.launch {
            transitionEngine(VoiceEngineState.ERROR)
            updateState { copy(errorMessage = error.message) }

            if (reconnectAttempts.get() < maxAttempts) {
                val attempts = reconnectAttempts.incrementAndGet()
                val delayMs =
                    (config?.reconnectDelayMs ?: GeminiConfig.DEFAULT_RECONNECT_DELAY_MS) *
                            attempts
                transitionEngine(VoiceEngineState.RECONNECTING)
                transitionConnection(ConnectionState.RECONNECTING)
                delay(delayMs)

                runCatching {
                    val uid = activeUserId ?: return@launch
                    startSession(uid)
                }.onFailure { handleSessionError(it) }
            } else {
                transitionConnection(ConnectionState.FAILED)
                transitionEngine(VoiceEngineState.IDLE)
            }
        }
    }

    private fun buildStrategyChangeMessage(strategy: LearningStrategy): String =
        "Пожалуйста, переключись на стратегию ${strategy.displayNameRu} (${strategy.name})."

    private fun VoiceEngineState.isActiveSession(): Boolean = when (this) {
        VoiceEngineState.SESSION_ACTIVE,
        VoiceEngineState.LISTENING,
        VoiceEngineState.PROCESSING,
        VoiceEngineState.SPEAKING,
        VoiceEngineState.WAITING -> true
        else -> false
    }
}
