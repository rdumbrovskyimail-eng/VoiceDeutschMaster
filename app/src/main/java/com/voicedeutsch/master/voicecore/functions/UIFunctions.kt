package com.voicedeutsch.master.voicecore.functions

// ✅ ИСПРАВЛЕНО: удалён неверный импорт из com.voicedeutsch.master.data.remote.gemini.
// GeminiFunctionDeclaration находится в том же пакете (voicecore.functions).

/**
 * Function declarations for UI-driven actions.
 * Architecture line 836 (UIFunctions.kt).
 */
object UIFunctions {

    val declarations: List<GeminiFunctionDeclaration> = listOf(
        FunctionRegistry.declare(
            name = "show_word_card",
            description = "Показать карточку слова в UI с переводом и примерами.",
            params = mapOf(
                "word"       to ("string" to "Немецкое слово"),
                "translation" to ("string" to "Перевод"),
                "example_de" to ("string" to "Пример предложения на немецком"),
                "example_ru" to ("string" to "Перевод примера"),
                "gender"     to ("string" to "Род: der/die/das"),
            ),
            required = listOf("word", "translation"),
        ),
        FunctionRegistry.declare(
            name = "show_grammar_hint",
            description = "Показать подсказку по грамматике в UI.",
            params = mapOf(
                "rule_title"  to ("string" to "Название правила"),
                "explanation" to ("string" to "Краткое объяснение"),
                "example"     to ("string" to "Пример"),
            ),
            required = listOf("rule_title", "explanation"),
        ),
        FunctionRegistry.declare(
            name = "trigger_celebration",
            description = "Запустить анимацию празднования в UI (достижение, пройден урок, серия).",
            params = mapOf(
                "type"    to ("string" to "Тип: achievement, lesson_complete, streak, level_up"),
                "message" to ("string" to "Текст поздравления"),
            ),
            required = listOf("type", "message"),
        ),
    )
}
