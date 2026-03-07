// Путь: src/test/java/com/voicedeutsch/master/domain/model/knowledge/GrammarRuleTest.kt
package com.voicedeutsch.master.domain.model.knowledge

import com.voicedeutsch.master.domain.model.user.CefrLevel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GrammarRuleTest {

    private fun createRule(
        id: String = "rule_001",
        nameRu: String = "Падеж Датив",
        nameDe: String = "Dativ",
        category: GrammarCategory = GrammarCategory.CASES,
        descriptionRu: String = "Описание правила",
        descriptionDe: String = "",
        difficultyLevel: CefrLevel = CefrLevel.A2,
        examples: List<GrammarExample> = emptyList(),
        exceptions: List<String> = emptyList(),
        relatedRuleIds: List<String> = emptyList(),
        bookChapter: Int? = null,
        bookLesson: Int? = null,
    ) = GrammarRule(
        id = id,
        nameRu = nameRu,
        nameDe = nameDe,
        category = category,
        descriptionRu = descriptionRu,
        descriptionDe = descriptionDe,
        difficultyLevel = difficultyLevel,
        examples = examples,
        exceptions = exceptions,
        relatedRuleIds = relatedRuleIds,
        bookChapter = bookChapter,
        bookLesson = bookLesson,
    )

    // ── GrammarRule — creation ────────────────────────────────────────────

    @Test
    fun creation_requiredFields_storedCorrectly() {
        val rule = createRule(id = "r1", nameRu = "Артикль", nameDe = "Artikel")
        assertEquals("r1", rule.id)
        assertEquals("Артикль", rule.nameRu)
        assertEquals("Artikel", rule.nameDe)
    }

    @Test
    fun creation_defaultDescriptionDe_isEmpty() {
        val rule = createRule()
        assertEquals("", rule.descriptionDe)
    }

    @Test
    fun creation_defaultExamples_isEmpty() {
        assertTrue(createRule().examples.isEmpty())
    }

    @Test
    fun creation_defaultExceptions_isEmpty() {
        assertTrue(createRule().exceptions.isEmpty())
    }

    @Test
    fun creation_defaultRelatedRuleIds_isEmpty() {
        assertTrue(createRule().relatedRuleIds.isEmpty())
    }

    @Test
    fun creation_defaultBookChapter_isNull() {
        assertNull(createRule().bookChapter)
    }

    @Test
    fun creation_defaultBookLesson_isNull() {
        assertNull(createRule().bookLesson)
    }

    @Test
    fun creation_createdAt_isPositive() {
        assertTrue(createRule().createdAt > 0L)
    }

    @Test
    fun creation_withBookChapterAndLesson_storedCorrectly() {
        val rule = createRule(bookChapter = 3, bookLesson = 7)
        assertEquals(3, rule.bookChapter)
        assertEquals(7, rule.bookLesson)
    }

    @Test
    fun creation_withExamples_storedCorrectly() {
        val examples = listOf(
            GrammarExample("Ich helfe dem Mann.", "Я помогаю мужчине.", "Dativ"),
        )
        val rule = createRule(examples = examples)
        assertEquals(1, rule.examples.size)
        assertEquals("Ich helfe dem Mann.", rule.examples[0].german)
    }

    @Test
    fun creation_withExceptions_storedCorrectly() {
        val rule = createRule(exceptions = listOf("außer", "mit", "nach"))
        assertEquals(3, rule.exceptions.size)
        assertTrue(rule.exceptions.contains("mit"))
    }

    @Test
    fun creation_withRelatedRuleIds_storedCorrectly() {
        val rule = createRule(relatedRuleIds = listOf("rule_002", "rule_003"))
        assertEquals(2, rule.relatedRuleIds.size)
    }

    // ── GrammarRule — equals / hashCode / copy ────────────────────────────

    @Test
    fun equals_twoIdenticalRules_returnsTrue() {
        val ts = 1_000L
        val a = createRule().copy(createdAt = ts)
        val b = createRule().copy(createdAt = ts)
        assertEquals(a, b)
    }

    @Test
    fun equals_differentId_returnsFalse() {
        val a = createRule(id = "rule_a")
        val b = createRule(id = "rule_b")
        assertNotEquals(a, b)
    }

    @Test
    fun hashCode_equalRules_samHash() {
        val ts = 1_000L
        val a = createRule().copy(createdAt = ts)
        val b = createRule().copy(createdAt = ts)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun copy_changesOnlySpecifiedField() {
        val original = createRule(nameRu = "Оригинал")
        val copied = original.copy(nameRu = "Копия")
        assertEquals("Копия", copied.nameRu)
        assertEquals(original.id, copied.id)
        assertEquals(original.category, copied.category)
    }

    // ── GrammarExample — creation ─────────────────────────────────────────

    @Test
    fun example_requiredFields_storedCorrectly() {
        val ex = GrammarExample(german = "Das ist ein Hund.", russian = "Это собака.")
        assertEquals("Das ist ein Hund.", ex.german)
        assertEquals("Это собака.", ex.russian)
    }

    @Test
    fun example_defaultExplanation_isEmpty() {
        val ex = GrammarExample(german = "G", russian = "R")
        assertEquals("", ex.explanation)
    }

    @Test
    fun example_withExplanation_storedCorrectly() {
        val ex = GrammarExample("Er gibt dem Kind.", "Он даёт ребёнку.", "Dativ с geben")
        assertEquals("Dativ с geben", ex.explanation)
    }

    @Test
    fun example_equals_twoIdentical() {
        val a = GrammarExample("G", "R", "E")
        val b = GrammarExample("G", "R", "E")
        assertEquals(a, b)
    }

    @Test
    fun example_notEquals_differentGerman() {
        val a = GrammarExample("A", "R")
        val b = GrammarExample("B", "R")
        assertNotEquals(a, b)
    }

    @Test
    fun example_copy_changesOnlySpecifiedField() {
        val original = GrammarExample("G", "R", "E")
        val copied = original.copy(russian = "New")
        assertEquals("G", copied.german)
        assertEquals("New", copied.russian)
        assertEquals("E", copied.explanation)
    }

    // ── GrammarCategory ───────────────────────────────────────────────────

    @Test
    fun grammarCategory_entryCount_equals13() {
        assertEquals(13, GrammarCategory.entries.size)
    }

    @Test
    fun grammarCategory_containsAllExpectedValues() {
        val expected = setOf(
            GrammarCategory.ARTICLES,
            GrammarCategory.CASES,
            GrammarCategory.VERBS,
            GrammarCategory.WORD_ORDER,
            GrammarCategory.PRONOUNS,
            GrammarCategory.ADJECTIVES,
            GrammarCategory.PREPOSITIONS,
            GrammarCategory.CONJUNCTIONS,
            GrammarCategory.NEGATION,
            GrammarCategory.PASSIVE,
            GrammarCategory.SUBJUNCTIVE,
            GrammarCategory.RELATIVE_CLAUSES,
            GrammarCategory.OTHER,
        )
        assertEquals(expected, GrammarCategory.entries.toSet())
    }

    @Test
    fun grammarCategory_valueOf_articles() {
        assertEquals(GrammarCategory.ARTICLES, GrammarCategory.valueOf("ARTICLES"))
    }

    @Test
    fun grammarCategory_valueOf_relativeClauses() {
        assertEquals(GrammarCategory.RELATIVE_CLAUSES, GrammarCategory.valueOf("RELATIVE_CLAUSES"))
    }

    @Test
    fun grammarCategory_valueOf_other() {
        assertEquals(GrammarCategory.OTHER, GrammarCategory.valueOf("OTHER"))
    }

    @Test
    fun grammarCategory_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            GrammarCategory.valueOf("UNKNOWN")
        }
    }

    @Test
    fun grammarCategory_ordinalsAreUnique() {
        val ordinals = GrammarCategory.entries.map { it.ordinal }
        assertEquals(ordinals.size, ordinals.toSet().size)
    }

    // ── GrammarRule — difficultyLevel ─────────────────────────────────────

    @Test
    fun rule_difficultyLevel_a1_storedCorrectly() {
        val rule = createRule(difficultyLevel = CefrLevel.A1)
        assertEquals(CefrLevel.A1, rule.difficultyLevel)
    }

    @Test
    fun rule_difficultyLevel_c2_storedCorrectly() {
        val rule = createRule(difficultyLevel = CefrLevel.C2)
        assertEquals(CefrLevel.C2, rule.difficultyLevel)
    }
}
