package com.voicedeutsch.master.domain.usecase.achievement

import com.voicedeutsch.master.domain.model.achievement.UserAchievement
import com.voicedeutsch.master.domain.repository.AchievementRepository

class GetUnannouncedAchievementsUseCase(
    private val achievementRepository: AchievementRepository
) {
    suspend operator fun invoke(userId: String): List<UserAchievement> {
        return achievementRepository.getUnannouncedAchievements(userId)
    }
}