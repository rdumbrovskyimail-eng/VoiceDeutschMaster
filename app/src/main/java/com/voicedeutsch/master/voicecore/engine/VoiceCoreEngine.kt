package com.voicedeutsch.master.voicecore.engine

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.session.SessionResult
import com.voicedeutsch.master.voicecore.session.AudioState
import com.voicedeutsch.master.voicecore.session.ConnectionState
import com.voicedeutsch.master.voicecore.session.VoiceSessionState
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

    val sessionState: StateFlow<VoiceSessionState>
    val connectionState: StateFlow<ConnectionState>
    val audioState: StateFlow<AudioState>

    // ── Lifecycle ────────────────────────────────────────────────────────────

    /**
     * Initialises the engine and underlying Gemini client with [config].
     * Must be called before [startSession]. Idempotent if already initialised
     * with the same config; re-initialises on config change.
     */
    suspend fun initialize(config: GeminiConfig)

    /**
     * Builds context, connects to Gemini, and starts the session for [userId].
     * @return the initial [VoiceSessionState] after connection is established.
     */
    suspend fun startSession(userId: String): VoiceSessionState

    /**
     * Gracefully ends the active session, saves statistics, and disconnects.
     * @return [SessionResult] if a session was active, `null` otherwise.
     */
    suspend fun endSession(): SessionResult?

    /**
     * Releases all resources (coroutine scope, audio hardware, network).
     * After calling this the engine cannot be used without [initialize].
     */
    suspend fun destroy()

    // ── Audio control ────────────────────────────────────────────────────────

    fun startListening()
    fun stopListening()
    fun pausePlayback()
    fun resumePlayback()

    // ── Manual control (rarely used from UI) ─────────────────────────────────

    suspend fun sendTextMessage(text: String)
    suspend fun requestStrategyChange(strategy: LearningStrategy)
    suspend fun requestBookNavigation(chapter: Int, lesson: Int)

    /**
     * Delivers the result of a function call back to Gemini so it can
     * continue the generation stream. Called by [FunctionRouter] internally,
     * but exposed here to allow manual result injection in tests.
     */
    suspend fun submitFunctionResult(callId: String, resultJson: String)
}