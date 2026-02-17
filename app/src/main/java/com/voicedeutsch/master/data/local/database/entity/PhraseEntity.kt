package com.voicedeutsch.master.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phrases")
data class PhraseEntity(
    @PrimaryKey
    val id: String,
    val german: String,
    val russian: String,
    val category: String,
    @ColumnInfo(name = "difficulty_level")
    val difficultyLevel: String = "A1",
    @ColumnInfo(name = "book_chapter")
    val bookChapter: Int? = null,
    @ColumnInfo(name = "book_lesson")
    val bookLesson: Int? = null,
    val context: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)