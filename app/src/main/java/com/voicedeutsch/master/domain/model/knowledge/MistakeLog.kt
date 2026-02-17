package com.voicedeutsch.master.domain.model.knowledge

import kotlinx.serialization.Serializable

/**
 * A persistent record of a user mistake.
 *
 * Stored in the `mistakes_log` table and used for weak-point detection,
 * gap-filling strategy, and Gemini context enrichment.
 */
@Serializable
data class MistakeLog(
    val id: String,
    val userId: String,
    val sessionId: String?,
    val type: MistakeType,
    val item: String,
    val expected: String,
    val actual: String,
    val context: String = "",
    val explanation: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * The category of a mistake.
 */
@Serializable
enum class MistakeType { WORD, GRAMMAR, PRONUNCIATION, PHRASE }