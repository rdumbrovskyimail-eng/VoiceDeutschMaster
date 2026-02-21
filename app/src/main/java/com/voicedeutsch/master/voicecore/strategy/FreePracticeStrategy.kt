package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot

/**
 * Free conversation practice at the user's level.
 * Architecture line 853 (FreePracticeStrategy.kt).
 */
class FreePracticeStrategy : StrategyHandler {

    override val strategy = LearningStrategy.FREE_PRACTICE

    override fun getStrategyContext(snapshot: KnowledgeSnapshot): String = buildString {
        appendLine("РЕЖИМ: Свободная практика разговора.")
        appendLine("Веди естественный диалог на немецком на темы, интересные пользователю.")
        appendLine("Корректируй ошибки мягко, расширяй словарный запас через контекст.")
        appendLine("Используй слова и грамматику на уровне пользователя с небольшим усложнением.")
        appendLine("Периодически вызывай save_word_knowledge для новых слов.")
    }

    override fun evaluateMidSession(
        elapsedMinutes: Int,
        errorRate: Float,
        itemsCompleted: Int,
    ): LearningStrategy? = null // Free practice doesn't auto-switch
}