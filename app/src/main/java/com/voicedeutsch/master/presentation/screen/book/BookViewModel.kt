package com.voicedeutsch.master.presentation.screen.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicedeutsch.master.domain.model.book.BookProgress
import com.voicedeutsch.master.domain.model.book.Chapter
import com.voicedeutsch.master.domain.model.book.Lesson
import com.voicedeutsch.master.domain.model.book.LessonContent
import com.voicedeutsch.master.domain.usecase.book.GetCurrentLessonUseCase
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── State / Event ─────────────────────────────────────────────────────────────

data class BookUiState(
    val isLoading: Boolean = true,
    val currentLesson: GetCurrentLessonUseCase.CurrentLessonData? = null,
    val allProgress: List<BookProgress> = emptyList(),
    val completionPercent: Float = 0f,
    val errorMessage: String? = null,
)

sealed interface BookEvent {
    data object Refresh : BookEvent
    data object DismissError : BookEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel for [BookScreen].
 *
 * Loads the current lesson data and overall book completion percentage.
 *
 * @param getCurrentLesson  Returns current chapter, lesson, content and vocabulary.
 * @param bookRepository    Used to load all progress records and completion %.
 * @param userRepository    Resolves active user ID.
 */
class BookViewModel(
    private val getCurrentLesson: GetCurrentLessonUseCase,
    private val bookRepository: BookRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookUiState())
    val uiState: StateFlow<BookUiState> = _uiState.asStateFlow()

    init { loadData() }

    fun onEvent(event: BookEvent) {
        when (event) {
            is BookEvent.Refresh     -> loadData()
            is BookEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val userId       = userRepository.getActiveUserId() ?: error("No active user")
                val lessonDef    = async { getCurrentLesson(userId) }
                val progressDef  = async { bookRepository.getAllBookProgress(userId) }
                val percentDef   = async { bookRepository.getBookCompletionPercentage(userId) }

                _uiState.update {
                    it.copy(
                        isLoading        = false,
                        currentLesson    = lessonDef.await(),
                        allProgress      = progressDef.await(),
                        completionPercent = percentDef.await(),
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}
