// Путь: src/test/java/com/voicedeutsch/master/data/mapper/WordMapperTest.kt
package com.voicedeutsch.master.data.mapper

import com.voicedeutsch.master.data.local.database.entity.WordEntity
import com.voicedeutsch.master.domain.model.knowledge.Gender
import com.voicedeutsch.master.domain.model.knowledge.PartOfSpeech
import com.voicedeutsch.master.domain.model.knowledge.Word
import com.voicedeutsch.master.domain.model.knowledge.WordSource
import com.voicedeutsch.master.domain.model.user.CefrLevel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WordMapperTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildWordEntity(
        id: String = "word_1",
        german: String = "der Hund",
        russian: String = "собака",
        partOfSpeech: String = "NOUN",
        gender: String? = "MASCULINE",
        plural: String? = "die Hunde",
        conjugationJson: String? = null,
        declensionJson: String? = null,
        exampleSentenceDe: String = "Der Hund läuft.",
        exampleSentenceRu: String = "Собака бежит.",
        phoneticTranscription: String? = "huːnt",
        difficultyLevel: String = "A1",
        topic: String = "animals",
        bookChapter: Int? = 1,
        bookLesson: Int? = 2,
        audioCachePath: String? = "/audio/hund.mp3",
        createdAt: Long = 1000L,
        source: String = "book",
    ) = WordEntity(
        id = id,
        german = german,
        russian = russian,
        partOfSpeech = partOfSpeech,
        gender = gender,
        plural = plural,
        conjugationJson = conjugationJson,
        declensionJson = declensionJson,
        exampleSentenceDe = exampleSentenceDe,
        exampleSentenceRu = exampleSentenceRu,
        phoneticTranscription = phoneticTranscription,
        difficultyLevel = difficultyLevel,
        topic = topic,
        bookChapter = bookChapter,
        bookLesson = bookLesson,
        audioCachePath = audioCachePath,
        createdAt = createdAt,
        source = source,
    )

    private fun buildWord(
        id: String = "word_1",
        german: String = "der Hund",
        russian: String = "собака",
        partOfSpeech: PartOfSpeech = PartOfSpeech.NOUN,
        gender: Gender? = Gender.MASCULINE,
        plural: String? = "die Hunde",
        conjugationJson: String? = null,
        declensionJson: String? = null,
        exampleSentenceDe: String? = "Der Hund läuft.",
        exampleSentenceRu: String? = "Собака бежит.",
        phoneticTranscription: String? = "huːnt",
        difficultyLevel: CefrLevel = CefrLevel.A1,
        topic: String? = "animals",
        bookChapter: Int? = 1,
        bookLesson: Int? = 2,
        audioCachePath: String? = "/audio/hund.mp3",
        createdAt: Long = 1000L,
        source: WordSource = WordSource.BOOK,
    ) = Word(
        id = id,
        german = german,
        russian = russian,
        partOfSpeech = partOfSpeech,
        gender = gender,
        plural = plural,
        conjugationJson = conjugationJson,
        declensionJson = declensionJson,
        exampleSentenceDe = exampleSentenceDe,
        exampleSentenceRu = exampleSentenceRu,
        phoneticTranscription = phoneticTranscription,
        difficultyLevel = difficultyLevel,
        topic = topic,
        bookChapter = bookChapter,
        bookLesson = bookLesson,
        audioCachePath = audioCachePath,
        createdAt = createdAt,
        source = source,
    )

    // ── WordEntity.toDomain ───────────────────────────────────────────────────

    @Test
    fun wordEntity_toDomain_validData_mapsAllFields() {
        val entity = buildWordEntity()
        with(WordMapper) {
            val domain = entity.toDomain()
            assertEquals(entity.id, domain.id)
            assertEquals(entity.german, domain.german)
            assertEquals(entity.russian, domain.russian)
            assertEquals(PartOfSpeech.NOUN, domain.partOfSpeech)
            assertEquals(entity.plural, domain.plural)
            assertEquals(entity.conjugationJson, domain.conjugationJson)
            assertEquals(entity.declensionJson, domain.declensionJson)
            assertEquals(entity.exampleSentenceDe, domain.exampleSentenceDe)
            assertEquals(entity.exampleSentenceRu, domain.exampleSentenceRu)
            assertEquals(entity.phoneticTranscription, domain.phoneticTranscription)
            assertEquals(entity.topic, domain.topic)
            assertEquals(entity.bookChapter, domain.bookChapter)
            assertEquals(entity.bookLesson, domain.bookLesson)
            assertEquals(entity.audioCachePath, domain.audioCachePath)
            assertEquals(entity.createdAt, domain.createdAt)
        }
    }

    @Test
    fun wordEntity_toDomain_allPartOfSpeechValues_mappedCorrectly() {
        PartOfSpeech.entries.forEach { expected ->
            val entity = buildWordEntity(partOfSpeech = expected.name)
            with(WordMapper) {
                assertEquals(expected, entity.toDomain().partOfSpeech)
            }
        }
    }

    @Test
    fun wordEntity_toDomain_partOfSpeechLowercase_parsedCaseInsensitive() {
        val entity = buildWordEntity(partOfSpeech = "noun")
        with(WordMapper) {
            assertEquals(PartOfSpeech.NOUN, entity.toDomain().partOfSpeech)
        }
    }

    @Test
    fun wordEntity_toDomain_partOfSpeechMixedCase_parsedCaseInsensitive() {
        val entity = buildWordEntity(partOfSpeech = "Verb")
        with(WordMapper) {
            assertEquals(PartOfSpeech.VERB, entity.toDomain().partOfSpeech)
        }
    }

    @Test
    fun wordEntity_toDomain_invalidPartOfSpeech_fallsBackToNoun() {
        val entity = buildWordEntity(partOfSpeech = "TOTALLY_UNKNOWN")
        with(WordMapper) {
            assertEquals(PartOfSpeech.NOUN, entity.toDomain().partOfSpeech)
        }
    }

    @Test
    fun wordEntity_toDomain_emptyPartOfSpeech_fallsBackToNoun() {
        val entity = buildWordEntity(partOfSpeech = "")
        with(WordMapper) {
            assertEquals(PartOfSpeech.NOUN, entity.toDomain().partOfSpeech)
        }
    }

    @Test
    fun wordEntity_toDomain_allGenderValues_mappedCorrectly() {
        Gender.entries.forEach { expected ->
            val entity = buildWordEntity(gender = expected.name)
            with(WordMapper) {
                assertEquals(expected, entity.toDomain().gender)
            }
        }
    }

    @Test
    fun wordEntity_toDomain_nullGender_preservedAsNull() {
        val entity = buildWordEntity(gender = null)
        with(WordMapper) {
            assertNull(entity.toDomain().gender)
        }
    }

    @Test
    fun wordEntity_toDomain_allCefrLevels_mappedCorrectly() {
        CefrLevel.entries.forEach { level ->
            val entity = buildWordEntity(difficultyLevel = level.name)
            with(WordMapper) {
                assertEquals(level, entity.toDomain().difficultyLevel)
            }
        }
    }

    @Test
    fun wordEntity_toDomain_allWordSourceValues_mappedCorrectly() {
        WordSource.entries.forEach { expected ->
            val entity = buildWordEntity(source = expected.name)
            with(WordMapper) {
                assertEquals(expected, entity.toDomain().source)
            }
        }
    }

    @Test
    fun wordEntity_toDomain_sourceLowercase_parsedCaseInsensitive() {
        val entity = buildWordEntity(source = "book")
        with(WordMapper) {
            assertEquals(WordSource.BOOK, entity.toDomain().source)
        }
    }

    @Test
    fun wordEntity_toDomain_sourceMixedCase_parsedCaseInsensitive() {
        val entity = buildWordEntity(source = "Book")
        with(WordMapper) {
            assertEquals(WordSource.BOOK, entity.toDomain().source)
        }
    }

    @Test
    fun wordEntity_toDomain_invalidSource_fallsBackToBook() {
        val entity = buildWordEntity(source = "INVALID_SOURCE_XYZ")
        with(WordMapper) {
            assertEquals(WordSource.BOOK, entity.toDomain().source)
        }
    }

    @Test
    fun wordEntity_toDomain_emptySource_fallsBackToBook() {
        val entity = buildWordEntity(source = "")
        with(WordMapper) {
            assertEquals(WordSource.BOOK, entity.toDomain().source)
        }
    }

    @Test
    fun wordEntity_toDomain_nullOptionalFields_preservedAsNull() {
        val entity = buildWordEntity(
            gender = null,
            plural = null,
            conjugationJson = null,
            declensionJson = null,
            exampleSentenceDe = "",
            exampleSentenceRu = "",
            phoneticTranscription = null,
            topic = "",
            bookChapter = null,
            bookLesson = null,
            audioCachePath = null,
        )
        with(WordMapper) {
            val domain = entity.toDomain()
            assertNull(domain.gender)
            assertNull(domain.plural)
            assertNull(domain.conjugationJson)
            assertNull(domain.declensionJson)
            assertEquals("", domain.exampleSentenceDe)
            assertEquals("", domain.exampleSentenceRu)
            assertNull(domain.phoneticTranscription)
            assertEquals("", domain.topic)
            assertNull(domain.bookChapter)
            assertNull(domain.bookLesson)
            assertNull(domain.audioCachePath)
        }
    }

    // ── Word.toEntity ─────────────────────────────────────────────────────────

    @Test
    fun word_toEntity_validData_mapsAllFields() {
        val domain = buildWord()
        with(WordMapper) {
            val entity = domain.toEntity()
            assertEquals(domain.id, entity.id)
            assertEquals(domain.german, entity.german)
            assertEquals(domain.russian, entity.russian)
            assertEquals(domain.partOfSpeech.name, entity.partOfSpeech)
            assertEquals(domain.plural, entity.plural)
            assertEquals(domain.conjugationJson, entity.conjugationJson)
            assertEquals(domain.declensionJson, entity.declensionJson)
            assertEquals(domain.exampleSentenceDe, entity.exampleSentenceDe)
            assertEquals(domain.exampleSentenceRu, entity.exampleSentenceRu)
            assertEquals(domain.phoneticTranscription, entity.phoneticTranscription)
            assertEquals(domain.difficultyLevel.name, entity.difficultyLevel)
            assertEquals(domain.topic, entity.topic)
            assertEquals(domain.bookChapter, entity.bookChapter)
            assertEquals(domain.bookLesson, entity.bookLesson)
            assertEquals(domain.audioCachePath, entity.audioCachePath)
            assertEquals(domain.createdAt, entity.createdAt)
        }
    }

    @Test
    fun word_toEntity_partOfSpeechStoredAsEnumName() {
        PartOfSpeech.entries.forEach { pos ->
            val domain = buildWord(partOfSpeech = pos)
            with(WordMapper) {
                assertEquals(pos.name, domain.toEntity().partOfSpeech)
            }
        }
    }

    @Test
    fun word_toEntity_genderStoredAsEnumName() {
        Gender.entries.forEach { g ->
            val domain = buildWord(gender = g)
            with(WordMapper) {
                assertEquals(g.name, domain.toEntity().gender)
            }
        }
    }

    @Test
    fun word_toEntity_nullGender_storedAsNull() {
        val domain = buildWord(gender = null)
        with(WordMapper) {
            assertNull(domain.toEntity().gender)
        }
    }

    @Test
    fun word_toEntity_difficultyLevelStoredAsEnumName() {
        CefrLevel.entries.forEach { level ->
            val domain = buildWord(difficultyLevel = level)
            with(WordMapper) {
                assertEquals(level.name, domain.toEntity().difficultyLevel)
            }
        }
    }

    @Test
    fun word_toEntity_sourceStoredAsLowercase() {
        WordSource.entries.forEach { source ->
            val domain = buildWord(source = source)
            with(WordMapper) {
                assertEquals(source.name.lowercase(), domain.toEntity().source)
            }
        }
    }

    @Test
    fun word_toEntity_sourceIsAlwaysLowercaseNotUppercase() {
        val domain = buildWord(source = WordSource.BOOK)
        with(WordMapper) {
            val entity = domain.toEntity()
            assertEquals(entity.source, entity.source.lowercase())
        }
    }

    @Test
    fun word_toEntity_nullOptionalFields_preservedAsNull() {
        val domain = buildWord(
            gender = null,
            plural = null,
            conjugationJson = null,
            declensionJson = null,
            exampleSentenceDe = null,
            exampleSentenceRu = null,
            phoneticTranscription = null,
            topic = null,
            bookChapter = null,
            bookLesson = null,
            audioCachePath = null,
        )
        with(WordMapper) {
            val entity = domain.toEntity()
            assertNull(entity.gender)
            assertNull(entity.plural)
            assertNull(entity.conjugationJson)
            assertNull(entity.declensionJson)
            assertEquals("", entity.exampleSentenceDe)
            assertEquals("", entity.exampleSentenceRu)
            assertNull(entity.phoneticTranscription)
            assertEquals("", entity.topic)
            assertNull(entity.bookChapter)
            assertNull(entity.bookLesson)
            assertNull(entity.audioCachePath)
        }
    }

    // ── Word roundtrip ────────────────────────────────────────────────────────

    @Test
    fun word_roundtrip_entityToDomainToEntity_scalarFieldsMatch() {
        val original = buildWordEntity()
        with(WordMapper) {
            val domain = original.toDomain()
            val restored = domain.toEntity()
            assertEquals(original.id, restored.id)
            assertEquals(original.german, restored.german)
            assertEquals(original.russian, restored.russian)
            assertEquals(original.plural, restored.plural)
            assertEquals(original.conjugationJson, restored.conjugationJson)
            assertEquals(original.declensionJson, restored.declensionJson)
            assertEquals(original.exampleSentenceDe, restored.exampleSentenceDe)
            assertEquals(original.exampleSentenceRu, restored.exampleSentenceRu)
            assertEquals(original.phoneticTranscription, restored.phoneticTranscription)
            assertEquals(original.topic, restored.topic)
            assertEquals(original.bookChapter, restored.bookChapter)
            assertEquals(original.bookLesson, restored.bookLesson)
            assertEquals(original.audioCachePath, restored.audioCachePath)
            assertEquals(original.createdAt, restored.createdAt)
        }
    }

    @Test
    fun word_roundtrip_partOfSpeechPreserved() {
        PartOfSpeech.entries.forEach { pos ->
            val original = buildWordEntity(partOfSpeech = pos.name)
            with(WordMapper) {
                val domain = original.toDomain()
                val restored = domain.toEntity()
                assertEquals(pos.name, restored.partOfSpeech)
            }
        }
    }

    @Test
    fun word_roundtrip_genderPreserved() {
        Gender.entries.forEach { g ->
            val original = buildWordEntity(gender = g.name)
            with(WordMapper) {
                val domain = original.toDomain()
                val restored = domain.toEntity()
                assertEquals(g.name, restored.gender)
            }
        }
    }

    @Test
    fun word_roundtrip_invalidPartOfSpeech_restoredAsNoun() {
        val original = buildWordEntity(partOfSpeech = "GARBAGE")
        with(WordMapper) {
            val domain = original.toDomain()
            assertEquals(PartOfSpeech.NOUN, domain.partOfSpeech)
            val restored = domain.toEntity()
            assertEquals(PartOfSpeech.NOUN.name, restored.partOfSpeech)
        }
    }

    @Test
    fun word_roundtrip_invalidSource_restoredAsBook() {
        val original = buildWordEntity(source = "GARBAGE")
        with(WordMapper) {
            val domain = original.toDomain()
            assertEquals(WordSource.BOOK, domain.source)
            val restored = domain.toEntity()
            assertEquals(WordSource.BOOK.name.lowercase(), restored.source)
        }
    }

    @Test
    fun word_roundtrip_nullGender_preservedAsNull() {
        val original = buildWordEntity(gender = null)
        with(WordMapper) {
            val domain = original.toDomain()
            assertNull(domain.gender)
            val restored = domain.toEntity()
            assertNull(restored.gender)
        }
    }

    @Test
    fun word_roundtrip_nullOptionalFields_preservedAsNull() {
        val original = buildWordEntity(
            gender = null,
            plural = null,
            conjugationJson = null,
            declensionJson = null,
            exampleSentenceDe = "",
            exampleSentenceRu = "",
            phoneticTranscription = null,
            topic = "",
            bookChapter = null,
            bookLesson = null,
            audioCachePath = null,
        )
        with(WordMapper) {
            val domain = original.toDomain()
            val restored = domain.toEntity()
            assertNull(restored.gender)
            assertNull(restored.plural)
            assertNull(restored.conjugationJson)
            assertNull(restored.declensionJson)
            assertEquals("", restored.exampleSentenceDe)
            assertEquals("", restored.exampleSentenceRu)
            assertNull(restored.phoneticTranscription)
            assertEquals("", restored.topic)
            assertNull(restored.bookChapter)
            assertNull(restored.bookLesson)
            assertNull(restored.audioCachePath)
        }
    }

    @Test
    fun word_roundtrip_sourceLowercaseInEntity_restoredCorrectly() {
        WordSource.entries.forEach { source ->
            val original = buildWordEntity(source = source.name.lowercase())
            with(WordMapper) {
                val domain = original.toDomain()
                assertEquals(source, domain.source)
                val restored = domain.toEntity()
                assertEquals(source.name.lowercase(), restored.source)
            }
        }
    }
}
