package com.voicedeutsch.master.presentation.screen.book

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.domain.model.book.LessonStatus
import com.voicedeutsch.master.domain.usecase.book.GetCurrentLessonUseCase
import com.voicedeutsch.master.presentation.theme.Background
import com.voicedeutsch.master.presentation.theme.Secondary
import org.koin.androidx.compose.koinViewModel

/**
 * Book screen — shows current lesson, overall completion progress, and lesson list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(
    onBack: () -> Unit,
    onStartSession: () -> Unit = {},
    viewModel: BookViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedLesson by remember {
        mutableStateOf<GetCurrentLessonUseCase.CurrentLessonData?>(null)
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Учебник", color = MaterialTheme.colorScheme.onBackground) },
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

        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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

            // ── Overall progress ──────────────────────────────────────────────
            Card(
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text  = "Прогресс по книге",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text  = "${(state.completionPercent * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    LinearProgressIndicator(
                        progress  = { state.completionPercent },
                        modifier  = Modifier.fillMaxWidth().height(8.dp),
                        color     = Secondary,
                    )
                }
            }

            // ── Current lesson card ───────────────────────────────────────────
            state.currentLesson?.let { data ->
                Text(
                    text  = "Текущий урок",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
                Card(
                    onClick = { selectedLesson = data },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text  = "Глава ${data.chapterNumber}: ${data.chapter.titleRu}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text  = "Урок ${data.lessonNumber}: ${data.lesson.titleRu}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        if (data.vocabulary.isNotEmpty()) {
                            Text(
                                text  = "${data.vocabulary.size} слов · Нажмите чтобы открыть",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onStartSession,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(Icons.Filled.Mic, null, modifier = Modifier.padding(end = 8.dp))
                            Text("Учить сейчас голосом")
                        }
                    }
                }
            }

            // ── All lessons grouped by chapter ────────────────────────────────
            val byChapter = state.allProgress.groupBy { it.chapter }
            if (byChapter.isNotEmpty()) {
                byChapter.forEach { (chapterNum, lessons) ->
                    Text(
                        text  = "Глава $chapterNum",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    lessons.forEach { progress ->
                        LessonProgressRow(
                            progress = progress,
                            onClick = {
                                if (state.currentLesson?.lessonNumber == progress.lesson &&
                                    state.currentLesson?.chapterNumber == progress.chapter) {
                                    selectedLesson = state.currentLesson
                                }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (selectedLesson != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedLesson = null },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val lesson = selectedLesson!!

                Text(
                    text  = "Глава ${lesson.chapterNumber}: ${lesson.chapter.titleRu}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text  = "Урок ${lesson.lessonNumber}: ${lesson.lesson.titleRu}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                HorizontalDivider()

                if (lesson.vocabulary.isNotEmpty()) {
                    Text(
                        text  = "Слова урока (${lesson.vocabulary.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    lesson.vocabulary.forEach { word ->
                        com.voicedeutsch.master.presentation.components.WordCard(
                            german          = word.german,
                            russian         = word.russian,
                            knowledgeLevel  = 0,
                            partOfSpeech    = word.gender?.let { "($it)" },
                            exampleSentence = null,
                        )
                    }
                } else {
                    Text(
                        text  = "Словарь не загружен для этого урока",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        selectedLesson = null
                        onStartSession()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                ) {
                    Icon(Icons.Filled.Mic, null, modifier = Modifier.padding(end = 8.dp))
                    Text("Учить этот урок голосом", style = MaterialTheme.typography.titleMedium)
                }

                OutlinedButton(
                    onClick = { selectedLesson = null },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Закрыть") }
            }
        }
    }
}



@Composable
private fun LessonProgressRow(
    progress: com.voicedeutsch.master.domain.model.book.BookProgress,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (icon, tint) = when (progress.status) {
                LessonStatus.COMPLETED   -> Icons.Outlined.CheckCircle to Secondary
                LessonStatus.IN_PROGRESS -> Icons.Outlined.PlayCircle to MaterialTheme.colorScheme.primary
                LessonStatus.NOT_STARTED -> Icons.Outlined.Lock to MaterialTheme.colorScheme.outline
            }
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(
                text  = "Гл.${progress.chapter} · Ур.${progress.lesson}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        progress.score?.let { score ->
            Text(
                text  = "${(score * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = if (score >= 0.8f) Secondary else MaterialTheme.colorScheme.primary,
            )
        }
    }
}
