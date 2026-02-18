package com.voicedeutsch.master.voicecore.context

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.voicecore.prompt.PromptTemplates
import kotlinx.serialization.json.Json

/**
 * Assembles the complete context for a Gemini session.
 * Architecture lines 570-640 (ContextBuilder, context hierarchy).
 *
 * Context token budget (approximate):
 *   1. System Prompt  — ~5 000 tokens
 *   2. User Context   — ~10 000 tokens
 *   3. Book Context   — ~5 000 tokens
 *   4. Strategy Prompt — ~2 000 tokens
 *   5. Session History — grows during session
 *
 * Total budget: 2 000 000 tokens (Gemini Live).
 */
class ContextBuilder(
    private val systemPromptBuilder: SystemPromptBuilder,
    private val userContextProvider: UserContextProvider,
    private val bookContextProvider: BookContextProvider,
    @Suppress("UnusedPrivateMember")
    private val json: Json, // reserved for future incremental serialisation
) {

    /**
     * Immutable snapshot of the full session context sent to Gemini at session start.
     * [fullContext] assembles all parts in priority order.
     */
    data class SessionContext(
        val systemPrompt: String,
        val userContext: String,
        val bookContext: String,
        val strategyPrompt: String,
        val functionDeclarations: String,
    ) {
        /** Combined context in the order Gemini should process it. */
        val fullContext: String
            get() = buildString {
                append(systemPrompt)
                appendLine()
                appendLine()
                appendLine("--- USER CONTEXT ---")
                appendLine()
                append(userContext)
                appendLine()
                appendLine("--- BOOK CONTEXT ---")
                appendLine()
                append(bookContext)
                appendLine()
                appendLine("--- CURRENT STRATEGY ---")
                appendLine()
                append(strategyPrompt)
            }

        /** Rough token estimate: 1 token ≈ 4 characters. */
        val estimatedTokens: Int get() = fullContext.length / TOKEN_CHAR_RATIO
    }

    suspend fun buildSessionContext(
        userId: String,
        knowledgeSnapshot: KnowledgeSnapshot,
        currentStrategy: LearningStrategy,
        currentChapter: Int,
        currentLesson: Int,
    ): SessionContext {
        val systemPrompt = systemPromptBuilder.build()
        val userContext = userContextProvider.buildUserContext(knowledgeSnapshot)
        val bookContext = bookContextProvider.buildBookContext(currentChapter, currentLesson)
        val strategyPrompt = PromptTemplates.getStrategyPrompt(currentStrategy, knowledgeSnapshot)
        val functionDeclarations = PromptTemplates.getFunctionDeclarationsJson()

        return SessionContext(
            systemPrompt = systemPrompt,
            userContext = userContext,
            bookContext = bookContext,
            strategyPrompt = strategyPrompt,
            functionDeclarations = functionDeclarations,
        )
    }

    companion object {
        private const val TOKEN_CHAR_RATIO = 4
    }
}