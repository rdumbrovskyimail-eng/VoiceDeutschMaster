package com.voicedeutsch.master.voicecore.context

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.voicecore.functions.FunctionRouter
import com.voicedeutsch.master.voicecore.prompt.MasterPrompt
import com.voicedeutsch.master.voicecore.prompt.PromptTemplates
import kotlinx.serialization.json.Json

/**
 * Assembles the complete context for a Gemini session.
 * Architecture lines 570-640 (ContextBuilder, context hierarchy).
 *
 * Context token budget (approximate):
 *   1. System Prompt       — ~5 000 tokens
 *   2. User Context        — ~10 000 tokens
 *   3. Book Context        — ~5 000 tokens
 *   4. Strategy Prompt     — ~2 000 tokens
 *   5. Function Declarations — ~3 000 tokens
 *   6. Session History     — grows during session
 *
 * Total budget: 32 768 tokens (Gemini Live API — жёсткий лимит!).
 */
class ContextBuilder(
    private val userContextProvider: UserContextProvider,
    private val bookContextProvider: BookContextProvider,
    private val functionRouter: FunctionRouter,
    @Suppress("UnusedPrivateMember")
    private val json: Json, // reserved for future incremental serialisation
) {

    /**
     * Immutable snapshot of the full session context sent to Gemini at session start.
     *
     * [functionDeclarations] — List<String> где каждый элемент валидный JSON
     * объект functionDeclaration. Передаётся напрямую в GeminiClient.sendSetup()
     * как tools[0].functionDeclarations согласно спецификации BidiGenerateContentSetup.
     *
     * [fullContext] assembles all parts in priority order для systemInstruction.
     */
    data class SessionContext(
        val systemPrompt: String,
        val userContext: String,
        val bookContext: String,
        val strategyPrompt: String,
        val functionDeclarations: List<String>, // ← List<String>, не String
    ) {
        /** Combined context в порядке который Gemini должен обработать.
         *  Идёт в systemInstruction.parts[0].text при Setup. */
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
        fun totalEstimatedTokens(functionDeclarations: List<String>): Int =
            (fullContext.length + functionDeclarations.sumOf { it.length }) / TOKEN_CHAR_RATIO
    }

    suspend fun buildSessionContext(
        userId: String,
        knowledgeSnapshot: KnowledgeSnapshot,
        currentStrategy: LearningStrategy,
        currentChapter: Int,
        currentLesson: Int,
    ): SessionContext {
        // MasterPrompt — pure object, no dependencies needed
        val systemPrompt   = MasterPrompt.build()
        val userContext    = userContextProvider.buildUserContext(knowledgeSnapshot)
        val bookContext    = bookContextProvider.buildBookContext(currentChapter, currentLesson)
        val strategyPrompt = PromptTemplates.getStrategyPrompt(currentStrategy, knowledgeSnapshot)

        // Декларации функций берём из FunctionRouter — единственный источник правды.
        // PromptTemplates.getFunctionDeclarationsJson() больше не используется
        // во избежание рассинхронизации между декларациями и хендлерами.
        val functionDeclarations = functionRouter.getDeclarations()

        val sessionContext = SessionContext(
            systemPrompt         = systemPrompt,
            userContext          = userContext,
            bookContext          = bookContext,
            strategyPrompt       = strategyPrompt,
            functionDeclarations = functionDeclarations,
        )

        val totalTokens = sessionContext.totalEstimatedTokens(functionDeclarations)
        if (totalTokens > 28_000) {
            android.util.Log.w("ContextBuilder",
                "ВНИМАНИЕ: контекст $totalTokens токенов из 32768. " +
                "Осталось ${32_768 - totalTokens} токенов на историю сессии.")
        }

        return sessionContext
    }

    companion object {
        private const val TOKEN_CHAR_RATIO = 4
    }
}
