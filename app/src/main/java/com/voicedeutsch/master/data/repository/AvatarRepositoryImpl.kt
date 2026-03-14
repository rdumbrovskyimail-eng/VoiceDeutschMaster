package com.voicedeutsch.master.data.repository

import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.voicecore.engine.AvatarGender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Determines avatar gender from voice settings.
 * Default voice "Kore" → FEMALE, "Puck"/"Charon"/"Fenrir" → MALE.
 */
class AvatarRepository(
    private val preferencesDataStore: UserPreferencesDataStore,
) {
    private val femaleVoices = setOf("Kore", "Zephyr")

    suspend fun getCurrentGender(): AvatarGender {
        val config = preferencesDataStore.loadGeminiConfig()
        return if (config.voiceName in femaleVoices) AvatarGender.FEMALE else AvatarGender.MALE
    }

    fun observeGenderChanges(): Flow<AvatarGender> = flow {
        preferencesDataStore.geminiConfigFlow().collect { config ->
            emit(if (config.voiceName in femaleVoices) AvatarGender.FEMALE else AvatarGender.MALE)
        }
    }
}