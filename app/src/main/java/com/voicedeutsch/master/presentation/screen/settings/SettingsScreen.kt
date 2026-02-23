package com.voicedeutsch.master.presentation.screen.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.presentation.theme.Background
import com.voicedeutsch.master.util.AppLogger
import com.voicedeutsch.master.util.CrashLogger
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings screen — theme, session duration, daily goal, reminders.
 * + секция "Диагностика" с кнопкой сохранения лога.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var isSavingLog by remember { mutableStateOf(false) }
    var logStats by remember { mutableStateOf(CrashLogger.getInstance()?.getStats()) }
    var showTimePicker by remember { mutableStateOf(false) }

    val saveLogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        isSavingLog = false
        if (uri == null) return@rememberLauncherForActivityResult

        val appLogger = AppLogger.getInstance()
        val success = appLogger?.saveToUri(uri) ?: false

        val message = if (success) "✅ Лог сохранён успешно" else "❌ Не удалось сохранить лог"
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()

        logStats = CrashLogger.getInstance()?.getStats()
    }

    LaunchedEffect(state.successMessage, state.errorMessage) {
        val msg = state.successMessage ?: state.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(SettingsEvent.DismissMessages)
        }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Настройки", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {

            // ── Theme ─────────────────────────────────────────────────────────
            SettingsSection(title = "Оформление") {
                val themes = listOf(
                    "system" to "Системная",
                    "dark"   to "Тёмная",
                    "light"  to "Светлая",
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    themes.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            shape    = SegmentedButtonDefaults.itemShape(index = index, count = themes.size),
                            onClick  = { viewModel.onEvent(SettingsEvent.UpdateTheme(value)) },
                            selected = state.theme == value,
                            label    = { Text(label) },
                        )
                    }
                }
            }

            // ── Session preferences ───────────────────────────────────────────
            SettingsSection(title = "Занятия") {
                LabeledSlider(
                    label         = "Длительность: ${state.sessionDurationMinutes} мин",
                    value         = state.sessionDurationMinutes.toFloat(),
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateSessionDuration(it.toInt())) },
                    valueRange    = 10f..60f,
                    steps         = 9,
                )
                Spacer(Modifier.height(8.dp))
                LabeledSlider(
                    label         = "Цель: ${state.dailyGoalWords} слов/день",
                    value         = state.dailyGoalWords.toFloat(),
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateDailyGoal(it.toInt())) },
                    valueRange    = 5f..50f,
                    steps         = 8,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick  = {
                        focusManager.clearFocus()
                        viewModel.onEvent(SettingsEvent.SaveLearningPrefs)
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Сохранить настройки")
                }
            }

            // ── Reminder ──────────────────────────────────────────────────────
            SettingsSection(title = "Напоминание") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Включить напоминание", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked         = state.reminderEnabled,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleReminder(it)) },
                    )
                }
                if (state.reminderEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Время: ${state.reminderHour.toString().padStart(2, '0')}:" +
                                state.reminderMinute.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Изменить время",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            // ── TimePicker диалог ─────────────────────────────────────────────
            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(
                    initialHour   = state.reminderHour,
                    initialMinute = state.reminderMinute,
                    is24Hour      = true,
                )
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    title = { Text("Время напоминания") },
                    text  = {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                            TimePicker(state = timePickerState)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.onEvent(
                                SettingsEvent.UpdateReminderTime(
                                    timePickerState.hour,
                                    timePickerState.minute,
                                )
                            )
                            showTimePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
                    },
                )
            }

            // ── 🐛 Диагностика ────────────────────────────────────────────────
            DiagnosticsSection(
                logStats  = logStats,
                isSaving  = isSavingLog,
                onSaveLog = {
                    isSavingLog = true
                    val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                    saveLogLauncher.launch("voicedeutsch_log_$ts.txt")
                },
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Секция диагностики
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DiagnosticsSection(
    logStats: com.voicedeutsch.master.util.LogStats?,
    isSaving: Boolean,
    onSaveLog: () -> Unit,
) {
    SettingsSection(title = "Диагностика") {

        val appLogger = AppLogger.getInstance()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = if (appLogger?.isRunning == true)
                    Color(0xFF22C55E) else Color(0xFFF59E0B),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (appLogger?.isRunning == true)
                    "Логирование активно · ${appLogger.lineCount()} строк в буфере"
                else
                    "Логирование не запущено",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (logStats != null) {
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = null,
                    tint = if (logStats.totalCrashes > 0) Color(0xFFEF4444)
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = buildString {
                        append("Крэшей: ${logStats.totalCrashes}")
                        append(" · Сессий: ${logStats.totalSessions}")
                        append(" · ${logStats.totalSizeKB} КБ на диске")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick  = onSaveLog,
            enabled  = !isSaving && appLogger?.isRunning == true,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(18.dp),
                    color       = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text("Открываю проводник...")
            } else {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Сохранить лог")
            }
        }

        Text(
            text = "Откроет проводник для выбора места сохранения.\n" +
                   "При крэше лог сохраняется автоматически.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text          = title.uppercase(),
            style         = MaterialTheme.typography.labelSmall,
            color         = MaterialTheme.colorScheme.primary,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
        )
        Card(
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content             = content,
            )
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value         = value,
            onValueChange = onValueChange,
            valueRange    = valueRange,
            steps         = steps,
            modifier      = Modifier.fillMaxWidth(),
        )
    }
}