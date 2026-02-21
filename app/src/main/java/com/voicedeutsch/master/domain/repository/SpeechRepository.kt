package com.voicedeutsch.master.domain.repository

import com.voicedeutsch.master.domain.model.speech.PronunciationResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository for speech/pronunciation data.
 * Architecture line 992 (SpeechRepositoryImpl).
 */
interface SpeechRepository {

    suspend fun savePronunciationResult(result: PronunciationResult)

    suspend fun getPronunciationHistory(userId: String, word: String): List<PronunciationResult>

    suspend fun getRecentPronunciationResults(userId: String, limit: Int = 50): List<PronunciationResult>

    suspend fun getAveragePronunciationScore(userId: String): Float

    suspend fun getProblematicSounds(userId: String): List<String>

    fun observePronunciationScore(userId: String): Flow<Float>
}