// Путь: src/test/java/com/voicedeutsch/master/data/local/database/entity/WordEntityTest.kt
package com.voicedeutsch.master.data.local.database.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WordEntityTest {

    private fun createEntity(
        id: String = "word_001",
        german: String = "der Hund",
        russian: String = "собака",
        partOfSpeech: String = "noun",
        gender: String? = "m",
        plural: String? = "die Hunde",
        conjugationJson: String? = null,
        declensionJson: String? = null,
        exampleSentenceDe: String = "Der Hund bellt.",
        exampleSentenceRu: String = "Собака лает.",
        phoneticTranscription: String? = "hʊnt",
        difficultyLevel: String = "A1",
        topic: String = "animals",
        bookChapter: Int? = 1,
        bookLesson: Int? = 2,
        audioCachePath: String? = null,
        createdAt: Long = 15_000_000L,
        source: String = "book",
    ) = WordEntity(
        id = id, german = german, russian = russian, partOfSpeech = partOfSpeech,
        gender = gender, plural = plural, conjugationJson = conjugationJson,
        declensionJson = declensionJson, exampleSentenceDe = exampleSentenceDe,
        exampleSentenceRu = exampleSentenceRu, phoneticTranscription = phoneticTranscription,
        difficultyLevel = difficultyLevel, topic = topic, bookChapter = bookChapter,
        bookLesson = bookLesson, audioCachePath = audioCachePath,
        createdAt = createdAt, source = source,
    )

    @Test
    fun creation_withAllFields_fieldsMatchExpected() {
        val entity = createEntity()
        assertEquals("word_001", entity.id)
        assertEquals("der Hund", entity.german)
        assertEquals("собака", entity.russian)
        assertEquals("noun", entity.partOfSpeech)
        assertEquals("m", entity.gender)
        assertEquals("die Hunde", entity.plural)
        assertNull(entity.conjugationJson)
        assertNull(entity.declensionJson)
        assertEquals("Der Hund bellt.", entity.exampleSentenceDe)
        assertEquals("Собака лает.", entity.exampleSentenceRu)
        assertEquals("hʊnt", entity.phoneticTranscription)
        assertEquals("A1", entity.difficultyLevel)
        assertEquals("animals", entity.topic)
        assertEquals(1, entity.bookChapter)
        assertEquals(2, entity.bookLesson)
        assertNull(entity.audioCachePath)
        assertEquals(15_000_000L, entity.createdAt)
        assertEquals("book", entity.source)
    }

    @Test
    fun creation_verbWithConjugation_conjugationJsonIsStored() {
        val json = """{"ich":"laufe","du":"läufst","er":"läuft"}"""
        val entity = createEntity(partOfSpeech = "verb", conjugationJson = json, gender = null, plural = null)
        assertEquals("verb", entity.partOfSpeech)
        assertEquals(json, entity.conjugationJson)
        assertNull(entity.gender)
    }

    @Test
    fun creation_nounWithDeclension_declensionJsonIsStored() {
        val json = """{"nom":"der Hund","gen":"des Hundes","dat":"dem Hund","acc":"den Hund"}"""
        assertEquals(json, createEntity(declensionJson = json).declensionJson)
    }

    @Test fun creation_withAudioCachePath_pathIsStored() = assertEquals("/cache/audio/hund.mp3", createEntity(audioCachePath = "/cache/audio/hund.mp3").audioCachePath)

    private fun minimal() = WordEntity(id = "w2", german = "laufen", russian = "бежать", partOfSpeech = "verb")

    @Test fun defaultGender_isNull() = assertNull(minimal().gender)
    @Test fun defaultPlural_isNull() = assertNull(minimal().plural)
    @Test fun defaultExampleSentenceDe_isEmptyString() = assertEquals("", minimal().exampleSentenceDe)
    @Test fun defaultExampleSentenceRu_isEmptyString() = assertEquals("", minimal().exampleSentenceRu)
    @Test fun defaultDifficultyLevel_isA1() = assertEquals("A1", minimal().difficultyLevel)
    @Test fun defaultTopic_isEmptyString() = assertEquals("", minimal().topic)
    @Test fun defaultSource_isBook() = assertEquals("book", minimal().source)
    @Test fun defaultBookChapter_isNull() = assertNull(minimal().bookChapter)
    @Test fun defaultAudioCachePath_isNull() = assertNull(minimal().audioCachePath)

    @Test
    fun defaultCreatedAt_isPositive() {
        val before = System.currentTimeMillis()
        val entity = WordEntity(id = "w3", german = "laufen", russian = "бежать", partOfSpeech = "verb")
        val after = System.currentTimeMillis()
        assertTrue(entity.createdAt in before..after)
    }

    @Test fun equals_sameFields_returnsTrue() = assertEquals(createEntity(), createEntity())
    @Test fun equals_differentGerman_returnsFalse() = assertNotEquals(createEntity(german = "der Hund"), createEntity(german = "die Katze"))
    @Test fun equals_differentPartOfSpeech_returnsFalse() = assertNotEquals(createEntity(partOfSpeech = "noun"), createEntity(partOfSpeech = "verb"))
    @Test fun hashCode_sameFields_sameHashCode() = assertEquals(createEntity().hashCode(), createEntity().hashCode())

    @Test
    fun copy_withNewDifficultyLevel_onlyDifficultyChanges() {
        val original = createEntity(difficultyLevel = "A1")
        val copied = original.copy(difficultyLevel = "B1")
        assertEquals("B1", copied.difficultyLevel)
        assertEquals(original.german, copied.german)
        assertEquals(original.partOfSpeech, copied.partOfSpeech)
    }

    @Test
    fun copy_setAudioCachePath_pathUpdated() {
        val copied = createEntity(audioCachePath = null).copy(audioCachePath = "/cache/audio/word.mp3")
        assertEquals("/cache/audio/word.mp3", copied.audioCachePath)
    }

    @Test
    fun copy_withNewTopic_topicUpdated() {
        val copied = createEntity(topic = "animals").copy(topic = "food")
        assertEquals("food", copied.topic)
    }

    @Test fun partOfSpeech_adjective_isAllowed() = assertEquals("adjective", createEntity(partOfSpeech = "adjective").partOfSpeech)
    @Test fun partOfSpeech_adverb_isAllowed() = assertEquals("adverb", createEntity(partOfSpeech = "adverb").partOfSpeech)
    @Test fun gender_femaleNoun_isAllowed() = assertEquals("f", createEntity(gender = "f").gender)
    @Test fun gender_neutralNoun_isAllowed() = assertEquals("n", createEntity(gender = "n").gender)
}
