package com.voicedeutsch.master.domain.model.user

/**
 * Aggregated user statistics derived from the knowledge database.
 *
 * Not serializable — this is a transient view model assembled by queries
 * in [com.voicedeutsch.master.domain.repository.UserRepository.getUserStatistics].
 */
data class UserStatistics(
    val totalWords: Int,
    val activeWords: Int,        // knowledge level >= 5
    val passiveWords: Int,       // knowledge level 2-4
    val totalRules: Int,
    val knownRules: Int,         // knowledge level >= 4
    val totalSessions: Int,
    val totalMinutes: Int,
    val streakDays: Int,
    val averageScore: Float,
    val averagePronunciationScore: Float,
    val wordsForReviewToday: Int,
    val rulesForReviewToday: Int,
    val bookProgress: Float,     // 0.0 - 1.0
    val currentChapter: Int,
    val totalChapters: Int
) {
    /**
     * Total learning time expressed in fractional hours.
     */
    val totalHours: Float get() = totalMinutes / 60f

    /**
     * Average number of new words learned per day of the current streak.
     */
    val wordsPerDay: Float get() {
        if (totalSessions == 0) return 0f
        return totalWords.toFloat() / maxOf(1, streakDays).toFloat()
    }
}