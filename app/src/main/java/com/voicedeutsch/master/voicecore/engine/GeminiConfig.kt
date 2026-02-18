package com.voicedeutsch.master.voicecore.engine

/**
 * Конфигурация для подключения к Google Gemini API.
 * Содержит все необходимые параметры для инициализации и работы с LLM.
 */
data class GeminiConfig(
    val apiKey: String,
    val modelId: String = "gemini-pro",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val timeout: Long = 30000L, // milliseconds
    val retryAttempts: Int = 3,
    val retryDelayMs: Long = 1000L,
    val enableSafetyFilters: Boolean = true,
    val safetyThreshold: String = "BLOCK_MEDIUM_AND_ABOVE"
) {
    init {
        require(apiKey.isNotBlank()) { "API Key cannot be empty" }
        require(temperature in 0f..2f) { "Temperature must be between 0 and 2" }
        require(maxTokens > 0) { "Max tokens must be positive" }
        require(topP in 0f..1f) { "Top P must be between 0 and 1" }
        require(topK > 0) { "Top K must be positive" }
    }
    
    companion object {
        /**
         * Создает конфиг с минимальными параметрами
         */
        fun minimal(apiKey: String): GeminiConfig {
            return GeminiConfig(apiKey = apiKey)
        }
        
        /**
         * Создает конфиг для быстрого ответа
         */
        fun fastResponse(apiKey: String): GeminiConfig {
            return GeminiConfig(
                apiKey = apiKey,
                temperature = 0.5f,
                maxTokens = 512,
                timeout = 10000L
            )
        }
        
        /**
         * Создает конфиг для детальных ответов
         */
        fun detailed(apiKey: String): GeminiConfig {
            return GeminiConfig(
                apiKey = apiKey,
                temperature = 0.9f,
                maxTokens = 4096,
                timeout = 60000L
            )
        }
    }
}
