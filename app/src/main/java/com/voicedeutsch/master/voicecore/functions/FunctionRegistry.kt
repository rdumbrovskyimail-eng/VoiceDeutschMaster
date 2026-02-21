package com.voicedeutsch.master.voicecore.functions

import com.voicedeutsch.master.data.remote.gemini.GeminiFunctionDeclaration
import com.voicedeutsch.master.data.remote.gemini.GeminiParameters
import com.voicedeutsch.master.data.remote.gemini.GeminiProperty

/**
 * Central registry of all function declarations sent to Gemini.
 *
 * Architecture line 830 (FunctionRegistry.kt).
 *
 * Each function group provides its own declarations. The registry
 * aggregates them into a single list for the setup message.
 */
object FunctionRegistry {

    /** Returns all function declarations for the Gemini setup message. */
    fun getAllDeclarations(): List<GeminiFunctionDeclaration> = buildList {
        addAll(KnowledgeFunctions.declarations)
        addAll(BookFunctions.declarations)
        addAll(SessionFunctions.declarations)
        addAll(LearningFunctions.declarations)
        addAll(ProgressFunctions.declarations)
        addAll(UIFunctions.declarations)
    }

    // ── Helper builders ───────────────────────────────────────────────────────

    fun declare(
        name: String,
        description: String,
        params: Map<String, Pair<String, String>> = emptyMap(), // name -> (type, description)
        required: List<String> = emptyList(),
    ) = GeminiFunctionDeclaration(
        name = name,
        description = description,
        parameters = if (params.isEmpty()) null else GeminiParameters(
            properties = params.mapValues { (_, v) -> GeminiProperty(type = v.first, description = v.second) },
            required = required,
        )
    )
}