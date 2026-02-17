package com.voicedeutsch.master.domain.model.progress

import com.voicedeutsch.master.domain.model.user.CefrLevel
import kotlinx.serialization.Serializable

/**
 * Aggregated progress across all skill dimensions.
 *
 * Assembled from multiple repository queries and displayed on the dashboard.
 */
data class OverallProgress(
    val currentCefrLevel: CefrLevel,
    val currentSubLevel: Int,
    val vocabularyProgress: VocabularyProgress,
    val grammarProgress: GrammarProgress,
    val pronunciationProgress: PronunciationProgress,
    val bookProgress: BookOverallProgress,
    val streakDays: Int,
    val totalHours: Float,
    val totalSessions: Int
)

/**
 * Vocabulary dimension of the overall progress.
 */
data class VocabularyProgress(
    val totalWords: Int,
    val byLevel: Map<Int, Int>,          // knowledge level -> word count
    val byTopic: Map<String, TopicProgress>,
    val activeWords: Int,                 // knowledge level >= 5
    val passiveWords: Int,                // knowledge level 2-4
    val wordsForReviewToday: Int
)

/**
 * Progress within a single vocabulary topic.
 */
data class TopicProgress(
    val known: Int,
    val total: Int
) {
    /** Completion percentage for the topic. */
    val percentage: Float get() = if (total == 0) 0f else known.toFloat() / total
}

/**
 * Grammar dimension of the overall progress.
 */
data class GrammarProgress(
    val totalRules: Int,
    val knownRules: Int,
    val byCategory: Map<String, Int>,     // category name -> known count
    val rulesForReviewToday: Int
)

/**
 * Pronunciation dimension of the overall progress.
 */
data class PronunciationProgress(
    val overallScore: Float,
    val problemSounds: List<String>,
    val goodSounds: List<String>,
    val trend: String                     // "improving" / "stable" / "declining"
)

/**
 * Book completion dimension of the overall progress.
 */
data class BookOverallProgress(
    val currentChapter: Int,
    val currentLesson: Int,
    val totalChapters: Int,
    val totalLessons: Int,
    val completionPercentage: Float,
    val currentTopic: String
)

/**
 * Daily progress snapshot persisted for streak and analytics tracking.
 */
@Serializable
data class DailyProgress(
    val id: String,
    val userId: String,
    val date: String,                     // YYYY-MM-DD
    val sessionsCount: Int = 0,
    val totalMinutes: Int = 0,
    val wordsLearned: Int = 0,
    val wordsReviewed: Int = 0,
    val exercisesCompleted: Int = 0,
    val exercisesCorrect: Int = 0,
    val averageScore: Float = 0f,
    val streakMaintained: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Skill radar — normalized scores for each language skill.
 */
data class SkillProgress(
    val vocabulary: Float,                // 0.0 - 1.0
    val grammar: Float,
    val pronunciation: Float,
    val listening: Float,
    val speaking: Float
)