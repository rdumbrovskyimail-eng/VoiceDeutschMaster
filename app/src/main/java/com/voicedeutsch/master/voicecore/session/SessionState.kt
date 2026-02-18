package com.voicedeutsch.master.voicecore.session

import java.time.LocalDateTime

/**
 * Состояния сессии голосового взаимодействия
 */
sealed class SessionState {
    /**
     * Сессия неактивна
     */
    object Idle : SessionState()
    
    /**
     * Сессия активна и готова к приему аудио
     */
    object Active : SessionState()
    
    /**
     * Система слушает входящий голос
     */
    object Listening : SessionState()
    
    /**
     * Происходит обработка аудио и генерация ответа
     */
    data class Processing(val startTime: LocalDateTime = LocalDateTime.now()) : SessionState()
    
    /**
     * Воспроизведение ответа пользователю
     */
    object Playing : SessionState()
    
    /**
     * Ошибка в сессии
     */
    data class Error(val exception: Throwable, val message: String) : SessionState()
    
    /**
     * Сессия завершена
     */
    object Completed : SessionState()
}

/**
 * Данные текущей сессии
 */
data class SessionData(
    val sessionId: String,
    val startTime: LocalDateTime,
    val state: SessionState = SessionState.Idle,
    val conversationHistory: List<ConversationTurn> = emptyList(),
    val language: String = "de",
    val userId: String? = null
)

/**
 * Один обмен в диалоге
 */
data class ConversationTurn(
    val userInput: String,
    val assistantOutput: String,
    val timestamp: LocalDateTime,
    val processingTimeMs: Long
)
