package com.voicedeutsch.master.presentation.screen.session

import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.voicecore.engine.GeminiConfig
import com.voicedeutsch.master.voicecore.engine.VoiceCoreEngine
import com.voicedeutsch.master.voicecore.service.VoiceSessionService
import com.voicedeutsch.master.voicecore.session.VoiceSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * ViewModel for [SessionScreen].
 *
 * Manages voice session lifecycle via [VoiceCoreEngine].
 *
 * ════════════════════════════════════════════════════════════════════════════
 * ИЗМЕНЕНИЯ (Производительность — VirtualAvatar рекомпозиции):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   ДОБАВЛЕНО: currentAmplitude = mutableFloatStateOf(0f)
 *
 *   БЫЛО: FloatArray amplitudes прокидывался в VirtualAvatar.
 *   amplitudes.last() в remember(amplitudes) вызывал полную рекомпозицию
 *   VirtualAvatar 50 раз в секунду при каждом аудио-чанке.
 *
 *   СТАЛО: mutableFloatStateOf — Compose отслеживает изменение Float,
 *   но VirtualAvatar читает значение только внутри Canvas (фаза draw).
 *   Рекомпозиция компонента не происходит вообще — только перерисовка Canvas.
 *
 *   Подписка на voiceCoreEngine.amplitudeFlow запускается в startSession()
 *   и автоматически отменяется вместе с viewModelScope при endSession()
 *   через хранение Job в amplitudeJob.
 *
 *   ТРЕБОВАНИЕ К VoiceCoreEngine:
 *   Интерфейс VoiceCoreEngine должен экспортировать:
 *     val amplitudeFlow: Flow<Float>
 *   Реализация в VoiceCoreEngineImpl:
 *     override val amplitudeFlow: Flow<Float> = audioPipeline.audioChunks()
 *         .map { pcm -> RmsCalculator.calculate(pcm).coerceIn(0f, 1f) }
 *
 * ════════════════════════════════════════════════════════════════════════════
 * Прочие исправления (без изменений):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   ✅ FIX 1: Убран runBlocking из onCleared() → был ANR.
 *   ✅ FIX 2: VoiceSessionService стартует/останавливается вместе с сессией.
 *   ✅ FIX 3: stopService убран из onCleared() → ForegroundServiceDidNotStartInTimeException.
 *   ✅ FIX 4: startForegroundService обёрнут в try-catch → ForegroundServiceStartNotAllowedException.
 */
