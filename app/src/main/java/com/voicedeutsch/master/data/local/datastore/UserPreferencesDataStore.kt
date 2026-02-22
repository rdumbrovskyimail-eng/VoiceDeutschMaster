package com.voicedeutsch.master.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
        val SESSION_DURATION = androidx.datastore.preferences.core.intPreferencesKey("session_duration")
        val DAILY_GOAL = androidx.datastore.preferences.core.intPreferencesKey("daily_goal")
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
}