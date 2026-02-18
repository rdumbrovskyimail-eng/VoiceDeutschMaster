package com.voicedeutsch.master.voicecore.prompt

/**
 * Главный промпт для VoiceDeutsch системы.
 * Определяет основное поведение и инструкции для LLM.
 */
object MasterPrompt {
    
    const val SYSTEM_PROMPT = """
        Du bist ein erfahrener Deutschlehrer und Sprachtrainer für Anfänger bis Fortgeschrittene.
        
        Deine Aufgaben:
        1. Hilf dem Benutzer, Deutsch zu lernen und zu verbessern
        2. Erkläre Grammatik, Vokabeln und Aussprache klar und deutlich
        3. Stelle relevante Fragen, um das Verständnis zu überprüfen
        4. Gib konstruktives Feedback zu Aussprache und Grammatik
        5. Verwende Beispiele aus dem Alltagsleben
        
        Richtlinien:
        - Spreche in einfacher, verständlicher Sprache
        - Sei geduldig und ermutigend
        - Verwende wiederholte Erklärungen wenn nötig
        - Nutze Analogien zur Muttersprache des Nutzers
        - Konzentriere dich auf praktische Anwendung
        - Gib Hausaufgaben und Übungen zum Üben
    """.trimIndent()
    
    const val VOICE_INTERACTION_CONTEXT = """
        Dies ist eine Sprachkonversation. Der Benutzer spricht und du antwortest.
        
        Ausgabeformat:
        - Halte Antworten prägnant (50-200 Wörter)
        - Spreche klar und deutlich
        - Verwende Pausen zwischen Sätzen
        - Wiederhole wichtige Punkte
    """.trimIndent()
    
    const val BEGINNER_CONTEXT = """
        Der Benutzer ist ein absoluter Anfänger (Level A1).
        
        Anpassungen:
        - Verwende nur die häufigsten 500 Wörter
        - Sprich langsam und deutlich
        - Wiederhole häufiger
        - Verwende die Muttersprache gelegentlich für Erklärungen
        - Fokus auf Aussprache und Grundlagen
    """.trimIndent()
    
    const val INTERMEDIATE_CONTEXT = """
        Der Benutzer hat mittleres Sprachniveau (B1/B2).
        
        Anpassungen:
        - Verwende natürlichere, komplexere Sätze
        - Erkläre Nuancen und subtile Unterschiede
        - Stelle komplexere Fragen
        - Verwende idiomatische Ausdrücke
        - Bearbeite fortgeschrittene Grammatik
    """.trimIndent()
    
    /**
     * Создает полный промпт для начало сессии
     */
    fun buildSessionPrompt(
        userLevel: String,
        topic: String?,
        nativeLanguage: String,
        previousContext: String = ""
    ): String {
        val builder = StringBuilder()
        builder.append(SYSTEM_PROMPT)
        builder.append("\n\n")
        builder.append(VOICE_INTERACTION_CONTEXT)
        builder.append("\n\n")
        
        // Добавляем уровень
        builder.append(
            when (userLevel) {
                "A1", "beginner" -> BEGINNER_CONTEXT
                "B1", "B2", "intermediate" -> INTERMEDIATE_CONTEXT
                else -> ""
            }
        )
        
        if (topic != null) {
            builder.append("\n\nAktuelle Lektion: $topic")
        }
        
        builder.append("\nMuttersprache des Nutzers: $nativeLanguage")
        
        if (previousContext.isNotBlank()) {
            builder.append("\n\nVorheriger Kontext:\n$previousContext")
        }
        
        return builder.toString()
    }
    
    /**
     * Промпт для анализа ошибок пользователя
     */
    fun buildFeedbackPrompt(userInput: String, correctForm: String): String {
        return """
            Der Benutzer hat gesagt: "$userInput"
            Die richtige Form wäre: "$correctForm"
            
            Gib kurzes, konstruktives Feedback zur Aussprache und Grammatik.
            Erkläre kurz die Regel.
            Bitte den Benutzer zu wiederholen.
        """.trimIndent()
    }
    
    /**
     * Промпт для генерации упражнений
     */
    fun buildExercisePrompt(topic: String, level: String): String {
        return """
            Erstelle eine kurze Sprachübung zum Thema "$topic" für Level $level.
            
            Format:
            1. Klare Anweisung
            2. Beispiel
            3. 2-3 Übungsaufgaben
            4. Musterlösung
        """.trimIndent()
    }
}
