package com.voicedeutsch.master.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.presentation.theme.WaveError
import com.voicedeutsch.master.presentation.theme.WaveIdle
import com.voicedeutsch.master.presentation.theme.WaveListening
import com.voicedeutsch.master.presentation.theme.WaveProcessing
import com.voicedeutsch.master.presentation.theme.WaveSpeaking
import com.voicedeutsch.master.voicecore.session.VoiceEngineState
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated voice waveform visualisation drawn on a Compose [Canvas].
 *
 * Behaviour by [engineState]:
 *  - IDLE          → gentle breathing sine wave, [WaveIdle] colour
 *  - LISTENING     → bar chart from [amplitudes], [WaveListening] colour
 *  - PROCESSING    → pulsating concentric rings, [WaveProcessing] colour
 *  - SPEAKING      → animated sine wave, [WaveSpeaking] colour
 *  - ERROR         → red pulsating flat line, [WaveError] colour
 *
 * Architecture reference: lines 1225-1238 (Voice Waveform component).
 *
 * @param engineState   Current voice engine state — controls animation mode.
 * @param amplitudes    Float array of audio amplitude values in [0f..1f].
 *                      Used in LISTENING mode. Can be empty.
 * @param modifier      Compose modifier — typically [fillMaxWidth] + fixed height.
 * @param barCount      Number of bars in bar-chart mode (default 40).
 * @param barWidthDp    Width of each amplitude bar in dp (default 3.dp).
 * @param gapWidthDp    Gap between bars in dp (default 2.dp).
 */
@Composable
fun VoiceWaveform(
    engineState: VoiceEngineState,
    amplitudes: FloatArray,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(120.dp),
    barCount: Int = 40,
    barWidthDp: Dp = 3.dp,
    gapWidthDp: Dp = 2.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    // ── Global animation phase (used for sine / breathing) ────────────────────
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    // ── Pulse scale for PROCESSING / ERROR states ─────────────────────────────
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    // ── Determine active colour ───────────────────────────────────────────────
    val waveColor = when (engineState) {
        VoiceEngineState.LISTENING   -> WaveListening
        VoiceEngineState.PROCESSING  -> WaveProcessing
        VoiceEngineState.SPEAKING    -> WaveSpeaking
        VoiceEngineState.ERROR       -> WaveError
        else                         -> WaveIdle
    }

    Canvas(modifier = modifier) {
        when (engineState) {
            VoiceEngineState.IDLE,
            VoiceEngineState.CONNECTED,
            VoiceEngineState.SESSION_ACTIVE,
            VoiceEngineState.WAITING -> {
                drawBreathingWave(phase = phase, color = waveColor.copy(alpha = 0.6f))
            }

            VoiceEngineState.LISTENING -> {
                drawAmplitudeBars(
                    amplitudes = amplitudes,
                    barCount   = barCount,
                    barWidthPx = barWidthDp.toPx(),
                    gapWidthPx = gapWidthDp.toPx(),
                    color      = waveColor,
                )
            }

            VoiceEngineState.PROCESSING -> {
                drawPulsingRings(pulseScale = pulseScale, color = waveColor)
            }

            VoiceEngineState.SPEAKING -> {
                drawActiveSineWave(phase = phase, color = waveColor)
            }

            VoiceEngineState.ERROR -> {
                drawFlatLine(pulseScale = pulseScale, color = waveColor)
            }

            else -> {
                drawBreathingWave(phase = phase, color = waveColor.copy(alpha = 0.4f))
            }
        }
    }
}

// ── Canvas draw helpers ───────────────────────────────────────────────────────

/** Slow, gentle sine wave — engine is idle. */
private fun DrawScope.drawBreathingWave(phase: Float, color: Color) {
    val midY = size.height / 2f
    val amplitude = size.height * 0.08f
    val steps = 200

    for (i in 0 until steps) {
        val x = size.width * i / steps
        val y = midY + amplitude * sin(phase + 2f * Math.PI.toFloat() * i / steps)
        drawCircle(
            color  = color,
            radius = 1.5f,
            center = Offset(x, y),
        )
    }
}

/** Bar chart from real amplitude data. */
private fun DrawScope.drawAmplitudeBars(
    amplitudes: FloatArray,
    barCount: Int,
    barWidthPx: Float,
    gapWidthPx: Float,
    color: Color,
) {
    val midY = size.height / 2f
    val maxBarHeight = size.height * 0.9f
    val totalSlotWidth = barWidthPx + gapWidthPx
    val startX = (size.width - totalSlotWidth * barCount) / 2f

    for (i in 0 until barCount) {
        val amplitudeIndex = if (amplitudes.isNotEmpty()) {
            (i * amplitudes.size / barCount).coerceIn(0, amplitudes.size - 1)
        } else -1
        val amplitude = if (amplitudeIndex >= 0) amplitudes[amplitudeIndex] else 0.05f
        val barHeight = (amplitude.coerceIn(0.02f, 1f) * maxBarHeight).coerceAtLeast(4f)
        val x = startX + i * totalSlotWidth + barWidthPx / 2

        drawLine(
            color       = color,
            start       = Offset(x, midY - barHeight / 2),
            end         = Offset(x, midY + barHeight / 2),
            strokeWidth = barWidthPx,
            cap         = StrokeCap.Round,
        )
    }
}

/** Concentric pulsating rings for processing state. */
private fun DrawScope.drawPulsingRings(pulseScale: Float, color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val baseRadius = minOf(size.width, size.height) * 0.22f

    for (ring in 0..2) {
        val r = baseRadius * (1f + ring * 0.35f) * pulseScale
        drawCircle(
            color  = color.copy(alpha = 0.6f - ring * 0.18f),
            radius = r,
            center = Offset(cx, cy),
            style  = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth = 2f + ring.toFloat()),
        )
    }
}

/** Active sine wave with higher amplitude — AI is speaking. */
private fun DrawScope.drawActiveSineWave(phase: Float, color: Color) {
    val midY = size.height / 2f
    val amplitude = size.height * 0.32f
    val steps = 300

    for (i in 1 until steps) {
        val x1 = size.width * (i - 1) / steps
        val y1 = midY + amplitude * sin(phase + 2f * Math.PI.toFloat() * (i - 1) / steps)
        val x2 = size.width * i / steps
        val y2 = midY + amplitude * sin(phase + 2f * Math.PI.toFloat() * i / steps)

        drawLine(
            color       = color.copy(alpha = 0.85f),
            start       = Offset(x1, y1),
            end         = Offset(x2, y2),
            strokeWidth = 2.5f,
            cap         = StrokeCap.Round,
        )
    }
}

/** Flat pulsating line for error state. */
private fun DrawScope.drawFlatLine(pulseScale: Float, color: Color) {
    val midY = size.height / 2f
    val halfWidth = size.width * 0.4f * pulseScale
    val cx = size.width / 2f

    drawLine(
        color       = color,
        start       = Offset(cx - halfWidth, midY),
        end         = Offset(cx + halfWidth, midY),
        strokeWidth = 3f,
        cap         = StrokeCap.Round,
    )
}
