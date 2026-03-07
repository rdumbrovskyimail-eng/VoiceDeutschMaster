// Путь: src/test/java/com/voicedeutsch/master/domain/model/session/SessionEventTest.kt
package com.voicedeutsch.master.domain.model.session

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// ═══════════════════════════════════════════════════════════════════════════
// SessionEvent
// ═══════════════════════════════════════════════════════════════════════════

class SessionEventTest {

    private fun createSessionEvent(
        id: String = "evt_1",
        sessionId: String = "session_1",
        eventType: SessionEventType = SessionEventType.WORD_LEARNED,
        timestamp: Long = 1_000_000L,
        detailsJson: String = "{}",
        createdAt: Long = 1_000_000L
    ) = SessionEvent(
        id = id,
        sessionId = sessionId,
        eventType = eventType,
        timestamp = timestamp,
        detailsJson = detailsJson,
        createdAt = createdAt
    )

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val event = SessionEvent(id = "evt_1", sessionId = "s_1", eventType = SessionEventType.SESSION_START)
        assertEquals("{}", event.detailsJson)
        assertTrue(event.timestamp > 0)
        assertTrue(event.createdAt > 0)
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_allFields_storedCorrectly() {
        val details = "{\"wordId\":\"w_42\",\"score\":0.9}"
        val event = createSessionEvent(
            id = "evt_99",
            sessionId = "s_42",
            eventType = SessionEventType.PRONUNCIATION_ATTEMPT,
            timestamp = 2_000_000L,
            detailsJson = details,
            createdAt = 2_000_001L
        )
        assertEquals("evt_99", event.id)
        assertEquals("s_42", event.sessionId)
        assertEquals(SessionEventType.PRONUNCIATION_ATTEMPT, event.eventType)
        assertEquals(2_000_000L, event.timestamp)
        assertEquals(details, event.detailsJson)
        assertEquals(2_000_001L, event.createdAt)
    }

    @Test
    fun constructor_emptyDetailsJson_storedCorrectly() {
        val event = createSessionEvent(detailsJson = "{}")
        assertEquals("{}", event.detailsJson)
    }

    @Test
    fun constructor_richDetailsJson_storedCorrectly() {
        val json = "{\"strategy\":\"REPETITION\",\"reason\":\"srs_queue_full\"}"
        val event = createSessionEvent(detailsJson = json)
        assertEquals(json, event.detailsJson)
    }

