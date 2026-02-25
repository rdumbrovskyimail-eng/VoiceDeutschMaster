package com.voicedeutsch.master.voicecore.functions

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.MistakeLog
import com.voicedeutsch.master.domain.model.knowledge.MistakeType
import com.voicedeutsch.master.domain.usecase.book.AdvanceBookProgressUseCase
import com.voicedeutsch.master.domain.usecase.book.GetCurrentLessonUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.GetWeakPointsUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.GetWordsForRepetitionUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.UpdateRuleKnowledgeUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.UpdateWordKnowledgeUseCase
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
 * ════════════════════════════════════════════════════════════════════════════
 * ИЗМЕНЕНИЯ (Модуль 6):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   1. УДАЛЕНО: getDeclarations() — 250 строк hardcoded JSON-строк.
 *      Нативные declarations уже есть в *Functions.kt файлах
 *      (KnowledgeFunctions, BookFunctions, SessionFunctions и т.д.).
 *      GeminiClient.mapToFirebaseDeclaration() маппит их в SDK-объекты.
 *
 *   2. ДОБАВЛЕНО: Type coercion helpers — intCoerced(), floatCoerced(), boolCoerced().
 *      Gemini иногда галлюцинирует типы: шлёт "2" (строку) вместо 2 (число),
 *      "true" вместо true, "0.5" вместо 0.5.
 *      Старые хелперы .int() / .float() падали с ClassCastException.
 *
 *   3. ИЗМЕНЕНО: Все handler-ы используют coerced-хелперы вместо старых.
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

    data class FunctionCallResult(
        val functionName: String,
        val success:      Boolean,
        val resultJson:   String,
    )

    // ── Entry point ───────────────────────────────────────────────────────────

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
                // UI
                "show_word_card"            -> handleShowWordCard(args)
                "show_grammar_hint"         -> handleShowGrammarHint(args)
                "trigger_celebration"       -> handleTriggerCelebration(args)

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
            newLevel           = args.intCoerced("level", 1),      // ✅ coerced
            quality            = args.intCoerced("quality", 3),    // ✅ coerced
            pronunciationScore = args.floatCoerced("pronunciation_score"),  // ✅ coerced
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
        val ruleId = args.str("rule_id")
            ?: return error("save_rule_knowledge", "missing 'rule_id'")
        val params = UpdateRuleKnowledgeUseCase.Params(
            userId   = userId,
            ruleId   = ruleId,
            newLevel = args.intCoerced("level", 1),      // ✅ coerced
            quality  = args.intCoerced("quality", 3),    // ✅ coerced
        )
        updateRuleKnowledge(params)
        return FunctionCallResult(
            functionName = "save_rule_knowledge",
            success      = true,
            resultJson   = """{"status":"saved","rule_id":"$ruleId","level":${params.newLevel}}""",
        )
    }

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

    private suspend fun handleGetCurrentLesson(userId: String): FunctionCallResult {
        val data = getCurrentLesson(userId)
        return FunctionCallResult(
            functionName = "get_current_lesson",
            success      = true,
            resultJson   = buildJsonObject {
                put("chapter",  data?.chapterNumber ?: 1)
                put("lesson",   data?.lessonNumber ?: 1)
                put("title",    data?.lesson?.titleDe ?: "")
                put("title_ru", data?.lesson?.titleRu ?: "")
                put("focus",    data?.lesson?.focus?.name ?: "MIXED")
                data?.content?.let { c ->
                    put("vocabulary_count", c.vocabulary.size)
                    put("has_exercises",    c.exerciseMarkers.isNotEmpty())
                }
            }.toString(),
        )
    }

    private suspend fun handleAdvanceToNextLesson(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        val score = args.floatCoerced("score") ?: 1.0f    // ✅ coerced
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

    private suspend fun handleMarkLessonComplete(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        val score = args.floatCoerced("score") ?: 1.0f    // ✅ coerced
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
        val limit = args.intCoerced("limit", 15)    // ✅ coerced
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
                    weakPoints.take(10).forEach { point ->
                        addJsonObject {
                            put("description", point.description)
                            put("category",    point.category)
                            put("severity",    point.severity)
                        }
                    }
                })
            }.toString(),
        )
    }

    private fun handleSetCurrentStrategy(args: JsonObject): FunctionCallResult {
        val strategyName = args.str("strategy") ?: "LINEAR_BOOK"
        val strategy = LearningStrategy.fromString(strategyName)
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

    private suspend fun handleUpdateUserLevel(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        val result = updateUserLevel(userId)
        return FunctionCallResult(
            functionName = "update_user_level",
            success      = true,
            resultJson   = buildJsonObject {
                put("status",        if (result.levelChanged) "updated" else "confirmed")
                put("level",         result.newLevel.name)
                put("sub_level",     result.newSubLevel)
                put("level_changed", result.levelChanged)
                put("reason",        result.reason)
            }.toString(),
        )
    }

    private suspend fun handleGetUserStatistics(userId: String): FunctionCallResult {
        val stats = getUserStatistics(userId)
        return FunctionCallResult(
            functionName = "get_user_statistics",
            success      = true,
            resultJson   = buildJsonObject {
                put("total_words",       stats.totalWords)
                put("active_words",      stats.activeWords)
                put("passive_words",     stats.passiveWords)
                put("total_rules",       stats.totalRules)
                put("known_rules",       stats.knownRules)
                put("total_sessions",    stats.totalSessions)
                put("total_minutes",     stats.totalMinutes)
                put("streak_days",       stats.streakDays)
                put("average_score",     stats.averageScore)
                put("avg_pronunciation", stats.averagePronunciationScore)
                put("words_for_review",  stats.wordsForReviewToday)
                put("rules_for_review",  stats.rulesForReviewToday)
                put("book_progress",     stats.bookProgress)
                put("current_chapter",   stats.currentChapter)
                put("total_chapters",    stats.totalChapters)
            }.toString(),
        )
    }

    // ── Pronunciation handlers ────────────────────────────────────────────────

    private suspend fun handleSavePronunciationResult(
        args:      JsonObject,
        userId:    String,
        sessionId: String?,
    ): FunctionCallResult {
        recordPronunciation(
            RecordPronunciationResultUseCase.Params(
                userId        = userId,
                sessionId     = sessionId,
                word          = args.str("word") ?: "",
                score         = args.floatCoerced("score") ?: 0.5f,    // ✅ coerced
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

    // УДАЛЕНО: getDeclarations(): List<String>
    // 250 строк hardcoded JSON-строк. Нативные declarations находятся в:
    //   - KnowledgeFunctions.declarations
    //   - BookFunctions.declarations
    //   - SessionFunctions.declarations
    //   - LearningFunctions.declarations
    //   - ProgressFunctions.declarations
    //   - UIFunctions.declarations
    // FunctionRegistry.getAllDeclarations() агрегирует их.
    // GeminiClient.mapToFirebaseDeclaration() маппит в Firebase SDK объекты.

    // ── UI handlers ───────────────────────────────────────────────────────────

    private fun handleShowWordCard(args: JsonObject) =
        FunctionCallResult("show_word_card", true, """{"status":"displayed"}""")

    private fun handleShowGrammarHint(args: JsonObject) =
        FunctionCallResult("show_grammar_hint", true, """{"status":"displayed"}""")

    private fun handleTriggerCelebration(args: JsonObject) =
        FunctionCallResult("trigger_celebration", true, """{"status":"triggered"}""")

    // ══════════════════════════════════════════════════════════════════════════
    // Type-safe JSON helpers с COERCION для AI-галлюцинаций
    // ══════════════════════════════════════════════════════════════════════════
    //
    // Gemini иногда возвращает:
    //   "level": "2"  вместо  "level": 2
    //   "score": "0.5" вместо "score": 0.5
    //   "flag": "true" вместо "flag": true
    //
    // Стандартные jsonPrimitive.intOrNull / .floatOrNull возвращают null
    // для строковых значений → функция получает default вместо реального значения.
    // Coerced-хелперы сначала пробуют число, потом парсят строку.
    // ══════════════════════════════════════════════════════════════════════════

    /** Извлекает строку. Работает и для числовых значений (приводит к строке). */
    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    /**
     * Извлекает Int с coercion: 2 → 2, "2" → 2, "abc" → default.
     * Защищает от AI-галлюцинации типов.
     */
    private fun JsonObject.intCoerced(key: String, default: Int): Int {
        val element = this[key]?.jsonPrimitive ?: return default
        // Сначала пробуем как число
        element.intOrNull?.let { return it }
        // Fallback: парсим строку "2" → 2
        element.contentOrNull?.toIntOrNull()?.let { return it }
        // Fallback 2: "2.0" → 2 (Gemini иногда шлёт float вместо int)
        element.contentOrNull?.toFloatOrNull()?.toInt()?.let { return it }
        return default
    }

    /**
     * Извлекает Float с coercion: 0.5 → 0.5, "0.5" → 0.5, "abc" → null.
     */
    private fun JsonObject.floatCoerced(key: String): Float? {
        val element = this[key]?.jsonPrimitive ?: return null
        element.floatOrNull?.let { return it }
        return element.contentOrNull?.toFloatOrNull()
    }

    /**
     * Извлекает Float с coercion и default.
     */
    private fun JsonObject.floatCoerced(key: String, default: Float): Float =
        floatCoerced(key) ?: default

    /**
     * Извлекает Boolean с coercion: true → true, "true" → true, 1 → true.
     */
    private fun JsonObject.boolCoerced(key: String, default: Boolean = false): Boolean {
        val element = this[key]?.jsonPrimitive ?: return default
        element.booleanOrNull?.let { return it }
        return when (element.contentOrNull?.lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else               -> default
        }
    }

    private fun error(fn: String, msg: String) =
        FunctionCallResult(fn, false, """{"error":"$msg"}""")
}