package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot

/**
 * Focused pronunciation practice.
 * Architecture line 854 (PronunciationStrategy.kt).
 */
class PronunciationStrategy : StrategyHandler {

    override val strategy = LearningStrategy.PRONUNCIATION

    override fun getStrategyContext(snapshot: KnowledgeSnapshot): String = buildString {
        appendLine("РЕЖИМ: Тренировка произношения.")
        appendLine("Вызови get_pronunciation_targets для проблемных звуков.")
        val problems = snapshot.pronunciation.problemSounds
        if (problems.isNotEmpty()) {
            appendLine("Проблемные звуки: ${problems.joinToString(", ")}")
        }
        appendLine("Общая оценка произношения: ${(snapshot.pronunciation.overallScore * 100).toInt()}%")
        appendLine("Для каждого слова: произнеси образец → попроси повторить →")
        appendLine("оцени через save_pronunciation_result.")
        appendLine("Давай подсказки по артикуляции (положение языка, губ).")
    }
}