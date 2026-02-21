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

    fun WordKnowledgeEntity.toDomain(json: Json): WordKnowledge {
        val contextsList = try {
            json.decodeFromString<List<String>>(contextsJson)
        } catch (e: Exception) {
            emptyList()
        }
        val mistakesList = try {
            json.decodeFromString<List<MistakeRecord>>(mistakesJson)
        } catch (e: Exception) {
            emptyList()
        }
        return WordKnowledge(
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
            contexts = contextsList,
            mistakes = mistakesList,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun WordKnowledge.toEntity(json: Json): WordKnowledgeEntity = WordKnowledgeEntity(
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
        contextsJson = json.encodeToString(contexts),
        mistakesJson = json.encodeToString(mistakes),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun RuleKnowledgeEntity.toDomain(json: Json): RuleKnowledge {
        val commonMistakesList = try {
            json.decodeFromString<List<String>>(commonMistakesJson)
        } catch (e: Exception) {
            emptyList()
        }
        return RuleKnowledge(
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
            commonMistakes = commonMistakesList,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun RuleKnowledge.toEntity(json: Json): RuleKnowledgeEntity = RuleKnowledgeEntity(
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
        commonMistakesJson = json.encodeToString(commonMistakes),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun PhraseKnowledgeEntity.toDomain(): PhraseKnowledge = PhraseKnowledge(
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
        updatedAt = updatedAt
    )

    fun PhraseKnowledge.toEntity(): PhraseKnowledgeEntity = PhraseKnowledgeEntity(
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
        updatedAt = updatedAt
    )

    fun PronunciationRecordEntity.toDomain(json: Json): PronunciationResult = PronunciationResult(
        id = id,
        userId = userId,
        word = word,
        score = score,
        problemSounds = runCatching {
            json.decodeFromString<List<String>>(problemSoundsJson)
        }.getOrDefault(emptyList()),
        attemptNumber = attemptNumber,
        sessionId = sessionId.ifEmpty { null },
        timestamp = timestamp
    )

    fun PronunciationResult.toEntity(json: Json): PronunciationRecordEntity = PronunciationRecordEntity(
        id = id,
        userId = userId,
        word = word,
        score = score,
        problemSoundsJson = json.encodeToString(problemSounds),
        attemptNumber = attemptNumber,
        sessionId = sessionId ?: "",
        timestamp = timestamp,
        createdAt = System.currentTimeMillis()
    )
}