package com.voicedeutsch.master.presentation.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.domain.usecase.user.GetUserProfileUseCase
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.util.generateUUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── State / Event ─────────────────────────────────────────────────────────────

enum class OnboardingStep { WELCOME, NAME, LEVEL, BOOK_LOAD, DONE }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val name: String = "",
    val selectedLevel: CefrLevel = CefrLevel.A1,
    val isLoadingBook: Boolean = false,
    val bookLoaded: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface OnboardingEvent {
    data object Next : OnboardingEvent
    data object Back : OnboardingEvent
    data class UpdateName(val name: String) : OnboardingEvent
    data class SelectLevel(val level: CefrLevel) : OnboardingEvent
    data object LoadBook : OnboardingEvent
    data object DismissError : OnboardingEvent
    data object Complete : OnboardingEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel for [OnboardingScreen].
 *
 * Manages a step-by-step flow:
 *   Welcome → Name → Level → Book Load → Done
 *
 * On completion it:
 *  1. Creates the user profile via UserRepository
 *  2. Sets onboarding complete flag
 *
 * @param userRepository        Create user, set active user ID.
 * @param bookRepository        Load book assets into the database.
 * @param preferencesDataStore  Save onboarding flags.
 * @param getUserProfile        Verify profile was saved correctly.
 */
class OnboardingViewModel(
    private val userRepository: UserRepository,
    private val bookRepository: BookRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val getUserProfile: GetUserProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.Next          -> nextStep()
            is OnboardingEvent.Back          -> previousStep()
            is OnboardingEvent.UpdateName    -> _uiState.update { it.copy(name = event.name) }
            is OnboardingEvent.SelectLevel   -> _uiState.update { it.copy(selectedLevel = event.level) }
            is OnboardingEvent.LoadBook      -> loadBook()
            is OnboardingEvent.DismissError  -> _uiState.update { it.copy(errorMessage = null) }
            is OnboardingEvent.Complete      -> completeOnboarding()
        }
    }

    private fun nextStep() {
        val current = _uiState.value
        val next = when (current.step) {
            OnboardingStep.WELCOME   -> {
                OnboardingStep.NAME
            }
            OnboardingStep.NAME      -> {
                if (current.name.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Введите ваше имя") }
                    return
                }
                OnboardingStep.LEVEL
            }
            OnboardingStep.LEVEL     -> OnboardingStep.BOOK_LOAD
            OnboardingStep.BOOK_LOAD -> {
                if (_uiState.value.bookLoaded) {
                    completeOnboarding()
                } else {
                    loadBook()
                }
                return
            }
            OnboardingStep.DONE      -> return
        }
        _uiState.update { it.copy(step = next, errorMessage = null) }
    }

    private fun previousStep() {
        val prev = when (_uiState.value.step) {
            OnboardingStep.WELCOME   -> return
            OnboardingStep.NAME      -> OnboardingStep.WELCOME
            OnboardingStep.LEVEL     -> OnboardingStep.NAME
            OnboardingStep.BOOK_LOAD -> OnboardingStep.LEVEL
            OnboardingStep.DONE      -> OnboardingStep.BOOK_LOAD
        }
        _uiState.update { it.copy(step = prev, errorMessage = null) }
    }

    private fun loadBook() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBook = true, errorMessage = null) }
            runCatching {
                if (!bookRepository.isBookLoaded()) {
                    bookRepository.loadBookIntoDatabase()
                }
            }.onSuccess {
                completeOnboarding()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoadingBook = false,
                        errorMessage  = "Не удалось загрузить книгу: ${e.message}",
                    )
                }
            }
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBook = false) }
            runCatching {
                val state = _uiState.value
                val userId = generateUUID()

                val profile = com.voicedeutsch.master.domain.model.user.UserProfile(
                    id        = userId,
                    name      = state.name.trim(),
                    cefrLevel = state.selectedLevel,
                )

                userRepository.createUser(profile)
                userRepository.setActiveUserId(userId)
                preferencesDataStore.setOnboardingComplete(true)

            }.onSuccess {
                _uiState.update { it.copy(bookLoaded = true, step = OnboardingStep.DONE, errorMessage = null) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoadingBook = false, errorMessage = "Ошибка: ${e.message}") }
            }
        }
    }
}