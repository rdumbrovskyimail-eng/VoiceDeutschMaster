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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.data.remote.sync.BackupMetadata
import com.voicedeutsch.master.presentation.theme.Background
import com.voicedeutsch.master.util.AppLogger
import com.voicedeutsch.master.util.CrashLogger
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings screen — полное покрытие UserPreferences + VoiceSettings + Backup.
 *
 * Секции:
 *  1. Обучение (Алгоритмы) — SRS, темп, цели
 *  2. Голос и ИИ (Gemini) — скорость, субтитры, строгость произношения
 *  3. Напоминание
 *  4. Система — экономия трафика
 *  5. Резервная копия — облачный бекап / рестор
 *  6. Диагностика
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToBookManager: () -> Unit = {},
    onNavigateToTests: () -> Unit = {},
    onNavigateToComprehensiveTests: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSavingLog by remember { mutableStateOf(false) }
    var logStats by remember { mutableStateOf(CrashLogger.getInstance()?.getStats()) }
    var showTimePicker by remember { mutableStateOf(false) }

    val saveLogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        isSavingLog = false
        if (uri == null) return@rememberLauncherForActivityResult
        val success = AppLogger.getInstance()?.saveToUri(uri) ?: false
        Toast.makeText(
            context,
            if (success) "✅ Лог сохранён успешно" else "❌ Не удалось сохранить лог",
            Toast.LENGTH_LONG,
        ).show()
        logStats = CrashLogger.getInstance()?.getStats()
    }

    LaunchedEffect(state.successMessage, state.errorMessage) {
        val msg = state.successMessage ?: state.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(SettingsEvent.DismissMessages)
        }
    }

    // ── Restore dialog ────────────────────────────────────────────────────────
    if (state.showRestoreDialog) {
        RestoreDialog(
            backups    = state.cloudBackups,
            isLoading  = state.isLoadingBackups,
            onRestore  = { viewModel.onEvent(SettingsEvent.RestoreFromCloud(it)) },
            onDismiss  = { viewModel.onEvent(SettingsEvent.HideRestoreDialog) },
        )
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

            // ── 0. Профиль пользователя ───────────────────────────────────────
            SettingsSection(title = "Профиль пользователя") {
                OutlinedTextField(
                    value         = state.userName,
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateUserName(it)) },
                    label         = { Text("Имя") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = state.userAge,
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateUserAge(it)) },
                    label         = { Text("Возраст") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = state.userHobbies,
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateUserHobbies(it)) },
                    label         = { Text("Хобби (через запятую)") },
                    modifier      = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = state.userLearningGoals,
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateUserLearningGoals(it)) },
                    label         = { Text("Цели изучения") },
                    modifier      = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = state.userNativeLanguage,
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateUserNativeLanguage(it)) },
                    label         = { Text("Родной язык") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                Text(
                    "Уровень немецкого",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val levelOptions = listOf(
                    "A1" to "A1", "A2" to "A2", "B1" to "B1",
                    "B2" to "B2", "C1" to "C1", "C2" to "C2",
                )
                SegmentedControl(
                    options  = levelOptions,
                    selected = state.userLevel,
                    onSelect = { viewModel.onEvent(SettingsEvent.UpdateUserLevel(it)) },
                )
            }

            // ── 1. Обучение (Алгоритмы) ───────────────────────────────────────
            SettingsSection(title = "Обучение (Алгоритмы)") {
                LabeledSlider(
                    label         = "Длительность сессии: ${state.sessionDurationMinutes} мин",
                    value         = state.sessionDurationMinutes.toFloat(),
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateSessionDuration(it.toInt())) },
                    valueRange    = 5f..60f,
                    steps         = 10,
                )
                LabeledSlider(
                    label         = "Новых слов в день: ${state.dailyGoalWords}",
                    value         = state.dailyGoalWords.toFloat(),
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateDailyGoal(it.toInt())) },
                    valueRange    = 5f..50f,
                    steps         = 8,
                )
                LabeledSlider(
                    label         = "Макс. повторений (SRS): ${state.maxReviewsPerSession}",
                    value         = state.maxReviewsPerSession.toFloat(),
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateMaxReviews(it.toInt())) },
                    valueRange    = 10f..100f,
                    steps         = 8,
                )
                RowSwitch(
                    label           = "Интервальное повторение (SRS)",
                    checked         = state.srsEnabled,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleSrs(it)) },
                )
                val paceOptions = listOf(
                    "SLOW"   to "Медленно",
                    "NORMAL" to "Нормально",
                    "FAST"   to "Быстро",
                )
                Text(
                    "Темп обучения",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SegmentedControl(
                    options  = paceOptions,
                    selected = state.learningPace,
                    onSelect = { viewModel.onEvent(SettingsEvent.UpdateLearningPace(it)) },
                )
            }

            // ── 2. Голос и ИИ (Gemini) ────────────────────────────────────────
            SettingsSection(title = "Голос и ИИ (Gemini)") {
                LabeledSlider(
                    label         = "Скорость немецкой речи: ${"%.1f".format(state.germanVoiceSpeed)}×",
                    value         = state.germanVoiceSpeed,
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateGermanSpeed(it)) },
                    valueRange    = 0.5f..1.5f,
                    steps         = 9,
                )
                RowSwitch(
                    label           = "Показывать субтитры",
                    checked         = state.showTranscription,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleTranscription(it)) },
                )
                val strictnessOptions = listOf(
                    "LENIENT"  to "Мягко",
                    "MODERATE" to "Нормально",
                    "STRICT"   to "Строго",
                )
                Text(
                    "Строгость произношения",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SegmentedControl(
                    options  = strictnessOptions,
                    selected = state.pronunciationStrictness,
                    onSelect = { viewModel.onEvent(SettingsEvent.UpdateStrictness(it)) },
                )
            }

            // ── 2.5. Настройки Gemini ──────────────────────────────────────────
            SettingsSection(title = "Настройки Gemini") {
                OutlinedTextField(
                    value         = state.geminiModelName,
                    onValueChange = { /* model name change requires app restart */ },
                    label         = { Text("Модель Gemini") },
                    singleLine    = true,
                    readOnly      = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                LabeledSlider(
                    label         = "Temperature: ${"%.2f".format(state.geminiTemperature)}",
                    value         = state.geminiTemperature,
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateGeminiTemperature(it)) },
                    valueRange    = 0f..2f,
                    steps         = 19,
                )
                LabeledSlider(
                    label         = "Top P: ${"%.2f".format(state.geminiTopP)}",
                    value         = state.geminiTopP,
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateGeminiTopP(it)) },
                    valueRange    = 0f..1f,
                    steps         = 19,
                )
                LabeledSlider(
                    label         = "Top K: ${state.geminiTopK}",
                    value         = state.geminiTopK.toFloat(),
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateGeminiTopK(it.toInt())) },
                    valueRange    = 1f..100f,
                    steps         = 98,
                )
                Text(
                    "Голос Gemini",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val voiceOptions = listOf(
                    "Kore" to "Kore", "Puck" to "Puck", "Charon" to "Charon",
                    "Fenrir" to "Fenrir", "Zephyr" to "Zephyr",
                )
                SegmentedControl(
                    options  = voiceOptions,
                    selected = state.geminiVoiceName,
                    onSelect = { viewModel.onEvent(SettingsEvent.UpdateGeminiVoiceName(it)) },
                )
                LabeledSlider(
                    label         = "Скорость речи: ${"%.1f".format(state.voiceSpeed)}x",
                    value         = state.voiceSpeed,
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateVoiceSpeed(it)) },
                    valueRange    = 0.5f..2.0f,
                    steps         = 14,
                )
                RowSwitch(
                    label           = "Транскрипция ввода (речь пользователя)",
                    checked         = state.geminiInputTranscription,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleGeminiInputTranscription(it)) },
                )
                RowSwitch(
                    label           = "Транскрипция вывода (речь AI)",
                    checked         = state.geminiOutputTranscription,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleGeminiOutputTranscription(it)) },
                )
            }

            // ── 2.6. Контекст и лимиты ───────────────────────────────────────
            SettingsSection(title = "Контекст и лимиты") {
                Text(
                    "Размер контекста определяет сколько данных загружается в Gemini при старте сессии. " +
                    "Большой контекст (платная версия) позволяет загрузить всю книгу, историю и словарь.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                LabeledSlider(
                    label         = "Макс. контекст: ${state.geminiMaxContextTokens / 1024}K токенов",
                    value         = state.geminiMaxContextTokens.toFloat(),
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateGeminiMaxContext(it.toInt())) },
                    valueRange    = 8_192f..1_048_576f,
                    steps         = 15,
                )
                Text(
                    buildString {
                        val k = state.geminiMaxContextTokens / 1024
                        append("Текущий лимит: ${k}K токенов. ")
                        when {
                            k <= 16 -> append("Базовый — только промпт и текущий урок.")
                            k <= 128 -> append("Стандартный — промпт, урок, словарь, история.")
                            k <= 512 -> append("Расширенный — вся книга + полная история.")
                            else -> append("Максимальный — все данные системы загружаются целиком.")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                )
            }

            // ── 2.7. Управление книгами ───────────────────────────────────────
            SettingsSection(title = "Книги") {
                Text(
                    "Создавайте книги с главами для использования в контексте Gemini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick  = onNavigateToBookManager,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Управление книгами")
                }
            }

            // ── 3. Напоминание ────────────────────────────────────────────────
            SettingsSection(title = "Напоминание") {
                RowSwitch(
                    label           = "Включить напоминание",
                    checked         = state.reminderEnabled,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleReminder(it)) },
                )
                if (state.reminderEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
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
                            tint     = MaterialTheme.colorScheme.primary,
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
                            viewModel.onEvent(SettingsEvent.UpdateReminderTime(timePickerState.hour, timePickerState.minute))
                            showTimePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
                    },
                )
            }

            // ── 4. Система ────────────────────────────────────────────────────
            SettingsSection(title = "Система") {
                RowSwitch(
                    label           = "Экономия трафика (Data Saving)",
                    checked         = state.dataSavingMode,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleDataSaving(it)) },
                )
            }

            // ── 5. Резервная копия ────────────────────────────────────────────
            SettingsSection(title = "Резервная копия") {
                Text(
                    text  = "Бекап хранится в Firebase Storage. Привязан к вашему анонимному аккаунту. Максимум 5 копий.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))

                // Кнопка: создать бекап
                Button(
                    onClick  = { viewModel.onEvent(SettingsEvent.CreateCloudBackup) },
                    enabled  = !state.isBackingUp && !state.isRestoring,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isBackingUp) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            color       = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Создаю бекап...")
                    } else {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Создать бекап в облако")
                    }
                }

                // Кнопка: восстановить
                OutlinedButton(
                    onClick  = { viewModel.onEvent(SettingsEvent.ShowRestoreDialog) },
                    enabled  = !state.isBackingUp && !state.isRestoring,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isRestoring) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Восстанавливаю...")
                    } else {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Восстановить из облака")
                    }
                }

                Text(
                    text  = "⚠️ После восстановления потребуется перезапуск приложения.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }

            // ── 5.5. Тесты ────────────────────────────────────────────────────
            SettingsSection(title = "Тесты") {
                Text(
                    "Runtime-проверки: сеть, БД, микрофон, Firebase, DataStore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick  = onNavigateToTests,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Запустить тесты")
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick  = onNavigateToComprehensiveTests,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E40AF),
                    ),
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("🧪 Полная тест-лаборатория (101%)")
                }
            }

            // ── 6. Диагностика ────────────────────────────────────────────────
            DiagnosticsSection(
                logStats  = logStats,
                isSaving  = isSavingLog,
                onSaveLog = {
                    isSavingLog = true
                    val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                    saveLogLauncher.launch("voicedeutsch_log_$ts.txt")
                },
            )

            // ── 7. О приложении ────────────────────────────────────────────────
            SettingsSection(title = "О приложении") {
                Text(
                    "VoiceDeutsch Master",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Персональный AI-репетитор немецкого языка",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Модель: ${state.geminiModelName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Кнопка сохранения ─────────────────────────────────────────────
            Button(
                onClick  = { viewModel.onEvent(SettingsEvent.SaveAll) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text("Сохранить изменения")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Restore dialog ────────────────────────────────────────────────────────────

@Composable
private fun RestoreDialog(
    backups: List<BackupMetadata>,
    isLoading: Boolean,
    onRestore: (BackupMetadata) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выбрать бекап для восстановления") },
        text  = {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                backups.isEmpty() -> {
                    Text(
                        "Нет доступных бекапов в облаке.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        backups.forEach { backup ->
                            val date = SimpleDateFormat(
                                "dd.MM.yyyy HH:mm",
                                Locale.getDefault(),
                            ).format(Date(backup.timestamp))
                            val sizeKb = backup.sizeBytes / 1024

                            OutlinedButton(
                                onClick  = { onRestore(backup) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier            = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start,
                                ) {
                                    Text(
                                        text  = date,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text  = "${sizeKb} КБ · ${backup.deviceModel} · v${backup.appVersion}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

// ── Diagnostics section ───────────────────────────────────────────────────────

@Composable
private fun DiagnosticsSection(
    logStats: com.voicedeutsch.master.util.LogStats?,
    isSaving: Boolean,
    onSaveLog: () -> Unit,
) {
    SettingsSection(title = "Диагностика") {
        val appLogger = AppLogger.getInstance()
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint     = if (appLogger?.isRunning == true) Color(0xFF22C55E) else Color(0xFFF59E0B),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = if (appLogger?.isRunning == true)
                    "Логирование активно · ${appLogger.lineCount()} строк в буфере"
                else
                    "Логирование не запущено",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (logStats != null) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = null,
                    tint     = if (logStats.totalCrashes > 0) Color(0xFFEF4444)
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = "Крэшей: ${logStats.totalCrashes} · Сессий: ${logStats.totalSessions} · ${logStats.totalSizeKB} КБ на диске",
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
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Сохранить лог")
            }
        }
        Text(
            text  = "Откроет проводник для выбора места сохранения.\nПри крэше лог сохраняется автоматически.",
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

@Composable
private fun RowSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentedControl(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                shape    = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                selected = selected == value,
                onClick  = { onSelect(value) },
                label    = { Text(label) },
            )
        }
    }
}