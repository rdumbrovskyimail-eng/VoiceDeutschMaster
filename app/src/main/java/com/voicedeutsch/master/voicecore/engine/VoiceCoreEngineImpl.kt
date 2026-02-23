package com.voicedeutsch.master.voicecore.engine

import android.util.Log
import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.session.SessionResult
import com.voicedeutsch.master.domain.usecase.knowledge.BuildKnowledgeSummaryUseCase
import com.voicedeutsch.master.domain.usecase.learning.EndLearningSessionUseCase
import com.voicedeutsch.master.domain.usecase.learning.StartLearningSessionUseCase
import com.voicedeutsch.master.util.NetworkMonitor
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
 * МИГРАЦИЯ: EphemeralTokenService + HttpClient → firebase-ai GeminiClient
 * ════════════════════════════════════════════════════════════════════════════
 *
 * БЫЛО:
 *   ephemeralTokenService.fetchToken(userId)     — ручной HTTP → Cloud Function
 *   GeminiClient(config, httpClient, json)       — Ktor WebSocket клиент
 *   geminiClient.receiveNextResponse()           — pull из Channel
 *   geminiClient.connect(cfg, context, token)    — с ephemeral token
 *
 * СТАЛО:
 *   GeminiClient(config, json)                  — firebase-ai LiveSession
 *   geminiClient.connect(sessionContext)         — App Check прозрачно
 *   geminiClient.receiveFlow()                   — cold Flow из LiveSession
 *   sessionJob = receiveFlow().onEach{}.launchIn — реактивный цикл
 *
 * УДАЛЕНО из конструктора:
 *   - httpClient: io.ktor.client.HttpClient      — WebSocket управляет SDK
 *   - ephemeralTokenService: EphemeralTokenService — заменён App Check
 *
 * ════════════════════════════════════════════════════════════════════════════
 * LIFECYCLE:
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   IDLE → INITIALIZING → IDLE (готов к сессии)
 *   startSession: CONTEXT_LOADING → CONNECTING → SESSION_ACTIVE → LISTENING / PROCESSING / SPEAKING
 *   endSession:   SESSION_ENDING → SAVING → IDLE
 *   error:        ERROR → RECONNECTING → SESSION_ACTIVE (или IDLE после max попыток)
 *
 * ════════════════════════════════════════════════════════════════════════════
 * CONCURRENCY:
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   [lifecycleMutex] — сериализует все lifecycle-переходы
 *   [_sessionState]  — пишется только через [updateState] / [transitionXxx]
 *   [sessionJob]     — Flow-подписка на receiveFlow(), отменяется в endSession/destroy
 *   [audioForwardJob]— форвардит AudioPipeline chunks → GeminiClient параллельно
 */
