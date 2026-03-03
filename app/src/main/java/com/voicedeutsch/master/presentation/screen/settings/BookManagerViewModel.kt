package com.voicedeutsch.master.presentation.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicedeutsch.master.data.local.database.dao.BookDao
import com.voicedeutsch.master.data.local.database.entity.BookChapterEntity
import com.voicedeutsch.master.data.local.database.entity.BookEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookManagerUiState(
    val books: List<BookEntity> = emptyList(),
    val chapters: List<BookChapterEntity> = emptyList(),
    val selectedBook: BookEntity? = null,
)

sealed interface BookManagerEvent {
    data class CreateBook(val title: String, val description: String) : BookManagerEvent
    data class DeleteBook(val book: BookEntity) : BookManagerEvent
    data class SelectBook(val book: BookEntity) : BookManagerEvent
    data object DeselectBook : BookManagerEvent
    data class CreateChapter(val title: String, val content: String) : BookManagerEvent
    data class UpdateChapter(val id: Long, val title: String, val content: String) : BookManagerEvent
    data class DeleteChapter(val chapter: BookChapterEntity) : BookManagerEvent
}

class BookManagerViewModel(
    private val bookDao: BookDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookManagerUiState())
    val uiState: StateFlow<BookManagerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bookDao.getAllBooksFlow().collect { books ->
                _uiState.update { it.copy(books = books) }
            }
        }
    }

    fun onEvent(event: BookManagerEvent) {
        when (event) {
            is BookManagerEvent.CreateBook -> createBook(event.title, event.description)
            is BookManagerEvent.DeleteBook -> deleteBook(event.book)
            is BookManagerEvent.SelectBook -> selectBook(event.book)
            is BookManagerEvent.DeselectBook -> _uiState.update { it.copy(selectedBook = null, chapters = emptyList()) }
            is BookManagerEvent.CreateChapter -> createChapter(event.title, event.content)
            is BookManagerEvent.UpdateChapter -> updateChapter(event.id, event.title, event.content)
            is BookManagerEvent.DeleteChapter -> deleteChapter(event.chapter)
        }
    }

    private fun createBook(title: String, description: String) {
        viewModelScope.launch {
            bookDao.insertBook(
                BookEntity(
                    title = title,
                    description = description.ifBlank { null },
                )
            )
        }
    }

    private fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            bookDao.deleteBook(book)
            if (_uiState.value.selectedBook?.id == book.id) {
                _uiState.update { it.copy(selectedBook = null, chapters = emptyList()) }
            }
        }
    }

    private fun selectBook(book: BookEntity) {
        _uiState.update { it.copy(selectedBook = book) }
        viewModelScope.launch {
            bookDao.getChaptersFlow(book.id).collect { chapters ->
                _uiState.update { it.copy(chapters = chapters) }
            }
        }
    }

    private fun createChapter(title: String, content: String) {
        val bookId = _uiState.value.selectedBook?.id ?: return
        viewModelScope.launch {
            val nextNumber = bookDao.getChapterCount(bookId) + 1
            bookDao.insertChapter(
                BookChapterEntity(
                    bookId = bookId,
                    chapterNumber = nextNumber,
                    title = title,
                    content = content,
                )
            )
        }
    }

    private fun updateChapter(id: Long, title: String, content: String) {
        viewModelScope.launch {
            val existing = bookDao.getChapterById(id) ?: return@launch
            bookDao.updateChapter(existing.copy(title = title, content = content))
        }
    }

    private fun deleteChapter(chapter: BookChapterEntity) {
        viewModelScope.launch {
            bookDao.deleteChapter(chapter)
        }
    }
}
