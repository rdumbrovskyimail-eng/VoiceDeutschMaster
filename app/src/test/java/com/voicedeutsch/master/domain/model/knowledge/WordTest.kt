// Путь: src/test/java/com/voicedeutsch/master/domain/model/knowledge/WordTest.kt
package com.voicedeutsch.master.domain.model.knowledge

import com.voicedeutsch.master.domain.model.user.CefrLevel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WordTest {

    private fun createWord(
        id: String = "word_001",
        german: String = "Tisch",
        russian: String = "стол",
        partOfSpeech: PartOfSpeech = PartOfSpeech.NOUN,
        gender: Gender? = Gender.MASCULINE,
        plural: String? = null,
        conjugationJson: String? = null,
        declensionJson: String? = null,
        exampleSentenceDe: String = "Der Tisch ist groß.",
        exampleSentenceRu: String = "Стол большой.",
        phoneticTranscription: String? = null,
        difficultyLevel: CefrLevel = CefrLevel.A1,
        topic: String = "Möbel",
        bookChapter: Int? = null,
        bookLesson: Int? = null,
        audioCachePath: String? = null,
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
        source = source,
    )

    // ── Word — creation ───────────────────────────────────────────────────

    @Test
    fun creation_requiredFields_storedCorrectly() {
        val word = createWord(id = "w1", german = "Hund", russian = "собака")
        assertEquals("w1", word.id)
        assertEquals("Hund", word.german)
        assertEquals("собака", word.russian)
    }

    @Test
    fun creation_defaultGender_isNull_whenNotProvided() {
        val word = createWord(gender = null)
        assertNull(word.gender)
    }

    @Test
    fun creation_defaultPlural_isNull() {
        assertNull(createWord(plural = null).plural)
    }

    @Test
    fun creation_defaultConjugationJson_isNull() {
        assertNull(createWord().conjugationJson)
    }

    @Test
    fun creation_defaultDeclensionJson_isNull() {
        assertNull(createWord().declensionJson)
    }

    @Test
    fun creation_defaultPhoneticTranscription_isNull() {
        assertNull(createWord().phoneticTranscription)
    }

    @Test
    fun creation_defaultAudioCachePath_isNull() {
        assertNull(createWord().audioCachePath)
    }

    @Test
    fun creation_defaultBookChapter_isNull() {
        assertNull(createWord().bookChapter)
    }

    @Test
    fun creation_defaultBookLesson_isNull() {
        assertNull(createWord().bookLesson)
    }

    @Test
    fun creation_defaultSource_isBook() {
        assertEquals(WordSource.BOOK, createWord().source)
    }

    @Test
    fun creation_createdAt_isPositive() {
        assertTrue(createWord().createdAt > 0L)
    }

    @Test
    fun creation_withAllOptionalFields_storedCorrectly() {
        val word = createWord(
            plural = "Tische",
            conjugationJson = """{"present":"ist"}""",
            declensionJson = """{"gen":"Tisches"}""",
            phoneticTranscription = "[tɪʃ]",
            audioCachePath = "/cache/tisch.mp3",
            bookChapter = 2,
            bookLesson = 4,
            source = WordSource.CONVERSATION,
        )
        assertEquals("Tische", word.plural)
        assertEquals("""{"present":"ist"}""", word.conjugationJson)
        assertEquals("[tɪʃ]", word.phoneticTranscription)
        assertEquals("/cache/tisch.mp3", word.audioCachePath)
        assertEquals(2, word.bookChapter)
        assertEquals(4, word.bookLesson)
        assertEquals(WordSource.CONVERSATION, word.source)
    }

    // ── displayWithArticle ────────────────────────────────────────────────

    @Test
    fun displayWithArticle_masculineGender_returnsDer() {
        val word = createWord(german = "Tisch", gender = Gender.MASCULINE)
        assertEquals("der Tisch", word.displayWithArticle)
    }

    @Test
    fun displayWithArticle_feminineGender_returnsDie() {
        val word = createWord(german = "Lampe", gender = Gender.FEMININE)
        assertEquals("die Lampe", word.displayWithArticle)
    }

    @Test
    fun displayWithArticle_neuterGender_returnsDas() {
        val word = createWord(german = "Buch", gender = Gender.NEUTER)
        assertEquals("das Buch", word.displayWithArticle)
    }

    @Test
    fun displayWithArticle_nullGender_returnsBareWord() {
        val word = createWord(german = "schnell", gender = null)
        assertEquals("schnell", word.displayWithArticle)
    }

    // ── Word — equals / hashCode / copy ───────────────────────────────────

    @Test
    fun equals_twoIdentical_returnsTrue() {
        val ts = 1_000L
        assertEquals(createWord().copy(createdAt = ts), createWord().copy(createdAt = ts))
    }

    @Test
    fun equals_differentId_returnsFalse() {
        assertNotEquals(createWord(id = "a"), createWord(id = "b"))
    }

    @Test
    fun hashCode_equalWords_sameHash() {
        val ts = 1_000L
        assertEquals(
            createWord().copy(createdAt = ts).hashCode(),
            createWord().copy(createdAt = ts).hashCode(),
        )
    }

    @Test
    fun copy_changesOnlySpecifiedField() {
        val original = createWord(german = "Alt")
        val copied = original.copy(german = "Neu")
        assertEquals("Neu", copied.german)
        assertEquals(original.id, copied.id)
        assertEquals(original.partOfSpeech, copied.partOfSpeech)
    }

    // ── PartOfSpeech enum ─────────────────────────────────────────────────

    @Test
    fun partOfSpeech_entryCount_equals12() {
        assertEquals(12, PartOfSpeech.entries.size)
    }

    @Test
    fun partOfSpeech_containsAllExpectedValues() {
        val expected = setOf(
            PartOfSpeech.NOUN, PartOfSpeech.VERB, PartOfSpeech.ADJECTIVE,
            PartOfSpeech.ADVERB, PartOfSpeech.PREPOSITION, PartOfSpeech.CONJUNCTION,
            PartOfSpeech.PRONOUN, PartOfSpeech.ARTICLE, PartOfSpeech.NUMERAL,
            PartOfSpeech.PARTICLE, PartOfSpeech.INTERJECTION, PartOfSpeech.OTHER,
        )
        assertEquals(expected, PartOfSpeech.entries.toSet())
    }

    @Test
    fun partOfSpeech_valueOf_noun() {
        assertEquals(PartOfSpeech.NOUN, PartOfSpeech.valueOf("NOUN"))
    }

    @Test
    fun partOfSpeech_valueOf_interjection() {
        assertEquals(PartOfSpeech.INTERJECTION, PartOfSpeech.valueOf("INTERJECTION"))
    }

    @Test
    fun partOfSpeech_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            PartOfSpeech.valueOf("UNKNOWN")
        }
    }

    @Test
    fun partOfSpeech_ordinalsAreUnique() {
        val ordinals = PartOfSpeech.entries.map { it.ordinal }
        assertEquals(ordinals.size, ordinals.toSet().size)
    }

    // ── Gender enum ───────────────────────────────────────────────────────

    @Test
    fun gender_entryCount_equals3() {
        assertEquals(3, Gender.entries.size)
    }

    @Test
    fun gender_containsAllValues() {
        val expected = setOf(Gender.MASCULINE, Gender.FEMININE, Gender.NEUTER)
        assertEquals(expected, Gender.entries.toSet())
    }

    @Test
    fun gender_article_masculine_returnsDer() {
        assertEquals("der", Gender.MASCULINE.article)
    }

    @Test
    fun gender_article_feminine_returnsDie() {
        assertEquals("die", Gender.FEMININE.article)
    }

    @Test
    fun gender_article_neuter_returnsDas() {
        assertEquals("das", Gender.NEUTER.article)
    }

    @Test
    fun gender_fromString_derReturnsMasculine() {
        assertEquals(Gender.MASCULINE, Gender.fromString("der"))
    }

    @Test
    fun gender_fromString_dieReturnsFeminine() {
        assertEquals(Gender.FEMININE, Gender.fromString("die"))
    }

    @Test
    fun gender_fromString_dasReturnsNeuter() {
        assertEquals(Gender.NEUTER, Gender.fromString("das"))
    }

    @Test
    fun gender_fromString_masculineKeyword_returnsMasculine() {
        assertEquals(Gender.MASCULINE, Gender.fromString("masculine"))
    }

    @Test
    fun gender_fromString_feminineKeyword_returnsFeminine() {
        assertEquals(Gender.FEMININE, Gender.fromString("feminine"))
    }

    @Test
    fun gender_fromString_neuterKeyword_returnsNeuter() {
        assertEquals(Gender.NEUTER, Gender.fromString("neuter"))
    }

    @Test
    fun gender_fromString_abbreviationM_returnsMasculine() {
        assertEquals(Gender.MASCULINE, Gender.fromString("m"))
    }

    @Test
    fun gender_fromString_abbreviationF_returnsFeminine() {
        assertEquals(Gender.FEMININE, Gender.fromString("f"))
    }

    @Test
    fun gender_fromString_abbreviationN_returnsNeuter() {
        assertEquals(Gender.NEUTER, Gender.fromString("n"))
    }

    @Test
    fun gender_fromString_uppercaseDer_returnsMasculine() {
        assertEquals(Gender.MASCULINE, Gender.fromString("DER"))
    }

    @Test
    fun gender_fromString_unknownValue_returnsNull() {
        assertNull(Gender.fromString("unknown"))
    }

    @Test
    fun gender_fromString_emptyString_returnsNull() {
        assertNull(Gender.fromString(""))
    }

    // ── WordSource enum ───────────────────────────────────────────────────

    @Test
    fun wordSource_entryCount_equals3() {
        assertEquals(3, WordSource.entries.size)
    }

    @Test
    fun wordSource_containsAllValues() {
        val expected = setOf(WordSource.BOOK, WordSource.CONVERSATION, WordSource.MANUAL)
        assertEquals(expected, WordSource.entries.toSet())
    }

    @Test
    fun wordSource_valueOf_book() {
        assertEquals(WordSource.BOOK, WordSource.valueOf("BOOK"))
    }

    @Test
    fun wordSource_valueOf_conversation() {
        assertEquals(WordSource.CONVERSATION, WordSource.valueOf("CONVERSATION"))
    }

    @Test
    fun wordSource_valueOf_manual() {
        assertEquals(WordSource.MANUAL, WordSource.valueOf("MANUAL"))
    }

    @Test
    fun wordSource_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            WordSource.valueOf("IMPORTED")
        }
    }
}
