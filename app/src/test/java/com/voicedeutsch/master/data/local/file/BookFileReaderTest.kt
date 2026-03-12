// Путь: src/test/java/com/voicedeutsch/master/data/local/file/BookFileReaderTest.kt
package com.voicedeutsch.master.data.local.file

import android.content.Context
import android.content.res.AssetManager
import com.voicedeutsch.master.domain.model.book.BookMetadata
import com.voicedeutsch.master.domain.model.book.ChapterInfo
import com.voicedeutsch.master.domain.model.book.LessonContent
import com.voicedeutsch.master.domain.model.book.LessonVocabularyEntry
import com.voicedeutsch.master.util.Constants
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException

class BookFileReaderTest {

    private lateinit var context: Context
    private lateinit var assets: AssetManager
    private lateinit var json: Json
    private lateinit var sut: BookFileReader

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        assets = mockk(relaxed = true)
        json = Json { ignoreUnknownKeys = true }

        every { context.assets } returns assets

        sut = BookFileReader(context, json)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun stubAsset(path: String, content: String) {
        every { assets.open(path) } returns ByteArrayInputStream(content.toByteArray())
    }

    private fun stubAssetThrows(path: String) {
        every { assets.open(path) } throws FileNotFoundException("Not found: $path")
    }

    private fun chapterDir(n: Int) = "chapter_${n.toString().padStart(2, '0')}"
    private fun metadataPath() = "${Constants.BOOK_ASSETS_PATH}/${Constants.BOOK_METADATA_FILE}"
    private fun chapterInfoPath(n: Int) =
        "${Constants.BOOK_ASSETS_PATH}/${chapterDir(n)}/${Constants.BOOK_CHAPTER_INFO_FILE}"
    private fun lessonPath(ch: Int, ls: Int) =
        "${Constants.BOOK_ASSETS_PATH}/${chapterDir(ch)}/lesson_${ls.toString().padStart(2, '0')}.txt"
    private fun vocabularyPath(ch: Int) =
        "${Constants.BOOK_ASSETS_PATH}/${chapterDir(ch)}/${Constants.BOOK_VOCABULARY_FILE}"
    private fun grammarPath(ch: Int) =
        "${Constants.BOOK_ASSETS_PATH}/${chapterDir(ch)}/${Constants.BOOK_GRAMMAR_FILE}"

    private val sampleMetadataJson = """
        {
          "title": "Deutsch Meister",
          "totalChapters": 10,
          "language": "de"
        }
    """.trimIndent()

    private val sampleChapterInfoJson = """
        {
          "chapterNumber": 1,
          "title": "Erste Schritte",
          "totalLessons": 5
        }
    """.trimIndent()

    private val sampleVocabularyJson = """
        [
          {"german": "der Hund", "russian": "собака", "gender": "MASCULINE", "plural": "die Hunde", "level": "A1"},
          {"german": "die Katze", "russian": "кошка", "gender": "FEMININE", "plural": "die Katzen", "level": "A1"}
        ]
    """.trimIndent()

    private val sampleGrammarJson = """
        ["Artikel", "Kasus", "Verb sein"]
    """.trimIndent()

    private fun buildLessonText(
        title: String = "Lektion 1",
        introduction: String = "Intro text",
        content: String = "Main content here",
        phoneticNotes: String = "Some notes",
        vocabulary: String = "der Hund | собака | MASCULINE | die Hunde | A1\ndie Katze | кошка | FEMININE | die Katzen | A1",
        exerciseMarkers: String = "EXERCISE:fill_in_blank\nEXERCISE:multiple_choice",
    ) = """
[TITLE]
$title
[INTRODUCTION]
$introduction
[CONTENT]
$content
[PHONETIC_NOTES]
$phoneticNotes
[VOCABULARY]
$vocabulary
[EXERCISES_MARKERS]
$exerciseMarkers
    """.trimIndent()

    // ── readMetadata ──────────────────────────────────────────────────────────

