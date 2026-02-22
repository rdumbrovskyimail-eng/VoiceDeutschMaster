package com.voicedeutsch.master.presentation.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.voicedeutsch.master.presentation.components.shimmerEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.presentation.components.LevelIndicator
import com.voicedeutsch.master.presentation.components.ProgressRing
import com.voicedeutsch.master.presentation.theme.Background
import com.voicedeutsch.master.presentation.theme.Secondary
import org.koin.androidx.compose.koinViewModel

/**
 * Dashboard — home screen showing user progress and daily stats.
 *
 * Layout:
 *  ┌────────────────────────────────────────┐
 *  │  TopBar: "Guten Morgen, {name}!"  [⚙]  │
 *  ├────────────────────────────────────────┤
 *  │  CEFR Level + ProgressRing             │
 *  ├────────────────────────────────────────┤
 *  │  Daily stats: words / minutes / streak │
 *  ├────────────────────────────────────────┤
 *  │  Weekly bar chart (7 bars)              │
 *  ├────────────────────────────────────────┤
 *  │  🎤 START SESSION  (large CTA)          │
 *  ├────────────────────────────────────────┤
 *  │  Quick links: Knowledge | Book | Stats  │
 *  └────────────────────────────────────────┘
 *
 * Architecture: lines 1195-1230 (Dashboard screen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onStartSession: () -> Unit,
    onNavigateToKnowledge: () -> Unit,
    onNavigateToBook: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = buildGreeting(state.userProfile?.name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Настройки",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.statusBarsPadding(),
            )
        },
    ) { padding ->

        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                // Level card skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .shimmerEffect()
                )
                // Stats row skeleton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .shimmerEffect()
                        )
                    }
                }
                // Chart skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .shimmerEffect()
                )
                // Button skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .shimmerEffect()
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── CEFR Level card ───────────────────────────────────────────────
            state.userProfile?.let { profile ->
                LevelCard(
                    cefrDisplay    = profile.cefrDisplay,
                    totalWords     = profile.totalWordsLearned,
                    totalSessions  = profile.totalSessions,
                    overallPercent = state.overallProgress?.let { prog ->
                        prog.vocabularyProgress.activeWords.toFloat() /
                            (prog.vocabularyProgress.totalWords.coerceAtLeast(1)) * 100f
                    } ?: 0f,
                )
            }

            // ── Daily stats row ───────────────────────────────────────────────
            DailyStatsRow(
                wordsToday   = state.wordsLearnedToday,
                minutesToday = state.minutesToday,
                streak       = state.streakDays,
            )

            // ── Weekly chart ──────────────────────────────────────────────────
            if (state.weeklyProgress.isNotEmpty()) {
                WeeklyChart(days = state.weeklyProgress)
            }

            // ── CTA button ────────────────────────────────────────────────────
            Button(
                onClick  = onStartSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text  = "Начать занятие",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // ── Quick navigation ──────────────────────────────────────────────
            QuickNavRow(
                onKnowledge  = onNavigateToKnowledge,
                onBook       = onNavigateToBook,
                onStatistics = onNavigateToStatistics,
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun LevelCard(
    cefrDisplay: String,
    totalWords: Int,
    totalSessions: Int,
    overallPercent: Float,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text  = "Ваш уровень",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text  = cefrDisplay,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text  = "$totalWords слов · $totalSessions сессий",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ProgressRing(
                progress    = overallPercent / 100f,
                size        = 80.dp,
                strokeWidth = 8.dp,
                label       = "${overallPercent.toInt()}%",
            )
        }
    }
}

@Composable
private fun DailyStatsRow(
    wordsToday: Int,
    minutesToday: Int,
    streak: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            value    = wordsToday.toString(),
            label    = "Слов сегодня",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value    = "$minutesToday мин",
            label    = "Время",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value    = "🔥 $streak",
            label    = "Серия дней",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier               = Modifier.padding(12.dp),
            horizontalAlignment    = Alignment.CenterHorizontally,
            verticalArrangement    = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text  = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeeklyChart(
    days: List<com.voicedeutsch.master.domain.model.progress.DailyProgress>,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text  = "Неделя",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier                = Modifier.fillMaxWidth(),
                horizontalArrangement   = Arrangement.SpaceEvenly,
                verticalAlignment       = Alignment.Bottom,
            ) {
                val maxWords = days.maxOfOrNull { it.wordsLearned }?.coerceAtLeast(1) ?: 1
                val dayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                days.forEachIndexed { index, day ->
                    val heightFraction = day.wordsLearned.toFloat() / maxWords
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 20.dp, height = (60 * heightFraction.coerceAtLeast(0.04f)).dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(
                                    if (day.wordsLearned > 0) Secondary
                                    else MaterialTheme.colorScheme.outline,
                                ),
                        )
                        Text(
                            text  = dayLabels.getOrElse(index) { "" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickNavRow(
    onKnowledge: () -> Unit,
    onBook: () -> Unit,
    onStatistics: () -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        QuickNavButton(icon = Icons.Outlined.Map,          label = "Знания",     onClick = onKnowledge)
        QuickNavButton(icon = Icons.Outlined.AutoStories,  label = "Книга",      onClick = onBook)
        QuickNavButton(icon = Icons.Outlined.BarChart,     label = "Статистика", onClick = onStatistics)
    }
}

@Composable
private fun QuickNavButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilledIconButton(
            onClick = onClick,
            colors  = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor   = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(icon, contentDescription = label)
        }
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}

private fun buildGreeting(name: String?): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 6  -> "Доброй ночи"
        hour < 12 -> "Guten Morgen"
        hour < 17 -> "Guten Tag"
        hour < 21 -> "Guten Abend"
        else      -> "Gute Nacht"
    }
    return if (name != null) "$greeting, $name!" else greeting
}
