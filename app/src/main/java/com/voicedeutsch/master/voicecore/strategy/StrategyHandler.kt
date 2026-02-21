package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot

/**
 * Base interface for all strategy handlers.
 * Each handler provides strategy-specific context and item selection.
 */
interface StrategyHandler {

    val strategy: LearningStrategy

    /**
     * Additional prompt context for this strategy.
     * Injected into the system prompt after the master prompt.
     */
    fun getStrategyContext(snapshot: KnowledgeSnapshot): String

    /**
     * Checks if strategy should switch mid-session.
     * @return Suggested new strategy, or null to continue.
     */
    fun evaluateMidSession(
        elapsedMinutes: Int,
        errorRate: Float,
        itemsCompleted: Int,
    ): LearningStrategy? {
        // Default: switch if error rate > 60% for > 5 items
        if (itemsCompleted > 5 && errorRate > 0.6f) {
            return LearningStrategy.FREE_PRACTICE
        }
        // Default: switch after 25 minutes on same strategy
        if (elapsedMinutes > 25) {
            return LearningStrategy.FREE_PRACTICE
        }
        return null
    }
}