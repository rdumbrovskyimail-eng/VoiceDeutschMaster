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

        // Актуальная модель на февраль 2026.
        // gemini-2.0-flash-live-001 retire 31.03.2026 → используем 2.5
        private const val MODEL_LIVE = "gemini-2.5-flash-live"

        // Размер внутреннего буфера входящих ответов от Gemini
        private const val RESPONSE_CHANNEL_CAPACITY = 64
    }

    // ── Состояние соединения ──────────────────────────────────────────────────

    private var wsSession: WebSocketSession? = null
    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Канал входящих ответов от сервера (заполняется в receiveLoop)
    private val responseChannel = Channel<GeminiResponse>(RESPONSE_CHANNEL_CAPACITY)

    // Флаг готовности: сервер ответил setupComplete
    @Volatile private var setupComplete = false

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

        httpClient.webSocket(
            host = WS_HOST,
            path = "$WS_PATH?key=${config.apiKey}",
        ) {
            wsSession = this

            // 1. Отправить Setup — первое сообщение, единственное в жизни сессии
            sendSetup(context, config)

            // 2. Запустить фоновый цикл получения сообщений от сервера
            clientScope.launch {
                receiveLoop()
            }

            // 3. Ждать подтверждения setupComplete (сервер ответит быстро)
            waitForSetupComplete()
        }
    }

    /** Закрывает WebSocket-соединение. */
    suspend fun disconnect() {
        wsSession?.close()
        wsSession = null
        setupComplete = false
        responseChannel.tryReceive() // дренировать остатки
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
    suspend fun sendFunctionResult(callId: String, resultJson: String) {
        val frame = buildJsonObject {
            putJsonObject("toolResponse") {
                put("functionResponses", JsonArray(listOf(
                    buildJsonObject {
                        put("id", callId)
                        put("name", "") // сервер идентифицирует по id
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
                put("model", "models/$MODEL_LIVE")

                putJsonObject("generationConfig") {
                    put("temperature", config.temperature)
                    put("topP", config.topP)
                    put("topK", config.topK)
                    put("responseModalities", JsonArray(listOf(JsonPrimitive("AUDIO"))))

                    putJsonObject("speechConfig") {
                        putJsonObject("voiceConfig") {
                            putJsonObject("prebuiltVoiceConfig") {
                                // Puck — дефолтный голос, подходит для учебного ассистента
                                put("voiceName", "Puck")
                            }
                        }
                    }
                }

                // Системный промпт из ContextBuilder
                putJsonObject("systemInstruction") {
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
        val turnComplete = serverContent["turnComplete"]?.jsonPrimitive?.contentOrNull == "true"
        val interrupted = serverContent["interrupted"]?.jsonPrimitive?.contentOrNull == "true"

        // Транскрипт выходного аудио (отдельный поток, может прийти раньше/позже аудио)
        val outputTranscript = serverContent["outputTranscription"]
            ?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull

        // Транскрипт входного аудио (речь пользователя)
        val inputTranscript = serverContent["inputTranscription"]
            ?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull

        // Извлечь аудио-данные (inlineData.data — Base64 PCM)
        val audioParts = modelTurn?.get("parts")?.jsonArray
            ?.mapNotNull { part ->
                part.jsonObject["inlineData"]?.jsonObject
                    ?.get("data")?.jsonPrimitive?.contentOrNull
                    ?.let { Base64.decode(it, Base64.NO_WRAP) }
            }

        val audioData = audioParts?.firstOrNull()

        // Текстовые части (если модель отвечает текстом)
        val textTranscript = modelTurn?.get("parts")?.jsonArray
            ?.mapNotNull { part ->
                part.jsonObject["text"]?.jsonPrimitive?.contentOrNull
            }?.joinToString("")

        val finalTranscript = outputTranscript ?: textTranscript

        // Если ничего нет и turn просто complete — всё равно сигнализируем
        if (audioData != null || finalTranscript != null || turnComplete || interrupted) {
            responseChannel.trySend(
                GeminiResponse(
                    audioData = audioData,
                    transcript = finalTranscript ?: inputTranscript,
                    functionCall = null,
                    isTurnComplete = turnComplete || interrupted,
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
        // Ждём максимум 5 секунд — сервер обычно отвечает за < 500 ms
        var waited = 0L
        while (!setupComplete && waited < 5_000L) {
            kotlinx.coroutines.delay(50)
            waited += 50
        }
        if (!setupComplete) {
            throw IllegalStateException("Gemini setup timeout — no setupComplete received")
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
                (audioData == null && other.audioData == null ||
                        audioData != null && other.audioData != null &&
                        audioData.contentEquals(other.audioData))
    }

    override fun hashCode(): Int = transcript.hashCode() * 31 + isTurnComplete.hashCode()
}
