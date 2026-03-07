// Path: src/test/java/com/voicedeutsch/master/domain/model/book/ChapterLessonTest.kt
package com.voicedeutsch.master.domain.model.book

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ════════════════════════════════════════════════════════════════════════════
// LessonContent
// ════════════════════════════════════════════════════════════════════════════

class LessonContentTest {

    private fun makeLessonContent(
        title: String = "Lektion 1",
        introduction: String = "",
        mainContent: String = "",
        phoneticNotes: String = "",
        exerciseMarkers: List<String> = emptyList(),
        vocabulary: List<LessonVocabularyEntry> = emptyList(),
    ) = LessonContent(
        title = title,
        introduction = introduction,
        mainContent = mainContent,
        phoneticNotes = phoneticNotes,
        exerciseMarkers = exerciseMarkers,
        vocabulary = vocabulary,
    )

    @Test
    fun creation_withTitle_setsTitle() {
        val content = makeLessonContent(title = "Lektion 1")
        assertEquals("Lektion 1", content.title)
    }

    @Test
    fun creation_defaultTitle_isEmpty() {
        val content = LessonContent()
        assertEquals("", content.title)
    }

    @Test
    fun creation_defaultIntroduction_isEmpty() {
        val content = LessonContent()
        assertEquals("", content.introduction)
    }

    @Test
    fun creation_defaultMainContent_isEmpty() {
        val content = LessonContent()
        assertEquals("", content.mainContent)
    }

    @Test
    fun creation_defaultPhoneticNotes_isEmpty() {
        val content = LessonContent()
        assertEquals("", content.phoneticNotes)
    }

    @Test
    fun creation_defaultExerciseMarkers_isEmpty() {
        val content = LessonContent()
        assertTrue(content.exerciseMarkers.isEmpty())
    }

    @Test
    fun creation_defaultVocabulary_isEmpty() {
        val content = LessonContent()
        assertTrue(content.vocabulary.isEmpty())
    }

    @Test
    fun creation_withVocabulary_setsCorrectly() {
        val entry = LessonVocabularyEntry(german = "Haus", russian = "дом")
        val content = makeLessonContent(vocabulary = listOf(entry))
        assertEquals(1, content.vocabulary.size)
        assertEquals("Haus", content.vocabulary[0].german)
    }

    @Test
    fun creation_withExerciseMarkers_setsCorrectly() {
        val content = makeLessonContent(exerciseMarkers = listOf("EX1", "EX2"))
        assertEquals(2, content.exerciseMarkers.size)
    }

