// Путь: src/test/java/com/voicedeutsch/master/data/mapper/RuleMapperTest.kt
package com.voicedeutsch.master.data.mapper

import com.voicedeutsch.master.data.local.database.entity.GrammarRuleEntity
import com.voicedeutsch.master.domain.model.knowledge.GrammarCategory
import com.voicedeutsch.master.domain.model.knowledge.GrammarExample
import com.voicedeutsch.master.domain.model.knowledge.GrammarRule
import com.voicedeutsch.master.domain.model.user.CefrLevel
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RuleMapperTest {

    private lateinit var json: Json

    @BeforeEach
    fun setUp() {
        json = Json { ignoreUnknownKeys = true }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildGrammarRuleEntity(
        id: String = "rule_1",
        nameRu: String = "Артикли",
        nameDe: String = "Artikel",
        category: String = "ARTICLES",
        descriptionRu: String = "Описание артиклей",
        descriptionDe: String = "Beschreibung der Artikel",
        difficultyLevel: String = "A1",
        examplesJson: String = """[{"german":"Der Hund","russian":"Собака","explanation":"Мужской род"}]""",
        exceptionsJson: String = """["исключение1","исключение2"]""",
        relatedRulesJson: String = """["rule_2","rule_3"]""",
        bookChapter: Int? = 1,
        bookLesson: Int? = 2,
        createdAt: Long = 1000L,
    ) = GrammarRuleEntity(
        id = id,
        nameRu = nameRu,
        nameDe = nameDe,
        category = category,
        descriptionRu = descriptionRu,
        descriptionDe = descriptionDe,
        difficultyLevel = difficultyLevel,
        examplesJson = examplesJson,
        exceptionsJson = exceptionsJson,
        relatedRulesJson = relatedRulesJson,
        bookChapter = bookChapter,
        bookLesson = bookLesson,
        createdAt = createdAt,
    )

    private fun buildGrammarRule(
        id: String = "rule_1",
        nameRu: String = "Артикли",
        nameDe: String = "Artikel",
        category: GrammarCategory = GrammarCategory.ARTICLES,
        descriptionRu: String = "Описание артиклей",
        descriptionDe: String = "Beschreibung der Artikel",
        difficultyLevel: CefrLevel = CefrLevel.A1,
        examples: List<GrammarExample> = listOf(
            GrammarExample(german = "Der Hund", russian = "Собака", explanation = "Мужской род")
        ),
        exceptions: List<String> = listOf("исключение1", "исключение2"),
        relatedRuleIds: List<String> = listOf("rule_2", "rule_3"),
        bookChapter: Int? = 1,
        bookLesson: Int? = 2,
        createdAt: Long = 1000L,
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
        createdAt = createdAt,
    )

    // ── GrammarRuleEntity.toDomain ────────────────────────────────────────────

    @Test
    fun grammarRuleEntity_toDomain_validData_mapsAllFields() {
        val entity = buildGrammarRuleEntity()
        with(RuleMapper) {
            val domain = entity.toDomain(json)
            assertEquals(entity.id, domain.id)
            assertEquals(entity.nameRu, domain.nameRu)
            assertEquals(entity.nameDe, domain.nameDe)
            assertEquals(GrammarCategory.ARTICLES, domain.category)
            assertEquals(entity.descriptionRu, domain.descriptionRu)
            assertEquals(entity.descriptionDe, domain.descriptionDe)
            assertEquals(entity.bookChapter, domain.bookChapter)
            assertEquals(entity.bookLesson, domain.bookLesson)
            assertEquals(entity.createdAt, domain.createdAt)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_validCategory_mapsCorrectly() {
        val entity = buildGrammarRuleEntity(category = "ARTICLES")
        with(RuleMapper) {
            assertEquals(GrammarCategory.ARTICLES, entity.toDomain(json).category)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_categoryLowercased_parsedCaseInsensitive() {
        val entity = buildGrammarRuleEntity(category = "articles")
        with(RuleMapper) {
            assertEquals(GrammarCategory.ARTICLES, entity.toDomain(json).category)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_categoryMixedCase_parsedCaseInsensitive() {
        val entity = buildGrammarRuleEntity(category = "Articles")
        with(RuleMapper) {
            assertEquals(GrammarCategory.ARTICLES, entity.toDomain(json).category)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_invalidCategory_fallsBackToOther() {
        val entity = buildGrammarRuleEntity(category = "TOTALLY_UNKNOWN_CATEGORY")
        with(RuleMapper) {
            assertEquals(GrammarCategory.OTHER, entity.toDomain(json).category)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_emptyCategory_fallsBackToOther() {
        val entity = buildGrammarRuleEntity(category = "")
        with(RuleMapper) {
            assertEquals(GrammarCategory.OTHER, entity.toDomain(json).category)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_allGrammarCategories_mappedCorrectly() {
        GrammarCategory.entries.filter { it != GrammarCategory.OTHER }.forEach { expected ->
            val entity = buildGrammarRuleEntity(category = expected.name)
            with(RuleMapper) {
                assertEquals(expected, entity.toDomain(json).category)
            }
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_validDifficultyLevel_mapsCorrectly() {
        val entity = buildGrammarRuleEntity(difficultyLevel = "B2")
        with(RuleMapper) {
            assertEquals(CefrLevel.B2, entity.toDomain(json).difficultyLevel)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_validExamplesJson_parsedToList() {
        val entity = buildGrammarRuleEntity(
            examplesJson = """[{"german":"Der Hund","translation":"Собака","explanation":"Мужской род"}]"""
        )
        with(RuleMapper) {
            val domain = entity.toDomain(json)
            assertEquals(1, domain.examples.size)
            assertEquals("Der Hund", domain.examples[0].german)
            assertEquals("Собака", domain.examples[0].russian)
            assertEquals("Мужской род", domain.examples[0].explanation)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_emptyExamplesJson_returnsEmptyList() {
        val entity = buildGrammarRuleEntity(examplesJson = "")
        with(RuleMapper) {
            assertEquals(emptyList<GrammarExample>(), entity.toDomain(json).examples)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_blankExamplesJson_returnsEmptyList() {
        val entity = buildGrammarRuleEntity(examplesJson = "   ")
        with(RuleMapper) {
            assertEquals(emptyList<GrammarExample>(), entity.toDomain(json).examples)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_invalidExamplesJson_returnsEmptyList() {
        val entity = buildGrammarRuleEntity(examplesJson = "{not_valid_json}")
        with(RuleMapper) {
            assertEquals(emptyList<GrammarExample>(), entity.toDomain(json).examples)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_emptyArrayExamplesJson_returnsEmptyList() {
        val entity = buildGrammarRuleEntity(examplesJson = "[]")
        with(RuleMapper) {
            assertEquals(emptyList<GrammarExample>(), entity.toDomain(json).examples)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_validExceptionsJson_parsedToList() {
        val entity = buildGrammarRuleEntity(exceptionsJson = """["исключение1","исключение2"]""")
        with(RuleMapper) {
            val domain = entity.toDomain(json)
            assertEquals(listOf("исключение1", "исключение2"), domain.exceptions)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_emptyExceptionsJson_returnsEmptyList() {
        val entity = buildGrammarRuleEntity(exceptionsJson = "")
        with(RuleMapper) {
            assertEquals(emptyList<String>(), entity.toDomain(json).exceptions)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_invalidExceptionsJson_returnsEmptyList() {
        val entity = buildGrammarRuleEntity(exceptionsJson = "!!!bad!!!")
        with(RuleMapper) {
            assertEquals(emptyList<String>(), entity.toDomain(json).exceptions)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_validRelatedRulesJson_parsedToList() {
        val entity = buildGrammarRuleEntity(relatedRulesJson = """["rule_2","rule_3"]""")
        with(RuleMapper) {
            val domain = entity.toDomain(json)
            assertEquals(listOf("rule_2", "rule_3"), domain.relatedRuleIds)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_emptyRelatedRulesJson_returnsEmptyList() {
        val entity = buildGrammarRuleEntity(relatedRulesJson = "")
        with(RuleMapper) {
            assertEquals(emptyList<String>(), entity.toDomain(json).relatedRuleIds)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_invalidRelatedRulesJson_returnsEmptyList() {
        val entity = buildGrammarRuleEntity(relatedRulesJson = "not_json_at_all")
        with(RuleMapper) {
            assertEquals(emptyList<String>(), entity.toDomain(json).relatedRuleIds)
        }
    }

    @Test
    fun grammarRuleEntity_toDomain_nullBookChapterAndLesson_preservedAsNull() {
        val entity = buildGrammarRuleEntity(bookChapter = null, bookLesson = null)
        with(RuleMapper) {
            val domain = entity.toDomain(json)
            assertNull(domain.bookChapter)
            assertNull(domain.bookLesson)
        }
    }

    // ── GrammarRule.toEntity ──────────────────────────────────────────────────

    @Test
    fun grammarRule_toEntity_validData_mapsAllFields() {
        val domain = buildGrammarRule()
        with(RuleMapper) {
            val entity = domain.toEntity(json)
            assertEquals(domain.id, entity.id)
            assertEquals(domain.nameRu, entity.nameRu)
            assertEquals(domain.nameDe, entity.nameDe)
            assertEquals(domain.category.name, entity.category)
            assertEquals(domain.descriptionRu, entity.descriptionRu)
            assertEquals(domain.descriptionDe, entity.descriptionDe)
            assertEquals(domain.difficultyLevel.name, entity.difficultyLevel)
            assertEquals(domain.bookChapter, entity.bookChapter)
            assertEquals(domain.bookLesson, entity.bookLesson)
            assertEquals(domain.createdAt, entity.createdAt)
            assertNotNull(entity.examplesJson)
            assertNotNull(entity.exceptionsJson)
            assertNotNull(entity.relatedRulesJson)
        }
    }

    @Test
    fun grammarRule_toEntity_categoryStoredAsEnumName() {
        GrammarCategory.entries.forEach { category ->
            val domain = buildGrammarRule(category = category)
            with(RuleMapper) {
                assertEquals(category.name, domain.toEntity(json).category)
            }
        }
    }

    @Test
    fun grammarRule_toEntity_difficultyStoredAsEnumName() {
        CefrLevel.entries.forEach { level ->
            val domain = buildGrammarRule(difficultyLevel = level)
            with(RuleMapper) {
                assertEquals(level.name, domain.toEntity(json).difficultyLevel)
            }
        }
    }

    @Test
    fun grammarRule_toEntity_emptyExamples_producesEmptyJsonArray() {
        val domain = buildGrammarRule(examples = emptyList())
        with(RuleMapper) {
            assertEquals("[]", domain.toEntity(json).examplesJson)
        }
    }

    @Test
    fun grammarRule_toEntity_emptyExceptions_producesEmptyJsonArray() {
        val domain = buildGrammarRule(exceptions = emptyList())
        with(RuleMapper) {
            assertEquals("[]", domain.toEntity(json).exceptionsJson)
        }
    }

    @Test
    fun grammarRule_toEntity_emptyRelatedRuleIds_producesEmptyJsonArray() {
        val domain = buildGrammarRule(relatedRuleIds = emptyList())
        with(RuleMapper) {
            assertEquals("[]", domain.toEntity(json).relatedRulesJson)
        }
    }

    @Test
    fun grammarRule_toEntity_nullBookChapterAndLesson_preservedAsNull() {
        val domain = buildGrammarRule(bookChapter = null, bookLesson = null)
        with(RuleMapper) {
            val entity = domain.toEntity(json)
            assertNull(entity.bookChapter)
            assertNull(entity.bookLesson)
        }
    }

    // ── GrammarRule roundtrip ─────────────────────────────────────────────────

    @Test
    fun grammarRule_roundtrip_entityToDomainToEntity_scalarFieldsMatch() {
        val original = buildGrammarRuleEntity()
        with(RuleMapper) {
            val domain = original.toDomain(json)
            val restored = domain.toEntity(json)
            assertEquals(original.id, restored.id)
            assertEquals(original.nameRu, restored.nameRu)
            assertEquals(original.nameDe, restored.nameDe)
            assertEquals(original.category, restored.category)
            assertEquals(original.descriptionRu, restored.descriptionRu)
            assertEquals(original.descriptionDe, restored.descriptionDe)
            assertEquals(original.difficultyLevel, restored.difficultyLevel)
            assertEquals(original.bookChapter, restored.bookChapter)
            assertEquals(original.bookLesson, restored.bookLesson)
            assertEquals(original.createdAt, restored.createdAt)
        }
    }

    @Test
    fun grammarRule_roundtrip_exceptionsPreserved() {
        val original = buildGrammarRuleEntity(exceptionsJson = """["e1","e2","e3"]""")
        with(RuleMapper) {
            val domain = original.toDomain(json)
            assertEquals(listOf("e1", "e2", "e3"), domain.exceptions)
            val restored = domain.toEntity(json)
            val restoredDomain = restored.toDomain(json)
            assertEquals(domain.exceptions, restoredDomain.exceptions)
        }
    }

    @Test
    fun grammarRule_roundtrip_relatedRuleIdsPreserved() {
        val original = buildGrammarRuleEntity(relatedRulesJson = """["r1","r2"]""")
        with(RuleMapper) {
            val domain = original.toDomain(json)
            assertEquals(listOf("r1", "r2"), domain.relatedRuleIds)
            val restored = domain.toEntity(json)
            val restoredDomain = restored.toDomain(json)
            assertEquals(domain.relatedRuleIds, restoredDomain.relatedRuleIds)
        }
    }

    @Test
    fun grammarRule_roundtrip_invalidCategoryRestoredAsOther() {
        val original = buildGrammarRuleEntity(category = "GARBAGE")
        with(RuleMapper) {
            val domain = original.toDomain(json)
            assertEquals(GrammarCategory.OTHER, domain.category)
            val restored = domain.toEntity(json)
            assertEquals(GrammarCategory.OTHER.name, restored.category)
        }
    }

    @Test
    fun grammarRule_roundtrip_emptyJsonFields_restoredAsEmptyLists() {
        val original = buildGrammarRuleEntity(
            examplesJson = "",
            exceptionsJson = "",
            relatedRulesJson = "",
        )
        with(RuleMapper) {
            val domain = original.toDomain(json)
            assertEquals(emptyList<GrammarExample>(), domain.examples)
            assertEquals(emptyList<String>(), domain.exceptions)
            assertEquals(emptyList<String>(), domain.relatedRuleIds)
            val restored = domain.toEntity(json)
            assertEquals("[]", restored.examplesJson)
            assertEquals("[]", restored.exceptionsJson)
            assertEquals("[]", restored.relatedRulesJson)
        }
    }

    @Test
    fun grammarRule_roundtrip_nullBookFields_preservedAsNull() {
        val original = buildGrammarRuleEntity(bookChapter = null, bookLesson = null)
        with(RuleMapper) {
            val domain = original.toDomain(json)
            assertNull(domain.bookChapter)
            assertNull(domain.bookLesson)
            val restored = domain.toEntity(json)
            assertNull(restored.bookChapter)
            assertNull(restored.bookLesson)
        }
    }
}
