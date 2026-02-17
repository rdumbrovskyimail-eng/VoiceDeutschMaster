package com.voicedeutsch.master.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "native_language")
    val nativeLanguage: String = "ru",
    @ColumnInfo(name = "target_language")
    val targetLanguage: String = "de",
    @ColumnInfo(name = "cefr_level")
    val cefrLevel: String = "A1",
    @ColumnInfo(name = "cefr_sub_level")
    val cefrSubLevel: Int = 1,
    @ColumnInfo(name = "total_sessions")
    val totalSessions: Int = 0,
    @ColumnInfo(name = "total_minutes")
    val totalMinutes: Int = 0,
    @ColumnInfo(name = "total_words_learned")
    val totalWordsLearned: Int = 0,
    @ColumnInfo(name = "total_rules_learned")
    val totalRulesLearned: Int = 0,
    @ColumnInfo(name = "streak_days")
    val streakDays: Int = 0,
    @ColumnInfo(name = "last_session_date")
    val lastSessionDate: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "preferences_json")
    val preferencesJson: String = "{}",
    @ColumnInfo(name = "voice_settings_json")
    val voiceSettingsJson: String = "{}"
)