class SessionViewModel(
    private val voiceCoreEngine:      VoiceCoreEngine,
    private val userRepository:       UserRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val context:              Context,
) : ViewModel() {

    val voiceState: StateFlow<VoiceSessionState> = voiceCoreEngine.sessionState

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    // ✅ ДОБАВЛЕНО: текущая амплитуда для VirtualAvatar.
    //
    // mutableFloatStateOf — специализированный стейт для Float, без boxing.
    // Передаётся в VirtualAvatar как State<Float>:
    //   VirtualAvatar(currentAmplitude = viewModel.currentAmplitude)
    //
    // Изменение floatValue вызывает только перерисовку Canvas (фаза draw),
    // а не рекомпозицию VirtualAvatar — это ключевое отличие от FloatArray.
    val currentAmplitude = mutableFloatStateOf(0f)

    // Job подписки на амплитуду — отменяется при endSession()
    // чтобы не обновлять currentAmplitude когда сессия не активна.
    private var amplitudeJob: kotlinx.coroutines.Job? = null

    fun onEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.StartSession     -> startSession()
            is SessionEvent.EndSession       -> endSession()
            is SessionEvent.PauseResume      -> togglePause()
            is SessionEvent.ToggleMic        -> toggleMic()
            is SessionEvent.SendTextMessage  -> sendText(event.text)
            is SessionEvent.DismissError     -> _uiState.update { it.copy(errorMessage = null) }
            is SessionEvent.DismissResult    -> _uiState.update { it.copy(sessionResult = null) }
            is SessionEvent.ToggleTextInput  -> _uiState.update { it.copy(showTextInput = !it.showTextInput) }
            is SessionEvent.DismissHint      -> _uiState.update { it.copy(showHint = false) }
            is SessionEvent.ConsumeSnackbar  -> _uiState.update { it.copy(snackbarMessage = null) }
            is SessionEvent.PermissionDenied -> handlePermissionDenied()
        }
    }

    private fun startSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val userId = userRepository.getActiveUserId()
                    ?: error("No active user found. Please complete onboarding.")

                voiceCoreEngine.initialize(GeminiConfig())
                voiceCoreEngine.startSession(userId)

                // ✅ Подписываемся на амплитуду после старта сессии.
                // amplitudeFlow эмитирует RMS каждого PCM-чанка (0f..1f).
                // Запись в mutableFloatStateOf на Main не нужна — Compose
                // читает floatValue в Canvas на draw-фазе, поток Dispatchers.Default.
                amplitudeJob = voiceCoreEngine.amplitudeFlow
                    .onEach  { amp -> currentAmplitude.floatValue = amp }
                    .catch   { e -> android.util.Log.w("SessionViewModel", "amplitudeFlow error: ${e.message}") }
                    .launchIn(viewModelScope)

                // ✅ FIX 4: try-catch против ForegroundServiceStartNotAllowedException (Android 12+)
                try {
                    ContextCompat.startForegroundService(
                        context,
                        VoiceSessionService.startIntent(context),
                    )
                } catch (e: Exception) {
                    android.util.Log.w(
                        "SessionViewModel",
                        "Could not start foreground service (app likely in background): ${e.message}"
                    )
                }

                _uiState.update {
                    it.copy(isLoading = false, isSessionActive = true, showHint = false)
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error starting session")
                }
            }
        }
    }

    private fun endSession() {
        // Останавливаем обновление амплитуды — сессия заканчивается
        amplitudeJob?.cancel()
        amplitudeJob = null
        currentAmplitude.floatValue = 0f

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = runCatching {
                voiceCoreEngine.endSession()
            }.getOrNull()

            // ✅ FIX 2: Останавливаем foreground service вместе с сессией
            runCatching {
                context.startService(VoiceSessionService.stopIntent(context))
            }.onFailure { e ->
                android.util.Log.w("SessionViewModel", "Could not stop foreground service: ${e.message}")
            }

            _uiState.update {
                it.copy(
                    isLoading       = false,
                    isSessionActive = false,
                    sessionResult   = result,
                )
            }
        }
    }

    private fun togglePause() {
        val current = voiceState.value
        if (current.isListening) voiceCoreEngine.stopListening()
        else if (current.isSessionActive) voiceCoreEngine.startListening()
    }

    private fun toggleMic() {
        val current = voiceState.value
        if (current.isListening) voiceCoreEngine.stopListening()
        else voiceCoreEngine.startListening()
    }

    private fun sendText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            runCatching {
                voiceCoreEngine.sendTextMessage(text)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(snackbarMessage = error.message ?: "Failed to send message")
                }
            }
        }
    }

    private fun handlePermissionDenied() {
        _uiState.update {
            it.copy(
                isLoading    = false,
                errorMessage = "Доступ к микрофону запрещён. Откройте Настройки → Приложения → Разрешения."
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        amplitudeJob?.cancel()

        // ✅ FIX 1: fire-and-forget вместо runBlocking (был ANR)
        // ✅ FIX 3: stopService убран отсюда (был ForegroundServiceDidNotStartInTimeException)
        if (uiState.value.isSessionActive) {
            CoroutineScope(Dispatchers.IO + NonCancellable).launch {
                runCatching {
                    voiceCoreEngine.endSession()
                }.onFailure { e ->
                    android.util.Log.e("SessionViewModel", "cleanup endSession failed", e)
                }
            }
        }
    }
}
