package com.voicedeutsch.master.domain.model.knowledge

import com.voicedeutsch.master.domain.model.user.CefrLevel
import kotlinx.serialization.Serializable

/**
 * Domain model of a German grammar rule.
 *
 * Contains names in both languages, a category, descriptions, difficulty level,
 * examples with explanations, exceptions, and links to related rules.
 *
 * **Mappings:**
 * - Maps from/to `GrammarRuleEntity`
 * - Linked to [RuleKnowledge]
 * - Parsed from `grammar.json`
 * - Used in the `GRAMMAR_DRILL` strategy
 */
@Serializable
data class GrammarRule(
    val id: String,
    val nameRu: String,
    val nameDe: String,
    val category: GrammarCategory,
    val descriptionRu: String,
    val descriptionDe: String = "",
    val difficultyLevel: CefrLevel,
    val examples: List<GrammarExample> = emptyList(),
    val exceptions: List<String> = emptyList(),
    val relatedRuleIds: List<String> = emptyList(),
    val bookChapter: Int? = null,
    val bookLesson: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * An example illustrating a grammar rule.
 */
@Serializable
data class GrammarExample(
    val german: String,
    val russian: String,
    val explanation: String = ""
)

/**
 * Categories of German grammar rules.
 */
@Serializable
enum class GrammarCategory {
    ARTICLES,          // Артикли
    CASES,             // Падежи
    VERBS,             // Глаголы (времена, модальные, etc.)
    WORD_ORDER,        // Порядок слов
    PRONOUNS,          // Местоимения
    ADJECTIVES,        // Прилагательные (склонение)
    PREPOSITIONS,      // Предлоги
    CONJUNCTIONS,      // Союзы
    NEGATION,          // Отрицание
    PASSIVE,           // Пассивный залог
    SUBJUNCTIVE,       // Сослагательное наклонение
    RELATIVE_CLAUSES,  // Относительные предложения
    OTHER              // Прочее
}