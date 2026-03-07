// Path: src/test/java/com/voicedeutsch/master/presentation/screen/dashboard/DashboardEventTest.kt
package com.voicedeutsch.master.presentation.screen.dashboard

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DashboardEventTest {

    // ── type identity ─────────────────────────────────────────────────────────

    @Test
    fun refresh_isCorrectType() {
        val event: DashboardEvent = DashboardEvent.Refresh
        assertTrue(event is DashboardEvent.Refresh)
    }

    @Test
    fun dismissError_isCorrectType() {
        val event: DashboardEvent = DashboardEvent.DismissError
        assertTrue(event is DashboardEvent.DismissError)
    }

    @Test
    fun startSession_isCorrectType() {
        val event: DashboardEvent = DashboardEvent.StartSession
        assertTrue(event is DashboardEvent.StartSession)
    }

    // ── when exhaustiveness ───────────────────────────────────────────────────

    @Test
    fun refresh_whenBranch_isHandled() {
        val event: DashboardEvent = DashboardEvent.Refresh
        val result = when (event) {
            is DashboardEvent.Refresh -> "refresh"
            is DashboardEvent.DismissError -> "dismiss"
            is DashboardEvent.StartSession -> "start"
        }
        assertEquals("refresh", result)
    }

    @Test
    fun dismissError_whenBranch_isHandled() {
        val event: DashboardEvent = DashboardEvent.DismissError
        val result = when (event) {
            is DashboardEvent.Refresh -> "refresh"
            is DashboardEvent.DismissError -> "dismiss"
            is DashboardEvent.StartSession -> "start"
        }
        assertEquals("dismiss", result)
    }

    @Test
    fun startSession_whenBranch_isHandled() {
        val event: DashboardEvent = DashboardEvent.StartSession
        val result = when (event) {
            is DashboardEvent.Refresh -> "refresh"
            is DashboardEvent.DismissError -> "dismiss"
            is DashboardEvent.StartSession -> "start"
        }
        assertEquals("start", result)
    }

    // ── object identity ───────────────────────────────────────────────────────

    @Test
    fun refresh_sameInstance_isSingleton() {
        val a: DashboardEvent = DashboardEvent.Refresh
        val b: DashboardEvent = DashboardEvent.Refresh
        assertSame(a, b)
    }

    @Test
    fun dismissError_sameInstance_isSingleton() {
        val a: DashboardEvent = DashboardEvent.DismissError
        val b: DashboardEvent = DashboardEvent.DismissError
        assertSame(a, b)
    }

    @Test
    fun startSession_sameInstance_isSingleton() {
        val a: DashboardEvent = DashboardEvent.StartSession
        val b: DashboardEvent = DashboardEvent.StartSession
        assertSame(a, b)
    }

    // ── distinct types ────────────────────────────────────────────────────────

    @Test
    fun refresh_isNotDismissError() {
        val refresh: DashboardEvent = DashboardEvent.Refresh
        assertFalse(refresh is DashboardEvent.DismissError)
    }

    @Test
    fun refresh_isNotStartSession() {
        val refresh: DashboardEvent = DashboardEvent.Refresh
        assertFalse(refresh is DashboardEvent.StartSession)
    }

    @Test
    fun dismissError_isNotStartSession() {
        val dismiss: DashboardEvent = DashboardEvent.DismissError
        assertFalse(dismiss is DashboardEvent.StartSession)
    }

    @Test
    fun startSession_isNotRefresh() {
        val start: DashboardEvent = DashboardEvent.StartSession
        assertFalse(start is DashboardEvent.Refresh)
    }

    // ── equals ────────────────────────────────────────────────────────────────

    @Test
    fun refresh_equalsItself() {
        assertEquals(DashboardEvent.Refresh, DashboardEvent.Refresh)
    }

    @Test
    fun dismissError_equalsItself() {
        assertEquals(DashboardEvent.DismissError, DashboardEvent.DismissError)
    }

    @Test
    fun startSession_equalsItself() {
        assertEquals(DashboardEvent.StartSession, DashboardEvent.StartSession)
    }

    @Test
    fun refresh_doesNotEqualDismissError() {
        assertNotEquals(DashboardEvent.Refresh, DashboardEvent.DismissError)
    }

    @Test
    fun startSession_doesNotEqualRefresh() {
        assertNotEquals(DashboardEvent.StartSession, DashboardEvent.Refresh)
    }

    // ── event dispatch simulation ─────────────────────────────────────────────

    @Test
    fun eventList_containsAllThreeTypes() {
        val events: List<DashboardEvent> = listOf(
            DashboardEvent.Refresh,
            DashboardEvent.DismissError,
            DashboardEvent.StartSession,
        )
        assertEquals(3, events.size)
        assertTrue(events.any { it is DashboardEvent.Refresh })
        assertTrue(events.any { it is DashboardEvent.DismissError })
        assertTrue(events.any { it is DashboardEvent.StartSession })
    }
}