    @Test
    fun readMetadata_validJson_returnsBookMetadata() {
        stubAsset(metadataPath(), sampleMetadataJson)

        val result = sut.readMetadata()

        assertNotNull(result)
        assertEquals("Deutsch Meister", result.title)
        assertEquals(10, result.totalChapters)
    }

    @Test
    fun readMetadata_readsFromCorrectAssetPath() {
        stubAsset(metadataPath(), sampleMetadataJson)
        sut.readMetadata()
        io.mockk.verify { assets.open(metadataPath()) }
    }

    @Test
    fun readMetadata_assetThrows_propagatesException() {
        stubAssetThrows(metadataPath())
        assertThrows(Exception::class.java) { sut.readMetadata() }
    }

    // ── readChapterInfo ───────────────────────────────────────────────────────

    @Test
    fun readChapterInfo_validJson_returnsChapterInfo() {
        stubAsset(chapterInfoPath(1), sampleChapterInfoJson)

        val result = sut.readChapterInfo(1)

        assertNotNull(result)
        assertEquals(1, result!!.number)
        assertEquals("Erste Schritte", result.titleDe)
        assertEquals(5, result.lessonsCount)
    }

    @Test
    fun readChapterInfo_chapterNumberPaddedToTwoDigits_path() {
        stubAsset(chapterInfoPath(3), sampleChapterInfoJson)
        sut.readChapterInfo(3)
        io.mockk.verify { assets.open(chapterInfoPath(3)) }
    }

    @Test
    fun readChapterInfo_chapterNumberGte10_paddedCorrectly() {
        val path = "${Constants.BOOK_ASSETS_PATH}/chapter_10/${Constants.BOOK_CHAPTER_INFO_FILE}"
        stubAsset(path, sampleChapterInfoJson)
        val result = sut.readChapterInfo(10)
        assertNotNull(result)
    }

    @Test
    fun readChapterInfo_assetThrows_returnsNull() {
        stubAssetThrows(chapterInfoPath(99))
        val result = sut.readChapterInfo(99)
        assertNull(result)
    }

    @Test
    fun readChapterInfo_invalidJson_returnsNull() {
        stubAsset(chapterInfoPath(1), "{invalid_json}")
        val result = sut.readChapterInfo(1)
        assertNull(result)
    }

    // ── readLessonContent ─────────────────────────────────────────────────────

    @Test
    fun readLessonContent_validText_returnsParsedContent() {
        stubAsset(lessonPath(1, 1), buildLessonText())

        val result = sut.readLessonContent(1, 1)

        assertNotNull(result)
        assertEquals("Lektion 1", result!!.title)
        assertEquals("Intro text", result.introduction)
        assertEquals("Main content here", result.mainContent)
        assertEquals("Some notes", result.phoneticNotes)
    }

    @Test
    fun readLessonContent_lessonNumberPaddedToTwoDigits() {
        stubAsset(lessonPath(1, 5), buildLessonText())
        sut.readLessonContent(1, 5)
        io.mockk.verify { assets.open(lessonPath(1, 5)) }
    }

    @Test
    fun readLessonContent_assetThrows_returnsNull() {
        stubAssetThrows(lessonPath(1, 1))
        val result = sut.readLessonContent(1, 1)
        assertNull(result)
    }

    @Test
    fun readLessonContent_emptyText_returnsContentWithEmptyFields() {
        stubAsset(lessonPath(1, 1), "")

        val result = sut.readLessonContent(1, 1)

        assertNotNull(result)
        assertEquals("", result!!.title)
        assertEquals("", result.mainContent)
    }

    @Test
    fun readLessonContent_vocabularySection_parsedToEntries() {
        val lessonText = buildLessonText(
            vocabulary = "der Hund | собака | MASCULINE | die Hunde | A1\ndie Katze | кошка | FEMININE | die Katzen | A2"
        )
        stubAsset(lessonPath(1, 1), lessonText)

        val result = sut.readLessonContent(1, 1)!!

        assertEquals(2, result.vocabulary.size)
        assertEquals("der Hund", result.vocabulary[0].german)
        assertEquals("собака", result.vocabulary[0].russian)
        assertEquals("MASCULINE", result.vocabulary[0].gender)
        assertEquals("die Hunde", result.vocabulary[0].plural)
        assertEquals("A1", result.vocabulary[0].level)
    }

