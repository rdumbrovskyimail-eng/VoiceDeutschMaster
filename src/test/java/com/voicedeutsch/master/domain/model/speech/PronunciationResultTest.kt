// Path: src/test/java/com/voicedeutsch/master/domain/model/speech/PronunciationResultTest.kt
package com.voicedeutsch.master.domain.model.speech

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PronunciationResultTest {

    private fun makePronunciationResult(
        id: String = "pr_1",
        userId: String = "user_1",
        word: String = "Haus",
        score: Float = 0.85f,
        problemSounds: List<String> = emptyList(),
        attemptNumber: Int = 1,
        sessionId: String? = null,
        timestamp: Long = 1_000_000L,
        createdAt: Long = 1_000_000L,
    ) = PronunciationResult(
        id = id,
        userId = userId,
        word = word,
        score = score,
        problemSounds = problemSounds,
        attemptNumber = attemptNumber,
        sessionId = sessionId,
        timestamp = timestamp,
        createdAt = createdAt,
    )

    @Test
    fun creation_withRequiredFields_setsValues() {
        val pr = makePronunciationResult()
        assertEquals("pr_1", pr.id)
        assertEquals("user_1", pr.userId)
        assertEquals("Haus", pr.word)
        assertEquals(0.85f, pr.score)
    }

    @Test
    fun creation_defaultProblemSounds_isEmpty() {
        val pr = makePronunciationResult()
        assertTrue(pr.problemSounds.isEmpty())
    }

    @Test
    fun creation_defaultAttemptNumber_isOne() {
        val pr = makePronunciationResult()
        assertEquals(1, pr.attemptNumber)
    }

    @Test
    fun creation_defaultSessionId_isNull() {
        val pr = makePronunciationResult()
        assertNull(pr.sessionId)
    }

    @Test
    fun creation_withProblemSounds_setsSounds() {
        val pr = makePronunciationResult(problemSounds = listOf("ü", "ch"))
        assertEquals(2, pr.problemSounds.size)
        assertTrue(pr.problemSounds.contains("ü"))
    }

    @Test
    fun creation_withSessionId_setsSessionId() {
        val pr = makePronunciationResult(sessionId = "session_42")
        assertEquals("session_42", pr.sessionId)
    }

    @Test
    fun creation_perfectScore_isOne() {
        val pr = makePronunciationResult(score = 1.0f)
        assertEquals(1.0f, pr.score)
    }

    @Test
    fun creation_zeroScore_isValid() {
        val pr = makePronunciationResult(score = 0.0f)
        assertEquals(0.0f, pr.score)
    }

    @Test
    fun creation_highAttemptNumber_isValid() {
        val pr = makePronunciationResult(attemptNumber = 10)
        assertEquals(10, pr.attemptNumber)
    }

    @Test
    fun copy_changesScore_restUnchanged() {
        val original = makePronunciationResult(score = 0.5f)
        val copy = original.copy(score = 0.9f)
        assertEquals(0.9f, copy.score)
        assertEquals("pr_1", copy.id)
        assertEquals("Haus", copy.word)
    }

    @Test
    fun copy_changesAttemptNumber() {
        val original = makePronunciationResult(attemptNumber = 1)
        val copy = original.copy(attemptNumber = 3)
        assertEquals(3, copy.attemptNumber)
    }

    @Test
    fun copy_changesWord() {
        val original = makePronunciationResult(word = "Haus")
        val copy = original.copy(word = "Schule")
        assertEquals("Schule", copy.word)
    }

    @Test
    fun copy_addsSessionId() {
        val original = makePronunciationResult(sessionId = null)
        val copy = original.copy(sessionId = "sess_1")
        assertEquals("sess_1", copy.sessionId)
    }

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val a = makePronunciationResult()
        val b = makePronunciationResult()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentScore_areNotEqual() {
        val a = makePronunciationResult(score = 0.7f)
        val b = makePronunciationResult(score = 0.8f)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentWord_areNotEqual() {
        val a = makePronunciationResult(word = "Haus")
        val b = makePronunciationResult(word = "Auto")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentAttemptNumber_areNotEqual() {
        val a = makePronunciationResult(attemptNumber = 1)
        val b = makePronunciationResult(attemptNumber = 2)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_nullVsNonNullSessionId_areNotEqual() {
        val a = makePronunciationResult(sessionId = null)
        val b = makePronunciationResult(sessionId = "sess_1")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentUserId_areNotEqual() {
        val a = makePronunciationResult(userId = "user_1")
        val b = makePronunciationResult(userId = "user_2")
        assertNotEquals(a, b)
    }
}
