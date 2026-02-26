package com.voicedeutsch.master.presentation.screen.session

import android.content.Context
import android.os.Build
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
 * Заменён на fire-and-forget корутину с NonCancellable.
 * withTimeout УДАЛЁН — он не отменяет вызовы к Room после перехода в поток SQLite,
 * зато сам бросает TimeoutCancellationException, маскируя реальную причину зависания.
 *
 * ✅ FIX 2: VoiceSessionService теперь стартует при начале сессии и останавливается при конце.
 * Без foreground service Android убивает микрофон когда приложение уходит в фон.
 *
 * ✅ FIX 3: Убран context.startService(stopIntent) из onCleared().
 * На Android 16 вызов startService из onCleared() (когда приложение уходит в фон)
 * вызывает ForegroundServiceDidNotStartInTimeException или краш.
 * Остановка сервиса привязана строго к endSession().
 * При уничтожении процесса Android сам убьёт Foreground Service.
 *
 * ✅ FIX 4: ForegroundServiceStartNotAllowedException (Android 12+).
 * startForegroundService() крашит приложение если оно свёрнуто в момент вызова.
 * Оборачиваем в try-catch — если сервис не стартовал, сессия продолжает работать
 * без foreground protection (микрофон может быть убит системой при уходе в фон,
 * но краш не происходит). Ошибка логируется для диагностики.
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

                // ✅ FIX 4: Оборачиваем в try-catch против ForegroundServiceStartNotAllowedException.
                // На Android 12+ (API 31) система бросает это исключение если приложение
                // находится в фоне в момент вызова startForegroundService().
                // Сессия голосового движка уже запущена — краш здесь недопустим.
                // Без сервиса сессия продолжит работать, но Android может убить микрофон
                // при уходе приложения в фон. Пользователь об этом не уведомляется —
                // это деградация, а не краш.
                try {
                    ContextCompat.startForegroundService(
                        context,
                        VoiceSessionService.startIntent(context),
                    )
                } catch (e: Exception) {
                    // ForegroundServiceStartNotAllowedException — API 31+, ловим через базовый Exception
                    // чтобы не добавлять minSdk зависимость на конкретный класс исключения.
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = runCatching {
                voiceCoreEngine.endSession()
            }.getOrNull()

            // ✅ FIX 2: Останавливаем foreground service вместе с сессией.
            // Единственное место остановки сервиса — здесь, не в onCleared().
            // stopService() безопасен даже если сервис не был запущен.
            runCatching {
                context.startService(VoiceSessionService.stopIntent(context))
            }.onFailure { e ->
                android.util.Log.w("SessionViewModel", "Could not stop foreground service: ${e.message}")
            }

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
        // ✅ FIX: withTimeout УДАЛЁН — не отменяет вызовы Room после перехода в поток SQLite.
        // Вместо этого: fire-and-forget корутина на Dispatchers.IO только если сессия активна.
        // NonCancellable гарантирует что cleanup выполнится даже после cancel ViewModel.
        //
        // ✅ FIX 3: context.startService(stopIntent) УДАЛЁН отсюда.
        // На Android 16 вызов startService из onCleared() вызывает
        // ForegroundServiceDidNotStartInTimeException когда приложение уходит в фон.
        // Сервис останавливается строго в endSession().
        // Если ViewModel уничтожается системой — Android сам убьёт Foreground Service.
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