package com.voicedeutsch.master.voicecore.functions

// ✅ ИСПРАВЛЕНО: удалён неверный импорт из com.voicedeutsch.master.data.remote.gemini.
// GeminiFunctionDeclaration находится в том же пакете (voicecore.functions).

/**
 * Function declarations for knowledge-base operations.
 * Architecture line 832 (KnowledgeFunctions.kt).
 */
object KnowledgeFunctions {

    val declarations: List<GeminiFunctionDeclaration> = listOf(
        FunctionRegistry.declare(
            name = "save_word_knowledge",
            description = "Сохранить оценку знания слова пользователем. Вызывай после каждого взаимодействия со словом.",
            params = mapOf(
                "word"               to ("string"  to "Немецкое слово"),
                "translation"        to ("string"  to "Перевод на русский"),
                "level"              to ("integer" to "Уровень знания 0-5"),
                "quality"            to ("integer" to "Качество ответа 0-5 (SM-2)"),
                "pronunciation_score" to ("number" to "Оценка произношения 0.0-1.0"),
                "context"            to ("string"  to "Контекст использования"),
            ),
            required = listOf("word", "translation", "level", "quality"),
        ),
        FunctionRegistry.declare(
            name = "save_rule_knowledge",
            description = "Сохранить оценку знания грамматического правила.",
            params = mapOf(
                "rule_id" to ("string"  to "ID правила"),
                "level"   to ("integer" to "Уровень знания 0-5"),
                "quality" to ("integer" to "Качество ответа 0-5 (SM-2)"),
            ),
            required = listOf("rule_id", "quality"),
        ),
        FunctionRegistry.declare(
            name = "record_mistake",
            description = "Зафиксировать ошибку пользователя для анализа слабых мест.",
            params = mapOf(
                // ✅ FIX (Баг #5): явный enum в описании — ИИ не выдумывает
                // произвольные типы ошибок (напр. "syntax", "spelling").
                // Строгое перечисление снижает вероятность галлюцинации значения.
                "mistake_type" to ("string" to "Тип ошибки. Строго одно из значений: grammar, vocabulary, pronunciation, phrase"),
                "user_input"   to ("string" to "Что сказал/написал пользователь"),
                "correct_form" to ("string" to "Правильный вариант"),
                "explanation"  to ("string" to "Объяснение ошибки"),
                "context"      to ("string" to "Контекст"),
            ),
            required = listOf("mistake_type", "user_input", "correct_form"),
        ),
        FunctionRegistry.declare(
            name = "get_words_for_repetition",
            description = "Получить список слов для повторения по SRS. Вызывай при стратегии REPETITION.",
            params = mapOf(
                "limit" to ("integer" to "Максимальное количество слов (по умолчанию 10)"),
            ),
        ),
        FunctionRegistry.declare(
            name = "get_weak_points",
            description = "Получить слабые места пользователя для GAP_FILLING стратегии.",
        ),
    )
}
