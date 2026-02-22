package com.voicedeutsch.master.domain.usecase.achievement

import com.voicedeutsch.master.domain.model.achievement.UserAchievement
import com.voicedeutsch.master.domain.repository.AchievementRepository

class GetUserAchievementsUseCase(
    private val achievementRepository: AchievementRepository
) {
    suspend operator fun invoke(userId: String): List<UserAchievement> {
        return achievementRepository.getUserAchievements(userId)
    }
}