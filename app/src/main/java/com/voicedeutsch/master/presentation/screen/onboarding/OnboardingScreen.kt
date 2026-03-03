package com.voicedeutsch.master.presentation.screen.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.presentation.theme.Background
import com.voicedeutsch.master.presentation.theme.Primary
import com.voicedeutsch.master.presentation.theme.Secondary
import org.koin.androidx.compose.koinViewModel

/**
 * Onboarding flow — 4-step wizard:
 *
 *  1. WELCOME    – Splash + tagline
 *  2. NAME       – Enter user name
 *  3. LEVEL      – Choose starting CEFR level
 *  4. BOOK_LOAD  – Load book assets
 *  5. DONE       – Completion confirmation
 */
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.step) {
        if (state.step == OnboardingStep.DONE) {
            onOnboardingComplete()
        }
    }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Background, Background.copy(red = 0.08f)),
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(Modifier.weight(0.2f))

            // ── Step indicator ────────────────────────────────────────────────
            StepIndicator(
                totalSteps  = OnboardingStep.entries.size - 1, // exclude DONE
                currentStep = state.step.ordinal,
            )

            Spacer(Modifier.height(32.dp))

            // ── Step content ──────────────────────────────────────────────────
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                },
                modifier = Modifier.weight(1f),
                label    = "onboarding_step",
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME   -> WelcomeStep()
                    OnboardingStep.NAME      -> NameStep(
                        name         = state.name,
                        error        = state.errorMessage,
                        onNameChange = { viewModel.onEvent(OnboardingEvent.UpdateName(it)) },
                        onDone       = { focusManager.clearFocus(); viewModel.onEvent(OnboardingEvent.Next) },
                    )
                    OnboardingStep.LEVEL     -> LevelStep(
                        selected = state.selectedLevel,
                        onSelect = { viewModel.onEvent(OnboardingEvent.SelectLevel(it)) },
                    )
                    OnboardingStep.DONE      -> DoneStep(name = state.name)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Navigation buttons ────────────────────────────────────────────
            if (state.step != OnboardingStep.DONE) {
                OnboardingNavRow(
                    step      = state.step,
                    isLoading = state.isLoading,
                    onBack    = { viewModel.onEvent(OnboardingEvent.Back) },
                    onNext    = { viewModel.onEvent(OnboardingEvent.Next) },
                )
            } else {
                Button(
                    onClick  = onOnboardingComplete,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Secondary),
                ) {
                    Text("Начать изучение!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.weight(0.1f))
        }
    }
}

// ── Step composables ──────────────────────────────────────────────────────────

@Composable
private fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier            = Modifier.fillMaxSize(),
    ) {
        Text("🇩🇪", fontSize = 72.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            text       = "VoiceDeutsch\nMaster",
            style      = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground,
            textAlign  = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text      = "Учите немецкий голосом с\nперсональным AI-репетитором",
            style     = MaterialTheme.typography.bodyLarge,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NameStep(
    name: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier            = Modifier.fillMaxSize(),
    ) {
        Text("Как вас зовут?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("Репетитор будет обращаться к вам по имени", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value          = name,
            onValueChange  = onNameChange,
            label          = { Text("Имя") },
            placeholder    = { Text("Например: Алекс") },
            singleLine     = true,
            isError        = error != null,
            supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier       = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction      = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
        )
    }
}

@Composable
private fun LevelStep(selected: CefrLevel, onSelect: (CefrLevel) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.fillMaxSize(),
    ) {
        Text("Ваш текущий уровень", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("Не страшно угадать — AI адаптируется", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(24.dp))

        val descriptions = mapOf(
            CefrLevel.A1 to "Полный новичок",
            CefrLevel.A2 to "Знаю приветствия и базу",
            CefrLevel.B1 to "Могу общаться в простых ситуациях",
            CefrLevel.B2 to "Уверенно общаюсь",
            CefrLevel.C1 to "Продвинутый уровень",
            CefrLevel.C2 to "Почти носитель",
        )

        CefrLevel.entries.forEach { level ->
            Card(
                onClick = { onSelect(level) },
                colors  = CardDefaults.cardColors(
                    containerColor = if (selected == level)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surface,
                ),
                border   = if (selected == level)
                    androidx.compose.foundation.BorderStroke(2.dp, Primary) else null,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(level.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (selected == level) Primary else MaterialTheme.colorScheme.onBackground)
                        Text(descriptions[level] ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (selected == level) {
                        Text("✓", color = Primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DoneStep(name: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier            = Modifier.fillMaxSize(),
    ) {
        Text("🎉", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            text       = "Добро пожаловать,\n$name!",
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground,
            textAlign  = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text      = "Всё готово! Нажмите кнопку ниже и начните говорить по-немецки.",
            style     = MaterialTheme.typography.bodyLarge,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

// ── Navigation row ────────────────────────────────────────────────────────────

@Composable
private fun OnboardingNavRow(
    step: OnboardingStep,
    isLoading: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        if (step != OnboardingStep.WELCOME) {
            TextButton(onClick = onBack) { Text("Назад") }
        } else {
            Spacer(Modifier.width(80.dp))
        }

        Button(
            onClick  = onNext,
            enabled  = !isLoading,
            modifier = Modifier.height(48.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    color       = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    when (step) {
                        OnboardingStep.WELCOME -> "Начать"
                        OnboardingStep.LEVEL   -> "Готово!"
                        else                   -> "Далее"
                    }
                )
            }
        }
    }
}

// ── Step indicator dots ───────────────────────────────────────────────────────

@Composable
private fun StepIndicator(totalSteps: Int, currentStep: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(totalSteps) { index ->
            val isActive = index == currentStep
            val isDone   = index < currentStep
            Surface(
                modifier = Modifier.size(if (isActive) 20.dp else 8.dp, 8.dp),
                shape    = MaterialTheme.shapes.extraSmall,
                color    = when {
                    isActive -> Primary
                    isDone   -> Secondary
                    else     -> MaterialTheme.colorScheme.outline
                },
            ) {}
        }
    }
}