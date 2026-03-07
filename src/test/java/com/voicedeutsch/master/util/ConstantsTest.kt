// src/test/java/com/voicedeutsch/master/util/ConstantsTest.kt
package com.voicedeutsch.master.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ConstantsTest {

    @Test fun databaseName_isNotEmpty() { assertTrue(Constants.DATABASE_NAME.isNotEmpty()) }
    @Test fun databaseVersion_isPositive() { assertTrue(Constants.DATABASE_VERSION > 0) }

    // ── SRS ───────────────────────────────────────────────────────────────

    @Test fun srsDefaultEaseFactor_isAboveMinimum() {
        assertTrue(Constants.SRS_DEFAULT_EASE_FACTOR > Constants.SRS_MIN_EASE_FACTOR)
    }
    @Test fun srsMinEaseFactor_isPositive() { assertTrue(Constants.SRS_MIN_EASE_FACTOR > 0f) }
    @Test fun srsInitialIntervalDays_isPositive() { assertTrue(Constants.SRS_INITIAL_INTERVAL_DAYS > 0f) }
    @Test fun srsSecondIntervalDays_isGreaterThanInitial() {
        assertTrue(Constants.SRS_SECOND_INTERVAL_DAYS > Constants.SRS_INITIAL_INTERVAL_DAYS)
    }
    @Test fun srsFailedIntervalDays_isPositiveAndLessThanInitial() {
        assertTrue(Constants.SRS_FAILED_INTERVAL_DAYS > 0f)
        assertTrue(Constants.SRS_FAILED_INTERVAL_DAYS < Constants.SRS_INITIAL_INTERVAL_DAYS)
    }
    @Test fun srsBoostMultiplier_isGreaterThanOne() { assertTrue(Constants.SRS_BOOST_MULTIPLIER > 1f) }
    @Test fun srsMaxReviewsPerSession_isPositive() { assertTrue(Constants.SRS_MAX_REVIEWS_PER_SESSION > 0) }
    @Test fun srsMaxWordsPerReview_isPositive() { assertTrue(Constants.SRS_MAX_WORDS_PER_REVIEW > 0) }
    @Test fun srsMaxRulesPerReview_isPositive() { assertTrue(Constants.SRS_MAX_RULES_PER_REVIEW > 0) }
    @Test fun srsMaxPhrasesPerReview_isPositive() { assertTrue(Constants.SRS_MAX_PHRASES_PER_REVIEW > 0) }

    // ── Knowledge Levels ─────────────────────────────────────────────────

    @Test fun knowledgeLevelNeverSeen_isZero() { assertEquals(0, Constants.KNOWLEDGE_LEVEL_NEVER_SEEN) }
    @Test fun knowledgeLevelMastery_isSeven() { assertEquals(7, Constants.KNOWLEDGE_LEVEL_MASTERY) }

    @Test fun knowledgeLevels_areStrictlyIncreasing() {
        val levels = listOf(
            Constants.KNOWLEDGE_LEVEL_NEVER_SEEN, Constants.KNOWLEDGE_LEVEL_SEEN,
            Constants.KNOWLEDGE_LEVEL_RECOGNIZED, Constants.KNOWLEDGE_LEVEL_RECALLED_WITH_HINT,
            Constants.KNOWLEDGE_LEVEL_RECALLED, Constants.KNOWLEDGE_LEVEL_USED_IN_CONTEXT,
            Constants.KNOWLEDGE_LEVEL_AUTOMATIC, Constants.KNOWLEDGE_LEVEL_MASTERY,
        )
        for (i in 0 until levels.size - 1)
            assertTrue(levels[i] < levels[i + 1], "Level $i should be less than ${i + 1}")
    }

    @Test fun knowledgeLevels_containExactly8Distinct() {
        assertEquals(8, setOf(
            Constants.KNOWLEDGE_LEVEL_NEVER_SEEN, Constants.KNOWLEDGE_LEVEL_SEEN,
            Constants.KNOWLEDGE_LEVEL_RECOGNIZED, Constants.KNOWLEDGE_LEVEL_RECALLED_WITH_HINT,
            Constants.KNOWLEDGE_LEVEL_RECALLED, Constants.KNOWLEDGE_LEVEL_USED_IN_CONTEXT,
            Constants.KNOWLEDGE_LEVEL_AUTOMATIC, Constants.KNOWLEDGE_LEVEL_MASTERY,
        ).size)
    }

    // ── Quality Grades ────────────────────────────────────────────────────

    @Test fun qualityTotalFailure_isZero() { assertEquals(0, Constants.QUALITY_TOTAL_FAILURE) }
    @Test fun qualityInstantCorrect_isFive() { assertEquals(5, Constants.QUALITY_INSTANT_CORRECT) }

    @Test fun qualityGrades_areStrictlyIncreasing() {
        val grades = listOf(
            Constants.QUALITY_TOTAL_FAILURE, Constants.QUALITY_WRONG_BUT_REMEMBERED,
            Constants.QUALITY_WRONG_BUT_CLOSE, Constants.QUALITY_CORRECT_WITH_DIFFICULTY,
            Constants.QUALITY_CORRECT, Constants.QUALITY_INSTANT_CORRECT,
        )
        for (i in 0 until grades.size - 1) assertTrue(grades[i] < grades[i + 1])
    }

    // ── Audio ─────────────────────────────────────────────────────────────

    @Test fun audioInputSampleRate_is16000() { assertEquals(16_000, Constants.AUDIO_INPUT_SAMPLE_RATE) }
    @Test fun audioOutputSampleRate_is24000() { assertEquals(24_000, Constants.AUDIO_OUTPUT_SAMPLE_RATE) }
    @Test fun audioChannels_isMono() { assertEquals(1, Constants.AUDIO_CHANNELS) }
    @Test fun audioBitsPerSample_is16() { assertEquals(16, Constants.AUDIO_BITS_PER_SAMPLE) }
    @Test fun audioBufferSize_isPositive() { assertTrue(Constants.AUDIO_BUFFER_SIZE > 0) }

    // ── Pronunciation thresholds ──────────────────────────────────────────

    @Test fun pronunciationThresholds_areStrictlyIncreasing() {
        assertTrue(Constants.PRONUNCIATION_POOR_THRESHOLD < Constants.PRONUNCIATION_WEAK_THRESHOLD)
        assertTrue(Constants.PRONUNCIATION_WEAK_THRESHOLD < Constants.PRONUNCIATION_OK_THRESHOLD)
        assertTrue(Constants.PRONUNCIATION_OK_THRESHOLD < Constants.PRONUNCIATION_GOOD_THRESHOLD)
        assertTrue(Constants.PRONUNCIATION_GOOD_THRESHOLD < Constants.PRONUNCIATION_EXCELLENT_THRESHOLD)
    }

    @Test fun pronunciationThresholds_areBetweenZeroAndOne() {
        listOf(Constants.PRONUNCIATION_POOR_THRESHOLD, Constants.PRONUNCIATION_WEAK_THRESHOLD,
            Constants.PRONUNCIATION_OK_THRESHOLD, Constants.PRONUNCIATION_GOOD_THRESHOLD,
            Constants.PRONUNCIATION_EXCELLENT_THRESHOLD,
        ).forEach { assertTrue(it in 0f..1f, "Threshold $it should be in [0, 1]") }
    }

    @Test fun pronunciationRetryThreshold_equalsOkThreshold() {
        assertEquals(Constants.PRONUNCIATION_OK_THRESHOLD, Constants.PRONUNCIATION_RETRY_THRESHOLD)
    }

    // ── Session ───────────────────────────────────────────────────────────

    @Test fun sessionAutoSaveInterval_isPositive() { assertTrue(Constants.SESSION_AUTOSAVE_INTERVAL_MS > 0) }
    @Test fun sessionMaxDurationMinutes_isPositive() { assertTrue(Constants.SESSION_MAX_DURATION_MINUTES > 0) }
    @Test fun sessionReconnectMaxAttempts_isAtLeastOne() { assertTrue(Constants.SESSION_RECONNECT_MAX_ATTEMPTS >= 1) }

    // ── Gemini ────────────────────────────────────────────────────────────

    @Test fun geminiModelName_isNotEmpty() { assertTrue(Constants.GEMINI_MODEL_NAME.isNotEmpty()) }
    @Test fun geminiLiveMaxContextTokens_is131072() { assertEquals(131_072, Constants.GEMINI_LIVE_MAX_CONTEXT_TOKENS) }
    @Test fun geminiRestMaxContextTokens_isGreaterThanLive() {
        assertTrue(Constants.GEMINI_REST_MAX_CONTEXT_TOKENS > Constants.GEMINI_LIVE_MAX_CONTEXT_TOKENS)
    }
    @Test fun geminiTemperatures_areBetweenZeroAndOne() {
        listOf(Constants.GEMINI_DEFAULT_TEMPERATURE, Constants.GEMINI_EXERCISE_TEMPERATURE,
            Constants.GEMINI_CONVERSATION_TEMPERATURE,
        ).forEach { assertTrue(it in 0f..1f, "Temperature $it out of range") }
    }
    @Test fun geminiTopP_isBetweenZeroAndOne() { assertTrue(Constants.GEMINI_TOP_P in 0f..1f) }
    @Test fun geminiTopK_isPositive() { assertTrue(Constants.GEMINI_TOP_K > 0) }

    // ── CEFR ─────────────────────────────────────────────────────────────

    @Test fun cefrSubLevels_is10() { assertEquals(10, Constants.CEFR_SUB_LEVELS) }
    @Test fun cefrVocabThreshold_isBetweenZeroAndOne() { assertTrue(Constants.CEFR_VOCAB_THRESHOLD in 0f..1f) }
    @Test fun cefrGrammarThreshold_isBetweenZeroAndOne() { assertTrue(Constants.CEFR_GRAMMAR_THRESHOLD in 0f..1f) }

    // ── Book ─────────────────────────────────────────────────────────────

    @Test fun bookAssetsPath_isNotEmpty() { assertTrue(Constants.BOOK_ASSETS_PATH.isNotEmpty()) }
    @Test fun bookLessonCompletionThreshold_isBetweenZeroAndOne() {
        assertTrue(Constants.BOOK_LESSON_COMPLETION_THRESHOLD in 0f..1f)
    }
    @Test fun bookMaxNewWordsPerBlock_isPositive() { assertTrue(Constants.BOOK_MAX_NEW_WORDS_PER_BLOCK > 0) }

    // ── Strategy ─────────────────────────────────────────────────────────

    @Test fun strategyChangeTimeThreshold_isPositive() { assertTrue(Constants.STRATEGY_CHANGE_TIME_THRESHOLD_MIN > 0) }
    @Test fun strategyErrorRateThreshold_isBetweenZeroAndOne() {
        assertTrue(Constants.STRATEGY_ERROR_RATE_THRESHOLD in 0f..1f)
    }

    // ── Firebase ─────────────────────────────────────────────────────────

    @Test fun firestoreCollections_areNotEmpty() {
        listOf(Constants.FIRESTORE_USERS_COLLECTION, Constants.FIRESTORE_PROGRESS_COLLECTION,
            Constants.FIRESTORE_STATISTICS_COLLECTION, Constants.FIRESTORE_BACKUPS_COLLECTION,
        ).forEach { assertTrue(it.isNotEmpty(), "Collection name should not be empty") }
    }
    @Test fun backupMaxCloudCount_isPositive() { assertTrue(Constants.BACKUP_MAX_CLOUD_COUNT > 0) }
    @Test fun backupKeepCloudDays_isGreaterThanLocal() {
        assertTrue(Constants.BACKUP_KEEP_CLOUD_DAYS > Constants.BACKUP_KEEP_LOCAL_DAYS)
    }
}
