package com.voicedeutsch.master.domain.model.knowledge

import kotlinx.serialization.Serializable

/**
 * A complete snapshot of the user's knowledge state.
 *
 * **This is the KEY model of the system.** It is assembled from Room DB
 * before every session and passed to `ContextBuilder`, which serializes it
 * to JSON for the Gemini context window.
 *
 * **Lifecycle:**
 * 1. Formed in `BuildKnowledgeSummaryUseCase`
 * 2. Passed to `ContextBuilder` → `UserContextProvider`
 * 3. Serialized to JSON and sent to Gemini
 * 4. Rebuilt at the start of each session
 */
@Serializable
data class KnowledgeSnapshot(
    val vocabulary: VocabularySnapshot,
    val grammar: GrammarSnapshot,
    val pronunciation: PronunciationSnapshot,
    val bookProgress: BookProgressSnapshot,
    val sessionHistory: SessionHistorySnapshot,
    val weakPoints: List<String>,
    val recommendations: RecommendationsSnapshot
)

/**
 * Vocabulary portion of the knowledge snapshot.
 */
@Serializable
data class VocabularySnapshot(
    val totalWords: Int,
    val byLevel: Map<Int, Int>,                  // knowledge level -> count
    val byTopic: Map<String, TopicStats>,
    val recentNewWords: List<String>,
    val problemWords: List<ProblemWordInfo>,
    val wordsForReviewToday: Int
)

/**
 * Known/total counts for a vocabulary topic.
 */
@Serializable
data class TopicStats(
    val known: Int,
    val total: Int
)

/**
 * Information about a word the user struggles with.
 */
@Serializable
data class ProblemWordInfo(
    val word: String,
    val level: Int,
    val attempts: Int
)

/**
 * Grammar portion of the knowledge snapshot.
 */
@Serializable
data class GrammarSnapshot(
    val totalRules: Int,
    val byLevel: Map<Int, Int>,
    val knownRules: List<KnownRuleInfo>,
    val problemRules: List<String>,
    val rulesForReviewToday: Int
)

/**
 * Brief info about a grammar rule the user knows.
 */
@Serializable
data class KnownRuleInfo(
    val name: String,
    val level: Int
)

/**
 * Pronunciation portion of the knowledge snapshot.
 */
@Serializable
data class PronunciationSnapshot(
    val overallScore: Float,
    val problemSounds: List<String>,
    val goodSounds: List<String>,
    val averageWordScore: Float,
    val trend: String
)

/**
 * Book progress portion of the knowledge snapshot.
 */
@Serializable
data class BookProgressSnapshot(
    val currentChapter: Int,
    val currentLesson: Int,
    val totalChapters: Int,
    val completionPercentage: Float,
    val currentTopic: String
)

/**
 * Recent session history for the knowledge snapshot.
 */
@Serializable
data class SessionHistorySnapshot(
    val lastSession: String,
    val lastSessionSummary: String,
    val averageSessionDuration: String,
    val streak: Int,
    val totalSessions: Int
)

/**
 * AI-facing recommendations derived from the current knowledge state.
 *
 * **Strategy selection priority:**
 * 1. SRS queue > 10 → REPETITION
 * 2. Weak points > 5 → GAP_FILLING
 * 3. Skill gap > 2 sub-levels → corresponding strategy
 * 4. Pronunciation gap > 3 days → PRONUNCIATION
 * 5. Default → LINEAR_BOOK
 */
@Serializable
data class RecommendationsSnapshot(
    val primaryStrategy: String,
    val secondaryStrategy: String,
    val focusAreas: List<String>,
    val suggestedSessionDuration: String
)