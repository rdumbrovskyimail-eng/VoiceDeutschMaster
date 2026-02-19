package com.voicedeutsch.master.voicecore.prompt

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot

/**
 * Strategy-specific prompt fragments and Gemini function declarations.
 * Architecture lines 1295-1400 (strategy prompts), 1540-1720 (function declarations).
 *
 * Each strategy prompt is appended to the master prompt to give Gemini
 * specific operating instructions for the current learning mode.
 */
object PromptTemplates {

    // ── Strategy prompts ──────────────────────────────────────────────────────

    fun getStrategyPrompt(strategy: LearningStrategy, snapshot: KnowledgeSnapshot): String =
        when (strategy) {
            LearningStrategy.REPETITION -> buildRepetitionPrompt(snapshot)
            LearningStrategy.LINEAR_BOOK -> buildLinearBookPrompt(snapshot)
            LearningStrategy.FREE_PRACTICE -> buildFreePracticePrompt(snapshot)
            LearningStrategy.PRONUNCIATION -> buildPronunciationPrompt(snapshot)
            LearningStrategy.GAP_FILLING -> buildGapFillingPrompt(snapshot)
            LearningStrategy.GRAMMAR_DRILL -> buildGrammarDrillPrompt(snapshot)
            LearningStrategy.VOCABULARY_BOOST -> buildVocabularyBoostPrompt(snapshot)
            LearningStrategy.LISTENING -> buildListeningPrompt(snapshot)
            LearningStrategy.ASSESSMENT -> buildAssessmentPrompt(snapshot)
        }

    private fun buildRepetitionPrompt(snapshot: KnowledgeSnapshot): String = """
        ТЕКУЩАЯ СТРАТЕГИЯ: REPETITION (Интервальное повторение)
        
        Задача: повторить слова и правила по системе SRS.
        Очередь на сегодня: ${snapshot.vocabulary.wordsForReviewToday} слов,
        ${snapshot.grammar.rulesForReviewToday} правил.
        
        Инструкции:
        1. Вызови get_words_for_repetition(limit=30) для получения актуального списка
        2. Спрашивай слова в случайном порядке — НЕ по алфавиту
        3. Для каждого слова: назови русский перевод, жди немецкого слова от пользователя
        4. Или наоборот: назови немецкое слово — жди перевод
        5. Оценивай ответ по шкале 0-5 и сразу вызывай save_word_knowledge
        6. Проблемные слова (уровень 0-2) повторяй через 2-3 задания
        7. После каждых 10 слов: краткий отчёт "10 слов готово, продолжаем!"
        8. По завершении очереди: переключись на LINEAR_BOOK или FREE_PRACTICE
        
        Запрещено:
        - Давать подсказки до того, как пользователь ответил
        - Пропускать оценку качества ответа
        - Спрашивать слова, которых нет в очереди повторения
    """.trimIndent()

    private fun buildLinearBookPrompt(snapshot: KnowledgeSnapshot): String = """
        ТЕКУЩАЯ СТРАТЕГИЯ: LINEAR_BOOK (Прохождение книги)
        
        Текущий прогресс: глава ${snapshot.bookProgress.currentChapter},
        урок ${snapshot.bookProgress.currentLesson}.
        Завершено: ${snapshot.bookProgress.completionPercentage.toInt()}% книги.
        Текущая тема: ${snapshot.bookProgress.currentTopic}
        
        Инструкции:
        1. Вызови get_current_lesson() для получения свежего контекста урока
        2. Прочитай заголовок и кратко объясни, о чём этот урок
        3. Читай текст ПО ПРЕДЛОЖЕНИЯМ — паузируй после каждого
        4. После каждого предложения задавай 1 вопрос ИЛИ проси перевести 1 слово
        5. Новые слова: произноси, переводи, давай пример, вызывай save_word_knowledge
        6. Новые правила: объясни коротко, дай 2 примера, вызывай save_rule_knowledge
        7. По завершении урока: вызови mark_lesson_complete, подведи итог
        8. Спроси: "Переходим к следующему уроку?" — если да, advance_to_next_lesson
        
        Темп: 1 урок за 20-30 минут. Не торопись, но и не затягивай.
    """.trimIndent()

