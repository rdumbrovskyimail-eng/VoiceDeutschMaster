package com.voicedeutsch.master.domain.model.session

import kotlinx.serialization.Serializable

/**
 * A discrete event that occurred during a learning session.
 *
 * Events form a timeline of the session and are persisted for analytics
 * and for building richer Gemini context.
 */
@Serializable
data class SessionEvent(
    val id: String,
    val sessionId: String,
    val eventType: SessionEventType,
    val timestamp: Long = System.currentTimeMillis(),
    val detailsJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Types of events that can occur within a session.
 */
@Serializable
enum class SessionEventType {
    SESSION_START,
    SESSION_END,
    WORD_LEARNED,
    WORD_REVIEWED,
    RULE_PRACTICED,
    PRONUNCIATION_ATTEMPT,
    MISTAKE,
    STRATEGY_CHANGE,
    LESSON_COMPLETE,
    BREAK_TAKEN,
    ACHIEVEMENT_EARNED,
    USER_REQUEST,
    REPETITION_COMPLETE,
    TOPIC_CHANGED
}