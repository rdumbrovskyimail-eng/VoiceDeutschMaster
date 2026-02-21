package com.voicedeutsch.master.domain.usecase.session

import com.voicedeutsch.master.domain.model.session.LearningSession
import com.voicedeutsch.master.domain.repository.SessionRepository

/**
 * Retrieves past session history for statistics and analysis.
 * Architecture line 932 (GetSessionHistoryUseCase.kt).
 */
class GetSessionHistoryUseCase(
    private val sessionRepository: SessionRepository,
) {

    data class Params(
        val userId: String,
        val limit: Int = 30,
        val fromDate: Long? = null,
    )

    suspend operator fun invoke(params: Params): List<LearningSession> {
        return if (params.fromDate != null) {
            sessionRepository.getSessionsSince(params.userId, params.fromDate)
        } else {
            sessionRepository.getRecentSessions(params.userId, params.limit)
        }
    }
}