package com.voicedeutsch.master.data.mapper

import com.voicedeutsch.master.data.local.database.entity.UserEntity
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.domain.model.user.UserPreferences
import com.voicedeutsch.master.domain.model.user.UserProfile
import com.voicedeutsch.master.domain.model.user.VoiceSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object UserMapper {

    fun UserEntity.toDomain(json: Json): UserProfile {
        val preferences = try {
            json.decodeFromString<UserPreferences>(preferencesJson)
        } catch (e: Exception) {
            UserPreferences()
        }
        val voiceSettings = try {
            json.decodeFromString<VoiceSettings>(voiceSettingsJson)
        } catch (e: Exception) {
            VoiceSettings()
        }
        return UserProfile(
            id = id,
            name = name,
            nativeLanguage = nativeLanguage,
            targetLanguage = targetLanguage,
            cefrLevel = CefrLevel.fromString(cefrLevel),
            cefrSubLevel = cefrSubLevel,
            totalSessions = totalSessions,
            totalMinutes = totalMinutes,
            totalWordsLearned = totalWordsLearned,
            totalRulesLearned = totalRulesLearned,
            streakDays = streakDays,
            lastSessionDate = lastSessionDate,
            createdAt = createdAt,
            updatedAt = updatedAt,
            preferences = preferences,
            voiceSettings = voiceSettings
        )
    }

    fun UserProfile.toEntity(json: Json): UserEntity {
        return UserEntity(
            id = id,
            name = name,
            nativeLanguage = nativeLanguage,
            targetLanguage = targetLanguage,
            cefrLevel = cefrLevel.name,
            cefrSubLevel = cefrSubLevel,
            totalSessions = totalSessions,
            totalMinutes = totalMinutes,
            totalWordsLearned = totalWordsLearned,
            totalRulesLearned = totalRulesLearned,
            streakDays = streakDays,
            lastSessionDate = lastSessionDate,
            createdAt = createdAt,
            updatedAt = updatedAt,
            preferencesJson = json.encodeToString(preferences),
            voiceSettingsJson = json.encodeToString(voiceSettings)
        )
    }
}