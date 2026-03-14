package com.voicedeutsch.master.presentation.screen.knowledgebase

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.presentation.components.GenericEmptyState
import com.voicedeutsch.master.presentation.theme.Background
import org.koin.androidx.compose.koinViewModel

data class KnowledgeBaseItem(
    val id: String,
    val type: String,
    val german: String,
    val russian: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class KnowledgeBaseUiState(
    val isLoading: Boolean = true,
    val items: List<KnowledgeBaseItem> = emptyList(),
    val errorMessage: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseScreen(
    onBack: () -> Unit,
) {
    var items by remember { mutableStateOf<List<KnowledgeBaseItem>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Моя база знаний") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Добавить")
                    }
                },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                GenericEmptyState(
                    title = "База знаний пуста",
                    description = "Добавьте слова, фразы и заметки для ИИ-репетитора",
                    icon = Icons.Outlined.Lightbulb,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.german,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    item.russian,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (item.notes.isNotBlank()) {
                                    Text(
                                        item.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    )
                                }
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    item.type,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            IconButton(onClick = { items = items.filter { it.id != item.id } }) {
                                Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddKnowledgeItemDialog(
            onConfirm = { type, german, russian, notes ->
                items = items + KnowledgeBaseItem(
                    id = java.util.UUID.randomUUID().toString(),
                    type = type,
                    german = german,
                    russian = russian,
                    notes = notes,
                )
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun AddKnowledgeItemDialog(
    onConfirm: (type: String, german: String, russian: String, notes: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var type by remember { mutableStateOf("слово") }
    var german by remember { mutableStateOf("") }
    var russian by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val types = listOf("слово", "фраза", "правило", "заметка")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить в базу") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    types.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) },
                        )
                    }
                }
                OutlinedTextField(
                    value = german, onValueChange = { german = it },
                    label = { Text("Немецкий") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = russian, onValueChange = { russian = it },
                    label = { Text("Русский") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Заметка (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(type, german.trim(), russian.trim(), notes.trim()) },
                enabled = german.isNotBlank() && russian.isNotBlank(),
            ) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}