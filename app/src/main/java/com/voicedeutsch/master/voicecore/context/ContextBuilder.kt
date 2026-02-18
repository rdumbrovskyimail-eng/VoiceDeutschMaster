package com.voicedeutsch.master.voicecore.context

import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Serialises [KnowledgeSnapshot] into a compact JSON block for the Gemini context.
 * Architecture lines 600-610 (User Context block, ~10K tokens).
 *
 * Uses compact JSON (no pretty-print) to conserve context tokens.
 */
class UserContextProvider(private val json: Json) {

    fun buildUserContext(snapshot: KnowledgeSnapshot): String {
        val compactJson = json.encodeToString(snapshot)
        return buildString {
            appendLine("=== USER KNOWLEDGE CONTEXT ===")
            appendLine(compactJson)
            appendLine("=== END USER KNOWLEDGE CONTEXT ===")
        }
    }
}