package com.voicedeutsch.master.data.repository

import android.content.Context
import com.voicedeutsch.master.data.local.database.dao.BookProgressDao
import com.voicedeutsch.master.data.local.database.dao.GrammarRuleDao
import com.voicedeutsch.master.data.local.database.dao.WordDao
import com.voicedeutsch.master.data.local.database.entity.BookProgressEntity
import com.voicedeutsch.master.data.local.database.entity.WordEntity
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.data.local.file.BookFileReader
import com.voicedeutsch.master.data.mapper.ProgressMapper.toDomain
import com.voicedeutsch.master.data.mapper.ProgressMapper.toEntity
import com.voicedeutsch.master.data.mapper.RuleMapper
import com.voicedeutsch.master.domain.model.book.BookMetadata
import com.voicedeutsch.master.domain.model.book.BookProgress
import com.voicedeutsch.master.domain.model.book.Chapter
import com.voicedeutsch.master.domain.model.book.Lesson
import com.voicedeutsch.master.domain.model.book.LessonContent
import com.voicedeutsch.master.domain.model.book.LessonVocabularyEntry
import com.voicedeutsch.master.domain.model.knowledge.GrammarRule
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.util.DateUtils
import com.voicedeutsch.master.util.generateUUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * ════════════════════════════════════════════════════════════════════════════
 * FIX: Баг #3 — "Чёрная дыра" грамматики
 * ════════════════════════════════════════════════════════════════════════════
 *
 * БЫЛО (два места):
 *
 * 1. getChapterGrammar() — тело метода содержало только заглушку return emptyList().
 *    Файл grammar.json открывался в пути, но данные никогда не читались.
 *    Стратегия GRAMMAR_DRILL получала пустой список правил → ИИ не знал грамматики.
 *
 * 2. loadBookIntoDatabase() — загружала только слова (vocabulary).
 *    Блок обработки grammar.json отсутствовал полностью.
 *    После onboarding БД содержала 0 грамматических правил.
 *
 * СТАЛО:
 *
 * 1. getChapterGrammar() — читает grammar.json из assets и возвращает
 *    список nameRu правил для данной главы. Используется в SystemInstruction
 *    для передачи ИИ списка тем текущей главы.
 *
 * 2. loadBookIntoDatabase() — добавлен блок загрузки грамматики для каждой главы.
 *    Парсит List<GrammarRule> из grammar.json, проставляет bookChapter,
 *    конвертирует через RuleMapper и сохраняет через grammarRuleDao.insertRules().
 *    Ошибка парсинга одной главы не прерывает загрузку остальных.
 *
 * Структура ожидаемого assets:
 *   book/
 *     chapter_01/
 *       vocabulary.json
 *       grammar.json     ← List<GrammarRule>
 *       lesson_01.json
 *       ...
 *     chapter_02/
 *       ...
 */
