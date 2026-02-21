package com.voicedeutsch.master.data.remote.gemini

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json

/**
 * Low-level service for managing the Gemini Live API WebSocket connection.
 * Extracted from GeminiClient to keep wire-protocol concerns isolated.
 */
class GeminiService(
    private val httpClient: HttpClient,
    private val json: Json
) {

    private var session: WebSocketSession? = null
    @Volatile private var _isConnected = false
    private val _incomingMessages = Channel<GeminiServerMessage>(Channel.UNLIMITED)
    val incomingMessages: Flow<GeminiServerMessage> = _incomingMessages.receiveAsFlow()

    suspend fun connect(apiKey: String, model: String): Boolean {
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        return runCatching {
            httpClient.webSocket(url) {
                session = this
                _isConnected = true
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            runCatching {
                                val msg = json.decodeFromString<GeminiServerMessage>(text)
                                _incomingMessages.send(msg)
                            }.onFailure { e ->
                                android.util.Log.w("GeminiService", "Frame parse error", e)
                            }
                        }
                    }
                } finally {
                    _isConnected = false
                    session = null
                }
            }
        }.isSuccess
    }

    suspend fun send(text: String) {
        session?.send(Frame.Text(text))
    }

    suspend fun disconnect() {
        _isConnected = false
        session?.close()
        session = null
    }

    val isConnected: Boolean get() = _isConnected && session?.isActive == true
}