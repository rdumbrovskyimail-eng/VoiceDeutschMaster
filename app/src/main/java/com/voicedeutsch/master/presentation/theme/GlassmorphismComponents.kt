package com.voicedeutsch.master.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    blurRadius: Dp = 16.dp,
    backgroundAlpha: Float = 0.12f,
    borderAlpha: Float = 0.18f,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = backgroundAlpha + 0.05f),
                        Color.White.copy(alpha = backgroundAlpha),
                    )
                )
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = borderAlpha),
                        Color.Transparent,
                    )
                ),
                shape = shape,
            ),
        content = content,
    )
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        backgroundAlpha = 0.08f,
        borderAlpha = 0.12f,
        content = content,
    )
}