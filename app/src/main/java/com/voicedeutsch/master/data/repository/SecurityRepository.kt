package com.voicedeutsch.master.domain.repository

/**
 * Контракт для хранения чувствительных данных (API-ключи).
 * Реализация использует EncryptedSharedPreferences (security-crypto 1.1.0-alpha07).
 */
interface SecurityRepository {

    /** Возвращает сохранённый Gemini API-ключ, или пустую строку если не задан. */
    fun getGeminiApiKey(): String

    /** Сохраняет Gemini API-ключ в зашифрованное хранилище. */
    fun saveGeminiApiKey(apiKey: String)

    /** Удаляет API-ключ из хранилища. */
    fun clearGeminiApiKey()

    /** Возвращает true если ключ задан и не пустой. */
    fun hasGeminiApiKey(): Boolean
}