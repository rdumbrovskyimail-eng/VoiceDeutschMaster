package com.voicedeutsch.master.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [
        Index("german"),
        Index("difficulty_level"),
        Index("topic")
    ]
)
data class WordEntity(
    @PrimaryKey
    val id: String,
    val german: String,
    val russian: String,
    @ColumnInfo(name = "part_of_speech")
    val partOfSpeech: String,
    val gender: String? = null,
    val plural: String? = null,
    @ColumnInfo(name = "conjugation_json")
    val conjugationJson: String? = null,
    @ColumnInfo(name = "declension_json")
    val declensionJson: String? = null,
    @ColumnInfo(name = "example_sentence_de")
    val exampleSentenceDe: String = "",
    @ColumnInfo(name = "example_sentence_ru")
    val exampleSentenceRu: String = "",
    @ColumnInfo(name = "phonetic_transcription")
    val phoneticTranscription: String? = null,
    @ColumnInfo(name = "difficulty_level")
    val difficultyLevel: String = "A1",
    val topic: String = "",
    @ColumnInfo(name = "book_chapter")
    val bookChapter: Int? = null,
    @ColumnInfo(name = "book_lesson")
    val bookLesson: Int? = null,
    @ColumnInfo(name = "audio_cache_path")
    val audioCachePath: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = "book"
)