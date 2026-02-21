package com.voicedeutsch.master.voicecore.functions

import com.voicedeutsch.master.data.remote.gemini.GeminiFunctionDeclaration

/**
 * Function declarations for book/lesson operations.
 * Architecture line 834 (BookFunctions.kt).
 */
object BookFunctions {

    val declarations: List<GeminiFunctionDeclaration> = listOf(
        FunctionRegistry.declare(
            name = "get_current_lesson",
            description = "Получить текущий урок, его содержание и словарь.",
        ),
        FunctionRegistry.declare(
            name = "advance_to_next_lesson",
            description = "Перейти к следующему уроку. Вызывай после завершения текущего.",
            params = mapOf("score" to ("number" to "Оценка за урок 0.0-1.0")),
            required = listOf("score"),
        ),
        FunctionRegistry.declare(
            name = "mark_lesson_complete",
            description = "Отметить урок как пройденный.",
            params = mapOf(
                "chapter" to ("integer" to "Номер главы"),
                "lesson" to ("integer" to "Номер урока"),
            ),
            required = listOf("chapter", "lesson"),
        ),
    )
}