package com.voicedeutsch.master.data.repository

import com.voicedeutsch.master.data.local.database.dao.KnowledgeDao
import com.voicedeutsch.master.data.local.database.entity.PronunciationRecordEntity
import com.voicedeutsch.master.domain.model.speech.PronunciationResult
import com.voicedeutsch.master.domain.repository.SpeechRepository
import com.voicedeutsch.master.util.generateUUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SpeechRepositoryImpl(
    private val knowledgeDao: KnowledgeDao,
    private val json: Json
) : SpeechRepository {

    override suspend fun savePronunciationResult(result: PronunciationResult) {
        val entity = PronunciationRecordEntity(
            id = result.id.ifEmpty { generateUUID() },
            userId = result.userId,
            word = result.word,
            score = result.score,
            problemSoundsJson = json.encodeToString(result.problemSounds),
            attemptNumber = result.attemptNumber,
            sessionId = result.sessionId ?: "",
            timestamp = result.timestamp,
            createdAt = System.currentTimeMillis()
        )
        knowledgeDao.insertPronunciationRecord(entity)
    }

    override suspend fun getPronunciationHistory(
        userId: String,
        word: String
    ): List<PronunciationResult> {
        return knowledgeDao.getPronunciationRecords(userId, word).map { it.toDomain() }
    }

    override suspend fun getRecentPronunciationResults(
        userId: String,
        limit: Int
    ): List<PronunciationResult> {
        return knowledgeDao.getRecentPronunciationRecords(userId, limit).map { it.toDomain() }
    }

    override suspend fun getAveragePronunciationScore(userId: String): Float {
        return knowledgeDao.getAveragePronunciationScore(userId) ?: 0f
    }

    override suspend fun getProblematicSounds(userId: String): List<String> {
        val records = knowledgeDao.getRecentPronunciationRecords(userId, 100)
        val soundCounts = mutableMapOf<String, Int>()
        records.forEach { record ->
            runCatching {
                val sounds: List<String> = json.decodeFromString(record.problemSoundsJson)
                sounds.forEach { sound -> soundCounts[sound] = (soundCounts[sound] ?: 0) + 1 }
            }
        }
        return soundCounts.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }
    }

    override fun observePronunciationScore(userId: String): Flow<Float> {
        return knowledgeDao.observeAveragePronunciationScore(userId)
            .map { it ?: 0f }
    }

    private fun PronunciationRecordEntity.toDomain() = PronunciationResult(
        id = id,
        userId = userId,
        word = word,
        score = score,
        problemSounds = runCatching {
            json.decodeFromString<List<String>>(problemSoundsJson)
        }.getOrDefault(emptyList()),
        attemptNumber = attemptNumber,
        sessionId = sessionId?.ifEmpty { null },
        timestamp = timestamp
    )
}