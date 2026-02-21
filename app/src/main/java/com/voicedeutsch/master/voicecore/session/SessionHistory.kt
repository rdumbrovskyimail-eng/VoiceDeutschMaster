package com.voicedeutsch.master.voicecore.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory conversation history for the current session.
 * Sent to Gemini with each request to maintain context.
 *
 * Architecture line 846 (SessionHistory.kt).
 */
class SessionHistory {

    data class Turn(
        val role: String,     // "user" or "model"
        val text: String,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private val _turns = MutableStateFlow<List<Turn>>(emptyList())
    val turns: StateFlow<List<Turn>> = _turns.asStateFlow()

    fun addUserTurn(text: String) {
        _turns.value = _turns.value + Turn(role = "user", text = text)
        trimIfNeeded()
    }

    fun addModelTurn(text: String) {
        _turns.value = _turns.value + Turn(role = "model", text = text)
        trimIfNeeded()
    }

    fun clear() {
        _turns.value = emptyList()
    }

    /** Keep last N turns to stay within Gemini context window. */
    private fun trimIfNeeded(maxTurns: Int = MAX_TURNS) {
        if (_turns.value.size > maxTurns) {
            _turns.value = _turns.value.takeLast(maxTurns)
        }
    }

    /** Build conversation text for prompt injection. */
    fun toPromptText(): String = _turns.value.joinToString("\n") { turn ->
        "${turn.role.uppercase()}: ${turn.text}"
    }

    companion object {
        const val MAX_TURNS = 50
    }
}