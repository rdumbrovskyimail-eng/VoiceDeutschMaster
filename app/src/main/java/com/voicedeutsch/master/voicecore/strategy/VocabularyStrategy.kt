package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot

/**
 * Intensive vocabulary building strategy.
 * Architecture line 856 (VocabularyStrategy.kt).
 */
class VocabularyStrategy : StrategyHandler {

    override val strategy = LearningStrategy.VOCABULARY_BOOST

    override fun getStrategyContext(snapshot: KnowledgeSnapshot): String = buildString {
        appendLine("РЕЖИМ: Расширение словарного запаса.")
        appendLine("Текущий запас: ${snapshot.vocabulary.totalWords} слов.")
        appendLine("Представляй новые слова тематическими группами по 5-7 штук.")
        appendLine("Метод: слово → перевод → пример → контекст → проверка.")
        appendLine("Вызывай save_word_knowledge для каждого нового слова (level=1, quality=3).")
        appendLine("Включай мнемонические ассоциации и группировку по темам.")
    }
}