package com.voicedeutsch.master.voicecore.functions

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.MistakeRecord
import com.voicedeutsch.master.domain.usecase.book.AdvanceBookProgressUseCase
import com.voicedeutsch.master.domain.usecase.book.GetCurrentLessonUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.GetWeakPointsUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.UpdateRuleKnowledgeUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.UpdateWordKnowledgeUseCase
import com.voicedeutsch.master.domain.usecase.learning.GetWordsForRepetitionUseCase
import com.voicedeutsch.master.domain.usecase.speech.RecordPronunciationResultUseCase
import com.voicedeutsch.master.domain.usecase.user.GetUserStatisticsUseCase
import com.voicedeutsch.master.domain.usecase.user.UpdateUserLevelUseCase
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.util.DateUtils
import com.voicedeutsch.master.util.generateUUID
import kotlinx.serialization.json.*

/**
 * Routes function calls from Gemini Live API to the appropriate Domain Use Cases.
 *
 * Architecture lines 700-730 (FunctionRouter), 505-530 (Function Call flow).
 *
 * Function groups:
 *   1. Knowledge    : save_word_knowledge, save_rule_knowledge, record_mistake
 *   2. Book         : get_current_lesson, advance_to_next_lesson, mark_lesson_complete
 *   3. Session      : set_current_strategy, log_session_event,
 *                     get_words_for_repetition, get_weak_points
 *   4. User         : update_user_level, get_user_statistics
 *   5. Pronunciation: save_pronunciation_result, get_pronunciation_targets
 *
 * Every handler is a suspend function so it can safely call suspend Use Cases
 * without blocking the caller coroutine.
 *
 * **Important (M4):** [handleSetCurrentStrategy] returns success immediately but
 * does NOT apply the strategy change itself. The actual switch happens in
 * `VoiceCoreEngineImpl.applyFunctionSideEffects()` after every routed call.
 * If that method is missing or doesn't handle `set_current_strategy`, Gemini
 * will believe the strategy changed while the app state remains unchanged.
 *
 * **Parameter naming convention:** handler argument keys MUST match the JSON
 * property names in PromptTemplates function declarations exactly. Mismatches
 * cause silent `null` fallbacks because Gemini sends the declared name but the
 * handler reads a different key.
 */
