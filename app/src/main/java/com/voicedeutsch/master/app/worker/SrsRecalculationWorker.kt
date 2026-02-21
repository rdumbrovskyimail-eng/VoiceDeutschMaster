package com.voicedeutsch.master.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.domain.usecase.achievement.CheckAchievementsUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Daily background recalculation of SRS intervals and achievement checks.
 * Scheduled by WorkManager to run once every 24 hours.
 *
 * Architecture lines 584-592 (WorkManager) and 1730-1734 (Post-processing).
 */
class SrsRecalculationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val userRepository: UserRepository by inject()
    private val knowledgeRepository: KnowledgeRepository by inject()
    private val checkAchievements: CheckAchievementsUseCase by inject()

    override suspend fun doWork(): Result {
        return runCatching {
            val users = userRepository.getAllUserIds()
            for (userId in users) {
                // Recalculate overdue SRS items
                knowledgeRepository.recalculateOverdueItems(userId)

                // Update streak (break if no session yesterday)
                userRepository.updateStreakIfNeeded(userId)

                // Check achievements
                checkAchievements(userId)
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val WORK_NAME = "srs_recalculation"
    }
}