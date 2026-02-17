package com.voicedeutsch.master.domain.model.user

import kotlinx.serialization.Serializable

/**
 * User learning preferences.
 *
 * Controls session duration, daily goals, SRS behaviour, reminders,
 * pronunciation strictness, topic interests, and data-saving options.
 */
@Serializable
data class UserPreferences(
    val preferredSessionDuration: Int = 30,
    val dailyGoalWords: Int = 10,
    val dailyGoalMinutes: Int = 30,
    val learningPace: LearningPace = LearningPace.NORMAL,
    val srsEnabled: Boolean = true,
    val maxReviewsPerSession: Int = 30,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val pronunciationStrictness: PronunciationStrictness = PronunciationStrictness.MODERATE,
    val topicsOfInterest: List<String> = emptyList(),
    val professionalDomain: String? = null,
    val offlineModeEnabled: Boolean = false,
    val dataSavingMode: Boolean = false
)

/**
 * Pace at which new material is introduced.
 */
@Serializable
enum class LearningPace { SLOW, NORMAL, FAST }

/**
 * How strictly pronunciation errors are flagged.
 */
@Serializable
enum class PronunciationStrictness { LENIENT, MODERATE, STRICT }