// Путь: src/test/java/com/voicedeutsch/master/domain/model/book/BookModelsTest.kt
package com.voicedeutsch.master.domain.model.book

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ═══════════════════════════════════════════════════════════════════════════
// BookMetadata
// ═══════════════════════════════════════════════════════════════════════════

class BookMetadataTest {

    private fun createChapterInfo(number: Int = 1) = ChapterInfo(
        number = number,
        titleDe = "Kapitel $number",
        titleRu = "Глава $number",
        level = "A1.1",
        lessonsCount = 5
    )

    private fun createBookMetadata(
        title: String = "Deutsch lernen",
        titleRu: String = "Учим немецкий",
        author: String = "",
        edition: String = "",
        targetLevel: String = "",
        totalChapters: Int = 20,
        totalLessons: Int = 120,
        description: String = "",
        chapters: List<ChapterInfo> = listOf(createChapterInfo())
    ) = BookMetadata(
        title = title,
        titleRu = titleRu,
        author = author,
        edition = edition,
        targetLevel = targetLevel,
        totalChapters = totalChapters,
        totalLessons = totalLessons,
        description = description,
        chapters = chapters
    )

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val meta = BookMetadata(
            title = "T",
            titleRu = "Т",
            totalChapters = 20,
            totalLessons = 120,
            chapters = emptyList()
        )
        assertEquals("", meta.author)
        assertEquals("", meta.edition)
        assertEquals("", meta.targetLevel)
        assertEquals("", meta.description)
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val chapters = listOf(createChapterInfo(1), createChapterInfo(2))
        val meta = createBookMetadata(
            title = "Deutsche Grammatik",
            titleRu = "Немецкая грамматика",
            author = "Max Müller",
            edition = "3rd",
            targetLevel = "A1-B1",
            totalChapters = 2,
            totalLessons = 10,
            description = "Comprehensive guide",
            chapters = chapters
        )
        assertEquals("Deutsche Grammatik", meta.title)
        assertEquals("Немецкая грамматика", meta.titleRu)
        assertEquals("Max Müller", meta.author)
        assertEquals("3rd", meta.edition)
        assertEquals("A1-B1", meta.targetLevel)
        assertEquals(2, meta.totalChapters)
        assertEquals(10, meta.totalLessons)
        assertEquals("Comprehensive guide", meta.description)
        assertEquals(2, meta.chapters.size)
    }

    @Test
    fun constructor_emptyChaptersList_storedCorrectly() {
        val meta = createBookMetadata(chapters = emptyList())
        assertTrue(meta.chapters.isEmpty())
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_updateTotalLessons_onlyLessonsChange() {
        val original = createBookMetadata(totalLessons = 120)
        val modified = original.copy(totalLessons = 130)
        assertEquals(130, modified.totalLessons)
        assertEquals(original.title, modified.title)
        assertEquals(original.totalChapters, modified.totalChapters)
    }

    @Test
    fun copy_addChapter_chaptersUpdated() {
        val original = createBookMetadata(chapters = listOf(createChapterInfo(1)))
        val modified = original.copy(chapters = listOf(createChapterInfo(1), createChapterInfo(2)))
        assertEquals(2, modified.chapters.size)
        assertEquals(1, original.chapters.size)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createBookMetadata(), createBookMetadata())
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(createBookMetadata().hashCode(), createBookMetadata().hashCode())
    }

    @Test
    fun equals_differentTitle_notEqual() {
        assertNotEquals(
            createBookMetadata(title = "Book A"),
            createBookMetadata(title = "Book B")
        )
    }

    @Test
    fun equals_differentTotalChapters_notEqual() {
        assertNotEquals(
            createBookMetadata(totalChapters = 10),
            createBookMetadata(totalChapters = 20)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ChapterInfo
// ═══════════════════════════════════════════════════════════════════════════

class ChapterInfoTest {

    private fun createChapterInfo(
        number: Int = 1,
        titleDe: String = "Erste Schritte",
        titleRu: String = "Первые шаги",
        level: String = "A1.1",
        topics: List<String> = emptyList(),
        lessonsCount: Int = 6,
        estimatedHours: Float = 0f
    ) = ChapterInfo(
        number = number,
        titleDe = titleDe,
        titleRu = titleRu,
        level = level,
        topics = topics,
        lessonsCount = lessonsCount,
        estimatedHours = estimatedHours
    )

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val info = ChapterInfo(
            number = 1,
            titleDe = "T",
            titleRu = "Т",
            level = "A1.1",
            lessonsCount = 5
        )
        assertTrue(info.topics.isEmpty())
        assertEquals(0f, info.estimatedHours, 0.001f)
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val topics = listOf("Greetings", "Numbers")
        val info = createChapterInfo(
            number = 3,
            titleDe = "Familie",
            titleRu = "Семья",
            level = "A1.3",
            topics = topics,
            lessonsCount = 8,
            estimatedHours = 3.5f
        )
        assertEquals(3, info.number)
        assertEquals("Familie", info.titleDe)
        assertEquals("Семья", info.titleRu)
        assertEquals("A1.3", info.level)
        assertEquals(topics, info.topics)
        assertEquals(8, info.lessonsCount)
        assertEquals(3.5f, info.estimatedHours, 0.001f)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_changeLevel_levelUpdated() {
        val original = createChapterInfo(level = "A1.1")
        val modified = original.copy(level = "A1.2")
        assertEquals("A1.2", modified.level)
        assertEquals(original.number, modified.number)
    }

    @Test
    fun copy_updateTopics_topicsUpdated() {
        val original = createChapterInfo(topics = emptyList())
        val modified = original.copy(topics = listOf("Verbs", "Nouns"))
        assertEquals(listOf("Verbs", "Nouns"), modified.topics)
        assertTrue(original.topics.isEmpty())
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createChapterInfo(), createChapterInfo())
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(createChapterInfo().hashCode(), createChapterInfo().hashCode())
    }

    @Test
    fun equals_differentNumber_notEqual() {
        assertNotEquals(createChapterInfo(number = 1), createChapterInfo(number = 2))
    }

    @Test
    fun equals_differentEstimatedHours_notEqual() {
        assertNotEquals(
            createChapterInfo(estimatedHours = 1f),
            createChapterInfo(estimatedHours = 2f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Chapter
// ═══════════════════════════════════════════════════════════════════════════

class ChapterTest {

    private fun createLesson(number: Int = 1) = Lesson(
        number = number,
        chapterNumber = 1,
        titleDe = "Lektion $number",
        titleRu = "Урок $number"
    )

    private fun createChapter(
        number: Int = 1,
        titleDe: String = "Kapitel 1",
        titleRu: String = "Глава 1",
        level: String = "A1.1",
        lessons: List<Lesson> = listOf(createLesson()),
        grammarTopics: List<String> = emptyList(),
        culturalNotes: List<String> = emptyList()
    ) = Chapter(
        number = number,
        titleDe = titleDe,
        titleRu = titleRu,
        level = level,
        lessons = lessons,
        grammarTopics = grammarTopics,
        culturalNotes = culturalNotes
    )

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val chapter = Chapter(
            number = 1,
            titleDe = "T",
            titleRu = "Т",
            level = "A1",
            lessons = emptyList()
        )
        assertTrue(chapter.grammarTopics.isEmpty())
        assertTrue(chapter.culturalNotes.isEmpty())
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val lessons = listOf(createLesson(1), createLesson(2))
        val grammar = listOf("Nominativ", "Akkusativ")
        val cultural = listOf("German punctuality")
        val chapter = createChapter(
            number = 5,
            titleDe = "Arbeit",
            titleRu = "Работа",
            level = "B1.2",
            lessons = lessons,
            grammarTopics = grammar,
            culturalNotes = cultural
        )
        assertEquals(5, chapter.number)
        assertEquals("Arbeit", chapter.titleDe)
        assertEquals("Работа", chapter.titleRu)
        assertEquals("B1.2", chapter.level)
        assertEquals(2, chapter.lessons.size)
        assertEquals(grammar, chapter.grammarTopics)
        assertEquals(cultural, chapter.culturalNotes)
    }

    @Test
    fun constructor_emptyLessons_storedCorrectly() {
        val chapter = createChapter(lessons = emptyList())
        assertTrue(chapter.lessons.isEmpty())
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_addGrammarTopics_topicsUpdated() {
        val original = createChapter(grammarTopics = emptyList())
        val modified = original.copy(grammarTopics = listOf("Dativ"))
        assertEquals(listOf("Dativ"), modified.grammarTopics)
        assertTrue(original.grammarTopics.isEmpty())
    }

    @Test
    fun copy_changeTitleDe_onlyTitleDeChanges() {
        val original = createChapter(titleDe = "Alt")
        val modified = original.copy(titleDe = "Neu")
        assertEquals("Neu", modified.titleDe)
        assertEquals(original.number, modified.number)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createChapter(), createChapter())
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(createChapter().hashCode(), createChapter().hashCode())
    }

    @Test
    fun equals_differentNumber_notEqual() {
        assertNotEquals(createChapter(number = 1), createChapter(number = 2))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Lesson
// ═══════════════════════════════════════════════════════════════════════════

class LessonTest {

    private fun createLesson(
        number: Int = 1,
        chapterNumber: Int = 1,
        titleDe: String = "Hallo!",
        titleRu: String = "Привет!",
        focus: LessonFocus = LessonFocus.MIXED,
        newWordsCount: Int = 0,
        estimatedMinutes: Int = 30,
        content: LessonContent? = null
    ) = Lesson(
        number = number,
        chapterNumber = chapterNumber,
        titleDe = titleDe,
        titleRu = titleRu,
        focus = focus,
        newWordsCount = newWordsCount,
        estimatedMinutes = estimatedMinutes,
        content = content
    )

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val lesson = Lesson(number = 1, chapterNumber = 1, titleDe = "T", titleRu = "Т")
        assertEquals(LessonFocus.MIXED, lesson.focus)
        assertEquals(0, lesson.newWordsCount)
        assertEquals(30, lesson.estimatedMinutes)
        assertNull(lesson.content)
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val content = LessonContent(title = "Lektion 1")
        val lesson = createLesson(
            number = 3,
            chapterNumber = 2,
            titleDe = "Familie",
            titleRu = "Семья",
            focus = LessonFocus.VOCABULARY,
            newWordsCount = 15,
            estimatedMinutes = 45,
            content = content
        )
        assertEquals(3, lesson.number)
        assertEquals(2, lesson.chapterNumber)
        assertEquals("Familie", lesson.titleDe)
        assertEquals("Семья", lesson.titleRu)
        assertEquals(LessonFocus.VOCABULARY, lesson.focus)
        assertEquals(15, lesson.newWordsCount)
        assertEquals(45, lesson.estimatedMinutes)
        assertEquals(content, lesson.content)
    }

    @Test
    fun constructor_nullContent_isNull() {
        val lesson = createLesson(content = null)
        assertNull(lesson.content)
    }

    @Test
    fun constructor_withContent_contentNotNull() {
        val lesson = createLesson(content = LessonContent(title = "Test"))
        assertNotNull(lesson.content)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_changeFocus_focusUpdated() {
        val original = createLesson(focus = LessonFocus.MIXED)
        val modified = original.copy(focus = LessonFocus.GRAMMAR)
        assertEquals(LessonFocus.GRAMMAR, modified.focus)
        assertEquals(LessonFocus.MIXED, original.focus)
    }

    @Test
    fun copy_incrementNewWordsCount_countUpdated() {
        val original = createLesson(newWordsCount = 10)
        val modified = original.copy(newWordsCount = 20)
        assertEquals(20, modified.newWordsCount)
        assertEquals(10, original.newWordsCount)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createLesson(), createLesson())
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(createLesson().hashCode(), createLesson().hashCode())
    }

    @Test
    fun equals_differentChapterNumber_notEqual() {
        assertNotEquals(createLesson(chapterNumber = 1), createLesson(chapterNumber = 2))
    }

    @Test
    fun equals_differentEstimatedMinutes_notEqual() {
        assertNotEquals(createLesson(estimatedMinutes = 30), createLesson(estimatedMinutes = 60))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LessonContent
// ═══════════════════════════════════════════════════════════════════════════

class LessonContentTest {

    private fun createLessonContent(
        title: String = "Lektion 1",
        introduction: String = "",
        mainContent: String = "",
        phoneticNotes: String = "",
        exerciseMarkers: List<String> = emptyList(),
        vocabulary: List<LessonVocabularyEntry> = emptyList()
    ) = LessonContent(
        title = title,
        introduction = introduction,
        mainContent = mainContent,
        phoneticNotes = phoneticNotes,
        exerciseMarkers = exerciseMarkers,
        vocabulary = vocabulary
    )

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val content = LessonContent()
        assertEquals("", content.title)
        assertEquals("", content.introduction)
        assertEquals("", content.mainContent)
        assertEquals("", content.phoneticNotes)
        assertTrue(content.exerciseMarkers.isEmpty())
        assertTrue(content.vocabulary.isEmpty())
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val vocab = listOf(LessonVocabularyEntry(german = "Haus", russian = "дом"))
        val markers = listOf("EX_1", "EX_2")
        val content = createLessonContent(
            title = "Meine Familie",
            introduction = "In dieser Lektion...",
            mainContent = "Hauptinhalt...",
            phoneticNotes = "Das 'ü' klingt...",
            exerciseMarkers = markers,
            vocabulary = vocab
        )
        assertEquals("Meine Familie", content.title)
        assertEquals("In dieser Lektion...", content.introduction)
        assertEquals("Hauptinhalt...", content.mainContent)
        assertEquals("Das 'ü' klingt...", content.phoneticNotes)
        assertEquals(markers, content.exerciseMarkers)
        assertEquals(vocab, content.vocabulary)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_updateMainContent_onlyMainContentChanges() {
        val original = createLessonContent(mainContent = "old")
        val modified = original.copy(mainContent = "new")
        assertEquals("new", modified.mainContent)
        assertEquals(original.title, modified.title)
    }

    @Test
    fun copy_addVocabulary_vocabularyUpdated() {
        val original = createLessonContent(vocabulary = emptyList())
        val entry = LessonVocabularyEntry(german = "Auto", russian = "машина")
        val modified = original.copy(vocabulary = listOf(entry))
        assertEquals(1, modified.vocabulary.size)
        assertTrue(original.vocabulary.isEmpty())
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createLessonContent(), createLessonContent())
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(createLessonContent().hashCode(), createLessonContent().hashCode())
    }

    @Test
    fun equals_differentTitle_notEqual() {
        assertNotEquals(createLessonContent(title = "A"), createLessonContent(title = "B"))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LessonVocabularyEntry
// ═══════════════════════════════════════════════════════════════════════════

class LessonVocabularyEntryTest {

    private fun createEntry(
        german: String = "Haus",
        russian: String = "дом",
        gender: String? = null,
        plural: String? = null,
        level: String = "A1"
    ) = LessonVocabularyEntry(
        german = german,
        russian = russian,
        gender = gender,
        plural = plural,
        level = level
    )

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val entry = LessonVocabularyEntry(german = "Haus", russian = "дом")
        assertNull(entry.gender)
        assertNull(entry.plural)
        assertEquals("A1", entry.level)
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val entry = createEntry(
            german = "Hund",
            russian = "собака",
            gender = "m",
            plural = "Hunde",
            level = "A1"
        )
        assertEquals("Hund", entry.german)
        assertEquals("собака", entry.russian)
        assertEquals("m", entry.gender)
        assertEquals("Hunde", entry.plural)
        assertEquals("A1", entry.level)
    }

    @Test
    fun constructor_nullGenderAndPlural_storedCorrectly() {
        val entry = createEntry(gender = null, plural = null)
        assertNull(entry.gender)
        assertNull(entry.plural)
    }

    @Test
    fun constructor_withGender_genderStored() {
        val entry = createEntry(gender = "f")
        assertEquals("f", entry.gender)
    }

    @Test
    fun constructor_higherLevel_levelStored() {
        val entry = createEntry(level = "B2")
        assertEquals("B2", entry.level)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_setGender_genderUpdated() {
        val original = createEntry(gender = null)
        val modified = original.copy(gender = "n")
        assertEquals("n", modified.gender)
        assertNull(original.gender)
    }

    @Test
    fun copy_setPlural_pluralUpdated() {
        val original = createEntry(plural = null)
        val modified = original.copy(plural = "Häuser")
        assertEquals("Häuser", modified.plural)
        assertNull(original.plural)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createEntry(), createEntry())
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(createEntry().hashCode(), createEntry().hashCode())
    }

    @Test
    fun equals_differentGerman_notEqual() {
        assertNotEquals(createEntry(german = "Haus"), createEntry(german = "Maus"))
    }

    @Test
    fun equals_differentGender_notEqual() {
        assertNotEquals(createEntry(gender = "m"), createEntry(gender = "f"))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LessonFocus
// ═══════════════════════════════════════════════════════════════════════════

class LessonFocusTest {

    @Test
    fun entries_size_isSeven() {
        assertEquals(7, LessonFocus.entries.size)
    }

    @Test
    fun entries_containsAllExpectedValues() {
        val expected = setOf(
            LessonFocus.VOCABULARY,
            LessonFocus.GRAMMAR,
            LessonFocus.PRONUNCIATION,
            LessonFocus.LISTENING,
            LessonFocus.READING,
            LessonFocus.SPEAKING,
            LessonFocus.MIXED
        )
        assertEquals(expected, LessonFocus.entries.toSet())
    }

    @Test
    fun ordinal_vocabulary_isZero() {
        assertEquals(0, LessonFocus.VOCABULARY.ordinal)
    }

    @Test
    fun ordinal_grammar_isOne() {
        assertEquals(1, LessonFocus.GRAMMAR.ordinal)
    }

    @Test
    fun ordinal_pronunciation_isTwo() {
        assertEquals(2, LessonFocus.PRONUNCIATION.ordinal)
    }

    @Test
    fun ordinal_listening_isThree() {
        assertEquals(3, LessonFocus.LISTENING.ordinal)
    }

    @Test
    fun ordinal_reading_isFour() {
        assertEquals(4, LessonFocus.READING.ordinal)
    }

    @Test
    fun ordinal_speaking_isFive() {
        assertEquals(5, LessonFocus.SPEAKING.ordinal)
    }

    @Test
    fun ordinal_mixed_isSix() {
        assertEquals(6, LessonFocus.MIXED.ordinal)
    }

    @Test
    fun valueOf_vocabulary_returnsVocabulary() {
        assertEquals(LessonFocus.VOCABULARY, LessonFocus.valueOf("VOCABULARY"))
    }

    @Test
    fun valueOf_mixed_returnsMixed() {
        assertEquals(LessonFocus.MIXED, LessonFocus.valueOf("MIXED"))
    }

    @Test
    fun valueOf_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            LessonFocus.valueOf("UNKNOWN")
        }
    }

    @Test
    fun defaultLesson_usesMixedFocus() {
        val lesson = Lesson(number = 1, chapterNumber = 1, titleDe = "T", titleRu = "Т")
        assertEquals(LessonFocus.MIXED, lesson.focus)
    }
}