    @Test
    fun readLessonContent_vocabularyEntryMissingGender_genderIsNull() {
        val lessonText = buildLessonText(vocabulary = "das Buch | книга |  |  | A1")
        stubAsset(lessonPath(1, 1), lessonText)

        val result = sut.readLessonContent(1, 1)!!

        assertEquals(1, result.vocabulary.size)
        assertNull(result.vocabulary[0].gender)
    }

    @Test
    fun readLessonContent_vocabularyEntryMissingPlural_pluralIsNull() {
        val lessonText = buildLessonText(vocabulary = "das Buch | книга |  |  | A1")
        stubAsset(lessonPath(1, 1), lessonText)

        val result = sut.readLessonContent(1, 1)!!

        assertNull(result.vocabulary[0].plural)
    }

    @Test
    fun readLessonContent_vocabularyLineWithoutPipe_notIncluded() {
        val lessonText = buildLessonText(vocabulary = "no pipe here\nder Hund | собака | MASCULINE | die Hunde | A1")
        stubAsset(lessonPath(1, 1), lessonText)

        val result = sut.readLessonContent(1, 1)!!

        assertEquals(1, result.vocabulary.size)
        assertEquals("der Hund", result.vocabulary[0].german)
    }

    @Test
    fun readLessonContent_emptyVocabularySection_returnsEmptyList() {
        val lessonText = buildLessonText(vocabulary = "")
        stubAsset(lessonPath(1, 1), lessonText)

        val result = sut.readLessonContent(1, 1)!!

        assertEquals(emptyList<LessonVocabularyEntry>(), result.vocabulary)
    }

    @Test
    fun readLessonContent_exerciseMarkers_parsedCorrectly() {
        val lessonText = buildLessonText(
            exerciseMarkers = "EXERCISE:fill_in_blank\nEXERCISE:multiple_choice\nnot_an_exercise"
        )
        stubAsset(lessonPath(1, 1), lessonText)

        val result = sut.readLessonContent(1, 1)!!

        assertEquals(2, result.exerciseMarkers.size)
        assertTrue(result.exerciseMarkers[0].startsWith("EXERCISE:"))
        assertTrue(result.exerciseMarkers[1].startsWith("EXERCISE:"))
    }

    @Test
    fun readLessonContent_noExerciseMarkers_emptyList() {
        val lessonText = buildLessonText(exerciseMarkers = "no markers here")
        stubAsset(lessonPath(1, 1), lessonText)

        val result = sut.readLessonContent(1, 1)!!

        assertEquals(emptyList<String>(), result.exerciseMarkers)
    }

    @Test
    fun readLessonContent_unknownSection_doesNotCrash() {
        val lessonText = "[UNKNOWN_SECTION]\nsome content\n[TITLE]\nLektion X"
        stubAsset(lessonPath(1, 1), lessonText)

        val result = sut.readLessonContent(1, 1)

        assertNotNull(result)
        assertEquals("Lektion X", result!!.title)
    }

    @Test
    fun readLessonContent_vocabularyLevelMissing_defaultsToA1() {
        val lessonText = buildLessonText(vocabulary = "der Hund | собака | MASCULINE | die Hunde")
        stubAsset(lessonPath(1, 1), lessonText)

        val result = sut.readLessonContent(1, 1)!!

        assertEquals("A1", result.vocabulary[0].level)
    }

    // ── readChapterVocabulary ─────────────────────────────────────────────────

