package com.voicedeutsch.master.domain.model

import kotlinx.serialization.Serializable

/**
 * The 8+1 learning strategies available in Voice Deutsch Master.
 *
 * The `StrategySelector` chooses a strategy before and during each session
 * based on the user's [KnowledgeSnapshot].
 *
 * **Selection priority:**
 * 1. SRS queue > 10 items → [REPETITION]
 * 2. Weak points > 5 → [GAP_FILLING]
 * 3. Skill gap > 2 sub-levels → corresponding strategy
 * 4. Pronunciation not practiced > 3 days → [PRONUNCIATION]
 * 5. Default → [LINEAR_BOOK]
 *
 * **Mid-session switches** (after ~25 min or error rate > 60%):
 * - Can switch to a lighter/different strategy to maintain engagement
 */
@Serializable
enum class LearningStrategy(
    val displayNameRu: String,
    val description: String
) {
    LINEAR_BOOK(
        "Прохождение книги",
        "Последовательное прохождение материала книги"
    ),
    GAP_FILLING(
        "Заполнение пробелов",
        "Возврат к темам, где обнаружены пробелы"
    ),
    REPETITION(
        "Повторение",
        "Интервальное повторение накопленного материала"
    ),
    FREE_PRACTICE(
        "Свободная практика",
        "Свободный разговор на немецком с коррекцией"
    ),
    PRONUNCIATION(
        "Произношение",
        "Целенаправленная работа над произношением"
    ),
    GRAMMAR_DRILL(
        "Грамматический штурм",
        "Интенсивная проработка грамматического правила"
    ),
    VOCABULARY_BOOST(
        "Словарный бросок",
        "Интенсивное расширение словарного запаса"
    ),
    LISTENING(
        "Аудирование",
        "Тренировка понимания немецкой речи на слух"
    ),
    ASSESSMENT(
        "Оценка уровня",
        "Определение текущего уровня пользователя"
    );

    companion object {
        /**
         * Parses a [LearningStrategy] from its [name] string.
         * Defaults to [LINEAR_BOOK] when not found.
         */
        fun fromString(value: String): LearningStrategy =
            entries.find { it.name == value } ?: LINEAR_BOOK
    }
}