    private fun buildFreePracticePrompt(snapshot: KnowledgeSnapshot): String {
        val level = snapshot.recommendations.primaryStrategy
        return """
        ТЕКУЩАЯ СТРАТЕГИЯ: FREE_PRACTICE (Свободная практика)
        
        Задача: живой разговор на немецком с ненавязчивой коррекцией.
        Известные темы пользователя: ${snapshot.bookProgress.currentTopic}
        Уровень: определяется по контексту
        
        Инструкции:
        1. Выбери тему из жизни: работа, хобби, путешествия, кино, еда
        2. Задавай ОТКРЫТЫЕ вопросы, которые требуют развёрнутого ответа
        3. Исправляй ошибки НЕНАВЯЗЧИВО — повтори фразу правильно в своём ответе
        4. Если пользователь говорит по-русски — мягко переключай на немецкий:
           "Попробуй сказать это по-немецки. Как будет 'я думаю что'?"
        5. Вводи новые слова ОРГАНИЧНО в разговор
        6. Каждые 5-7 минут: вызывай save_word_knowledge для слов, прозвучавших в разговоре
        7. НЕ превращай разговор в урок — это должно быть приятно
        
        Примеры вопросов для начала:
        - "Was hast du gestern gemacht?" (A2-B1)
        - "Beschreib mir deinen typischen Tag." (B1)
        - "Was denkst du über Fernarbeit?" (B2)
        """.trimIndent()
    }

    private fun buildPronunciationPrompt(snapshot: KnowledgeSnapshot): String {
        val problemSounds = snapshot.pronunciation.problemSounds.take(5).joinToString(", ")
        return """
        ТЕКУЩАЯ СТРАТЕГИЯ: PRONUNCIATION (Работа над произношением)
        
        Проблемные звуки пользователя: $problemSounds
        Общий балл произношения: ${snapshot.pronunciation.overallScore}
        Тренд: ${snapshot.pronunciation.trend}
        
        Инструкции:
        1. Начни с самого проблемного звука из списка
        2. ИЗОЛИРОВАННЫЙ ЗВУК: объясни артикуляцию, произнеси, попроси повторить 3 раза
        3. СЛОВО: включи звук в слово, попроси повторить
        4. ФРАЗА: дай короткую фразу с этим звуком
        5. После каждой попытки: оцени 0.0-1.0 и вызови save_pronunciation_result
        6. Хвали за улучшения: "Стало лучше! Теперь попробуй быстрее."
        7. Через 10 минут переходи к следующему проблемному звуку
        
        Специфика немецких звуков:
        - ü [y]: как русский "и", но с округлёнными губами ("Ü-bung")
        - ö [ø]: как русский "э", но с округлёнными губами ("schön")
        - ch: после a/o/u — [x] как "х", после i/e/ü/ö — [ç] мягче
        - r: в начале слова — вибрирующий [r], в конце — вокализованный
        - w: всегда [v], никогда не [w]
        - z: всегда [ts]
        - sp/st в начале слова: [ʃp]/[ʃt] (шп/шт)
        """.trimIndent()
    }

    private fun buildGapFillingPrompt(snapshot: KnowledgeSnapshot): String {
        val weakAreas = snapshot.weakPoints.take(5).joinToString("\n- ", prefix = "- ")
        return """
        ТЕКУЩАЯ СТРАТЕГИЯ: GAP_FILLING (Заполнение пробелов)
        
        Слабые места пользователя:
        $weakAreas
        
        Инструкции:
        1. Вызови get_weak_points() для получения актуального списка слабых мест
        2. Начни с ПЕРВОГО пункта в списке — это самое приоритетное
        3. Не переходи к следующему пункту, пока текущий не закреплён (3 правильных ответа подряд)
        4. Используй разные форматы упражнений для одного и того же материала:
           - Перевод с русского на немецкий
           - Перевод с немецкого на русский
           - Подстановка в предложение
           - Составление собственного предложения
        5. После 3 правильных ответов: вызови save_word_knowledge или save_rule_knowledge с quality=5
        6. Отмечай прогресс: "Отлично! С этим правилом ты теперь разобрался."
        """.trimIndent()
    }

