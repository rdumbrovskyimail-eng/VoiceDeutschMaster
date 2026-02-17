package com.voicedeutsch.master.domain.model.book

import kotlinx.serialization.Serializable

/**
 * Tracks a user's progress on a specific lesson.
 *
 * Each record represents the state of one (chapter, lesson) pair for one user.
 */
@Serializable
data class BookProgress(
    val id: String,
    val userId: String,
    val chapter: Int,
    val lesson: Int,
    val status: LessonStatus = LessonStatus.NOT_STARTED,
    val score: Float? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val timesPracticed: Int = 0,
    val notes: String? = null
) {
    /** Convenience check for completion. */
    val isCompleted: Boolean get() = status == LessonStatus.COMPLETED
}

/**
 * Lifecycle status of a lesson.
 */
@Serializable
enum class LessonStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}