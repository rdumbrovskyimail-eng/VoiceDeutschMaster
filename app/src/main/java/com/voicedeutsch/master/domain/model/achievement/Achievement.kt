package com.voicedeutsch.master.domain.model.achievement

import kotlinx.serialization.Serializable

@Serializable
data class Achievement(
    val id: String,
    val nameRu: String,
    val nameDe: String,
    val descriptionRu: String,
    val icon: String,
    val condition: AchievementCondition,
    val category: AchievementCategory
)

@Serializable
data class UserAchievement(
    val id: String,
    val userId: String,
    val achievementId: String,
    val earnedAt: Long,
    val announced: Boolean,
    val achievement: Achievement? = null // populated by join
)

@Serializable
enum class AchievementCategory {
    VOCABULARY, GRAMMAR, PRONUNCIATION, STREAK, SESSION, BOOK, TIME, CEFR, SPECIAL
}

@Serializable
data class AchievementCondition(
    val type: String,       // "word_count", "streak_days", "total_hours", etc.
    val threshold: Int,     // Значение порога
    val extra: String = ""  // Доп. параметры (JSON)
)