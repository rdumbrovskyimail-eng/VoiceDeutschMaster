package com.voicedeutsch.master.voicecore.engine

import android.util.Log
import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.session.SessionResult
import com.voicedeutsch.master.domain.usecase.knowledge.BuildKnowledgeSummaryUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.FlushKnowledgeSyncUseCase
import com.voicedeutsch.master.domain.usecase.learning.EndLearningSessionUseCase
import com.voicedeutsch.master.domain.usecase.learning.StartLearningSessionUseCase
import com.voicedeutsch.master.util.NetworkMonitor
import com.voicedeutsch.master.voicecore.audio.AudioPipeline
import com.voicedeutsch.master.voicecore.audio.RmsCalculator
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicInteger

/**
 * Heart of the system — оркестрирует все VoiceCore-компоненты.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * ИЗМЕНЕНИЯ (VirtualAvatar — устранение рекомпозиций):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   ДОБАВЛЕНО: amplitudeFlow: Flow<Float>
 *
 *   Реализация через audioPipeline.audioChunks().map { RmsCalculator.calculate(it) }.
 *   SessionViewModel подписывается на этот Flow и пишет значение в
 *   mutableFloatStateOf — Compose читает его только в фазе draw Canvas,
 *   рекомпозиция VirtualAvatar не происходит.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * ИЗМЕНЕНИЯ (Parallel Function Calling fix):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   hasFunctionCalls() → async/awaitAll → sendFunctionResults() (батч).
 *
 * ════════════════════════════════════════════════════════════════════════════
 * ИЗМЕНЕНИЯ (Синхронизация и Firebase — Батчинг):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   flushKnowledgeSync() вызывается в endSession() одним batch-коммитом.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * FIX (Race condition — receiveFlow vs startListening):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   Баг: launchIn() шедулит корутину асинхронно, но startListening()
 *        вызывается мгновенно после. Первые аудиочанки могли уйти в
 *        Gemini ДО того, как receiveFlow() начал коллекцию session.receive().
 *        Firebase SDK мог отклонить данные → "Something unexpected happened".
 *
 *   Fix: startListening() перенесён в onStart {} оператор Flow.
 *        onStart выполняется ПОСЛЕ подписки на upstream (session.receive()),
 *        но ДО первого элемента — гарантирует правильный порядок.
 *
 *   Применено и в startSession(), и в reconnectInternal().
 * ════════════════════════════════════════════════════════════════════════════
 */
