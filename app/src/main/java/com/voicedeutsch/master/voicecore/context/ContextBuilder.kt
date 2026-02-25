package com.voicedeutsch.master.voicecore.context

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.voicecore.functions.FunctionRegistry
import com.voicedeutsch.master.voicecore.functions.GeminiFunctionDeclaration
import com.voicedeutsch.master.voicecore.prompt.MasterPrompt
import com.voicedeutsch.master.voicecore.prompt.PromptOptimizer
import com.voicedeutsch.master.voicecore.prompt.PromptTemplates

/**
 * Assembles the complete context for a Gemini session.
 * Updated: 2026-02-25 — Gemini 2.5 Flash Live 128K context
 *
 * Token budget (Gemini 2.5 Flash Live API — total input limit):
 *
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │  131 072  — суммарный лимит Live API (input)               │
 *   │  -   3 000  — function declarations (~16 функций)          │
 *   │  -  38 072  — буфер на историю разговора + function calls  │
 *   │  =  90 000  — SAFE_CONTEXT_TOKEN_BUDGET (системный промпт) │
 *   └─────────────────────────────────────────────────────────────┘
 */
class ContextBuilder(
    private val userContextProvider: UserContextProvider,
    private val bookContextProvider: BookContextProvider,
    // ✅ FIX (Баг #6): добавлен userRepository — нужен для инъекции пользовательских
    // настроек (strictness, pace) прямо в системный промпт Gemini.
    private val userRepository: UserRepository,
) {

    data class SessionContext(
        val systemPrompt: String,
        val userContext: String,
        val bookContext: String,
        val strategyPrompt: String,
        val functionDeclarations: List<GeminiFunctionDeclaration>,
    ) {
        val fullContext: String get() = systemPrompt

        fun totalEstimatedTokens(): Int {
            val promptTokens = systemPrompt.length / TOKEN_CHAR_RATIO
            val declTokens = functionDeclarations.sumOf { decl ->
                (decl.name.length + decl.description.length +
                    (decl.parameters?.properties?.entries?.sumOf { (k, v) ->
                        k.length + v.type.length + v.description.length
                    } ?: 0)) / TOKEN_CHAR_RATIO
            }
            return promptTokens + declTokens
        }
    }

    suspend fun buildSessionContext(
        userId: String,
        knowledgeSnapshot: KnowledgeSnapshot,
        currentStrategy: LearningStrategy,
        currentChapter: Int,
        currentLesson: Int,
    ): SessionContext {
        val staticSystemPrompt = MasterPrompt.build()
        val userContext        = userContextProvider.buildUserContext(knowledgeSnapshot)
        val bookContext        = bookContextProvider.buildBookContext(currentChapter, currentLesson)
        val strategyPrompt     = PromptTemplates.getStrategyPrompt(currentStrategy, knowledgeSnapshot)

        val functionDeclarations = FunctionRegistry.getAllDeclarations()

        // ✅ FIX (Баг #6): читаем UserPreferences и формируем settingsPrompt.
        // Это позволяет Gemini менять поведение (строгость, темп) без перезапуска сессии —
        // достаточно изменить настройки в UI, и при следующей сессии ИИ получит новые параметры.
        val profile = userRepository.getUserProfile(userId)
        val strictness = profile?.preferences?.pronunciationStrictness?.name ?: "MODERATE"
        val pace       = profile?.preferences?.learningPace?.name ?: "NORMAL"
        val voiceSpeed = profile?.voiceSettings?.germanVoiceSpeed ?: 0.8f

        val settingsPrompt = """
            НАСТРОЙКИ ПОЛЬЗОВАТЕЛЯ:
            - Строгость произношения: $strictness (если STRICT — придирайся к каждой фонеме, если LENIENT — игнорируй лёгкий акцент, если MODERATE — указывай на грубые ошибки).
            - Темп обучения: $pace (если SLOW — больше объяснений и повторений, если FAST — сразу переходи к следующей теме).
            - Скорость немецкой речи: $voiceSpeed (используй как ориентир при TTS-подборе темпа).
        """.trimIndent()

        val rawFullContext = buildString {
            append(staticSystemPrompt)
            appendLine()
            appendLine()
            appendLine("--- USER SETTINGS ---")
            appendLine()
            append(settingsPrompt)
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

        val optimizedContext = PromptOptimizer.optimize(
            fullPrompt = rawFullContext,
            maxTokens  = SAFE_CONTEXT_TOKEN_BUDGET,
        )

        val sessionContext = SessionContext(
            systemPrompt         = optimizedContext,
            userContext          = userContext,
            bookContext          = bookContext,
            strategyPrompt       = strategyPrompt,
            functionDeclarations = functionDeclarations,
        )

        // Диагностика
        val contextTokens = optimizedContext.length / TOKEN_CHAR_RATIO
        val functionTokens = functionDeclarations.sumOf { decl ->
            decl.name.length + decl.description.length +
                (decl.parameters?.properties?.entries?.sumOf { (k, v) ->
                    k.length + v.type.length + v.description.length
                } ?: 0)
        } / TOKEN_CHAR_RATIO

        val totalTokens = contextTokens + functionTokens
        val remainingForConversation = LIVE_API_TOTAL_TOKEN_LIMIT - totalTokens

        android.util.Log.d("ContextBuilder",
            "Контекст собран: системный ~$contextTokens ток., функции ~$functionTokens ток. " +
            "(всего ~$totalTokens / $LIVE_API_TOTAL_TOKEN_LIMIT). Буфер на историю: ~$remainingForConversation токенов.")

        if (totalTokens > WARN_TOKEN_THRESHOLD) {
            android.util.Log.w("ContextBuilder",
                "ВНИМАНИЕ: Setup занимает $totalTokens токенов (>85%). " +
                "На историю остаётся только ~$remainingForConversation токенов.")
        }

        return sessionContext
    }

    companion object {
        private const val TOKEN_CHAR_RATIO = 4

        /** Суммарный input-лимит Gemini 2.5 Flash Live API (128K) */
        const val LIVE_API_TOTAL_TOKEN_LIMIT = 131_072

        /**
         * Бюджет для системного контекста (Setup-сообщение).
         * Увеличен до 90_000 (25.02.2026) — оставляем ~41k на историю разговора.
         */
        const val SAFE_CONTEXT_TOKEN_BUDGET = 90_000

        /** Порог предупреждения (>85% от общего лимита) */
        private const val WARN_TOKEN_THRESHOLD = (LIVE_API_TOTAL_TOKEN_LIMIT * 0.85).toInt()
    }
}
