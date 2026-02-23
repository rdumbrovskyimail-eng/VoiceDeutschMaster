package com.voicedeutsch.master.util

/**
 * Global application constants — limits, intervals, default configuration values.
 *
 * All "magic numbers" of the application are centralized here.
 * Includes constants for SRS, audio, sessions, user preferences defaults,
 * threshold values, and CEFR levels.
 */
object Constants {

    // ==========================================
    // DATABASE
    // ==========================================
    const val DATABASE_NAME = "voice_deutsch_master.db"
    const val DATABASE_VERSION = 1

    // ==========================================
    // SRS (Spaced Repetition System)
    // SM-2 algorithm parameters
    // ==========================================
    const val SRS_DEFAULT_EASE_FACTOR = 2.5f
    const val SRS_MIN_EASE_FACTOR = 1.3f
    const val SRS_INITIAL_INTERVAL_DAYS = 1f
    const val SRS_SECOND_INTERVAL_DAYS = 3f
    const val SRS_FAILED_INTERVAL_DAYS = 0.5f
    const val SRS_BOOST_MULTIPLIER = 1.5f
    const val SRS_MAX_REVIEWS_PER_SESSION = 30
    const val SRS_MAX_WORDS_PER_REVIEW = 15
    const val SRS_MAX_RULES_PER_REVIEW = 10
    const val SRS_MAX_PHRASES_PER_REVIEW = 5

    // ==========================================
    // KNOWLEDGE LEVELS (0-7)
    // ==========================================
    const val KNOWLEDGE_LEVEL_NEVER_SEEN = 0
    const val KNOWLEDGE_LEVEL_SEEN = 1
    const val KNOWLEDGE_LEVEL_RECOGNIZED = 2
    const val KNOWLEDGE_LEVEL_RECALLED_WITH_HINT = 3
    const val KNOWLEDGE_LEVEL_RECALLED = 4
    const val KNOWLEDGE_LEVEL_USED_IN_CONTEXT = 5
    const val KNOWLEDGE_LEVEL_AUTOMATIC = 6
    const val KNOWLEDGE_LEVEL_MASTERY = 7

    // ==========================================
    // SRS QUALITY GRADES (0-5)
    // ==========================================
    const val QUALITY_TOTAL_FAILURE = 0
    const val QUALITY_WRONG_BUT_REMEMBERED = 1
    const val QUALITY_WRONG_BUT_CLOSE = 2
    const val QUALITY_CORRECT_WITH_DIFFICULTY = 3
    const val QUALITY_CORRECT = 4
    const val QUALITY_INSTANT_CORRECT = 5

    // ==========================================
    // AUDIO CONFIGURATION
    // ==========================================
    const val AUDIO_INPUT_SAMPLE_RATE = 16000
    const val AUDIO_OUTPUT_SAMPLE_RATE = 24000
    const val AUDIO_CHANNELS = 1 // Mono
    const val AUDIO_BITS_PER_SAMPLE = 16
    const val AUDIO_BUFFER_SIZE = 4096
    const val VAD_SILENCE_THRESHOLD_MS = 1500L
    const val VAD_MIN_SPEECH_DURATION_MS = 300L
    const val VAD_SPEECH_START_THRESHOLD = 0.02f // Adaptive

    // ==========================================
    // SESSION
    // ==========================================
    const val SESSION_AUTOSAVE_INTERVAL_MS = 30_000L
    const val SESSION_MAX_DURATION_MINUTES = 60
    const val SESSION_CONTEXT_LOAD_TIMEOUT_MS = 2000L
    const val SESSION_CONNECTION_TIMEOUT_MS = 5000L
    const val SESSION_GREETING_TIMEOUT_MS = 5000L
    const val SESSION_RECONNECT_MAX_ATTEMPTS = 3

