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
    private val _incomingMessages = Channel<GeminiServerMessage>(Channel.BUFFERED)
    val incomingMessages: Flow<GeminiServerMessage> = _incomingMessages.receiveAsFlow()

    suspend fun connect(apiKey: String, model: String): Boolean {
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        return runCatching {
            httpClient.webSocket(url) {
                session = this
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        runCatching {
                            val msg = json.decodeFromString<GeminiServerMessage>(text)
                            _incomingMessages.send(msg)
                        }
                    }
                }
            }
        }.isSuccess
    }

    suspend fun send(text: String) {
        session?.send(Frame.Text(text))
    }

    suspend fun disconnect() {
        session?.close()
        session = null
    }

    val isConnected: Boolean get() = session != null
}