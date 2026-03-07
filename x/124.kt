// Путь: src/test/java/com/voicedeutsch/master/domain/model/book/BookProgressTest.kt
package com.voicedeutsch.master.domain.model.book

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ═══════════════════════════════════════════════════════════════════════════
// BookProgress
// ═══════════════════════════════════════════════════════════════════════════

class BookProgressTest {

    private fun createBookProgress(
        id: String = "bp_1",
        userId: String = "user_1",
        chapter: Int = 1,
        lesson: Int = 1,
        status: LessonStatus = LessonStatus.NOT_STARTED,
        score: Float? = null,
        startedAt: Long? = null,
        completedAt: Long? = null,
        timesPracticed: Int = 0,
        notes: String? = null
    ) = BookProgress(
        id = id,
        userId = userId,
        chapter = chapter,
        lesson = lesson,
        status = status,
        score = score,
        startedAt = startedAt,
        completedAt = completedAt,
        timesPracticed = timesPracticed,
        notes = notes
    )

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val progress = BookProgress(id = "bp_1", userId = "u1", chapter = 1, lesson = 1)
        assertEquals(LessonStatus.NOT_STARTED, progress.status)
        assertNull(progress.score)
        assertNull(progress.startedAt)
        assertNull(progress.completedAt)
        assertEquals(0, progress.timesPracticed)
        assertNull(progress.notes)
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val progress = createBookProgress(
            id = "bp_42",
            userId = "u99",
            chapter = 5,
            lesson = 3,
            status = LessonStatus.COMPLETED,
            score = 0.95f,
            startedAt = 1_000_000L,
            completedAt = 2_000_000L,
            timesPracticed = 4,
            notes = "Tricky grammar"
        )
        assertEquals("bp_42", progress.id)
        assertEquals("u99", progress.userId)
        assertEquals(5, progress.chapter)
        assertEquals(3, progress.lesson)
        assertEquals(LessonStatus.COMPLETED, progress.status)
        assertEquals(0.95f, progress.score!!, 0.001f)
        assertEquals(1_000_000L, progress.startedAt)
        assertEquals(2_000_000L, progress.completedAt)
        assertEquals(4, progress.timesPracticed)
        assertEquals("Tricky grammar", progress.notes)
    }

    @Test
    fun constructor_inProgressStatus_storedCorrectly() {
        val progress = createBookProgress(status = LessonStatus.IN_PROGRESS)
        assertEquals(LessonStatus.IN_PROGRESS, progress.status)
    }

    @Test
    fun constructor_scoreZero_storedCorrectly() {
        val progress = createBookProgress(score = 0.0f)
        assertEquals(0.0f, progress.score!!, 0.001f)
    }

    @Test
    fun constructor_scorePerfect_storedCorrectly() {
        val progress = createBookProgress(score = 1.0f)
        assertEquals(1.0f, progress.score!!, 0.001f)
    }

    @Test
    fun constructor_nullScore_isNull() {
        val progress = createBookProgress(score = null)
        assertNull(progress.score)
    }

    @Test
    fun constructor_nullNotes_isNull() {
        val progress = createBookProgress(notes = null)
        assertNull(progress.notes)
    }

    @Test
    fun constructor_withNotes_notesStored() {
        val progress = createBookProgress(notes = "Review again")
        assertEquals("Review again", progress.notes)
    }

    // ── isCompleted ───────────────────────────────────────────────────────

    @Test
    fun isCompleted_statusCompleted_returnsTrue() {
        val progress = createBookProgress(status = LessonStatus.COMPLETED)
        assertTrue(progress.isCompleted)
    }

    @Test
    fun isCompleted_statusNotStarted_returnsFalse() {
        val progress = createBookProgress(status = LessonStatus.NOT_STARTED)
        assertFalse(progress.isCompleted)
    }

    @Test
    fun isCompleted_statusInProgress_returnsFalse() {
        val progress = createBookProgress(status = LessonStatus.IN_PROGRESS)
        assertFalse(progress.isCompleted)
    }

    @Test
    fun isCompleted_afterCopyToCompleted_returnsTrue() {
        val original = createBookProgress(status = LessonStatus.IN_PROGRESS)
        val completed = original.copy(status = LessonStatus.COMPLETED)
        assertTrue(completed.isCompleted)
        assertFalse(original.isCompleted)
    }

    @Test
    fun isCompleted_afterCopyFromCompletedToInProgress_returnsFalse() {
        val original = createBookProgress(status = LessonStatus.COMPLETED)
        val regressed = original.copy(status = LessonStatus.IN_PROGRESS)
        assertFalse(regressed.isCompleted)
        assertTrue(original.isCompleted)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_changeStatus_onlyStatusChanges() {
        val original = createBookProgress(status = LessonStatus.NOT_STARTED)
        val modified = original.copy(status = LessonStatus.IN_PROGRESS)
        assertEquals(LessonStatus.IN_PROGRESS, modified.status)
        assertEquals(original.id, modified.id)
        assertEquals(original.chapter, modified.chapter)
        assertEquals(original.lesson, modified.lesson)
    }

    @Test
    fun copy_setScore_scoreUpdated() {
        val original = createBookProgress(score = null)
        val modified = original.copy(score = 0.85f)
        assertEquals(0.85f, modified.score!!, 0.001f)
        assertNull(original.score)
    }

    @Test
    fun copy_setCompletedAt_timestampUpdated() {
        val original = createBookProgress(completedAt = null)
        val modified = original.copy(completedAt = 5_000_000L)
        assertEquals(5_000_000L, modified.completedAt)
        assertNull(original.completedAt)
    }

    @Test
    fun copy_incrementTimesPracticed_countUpdated() {
        val original = createBookProgress(timesPracticed = 2)
        val modified = original.copy(timesPracticed = 3)
        assertEquals(3, modified.timesPracticed)
        assertEquals(2, original.timesPracticed)
    }

    @Test
    fun copy_setNotes_notesUpdated() {
        val original = createBookProgress(notes = null)
        val modified = original.copy(notes = "Needs review")
        assertEquals("Needs review", modified.notes)
        assertNull(original.notes)
    }

    @Test
    fun copy_clearNotes_notesBecomesNull() {
        val original = createBookProgress(notes = "Some note")
        val modified = original.copy(notes = null)
        assertNull(modified.notes)
        assertEquals("Some note", original.notes)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createBookProgress(), createBookProgress())
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(createBookProgress().hashCode(), createBookProgress().hashCode())
    }

    @Test
    fun equals_differentId_notEqual() {
        assertNotEquals(createBookProgress(id = "bp_1"), createBookProgress(id = "bp_2"))
    }

    @Test
    fun equals_differentChapter_notEqual() {
        assertNotEquals(createBookProgress(chapter = 1), createBookProgress(chapter = 2))
    }

    @Test
    fun equals_differentLesson_notEqual() {
        assertNotEquals(createBookProgress(lesson = 1), createBookProgress(lesson = 2))
    }

    @Test
    fun equals_differentStatus_notEqual() {
        assertNotEquals(
            createBookProgress(status = LessonStatus.NOT_STARTED),
            createBookProgress(status = LessonStatus.COMPLETED)
        )
    }

    @Test
    fun equals_differentScore_notEqual() {
        assertNotEquals(createBookProgress(score = 0.5f), createBookProgress(score = 0.9f))
    }

    @Test
    fun equals_nullScoreVsNonNull_notEqual() {
        assertNotEquals(createBookProgress(score = null), createBookProgress(score = 0.5f))
    }

    @Test
    fun equals_differentTimesPracticed_notEqual() {
        assertNotEquals(
            createBookProgress(timesPracticed = 0),
            createBookProgress(timesPracticed = 5)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LessonStatus
// ═══════════════════════════════════════════════════════════════════════════

class LessonStatusTest {

    @Test
    fun entries_size_isThree() {
        assertEquals(3, LessonStatus.entries.size)
    }

    @Test
    fun entries_containsNotStarted() {
        assertTrue(LessonStatus.entries.contains(LessonStatus.NOT_STARTED))
    }

    @Test
    fun entries_containsInProgress() {
        assertTrue(LessonStatus.entries.contains(LessonStatus.IN_PROGRESS))
    }

    @Test
    fun entries_containsCompleted() {
        assertTrue(LessonStatus.entries.contains(LessonStatus.COMPLETED))
    }

    @Test
    fun ordinal_notStarted_isZero() {
        assertEquals(0, LessonStatus.NOT_STARTED.ordinal)
    }

    @Test
    fun ordinal_inProgress_isOne() {
        assertEquals(1, LessonStatus.IN_PROGRESS.ordinal)
    }

    @Test
    fun ordinal_completed_isTwo() {
        assertEquals(2, LessonStatus.COMPLETED.ordinal)
    }

    @Test
    fun valueOf_notStarted_returnsNotStarted() {
        assertEquals(LessonStatus.NOT_STARTED, LessonStatus.valueOf("NOT_STARTED"))
    }

    @Test
    fun valueOf_inProgress_returnsInProgress() {
        assertEquals(LessonStatus.IN_PROGRESS, LessonStatus.valueOf("IN_PROGRESS"))
    }

    @Test
    fun valueOf_completed_returnsCompleted() {
        assertEquals(LessonStatus.COMPLETED, LessonStatus.valueOf("COMPLETED"))
    }

    @Test
    fun valueOf_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            LessonStatus.valueOf("SKIPPED")
        }
    }

    @Test
    fun defaultBookProgress_usesNotStartedStatus() {
        val progress = BookProgress(id = "bp_1", userId = "u1", chapter = 1, lesson = 1)
        assertEquals(LessonStatus.NOT_STARTED, progress.status)
    }

    @Test
    fun isCompleted_onlyCompletedStatusReturnsTrue() {
        LessonStatus.entries.forEach { status ->
            val progress = BookProgress(
                id = "bp_1", userId = "u1", chapter = 1, lesson = 1, status = status
            )
            if (status == LessonStatus.COMPLETED) {
                assertTrue(progress.isCompleted, "Expected isCompleted=true for $status")
            } else {
                assertFalse(progress.isCompleted, "Expected isCompleted=false for $status")
            }
        }
    }
}
