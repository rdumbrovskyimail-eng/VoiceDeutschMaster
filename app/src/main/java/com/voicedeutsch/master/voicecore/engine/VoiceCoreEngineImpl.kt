package com.voicedeutsch.master.voicecore.engine

import android.util.Log
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.PublicPreviewAPI
import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.session.SessionResult
import com.voicedeutsch.master.domain.usecase.knowledge.BuildKnowledgeSummaryUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.FlushKnowledgeSyncUseCase
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.concurrent.atomic.AtomicInteger

/**
 * Heart of the system — оркестрирует все VoiceCore-компоненты.
 *
 * Использует startAudioConversation / stopAudioConversation из Firebase AI SDK.
 * SDK сам управляет микрофоном, отправкой/приёмом аудио и воспроизведением.
 * Function calls обрабатываются синхронно через callback ::handleFunctionCall.
 */
@OptIn(PublicPreviewAPI::class)
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

    private val engineScope    = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lifecycleMutex = Mutex()

    // ── State ─────────────────────────────────────────────────────────────────

    private val _sessionState    = MutableStateFlow(VoiceSessionState())
    override val sessionState:   StateFlow<VoiceSessionState> = _sessionState.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _audioState      = MutableStateFlow(AudioState.IDLE)
    override val audioState:     StateFlow<AudioState> = _audioState.asStateFlow()

    override val amplitudeFlow: Flow<Float> = _audioState
        .map { state ->
            when (state) {
                AudioState.RECORDING -> 0.5f
                AudioState.PLAYING   -> 0.7f
                else                 -> 0f
            }
        }

    @Volatile private var config: GeminiConfig? = null
    @Volatile private var activeSessionId: String? = null
    @Volatile private var activeUserId:    String? = null
    private val reconnectAttempts = AtomicInteger(0)
    @Volatile private var sessionStartMs: Long = 0L
    private val reconnectMutex = Mutex()

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
                geminiClient.config = config
            }.onFailure { error ->
                transitionEngine(VoiceEngineState.ERROR)
                updateState { copy(errorMessage = error.message) }
                throw error
            }

            transitionEngine(VoiceEngineState.IDLE)
            Log.d(TAG, "Engine initialized [model=${config.modelName}]")
        }
    }

    override suspend fun startSession(userId: String): VoiceSessionState =
        lifecycleMutex.withLock {
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

            val sessionData = withContext(Dispatchers.IO) { startLearningSession(userId) }
            activeSessionId = sessionData.session.id
            activeUserId    = userId
            sessionStartMs  = System.currentTimeMillis()
            engineScope.launch {
                while (isActive) {
                    delay(1000L)
                    updateState { copy(sessionDurationMs = System.currentTimeMillis() - sessionStartMs) }
                }
            }

            val snapshot = withContext(Dispatchers.IO) { buildKnowledgeSummary(userId) }
            val strategy = strategySelector.selectStrategy(snapshot)

            val sessionContext = withContext(Dispatchers.IO) {
                contextBuilder.buildSessionContext(
                    userId            = userId,
                    knowledgeSnapshot = snapshot,
                    currentStrategy   = strategy,
                    currentChapter    = sessionData.currentChapter,
                    currentLesson     = sessionData.currentLesson,
                    maxContextTokens  = cfg.maxContextTokens,
                )
            }

            transitionEngine(VoiceEngineState.CONNECTING)
            transitionConnection(ConnectionState.CONNECTING)

            val connectResult = runCatching {
                withContext(Dispatchers.IO) { geminiClient.connect(sessionContext) }
            }

            if (connectResult.isFailure) {
                val error = connectResult.exceptionOrNull()!!
                Log.e(TAG, "connect() failed: ${error.message}", error)
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

            startListening()

            Log.d(TAG, "Session started [userId=$userId, sessionId=${sessionData.session.id}]")
            _sessionState.value
        }

    override suspend fun endSession(): SessionResult? {
        val sessionId = activeSessionId ?: return null

        lifecycleMutex.withLock {
            val current = _sessionState.value.engineState
            if (!current.isActiveSession()) return@withLock
            transitionEngine(VoiceEngineState.SESSION_ENDING)
        }

        return lifecycleMutex.withLock {
            transitionEngine(VoiceEngineState.SAVING)

            val sessionResult: SessionResult? = withContext(Dispatchers.IO) {
                runCatching {
                    runCatching { geminiClient.stopConversation() }
                        .onFailure { Log.w(TAG, "stopConversation() warning: ${it.message}") }

                    transitionAudio(AudioState.IDLE)
                    transitionConnection(ConnectionState.DISCONNECTED)

                    runCatching { geminiClient.disconnect() }
                        .onFailure { Log.w(TAG, "disconnect() warning: ${it.message}") }

                    geminiClient.clearResumptionHandle()

                    val result = endLearningSession(sessionId)

                    val syncOk = runCatching { flushKnowledgeSync() }.getOrElse { e ->
                        Log.w(TAG, "flushKnowledgeSync failed (data safe in Room): ${e.message}")
                        false
                    }
                    if (syncOk) Log.d(TAG, "Knowledge sync flushed")
                    else        Log.w(TAG, "Knowledge sync deferred — will retry next session")

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

    /**
     * Запускает голосовой разговор через SDK.
     * SDK сам управляет микрофоном, аудиопотоком и воспроизведением.
     */
    override fun startListening() {
        if (!_sessionState.value.isSessionActive || _sessionState.value.isListening) return

        transitionAudio(AudioState.RECORDING)

        engineScope.launch {
            runCatching {
                geminiClient.startConversation(::handleFunctionCall)
            }.onFailure { e ->
                Log.e(TAG, "startConversation failed", e)
                transitionAudio(AudioState.IDLE)
                handleSessionError(e)
            }
        }
    }

    /**
     * Останавливает голосовой разговор. Соединение сохраняется.
     */
    override fun stopListening() {
        if (!_sessionState.value.isListening) return
        transitionAudio(AudioState.IDLE)

        engineScope.launch {
            runCatching { geminiClient.stopConversation() }
                .onFailure { Log.w(TAG, "stopConversation failed: ${it.message}") }
        }
    }

    override fun pausePlayback() {
        // SDK управляет воспроизведением — no-op
    }

    override fun resumePlayback() {
        // SDK управляет воспроизведением — no-op
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
        // В новой архитектуре function calls обрабатываются синхронно в callback.
        // Метод оставлен для обратной совместимости интерфейса.
        Log.w(TAG, "submitFunctionResult called — function calls are now handled via callback")
    }

    override suspend fun sendAudioStreamEnd() {
        // SDK управляет аудиопотоком — no-op
    }

    override fun getTokenUsage(): GeminiClient.TokenUsage? = geminiClient.lastTokenUsage

    // ── Синхронный callback для function calls ────────────────────────────────

    /**
     * Обрабатывает function call от Gemini синхронно.
     * Вызывается SDK-ом из startAudioConversation.
     * Использует runBlocking т.к. FunctionRouter.route() — suspend.
     */
    private fun handleFunctionCall(functionCall: FunctionCallPart): FunctionResponsePart {
        val userId = activeUserId
        val sessionId = activeSessionId

        Log.d(TAG, "Function call received: ${functionCall.name}")
        updateState { copy(isProcessing = true) }

        val response = if (userId == null) {
            Log.e(TAG, "handleFunctionCall: activeUserId == null")
            FunctionResponsePart(
                functionCall.name,
                JsonObject(mapOf("error" to JsonPrimitive("no active user session"))),
                functionCall.id,
            )
        } else {
            try {
                val result = runBlocking(Dispatchers.IO) {
                    withTimeout(FUNCTION_CALL_TIMEOUT_MS) {
                        val argsJson = JsonObject(functionCall.args?.mapValues { (_, v) -> v } ?: emptyMap()).toString()
                        functionRouter.route(
                            functionCall.name,
                            argsJson,
                            userId,
                            sessionId,
                        )
                    }
                }

                applyFunctionSideEffects(functionCall.name, result)

                val responseJson = try {
                    Json.parseToJsonElement(result.resultJson) as? JsonObject
                        ?: JsonObject(mapOf("result" to JsonPrimitive(result.resultJson)))
                } catch (_: Exception) {
                    JsonObject(mapOf("result" to JsonPrimitive(result.resultJson)))
                }

                FunctionResponsePart(functionCall.name, responseJson, functionCall.id)
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Function ${functionCall.name} timed out")
                FunctionResponsePart(
                    functionCall.name,
                    JsonObject(mapOf("error" to JsonPrimitive("function execution timed out"))),
                    functionCall.id,
                )
            } catch (e: Exception) {
                Log.e(TAG, "handleFunctionCall error for ${functionCall.name}", e)
                FunctionResponsePart(
                    functionCall.name,
                    JsonObject(mapOf("error" to JsonPrimitive(e.message ?: "unknown error"))),
                    functionCall.id,
                )
            }
        }

        updateState { copy(isProcessing = false) }
        return response
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

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
            if (!reconnectMutex.tryLock()) {
                Log.w(TAG, "handleSessionError: reconnect already in progress — ignoring duplicate call")
                return@launch
            }
            try {
                transitionEngine(VoiceEngineState.ERROR)
                updateState { copy(errorMessage = error.message) }

                if (reconnectAttempts.get() < maxAttempts) {
                    val attempt = reconnectAttempts.incrementAndGet()
                    val delayMs = (config?.reconnectDelayMs ?: GeminiConfig.DEFAULT_RECONNECT_DELAY_MS) * attempt

                    Log.d(TAG, "Reconnecting in ${delayMs}ms [attempt $attempt/$maxAttempts]")
                    transitionEngine(VoiceEngineState.RECONNECTING)
                    transitionConnection(ConnectionState.RECONNECTING)
                    delay(delayMs)

                    var lockAcquired = false
                    try {
                        lockAcquired = reconnectMutex.tryLock()
                        if (lockAcquired) reconnectInternal()
                    } finally {
                        if (lockAcquired) reconnectMutex.unlock()
                    }
                } else {
                    Log.e(TAG, "Max reconnect attempts reached — giving up")
                    transitionConnection(ConnectionState.FAILED)
                    transitionEngine(VoiceEngineState.IDLE)
                }
            } finally {
                if (reconnectMutex.isLocked) reconnectMutex.unlock()
            }
        }
    }

    private suspend fun reconnectInternal() {
        val uid = activeUserId ?: error("reconnectInternal: no activeUserId")

        runCatching { geminiClient.stopConversation() }
            .onFailure { Log.w(TAG, "stopConversation during reconnect: ${it.message}") }

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
                maxContextTokens  = config?.maxContextTokens ?: GeminiConfig.MAX_CONTEXT_TOKENS,
            )
        }

        transitionConnection(ConnectionState.CONNECTING)
        withContext(Dispatchers.IO) { geminiClient.connect(sessionContext) }

        transitionEngine(VoiceEngineState.SESSION_ACTIVE)
        transitionConnection(ConnectionState.CONNECTED)
        reconnectAttempts.set(0)
        updateState { copy(errorMessage = null, currentStrategy = strategy) }

        startListening()

        Log.d(TAG, "Reconnected successfully")
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
