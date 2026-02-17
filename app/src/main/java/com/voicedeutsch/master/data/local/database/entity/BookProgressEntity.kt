package com.voicedeutsch.master.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "book_progress",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id", "chapter", "lesson"], unique = true)
    ]
)
data class BookProgressEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val chapter: Int,
    val lesson: Int,
    val status: String = "NOT_STARTED",
    val score: Float? = null,
    @ColumnInfo(name = "started_at")
    val startedAt: Long? = null,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    @ColumnInfo(name = "times_practiced")
    val timesPracticed: Int = 0,
    val notes: String? = null
)