    private fun buildGrammarDrillPrompt(snapshot: KnowledgeSnapshot): String {
        val problemRules = snapshot.grammar.problemRules.take(3).joinToString(", ")
        return """
        ТЕКУЩАЯ СТРАТЕГИЯ: GRAMMAR_DRILL (Грамматический штурм)
        
        Проблемные правила: $problemRules
        Всего правил: ${snapshot.grammar.totalRules}, проблемных: ${snapshot.grammar.problemRules.size}
        
        Инструкции:
        1. Выбери одно правило для глубокой проработки
        2. Объясни правило за 2-3 предложения + 2 примера
        3. Дай 5-7 упражнений на применение правила
        4. Упражнения должны становиться сложнее (от заполнения пропусков до свободного составления)
        5. После каждого упражнения: сразу давай feedback
        6. После 5 правильных ответов подряд: вызови save_rule_knowledge с quality=5
        7. Затем переходи к следующему проблемному правилу
        
        Важно: не объясняй правило через исключения — сначала общий принцип,
        потом исключения только если пользователь с ними столкнулся.
        """.trimIndent()
    }

    private fun buildVocabularyBoostPrompt(snapshot: KnowledgeSnapshot): String {
        val currentTopic = snapshot.bookProgress.currentTopic
        return """
        ТЕКУЩАЯ СТРАТЕГИЯ: VOCABULARY_BOOST (Словарный рывок)
        
        Текущая тема книги: $currentTopic
        Всего слов в базе: ${snapshot.vocabulary.totalWords}
        Слов уровня 0-2 (плохо знает): ${(snapshot.vocabulary.byLevel[0] ?: 0) + (snapshot.vocabulary.byLevel[1] ?: 0) + (snapshot.vocabulary.byLevel[2] ?: 0)}
        
        Инструкции:
        1. Выбери тематическую группу слов, связанную с текущей темой книги
        2. Вводи слова блоками по 5-7 штук
        3. Для каждого слова:
           - Произнеси правильно, выдели ударение
           - Дай перевод
           - Дай пример употребления в предложении
           - Попроси составить своё предложение
        4. Используй мнемонические техники:
           - Ассоциации: "Haus = дом, представь немецкий домик с крышей-H"
           - Похожие слова: "Wasser = water = вода"
           - Истории: создай короткую историю с несколькими новыми словами
        5. После введения блока: проверь все 5-7 слов
        6. Вызывай save_word_knowledge после каждой проверки
        """.trimIndent()
    }

    private fun buildListeningPrompt(snapshot: KnowledgeSnapshot): String = """
        ТЕКУЩАЯ СТРАТЕГИЯ: LISTENING (Аудирование)
        
        Инструкции:
        1. Выбери SHORT текст (3-5 предложений) на немецком, соответствующий уровню пользователя
        2. Предупреди: "Я прочитаю текст ОДИН РАЗ. Слушай внимательно."
        3. Прочитай текст в НОРМАЛЬНОМ темпе (не замедленно)
        4. Задай 2-3 вопроса на понимание:
           - Фактические: "Кто? Что? Где? Когда?"
           - Детальные: "Какого цвета был...?"
        5. Если пользователь не понял — прочитай ещё раз, затем разбери по предложениям
        6. Отмечай правильно понятые детали: "Ты правильно услышал X и Y"
        
        Уровень сложности:
        - A2: простые предложения, знакомая лексика, чёткое произношение
        - B1: разговорный темп, некоторые новые слова, понятные из контекста
        - B2+: нормальный темп, идиоматические выражения
        
        Пример текста для A2:
        "Anna geht heute in den Supermarkt. Sie kauft Brot, Milch und Äpfel.
         Die Äpfel sind sehr frisch und kosten 2 Euro. Anna ist zufrieden."
    """.trimIndent()

