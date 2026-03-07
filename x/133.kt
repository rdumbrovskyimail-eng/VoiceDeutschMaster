// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/BookProgressEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BookProgressEntityTest {

    private fun createEntity(
        id: String = "bp_001",
        userId: String = "user_001",
        chapter: Int = 1,
        lesson: Int = 1,
        status: String = "NOT_STARTED",
        score: Float? = null,
        startedAt: Long? = null,
        completedAt: Long? = null,
        timesPracticed: Int = 0,
        notes: String? = null,
    ) = BookProgressEntity(
        id = id, userId = userId, chapter = chapter, lesson = lesson,
        status = status, score = score, startedAt = startedAt,
        completedAt = completedAt, timesPracticed = timesPracticed, notes = notes,
    )

    @Test
    fun creation_withRequiredFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals("bp_001", entity.id)
        assertEquals("user_001", entity.userId)
        assertEquals(1, entity.chapter)
        assertEquals(1, entity.lesson)
        assertEquals("NOT_STARTED", entity.status)
        assertNull(entity.score)
        assertNull(entity.startedAt)
        assertNull(entity.completedAt)
        assertEquals(0, entity.timesPracticed)
        assertNull(entity.notes)
    }

    @Test fun creation_statusInProgress_isStored() = assertEquals("IN_PROGRESS", createEntity(status = "IN_PROGRESS").status)
    @Test fun creation_statusCompleted_isStored() = assertEquals("COMPLETED", createEntity(status = "COMPLETED").status)
    @Test fun creation_withScore_scoreIsStored() = assertEquals(0.85f, createEntity(score = 0.85f).score!!, 0.001f)

    @Test
    fun creation_withTimestamps_timestampsAreStored() {
        val entity = createEntity(startedAt = 1_000L, completedAt = 2_000L)
        assertEquals(1_000L, entity.startedAt)
        assertEquals(2_000L, entity.completedAt)
    }

    private fun minimal() = BookProgressEntity(id = "bp_002", userId = "user_001", chapter = 1, lesson = 1)

    @Test fun defaultStatus_isNotStarted() = assertEquals("NOT_STARTED", minimal().status)
    @Test fun defaultScore_isNull() = assertNull(minimal().score)
    @Test fun defaultTimesPracticed_isZero() = assertEquals(0, minimal().timesPracticed)

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentId_returnsFalse() = assertNotEquals(createEntity(id = "bp_001"), createEntity(id = "bp_002"))
    @Test fun equals_differentChapter_returnsFalse() = assertNotEquals(createEntity(chapter = 1), createEntity(chapter = 2))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_withNewStatus_onlyStatusChanges() {
        val original = createEntity(status = "NOT_STARTED")
        val copied = original.copy(status = "COMPLETED")
        assertEquals("COMPLETED", copied.status)
        assertEquals(original.id, copied.id)
        assertEquals(original.chapter, copied.chapter)
    }

    @Test
    fun copy_withNewScore_onlyScoreChanges() {
        val original = createEntity(score = null)
        val copied = original.copy(score = 1.0f)
        assertEquals(1.0f, copied.score!!, 0.001f)
        assertEquals(original.status, copied.status)
    }

    @Test
    fun copy_incrementTimesPracticed_valueUpdated() {
        val original = createEntity(timesPracticed = 3)
        val copied = original.copy(timesPracticed = 4)
        assertEquals(4, copied.timesPracticed)
        assertEquals(original.id, copied.id)
    }

    @Test fun score_zeroFloat_isAllowed() = assertEquals(0f, createEntity(score = 0f).score!!, 0.001f)
    @Test fun score_maxFloat_isAllowed() = assertEquals(1.0f, createEntity(score = 1.0f).score!!, 0.001f)
    @Test fun notes_withValue_isStoredCorrectly() = assertEquals("Повторить артикли", createEntity(notes = "Повторить артикли").notes)
}
