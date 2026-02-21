package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot

/**
 * Listening comprehension strategy.
 * Architecture line 857 (ListeningStrategy.kt).
 */
class ListeningStrategy : StrategyHandler {

    override val strategy = LearningStrategy.LISTENING

    override fun getStrategyContext(snapshot: KnowledgeSnapshot): String = buildString {
        appendLine("РЕЖИМ: Аудирование и понимание на слух.")
        appendLine("Говори на немецком — пользователь должен понять и ответить.")
        appendLine("Начни с коротких предложений, постепенно усложняй.")
        appendLine("Проверяй понимание: задавай вопросы по содержанию.")
        appendLine("Используй лексику на уровне пользователя ± 1 подуровень.")
        appendLine("Если не понял — повтори медленнее, затем объясни по-русски.")
    }
}