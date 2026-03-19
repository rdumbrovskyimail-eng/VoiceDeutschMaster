package com.voicedeutsch.master.voicecore.engine

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.withTimeout

@OptIn(PublicPreviewAPI::class)
class GeminiClient {

    companion object {
        private const val TAG = "GeminiClient"
    }

    private var liveSession: LiveSession? = null

    suspend fun connect() {
        val liveModel = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
            modelName = "gemini-2.5-flash-native-audio-preview-12-2025",
            generationConfig = liveGenerationConfig {
                responseModality = ResponseModality.AUDIO
            }
        )
        liveSession = withTimeout(15_000L) {
            liveModel.connect()
        }
        Log.d(TAG, "Connected: $liveSession")
    }

    suspend fun sendAudio(pcmBytes: ByteArray) {
        liveSession?.sendAudioRealtime(InlineData(pcmBytes, "audio/pcm;rate=16000"))
    }

    suspend fun startReceiving() {
        liveSession?.receive()?.collect { response ->
            Log.d(TAG, "Response: $response")
        }
    }

    suspend fun disconnect() {
        liveSession?.close()
        liveSession = null
        Log.d(TAG, "Disconnected")
    }
}