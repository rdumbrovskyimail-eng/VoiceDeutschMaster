package com.voicedeutsch.master.presentation.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicedeutsch.master.domain.usecase.progress.CalculateOverallProgressUseCase
import com.voicedeutsch.master.domain.usecase.progress.GetDailyProgressUseCase
import com.voicedeutsch.master.domain.usecase.user.GetUserProfileUseCase
import com.voicedeutsch.master.domain.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [DashboardScreen].
 *
 * Loads user profile, overall progress and daily stats in parallel.
 * Exposes a single [uiState] for the UI to collect.
 *
 * Architecture reference: lines 1195-1230 (Dashboard screen dependencies).
 *
 * @param getUserProfile           Fetches user profile (name, CEFR level, streak).
 * @param calculateOverallProgress Aggregates progress across all skill dimensions.
 * @param getDailyProgress         Today's session stats + weekly bar-chart data.
 * @param userRepository           Needed to resolve the active user ID.
 */
class DashboardViewModel(
    private val getUserProfile: GetUserProfileUseCase,
    private val calculateOverallProgress: CalculateOverallProgressUseCase,
    private val getDailyProgress: GetDailyProgressUseCase,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.Refresh    -> loadData()
            is DashboardEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            is DashboardEvent.StartSession -> { /* navigation handled by Screen */ }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching {
                val userId = userRepository.getActiveUserId()
                    ?: error("No active user. Please complete onboarding.")

                // Load all data in parallel
                val profileDeferred  = async { getUserProfile(userId) }
                val progressDeferred = async { calculateOverallProgress(userId) }
                val todayDeferred    = async { getDailyProgress.getToday(userId) }
                val weeklyDeferred   = async { getDailyProgress.getWeekly(userId) }
                val streakDeferred   = async { getDailyProgress.getStreak(userId) }

                val profile  = profileDeferred.await()
                val progress = progressDeferred.await()
                val today    = todayDeferred.await()
                val weekly   = weeklyDeferred.await()
                val streak   = streakDeferred.await()

                _uiState.update {
                    it.copy(
                        isLoading          = false,
                        userProfile        = profile,
                        overallProgress    = progress,
                        todayProgress      = today,
                        weeklyProgress     = weekly,
                        streakDays         = streak,
                        wordsLearnedToday  = today?.wordsLearned ?: 0,
                        minutesToday       = today?.totalMinutes ?: 0,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        errorMessage = error.message ?: "Не удалось загрузить данные",
                    )
                }
            }
        }
    }
}
