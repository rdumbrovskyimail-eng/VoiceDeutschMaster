// Path: src/test/java/com/voicedeutsch/master/domain/model/book/BookMetadataTest.kt
package com.voicedeutsch.master.domain.model.book

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ════════════════════════════════════════════════════════════════════════════
// ChapterInfo
// ════════════════════════════════════════════════════════════════════════════

class ChapterInfoTest {

    private fun makeChapterInfo(
        number: Int = 1,
        titleDe: String = "Guten Tag",
        titleRu: String = "Добрый день",
        level: String = "A1.1",
        topics: List<String> = emptyList(),
        lessonsCount: Int = 5,
        estimatedHours: Float = 2.0f,
    ) = ChapterInfo(
        number = number,
        titleDe = titleDe,
        titleRu = titleRu,
        level = level,
        topics = topics,
        lessonsCount = lessonsCount,
        estimatedHours = estimatedHours,
    )

    @Test
    fun creation_withRequiredFields_setsValues() {
        val info = makeChapterInfo()
        assertEquals(1, info.number)
        assertEquals("Guten Tag", info.titleDe)
        assertEquals("Добрый день", info.titleRu)
        assertEquals("A1.1", info.level)
        assertEquals(5, info.lessonsCount)
    }

    @Test
    fun creation_defaultTopics_isEmpty() {
        val info = makeChapterInfo()
        assertTrue(info.topics.isEmpty())
    }

    @Test
    fun creation_defaultEstimatedHours_isZero() {
        val info = ChapterInfo(
            number = 1,
            titleDe = "Test",
            titleRu = "Test",
            level = "A1",
            lessonsCount = 3,
        )
        assertEquals(0f, info.estimatedHours)
    }

    @Test
    fun creation_withTopics_setsTopics() {
        val topics = listOf("Greetings", "Numbers")
        val info = makeChapterInfo(topics = topics)
        assertEquals(2, info.topics.size)
        assertEquals("Greetings", info.topics[0])
    }

    @Test
    fun copy_changesLevel_restUnchanged() {
        val original = makeChapterInfo(level = "A1.1")
        val copy = original.copy(level = "A1.2")
        assertEquals("A1.2", copy.level)
        assertEquals(1, copy.number)
        assertEquals("Guten Tag", copy.titleDe)
    }

    @Test
    fun copy_changesLessonsCount() {
        val original = makeChapterInfo(lessonsCount = 5)
        val copy = original.copy(lessonsCount = 8)
        assertEquals(8, copy.lessonsCount)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeChapterInfo()
        val b = makeChapterInfo()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentNumber_areNotEqual() {
        val a = makeChapterInfo(number = 1)
        val b = makeChapterInfo(number = 2)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentEstimatedHours_areNotEqual() {
        val a = makeChapterInfo(estimatedHours = 1.0f)
        val b = makeChapterInfo(estimatedHours = 2.0f)
        assertNotEquals(a, b)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// BookMetadata
// ════════════════════════════════════════════════════════════════════════════

class BookMetadataTest {

    private fun makeChapterInfo(n: Int) = ChapterInfo(
        number = n,
        titleDe = "Kapitel $n",
        titleRu = "Глава $n",
        level = "A1",
        lessonsCount = 5,
    )

    private fun makeBookMetadata(
        title: String = "German Course",
        titleRu: String = "Немецкий курс",
        author: String = "",
        edition: String = "",
        targetLevel: String = "",
        totalChapters: Int = 20,
        totalLessons: Int = 120,
        description: String = "",
        chapters: List<ChapterInfo> = listOf(makeChapterInfo(1), makeChapterInfo(2)),
    ) = BookMetadata(
        title = title,
        titleRu = titleRu,
        author = author,
        edition = edition,
        targetLevel = targetLevel,
        totalChapters = totalChapters,
        totalLessons = totalLessons,
        description = description,
        chapters = chapters,
    )

    @Test
    fun creation_withRequiredFields_setsValues() {
        val meta = makeBookMetadata()
        assertEquals("German Course", meta.title)
        assertEquals("Немецкий курс", meta.titleRu)
        assertEquals(20, meta.totalChapters)
        assertEquals(120, meta.totalLessons)
    }

    @Test
    fun creation_defaultAuthor_isEmpty() {
        val meta = makeBookMetadata()
        assertEquals("", meta.author)
    }

    @Test
    fun creation_defaultEdition_isEmpty() {
        val meta = makeBookMetadata()
        assertEquals("", meta.edition)
    }

    @Test
    fun creation_defaultTargetLevel_isEmpty() {
        val meta = makeBookMetadata()
        assertEquals("", meta.targetLevel)
    }

    @Test
    fun creation_defaultDescription_isEmpty() {
        val meta = makeBookMetadata()
        assertEquals("", meta.description)
    }

    @Test
    fun creation_withFullData_setsAllFields() {
        val meta = makeBookMetadata(
            author = "Dr. Müller",
            edition = "3rd",
            targetLevel = "A1-B1",
            description = "Полный курс немецкого",
        )
        assertEquals("Dr. Müller", meta.author)
        assertEquals("3rd", meta.edition)
        assertEquals("A1-B1", meta.targetLevel)
        assertEquals("Полный курс немецкого", meta.description)
    }

    @Test
    fun creation_chaptersListIsStored() {
        val meta = makeBookMetadata()
        assertEquals(2, meta.chapters.size)
        assertEquals(1, meta.chapters[0].number)
        assertEquals(2, meta.chapters[1].number)
    }

    @Test
    fun copy_changesTotalLessons_restUnchanged() {
        val original = makeBookMetadata(totalLessons = 120)
        val copy = original.copy(totalLessons = 150)
        assertEquals(150, copy.totalLessons)
        assertEquals("German Course", copy.title)
        assertEquals(20, copy.totalChapters)
    }

    @Test
    fun copy_changesTotalChapters() {
        val original = makeBookMetadata(totalChapters = 20)
        val copy = original.copy(totalChapters = 25)
        assertEquals(25, copy.totalChapters)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeBookMetadata()
        val b = makeBookMetadata()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentTitle_areNotEqual() {
        val a = makeBookMetadata(title = "Course A")
        val b = makeBookMetadata(title = "Course B")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentTotalChapters_areNotEqual() {
        val a = makeBookMetadata(totalChapters = 20)
        val b = makeBookMetadata(totalChapters = 25)
        assertNotEquals(a, b)
    }

    @Test
    fun creation_emptyChaptersList_isValid() {
        val meta = makeBookMetadata(chapters = emptyList())
        assertTrue(meta.chapters.isEmpty())
    }
}
