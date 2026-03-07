// Путь: src/test/java/com/voicedeutsch/master/domain/model/knowledge/PhraseTest.kt
package com.voicedeutsch.master.domain.model.knowledge

import com.voicedeutsch.master.domain.model.user.CefrLevel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PhraseTest {

    private fun createPhrase(
        id: String = "phrase_001",
        german: String = "Guten Morgen",
        russian: String = "Доброе утро",
        category: PhraseCategory = PhraseCategory.GREETING,
        difficultyLevel: CefrLevel = CefrLevel.A1,
        bookChapter: Int? = null,
        bookLesson: Int? = null,
        context: String = "",
    ) = Phrase(
        id = id,
        german = german,
        russian = russian,
        category = category,
        difficultyLevel = difficultyLevel,
        bookChapter = bookChapter,
        bookLesson = bookLesson,
        context = context,
    )

    private fun createKnowledge(
        id: String = "pk_001",
        userId: String = "user_1",
        phraseId: String = "phrase_001",
        knowledgeLevel: Int = 0,
        timesPracticed: Int = 0,
        timesCorrect: Int = 0,
        lastPracticed: Long? = null,
        nextReview: Long? = null,
        srsIntervalDays: Float = 0f,
        srsEaseFactor: Float = 2.5f,
        pronunciationScore: Float = 0f,
    ) = PhraseKnowledge(
        id = id,
        userId = userId,
        phraseId = phraseId,
        knowledgeLevel = knowledgeLevel,
        timesPracticed = timesPracticed,
        timesCorrect = timesCorrect,
        lastPracticed = lastPracticed,
        nextReview = nextReview,
        srsIntervalDays = srsIntervalDays,
        srsEaseFactor = srsEaseFactor,
        pronunciationScore = pronunciationScore,
    )

    // ── Phrase — creation ─────────────────────────────────────────────────

    @Test
    fun phrase_requiredFields_storedCorrectly() {
        val phrase = createPhrase(id = "p1", german = "Hallo", russian = "Привет")
        assertEquals("p1", phrase.id)
        assertEquals("Hallo", phrase.german)
        assertEquals("Привет", phrase.russian)
    }

    @Test
    fun phrase_defaultBookChapter_isNull() {
        assertNull(createPhrase().bookChapter)
    }

    @Test
    fun phrase_defaultBookLesson_isNull() {
        assertNull(createPhrase().bookLesson)
    }

    @Test
    fun phrase_defaultContext_isEmpty() {
        assertEquals("", createPhrase().context)
    }

    @Test
    fun phrase_createdAt_isPositive() {
        assertTrue(createPhrase().createdAt > 0L)
    }

    @Test
    fun phrase_withBookChapterAndLesson_storedCorrectly() {
        val phrase = createPhrase(bookChapter = 3, bookLesson = 5)
        assertEquals(3, phrase.bookChapter)
        assertEquals(5, phrase.bookLesson)
    }

    @Test
    fun phrase_withContext_storedCorrectly() {
        val phrase = createPhrase(context = "Am Bahnhof")
        assertEquals("Am Bahnhof", phrase.context)
    }

    @Test
    fun phrase_difficultyLevel_storedCorrectly() {
        assertEquals(CefrLevel.B1, createPhrase(difficultyLevel = CefrLevel.B1).difficultyLevel)
    }

    @Test
    fun phrase_category_storedCorrectly() {
        assertEquals(PhraseCategory.TRAVEL, createPhrase(category = PhraseCategory.TRAVEL).category)
    }

    // ── Phrase — equals / hashCode / copy ────────────────────────────────

    @Test
    fun phrase_equals_twoIdentical() {
        val ts = 1_000L
        assertEquals(createPhrase().copy(createdAt = ts), createPhrase().copy(createdAt = ts))
    }

    @Test
    fun phrase_notEquals_differentId() {
        assertNotEquals(createPhrase(id = "a"), createPhrase(id = "b"))
    }

    @Test
    fun phrase_hashCode_equalPhrasesSameHash() {
        val ts = 1_000L
        assertEquals(
            createPhrase().copy(createdAt = ts).hashCode(),
            createPhrase().copy(createdAt = ts).hashCode(),
        )
    }

    @Test
    fun phrase_copy_changesOnlySpecifiedField() {
        val original = createPhrase(german = "Hallo")
        val copied = original.copy(german = "Tschüss")
        assertEquals("Tschüss", copied.german)
        assertEquals(original.id, copied.id)
        assertEquals(original.category, copied.category)
    }

    // ── PhraseCategory enum ───────────────────────────────────────────────

    @Test
    fun phraseCategory_entryCount_equals12() {
        assertEquals(12, PhraseCategory.entries.size)
    }

    @Test
    fun phraseCategory_containsAllExpectedValues() {
        val expected = setOf(
            PhraseCategory.GREETING, PhraseCategory.FAREWELL,
            PhraseCategory.TRAVEL, PhraseCategory.WORK,
            PhraseCategory.DAILY, PhraseCategory.SHOPPING,
            PhraseCategory.RESTAURANT, PhraseCategory.HEALTH,
            PhraseCategory.EMERGENCY, PhraseCategory.POLITE,
            PhraseCategory.OPINION, PhraseCategory.OTHER,
        )
        assertEquals(expected, PhraseCategory.entries.toSet())
    }

    @Test
    fun phraseCategory_valueOf_greeting() {
        assertEquals(PhraseCategory.GREETING, PhraseCategory.valueOf("GREETING"))
    }

    @Test
    fun phraseCategory_valueOf_emergency() {
        assertEquals(PhraseCategory.EMERGENCY, PhraseCategory.valueOf("EMERGENCY"))
    }

    @Test
    fun phraseCategory_valueOf_other() {
        assertEquals(PhraseCategory.OTHER, PhraseCategory.valueOf("OTHER"))
    }

    @Test
    fun phraseCategory_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            PhraseCategory.valueOf("UNKNOWN")
        }
    }

    @Test
    fun phraseCategory_ordinalsAreUnique() {
        val ordinals = PhraseCategory.entries.map { it.ordinal }
        assertEquals(ordinals.size, ordinals.toSet().size)
    }

    // ── PhraseKnowledge — creation ────────────────────────────────────────

    @Test
    fun phraseKnowledge_requiredFields_storedCorrectly() {
        val pk = createKnowledge(id = "pk1", userId = "u1", phraseId = "p1")
        assertEquals("pk1", pk.id)
        assertEquals("u1", pk.userId)
        assertEquals("p1", pk.phraseId)
    }

    @Test
    fun phraseKnowledge_defaultKnowledgeLevel_isZero() {
        assertEquals(0, createKnowledge().knowledgeLevel)
    }

    @Test
    fun phraseKnowledge_defaultTimesPracticed_isZero() {
        assertEquals(0, createKnowledge().timesPracticed)
    }

    @Test
    fun phraseKnowledge_defaultTimesCorrect_isZero() {
        assertEquals(0, createKnowledge().timesCorrect)
    }

    @Test
    fun phraseKnowledge_defaultLastPracticed_isNull() {
        assertNull(createKnowledge().lastPracticed)
    }

    @Test
    fun phraseKnowledge_defaultNextReview_isNull() {
        assertNull(createKnowledge().nextReview)
    }

    @Test
    fun phraseKnowledge_defaultSrsIntervalDays_isZero() {
        assertEquals(0f, createKnowledge().srsIntervalDays)
    }

    @Test
    fun phraseKnowledge_defaultSrsEaseFactor_is2point5() {
        assertEquals(2.5f, createKnowledge().srsEaseFactor)
    }

    @Test
    fun phraseKnowledge_defaultPronunciationScore_isZero() {
        assertEquals(0f, createKnowledge().pronunciationScore)
    }

    @Test
    fun phraseKnowledge_createdAt_isPositive() {
        assertTrue(createKnowledge().createdAt > 0L)
    }

    @Test
    fun phraseKnowledge_updatedAt_isPositive() {
        assertTrue(createKnowledge().updatedAt > 0L)
    }

    // ── PhraseKnowledge — needsReview ─────────────────────────────────────

    @Test
    fun needsReview_pastTimestamp_returnsTrue() {
        val pk = createKnowledge(nextReview = System.currentTimeMillis() - 1_000L)
        assertTrue(pk.needsReview)
    }

    @Test
    fun needsReview_futureTimestamp_returnsFalse() {
        val pk = createKnowledge(nextReview = System.currentTimeMillis() + 100_000L)
        assertFalse(pk.needsReview)
    }

    @Test
    fun needsReview_nullNextReview_returnsFalse() {
        assertFalse(createKnowledge(nextReview = null).needsReview)
    }

    // ── PhraseKnowledge — equals / hashCode / copy ────────────────────────

    @Test
    fun phraseKnowledge_equals_twoIdentical() {
        val ts = 1_000L
        assertEquals(
            createKnowledge().copy(createdAt = ts, updatedAt = ts),
            createKnowledge().copy(createdAt = ts, updatedAt = ts),
        )
    }

    @Test
    fun phraseKnowledge_notEquals_differentId() {
        assertNotEquals(createKnowledge(id = "a"), createKnowledge(id = "b"))
    }

    @Test
    fun phraseKnowledge_copy_changesOnlyKnowledgeLevel() {
        val original = createKnowledge(knowledgeLevel = 1)
        val copied = original.copy(knowledgeLevel = 5)
        assertEquals(5, copied.knowledgeLevel)
        assertEquals(original.id, copied.id)
        assertEquals(original.phraseId, copied.phraseId)
    }

    @Test
    fun phraseKnowledge_hashCode_equalObjectsSameHash() {
        val ts = 1_000L
        val a = createKnowledge().copy(createdAt = ts, updatedAt = ts)
        val b = createKnowledge().copy(createdAt = ts, updatedAt = ts)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