class FunctionRouter(
    private val updateWordKnowledge:   UpdateWordKnowledgeUseCase,
    private val updateRuleKnowledge:   UpdateRuleKnowledgeUseCase,
    private val getWordsForRepetition: GetWordsForRepetitionUseCase,
    private val getWeakPoints:         GetWeakPointsUseCase,
    private val getCurrentLesson:      GetCurrentLessonUseCase,
    private val advanceBookProgress:   AdvanceBookProgressUseCase,
    private val updateUserLevel:       UpdateUserLevelUseCase,
    private val getUserStatistics:     GetUserStatisticsUseCase,
    private val recordPronunciation:   RecordPronunciationResultUseCase,
    private val knowledgeRepository:   KnowledgeRepository,
    private val json:                  Json,
) {

    // ── Result type ───────────────────────────────────────────────────────────

    /**
     * Result returned to [VoiceCoreEngineImpl] after routing a single function call.
     *
     * @param functionName  Exact name received from Gemini (used for side-effect tracking).
     * @param success       Whether the handler completed without an exception.
     * @param resultJson    JSON payload forwarded back to Gemini as a tool result.
     */
    data class FunctionCallResult(
        val functionName: String,
        val success:      Boolean,
        val resultJson:   String,
    )

    // ── Entry point ───────────────────────────────────────────────────────────

    /**
     * Dispatches a single Gemini function call to the correct handler.
     *
     * @param functionName  Exact name matching a declaration in getDeclarations().
     * @param argsJson      Raw JSON object string produced by Gemini.
     * @param userId        Active user identifier.
     * @param sessionId     Current session id, or null before session is open.
     */
    suspend fun route(
        functionName: String,
        argsJson:     String,
        userId:       String,
        sessionId:    String?,
    ): FunctionCallResult {
        return try {
            val args = json.parseToJsonElement(argsJson).jsonObject
            when (functionName) {
                // Knowledge
                "save_word_knowledge"       -> handleSaveWordKnowledge(args, userId)
                "save_rule_knowledge"       -> handleSaveRuleKnowledge(args, userId)
                "record_mistake"            -> handleRecordMistake(args, userId, sessionId)
                // Book
                "get_current_lesson"        -> handleGetCurrentLesson(userId)
                "advance_to_next_lesson"    -> handleAdvanceToNextLesson(args, userId)
                "mark_lesson_complete"      -> handleMarkLessonComplete(args, userId)
                // Session
                "get_words_for_repetition"  -> handleGetWordsForRepetition(args, userId)
                "get_weak_points"           -> handleGetWeakPoints(userId)
                "set_current_strategy"      -> handleSetCurrentStrategy(args)
                "log_session_event"         -> handleLogSessionEvent(args, sessionId)
                // User
                "update_user_level"         -> handleUpdateUserLevel(args, userId)
                "get_user_statistics"       -> handleGetUserStatistics(userId)
                // Pronunciation
                "save_pronunciation_result" -> handleSavePronunciationResult(args, userId, sessionId)
                "get_pronunciation_targets" -> handleGetPronunciationTargets(userId)

                else -> FunctionCallResult(
                    functionName = functionName,
                    success      = false,
                    resultJson   = """{"error":"Unknown function: $functionName"}""",
                )
            }
        } catch (e: Exception) {
            FunctionCallResult(
                functionName = functionName,
                success      = false,
                resultJson   = """{"error":"${e.message?.replace("\"", "'")}"}""",
            )
        }
    }

    // ── Knowledge handlers ────────────────────────────────────────────────────

    private suspend fun handleSaveWordKnowledge(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        val params = UpdateWordKnowledgeUseCase.Params(
            userId             = userId,
            wordGerman         = args.str("word")
                ?: return error("save_word_knowledge", "missing 'word'"),
            translation        = args.str("translation") ?: "",
            newLevel           = args.int("level", 1),
            quality            = args.int("quality", 3),
            pronunciationScore = args.float("pronunciation_score"),
            context            = args.str("context"),
        )
        updateWordKnowledge(params)
        return FunctionCallResult(
            functionName = "save_word_knowledge",
            success      = true,
            resultJson   = """{"status":"saved","word":"${params.wordGerman}","level":${params.newLevel}}""",
        )
    }

    private suspend fun handleSaveRuleKnowledge(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        val params = UpdateRuleKnowledgeUseCase.Params(
            userId    = userId,
            ruleId    = args.str("rule_id")
                ?: return error("save_rule_knowledge", "missing 'rule_id'"),
            ruleTitle = args.str("rule_title") ?: "",
            newLevel  = args.int("level", 1),
            quality   = args.int("quality", 3),
            context   = args.str("context"),
        )
        updateRuleKnowledge(params)
        return FunctionCallResult(
            functionName = "save_rule_knowledge",
            success      = true,
            resultJson   = """{"status":"saved","rule_id":"${params.ruleId}","level":${params.newLevel}}""",
        )
    }

    private suspend fun handleRecordMistake(
        args:      JsonObject,
        userId:    String,
        sessionId: String?,
    ): FunctionCallResult {
        val mistake = MistakeRecord(
            id          = generateUUID(),
            userId      = userId,
            sessionId   = sessionId ?: "",
            mistakeType = args.str("mistake_type") ?: "unknown",
            userInput   = args.str("user_input") ?: "",
            correctForm = args.str("correct_form") ?: "",
            context     = args.str("context") ?: "",
            explanation = args.str("explanation") ?: "",
            timestamp   = System.currentTimeMillis(),
        )
        knowledgeRepository.saveMistake(mistake)
        return FunctionCallResult(
            functionName = "record_mistake",
            success      = true,
            resultJson   = """{"status":"recorded","mistake_id":"${mistake.id}"}""",
        )
    }

    // ── Book handlers ─────────────────────────────────────────────────────────

    private suspend fun handleGetCurrentLesson(userId: String): FunctionCallResult {
        val lesson = getCurrentLesson(userId)
        return FunctionCallResult(
            functionName = "get_current_lesson",
            success      = true,
            resultJson   = buildJsonObject {
                put("chapter", lesson?.chapterNumber ?: 1)
                put("lesson",  lesson?.lessonNumber  ?: 1)
                put("title",   lesson?.title         ?: "")
                put("topic",   lesson?.topic         ?: "")
                lesson?.content?.let { c ->
                    put("vocabulary_count",    c.vocabulary.size)
                    put("grammar_rules_count", c.grammarRules.size)
                    put("exercises_count",     c.exercises.size)
                }
            }.toString(),
        )
    }

    private suspend fun handleAdvanceToNextLesson(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        val result = advanceBookProgress(
            AdvanceBookProgressUseCase.Params(
                userId           = userId,
                completedLesson  = args.int("completed_lesson", 1),
                completedChapter = args.int("completed_chapter", 1),
            )
        )
        return FunctionCallResult(
            functionName = "advance_to_next_lesson",
            success      = true,
            resultJson   = buildJsonObject {
                put("status",              "advanced")
                put("next_chapter",        result.nextChapter)
                put("next_lesson",         result.nextLesson)
                put("is_chapter_complete", result.isChapterComplete)
                put("is_book_complete",    result.isBookComplete)
            }.toString(),
        )
    }

    private suspend fun handleMarkLessonComplete(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        advanceBookProgress(
            AdvanceBookProgressUseCase.Params(
                userId           = userId,
                completedLesson  = args.int("lesson", 1),
                completedChapter = args.int("chapter", 1),
            )
        )
        return FunctionCallResult(
            functionName = "mark_lesson_complete",
            success      = true,
            resultJson   = """{"status":"marked_complete"}""",
        )
    }

    // ── Session handlers ──────────────────────────────────────────────────────

    private suspend fun handleGetWordsForRepetition(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        val limit = args.int("limit", 15)
        val items = getWordsForRepetition(userId, limit)
        return FunctionCallResult(
            functionName = "get_words_for_repetition",
            success      = true,
            resultJson   = buildJsonObject {
                put("count", items.size)
                put("words", buildJsonArray {
                    items.forEach { item ->
                        addJsonObject {
                            put("word",        item.word.german)
                            put("translation", item.word.russian)
                            put("level",       item.knowledge.knowledgeLevel)
                            put("priority",    item.priority.name)
                            put("last_seen",   item.knowledge.lastSeen?.let {
                                DateUtils.formatRelativeTime(it)
                            } ?: "never")
                        }
                    }
                })
            }.toString(),
        )
    }

    private suspend fun handleGetWeakPoints(userId: String): FunctionCallResult {
        val weakPoints = getWeakPoints(userId)
        return FunctionCallResult(
            functionName = "get_weak_points",
            success      = true,
            resultJson   = buildJsonObject {
                put("total", weakPoints.size)
                put("weak_points", buildJsonArray {
                    weakPoints.take(10).forEach { p ->
                        addJsonObject {
                            put("type",        p.type)
                            put("description", p.description)
                            put("severity",    p.severity)
                            put("examples",    buildJsonArray {
                                p.examples.forEach { add(it) }
                            })
                        }
                    }
                })
            }.toString(),
        )
    }

    private fun handleSetCurrentStrategy(args: JsonObject): FunctionCallResult {
        val strategyName = args.str("strategy") ?: "LINEAR_BOOK"
        val strategy = LearningStrategy.fromString(strategyName)
        // NOTE (M4): The actual strategy switch is applied in
        // VoiceCoreEngineImpl.applyFunctionSideEffects() after this result is
        // returned. This handler only acknowledges to Gemini — it does NOT
        // mutate session state directly.
        return FunctionCallResult(
            functionName = "set_current_strategy",
            success      = true,
            resultJson   = """{"status":"strategy_set","strategy":"${strategy.name}"}""",
        )
    }

    private fun handleLogSessionEvent(
        args:      JsonObject,
        sessionId: String?,
    ): FunctionCallResult {
        return FunctionCallResult(
            functionName = "log_session_event",
            success      = true,
            resultJson   = """{"status":"logged","session_id":"${sessionId ?: ""}"}""",
        )
    }

    // ── User handlers ─────────────────────────────────────────────────────────

    /**
     * M5 FIX: Changed `args.str("level")` → `args.str("cefr_level")`.
     *
     * The function declaration in PromptTemplates defines the parameter as
     * `"cefr_level"` with enum values ["A1", "A2", "B1", "B2", "C1", "C2"].
     * Previously this handler read `"level"` which always returned null,
     * causing every Gemini level assessment to silently default to "A1".
     */
    private suspend fun handleUpdateUserLevel(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        val cefrLevel = args.str("cefr_level") ?: "A1"
        updateUserLevel(
            UpdateUserLevelUseCase.Params(
                userId      = userId,
                cefrLevel   = cefrLevel,
                subLevel    = args.int("sub_level", 1),
                assessedBy  = "gemini_assessment",
            )
        )
        return FunctionCallResult(
            functionName = "update_user_level",
            success      = true,
            resultJson   = """{"status":"updated","level":"$cefrLevel"}""",
        )
    }

    private suspend fun handleGetUserStatistics(userId: String): FunctionCallResult {
        val stats = getUserStatistics(userId)
        return FunctionCallResult(
            functionName = "get_user_statistics",
            success      = true,
            resultJson   = buildJsonObject {
                put("total_words_learned",   stats.totalWordsLearned)
                put("total_sessions",        stats.totalSessions)
                put("streak_days",           stats.streakDays)
                put("cefr_level",            stats.cefrLevel)
                put("total_minutes_studied", stats.totalMinutesStudied)
                put("current_chapter",       stats.currentChapter)
                put("current_lesson",        stats.currentLesson)
            }.toString(),
        )
    }

    // ── Pronunciation handlers ────────────────────────────────────────────────

    /**
     * M6 FIX: Changed `args["phonetic_errors"]` → `args["problem_sounds"]`.
     *
     * The function declaration in PromptTemplates defines the array parameter
     * as `"problem_sounds"`. Previously this handler read `"phonetic_errors"`
     * which always returned null, causing pronunciation problem data from
     * Gemini to be silently discarded.
     */
    private suspend fun handleSavePronunciationResult(
        args:      JsonObject,
        userId:    String,
        sessionId: String?,
    ): FunctionCallResult {
        recordPronunciation(
            RecordPronunciationResultUseCase.Params(
                userId         = userId,
                sessionId      = sessionId ?: "",
                word           = args.str("word") ?: "",
                score          = args.float("score") ?: 0.5f,
                phoneticErrors = args["problem_sounds"]?.jsonArray
                    ?.map { it.jsonPrimitive.content }
                    ?: emptyList(),
            )
        )
        return FunctionCallResult(
            functionName = "save_pronunciation_result",
            success      = true,
            resultJson   = """{"status":"saved"}""",
        )
    }

    private suspend fun handleGetPronunciationTargets(userId: String): FunctionCallResult {
        val targets = knowledgeRepository.getPronunciationTargets(userId)
        return FunctionCallResult(
            functionName = "get_pronunciation_targets",
            success      = true,
            resultJson   = buildJsonObject {
                put("targets", buildJsonArray {
                    targets.take(5).forEach { t ->
                        addJsonObject {
                            put("sound",        t.sound)
                            put("example_word", t.exampleWord)
                            put("score",        t.averageScore)
                            put("trend",        t.trend.name)
                        }
                    }
                })
            }.toString(),
        )
    }

    // ── Function declarations для Gemini Setup ────────────────────────────────
    // Вызывается из ContextBuilder при формировании SessionContext.
    // Каждая строка — валидный JSON объект functionDeclaration по спецификации
    // ai.google.dev/api/live (BidiGenerateContentSetup.tools).

    fun getDeclarations(): List<String> = listOf(
        // ── Knowledge ─────────────────────────────────────────────────────────
        """
        {
          "name": "save_word_knowledge",
          "description": "Сохраняет прогресс изучения немецкого слова пользователем после практики.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "word":                { "type": "STRING",  "description": "Немецкое слово" },
              "translation":         { "type": "STRING",  "description": "Перевод на русский" },
              "level":               { "type": "INTEGER", "description": "Уровень знания 1-5 (1=новое, 5=освоено)" },
              "quality":             { "type": "INTEGER", "description": "Качество ответа 1-5 (SM-2)" },
              "pronunciation_score": { "type": "NUMBER",  "description": "Оценка произношения 0.0-1.0, опционально" },
              "context":             { "type": "STRING",  "description": "Предложение-контекст использования слова" }
            },
            "required": ["word", "translation", "level", "quality"]
          }
        }
        """,
        """
        {
          "name": "save_rule_knowledge",
          "description": "Сохраняет прогресс изучения грамматического правила.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "rule_id":    { "type": "STRING",  "description": "Идентификатор правила из грамматики урока" },
              "rule_title": { "type": "STRING",  "description": "Название правила" },
              "level":      { "type": "INTEGER", "description": "Уровень знания 1-5" },
              "quality":    { "type": "INTEGER", "description": "Качество ответа 1-5 (SM-2)" },
              "context":    { "type": "STRING",  "description": "Пример использования правила" }
            },
            "required": ["rule_id", "rule_title", "level", "quality"]
          }
        }
        """,
        """
        {
          "name": "record_mistake",
          "description": "Фиксирует ошибку пользователя для последующего анализа и повторения.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "mistake_type": { "type": "STRING", "description": "Тип ошибки: grammar, vocabulary, pronunciation, word_order" },
              "user_input":   { "type": "STRING", "description": "Что сказал/написал пользователь" },
              "correct_form": { "type": "STRING", "description": "Правильный вариант" },
              "context":      { "type": "STRING", "description": "Контекст где возникла ошибка" },
              "explanation":  { "type": "STRING", "description": "Объяснение ошибки" }
            },
            "required": ["mistake_type", "user_input", "correct_form"]
          }
        }
        """,

        // ── Book ──────────────────────────────────────────────────────────────
        """
        {
          "name": "get_current_lesson",
          "description": "Возвращает информацию о текущем уроке пользователя.",
          "parameters": {
            "type": "OBJECT",
            "properties": {},
            "required": []
          }
        }
        """,
        """
        {
          "name": "advance_to_next_lesson",
          "description": "Переводит пользователя к следующему уроку после завершения текущего.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "completed_lesson":  { "type": "INTEGER", "description": "Номер завершённого урока" },
              "completed_chapter": { "type": "INTEGER", "description": "Номер завершённой главы" }
            },
            "required": ["completed_lesson", "completed_chapter"]
          }
        }
        """,
        """
        {
          "name": "mark_lesson_complete",
          "description": "Отмечает урок как пройденный без перехода к следующему.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "chapter": { "type": "INTEGER", "description": "Номер главы" },
              "lesson":  { "type": "INTEGER", "description": "Номер урока" }
            },
            "required": ["chapter", "lesson"]
          }
        }
        """,

        // ── Session ───────────────────────────────────────────────────────────
        """
        {
          "name": "get_words_for_repetition",
          "description": "Возвращает список слов для повторения по алгоритму SRS.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "limit": { "type": "INTEGER", "description": "Максимальное количество слов (по умолчанию 15)" }
            },
            "required": []
          }
        }
        """,
        """
        {
          "name": "get_weak_points",
          "description": "Возвращает слабые места пользователя для целенаправленной практики.",
          "parameters": {
            "type": "OBJECT",
            "properties": {},
            "required": []
          }
        }
        """,
        """
        {
          "name": "set_current_strategy",
          "description": "Переключает стратегию обучения в текущей сессии.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "strategy": {
                "type": "STRING",
                "description": "Название стратегии",
                "enum": ["LINEAR_BOOK", "SPACED_REPETITION", "CONVERSATION", "GRAMMAR_FOCUS", "PRONUNCIATION_FOCUS"]
              }
            },
            "required": ["strategy"]
          }
        }
        """,
        """
        {
          "name": "log_session_event",
          "description": "Логирует важное событие сессии для аналитики.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "event_type": { "type": "STRING", "description": "Тип события" },
              "event_data": { "type": "STRING", "description": "JSON-данные события" }
            },
            "required": ["event_type"]
          }
        }
        """,

        // ── User ──────────────────────────────────────────────────────────────
        """
        {
          "name": "update_user_level",
          "description": "Обновляет уровень CEFR пользователя на основе оценки Gemini.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "cefr_level": {
                "type": "STRING",
                "description": "Уровень CEFR",
                "enum": ["A1", "A2", "B1", "B2", "C1", "C2"]
              },
              "sub_level": { "type": "INTEGER", "description": "Подуровень 1-3 внутри CEFR" }
            },
            "required": ["cefr_level"]
          }
        }
        """,
        """
        {
          "name": "get_user_statistics",
          "description": "Возвращает сводную статистику обучения пользователя.",
          "parameters": {
            "type": "OBJECT",
            "properties": {},
            "required": []
          }
        }
        """,

        // ── Pronunciation ─────────────────────────────────────────────────────
        """
        {
          "name": "save_pronunciation_result",
          "description": "Сохраняет результат оценки произношения слова.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "word":           { "type": "STRING",  "description": "Немецкое слово" },
              "score":          { "type": "NUMBER",  "description": "Оценка произношения 0.0-1.0" },
              "problem_sounds": {
                "type": "ARRAY",
                "description": "Список проблемных звуков",
                "items": { "type": "STRING" }
              }
            },
            "required": ["word", "score"]
          }
        }
        """,
        """
        {
          "name": "get_pronunciation_targets",
          "description": "Возвращает звуки/слова для целенаправленной практики произношения.",
          "parameters": {
            "type": "OBJECT",
            "properties": {},
            "required": []
          }
        }
        """
    )

    // ── Tiny JsonObject extension helpers ─────────────────────────────────────

    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.int(key: String, default: Int): Int =
        this[key]?.jsonPrimitive?.intOrNull ?: default

    private fun JsonObject.float(key: String): Float? =
        this[key]?.jsonPrimitive?.floatOrNull

    private fun error(fn: String, msg: String) =
        FunctionCallResult(fn, false, """{"error":"$msg"}""")
}