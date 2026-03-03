package com.voicedeutsch.master.voicecore.functions

// ✅ ИСПРАВЛЕНО: удалён неверный импорт из com.voicedeutsch.master.data.remote.gemini.
// GeminiFunctionDeclaration находится в том же пакете (voicecore.functions).

/**
 * Function declarations for progress and statistics.
 * Architecture line 835 (ProgressFunctions.kt).
 */
object ProgressFunctions {

    val declarations: List<GeminiFunctionDeclaration> = listOf(
        FunctionRegistry.declare(
            name = "update_user_level",
            description = "Пересчитать уровень CEFR пользователя на основе его прогресса. Уровень определяется автоматически.",
        ),
        FunctionRegistry.declare(
            name = "get_user_statistics",
            description = "Получить сводную статистику пользователя.",
        ),
    )
}
