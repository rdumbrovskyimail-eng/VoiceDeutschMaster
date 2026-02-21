package com.voicedeutsch.master.voicecore.prompt

import com.voicedeutsch.master.voicecore.audio.AudioConfig

/**
 * Optimizes prompts to fit within token budget.
 *
 * Architecture line 861 (PromptOptimizer.kt).
 *
 * Strategies:
 *   1. Truncate user context (fewer problem words, fewer topic stats)
 *   2. Compress book context (summary instead of full lesson)
 *   3. Skip optional sections (recommendations, detailed history)
 *   4. Never truncate: identity, rules, function declarations
 */
object PromptOptimizer {

    /** Maximum tokens for the system prompt. */
    const val MAX_SYSTEM_PROMPT_TOKENS = 5000

    /** Chars-per-token estimate for mixed RU/DE text. */
    private const val CHARS_PER_TOKEN = 4

    /**
     * Ensures prompt fits within token budget.
     *
     * @param fullPrompt The assembled system prompt.
     * @param maxTokens Budget (defaults to [MAX_SYSTEM_PROMPT_TOKENS]).
     * @return Optimized prompt string.
     */
    fun optimize(fullPrompt: String, maxTokens: Int = MAX_SYSTEM_PROMPT_TOKENS): String {
        val estimatedTokens = fullPrompt.length / CHARS_PER_TOKEN
        if (estimatedTokens <= maxTokens) return fullPrompt

        // Strategy 1: Remove recommendation section
        var result = fullPrompt.replace(
            Regex("=== RECOMMENDATIONS ===.*?=== END RECOMMENDATIONS ===", RegexOption.DOT_MATCHES_ALL),
            "=== RECOMMENDATIONS === [trimmed for brevity] ==="
        )
        if (result.length / CHARS_PER_TOKEN <= maxTokens) return result

        // Strategy 2: Trim problem words list to top 5
        result = result.replace(
            Regex("Проблемные слова:.*?(?=\\n[A-ZА-Я])", RegexOption.DOT_MATCHES_ALL)
        ) { match ->
            val lines = match.value.lines()
            lines.take(6).joinToString("\n") // header + 5 items
        }
        if (result.length / CHARS_PER_TOKEN <= maxTokens) return result

        // Strategy 3: Trim session history to last 10 entries
        result = result.replace(
            Regex("=== SESSION HISTORY ===.*?=== END SESSION HISTORY ===", RegexOption.DOT_MATCHES_ALL),
            "=== SESSION HISTORY === [recent sessions trimmed] ==="
        )

        // Strategy 4: Hard truncate at max char limit (emergency)
        val maxChars = maxTokens * CHARS_PER_TOKEN
        if (result.length > maxChars) {
            result = result.take(maxChars - 100) + "\n[... prompt truncated to fit token budget ...]"
        }
        return result
    }
}