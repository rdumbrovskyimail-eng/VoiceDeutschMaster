package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot

/**
 * Focus on weak points identified by mistake analysis.
 * Architecture line 851 (GapFillingStrategy.kt).
 */
class GapFillingStrategy : StrategyHandler {

    override val strategy = LearningStrategy.GAP_FILLING

    override fun getStrategyContext(snapshot: KnowledgeSnapshot): String = buildString {
        appendLine("РЕЖИМ: Заполнение пробелов (слабые места).")
        appendLine("Вызови get_weak_points для получения проблемных областей.")
        appendLine("Сосредоточься на: ${snapshot.weakPoints.take(5).joinToString(", ")}")
        appendLine("Создавай упражнения специально для этих проблем.")
        appendLine("Фиксируй каждую ошибку через record_mistake.")
    }
}