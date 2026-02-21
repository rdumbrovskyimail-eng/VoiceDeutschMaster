package com.voicedeutsch.master.data.remote.gemini

import com.voicedeutsch.master.voicecore.engine.GeminiConfig
import kotlinx.serialization.json.Json

/**
 * Maps between domain/voicecore models and Gemini wire-format models.
 */
object GeminiMapper {

    fun buildSetupMessage(config: GeminiConfig, systemPrompt: String, tools: GeminiTool): GeminiSetupMessage {
        return GeminiSetupMessage(
            setup = GeminiSetup(
                model = config.modelName,
                generationConfig = GeminiGenerationConfig(
                    temperature = config.temperature,
                    speechConfig = GeminiSpeechConfig(
                        voiceConfig = GeminiVoiceConfig(
                            prebuiltVoiceConfig = GeminiPrebuiltVoice(voiceName = "Kore")
                        )
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt)),
                    role = "user"
                ),
                tools = listOf(tools)
            )
        )
    }

    fun buildAudioMessage(pcmBase64: String): String {
        val json = Json { encodeDefaults = true }
        val msg = mapOf(
            "realtimeInput" to mapOf(
                "mediaChunks" to listOf(
                    mapOf("mimeType" to "audio/pcm;rate=16000", "data" to pcmBase64)
                )
            )
        )
        return json.encodeToString(kotlinx.serialization.serializer(), msg)
    }
}