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
 * Function groups:
 *   1. Knowledge    : save_word_knowledge, save_rule_knowledge, record_mistake
 *   2. Book         : get_current_lesson, advance_to_next_lesson, mark_lesson_complete,
 *                     read_lesson_paragraph
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
 *   2. ДОБАВЛЕНО: Type coercion helpers — intCoerced(), floatCoerced(), boolCoerced().
 *   3. ИЗМЕНЕНО: Все handler-ы используют coerced-хелперы вместо старых.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * ИЗМЕНЕНИЯ (Баг #2 — "Слепой переход по книге"):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   FIX: handleAdvanceToNextLesson() теперь возвращает краткий превью нового
 *   урока (≤ 3000 символов), а не полный текст.
 *
 *   БЫЛО: new_lesson_text — весь текст урока (может быть 20 000+ символов),
 *   что легко превышает лимит toolResponse (~10k токенов).
 *
 *   СТАЛО:
 *   • advance_to_next_lesson  → new_lesson_preview (≤ 3000 символов),
 *                               total_paragraphs, vocabulary_preview (≤ 10 слов),
 *                               hint с подсказкой как читать дальше.
 *   • read_lesson_paragraph   → один абзац по индексу (новая функция).
 *
 *   ИИ читает урок «постранично» через read_lesson_paragraph(index),
 *   не упираясь в лимит размера ответа инструмента.
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

    companion object {
        /** Символов в превью текста урока, передаваемом при advance_to_next_lesson. */
        private const val LESSON_TEXT_PREVIEW_CHARS = 3_000

        /** Слов словаря в превью (остальное ИИ может запросить через get_current_lesson). */
        private const val VOCABULARY_PREVIEW_LIMIT  = 10

        /** Разделитель абзацев в тексте урока. */
        private val PARAGRAPH_DELIMITER = Regex("""\n{2,}""")
    }

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
                "read_lesson_paragraph"     -> handleReadLessonParagraph(args, userId)
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
            newLevel           = args.intCoerced("level", 1),
            quality            = args.intCoerced("quality", 3),
            pronunciationScore = args.floatCoerced("pronunciation_score"),
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
            newLevel = args.intCoerced("level", 1),
            quality  = args.intCoerced("quality", 3),
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

    /**
     * Переходит к следующему уроку и возвращает его **краткий** превью.
     *
     * Полный текст урока может занимать десятки тысяч символов и не помещается
     * в toolResponse (~10k токенов). Поэтому здесь передаются только:
     *   • new_lesson_preview — первые [LESSON_TEXT_PREVIEW_CHARS] символов текста;
     *   • vocabulary_preview  — первые [VOCABULARY_PREVIEW_LIMIT] слов словаря;
     *   • total_paragraphs    — чтобы ИИ знал, сколько абзацев ждёт впереди;
     *   • hint                — инструкция: вызывать read_lesson_paragraph(index).
     *
     * Это позволяет ИИ немедленно начать работу с новым уроком, а затем
     * дочитывать оставшийся текст по одному абзацу через отдельные вызовы.
     */
    private suspend fun handleAdvanceToNextLesson(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        val score  = args.floatCoerced("score") ?: 1.0f
        val result = advanceBookProgress(userId, score)

        val newLessonData = getCurrentLesson(userId)
        val content       = newLessonData?.content

        val fullText        = content?.mainContent ?: ""
        val previewText     = fullText.take(LESSON_TEXT_PREVIEW_CHARS)
        val totalParagraphs = fullText
            .split(PARAGRAPH_DELIMITER)
            .count { it.isNotBlank() }

        val vocabularyPreview = content?.vocabulary
            ?.take(VOCABULARY_PREVIEW_LIMIT)
            ?.map { "${it.german} — ${it.russian}" }
            ?: emptyList()

        return FunctionCallResult(
            functionName = "advance_to_next_lesson",
            success      = true,
            resultJson   = buildJsonObject {
                put("status",              "advanced")
                put("next_chapter",        result.newChapter)
                put("next_lesson",         result.newLesson)
                put("is_chapter_complete", result.isChapterComplete)
                put("is_book_complete",    result.isBookComplete)
                // Мета нового урока
                put("new_lesson_title",    newLessonData?.lesson?.titleDe ?: "")
                put("new_lesson_title_ru", newLessonData?.lesson?.titleRu ?: "")
                // Краткий превью текста (≤ LESSON_TEXT_PREVIEW_CHARS символов)
                put("new_lesson_preview",  previewText)
                put("text_truncated",      fullText.length > LESSON_TEXT_PREVIEW_CHARS)
                put("total_paragraphs",    totalParagraphs)
                // Первые VOCABULARY_PREVIEW_LIMIT слов словаря
                put("vocabulary_preview",  buildJsonArray { vocabularyPreview.forEach { add(it) } })
                put("total_vocabulary",    content?.vocabulary?.size ?: 0)
                // Подсказка ИИ: как читать оставшийся текст
                put("hint", "Use read_lesson_paragraph(index) to read paragraphs 0..${maxOf(0, totalParagraphs - 1)}")
            }.toString(),
        )
    }

    /**
     * Возвращает один абзац урока по его индексу (0-based).
     *
     * ИИ вызывает эту функцию последовательно после advance_to_next_lesson,
     * чтобы прочитать весь текст урока без превышения лимита toolResponse.
     *
     * Поле [has_next] позволяет ИИ понять, нужно ли продолжать чтение.
     *
     * Пример системного промпта:
     * "Если total_paragraphs > 0, читай абзацы через read_lesson_paragraph(0),
     *  read_lesson_paragraph(1), … пока has_next = false."
     */
    private suspend fun handleReadLessonParagraph(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        val index      = args.intCoerced("index", 0)
        val lessonData = getCurrentLesson(userId)
        val paragraphs = (lessonData?.content?.mainContent ?: "")
            .split(PARAGRAPH_DELIMITER)
            .filter { it.isNotBlank() }

        if (paragraphs.isEmpty()) {
            return error("read_lesson_paragraph", "lesson has no content")
        }
        if (index < 0 || index >= paragraphs.size) {
            return FunctionCallResult(
                functionName = "read_lesson_paragraph",
                success      = false,
                resultJson   = """{"error":"index $index out of range 0..${paragraphs.size - 1}"}""",
            )
        }

        return FunctionCallResult(
            functionName = "read_lesson_paragraph",
            success      = true,
            resultJson   = buildJsonObject {
                put("index",     index)
                put("total",     paragraphs.size)
                put("has_next",  index < paragraphs.size - 1)
                put("paragraph", paragraphs[index])
            }.toString(),
        )
    }

    private suspend fun handleMarkLessonComplete(
        args:   JsonObject,
        userId: String,
    ): FunctionCallResult {
        val score = args.floatCoerced("score") ?: 1.0f
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
        val limit = args.intCoerced("limit", 15)
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
        val eventType = args.str("event_type") ?: "unknown"
        val details   = args.str("details") ?: ""
        // ✅ ДОБАВЛЕНО: логирование событий сессии
        android.util.Log.d("FunctionRouter", "Session event: type=$eventType, details=$details, session=$sessionId")
        return FunctionCallResult(
            functionName = "log_session_event",
            success      = true,
            resultJson   = """{"status":"logged","session_id":"${sessionId ?: ""}","event_type":"$eventType"}""",
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
                score         = args.floatCoerced("score") ?: 0.5f,
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

    // ── UI handlers ───────────────────────────────────────────────────────────

    private fun handleShowWordCard(args: JsonObject) =
        FunctionCallResult("show_word_card", true, """{"status":"displayed"}""")

    private fun handleShowGrammarHint(args: JsonObject) =
        FunctionCallResult("show_grammar_hint", true, """{"status":"displayed"}""")

    private fun handleTriggerCelebration(args: JsonObject) =
        FunctionCallResult("trigger_celebration", true, """{"status":"triggered"}""")

    // ── Type-safe JSON helpers с COERCION ─────────────────────────────────────

    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.intCoerced(key: String, default: Int): Int {
        val element = this[key]?.jsonPrimitive ?: return default
        element.intOrNull?.let { return it }
        element.contentOrNull?.toIntOrNull()?.let { return it }
        element.contentOrNull?.toFloatOrNull()?.toInt()?.let { return it }
        return default
    }

    private fun JsonObject.floatCoerced(key: String): Float? {
        val element = this[key]?.jsonPrimitive ?: return null
        element.floatOrNull?.let { return it }
        return element.contentOrNull?.toFloatOrNull()
    }

    private fun JsonObject.floatCoerced(key: String, default: Float): Float =
        floatCoerced(key) ?: default

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
