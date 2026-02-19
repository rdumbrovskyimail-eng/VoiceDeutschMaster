package com.voicedeutsch.master.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.presentation.theme.Primary
import com.voicedeutsch.master.presentation.theme.Secondary

/**
 * CEFR Level indicator component.
 *
 * Shows the current level badge (e.g. "B1"), sub-level progress bar (1-10),
 * and optionally the level name.
 *
 * Used on:
 *  - DashboardScreen header area
 *  - StatisticsScreen overview tab
 *
 * @param cefrLevel     Current CEFR level enum.
 * @param subLevel      Sub-level within the CEFR band (1-10).
 * @param showLabel     Whether to display the human-readable level name.
 * @param showProgress  Whether to show the sub-level progress bar.
 * @param modifier      Applied to the root layout.
 */
@Composable
fun LevelIndicator(
    cefrLevel: CefrLevel,
    subLevel: Int,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    showProgress: Boolean = true,
) {
    var animated by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue   = if (animated) (subLevel.coerceIn(1, 10) - 1) / 9f else 0f,
        animationSpec = tween(900),
        label         = "level_progress",
    )
    LaunchedEffect(Unit) { animated = true }

    val levelColor = levelColor(cefrLevel)

    Row(
        modifier             = modifier,
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Level badge ───────────────────────────────────────────────────────
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = levelColor.copy(alpha = 0.15f),
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text       = cefrLevel.name,
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color      = levelColor,
                )
            }
        }

        // ── Label + progress ──────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showLabel) {
                Text(
                    text  = cefrLevel.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
            Text(
                text       = "Подуровень $subLevel / 10",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
            if (showProgress) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .widthIn(min = 80.dp, max = 200.dp)
                        .height(6.dp),
                    color        = levelColor,
                    trackColor   = levelColor.copy(alpha = 0.2f),
                )
            }
        }

        // ── Next level hint ───────────────────────────────────────────────────
        cefrLevel.next()?.let { next ->
            if (subLevel >= 9) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text  = "→",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    )
                    Text(
                        text  = next.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = levelColor(next).copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

/**
 * Level color semantic mapping — low levels warm (red/orange), high levels cool (blue/green).
 */
fun levelColor(level: CefrLevel): Color = when (level) {
    CefrLevel.A1 -> Color(0xFFEF5350) // red
    CefrLevel.A2 -> Color(0xFFFF7043) // deep orange
    CefrLevel.B1 -> Color(0xFFFFB300) // amber
    CefrLevel.B2 -> Color(0xFF66BB6A) // green
    CefrLevel.C1 -> Primary           // blue
    CefrLevel.C2 -> Color(0xFF9C27B0) // purple (mastery)
}
