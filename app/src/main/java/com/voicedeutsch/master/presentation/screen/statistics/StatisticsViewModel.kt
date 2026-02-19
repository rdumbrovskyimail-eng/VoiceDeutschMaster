package com.voicedeutsch.master.presentation.screen.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicedeutsch.master.domain.model.progress.DailyProgress
import com.voicedeutsch.master.domain.model.progress.OverallProgress
import com.voicedeutsch.master.domain.model.progress.SkillProgress
import com.voicedeutsch.master.domain.usecase.progress.CalculateOverallProgressUseCase
import com.voicedeutsch.master.domain.usecase.progress.GetDailyProgressUseCase
import com.voicedeutsch.master.domain.repository.ProgressRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── State / Event ─────────────────────────────────────────────────────────────

enum class StatsTab { OVERVIEW, WEEKLY, MONTHLY, SKILLS }

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val selectedTab: StatsTab = StatsTab.OVERVIEW,
    val overallProgress: OverallProgress? = null,
    val weeklyProgress: List<DailyProgress> = emptyList(),
    val monthlyProgress: List<DailyProgress> = emptyList(),
    val skillProgress: SkillProgress? = null,
    val streak: Int = 0,
    val totalSessions: Int = 0,
    val totalHours: Float = 0f,
    val errorMessage: String? = null,
)

sealed interface StatisticsEvent {
    data object Refresh : StatisticsEvent
    data class SelectTab(val tab: StatsTab) : StatisticsEvent
    data object DismissError : StatisticsEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel for [StatisticsScreen].
 *
 * Loads all statistics data in parallel: overall progress, weekly bars,
 * monthly chart, skill radar data, and streak info.
 *
 * @param calculateOverallProgress  All skill dimensions aggregated.
 * @param getDailyProgress          Weekly / monthly / streak data.
 * @param progressRepository        For skill-level progress radar.
 * @param userRepository            Resolves active user ID.
 */
class StatisticsViewModel(
    private val calculateOverallProgress: CalculateOverallProgressUseCase,
    private val getDailyProgress: GetDailyProgressUseCase,
    private val progressRepository: ProgressRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init { loadData() }

    fun onEvent(event: StatisticsEvent) {
        when (event) {
            is StatisticsEvent.Refresh     -> loadData()
            is StatisticsEvent.SelectTab   -> _uiState.update { it.copy(selectedTab = event.tab) }
            is StatisticsEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val userId         = userRepository.getActiveUserId() ?: error("No active user")
                val overallDef     = async { calculateOverallProgress(userId) }
                val weeklyDef      = async { getDailyProgress.getWeekly(userId) }
                val monthlyDef     = async { getDailyProgress.getMonthly(userId) }
                val skillDef       = async { progressRepository.getSkillProgress(userId) }
                val streakDef      = async { getDailyProgress.getStreak(userId) }

                val overall = overallDef.await()
                _uiState.update {
                    it.copy(
                        isLoading       = false,
                        overallProgress = overall,
                        weeklyProgress  = weeklyDef.await(),
                        monthlyProgress = monthlyDef.await(),
                        skillProgress   = skillDef.await(),
                        streak          = streakDef.await(),
                        totalSessions   = overall.totalSessions,
                        totalHours      = overall.totalHours,
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}
