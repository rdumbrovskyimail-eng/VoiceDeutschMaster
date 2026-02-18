package com.voicedeutsch.master.voicecore.context

/**
 * Строитель контекста для использования в промптах и запросах к LLM.
 * Собирает информацию о пользователе, сессии, предыдущих взаимодействиях и др.
 */
class ContextBuilder {
    
    private val contextMap = mutableMapOf<String, Any>()
    
    /**
     * Добавляет информацию о пользователе
     */
    fun withUserInfo(userId: String, userName: String? = null, level: String = "beginner"): ContextBuilder {
        contextMap["user_id"] = userId
        userName?.let { contextMap["user_name"] = it }
        contextMap["user_level"] = level
        return this
    }
    
    /**
     * Добавляет историю конверсации
     */
    fun withConversationHistory(history: List<String>): ContextBuilder {
        contextMap["conversation_history"] = history
        return this
    }
    
    /**
     * Добавляет языковые параметры
     */
    fun withLanguageSettings(
        targetLanguage: String = "de",
        nativeLanguage: String = "ru",
        dialect: String? = null
    ): ContextBuilder {
        contextMap["target_language"] = targetLanguage
        contextMap["native_language"] = nativeLanguage
        dialect?.let { contextMap["dialect"] = it }
        return this
    }
    
    /**
     * Добавляет тему урока или контекст обучения
     */
    fun withLessonContext(topic: String, subtopic: String? = null, difficulty: String = "A1"): ContextBuilder {
        contextMap["topic"] = topic
        subtopic?.let { contextMap["subtopic"] = it }
        contextMap["difficulty"] = difficulty
        return this
    }
    
    /**
     * Добавляет временной контекст
     */
    fun withTimeContext(timeOfDay: String = "morning", dayOfWeek: String? = null): ContextBuilder {
        contextMap["time_of_day"] = timeOfDay
        dayOfWeek?.let { contextMap["day_of_week"] = it }
        return this
    }
    
    /**
     * Добавляет произвольный контекст
     */
    fun addContext(key: String, value: Any): ContextBuilder {
        contextMap[key] = value
        return this
    }
    
    /**
     * Получает сформированный контекст
     */
    fun build(): Map<String, Any> {
        return contextMap.toMap()
    }
    
    /**
     * Получает контекст в виде строки для вставки в промпт
     */
    fun buildAsString(): String {
        return contextMap.entries
            .joinToString("\n") { (key, value) ->
                "$key: $value"
            }
    }
}

/**
 * Класс для управления контекстом сессии
 */
data class SessionContext(
    val userId: String? = null,
    val sessionId: String,
    val language: String = "de",
    val nativeLanguage: String = "ru",
    val topic: String? = null,
    val userLevel: String = "beginner",
    val metadata: Map<String, String> = emptyMap()
) {
    fun toContextMap(): Map<String, Any> {
        return mapOf(
            "session_id" to sessionId,
            "user_id" to (userId ?: "anonymous"),
            "language" to language,
            "native_language" to nativeLanguage,
            "topic" to (topic ?: "general"),
            "user_level" to userLevel
        ) + metadata
    }
}
