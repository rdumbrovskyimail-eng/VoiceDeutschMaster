package com.voicedeutsch.master.domain.model.book

import kotlinx.serialization.Serializable

/**
 * Top-level book metadata parsed from `assets/book/metadata.json`.
 *
 * The book consists of chapters, each containing lessons.
 * Structure: ~20 chapters, ~120 lessons total.
 */
@Serializable
data class BookMetadata(
    val title: String,
    val titleRu: String,
    val author: String = "",
    val edition: String = "",
    val targetLevel: String = "",    // "A1-B1"
    val totalChapters: Int,
    val totalLessons: Int,
    val description: String = "",
    val chapters: List<ChapterInfo>
)

/**
 * Lightweight chapter descriptor used in [BookMetadata].
 */
@Serializable
data class ChapterInfo(
    val number: Int,
    val titleDe: String,
    val titleRu: String,
    val level: String,               // "A1.1"
    val topics: List<String> = emptyList(),
    val lessonsCount: Int,
    val estimatedHours: Float = 0f
)

/**
 * A fully loaded chapter with its lessons and supplementary information.
 */
@Serializable
data class Chapter(
    val number: Int,
    val titleDe: String,
    val titleRu: String,
    val level: String,
    val lessons: List<Lesson>,
    val grammarTopics: List<String> = emptyList(),
    val culturalNotes: List<String> = emptyList()
)

/**
 * A single lesson within a chapter.
 *
 * **Function Calls:**
 * - `get_current_lesson` — returns the current lesson
 * - `advance_to_next_lesson` — moves to the next lesson
 * - `mark_lesson_complete` — marks the lesson as completed
 */
@Serializable
data class Lesson(
    val number: Int,
    val chapterNumber: Int,
    val titleDe: String,
    val titleRu: String,
    val focus: LessonFocus = LessonFocus.MIXED,
    val newWordsCount: Int = 0,
    val estimatedMinutes: Int = 30,
    val content: LessonContent? = null
)

/**
 * The textual content of a lesson — introduction, main body, phonetic notes,
 * exercise markers, and inline vocabulary.
 */
@Serializable
data class LessonContent(
    val title: String = "",
    val introduction: String = "",
    val mainContent: String = "",
    val phoneticNotes: String = "",
    val exerciseMarkers: List<String> = emptyList(),
    val vocabulary: List<LessonVocabularyEntry> = emptyList()
)

/**
 * A vocabulary entry embedded in lesson content.
 */
@Serializable
data class LessonVocabularyEntry(
    val german: String,
    val russian: String,
    val gender: String? = null,
    val plural: String? = null,
    val level: String = "A1"
)

/**
 * Primary pedagogical focus of a lesson.
 */
@Serializable
enum class LessonFocus {
    VOCABULARY, GRAMMAR, PRONUNCIATION, LISTENING,
    READING, SPEAKING, MIXED
}