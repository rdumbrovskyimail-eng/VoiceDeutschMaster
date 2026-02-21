package com.voicedeutsch.master.domain.model.book

import kotlinx.serialization.Serializable

/**
 * Represents an exercise embedded within a lesson.
 * Exercises are extracted from lesson text by [EXERCISE] markers.
 *
 * Architecture line 881 (domain/model/book/Exercise.kt).
 */
@Serializable
data class Exercise(
    val id: String,
    val type: ExerciseType,
    val prompt: String,
    val expectedAnswer: String = "",
    val hints: List<String> = emptyList(),
    val difficulty: Int = 1,            // 1-5
    val relatedWordIds: List<String> = emptyList(),
    val relatedRuleIds: List<String> = emptyList(),
)

@Serializable
enum class ExerciseType {
    FILL_IN_BLANK,      // Вставить пропущенное слово
    TRANSLATE_TO_DE,    // Перевести на немецкий
    TRANSLATE_TO_RU,    // Перевести на русский
    CONJUGATE,          // Проспрягать глагол
    CHOOSE_ARTICLE,     // Выбрать артикль (der/die/das)
    WORD_ORDER,         // Восстановить порядок слов
    LISTEN_AND_REPEAT,  // Послушать и повторить
    FREE_RESPONSE,      // Свободный ответ
}