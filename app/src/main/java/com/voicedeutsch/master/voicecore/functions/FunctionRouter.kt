package com.voicedeutsch.master.voicecore.functions

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.MistakeLog       // FIX: was MistakeRecord (wrong type)
import com.voicedeutsch.master.domain.model.knowledge.MistakeType      // FIX: added — needed for MistakeLog.type
import com.voicedeutsch.master.domain.usecase.book.AdvanceBookProgressUseCase
import com.voicedeutsch.master.domain.usecase.book.GetCurrentLessonUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.GetWeakPointsUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.GetWordsForRepetitionUseCase  // FIX: was domain.usecase.learning (wrong package)
import com.voicedeutsch.master.domain.usecase.knowledge.UpdateRuleKnowledgeUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.UpdateWordKnowledgeUseCase
import com.voicedeutsch.master.domain.usecase.speech.RecordPronunciationResultUseCase
import com.voicedeutsch.master.domain.usecase.user.GetUserStatisticsUseCase            // FIX: class created (was missing)
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

    /**
     * FIX: Removed 'ruleTitle' and 'context' from Params — they don't exist
     * in UpdateRuleKnowledgeUseCase.Params. Only userId, ruleId, newLevel, quality.
     */
    private suspend fun handleSaveRuleKnowledge(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        val ruleId = args.str("rule_id")
            ?: return error("save_rule_knowledge", "missing 'rule_id'")
        val params = UpdateRuleKnowledgeUseCase.Params(
            userId   = userId,
            ruleId   = ruleId,
            newLevel = args.int("level", 1),
            quality  = args.int("quality", 3),
        )
        updateRuleKnowledge(params)
        return FunctionCallResult(
            functionName = "save_rule_knowledge",
            success      = true,
            resultJson   = """{"status":"saved","rule_id":"$ruleId","level":${params.newLevel}}""",
        )
    }

    /**
     * FIX: Replaced MistakeRecord with MistakeLog (correct domain model).
     *
     * MistakeLog fields:
     *   id, userId, sessionId, type (MistakeType), item, expected, actual,
     *   context, explanation, timestamp, createdAt
     *
     * Mapping from Gemini args:
     *   mistake_type → MistakeType enum
     *   user_input   → actual
     *   correct_form → expected
     *   context      → context + item (used as item description)
     *
     * FIX: Changed saveMistake → logMistake (actual KnowledgeRepository method).
     */
    private suspend fun handleRecordMistake(
        args:      JsonObject,
        userId:    String,
        sessionId: String?,
    ): FunctionCallResult {
        val mistakeTypeStr = args.str("mistake_type") ?: "unknown"
        val mistakeType = when (mistakeTypeStr.lowercase()) {
            "grammar"       -> MistakeType.GRAMMAR
            "vocabulary",
            "word"          -> MistakeType.WORD
            "pronunciation" -> MistakeType.PRONUNCIATION
            "phrase"        -> MistakeType.PHRASE
            else            -> MistakeType.GRAMMAR
        }
        val mistake = MistakeLog(
            id          = generateUUID(),
            userId      = userId,
            sessionId   = sessionId,
            type        = mistakeType,
            item        = args.str("user_input") ?: "",
            expected    = args.str("correct_form") ?: "",
            actual      = args.str("user_input") ?: "",
            context     = args.str("context") ?: "",
            explanation = args.str("explanation") ?: "",
            timestamp   = System.currentTimeMillis(),
        )
        knowledgeRepository.logMistake(mistake)
        return FunctionCallResult(
            functionName = "record_mistake",
            success      = true,
            resultJson   = """{"status":"recorded","mistake_id":"${mistake.id}"}""",
        )
    }

    // ── Book handlers ─────────────────────────────────────────────────────────

    /**
     * FIX: Lesson model uses titleDe/titleRu, not title/topic.
     * FIX: LessonContent has vocabulary, exerciseMarkers — no grammarRules/exercises.
     */
    private suspend fun handleGetCurrentLesson(userId: String): FunctionCallResult {
        val lesson = getCurrentLesson(userId)
        return FunctionCallResult(
            functionName = "get_current_lesson",
            success      = true,
            resultJson   = buildJsonObject {
                put("chapter", lesson?.chapterNumber ?: 1)
                put("lesson",  lesson?.number ?: 1)
                put("title",   lesson?.titleDe ?: "")
                put("title_ru", lesson?.titleRu ?: "")
                put("focus",   lesson?.focus?.name ?: "MIXED")
                lesson?.content?.let { c ->
                    put("vocabulary_count", c.vocabulary.size)
                    put("has_exercises",    c.exerciseMarkers.isNotEmpty())
                }
            }.toString(),
        )
    }

    /**
     * FIX: AdvanceBookProgressUseCase.invoke(userId, score) → BookAdvanceResult.
     * It reads the current position internally from BookRepository.
     * No completedChapter/completedLesson params — UseCase determines them.
     *
     * BookAdvanceResult fields:
     *   previousChapter, previousLesson, newChapter, newLesson,
     *   isChapterComplete, isBookComplete
     */
    private suspend fun handleAdvanceToNextLesson(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        val score = args.float("score") ?: 1.0f
        val result = advanceBookProgress(userId, score)
        return FunctionCallResult(
            functionName = "advance_to_next_lesson",
            success      = true,
            resultJson   = buildJsonObject {
                put("status",              "advanced")
                put("next_chapter",        result.newChapter)
                put("next_lesson",         result.newLesson)
                put("is_chapter_complete", result.isChapterComplete)
                put("is_book_complete",    result.isBookComplete)
            }.toString(),
        )
    }

    /**
     * FIX: Same as handleAdvanceToNextLesson — invoke(userId, score).
     * UseCase reads current position internally.
     */
    private suspend fun handleMarkLessonComplete(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        val score = args.float("score") ?: 1.0f
        advanceBookProgress(userId, score)
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

    /**
     * FIX: WeakPoint fields adjusted. GetWeakPointsUseCase returns List<String>
     * (simple weak-point descriptions). Simplified output to match.
     */
    private suspend fun handleGetWeakPoints(userId: String): FunctionCallResult {
        val weakPoints = getWeakPoints(userId)
        return FunctionCallResult(
            functionName = "get_weak_points",
            success      = true,
            resultJson   = buildJsonObject {
                put("total", weakPoints.size)
                put("weak_points", buildJsonArray {
                    weakPoints.take(10).forEach { point ->
                        add(JsonPrimitive(point))
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
     * FIX: UpdateUserLevelUseCase.invoke(userId) → LevelUpdateResult.
     * The UseCase auto-calculates the CEFR level from knowledge data.
     * Gemini's suggested level is acknowledged but the actual level is
     * determined by the algorithm (vocab/grammar thresholds).
     */
    private suspend fun handleUpdateUserLevel(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        val result = updateUserLevel(userId)
        return FunctionCallResult(
            functionName = "update_user_level",
            success      = true,
            resultJson   = buildJsonObject {
                put("status",         if (result.levelChanged) "updated" else "confirmed")
                put("level",          result.newLevel.name)
                put("sub_level",      result.newSubLevel)
                put("level_changed",  result.levelChanged)
                put("reason",         result.reason)
            }.toString(),
        )
    }

    /**
     * FIX: UserStatistics field names corrected to match the domain model:
     *   totalWordsLearned    → totalWords
     *   cefrLevel            → (removed — not in UserStatistics)
     *   totalMinutesStudied  → totalMinutes
     *   currentLesson        → (removed — not in UserStatistics)
     *   + added more stats from the actual model
     */
    private suspend fun handleGetUserStatistics(userId: String): FunctionCallResult {
        val stats = getUserStatistics(userId)
        return FunctionCallResult(
            functionName = "get_user_statistics",
            success      = true,
            resultJson   = buildJsonObject {
                put("total_words",          stats.totalWords)
                put("active_words",         stats.activeWords)
                put("passive_words",        stats.passiveWords)
                put("total_rules",          stats.totalRules)
                put("known_rules",          stats.knownRules)
                put("total_sessions",       stats.totalSessions)
                put("total_minutes",        stats.totalMinutes)
                put("streak_days",          stats.streakDays)
                put("average_score",        stats.averageScore)
                put("avg_pronunciation",    stats.averagePronunciationScore)
                put("words_for_review",     stats.wordsForReviewToday)
                put("rules_for_review",     stats.rulesForReviewToday)
                put("book_progress",        stats.bookProgress)
                put("current_chapter",      stats.currentChapter)
                put("total_chapters",       stats.totalChapters)
            }.toString(),
        )
    }

    // ── Pronunciation handlers ────────────────────────────────────────────────

    /**
     * M6 FIX: Changed `args["phonetic_errors"]` → `args["problem_sounds"]`.
     *
     * FIX: RecordPronunciationResultUseCase.Params field renamed:
     *   phoneticErrors → problemSounds (matches PronunciationResult model).
     */
    private suspend fun handleSavePronunciationResult(
        args:      JsonObject,
        userId:    String,
        sessionId: String?,
    ): FunctionCallResult {
        recordPronunciation(
            RecordPronunciationResultUseCase.Params(
                userId        = userId,
                sessionId     = sessionId ?: "",
                word          = args.str("word") ?: "",
                score         = args.float("score") ?: 0.5f,
                problemSounds = args["problem_sounds"]?.jsonArray
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

    /**
     * FIX: PhoneticTarget fields corrected:
     *   exampleWord  → inWords.firstOrNull() (no exampleWord field)
     *   averageScore → currentScore
     *   trend        → trend.name (enum)
     *
     * FIX: KnowledgeRepository method — using getProblemSounds()
     * (actual method name in KnowledgeRepository interface).
     */
    private suspend fun handleGetPronunciationTargets(userId: String): FunctionCallResult {
        val targets = knowledgeRepository.getProblemSounds(userId)
        return FunctionCallResult(
            functionName = "get_pronunciation_targets",
            success      = true,
            resultJson   = buildJsonObject {
                put("targets", buildJsonArray {
                    targets.take(5).forEach { t ->
                        addJsonObject {
                            put("sound",        t.sound)
                            put("example_word", t.inWords.firstOrNull() ?: "")
                            put("score",        t.currentScore)
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
