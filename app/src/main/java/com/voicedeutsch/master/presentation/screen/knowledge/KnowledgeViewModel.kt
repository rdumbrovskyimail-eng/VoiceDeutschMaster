package com.voicedeutsch.master.presentation.screen.knowledge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicedeutsch.master.domain.usecase.knowledge.GetUserKnowledgeUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.GetWeakPointsUseCase
import com.voicedeutsch.master.domain.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════════
// State & Event
// ═══════════════════════════════════════════════════════════════════════════════

data class KnowledgeUiState(
    val isLoading: Boolean = true,
    val overview: GetUserKnowledgeUseCase.UserKnowledgeOverview? = null,
    // FIX: GetWeakPointsUseCase returns List<WeakPoint>, not a WeakPoints wrapper type.
    //      Changed from GetWeakPointsUseCase.WeakPoints? to List<GetWeakPointsUseCase.WeakPoint>?
    val weakPoints: List<GetWeakPointsUseCase.WeakPoint>? = null,
    val errorMessage: String? = null,
    val selectedTab: KnowledgeTab = KnowledgeTab.OVERVIEW,
)

enum class KnowledgeTab { OVERVIEW, WORDS, GRAMMAR, WEAK_POINTS }

sealed interface KnowledgeEvent {
    data object Refresh : KnowledgeEvent
    data class SelectTab(val tab: KnowledgeTab) : KnowledgeEvent
    data object DismissError : KnowledgeEvent
}

// ═══════════════════════════════════════════════════════════════════════════════
// ViewModel
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * ViewModel for [KnowledgeScreen].
 *
 * Loads the user's knowledge overview and weak points in parallel.
 *
 * @param getUserKnowledge  Full overview: words, grammar, phrases, topics.
 * @param getWeakPoints     Words/rules with poor retention that need more practice.
 * @param userRepository    Resolves the active user ID.
 */
class KnowledgeViewModel(
    private val getUserKnowledge: GetUserKnowledgeUseCase,
    private val getWeakPoints: GetWeakPointsUseCase,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(KnowledgeUiState())
    val uiState: StateFlow<KnowledgeUiState> = _uiState.asStateFlow()

    init { loadData() }

    fun onEvent(event: KnowledgeEvent) {
        when (event) {
            is KnowledgeEvent.Refresh      -> loadData()
            is KnowledgeEvent.SelectTab    -> _uiState.update { it.copy(selectedTab = event.tab) }
            is KnowledgeEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val userId = userRepository.getActiveUserId() ?: error("No active user")
                val overviewDef = async { getUserKnowledge(userId) }
                val weakDef     = async { getWeakPoints(userId) }
                _uiState.update {
                    it.copy(
                        isLoading  = false,
                        overview   = overviewDef.await(),
                        // FIX: weakDef.await() returns List<WeakPoint>, assign directly
                        weakPoints = weakDef.await(),
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}
