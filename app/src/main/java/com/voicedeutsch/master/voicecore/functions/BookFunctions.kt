package com.voicedeutsch.master.voicecore.functions

// ✅ ИСПРАВЛЕНО: удалён неверный импорт из com.voicedeutsch.master.data.remote.gemini.
// GeminiFunctionDeclaration находится в том же пакете (voicecore.functions).

/**
 * Function declarations for book/lesson operations.
 * Architecture line 834 (BookFunctions.kt).
 *
 * ════════════════════════════════════════════════════════════════════════════
 * ИЗМЕНЕНИЯ (Баг #2 — "Слепой переход по книге"):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   ДОБАВЛЕНО: декларация read_lesson_paragraph.
 *
 *   Функция нужна потому, что advance_to_next_lesson теперь возвращает только
 *   первые 3000 символов текста урока (new_lesson_preview). Если поле
 *   text_truncated = true, ИИ читает оставшийся текст постранично через
 *   read_lesson_paragraph(index), пока has_next = false.
 */
object BookFunctions {

    val declarations: List<GeminiFunctionDeclaration> = listOf(
        FunctionRegistry.declare(
            name        = "get_current_lesson",
            description = "Получить текущий урок, его содержание и словарь.",
            // ✅ ИСПРАВЛЕНИЕ: Добавлен dummy-параметр
            params      = mapOf("dummy" to ("string" to "Игнорируемый параметр, передай 'ok'")),
        ),
        FunctionRegistry.declare(
            name        = "advance_to_next_lesson",
            description = "Перейти к следующему уроку. Вызывай после завершения текущего.",
            params      = mapOf("score" to ("number" to "Оценка за урок 0.0-1.0")),
            required    = listOf("score"),
        ),
        FunctionRegistry.declare(
            name        = "mark_lesson_complete",
            description = "Отметить урок как пройденный.",
            params      = mapOf(
                "chapter" to ("integer" to "Номер главы"),
                "lesson"  to ("integer" to "Номер урока"),
            ),
            required    = listOf("chapter", "lesson"),
        ),
        // ✅ НОВАЯ ФУНКЦИЯ: постраничное чтение урока
        FunctionRegistry.declare(
            name        = "read_lesson_paragraph",
            description = """
                Получить один абзац текста текущего урока по индексу (0-based).
                Вызывай последовательно после advance_to_next_lesson,
                если поле text_truncated = true, пока has_next = false.
                Ответ содержит: index, total, has_next, paragraph.
            """.trimIndent(),
            params      = mapOf(
                "index" to ("integer" to "Индекс абзаца, начиная с 0"),
            ),
            required    = listOf("index"),
        ),
    )
}
 