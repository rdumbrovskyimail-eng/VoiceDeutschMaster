package com.voicedeutsch.master.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.presentation.theme.Primary
import com.voicedeutsch.master.presentation.theme.Secondary

/**
 * Circular ring progress indicator.
 *
 * Used on DashboardScreen to show overall vocabulary completion.
 *
 * @param progress      Value from 0.0 to 1.0.
 * @param size          Outer diameter of the ring.
 * @param strokeWidth   Width of the arc stroke.
 * @param label         Text displayed at the centre (e.g. "42%").
 * @param trackColor    Background ring color.
 * @param progressColor Filled arc color.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
    label: String? = null,
    trackColor: Color = Color.White.copy(alpha = 0.1f),
    progressColor: Color = Secondary,
) {
    // Animate on first composition and whenever progress changes
    var animated by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animated) progress.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "progress_ring",
    )

    LaunchedEffect(Unit) { animated = true }

    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier.size(size),
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke     = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val diameter   = minOf(this.size.width, this.size.height) - strokeWidth.toPx()
            val topLeft    = androidx.compose.ui.geometry.Offset(
                (this.size.width - diameter) / 2f,
                (this.size.height - diameter) / 2f,
            )
            val arcSize    = androidx.compose.ui.geometry.Size(diameter, diameter)

            // Track (background ring)
            drawArc(
                color        = trackColor,
                startAngle   = 0f,
                sweepAngle   = 360f,
                useCenter    = false,
                topLeft      = topLeft,
                size         = arcSize,
                style        = Stroke(width = strokeWidth.toPx()),
            )

            // Progress arc
            if (animatedProgress > 0f) {
                drawArc(
                    color      = progressColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = stroke,
                )
            }
        }

        // Centre label
        if (label != null) {
            Text(
                text       = label,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
