package com.voicedeutsch.master.domain.usecase.session

import com.voicedeutsch.master.domain.model.session.LearningSession
import com.voicedeutsch.master.domain.model.session.SessionResult
import com.voicedeutsch.master.domain.repository.SessionRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.util.DateUtils

/**
 * Persists a completed session and updates user aggregate statistics.
 * Architecture line 931 (SaveSessionUseCase.kt).
 */
class SaveSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
) {

    suspend operator fun invoke(session: LearningSession, result: SessionResult) {
        // 1. Save session record
        sessionRepository.saveSession(session)

        // 2. Update user statistics
        userRepository.incrementSessionStats(
            userId = session.userId,
            durationMinutes = result.durationMinutes,
            wordsLearned = result.wordsLearned,
            rulesLearned = result.rulesLearned,
        )

        // 3. Update daily statistics
        sessionRepository.updateDailyStatistics(
            userId = session.userId,
            date = DateUtils.todayDateString(),
            sessionDurationMinutes = result.durationMinutes,
            wordsLearned = result.wordsLearned,
            wordsReviewed = result.wordsReviewed,
        )
    }
}