package com.voicedeutsch.master.presentation.screen.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.domain.model.progress.DailyProgress
import com.voicedeutsch.master.domain.model.progress.SkillProgress
import com.voicedeutsch.master.presentation.theme.Background
import com.voicedeutsch.master.presentation.theme.Primary
import com.voicedeutsch.master.presentation.theme.Secondary
import com.voicedeutsch.master.presentation.theme.WaveSpeaking
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.voicedeutsch.master.presentation.components.GenericEmptyState
import org.koin.androidx.compose.koinViewModel
import kotlin.math.cos
import kotlin.math.sin

/**
 * Statistics screen — detailed learning analytics.
 *
 * Tabs: Overview | Weekly | Monthly | Skills radar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    onStartSession: () -> Unit = {},
    viewModel: StatisticsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Статистика", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Tab row ───────────────────────────────────────────────────────
            PrimaryScrollableTabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor   = Color.Transparent,
                edgePadding      = 16.dp,
            ) {
                StatsTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick  = { viewModel.onEvent(StatisticsEvent.SelectTab(tab)) },
                        text     = { Text(tab.label) },
                    )
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                return@Scaffold
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (state.selectedTab) {
                    StatsTab.OVERVIEW -> OverviewTab(state, onStartSession)
                    StatsTab.WEEKLY   -> BarChartTab(
                        days           = state.weeklyProgress,
                        title          = "Слова за неделю",
                        dayLabels      = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"),
                        onStartSession = onStartSession,
                    )
                    StatsTab.MONTHLY  -> BarChartTab(
                        days           = state.monthlyProgress,
                        title          = "Слова за месяц",
                        dayLabels      = state.monthlyProgress.mapIndexed { i, _ -> "${i + 1}" },
                        onStartSession = onStartSession,
                    )
                    StatsTab.SKILLS   -> SkillsTab(
                        skillProgress  = state.skillProgress,
                        onStartSession = onStartSession,
                    )
                }
            }
        }
    }
}

private val StatsTab.label: String get() = when (this) {
    StatsTab.OVERVIEW -> "Обзор"
    StatsTab.WEEKLY   -> "Неделя"
    StatsTab.MONTHLY  -> "Месяц"
    StatsTab.SKILLS   -> "Навыки"
}

// ── Tab content ───────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(
    state: StatisticsUiState,
    onStartSession: () -> Unit = {},
) {
    if (state.overallProgress == null && !state.isLoading) {
        EmptyStateWithCTA(
            icon        = Icons.Outlined.BarChart,
            title       = "Статистика пуста",
            description = "Проведите первое занятие, и здесь появится ваш прогресс",
            ctaText     = "Начать первое занятие",
            onCta       = onStartSession,
        )
        return
    }

    val prog = state.overallProgress

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BigStatCard("${state.totalSessions}", "Сессий", Modifier.weight(1f))
        BigStatCard("${state.totalHours.toInt()}ч", "Время", Modifier.weight(1f))
        BigStatCard("🔥${state.streak}", "Серия", Modifier.weight(1f))
    }

    prog?.let { p ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigStatCard("${p.vocabularyProgress.totalWords}", "Слов", Modifier.weight(1f))
            BigStatCard("${p.grammarProgress.knownRules}", "Правил", Modifier.weight(1f))
            BigStatCard("${p.vocabularyProgress.activeWords}", "Активных", Modifier.weight(1f))
        }

        // Vocabulary topics breakdown
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("По темам", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                p.vocabularyProgress.byTopic.entries.sortedByDescending { it.value.known }.take(6).forEach { (topic, topicProg) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(topic, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text("${topicProg.known}/${topicProg.total}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { topicProg.percentage },
                            modifier = Modifier.width(60.dp).height(4.dp),
                            color    = Secondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarChartTab(
    days: List<DailyProgress>,
    title: String,
    dayLabels: List<String>,
    onStartSession: () -> Unit = {},
) {
    if (days.isEmpty()) {
        EmptyStateWithCTA(
            icon        = Icons.Outlined.BarChart,
            title       = "Нет данных",
            description = "Данные появятся после первых занятий",
            ctaText     = "Начать занятие",
            onCta       = onStartSession,
        )
        return
    }

    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

            val maxWords = days.maxOfOrNull { it.wordsLearned }?.coerceAtLeast(1) ?: 1
            Row(
                modifier              = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.Bottom,
            ) {
                days.forEachIndexed { index, day ->
                    val fraction = day.wordsLearned.toFloat() / maxWords
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (day.wordsLearned > 0) {
                            Text(
                                text  = "${day.wordsLearned}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height((100 * fraction.coerceAtLeast(0.03f)).dp)
                                .background(
                                    color = if (day.wordsLearned > 0) Secondary else MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.extraSmall,
                                ),
                        )
                        Text(
                            text  = dayLabels.getOrElse(index) { "${index + 1}" },
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
private fun SkillsTab(
    skillProgress: SkillProgress?,
    onStartSession: () -> Unit = {},
) {
    if (skillProgress == null) {
        EmptyStateWithCTA(
            icon        = Icons.Outlined.Psychology,
            title       = "Навыки ещё не оценены",
            description = "Пройдите несколько сессий, чтобы увидеть прогресс",
            ctaText     = "Начать занятие",
            onCta       = onStartSession,
        )
        return
    }

    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Навыки", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

            // Spider / radar chart
            RadarChart(
                skills = listOf(
                    "Словарный\nзапас" to skillProgress.vocabulary,
                    "Грамматика"       to skillProgress.grammar,
                    "Произношение"     to skillProgress.pronunciation,
                    "Понимание"        to skillProgress.listening,
                    "Говорение"        to skillProgress.speaking,
                ),
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )

            // Skill bars
            val skillList = listOf(
                "Словарный запас" to skillProgress.vocabulary,
                "Грамматика"      to skillProgress.grammar,
                "Произношение"    to skillProgress.pronunciation,
                "Понимание"       to skillProgress.listening,
                "Говорение"       to skillProgress.speaking,
            )
            skillList.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(120.dp))
                    LinearProgressIndicator(
                        progress = { value },
                        modifier = Modifier.weight(1f).height(6.dp),
                        color    = when {
                            value >= 0.7f -> WaveSpeaking
                            value >= 0.4f -> Primary
                            else          -> MaterialTheme.colorScheme.error
                        },
                    )
                    Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Empty state with CTA ──────────────────────────────────────────────────────

@Composable
private fun EmptyStateWithCTA(
    icon: ImageVector,
    title: String,
    description: String,
    ctaText: String,
    onCta: () -> Unit,
) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon, null,
            modifier = Modifier.size(72.dp),
            tint     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
        )
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            description,
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
        Button(onClick = onCta) {
            Icon(Icons.Filled.Mic, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(ctaText)
        }
    }
}

// ── Radar chart ───────────────────────────────────────────────────────────────

@Composable
private fun RadarChart(
    skills: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
) {
    val gridColor = Color.Gray.copy(alpha = 0.3f)
    val fillColor = Primary.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val n     = skills.size
        val cx    = size.width / 2f
        val cy    = size.height / 2f
        val r     = minOf(cx, cy) * 0.7f
        val step  = (Math.PI * 2 / n).toFloat()

        // Grid rings
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { ring ->
            val ringPath = Path()
            for (i in 0 until n) {
                val angle = step * i - Math.PI.toFloat() / 2
                val x = cx + r * ring * cos(angle)
                val y = cy + r * ring * sin(angle)
                if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
            }
            ringPath.close()
            drawPath(ringPath, gridColor, style = Stroke(width = 1.dp.toPx()))
        }

        // Spokes
        for (i in 0 until n) {
            val angle = step * i - Math.PI.toFloat() / 2
            drawLine(gridColor, Offset(cx, cy), Offset(cx + r * cos(angle), cy + r * sin(angle)), strokeWidth = 1.dp.toPx())
        }

        // Skill polygon
        val path = Path()
        skills.forEachIndexed { i, (_, value) ->
            val angle = step * i - Math.PI.toFloat() / 2
            val x = cx + r * value * cos(angle)
            val y = cy + r * value * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, fillColor)
        drawPath(path, Primary.copy(alpha = 0.8f), style = Stroke(width = 2.dp.toPx()))

        // Skill dots
        skills.forEachIndexed { i, (_, value) ->
            val angle = step * i - Math.PI.toFloat() / 2
            drawCircle(Primary, 5.dp.toPx(), Offset(cx + r * value * cos(angle), cy + r * value * sin(angle)))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun BigStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