    @Test
    fun constructor_allEventTypes_storedCorrectly() {
        SessionEventType.entries.forEach { type ->
            val event = createSessionEvent(eventType = type)
            assertEquals(type, event.eventType)
        }
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_changeEventType_onlyTypeChanges() {
        val original = createSessionEvent(eventType = SessionEventType.WORD_LEARNED)
        val modified = original.copy(eventType = SessionEventType.WORD_REVIEWED)
        assertEquals(SessionEventType.WORD_REVIEWED, modified.eventType)
        assertEquals(original.id, modified.id)
        assertEquals(original.sessionId, modified.sessionId)
        assertEquals(original.timestamp, modified.timestamp)
    }

    @Test
    fun copy_updateDetailsJson_jsonUpdated() {
        val original = createSessionEvent(detailsJson = "{}")
        val newJson = "{\"key\":\"value\"}"
        val modified = original.copy(detailsJson = newJson)
        assertEquals(newJson, modified.detailsJson)
        assertEquals("{}", original.detailsJson)
    }

    @Test
    fun copy_updateTimestamp_timestampUpdated() {
        val original = createSessionEvent(timestamp = 1_000L)
        val modified = original.copy(timestamp = 5_000L)
        assertEquals(5_000L, modified.timestamp)
        assertEquals(1_000L, original.timestamp)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        assertEquals(createSessionEvent(), createSessionEvent())
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        assertEquals(createSessionEvent().hashCode(), createSessionEvent().hashCode())
    }

    @Test
    fun equals_differentId_notEqual() {
        assertNotEquals(createSessionEvent(id = "evt_1"), createSessionEvent(id = "evt_2"))
    }

    @Test
    fun equals_differentSessionId_notEqual() {
        assertNotEquals(
            createSessionEvent(sessionId = "s_1"),
            createSessionEvent(sessionId = "s_2")
        )
    }

    @Test
    fun equals_differentEventType_notEqual() {
        assertNotEquals(
            createSessionEvent(eventType = SessionEventType.SESSION_START),
            createSessionEvent(eventType = SessionEventType.SESSION_END)
        )
    }

    @Test
    fun equals_differentDetailsJson_notEqual() {
        assertNotEquals(
            createSessionEvent(detailsJson = "{}"),
            createSessionEvent(detailsJson = "{\"key\":\"val\"}")
        )
    }

    @Test
    fun equals_differentTimestamp_notEqual() {
        assertNotEquals(
            createSessionEvent(timestamp = 1_000L),
            createSessionEvent(timestamp = 2_000L)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SessionEventType
// ═══════════════════════════════════════════════════════════════════════════

class SessionEventTypeTest {

    @Test
    fun entries_size_isFourteen() {
        assertEquals(14, SessionEventType.entries.size)
    }

    @Test
    fun entries_containsAllExpectedValues() {
        val expected = setOf(
            SessionEventType.SESSION_START,
            SessionEventType.SESSION_END,
            SessionEventType.WORD_LEARNED,
            SessionEventType.WORD_REVIEWED,
            SessionEventType.RULE_PRACTICED,
            SessionEventType.PRONUNCIATION_ATTEMPT,
            SessionEventType.MISTAKE,
            SessionEventType.STRATEGY_CHANGE,
            SessionEventType.LESSON_COMPLETE,
            SessionEventType.BREAK_TAKEN,
            SessionEventType.ACHIEVEMENT_EARNED,
            SessionEventType.USER_REQUEST,
            SessionEventType.REPETITION_COMPLETE,
            SessionEventType.TOPIC_CHANGED
        )
        assertEquals(expected, SessionEventType.entries.toSet())
    }

    @Test
    fun ordinal_sessionStart_isZero() {
        assertEquals(0, SessionEventType.SESSION_START.ordinal)
    }

    @Test
    fun ordinal_sessionEnd_isOne() {
        assertEquals(1, SessionEventType.SESSION_END.ordinal)
    }

    @Test
    fun ordinal_wordLearned_isTwo() {
        assertEquals(2, SessionEventType.WORD_LEARNED.ordinal)
    }

    @Test
    fun ordinal_wordReviewed_isThree() {
        assertEquals(3, SessionEventType.WORD_REVIEWED.ordinal)
    }

    @Test
    fun ordinal_rulePracticed_isFour() {
        assertEquals(4, SessionEventType.RULE_PRACTICED.ordinal)
    }

    @Test
    fun ordinal_pronunciationAttempt_isFive() {
        assertEquals(5, SessionEventType.PRONUNCIATION_ATTEMPT.ordinal)
    }

    @Test
    fun ordinal_mistake_isSix() {
        assertEquals(6, SessionEventType.MISTAKE.ordinal)
    }

    @Test
    fun ordinal_strategyChange_isSeven() {
        assertEquals(7, SessionEventType.STRATEGY_CHANGE.ordinal)
    }

    @Test
    fun ordinal_lessonComplete_isEight() {
        assertEquals(8, SessionEventType.LESSON_COMPLETE.ordinal)
    }

    @Test
    fun ordinal_breakTaken_isNine() {
        assertEquals(9, SessionEventType.BREAK_TAKEN.ordinal)
    }

    @Test
    fun ordinal_achievementEarned_isTen() {
        assertEquals(10, SessionEventType.ACHIEVEMENT_EARNED.ordinal)
    }

    @Test
    fun ordinal_userRequest_isEleven() {
        assertEquals(11, SessionEventType.USER_REQUEST.ordinal)
    }

    @Test
    fun ordinal_repetitionComplete_isTwelve() {
        assertEquals(12, SessionEventType.REPETITION_COMPLETE.ordinal)
    }

    @Test
    fun ordinal_topicChanged_isThirteen() {
        assertEquals(13, SessionEventType.TOPIC_CHANGED.ordinal)
    }

    @Test
    fun valueOf_sessionStart_returnsSessionStart() {
        assertEquals(SessionEventType.SESSION_START, SessionEventType.valueOf("SESSION_START"))
    }

    @Test
    fun valueOf_topicChanged_returnsTopicChanged() {
        assertEquals(SessionEventType.TOPIC_CHANGED, SessionEventType.valueOf("TOPIC_CHANGED"))
    }

    @Test
    fun valueOf_achievementEarned_returnsAchievementEarned() {
        assertEquals(SessionEventType.ACHIEVEMENT_EARNED, SessionEventType.valueOf("ACHIEVEMENT_EARNED"))
    }

    @Test
    fun valueOf_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            SessionEventType.valueOf("UNKNOWN_EVENT")
        }
    }

    @Test
    fun valueOf_lowercaseValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            SessionEventType.valueOf("session_start")
        }
    }

    @Test
    fun allEntries_names_areUnique() {
        val names = SessionEventType.entries.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun defaultSessionEvent_usesEmptyDetailsJson() {
        val event = SessionEvent(
            id = "evt_1",
            sessionId = "s_1",
            eventType = SessionEventType.WORD_LEARNED
        )
        assertEquals("{}", event.detailsJson)
    }
}
