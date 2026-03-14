package com.voicedeutsch.master.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicedeutsch.master.voicecore.session.VoiceEngineState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AiProcessEntry(
    val id: Long = System.nanoTime(),
    val icon: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isComplete: Boolean = false,
)

enum class AiProcessState(val icon: String, val label: String) {
    LOADING_PROMPT("📥", "Получил промт и инструкции"),
    LOADING_CODEBASE("📦", "Кодовая база загружена"),
    TOOLS_READY("🔧", "Инструменты готовы"),
    READING_KNOWLEDGE("📚", "Читает базу знаний пользователя"),
    THINKING("🧠", "Думает / анализирует"),
    SPEAKING("🗣️", "Говорит"),
    SEARCHING_IMAGE("🔍", "Ищет картинку"),
    SAVING_MEMORY("💾", "Сохраняет в память"),
    READY("✅", "Готов к ответу"),
}

@Composable
fun AiProcessPanel(
    engineState: VoiceEngineState,
    isSessionActive: Boolean,
    modifier: Modifier = Modifier,
) {
    var entries by remember { mutableStateOf<List<AiProcessEntry>>(emptyList()) }
    var initDone by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LaunchedEffect(isSessionActive) {
        if (isSessionActive && !initDone) {
            entries = emptyList()
            val initSteps = listOf(
                AiProcessState.LOADING_PROMPT,
                AiProcessState.LOADING_CODEBASE,
                AiProcessState.TOOLS_READY,
            )
            for (step in initSteps) {
                delay(400L)
                entries = entries + AiProcessEntry(
                    icon = step.icon,
                    description = "${step.label} ✅",
                    isComplete = true,
                )
            }
            initDone = true
        }
        if (!isSessionActive) {
            initDone = false
            entries = emptyList()
        }
    }

    LaunchedEffect(engineState) {
        if (!isSessionActive) return@LaunchedEffect
        val newEntry = when (engineState) {
            VoiceEngineState.CONTEXT_LOADING -> AiProcessEntry(
                icon = AiProcessState.READING_KNOWLEDGE.icon,
                description = AiProcessState.READING_KNOWLEDGE.label,
            )
            VoiceEngineState.PROCESSING -> AiProcessEntry(
                icon = AiProcessState.THINKING.icon,
                description = AiProcessState.THINKING.label,
            )
            VoiceEngineState.SPEAKING -> AiProcessEntry(
                icon = AiProcessState.SPEAKING.icon,
                description = AiProcessState.SPEAKING.label,
            )
            VoiceEngineState.SESSION_ACTIVE,
            VoiceEngineState.LISTENING,
            VoiceEngineState.WAITING -> AiProcessEntry(
                icon = AiProcessState.READY.icon,
                description = AiProcessState.READY.label,
                isComplete = true,
            )
            else -> null
        }
        if (newEntry != null) {
            entries = entries + newEntry
            if (entries.size > 50) entries = entries.takeLast(50)
        }
    }

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D1117).copy(alpha = 0.85f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PulsingDot(isActive = isSessionActive)
                Text(
                    "AI Process Log",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8B949E),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier.heightIn(max = 160.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + slideInVertically { it },
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(entry.icon, fontSize = 14.sp)
                            Text(
                                entry.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (entry.isComplete) Color(0xFF3FB950) else Color(0xFFC9D1D9),
                                fontSize = 12.sp,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                timeFormat.format(Date(entry.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF484F58),
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PulsingDot(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "dotAlpha",
    )
    Surface(
        modifier = Modifier
            .size(8.dp)
            .alpha(if (isActive) alpha else 0.3f),
        shape = RoundedCornerShape(50),
        color = if (isActive) Color(0xFF3FB950) else Color(0xFF484F58),
    ) {}
}