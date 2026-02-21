package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot

/**
 * SRS-based repetition of due words and rules.
 * Architecture line 852 (RepetitionStrategy.kt).
 */
class RepetitionStrategy : StrategyHandler {

    override val strategy = LearningStrategy.REPETITION

    override fun getStrategyContext(snapshot: KnowledgeSnapshot): String = buildString {
        appendLine("РЕЖИМ: Повторение по SRS (интервальное повторение).")
        appendLine("Слов на повторение сегодня: ${snapshot.vocabulary.wordsForReviewToday}")
        appendLine("Правил на повторение: ${snapshot.grammar.rulesForReviewToday}")
        appendLine("Вызови get_words_for_repetition для получения списка слов.")
        appendLine("После каждого слова вызови save_word_knowledge с оценкой quality 0-5.")
        appendLine("Не представляй новый материал — только повторение.")
    }

    override fun evaluateMidSession(
        elapsedMinutes: Int,
        errorRate: Float,
        itemsCompleted: Int,
    ): LearningStrategy? {
        // If all items reviewed, switch to book
        // (This would be checked via snapshot.vocabulary.wordsForReviewToday == 0)
        return super.evaluateMidSession(elapsedMinutes, errorRate, itemsCompleted)
    }
}