    private fun buildAssessmentPrompt(snapshot: KnowledgeSnapshot): String = """
        ТЕКУЩАЯ СТРАТЕГИЯ: ASSESSMENT (Оценка уровня)
        
        Текущий уровень по данным системы: ${snapshot.recommendations.primaryStrategy}
        
        Инструкции:
        1. Проведи БЫСТРУЮ оценку (10-15 минут) для определения актуального уровня
        2. Начни с A1 уровня — быстро повышай если пользователь отвечает правильно
        3. Тестируй последовательно: лексика → грамматика → говорение → аудирование
        4. Каждый уровень: 3-5 вопросов, если 80%+ правильно — переходи выше
        5. Когда нашёл "потолок" — зафиксируй уровень
        6. Вызови update_user_level с определённым уровнем и подуровнем
        7. Дай пользователю честный фидбек об уровне
        
        После оценки: переключись на подходящую стратегию для найденного уровня.
    """.trimIndent()

    // ── Function declarations JSON ────────────────────────────────────────────

    /**
     * Returns the JSON array of Gemini function declarations.
     * Architecture lines 1540-1720 (Appendix B — full function declarations).
     *
     * These declarations are sent to Gemini Live API as the `tools` configuration,
     * enabling it to call back into the application during the session.
     */
    fun getFunctionDeclarationsJson(): String = FUNCTION_DECLARATIONS_JSON

