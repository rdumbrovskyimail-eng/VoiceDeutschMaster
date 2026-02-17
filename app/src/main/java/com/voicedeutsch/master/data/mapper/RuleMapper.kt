package com.voicedeutsch.master.data.mapper

import com.voicedeutsch.master.data.local.database.entity.GrammarRuleEntity
import com.voicedeutsch.master.domain.model.knowledge.GrammarCategory
import com.voicedeutsch.master.domain.model.knowledge.GrammarExample
import com.voicedeutsch.master.domain.model.knowledge.GrammarRule
import com.voicedeutsch.master.domain.model.user.CefrLevel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object RuleMapper {

    fun GrammarRuleEntity.toDomain(json: Json): GrammarRule {
        val examplesList = try {
            json.decodeFromString<List<GrammarExample>>(examplesJson)
        } catch (e: Exception) {
            emptyList()
        }
        val exceptionsList = try {
            json.decodeFromString<List<String>>(exceptionsJson)
        } catch (e: Exception) {
            emptyList()
        }
        val relatedIds = try {
            json.decodeFromString<List<String>>(relatedRulesJson)
        } catch (e: Exception) {
            emptyList()
        }
        return GrammarRule(
            id = id,
            nameRu = nameRu,
            nameDe = nameDe,
            category = try {
                GrammarCategory.valueOf(category.uppercase())
            } catch (e: Exception) {
                GrammarCategory.OTHER
            },
            descriptionRu = descriptionRu,
            descriptionDe = descriptionDe,
            difficultyLevel = CefrLevel.fromString(difficultyLevel),
            examples = examplesList,
            exceptions = exceptionsList,
            relatedRuleIds = relatedIds,
            bookChapter = bookChapter,
            bookLesson = bookLesson,
            createdAt = createdAt
        )
    }

    fun GrammarRule.toEntity(json: Json): GrammarRuleEntity = GrammarRuleEntity(
        id = id,
        nameRu = nameRu,
        nameDe = nameDe,
        category = category.name,
        descriptionRu = descriptionRu,
        descriptionDe = descriptionDe,
        difficultyLevel = difficultyLevel.name,
        examplesJson = json.encodeToString(examples),
        exceptionsJson = json.encodeToString(exceptions),
        relatedRulesJson = json.encodeToString(relatedRuleIds),
        bookChapter = bookChapter,
        bookLesson = bookLesson,
        createdAt = createdAt
    )
}