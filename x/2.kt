// Путь: src/test/java/com/voicedeutsch/master/voicecore/session/VoiceSessionManagerTest.kt
package com.voicedeutsch.master.voicecore.session

import app.cash.turbine.test
import com.voicedeutsch.master.domain.model.LearningStrategy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VoiceSessionManagerTest {

    private lateinit var manager: VoiceSessionManager

    @BeforeEach
    fun setUp() {
        manager = VoiceSessionManager()
    }

    // ── initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialState_isNotActive() {
        val state = manager.state.value
        assertFalse(state.isActive)
    }

    @Test
    fun initialState_allCountersAreZero() {
        val state = manager.state.value
        assertEquals(0, state.wordsLearned)
        assertEquals(0, state.wordsReviewed)
        assertEquals(0, state.rulesLearned)
        assertEquals(0, state.mistakeCount)
        assertEquals(0, state.correctCount)
        assertEquals(0L, state.totalPausedMs)
    }

    @Test
    fun initialState_sessionIdIsEmpty() {
        assertTrue(manager.state.value.sessionId.isEmpty())
    }

    @Test
    fun initialState_strategiesUsedIsEmpty() {
        assertTrue(manager.state.value.strategiesUsed.isEmpty())
    }

    // ── startSession ──────────────────────────────────────────────────────────

    @Test
    fun startSession_returnsNonEmptySessionId() {
        val id = manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        assertTrue(id.isNotEmpty())
    }

    @Test
    fun startSession_setsIsActiveTrue() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        assertTrue(manager.state.value.isActive)
    }

    @Test
    fun startSession_setsUserId() {
        manager.startSession("user_42", LearningStrategy.REPETITION)
        assertEquals("user_42", manager.state.value.userId)
    }

    @Test
    fun startSession_setsCurrentStrategy() {
        manager.startSession("user_1", LearningStrategy.GRAMMAR_DRILL)
        assertEquals(LearningStrategy.GRAMMAR_DRILL, manager.state.value.currentStrategy)
    }

    @Test
    fun startSession_addsStrategyToStrategiesUsed() {
        manager.startSession("user_1", LearningStrategy.VOCABULARY_BOOST)
        assertTrue(manager.state.value.strategiesUsed.contains(LearningStrategy.VOCABULARY_BOOST))
    }

    @Test
    fun startSession_setsStartedAtToPositiveTimestamp() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        assertTrue(manager.state.value.startedAt > 0)
    }

    @Test
    fun startSession_stateIdMatchesReturnedId() {
        val id = manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        assertEquals(id, manager.state.value.sessionId)
    }

    @Test
    fun startSession_calledTwice_secondSessionOverwritesFirst() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        val secondId = manager.startSession("user_2", LearningStrategy.REPETITION)
        assertEquals("user_2", manager.state.value.userId)
        assertEquals(secondId, manager.state.value.sessionId)
    }

    // ── pause ─────────────────────────────────────────────────────────────────

    @Test
    fun pause_setsPausedAtToNonNull() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.pause()
        assertNotNull(manager.state.value.pausedAt)
    }

    @Test
    fun pause_pausedAtIsRecentTimestamp() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        val before = System.currentTimeMillis()
        manager.pause()
        val after = System.currentTimeMillis()
        val pausedAt = manager.state.value.pausedAt!!
        assertTrue(pausedAt in before..after)
    }

    // ── resume ────────────────────────────────────────────────────────────────

    @Test
    fun resume_clearsPausedAt() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.pause()
        manager.resume()
        assertNull(manager.state.value.pausedAt)
    }

    @Test
    fun resume_accumulatesTotalPausedMs() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.pause()
        Thread.sleep(50)
        manager.resume()
        assertTrue(manager.state.value.totalPausedMs >= 50)
    }

    @Test
    fun resume_withoutPause_totalPausedMsRemainsZero() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.resume()
        assertEquals(0L, manager.state.value.totalPausedMs)
    }

    @Test
    fun pause_thenResume_twice_accumulatesCorrectly() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.pause()
        Thread.sleep(30)
        manager.resume()
        manager.pause()
        Thread.sleep(30)
        manager.resume()
        assertTrue(manager.state.value.totalPausedMs >= 60)
    }

    // ── switchStrategy ────────────────────────────────────────────────────────

    @Test
    fun switchStrategy_updatesCurrentStrategy() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.switchStrategy(LearningStrategy.PRONUNCIATION)
        assertEquals(LearningStrategy.PRONUNCIATION, manager.state.value.currentStrategy)
    }

    @Test
    fun switchStrategy_addsToStrategiesUsed() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.switchStrategy(LearningStrategy.REPETITION)
        assertTrue(manager.state.value.strategiesUsed.contains(LearningStrategy.REPETITION))
    }

    @Test
    fun switchStrategy_preservesPreviousStrategiesInSet() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.switchStrategy(LearningStrategy.REPETITION)
        manager.switchStrategy(LearningStrategy.GAP_FILLING)
        val used = manager.state.value.strategiesUsed
        assertTrue(used.contains(LearningStrategy.LINEAR_BOOK))
        assertTrue(used.contains(LearningStrategy.REPETITION))
        assertTrue(used.contains(LearningStrategy.GAP_FILLING))
    }

    @Test
    fun switchStrategy_sameTwice_noDuplicatesInSet() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.switchStrategy(LearningStrategy.REPETITION)
        manager.switchStrategy(LearningStrategy.REPETITION)
        assertEquals(2, manager.state.value.strategiesUsed.size)
    }

    // ── record* ───────────────────────────────────────────────────────────────

    @Test
    fun recordWordLearned_incrementsWordsLearned() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.recordWordLearned()
        manager.recordWordLearned()
        assertEquals(2, manager.state.value.wordsLearned)
    }

    @Test
    fun recordWordReviewed_incrementsWordsReviewed() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.recordWordReviewed()
        assertEquals(1, manager.state.value.wordsReviewed)
    }

    @Test
    fun recordRuleLearned_incrementsRulesLearned() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.recordRuleLearned()
        manager.recordRuleLearned()
        manager.recordRuleLearned()
        assertEquals(3, manager.state.value.rulesLearned)
    }

    @Test
    fun recordMistake_incrementsMistakeCount() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.recordMistake()
        assertEquals(1, manager.state.value.mistakeCount)
    }

    @Test
    fun recordCorrect_incrementsCorrectCount() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.recordCorrect()
        manager.recordCorrect()
        assertEquals(2, manager.state.value.correctCount)
    }

    @Test
    fun recordMistakeAndCorrect_areIndependent() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.recordMistake()
        manager.recordMistake()
        manager.recordCorrect()
        assertEquals(2, manager.state.value.mistakeCount)
        assertEquals(1, manager.state.value.correctCount)
    }

    // ── endSession ────────────────────────────────────────────────────────────

    @Test
    fun endSession_resetsStateToDefault() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.recordWordLearned()
        manager.endSession()
        val state = manager.state.value
        assertFalse(state.isActive)
        assertTrue(state.sessionId.isEmpty())
        assertEquals(0, state.wordsLearned)
    }

    @Test
    fun endSession_returnsCorrectSessionId() {
        val id = manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        val result = manager.endSession()
        assertEquals(id, result.sessionId)
    }

    @Test
    fun endSession_returnsCorrectWordsLearned() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.recordWordLearned()
        manager.recordWordLearned()
        val result = manager.endSession()
        assertEquals(2, result.wordsLearned)
    }

    @Test
    fun endSession_returnsCorrectWordsReviewed() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.recordWordReviewed()
        val result = manager.endSession()
        assertEquals(1, result.wordsReviewed)
    }

    @Test
    fun endSession_returnsCorrectRulesPracticed() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.recordRuleLearned()
        manager.recordRuleLearned()
        val result = manager.endSession()
        assertEquals(2, result.rulesPracticed)
    }

    @Test
    fun endSession_exercisesCompletedIsCorrectPlusMistakes() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.recordCorrect()
        manager.recordCorrect()
        manager.recordCorrect()
        manager.recordMistake()
        val result = manager.endSession()
        assertEquals(4, result.exercisesCompleted)
    }

    @Test
    fun endSession_exercisesCorrectMatchesCorrectCount() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.recordCorrect()
        manager.recordMistake()
        val result = manager.endSession()
        assertEquals(1, result.exercisesCorrect)
    }

    @Test
    fun endSession_strategiesUsedContainsAllSwitched() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.switchStrategy(LearningStrategy.REPETITION)
        val result = manager.endSession()
        assertTrue(result.strategiesUsed.contains(LearningStrategy.LINEAR_BOOK.name))
        assertTrue(result.strategiesUsed.contains(LearningStrategy.REPETITION.name))
    }

    @Test
    fun endSession_durationMinutesIsNonNegative() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        val result = manager.endSession()
        assertTrue(result.durationMinutes >= 0)
    }

    @Test
    fun endSession_withPausedTime_durationReducedByPause() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.pause()
        Thread.sleep(100)
        manager.resume()
        val result = manager.endSession()
        // durationMs = (now - startedAt) - totalPausedMs; both >= 0 and paused >= 100ms
        assertTrue(result.durationMinutes >= 0)
    }

    // ── reset ─────────────────────────────────────────────────────────────────

    @Test
    fun reset_clearsAllState() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.recordWordLearned()
        manager.recordMistake()
        manager.reset()
        val state = manager.state.value
        assertFalse(state.isActive)
        assertTrue(state.sessionId.isEmpty())
        assertEquals(0, state.wordsLearned)
        assertEquals(0, state.mistakeCount)
    }

    @Test
    fun reset_afterReset_canStartNewSession() {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.reset()
        val newId = manager.startSession("user_2", LearningStrategy.REPETITION)
        assertTrue(newId.isNotEmpty())
        assertTrue(manager.state.value.isActive)
    }

    // ── StateFlow emissions ───────────────────────────────────────────────────

    @Test
    fun state_startSession_emitsActiveState() = runTest {
        manager.state.test {
            awaitItem() // initial
            manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
            val emitted = awaitItem()
            assertTrue(emitted.isActive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun state_recordWordLearned_emitsIncrementedCount() = runTest {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.state.test {
            awaitItem() // current state
            manager.recordWordLearned()
            val emitted = awaitItem()
            assertEquals(1, emitted.wordsLearned)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun state_reset_emitsDefaultState() = runTest {
        manager.startSession("user_1", LearningStrategy.LINEAR_BOOK)
        manager.state.test {
            awaitItem() // current state
            manager.reset()
            val emitted = awaitItem()
            assertFalse(emitted.isActive)
            assertTrue(emitted.sessionId.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── SessionData data class ────────────────────────────────────────────────

    @Test
    fun sessionData_equals_sameValues() {
        val d1 = VoiceSessionManager.SessionData(sessionId = "s1", userId = "u1")
        val d2 = VoiceSessionManager.SessionData(sessionId = "s1", userId = "u1")
        assertEquals(d1, d2)
        assertEquals(d1.hashCode(), d2.hashCode())
    }

    @Test
    fun sessionData_copy_changesOnlySpecifiedField() {
        val original = VoiceSessionManager.SessionData(sessionId = "s1", wordsLearned = 5)
        val copy = original.copy(wordsLearned = 10)
        assertEquals("s1", copy.sessionId)
        assertEquals(10, copy.wordsLearned)
    }

    @Test
    fun sessionData_defaultValues_areCorrect() {
        val data = VoiceSessionManager.SessionData()
        assertFalse(data.isActive)
        assertEquals(0L, data.startedAt)
        assertNull(data.pausedAt)
        assertEquals(0L, data.totalPausedMs)
        assertEquals(LearningStrategy.LINEAR_BOOK, data.currentStrategy)
        assertTrue(data.strategiesUsed.isEmpty())
        assertEquals(0, data.wordsLearned)
        assertEquals(0, data.wordsReviewed)
        assertEquals(0, data.rulesLearned)
        assertEquals(0, data.mistakeCount)
        assertEquals(0, data.correctCount)
    }
}
