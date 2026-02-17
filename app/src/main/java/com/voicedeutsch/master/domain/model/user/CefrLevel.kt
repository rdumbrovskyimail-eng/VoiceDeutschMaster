package com.voicedeutsch.master.domain.model.user

import kotlinx.serialization.Serializable

/**
 * Common European Framework of Reference (CEFR) language proficiency levels.
 *
 * Each level has a Russian display name and a numeric order for comparison.
 * Sub-levels (1–10) are tracked separately in [UserProfile.cefrSubLevel].
 *
 * **Progression criteria:**
 * - vocabulary ≥ 70% + grammar ≥ 60% → level confirmed
 * - Sub-level determined via interpolation
 */
@Serializable
enum class CefrLevel(val displayName: String, val order: Int) {
    A1("Начальный", 1),
    A2("Элементарный", 2),
    B1("Средний", 3),
    B2("Выше среднего", 4),
    C1("Продвинутый", 5),
    C2("Мастер", 6);

    /**
     * Returns the next CEFR level, or `null` if already at [C2].
     */
    fun next(): CefrLevel? = entries.find { it.order == this.order + 1 }

    /**
     * Returns the previous CEFR level, or `null` if already at [A1].
     */
    fun previous(): CefrLevel? = entries.find { it.order == this.order - 1 }

    companion object {
        /**
         * Parses a CEFR level from its name string. Defaults to [A1] when not found.
         */
        fun fromString(value: String): CefrLevel =
            entries.find { it.name == value } ?: A1
    }
}