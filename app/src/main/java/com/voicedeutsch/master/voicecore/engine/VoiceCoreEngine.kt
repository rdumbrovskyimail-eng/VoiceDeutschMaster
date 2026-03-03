package com.voicedeutsch.master.voicecore.engine

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.session.SessionResult
import com.voicedeutsch.master.voicecore.session.AudioState
import com.voicedeutsch.master.voicecore.session.ConnectionState
import com.voicedeutsch.master.voicecore.session.VoiceSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface VoiceCoreEngine {

    // ── State observation ────────────────────────────────────────────────────

    val sessionState:    StateFlow<VoiceSessionState>
    val connectionState: StateFlow<ConnectionState>
    val audioState:      StateFlow<AudioState>
    val amplitudeFlow:   Flow<Float>

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

    // ── Дополнительные методы ──────────────────────────────────────────────────

    /**
     * @deprecated В новой архитектуре SDK управляет аудиопотоком. Метод — no-op.
     */
    suspend fun sendAudioStreamEnd()

    /**
     * Текущее количество использованных токенов за сессию.
     * Null если данные ещё не получены.
     */
    fun getTokenUsage(): GeminiClient.TokenUsage?
}
