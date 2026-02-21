package com.voicedeutsch.master.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "name_ru")
    val nameRu: String,
    @ColumnInfo(name = "name_de")
    val nameDe: String,
    @ColumnInfo(name = "description_ru")
    val descriptionRu: String,
    val icon: String,
    @ColumnInfo(name = "condition_json")
    val conditionJson: String,
    val category: String, // vocabulary / grammar / pronunciation / streak / session / book
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)