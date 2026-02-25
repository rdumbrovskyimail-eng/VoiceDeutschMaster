package com.voicedeutsch.master.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import
2. androidx.compose.foundation.layout.size
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

@Composable
fun VirtualAvatar(
    engineState: VoiceEngineState,
    amplitudes: FloatArray,
    modifier: Modifier = Modifier
) {
    // Берем последнюю амплитуду для анимации рта
    val currentAmp = remember(amplitudes) { 
        if (amplitudes.isNotEmpty()) amplitudes.last().coerceIn(0f, 1f) else 0f 
    }

    // Анимация дыхания (плечи/голова слегка двигаются)
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breath by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = SineEasing)), label = "breath"
    )

    // Плавное открытие рта
    val mouthOpen by animateFloatAsState(
        targetValue = if (engineState == VoiceEngineState.SPEAKING) 10f + (currentAmp * 60f) else 2f,
        animationSpec = tween(50), label = "mouth"
    )

    // Позиция зрачков (думает = смотрит вверх-вправо)
    val eyeOffsetX by animateFloatAsState(
        targetValue = if (engineState == VoiceEngineState.PROCESSING) 15f else 0f,
        animationSpec = tween(500), label = "eyeX"
    )
    val eyeOffsetY by animateFloatAsState(
        targetValue = if (engineState == VoiceEngineState.PROCESSING) -15f else 0f,
        animationSpec = tween(500), label = "eyeY"
    )

    Canvas(modifier = modifier.size(200.dp)) {
        val cx = size.width / 2
        val cy = size.height / 2 + (sin(breath) * 5f) // Дыхание

        // Лицо
        drawCircle(color = AvatarSkin, radius = 80f, center = Offset(cx, cy))
        
        // Волосы (стилизованная шапка)
        drawArc(
            color = AvatarHair, startAngle = 180f, sweepAngle = 180f,
            useCenter = true, topLeft = Offset(cx - 85f, cy - 90f), size = Size(170f, 120f)
        )

        // Глаза (белки)
        drawCircle(color = Color.White, radius = 15f, center = Offset(cx - 30f, cy - 10f))
        drawCircle(color = Color.White, radius = 15f, center = Offset(cx + 30f, cy - 10f))

        // Зрачки
        drawCircle(color = Primary, radius = 6f, center = Offset(cx - 30f + eyeOffsetX, cy - 10f + eyeOffsetY))
        drawCircle(color = Primary, radius = 6f, center = Offset(cx + 30f + eyeOffsetX, cy - 10f + eyeOffsetY))

        // Рот (Path)
        val mouthPath = Path().apply {
            moveTo(cx - 20f, cy + 30f)
            quadraticTo(cx, cy + 30f + mouthOpen, cx + 20f, cy + 30f)
            if (mouthOpen > 5f) {
                quadraticTo(cx, cy + 30f + (mouthOpen * 1.5f), cx - 20f, cy + 30f)
            }
        }
        
        if (mouthOpen > 5f) {
            drawPath(mouthPath, color = Color(0xFF991B1B)) // Открытый рот
        } else {
            drawPath(mouthPath, color = Color(0xFF78350F), style = Stroke(width = 4f, cap = StrokeCap.Round)) // Улыбка
        }
    }
}
