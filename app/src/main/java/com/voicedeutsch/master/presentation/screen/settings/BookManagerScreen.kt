package com.voicedeutsch.master.presentation.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.data.local.database.entity.BookChapterEntity
import com.voicedeutsch.master.data.local.database.entity.BookEntity
import com.voicedeutsch.master.presentation.theme.Background
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookManagerScreen(
    onBack: () -> Unit,
    viewModel: BookManagerViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(BookManagerEvent.DismissError)
        }
    }

    var showAddBookDialog by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var editingChapter by remember { mutableStateOf<BookChapterEntity?>(null) }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.selectedBook != null) state.selectedBook!!.title
                        else "Управление книгами",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.selectedBook != null) viewModel.onEvent(BookManagerEvent.DeselectBook)
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (state.selectedBook != null) showAddChapterDialog = true
                        else showAddBookDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить")
                    }
                },
            )
        },
    ) { padding ->
        if (state.selectedBook == null) {
            BookList(
                books = state.books,
                onSelect = { viewModel.onEvent(BookManagerEvent.SelectBook(it)) },
                onDelete = { viewModel.onEvent(BookManagerEvent.DeleteBook(it)) },
                modifier = Modifier.padding(padding),
            )
        } else {
            ChapterList(
                chapters = state.chapters,
                onEdit = { editingChapter = it },
                onDelete = { viewModel.onEvent(BookManagerEvent.DeleteChapter(it)) },
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showAddBookDialog) {
        AddBookDialog(
            onConfirm = { title, desc ->
                viewModel.onEvent(BookManagerEvent.CreateBook(title, desc))
                showAddBookDialog = false
            },
            onDismiss = { showAddBookDialog = false },
        )
    }

    if (showAddChapterDialog) {
        EditChapterDialog(
            chapter = null,
            nextNumber = state.chapters.size + 1,
            onConfirm = { title, content ->
                viewModel.onEvent(BookManagerEvent.CreateChapter(title, content))
                showAddChapterDialog = false
            },
            onDismiss = { showAddChapterDialog = false },
        )
    }

    editingChapter?.let { chapter ->
        EditChapterDialog(
            chapter = chapter,
            nextNumber = chapter.chapterNumber,
            onConfirm = { title, content ->
                viewModel.onEvent(BookManagerEvent.UpdateChapter(chapter.id, title, content))
                editingChapter = null
            },
            onDismiss = { editingChapter = null },
        )
    }
}

@Composable
private fun BookList(
    books: List<BookEntity>,
    onSelect: (BookEntity) -> Unit,
    onDelete: (BookEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (books.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Нет книг",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Нажмите + чтобы добавить книгу",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(books, key = { it.id }) { book ->
            Card(
                onClick = { onSelect(book) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        book.description?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { onDelete(book) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterList(
    chapters: List<BookChapterEntity>,
    onEdit: (BookChapterEntity) -> Unit,
    onDelete: (BookChapterEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chapters.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Нет глав. Нажмите + чтобы добавить.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(chapters, key = { it.id }) { chapter ->
            Card(
                onClick = { onEdit(chapter) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Глава ${chapter.chapterNumber}: ${chapter.title}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${chapter.content.length} символов",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onDelete(chapter) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddBookDialog(
    onConfirm: (title: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая книга") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim(), description.trim()) },
                enabled = title.isNotBlank(),
            ) { Text("Создать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun EditChapterDialog(
    chapter: BookChapterEntity?,
    nextNumber: Int,
    onConfirm: (title: String, content: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(chapter?.title ?: "") }
    var content by remember { mutableStateOf(chapter?.content ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (chapter != null) "Редактировать главу $nextNumber" else "Новая глава $nextNumber") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Заголовок") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Текст главы") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim(), content.trim()) },
                enabled = title.isNotBlank() && content.isNotBlank(),
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
