package com.voicedeutsch.master.domain.model.knowledge

import com.voicedeutsch.master.domain.model.user.CefrLevel
import kotlinx.serialization.Serializable

/**
 * Domain model of a single German word from the dictionary.
 *
 * Contains the German form, Russian translation, part of speech, grammatical gender
 * (for nouns), plural form, conjugation/declension JSON, example sentences,
 * phonetic transcription, CEFR difficulty, topic, and book coordinates.
 *
 * **Mappings:**
 * - Maps from/to `WordEntity`
 * - Linked to [WordKnowledge] (1:1 per user)
 * - Parsed from `vocabulary.json`
 * - Referenced in Function Calls: `save_word_knowledge`
 */
@Serializable
data class Word(
    val id: String,
    val german: String,
    val russian: String,
    val partOfSpeech: PartOfSpeech,
    val gender: Gender? = null,          // For nouns: der/die/das
    val plural: String? = null,          // Plural form
    val conjugationJson: String? = null, // For verbs
    val declensionJson: String? = null,  // For nouns/adjectives
    val exampleSentenceDe: String,
    val exampleSentenceRu: String,
    val phoneticTranscription: String? = null, // IPA
    val difficultyLevel: CefrLevel,
    val topic: String,                   // Essen, Reisen, Arbeit, etc.
    val bookChapter: Int? = null,
    val bookLesson: Int? = null,
    val audioCachePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val source: WordSource = WordSource.BOOK
) {
    /**
     * Returns the word with its definite article when the gender is known.
     * E.g., "der Tisch", "die Lampe", "das Buch".
     * Falls back to the bare word when gender is `null`.
     */
    val displayWithArticle: String get() = when (gender) {
        Gender.MASCULINE -> "der $german"
        Gender.FEMININE -> "die $german"
        Gender.NEUTER -> "das $german"
        null -> german
    }
}

/**
 * Grammatical part of speech.
 */
@Serializable
enum class PartOfSpeech {
    NOUN, VERB, ADJECTIVE, ADVERB, PREPOSITION,
    CONJUNCTION, PRONOUN, ARTICLE, NUMERAL, PARTICLE, INTERJECTION, OTHER
}

/**
 * Grammatical gender of a German noun.
 */
@Serializable
enum class Gender {
    MASCULINE,  // der
    FEMININE,   // die
    NEUTER;     // das

    companion object {
        /**
         * Parses a [Gender] from various string representations.
         * Accepts article forms ("der", "die", "das"), English names, and abbreviations.
         *
         * @return the matching [Gender] or `null` when the string is unrecognized
         */
        fun fromString(value: String): Gender? = when (value.lowercase()) {
            "der", "masculine", "m" -> MASCULINE
            "die", "feminine", "f" -> FEMININE
            "das", "neuter", "n" -> NEUTER
            else -> null
        }
    }

    /**
     * Returns the German definite article for this gender.
     */
    val article: String get() = when (this) {
        MASCULINE -> "der"
        FEMININE -> "die"
        NEUTER -> "das"
    }
}

/**
 * Origin source of a word entry.
 */
@Serializable
enum class WordSource { BOOK, CONVERSATION, MANUAL }