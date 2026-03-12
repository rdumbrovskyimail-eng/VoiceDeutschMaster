// Путь: src/test/java/com/voicedeutsch/master/voicecore/functions/FunctionRouterTest.kt
package com.voicedeutsch.master.voicecore.functions

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.MistakeType
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.domain.usecase.book.AdvanceBookProgressUseCase
import com.voicedeutsch.master.domain.usecase.book.GetCurrentLessonUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.GetWeakPointsUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.GetWordsForRepetitionUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.UpdateRuleKnowledgeUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.UpdateWordKnowledgeUseCase
import com.voicedeutsch.master.domain.usecase.speech.RecordPronunciationResultUseCase
import com.voicedeutsch.master.domain.usecase.user.GetUserStatisticsUseCase
import com.voicedeutsch.master.domain.usecase.user.UpdateUserLevelUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FunctionRouterTest {

    private lateinit var router: FunctionRouter

    private val updateWordKnowledge   = mockk<UpdateWordKnowledgeUseCase>(relaxed = true)
    private val updateRuleKnowledge   = mockk<UpdateRuleKnowledgeUseCase>(relaxed = true)
    private val getWordsForRepetition = mockk<GetWordsForRepetitionUseCase>(relaxed = true)
    private val getWeakPoints         = mockk<GetWeakPointsUseCase>(relaxed = true)
    private val getCurrentLesson      = mockk<GetCurrentLessonUseCase>(relaxed = true)
    private val advanceBookProgress   = mockk<AdvanceBookProgressUseCase>(relaxed = true)
    private val updateUserLevel       = mockk<UpdateUserLevelUseCase>(relaxed = true)
    private val getUserStatistics     = mockk<GetUserStatisticsUseCase>(relaxed = true)
    private val recordPronunciation   = mockk<RecordPronunciationResultUseCase>(relaxed = true)
    private val knowledgeRepository   = mockk<KnowledgeRepository>(relaxed = true)
    private val json                  = Json { ignoreUnknownKeys = true }

    private val userId    = "user_test"
    private val sessionId = "session_test"

    @BeforeEach
    fun setUp() {
        router = FunctionRouter(
            updateWordKnowledge   = updateWordKnowledge,
            updateRuleKnowledge   = updateRuleKnowledge,
            getWordsForRepetition = getWordsForRepetition,
            getWeakPoints         = getWeakPoints,
            getCurrentLesson      = getCurrentLesson,
            advanceBookProgress   = advanceBookProgress,
            updateUserLevel       = updateUserLevel,
            getUserStatistics     = getUserStatistics,
            recordPronunciation   = recordPronunciation,
            knowledgeRepository   = knowledgeRepository,
            json                  = json,
        )
    }

    // ── FunctionCallResult data class ────────────────────────────────────

    @Test
    fun functionCallResult_equals_twoIdentical() {
        val a = FunctionCallResult("f", true, "{}")
        val b = FunctionCallResult("f", true, "{}")
        assertEquals(a, b)
    }

    @Test
    fun functionCallResult_copy_changesOnlySpecifiedField() {
        val original = FunctionCallResult("f", true, "{}")
        val copied = original.copy(success = false)
        assertEquals("f", copied.functionName)
        assertFalse(copied.success)
        assertEquals("{}", copied.resultJson)
    }

    // ── unknown function ──────────────────────────────────────────────────

    @Test
    fun route_unknownFunction_returnsFailureWithError() = runTest {
        val result = router.route("unknown_func", "{}", userId, sessionId)
        assertFalse(result.success)
        assertTrue(result.resultJson.contains("Unknown function"))
        assertEquals("unknown_func", result.functionName)
    }

    // ── save_word_knowledge ───────────────────────────────────────────────

    @Test
    fun route_saveWordKnowledge_happyPath_returnsSuccess() = runTest {
        val args = """{"word":"Haus","translation":"дом","level":2,"quality":4}"""
        val result = router.route("save_word_knowledge", args, userId, sessionId)
        assertTrue(result.success)
        assertEquals("save_word_knowledge", result.functionName)
        assertTrue(result.resultJson.contains("saved"))
        assertTrue(result.resultJson.contains("Haus"))
    }

    @Test
    fun route_saveWordKnowledge_invokesUseCase() = runTest {
        val args = """{"word":"Wasser","translation":"вода","level":1,"quality":3}"""
        router.route("save_word_knowledge", args, userId, sessionId)
        coVerify(exactly = 1) { updateWordKnowledge(any()) }
    }

    @Test
    fun route_saveWordKnowledge_missingWord_returnsFailure() = runTest {
        val result = router.route("save_word_knowledge", """{"level":1}""", userId, sessionId)
        assertFalse(result.success)
        assertTrue(result.resultJson.contains("missing 'word'"))
    }

    @Test
    fun route_saveWordKnowledge_levelStoredInResult() = runTest {
        val args = """{"word":"Buch","translation":"книга","level":3,"quality":5}"""
        val result = router.route("save_word_knowledge", args, userId, sessionId)
        assertTrue(result.resultJson.contains("3"))
    }

    @Test
    fun route_saveWordKnowledge_levelAsString_coercedToInt() = runTest {
        val args = """{"word":"Test","translation":"тест","level":"2","quality":"3"}"""
        val result = router.route("save_word_knowledge", args, userId, sessionId)
        assertTrue(result.success)
    }

    @Test
    fun route_saveWordKnowledge_levelAsFloat_coercedToInt() = runTest {
        val args = """{"word":"Test","translation":"тест","level":2.0,"quality":3.0}"""
        val result = router.route("save_word_knowledge", args, userId, sessionId)
        assertTrue(result.success)
    }

    // ── save_rule_knowledge ───────────────────────────────────────────────

    @Test
    fun route_saveRuleKnowledge_happyPath_returnsSuccess() = runTest {
        val args = """{"rule_id":"dativ_001","level":2,"quality":4}"""
        val result = router.route("save_rule_knowledge", args, userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("saved"))
        assertTrue(result.resultJson.contains("dativ_001"))
    }

    @Test
    fun route_saveRuleKnowledge_invokesUseCase() = runTest {
        val args = """{"rule_id":"akkusativ_002","level":1,"quality":3}"""
        router.route("save_rule_knowledge", args, userId, sessionId)
        coVerify(exactly = 1) { updateRuleKnowledge(any()) }
    }

    @Test
    fun route_saveRuleKnowledge_missingRuleId_returnsFailure() = runTest {
        val result = router.route("save_rule_knowledge", """{"level":1}""", userId, sessionId)
        assertFalse(result.success)
        assertTrue(result.resultJson.contains("missing 'rule_id'"))
    }

    // ── record_mistake ────────────────────────────────────────────────────

    @Test
    fun route_recordMistake_grammarType_returnsSuccess() = runTest {
        val args = """{"mistake_type":"grammar","user_input":"ich bin","correct_form":"ich habe","context":"test"}"""
        val result = router.route("record_mistake", args, userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("recorded"))
    }

    @Test
    fun route_recordMistake_vocabularyType_returnsSuccess() = runTest {
        val args = """{"mistake_type":"vocabulary","user_input":"Tisch","correct_form":"Stuhl"}"""
        val result = router.route("record_mistake", args, userId, sessionId)
        assertTrue(result.success)
    }

    @Test
    fun route_recordMistake_wordAlias_returnsSuccess() = runTest {
        val args = """{"mistake_type":"word","user_input":"x","correct_form":"y"}"""
        val result = router.route("record_mistake", args, userId, sessionId)
        assertTrue(result.success)
    }

    @Test
    fun route_recordMistake_pronunciationType_returnsSuccess() = runTest {
        val args = """{"mistake_type":"pronunciation","user_input":"ü","correct_form":"ü"}"""
        val result = router.route("record_mistake", args, userId, sessionId)
        assertTrue(result.success)
    }

    @Test
    fun route_recordMistake_unknownType_fallsBackToGrammar() = runTest {
        val args = """{"mistake_type":"nonsense","user_input":"x","correct_form":"y"}"""
        val result = router.route("record_mistake", args, userId, sessionId)
        assertTrue(result.success)
    }

    @Test
    fun route_recordMistake_logsToRepository() = runTest {
        val args = """{"mistake_type":"grammar","user_input":"x","correct_form":"y"}"""
        router.route("record_mistake", args, userId, sessionId)
        coVerify(exactly = 1) { knowledgeRepository.logMistake(any()) }
    }

    @Test
    fun route_recordMistake_resultContainsMistakeId() = runTest {
        val args = """{"mistake_type":"grammar","user_input":"x","correct_form":"y"}"""
        val result = router.route("record_mistake", args, userId, sessionId)
        assertTrue(result.resultJson.contains("mistake_id"))
    }

    // ── get_current_lesson ────────────────────────────────────────────────

    @Test
    fun route_getCurrentLesson_nullData_returnsSuccessWithDefaults() = runTest {
        coEvery { getCurrentLesson(userId) } returns null
        val result = router.route("get_current_lesson", "{}", userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("chapter"))
    }

    @Test
    fun route_getCurrentLesson_invokesUseCase() = runTest {
        coEvery { getCurrentLesson(userId) } returns null
        router.route("get_current_lesson", "{}", userId, sessionId)
        coVerify(exactly = 1) { getCurrentLesson(userId) }
    }

    // ── advance_to_next_lesson ────────────────────────────────────────────

    @Test
    fun route_advanceToNextLesson_returnsSuccess() = runTest {
        coEvery { getCurrentLesson(userId) } returns null
        coEvery { advanceBookProgress(userId, any()) } returns mockk(relaxed = true)
        val args = """{"score":0.8}"""
        val result = router.route("advance_to_next_lesson", args, userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("advanced"))
    }

    @Test
    fun route_advanceToNextLesson_invokesAdvanceBookProgress() = runTest {
        coEvery { getCurrentLesson(userId) } returns null
        coEvery { advanceBookProgress(userId, any()) } returns mockk(relaxed = true)
        val args = """{"score":1.0}"""
        router.route("advance_to_next_lesson", args, userId, sessionId)
        coVerify(exactly = 1) { advanceBookProgress(userId, any()) }
    }

    @Test
    fun route_advanceToNextLesson_missingScore_usesDefault() = runTest {
        coEvery { getCurrentLesson(userId) } returns null
        coEvery { advanceBookProgress(userId, any()) } returns mockk(relaxed = true)
        val result = router.route("advance_to_next_lesson", "{}", userId, sessionId)
        assertTrue(result.success)
    }

    @Test
    fun route_advanceToNextLesson_resultContainsHint() = runTest {
        coEvery { getCurrentLesson(userId) } returns null
        coEvery { advanceBookProgress(userId, any()) } returns mockk(relaxed = true)
        val result = router.route("advance_to_next_lesson", """{"score":1.0}""", userId, sessionId)
        assertTrue(result.resultJson.contains("read_lesson_paragraph"))
    }

    @Test
    fun route_advanceToNextLesson_resultContainsTextTruncated() = runTest {
        coEvery { getCurrentLesson(userId) } returns null
        coEvery { advanceBookProgress(userId, any()) } returns mockk(relaxed = true)
        val result = router.route("advance_to_next_lesson", """{"score":1.0}""", userId, sessionId)
        assertTrue(result.resultJson.contains("text_truncated"))
    }

    // ── read_lesson_paragraph ─────────────────────────────────────────────

    @Test
    fun route_readLessonParagraph_noContent_returnsFailure() = runTest {
        coEvery { getCurrentLesson(userId) } returns null
        val args = """{"index":0}"""
        val result = router.route("read_lesson_paragraph", args, userId, sessionId)
        assertFalse(result.success)
        assertTrue(result.resultJson.contains("no content"))
    }

    @Test
    fun route_readLessonParagraph_negativeIndex_returnsFailure() = runTest {
        val lessonData = buildLessonDataWithContent("Para one.\n\nPara two.")
        coEvery { getCurrentLesson(userId) } returns lessonData
        val result = router.route("read_lesson_paragraph", """{"index":-1}""", userId, sessionId)
        assertFalse(result.success)
        assertTrue(result.resultJson.contains("out of range"))
    }

    @Test
    fun route_readLessonParagraph_indexOutOfRange_returnsFailure() = runTest {
        val lessonData = buildLessonDataWithContent("Only one paragraph.")
        coEvery { getCurrentLesson(userId) } returns lessonData
        val result = router.route("read_lesson_paragraph", """{"index":5}""", userId, sessionId)
        assertFalse(result.success)
        assertTrue(result.resultJson.contains("out of range"))
    }

    @Test
    fun route_readLessonParagraph_validIndex_returnsSuccess() = runTest {
        val lessonData = buildLessonDataWithContent("First paragraph.\n\nSecond paragraph.")
        coEvery { getCurrentLesson(userId) } returns lessonData
        val result = router.route("read_lesson_paragraph", """{"index":0}""", userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("paragraph"))
        assertTrue(result.resultJson.contains("has_next"))
    }

    @Test
    fun route_readLessonParagraph_firstOfTwo_hasNextIsTrue() = runTest {
        val lessonData = buildLessonDataWithContent("Para one.\n\nPara two.")
        coEvery { getCurrentLesson(userId) } returns lessonData
        val result = router.route("read_lesson_paragraph", """{"index":0}""", userId, sessionId)
        assertTrue(result.resultJson.contains("\"has_next\":true"))
    }

    @Test
    fun route_readLessonParagraph_lastParagraph_hasNextIsFalse() = runTest {
        val lessonData = buildLessonDataWithContent("Para one.\n\nPara two.")
        coEvery { getCurrentLesson(userId) } returns lessonData
        val result = router.route("read_lesson_paragraph", """{"index":1}""", userId, sessionId)
        assertTrue(result.resultJson.contains("\"has_next\":false"))
    }

    @Test
    fun route_readLessonParagraph_resultContainsTotalAndIndex() = runTest {
        val lessonData = buildLessonDataWithContent("A.\n\nB.\n\nC.")
        coEvery { getCurrentLesson(userId) } returns lessonData
        val result = router.route("read_lesson_paragraph", """{"index":1}""", userId, sessionId)
        assertTrue(result.resultJson.contains("\"index\":1"))
        assertTrue(result.resultJson.contains("\"total\":3"))
    }

    // ── mark_lesson_complete ──────────────────────────────────────────────

    @Test
    fun route_markLessonComplete_returnsSuccess() = runTest {
        coEvery { advanceBookProgress(userId, any()) } returns mockk(relaxed = true)
        val result = router.route("mark_lesson_complete", """{"score":0.9}""", userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("marked_complete"))
    }

    @Test
    fun route_markLessonComplete_missingScore_usesDefault() = runTest {
        coEvery { advanceBookProgress(userId, any()) } returns mockk(relaxed = true)
        val result = router.route("mark_lesson_complete", "{}", userId, sessionId)
        assertTrue(result.success)
    }

    // ── get_words_for_repetition ──────────────────────────────────────────

    @Test
    fun route_getWordsForRepetition_emptyList_returnsSuccess() = runTest {
        coEvery { getWordsForRepetition(userId, any()) } returns emptyList()
        val result = router.route("get_words_for_repetition", """{"limit":10}""", userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("\"count\":0"))
    }

    @Test
    fun route_getWordsForRepetition_invokesUseCase() = runTest {
        coEvery { getWordsForRepetition(userId, any()) } returns emptyList()
        router.route("get_words_for_repetition", """{"limit":5}""", userId, sessionId)
        coVerify(exactly = 1) { getWordsForRepetition(userId, any()) }
    }

    @Test
    fun route_getWordsForRepetition_missingLimit_usesDefault15() = runTest {
        coEvery { getWordsForRepetition(userId, 15) } returns emptyList()
        router.route("get_words_for_repetition", "{}", userId, sessionId)
        coVerify { getWordsForRepetition(userId, 15) }
    }

    // ── get_weak_points ───────────────────────────────────────────────────

    @Test
    fun route_getWeakPoints_emptyList_returnsSuccess() = runTest {
        coEvery { getWeakPoints(userId) } returns emptyList()
        val result = router.route("get_weak_points", "{}", userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("\"total\":0"))
    }

    @Test
    fun route_getWeakPoints_invokesUseCase() = runTest {
        coEvery { getWeakPoints(userId) } returns emptyList()
        router.route("get_weak_points", "{}", userId, sessionId)
        coVerify(exactly = 1) { getWeakPoints(userId) }
    }

    // ── set_current_strategy ──────────────────────────────────────────────

    @Test
    fun route_setCurrentStrategy_knownStrategy_returnsSuccess() = runTest {
        val result = router.route(
            "set_current_strategy", """{"strategy":"REPETITION"}""", userId, sessionId
        )
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("strategy_set"))
        assertTrue(result.resultJson.contains("REPETITION"))
    }

    @Test
    fun route_setCurrentStrategy_missingStrategy_usesDefault() = runTest {
        val result = router.route("set_current_strategy", "{}", userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("LINEAR_BOOK"))
    }

    // ── log_session_event ─────────────────────────────────────────────────

    @Test
    fun route_logSessionEvent_returnsSuccess() = runTest {
        val args = """{"event_type":"word_learned","details":"Haus"}"""
        val result = router.route("log_session_event", args, userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("logged"))
        assertTrue(result.resultJson.contains("word_learned"))
    }

    @Test
    fun route_logSessionEvent_nullSessionId_returnsSuccess() = runTest {
        val args = """{"event_type":"test"}"""
        val result = router.route("log_session_event", args, userId, null)
        assertTrue(result.success)
    }

    // ── update_user_level ─────────────────────────────────────────────────

    @Test
    fun route_updateUserLevel_returnsSuccess() = runTest {
        coEvery { updateUserLevel(userId) } returns mockk(relaxed = true) {
            io.mockk.every { levelChanged } returns false
            io.mockk.every { newLevel } returns mockk { io.mockk.every { name } returns "B1" }
            io.mockk.every { newSubLevel } returns 1
            io.mockk.every { reason } returns "stable"
        }
        val result = router.route("update_user_level", "{}", userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("level"))
    }

    @Test
    fun route_updateUserLevel_invokesUseCase() = runTest {
        coEvery { updateUserLevel(userId) } returns mockk(relaxed = true)
        router.route("update_user_level", "{}", userId, sessionId)
        coVerify(exactly = 1) { updateUserLevel(userId) }
    }

    // ── get_user_statistics ───────────────────────────────────────────────

    @Test
    fun route_getUserStatistics_returnsSuccess() = runTest {
        coEvery { getUserStatistics(userId) } returns mockk(relaxed = true)
        val result = router.route("get_user_statistics", "{}", userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("total_words"))
    }

    @Test
    fun route_getUserStatistics_invokesUseCase() = runTest {
        coEvery { getUserStatistics(userId) } returns mockk(relaxed = true)
        router.route("get_user_statistics", "{}", userId, sessionId)
        coVerify(exactly = 1) { getUserStatistics(userId) }
    }

    // ── save_pronunciation_result ─────────────────────────────────────────

    @Test
    fun route_savePronunciationResult_returnsSuccess() = runTest {
        val args = """{"word":"ü","score":0.7,"problem_sounds":["ü"]}"""
        val result = router.route("save_pronunciation_result", args, userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("saved"))
    }

    @Test
    fun route_savePronunciationResult_invokesUseCase() = runTest {
        val args = """{"word":"Bach","score":0.8}"""
        router.route("save_pronunciation_result", args, userId, sessionId)
        coVerify(exactly = 1) { recordPronunciation(any()) }
    }

    @Test
    fun route_savePronunciationResult_missingScore_usesDefault() = runTest {
        val args = """{"word":"Test"}"""
        val result = router.route("save_pronunciation_result", args, userId, sessionId)
        assertTrue(result.success)
    }

    // ── get_pronunciation_targets ─────────────────────────────────────────

    @Test
    fun route_getPronunciationTargets_emptyList_returnsSuccess() = runTest {
        coEvery { knowledgeRepository.getProblemSounds(userId) } returns emptyList()
        val result = router.route("get_pronunciation_targets", "{}", userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("targets"))
    }

    @Test
    fun route_getPronunciationTargets_invokesRepository() = runTest {
        coEvery { knowledgeRepository.getProblemSounds(userId) } returns emptyList()
        router.route("get_pronunciation_targets", "{}", userId, sessionId)
        coVerify(exactly = 1) { knowledgeRepository.getProblemSounds(userId) }
    }

    // ── UI handlers ───────────────────────────────────────────────────────

    @Test
    fun route_showWordCard_returnsSuccess() = runTest {
        val result = router.route("show_word_card", """{"word":"Haus"}""", userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("displayed"))
    }

    @Test
    fun route_showGrammarHint_returnsSuccess() = runTest {
        val result = router.route("show_grammar_hint", """{"rule":"Dativ"}""", userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("displayed"))
    }

    @Test
    fun route_triggerCelebration_returnsSuccess() = runTest {
        val result = router.route("trigger_celebration", "{}", userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("triggered"))
    }

    // ── exception handling ────────────────────────────────────────────────

    @Test
    fun route_useCaseThrowsException_returnsFailureWithError() = runTest {
        coEvery { updateWordKnowledge(any()) } throws RuntimeException("DB error")
        val args = """{"word":"Test","translation":"тест","level":1,"quality":3}"""
        val result = router.route("save_word_knowledge", args, userId, sessionId)
        assertFalse(result.success)
        assertTrue(result.resultJson.contains("DB error"))
    }

    @Test
    fun route_invalidJson_returnsFailure() = runTest {
        val result = router.route("save_word_knowledge", "not-json", userId, sessionId)
        assertFalse(result.success)
        assertTrue(result.resultJson.contains("error"))
    }

    // ── coercion helpers (via public route) ───────────────────────────────

    @Test
    fun intCoerced_stringValue_parsedCorrectly() = runTest {
        val args = """{"word":"Test","translation":"T","level":"3","quality":"4"}"""
        val result = router.route("save_word_knowledge", args, userId, sessionId)
        assertTrue(result.success)
        assertTrue(result.resultJson.contains("3"))
    }

    @Test
    fun intCoerced_floatValue_truncatedToInt() = runTest {
        val args = """{"word":"Test","translation":"T","level":2.9,"quality":3.1}"""
        val result = router.route("save_word_knowledge", args, userId, sessionId)
        assertTrue(result.success)
    }

    @Test
    fun boolCoerced_stringTrue_parsedCorrectly() = runTest {
        // Exercised indirectly through any handler that uses boolCoerced
        // Minimal smoke test — no handler currently exposes bool result in JSON
        val result = router.route("log_session_event", """{"event_type":"e","flag":"true"}""", userId, sessionId)
        assertTrue(result.success)
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private fun buildLessonDataWithContent(content: String): GetCurrentLessonUseCase.LessonData {
        val lessonContent = mockk<com.voicedeutsch.master.domain.model.book.LessonContent>(relaxed = true) {
            io.mockk.every { mainContent } returns content
            io.mockk.every { vocabulary } returns emptyList()
        }
        return mockk(relaxed = true) {
            io.mockk.every { this@mockk.lessonContent } returns lessonContent
        }
    }
}
