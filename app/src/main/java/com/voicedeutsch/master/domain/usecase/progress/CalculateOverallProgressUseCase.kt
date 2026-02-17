package com.voicedeutsch.master.domain.usecase.progress

import com.voicedeutsch.master.domain.model.progress.OverallProgress
import com.voicedeutsch.master.domain.repository.ProgressRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

/**
 * Calculates overall learning progress for the Dashboard.
 * Provides both one-shot and observable (Flow) access.
 */
class CalculateOverallProgressUseCase(
    private val progressRepository: ProgressRepository,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(userId: String): OverallProgress {
        return progressRepository.calculateOverallProgress(userId)
    }

    fun observeProgress(userId: String): Flow<OverallProgress> {
        return progressRepository.getOverallProgressFlow(userId)
    }
}