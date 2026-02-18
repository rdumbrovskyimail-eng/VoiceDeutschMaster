package com.voicedeutsch.master.voicecore.functions

/**
 * Интерфейс для функции, которая может быть вызвана LLM
 */
interface LLMFunction {
    val name: String
    val description: String
    val parameters: Map<String, String>
    
    suspend fun execute(params: Map<String, Any>): String
}

/**
 * Маршрутизатор функций для обработки вызовов функций от LLM
 */
class FunctionRouter {
    private val functions = mutableMapOf<String, LLMFunction>()
    
    /**
     * Регистрирует функцию
     */
    fun registerFunction(function: LLMFunction): FunctionRouter {
        functions[function.name] = function
        return this
    }
    
    /**
     * Вызывает функцию по названию
     */
    suspend fun callFunction(name: String, params: Map<String, Any>): Result<String> {
        return try {
            val function = functions[name]
                ?: return Result.failure(IllegalArgumentException("Function '$name' not found"))
            
            val result = function.execute(params)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Получает список всех зарегистрированных функций
     */
    fun getAvailableFunctions(): List<FunctionDefinition> {
        return functions.values.map { function ->
            FunctionDefinition(
                name = function.name,
                description = function.description,
                parameters = function.parameters
            )
        }
    }
    
    /**
     * Проверяет, существует ли функция
     */
    fun hasFunction(name: String): Boolean {
        return functions.containsKey(name)
    }
    
    /**
     * Удаляет функцию
     */
    fun unregisterFunction(name: String): FunctionRouter {
        functions.remove(name)
        return this
    }
    
    /**
     * Очищает все функции
     */
    fun clear(): FunctionRouter {
        functions.clear()
        return this
    }
}

/**
 * Определение функции для отправки в LLM
 */
data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, String>
)

/**
 * Результат вызова функции от LLM
 */
data class FunctionCall(
    val name: String,
    val parameters: Map<String, Any>,
    val id: String = ""
)

/**
 * Абстрактный базовый класс для функций
 */
abstract class BaseLLMFunction(
    override val name: String,
    override val description: String,
    override val parameters: Map<String, String> = emptyMap()
) : LLMFunction {
    
    override suspend fun execute(params: Map<String, Any>): String {
        validateParams(params)
        return executeInternal(params)
    }
    
    protected abstract suspend fun executeInternal(params: Map<String, Any>): String
    
    protected open fun validateParams(params: Map<String, Any>) {
        // Override if validation needed
    }
}

/**
 * Примеры встроенных функций
 */
object BuiltInFunctions {
    
    /**
     * Функция для получения текущего времени
     */
    class GetTime : BaseLLMFunction(
        name = "get_time",
        description = "Gets the current time",
        parameters = mapOf("timezone" to "string (optional)")
    ) {
        override suspend fun executeInternal(params: Map<String, Any>): String {
            return java.time.LocalDateTime.now().toString()
        }
    }
    
    /**
     * Функция для перевода текста
     */
    class Translate : BaseLLMFunction(
        name = "translate",
        description = "Translates text between languages",
        parameters = mapOf(
            "text" to "string (required)",
            "target_language" to "string (required)",
            "source_language" to "string (optional)"
        )
    ) {
        override suspend fun executeInternal(params: Map<String, Any>): String {
            val text = params["text"] as? String ?: return "Error: text parameter required"
            val targetLanguage = params["target_language"] as? String ?: return "Error: target_language parameter required"
            
            // TODO: Implement translation logic
            return "Translation of '$text' to $targetLanguage"
        }
        
        override fun validateParams(params: Map<String, Any>) {
            require(params.containsKey("text")) { "text parameter is required" }
            require(params.containsKey("target_language")) { "target_language parameter is required" }
        }
    }
    
    /**
     * Функция для поиска информации
     */
    class SearchInfo : BaseLLMFunction(
        name = "search_info",
        description = "Searches for information on a topic",
        parameters = mapOf(
            "query" to "string (required)",
            "language" to "string (optional)"
        )
    ) {
        override suspend fun executeInternal(params: Map<String, Any>): String {
            val query = params["query"] as? String ?: return "Error: query parameter required"
            
            // TODO: Implement search logic
            return "Search results for: $query"
        }
        
        override fun validateParams(params: Map<String, Any>) {
            require(params.containsKey("query")) { "query parameter is required" }
        }
    }
}
