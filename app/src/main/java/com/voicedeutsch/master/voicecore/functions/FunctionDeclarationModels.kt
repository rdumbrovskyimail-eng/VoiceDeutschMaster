package com.voicedeutsch.master.voicecore.functions

data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: GeminiParameters? = null,
)

data class GeminiParameters(
    val type: String = "object",
    val properties: Map<String, GeminiProperty> = emptyMap(),
    val required: List<String> = emptyList(),
)

data class GeminiProperty(
    val type: String,
    val description: String = "",
    val enum: List<String>? = null,
)