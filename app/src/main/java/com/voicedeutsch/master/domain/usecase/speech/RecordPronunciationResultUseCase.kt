package com.voicedeutsch.master.domain.usecase.speech

import com.voicedeutsch.master.domain.model.speech.PronunciationResult
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.util.DateUtils
import com.voicedeutsch.master.util.generateUUID

/**
 * Records a pronunciation evaluation result.
 * Called via Function Call: save_pronunciation_result() from Gemini.
 *
 * Also updates the WordKnowledge pronunciation score using a rolling average.
 */
class RecordPronunciationResultUseCase(
    private val knowledgeRepository: KnowledgeRepository
) {

    data class Params(
        val userId: String,
        val word: String,
        val score: Float,
        val problemSounds: List<String> = emptyList(),
        val sessionId: String? = null,
        val attemptNumber: Int = 1
    )

    suspend operator fun invoke(params: Params) {
        val now = DateUtils.nowTimestamp()

        val result = PronunciationResult(
            id = generateUUID(),
            userId = params.userId,
            word = params.word,
            score = params.score.coerceIn(0f, 1f),
            problemSounds = params.problemSounds,
            attemptNumber = params.attemptNumber,
            sessionId = params.sessionId,
            timestamp = now,
            createdAt = now
        )

        knowledgeRepository.savePronunciationResult(result)

        updateWordPronunciationScore(params.userId, params.word, params.score)
    }

    private suspend fun updateWordPronunciationScore(
        userId: String,
        word: String,
        newScore: Float
    ) {
        val existingKnowledge = knowledgeRepository.getWordKnowledgeByGerman(userId, word)
            ?: return

        val totalAttempts = existingKnowledge.pronunciationAttempts + 1
        val prevTotal = existingKnowledge.pronunciationScore * existingKnowledge.pronunciationAttempts
        val updatedScore = if (totalAttempts > 0) {
            (prevTotal + newScore) / totalAttempts
        } else {
            newScore
        }

        val updated = existingKnowledge.copy(
            pronunciationScore = updatedScore,
            pronunciationAttempts = totalAttempts,
            updatedAt = DateUtils.nowTimestamp()
        )

        knowledgeRepository.upsertWordKnowledge(updated)
    }
}