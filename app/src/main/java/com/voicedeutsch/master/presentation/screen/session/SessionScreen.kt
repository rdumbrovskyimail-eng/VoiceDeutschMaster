package com.voicedeutsch.master.presentation.screen.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.presentation.components.PulsingMicButton
import com.voicedeutsch.master.presentation.components.SessionTimer
import com.voicedeutsch.master.presentation.components.StatusBadge
import com.voicedeutsch.master.presentation.components.VoiceWaveform
import com.voicedeutsch.master.presentation.theme.Background
import com.voicedeutsch.master.presentation.theme.Secondary
import com.voicedeutsch.master.voicecore.session.VoiceEngineState
import org.koin.androidx.compose.koinViewModel

/**
 * Main voice session screen — the heart of VoiceDeutschMaster.
 *
 * Layout (top → bottom):
 *  ┌─────────────────────────────────────────────────────┐
 *  │  TopBar: Status badge  |  Timer  |  [•••] menu      │
 *  ├─────────────────────────────────────────────────────┤
 *  │  VoiceWaveform (Canvas, 120dp)                       │
 *  ├─────────────────────────────────────────────────────┤
 *  │  Transcript area (scrollable):                       │
 *  │    Voice subtitle (green)                            │
 *  │    User subtitle (blue)                              │
 *  ├─────────────────────────────────────────────────────┤
 *  │  [Optional] Text input (accessibility fallback)     │
 *  ├─────────────────────────────────────────────────────┤
 *  │  BottomBar: Pause | 🎤 PulsingMic | Stats | Settings │
 *  └─────────────────────────────────────────────────────┘
 *
 * Architecture reference: lines 1195-1220 (Session Screen design).
 *
 * @param onSessionEnd            Called when user ends the session (back navigation).
 * @param onNavigateToDashboard   Called to go to Dashboard after session summary.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    onSessionEnd: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: SessionViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(SessionEvent.ConsumeSnackbar)
        }
    }

    // Auto-start session when screen opens
    LaunchedEffect(Unit) {
        if (!uiState.isSessionActive && !uiState.isLoading) {
            viewModel.onEvent(SessionEvent.StartSession)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusBadge(
                            engineState = voiceState.engineState,
                            connectionState = voiceState.connectionState,
                        )
                        Spacer(Modifier.weight(1f))
                        if (uiState.isSessionActive) {
                            SessionTimer(
                                durationMs = voiceState.sessionDurationMs,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.onEvent(SessionEvent.EndSession)
                        onSessionEnd()
                    }) {
                        Icon(
                            imageVector        = Icons.Filled.Close,
                            contentDescription = "Завершить сессию",
                            tint               = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                modifier = Modifier.statusBarsPadding(),
            )
        },
        bottomBar = {
            SessionBottomBar(
                uiState = uiState,
                voiceEngineState = voiceState.engineState,
                onEvent = viewModel::onEvent,
                onNavigateToStats = onNavigateToDashboard,
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Background,
                            Background.copy(red = 0.09f, green = 0.10f, blue = 0.13f),
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                // ── Loading indicator ────────────────────────────────────────
                if (uiState.isLoading) {
                    Spacer(Modifier.height(32.dp))
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = "Подключаюсь к Voice...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                }

                // ── Error message ────────────────────────────────────────────
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter   = fadeIn() + slideInVertically { -it },
                    exit    = fadeOut() + slideOutVertically { -it },
                ) {
                    ErrorCard(
                        message = uiState.errorMessage ?: "",
                        onDismiss = { viewModel.onEvent(SessionEvent.DismissError) },
                        onRetry   = { viewModel.onEvent(SessionEvent.StartSession) },
                        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                // ── Voice waveform ───────────────────────────────────────────
                VoiceWaveform(
                    engineState = voiceState.engineState,
                    amplitudes  = voiceState.voiceWaveformData,
                    modifier    = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(horizontal = 16.dp),
                )

                Spacer(Modifier.height(16.dp))

                // ── Transcript area ──────────────────────────────────────────
                TranscriptArea(
                    voiceTranscript = voiceState.voiceTranscript,
                    userTranscript  = voiceState.currentTranscript,
                    modifier        = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                // ── Session stats row ────────────────────────────────────────
                if (uiState.isSessionActive) {
                    SessionStatsRow(
                        wordsLearned   = voiceState.wordsLearnedInSession,
                        wordsReviewed  = voiceState.wordsReviewedInSession,
                        exercisesDone  = voiceState.exercisesCompleted,
                        modifier       = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }

                // ── Text input (accessibility fallback) ──────────────────────
                AnimatedVisibility(
                    visible = uiState.showTextInput,
                    enter   = fadeIn() + slideInVertically { it },
                    exit    = fadeOut() + slideOutVertically { it },
                ) {
                    TextInputField(
                        onSend = { text ->
                            viewModel.onEvent(SessionEvent.SendTextMessage(text))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .imePadding(),
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            // ── Post-session summary overlay ─────────────────────────────────
            AnimatedVisibility(
                visible = uiState.sessionResult != null,
                enter   = fadeIn(),
                exit    = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                uiState.sessionResult?.let { result ->
                    SessionResultCard(
                        result            = result,
                        onDismiss         = { viewModel.onEvent(SessionEvent.DismissResult) },
                        onGoToDashboard   = onNavigateToDashboard,
                        onStartNewSession = { viewModel.onEvent(SessionEvent.StartSession) },
                        modifier          = Modifier
                            .padding(24.dp)
                            .widthIn(max = 400.dp),
                    )
                }
            }
        }
    }
}

// ── Transcript area ───────────────────────────────────────────────────────────

@Composable
private fun TranscriptArea(
    voiceTranscript: String,
    userTranscript: String,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new transcript arrives
    LaunchedEffect(voiceTranscript, userTranscript) {
        if (voiceTranscript.isNotEmpty() || userTranscript.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(Int.MAX_VALUE) }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        reverseLayout = false,
    ) {
        // Voice (AI) subtitle — green
        if (voiceTranscript.isNotBlank()) {
            item(key = "voice") {
                TranscriptBubble(
                    text      = voiceTranscript,
                    isVoice   = true,
                    alignment = Alignment.Start,
                )
            }
        }

        // User subtitle — blue
        if (userTranscript.isNotBlank()) {
            item(key = "user") {
                TranscriptBubble(
                    text      = userTranscript,
                    isVoice   = false,
                    alignment = Alignment.End,
                )
            }
        }
    }
}

@Composable
private fun TranscriptBubble(
    text: String,
    isVoice: Boolean,
    alignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isVoice) {
        Secondary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
    val textColor = if (isVoice) {
        Secondary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Text(
            text  = if (isVoice) "Voice" else "Вы",
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(
                topStart = if (isVoice) 4.dp else 12.dp,
                topEnd = if (isVoice) 12.dp else 4.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp,
            ),
        ) {
            Text(
                text     = text,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

// ── Session stats row ─────────────────────────────────────────────────────────

@Composable
private fun SessionStatsRow(
    wordsLearned: Int,
    wordsReviewed: Int,
    exercisesDone: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatChip(value = wordsLearned,  label = "Новые")
        StatChip(value = wordsReviewed, label = "Повторено")
        StatChip(value = exercisesDone, label = "Упражнения")
    }
}

@Composable
private fun StatChip(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

// ── Bottom bar ────────────────────────────────────────────────────────────────

@Composable
private fun SessionBottomBar(
    uiState: SessionUiState,
    voiceEngineState: VoiceEngineState,
    onEvent: (SessionEvent) -> Unit,
    onNavigateToStats: () -> Unit,
) {
    BottomAppBar(
        containerColor = Color.Transparent,
        modifier = Modifier.navigationBarsPadding(),
    ) {
        // Pause / Resume
        IconButton(
            onClick = { onEvent(SessionEvent.PauseResume) },
            enabled = uiState.isSessionActive,
            modifier = Modifier.weight(1f),
        ) {
            val isPaused = voiceEngineState == VoiceEngineState.WAITING ||
                           voiceEngineState == VoiceEngineState.IDLE
            Icon(
                imageVector        = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = if (isPaused) "Продолжить" else "Пауза",
                tint               = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }

        // ── Central pulsing mic button ──────────────────────────────────────
        Box(
            modifier = Modifier.weight(2f),
            contentAlignment = Alignment.Center,
        ) {
            PulsingMicButton(
                engineState = voiceEngineState,
                onClick     = { onEvent(SessionEvent.ToggleMic) },
                enabled     = uiState.isSessionActive || !uiState.isLoading,
                size        = 64.dp,
            )
        }

        // Statistics
        IconButton(
            onClick = onNavigateToStats,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector        = Icons.Outlined.BarChart,
                contentDescription = "Статистика",
                tint               = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }

        // Keyboard toggle
        IconButton(
            onClick  = { onEvent(SessionEvent.ToggleTextInput) },
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector        = Icons.Outlined.Keyboard,
                contentDescription = "Текстовый ввод",
                tint               = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = if (uiState.showTextInput) 1f else 0.7f,
                ),
            )
        }
    }
}

// ── Text input field ──────────────────────────────────────────────────────────

@Composable
private fun TextInputField(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier,
        placeholder = {
            Text(
                text  = "Напишите сообщение...",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                    }
                },
                enabled = text.isNotBlank(),
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Отправить")
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(
            onSend = {
                if (text.isNotBlank()) {
                    onSend(text)
                    text = ""
                }
            },
        ),
        maxLines = 3,
        shape    = RoundedCornerShape(16.dp),
    )
}

// ── Error card ────────────────────────────────────────────────────────────────

@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text  = "Ошибка подключения",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
                TextButton(onClick = onRetry) {
                    Text("Повторить")
                }
            }
        }
    }
}

// ── Session result card ───────────────────────────────────────────────────────

@Composable
private fun SessionResultCard(
    result: com.voicedeutsch.master.domain.model.session.SessionResult,
    onDismiss: () -> Unit,
    onGoToDashboard: () -> Unit,
    onStartNewSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier               = Modifier.padding(24.dp),
            horizontalAlignment    = Alignment.CenterHorizontally,
            verticalArrangement    = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text  = "Сессия завершена!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text  = "Слов изучено: ${result.wordsLearned}\n" +
                        "Слов повторено: ${result.wordsReviewed}\n" +
                        "Упражнений: ${result.exercisesCompleted}",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
                TextButton(onClick = onGoToDashboard) {
                    Text("Дашборд")
                }
                TextButton(onClick = onStartNewSession) {
                    Text("Ещё раз")
                }
            }
        }
    }
}
