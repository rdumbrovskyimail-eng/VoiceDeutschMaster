package com.voicedeutsch.master.domain.usecase.session

import com.voicedeutsch.master.domain.model.session.SessionEvent
import com.voicedeutsch.master.domain.model.session.SessionEventType
import com.voicedeutsch.master.domain.repository.SessionRepository
import com.voicedeutsch.master.util.DateUtils
import com.voicedeutsch.master.util.generateUUID

/**
 * Saves a session event (word_learned, mistake, strategy_change, etc.).
 * Called from FunctionRouter after each Function Call from Gemini.
 */
class SaveSessionEventUseCase(
    private val sessionRepository: SessionRepository
) {

    suspend operator fun invoke(
        sessionId: String,
        eventType: SessionEventType,
        details: Map<String, Any> = emptyMap()
    ) {
        val now = DateUtils.nowTimestamp()

        val detailsJson = if (details.isEmpty()) {
            "{}"
        } else {
            buildDetailsJson(details)
        }

        val event = SessionEvent(
            id = generateUUID(),
            sessionId = sessionId,
            eventType = eventType,
            timestamp = now,
            detailsJson = detailsJson,
            createdAt = now
        )

        sessionRepository.addSessionEvent(event)
    }

    private fun buildDetailsJson(details: Map<String, Any>): String {
        val entries = details.entries.joinToString(",") { (key, value) ->
            val jsonValue = when (value) {
                is String -> "\"${escapeJson(value)}\""
                is Boolean -> value.toString()
                is Number -> value.toString()
                else -> "\"${escapeJson(value.toString())}\""
            }
            "\"${escapeJson(key)}\":$jsonValue"
        }
        return "{$entries}"
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}