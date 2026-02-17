package com.voicedeutsch.master.data.repository

import com.voicedeutsch.master.data.local.database.dao.KnowledgeDao
import com.voicedeutsch.master.data.local.database.dao.UserDao
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.data.mapper.UserMapper.toDomain
import com.voicedeutsch.master.data.mapper.UserMapper.toEntity
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.domain.model.user.UserPreferences
import com.voicedeutsch.master.domain.model.user.UserProfile
import com.voicedeutsch.master.domain.model.user.UserStatistics
import com.voicedeutsch.master.domain.model.user.VoiceSettings
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val knowledgeDao: KnowledgeDao,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val json: Json
) : UserRepository {

    override fun getUserProfileFlow(userId: String): Flow<UserProfile?> =
        userDao.getUserFlow(userId).map { it?.toDomain(json) }

    override suspend fun getUserProfile(userId: String): UserProfile? =
        userDao.getUser(userId)?.toDomain(json)

    override suspend fun createUser(profile: UserProfile): String {
        userDao.insertUser(profile.toEntity(json))
        preferencesDataStore.setActiveUserId(profile.id)
        return profile.id
    }

    override suspend fun updateUser(profile: UserProfile) {
        userDao.updateUser(profile.toEntity(json))
    }

    override suspend fun updateUserLevel(userId: String, cefrLevel: CefrLevel, subLevel: Int) {
        userDao.updateLevel(userId, cefrLevel.name, subLevel, DateUtils.nowTimestamp())
    }

    override suspend fun updateUserPreferences(userId: String, preferences: UserPreferences) {
        val prefsJson = json.encodeToString(preferences)
        userDao.updatePreferences(userId, prefsJson, DateUtils.nowTimestamp())
    }

    override suspend fun updateVoiceSettings(userId: String, settings: VoiceSettings) {
        val settingsJson = json.encodeToString(settings)
        userDao.updateVoiceSettings(userId, settingsJson, DateUtils.nowTimestamp())
    }

    override suspend fun incrementSessionStats(
        userId: String,
        durationMinutes: Int,
        wordsLearned: Int,
        rulesLearned: Int
    ) {
        val now = DateUtils.nowTimestamp()
        userDao.incrementSessionStats(userId, durationMinutes, wordsLearned, rulesLearned, now, now)
    }

    override suspend fun updateStreak(userId: String, streakDays: Int) {
        userDao.updateStreak(userId, streakDays, DateUtils.nowTimestamp())
    }

    override suspend fun getUserStatistics(userId: String): UserStatistics {
        val user = userDao.getUser(userId)
            ?: throw IllegalStateException("User not found: $userId")
        val now = DateUtils.nowTimestamp()
        val knownWords = knowledgeDao.getKnownWordsCount(userId)
        val activeWords = knowledgeDao.getActiveWordsCount(userId)
        val knownRules = knowledgeDao.getKnownRulesCount(userId)
        val wordsForReview = knowledgeDao.getWordsForReviewCount(userId, now)
        val rulesForReview = knowledgeDao.getRulesForReviewCount(userId, now)

        return UserStatistics(
            totalWords = knownWords,
            activeWords = activeWords,
            passiveWords = (knownWords - activeWords).coerceAtLeast(0),
            totalRules = knownRules,
            knownRules = knownRules,
            totalSessions = user.totalSessions,
            totalMinutes = user.totalMinutes,
            streakDays = user.streakDays,
            averageScore = 0f,
            averagePronunciationScore = 0f,
            wordsForReviewToday = wordsForReview,
            rulesForReviewToday = rulesForReview,
            bookProgress = 0f,
            currentChapter = 1,
            totalChapters = 20
        )
    }

    override suspend fun getActiveUserId(): String? =
        preferencesDataStore.getActiveUserId()

    override suspend fun setActiveUserId(userId: String) {
        preferencesDataStore.setActiveUserId(userId)
    }

    override suspend fun userExists(): Boolean =
        userDao.getUserCount() > 0
}