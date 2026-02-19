package com.voicedeutsch.master.presentation.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.presentation.theme.Background
import org.koin.androidx.compose.koinViewModel

/**
 * Settings screen — API key, theme, session duration, daily goal, reminders.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar for success/error
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

            // ── Gemini API Key ────────────────────────────────────────────────
            SettingsSection(title = "Подключение") {
                OutlinedTextField(
                    value         = state.geminiApiKey,
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateApiKey(it)) },
                    label         = { Text("Gemini API Key") },
                    placeholder   = { Text("AIza...") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    visualTransformation = if (state.geminiApiKeyVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon  = {
                        IconButton(onClick = { viewModel.onEvent(SettingsEvent.ToggleApiKeyVisibility) }) {
                            Icon(
                                if (state.geminiApiKeyVisible) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                contentDescription = "Показать/скрыть",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        viewModel.onEvent(SettingsEvent.SaveApiKey)
                    }),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Получите ключ на aistudio.google.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
                Button(
                    onClick  = { viewModel.onEvent(SettingsEvent.SaveApiKey) },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Сохранить")
                }
            }

            // ── Theme ─────────────────────────────────────────────────────────
            SettingsSection(title = "Оформление") {
                val themes = listOf("system" to "Системная", "dark" to "Тёмная", "light" to "Светлая")
                themes.forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        RadioButton(
                            selected = state.theme == value,
                            onClick  = { viewModel.onEvent(SettingsEvent.UpdateTheme(value)) },
                        )
                    }
                }
            }

            // ── Session preferences ───────────────────────────────────────────
            SettingsSection(title = "Занятия") {
                LabeledSlider(
                    label    = "Длительность сессии: ${state.sessionDurationMinutes} мин",
                    value    = state.sessionDurationMinutes.toFloat(),
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateSessionDuration(it.toInt())) },
                    valueRange = 10f..60f,
                    steps    = 9,
                )
                Spacer(Modifier.height(8.dp))
                LabeledSlider(
                    label    = "Цель: ${state.dailyGoalWords} слов/день",
                    value    = state.dailyGoalWords.toFloat(),
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateDailyGoal(it.toInt())) },
                    valueRange = 5f..50f,
                    steps    = 8,
                )
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
                        checked  = state.reminderEnabled,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleReminder(it)) },
                    )
                }
                if (state.reminderEnabled) {
                    Text(
                        text  = "Время: ${state.reminderHour.toString().padStart(2, '0')}:${state.reminderMinute.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
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
            text       = title.uppercase(),
            style      = MaterialTheme.typography.labelSmall,
            color      = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
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
