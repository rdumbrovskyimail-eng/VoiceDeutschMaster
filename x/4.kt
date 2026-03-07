// Путь: src/test/java/com/voicedeutsch/master/voicecore/prompt/PromptOptimizerTest.kt
package com.voicedeutsch.master.voicecore.prompt

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PromptOptimizerTest {

    // ── Setup / teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.d(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns a prompt that is guaranteed to exceed the given token budget.
     * With CHARS_PER_TOKEN = 4: chars needed > maxTokens * 4.
     */
    private fun oversizedPrompt(maxTokens: Int, extraChars: Int = 100): String =
        "X".repeat(maxTokens * 4 + extraChars)

    /**
     * Returns a prompt that fits within the given token budget.
     */
    private fun fittingPrompt(maxTokens: Int): String =
        "A".repeat(maxTokens * 4 - 4)

    private fun wrapInRecommendations(inner: String) =
        "=== RECOMMENDATIONS ===\n$inner\n=== END RECOMMENDATIONS ==="

    private fun wrapInSessionHistory(inner: String) =
        "=== SESSION HISTORY ===\n$inner\n=== END SESSION HISTORY ==="

    private fun buildProblemWordsSection(lineCount: Int): String =
        "Проблемные слова:\n" + (1..lineCount).joinToString("\n") { "- Word $it" }

    // ── MAX_CONTEXT_TOKENS constant ───────────────────────────────────────────

    @Test
    fun maxContextTokens_is60000() {
        assertEquals(60_000, PromptOptimizer.MAX_CONTEXT_TOKENS)
    }

    // ── optimize: fast path ───────────────────────────────────────────────────

    @Test
    fun optimize_promptFitsWithinBudget_returnedUnchanged() {
        val prompt = fittingPrompt(maxTokens = 100)
        val result = PromptOptimizer.optimize(prompt, maxTokens = 100)
        assertEquals(prompt, result)
    }

    @Test
    fun optimize_promptExactlyAtBudget_returnedUnchanged() {
        // estimatedTokens = length / 4 = maxTokens * 4 / 4 = maxTokens → fits
        val prompt = "Z".repeat(100 * 4)
        val result = PromptOptimizer.optimize(prompt, maxTokens = 100)
        assertEquals(prompt, result)
    }

    @Test
    fun optimize_emptyPrompt_returnedUnchanged() {
        val result = PromptOptimizer.optimize("", maxTokens = 100)
        assertEquals("", result)
    }

    // ── optimize: step 1 — remove RECOMMENDATIONS ─────────────────────────────

    @Test
    fun optimize_step1_removesRecommendationsSection() {
        val recommendations = wrapInRecommendations("Recommendation 1\nRecommendation 2")
        // padding that alone fits, but combined with recommendations exceeds budget
        val maxTokens = 50
        val padding = "P".repeat(maxTokens * 4 - 4)
        val fullPrompt = padding + "\n" + recommendations

        assertTrue(fullPrompt.length / 4 > maxTokens)

        val result = PromptOptimizer.optimize(fullPrompt, maxTokens)

        assertFalse(result.contains("Recommendation 1"))
        assertTrue(result.contains("[trimmed for brevity]"))
    }

    @Test
    fun optimize_step1_resultFitsAfterRemovingRecommendations() {
        val maxTokens = 50
        val padding = "P".repeat(maxTokens * 4 - 4)
        val recommendations = wrapInRecommendations("A".repeat(40))
        val fullPrompt = padding + "\n" + recommendations

        val result = PromptOptimizer.optimize(fullPrompt, maxTokens)

        assertTrue(result.length / 4 <= maxTokens)
    }

    @Test
    fun optimize_step1_preservesContentOutsideRecommendations() {
        val maxTokens = 50
        val importantContent = "IDENTITY_RULES_PRESERVED"
        val padding = "P".repeat(maxTokens * 4 - importantContent.length - 4)
        val recommendations = wrapInRecommendations("A".repeat(40))
        val fullPrompt = importantContent + padding + "\n" + recommendations

        val result = PromptOptimizer.optimize(fullPrompt, maxTokens)

        assertTrue(result.contains(importantContent))
    }

    // ── optimize: step 2 — truncate problem words ─────────────────────────────

    @Test
    fun optimize_step2_truncatesProblemWordsToTop6() {
        val maxTokens = 80
        // Build a prompt where even after removing recommendations it still exceeds budget
        val problemWords = buildProblemWordsSection(lineCount = 20)
        // Suffix starting with uppercase to satisfy lookahead in PROBLEM_WORDS_REGEX
        val suffix = "\nNext Section Content"
        // Pad to exceed budget even after step 1 (no recommendations present)
        val padding = "P".repeat(maxTokens * 4 - problemWords.length - suffix.length + 50)
        val fullPrompt = padding + "\n" + problemWords + suffix

        assertTrue(fullPrompt.length / 4 > maxTokens)

        val result = PromptOptimizer.optimize(fullPrompt, maxTokens)

        val linesBetween = result
            .substringAfter("Проблемные слова:", "")
            .lines()
            .take(20)
        // After truncation to 6 lines, lines 7+ (Word 7 ... Word 20) should be gone
        assertFalse(result.contains("Word 10"))
    }

    @Test
    fun optimize_step2_keepsAtMostSixLines() {
        val maxTokens = 80
        val problemWords = buildProblemWordsSection(lineCount = 20)
        val suffix = "\nSOME_NEXT_SECTION"
        val padding = "P".repeat(maxTokens * 4 - problemWords.length - suffix.length + 100)
        val fullPrompt = padding + "\n" + problemWords + suffix

        val result = PromptOptimizer.optimize(fullPrompt, maxTokens)

        val problemSection = result.substringAfter("Проблемные слова:", "")
        val wordLines = problemSection.lines().filter { it.contains("Word ") }
        assertTrue(wordLines.size <= 6)
    }

    // ── optimize: step 3 — remove SESSION_HISTORY ────────────────────────────

    @Test
    fun optimize_step3_removesSessionHistorySection() {
        val maxTokens = 100
        val history = wrapInSessionHistory("Session 1 data\nSession 2 data\nSession 3 data")
        val problemWords = buildProblemWordsSection(lineCount = 20)
        val suffix = "\nNEXT_SECTION"
        // Large enough that steps 1 and 2 alone don't resolve it
        val padding = "P".repeat(maxTokens * 4 - 4)
        val fullPrompt = padding + "\n" + problemWords + suffix + "\n" + history

        val result = PromptOptimizer.optimize(fullPrompt, maxTokens)

        assertFalse(result.contains("Session 1 data"))
        assertTrue(result.contains("[recent sessions trimmed]"))
    }

    @Test
    fun optimize_step3_resultFitsAfterRemovingSessionHistory() {
        val maxTokens = 100
        val history = wrapInSessionHistory("S".repeat(300))
        val padding = "P".repeat(maxTokens * 4 - 4)
        val fullPrompt = padding + "\n" + history

        assertTrue(fullPrompt.length / 4 > maxTokens)

        val result = PromptOptimizer.optimize(fullPrompt, maxTokens)

        assertTrue(result.length / 4 <= maxTokens)
    }

    // ── optimize: step 4 — hard truncate ─────────────────────────────────────

    @Test
    fun optimize_step4_hardTruncate_resultFitsWithinBudget() {
        // A massive prompt with no sections to remove → falls through to hard truncate
        val maxTokens = 50
        val massivePrompt = "B".repeat(maxTokens * 4 * 10)

        val result = PromptOptimizer.optimize(massivePrompt, maxTokens)

        assertTrue(result.length / 4 <= maxTokens)
    }

    @Test
    fun optimize_step4_hardTruncate_appendsTruncationMarker() {
        val maxTokens = 50
        val massivePrompt = "B".repeat(maxTokens * 4 * 10)

        val result = PromptOptimizer.optimize(massivePrompt, maxTokens)

        assertTrue(result.contains("[... prompt truncated to fit token budget ...]"))
    }

    // ── optimize: priority order (step 1 before step 2 before step 3) ────────

    @Test
    fun optimize_step1AppliedFirst_noUnnecessaryStep2() {
        val maxTokens = 50
        val padding = "P".repeat(maxTokens * 4 - 4)
        // Only recommendations makes it exceed; problem words section absent
        val recommendations = wrapInRecommendations("A".repeat(40))
        val fullPrompt = padding + "\n" + recommendations

        val result = PromptOptimizer.optimize(fullPrompt, maxTokens)

        // Step 2 not needed, no session history marker
        assertFalse(result.contains("[recent sessions trimmed]"))
        assertTrue(result.contains("[trimmed for brevity]"))
    }

    @Test
    fun optimize_multipleRegionsPresent_step1TriedBeforeStep3() {
        val maxTokens = 80
        val recommendations = wrapInRecommendations("R".repeat(200))
        val history = wrapInSessionHistory("H".repeat(200))
        val padding = "P".repeat(maxTokens * 4 - 4)
        val fullPrompt = padding + "\n" + recommendations + "\n" + history

        val result = PromptOptimizer.optimize(fullPrompt, maxTokens)

        // Step 1 alone should resolve it if it fits after removing recommendations
        // Either way, recommendations marker should be present
        assertTrue(result.contains("[trimmed for brevity]"))
    }

    // ── optimize: result is always a String ──────────────────────────────────

    @Test
    fun optimize_alwaysReturnsString() {
        val result = PromptOptimizer.optimize(oversizedPrompt(maxTokens = 10), maxTokens = 10)
        assertNotNull(result)
    }

    @Test
    fun optimize_resultIsNeverLongerThanBudgetPlusMarker() {
        val maxTokens = 50
        val maxChars = maxTokens * 4
        val result = PromptOptimizer.optimize(oversizedPrompt(maxTokens, extraChars = 10_000), maxTokens)
        // Hard truncate path: take(maxChars - 100) + marker (~50 chars)
        assertTrue(result.length <= maxChars + 100)
    }

    // ── optimize: default maxTokens uses MAX_CONTEXT_TOKENS ──────────────────

    @Test
    fun optimize_defaultMaxTokens_fittingPrompt_returnedUnchanged() {
        val prompt = "A".repeat(100)
        val result = PromptOptimizer.optimize(prompt)
        assertEquals(prompt, result)
    }

    // ── optimize: exception fallback ─────────────────────────────────────────

    @Test
    fun optimize_resultNeverThrowsException() {
        // Verify that no exception escapes optimize() for arbitrary input
        assertDoesNotThrow {
            PromptOptimizer.optimize("test prompt", maxTokens = 1)
        }
    }

    @Test
    fun optimize_extremelySmallBudget_stillReturnsString() {
        val result = PromptOptimizer.optimize("Hello World Prompt", maxTokens = 1)
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }
}
