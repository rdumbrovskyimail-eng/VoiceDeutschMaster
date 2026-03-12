// Путь: src/test/java/com/voicedeutsch/master/data/mapper/ProgressMapperTest.kt
package com.voicedeutsch.master.data.mapper

import com.voicedeutsch.master.data.local.database.entity.BookProgressEntity
import com.voicedeutsch.master.data.local.database.entity.DailyStatisticsEntity
import com.voicedeutsch.master.data.local.database.entity.MistakeLogEntity
import com.voicedeutsch.master.data.local.database.entity.PronunciationRecordEntity
import com.voicedeutsch.master.domain.model.book.BookProgress
import com.voicedeutsch.master.domain.model.book.LessonStatus
import com.voicedeutsch.master.domain.model.knowledge.MistakeLog
import com.voicedeutsch.master.domain.model.knowledge.MistakeType
import com.voicedeutsch.master.domain.model.progress.DailyProgress
import com.voicedeutsch.master.domain.model.speech.PronunciationResult
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProgressMapperTest {

    private lateinit var json: Json

    @BeforeEach
    fun setUp() {
        json = Json { ignoreUnknownKeys = true }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildDailyStatisticsEntity(
        id: String = "ds_1",
        userId: String = "user_1",
        date: String = "2024-01-15",
        sessionsCount: Int = 3,
        totalMinutes: Int = 45,
        wordsLearned: Int = 10,
        wordsReviewed: Int = 20,
        exercisesCompleted: Int = 15,
        exercisesCorrect: Int = 12,
        averageScore: Float = 0.8f,
        streakMaintained: Boolean = true,
        createdAt: Long = 1000L,
    ) = DailyStatisticsEntity(
        id = id,
        userId = userId,
        date = date,
        sessionsCount = sessionsCount,
        totalMinutes = totalMinutes,
        wordsLearned = wordsLearned,
        wordsReviewed = wordsReviewed,
        exercisesCompleted = exercisesCompleted,
        exercisesCorrect = exercisesCorrect,
        averageScore = averageScore,
        streakMaintained = streakMaintained,
        createdAt = createdAt,
    )

    private fun buildDailyProgress(
        id: String = "ds_1",
        userId: String = "user_1",
        date: String = "2024-01-15",
        sessionsCount: Int = 3,
        totalMinutes: Int = 45,
        wordsLearned: Int = 10,
        wordsReviewed: Int = 20,
        exercisesCompleted: Int = 15,
        exercisesCorrect: Int = 12,
        averageScore: Float = 0.8f,
        streakMaintained: Boolean = true,
        createdAt: Long = 1000L,
    ) = DailyProgress(
        id = id,
        userId = userId,
        date = date,
        sessionsCount = sessionsCount,
        totalMinutes = totalMinutes,
        wordsLearned = wordsLearned,
        wordsReviewed = wordsReviewed,
        exercisesCompleted = exercisesCompleted,
        exercisesCorrect = exercisesCorrect,
        averageScore = averageScore,
        streakMaintained = streakMaintained,
        createdAt = createdAt,
    )

    private fun buildBookProgressEntity(
        id: String = "bp_1",
        userId: String = "user_1",
        chapter: Int = 2,
        lesson: Int = 3,
        status: String = "IN_PROGRESS",
        score: Float? = 0.75f,
        startedAt: Long? = 500L,
        completedAt: Long? = null,
        timesPracticed: Int = 4,
        notes: String? = "some note",
    ) = BookProgressEntity(
        id = id,
        userId = userId,
        chapter = chapter,
        lesson = lesson,
        status = status,
        score = score,
        startedAt = startedAt,
        completedAt = completedAt,
        timesPracticed = timesPracticed,
        notes = notes,
    )

    private fun buildBookProgress(
        id: String = "bp_1",
        userId: String = "user_1",
        chapter: Int = 2,
        lesson: Int = 3,
        status: LessonStatus = LessonStatus.IN_PROGRESS,
        score: Float = 0.75f,
        startedAt: Long? = 500L,
        completedAt: Long? = null,
        timesPracticed: Int = 4,
        notes: String? = "some note",
    ) = BookProgress(
        id = id,
        userId = userId,
        chapter = chapter,
        lesson = lesson,
        status = status,
        score = score,
        startedAt = startedAt,
        completedAt = completedAt,
        timesPracticed = timesPracticed,
        notes = notes,
    )

    private fun buildPronunciationRecordEntity(
        id: String = "pr_1",
        userId: String = "user_1",
        word: String = "Hund",
        score: Float = 0.9f,
        problemSoundsJson: String = """["ʊ","n"]""",
        attemptNumber: Int = 1,
        sessionId: String? = "ses_1",
        timestamp: Long = 2000L,
        createdAt: Long = 2100L,
    ) = PronunciationRecordEntity(
        id = id,
        userId = userId,
        word = word,
        score = score,
        problemSoundsJson = problemSoundsJson,
        attemptNumber = attemptNumber,
        sessionId = sessionId,
        timestamp = timestamp,
        createdAt = createdAt,
    )

    private fun buildPronunciationResult(
        id: String = "pr_1",
        userId: String = "user_1",
        word: String = "Hund",
        score: Float = 0.9f,
        problemSounds: List<String> = listOf("ʊ", "n"),
        attemptNumber: Int = 1,
        sessionId: String = "ses_1",
        timestamp: Long = 2000L,
        createdAt: Long = 2100L,
    ) = PronunciationResult(
        id = id,
        userId = userId,
        word = word,
        score = score,
        problemSounds = problemSounds,
        attemptNumber = attemptNumber,
        sessionId = sessionId,
        timestamp = timestamp,
        createdAt = createdAt,
    )

    private fun buildMistakeLogEntity(
        id: String = "ml_1",
        userId: String = "user_1",
        sessionId: String = "ses_1",
        type: String = "GRAMMAR",
        item: String = "der/die/das",
        expected: String = "der",
        actual: String = "die",
        context: String = "Ich sehe ___ Hund",
        explanation: String = "Masculine noun",
        timestamp: Long = 3000L,
        createdAt: Long = 3100L,
    ) = MistakeLogEntity(
        id = id,
        userId = userId,
        sessionId = sessionId,
        type = type,
        item = item,
        expected = expected,
        actual = actual,
        context = context,
        explanation = explanation,
        timestamp = timestamp,
        createdAt = createdAt,
    )

    private fun buildMistakeLog(
        id: String = "ml_1",
        userId: String = "user_1",
        sessionId: String = "ses_1",
        type: MistakeType = MistakeType.GRAMMAR,
        item: String = "der/die/das",
        expected: String = "der",
        actual: String = "die",
        context: String = "Ich sehe ___ Hund",
        explanation: String = "Masculine noun",
        timestamp: Long = 3000L,
        createdAt: Long = 3100L,
    ) = MistakeLog(
        id = id,
        userId = userId,
        sessionId = sessionId,
        type = type,
        item = item,
        expected = expected,
        actual = actual,
        context = context,
        explanation = explanation,
        timestamp = timestamp,
        createdAt = createdAt,
    )

    // ── DailyStatisticsEntity.toDomain ────────────────────────────────────────

    @Test
    fun dailyStatisticsEntity_toDomain_validData_mapsAllFields() {
        val entity = buildDailyStatisticsEntity()
        with(ProgressMapper) {
            val domain = entity.toDomain()
            assertEquals(entity.id, domain.id)
            assertEquals(entity.userId, domain.userId)
            assertEquals(entity.date, domain.date)
            assertEquals(entity.sessionsCount, domain.sessionsCount)
            assertEquals(entity.totalMinutes, domain.totalMinutes)
            assertEquals(entity.wordsLearned, domain.wordsLearned)
            assertEquals(entity.wordsReviewed, domain.wordsReviewed)
            assertEquals(entity.exercisesCompleted, domain.exercisesCompleted)
            assertEquals(entity.exercisesCorrect, domain.exercisesCorrect)
            assertEquals(entity.averageScore, domain.averageScore, 0.001f)
            assertEquals(entity.streakMaintained, domain.streakMaintained)
            assertEquals(entity.createdAt, domain.createdAt)
        }
    }

    @Test
    fun dailyStatisticsEntity_toDomain_streakFalse_preservedAsFalse() {
        val entity = buildDailyStatisticsEntity(streakMaintained = false)
        with(ProgressMapper) {
            assertFalse(entity.toDomain().streakMaintained)
        }
    }

    @Test
    fun dailyStatisticsEntity_toDomain_zeroValues_preservedAsZero() {
        val entity = buildDailyStatisticsEntity(
            sessionsCount = 0,
            totalMinutes = 0,
            wordsLearned = 0,
            wordsReviewed = 0,
            exercisesCompleted = 0,
            exercisesCorrect = 0,
            averageScore = 0f,
        )
        with(ProgressMapper) {
            val domain = entity.toDomain()
            assertEquals(0, domain.sessionsCount)
            assertEquals(0, domain.totalMinutes)
            assertEquals(0, domain.wordsLearned)
            assertEquals(0f, domain.averageScore, 0.001f)
        }
    }

    // ── DailyProgress.toEntity ────────────────────────────────────────────────

    @Test
    fun dailyProgress_toEntity_validData_mapsAllFields() {
        val domain = buildDailyProgress()
        with(ProgressMapper) {
            val entity = domain.toEntity()
            assertEquals(domain.id, entity.id)
            assertEquals(domain.userId, entity.userId)
            assertEquals(domain.date, entity.date)
            assertEquals(domain.sessionsCount, entity.sessionsCount)
            assertEquals(domain.totalMinutes, entity.totalMinutes)
            assertEquals(domain.wordsLearned, entity.wordsLearned)
            assertEquals(domain.wordsReviewed, entity.wordsReviewed)
            assertEquals(domain.exercisesCompleted, entity.exercisesCompleted)
            assertEquals(domain.exercisesCorrect, entity.exercisesCorrect)
            assertEquals(domain.averageScore, entity.averageScore, 0.001f)
            assertEquals(domain.streakMaintained, entity.streakMaintained)
            assertEquals(domain.createdAt, entity.createdAt)
        }
    }

    // ── DailyStatistics roundtrip ─────────────────────────────────────────────

    @Test
    fun dailyStatistics_roundtrip_entityToDomainToEntity_fieldsMatch() {
        val original = buildDailyStatisticsEntity()
        with(ProgressMapper) {
            val domain = original.toDomain()
            val restored = domain.toEntity()
            assertEquals(original.id, restored.id)
            assertEquals(original.userId, restored.userId)
            assertEquals(original.date, restored.date)
            assertEquals(original.sessionsCount, restored.sessionsCount)
            assertEquals(original.totalMinutes, restored.totalMinutes)
            assertEquals(original.wordsLearned, restored.wordsLearned)
            assertEquals(original.wordsReviewed, restored.wordsReviewed)
            assertEquals(original.exercisesCompleted, restored.exercisesCompleted)
            assertEquals(original.exercisesCorrect, restored.exercisesCorrect)
            assertEquals(original.averageScore, restored.averageScore, 0.001f)
            assertEquals(original.streakMaintained, restored.streakMaintained)
            assertEquals(original.createdAt, restored.createdAt)
        }
    }

    // ── BookProgressEntity.toDomain ───────────────────────────────────────────

    @Test
    fun bookProgressEntity_toDomain_validStatus_mapsAllFields() {
        val entity = buildBookProgressEntity(status = "IN_PROGRESS")
        with(ProgressMapper) {
            val domain = entity.toDomain()
            assertEquals(entity.id, domain.id)
            assertEquals(entity.userId, domain.userId)
            assertEquals(entity.chapter, domain.chapter)
            assertEquals(entity.lesson, domain.lesson)
            assertEquals(LessonStatus.IN_PROGRESS, domain.status)
            assertEquals(entity.score!!, domain.score!!, 0.001f)
            assertEquals(entity.startedAt, domain.startedAt)
            assertEquals(entity.completedAt, domain.completedAt)
            assertEquals(entity.timesPracticed, domain.timesPracticed)
            assertEquals(entity.notes, domain.notes)
        }
    }

    @Test
    fun bookProgressEntity_toDomain_statusNotStarted_mapsCorrectly() {
        val entity = buildBookProgressEntity(status = "NOT_STARTED")
        with(ProgressMapper) {
            assertEquals(LessonStatus.NOT_STARTED, entity.toDomain().status)
        }
    }

    @Test
    fun bookProgressEntity_toDomain_statusCompleted_mapsCorrectly() {
        val entity = buildBookProgressEntity(status = "COMPLETED")
        with(ProgressMapper) {
            assertEquals(LessonStatus.COMPLETED, entity.toDomain().status)
        }
    }

    @Test
    fun bookProgressEntity_toDomain_invalidStatus_fallsBackToNotStarted() {
        val entity = buildBookProgressEntity(status = "TOTALLY_BOGUS_STATUS")
        with(ProgressMapper) {
            assertEquals(LessonStatus.NOT_STARTED, entity.toDomain().status)
        }
    }

    @Test
    fun bookProgressEntity_toDomain_emptyStatus_fallsBackToNotStarted() {
        val entity = buildBookProgressEntity(status = "")
        with(ProgressMapper) {
            assertEquals(LessonStatus.NOT_STARTED, entity.toDomain().status)
        }
    }

    @Test
    fun bookProgressEntity_toDomain_nullOptionalFields_preservedAsNull() {
        val entity = buildBookProgressEntity(startedAt = null, completedAt = null, notes = null)
        with(ProgressMapper) {
            val domain = entity.toDomain()
            assertNull(domain.startedAt)
            assertNull(domain.completedAt)
            assertNull(domain.notes)
        }
    }

    // ── BookProgress.toEntity ─────────────────────────────────────────────────

    @Test
    fun bookProgress_toEntity_validData_mapsAllFields() {
        val domain = buildBookProgress()
        with(ProgressMapper) {
            val entity = domain.toEntity()
            assertEquals(domain.id, entity.id)
            assertEquals(domain.userId, entity.userId)
            assertEquals(domain.chapter, entity.chapter)
            assertEquals(domain.lesson, entity.lesson)
            assertEquals(domain.status.name, entity.status)
            assertEquals(domain.score!!, entity.score!!, 0.001f)
            assertEquals(domain.startedAt, entity.startedAt)
            assertEquals(domain.completedAt, entity.completedAt)
            assertEquals(domain.timesPracticed, entity.timesPracticed)
            assertEquals(domain.notes, entity.notes)
        }
    }

    @Test
    fun bookProgress_toEntity_statusStoredAsEnumName() {
        LessonStatus.entries.forEach { status ->
            val domain = buildBookProgress(status = status)
            with(ProgressMapper) {
                assertEquals(status.name, domain.toEntity().status)
            }
        }
    }

    // ── BookProgress roundtrip ────────────────────────────────────────────────

    @Test
    fun bookProgress_roundtrip_entityToDomainToEntity_fieldsMatch() {
        val original = buildBookProgressEntity(status = "COMPLETED", completedAt = 9999L)
        with(ProgressMapper) {
            val domain = original.toDomain()
            val restored = domain.toEntity()
            assertEquals(original.id, restored.id)
            assertEquals(original.userId, restored.userId)
            assertEquals(original.chapter, restored.chapter)
            assertEquals(original.lesson, restored.lesson)
            assertEquals(original.status, restored.status)
            assertEquals(original.score!!, restored.score, 0.001f)
            assertEquals(original.startedAt, restored.startedAt)
            assertEquals(original.completedAt, restored.completedAt)
            assertEquals(original.timesPracticed, restored.timesPracticed)
            assertEquals(original.notes, restored.notes)
        }
    }

    @Test
    fun bookProgress_roundtrip_invalidStatus_restoredAsNotStarted() {
        val original = buildBookProgressEntity(status = "INVALID")
        with(ProgressMapper) {
            val domain = original.toDomain()
            assertEquals(LessonStatus.NOT_STARTED, domain.status)
            val restored = domain.toEntity()
            assertEquals(LessonStatus.NOT_STARTED.name, restored.status)
        }
    }

    // ── PronunciationRecordEntity.toDomain ────────────────────────────────────

    @Test
    fun pronunciationRecordEntity_toDomain_validJson_mapsAllFields() {
        val entity = buildPronunciationRecordEntity()
        with(ProgressMapper) {
            val domain = entity.toDomain(json)
            assertEquals(entity.id, domain.id)
            assertEquals(entity.userId, domain.userId)
            assertEquals(entity.word, domain.word)
            assertEquals(entity.score, domain.score, 0.001f)
            assertEquals(listOf("ʊ", "n"), domain.problemSounds)
            assertEquals(entity.attemptNumber, domain.attemptNumber)
            assertEquals(entity.sessionId, domain.sessionId)
            assertEquals(entity.timestamp, domain.timestamp)
            assertEquals(entity.createdAt, domain.createdAt)
        }
    }

    @Test
    fun pronunciationRecordEntity_toDomain_emptyProblemSoundsJson_returnsEmptyList() {
        val entity = buildPronunciationRecordEntity(problemSoundsJson = "")
        with(ProgressMapper) {
            assertEquals(emptyList<String>(), entity.toDomain(json).problemSounds)
        }
    }

    @Test
    fun pronunciationRecordEntity_toDomain_invalidProblemSoundsJson_returnsEmptyList() {
        val entity = buildPronunciationRecordEntity(problemSoundsJson = "{bad_json}")
        with(ProgressMapper) {
            assertEquals(emptyList<String>(), entity.toDomain(json).problemSounds)
        }
    }

    @Test
    fun pronunciationRecordEntity_toDomain_emptyArrayJson_returnsEmptyList() {
        val entity = buildPronunciationRecordEntity(problemSoundsJson = "[]")
        with(ProgressMapper) {
            assertEquals(emptyList<String>(), entity.toDomain(json).problemSounds)
        }
    }

    // ── PronunciationResult.toEntity ──────────────────────────────────────────

    @Test
    fun pronunciationResult_toEntity_validData_mapsAllFields() {
        val domain = buildPronunciationResult()
        with(ProgressMapper) {
            val entity = domain.toEntity(json)
            assertEquals(domain.id, entity.id)
            assertEquals(domain.userId, entity.userId)
            assertEquals(domain.word, entity.word)
            assertEquals(domain.score, entity.score, 0.001f)
            assertEquals(domain.attemptNumber, entity.attemptNumber)
            assertEquals(domain.sessionId, entity.sessionId)
            assertEquals(domain.timestamp, entity.timestamp)
            assertEquals(domain.createdAt, entity.createdAt)
            assertNotNull(entity.problemSoundsJson)
        }
    }

    @Test
    fun pronunciationResult_toEntity_emptyProblemSounds_producesEmptyJsonArray() {
        val domain = buildPronunciationResult(problemSounds = emptyList())
        with(ProgressMapper) {
            assertEquals("[]", domain.toEntity(json).problemSoundsJson)
        }
    }

    // ── PronunciationResult roundtrip ─────────────────────────────────────────

    @Test
    fun pronunciationResult_roundtrip_entityToDomainToEntity_fieldsMatch() {
        val original = buildPronunciationRecordEntity()
        with(ProgressMapper) {
            val domain = original.toDomain(json)
            val restored = domain.toEntity(json)
            assertEquals(original.id, restored.id)
            assertEquals(original.userId, restored.userId)
            assertEquals(original.word, restored.word)
            assertEquals(original.score, restored.score, 0.001f)
            assertEquals(original.attemptNumber, restored.attemptNumber)
            assertEquals(original.sessionId, restored.sessionId)
            assertEquals(original.timestamp, restored.timestamp)
            assertEquals(original.createdAt, restored.createdAt)
        }
    }

    @Test
    fun pronunciationResult_roundtrip_invalidJson_emptyListPreserved() {
        val original = buildPronunciationRecordEntity(problemSoundsJson = "not_valid")
        with(ProgressMapper) {
            val domain = original.toDomain(json)
            assertEquals(emptyList<String>(), domain.problemSounds)
            val restored = domain.toEntity(json)
            assertEquals("[]", restored.problemSoundsJson)
        }
    }

    // ── MistakeLogEntity.toDomain ─────────────────────────────────────────────

    @Test
    fun mistakeLogEntity_toDomain_validType_mapsAllFields() {
        val entity = buildMistakeLogEntity(type = "GRAMMAR")
        with(ProgressMapper) {
            val domain = entity.toDomain()
            assertEquals(entity.id, domain.id)
            assertEquals(entity.userId, domain.userId)
            assertEquals(entity.sessionId, domain.sessionId)
            assertEquals(MistakeType.GRAMMAR, domain.type)
            assertEquals(entity.item, domain.item)
            assertEquals(entity.expected, domain.expected)
            assertEquals(entity.actual, domain.actual)
            assertEquals(entity.context, domain.context)
            assertEquals(entity.explanation, domain.explanation)
            assertEquals(entity.timestamp, domain.timestamp)
            assertEquals(entity.createdAt, domain.createdAt)
        }
    }

    @Test
    fun mistakeLogEntity_toDomain_typeWord_mapsCorrectly() {
        val entity = buildMistakeLogEntity(type = "WORD")
        with(ProgressMapper) {
            assertEquals(MistakeType.WORD, entity.toDomain().type)
        }
    }

    @Test
    fun mistakeLogEntity_toDomain_typeLowercased_parsedCaseInsensitive() {
        val entity = buildMistakeLogEntity(type = "grammar")
        with(ProgressMapper) {
            assertEquals(MistakeType.GRAMMAR, entity.toDomain().type)
        }
    }

    @Test
    fun mistakeLogEntity_toDomain_typeMixedCase_parsedCaseInsensitive() {
        val entity = buildMistakeLogEntity(type = "Grammar")
        with(ProgressMapper) {
            assertEquals(MistakeType.GRAMMAR, entity.toDomain().type)
        }
    }

    @Test
    fun mistakeLogEntity_toDomain_invalidType_fallsBackToWord() {
        val entity = buildMistakeLogEntity(type = "UNKNOWN_TYPE_XYZ")
        with(ProgressMapper) {
            assertEquals(MistakeType.WORD, entity.toDomain().type)
        }
    }

    @Test
    fun mistakeLogEntity_toDomain_emptyType_fallsBackToWord() {
        val entity = buildMistakeLogEntity(type = "")
        with(ProgressMapper) {
            assertEquals(MistakeType.WORD, entity.toDomain().type)
        }
    }

    @Test
    fun mistakeLogEntity_toDomain_nullOptionalFields_preservedAsNull() {
        val entity = buildMistakeLogEntity(context = "", explanation = "")
        with(ProgressMapper) {
            val domain = entity.toDomain()
            assertEquals("", domain.context)
            assertEquals("", domain.explanation)
        }
    }

    // ── MistakeLog.toEntity ───────────────────────────────────────────────────

    @Test
    fun mistakeLog_toEntity_validData_mapsAllFields() {
        val domain = buildMistakeLog()
        with(ProgressMapper) {
            val entity = domain.toEntity()
            assertEquals(domain.id, entity.id)
            assertEquals(domain.userId, entity.userId)
            assertEquals(domain.sessionId, entity.sessionId)
            assertEquals(domain.type.name, entity.type)
            assertEquals(domain.item, entity.item)
            assertEquals(domain.expected, entity.expected)
            assertEquals(domain.actual, entity.actual)
            assertEquals(domain.context, entity.context)
            assertEquals(domain.explanation, entity.explanation)
            assertEquals(domain.timestamp, entity.timestamp)
            assertEquals(domain.createdAt, entity.createdAt)
        }
    }

    @Test
    fun mistakeLog_toEntity_typeStoredAsEnumName() {
        MistakeType.entries.forEach { type ->
            val domain = buildMistakeLog(type = type)
            with(ProgressMapper) {
                assertEquals(type.name, domain.toEntity().type)
            }
        }
    }

    // ── MistakeLog roundtrip ──────────────────────────────────────────────────

    @Test
    fun mistakeLog_roundtrip_entityToDomainToEntity_fieldsMatch() {
        val original = buildMistakeLogEntity()
        with(ProgressMapper) {
            val domain = original.toDomain()
            val restored = domain.toEntity()
            assertEquals(original.id, restored.id)
            assertEquals(original.userId, restored.userId)
            assertEquals(original.sessionId, restored.sessionId)
            assertEquals(original.type, restored.type)
            assertEquals(original.item, restored.item)
            assertEquals(original.expected, restored.expected)
            assertEquals(original.actual, restored.actual)
            assertEquals(original.context, restored.context)
            assertEquals(original.explanation, restored.explanation)
            assertEquals(original.timestamp, restored.timestamp)
            assertEquals(original.createdAt, restored.createdAt)
        }
    }

    @Test
    fun mistakeLog_roundtrip_invalidType_restoredAsWord() {
        val original = buildMistakeLogEntity(type = "NONSENSE")
        with(ProgressMapper) {
            val domain = original.toDomain()
            assertEquals(MistakeType.WORD, domain.type)
            val restored = domain.toEntity()
            assertEquals(MistakeType.WORD.name, restored.type)
        }
    }

    @Test
    fun mistakeLog_roundtrip_nullFields_preservedAsNull() {
        val original = buildMistakeLogEntity(context = "", explanation = "")
        with(ProgressMapper) {
            val domain = original.toDomain()
            val restored = domain.toEntity()
            assertEquals("", restored.context)
            assertEquals("", restored.explanation)
        }
    }
}
