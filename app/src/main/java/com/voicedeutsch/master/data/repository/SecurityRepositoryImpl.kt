package com.voicedeutsch.master.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.voicedeutsch.master.domain.repository.SecurityRepository

/**
 * Реализация [SecurityRepository] через EncryptedSharedPreferences.
 *
 * ⚠️ Использует security-crypto 1.1.0-alpha07 (нет стабильного релиза на февраль 2026).
 * Ключ зашифрован через AES256-GCM в Android Keystore.
 * При обновлении библиотеки до несовместимой версии существующие данные
 * могут стать нечитаемыми — см. примечание в libs.versions.toml.
 */
class SecurityRepositoryImpl(
    private val context: Context,
) : SecurityRepository {

    companion object {
        private const val PREFS_FILE = "voice_deutsch_secure_prefs"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getGeminiApiKey(): String =
        prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""

    override fun saveGeminiApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, apiKey.trim()).apply()
    }

    override fun clearGeminiApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
    }

    override fun hasGeminiApiKey(): Boolean =
        getGeminiApiKey().isNotBlank()
}