package com.voicedeutsch.master.domain.usecase.learning

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.book.Exercise
import com.voicedeutsch.master.domain.model.book.ExerciseType
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.util.generateUUID

/**
 * Selects the next exercise based on current strategy and knowledge state.
 *
 * Architecture line 905 (GetNextExerciseUseCase.kt).
 *
 * For LINEAR_BOOK: next exercise from current lesson.
 * For REPETITION: generate exercise from SRS-due word.
 * For GAP_FILLING: exercise targeting weak points.
 */
class GetNextExerciseUseCase(
    private val bookRepository: BookRepository,
    private val knowledgeRepository: KnowledgeRepository,
) {

    data class Params(
        val userId: String,
        val strategy: LearningStrategy,
        val excludeIds: List<String> = emptyList(),
    )

    suspend operator fun invoke(params: Params): Exercise? {
        return when (params.strategy) {
            LearningStrategy.REPETITION -> {
                val dueWords = knowledgeRepository.getWordsForRepetition(
                    params.userId, limit = 1
                )
                dueWords.firstOrNull()?.let { wk ->
                    val word = knowledgeRepository.getWord(wk.wordId)
                    Exercise(
                        id = generateUUID(),
                        type = ExerciseType.TRANSLATE_TO_DE,
                        prompt = word?.russian ?: "",
                        expectedAnswer = word?.german ?: "",
                        relatedWordIds = listOf(wk.wordId),
                    )
                }
            }
            LearningStrategy.LINEAR_BOOK -> {
                val progress = bookRepository.getBookProgress(params.userId)
                    ?: return null
                // Delegate to Gemini for exercise generation from lesson content
                null
            }
            else -> null // Most strategies use Gemini-generated exercises
        }
    }
}