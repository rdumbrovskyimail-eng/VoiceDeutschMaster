package com.voicedeutsch.master.voicecore.engine

/**
 * Главный интерфейс для управления голосовым взаимодействием в VoiceDeutsch.
 * Координирует все компоненты системы для обработки голосовых команд и генерации ответов.
 */
interface VoiceCoreEngine {
    
    /**
     * Запускает сессию голосового взаимодействия
     */
    suspend fun startSession(): Result<Unit>
    
    /**
     * Останавливает текущую сессию
     */
    suspend fun stopSession(): Result<Unit>
    
    /**
     * Обрабатывает входящий голосовой сигнал
     * @param audioData Данные аудиосигнала
     */
    suspend fun processAudio(audioData: ByteArray): Result<String>
    
    /**
     * Отправляет текстовый запрос в систему
     * @param text Текст запроса
     */
    suspend fun sendRequest(text: String): Result<String>
    
    /**
     * Получает текущее состояние сессии
     */
    fun getSessionState(): SessionState
    
    /**
     * Освобождает ресурсы двигателя
     */
    suspend fun release()
}

// Data class для результата обработки
data class ProcessingResult(
    val text: String,
    val confidence: Float,
    val language: String = "de"
)
