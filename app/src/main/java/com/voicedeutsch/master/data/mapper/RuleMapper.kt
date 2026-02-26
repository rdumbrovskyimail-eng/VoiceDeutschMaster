package com.voicedeutsch.master.data.mapper

import com.voicedeutsch.master.data.local.database.entity.GrammarRuleEntity
import com.voicedeutsch.master.domain.model.knowledge.GrammarCategory
import com.voicedeutsch.master.domain.model.knowledge.GrammarExample
import com.voicedeutsch.master.domain.model.knowledge.GrammarRule
import com.voicedeutsch.master.domain.model.user.CefrLevel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object RuleMapper {

    // FIX: безопасная десериализация списка.
    // Если строка пустая ("") или невалидный JSON — возвращаем emptyList()
    // вместо SerializationException.
    private inline fun <reified T> Json.safeDecodeList(raw: String): List<T> =
        if (raw.isBlank()) emptyList()
        else runCatching { decodeFromString<List<T>>(raw) }.getOrDefault(emptyList())

    fun GrammarRuleEntity.toDomain(json: Json): GrammarRule = GrammarRule(
        id              = id,
        nameRu          = nameRu,
        nameDe          = nameDe,
        category        = runCatching {
            GrammarCategory.valueOf(category.uppercase())
        }.getOrDefault(GrammarCategory.OTHER),
        descriptionRu   = descriptionRu,
        descriptionDe   = descriptionDe,
        difficultyLevel = CefrLevel.fromString(difficultyLevel),
        examples        = json.safeDecodeList<GrammarExample>(examplesJson),
        exceptions      = json.safeDecodeList<String>(exceptionsJson),
        relatedRuleIds  = json.safeDecodeList<String>(relatedRulesJson),
        bookChapter     = bookChapter,
        bookLesson      = bookLesson,
        createdAt       = createdAt,
    )

    fun GrammarRule.toEntity(json: Json): GrammarRuleEntity = GrammarRuleEntity(
        id              = id,
        nameRu          = nameRu,
        nameDe          = nameDe,
        category        = category.name,
        descriptionRu   = descriptionRu,
        descriptionDe   = descriptionDe,
        difficultyLevel = difficultyLevel.name,
        examplesJson    = json.encodeToString(examples),
        exceptionsJson  = json.encodeToString(exceptions),
        relatedRulesJson = json.encodeToString(relatedRuleIds),
        bookChapter     = bookChapter,
        bookLesson      = bookLesson,
        createdAt       = createdAt,
    )
}
