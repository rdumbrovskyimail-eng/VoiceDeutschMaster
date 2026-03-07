// Путь: src/test/java/com/voicedeutsch/master/data/mapper/KnowledgeMapperTest.kt
package com.voicedeutsch.master.data.mapper

import com.voicedeutsch.master.data.local.database.entity.PhraseKnowledgeEntity
import com.voicedeutsch.master.data.local.database.entity.PronunciationRecordEntity
import com.voicedeutsch.master.data.local.database.entity.RuleKnowledgeEntity
import com.voicedeutsch.master.data.local.database.entity.WordKnowledgeEntity
import com.voicedeutsch.master.domain.model.knowledge.MistakeRecord
import com.voicedeutsch.master.domain.model.knowledge.PhraseKnowledge
import com.voicedeutsch.master.domain.model.knowledge.RuleKnowledge
import com.voicedeutsch.master.domain.model.knowledge.WordKnowledge
import com.voicedeutsch.master.domain.model.speech.PronunciationResult
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KnowledgeMapperTest {

    private lateinit var json: Json

    @BeforeEach
    fun setUp() {
        json = Json { ignoreUnknownKeys = true }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildWordKnowledgeEntity(
        id: String = "wk_1",
        userId: String = "user_1",
        wordId: String = "word_1",
        knowledgeLevel: Int = 2,
        timesSeen: Int = 5,
        timesCorrect: Int = 3,
        timesIncorrect: Int = 2,
        lastSeen: Long? = 1000L,
        lastCorrect: Long? = 900L,
        lastIncorrect: Long? = 800L,
        nextReview: Long? = 2000L,
        srsIntervalDays: Int = 3,
        srsEaseFactor: Float = 2.5f,
        pronunciationScore: Float = 0.8f,
        pronunciationAttempts: Int = 4,
        contextsJson: String = """["ctx1","ctx2"]""",
        mistakesJson: String = "[]",
        createdAt: Long = 100L,
        updatedAt: Long = 200L,
    ) = WordKnowledgeEntity(
        id = id,
        userId = userId,
        wordId = wordId,
        knowledgeLevel = knowledgeLevel,
        timesSeen = timesSeen,
        timesCorrect = timesCorrect,
        timesIncorrect = timesIncorrect,
        lastSeen = lastSeen,
        lastCorrect = lastCorrect,
        lastIncorrect = lastIncorrect,
        nextReview = nextReview,
        srsIntervalDays = srsIntervalDays,
        srsEaseFactor = srsEaseFactor,
        pronunciationScore = pronunciationScore,
        pronunciationAttempts = pronunciationAttempts,
        contextsJson = contextsJson,
        mistakesJson = mistakesJson,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun buildWordKnowledge(
        id: String = "wk_1",
        userId: String = "user_1",
        wordId: String = "word_1",
        knowledgeLevel: Int = 2,
        timesSeen: Int = 5,
        timesCorrect: Int = 3,
        timesIncorrect: Int = 2,
        lastSeen: Long? = 1000L,
        lastCorrect: Long? = 900L,
        lastIncorrect: Long? = 800L,
        nextReview: Long? = 2000L,
        srsIntervalDays: Int = 3,
        srsEaseFactor: Float = 2.5f,
        pronunciationScore: Float = 0.8f,
        pronunciationAttempts: Int = 4,
        contexts: List<String> = listOf("ctx1", "ctx2"),
        mistakes: List<MistakeRecord> = emptyList(),
        createdAt: Long = 100L,
        updatedAt: Long = 200L,
    ) = WordKnowledge(
        id = id,
        userId = userId,
        wordId = wordId,
        knowledgeLevel = knowledgeLevel,
        timesSeen = timesSeen,
        timesCorrect = timesCorrect,
        timesIncorrect = timesIncorrect,
        lastSeen = lastSeen,
        lastCorrect = lastCorrect,
        lastIncorrect = lastIncorrect,
        nextReview = nextReview,
        srsIntervalDays = srsIntervalDays,
        srsEaseFactor = srsEaseFactor,
        pronunciationScore = pronunciationScore,
        pronunciationAttempts = pronunciationAttempts,
        contexts = contexts,
        mistakes = mistakes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun buildRuleKnowledgeEntity(
        id: String = "rk_1",
        userId: String = "user_1",
        ruleId: String = "rule_1",
        knowledgeLevel: Int = 1,
        timesPracticed: Int = 4,
        timesCorrect: Int = 3,
        timesIncorrect: Int = 1,
        lastPracticed: Long? = 500L,
        nextReview: Long? = 1500L,
        srsIntervalDays: Int = 2,
        srsEaseFactor: Float = 2.3f,
        commonMistakesJson: String = """["mistake1"]""",
        createdAt: Long = 50L,
        updatedAt: Long = 150L,
    ) = RuleKnowledgeEntity(
        id = id,
        userId = userId,
        ruleId = ruleId,
        knowledgeLevel = knowledgeLevel,
        timesPracticed = timesPracticed,
        timesCorrect = timesCorrect,
        timesIncorrect = timesIncorrect,
        lastPracticed = lastPracticed,
        nextReview = nextReview,
        srsIntervalDays = srsIntervalDays,
        srsEaseFactor = srsEaseFactor,
        commonMistakesJson = commonMistakesJson,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun buildRuleKnowledge(
        id: String = "rk_1",
        userId: String = "user_1",
        ruleId: String = "rule_1",
        knowledgeLevel: Int = 1,
        timesPracticed: Int = 4,
        timesCorrect: Int = 3,
        timesIncorrect: Int = 1,
        lastPracticed: Long? = 500L,
        nextReview: Long? = 1500L,
        srsIntervalDays: Int = 2,
        srsEaseFactor: Float = 2.3f,
        commonMistakes: List<String> = listOf("mistake1"),
        createdAt: Long = 50L,
        updatedAt: Long = 150L,
    ) = RuleKnowledge(
        id = id,
        userId = userId,
        ruleId = ruleId,
        knowledgeLevel = knowledgeLevel,
        timesPracticed = timesPracticed,
        timesCorrect = timesCorrect,
        timesIncorrect = timesIncorrect,
        lastPracticed = lastPracticed,
        nextReview = nextReview,
        srsIntervalDays = srsIntervalDays,
        srsEaseFactor = srsEaseFactor,
        commonMistakes = commonMistakes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun buildPhraseKnowledgeEntity(
        id: String = "pk_1",
        userId: String = "user_1",
        phraseId: String = "phrase_1",
        knowledgeLevel: Int = 3,
        timesPracticed: Int = 6,
        timesCorrect: Int = 5,
        lastPracticed: Long? = 700L,
        nextReview: Long? = 1700L,
        srsIntervalDays: Int = 4,
        srsEaseFactor: Float = 2.6f,
        pronunciationScore: Float = 0.9f,
        createdAt: Long = 60L,
        updatedAt: Long = 160L,
    ) = PhraseKnowledgeEntity(
        id = id,
        userId = userId,
        phraseId = phraseId,
        knowledgeLevel = knowledgeLevel,
        timesPracticed = timesPracticed,
        timesCorrect = timesCorrect,
        lastPracticed = lastPracticed,
        nextReview = nextReview,
        srsIntervalDays = srsIntervalDays,
        srsEaseFactor = srsEaseFactor,
        pronunciationScore = pronunciationScore,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun buildPhraseKnowledge(
        id: String = "pk_1",
        userId: String = "user_1",
        phraseId: String = "phrase_1",
        knowledgeLevel: Int = 3,
        timesPracticed: Int = 6,
        timesCorrect: Int = 5,
        lastPracticed: Long? = 700L,
        nextReview: Long? = 1700L,
        srsIntervalDays: Int = 4,
        srsEaseFactor: Float = 2.6f,
        pronunciationScore: Float = 0.9f,
        createdAt: Long = 60L,
        updatedAt: Long = 160L,
    ) = PhraseKnowledge(
        id = id,
        userId = userId,
        phraseId = phraseId,
        knowledgeLevel = knowledgeLevel,
        timesPracticed = timesPracticed,
        timesCorrect = timesCorrect,
        lastPracticed = lastPracticed,
        nextReview = nextReview,
        srsIntervalDays = srsIntervalDays,
        srsEaseFactor = srsEaseFactor,
        pronunciationScore = pronunciationScore,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun buildPronunciationRecordEntity(
        id: String = "pr_1",
        userId: String = "user_1",
        word: String = "Hund",
        score: Float = 0.75f,
        problemSoundsJson: String = """["ʊ","n"]""",
        attemptNumber: Int = 2,
        sessionId: String = "session_42",
        timestamp: Long = 9999L,
        createdAt: Long = 10000L,
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
        score: Float = 0.75f,
        problemSounds: List<String> = listOf("ʊ", "n"),
        attemptNumber: Int = 2,
        sessionId: String? = "session_42",
        timestamp: Long = 9999L,
    ) = PronunciationResult(
        id = id,
        userId = userId,
        word = word,
        score = score,
        problemSounds = problemSounds,
        attemptNumber = attemptNumber,
        sessionId = sessionId,
        timestamp = timestamp,
    )

    // ── WordKnowledgeEntity.toDomain ──────────────────────────────────────────

    @Test
    fun wordKnowledgeEntity_toDomain_validJson_mapsAllFields() {
        val entity = buildWordKnowledgeEntity()
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertEquals(entity.id, domain.id)
            assertEquals(entity.userId, domain.userId)
            assertEquals(entity.wordId, domain.wordId)
            assertEquals(entity.knowledgeLevel, domain.knowledgeLevel)
            assertEquals(entity.timesSeen, domain.timesSeen)
            assertEquals(entity.timesCorrect, domain.timesCorrect)
            assertEquals(entity.timesIncorrect, domain.timesIncorrect)
            assertEquals(entity.lastSeen, domain.lastSeen)
            assertEquals(entity.lastCorrect, domain.lastCorrect)
            assertEquals(entity.lastIncorrect, domain.lastIncorrect)
            assertEquals(entity.nextReview, domain.nextReview)
            assertEquals(entity.srsIntervalDays, domain.srsIntervalDays)
            assertEquals(entity.srsEaseFactor, domain.srsEaseFactor, 0.001f)
            assertEquals(entity.pronunciationScore, domain.pronunciationScore, 0.001f)
            assertEquals(entity.pronunciationAttempts, domain.pronunciationAttempts)
            assertEquals(listOf("ctx1", "ctx2"), domain.contexts)
            assertEquals(emptyList<MistakeRecord>(), domain.mistakes)
            assertEquals(entity.createdAt, domain.createdAt)
            assertEquals(entity.updatedAt, domain.updatedAt)
        }
    }

    @Test
    fun wordKnowledgeEntity_toDomain_emptyContextsJson_returnsEmptyList() {
        val entity = buildWordKnowledgeEntity(contextsJson = "")
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertEquals(emptyList<String>(), domain.contexts)
        }
    }

    @Test
    fun wordKnowledgeEntity_toDomain_blankContextsJson_returnsEmptyList() {
        val entity = buildWordKnowledgeEntity(contextsJson = "   ")
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertEquals(emptyList<String>(), domain.contexts)
        }
    }

    @Test
    fun wordKnowledgeEntity_toDomain_invalidContextsJson_returnsEmptyList() {
        val entity = buildWordKnowledgeEntity(contextsJson = "{not_valid}")
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertEquals(emptyList<String>(), domain.contexts)
        }
    }

    @Test
    fun wordKnowledgeEntity_toDomain_emptyMistakesJson_returnsEmptyList() {
        val entity = buildWordKnowledgeEntity(mistakesJson = "")
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertEquals(emptyList<MistakeRecord>(), domain.mistakes)
        }
    }

    @Test
    fun wordKnowledgeEntity_toDomain_invalidMistakesJson_returnsEmptyList() {
        val entity = buildWordKnowledgeEntity(mistakesJson = "!!!invalid!!!")
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertEquals(emptyList<MistakeRecord>(), domain.mistakes)
        }
    }

    @Test
    fun wordKnowledgeEntity_toDomain_nullOptionalTimestamps_preservedAsNull() {
        val entity = buildWordKnowledgeEntity(lastSeen = null, lastCorrect = null, lastIncorrect = null, nextReview = null)
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertNull(domain.lastSeen)
            assertNull(domain.lastCorrect)
            assertNull(domain.lastIncorrect)
            assertNull(domain.nextReview)
        }
    }

    // ── WordKnowledge.toEntity ────────────────────────────────────────────────

    @Test
    fun wordKnowledge_toEntity_validData_mapsAllFields() {
        val domain = buildWordKnowledge()
        with(KnowledgeMapper) {
            val entity = domain.toEntity(json)
            assertEquals(domain.id, entity.id)
            assertEquals(domain.userId, entity.userId)
            assertEquals(domain.wordId, entity.wordId)
            assertEquals(domain.knowledgeLevel, entity.knowledgeLevel)
            assertEquals(domain.timesSeen, entity.timesSeen)
            assertEquals(domain.timesCorrect, entity.timesCorrect)
            assertEquals(domain.timesIncorrect, entity.timesIncorrect)
            assertEquals(domain.nextReview, entity.nextReview)
            assertEquals(domain.srsIntervalDays, entity.srsIntervalDays)
            assertEquals(domain.srsEaseFactor, entity.srsEaseFactor, 0.001f)
            assertEquals(domain.pronunciationScore, entity.pronunciationScore, 0.001f)
            assertEquals(domain.pronunciationAttempts, entity.pronunciationAttempts)
            assertEquals(domain.createdAt, entity.createdAt)
            assertEquals(domain.updatedAt, entity.updatedAt)
            assertNotNull(entity.contextsJson)
            assertNotNull(entity.mistakesJson)
        }
    }

    @Test
    fun wordKnowledge_toEntity_emptyContexts_producesEmptyJsonArray() {
        val domain = buildWordKnowledge(contexts = emptyList())
        with(KnowledgeMapper) {
            val entity = domain.toEntity(json)
            assertEquals("[]", entity.contextsJson)
        }
    }

    @Test
    fun wordKnowledge_toEntity_emptyMistakes_producesEmptyJsonArray() {
        val domain = buildWordKnowledge(mistakes = emptyList())
        with(KnowledgeMapper) {
            val entity = domain.toEntity(json)
            assertEquals("[]", entity.mistakesJson)
        }
    }

    // ── WordKnowledge roundtrip ───────────────────────────────────────────────

    @Test
    fun wordKnowledge_roundtrip_entityToDomainToEntity_fieldsMatch() {
        val original = buildWordKnowledgeEntity()
        with(KnowledgeMapper) {
            val domain = original.toDomain(json)
            val restored = domain.toEntity(json)
            assertEquals(original.id, restored.id)
            assertEquals(original.userId, restored.userId)
            assertEquals(original.wordId, restored.wordId)
            assertEquals(original.knowledgeLevel, restored.knowledgeLevel)
            assertEquals(original.timesSeen, restored.timesSeen)
            assertEquals(original.timesCorrect, restored.timesCorrect)
            assertEquals(original.timesIncorrect, restored.timesIncorrect)
            assertEquals(original.nextReview, restored.nextReview)
            assertEquals(original.srsIntervalDays, restored.srsIntervalDays)
            assertEquals(original.srsEaseFactor, restored.srsEaseFactor, 0.001f)
        }
    }

    @Test
    fun wordKnowledge_roundtrip_emptyJsonFields_preservedAsEmptyLists() {
        val original = buildWordKnowledgeEntity(contextsJson = "", mistakesJson = "")
        with(KnowledgeMapper) {
            val domain = original.toDomain(json)
            assertEquals(emptyList<String>(), domain.contexts)
            assertEquals(emptyList<MistakeRecord>(), domain.mistakes)
            val restored = domain.toEntity(json)
            assertEquals("[]", restored.contextsJson)
            assertEquals("[]", restored.mistakesJson)
        }
    }

    // ── RuleKnowledgeEntity.toDomain ──────────────────────────────────────────

    @Test
    fun ruleKnowledgeEntity_toDomain_validJson_mapsAllFields() {
        val entity = buildRuleKnowledgeEntity()
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertEquals(entity.id, domain.id)
            assertEquals(entity.userId, domain.userId)
            assertEquals(entity.ruleId, domain.ruleId)
            assertEquals(entity.knowledgeLevel, domain.knowledgeLevel)
            assertEquals(entity.timesPracticed, domain.timesPracticed)
            assertEquals(entity.timesCorrect, domain.timesCorrect)
            assertEquals(entity.timesIncorrect, domain.timesIncorrect)
            assertEquals(entity.lastPracticed, domain.lastPracticed)
            assertEquals(entity.nextReview, domain.nextReview)
            assertEquals(entity.srsIntervalDays, domain.srsIntervalDays)
            assertEquals(entity.srsEaseFactor, domain.srsEaseFactor, 0.001f)
            assertEquals(listOf("mistake1"), domain.commonMistakes)
            assertEquals(entity.createdAt, domain.createdAt)
            assertEquals(entity.updatedAt, domain.updatedAt)
        }
    }

    @Test
    fun ruleKnowledgeEntity_toDomain_emptyCommonMistakesJson_returnsEmptyList() {
        val entity = buildRuleKnowledgeEntity(commonMistakesJson = "")
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertEquals(emptyList<String>(), domain.commonMistakes)
        }
    }

    @Test
    fun ruleKnowledgeEntity_toDomain_invalidCommonMistakesJson_returnsEmptyList() {
        val entity = buildRuleKnowledgeEntity(commonMistakesJson = "not_json")
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertEquals(emptyList<String>(), domain.commonMistakes)
        }
    }

    @Test
    fun ruleKnowledgeEntity_toDomain_nullTimestamps_preservedAsNull() {
        val entity = buildRuleKnowledgeEntity(lastPracticed = null, nextReview = null)
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertNull(domain.lastPracticed)
            assertNull(domain.nextReview)
        }
    }

    // ── RuleKnowledge.toEntity ────────────────────────────────────────────────

    @Test
    fun ruleKnowledge_toEntity_validData_mapsAllFields() {
        val domain = buildRuleKnowledge()
        with(KnowledgeMapper) {
            val entity = domain.toEntity(json)
            assertEquals(domain.id, entity.id)
            assertEquals(domain.userId, entity.userId)
            assertEquals(domain.ruleId, entity.ruleId)
            assertEquals(domain.knowledgeLevel, entity.knowledgeLevel)
            assertEquals(domain.timesPracticed, entity.timesPracticed)
            assertEquals(domain.timesCorrect, entity.timesCorrect)
            assertEquals(domain.timesIncorrect, entity.timesIncorrect)
            assertEquals(domain.lastPracticed, entity.lastPracticed)
            assertEquals(domain.nextReview, entity.nextReview)
            assertEquals(domain.srsIntervalDays, entity.srsIntervalDays)
            assertEquals(domain.srsEaseFactor, entity.srsEaseFactor, 0.001f)
            assertEquals(domain.createdAt, entity.createdAt)
            assertEquals(domain.updatedAt, entity.updatedAt)
            assertNotNull(entity.commonMistakesJson)
        }
    }

    @Test
    fun ruleKnowledge_toEntity_emptyCommonMistakes_producesEmptyJsonArray() {
        val domain = buildRuleKnowledge(commonMistakes = emptyList())
        with(KnowledgeMapper) {
            val entity = domain.toEntity(json)
            assertEquals("[]", entity.commonMistakesJson)
        }
    }

    // ── RuleKnowledge roundtrip ───────────────────────────────────────────────

    @Test
    fun ruleKnowledge_roundtrip_entityToDomainToEntity_fieldsMatch() {
        val original = buildRuleKnowledgeEntity()
        with(KnowledgeMapper) {
            val domain = original.toDomain(json)
            val restored = domain.toEntity(json)
            assertEquals(original.id, restored.id)
            assertEquals(original.userId, restored.userId)
            assertEquals(original.ruleId, restored.ruleId)
            assertEquals(original.knowledgeLevel, restored.knowledgeLevel)
            assertEquals(original.timesPracticed, restored.timesPracticed)
            assertEquals(original.timesCorrect, restored.timesCorrect)
            assertEquals(original.timesIncorrect, restored.timesIncorrect)
            assertEquals(original.lastPracticed, restored.lastPracticed)
            assertEquals(original.nextReview, restored.nextReview)
            assertEquals(original.srsIntervalDays, restored.srsIntervalDays)
        }
    }

    // ── PhraseKnowledgeEntity.toDomain ────────────────────────────────────────

    @Test
    fun phraseKnowledgeEntity_toDomain_validData_mapsAllFields() {
        val entity = buildPhraseKnowledgeEntity()
        with(KnowledgeMapper) {
            val domain = entity.toDomain()
            assertEquals(entity.id, domain.id)
            assertEquals(entity.userId, domain.userId)
            assertEquals(entity.phraseId, domain.phraseId)
            assertEquals(entity.knowledgeLevel, domain.knowledgeLevel)
            assertEquals(entity.timesPracticed, domain.timesPracticed)
            assertEquals(entity.timesCorrect, domain.timesCorrect)
            assertEquals(entity.lastPracticed, domain.lastPracticed)
            assertEquals(entity.nextReview, domain.nextReview)
            assertEquals(entity.srsIntervalDays, domain.srsIntervalDays)
            assertEquals(entity.srsEaseFactor, domain.srsEaseFactor, 0.001f)
            assertEquals(entity.pronunciationScore, domain.pronunciationScore, 0.001f)
            assertEquals(entity.createdAt, domain.createdAt)
            assertEquals(entity.updatedAt, domain.updatedAt)
        }
    }

    @Test
    fun phraseKnowledgeEntity_toDomain_nullTimestamps_preservedAsNull() {
        val entity = buildPhraseKnowledgeEntity(lastPracticed = null, nextReview = null)
        with(KnowledgeMapper) {
            val domain = entity.toDomain()
            assertNull(domain.lastPracticed)
            assertNull(domain.nextReview)
        }
    }

    // ── PhraseKnowledge.toEntity ──────────────────────────────────────────────

    @Test
    fun phraseKnowledge_toEntity_validData_mapsAllFields() {
        val domain = buildPhraseKnowledge()
        with(KnowledgeMapper) {
            val entity = domain.toEntity()
            assertEquals(domain.id, entity.id)
            assertEquals(domain.userId, entity.userId)
            assertEquals(domain.phraseId, entity.phraseId)
            assertEquals(domain.knowledgeLevel, entity.knowledgeLevel)
            assertEquals(domain.timesPracticed, entity.timesPracticed)
            assertEquals(domain.timesCorrect, entity.timesCorrect)
            assertEquals(domain.lastPracticed, entity.lastPracticed)
            assertEquals(domain.nextReview, entity.nextReview)
            assertEquals(domain.srsIntervalDays, entity.srsIntervalDays)
            assertEquals(domain.srsEaseFactor, entity.srsEaseFactor, 0.001f)
            assertEquals(domain.pronunciationScore, entity.pronunciationScore, 0.001f)
            assertEquals(domain.createdAt, entity.createdAt)
            assertEquals(domain.updatedAt, entity.updatedAt)
        }
    }

    // ── PhraseKnowledge roundtrip ─────────────────────────────────────────────

    @Test
    fun phraseKnowledge_roundtrip_entityToDomainToEntity_fieldsMatch() {
        val original = buildPhraseKnowledgeEntity()
        with(KnowledgeMapper) {
            val domain = original.toDomain()
            val restored = domain.toEntity()
            assertEquals(original.id, restored.id)
            assertEquals(original.phraseId, restored.phraseId)
            assertEquals(original.userId, restored.userId)
            assertEquals(original.knowledgeLevel, restored.knowledgeLevel)
            assertEquals(original.pronunciationScore, restored.pronunciationScore, 0.001f)
        }
    }

    // ── PronunciationRecordEntity.toDomain ────────────────────────────────────

    @Test
    fun pronunciationRecordEntity_toDomain_validJson_mapsAllFields() {
        val entity = buildPronunciationRecordEntity()
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertEquals(entity.id, domain.id)
            assertEquals(entity.userId, domain.userId)
            assertEquals(entity.word, domain.word)
            assertEquals(entity.score, domain.score, 0.001f)
            assertEquals(listOf("ʊ", "n"), domain.problemSounds)
            assertEquals(entity.attemptNumber, domain.attemptNumber)
            assertEquals(entity.sessionId, domain.sessionId)
            assertEquals(entity.timestamp, domain.timestamp)
        }
    }

    @Test
    fun pronunciationRecordEntity_toDomain_emptyProblemSoundsJson_returnsEmptyList() {
        val entity = buildPronunciationRecordEntity(problemSoundsJson = "")
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertEquals(emptyList<String>(), domain.problemSounds)
        }
    }

    @Test
    fun pronunciationRecordEntity_toDomain_invalidProblemSoundsJson_returnsEmptyList() {
        val entity = buildPronunciationRecordEntity(problemSoundsJson = "{bad}")
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertEquals(emptyList<String>(), domain.problemSounds)
        }
    }

    @Test
    fun pronunciationRecordEntity_toDomain_emptySessionId_returnsNull() {
        val entity = buildPronunciationRecordEntity(sessionId = "")
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertNull(domain.sessionId)
        }
    }

    @Test
    fun pronunciationRecordEntity_toDomain_nonEmptySessionId_preservedAsIs() {
        val entity = buildPronunciationRecordEntity(sessionId = "ses_99")
        with(KnowledgeMapper) {
            val domain = entity.toDomain(json)
            assertEquals("ses_99", domain.sessionId)
        }
    }

    // ── PronunciationResult.toEntity ──────────────────────────────────────────

    @Test
    fun pronunciationResult_toEntity_validData_mapsAllFields() {
        val domain = buildPronunciationResult()
        with(KnowledgeMapper) {
            val entity = domain.toEntity(json)
            assertEquals(domain.id, entity.id)
            assertEquals(domain.userId, entity.userId)
            assertEquals(domain.word, entity.word)
            assertEquals(domain.score, entity.score, 0.001f)
            assertEquals(domain.attemptNumber, entity.attemptNumber)
            assertEquals(domain.sessionId, entity.sessionId)
            assertEquals(domain.timestamp, entity.timestamp)
            assertNotNull(entity.problemSoundsJson)
            assertTrue(entity.createdAt > 0)
        }
    }

    @Test
    fun pronunciationResult_toEntity_nullSessionId_storedAsEmptyString() {
        val domain = buildPronunciationResult(sessionId = null)
        with(KnowledgeMapper) {
            val entity = domain.toEntity(json)
            assertEquals("", entity.sessionId)
        }
    }

    @Test
    fun pronunciationResult_toEntity_emptyProblemSounds_producesEmptyJsonArray() {
        val domain = buildPronunciationResult(problemSounds = emptyList())
        with(KnowledgeMapper) {
            val entity = domain.toEntity(json)
            assertEquals("[]", entity.problemSoundsJson)
        }
    }

    // ── PronunciationResult roundtrip ─────────────────────────────────────────

    @Test
    fun pronunciationResult_roundtrip_entityToDomainToEntity_fieldsMatch() {
        val original = buildPronunciationRecordEntity()
        with(KnowledgeMapper) {
            val domain = original.toDomain(json)
            val restored = domain.toEntity(json)
            assertEquals(original.id, restored.id)
            assertEquals(original.userId, restored.userId)
            assertEquals(original.word, restored.word)
            assertEquals(original.score, restored.score, 0.001f)
            assertEquals(original.attemptNumber, restored.attemptNumber)
            assertEquals(original.sessionId, restored.sessionId)
            assertEquals(original.timestamp, restored.timestamp)
        }
    }

    @Test
    fun pronunciationResult_roundtrip_emptySessionId_remainsNull() {
        val original = buildPronunciationRecordEntity(sessionId = "")
        with(KnowledgeMapper) {
            val domain = original.toDomain(json)
            assertNull(domain.sessionId)
            val restored = domain.toEntity(json)
            assertEquals("", restored.sessionId)
        }
    }

    // ── wordKnowledgeToSyncMap ────────────────────────────────────────────────

    @Test
    fun wordKnowledgeToSyncMap_validEntity_containsExpectedKeys() {
        val entity = buildWordKnowledgeEntity()
        val map = KnowledgeMapper.wordKnowledgeToSyncMap(entity)
        assertTrue(map.containsKey("userId"))
        assertTrue(map.containsKey("wordId"))
        assertTrue(map.containsKey("knowledgeLevel"))
        assertTrue(map.containsKey("timesSeen"))
        assertTrue(map.containsKey("timesCorrect"))
        assertTrue(map.containsKey("timesIncorrect"))
        assertTrue(map.containsKey("lastSeen"))
        assertTrue(map.containsKey("lastCorrect"))
        assertTrue(map.containsKey("lastIncorrect"))
        assertTrue(map.containsKey("nextReview"))
        assertTrue(map.containsKey("srsIntervalDays"))
        assertTrue(map.containsKey("srsEaseFactor"))
        assertTrue(map.containsKey("pronunciationScore"))
        assertTrue(map.containsKey("pronunciationAttempts"))
        assertTrue(map.containsKey("updatedAt"))
    }

    @Test
    fun wordKnowledgeToSyncMap_doesNotContainContextsOrMistakes() {
        val entity = buildWordKnowledgeEntity()
        val map = KnowledgeMapper.wordKnowledgeToSyncMap(entity)
        assertFalse(map.containsKey("contextsJson"))
        assertFalse(map.containsKey("mistakesJson"))
    }

    @Test
    fun wordKnowledgeToSyncMap_nullTimestamps_replacedWithZero() {
        val entity = buildWordKnowledgeEntity(lastSeen = null, lastCorrect = null, lastIncorrect = null, nextReview = null)
        val map = KnowledgeMapper.wordKnowledgeToSyncMap(entity)
        assertEquals(0L, map["lastSeen"])
        assertEquals(0L, map["lastCorrect"])
        assertEquals(0L, map["lastIncorrect"])
        assertEquals(0L, map["nextReview"])
    }

    @Test
    fun wordKnowledgeToSyncMap_values_matchEntityFields() {
        val entity = buildWordKnowledgeEntity()
        val map = KnowledgeMapper.wordKnowledgeToSyncMap(entity)
        assertEquals(entity.userId, map["userId"])
        assertEquals(entity.wordId, map["wordId"])
        assertEquals(entity.knowledgeLevel, map["knowledgeLevel"])
        assertEquals(entity.timesSeen, map["timesSeen"])
        assertEquals(entity.timesCorrect, map["timesCorrect"])
        assertEquals(entity.timesIncorrect, map["timesIncorrect"])
        assertEquals(entity.lastSeen, map["lastSeen"])
        assertEquals(entity.srsIntervalDays, map["srsIntervalDays"])
        assertEquals(entity.srsEaseFactor.toDouble(), map["srsEaseFactor"])
        assertEquals(entity.pronunciationScore.toDouble(), map["pronunciationScore"])
        assertEquals(entity.pronunciationAttempts, map["pronunciationAttempts"])
        assertEquals(entity.updatedAt, map["updatedAt"])
    }

    // ── ruleKnowledgeToSyncMap ────────────────────────────────────────────────

    @Test
    fun ruleKnowledgeToSyncMap_validEntity_containsExpectedKeys() {
        val entity = buildRuleKnowledgeEntity()
        val map = KnowledgeMapper.ruleKnowledgeToSyncMap(entity)
        assertTrue(map.containsKey("userId"))
        assertTrue(map.containsKey("ruleId"))
        assertTrue(map.containsKey("knowledgeLevel"))
        assertTrue(map.containsKey("timesPracticed"))
        assertTrue(map.containsKey("timesCorrect"))
        assertTrue(map.containsKey("timesIncorrect"))
        assertTrue(map.containsKey("lastPracticed"))
        assertTrue(map.containsKey("nextReview"))
        assertTrue(map.containsKey("srsIntervalDays"))
        assertTrue(map.containsKey("srsEaseFactor"))
        assertTrue(map.containsKey("updatedAt"))
    }

    @Test
    fun ruleKnowledgeToSyncMap_doesNotContainCommonMistakes() {
        val entity = buildRuleKnowledgeEntity()
        val map = KnowledgeMapper.ruleKnowledgeToSyncMap(entity)
        assertFalse(map.containsKey("commonMistakesJson"))
    }

    @Test
    fun ruleKnowledgeToSyncMap_nullTimestamps_replacedWithZero() {
        val entity = buildRuleKnowledgeEntity(lastPracticed = null, nextReview = null)
        val map = KnowledgeMapper.ruleKnowledgeToSyncMap(entity)
        assertEquals(0L, map["lastPracticed"])
        assertEquals(0L, map["nextReview"])
    }

    @Test
    fun ruleKnowledgeToSyncMap_values_matchEntityFields() {
        val entity = buildRuleKnowledgeEntity()
        val map = KnowledgeMapper.ruleKnowledgeToSyncMap(entity)
        assertEquals(entity.userId, map["userId"])
        assertEquals(entity.ruleId, map["ruleId"])
        assertEquals(entity.knowledgeLevel, map["knowledgeLevel"])
        assertEquals(entity.timesPracticed, map["timesPracticed"])
        assertEquals(entity.timesCorrect, map["timesCorrect"])
        assertEquals(entity.timesIncorrect, map["timesIncorrect"])
        assertEquals(entity.srsIntervalDays, map["srsIntervalDays"])
        assertEquals(entity.srsEaseFactor.toDouble(), map["srsEaseFactor"])
        assertEquals(entity.updatedAt, map["updatedAt"])
    }
}
