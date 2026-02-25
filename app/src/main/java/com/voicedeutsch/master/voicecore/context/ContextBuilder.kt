package com.voicedeutsch.master.voicecore.context

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.voicecore.functions.FunctionRegistry
import com.voicedeutsch.master.voicecore.functions.GeminiFunctionDeclaration
import com.voicedeutsch.master.voicecore.prompt.MasterPrompt
import com.voicedeutsch.master.voicecore.prompt.PromptOptimizer
import com.voicedeutsch.master.voicecore.prompt.PromptTemplates
import kotlinx.serialization.json.Json

/**
 * Assembles the complete context for a Gemini session.
 * Architecture lines 570-640 (ContextBuilder, context hierarchy).
 *
 * Token budget (Gemini 2.5 Flash Live, INPUT limit = 131 072 токена):
 *
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │  131 072  — суммарный лимит Live API (input)               │
 *   │  -  3 000  — function declarations (~16 функций)           │
 *   │  - 68 000  — буфер на историю разговора (~30 мин сессии)   │
 *   │  = 60 000  — бюджет системного контекста (Setup-сообщение) │
 *   └─────────────────────────────────────────────────────────────┘
 *
 * ⚠️ 131 072 — это НЕ лимит только Setup-сообщения.
 * История разговора, аудио-транскрипты и результаты function calls
 * тоже тратят этот бюджет в течение сессии.
 *
 * Output токены для Live API (AUDIO modality) — не нужно конфигурировать.
 * Аудио-стриминг не измеряется токенами как текст. maxOutputTokens игнорируется
 * при responseModalities=["AUDIO"].
 *
 * ════════════════════════════════════════════════════════════════════════════
 * ИЗМЕНЕНИЯ (Модуль 7):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   1. УДАЛЕНО: зависимость на FunctionRouter из конструктора.
 *      FunctionRouter.getDeclarations() удалён в Модуле 6.
 *      Декларации теперь берутся из FunctionRegistry.getAllDeclarations().
 *
 *   2. ИЗМЕНЕНО: SessionContext.functionDeclarations: List<String> → List<GeminiFunctionDeclaration>.
 *      Нативные объекты вместо JSON-строк. GeminiClient.mapToFirebaseDeclaration()
 *      маппит их в Firebase SDK объекты.
 *
 *   3. ИЗМЕНЕНО: totalEstimatedTokens() считает токены по нативным декларациям
 *      (name.length + description.length + parameters) вместо JSON-строк.
 *
 *   4. УДАЛЕНО: json из конструктора — больше не нужен (не сериализуем декларации).
 */
class ContextBuilder(
    private val userContextProvider: UserContextProvider,
    private val bookContextProvider: BookContextProvider,
    // functionRouter УДАЛЁН — декларации берутся из FunctionRegistry напрямую
    // json           УДАЛЁН — не нужен без JSON-сериализации деклараций
) {

    /**
     * Immutable snapshot of the full session context sent to Gemini at session start.
     *
     * [systemPrompt] — полный оптимизированный контекст:
     *   MasterPrompt + userContext + bookContext + strategyPrompt.
     *   GeminiClient кладёт это поле в systemInstruction.parts[0].text.
     *
     * [functionDeclarations] — нативные объекты GeminiFunctionDeclaration.
     *   GeminiClient.mapToFirebaseDeclaration() маппит их в Firebase FunctionDeclaration.
     */
    data class SessionContext(
        val systemPrompt: String,
        val userContext: String,
        val bookContext: String,
        val strategyPrompt: String,
        val functionDeclarations: List<GeminiFunctionDeclaration>,   // ✅ ИЗМЕНЕНО: было List<String>
    ) {
        /** fullContext = systemPrompt (уже объединены при сборке). */
        val fullContext: String get() = systemPrompt

        /**
         * Rough token estimate: 1 token ≈ 4 characters.
         * ✅ ИЗМЕНЕНО: считает по нативным декларациям, а не JSON-строкам.
         */
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
        // 1. Собираем все части контекста
        val staticSystemPrompt = MasterPrompt.build()
        val userContext        = userContextProvider.buildUserContext(knowledgeSnapshot)
        val bookContext        = bookContextProvider.buildBookContext(currentChapter, currentLesson)
        val strategyPrompt     = PromptTemplates.getStrategyPrompt(currentStrategy, knowledgeSnapshot)

        // 2. ✅ ИЗМЕНЕНО: Декларации функций — из FunctionRegistry (нативные объекты)
        //    Было: functionRouter.getDeclarations() → List<String> (JSON)
        //    Стало: FunctionRegistry.getAllDeclarations() → List<GeminiFunctionDeclaration>
        val functionDeclarations = FunctionRegistry.getAllDeclarations()

        // 3. Объединяем всё в один блок для systemInstruction
        val rawFullContext = buildString {
            append(staticSystemPrompt)
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

        // 4. Оптимизируем под бюджет 60 000 токенов
        //    (131 072 лимит − 68 000 буфер разговора − 3 000 функции = 60 000)
        val optimizedContext = PromptOptimizer.optimize(
            fullPrompt = rawFullContext,
            maxTokens  = SAFE_CONTEXT_TOKEN_BUDGET,
        )

        val sessionContext = SessionContext(
            systemPrompt         = optimizedContext,
            userContext           = userContext,
            bookContext           = bookContext,
            strategyPrompt       = strategyPrompt,
            functionDeclarations = functionDeclarations,
        )

        // 5. Диагностический лог
        val contextTokens  = optimizedContext.length / TOKEN_CHAR_RATIO
        // ✅ ИЗМЕНЕНО: оценка токенов деклараций по нативным полям
        val functionTokens = functionDeclarations.sumOf { decl ->
            decl.name.length + decl.description.length +
                (decl.parameters?.properties?.entries?.sumOf { (k, v) ->
                    k.length + v.type.length + v.description.length
                } ?: 0)
        } / TOKEN_CHAR_RATIO
        val totalTokens    = contextTokens + functionTokens
        val remainingForConversation = LIVE_API_TOTAL_TOKEN_LIMIT - totalTokens

        android.util.Log.d("ContextBuilder",
            "Контекст собран: системный ~$contextTokens токенов, " +
            "функции ~$functionTokens токенов (${functionDeclarations.size} деклараций), " +
            "итого ~$totalTokens / $LIVE_API_TOTAL_TOKEN_LIMIT. " +
            "Буфер на разговор: ~$remainingForConversation токенов.")

        if (totalTokens > WARN_TOKEN_THRESHOLD) {
            android.util.Log.w("ContextBuilder",
                "ВНИМАНИЕ: Setup занимает $totalTokens токенов. " +
                "На историю разговора остаётся только ~$remainingForConversation токенов. " +
                "Рассмотри сокращение UserContextProvider или BookContextProvider.")
        }

        return sessionContext
    }

    companion object {
        private const val TOKEN_CHAR_RATIO = 4

        /**
         * Суммарный input-лимит Gemini 2.5 Flash Live API.
         * Источник: ai.google.dev (февраль 2026).
         */
        const val LIVE_API_TOTAL_TOKEN_LIMIT = 131_072

        /**
         * Бюджет для системного контекста в Setup-сообщении:
         *   131 072 − 68 000 (буфер разговора) − 3 000 (функции) = 60 000
         */
        const val SAFE_CONTEXT_TOKEN_BUDGET = 60_000

        /**
         * Порог предупреждения — когда Setup занимает >85% от суммарного лимита.
         */
        private const val WARN_TOKEN_THRESHOLD = (LIVE_API_TOTAL_TOKEN_LIMIT * 0.85).toInt()
    }
}