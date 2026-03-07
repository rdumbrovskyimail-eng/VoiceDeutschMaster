// Путь: src/test/java/com/voicedeutsch/master/domain/model/session/LearningSessionTest.kt
package com.voicedeutsch.master.domain.model.session

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ═══════════════════════════════════════════════════════════════════════════
// LearningSession
// ═══════════════════════════════════════════════════════════════════════════

class LearningSessionTest {

    private fun createSession(
        id: String = "session_1",
        userId: String = "user_1",
        startedAt: Long = 1_000_000L,
        endedAt: Long? = null,
        durationMinutes: Int = 0,
        strategiesUsed: List<String> = emptyList(),
        wordsLearned: Int = 0,
        wordsReviewed: Int = 0,
        rulesPracticed: Int = 0,
        exercisesCompleted: Int = 0,
        exercisesCorrect: Int = 0,
        averagePronunciationScore: Float = 0f,
        bookChapterStart: Int = 0,
        bookLessonStart: Int = 0,
        bookChapterEnd: Int = 0,
        bookLessonEnd: Int = 0,
        sessionSummary: String = "",
        moodEstimate: MoodEstimate? = null
    ) = LearningSession(
        id = id,
        userId = userId,
        startedAt = startedAt,
        endedAt = endedAt,
        durationMinutes = durationMinutes,
        strategiesUsed = strategiesUsed,
        wordsLearned = wordsLearned,
        wordsReviewed = wordsReviewed,
        rulesPracticed = rulesPracticed,
        exercisesCompleted = exercisesCompleted,
        exercisesCorrect = exercisesCorrect,
        averagePronunciationScore = averagePronunciationScore,
        bookChapterStart = bookChapterStart,
        bookLessonStart = bookLessonStart,
        bookChapterEnd = bookChapterEnd,
        bookLessonEnd = bookLessonEnd,
        sessionSummary = sessionSummary,
        moodEstimate = moodEstimate
    )

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val session = LearningSession(id = "s_1", userId = "u1", startedAt = 1_000L)
        assertNull(session.endedAt)
        assertEquals(0, session.durationMinutes)
        assertTrue(session.strategiesUsed.isEmpty())
        assertEquals(0, session.wordsLearned)
        assertEquals(0, session.wordsReviewed)
        assertEquals(0, session.rulesPracticed)
        assertEquals(0, session.exercisesCompleted)
        assertEquals(0, session.exercisesCorrect)
        assertEquals(0f, session.averagePronunciationScore, 0.001f)
        assertEquals(0, session.bookChapterStart)
        assertEquals(0, session.bookLessonStart)
        assertEquals(0, session.bookChapterEnd)
        assertEquals(0, session.bookLessonEnd)
        assertEquals("", session.sessionSummary)
        assertNull(session.moodEstimate)
        assertTrue(session.createdAt > 0)
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val strategies = listOf("REPETITION", "GAP_FILLING")
        val session = createSession(
            id = "s_99",
            userId = "u42",
            startedAt = 1_000_000L,
            endedAt = 2_000_000L,
            durationMinutes = 45,
            strategiesUsed = strategies,
            wordsLearned = 15,
            wordsReviewed = 30,
            rulesPracticed = 5,
            exercisesCompleted = 20,
            exercisesCorrect = 18,
            averagePronunciationScore = 0.85f,
            bookChapterStart = 2,
            bookLessonStart = 3,
            bookChapterEnd = 2,
            bookLessonEnd = 5,
            sessionSummary = "Great session!",
            moodEstimate = MoodEstimate.MOTIVATED
        )
        assertEquals("s_99", session.id)
        assertEquals("u42", session.userId)
        assertEquals(1_000_000L, session.startedAt)
        assertEquals(2_000_000L, session.endedAt)
        assertEquals(45, session.durationMinutes)
        assertEquals(strategies, session.strategiesUsed)
        assertEquals(15, session.wordsLearned)
        assertEquals(30, session.wordsReviewed)
        assertEquals(5, session.rulesPracticed)
        assertEquals(20, session.exercisesCompleted)
        assertEquals(18, session.exercisesCorrect)
        assertEquals(0.85f, session.averagePronunciationScore, 0.001f)
        assertEquals(2, session.bookChapterStart)
        assertEquals(3, session.bookLessonStart)
        assertEquals(2, session.bookChapterEnd)
        assertEquals(5, session.bookLessonEnd)
        assertEquals("Great session!", session.sessionSummary)
        assertEquals(MoodEstimate.MOTIVATED, session.moodEstimate)
    }

    @Test
    fun constructor_nullEndedAt_isNull() {
        val session = createSession(endedAt = null)
        assertNull(session.endedAt)
    }

    @Test
    fun constructor_withEndedAt_storedCorrectly() {
        val session = createSession(endedAt = 5_000_000L)
        assertEquals(5_000_000L, session.endedAt)
    }

    @Test
    fun constructor_nullMoodEstimate_isNull() {
        val session = createSession(moodEstimate = null)
        assertNull(session.moodEstimate)
    }

    @Test
    fun constructor_allMoodEstimates_storedCorrectly() {
        MoodEstimate.entries.forEach { mood ->
            val session = createSession(moodEstimate = mood)
            assertEquals(mood, session.moodEstimate)
        }
    }

    // ── accuracy ──────────────────────────────────────────────────────────

    @Test
    fun accuracy_zeroExercisesCompleted_returnsZero() {
        val session = createSession(exercisesCompleted = 0, exercisesCorrect = 0)
        assertEquals(0f, session.accuracy, 0.001f)
    }

    @Test
    fun accuracy_zeroExercisesCompletedNonZeroCorrect_returnsZero() {
        val session = createSession(exercisesCompleted = 0, exercisesCorrect = 5)
        assertEquals(0f, session.accuracy, 0.001f)
    }

    @Test
    fun accuracy_allCorrect_returnsOne() {
        val session = createSession(exercisesCompleted = 10, exercisesCorrect = 10)
        assertEquals(1.0f, session.accuracy, 0.001f)
    }

    @Test
    fun accuracy_halfCorrect_returnsHalf() {
        val session = createSession(exercisesCompleted = 10, exercisesCorrect = 5)
        assertEquals(0.5f, session.accuracy, 0.001f)
    }

    @Test
    fun accuracy_noneCorrect_returnsZero() {
        val session = createSession(exercisesCompleted = 10, exercisesCorrect = 0)
        assertEquals(0f, session.accuracy, 0.001f)
    }

    @Test
    fun accuracy_oneOfFive_returnsTwentyPercent() {
        val session = createSession(exercisesCompleted = 5, exercisesCorrect = 1)
        assertEquals(0.2f, session.accuracy, 0.001f)
    }

    @Test
    fun accuracy_largeNumbers_calculatedCorrectly() {
        val session = createSession(exercisesCompleted = 1000, exercisesCorrect = 873)
        assertEquals(873f / 1000f, session.accuracy, 0.001f)
    }

    @Test
    fun accuracy_afterCopyIncreasesCorrect_recalculated() {
        val original = createSession(exercisesCompleted = 10, exercisesCorrect = 5)
        val modified = original.copy(exercisesCorrect = 8)
        assertEquals(0.8f, modified.accuracy, 0.001f)
        assertEquals(0.5f, original.accuracy, 0.001f)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_setEndedAt_onlyEndedAtChanges() {
        val original = createSession(endedAt = null)
        val modified = original.copy(endedAt = 9_999_999L)
        assertEquals(9_999_999L, modified.endedAt)
        assertNull(original.endedAt)
        assertEquals(original.id, modified.id)
    }

    @Test
    fun copy_incrementWordsLearned_wordsUpdated() {
        val original = createSession(wordsLearned = 10)
        val modified = original.copy(wordsLearned = 20)
        assertEquals(20, modified.wordsLearned)
        assertEquals(10, original.wordsLearned)
    }

    @Test
    fun copy_changeMoodEstimate_moodUpdated() {
        val original = createSession(moodEstimate = MoodEstimate.NEUTRAL)
        val modified = original.copy(moodEstimate = MoodEstimate.TIRED)
        assertEquals(MoodEstimate.TIRED, modified.moodEstimate)
        assertEquals(MoodEstimate.NEUTRAL, original.moodEstimate)
    }

    @Test
    fun copy_clearMoodEstimate_becomesNull() {
        val original = createSession(moodEstimate = MoodEstimate.MOTIVATED)
        val modified = original.copy(moodEstimate = null)
        assertNull(modified.moodEstimate)
        assertEquals(MoodEstimate.MOTIVATED, original.moodEstimate)
    }

    @Test
    fun copy_addStrategies_strategiesUpdated() {
        val original = createSession(strategiesUsed = emptyList())
        val modified = original.copy(strategiesUsed = listOf("LINEAR_BOOK", "REPETITION"))
        assertEquals(listOf("LINEAR_BOOK", "REPETITION"), modified.strategiesUsed)
        assertTrue(original.strategiesUsed.isEmpty())
    }

    @Test
    fun copy_updateBookCoordinates_coordinatesUpdated() {
        val original = createSession(bookChapterEnd = 1, bookLessonEnd = 2)
        val modified = original.copy(bookChapterEnd = 3, bookLessonEnd = 5)
        assertEquals(3, modified.bookChapterEnd)
        assertEquals(5, modified.bookLessonEnd)
        assertEquals(1, original.bookChapterEnd)
        assertEquals(2, original.bookLessonEnd)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val ts = 1_000_000L
        val s1 = LearningSession(id = "s_1", userId = "u1", startedAt = ts, createdAt = ts)
        val s2 = LearningSession(id = "s_1", userId = "u1", startedAt = ts, createdAt = ts)
        assertEquals(s1, s2)
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        val ts = 1_000_000L
        val s1 = LearningSession(id = "s_1", userId = "u1", startedAt = ts, createdAt = ts)
        val s2 = LearningSession(id = "s_1", userId = "u1", startedAt = ts, createdAt = ts)
        assertEquals(s1.hashCode(), s2.hashCode())
    }

    @Test
    fun equals_differentId_notEqual() {
        assertNotEquals(createSession(id = "s_1"), createSession(id = "s_2"))
    }

    @Test
    fun equals_differentUserId_notEqual() {
        assertNotEquals(createSession(userId = "u1"), createSession(userId = "u2"))
    }

    @Test
    fun equals_differentDurationMinutes_notEqual() {
        assertNotEquals(createSession(durationMinutes = 30), createSession(durationMinutes = 60))
    }

    @Test
    fun equals_differentWordsLearned_notEqual() {
        assertNotEquals(createSession(wordsLearned = 5), createSession(wordsLearned = 10))
    }

    @Test
    fun equals_differentMoodEstimate_notEqual() {
        assertNotEquals(
            createSession(moodEstimate = MoodEstimate.MOTIVATED),
            createSession(moodEstimate = MoodEstimate.TIRED)
        )
    }

    @Test
    fun equals_nullVsNonNullMood_notEqual() {
        assertNotEquals(
            createSession(moodEstimate = null),
            createSession(moodEstimate = MoodEstimate.NEUTRAL)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MoodEstimate
// ═══════════════════════════════════════════════════════════════════════════

class MoodEstimateTest {

    @Test
    fun entries_size_isThree() {
        assertEquals(3, MoodEstimate.entries.size)
    }

    @Test
    fun entries_containsMotivated() {
        assertTrue(MoodEstimate.entries.contains(MoodEstimate.MOTIVATED))
    }

    @Test
    fun entries_containsNeutral() {
        assertTrue(MoodEstimate.entries.contains(MoodEstimate.NEUTRAL))
    }

    @Test
    fun entries_containsTired() {
        assertTrue(MoodEstimate.entries.contains(MoodEstimate.TIRED))
    }

    @Test
    fun ordinal_motivated_isZero() {
        assertEquals(0, MoodEstimate.MOTIVATED.ordinal)
    }

    @Test
    fun ordinal_neutral_isOne() {
        assertEquals(1, MoodEstimate.NEUTRAL.ordinal)
    }

    @Test
    fun ordinal_tired_isTwo() {
        assertEquals(2, MoodEstimate.TIRED.ordinal)
    }

    @Test
    fun valueOf_motivated_returnsMotivated() {
        assertEquals(MoodEstimate.MOTIVATED, MoodEstimate.valueOf("MOTIVATED"))
    }

    @Test
    fun valueOf_neutral_returnsNeutral() {
        assertEquals(MoodEstimate.NEUTRAL, MoodEstimate.valueOf("NEUTRAL"))
    }

    @Test
    fun valueOf_tired_returnsTired() {
        assertEquals(MoodEstimate.TIRED, MoodEstimate.valueOf("TIRED"))
    }

    @Test
    fun valueOf_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            MoodEstimate.valueOf("HAPPY")
        }
    }

    @Test
    fun valueOf_lowercaseValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            MoodEstimate.valueOf("motivated")
        }
    }

    @Test
    fun allEntries_names_areUnique() {
        val names = MoodEstimate.entries.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun defaultSession_moodEstimateIsNull() {
        val session = LearningSession(id = "s_1", userId = "u1", startedAt = 1_000L)
        assertNull(session.moodEstimate)
    }
}
