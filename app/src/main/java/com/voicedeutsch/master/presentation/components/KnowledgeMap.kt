package com.voicedeutsch.master.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.presentation.theme.Primary
import com.voicedeutsch.master.presentation.theme.Secondary
import kotlin.math.min

@Composable
fun KnowledgeMap(
    topicDistribution: Map<String, Int>,
    modifier: Modifier = Modifier,
) {
    if (topicDistribution.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                text  = "Начните занятия, чтобы увидеть карту знаний",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        }
        return
    }

    val palette = listOf(
        Primary,
        Secondary,
        Color(0xFF9C27B0),
        Color(0xFFFF9800),
        Color(0xFF00BCD4),
        Color(0xFFE91E63),
        Color(0xFF4CAF50),
        Color(0xFF795548),
    )

    val sorted = topicDistribution.entries
        .sortedByDescending { it.value }
        .take(12)
    val total = sorted.sumOf { it.value }.coerceAtLeast(1)

    Canvas(modifier = modifier) {
        val rects = buildTreemap(
            items  = sorted.map { it.value.toFloat() / total },
            bounds = Size(size.width, size.height),
        )

        rects.forEachIndexed { i, rect ->
            val topic = sorted.getOrNull(i) ?: return@forEachIndexed
            val cellColor = palette[i % palette.size]

            drawRect(
                color   = cellColor.copy(alpha = 0.7f),
                topLeft = rect.topLeft,
                size    = rect.size,
            )

            drawRect(
                color     = Color.Black.copy(alpha = 0.2f),
                topLeft   = rect.topLeft,
                size      = rect.size,
                style     = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
            )

            if (rect.size.width > 40.dp.toPx() && rect.size.height > 24.dp.toPx()) {
                val paint = android.graphics.Paint().apply {
                    color       = android.graphics.Color.WHITE
                    textSize    = 10.dp.toPx()
                    isAntiAlias = true
                    textAlign   = android.graphics.Paint.Align.CENTER
                }
                val cx = rect.topLeft.x + rect.size.width / 2
                val cy = rect.topLeft.y + rect.size.height / 2 + paint.textSize / 3
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(topic.key.take(12), cx, cy, paint)
                }
            }
        }
    }
}

private data class Rect(val topLeft: Offset, val size: Size)

private fun buildTreemap(items: List<Float>, bounds: Size): List<Rect> {
    if (items.isEmpty()) return emptyList()
    val rects = mutableListOf<Rect>()
    var remaining = items.toMutableList()
    var x = 0f; var y = 0f; var w = bounds.width; var h = bounds.height

    while (remaining.isNotEmpty()) {
        val horizontal  = w >= h
        val stripCount  = min(remaining.size, 4)
        val strip       = remaining.take(stripCount)
        val stripSum    = strip.sum()
        val stripSize   = if (horizontal) w * stripSum else h * stripSum
        var offset      = if (horizontal) y else x

        strip.forEach { frac ->
            val itemSize = if (stripSum > 0) frac / stripSum * (if (horizontal) h else w) else 0f
            rects.add(
                if (horizontal) Rect(Offset(x, offset), Size(stripSize, itemSize))
                else            Rect(Offset(offset, y), Size(itemSize, stripSize))
            )
            offset += itemSize
        }

        remaining = remaining.drop(stripCount).toMutableList()
        if (horizontal) { x += stripSize; w -= stripSize }
        else            { y += stripSize; h -= stripSize }
    }

    return rects
}
