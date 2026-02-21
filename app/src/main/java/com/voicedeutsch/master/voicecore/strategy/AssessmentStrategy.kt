package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot

class AssessmentStrategy : StrategyHandler {

    override val strategy = LearningStrategy.ASSESSMENT

    override fun getStrategyContext(snapshot: KnowledgeSnapshot): String = buildString {
        appendLine("РЕЖИМ: Оценка уровня пользователя.")
        appendLine("Текущий уровень: ${snapshot.vocabulary.totalWords} слов, ${snapshot.grammar.totalRules} правил изучено.")
        appendLine("Проведи диагностическую сессию:")
        appendLine("1. Начни с простых A1/A2 вопросов.")
        appendLine("2. Постепенно усложняй до предполагаемого уровня пользователя.")
        appendLine("3. Фиксируй каждую ошибку через record_mistake.")
        appendLine("4. По результатам вызови update_user_level с реальным CEFR уровнем.")
        appendLine("5. После оценки предложи переключиться на основную стратегию.")
    }

    override fun evaluateMidSession(
        elapsedMinutes: Int,
        errorRate: Float,
        itemsCompleted: Int,
    ): LearningStrategy? {
        return if (itemsCompleted >= 15 || elapsedMinutes >= 10) {
            LearningStrategy.LINEAR_BOOK
        } else null
    }
}