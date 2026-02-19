package com.voicedeutsch.master.domain.usecase.user

import com.voicedeutsch.master.domain.model.user.UserStatistics
import com.voicedeutsch.master.domain.repository.UserRepository

/**
 * Retrieves aggregated learning statistics for a user.
 *
 * Delegates to [UserRepository.getUserStatistics] which assembles the
 * [UserStatistics] from multiple Room DAO queries:
 *   - word/rule knowledge counts and levels
 *   - session history (count, duration, streak)
 *   - pronunciation averages
 *   - SRS review queue sizes
 *   - book progress
 *
 * Called by:
 *   - FunctionRouter.handleGetUserStatistics() → Gemini function call
 *   - StatisticsViewModel → Statistics screen
 *   - DashboardViewModel → Dashboard summary
 *
 * This is a read-only use case; it never mutates state.
 */
class GetUserStatisticsUseCase(
    private val userRepository: UserRepository,
) {
    /**
     * @param userId The active user's identifier.
     * @return Aggregated [UserStatistics] snapshot.
     */
    suspend operator fun invoke(userId: String): UserStatistics {
        return userRepository.getUserStatistics(userId)
    }
}
