package com.voicedeutsch.master.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.voicecore.session.VoiceEngineState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * VirtualAvatar - detailed animated female AI tutor avatar.
 *
 * Size: 350dp. Illustrative style with blinking, breathing, speaking animations.
 * Since Firebase AI SDK manages audio internally, animations are state-based.
 *
 * Changes (03.03.2026):
 * - Redesigned from basic circle to detailed female character
 * - Added blinking, breathing, head sway animations
 * - Mouth animates based on session state + amplitude
 * - 6-7x larger than original (350dp vs ~50dp effective)
 */
@Composable
fun VirtualAvatar(
    engineState: VoiceEngineState,
    currentAmplitude: State<Float>,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")

    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "breath",
    )

    val blinkPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing)),
        label = "blink",
    )

    val swayPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "sway",
    )

    val speakPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing)),
        label = "speak",
    )

    val eyeOffsetX by animateFloatAsState(
        targetValue = when (engineState) {
            VoiceEngineState.PROCESSING -> 8f
            VoiceEngineState.CONTEXT_LOADING -> -5f
            else -> 0f
        },
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "eyeX",
    )
    val eyeOffsetY by animateFloatAsState(
        targetValue = if (engineState == VoiceEngineState.PROCESSING) -6f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "eyeY",
    )

    val isSessionActive = engineState == VoiceEngineState.SESSION_ACTIVE ||
            engineState == VoiceEngineState.LISTENING ||
            engineState == VoiceEngineState.SPEAKING ||
            engineState == VoiceEngineState.WAITING

    Canvas(modifier = modifier.size(350.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val baseY = h * 0.38f
        val breathOff = sin(breathPhase) * 3f
        val sway = sin(swayPhase) * 2f
        val cy = baseY + breathOff
        val hx = cx + sway

        val blinkNorm = (blinkPhase % (2f * PI.toFloat())) / (2f * PI.toFloat())
        val blinkOpen = if (blinkNorm > 0.93f || blinkNorm < 0.03f) {
            val t = if (blinkNorm > 0.93f) (blinkNorm - 0.93f) / 0.07f else (0.03f - blinkNorm) / 0.03f
            1f - (sin(t * PI.toFloat()) * 0.95f)
        } else 1f

        val amp = currentAmplitude.value
        val mouthOpen = if (isSessionActive) {
            val sp = (sin(speakPhase) * 0.5f + 0.5f)
            val combined = (amp * 0.6f + sp * 0.4f).coerceIn(0f, 1f)
            combined * 18f + 2f
        } else {
            2f + sin(breathPhase * 0.5f) * 1f
        }

        val skinBase = Color(0xFFFADCD0)
        val skinShadow = Color(0xFFF0C4B0)
        val skinHi = Color(0xFFFFF0E8)
        val hairDk = Color(0xFF3D2314)
        val hairMd = Color(0xFF5C3A28)
        val hairLt = Color(0xFF7A4E35)
        val eyeWh = Color(0xFFFFFEFC)
        val irisC = Color(0xFF4A90B8)
        val irisDk = Color(0xFF2C6080)
        val pupilC = Color(0xFF1A1A2E)
        val lashC = Color(0xFF2A1A10)
        val lipC = Color(0xFFD4707A)
        val lipDk = Color(0xFFA85060)
        val lipHi = Color(0xFFE8A0A8)
        val browC = Color(0xFF4A3228)
        val clothC = Color(0xFF5B7DB1)
        val clothDk = Color(0xFF3D5A8A)
        val clothHi = Color(0xFF7A9DCE)

        // Shoulders
        val shY = h * 0.78f
        val shPath = Path().apply {
            moveTo(hx - 140f, h + 20f)
            cubicTo(hx - 140f, shY + 30f, hx - 100f, shY - 10f, hx - 30f, shY)
            lineTo(hx + 30f, shY)
            cubicTo(hx + 100f, shY - 10f, hx + 140f, shY + 30f, hx + 140f, h + 20f)
            close()
        }
        drawPath(shPath, brush = Brush.verticalGradient(listOf(clothHi, clothC, clothDk), shY - 10f, h + 20f))
        val nlPath = Path().apply {
            moveTo(hx - 35f, shY + 5f)
            quadraticTo(hx, shY + 30f, hx + 35f, shY + 5f)
        }
        drawPath(nlPath, color = clothDk, style = Stroke(width = 2f, cap = StrokeCap.Round))

        // Neck
        val nkPath = Path().apply {
            moveTo(hx - 28f, cy + 75f)
            lineTo(hx - 32f, shY + 5f)
            lineTo(hx + 32f, shY + 5f)
            lineTo(hx + 28f, cy + 75f)
            close()
        }
        drawPath(nkPath, color = skinBase)

        // Hair back
        for (side in listOf(-1f, 1f)) {
            val hbPath = Path().apply {
                moveTo(hx + side * 95f, cy - 40f)
                cubicTo(hx + side * 110f, cy + 50f, hx + side * 100f, cy + 150f, hx + side * 80f, h * 0.85f)
                lineTo(hx + side * 50f, h * 0.88f)
                cubicTo(hx + side * 60f, cy + 140f, hx + side * 75f, cy + 50f, hx + side * 65f, cy - 10f)
                close()
            }
            drawPath(hbPath, color = hairDk)
        }

        // Face
        val facePath = Path().apply {
            moveTo(hx, cy - 85f)
            cubicTo(hx + 72f, cy - 85f, hx + 78f, cy - 20f, hx + 72f, cy + 20f)
            cubicTo(hx + 65f, cy + 55f, hx + 35f, cy + 80f, hx, cy + 85f)
            cubicTo(hx - 35f, cy + 80f, hx - 65f, cy + 55f, hx - 72f, cy + 20f)
            cubicTo(hx - 78f, cy - 20f, hx - 72f, cy - 85f, hx, cy - 85f)
            close()
        }
        drawPath(facePath, brush = Brush.radialGradient(listOf(skinHi, skinBase, skinShadow), Offset(hx - 10f, cy - 20f), 160f))

        // Ears
        for (side in listOf(-1f, 1f)) {
            val ear = Path().apply {
                moveTo(hx + side * 72f, cy - 10f)
                cubicTo(hx + side * 88f, cy - 15f, hx + side * 90f, cy + 15f, hx + side * 72f, cy + 18f)
            }
            drawPath(ear, color = skinBase, style = Stroke(width = 8f, cap = StrokeCap.Round))
        }

        // Eyebrows
        val browLift = if (engineState == VoiceEngineState.PROCESSING) -4f else 0f
        for (side in listOf(-1f, 1f)) {
            val brow = Path().apply {
                moveTo(hx + side * (-1f) * 56f, cy - 32f + browLift)
                quadraticTo(hx + side * (-1f) * 38f, cy - 42f + browLift, hx + side * (-1f) * 18f, cy - 34f + browLift)
            }
            drawPath(brow, color = browC, style = Stroke(width = 4f, cap = StrokeCap.Round))
        }

        // Eyes
        val eyeY = cy - 15f
        val eSpacing = 38f
        val eW = 28f
        val eH = 18f * blinkOpen
        for (side in listOf(-1f, 1f)) {
            val ex = hx + side * eSpacing
            drawOval(color = eyeWh, topLeft = Offset(ex - eW, eyeY - eH), size = Size(eW * 2, eH * 2))
            if (blinkOpen > 0.15f) {
                val iR = 12f * blinkOpen
                drawCircle(brush = Brush.radialGradient(listOf(irisC, irisDk), Offset(ex + eyeOffsetX, eyeY + eyeOffsetY), iR), radius = iR, center = Offset(ex + eyeOffsetX, eyeY + eyeOffsetY))
                drawCircle(color = pupilC, radius = 5f * blinkOpen, center = Offset(ex + eyeOffsetX, eyeY + eyeOffsetY))
                drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 3f, center = Offset(ex + eyeOffsetX + 4f, eyeY + eyeOffsetY - 4f))
            }
            val lid = Path().apply {
                moveTo(ex - eW - 2f, eyeY)
                quadraticTo(ex, eyeY - eH - 3f, ex + eW + 2f, eyeY)
            }
            drawPath(lid, color = lashC, style = Stroke(width = 3f, cap = StrokeCap.Round))
            if (blinkOpen > 0.5f) {
                for (i in 0..4) {
                    val t = i / 4f
                    val lx = ex - eW + t * eW * 2
                    val ly = eyeY - eH - 2f + abs(t - 0.5f) * 6f
                    val lLen = if (i == 2) 8f else 5f
                    val ang = (-80f + t * (-20f)) * PI.toFloat() / 180f
                    drawLine(lashC, Offset(lx, ly), Offset(lx + cos(ang) * lLen, ly + sin(ang) * lLen), 2f, StrokeCap.Round)
                }
            }
        }

        // Nose
        val nosePath = Path().apply {
            moveTo(hx - 2f, cy + 5f)
            quadraticTo(hx - 8f, cy + 25f, hx - 5f, cy + 30f)
            quadraticTo(hx, cy + 34f, hx + 5f, cy + 30f)
            quadraticTo(hx + 8f, cy + 25f, hx + 2f, cy + 5f)
        }
        drawPath(nosePath, color = skinShadow.copy(alpha = 0.4f), style = Stroke(width = 1.5f, cap = StrokeCap.Round))

        // Blush
        drawCircle(color = Color(0x30E88090), radius = 22f, center = Offset(hx - 52f, cy + 30f))
        drawCircle(color = Color(0x30E88090), radius = 22f, center = Offset(hx + 52f, cy + 30f))

        // Mouth
        val mY = cy + 52f
        val mW = 22f
        if (mouthOpen > 5f) {
            val moPath = Path().apply {
                moveTo(hx - mW, mY)
                quadraticTo(hx, mY - 4f, hx + mW, mY)
                quadraticTo(hx, mY + mouthOpen, hx - mW, mY)
                close()
            }
            drawPath(moPath, color = Color(0xFF5A1A1A))
            val ulPath = Path().apply {
                moveTo(hx - mW - 3f, mY)
                quadraticTo(hx - 10f, mY - 7f, hx, mY - 5f)
                quadraticTo(hx + 10f, mY - 7f, hx + mW + 3f, mY)
                quadraticTo(hx, mY + 2f, hx - mW - 3f, mY)
                close()
            }
            drawPath(ulPath, color = lipC)
            val llPath = Path().apply {
                moveTo(hx - mW, mY + 2f)
                quadraticTo(hx, mY + mouthOpen + 5f, hx + mW, mY + 2f)
                quadraticTo(hx, mY + mouthOpen - 2f, hx - mW, mY + 2f)
                close()
            }
            drawPath(llPath, color = lipC)
        } else {
            val ulPath = Path().apply {
                moveTo(hx - mW - 2f, mY)
                quadraticTo(hx - 8f, mY - 5f, hx, mY - 3f)
                quadraticTo(hx + 8f, mY - 5f, hx + mW + 2f, mY)
                quadraticTo(hx, mY + 3f, hx - mW - 2f, mY)
                close()
            }
            drawPath(ulPath, color = lipC)
            val llPath = Path().apply {
                moveTo(hx - mW, mY + 1f)
                quadraticTo(hx, mY + 10f, hx + mW, mY + 1f)
                quadraticTo(hx, mY + 6f, hx - mW, mY + 1f)
                close()
            }
            drawPath(llPath, color = lipC)
            val llnPath = Path().apply {
                moveTo(hx - mW, mY)
                quadraticTo(hx, mY + 2f + mouthOpen * 0.3f, hx + mW, mY)
            }
            drawPath(llnPath, color = lipDk, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
        }

        // Hair front - top
        val htPath = Path().apply {
            moveTo(hx - 80f, cy - 50f)
            cubicTo(hx - 85f, cy - 100f, hx - 30f, cy - 115f, hx, cy - 110f)
            cubicTo(hx + 30f, cy - 115f, hx + 85f, cy - 100f, hx + 80f, cy - 50f)
            cubicTo(hx + 75f, cy - 80f, hx + 20f, cy - 95f, hx, cy - 90f)
            cubicTo(hx - 20f, cy - 95f, hx - 75f, cy - 80f, hx - 80f, cy - 50f)
            close()
        }
        drawPath(htPath, brush = Brush.verticalGradient(listOf(hairDk, hairMd), cy - 115f, cy - 50f))

        // Hair shine
        val hsPath = Path().apply {
            moveTo(hx - 20f, cy - 105f)
            quadraticTo(hx, cy - 95f, hx + 25f, cy - 100f)
            quadraticTo(hx + 5f, cy - 88f, hx - 20f, cy - 95f)
            close()
        }
        drawPath(hsPath, color = hairLt.copy(alpha = 0.4f))

        // Side bangs
        for (side in listOf(-1f, 1f)) {
            val bang = Path().apply {
                moveTo(hx + side * 55f, cy - 80f)
                cubicTo(hx + side * 75f, cy - 60f, hx + side * 80f, cy - 20f, hx + side * 72f, cy + 5f)
                cubicTo(hx + side * 68f, cy - 15f, hx + side * 65f, cy - 50f, hx + side * 45f, cy - 70f)
                close()
            }
            drawPath(bang, color = hairDk)
        }

        // Fringe
        val fringe = Path().apply {
            moveTo(hx - 60f, cy - 75f)
            cubicTo(hx - 40f, cy - 55f, hx - 25f, cy - 60f, hx - 10f, cy - 55f)
            cubicTo(hx + 5f, cy - 52f, hx + 20f, cy - 58f, hx + 35f, cy - 50f)
            cubicTo(hx + 50f, cy - 55f, hx + 65f, cy - 70f, hx + 70f, cy - 75f)
            cubicTo(hx + 60f, cy - 90f, hx - 50f, cy - 95f, hx - 60f, cy - 75f)
            close()
        }
        drawPath(fringe, brush = Brush.verticalGradient(listOf(hairDk, hairMd), cy - 95f, cy - 50f))

        // Status dot
        val dotColor = when {
            isSessionActive -> Color(0xFF4CAF50)
            engineState == VoiceEngineState.CONNECTING || engineState == VoiceEngineState.CONTEXT_LOADING -> Color(0xFFFFA726)
            engineState == VoiceEngineState.ERROR -> Color(0xFFEF5350)
            else -> Color(0xFF9E9E9E)
        }
        val pulse = if (isSessionActive) (sin(breathPhase * 2f) * 0.3f + 0.7f) else 1f
        drawCircle(color = dotColor.copy(alpha = 0.3f * pulse), radius = 12f, center = Offset(w - 30f, 30f))
        drawCircle(color = dotColor, radius = 6f, center = Offset(w - 30f, 30f))
    }
}
