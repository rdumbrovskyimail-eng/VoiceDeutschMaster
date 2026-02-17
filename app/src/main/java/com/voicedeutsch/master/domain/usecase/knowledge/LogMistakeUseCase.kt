package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.model.knowledge.MistakeLog
import com.voicedeutsch.master.domain.model.knowledge.MistakeType
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.util.DateUtils
import com.voicedeutsch.master.util.generateUUID

/**
 * Logs a mistake made by the user during a learning session.
 * Called via Function Call: log_mistake() from Gemini after each error.
 */
class LogMistakeUseCase(
    private val knowledgeRepository: KnowledgeRepository
) {

    data class Params(
        val userId: String,
        val sessionId: String?,
        val type: MistakeType,
        val item: String,
        val expected: String,
        val actual: String,
        val context: String = "",
        val explanation: String = ""
    )

    suspend operator fun invoke(params: Params) {
        val now = DateUtils.nowTimestamp()

        val mistake = MistakeLog(
            id = generateUUID(),
            userId = params.userId,
            sessionId = params.sessionId,
            type = params.type,
            item = params.item,
            expected = params.expected,
            actual = params.actual,
            context = params.context,
            explanation = params.explanation,
            timestamp = now,
            createdAt = now
        )

        knowledgeRepository.logMistake(mistake)
    }
}