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
            description = "Обновить уровень CEFR пользователя (A1→A2→B1→B2→C1→C2).",
            params = mapOf(
                "cefr_level" to ("string"  to "Новый уровень: A1, A2, B1, B2, C1, C2"),
                "sub_level"  to ("integer" to "Подуровень 1-10"),
            ),
            required = listOf("cefr_level"),
        ),
        FunctionRegistry.declare(
            name = "get_user_statistics",
            description = "Получить сводную статистику пользователя.",
        ),
    )
}
