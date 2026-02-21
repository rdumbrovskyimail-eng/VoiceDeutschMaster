package com.voicedeutsch.master.domain.usecase.achievement

import com.voicedeutsch.master.domain.model.achievement.UserAchievement
import com.voicedeutsch.master.domain.repository.AchievementRepository

/**
 * Returns achievements that Voice hasn't yet announced to the user.
 * Called by FunctionRouter for get_unannounced_achievements().
 */
class GetUnannouncedAchievementsUseCase(
    private val achievementRepository: AchievementRepository
) {
    suspend operator fun invoke(userId: String): List<UserAchievement> {
        return achievementRepository.getUnannouncedAchievements(userId)
    }
}