class VoiceCoreEngineImpl(
    private val contextBuilder:        ContextBuilder,
    private val functionRouter:        FunctionRouter,
    private val audioPipeline:         AudioPipeline,
    private val strategySelector:      StrategySelector,
    private val geminiClient:          GeminiClient,
    private val buildKnowledgeSummary: BuildKnowledgeSummaryUseCase,
    private val startLearningSession:  StartLearningSessionUseCase,
    private val endLearningSession:    EndLearningSessionUseCase,
    private val networkMonitor:        NetworkMonitor,
    private val flushKnowledgeSync:    FlushKnowledgeSyncUseCase,
) : VoiceCoreEngine {

    companion object {
        private const val TAG = "VoiceCoreEngine"
        private const val FUNCTION_CALL_TIMEOUT_MS = 12_000L
    }

    // ── Coroutine infrastructure ──────────────────────────────────────────────

    private val engineScope      = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sessionJob:      Job? = null
    private var audioForwardJob: Job? = null
    private val lifecycleMutex   = Mutex()

    // ── State ─────────────────────────────────────────────────────────────────

    private val _sessionState    = MutableStateFlow(VoiceSessionState())
    override val sessionState:   StateFlow<VoiceSessionState> = _sessionState.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _audioState      = MutableStateFlow(AudioState.IDLE)
    override val audioState:     StateFlow<AudioState> = _audioState.asStateFlow()

    // amplitudeFlow для VirtualAvatar.
    // audioPipeline.audioChunks() используется в startListening() для отправки
    // чанков в Gemini. Здесь создаём отдельный холодный Flow от того же источника.
    // RmsCalculator вычисляет среднеквадратичное значение PCM → нормализует в 0f..1f.
    override val amplitudeFlow: Flow<Float> = audioPipeline.audioChunks()
        .map { pcm -> RmsCalculator.calculate(pcm).coerceIn(0f, 1f) }

    @Volatile private var config: GeminiConfig? = null
    @Volatile private var activeSessionId: String? = null
    @Volatile private var activeUserId:    String? = null
    private val reconnectAttempts = AtomicInteger(0)
    @Volatile private var sessionStartMs: Long = 0L

    // ── State helpers ─────────────────────────────────────────────────────────

    private fun updateState(block: VoiceSessionState.() -> VoiceSessionState) =
        _sessionState.update(block)

    private fun transitionEngine(state: VoiceEngineState) =
        _sessionState.update { it.copy(engineState = state) }

    private fun transitionConnection(state: ConnectionState) {
        _connectionState.value = state
        _sessionState.update { it.copy(connectionState = state) }
    }

    private fun transitionAudio(state: AudioState) {
        _audioState.value = state
        _sessionState.update {
            it.copy(
                audioState  = state,
                isListening = state == AudioState.RECORDING,
                isSpeaking  = state == AudioState.PLAYING,
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
            Log.d(TAG, "✅ Engine initialized [model=${config.modelName}]")
        }
    }

    override suspend fun startSession(userId: String): VoiceSessionState =
        lifecycleMutex.withLock {
            val current = _sessionState.value.engineState
            check(current == VoiceEngineState.IDLE || current == VoiceEngineState.CONNECTED) {
                "startSession() called in invalid state: $current"
            }
            checkNotNull(config) { "Call initialize() before startSession()" }

            if (!networkMonitor.isOnline()) {
                transitionEngine(VoiceEngineState.ERROR)
                updateState { copy(errorMessage = "Нет подключения к интернету. Проверьте сеть.") }
                return@withLock _sessionState.value
            }

            cancelActiveJobs()

            transitionEngine(VoiceEngineState.CONTEXT_LOADING)
            reconnectAttempts.set(0)

            val sessionData = withContext(Dispatchers.IO) { startLearningSession(userId) }
            activeSessionId = sessionData.session.id
            activeUserId    = userId
            sessionStartMs  = System.currentTimeMillis()

            val snapshot = withContext(Dispatchers.IO) { buildKnowledgeSummary(userId) }
            val strategy = strategySelector.selectStrategy(snapshot)

            val sessionContext = withContext(Dispatchers.IO) {
                contextBuilder.buildSessionContext(
                    userId            = userId,
                    knowledgeSnapshot = snapshot,
                    currentStrategy   = strategy,
                    currentChapter    = sessionData.currentChapter,
                    currentLesson     = sessionData.currentLesson,
                )
            }

            transitionEngine(VoiceEngineState.CONNECTING)
            transitionConnection(ConnectionState.CONNECTING)

            val connectResult = runCatching {
                withContext(Dispatchers.IO) { geminiClient.connect(sessionContext) }
            }

            if (connectResult.isFailure) {
                val error = connectResult.exceptionOrNull()!!
                Log.e(TAG, "❌ connect() failed: ${error.message}", error)
                transitionEngine(VoiceEngineState.ERROR)
                transitionConnection(ConnectionState.FAILED)
                updateState { copy(errorMessage = "Ошибка подключения: ${error.message}") }
                return@withLock _sessionState.value
            }

            transitionEngine(VoiceEngineState.SESSION_ACTIVE)
            transitionConnection(ConnectionState.CONNECTED)
            reconnectAttempts.set(0)

            updateState {
                copy(
                    isVoiceActive   = true,
                    currentStrategy = strategy,
                    sessionId       = sessionData.session.id,
                    errorMessage    = null,
                )
            }

            // ════════════════════════════════════════════════════════════════
            // FIX: startListening() перенесён в onStart {}.
            //
            // onStart выполняется ПОСЛЕ того, как receiveFlow() подписался
            // на session.receive(), но ДО первого элемента.
            // Это гарантирует, что Gemini SDK уже слушает ответы
            // к моменту отправки первого аудиочанка.
            //
            // Раньше: launchIn (async) → startListening() (сразу) → race.
            // Теперь: launchIn → receive() подписка → onStart → startListening()
            // ════════════════════════════════════════════════════════════════
            sessionJob = geminiClient
                .receiveFlow()
                .onStart {
                    Log.d(TAG, "receiveFlow started collecting — now starting audio")
                    startListening()
                }
                .onEach  { response -> handleGeminiResponse(response) }
                .catch   { error    -> handleSessionError(error) }
                .launchIn(engineScope)

            Log.d(TAG, "✅ Session started [userId=$userId, sessionId=${sessionData.session.id}]")
            _sessionState.value
        }

    override suspend fun endSession(): SessionResult? {
        val sessionId = activeSessionId ?: return null

        lifecycleMutex.withLock {
            val current = _sessionState.value.engineState
            if (!current.isActiveSession()) return@withLock
            transitionEngine(VoiceEngineState.SESSION_ENDING)
        }

        cancelActiveJobs()

        return lifecycleMutex.withLock {
            transitionEngine(VoiceEngineState.SAVING)

            val sessionResult: SessionResult? = withContext(Dispatchers.IO) {
                runCatching {
                    audioPipeline.stopAll()
                    transitionAudio(AudioState.IDLE)
                    transitionConnection(ConnectionState.DISCONNECTED)

                    runCatching { geminiClient.disconnect() }
                        .onFailure { Log.w(TAG, "disconnect() warning: ${it.message}") }

                    geminiClient.clearResumptionHandle()

                    val result = endLearningSession(sessionId)

                    val syncOk = runCatching { flushKnowledgeSync() }.getOrElse { e ->
                        Log.w(TAG, "⚠️ flushKnowledgeSync failed (data safe in Room): ${e.message}")
                        false
                    }
                    if (syncOk) Log.d(TAG, "✅ Knowledge sync flushed")
                    else        Log.w(TAG, "⚠️ Knowledge sync deferred — will retry next session")

                    result
                }.onFailure { error ->
                    Log.e(TAG, "Session save failed: ${error.message}", error)
                    updateState { copy(errorMessage = "Session save failed: ${error.message}") }
                }.getOrNull()
            }

            activeSessionId = null
            activeUserId    = null

            transitionEngine(VoiceEngineState.IDLE)
            updateState {
                copy(
                    isVoiceActive     = false,
                    isListening       = false,
                    isSpeaking        = false,
                    isProcessing      = false,
                    currentTranscript = "",
                    voiceTranscript   = "",
                )
            }

            Log.d(TAG, "Session ended [sessionId=$sessionId]")
            sessionResult
        }
    }

    override suspend fun destroy() {
        runCatching { endSession() }
        lifecycleMutex.withLock {
            geminiClient.release()
            audioPipeline.release()
            config = null
        }
        engineScope.cancel()
        Log.d(TAG, "Engine destroyed")
    }

    // ── VoiceCoreEngine: audio control ────────────────────────────────────────

    override fun startListening() {
        if (!_sessionState.value.isSessionActive || _sessionState.value.isListening) return

        runCatching {
            audioPipeline.startRecording()
        }.onFailure { e ->
            Log.e(TAG, "startRecording failed", e)
            updateState { copy(errorMessage = "Не удалось запустить микрофон: ${e.message}") }
            return
        }

        transitionAudio(AudioState.RECORDING)

        audioForwardJob = audioPipeline.audioChunks()
            .onEach { pcmChunk ->
                runCatching {
                    geminiClient.sendAudioChunk(pcmChunk)
                }.onFailure { e ->
                    Log.w(TAG, "sendAudioChunk failed: ${e.message}")
                }
            }
            .catch { e ->
                Log.e(TAG, "Audio stream error", e)
                transitionAudio(AudioState.IDLE)
                handleSessionError(e)
            }
            .launchIn(engineScope)
    }

    override fun stopListening() {
        if (!_sessionState.value.isListening) return
        audioForwardJob?.cancel()
        audioForwardJob = null
        audioPipeline.stopRecording()
        transitionAudio(AudioState.IDLE)

        // Сигнал паузы аудиопотока для серверного VAD
        // (no-op пока Firebase SDK не поддержит)
        engineScope.launch {
            runCatching { geminiClient.sendAudioStreamEnd() }
                .onFailure { Log.w(TAG, "sendAudioStreamEnd failed: ${it.message}") }
        }
    }

    override fun pausePlayback() {
        if (!_sessionState.value.isSpeaking) return
        audioPipeline.pausePlayback()
        transitionAudio(AudioState.PAUSED)
    }

    override fun resumePlayback() {
        if (_sessionState.value.audioState != AudioState.PAUSED) return
        audioPipeline.resumePlayback()
        transitionAudio(AudioState.PLAYING)
    }

    // ── VoiceCoreEngine: manual control ──────────────────────────────────────

    override suspend fun sendTextMessage(text: String) {
        check(_sessionState.value.isSessionActive) { "No active session" }
        geminiClient.sendText(text)
    }

    override suspend fun requestStrategyChange(strategy: LearningStrategy) {
        updateState { copy(currentStrategy = strategy) }
        geminiClient.sendText(buildStrategyChangeMessage(strategy))
    }

    override suspend fun requestBookNavigation(chapter: Int, lesson: Int) {
        check(chapter > 0 && lesson > 0) { "Chapter and lesson must be positive" }
        geminiClient.sendText("Перейди к главе $chapter, уроку $lesson.")
    }

    override suspend fun submitFunctionResult(callId: String, name: String, resultJson: String) {
        geminiClient.sendFunctionResult(callId, name, resultJson)
    }

    // ── Обработка ответов Gemini ──────────────────────────────────────────────

    private suspend fun handleGeminiResponse(response: GeminiResponse) {
        when {
            // GoAway — предупреждение о скором разрыве
            // (не поддерживается Firebase SDK, но обрабатываем на будущее)
            response.hasGoAway() -> {
                val timeLeft = response.goAway!!.timeLeftMs
                Log.w(TAG, "⚠️ GoAway received! Connection closing in ${timeLeft}ms")
                updateState { copy(errorMessage = null) }
            }

            response.isInterrupted -> {
                Log.d(TAG, "User interrupted AI — flushing audio queue")
                audioPipeline.flushPlayback()
                transitionAudio(AudioState.IDLE)
                transitionEngine(VoiceEngineState.LISTENING)
                updateState { copy(isSpeaking = false, isProcessing = false) }
            }

            response.hasAudio() -> {
                transitionAudio(AudioState.PLAYING)
                updateState {
                    copy(
                        isProcessing    = false,
                        isSpeaking      = true,
                        voiceTranscript = response.outputTranscript
                            ?: response.transcript
                            ?: voiceTranscript,
                    )
                }
                val audioData = response.audioData
                if (audioData == null) {
                    Log.e(TAG, "hasAudio() = true но audioData == null — пропускаем")
                } else {
                    audioPipeline.enqueueAudio(audioData)
                }
                if (response.isTurnComplete) {
                    transitionAudio(AudioState.IDLE)
                    transitionEngine(VoiceEngineState.WAITING)
                    updateState { copy(isSpeaking = false) }
                }
            }

            response.hasFunctionCalls() -> {
                val calls     = response.functionCalls
                val userId    = activeUserId
                val sessionId = activeSessionId

                Log.d(TAG, "Function calls received (${calls.size}): ${calls.map { it.name }}")
                updateState { copy(isProcessing = true) }

                engineScope.launch {
                    val results = calls.map { call ->
                        async(Dispatchers.IO) {
                            val result = if (userId == null) {
                                Log.e(TAG, "hasFunctionCalls: activeUserId == null")
                                FunctionRouter.FunctionCallResult(
                                    call.name, false,
                                    """{"error":"no active user session"}"""
                                )
                            } else {
                                try {
                                    withTimeout(FUNCTION_CALL_TIMEOUT_MS) {
                                        functionRouter.route(call.name, call.argsJson, userId, sessionId)
                                    }
                                } catch (e: TimeoutCancellationException) {
                                    Log.w(TAG, "Function ${call.name} timed out")
                                    FunctionRouter.FunctionCallResult(
                                        call.name, false,
                                        """{"error":"function execution timed out"}"""
                                    )
                                }
                            }
                            applyFunctionSideEffects(call.name, result)
                            Triple(call.id, result.functionName, result.resultJson)
                        }
                    }.awaitAll()

                    geminiClient.sendFunctionResults(results)
                    updateState { copy(isProcessing = false) }
                }
            }

            response.inputTranscript != null -> {
                updateState { copy(currentTranscript = response.inputTranscript) }
                transitionEngine(VoiceEngineState.PROCESSING)
            }

            response.hasTranscript() -> {
                updateState { copy(currentTranscript = response.transcript ?: "") }
                transitionEngine(VoiceEngineState.PROCESSING)
            }

            // generationComplete без другого контента
            response.isGenerationComplete && !response.isTurnComplete -> {
                Log.d(TAG, "Generation complete (waiting for turnComplete)")
            }
        }
    }

    // ── VoiceCoreEngine: new methods ──────────────────────────────────────────

    override suspend fun sendAudioStreamEnd() {
        geminiClient.sendAudioStreamEnd()
    }

    override fun getTokenUsage(): GeminiClient.TokenUsage? = geminiClient.lastTokenUsage

    // ── Вспомогательные методы ────────────────────────────────────────────────

    private fun cancelActiveJobs() {
        audioForwardJob?.cancel()
        audioForwardJob = null
        sessionJob?.cancel()
        sessionJob = null
    }

    private fun applyFunctionSideEffects(
        functionName: String,
        result: FunctionRouter.FunctionCallResult,
    ) {
        if (!result.success) return
        when (functionName) {
            "save_word_knowledge"      -> updateState { copy(wordsLearnedInSession  = wordsLearnedInSession + 1) }
            "get_words_for_repetition" -> updateState { copy(wordsReviewedInSession = wordsReviewedInSession + 1) }
            "mark_lesson_complete",
            "advance_to_next_lesson"   -> updateState { copy(exercisesCompleted     = exercisesCompleted + 1) }
        }
    }

    private fun handleSessionError(error: Throwable) {
        val maxAttempts = config?.reconnectMaxAttempts ?: GeminiConfig.DEFAULT_RECONNECT_ATTEMPTS
        Log.e(TAG, "Session error [attempts=${reconnectAttempts.get()}/$maxAttempts]: ${error.message}", error)

        engineScope.launch {
            transitionEngine(VoiceEngineState.ERROR)
            updateState { copy(errorMessage = error.message) }

            if (reconnectAttempts.get() < maxAttempts) {
                val attempt = reconnectAttempts.incrementAndGet()
                val delayMs = (config?.reconnectDelayMs ?: GeminiConfig.DEFAULT_RECONNECT_DELAY_MS) * attempt

                Log.d(TAG, "Reconnecting in ${delayMs}ms [attempt $attempt/$maxAttempts]")
                transitionEngine(VoiceEngineState.RECONNECTING)
                transitionConnection(ConnectionState.RECONNECTING)
                delay(delayMs)

                runCatching { reconnectInternal() }.onFailure { handleSessionError(it) }
            } else {
                Log.e(TAG, "Max reconnect attempts reached — giving up")
                transitionConnection(ConnectionState.FAILED)
                transitionEngine(VoiceEngineState.IDLE)
            }
        }
    }

    /**
     * ════════════════════════════════════════════════════════════════
     * FIX: startListening() перенесён в onStart {} (аналогично startSession).
     * ════════════════════════════════════════════════════════════════
     */
    private suspend fun reconnectInternal() {
        val uid = activeUserId ?: error("reconnectInternal: no activeUserId")

        cancelActiveJobs()
        audioPipeline.stopAll()

        // Session resumption не поддерживается Firebase SDK —
        // всегда делаем полный disconnect + reconnect.
        runCatching { geminiClient.disconnect() }
            .onFailure { Log.w(TAG, "disconnect() during reconnect: ${it.message}") }

        Log.d(TAG, "Reconnecting...")

        val snapshot = withContext(Dispatchers.IO) { buildKnowledgeSummary(uid) }
        val strategy = strategySelector.selectStrategy(snapshot)
        val sessionContext = withContext(Dispatchers.IO) {
            contextBuilder.buildSessionContext(
                userId            = uid,
                knowledgeSnapshot = snapshot,
                currentStrategy   = strategy,
                currentChapter    = 1,
                currentLesson     = 1,
            )
        }

        transitionConnection(ConnectionState.CONNECTING)
        withContext(Dispatchers.IO) { geminiClient.connect(sessionContext) }

        transitionEngine(VoiceEngineState.SESSION_ACTIVE)
        transitionConnection(ConnectionState.CONNECTED)
        reconnectAttempts.set(0)
        updateState { copy(errorMessage = null, currentStrategy = strategy) }

        // FIX: startListening() в onStart — гарантирует порядок receive → send
        sessionJob = geminiClient
            .receiveFlow()
            .onStart {
                Log.d(TAG, "receiveFlow started collecting (reconnect) — now starting audio")
                startListening()
            }
            .onEach  { response -> handleGeminiResponse(response) }
            .catch   { err      -> handleSessionError(err) }
            .launchIn(engineScope)

        Log.d(TAG, "✅ Reconnected successfully")
    }

    private fun buildStrategyChangeMessage(strategy: LearningStrategy): String =
        "Пожалуйста, переключись на стратегию ${strategy.displayNameRu} (${strategy.name})."

    private fun VoiceEngineState.isActiveSession(): Boolean = when (this) {
        VoiceEngineState.SESSION_ACTIVE,
        VoiceEngineState.LISTENING,
        VoiceEngineState.PROCESSING,
        VoiceEngineState.SPEAKING,
        VoiceEngineState.WAITING -> true
        else                     -> false
    }
}
