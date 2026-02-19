package com.voicedeutsch.master.presentation.screen.dashboard

import com.voicedeutsch.master.domain.model.progress.DailyProgress
import com.voicedeutsch.master.domain.model.progress.OverallProgress
import com.voicedeutsch.master.domain.model.user.UserProfile

// ═══════════════════════════════════════════════════════════════════════════════
// DashboardUiState
// ═══════════════════════════════════════════════════════════════════════════════

data class DashboardUiState(
    val isLoading: Boolean = true,
    val userProfile: UserProfile? = null,
    val overallProgress: OverallProgress? = null,
    val todayProgress: DailyProgress? = null,
    val weeklyProgress: List<DailyProgress> = emptyList(),
    val streakDays: Int = 0,
    val wordsLearnedToday: Int = 0,
    val minutesToday: Int = 0,
    val errorMessage: String? = null,
)

// ═══════════════════════════════════════════════════════════════════════════════
// DashboardEvent
// ═══════════════════════════════════════════════════════════════════════════════

sealed interface DashboardEvent {
    data object Refresh : DashboardEvent
    data object DismissError : DashboardEvent
    data object StartSession : DashboardEvent
}