    @Test
    fun copy_changesMainContent_restUnchanged() {
        val original = makeLessonContent(title = "T1", mainContent = "old")
        val copy = original.copy(mainContent = "new content")
        assertEquals("new content", copy.mainContent)
        assertEquals("T1", copy.title)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeLessonContent(title = "T1", mainContent = "Content")
        val b = makeLessonContent(title = "T1", mainContent = "Content")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentTitle_areNotEqual() {
        val a = makeLessonContent(title = "T1")
        val b = makeLessonContent(title = "T2")
        assertNotEquals(a, b)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Lesson
// ════════════════════════════════════════════════════════════════════════════

class LessonTest {

    private fun makeLesson(
        number: Int = 1,
        chapterNumber: Int = 1,
        titleDe: String = "Begrüßungen",
        titleRu: String = "Приветствия",
        focus: LessonFocus = LessonFocus.VOCABULARY,
        newWordsCount: Int = 0,
        estimatedMinutes: Int = 30,
        content: LessonContent? = null,
    ) = Lesson(
        number = number,
        chapterNumber = chapterNumber,
        titleDe = titleDe,
        titleRu = titleRu,
        focus = focus,
        newWordsCount = newWordsCount,
        estimatedMinutes = estimatedMinutes,
        content = content,
    )

    @Test
    fun creation_withRequiredFields_setsValues() {
        val lesson = makeLesson()
        assertEquals(1, lesson.number)
        assertEquals(1, lesson.chapterNumber)
        assertEquals("Begrüßungen", lesson.titleDe)
        assertEquals("Приветствия", lesson.titleRu)
    }

    @Test
    fun creation_defaultFocus_isMixed() {
        val lesson = Lesson(
            number = 1, chapterNumber = 1, titleDe = "T", titleRu = "Т",
        )
        assertEquals(LessonFocus.MIXED, lesson.focus)
    }

    @Test
    fun creation_defaultNewWordsCount_isZero() {
        val lesson = makeLesson()
        assertEquals(0, lesson.newWordsCount)
    }

    @Test
    fun creation_defaultEstimatedMinutes_is30() {
        val lesson = makeLesson()
        assertEquals(30, lesson.estimatedMinutes)
    }

    @Test
    fun creation_defaultContent_isNull() {
        val lesson = makeLesson()
        assertNull(lesson.content)
    }

    @Test
    fun creation_withContent_setsContent() {
        val content = LessonContent(title = "LC")
        val lesson = makeLesson(content = content)
        assertNotNull(lesson.content)
        assertEquals("LC", lesson.content!!.title)
    }

    @Test
    fun copy_changesNewWordsCount_restUnchanged() {
        val original = makeLesson(newWordsCount = 5)
        val copy = original.copy(newWordsCount = 15)
        assertEquals(15, copy.newWordsCount)
        assertEquals(1, copy.number)
    }

    @Test
    fun copy_changesEstimatedMinutes() {
        val original = makeLesson(estimatedMinutes = 30)
        val copy = original.copy(estimatedMinutes = 45)
        assertEquals(45, copy.estimatedMinutes)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeLesson()
        val b = makeLesson()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentChapterNumber_areNotEqual() {
        val a = makeLesson(chapterNumber = 1)
        val b = makeLesson(chapterNumber = 2)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentFocus_areNotEqual() {
        val a = makeLesson(focus = LessonFocus.VOCABULARY)
        val b = makeLesson(focus = LessonFocus.GRAMMAR)
        assertNotEquals(a, b)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Chapter
// ════════════════════════════════════════════════════════════════════════════

class ChapterTest {

    private fun makeLesson(n: Int) = Lesson(
        number = n, chapterNumber = 1, titleDe = "L$n", titleRu = "У$n"
    )

    private fun makeChapter(
        number: Int = 1,
        titleDe: String = "Kapitel 1",
        titleRu: String = "Глава 1",
        level: String = "A1",
        lessons: List<Lesson> = listOf(makeLesson(1), makeLesson(2)),
        grammarTopics: List<String> = emptyList(),
        culturalNotes: List<String> = emptyList(),
    ) = Chapter(
        number = number,
        titleDe = titleDe,
        titleRu = titleRu,
        level = level,
        lessons = lessons,
        grammarTopics = grammarTopics,
        culturalNotes = culturalNotes,
    )

    @Test
    fun creation_withRequiredFields_setsValues() {
        val chapter = makeChapter()
        assertEquals(1, chapter.number)
        assertEquals("Kapitel 1", chapter.titleDe)
        assertEquals("Глава 1", chapter.titleRu)
        assertEquals("A1", chapter.level)
    }

    @Test
    fun creation_lessonsList_isStored() {
        val chapter = makeChapter()
        assertEquals(2, chapter.lessons.size)
        assertEquals(1, chapter.lessons[0].number)
    }

    @Test
    fun creation_defaultGrammarTopics_isEmpty() {
        val chapter = makeChapter()
        assertTrue(chapter.grammarTopics.isEmpty())
    }

    @Test
    fun creation_defaultCulturalNotes_isEmpty() {
        val chapter = makeChapter()
        assertTrue(chapter.culturalNotes.isEmpty())
    }

    @Test
    fun creation_withGrammarTopics_setsCorrectly() {
        val chapter = makeChapter(grammarTopics = listOf("Artikel", "Nominativ"))
        assertEquals(2, chapter.grammarTopics.size)
        assertEquals("Artikel", chapter.grammarTopics[0])
    }

    @Test
    fun copy_changesLevel_restUnchanged() {
        val original = makeChapter(level = "A1")
        val copy = original.copy(level = "A2")
        assertEquals("A2", copy.level)
        assertEquals(1, copy.number)
        assertEquals("Kapitel 1", copy.titleDe)
    }

    @Test
    fun copy_changesLessons() {
        val original = makeChapter()
        val newLessons = listOf(makeLesson(5))
        val copy = original.copy(lessons = newLessons)
        assertEquals(1, copy.lessons.size)
        assertEquals(5, copy.lessons[0].number)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makeChapter()
        val b = makeChapter()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentNumber_areNotEqual() {
        val a = makeChapter(number = 1)
        val b = makeChapter(number = 2)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentLevel_areNotEqual() {
        val a = makeChapter(level = "A1")
        val b = makeChapter(level = "B1")
        assertNotEquals(a, b)
    }

    @Test
    fun creation_emptyLessons_isValid() {
        val chapter = makeChapter(lessons = emptyList())
        assertTrue(chapter.lessons.isEmpty())
    }
}
