package com.voicedeutsch.master.domain.usecase.learning

import com.voicedeutsch.master.domain.model.knowledge.MistakeType

/**
 * Evaluates a user's answer against the expected answer.
 *
 * Architecture line 906 (EvaluateAnswerUseCase.kt).
 *
 * Note: Most evaluation is done by Gemini (voice interaction).
 * This use case handles offline/fallback evaluation for typed answers.
 */
class EvaluateAnswerUseCase {

    data class Params(
        val userAnswer: String,
        val expectedAnswer: String,
        val exerciseType: String = "translate",
    )

    data class Result(
        val isCorrect: Boolean,
        val quality: Int,             // 0-5 SM-2 quality
        val feedback: String,
        val mistakeType: MistakeType?,
    )

    operator fun invoke(params: Params): Result {
        val normalized = params.userAnswer.trim().lowercase()
        val expected = params.expectedAnswer.trim().lowercase()

        if (normalized == expected) {
            return Result(
                isCorrect = true,
                quality = 5,
                feedback = "Richtig! ✓",
                mistakeType = null,
            )
        }

        // Check for minor differences (punctuation, articles)
        val isClose = levenshteinDistance(normalized, expected) <= 2
        if (isClose) {
            return Result(
                isCorrect = true,
                quality = 4,
                feedback = "Fast richtig! Правильный ответ: ${params.expectedAnswer}",
                mistakeType = null,
            )
        }

        return Result(
            isCorrect = false,
            quality = 1,
            feedback = "Nicht ganz. Правильный ответ: ${params.expectedAnswer}",
            mistakeType = MistakeType.WORD,
        )
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[a.length][b.length]
    }
}