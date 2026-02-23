package com.voicedeutsch.master.voicecore.prompt

/**
 * Optimizes the full session context to fit within the Live API token budget.
 *
 * Architecture line 861 (PromptOptimizer.kt).
 *
 * Token budget breakdown (total Live API input limit = 131 072 tokens):
 *   - System context (prompt + user + book + strategy) : up to 60 000 tokens  ← оптимизируем здесь
 *   - Function declarations                             : ~3 000 tokens
 *   - Live conversation history (накапливается)        : ~68 000 токенов
 *
 * ⚠️ Важно: 131 072 — это СУММАРНЫЙ лимит за всю сессию, а НЕ только для Setup-сообщения.
 * История разговора, аудио-транскрипты и результаты function calls тоже тратят этот бюджет.
 * Поэтому системный промпт не должен занимать более 50-60k токенов.
 *
 * Optimization strategies (applied in priority order):
 *   1. Remove RECOMMENDATIONS section (most expendable)
 *   2. Truncate PROBLEM_WORDS to top-6 entries
 *   3. Remove SESSION_HISTORY section
 *   4. Hard truncate as last resort
 *
 * Never truncated: identity, rules, function declarations, book context.
 */
object PromptOptimizer {

    /**
     * Максимальный бюджет системного контекста (в токенах).
     *
     * Рассчёт:
     *   131 072 (лимит модели)
     *   - 68 000 (буфер на историю разговора ~30 мин сессии)
     *   -  3 000 (function declarations)
     *   =  60 072 → округляем до 60 000
     *
     * Было: MAX_SYSTEM_PROMPT_TOKENS = 5 000 (слишком мало, вводило в заблуждение)
     */
    const val MAX_CONTEXT_TOKENS = 60_000

    /** Chars-per-token estimate for mixed RU/DE text. */
    private const val CHARS_PER_TOKEN = 4

    private val RECOMMENDATIONS_REGEX = Regex(
        "=== RECOMMENDATIONS ===.*?=== END RECOMMENDATIONS ===",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val PROBLEM_WORDS_REGEX = Regex(
        "Проблемные слова:.*?(?=\\n[A-ZА-Я])",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val SESSION_HISTORY_REGEX = Regex(
        "=== SESSION HISTORY ===.*?=== END SESSION HISTORY ===",
        RegexOption.DOT_MATCHES_ALL,
    )

    /**
     * Ensures the full session context fits within [maxTokens].
     *
     * @param fullPrompt  Assembled full context from ContextBuilder.
     * @param maxTokens   Token budget. Defaults to [MAX_CONTEXT_TOKENS] (60 000).
     * @return            Optimized context string, never exceeding maxTokens.
     */
    fun optimize(fullPrompt: String, maxTokens: Int = MAX_CONTEXT_TOKENS): String {
        val estimatedTokens = fullPrompt.length / CHARS_PER_TOKEN

        // Быстрый путь: контекст влезает без оптимизации
        if (estimatedTokens <= maxTokens) return fullPrompt

        android.util.Log.w(
            "PromptOptimizer",
            "Контекст $estimatedTokens токенов превышает бюджет $maxTokens. Применяем оптимизацию.",
        )

        return try {
            // Шаг 1: убрать секцию рекомендаций
            var result = RECOMMENDATIONS_REGEX.replace(
                fullPrompt,
                "=== RECOMMENDATIONS === [trimmed for brevity] ===",
            )
            if (result.length / CHARS_PER_TOKEN <= maxTokens) {
                android.util.Log.d("PromptOptimizer", "Оптимизация: убраны RECOMMENDATIONS")
                return result
            }

            // Шаг 2: обрезать список проблемных слов до топ-6
            result = PROBLEM_WORDS_REGEX.replace(result) { match ->
                match.value.lines().take(6).joinToString("\n")
            }
            if (result.length / CHARS_PER_TOKEN <= maxTokens) {
                android.util.Log.d("PromptOptimizer", "Оптимизация: обрезаны PROBLEM_WORDS до 6")
                return result
            }

            // Шаг 3: убрать историю сессий
            result = SESSION_HISTORY_REGEX.replace(
                result,
                "=== SESSION HISTORY === [recent sessions trimmed] ===",
            )
            if (result.length / CHARS_PER_TOKEN <= maxTokens) {
                android.util.Log.d("PromptOptimizer", "Оптимизация: убрана SESSION_HISTORY")
                return result
            }

            // Шаг 4: жёсткое обрезание (крайний случай)
            val maxChars = maxTokens * CHARS_PER_TOKEN
            android.util.Log.e(
                "PromptOptimizer",
                "ВНИМАНИЕ: жёсткое обрезание контекста до $maxTokens токенов. " +
                "Проверь объём UserContextProvider и BookContextProvider.",
            )
            result.take(maxChars - 100) + "\n[... prompt truncated to fit token budget ...]"

        } catch (e: Exception) {
            android.util.Log.e("PromptOptimizer", "Ошибка оптимизации: ${e.message}")
            val maxChars = maxTokens * CHARS_PER_TOKEN
            if (fullPrompt.length > maxChars) {
                fullPrompt.take(maxChars - 100) + "\n[... truncated ...]"
            } else {
                fullPrompt
            }
        }
    }
}
