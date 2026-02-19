package com.voicedeutsch.master.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Compact session timer display: 🕐 MM:SS
 *
 * Animates digit changes with a vertical slide for a polished feel.
 *
 * @param durationMs  Elapsed session duration in milliseconds.
 * @param color       Text/icon colour — defaults to the current content colour.
 */
@Composable
fun SessionTimer(
    durationMs: Long,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = (totalSeconds / 60L).toInt()
    val seconds = (totalSeconds % 60L).toInt()

    // Format as MM:SS
    val minutesStr = minutes.toString().padStart(2, '0')
    val secondsStr = seconds.toString().padStart(2, '0')

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = Icons.Outlined.Timer,
            contentDescription = null,
            tint               = color,
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Animated minutes
        AnimatedDigitGroup(text = minutesStr, color = color)

        Text(
            text  = ":",
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )

        // Animated seconds
        AnimatedDigitGroup(text = secondsStr, color = color)
    }
}

@Composable
private fun AnimatedDigitGroup(text: String, color: Color) {
    for (char in text) {
        AnimatedContent(
            targetState = char,
            transitionSpec = {
                slideInVertically { it } togetherWith slideOutVertically { -it }
            },
            label = "digit_$char",
        ) { digit ->
            Text(
                text  = digit.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
        }
    }
}