class BookRepositoryImpl(
    private val context: Context,
    private val bookFileReader: BookFileReader,
    private val bookProgressDao: BookProgressDao,
    private val wordDao: WordDao,
    private val grammarRuleDao: GrammarRuleDao,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val json: Json,
) : BookRepository {

    override suspend fun getBookMetadata(): BookMetadata =
        bookFileReader.readMetadata()

    override suspend fun getChapter(chapterNumber: Int): Chapter? {
        val info = bookFileReader.readChapterInfo(chapterNumber) ?: return null
        val lessons = (1..info.lessonsCount).map { lessonNum ->
            val content = bookFileReader.readLessonContent(chapterNumber, lessonNum)
            Lesson(
                number        = lessonNum,
                chapterNumber = chapterNumber,
                titleDe       = "Lektion $lessonNum",
                titleRu       = "Урок $lessonNum",
                content       = content,
                newWordsCount = content?.vocabulary?.size ?: 0,
            )
        }
        return Chapter(
            number        = chapterNumber,
            titleDe       = info.titleDe,
            titleRu       = info.titleRu,
            level         = info.level,
            lessons       = lessons,
            grammarTopics = info.topics,
        )
    }

    override suspend fun getLesson(chapterNumber: Int, lessonNumber: Int): Lesson? {
        val chapter = getChapter(chapterNumber) ?: return null
        return chapter.lessons.find { it.number == lessonNumber }
    }

    override suspend fun getLessonContent(
        chapterNumber: Int,
        lessonNumber: Int,
    ): LessonContent? =
        bookFileReader.readLessonContent(chapterNumber, lessonNumber)

    override suspend fun getChapterVocabulary(chapterNumber: Int): List<LessonVocabularyEntry> =
        bookFileReader.readChapterVocabulary(chapterNumber)

    /**
     * ✅ FIX: Читает grammar.json и возвращает список названий правил главы.
     *
     * БЫЛО: тело содержало path-переменную и сразу return emptyList() — файл не читался.
     * СТАЛО: открываем assets, парсим List<GrammarRule>, возвращаем nameRu каждого правила.
     *
     * Используется в ContextBuilder для передачи ИИ списка грамматических тем,
     * и в KnowledgeSummary для отображения прогресса по грамматике.
     */
    override suspend fun getChapterGrammar(chapterNumber: Int): List<String> {
        return try {
            val chapterDir = "chapter_${chapterNumber.toString().padStart(2, '0')}"
            val path = "book/$chapterDir/grammar.json"
            val jsonString = context.assets.open(path).bufferedReader().use { it.readText() }
            val rules = json.decodeFromString<List<GrammarRule>>(jsonString)
            rules.map { it.nameRu } // ИСПРАВЛЕНО: it.title → it.nameRu
        } catch (e: Exception) {
            android.util.Log.w("BookRepository", "getChapterGrammar($chapterNumber): ${e.message}")
            emptyList()
        }
    }

    override suspend fun getBookProgress(
        userId: String,
        chapter: Int,
        lesson: Int,
    ): BookProgress? =
        bookProgressDao.getProgress(userId, chapter, lesson)?.toDomain()

    override suspend fun getCurrentBookPosition(userId: String): Pair<Int, Int> {
        val current = bookProgressDao.getCurrentPosition(userId)
        return if (current != null) {
            Pair(current.chapter, current.lesson)
        } else {
            val firstProgress = BookProgressEntity(
                id      = generateUUID(),
                userId  = userId,
                chapter = 1,
                lesson  = 1,
                status  = "NOT_STARTED",
            )
            bookProgressDao.upsertProgress(firstProgress)
            Pair(1, 1)
        }
    }

    override suspend fun getAllBookProgress(userId: String): List<BookProgress> =
        bookProgressDao.getAllProgress(userId).map { it.toDomain() }

    override fun getBookProgressFlow(userId: String): Flow<List<BookProgress>> =
        bookProgressDao.getAllProgressFlow(userId).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun upsertBookProgress(progress: BookProgress) =
        bookProgressDao.upsertProgress(progress.toEntity())

    override suspend fun markLessonComplete(
        userId: String,
        chapter: Int,
        lesson: Int,
        score: Float,
    ) {
        val existing = bookProgressDao.getProgress(userId, chapter, lesson)
        if (existing == null) {
            bookProgressDao.upsertProgress(
                BookProgressEntity(
                    id          = generateUUID(),
                    userId      = userId,
                    chapter     = chapter,
                    lesson      = lesson,
                    status      = "COMPLETED",
                    score       = score,
                    startedAt   = DateUtils.nowTimestamp(),
                    completedAt = DateUtils.nowTimestamp(),
                )
            )
        } else {
            bookProgressDao.markComplete(userId, chapter, lesson, score, DateUtils.nowTimestamp())
        }
    }

    override suspend fun advanceToNextLesson(userId: String): Pair<Int, Int> {
        val (currentCh, currentLes) = getCurrentBookPosition(userId)
        val metadata    = getBookMetadata()
        val chapterInfo = bookFileReader.readChapterInfo(currentCh)

        return when {
            chapterInfo != null && currentLes < chapterInfo.lessonsCount -> {
                val nextLesson = currentLes + 1
                ensureProgressExists(userId, currentCh, nextLesson)
                Pair(currentCh, nextLesson)
            }
            currentCh < metadata.totalChapters -> {
                val nextChapter = currentCh + 1
                ensureProgressExists(userId, nextChapter, 1)
                Pair(nextChapter, 1)
            }
            else -> Pair(currentCh, currentLes)
        }
    }

    private suspend fun ensureProgressExists(userId: String, chapter: Int, lesson: Int) {
        if (bookProgressDao.getProgress(userId, chapter, lesson) == null) {
            bookProgressDao.upsertProgress(
                BookProgressEntity(
                    id      = generateUUID(),
                    userId  = userId,
                    chapter = chapter,
                    lesson  = lesson,
                    status  = "NOT_STARTED",
                )
            )
        }
    }

    override suspend fun getCompletedLessonsCount(userId: String): Int =
        bookProgressDao.getCompletedCount(userId)

    override suspend fun getTotalLessonsCount(): Int =
        getBookMetadata().totalLessons

    override suspend fun getBookCompletionPercentage(userId: String): Float {
        val completed = getCompletedLessonsCount(userId).toFloat()
        val total     = getTotalLessonsCount().toFloat()
        return if (total > 0) completed / total else 0f
    }

    override suspend fun isBookLoaded(): Boolean =
        preferencesDataStore.isBookLoaded()

    /**
     * ✅ FIX: Добавлена загрузка грамматики в loadBookIntoDatabase().
     *
     * БЫЛО: цикл по главам загружал только vocabulary → 0 правил в БД после onboarding.
     * СТАЛО: для каждой главы дополнительно читается grammar.json:
     *   1. Открываем assets/book/chapter_XX/grammar.json
     *   2. Парсим List<GrammarRule>
     *   3. Проставляем bookChapter (нужно для фильтрации по главе)
     *   4. Конвертируем через RuleMapper.toEntity()
     *   5. Сохраняем через grammarRuleDao.insertRules()
     *
     * Ошибка парсинга одной главы логируется, но не прерывает загрузку остальных.
     */
    override suspend fun loadBookIntoDatabase() {
        if (isBookLoaded()) return

        val metadata = getBookMetadata()

        for (chapterNum in 1..metadata.totalChapters) {

            // 1. Загрузка словаря
            val vocabulary   = bookFileReader.readChapterVocabulary(chapterNum)
            val wordEntities = vocabulary.map { entry ->
                WordEntity(
                    id              = generateUUID(),
                    german          = entry.german,
                    russian         = entry.russian,
                    partOfSpeech    = "NOUN",
                    gender          = entry.gender,
                    plural          = entry.plural,
                    difficultyLevel = entry.level,
                    topic           = "",
                    bookChapter     = chapterNum,
                    createdAt       = DateUtils.nowTimestamp(),
                    source          = "book",
                )
            }
            if (wordEntities.isNotEmpty()) {
                wordDao.insertWords(wordEntities)
            }

            // 2. ✅ FIX: Загрузка грамматики
            // Ошибка одной главы не должна ломать загрузку всей книги → try/catch на блок.
            try {
                val chapterDir   = "chapter_${chapterNum.toString().padStart(2, '0')}"
                val grammarJson  = context.assets
                    .open("book/$chapterDir/grammar.json")
                    .bufferedReader()
                    .use { it.readText() }

                val rules        = json.decodeFromString<List<GrammarRule>>(grammarJson)
                val ruleEntities = rules
                    .map { rule -> rule.copy(bookChapter = chapterNum) }
                    .map { rule -> with(RuleMapper) { rule.toEntity(json) } }

                if (ruleEntities.isNotEmpty()) {
                    grammarRuleDao.insertRules(ruleEntities)
                    android.util.Log.d(
                        "BookRepository",
                        "Chapter $chapterNum: loaded ${ruleEntities.size} grammar rules"
                    )
                }
            } catch (e: Exception) {
                // grammar.json может отсутствовать в главе — это не ошибка
                android.util.Log.w(
                    "BookRepository",
                    "Chapter $chapterNum: grammar.json not found or invalid — ${e.message}"
                )
            }
        }

        preferencesDataStore.setBookLoaded(true)
    }

    override suspend fun searchContent(query: String): List<BookRepository.SearchHit> {
        val results  = mutableListOf<BookRepository.SearchHit>()
        val metadata = getBookMetadata()
        for (chapterNum in 1..metadata.totalChapters) {
            val chapterInfo = bookFileReader.readChapterInfo(chapterNum) ?: continue
            for (lessonNum in 1..chapterInfo.lessonsCount) {
                val content = bookFileReader.readLessonContent(chapterNum, lessonNum) ?: continue
                val vocab   = content.vocabulary ?: emptyList()
                for (entry in vocab) {
                    if (entry.german.contains(query, ignoreCase = true) ||
                        entry.russian.contains(query, ignoreCase = true)) {
                        results.add(
                            BookRepository.SearchHit(
                                chapter = chapterNum,
                                lesson  = lessonNum,
                                snippet = "${entry.german} — ${entry.russian}",
                            )
                        )
                    }
                }
            }
        }
        return results
    }
}