package com.voicedeutsch.master.presentation.screen.session

import android.content.Context
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

/**
 * ViewModel for [SessionScreen].
 *
 * Manages voice session lifecycle via [VoiceCoreEngine].
 *
 * Production Standard 2026:
 * API ключ не хранится на устройстве.
 * EphemeralTokenService вызывается внутри VoiceCoreEngineImpl.startSession().
 *
 * ✅ FIX 1: Убран runBlocking из onCleared().
 * runBlocking блокировал Main Thread на 5.5 сек → ANR (Android убивает через 5 сек).
 * Заменён на fire-and-forget корутину с NonCancellable + таймаутом.
 *
 * ✅ FIX 2: VoiceSessionService теперь стартует при начале сессии и останавливается при конце.
 * Без foreground service Android убивает микрофон когда приложение уходит в фон.
 *
 * ✅ FIX 3: Убран context.startService(stopIntent) из onCleared().
 * На Android 16 вызов startService из onCleared() (когда приложение уходит в фон)
 * вызывает ForegroundServiceDidNotStartInTimeException или краш.
 * Остановка сервиса привязана строго к endSession().
 * При уничтожении процесса Android сам убьёт Foreground Service.
 */
class SessionViewModel(
    private val voiceCoreEngine: VoiceCoreEngine,
    private val userRepository: UserRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val context: Context,
) : ViewModel() {

    val voiceState: StateFlow<VoiceSessionState> = voiceCoreEngine.sessionState

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

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

                // ✅ FIX 2: Стартуем foreground service — удерживает микрофон в фоне.
                // Без него Android 14+ убивает AudioRecord когда экран гаснет.
                ContextCompat.startForegroundService(
                    context,
                    VoiceSessionService.startIntent(context),
                )

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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = runCatching {
                voiceCoreEngine.endSession()
            }.getOrNull()

            // ✅ FIX 2: Останавливаем foreground service вместе с сессией.
            // Единственное место остановки сервиса — здесь, не в onCleared().
            context.startService(VoiceSessionService.stopIntent(context))

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSessionActive = false,
                    sessionResult = result,
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
                isLoading = false,
                errorMessage = "Доступ к микрофону запрещён. Откройте Настройки → Приложения → Разрешения."
            )
        }
    }

    override fun onCleared() {
        super.onCleared()

        // ✅ FIX 1: Убран runBlocking → был ANR (блокировал Main Thread на 5.5 сек).
        // Теперь: fire-and-forget корутина на Dispatchers.IO.
        // NonCancellable гарантирует что cleanup выполнится даже после cancel ViewModel.
        // Timeout 5 сек — защита от зависания endSession().
        //
        // ✅ FIX 3: context.startService(stopIntent) УДАЛЁН отсюда.
        // На Android 16 вызов startService из onCleared() вызывает
        // ForegroundServiceDidNotStartInTimeException когда приложение уходит в фон.
        // Сервис останавливается строго в endSession().
        // Если ViewModel уничтожается системой — Android сам убьёт Foreground Service.
        CoroutineScope(Dispatchers.IO + NonCancellable).launch {
            runCatching {
                withTimeout(5_000L) { voiceCoreEngine.endSession() }
            }.onFailure { e ->
                android.util.Log.e("SessionViewModel", "cleanup endSession failed", e)
            }
        }
    }
}
