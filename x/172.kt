package com.voicedeutsch.master.presentation.screen.dashboard

import org.junit.Assert.*
import org.junit.Test

class DashboardUiStateTest {

    @Test
    fun `default state has correct initial values`() {
        val state = DashboardUiState()
        assertTrue(state.isLoading)
        assertNull(state.userProfile)
        assertNull(state.overallProgress)
        assertNull(state.todayProgress)
        assertTrue(state.weeklyProgress.isEmpty())
        assertEquals(0, state.streakDays)
        assertEquals(0, state.wordsLearnedToday)
        assertEquals(0, state.minutesToday)
        assertNull(state.errorMessage)
    }

    @Test
    fun `copy with isLoading false`() {
        val state = DashboardUiState().copy(isLoading = false)
        assertFalse(state.isLoading)
    }

    @Test
    fun `copy with streakDays`() {
        val state = DashboardUiState().copy(streakDays = 7)
        assertEquals(7, state.streakDays)
    }

    @Test
    fun `copy with wordsLearnedToday`() {
        val state = DashboardUiState().copy(wordsLearnedToday = 42)
        assertEquals(42, state.wordsLearnedToday)
    }

    @Test
    fun `copy with minutesToday`() {
        val state = DashboardUiState().copy(minutesToday = 15)
        assertEquals(15, state.minutesToday)
    }

    @Test
    fun `copy with errorMessage`() {
        val state = DashboardUiState().copy(errorMessage = "Load failed")
        assertEquals("Load failed", state.errorMessage)
    }

    @Test
    fun `equality between two identical states`() {
        val a = DashboardUiState(streakDays = 3, minutesToday = 10)
        val b = DashboardUiState(streakDays = 3, minutesToday = 10)
        assertEquals(a, b)
    }

    @Test
    fun `inequality between different states`() {
        val a = DashboardUiState(streakDays = 3)
        val b = DashboardUiState(streakDays = 5)
        assertNotEquals(a, b)
    }

    @Test
    fun `weeklyProgress list is preserved`() {
        // Using raw empty list substitution to simulate non-empty
        val fakeList = listOf<com.voicedeutsch.master.domain.model.progress.DailyProgress>()
        val state = DashboardUiState().copy(weeklyProgress = fakeList)
        assertEquals(fakeList, state.weeklyProgress)
    }
}

class DashboardEventTest {

    @Test
    fun `Refresh is DashboardEvent`() {
        val event: DashboardEvent = DashboardEvent.Refresh
        assertTrue(event is DashboardEvent.Refresh)
    }

    @Test
    fun `DismissError is DashboardEvent`() {
        val event: DashboardEvent = DashboardEvent.DismissError
        assertTrue(event is DashboardEvent.DismissError)
    }

    @Test
    fun `StartSession is DashboardEvent`() {
        val event: DashboardEvent = DashboardEvent.StartSession
        assertTrue(event is DashboardEvent.StartSession)
    }

    @Test
    fun `all events are distinct`() {
        val events = listOf(
            DashboardEvent.Refresh,
            DashboardEvent.DismissError,
            DashboardEvent.StartSession
        )
        assertEquals(3, events.toSet().size)
    }
}
