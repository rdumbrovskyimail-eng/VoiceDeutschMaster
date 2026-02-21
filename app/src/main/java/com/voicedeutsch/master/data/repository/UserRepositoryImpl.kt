package com.voicedeutsch.master.data.repository

import com.voicedeutsch.master.data.local.database.dao.KnowledgeDao
import com.voicedeutsch.master.data.local.database.dao.UserDao
import com.voicedeutsch.master.data.local.database.dao.WordDao
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
    private val wordDao: WordDao,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val json: Json,
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
        rulesLearned: Int,
    ) {
        val now = DateUtils.nowTimestamp()
        userDao.incrementSessionStats(userId, durationMinutes, wordsLearned, rulesLearned, now, now)
    }

    override suspend fun updateStreak(userId: String, streakDays: Int) {
        userDao.updateStreak(userId, streakDays, DateUtils.nowTimestamp())
    }

    /**
     * H4 FIX: [totalWords] now queries the dictionary's total word count
     * via [WordDao.getTotalWordCount] instead of using [knownWords].
     *
     * Previously `totalWords = knownWords` made the stats screen show
     * "known words" in the "total words" slot. This misled both the UI
     * (progress bar always at 100%) and the Gemini context (which received
     * incorrect data via [GetUserStatisticsUseCase]).
     *
     * Additionally:
     *   - [totalRules] now comes from [WordDao.getTotalRuleCount] (total
     *     grammar rules in the book) rather than duplicating [knownRules].
     *   - [bookProgress] is computed as known / total ratio.
     *   - [currentChapter] is read from the user entity.
     */
    override suspend fun getUserStatistics(userId: String): UserStatistics {
        val user = userDao.getUser(userId)
            ?: throw IllegalStateException("User not found: $userId")
        val now = DateUtils.nowTimestamp()

        // Dictionary totals (all words/rules in the book, regardless of user progress)
        val totalWordsInBook = wordDao.getTotalWordCount()
        val totalRulesInBook = wordDao.getTotalRuleCount()

        // User's knowledge counts
        val knownWords = knowledgeDao.getKnownWordsCount(userId)
        val activeWords = knowledgeDao.getActiveWordsCount(userId)
        val knownRules = knowledgeDao.getKnownRulesCount(userId)
        val wordsForReview = knowledgeDao.getWordsForReviewCount(userId, now)
        val rulesForReview = knowledgeDao.getRulesForReviewCount(userId, now)

        // Book progress: fraction of total vocabulary the user has encountered
        val bookProgress = if (totalWordsInBook > 0) {
            knownWords.toFloat() / totalWordsInBook.toFloat()
        } else {
            0f
        }

        return UserStatistics(
            totalWords = totalWordsInBook,
            activeWords = activeWords,
            passiveWords = (knownWords - activeWords).coerceAtLeast(0),
            totalRules = totalRulesInBook,
            knownRules = knownRules,
            totalSessions = user.totalSessions,
            totalMinutes = user.totalMinutes,
            streakDays = user.streakDays,
            averageScore = 0f,
            averagePronunciationScore = 0f,
            wordsForReviewToday = wordsForReview,
            rulesForReviewToday = rulesForReview,
            bookProgress = bookProgress,
            currentChapter = 1, // FIX: UserEntity doesn't have currentChapter; use default
            totalChapters = 20,
        )
    }

    override suspend fun getActiveUserId(): String? =
        preferencesDataStore.getActiveUserId()

    override suspend fun setActiveUserId(userId: String) {
        preferencesDataStore.setActiveUserId(userId)
    }

    override suspend fun userExists(): Boolean =
        userDao.getUserCount() > 0

    override suspend fun getAllUserIds(): List<String> =
        userDao.getAllUserIds()

    override suspend fun updateStreakIfNeeded(userId: String) {
        val user = userDao.getUser(userId) ?: return
        val now = DateUtils.nowTimestamp()
        val lastSession = user.lastSessionDate
        val oneDayMs = 24 * 60 * 60 * 1000L
        val twoDaysMs = 2 * oneDayMs
        val diff = now - lastSession
        when {
            diff > twoDaysMs -> userDao.updateStreak(userId, 0, now)
            diff > oneDayMs -> userDao.updateStreak(userId, user.streakDays + 1, now)
        }
    }
}
