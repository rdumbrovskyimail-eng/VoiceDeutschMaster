package com.voicedeutsch.master.voicecore.functions

// ✅ ИСПРАВЛЕНО: удалён неверный импорт из com.voicedeutsch.master.data.remote.gemini.
// GeminiFunctionDeclaration находится в том же пакете (voicecore.functions).

/**
 * Function declarations for session management.
 * Architecture line 833 (SessionFunctions.kt).
 */
object SessionFunctions {

    val declarations: List<GeminiFunctionDeclaration> = listOf(
        FunctionRegistry.declare(
            name = "set_current_strategy",
            description = "Переключить стратегию обучения в текущей сессии.",
            params = mapOf(
                "strategy" to ("string" to "Стратегия: REPETITION, LINEAR_BOOK, FREE_PRACTICE, PRONUNCIATION, GAP_FILLING, GRAMMAR_DRILL, VOCABULARY_BOOST, LISTENING, ASSESSMENT"),
                "reason"   to ("string" to "Причина переключения"),
            ),
            required = listOf("strategy"),
        ),
        FunctionRegistry.declare(
            name = "log_session_event",
            description = "Записать событие сессии (начало темы, ошибка, достижение, и т.д.).",
            params = mapOf(
                "event_type" to ("string" to "Тип события"),
                "details"    to ("string" to "JSON с деталями"),
            ),
            required = listOf("event_type"),
        ),
    )
}
