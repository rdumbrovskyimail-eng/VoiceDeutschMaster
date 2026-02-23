package com.voicedeutsch.master.data.remote.gemini

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Сервис получения Ephemeral Token для Gemini Live API.
 *
 * Production Standard 2026: API ключ не хранится в APK.
 * Android запрашивает временный токен (TTL ~30 мин) у Firebase Function.
 * Firebase Function хранит настоящий ключ в Secret Manager.
 *
 * Схема:
 *   Android → POST /getEphemeralToken → Firebase Function
 *   Firebase Function → Google API → ephemeral token
 *   Firebase Function → token → Android
 *   Android → WSS с token вместо API key
 */
class EphemeralTokenService(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    companion object {
        private const val TAG = "EphemeralTokenService"

        private const val FUNCTION_URL =
            "https://us-central1-voicedeutschmaster.cloudfunctions.net/getEphemeralToken"

        private const val REFRESH_BEFORE_EXPIRY_MS = 5 * 60 * 1000L
    }

    @Volatile private var cachedToken: CachedToken? = null

    /**
     * Возвращает валидный Ephemeral Token.
     * Использует кэш если токен ещё не истёк.
     * Автоматически обновляет токен за 5 минут до истечения.
     *
     * @param userId ID пользователя (для Firebase Auth в будущем)
     * @throws EphemeralTokenException если не удалось получить токен
     */
    suspend fun fetchToken(userId: String): String {
        cachedToken?.let { cached ->
            if (!cached.isExpiredSoon()) {
                Log.d(TAG, "Returning cached token, expires in ${cached.minutesLeft()} min")
                return cached.token
            }
        }

        return refreshToken(userId)
    }

    private suspend fun refreshToken(userId: String): String {
        Log.d(TAG, "Fetching new ephemeral token for user: $userId")

        try {
            val response = httpClient.post(FUNCTION_URL) {
                contentType(ContentType.Application.Json)
                setBody(
                    json.encodeToString(
                        kotlinx.serialization.json.JsonObject.serializer(),
                        buildJsonObject { put("userId", userId) }
                    )
                )
            }

            if (!response.status.isSuccess()) {
                val body = response.bodyAsText()
                Log.e(TAG, "Firebase Function error ${response.status}: $body")
                throw EphemeralTokenException("Server returned ${response.status}: $body")
            }

            val tokenResponse = response.body<EphemeralTokenResponse>()

            // ✅ FIX: НЕ стрипаем префикс "auth_tokens/".
            // Google Live API ожидает полный resource name токена в ?key=
            // формат: auth_tokens/TOKEN_VALUE
            // Стрипание префикса было причиной потенциального 403.
            val fullToken = tokenResponse.token
            Log.d(TAG, "Token resource name: ${fullToken.take(30)}...")

            cachedToken = CachedToken(
                token = fullToken,
                expiresAt = parseExpiresAt(tokenResponse.expiresAt),
            )

            Log.d(TAG, "New token received, expires at: ${tokenResponse.expiresAt}")
            return fullToken

        } catch (e: EphemeralTokenException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch ephemeral token", e)
            throw EphemeralTokenException("Network error: ${e.message}", e)
        }
    }

    /** Сбрасывает кэш — вызывается при выходе пользователя. */
    fun clearCache() {
        cachedToken = null
    }

    private fun parseExpiresAt(expiresAt: String?): Long {
        if (expiresAt == null) return System.currentTimeMillis() + 25 * 60 * 1000L
        return try {
            java.time.Instant.parse(expiresAt).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis() + 25 * 60 * 1000L
        }
    }

    // ── Data classes ──────────────────────────────────────────────────────────

    @Serializable
    private data class EphemeralTokenResponse(
        @SerialName("token") val token: String,
        @SerialName("expiresAt") val expiresAt: String? = null,
    )

    private data class CachedToken(
        val token: String,
        val expiresAt: Long,
    ) {
        fun isExpiredSoon(): Boolean =
            System.currentTimeMillis() >= expiresAt - REFRESH_BEFORE_EXPIRY_MS

        fun minutesLeft(): Long =
            (expiresAt - System.currentTimeMillis()) / 60_000
    }
}

class EphemeralTokenException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
