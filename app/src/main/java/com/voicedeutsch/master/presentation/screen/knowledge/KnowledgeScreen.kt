package com.voicedeutsch.master.presentation.screen.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import com.voicedeutsch.master.presentation.components.KnowledgeMap
import com.voicedeutsch.master.presentation.theme.Background
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
            ScrollableTabRow(
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
                    KnowledgeTab.OVERVIEW    -> OverviewTab(state)
                    KnowledgeTab.WORDS       -> WordsTab(state)
                    KnowledgeTab.GRAMMAR     -> GrammarTab(state)
                    KnowledgeTab.WEAK_POINTS -> WeakPointsTab(state)
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
private fun OverviewTab(state: KnowledgeUiState) {
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
        InfoCard(
            title = "Сегодня на повторение",
            body  = "${overview.wordsForReviewToday} слов, ${overview.rulesForReviewToday} правил, ${overview.phrasesForReviewToday} фраз",
        )
    }
}

@Composable
private fun WordsTab(state: KnowledgeUiState) {
    val overview = state.overview ?: return

    Text(
        "Всего встречено: ${overview.totalWordsEncountered}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )

    overview.recentActivity.take(10).forEach { activity ->
        RecentWordRow(
            german  = activity.wordGerman,
            russian = activity.wordRussian,
            level   = activity.knowledgeLevel,
        )
    }
}

@Composable
private fun GrammarTab(state: KnowledgeUiState) {
    val overview = state.overview ?: return

    Text(
        "Правил изучено: ${overview.rulesKnown} / ${overview.totalGrammarRules}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )

    LinearProgressIndicator(
        progress = { if (overview.totalGrammarRules > 0) overview.rulesKnown.toFloat() / overview.totalGrammarRules else 0f },
        modifier = Modifier.fillMaxWidth().height(8.dp),
    )
}

// PATCH APPLIED: WeakPointsTab теперь работает с List<GetWeakPointsUseCase.WeakPoint>?
// Старый код использовал weak.weakWords (не существует), новый итерирует List<WeakPoint>
// с полями description, category, severity.
@Composable
private fun WeakPointsTab(state: KnowledgeUiState) {
    val weakPoints = state.weakPoints
    if (weakPoints.isNullOrEmpty()) {
        Text(
            "Слабых мест не найдено. Продолжайте занятия!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        return
    }

    weakPoints.take(10).forEach { point ->
        WeakPointCard(point = point)
    }
}

@Composable
private fun WeakPointCard(point: GetWeakPointsUseCase.WeakPoint) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                alpha = (0.1f + point.severity * 0.4f).coerceIn(0.1f, 0.5f)
            ),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    point.description,
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onErrorContainer,
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
