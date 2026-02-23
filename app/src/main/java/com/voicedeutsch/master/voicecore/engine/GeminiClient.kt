package com.voicedeutsch.master.voicecore.engine

import android.util.Base64
import android.util.Log
import com.voicedeutsch.master.voicecore.context.ContextBuilder
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * GeminiClient — низкоуровневый WebSocket-клиент для Gemini Live API.
 *
 * WebSocket endpoint (v1beta, актуален на февраль 2026):
 * wss://generativelanguage.googleapis.com/ws/
 *     google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent
 *
 * Протокол (официальная спецификация ai.google.dev/api/live):
 *   1. Connect → отправить BidiGenerateContentSetup (первое и единственное)
 *   2. Дождаться setupComplete от сервера
 *   3. Стримить аудио через realtimeInput.audio (Base64 PCM)
 *   4. Получать serverContent (audio + transcript) и toolCall (function calls)
 *   5. Отвечать на toolCall через toolResponse
 *
 * Модель: gemini-2.5-flash-live (gemini-2.0-flash-live-001 retire 31.03.2026)
 *
 * Thread-safety:
 *   - [connect] и [disconnect] должны вызываться последовательно (под lifecycleMutex в Engine)
 *   - [sendAudioChunk], [sendText], [sendFunctionResult] thread-safe через Channel
 *   - [receiveNextResponse] — suspend, безопасен для вызова из одной корутины (sessionJob)
 */
