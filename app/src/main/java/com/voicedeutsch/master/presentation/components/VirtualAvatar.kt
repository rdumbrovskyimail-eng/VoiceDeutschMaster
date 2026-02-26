package com.voicedeutsch.master.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.presentation.theme.*
import com.voicedeutsch.master.voicecore.session.VoiceEngineState
import kotlin.math.sin

/**
 * VirtualAvatar — анимированный аватар AI-репетитора.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * ИСПРАВЛЕНИЕ (Производительность — лишние рекомпозиции):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   БЫЛО: amplitudes: FloatArray
 *     • amplitudes.last() в remember(amplitudes) вызывает рекомпозицию
 *       всего VirtualAvatar при каждом новом аудио-чанке (50 раз/сек).
 *     • Canvas перерисовывался через полный цикл: рекомпозиция → layout → draw.
 *
 *   СТАЛО: currentAmplitude: State<Float>
 *     • ViewModel обновляет mutableFloatStateOf() — это структурное равенство,
 *       Compose отслеживает изменение только внутри Canvas (фаза draw).
 *     • Рекомпозиция VirtualAvatar НЕ происходит при изменении амплитуды.
 *     • Canvas читает currentAmplitude.value ТОЛЬКО на этапе отрисовки
 *       → пропускаем фазы рекомпозиции и layout полностью.
 *     • mouthOpen вычисляется прямо в Canvas вместо animateFloatAsState,
 *       чтобы не создавать лишний Animatable, подписанный на рекомпозицию.
 *
 *   КАК ИСПОЛЬЗОВАТЬ В VIEWMODEL:
 *     val currentAmplitude = mutableFloatStateOf(0f)
 *
 *     // При получении аудио-чанка:
 *     currentAmplitude.floatValue = newAmplitude.coerceIn(0f, 1f)
 *
 *   КАК ПЕРЕДАВАТЬ В COMPOSABLE:
 *     VirtualAvatar(
 *         engineState      = state.engineState,
 *         currentAmplitude = viewModel.currentAmplitude,
 *     )
 */
@Composable
fun VirtualAvatar(
    engineState:      VoiceEngineState,
    // ✅ FIX: State<Float> вместо FloatArray.
    // Чтение .value внутри Canvas {} происходит в фазе draw,
    // а не в фазе рекомпозиции — Canvas пропускает recompose при изменении.
    currentAmplitude: State<Float>,
    modifier:         Modifier = Modifier,
) {
    // Анимация дыхания — меняется редко (3 сек цикл), рекомпозиция нормальна
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breath by infiniteTransition.animateFloat(
        initialValue   = 0f,
        targetValue    = 2f * Math.PI.toFloat(),
        animationSpec  = infiniteRepeatable(tween(3000, easing = LinearEasing)), // ИСПРАВЛЕНО: SineEasing → LinearEasing
        label          = "breath",
    )

    // Позиция зрачков — меняется только при смене engineState, рекомпозиция нормальна
    val eyeOffsetX by animateFloatAsState(
        targetValue   = if (engineState == VoiceEngineState.PROCESSING) 15f else 0f,
        animationSpec = tween(500),
        label         = "eyeX",
    )
    val eyeOffsetY by animateFloatAsState(
        targetValue   = if (engineState == VoiceEngineState.PROCESSING) -15f else 0f,
        animationSpec = tween(500),
        label         = "eyeY",
    )

    // isSpeaking вычисляем снаружи Canvas — меняется редко, это нормальная рекомпозиция
    val isSpeaking = engineState == VoiceEngineState.SPEAKING

    Canvas(modifier = modifier.size(200.dp)) {
        val cx = size.width / 2
        val cy = size.height / 2 + (sin(breath) * 5f)

        // ✅ Читаем амплитуду прямо в Canvas (фаза draw).
        // При изменении currentAmplitude рекомпозиция VirtualAvatar НЕ происходит —
        // Compose инвалидирует только фазу draw для этого Canvas.
        val amp = currentAmplitude.value // ИСПРАВЛЕНО: .floatValue → .value (тип State<Float>)

        // mouthOpen вычисляется здесь, а не через animateFloatAsState снаружи —
        // это устраняет лишний Animatable и подписку на рекомпозицию.
        val mouthOpen = if (isSpeaking) 10f + (amp * 60f) else 2f

        // ── Лицо ─────────────────────────────────────────────────────────────
        drawCircle(
            color  = AvatarSkin,
            radius = 80f,
            center = Offset(cx, cy),
        )

        // ── Волосы ────────────────────────────────────────────────────────────
        drawArc(
            color      = AvatarHair,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter  = true,
            topLeft    = Offset(cx - 85f, cy - 90f),
            size       = Size(170f, 120f),
        )

        // ── Глаза (белки) ─────────────────────────────────────────────────────
        drawCircle(color = Color.White, radius = 15f, center = Offset(cx - 30f, cy - 10f))
        drawCircle(color = Color.White, radius = 15f, center = Offset(cx + 30f, cy - 10f))

        // ── Зрачки ────────────────────────────────────────────────────────────
        drawCircle(color = Primary, radius = 6f, center = Offset(cx - 30f + eyeOffsetX, cy - 10f + eyeOffsetY))
        drawCircle(color = Primary, radius = 6f, center = Offset(cx + 30f + eyeOffsetX, cy - 10f + eyeOffsetY))

        // ── Рот ───────────────────────────────────────────────────────────────
        val mouthPath = Path().apply {
            moveTo(cx - 20f, cy + 30f)
            quadraticTo(cx, cy + 30f + mouthOpen, cx + 20f, cy + 30f)
            if (mouthOpen > 5f) {
                quadraticTo(cx, cy + 30f + (mouthOpen * 1.5f), cx - 20f, cy + 30f)
            }
        }

        if (mouthOpen > 5f) {
            drawPath(mouthPath, color = Color(0xFF991B1B))
        } else {
            drawPath(mouthPath, color = Color(0xFF78350F), style = Stroke(width = 4f, cap = StrokeCap.Round))
        }
    }
}