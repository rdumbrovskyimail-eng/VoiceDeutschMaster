package com.voicedeutsch.master.data.remote.gemini

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Сервис получения Ephemeral Token для Gemini Live API.
 *
 * Production Standard 2026: API ключ не хранится в APK.
 * Android → Firebase Anonymous Auth → ID Token
 * Android → POST /getEphemeralToken + Bearer ID Token → Firebase Function
 * Firebase Function → verifyIdToken → Google API → ephemeral token
 * Android → WSS с ephemeral token вместо API key
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

    private val firebaseAuth = FirebaseAuth.getInstance()

    @Volatile private var cachedToken: CachedToken? = null

    /**
     * Возвращает валидный Ephemeral Token.
     * Использует кэш если токен ещё не истёк.
     * Автоматически обновляет токен за 5 минут до истечения.
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

        // ✅ Получаем Firebase ID Token (анонимный вход если нужно)
        val firebaseIdToken = getFirebaseIdToken()

        try {
            val response = httpClient.post(FUNCTION_URL) {
                contentType(ContentType.Application.Json)
                // ✅ Firebase ID Token в заголовке Authorization
                header(HttpHeaders.Authorization, "Bearer $firebaseIdToken")
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

            // ✅ Полный resource name токена — НЕ стрипаем "auth_tokens/"
            val fullToken = tokenResponse.token
            Log.d(TAG, "Token received: ${fullToken.take(30)}...")

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

    /**
     * Получает Firebase ID Token.
     * Если пользователь не авторизован — анонимный вход автоматически.
     */
    private suspend fun getFirebaseIdToken(): String {
        val currentUser = firebaseAuth.currentUser
            ?: firebaseAuth.signInAnonymously().await().user
            ?: throw EphemeralTokenException("Firebase Auth: failed to sign in anonymously")

        return currentUser.getIdToken(false).await().token
            ?: throw EphemeralTokenException("Firebase Auth: failed to get ID token")
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
