package com.voicedeutsch.master.voicecore.functions

import com.voicedeutsch.master.data.remote.gemini.GeminiFunctionDeclaration

/**
 * Function declarations for core learning interactions.
 * Architecture line 831 (LearningFunctions.kt).
 */
object LearningFunctions {

    val declarations: List<GeminiFunctionDeclaration> = listOf(
        FunctionRegistry.declare(
            name = "save_pronunciation_result",
            description = "Сохранить результат оценки произношения.",
            params = mapOf(
                "word" to ("string" to "Слово, которое произносил пользователь"),
                "score" to ("number" to "Общая оценка 0.0-1.0"),
                "problem_sounds" to ("string" to "JSON-массив проблемных звуков, например [\"ü\",\"ö\"]"),
                "attempt_number" to ("integer" to "Номер попытки"),
            ),
            required = listOf("word", "score"),
        ),
        FunctionRegistry.declare(
            name = "get_pronunciation_targets",
            description = "Получить звуки и слова для тренировки произношения.",
        ),
    )
}