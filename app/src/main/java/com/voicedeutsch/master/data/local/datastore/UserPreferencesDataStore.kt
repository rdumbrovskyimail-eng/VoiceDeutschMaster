package com.voicedeutsch.master.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.voicedeutsch.master.voicecore.engine.GeminiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesDataStore(private val context: Context) {

    companion object {
        val ACTIVE_USER_ID = stringPreferencesKey("active_user_id")

        val THEME = stringPreferencesKey("theme")
        val IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")
        val IS_BOOK_LOADED = booleanPreferencesKey("is_book_loaded")
        val SESSION_DURATION = intPreferencesKey("session_duration")
        val DAILY_GOAL = intPreferencesKey("daily_goal")

        // Gemini config keys
        val GEMINI_TEMPERATURE = floatPreferencesKey("gemini_temperature")
        val GEMINI_TOP_P = floatPreferencesKey("gemini_top_p")
        val GEMINI_TOP_K = intPreferencesKey("gemini_top_k")
        val GEMINI_VOICE_NAME = stringPreferencesKey("gemini_voice_name")
        val GEMINI_MODEL_NAME = stringPreferencesKey("gemini_model_name")
        val GEMINI_MAX_CONTEXT_TOKENS = intPreferencesKey("gemini_max_context_tokens")
        val GEMINI_INPUT_TRANSCRIPTION = booleanPreferencesKey("gemini_input_transcription")
        val GEMINI_OUTPUT_TRANSCRIPTION = booleanPreferencesKey("gemini_output_transcription")
    }

    suspend fun getActiveUserId(): String? =
        context.dataStore.data.first()[ACTIVE_USER_ID]

    suspend fun setActiveUserId(userId: String) {
        context.dataStore.edit { it[ACTIVE_USER_ID] = userId }
    }


    fun getThemeFlow(): Flow<String> =
        context.dataStore.data.map { it[THEME] ?: "system" }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[THEME] = theme }
    }

    suspend fun isOnboardingComplete(): Boolean =
        context.dataStore.data.first()[IS_ONBOARDING_COMPLETE] ?: false

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[IS_ONBOARDING_COMPLETE] = complete }
    }

    suspend fun isBookLoaded(): Boolean =
        context.dataStore.data.first()[IS_BOOK_LOADED] ?: false

    suspend fun setBookLoaded(loaded: Boolean) {
        context.dataStore.edit { it[IS_BOOK_LOADED] = loaded }
    }

    suspend fun getSessionDuration(): Int? =
        context.dataStore.data.first()[SESSION_DURATION]

    suspend fun setSessionDuration(minutes: Int) {
        context.dataStore.edit { it[SESSION_DURATION] = minutes }
    }

    suspend fun getDailyGoal(): Int? =
        context.dataStore.data.first()[DAILY_GOAL]

    suspend fun setDailyGoal(words: Int) {
        context.dataStore.edit { it[DAILY_GOAL] = words }
    }

    // ── Gemini Config ─────────────────────────────────────────────────────────

    suspend fun loadGeminiConfig(): GeminiConfig {
        val prefs = context.dataStore.data.first()
        return GeminiConfig(
            modelName = prefs[GEMINI_MODEL_NAME] ?: GeminiConfig.MODEL_GEMINI_LIVE,
            temperature = prefs[GEMINI_TEMPERATURE] ?: GeminiConfig.DEFAULT_TEMPERATURE,
            topP = prefs[GEMINI_TOP_P] ?: GeminiConfig.DEFAULT_TOP_P,
            topK = prefs[GEMINI_TOP_K] ?: GeminiConfig.DEFAULT_TOP_K,
            voiceName = prefs[GEMINI_VOICE_NAME] ?: GeminiConfig.DEFAULT_VOICE,
            maxContextTokens = prefs[GEMINI_MAX_CONTEXT_TOKENS] ?: GeminiConfig.MAX_CONTEXT_TOKENS,
            transcriptionConfig = GeminiConfig.TranscriptionConfig(
                inputTranscriptionEnabled = prefs[GEMINI_INPUT_TRANSCRIPTION] ?: false,
                outputTranscriptionEnabled = prefs[GEMINI_OUTPUT_TRANSCRIPTION] ?: false,
            ),
        )
    }

    suspend fun saveGeminiConfig(config: GeminiConfig) {
        context.dataStore.edit { prefs ->
            prefs[GEMINI_TEMPERATURE] = config.temperature
            prefs[GEMINI_TOP_P] = config.topP
            prefs[GEMINI_TOP_K] = config.topK
            prefs[GEMINI_VOICE_NAME] = config.voiceName
            prefs[GEMINI_MODEL_NAME] = config.modelName
            prefs[GEMINI_MAX_CONTEXT_TOKENS] = config.maxContextTokens
            prefs[GEMINI_INPUT_TRANSCRIPTION] = config.transcriptionConfig.inputTranscriptionEnabled
            prefs[GEMINI_OUTPUT_TRANSCRIPTION] = config.transcriptionConfig.outputTranscriptionEnabled
        }
    }

    fun geminiConfigFlow(): Flow<GeminiConfig> = context.dataStore.data.map { prefs ->
        GeminiConfig(
            modelName = prefs[GEMINI_MODEL_NAME] ?: GeminiConfig.MODEL_GEMINI_LIVE,
            temperature = prefs[GEMINI_TEMPERATURE] ?: GeminiConfig.DEFAULT_TEMPERATURE,
            topP = prefs[GEMINI_TOP_P] ?: GeminiConfig.DEFAULT_TOP_P,
            topK = prefs[GEMINI_TOP_K] ?: GeminiConfig.DEFAULT_TOP_K,
            voiceName = prefs[GEMINI_VOICE_NAME] ?: GeminiConfig.DEFAULT_VOICE,
            maxContextTokens = prefs[GEMINI_MAX_CONTEXT_TOKENS] ?: GeminiConfig.MAX_CONTEXT_TOKENS,
            transcriptionConfig = GeminiConfig.TranscriptionConfig(
                inputTranscriptionEnabled = prefs[GEMINI_INPUT_TRANSCRIPTION] ?: false,
                outputTranscriptionEnabled = prefs[GEMINI_OUTPUT_TRANSCRIPTION] ?: false,
            ),
        )
    }
}