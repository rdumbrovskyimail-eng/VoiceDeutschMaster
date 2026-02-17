package com.voicedeutsch.master.data.local.file

import android.content.Context
import com.voicedeutsch.master.domain.model.book.BookMetadata
import com.voicedeutsch.master.domain.model.book.ChapterInfo
import com.voicedeutsch.master.domain.model.book.LessonContent
import com.voicedeutsch.master.domain.model.book.LessonVocabularyEntry
import com.voicedeutsch.master.util.Constants
import kotlinx.serialization.json.Json

class BookFileReader(
    private val context: Context,
    private val json: Json
) {

    fun readMetadata(): BookMetadata {
        val jsonString = readAssetFile(
            "${Constants.BOOK_ASSETS_PATH}/${Constants.BOOK_METADATA_FILE}"
        )
        return json.decodeFromString(jsonString)
    }

    fun readChapterInfo(chapterNumber: Int): ChapterInfo? {
        val path = "${Constants.BOOK_ASSETS_PATH}/chapter_${
            chapterNumber.toString().padStart(2, '0')
        }/${Constants.BOOK_CHAPTER_INFO_FILE}"
        return try {
            val jsonString = readAssetFile(path)
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            null
        }
    }

    fun readLessonContent(chapterNumber: Int, lessonNumber: Int): LessonContent? {
        val chapterDir = "chapter_${chapterNumber.toString().padStart(2, '0')}"
        val path = "${Constants.BOOK_ASSETS_PATH}/$chapterDir/lesson_${
            lessonNumber.toString().padStart(2, '0')
        }.txt"

        return try {
            val text = readAssetFile(path)
            parseLessonText(text)
        } catch (e: Exception) {
            null
        }
    }

    fun readChapterVocabulary(chapterNumber: Int): List<LessonVocabularyEntry> {
        val chapterDir = "chapter_${chapterNumber.toString().padStart(2, '0')}"
        val path = "${Constants.BOOK_ASSETS_PATH}/$chapterDir/${Constants.BOOK_VOCABULARY_FILE}"

        return try {
            val jsonString = readAssetFile(path)
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun readChapterGrammarTopics(chapterNumber: Int): List<String> {
        val chapterDir = "chapter_${chapterNumber.toString().padStart(2, '0')}"
        val path = "${Constants.BOOK_ASSETS_PATH}/$chapterDir/${Constants.BOOK_GRAMMAR_FILE}"

        return try {
            val jsonString = readAssetFile(path)
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseLessonText(text: String): LessonContent {
        val sections = mutableMapOf<String, StringBuilder>()
        var currentSection = "UNKNOWN"

        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                currentSection = trimmed.removePrefix("[").removeSuffix("]")
                sections[currentSection] = StringBuilder()
            } else {
                sections.getOrPut(currentSection) { StringBuilder() }
                    .appendLine(line)
            }
        }

        val vocabularyEntries = sections["VOCABULARY"]?.toString()
            ?.lines()
            ?.filter { it.contains("|") }
            ?.map { line ->
                val parts = line.split("|")
                LessonVocabularyEntry(
                    german = parts.getOrElse(0) { "" }.trim(),
                    russian = parts.getOrElse(1) { "" }.trim(),
                    gender = parts.getOrElse(2) { "" }.trim().ifEmpty { null },
                    plural = parts.getOrElse(3) { "" }.trim().ifEmpty { null },
                    level = parts.getOrElse(4) { "A1" }.trim()
                )
            } ?: emptyList()

        val exerciseMarkers = sections["EXERCISES_MARKERS"]?.toString()
            ?.lines()
            ?.filter { it.startsWith("EXERCISE:") }
            ?: emptyList()

        return LessonContent(
            title = sections["TITLE"]?.toString()?.trim() ?: "",
            introduction = sections["INTRODUCTION"]?.toString()?.trim() ?: "",
            mainContent = sections["CONTENT"]?.toString()?.trim() ?: "",
            phoneticNotes = sections["PHONETIC_NOTES"]?.toString()?.trim() ?: "",
            exerciseMarkers = exerciseMarkers,
            vocabulary = vocabularyEntries
        )
    }

    private fun readAssetFile(path: String): String {
        return context.assets.open(path).bufferedReader().use { it.readText() }
    }
}