package com.voicedeutsch.master.data.mapper

import com.voicedeutsch.master.data.local.database.entity.PhraseKnowledgeEntity
import com.voicedeutsch.master.data.local.database.entity.RuleKnowledgeEntity
import com.voicedeutsch.master.data.local.database.entity.WordKnowledgeEntity
import com.voicedeutsch.master.domain.model.knowledge.MistakeRecord
import com.voicedeutsch.master.domain.model.knowledge.PhraseKnowledge
import com.voicedeutsch.master.domain.model.knowledge.RuleKnowledge
import com.voicedeutsch.master.domain.model.knowledge.WordKnowledge
import com.voicedeutsch.master.data.local.database.entity.PronunciationRecordEntity
import com.voicedeutsch.master.domain.model.speech.PronunciationResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object KnowledgeMapper {

    // FIX: безопасная десериализация списка.
    // Если строка пустая ("") или невалидный JSON — возвращаем emptyList()
    // вместо SerializationException.
    private inline fun <reified T> Json.safeDecodeList(raw: String): List<T> =
        if (raw.isBlank()) emptyList()
        else runCatching { decodeFromString<List<T>>(raw) }.getOrDefault(emptyList())

    // ── Entity → Domain ───────────────────────────────────────────────────────

    fun WordKnowledgeEntity.toDomain(json: Json): WordKnowledge = WordKnowledge(
        id                    = id,
        userId                = userId,
        wordId                = wordId,
        knowledgeLevel        = knowledgeLevel,
        timesSeen             = timesSeen,
        timesCorrect          = timesCorrect,
        timesIncorrect        = timesIncorrect,
        lastSeen              = lastSeen,
        lastCorrect           = lastCorrect,
        lastIncorrect         = lastIncorrect,
        nextReview            = nextReview,
        srsIntervalDays       = srsIntervalDays,
        srsEaseFactor         = srsEaseFactor,
        pronunciationScore    = pronunciationScore,
        pronunciationAttempts = pronunciationAttempts,
        contexts              = json.safeDecodeList<String>(contextsJson),
        mistakes              = json.safeDecodeList<MistakeRecord>(mistakesJson),
        createdAt             = createdAt,
        updatedAt             = updatedAt,
    )

    fun WordKnowledge.toEntity(json: Json): WordKnowledgeEntity = WordKnowledgeEntity(
        id                    = id,
        userId                = userId,
        wordId                = wordId,
        knowledgeLevel        = knowledgeLevel,
        timesSeen             = timesSeen,
        timesCorrect          = timesCorrect,
        timesIncorrect        = timesIncorrect,
        lastSeen              = lastSeen,
        lastCorrect           = lastCorrect,
        lastIncorrect         = lastIncorrect,
        nextReview            = nextReview,
        srsIntervalDays       = srsIntervalDays,
        srsEaseFactor         = srsEaseFactor,
        pronunciationScore    = pronunciationScore,
        pronunciationAttempts = pronunciationAttempts,
        contextsJson          = json.encodeToString(contexts),
        mistakesJson          = json.encodeToString(mistakes),
        createdAt             = createdAt,
        updatedAt             = updatedAt,
    )

    fun RuleKnowledgeEntity.toDomain(json: Json): RuleKnowledge = RuleKnowledge(
        id              = id,
        userId          = userId,
        ruleId          = ruleId,
        knowledgeLevel  = knowledgeLevel,
        timesPracticed  = timesPracticed,
        timesCorrect    = timesCorrect,
        timesIncorrect  = timesIncorrect,
        lastPracticed   = lastPracticed,
        nextReview      = nextReview,
        srsIntervalDays = srsIntervalDays,
        srsEaseFactor   = srsEaseFactor,
        commonMistakes  = json.safeDecodeList<String>(commonMistakesJson),
        createdAt       = createdAt,
        updatedAt       = updatedAt,
    )

    fun RuleKnowledge.toEntity(json: Json): RuleKnowledgeEntity = RuleKnowledgeEntity(
        id                 = id,
        userId             = userId,
        ruleId             = ruleId,
        knowledgeLevel     = knowledgeLevel,
        timesPracticed     = timesPracticed,
        timesCorrect       = timesCorrect,
        timesIncorrect     = timesIncorrect,
        lastPracticed      = lastPracticed,
        nextReview         = nextReview,
        srsIntervalDays    = srsIntervalDays,
        srsEaseFactor      = srsEaseFactor,
        commonMistakesJson = json.encodeToString(commonMistakes),
        createdAt          = createdAt,
        updatedAt          = updatedAt,
    )

    fun PhraseKnowledgeEntity.toDomain(): PhraseKnowledge = PhraseKnowledge(
        id                 = id,
        userId             = userId,
        phraseId           = phraseId,
        knowledgeLevel     = knowledgeLevel,
        timesPracticed     = timesPracticed,
        timesCorrect       = timesCorrect,
        lastPracticed      = lastPracticed,
        nextReview         = nextReview,
        srsIntervalDays    = srsIntervalDays,
        srsEaseFactor      = srsEaseFactor,
        pronunciationScore = pronunciationScore,
        createdAt          = createdAt,
        updatedAt          = updatedAt,
    )

    fun PhraseKnowledge.toEntity(): PhraseKnowledgeEntity = PhraseKnowledgeEntity(
        id                 = id,
        userId             = userId,
        phraseId           = phraseId,
        knowledgeLevel     = knowledgeLevel,
        timesPracticed     = timesPracticed,
        timesCorrect       = timesCorrect,
        lastPracticed      = lastPracticed,
        nextReview         = nextReview,
        srsIntervalDays    = srsIntervalDays,
        srsEaseFactor      = srsEaseFactor,
        pronunciationScore = pronunciationScore,
        createdAt          = createdAt,
        updatedAt          = updatedAt,
    )

    fun PronunciationRecordEntity.toDomain(json: Json): PronunciationResult = PronunciationResult(
        id            = id,
        userId        = userId,
        word          = word,
        score         = score,
        problemSounds = json.safeDecodeList<String>(problemSoundsJson),
        attemptNumber = attemptNumber,
        sessionId     = sessionId?.ifEmpty { null },
        timestamp     = timestamp,
    )

    fun PronunciationResult.toEntity(json: Json): PronunciationRecordEntity = PronunciationRecordEntity(
        id                = id,
        userId            = userId,
        word              = word,
        score             = score,
        problemSoundsJson = json.encodeToString(problemSounds),
        attemptNumber     = attemptNumber,
        sessionId         = sessionId ?: "",
        timestamp         = timestamp,
        createdAt         = System.currentTimeMillis(),
    )

    // ── Entity → Firestore Map (для CloudSyncService.enqueueKnowledgeItem) ────

    /**
     * Конвертирует WordKnowledgeEntity в Map для отправки в Firestore.
     *
     * Правила:
     * - Включаем только числовые/скалярные поля — они всегда безопасны по размеру.
     * - contextsJson и mistakesJson ИСКЛЮЧЕНЫ: могут быть очень большими (>1MB),
     *   Firestore лимит на документ — 1MB. Контексты и ошибки хранятся только в Room.
     * - null-значения заменяем на 0L/0 чтобы не создавать разреженные документы.
     *
     * @param entity WordKnowledgeEntity из Room
     */
    fun wordKnowledgeToSyncMap(entity: WordKnowledgeEntity): Map<String, Any> = buildMap {
        put("userId",                entity.userId)
        put("wordId",                entity.wordId)
        put("knowledgeLevel",        entity.knowledgeLevel)
        put("timesSeen",             entity.timesSeen)
        put("timesCorrect",          entity.timesCorrect)
        put("timesIncorrect",        entity.timesIncorrect)
        put("lastSeen",              entity.lastSeen ?: 0L)
        put("lastCorrect",           entity.lastCorrect ?: 0L)
        put("lastIncorrect",         entity.lastIncorrect ?: 0L)
        put("nextReview",            entity.nextReview ?: 0L)
        put("srsIntervalDays",       entity.srsIntervalDays)
        put("srsEaseFactor",         entity.srsEaseFactor.toDouble())
        put("pronunciationScore",    entity.pronunciationScore.toDouble())
        put("pronunciationAttempts", entity.pronunciationAttempts)
        put("updatedAt",             entity.updatedAt)
        // contextsJson и mistakesJson — намеренно не включены (size safety)
    }

    /**
     * Конвертирует RuleKnowledgeEntity в Map для отправки в Firestore.
     *
     * commonMistakesJson ИСКЛЮЧЁН по той же причине что и mistakesJson выше.
     *
     * @param entity RuleKnowledgeEntity из Room
     */
    fun ruleKnowledgeToSyncMap(entity: RuleKnowledgeEntity): Map<String, Any> = buildMap {
        put("userId",          entity.userId)
        put("ruleId",          entity.ruleId)
        put("knowledgeLevel",  entity.knowledgeLevel)
        put("timesPracticed",  entity.timesPracticed)
        put("timesCorrect",    entity.timesCorrect)
        put("timesIncorrect",  entity.timesIncorrect)
        put("lastPracticed",   entity.lastPracticed ?: 0L)
        put("nextReview",      entity.nextReview ?: 0L)
        put("srsIntervalDays", entity.srsIntervalDays)
        put("srsEaseFactor",   entity.srsEaseFactor.toDouble())
        put("updatedAt",       entity.updatedAt)
        // commonMistakesJson — намеренно не включён (size safety)
    }
}
