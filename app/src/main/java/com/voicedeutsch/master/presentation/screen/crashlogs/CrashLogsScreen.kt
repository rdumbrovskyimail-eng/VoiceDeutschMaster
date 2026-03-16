package com.voicedeutsch.master.presentation.screen.crashlogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voicedeutsch.master.util.CrashLogger
import com.voicedeutsch.master.util.LogFile
import com.voicedeutsch.master.util.LogType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashLogsScreen(onBack: () -> Unit) {
    val crashLogger = CrashLogger.getInstance()
    var logs by remember { mutableStateOf(crashLogger?.getAllLogs() ?: emptyList()) }
    var selectedLog by remember { mutableStateOf<LogFile?>(null) }

    // Диалог просмотра лога
    selectedLog?.let { log ->
        LogViewerDialog(
            log = log,
            onDismiss = { selectedLog = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Краш логи") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        crashLogger?.cleanOldLogs(keepCount = 0)
                        logs = emptyList()
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить все")
                    }
                },
            )
        },
    ) { padding ->
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "🎉 Крашей не найдено",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(logs) { log ->
                    LogCard(
                        log = log,
                        onClick = { selectedLog = log },
                    )
                }
            }
        }
    }
}

@Composable
private fun LogCard(log: LogFile, onClick: () -> Unit) {
    val typeColor = when (log.type) {
        LogType.CRASH   -> MaterialTheme.colorScheme.error
        LogType.SESSION -> MaterialTheme.colorScheme.primary
        LogType.LOGCAT  -> MaterialTheme.colorScheme.tertiary
        LogType.ANR     -> MaterialTheme.colorScheme.secondary
    }
    val typeLabel = when (log.type) {
        LogType.CRASH   -> "CRASH"
        LogType.SESSION -> "SESSION"
        LogType.LOGCAT  -> "LOGCAT"
        LogType.ANR     -> "ANR"
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = typeColor.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = typeLabel,
                    color = typeColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${log.sizeKB} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LogViewerDialog(log: LogFile, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val content = remember { runCatching { log.file.readText() }.getOrDefault("Не удалось прочитать файл") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.88f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = log.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = log.formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = content,
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(
                                android.content.Context.CLIPBOARD_SERVICE
                            ) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(
                                android.content.ClipData.newPlainText("crash_log", content)
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Копировать") }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) { Text("Закрыть") }
                }
            }
        }
    }
}