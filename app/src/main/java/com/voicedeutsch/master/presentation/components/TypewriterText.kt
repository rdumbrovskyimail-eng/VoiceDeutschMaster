package com.voicedeutsch.master.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

@Composable
fun TypewriterText(
    fullText: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onBackground,
    charDelayMs: Long = 25L,
    onComplete: () -> Unit = {},
) {
    var displayedLength by remember(fullText) { mutableIntStateOf(0) }

    LaunchedEffect(fullText) {
        displayedLength = 0
        for (i in 1..fullText.length) {
            displayedLength = i
            delay(charDelayMs)
        }
        onComplete()
    }

    Text(
        text = fullText.take(displayedLength),
        modifier = modifier,
        style = style,
        color = color,
    )
}