    // ==========================================
    // GEMINI API
    // ==========================================
    // ✅ FIX: обновлена модель — старая "gemini-2.0-flash-live" retire 31.03.2026.
    // Актуальная модель совпадает с GeminiConfig.MODEL_GEMINI_LIVE
    const val GEMINI_MODEL_NAME = "gemini-2.5-flash-native-audio-preview"
    const val GEMINI_MAX_CONTEXT_TOKENS = 2_000_000
    const val GEMINI_DEFAULT_TEMPERATURE = 0.5f
    const val GEMINI_EXERCISE_TEMPERATURE = 0.3f
    const val GEMINI_CONVERSATION_TEMPERATURE = 0.7f
    const val GEMINI_TOP_P = 0.95f
    const val GEMINI_TOP_K = 40
    const val GEMINI_STREAMING_ENABLED = true

    // ==========================================
    // PERFORMANCE TARGETS
    // ==========================================
    const val PERF_APP_READY_MS = 3000L
    const val PERF_VOICE_FIRST_BYTE_MS = 500L
    const val PERF_RESPONSE_DELAY_MS = 1500L
    const val PERF_TARGET_FPS = 60
    const val PERF_MAX_RAM_MB = 200
    const val PERF_MAX_APK_MB = 100

    // ==========================================
    // DEFAULT USER PREFERENCES
    // ==========================================
    const val DEFAULT_SESSION_DURATION_MIN = 30
    const val DEFAULT_DAILY_GOAL_WORDS = 10
    const val DEFAULT_DAILY_GOAL_MINUTES = 30
    const val DEFAULT_VOICE_SPEED = 1.0f
    const val DEFAULT_GERMAN_VOICE_SPEED = 0.8f
    const val DEFAULT_LANGUAGE_MIX = 0.2f
    const val DEFAULT_MAX_REVIEWS = 30
    const val DEFAULT_REMINDER_HOUR = 19
    const val DEFAULT_REMINDER_MINUTE = 0

    // ==========================================
    // PRONUNCIATION
    // ==========================================
    const val PRONUNCIATION_POOR_THRESHOLD = 0.3f
    const val PRONUNCIATION_WEAK_THRESHOLD = 0.5f
    const val PRONUNCIATION_OK_THRESHOLD = 0.7f
    const val PRONUNCIATION_GOOD_THRESHOLD = 0.85f
    const val PRONUNCIATION_EXCELLENT_THRESHOLD = 0.95f
    const val PRONUNCIATION_RETRY_THRESHOLD = 0.7f

    // ==========================================
    // BOOK
    // ==========================================
    const val BOOK_ASSETS_PATH = "book"
    const val BOOK_METADATA_FILE = "metadata.json"
    const val BOOK_CHAPTER_INFO_FILE = "info.json"
    const val BOOK_VOCABULARY_FILE = "vocabulary.json"
    const val BOOK_GRAMMAR_FILE = "grammar.json"
    const val BOOK_EXERCISES_FILE = "exercises.json"
    const val BOOK_LESSON_COMPLETION_THRESHOLD = 0.8f
    const val BOOK_MAX_NEW_WORDS_PER_BLOCK = 7

    // ==========================================
    // STRATEGY
    // ==========================================
    const val STRATEGY_CHANGE_TIME_THRESHOLD_MIN = 25
    const val STRATEGY_ERROR_RATE_THRESHOLD = 0.6f
    const val STRATEGY_SRS_QUEUE_THRESHOLD = 10
    const val STRATEGY_WEAK_POINTS_THRESHOLD = 5
    const val STRATEGY_SKILL_GAP_THRESHOLD = 2 // sub-levels difference
    const val STRATEGY_PRONUNCIATION_SESSION_GAP_DAYS = 3

    // ==========================================
    // CACHE
    // ==========================================
    const val CACHE_MAX_AUDIO_MB = 500
    const val CACHE_MAX_CONTEXT_ENTRIES = 100
    const val CACHE_CLEANUP_INTERVAL_HOURS = 24

    // ==========================================
    // CEFR LEVELS
    // ==========================================
    const val CEFR_SUB_LEVELS = 10
    const val CEFR_VOCAB_THRESHOLD = 0.7f
    const val CEFR_GRAMMAR_THRESHOLD = 0.6f
}
