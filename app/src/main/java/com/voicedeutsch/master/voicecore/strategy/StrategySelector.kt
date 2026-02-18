package com.voicedeutsch.master.voicecore.strategy

/**
 * Интерфейс для стратегии взаимодействия с пользователем
 */
interface InteractionStrategy {
    val name: String
    val description: String
    
    /**
     * Выполняет стратегию
     */
    suspend fun execute(context: StrategyContext): StrategyResult
    
    /**
     * Проверяет, подходит ли стратегия для данного контекста
     */
    fun isApplicable(context: StrategyContext): Boolean
}

/**
 * Контекст для выполнения стратегии
 */
data class StrategyContext(
    val userInput: String,
    val userLevel: String,
    val topic: String? = null,
    val conversationTurns: Int = 0,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Результат выполнения стратегии
 */
data class StrategyResult(
    val response: String,
    val nextAction: String,
    val shouldContinue: Boolean = true,
    val confidence: Float = 1.0f,
    val executionTimeMs: Long = 0
)

/**
 * Селектор стратегий для выбора оптимальной стратегии
 */
class StrategySelector {
    private val strategies = mutableListOf<InteractionStrategy>()
    private var defaultStrategy: InteractionStrategy? = null
    
    /**
     * Регистрирует стратегию
     */
    fun registerStrategy(strategy: InteractionStrategy): StrategySelector {
        strategies.add(strategy)
        return this
    }
    
    /**
     * Устанавливает стратегию по умолчанию
     */
    fun setDefaultStrategy(strategy: InteractionStrategy): StrategySelector {
        defaultStrategy = strategy
        return this
    }
    
    /**
     * Выбирает подходящую стратегию для контекста
     */
    fun selectStrategy(context: StrategyContext): InteractionStrategy? {
        // Сортируем стратегии по приоритету (сначала проверяем те, которые зарегистрированы первыми)
        return strategies.firstOrNull { it.isApplicable(context) }
            ?: defaultStrategy
    }
    
    /**
     * Выполняет выбранную стратегию
     */
    suspend fun executeStrategy(context: StrategyContext): Result<StrategyResult> {
        return try {
            val strategy = selectStrategy(context)
                ?: return Result.failure(IllegalStateException("No applicable strategy found"))
            
            val startTime = System.currentTimeMillis()
            val result = strategy.execute(context)
            val executionTime = System.currentTimeMillis() - startTime
            
            Result.success(result.copy(executionTimeMs = executionTime))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Получает список всех стратегий
     */
    fun getAllStrategies(): List<InteractionStrategy> {
        return strategies.toList()
    }
    
    /**
     * Получает количество стратегий
     */
    fun getStrategyCount(): Int {
        return strategies.size
    }
}

/**
 * Встроенные стратегии
 */
object BuiltInStrategies {
    
    /**
     * Стратегия задавания вопросов для проверки понимания
     */
    class QuestioningStrategy : InteractionStrategy {
        override val name = "questioning"
        override val description = "Asks probing questions to check understanding"
        
        override suspend fun execute(context: StrategyContext): StrategyResult {
            return StrategyResult(
                response = "Можешь объяснить это своими словами?",
                nextAction = "wait_for_response"
            )
        }
        
        override fun isApplicable(context: StrategyContext): Boolean {
            return context.conversationTurns % 3 == 0 && context.userLevel == "beginner"
        }
    }
    
    /**
     * Стратегия исправления ошибок
     */
    class CorrectionStrategy : InteractionStrategy {
        override val name = "correction"
        override val description = "Corrects user errors gently and provides explanations"
        
        override suspend fun execute(context: StrategyContext): StrategyResult {
            val userInput = context.userInput
            return StrategyResult(
                response = "Маленькое уточнение: правильнее сказать...",
                nextAction = "repeat_and_continue"
            )
        }
        
        override fun isApplicable(context: StrategyContext): Boolean {
            return context.userInput.length > 10
        }
    }
    
    /**
     * Стратегия диалога
     */
    class DialogueStrategy : InteractionStrategy {
        override val name = "dialogue"
        override val description = "Engages in natural conversation"
        
        override suspend fun execute(context: StrategyContext): StrategyResult {
            return StrategyResult(
                response = "Интересно! Расскажи мне больше.",
                nextAction = "continue_dialogue"
            )
        }
        
        override fun isApplicable(context: StrategyContext): Boolean {
            return true // Default strategy
        }
    }
    
    /**
     * Стратегия обучения новому материалу
     */
    class TeachingStrategy : InteractionStrategy {
        override val name = "teaching"
        override val description = "Introduces and explains new material"
        
        override suspend fun execute(context: StrategyContext): StrategyResult {
            return StrategyResult(
                response = "Давайте выучим новый материал...",
                nextAction = "teach_and_practice"
            )
        }
        
        override fun isApplicable(context: StrategyContext): Boolean {
            return context.conversationTurns == 0 || context.topic != null
        }
    }
    
    /**
     * Стратегия практики
     */
    class PracticeStrategy : InteractionStrategy {
        override val name = "practice"
        override val description = "Provides exercises and practical examples"
        
        override suspend fun execute(context: StrategyContext): StrategyResult {
            return StrategyResult(
                response = "Давайте потренируемся! Попробуй сказать...",
                nextAction = "wait_for_practice_response"
            )
        }
        
        override fun isApplicable(context: StrategyContext): Boolean {
            return context.conversationTurns > 2
        }
    }
}
