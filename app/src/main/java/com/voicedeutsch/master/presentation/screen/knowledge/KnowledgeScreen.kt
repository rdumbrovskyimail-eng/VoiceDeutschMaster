package com.voicedeutsch.master.presentation.screen.knowledge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.domain.usecase.knowledge.GetWeakPointsUseCase
import com.voicedeutsch.master.presentation.components.GenericEmptyState
import com.voicedeutsch.master.presentation.components.KnowledgeMap
import com.voicedeutsch.master.presentation.theme.Background
import com.voicedeutsch.master.presentation.theme.Warning
import org.koin.androidx.compose.koinViewModel

/**
 * Knowledge Map screen — overview of learned words, grammar, phrases, and weak spots.
 *
 * Tabs: Overview | Words | Grammar | Weak Points
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(
    onBack: () -> Unit,
    onStartSession: () -> Unit = {},
    viewModel: KnowledgeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("База знаний", color = MaterialTheme.colorScheme.onBackground) },
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

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Tab row ───────────────────────────────────────────────────────
            val tabs = KnowledgeTab.entries
            PrimaryScrollableTabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor   = Color.Transparent,
                edgePadding      = 16.dp,
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick  = { viewModel.onEvent(KnowledgeEvent.SelectTab(tab)) },
                        text     = { Text(tab.label) },
                    )
                }
            }

            if (state.isLoading) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                return@Scaffold
            }

            // ── Tab content ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (state.selectedTab) {
                    KnowledgeTab.OVERVIEW    -> OverviewTab(state, onStartSession)
                    KnowledgeTab.WORDS       -> WordsTab(state, viewModel)
                    KnowledgeTab.GRAMMAR     -> GrammarTab(state)
                    KnowledgeTab.WEAK_POINTS -> WeakPointsTab(state, onStartSession)
                }
            }
        }
    }
}

private val KnowledgeTab.label: String get() = when (this) {
    KnowledgeTab.OVERVIEW    -> "Обзор"
    KnowledgeTab.WORDS       -> "Слова"
    KnowledgeTab.GRAMMAR     -> "Грамматика"
    KnowledgeTab.WEAK_POINTS -> "Слабые места"
}

// ── Tab composables ───────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(state: KnowledgeUiState, onStartSession: () -> Unit) {
    val overview = state.overview ?: return

    // Visual map of knowledge by topic
    KnowledgeMap(
        topicDistribution = overview.topicDistribution,
        modifier          = Modifier.fillMaxWidth().height(200.dp),
    )

    Spacer(Modifier.height(8.dp))

    // Summary stats
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard("${overview.wordsKnown}", "Слов знаю", Modifier.weight(1f))
        StatCard("${overview.rulesKnown}", "Правил", Modifier.weight(1f))
        StatCard("${overview.phrasesKnown}", "Фраз", Modifier.weight(1f))
    }

    // Words for review today
    if (overview.wordsForReviewToday > 0) {
        Card(
            onClick = onStartSession,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Outlined.Alarm,
                    contentDescription = null,
                    tint = Warning,
                    modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Сегодня на повторение",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${overview.wordsForReviewToday} слов · нажмите чтобы начать",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun WordsTab(
    state: KnowledgeUiState,
    viewModel: KnowledgeViewModel,
) {
    val overview = state.overview ?: run {
        GenericEmptyState(
            title = "Нет данных о словах",
            description = "Начните занятия, чтобы слова появились здесь",
        )
        return
    }

    Text(
        "Всего встречено: ${overview.totalWordsEncountered}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )

    val words = if (state.showAllWords)
        overview.recentActivity
    else
        overview.recentActivity.take(10)

    words.forEach { activity ->
        com.voicedeutsch.master.presentation.components.WordCard(
            german         = activity.wordGerman,
            russian        = activity.wordRussian,
            knowledgeLevel = activity.knowledgeLevel,
            isNew          = activity.knowledgeLevel <= 1,
        )
    }

    if (overview.recentActivity.size > 10) {
        TextButton(
            onClick  = { viewModel.onEvent(KnowledgeEvent.ToggleShowAllWords) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (state.showAllWords)
                    "Показать меньше ↑"
                else
                    "Показать все ${overview.recentActivity.size} слов ↓"
            )
        }
    }

    if (overview.recentActivity.isEmpty()) {
        GenericEmptyState(
            title = "Слов пока нет",
            description = "После первых занятий здесь появятся изученные слова",
        )
    }
}

@Composable
private fun GrammarTab(state: KnowledgeUiState) {
    val overview = state.overview ?: return

    Text(
        "Изучено: ${overview.rulesKnown} / ${overview.totalGrammarRules} правил",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )

    LinearProgressIndicator(
        progress = {
            if (overview.totalGrammarRules > 0)
                overview.rulesKnown.toFloat() / overview.totalGrammarRules
            else 0f
        },
        modifier = Modifier.fillMaxWidth().height(8.dp),
    )

    Spacer(Modifier.height(8.dp))

    // Список категорий грамматики с прогрессом
    if (overview.grammarByCategory.isNotEmpty()) {
        overview.grammarByCategory.forEach { (category, progress) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        category,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    } else {
        GenericEmptyState(
            icon = Icons.Outlined.School,
            title = "Правила ещё не изучались",
            description = "Пройдите несколько занятий — грамматика появится здесь",
        )
    }
}

// PATCH APPLIED: WeakPointsTab теперь работает с List<GetWeakPointsUseCase.WeakPoint>?
// Старый код использовал weak.weakWords (не существует), новый итерирует List<WeakPoint>
// с полями description, category, severity.
@Composable
private fun WeakPointsTab(state: KnowledgeUiState, onStartSession: () -> Unit) {
    val weakPoints = state.weakPoints
    if (weakPoints.isNullOrEmpty()) {
        GenericEmptyState(
            icon        = Icons.Outlined.EmojiEvents,
            title       = "Слабых мест не найдено",
            description = "Отличная работа! Продолжайте занятия, чтобы отслеживать прогресс",
        )
        return
    }

    weakPoints.take(10).forEach { point ->
        WeakPointCard(
            point = point,
            onPractice = onStartSession,
        )
    }
}

@Composable
private fun WeakPointCard(
    point: GetWeakPointsUseCase.WeakPoint,
    onPractice: () -> Unit = {},
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                alpha = (0.1f + point.severity * 0.4f).coerceIn(0.1f, 0.5f)
            ),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        point.description,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        point.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                    )
                }
                Text(
                    "${(point.severity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick  = onPractice,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            ) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Отработать",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// ── Small helper composables ──────────────────────────────────────────────────

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecentWordRow(german: String, russian: String, level: Int) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                german,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                russian,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
        Text(
            text  = "Ур.$level",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
