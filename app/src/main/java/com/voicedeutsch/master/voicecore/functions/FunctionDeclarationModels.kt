package com.voicedeutsch.master.voicecore.functions

/**
 * ════════════════════════════════════════════════════════════════════
 * FIX: parameters теперь nullable (null = функция без параметров).
 *
 * БЫЛО:  val parameters: GeminiParameters = GeminiParameters()
 *        → Всегда non-null, проверка `params == null` никогда не работала.
 *
 * СТАЛО: val parameters: GeminiParameters? = null
 *        → Null для parameterless функций, non-null для функций с параметрами.
 * ════════════════════════════════════════════════════════════════════
 */
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