    private val FUNCTION_DECLARATIONS_JSON = """
    [
      {
        "name": "save_word_knowledge",
        "description": "Сохранить результат упражнения со словом. Вызывается после КАЖДОГО упражнения с немецким словом.",
        "parameters": {
          "type": "object",
          "properties": {
            "word": {
              "type": "string",
              "description": "Немецкое слово (в базовой форме, например 'das Haus')"
            },
            "translation": {
              "type": "string",
              "description": "Русский перевод слова"
            },
            "level": {
              "type": "integer",
              "description": "Новый уровень знания 0-7 (0=незнакомо, 4=хорошо, 7=отлично)"
            },
            "quality": {
              "type": "integer",
              "description": "Качество ответа по шкале 0-5 (5=идеально, 0=не знает)"
            },
            "pronunciation_score": {
              "type": "number",
              "description": "Оценка произношения 0.0-1.0 (необязательно)"
            },
            "context": {
              "type": "string",
              "description": "Предложение-контекст, в котором встретилось слово (необязательно)"
            }
          },
          "required": ["word", "translation", "level", "quality"]
        }
      },
      {
        "name": "save_rule_knowledge",
        "description": "Сохранить результат упражнения с грамматическим правилом.",
        "parameters": {
          "type": "object",
          "properties": {
            "rule_id": {
              "type": "string",
              "description": "Идентификатор правила из базы данных"
            },
            "level": {
              "type": "integer",
              "description": "Новый уровень знания 0-7"
            },
            "quality": {
              "type": "integer",
              "description": "Качество ответа по шкале 0-5"
            }
          },
          "required": ["rule_id", "level", "quality"]
        }
      },
      {
        "name": "record_mistake",
        "description": "Записать ошибку пользователя для последующего анализа и повторения.",
        "parameters": {
          "type": "object",
          "properties": {
            "type": {
              "type": "string",
              "enum": ["word", "grammar", "pronunciation", "phrase"],
              "description": "Тип ошибки"
            },
            "item": {
              "type": "string",
              "description": "Элемент, в котором была допущена ошибка (слово или правило)"
            },
            "expected": {
              "type": "string",
              "description": "Правильный ответ"
            },
            "actual": {
              "type": "string",
              "description": "Что сказал или написал пользователь"
            },
            "explanation": {
              "type": "string",
              "description": "Краткое объяснение правильного варианта"
            }
          },
          "required": ["type", "item", "expected", "actual"]
        }
      },
      {
        "name": "get_words_for_repetition",
        "description": "Получить список слов для повторения по системе SRS на сегодня.",
        "parameters": {
          "type": "object",
          "properties": {
            "limit": {
              "type": "integer",
              "description": "Максимальное количество слов в очереди (рекомендуется 20-30)"
            }
          }
        }
      },
      {
        "name": "get_current_lesson",
        "description": "Получить полное содержимое текущего урока книги (текст, упражнения, словарь).",
        "parameters": {
          "type": "object",
          "properties": {}
        }
      },
      {
        "name": "advance_to_next_lesson",
        "description": "Перейти к следующему уроку в книге после завершения текущего.",
        "parameters": {
          "type": "object",
          "properties": {
            "current_lesson_score": {
              "type": "number",
              "description": "Итоговая оценка за текущий урок от 0.0 до 1.0"
            }
          },
          "required": ["current_lesson_score"]
        }
      },
      {
        "name": "mark_lesson_complete",
        "description": "Отметить урок как завершённый и сохранить оценку.",
        "parameters": {
          "type": "object",
          "properties": {
            "chapter": {
              "type": "integer",
              "description": "Номер главы"
            },
            "lesson": {
              "type": "integer",
              "description": "Номер урока в главе"
            },
            "score": {
              "type": "number",
              "description": "Итоговая оценка за урок от 0.0 до 1.0"
            }
          },
          "required": ["chapter", "lesson", "score"]
        }
      },
      {
        "name": "save_pronunciation_result",
        "description": "Сохранить результат оценки произношения слова или фразы.",
        "parameters": {
          "type": "object",
          "properties": {
            "word": {
              "type": "string",
              "description": "Слово или фраза, произношение которой оценивалось"
            },
            "score": {
              "type": "number",
              "description": "Оценка произношения от 0.0 (плохо) до 1.0 (отлично)"
            },
            "problem_sounds": {
              "type": "array",
              "items": {"type": "string"},
              "description": "Список проблемных звуков (например ['ü', 'ch', 'r'])"
            }
          },
          "required": ["word", "score"]
        }
      },
      {
        "name": "set_current_strategy",
        "description": "Установить текущую стратегию обучения. Вызывается при каждой смене стратегии.",
        "parameters": {
          "type": "object",
          "properties": {
            "strategy": {
              "type": "string",
              "enum": [
                "LINEAR_BOOK",
                "GAP_FILLING",
                "REPETITION",
                "FREE_PRACTICE",
                "PRONUNCIATION",
                "GRAMMAR_DRILL",
                "VOCABULARY_BOOST",
                "LISTENING",
                "ASSESSMENT"
              ],
              "description": "Выбранная стратегия обучения"
            },
            "reason": {
              "type": "string",
              "description": "Краткое обоснование выбора стратегии"
            }
          },
          "required": ["strategy"]
        }
      },
      {
        "name": "update_user_level",
        "description": "Обновить CEFR уровень пользователя на основе наблюдений за сессию.",
        "parameters": {
          "type": "object",
          "properties": {
            "cefr_level": {
              "type": "string",
              "enum": ["A1", "A2", "B1", "B2", "C1", "C2"],
              "description": "Новый CEFR уровень"
            },
            "sub_level": {
              "type": "integer",
              "description": "Подуровень от 1 до 10 (1=только что начал уровень, 10=готов к следующему)"
            }
          },
          "required": ["cefr_level", "sub_level"]
        }
      },
      {
        "name": "get_user_statistics",
        "description": "Получить текущую статистику пользователя (серия дней, всего сессий, прогресс).",
        "parameters": {
          "type": "object",
          "properties": {}
        }
      },
      {
        "name": "log_session_event",
        "description": "Записать значимое событие сессии для аналитики и истории.",
        "parameters": {
          "type": "object",
          "properties": {
            "event_type": {
              "type": "string",
              "enum": [
                "WORD_LEARNED",
                "WORD_REVIEWED",
                "RULE_PRACTICED",
                "PRONUNCIATION_ATTEMPT",
                "STRATEGY_CHANGE",
                "LESSON_STARTED",
                "LESSON_COMPLETED",
                "MISTAKE_MADE",
                "MILESTONE_REACHED",
                "SESSION_START",
                "SESSION_END"
              ],
              "description": "Тип события"
            },
            "details": {
              "type": "string",
              "description": "JSON-строка с дополнительными деталями события"
            }
          },
          "required": ["event_type"]
        }
      },
      {
        "name": "get_weak_points",
        "description": "Получить актуальный список слабых мест пользователя для стратегии GAP_FILLING.",
        "parameters": {
          "type": "object",
          "properties": {}
        }
      }
    ]
    """.trimIndent()
}
