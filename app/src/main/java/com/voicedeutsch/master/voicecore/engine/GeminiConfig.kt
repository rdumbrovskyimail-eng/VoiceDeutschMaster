package com.voicedeutsch.master.voicecore.engine

/**
 * Complete configuration for Gemini Live API connection.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * ИЗМЕНЕНИЯ (Live API Capabilities — полная реализация):
 * ════════════════════════════════════════════════════════════════════════════
 *   ДОБАВЛЕНО: vadConfig, sessionResumption, contextWindowCompression,
 *              transcriptionConfig, affectiveDialog, proactiveAudio,
 *              thinkingBudget, enableSearchGrounding
 */
data class GeminiConfig(
    val modelName: String = MODEL_GEMINI_LIVE,
    val audioInputFormat: AudioFormat = AudioFormat.PCM_16KHZ_16BIT_MONO,
    val audioOutputFormat: AudioFormat = AudioFormat.PCM_24KHZ_16BIT_MONO,
    val streamingEnabled: Boolean = true,
    val maxContextTokens: Int = MAX_CONTEXT_TOKENS,
    val temperature: Float = DEFAULT_TEMPERATURE,
    val topP: Float = DEFAULT_TOP_P,
    val topK: Int = DEFAULT_TOP_K,
    val reconnectMaxAttempts: Int = DEFAULT_RECONNECT_ATTEMPTS,
    val reconnectDelayMs: Long = DEFAULT_RECONNECT_DELAY_MS,
    val voiceName: String = DEFAULT_VOICE,

    // ── Live API Session Management ──────────────────────────────────────
    /** Включить сжатие контекстного окна (sliding window) для неограниченных сессий */
    val contextWindowCompression: Boolean = true,

    /** Включить возобновление сессии при разрыве WebSocket */
    val sessionResumptionEnabled: Boolean = true,

    // ── Voice Activity Detection ─────────────────────────────────────────
    val vadConfig: VadConfig = VadConfig(),

    // ── Audio Transcription ──────────────────────────────────────────────
    val transcriptionConfig: TranscriptionConfig = TranscriptionConfig(),

    // ── Native Audio Features ────────────────────────────────────────────
    /** Эмоциональный диалог — модель адаптирует тон к эмоциям пользователя */
    val affectiveDialogEnabled: Boolean = true,

    /** Проактивное аудио — модель решает, когда отвечать, а когда молчать */
    val proactiveAudioEnabled: Boolean = false,

    // ── Thinking ─────────────────────────────────────────────────────────
    /** Бюджет токенов мышления. 0 = отключить, null = модель решает сама */
    val thinkingBudget: Int? = null,

    /** Включить краткие обзоры мыслей в ответы */
    val includeThoughts: Boolean = false,

    // ── Grounding ────────────────────────────────────────────────────────
    /** Включить Google Search для проверки фактов */
    val enableSearchGrounding: Boolean = false,

    // ── Async Function Calling ───────────────────────────────────────────
    /** Режим вызова функций: AUTO, ANY, NONE, VALIDATED */
    val functionCallingMode: FunctionCallingMode = FunctionCallingMode.AUTO,
) {
    init {
        require(temperature in 0f..2f) { "temperature must be in [0, 2]" }
        require(topP in 0f..1f) { "topP must be in [0, 1]" }
        require(topK > 0) { "topK must be positive" }
        require(reconnectMaxAttempts > 0) { "reconnectMaxAttempts must be positive" }
        require(thinkingBudget == null || thinkingBudget >= 0) { "thinkingBudget must be >= 0 or null" }
    }

    /** Конфигурация VAD (Voice Activity Detection) */
    data class VadConfig(
        /** Отключить автоматический VAD (ручное управление activityStart/activityEnd) */
        val disabled: Boolean = false,
        /** Чувствительность начала речи: LOW или HIGH */
        val startSensitivity: Sensitivity = Sensitivity.DEFAULT,
        /** Чувствительность конца речи: LOW или HIGH */
        val endSensitivity: Sensitivity = Sensitivity.DEFAULT,
        /** Padding до начала речи (мс) */
        val prefixPaddingMs: Int = 20,
        /** Длительность тишины для определения конца речи (мс) */
        val silenceDurationMs: Int = 100,
    ) {
        enum class Sensitivity { DEFAULT, LOW, HIGH }
    }

    /** Конфигурация транскрипции аудио */
    data class TranscriptionConfig(
        /** Транскрипция входящего аудио (речь пользователя) */
        val inputTranscriptionEnabled: Boolean = true,
        /** Транскрипция исходящего аудио (речь модели) */
        val outputTranscriptionEnabled: Boolean = true,
    )

    /** Режим вызова функций */
    enum class FunctionCallingMode {
        /** Модель сама решает, вызывать функцию или отвечать текстом */
        AUTO,
        /** Модель обязана вызвать функцию */
        ANY,
        /** Модель НЕ может вызывать функции */
        NONE,
        /** Как AUTO, но с валидацией схемы */
        VALIDATED,
    }

    enum class AudioFormat(
        val sampleRateHz: Int,
        val bitsPerSample: Int,
        val channels: Int,
    ) {
        PCM_16KHZ_16BIT_MONO(sampleRateHz = 16_000, bitsPerSample = 16, channels = 1),
        PCM_24KHZ_16BIT_MONO(sampleRateHz = 24_000, bitsPerSample = 16, channels = 1),
    }

    companion object {
        const val MODEL_GEMINI_LIVE = "gemini-2.5-flash-native-audio-preview-12-2025"
        const val MAX_CONTEXT_TOKENS = 131_072
        const val DEFAULT_TEMPERATURE = 0.5f
        const val DEFAULT_TOP_P = 0.95f
        const val DEFAULT_TOP_K = 40
        const val DEFAULT_RECONNECT_ATTEMPTS = 3
        const val DEFAULT_RECONNECT_DELAY_MS = 2_000L
        const val DEFAULT_VOICE = "Kore"
    }
}