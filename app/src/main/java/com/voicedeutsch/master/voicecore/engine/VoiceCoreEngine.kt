package com.voicedeutsch.master.voicecore.engine

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.session.SessionResult
import com.voicedeutsch.master.voicecore.session.AudioState
import com.voicedeutsch.master.voicecore.session.ConnectionState
import com.voicedeutsch.master.voicecore.session.VoiceSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for the Voice Core Engine — the heart of the system.
 * Architecture lines 530-570 (VoiceCoreEngine interface).
 *
 * Responsibilities:
 *   1. Initialize and configure the Gemini Live API connection
 *   2. Manage the full session lifecycle
 *   3. Coordinate AudioPipeline, ContextBuilder, FunctionRouter
 *   4. Handle errors and transparent reconnection
 *   5. Expose observable state to the UI layer via [StateFlow]
 *
 * Threading contract: all `suspend` functions are safe to call from any coroutine.
 * Non-suspend functions ([startListening] etc.) are safe to call from the main thread.
 */
interface VoiceCoreEngine {

    // ── State observation ────────────────────────────────────────────────────

    val sessionState:    StateFlow<VoiceSessionState>
    val connectionState: StateFlow<ConnectionState>
    val audioState:      StateFlow<AudioState>

    /**
     * RMS-амплитуда текущего аудио-чанка с микрофона, нормализованная в 0f..1f.
     *
     * Эмитирует значение на каждый PCM-чанк (~50 раз/сек).
     * Используется в SessionViewModel для обновления [currentAmplitude],
     * которое передаётся в VirtualAvatar как State<Float> и читается
     * только в фазе draw Canvas — без рекомпозиции компонента.
     *
     * Эмитирует 0f когда сессия не активна или микрофон не пишет.
     */
    val amplitudeFlow: Flow<Float>

    // ── Lifecycle ────────────────────────────────────────────────────────────

    suspend fun initialize(config: GeminiConfig)

    suspend fun startSession(userId: String): VoiceSessionState

    suspend fun endSession(): SessionResult?

    suspend fun destroy()

    // ── Audio control ────────────────────────────────────────────────────────

    fun startListening()
    fun stopListening()
    fun pausePlayback()
    fun resumePlayback()

    // ── Manual control ────────────────────────────────────────────────────────

    suspend fun sendTextMessage(text: String)
    suspend fun requestStrategyChange(strategy: LearningStrategy)
    suspend fun requestBookNavigation(chapter: Int, lesson: Int)

    suspend fun submitFunctionResult(callId: String, name: String, resultJson: String)
}
