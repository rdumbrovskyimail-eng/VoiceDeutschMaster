package com.voicedeutsch.master.voicecore.session

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.session.SessionResult
import com.voicedeutsch.master.util.DateUtils
import com.voicedeutsch.master.util.generateUUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the lifecycle of a single voice learning session.
 *
 * Responsibilities:
 *   - Track session timing (start, pause, resume, end)
 *   - Track items learned/reviewed within session
 *   - Track strategy changes
 *   - Produce [SessionResult] at session end
 *
 * Architecture line 844 (VoiceSessionManager.kt).
 */
class VoiceSessionManager {

    private val _state = MutableStateFlow(SessionData())
    val state: StateFlow<SessionData> = _state.asStateFlow()

    data class SessionData(
        val sessionId: String = "",
        val userId: String = "",
        val isActive: Boolean = false,
        val startedAt: Long = 0,
        val pausedAt: Long? = null,
        val totalPausedMs: Long = 0,
        val currentStrategy: LearningStrategy = LearningStrategy.LINEAR_BOOK,
        val strategiesUsed: Set<LearningStrategy> = emptySet(),
        val wordsLearned: Int = 0,
        val wordsReviewed: Int = 0,
        val rulesLearned: Int = 0,
        val mistakeCount: Int = 0,
        val correctCount: Int = 0,
    )

    fun startSession(userId: String, strategy: LearningStrategy): String {
        val sessionId = generateUUID()
        _state.value = SessionData(
            sessionId = sessionId,
            userId = userId,
            isActive = true,
            startedAt = DateUtils.nowTimestamp(),
            currentStrategy = strategy,
            strategiesUsed = setOf(strategy),
        )
        return sessionId
    }

    fun pause() {
        _state.update { it.copy(pausedAt = DateUtils.nowTimestamp()) }
    }

    fun resume() {
        _state.update { current ->
            val pausedDuration = current.pausedAt?.let { DateUtils.nowTimestamp() - it } ?: 0
            current.copy(
                pausedAt = null,
                totalPausedMs = current.totalPausedMs + pausedDuration,
            )
        }
    }

    fun switchStrategy(strategy: LearningStrategy) {
        _state.update { it.copy(strategiesUsed = it.strategiesUsed + strategy, currentStrategy = strategy) }
    }

    fun recordWordLearned() = _state.update { it.copy(wordsLearned = it.wordsLearned + 1) }
    fun recordWordReviewed() = _state.update { it.copy(wordsReviewed = it.wordsReviewed + 1) }
    fun recordRuleLearned() = _state.update { it.copy(rulesLearned = it.rulesLearned + 1) }
    fun recordMistake() = _state.update { it.copy(mistakeCount = it.mistakeCount + 1) }
    fun recordCorrect() = _state.update { it.copy(correctCount = it.correctCount + 1) }

    fun endSession(): SessionResult {
        val data = _state.value
        val now = DateUtils.nowTimestamp()
        val durationMs = now - data.startedAt - data.totalPausedMs

        _state.value = SessionData() // Reset

        return SessionResult(
            sessionId = data.sessionId,
            durationMinutes = (durationMs / 60_000).toInt(),
            wordsLearned = data.wordsLearned,
            wordsReviewed = data.wordsReviewed,
            rulesPracticed = data.rulesLearned,
            exercisesCompleted = data.correctCount + data.mistakeCount,
            exercisesCorrect = data.correctCount,
            averageScore = 0f,
            averagePronunciationScore = 0f,
            strategiesUsed = data.strategiesUsed.map { it.name },
            summary = ""
        )
    }

    fun reset() {
        _state.value = SessionData()
    }
}