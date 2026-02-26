package com.voicedeutsch.master.presentation.screen.session

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.presentation.theme.Primary
import com.voicedeutsch.master.presentation.theme.Secondary

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun StrategyTestCanvas(
    strategy: LearningStrategy,
    wordsLearned: Int,
    exercisesCompleted: Int,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = strategy,
        transitionSpec = {
            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                slideOutHorizontally { width -> -width } + fadeOut()
            )
        },
        label = "StrategyTransition",
        modifier = modifier
    ) { currentStrategy ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getStrategyIcon(currentStrategy),
                        contentDescription = null,
                        tint = getStrategyColor(currentStrategy),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = currentStrategy.displayNameRu.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = getStrategyColor(currentStrategy),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = TextUnit(1.5f, TextUnitType.Sp)
                        )
                        Text(
                            text = currentStrategy.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (currentStrategy) {
                    LearningStrategy.ASSESSMENT       -> AssessmentTestUI(exercisesCompleted)
                    LearningStrategy.REPETITION       -> RepetitionTestUI(exercisesCompleted)
                    LearningStrategy.PRONUNCIATION    -> PronunciationTestUI()
                    LearningStrategy.GAP_FILLING      -> GapFillingTestUI()
                    LearningStrategy.GRAMMAR_DRILL    -> GrammarTestUI()
                    LearningStrategy.VOCABULARY_BOOST -> VocabBoostTestUI(wordsLearned)
                    LearningStrategy.LINEAR_BOOK      -> BookTestUI()
                    LearningStrategy.FREE_PRACTICE    -> FreePracticeUI()
                    LearningStrategy.LISTENING        -> ListeningUI()
                }
            }
        }
    }
}

@Composable
private fun AssessmentTestUI(completed: Int) {
    Column {
        Text("Оценка уровня (CEFR)", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (completed / 15f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Primary
        )
        Text(
            text = "Вопрос $completed из ~15",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun RepetitionTestUI(completed: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Интервальное повторение", style = MaterialTheme.typography.titleSmall)
        Surface(
            color = Secondary.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Повторено: $completed",
                color = Secondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PronunciationTestUI() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Mic, contentDescription = null, tint = Primary)
        Spacer(Modifier.width(8.dp))
        Text("Слушайте и повторяйте чётко", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun GapFillingTestUI() {
    Column {
        Text("Отработка слабых мест", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Фокус на паттернах ошибок",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun GrammarTestUI() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Outlined.Rule, contentDescription = null, tint = Color(0xFF9C27B0)) // ИСПРАВЛЕНО
        Spacer(Modifier.width(8.dp))
        Text("Грамматический штурм", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun VocabBoostTestUI(learned: Int) {
    Column {
        Text("Интенсив: Лексика", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(minOf(learned, 10)) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Secondary)
                )
            }
        }
    }
}

@Composable
private fun BookTestUI() {
    Text("Идём по программе учебника", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun FreePracticeUI() {
    Text("Говорите свободно на любые темы", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun ListeningUI() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Headphones, contentDescription = null, tint = Primary)
        Spacer(Modifier.width(8.dp))
        Text("Аудирование: слушайте текст", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun getStrategyIcon(strategy: LearningStrategy): ImageVector = when (strategy) {
    LearningStrategy.LINEAR_BOOK      -> Icons.Outlined.AutoStories
    LearningStrategy.REPETITION       -> Icons.Outlined.Replay
    LearningStrategy.ASSESSMENT       -> Icons.Outlined.Speed
    LearningStrategy.FREE_PRACTICE    -> Icons.Outlined.Forum
    LearningStrategy.PRONUNCIATION    -> Icons.Outlined.RecordVoiceOver
    LearningStrategy.GAP_FILLING      -> Icons.Outlined.Healing
    LearningStrategy.GRAMMAR_DRILL    -> Icons.Outlined.Construction
    LearningStrategy.VOCABULARY_BOOST -> Icons.Outlined.Psychology
    LearningStrategy.LISTENING        -> Icons.Outlined.Headphones
}

private fun getStrategyColor(strategy: LearningStrategy): Color = when (strategy) {
    LearningStrategy.REPETITION,
    LearningStrategy.VOCABULARY_BOOST -> Secondary
    LearningStrategy.GAP_FILLING      -> Color(0xFFE53935)
    LearningStrategy.GRAMMAR_DRILL,
    LearningStrategy.ASSESSMENT       -> Color(0xFF8E24AA)
    LearningStrategy.PRONUNCIATION    -> Color(0xFF00ACC1)
    else                              -> Primary
}