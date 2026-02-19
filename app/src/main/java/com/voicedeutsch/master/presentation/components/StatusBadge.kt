package com.voicedeutsch.master.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.presentation.theme.Error
import com.voicedeutsch.master.presentation.theme.Primary
import com.voicedeutsch.master.presentation.theme.Secondary
import com.voicedeutsch.master.presentation.theme.Warning
import com.voicedeutsch.master.presentation.theme.WaveIdle
import com.voicedeutsch.master.voicecore.session.ConnectionState
import com.voicedeutsch.master.voicecore.session.VoiceEngineState

/**
 * Small pill-shaped badge showing the current voice engine status.
 *
 * Examples:
 *  - ● IDLE       (grey)
 *  - ● LISTENING  (blue, animated)
 *  - ● SPEAKING   (green)
 *  - ● PROCESSING (yellow)
 *  - ● ERROR      (red)
 *
 * Architecture reference: lines 1220-1225 (StatusBadge component).
 *
 * @param engineState     Current voice engine state.
 * @param connectionState Current WebSocket connection state.
 */
@Composable
fun StatusBadge(
    engineState: VoiceEngineState,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    val (dotColor, label) = statusInfo(engineState, connectionState)

    val animatedDotColor by animateColorAsState(
        targetValue = dotColor,
        animationSpec = tween(durationMillis = 300),
        label = "statusDot",
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status dot
            Spacer(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(animatedDotColor),
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun statusInfo(
    engineState: VoiceEngineState,
    connectionState: ConnectionState,
): Pair<Color, String> = when {
    connectionState == ConnectionState.CONNECTING  -> Warning to "Подключение..."
    connectionState == ConnectionState.RECONNECTING -> Warning to "Переподключение..."
    engineState == VoiceEngineState.ERROR          -> Error to "Ошибка"
    engineState == VoiceEngineState.LISTENING      -> Primary to "Слушаю"
    engineState == VoiceEngineState.PROCESSING     -> Warning to "Думаю..."
    engineState == VoiceEngineState.SPEAKING       -> Secondary to "Говорю"
    engineState == VoiceEngineState.SESSION_ACTIVE -> Secondary to "Сессия активна"
    engineState == VoiceEngineState.WAITING        -> Primary.copy(alpha = 0.7f) to "Жду..."
    connectionState == ConnectionState.CONNECTED   -> Secondary to "Подключено"
    else                                           -> WaveIdle to "Неактивно"
}
