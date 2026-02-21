package com.voicedeutsch.master.domain.usecase.achievement

import com.voicedeutsch.master.domain.model.achievement.UserAchievement
import com.voicedeutsch.master.domain.repository.AchievementRepository
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.domain.repository.ProgressRepository
import com.voicedeutsch.master.domain.repository.UserRepository

/**
 * Checks all achievement conditions and grants any newly earned ones.
 * Called after every session ends and after every significant action.
 */
class CheckAchievementsUseCase(
    private val achievementRepository: AchievementRepository,
    private val userRepository: UserRepository,
    private val knowledgeRepository: KnowledgeRepository,
    private val progressRepository: ProgressRepository,
) {

    suspend operator fun invoke(userId: String): List<UserAchievement> {
        val allAchievements = achievementRepository.getAllAchievements()
        val profile = userRepository.getUserProfile(userId) ?: return emptyList()
        val stats = userRepository.getUserStatistics(userId)
        val granted = mutableListOf<UserAchievement>()

        for (achievement in allAchievements) {
            if (achievementRepository.hasAchievement(userId, achievement.id)) continue

            val earned = when (achievement.condition.type) {
                "streak_days" -> profile.streakDays >= achievement.condition.threshold
                "word_count" -> (stats?.totalWordsLearned ?: 0) >= achievement.condition.threshold
                "rule_count" -> (stats?.totalRulesLearned ?: 0) >= achievement.condition.threshold
                "total_minutes" -> (stats?.totalMinutes ?: 0) >= achievement.condition.threshold
                "chapters_completed" -> {
                    val count = progressRepository.getCompletedChapterCount(userId)
                    count >= achievement.condition.threshold
                }
                "cefr_level" -> {
                    val levelOrdinal = when (profile.cefrLevel.level) {
                        "A1" -> 1; "A2" -> 2; "B1" -> 3; "B2" -> 4; "C1" -> 5; "C2" -> 6
                        else -> 0
                    }
                    levelOrdinal >= achievement.condition.threshold
                }
                "perfect_pronunciation" -> {
                    val count = knowledgeRepository.getPerfectPronunciationCount(userId)
                    count >= achievement.condition.threshold
                }
                "avg_pronunciation" -> {
                    val avg = knowledgeRepository.getAveragePronunciationScore(userId)
                    (avg * 100).toInt() >= achievement.condition.threshold
                }
                else -> false
            }

            if (earned) {
                achievementRepository.grantAchievement(userId, achievement.id)?.let {
                    granted.add(it)
                }
            }
        }
        return granted
    }
}