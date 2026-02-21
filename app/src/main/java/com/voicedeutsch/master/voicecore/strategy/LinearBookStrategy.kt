package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot

/**
 * Linear progression through the textbook.
 * Architecture line 850 (LinearBookStrategy.kt).
 */
class LinearBookStrategy : StrategyHandler {

    override val strategy = LearningStrategy.LINEAR_BOOK

    override fun getStrategyContext(snapshot: KnowledgeSnapshot): String = buildString {
        appendLine("РЕЖИМ: Линейное прохождение книги.")
        appendLine("Следуй текущему уроку. Представь новые слова, объясни грамматику,")
        appendLine("проведи упражнения из урока. После завершения — вызови mark_lesson_complete.")
        appendLine("Текущий прогресс: глава ${snapshot.bookProgress.currentChapter}, урок ${snapshot.bookProgress.currentLesson}")
    }
}