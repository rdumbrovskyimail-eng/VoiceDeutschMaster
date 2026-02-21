package com.voicedeutsch.master.data.remote.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire-format models for the Gemini Live API WebSocket protocol. */

@Serializable
data class GeminiSetupMessage(
    val setup: GeminiSetup
)

@Serializable
data class GeminiSetup(
    val model: String,
    @SerialName("generation_config")
    val generationConfig: GeminiGenerationConfig,
    @SerialName("system_instruction")
    val systemInstruction: GeminiContent? = null,
    val tools: List<GeminiTool> = emptyList()
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Float = 0.5f,
    @SerialName("top_p")
    val topP: Float = 0.95f,
    @SerialName("top_k")
    val topK: Int = 40,
    @SerialName("response_modalities")
    val responseModalities: List<String> = listOf("AUDIO", "TEXT"),
    @SerialName("speech_config")
    val speechConfig: GeminiSpeechConfig? = null
)

@Serializable
data class GeminiSpeechConfig(
    @SerialName("voice_config")
    val voiceConfig: GeminiVoiceConfig? = null
)

@Serializable
data class GeminiVoiceConfig(
    @SerialName("prebuilt_voice_config")
    val prebuiltVoiceConfig: GeminiPrebuiltVoice? = null
)

@Serializable
data class GeminiPrebuiltVoice(
    @SerialName("voice_name")
    val voiceName: String = "Kore" // Neutral, clear voice
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String = "user"
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    @SerialName("inline_data")
    val inlineData: GeminiInlineData? = null
)

@Serializable
data class GeminiInlineData(
    @SerialName("mime_type")
    val mimeType: String, // "audio/pcm;rate=16000"
    val data: String       // Base64-encoded audio
)

@Serializable
data class GeminiTool(
    @SerialName("function_declarations")
    val functionDeclarations: List<GeminiFunctionDeclaration> = emptyList()
)

@Serializable
data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: GeminiParameters? = null
)

@Serializable
data class GeminiParameters(
    val type: String = "object",
    val properties: Map<String, GeminiProperty> = emptyMap(),
    val required: List<String> = emptyList()
)

@Serializable
data class GeminiProperty(
    val type: String,
    val description: String = "",
    val enum: List<String>? = null
)

// ── Response models ───────────────────────────────────────────────────────────

@Serializable
data class GeminiServerMessage(
    @SerialName("setupComplete")
    val setupComplete: GeminiSetupComplete? = null,
    @SerialName("serverContent")
    val serverContent: GeminiServerContent? = null,
    @SerialName("toolCall")
    val toolCall: GeminiToolCall? = null,
    @SerialName("toolCallCancellation")
    val toolCallCancellation: GeminiToolCallCancellation? = null
)

@Serializable
data class GeminiSetupComplete(val placeholder: String? = null)

@Serializable
data class GeminiServerContent(
    val modelTurn: GeminiContent? = null,
    val turnComplete: Boolean = false
)

@Serializable
data class GeminiToolCall(
    @SerialName("functionCalls")
    val functionCalls: List<GeminiFunctionCall> = emptyList()
)

@Serializable
data class GeminiFunctionCall(
    val id: String,
    val name: String,
    val args: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap()
)

@Serializable
data class GeminiToolCallCancellation(
    val ids: List<String> = emptyList()
)

@Serializable
data class GeminiToolResponse(
    @SerialName("toolResponse")
    val toolResponse: GeminiToolResponsePayload
)

@Serializable
data class GeminiToolResponsePayload(
    @SerialName("functionResponses")
    val functionResponses: List<GeminiFunctionResponse>
)

@Serializable
data class GeminiFunctionResponse(
    val id: String,
    val name: String,
    val response: Map<String, kotlinx.serialization.json.JsonElement>
)