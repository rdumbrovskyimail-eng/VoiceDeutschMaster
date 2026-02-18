package com.voicedeutsch.master.voicecore.prompt

/**
 * Шаблоны промптов для различных сценариев взаимодействия
 */
object PromptTemplates {
    
    /**
     * Шаблон для начала урока
     */
    fun lessonStart(topic: String, level: String): String {
        return """
            Начни урок на тему "$topic" для уровня $level.
            
            План:
            1. Приветствие и мотивация (1 предложение)
            2. Введение в тему (2-3 предложения)
            3. Первый вопрос или упражнение
            
            Язык: только немецкий
        """.trimIndent()
    }
    
    /**
     * Шаблон для исправления ошибок
     */
    fun correctionTemplate(userText: String, errorType: String): String {
        return """
            Пользователь сказал: "$userText"
            Тип ошибки: $errorType
            
            Задачи:
            1. Мягко исправить ошибку
            2. Объяснить правило на простом языке
            3. Дать пример правильного использования
            4. Попросить пользователя повторить
        """.trimIndent()
    }
    
    /**
     * Шаблон для оценки ответа
     */
    fun assessmentTemplate(userAnswer: String, question: String): String {
        return """
            Вопрос: $question
            Ответ пользователя: $userAnswer
            
            Оцени ответ:
            1. Правильность (Да/Нет/Частично)
            2. Качество (Отлично/Хорошо/Нужно улучшить)
            3. Краткое объяснение
            4. Подсказка при необходимости
        """.trimIndent()
    }
    
    /**
     * Шаблон для генерации диалога
     */
    fun dialogueTemplate(situation: String, level: String): String {
        return """
            Создай практический диалог:
            Ситуация: $situation
            Уровень: $level
            
            Формат:
            A: (ваша реплика)
            B: (ответ)
            
            Затем объясни ключевые выражения.
        """.trimIndent()
    }
    
    /**
     * Шаблон для объяснения грамматики
     */
    fun grammarTemplate(rule: String, level: String): String {
        return """
            Объясни грамматическое правило: $rule
            Уровень: $level
            
            План объяснения:
            1. Определение (1 предложение)
            2. Правило образования (2-3 предложения)
            3. Примеры (минимум 3)
            4. Исключения (если есть)
            5. Практическое упражнение (1-2 примера)
        """.trimIndent()
    }
    
    /**
     * Шаблон для объяснения вокабуляра
     */
    fun vocabularyTemplate(words: List<String>, context: String): String {
        val wordList = words.joinToString(", ")
        return """
            Объясни слова: $wordList
            Контекст: $context
            
            Для каждого слова:
            1. Произношение
            2. Перевод
            3. Пример в предложении
            4. Синонимы или антонимы
            5. Практическое использование
        """.trimIndent()
    }
    
    /**
     * Шаблон для проверки понимания
     */
    fun comprehensionCheckTemplate(text: String): String {
        return """
            Текст: "$text"
            
            Проверь понимание:
            1. Задай 2 вопроса на понимание
            2. Попроси пересказать своими словами
            3. Дай похвалу или исправление
            4. Перейди к следующей части
        """.trimIndent()
    }
    
    /**
     * Шаблон для совета по произношению
     */
    fun pronunciationTipTemplate(word: String, targetLanguage: String = "de"): String {
        return """
            Слово: $word (на $targetLanguage)
            
            Дай совет по произношению:
            1. Фонетическая запись
            2. Медленное произношение
            3. Быстрое произношение
            4. Типичные ошибки
            5. Совет для улучшения
        """.trimIndent()
    }
    
    /**
     * Шаблон для закрытия урока
     */
    fun lessonClosureTemplate(topicsCovered: List<String>): String {
        val topics = topicsCovered.joinToString(", ")
        return """
            Закрой урок по темам: $topics
            
            План закрытия:
            1. Резюме (что мы выучили)
            2. Домашнее задание
            3. Мотивирующий комментарий
            4. Прощание на немецком
        """.trimIndent()
    }
}

/**
 * Строитель динамических промптов
 */
class PromptBuilder {
    private val parts = mutableListOf<String>()
    
    fun addSystemContext(context: String) = apply {
        parts.add("СИСТЕМНЫЙ КОНТЕКСТ:\n$context")
    }
    
    fun addUserLevel(level: String) = apply {
        parts.add("Уровень пользователя: $level")
    }
    
    fun addTopic(topic: String) = apply {
        parts.add("Текущая тема: $topic")
    }
    
    fun addInstruction(instruction: String) = apply {
        parts.add("ИНСТРУКЦИИ:\n$instruction")
    }
    
    fun addContext(context: String) = apply {
        parts.add("КОНТЕКСТ:\n$context")
    }
    
    fun addExample(example: String) = apply {
        parts.add("ПРИМЕР:\n$example")
    }
    
    fun build(): String {
        return parts.joinToString("\n\n")
    }
}
