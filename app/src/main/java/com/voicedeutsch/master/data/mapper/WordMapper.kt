package com.voicedeutsch.master.data.mapper

import com.voicedeutsch.master.data.local.database.entity.WordEntity
import com.voicedeutsch.master.domain.model.knowledge.Gender
import com.voicedeutsch.master.domain.model.knowledge.PartOfSpeech
import com.voicedeutsch.master.domain.model.knowledge.Word
import com.voicedeutsch.master.domain.model.knowledge.WordSource
import com.voicedeutsch.master.domain.model.user.CefrLevel

object WordMapper {

    fun WordEntity.toDomain(): Word = Word(
        id = id,
        german = german,
        russian = russian,
        partOfSpeech = try {
            PartOfSpeech.valueOf(partOfSpeech.uppercase())
        } catch (e: Exception) {
            PartOfSpeech.NOUN
        },
        gender = gender?.let { Gender.fromString(it) },
        plural = plural,
        conjugationJson = conjugationJson,
        declensionJson = declensionJson,
        exampleSentenceDe = exampleSentenceDe,
        exampleSentenceRu = exampleSentenceRu,
        phoneticTranscription = phoneticTranscription,
        difficultyLevel = CefrLevel.fromString(difficultyLevel),
        topic = topic,
        bookChapter = bookChapter,
        bookLesson = bookLesson,
        audioCachePath = audioCachePath,
        createdAt = createdAt,
        source = try {
            WordSource.valueOf(source.uppercase())
        } catch (e: Exception) {
            WordSource.BOOK
        }
    )

    fun Word.toEntity(): WordEntity = WordEntity(
        id = id,
        german = german,
        russian = russian,
        partOfSpeech = partOfSpeech.name,
        gender = gender?.name,
        plural = plural,
        conjugationJson = conjugationJson,
        declensionJson = declensionJson,
        exampleSentenceDe = exampleSentenceDe,
        exampleSentenceRu = exampleSentenceRu,
        phoneticTranscription = phoneticTranscription,
        difficultyLevel = difficultyLevel.name,
        topic = topic,
        bookChapter = bookChapter,
        bookLesson = bookLesson,
        audioCachePath = audioCachePath,
        createdAt = createdAt,
        source = source.name.lowercase()
    )
}