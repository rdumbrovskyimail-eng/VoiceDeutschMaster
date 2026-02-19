package com.voicedeutsch.master.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.presentation.theme.Primary
import com.voicedeutsch.master.presentation.theme.Secondary
import com.voicedeutsch.master.presentation.theme.WaveError
import com.voicedeutsch.master.voicecore.session.VoiceEngineState

/**
 * Animated microphone button for the Session screen.
 *
 * Visual states:
 *  - **Active / Listening**: pulsating green glow behind the button
 *  - **Processing / Speaking**: scaled pulse, blue/green colour
 *  - **Idle / Inactive**: static blue button, no pulse
 *  - **Error**: red colour, slow pulse
 *
 * Architecture reference: lines 1210-1218 (PulsingMicButton component).
 *
 * @param engineState   Current voice engine state.
 * @param onClick       Called when the user taps the button.
 * @param enabled       Whether the button is interactive.
 * @param size          Diameter of the button (default 72.dp).
 */
@Composable
fun PulsingMicButton(
    engineState: VoiceEngineState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 72.dp,
) {
    val isListening = engineState == VoiceEngineState.LISTENING
    val isSpeaking = engineState == VoiceEngineState.SPEAKING
    val isProcessing = engineState == VoiceEngineState.PROCESSING
    val isError = engineState == VoiceEngineState.ERROR
    val isPulsing = isListening || isSpeaking || isProcessing || isError

    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isProcessing) 600 else 900,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseAlpha",
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isProcessing) 600 else 900,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseScale",
    )

    // Determine button colour
    val buttonColor = when {
        isError      -> WaveError
        isListening  -> Secondary     // green — listening
        isSpeaking   -> Secondary     // green — AI talking
        isProcessing -> Primary       // blue — processing
        else         -> Primary       // blue — idle
    }

    // The glow ring colour
    val glowColor = buttonColor

    Box(
        modifier = modifier.size(size * 2),
        contentAlignment = Alignment.Center,
    ) {
        // ── Pulsing glow ring ─────────────────────────────────────────────────
        if (isPulsing) {
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(pulseScale)
                    .drawBehind {
                        drawCircle(
                            color  = glowColor.copy(alpha = pulseAlpha),
                            radius = this.size.minDimension / 2f,
                        )
                    },
            )
        }

        // ── Mic button ────────────────────────────────────────────────────────
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(size),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = buttonColor,
                contentColor   = Color.White,
                disabledContainerColor = buttonColor.copy(alpha = 0.4f),
                disabledContentColor   = Color.White.copy(alpha = 0.5f),
            ),
        ) {
            val isMicOff = engineState == VoiceEngineState.IDLE ||
                           engineState == VoiceEngineState.INITIALIZING ||
                           engineState == VoiceEngineState.ERROR

            Icon(
                imageVector  = if (isMicOff) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = if (isMicOff) "Start listening" else "Stop listening",
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
