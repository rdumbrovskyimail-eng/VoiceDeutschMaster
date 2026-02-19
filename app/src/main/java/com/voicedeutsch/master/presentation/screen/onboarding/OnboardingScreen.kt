package com.voicedeutsch.master.presentation.screen.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.presentation.theme.Background
import com.voicedeutsch.master.presentation.theme.Primary
import com.voicedeutsch.master.presentation.theme.Secondary
import org.koin.androidx.compose.koinViewModel

/**
 * Onboarding flow — 5-step wizard:
 *
 *  1. WELCOME    – Splash + tagline
 *  2. NAME       – Enter user name
 *  3. LEVEL      – Choose starting CEFR level
 *  4. API_KEY    – Enter Gemini API key
 *  5. BOOK_LOAD  – Load book assets
 *  6. DONE       – Completion confirmation
 */
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // Navigate away when completed
    LaunchedEffect(state.step) {
        if (state.step == OnboardingStep.DONE) {
            viewModel.onEvent(OnboardingEvent.Complete)
        }
    }

    // Observe completion flag
    LaunchedEffect(Unit) {
        // Re-check after Complete event fires
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
                totalSteps   = OnboardingStep.entries.size - 1, // exclude DONE
                currentStep  = state.step.ordinal,
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
                        name          = state.name,
                        error         = state.errorMessage,
                        onNameChange  = { viewModel.onEvent(OnboardingEvent.UpdateName(it)) },
                        onDone        = { focusManager.clearFocus(); viewModel.onEvent(OnboardingEvent.Next) },
                    )
                    OnboardingStep.LEVEL     -> LevelStep(
                        selected  = state.selectedLevel,
                        onSelect  = { viewModel.onEvent(OnboardingEvent.SelectLevel(it)) },
                    )
                    OnboardingStep.API_KEY   -> ApiKeyStep(
                        apiKey    = state.apiKey,
                        visible   = state.apiKeyVisible,
                        error     = state.errorMessage,
                        onChange  = { viewModel.onEvent(OnboardingEvent.UpdateApiKey(it)) },
                        onToggle  = { viewModel.onEvent(OnboardingEvent.ToggleApiKeyVisibility) },
                        onDone    = { focusManager.clearFocus() },
                    )
                    OnboardingStep.BOOK_LOAD -> BookLoadStep(
                        isLoading = state.isLoadingBook,
                        loaded    = state.bookLoaded,
                        error     = state.errorMessage,
                        onLoad    = { viewModel.onEvent(OnboardingEvent.LoadBook) },
                    )
                    OnboardingStep.DONE      -> DoneStep(name = state.name)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Navigation buttons ────────────────────────────────────────────
            if (state.step != OnboardingStep.DONE) {
                OnboardingNavRow(
                    step        = state.step,
                    isLoading   = state.isLoadingBook,
                    onBack      = { viewModel.onEvent(OnboardingEvent.Back) },
                    onNext      = { viewModel.onEvent(OnboardingEvent.Next) },
                    onLoadBook  = { viewModel.onEvent(OnboardingEvent.LoadBook) },
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
            value         = name,
            onValueChange = onNameChange,
            label         = { Text("Имя") },
            placeholder   = { Text("Например: Алекс") },
            singleLine    = true,
            isError       = error != null,
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
                onClick   = { onSelect(level) },
                colors    = CardDefaults.cardColors(
                    containerColor = if (selected == level)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surface,
                ),
                border    = if (selected == level)
                    androidx.compose.foundation.BorderStroke(2.dp, Primary) else null,
                modifier  = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
private fun ApiKeyStep(
    apiKey: String,
    visible: Boolean,
    error: String?,
    onChange: (String) -> Unit,
    onToggle: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier            = Modifier.fillMaxSize(),
    ) {
        Text("Gemini API Key", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Приложение использует Gemini Live API для голосового общения. Ключ хранится только на вашем устройстве.",
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value                = apiKey,
            onValueChange        = onChange,
            label                = { Text("API Key") },
            placeholder          = { Text("AIzaSy...") },
            singleLine           = true,
            isError              = error != null,
            supportingText       = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon         = {
                IconButton(onClick = onToggle) {
                    Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text  = "Получите бесплатно на aistudio.google.com",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun BookLoadStep(
    isLoading: Boolean,
    loaded: Boolean,
    error: String?,
    onLoad: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier            = Modifier.fillMaxSize(),
    ) {
        Text(if (loaded) "📚 Книга загружена!" else "📚 Загрузка учебника", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Слова, грамматика и упражнения из учебника будут доступны во время занятий",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Загружаем...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        } else if (!loaded) {
            Button(onClick = onLoad, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Загрузить учебник")
            }
        } else {
            Text("✓ Все главы загружены", style = MaterialTheme.typography.bodyLarge, color = Secondary, fontWeight = FontWeight.SemiBold)
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
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
        Text("Добро пожаловать,\n$name!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text("Всё готово! Нажмите кнопку ниже и начните говорить по-немецки.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), textAlign = TextAlign.Center)
    }
}

// ── Navigation row ────────────────────────────────────────────────────────────

@Composable
private fun OnboardingNavRow(
    step: OnboardingStep,
    isLoading: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onLoadBook: () -> Unit,
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
            onClick  = if (step == OnboardingStep.BOOK_LOAD) onLoadBook else onNext,
            enabled  = !isLoading,
            modifier = Modifier.height(48.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color    = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    when (step) {
                        OnboardingStep.WELCOME   -> "Начать"
                        OnboardingStep.BOOK_LOAD -> "Загрузить"
                        else                     -> "Далее →"
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
