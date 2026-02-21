package com.voicedeutsch.master.voicecore.context

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.voicecore.prompt.MasterPrompt
import com.voicedeutsch.master.voicecore.prompt.PromptTemplates

/**
 * Assembles the full system prompt from components:
 *   1. MasterPrompt (identity, rules, function specs)
 *   2. UserContext (knowledge snapshot)
 *   3. BookContext (current lesson)
 *   4. StrategyPrompt (current strategy instructions)
 *
 * Architecture line 825 (SystemPromptBuilder.kt).
 *
 * Token budget: ~3000-5000 tokens total system prompt.
 */
class SystemPromptBuilder(
    private val userContextProvider: UserContextProvider,
    private val bookContextProvider: BookContextProvider,
) {

    /**
     * Builds the complete system prompt for a session.
     *
     * @param snapshot User's current knowledge state.
     * @param strategy Selected learning strategy.
     * @param userId User ID for book context.
     * @return Full system prompt string.
     */
    suspend fun build(
        snapshot: KnowledgeSnapshot,
        strategy: LearningStrategy,
        userId: String,
    ): String = buildString {
        // 1. Core identity and rules (~1500 tokens)
        appendLine(MasterPrompt.build())
        appendLine()

        // 2. User knowledge context (~800 tokens)
        appendLine(userContextProvider.buildUserContext(snapshot))
        appendLine()

        // 3. Book context — current lesson (~500 tokens)
        val bookContext = bookContextProvider.buildBookContext(userId)
        if (bookContext.isNotEmpty()) {
            appendLine(bookContext)
            appendLine()
        }

        // 4. Strategy-specific instructions (~500 tokens)
        appendLine(PromptTemplates.getStrategyPrompt(strategy, snapshot))
    }

    /**
     * Estimates token count for the assembled prompt.
     * Rule of thumb: ~4 chars per token for mixed RU/DE text.
     */
    fun estimateTokens(prompt: String): Int = prompt.length / 4
}