class GeminiClient(
    private val config: GeminiConfig,
    private val httpClient: HttpClient,
    private val json: Json,
) {
    companion object {
        private const val TAG = "GeminiClient"

        // Официальный WebSocket endpoint Gemini Live API (v1beta, февраль 2026)
        private const val WS_HOST = "generativelanguage.googleapis.com"
        private const val WS_PATH =
            "/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"

        // Размер внутреннего буфера входящих ответов от Gemini
        private const val RESPONSE_CHANNEL_CAPACITY = 64
    }

    // ── Состояние соединения ──────────────────────────────────────────────────

    private var wsSession: WebSocketSession? = null
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e(TAG, "Unhandled exception in GeminiClient scope", throwable)
        setupComplete = false
    }
    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private var connectionJob: Job? = null

    // Канал входящих ответов от сервера (заполняется в receiveLoop)
    private val responseChannel = Channel<GeminiResponse>(RESPONSE_CHANNEL_CAPACITY)

    // Флаг готовности: сервер ответил setupComplete
    @Volatile private var setupComplete = false

    // Deferred для ожидания setupComplete от сервера
    private var setupDeferred = kotlinx.coroutines.CompletableDeferred<Unit>()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Устанавливает WebSocket-соединение и отправляет BidiGenerateContentSetup.
     * Блокирует до получения setupComplete от сервера.
     *
     * @param config    конфигурация Gemini (apiKey, model, audio formats)
     * @param context   контекст сессии (systemPrompt + функции)
     */
    suspend fun connect(
        config: GeminiConfig,
        context: ContextBuilder.SessionContext,
    ) {
        setupComplete = false
        setupDeferred = kotlinx.coroutines.CompletableDeferred()
        connectionJob = clientScope.launch {
            try {
                val urlString = "wss://$WS_HOST$WS_PATH?key=${config.apiKey}"
                httpClient.webSocket(
                    urlString = urlString,
                    request = {
                        // 🟢 В будущем, если ограничишь API ключ по Android приложению в Google Cloud,
                        // раскомментируй эти две строки и вставь свои данные:
                        // header("X-Android-Package", "com.voicedeutsch.master")
                        // header("X-Android-Cert", "ТВОЙ_SHA1_БЕЗ_ДВОЕТОЧИЙ")
                    }
                ) {
                    wsSession = this
                    sendSetup(context, config)
                    receiveLoop()
                }
            } catch (e: Exception) {
                val cause = e.cause?.let { " | Причина: ${it::class.java.simpleName}: ${it.message}" } ?: ""
                val errorMsg = "Ошибка сети: ${e::class.java.simpleName}: ${e.message}$cause"
                android.util.Log.e(TAG, errorMsg, e)
                setupComplete = false

                // 🟢 ГЛАВНОЕ: Моментально отменяем ожидание setupComplete!
                setupDeferred.completeExceptionally(IllegalStateException(errorMsg, e))
                if (!responseChannel.isClosedForSend) responseChannel.close(IllegalStateException(errorMsg, e))
            } finally {
                wsSession = null
                responseChannel.close()
            }
        }
        waitForSetupComplete()
    }

    /** Закрывает WebSocket-соединение. */
    suspend fun disconnect() {
        wsSession?.close()
        wsSession = null
        setupComplete = false
        connectionJob?.cancel()
        connectionJob = null
    }

    /**
     * Отправляет chunk PCM-аудио через realtimeInput.audio.
     * Аудио кодируется в Base64 согласно спецификации Blob.
     */
    suspend fun sendAudioChunk(pcmBytes: ByteArray) {
        val base64Audio = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
        val frame = buildJsonObject {
            putJsonObject("realtimeInput") {
                putJsonObject("audio") {
                    put("data", base64Audio)
                    put("mimeType", "audio/pcm;rate=${config.audioInputFormat.sampleRateHz}")
                }
            }
        }
        sendFrame(frame)
    }

    /**
     * Отправляет текстовое сообщение через clientContent.
     * Используется для текстовых команд и смены стратегии.
     */
    suspend fun sendText(text: String) {
        val frame = buildJsonObject {
            putJsonObject("clientContent") {
                put("turnComplete", true)
                put("turns", JsonArray(listOf(
                    buildJsonObject {
                        put("role", "user")
                        put("parts", JsonArray(listOf(
                            buildJsonObject { put("text", text) }
                        )))
                    }
                )))
            }
        }
        sendFrame(frame)
    }

    /**
     * Возвращает результат выполнения function call обратно в Gemini.
     * Формат: toolResponse с массивом functionResponses.
     */
    suspend fun sendFunctionResult(callId: String, name: String, resultJson: String) {
        val frame = buildJsonObject {
            putJsonObject("toolResponse") {
                put("functionResponses", JsonArray(listOf(
                    buildJsonObject {
                        put("id", callId)
                        put("name", name)
                        put("response", json.parseToJsonElement(resultJson))
                    }
                )))
            }
        }
        sendFrame(frame)
    }

    /**
     * Возвращает следующий ответ от Gemini из внутреннего канала.
     * Suspend — ждёт если канал пуст.
     * Возвращает null если соединение закрыто.
     */
    suspend fun receiveNextResponse(): GeminiResponse? {
        return responseChannel.receiveCatching().getOrNull()
    }

    // ── Внутренняя логика ─────────────────────────────────────────────────────

    /**
     * Отправляет BidiGenerateContentSetup — первое и единственное сообщение
     * инициализации. Включает модель, конфигурацию генерации, системный промпт
     * и объявления функций (tools).
     */
    private suspend fun sendSetup(
        context: ContextBuilder.SessionContext,
        config: GeminiConfig,
    ) {
        val setupMessage = buildJsonObject {
            putJsonObject("setup") {
                put("model", "models/${config.modelName}")

                putJsonObject("generationConfig") {
                    put("temperature", config.temperature)
                    put("topP", config.topP)
                    put("topK", config.topK)
                    put("responseModalities", JsonArray(listOf(
                        JsonPrimitive("AUDIO")
                    )))

                    putJsonObject("speechConfig") {
                        putJsonObject("voiceConfig") {
                            putJsonObject("prebuiltVoiceConfig") {
                                put("voiceName", config.voiceName)
                            }
                        }
                    }
                }

                // Системный промпт из ContextBuilder
                putJsonObject("systemInstruction") {
                    put("role", "user")
                    put("parts", JsonArray(listOf(
                        buildJsonObject { put("text", context.systemPrompt) }
                    )))
                }

                // Function declarations из FunctionRouter
                if (context.functionDeclarations.isNotEmpty()) {
                    put("tools", JsonArray(listOf(
                        buildJsonObject {
                            put("functionDeclarations", JsonArray(
                                context.functionDeclarations.map { json.parseToJsonElement(it) }
                            ))
                        }
                    )))
                }
            }
        }
        sendFrame(setupMessage)
    }

    /**
     * Основной цикл получения WebSocket-фреймов от сервера.
     * Парсит BidiGenerateContentServerMessage и маршрутизирует:
     *   - setupComplete → устанавливает флаг готовности
     *   - serverContent → audio chunks + transcript → GeminiResponse
     *   - toolCall → function calls → GeminiResponse
     *   - goAway → инициирует переподключение (TODO: сигнал в Engine)
     */
    private suspend fun receiveLoop() {
        val session = wsSession ?: return
        try {
            for (frame in session.incoming) {
                if (frame !is Frame.Text) continue
                val raw = frame.readText()
                // Выводим ответ Google в лог, чтобы видеть, на что он ругается
                android.util.Log.d(TAG, "Ответ от сервера: $raw")
                parseServerMessage(raw)
            }
        } catch (e: Exception) {
            Log.e(TAG, "receiveLoop error: ${e.message}", e)
        } finally {
            responseChannel.close()
        }
    }

    private fun parseServerMessage(raw: String) {
        runCatching {
            val root = json.parseToJsonElement(raw).jsonObject

            when {
                // Сервер подтвердил инициализацию сессии
                root.containsKey("setupComplete") -> {
                    setupComplete = true
                    setupDeferred.complete(Unit)
                    Log.d(TAG, "Setup complete — session ready")
                }

                // Основной контент: аудио + транскрипт
                root.containsKey("serverContent") -> {
                    parseServerContent(root["serverContent"]!!.jsonObject)
                }

                // Запрос на выполнение функций
                root.containsKey("toolCall") -> {
                    parseToolCall(root["toolCall"]!!.jsonObject)
                }

                // Сервер скоро отключится — Engine должен переподключиться
                root.containsKey("goAway") -> {
                    Log.w(TAG, "GoAway received — server closing connection soon")
                    // Engine обработает через receiveNextResponse() → null → reconnect
                    responseChannel.close()
                }
            }
        }.onFailure { e ->
            Log.e(TAG, "Failed to parse server message: ${e.message}")
        }
    }

    private fun parseServerContent(serverContent: JsonObject) {
        val modelTurn = serverContent["modelTurn"]?.jsonObject
        val turnComplete = serverContent["turnComplete"]?.jsonPrimitive?.booleanOrNull == true
        // 🟢 Читаем флаг перебивания от сервера
        val interrupted = serverContent["interrupted"]?.jsonPrimitive?.booleanOrNull == true

        val outputTranscript = serverContent["outputTranscription"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
        val inputTranscript = serverContent["inputTranscription"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull

        val audioParts = modelTurn?.get("parts")?.jsonArray?.mapNotNull { part ->
            part.jsonObject["inlineData"]?.jsonObject?.get("data")?.jsonPrimitive?.contentOrNull?.let { Base64.decode(it, Base64.NO_WRAP) }
        }
        val audioData = audioParts?.firstOrNull()
        val textTranscript = modelTurn?.get("parts")?.jsonArray?.mapNotNull { part ->
            part.jsonObject["text"]?.jsonPrimitive?.contentOrNull
        }?.joinToString("")

        val finalTranscript = outputTranscript ?: textTranscript

        // 🟢 Передаем флаг isInterrupted наверх
        if (audioData != null || finalTranscript != null || turnComplete || interrupted) {
            responseChannel.trySend(
                GeminiResponse(
                    audioData = audioData,
                    transcript = finalTranscript ?: inputTranscript,
                    functionCall = null,
                    isTurnComplete = turnComplete,
                    isInterrupted = interrupted,
                )
            )
        }
    }

    private fun parseToolCall(toolCall: JsonObject) {
        val functionCalls = toolCall["functionCalls"]?.jsonArray ?: return

        functionCalls.forEach { callElement ->
            val call = callElement.jsonObject
            val id = call["id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val name = call["name"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val args = call["args"]?.let { json.encodeToString(JsonElement.serializer(), it) } ?: "{}"

            responseChannel.trySend(
                GeminiResponse(
                    audioData = null,
                    transcript = null,
                    functionCall = GeminiFunctionCall(id = id, name = name, argsJson = args),
                    isTurnComplete = false,
                )
            )
        }
    }

    private suspend fun waitForSetupComplete() {
        try {
            kotlinx.coroutines.withTimeout(30_000L) {
                setupDeferred.await()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            val sessionActive = wsSession?.isActive ?: false
            throw IllegalStateException(
                "Timeout 30s. WS session active=$sessionActive. " +
                "Setup отправлен но setupComplete не получен. " +
                "Смотри ошибки выше."
            )
        }
    }

    private suspend fun sendFrame(payload: JsonObject) {
        val session = wsSession
        if (session == null || !session.isActive) {
            Log.w(TAG, "sendFrame: no active session, dropping frame")
            return
        }
        runCatching {
            session.send(Frame.Text(json.encodeToString(JsonObject.serializer(), payload)))
        }.onFailure { e ->
            Log.e(TAG, "sendFrame error: ${e.message}", e)
        }
    }

    fun release() {
        clientScope.cancel()
        responseChannel.close()
    }
}

// ── Gemini response models ────────────────────────────────────────────────────
// Перенесены из VoiceCoreEngineImpl.kt — логически принадлежат клиенту.
// Убран modifier `internal` чтобы receiveNextResponse() мог быть public.

data class GeminiFunctionCall(
    val id: String,
    val name: String,
    val argsJson: String,
)

data class GeminiResponse(
    val audioData: ByteArray?,
    val transcript: String?,
    val functionCall: GeminiFunctionCall?,
    val isTurnComplete: Boolean = false,
    val isInterrupted: Boolean = false, // 🟢 ДОБАВЛЕН ФЛАГ ПЕРЕБИВАНИЯ
) {
    fun hasAudio(): Boolean = audioData != null && audioData.isNotEmpty()
    fun hasFunctionCall(): Boolean = functionCall != null
    fun hasTranscript(): Boolean =
        !transcript.isNullOrEmpty() && audioData == null && functionCall == null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeminiResponse) return false
        return transcript == other.transcript &&
                functionCall == other.functionCall &&
                isTurnComplete == other.isTurnComplete &&
                isInterrupted == other.isInterrupted &&
                (audioData?.contentEquals(other.audioData) == true || (audioData == null && other.audioData == null))
    }

    override fun hashCode(): Int = transcript.hashCode() * 31 + isTurnComplete.hashCode() + isInterrupted.hashCode()
}
