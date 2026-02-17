package com.voicedeutsch.master.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grammar_rules")
data class GrammarRuleEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "name_ru")
    val nameRu: String,
    @ColumnInfo(name = "name_de")
    val nameDe: String,
    val category: String,
    @ColumnInfo(name = "description_ru")
    val descriptionRu: String,
    @ColumnInfo(name = "description_de")
    val descriptionDe: String = "",
    @ColumnInfo(name = "difficulty_level")
    val difficultyLevel: String = "A1",
    @ColumnInfo(name = "examples_json")
    val examplesJson: String = "[]",
    @ColumnInfo(name = "exceptions_json")
    val exceptionsJson: String = "[]",
    @ColumnInfo(name = "related_rules_json")
    val relatedRulesJson: String = "[]",
    @ColumnInfo(name = "book_chapter")
    val bookChapter: Int? = null,
    @ColumnInfo(name = "book_lesson")
    val bookLesson: Int? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)