    @Test
    fun readChapterVocabulary_validJson_returnsParsedList() {
        stubAsset(vocabularyPath(1), sampleVocabularyJson)

        val result = sut.readChapterVocabulary(1)

        assertEquals(2, result.size)
        assertEquals("der Hund", result[0].german)
        assertEquals("собака", result[0].russian)
        assertEquals("MASCULINE", result[0].gender)
        assertEquals("die Hunde", result[0].plural)
        assertEquals("A1", result[0].level)
    }

    @Test
    fun readChapterVocabulary_assetThrows_returnsEmptyList() {
        stubAssetThrows(vocabularyPath(1))
        val result = sut.readChapterVocabulary(1)
        assertEquals(emptyList<LessonVocabularyEntry>(), result)
    }

    @Test
    fun readChapterVocabulary_invalidJson_returnsEmptyList() {
        stubAsset(vocabularyPath(1), "{not_valid_json}")
        val result = sut.readChapterVocabulary(1)
        assertEquals(emptyList<LessonVocabularyEntry>(), result)
    }

    @Test
    fun readChapterVocabulary_emptyJsonArray_returnsEmptyList() {
        stubAsset(vocabularyPath(1), "[]")
        val result = sut.readChapterVocabulary(1)
        assertEquals(emptyList<LessonVocabularyEntry>(), result)
    }

    @Test
    fun readChapterVocabulary_chapterNumberPadded() {
        stubAsset(vocabularyPath(2), sampleVocabularyJson)
        sut.readChapterVocabulary(2)
        io.mockk.verify { assets.open(vocabularyPath(2)) }
    }

    // ── readChapterGrammarTopics ──────────────────────────────────────────────

    @Test
    fun readChapterGrammarTopics_validJson_returnsParsedList() {
        stubAsset(grammarPath(1), sampleGrammarJson)

        val result = sut.readChapterGrammarTopics(1)

        assertEquals(3, result.size)
        assertEquals("Artikel", result[0])
        assertEquals("Kasus", result[1])
        assertEquals("Verb sein", result[2])
    }

    @Test
    fun readChapterGrammarTopics_assetThrows_returnsEmptyList() {
        stubAssetThrows(grammarPath(1))
        val result = sut.readChapterGrammarTopics(1)
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun readChapterGrammarTopics_invalidJson_returnsEmptyList() {
        stubAsset(grammarPath(1), "not_json_at_all")
        val result = sut.readChapterGrammarTopics(1)
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun readChapterGrammarTopics_emptyJsonArray_returnsEmptyList() {
        stubAsset(grammarPath(1), "[]")
        val result = sut.readChapterGrammarTopics(1)
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun readChapterGrammarTopics_chapterNumberPadded() {
        stubAsset(grammarPath(7), sampleGrammarJson)
        sut.readChapterGrammarTopics(7)
        io.mockk.verify { assets.open(grammarPath(7)) }
    }

    // ── Path construction ─────────────────────────────────────────────────────

    @Test
    fun chapterPath_singleDigitChapter_paddedWithLeadingZero() {
        stubAsset(chapterInfoPath(1), sampleChapterInfoJson)
        sut.readChapterInfo(1)
        io.mockk.verify { assets.open(match { it.contains("chapter_01") }) }
    }

    @Test
    fun chapterPath_doubleDigitChapter_notExtralyPadded() {
        stubAsset(chapterInfoPath(12), sampleChapterInfoJson)
        sut.readChapterInfo(12)
        io.mockk.verify { assets.open(match { it.contains("chapter_12") }) }
    }

    @Test
    fun lessonPath_singleDigitLesson_paddedWithLeadingZero() {
        stubAsset(lessonPath(1, 1), buildLessonText())
        sut.readLessonContent(1, 1)
        io.mockk.verify { assets.open(match { it.contains("lesson_01") }) }
    }

    @Test
    fun lessonPath_doubleDigitLesson_notExtralyPadded() {
        stubAsset(lessonPath(1, 10), buildLessonText())
        sut.readLessonContent(1, 10)
        io.mockk.verify { assets.open(match { it.contains("lesson_10") }) }
    }
}
