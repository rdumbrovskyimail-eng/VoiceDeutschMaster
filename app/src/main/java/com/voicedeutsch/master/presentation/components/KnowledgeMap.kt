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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.presentation.theme.Primary
import com.voicedeutsch.master.presentation.theme.Secondary
import kotlin.math.min

/**
 * Knowledge Map — treemap-style visualization of vocabulary by topic.
 *
 * Each rectangle's area is proportional to the number of known words in that topic.
 * Used on [KnowledgeScreen] Overview tab.
 *
 * @param topicDistribution  Map of topic name → known words count.
 * @param modifier           Applied to the Canvas container.
 */
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

    // Palette — cycle through predefined colors
    val palette = listOf(
        Primary,
        Secondary,
        Color(0xFF9C27B0), // purple
        Color(0xFFFF9800), // orange
        Color(0xFF00BCD4), // cyan
        Color(0xFFE91E63), // pink
        Color(0xFF4CAF50), // green
        Color(0xFF795548), // brown
    )

    // Sort by count descending, take top 12 topics
    val sorted = topicDistribution.entries
        .sortedByDescending { it.value }
        .take(12)
    val total  = sorted.sumOf { it.value }.coerceAtLeast(1)

    Canvas(modifier = modifier) {
        val rects = buildTreemap(
            items  = sorted.map { it.value.toFloat() / total },
            bounds = Size(size.width, size.height),
        )

        rects.forEachIndexed { i, rect ->
            val topic = sorted.getOrNull(i) ?: return@forEachIndexed
            val color = palette[i % palette.size]

            // Fill
            drawRect(
                color   = color.copy(alpha = 0.7f),
                topLeft = rect.topLeft,
                size    = rect.size,
            )

            // Border
            drawRect(
                color     = Color.Black.copy(alpha = 0.2f),
                topLeft   = rect.topLeft,
                size      = rect.size,
                style     = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
            )

            // Label — only if rect is big enough
            if (rect.size.width > 40.dp.toPx() && rect.size.height > 24.dp.toPx()) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        textSize   = 10.dp.toPx()
                        isAntiAlias = true
                        textAlign  = android.graphics.Paint.Align.CENTER
                    }
                    val cx = rect.topLeft.x + rect.size.width / 2
                    val cy = rect.topLeft.y + rect.size.height / 2 + textSize / 3
                    drawText(
                        topic.key.take(12),
                        cx,
                        cy,
                        paint,
                    )
                }
            }
        }
    }
}

// ── Squarified Treemap ────────────────────────────────────────────────────────

private data class Rect(val topLeft: Offset, val size: Size)

/**
 * Very simple strip-based treemap layout (not perfectly squarified, but readable).
 */
private fun buildTreemap(
    items: List<Float>,
    bounds: Size,
): List<Rect> {
    if (items.isEmpty()) return emptyList()

    val result   = mutableListOf<Rect>()
    var remaining = items.toMutableList()
    var x = 0f
    var y = 0f
    var availableWidth  = bounds.width
    var availableHeight = bounds.height

    while (remaining.isNotEmpty()) {
        val isHorizontal = availableWidth >= availableHeight
        val stripSize    = if (isHorizontal) availableHeight else availableWidth
        val totalFraction = remaining.sumOf { it.toDouble() }.toFloat().coerceAtLeast(0.001f)

        // Greedily fill one strip
        val stripItems = remaining.toList()
        remaining.clear()

        var offset = 0f
        val stripWidth = min(
            if (isHorizontal) availableWidth else availableHeight,
            (if (isHorizontal) availableWidth else availableHeight),
        ) * totalFraction

        stripItems.forEach { fraction ->
            val itemLength = (fraction / totalFraction) * stripSize
            if (isHorizontal) {
                result.add(Rect(Offset(x, y + offset), Size(stripWidth, itemLength)))
                offset += itemLength
            } else {
                result.add(Rect(Offset(x + offset, y), Size(itemLength, stripWidth)))
                offset += itemLength
            }
        }

        if (isHorizontal) {
            x                += stripWidth
            availableWidth   -= stripWidth
        } else {
            y                += stripWidth
            availableHeight  -= stripWidth
        }

        break // single-pass simplified layout; good enough for up to 12 items
    }

    return result
}
