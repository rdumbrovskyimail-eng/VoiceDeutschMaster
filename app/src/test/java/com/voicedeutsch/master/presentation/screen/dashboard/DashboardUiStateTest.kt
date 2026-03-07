// Path: src/test/java/com/voicedeutsch/master/presentation/screen/dashboard/DashboardUiStateTest.kt
package com.voicedeutsch.master.presentation.screen.dashboard

import com.voicedeutsch.master.domain.model.progress.DailyProgress
import com.voicedeutsch.master.domain.model.progress.OverallProgress
import com.voicedeutsch.master.domain.model.user.UserProfile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DashboardUiStateTest {

    // ── default values ────────────────────────────────────────────────────────

    @Test
    fun defaultState_isLoading_isTrue() {
        val state = DashboardUiState()
        assertTrue(state.isLoading)
    }

    @Test
    fun defaultState_userProfile_isNull() {
        val state = DashboardUiState()
        assertNull(state.userProfile)
    }

    @Test
    fun defaultState_overallProgress_isNull() {
        val state = DashboardUiState()
        assertNull(state.overallProgress)
    }

    @Test
    fun defaultState_todayProgress_isNull() {
        val state = DashboardUiState()
        assertNull(state.todayProgress)
    }

    @Test
    fun defaultState_weeklyProgress_isEmpty() {
        val state = DashboardUiState()
        assertTrue(state.weeklyProgress.isEmpty())
    }

    @Test
    fun defaultState_streakDays_isZero() {
        val state = DashboardUiState()
        assertEquals(0, state.streakDays)
    }

    @Test
    fun defaultState_wordsLearnedToday_isZero() {
        val state = DashboardUiState()
        assertEquals(0, state.wordsLearnedToday)
    }

    @Test
    fun defaultState_minutesToday_isZero() {
        val state = DashboardUiState()
        assertEquals(0, state.minutesToday)
    }

    @Test
    fun defaultState_errorMessage_isNull() {
        val state = DashboardUiState()
        assertNull(state.errorMessage)
    }

    // ── copy ──────────────────────────────────────────────────────────────────

    @Test
    fun copy_isLoadingFalse_setsCorrectly() {
        val original = DashboardUiState(isLoading = true)
        val copy = original.copy(isLoading = false)
        assertFalse(copy.isLoading)
    }

    @Test
    fun copy_setsStreakDays_restUnchanged() {
        val original = DashboardUiState()
        val copy = original.copy(streakDays = 14)
        assertEquals(14, copy.streakDays)
        assertTrue(copy.isLoading)
        assertEquals(0, copy.wordsLearnedToday)
    }

    @Test
    fun copy_setsWordsLearnedToday() {
        val original = DashboardUiState()
        val copy = original.copy(wordsLearnedToday = 25)
        assertEquals(25, copy.wordsLearnedToday)
    }

    @Test
    fun copy_setsMinutesToday() {
        val original = DashboardUiState()
        val copy = original.copy(minutesToday = 45)
        assertEquals(45, copy.minutesToday)
    }

    @Test
    fun copy_setsErrorMessage() {
        val original = DashboardUiState()
        val copy = original.copy(errorMessage = "Network error")
        assertEquals("Network error", copy.errorMessage)
    }

    @Test
    fun copy_clearsErrorMessage() {
        val original = DashboardUiState(errorMessage = "Error")
        val copy = original.copy(errorMessage = null)
        assertNull(copy.errorMessage)
    }

    @Test
    fun copy_setsWeeklyProgress() {
        val daily = DailyProgress(id = "dp_1", userId = "u1", date = "2024-01-01")
        val original = DashboardUiState()
        val copy = original.copy(weeklyProgress = listOf(daily))
        assertEquals(1, copy.weeklyProgress.size)
        assertEquals("dp_1", copy.weeklyProgress[0].id)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────────

    @Test
    fun equals_twoDefaultInstances_areEqual() {
        val a = DashboardUiState()
        val b = DashboardUiState()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentIsLoading_areNotEqual() {
        val a = DashboardUiState(isLoading = true)
        val b = DashboardUiState(isLoading = false)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentStreakDays_areNotEqual() {
        val a = DashboardUiState(streakDays = 5)
        val b = DashboardUiState(streakDays = 10)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentErrorMessage_areNotEqual() {
        val a = DashboardUiState(errorMessage = null)
        val b = DashboardUiState(errorMessage = "Error")
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentMinutesToday_areNotEqual() {
        val a = DashboardUiState(minutesToday = 0)
        val b = DashboardUiState(minutesToday = 30)
        assertNotEquals(a, b)
    }

    // ── loaded state scenario ─────────────────────────────────────────────────

    @Test
    fun loadedState_allFieldsPopulated_isNotLoading() {
        val state = DashboardUiState(
            isLoading = false,
            streakDays = 7,
            wordsLearnedToday = 15,
            minutesToday = 30,
            errorMessage = null,
        )
        assertFalse(state.isLoading)
        assertEquals(7, state.streakDays)
        assertEquals(15, state.wordsLearnedToday)
        assertEquals(30, state.minutesToday)
        assertNull(state.errorMessage)
    }

    @Test
    fun errorState_hasErrorMessage_isNotLoading() {
        val state = DashboardUiState(
            isLoading = false,
            errorMessage = "Failed to load data",
        )
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertEquals("Failed to load data", state.errorMessage)
    }

    @Test
    fun loadingState_isLoading_noData() {
        val state = DashboardUiState(isLoading = true)
        assertTrue(state.isLoading)
        assertNull(state.userProfile)
        assertNull(state.overallProgress)
    }
}
