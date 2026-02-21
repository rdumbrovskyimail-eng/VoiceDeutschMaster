package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot

/**
 * Grammar drill strategy.
 * Architecture line 855 (GrammarStrategy.kt).
 */
class GrammarStrategy : StrategyHandler {

    override val strategy = LearningStrategy.GRAMMAR_DRILL

    override fun getStrategyContext(snapshot: KnowledgeSnapshot): String = buildString {
        appendLine("РЕЖИМ: Грамматические упражнения.")
        appendLine("Всего правил: ${snapshot.grammar.totalRules}")
        appendLine("На повторение: ${snapshot.grammar.rulesForReviewToday}")
        appendLine("Объясняй правило кратко → давай 3-5 упражнений → оценивай.")
        appendLine("Для каждого правила вызывай save_rule_knowledge с оценкой.")
        appendLine("При ошибках — record_mistake с type='grammar'.")
    }
}