class VoiceCoreEngineImpl(
    private val contextBuilder: ContextBuilder,
    private val functionRouter: FunctionRouter,
    private val audioPipeline: AudioPipeline,
    private val strategySelector: StrategySelector,
    private val geminiClient: GeminiClient,            // ✅ firebase-ai SDK, без Ktor
    private val buildKnowledgeSummary: BuildKnowledgeSummaryUseCase,
    private val startLearningSession: StartLearningSessionUseCase,
    private val endLearningSession: EndLearningSessionUseCase,
    private val networkMonitor: NetworkMonitor,
    // httpClient         УДАЛЁН — WebSocket управляет firebase-ai SDK
    // ephemeralTokenService УДАЛЁН — заменён Firebase App Check
) : VoiceCoreEngine {

    companion object {
        private const val TAG = "VoiceCoreEngine"

        /**
         * Максимальное время выполнения функции (мс).
         * Gemini Live API разрывает сессию если toolResponse не приходит ~15 сек.
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
    @Volatile private var activeSessionId: String? = null
    @Volatile private var activeUserId: String? = null
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

    /**
     * Инициализирует AudioPipeline и сохраняет GeminiConfig.
     *
     * ✅ ИЗМЕНЕНИЕ: GeminiClient больше не пересоздаётся здесь.
     * VoiceCoreModule.kt создаёт GeminiClient как factory — новый экземпляр
     * на каждую инжекцию. Пересоздавать его в initialize() нет смысла.
     */
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

    /**
     * Запускает голосовую сессию.
     *
     * ✅ ИЗМЕНЕНИЕ: ephemeralTokenService.fetchToken() УДАЛЁН.
     * geminiClient.connect(sessionContext) — без token-параметра.
     * Firebase App Check SDK прозрачно прикрепляет токен к каждому запросу.
     *
     * ✅ ИЗМЕНЕНИЕ: sessionJob теперь подписывается на receiveFlow() (cold Flow)
     * вместо цикла while { receiveNextResponse() }.
     * Flow автоматически завершается при закрытии LiveSession.
     */
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

            transitionEngine(VoiceEngineState.CONTEXT_LOADING)
            reconnectAttempts.set(0)

            // 1. Domain: создать запись сессии
            val sessionData = withContext(Dispatchers.IO) {
                startLearningSession(userId)
            }
            activeSessionId = sessionData.session.id
            activeUserId    = userId
            sessionStartMs  = System.currentTimeMillis()

            // 2. Domain: построить снимок знаний
            val snapshot = withContext(Dispatchers.IO) {
                buildKnowledgeSummary(userId)
            }

            // 3. Выбрать стратегию обучения
            val strategy = strategySelector.selectStrategy(snapshot)

            // 4. Собрать полный контекст сессии
            val sessionContext = withContext(Dispatchers.IO) {
                contextBuilder.buildSessionContext(
                    userId            = userId,
                    knowledgeSnapshot = snapshot,
                    currentStrategy   = strategy,
                    currentChapter    = sessionData.currentChapter,
                    currentLesson     = sessionData.currentLesson,
                )
            }

            // 5. ✅ Подключиться к Gemini Live API через firebase-ai
            // App Check токен прикрепляется SDK автоматически — ephemeralTokenService не нужен.
            transitionEngine(VoiceEngineState.CONNECTING)
            transitionConnection(ConnectionState.CONNECTING)

            withContext(Dispatchers.IO) {
                geminiClient.connect(sessionContext)
            }

            // 6. Активировать состояние сессии
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

            // 7. ✅ Запустить реактивный цикл через receiveFlow()
            // Flow завершается сам при закрытии LiveSession — нет нужды в while(isActive).
            sessionJob = geminiClient
                .receiveFlow()
                .onEach  { response -> handleGeminiResponse(response) }
                .catch   { error    -> handleSessionError(error) }
                .launchIn(engineScope)

            // 8. Начать запись микрофона
            startListening()

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
                    geminiClient.disconnect()
                    endLearningSession(sessionId)
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
                    isVoiceActive    = false,
                    isListening      = false,
                    isSpeaking       = false,
                    isProcessing     = false,
                    currentTranscript = "",
                    voiceTranscript  = "",
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

        // Форвардим PCM-чанки из AudioPipeline → GeminiClient.sendAudioChunk()
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
        // В firebase-ai Live API toolResponse отправляется автоматически через
        // handleGeminiResponse → functionRouter.route → geminiClient.sendFunctionResult.
        // Этот метод остаётся для внешних вызовов (напр. из UI при ручном подтверждении).
        geminiClient.sendFunctionResult(callId, name, resultJson)
    }

    // ── Обработка ответов Gemini ──────────────────────────────────────────────

    /**
     * Обрабатывает один GeminiResponse из receiveFlow().
     *
     * Вызывается в .onEach {} — в engineScope (Dispatchers.Default).
     * Для IO-операций (Room, FunctionRouter) явно переключаем на Dispatchers.IO.
     */
    private suspend fun handleGeminiResponse(response: GeminiResponse) {
        when {

            // ── Пользователь перебил модель ──────────────────────────────────
            response.isInterrupted -> {
                Log.d(TAG, "User interrupted AI — flushing audio queue")
                audioPipeline.flushPlayback()
                transitionAudio(AudioState.IDLE)
                transitionEngine(VoiceEngineState.LISTENING)
                updateState { copy(isSpeaking = false, isProcessing = false) }
            }

            // ── Аудио ответ ──────────────────────────────────────────────────
            response.hasAudio() -> {
                transitionAudio(AudioState.PLAYING)
                updateState {
                    copy(
                        isProcessing  = false,
                        isSpeaking    = true,
                        // outputTranscript — субтитры что говорит Gemini (TTS→text)
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
                }
            }

            // ── Function call ────────────────────────────────────────────────
            response.hasFunctionCall() -> {
                updateState { copy(isProcessing = true) }
                val call = response.functionCall!!

                // ⚠️ userId null в активной сессии — аномалия.
                // Отвечаем Gemini ошибкой немедленно, иначе ~15 сек таймаут разорвёт сессию.
                val userId    = activeUserId
                val sessionId = activeSessionId

                val result = if (userId == null) {
                    Log.e(TAG, "hasFunctionCall: activeUserId == null, returning error to Gemini")
                    FunctionRouter.FunctionCallResult(
                        functionName = call.name,
                        success      = false,
                        resultJson   = """{"error":"no active user session"}""",
                    )
                } else {
                    // Таймаут 12 сек — буфер до ~15-секундного таймаута Gemini Live API.
                    try {
                        withTimeout(FUNCTION_CALL_TIMEOUT_MS) {
                            withContext(Dispatchers.IO) {
                                functionRouter.route(
                                    functionName = call.name,
                                    argsJson     = call.argsJson,
                                    userId       = userId,
                                    sessionId    = sessionId,
                                )
                            }
                        }
                    } catch (e: TimeoutCancellationException) {
                        Log.e(TAG, "Function '${call.name}' timed out after ${FUNCTION_CALL_TIMEOUT_MS}ms")
                        FunctionRouter.FunctionCallResult(
                            functionName = call.name,
                            success      = false,
                            resultJson   = """{"error":"function execution timed out"}""",
                        )
                    }
                }

                // toolResponse уходит к Gemini в любом случае (success или error)
                applyFunctionSideEffects(call.name, result)
                geminiClient.sendFunctionResult(
                    callId    = call.id,
                    name      = result.functionName,
                    resultJson = result.resultJson,
                )
                updateState { copy(isProcessing = false) }
            }

            // ── Транскрипция ─────────────────────────────────────────────────
            // inputTranscript — что сказал пользователь (STT)
            response.inputTranscript != null -> {
                updateState { copy(currentTranscript = response.inputTranscript) }
                transitionEngine(VoiceEngineState.PROCESSING)
            }

            // Fallback: текстовый контент без аудио
            response.hasTranscript() -> {
                updateState { copy(currentTranscript = response.transcript ?: "") }
                transitionEngine(VoiceEngineState.PROCESSING)
            }
        }
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    private fun applyFunctionSideEffects(
        functionName: String,
        result: FunctionRouter.FunctionCallResult,
    ) {
        if (!result.success) return
        when (functionName) {
            "save_word_knowledge"       -> updateState { copy(wordsLearnedInSession  = wordsLearnedInSession + 1) }
            "get_words_for_repetition"  -> updateState { copy(wordsReviewedInSession = wordsReviewedInSession + 1) }
            "mark_lesson_complete",
            "advance_to_next_lesson"    -> updateState { copy(exercisesCompleted     = exercisesCompleted + 1) }
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

                runCatching {
                    val uid = activeUserId ?: return@launch
                    startSession(uid)
                }.onFailure { handleSessionError(it) }

            } else {
                Log.e(TAG, "Max reconnect attempts reached — giving up")
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
        else                     -> false
    }
}
