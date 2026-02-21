package com.voicedeutsch.master.domain.usecase.speech

import com.voicedeutsch.master.domain.model.speech.PhoneticTarget
import com.voicedeutsch.master.domain.repository.KnowledgeRepository

/**
 * Returns prioritized list of sounds and words for pronunciation practice.
 * Architecture line 923 (GetPronunciationTargetsUseCase.kt).
 */
class GetPronunciationTargetsUseCase(
    private val knowledgeRepository: KnowledgeRepository,
    private val analyzePronunciation: AnalyzePronunciationUseCase,
) {

    data class PronunciationPlan(
        val targets: List<PhoneticTarget>,
        val practiceWords: List<PracticeWord>,
    )

    data class PracticeWord(
        val german: String,
        val targetSound: String,
        val currentScore: Float,
    )

    suspend operator fun invoke(userId: String): PronunciationPlan {
        val targets = analyzePronunciation(userId).take(5)

        val practiceWords = targets.flatMap { target ->
            target.inWords.take(3).map { word ->
                PracticeWord(
                    german = word,
                    targetSound = target.sound,
                    currentScore = target.currentScore,
                )
            }
        }

        return PronunciationPlan(
            targets = targets,
            practiceWords = practiceWords,
        )
    }
}