// Путь: src/test/java/com/voicedeutsch/master/data/mapper/SessionMapperTest.kt
package com.voicedeutsch.master.data.mapper

import com.voicedeutsch.master.data.local.database.entity.SessionEntity
import com.voicedeutsch.master.data.local.database.entity.SessionEventEntity
import com.voicedeutsch.master.domain.model.session.LearningSession
import com.voicedeutsch.master.domain.model.session.MoodEstimate
import com.voicedeutsch.master.domain.model.session.SessionEvent
import com.voicedeutsch.master.domain.model.session.SessionEventType
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SessionMapperTest {

    private lateinit var json: Json

    @BeforeEach
    fun setUp() {
        json = Json { ignoreUnknownKeys = true }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildSessionEntity(
        id: String = "ses_1",
        userId: String = "user_1",
        startedAt: Long = 1000L,
        endedAt: Long? = 2000L,
        durationMinutes: Int = 15,
        strategiesUsedJson: String = """["VOCABULARY","GRAMMAR"]""",
        wordsLearned: Int = 5,
        wordsReviewed: Int = 10,
        rulesPracticed: Int = 3,
        exercisesCompleted: Int = 8,
        exercisesCorrect: Int = 6,
        averagePronunciationScore: Float = 0.85f,
        bookChapterStart: Int? = 1,
        bookLessonStart: Int? = 2,
        bookChapterEnd: Int? = 1,
        bookLessonEnd: Int? = 3,
        sessionSummary: String? = "Good session",
        moodEstimate: String? = "GOOD",
        createdAt: Long = 500L,
    ) = SessionEntity(
        id = id,
        userId = userId,
        startedAt = startedAt,
        endedAt = endedAt,
        durationMinutes = durationMinutes,
        strategiesUsedJson = strategiesUsedJson,
        wordsLearned = wordsLearned,
        wordsReviewed = wordsReviewed,
        rulesPracticed = rulesPracticed,
        exercisesCompleted = exercisesCompleted,
        exercisesCorrect = exercisesCorrect,
        averagePronunciationScore = averagePronunciationScore,
        bookChapterStart = bookChapterStart,
        bookLessonStart = bookLessonStart,
        bookChapterEnd = bookChapterEnd,
        bookLessonEnd = bookLessonEnd,
        sessionSummary = sessionSummary,
        moodEstimate = moodEstimate,
        createdAt = createdAt,
    )

    private fun buildLearningSession(
        id: String = "ses_1",
        userId: String = "user_1",
        startedAt: Long = 1000L,
        endedAt: Long? = 2000L,
        durationMinutes: Int = 15,
        strategiesUsed: List<String> = listOf("VOCABULARY", "GRAMMAR"),
        wordsLearned: Int = 5,
        wordsReviewed: Int = 10,
        rulesPracticed: Int = 3,
        exercisesCompleted: Int = 8,
        exercisesCorrect: Int = 6,
        averagePronunciationScore: Float = 0.85f,
        bookChapterStart: Int? = 1,
        bookLessonStart: Int? = 2,
        bookChapterEnd: Int? = 1,
        bookLessonEnd: Int? = 3,
        sessionSummary: String? = "Good session",
        moodEstimate: MoodEstimate? = MoodEstimate.GOOD,
        createdAt: Long = 500L,
    ) = LearningSession(
        id = id,
        userId = userId,
        startedAt = startedAt,
        endedAt = endedAt,
        durationMinutes = durationMinutes,
        strategiesUsed = strategiesUsed,
        wordsLearned = wordsLearned,
        wordsReviewed = wordsReviewed,
        rulesPracticed = rulesPracticed,
        exercisesCompleted = exercisesCompleted,
        exercisesCorrect = exercisesCorrect,
        averagePronunciationScore = averagePronunciationScore,
        bookChapterStart = bookChapterStart,
        bookLessonStart = bookLessonStart,
        bookChapterEnd = bookChapterEnd,
        bookLessonEnd = bookLessonEnd,
        sessionSummary = sessionSummary,
        moodEstimate = moodEstimate,
        createdAt = createdAt,
    )

    private fun buildSessionEventEntity(
        id: String = "evt_1",
        sessionId: String = "ses_1",
        eventType: String = "WORD_ANSWERED",
        timestamp: Long = 1500L,
        detailsJson: String? = """{"wordId":"word_1"}""",
        createdAt: Long = 1600L,
    ) = SessionEventEntity(
        id = id,
        sessionId = sessionId,
        eventType = eventType,
        timestamp = timestamp,
        detailsJson = detailsJson,
        createdAt = createdAt,
    )

    private fun buildSessionEvent(
        id: String = "evt_1",
        sessionId: String = "ses_1",
        eventType: SessionEventType = SessionEventType.WORD_ANSWERED,
        timestamp: Long = 1500L,
        detailsJson: String? = """{"wordId":"word_1"}""",
        createdAt: Long = 1600L,
    ) = SessionEvent(
        id = id,
        sessionId = sessionId,
        eventType = eventType,
        timestamp = timestamp,
        detailsJson = detailsJson,
        createdAt = createdAt,
    )

    // ── SessionEntity.toDomain ────────────────────────────────────────────────

    @Test
    fun sessionEntity_toDomain_validData_mapsAllFields() {
        val entity = buildSessionEntity()
        with(SessionMapper) {
            val domain = entity.toDomain(json)
            assertEquals(entity.id, domain.id)
            assertEquals(entity.userId, domain.userId)
            assertEquals(entity.startedAt, domain.startedAt)
            assertEquals(entity.endedAt, domain.endedAt)
            assertEquals(entity.durationMinutes, domain.durationMinutes)
            assertEquals(listOf("VOCABULARY", "GRAMMAR"), domain.strategiesUsed)
            assertEquals(entity.wordsLearned, domain.wordsLearned)
            assertEquals(entity.wordsReviewed, domain.wordsReviewed)
            assertEquals(entity.rulesPracticed, domain.rulesPracticed)
            assertEquals(entity.exercisesCompleted, domain.exercisesCompleted)
            assertEquals(entity.exercisesCorrect, domain.exercisesCorrect)
            assertEquals(entity.averagePronunciationScore, domain.averagePronunciationScore, 0.001f)
            assertEquals(entity.bookChapterStart, domain.bookChapterStart)
            assertEquals(entity.bookLessonStart, domain.bookLessonStart)
            assertEquals(entity.bookChapterEnd, domain.bookChapterEnd)
            assertEquals(entity.bookLessonEnd, domain.bookLessonEnd)
            assertEquals(entity.sessionSummary, domain.sessionSummary)
            assertEquals(MoodEstimate.GOOD, domain.moodEstimate)
            assertEquals(entity.createdAt, domain.createdAt)
        }
    }

    @Test
    fun sessionEntity_toDomain_validStrategiesJson_parsedToList() {
        val entity = buildSessionEntity(strategiesUsedJson = """["VOCABULARY","PRONUNCIATION"]""")
        with(SessionMapper) {
            assertEquals(listOf("VOCABULARY", "PRONUNCIATION"), entity.toDomain(json).strategiesUsed)
        }
    }

    @Test
    fun sessionEntity_toDomain_emptyStrategiesJson_returnsEmptyList() {
        val entity = buildSessionEntity(strategiesUsedJson = "")
        with(SessionMapper) {
            assertEquals(emptyList<String>(), entity.toDomain(json).strategiesUsed)
        }
    }

    @Test
    fun sessionEntity_toDomain_invalidStrategiesJson_returnsEmptyList() {
        val entity = buildSessionEntity(strategiesUsedJson = "{not_valid}")
        with(SessionMapper) {
            assertEquals(emptyList<String>(), entity.toDomain(json).strategiesUsed)
        }
    }

    @Test
    fun sessionEntity_toDomain_emptyArrayStrategiesJson_returnsEmptyList() {
        val entity = buildSessionEntity(strategiesUsedJson = "[]")
        with(SessionMapper) {
            assertEquals(emptyList<String>(), entity.toDomain(json).strategiesUsed)
        }
    }

    @Test
    fun sessionEntity_toDomain_validMoodEstimate_mapsCorrectly() {
        MoodEstimate.entries.forEach { mood ->
            val entity = buildSessionEntity(moodEstimate = mood.name)
            with(SessionMapper) {
                assertEquals(mood, entity.toDomain(json).moodEstimate)
            }
        }
    }

    @Test
    fun sessionEntity_toDomain_nullMoodEstimate_preservedAsNull() {
        val entity = buildSessionEntity(moodEstimate = null)
        with(SessionMapper) {
            assertNull(entity.toDomain(json).moodEstimate)
        }
    }

    @Test
    fun sessionEntity_toDomain_invalidMoodEstimate_returnsNull() {
        val entity = buildSessionEntity(moodEstimate = "TOTALLY_BOGUS_MOOD")
        with(SessionMapper) {
            assertNull(entity.toDomain(json).moodEstimate)
        }
    }

    @Test
    fun sessionEntity_toDomain_emptyMoodEstimate_returnsNull() {
        val entity = buildSessionEntity(moodEstimate = "")
        with(SessionMapper) {
            assertNull(entity.toDomain(json).moodEstimate)
        }
    }

    @Test
    fun sessionEntity_toDomain_nullEndedAt_preservedAsNull() {
        val entity = buildSessionEntity(endedAt = null)
        with(SessionMapper) {
            assertNull(entity.toDomain(json).endedAt)
        }
    }

    @Test
    fun sessionEntity_toDomain_nullBookFields_preservedAsNull() {
        val entity = buildSessionEntity(
            bookChapterStart = null,
            bookLessonStart = null,
            bookChapterEnd = null,
            bookLessonEnd = null,
        )
        with(SessionMapper) {
            val domain = entity.toDomain(json)
            assertNull(domain.bookChapterStart)
            assertNull(domain.bookLessonStart)
            assertNull(domain.bookChapterEnd)
            assertNull(domain.bookLessonEnd)
        }
    }

    @Test
    fun sessionEntity_toDomain_nullSessionSummary_preservedAsNull() {
        val entity = buildSessionEntity(sessionSummary = null)
        with(SessionMapper) {
            assertNull(entity.toDomain(json).sessionSummary)
        }
    }

    // ── LearningSession.toEntity ──────────────────────────────────────────────

    @Test
    fun learningSession_toEntity_validData_mapsAllFields() {
        val domain = buildLearningSession()
        with(SessionMapper) {
            val entity = domain.toEntity(json)
            assertEquals(domain.id, entity.id)
            assertEquals(domain.userId, entity.userId)
            assertEquals(domain.startedAt, entity.startedAt)
            assertEquals(domain.endedAt, entity.endedAt)
            assertEquals(domain.durationMinutes, entity.durationMinutes)
            assertEquals(domain.wordsLearned, entity.wordsLearned)
            assertEquals(domain.wordsReviewed, entity.wordsReviewed)
            assertEquals(domain.rulesPracticed, entity.rulesPracticed)
            assertEquals(domain.exercisesCompleted, entity.exercisesCompleted)
            assertEquals(domain.exercisesCorrect, entity.exercisesCorrect)
            assertEquals(domain.averagePronunciationScore, entity.averagePronunciationScore, 0.001f)
            assertEquals(domain.bookChapterStart, entity.bookChapterStart)
            assertEquals(domain.bookLessonStart, entity.bookLessonStart)
            assertEquals(domain.bookChapterEnd, entity.bookChapterEnd)
            assertEquals(domain.bookLessonEnd, entity.bookLessonEnd)
            assertEquals(domain.sessionSummary, entity.sessionSummary)
            assertEquals(domain.moodEstimate?.name, entity.moodEstimate)
            assertEquals(domain.createdAt, entity.createdAt)
            assertNotNull(entity.strategiesUsedJson)
        }
    }

    @Test
    fun learningSession_toEntity_emptyStrategies_producesEmptyJsonArray() {
        val domain = buildLearningSession(strategiesUsed = emptyList())
        with(SessionMapper) {
            assertEquals("[]", domain.toEntity(json).strategiesUsedJson)
        }
    }

    @Test
    fun learningSession_toEntity_nullMoodEstimate_storedAsNull() {
        val domain = buildLearningSession(moodEstimate = null)
        with(SessionMapper) {
            assertNull(domain.toEntity(json).moodEstimate)
        }
    }

    @Test
    fun learningSession_toEntity_moodEstimateStoredAsEnumName() {
        MoodEstimate.entries.forEach { mood ->
            val domain = buildLearningSession(moodEstimate = mood)
            with(SessionMapper) {
                assertEquals(mood.name, domain.toEntity(json).moodEstimate)
            }
        }
    }

    @Test
    fun learningSession_toEntity_nullBookFields_preservedAsNull() {
        val domain = buildLearningSession(
            bookChapterStart = null,
            bookLessonStart = null,
            bookChapterEnd = null,
            bookLessonEnd = null,
        )
        with(SessionMapper) {
            val entity = domain.toEntity(json)
            assertNull(entity.bookChapterStart)
            assertNull(entity.bookLessonStart)
            assertNull(entity.bookChapterEnd)
            assertNull(entity.bookLessonEnd)
        }
    }

    @Test
    fun learningSession_toEntity_nullEndedAt_preservedAsNull() {
        val domain = buildLearningSession(endedAt = null)
        with(SessionMapper) {
            assertNull(domain.toEntity(json).endedAt)
        }
    }

    // ── LearningSession roundtrip ─────────────────────────────────────────────

    @Test
    fun learningSession_roundtrip_entityToDomainToEntity_scalarFieldsMatch() {
        val original = buildSessionEntity()
        with(SessionMapper) {
            val domain = original.toDomain(json)
            val restored = domain.toEntity(json)
            assertEquals(original.id, restored.id)
            assertEquals(original.userId, restored.userId)
            assertEquals(original.startedAt, restored.startedAt)
            assertEquals(original.endedAt, restored.endedAt)
            assertEquals(original.durationMinutes, restored.durationMinutes)
            assertEquals(original.wordsLearned, restored.wordsLearned)
            assertEquals(original.wordsReviewed, restored.wordsReviewed)
            assertEquals(original.rulesPracticed, restored.rulesPracticed)
            assertEquals(original.exercisesCompleted, restored.exercisesCompleted)
            assertEquals(original.exercisesCorrect, restored.exercisesCorrect)
            assertEquals(original.averagePronunciationScore, restored.averagePronunciationScore, 0.001f)
            assertEquals(original.moodEstimate, restored.moodEstimate)
            assertEquals(original.sessionSummary, restored.sessionSummary)
            assertEquals(original.createdAt, restored.createdAt)
        }
    }

    @Test
    fun learningSession_roundtrip_strategiesPreserved() {
        val original = buildSessionEntity(strategiesUsedJson = """["A","B","C"]""")
        with(SessionMapper) {
            val domain = original.toDomain(json)
            assertEquals(listOf("A", "B", "C"), domain.strategiesUsed)
            val restored = domain.toEntity(json)
            val restoredDomain = restored.toDomain(json)
            assertEquals(domain.strategiesUsed, restoredDomain.strategiesUsed)
        }
    }

    @Test
    fun learningSession_roundtrip_invalidStrategiesJson_emptyListPreserved() {
        val original = buildSessionEntity(strategiesUsedJson = "not_valid_json")
        with(SessionMapper) {
            val domain = original.toDomain(json)
            assertEquals(emptyList<String>(), domain.strategiesUsed)
            val restored = domain.toEntity(json)
            assertEquals("[]", restored.strategiesUsedJson)
        }
    }

    @Test
    fun learningSession_roundtrip_invalidMoodEstimate_restoredAsNull() {
        val original = buildSessionEntity(moodEstimate = "NONSENSE_MOOD")
        with(SessionMapper) {
            val domain = original.toDomain(json)
            assertNull(domain.moodEstimate)
            val restored = domain.toEntity(json)
            assertNull(restored.moodEstimate)
        }
    }

    @Test
    fun learningSession_roundtrip_nullOptionalFields_preservedAsNull() {
        val original = buildSessionEntity(
            endedAt = null,
            sessionSummary = null,
            moodEstimate = null,
            bookChapterStart = null,
            bookLessonStart = null,
            bookChapterEnd = null,
            bookLessonEnd = null,
        )
        with(SessionMapper) {
            val domain = original.toDomain(json)
            val restored = domain.toEntity(json)
            assertNull(restored.endedAt)
            assertNull(restored.sessionSummary)
            assertNull(restored.moodEstimate)
            assertNull(restored.bookChapterStart)
            assertNull(restored.bookLessonStart)
            assertNull(restored.bookChapterEnd)
            assertNull(restored.bookLessonEnd)
        }
    }

    // ── SessionEventEntity.toDomain ───────────────────────────────────────────

    @Test
    fun sessionEventEntity_toDomain_validData_mapsAllFields() {
        val entity = buildSessionEventEntity()
        with(SessionMapper) {
            val domain = entity.toDomain()
            assertEquals(entity.id, domain.id)
            assertEquals(entity.sessionId, domain.sessionId)
            assertEquals(SessionEventType.WORD_ANSWERED, domain.eventType)
            assertEquals(entity.timestamp, domain.timestamp)
            assertEquals(entity.detailsJson, domain.detailsJson)
            assertEquals(entity.createdAt, domain.createdAt)
        }
    }

    @Test
    fun sessionEventEntity_toDomain_allValidEventTypes_mappedCorrectly() {
        SessionEventType.entries.forEach { expected ->
            val entity = buildSessionEventEntity(eventType = expected.name)
            with(SessionMapper) {
                assertEquals(expected, entity.toDomain().eventType)
            }
        }
    }

    @Test
    fun sessionEventEntity_toDomain_invalidEventType_fallsBackToUserRequest() {
        val entity = buildSessionEventEntity(eventType = "TOTALLY_UNKNOWN_EVENT")
        with(SessionMapper) {
            assertEquals(SessionEventType.USER_REQUEST, entity.toDomain().eventType)
        }
    }

    @Test
    fun sessionEventEntity_toDomain_emptyEventType_fallsBackToUserRequest() {
        val entity = buildSessionEventEntity(eventType = "")
        with(SessionMapper) {
            assertEquals(SessionEventType.USER_REQUEST, entity.toDomain().eventType)
        }
    }

    @Test
    fun sessionEventEntity_toDomain_nullDetailsJson_preservedAsNull() {
        val entity = buildSessionEventEntity(detailsJson = null)
        with(SessionMapper) {
            assertNull(entity.toDomain().detailsJson)
        }
    }

    @Test
    fun sessionEventEntity_toDomain_nonNullDetailsJson_preservedAsIs() {
        val details = """{"key":"value","count":42}"""
        val entity = buildSessionEventEntity(detailsJson = details)
        with(SessionMapper) {
            assertEquals(details, entity.toDomain().detailsJson)
        }
    }

    // ── SessionEvent.toEntity ─────────────────────────────────────────────────

    @Test
    fun sessionEvent_toEntity_validData_mapsAllFields() {
        val domain = buildSessionEvent()
        with(SessionMapper) {
            val entity = domain.toEntity()
            assertEquals(domain.id, entity.id)
            assertEquals(domain.sessionId, entity.sessionId)
            assertEquals(domain.eventType.name, entity.eventType)
            assertEquals(domain.timestamp, entity.timestamp)
            assertEquals(domain.detailsJson, entity.detailsJson)
            assertEquals(domain.createdAt, entity.createdAt)
        }
    }

    @Test
    fun sessionEvent_toEntity_eventTypeStoredAsEnumName() {
        SessionEventType.entries.forEach { type ->
            val domain = buildSessionEvent(eventType = type)
            with(SessionMapper) {
                assertEquals(type.name, domain.toEntity().eventType)
            }
        }
    }

    @Test
    fun sessionEvent_toEntity_nullDetailsJson_preservedAsNull() {
        val domain = buildSessionEvent(detailsJson = null)
        with(SessionMapper) {
            assertNull(domain.toEntity().detailsJson)
        }
    }

    // ── SessionEvent roundtrip ────────────────────────────────────────────────

    @Test
    fun sessionEvent_roundtrip_entityToDomainToEntity_fieldsMatch() {
        val original = buildSessionEventEntity()
        with(SessionMapper) {
            val domain = original.toDomain()
            val restored = domain.toEntity()
            assertEquals(original.id, restored.id)
            assertEquals(original.sessionId, restored.sessionId)
            assertEquals(original.eventType, restored.eventType)
            assertEquals(original.timestamp, restored.timestamp)
            assertEquals(original.detailsJson, restored.detailsJson)
            assertEquals(original.createdAt, restored.createdAt)
        }
    }

    @Test
    fun sessionEvent_roundtrip_invalidEventType_restoredAsUserRequest() {
        val original = buildSessionEventEntity(eventType = "GARBAGE_TYPE")
        with(SessionMapper) {
            val domain = original.toDomain()
            assertEquals(SessionEventType.USER_REQUEST, domain.eventType)
            val restored = domain.toEntity()
            assertEquals(SessionEventType.USER_REQUEST.name, restored.eventType)
        }
    }

    @Test
    fun sessionEvent_roundtrip_nullDetailsJson_preservedAsNull() {
        val original = buildSessionEventEntity(detailsJson = null)
        with(SessionMapper) {
            val domain = original.toDomain()
            assertNull(domain.detailsJson)
            val restored = domain.toEntity()
            assertNull(restored.detailsJson)
        }
    }
}
