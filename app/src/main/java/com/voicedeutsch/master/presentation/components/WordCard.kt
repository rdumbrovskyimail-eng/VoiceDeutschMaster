package com.voicedeutsch.master.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.presentation.theme.Primary
import com.voicedeutsch.master.presentation.theme.Secondary

@Composable
fun WordCard(
    german: String,
    russian: String,
    knowledgeLevel: Int,
    modifier: Modifier = Modifier,
    partOfSpeech: String? = null,
    exampleSentence: String? = null,
    isNew: Boolean = false,
    onPlayAudio: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isNew               -> Secondary
            knowledgeLevel >= 5 -> Primary.copy(alpha = 0.5f)
            else                -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "word_card_border",
    )

    val cardContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        text       = german,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onBackground,
                    )
                    partOfSpeech?.let {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Text(
                                text     = it,
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (isNew) {
                        Surface(
                            color = Secondary.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Text(
                                text       = "NEW",
                                style      = MaterialTheme.typography.labelSmall,
                                color      = Secondary,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                onPlayAudio?.let {
                    IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = "Произношение",
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Text(
                text  = russian,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )

            exampleSentence?.let {
                Text(
                    text  = "\u201e$it\u201c",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
            }

            Spacer(Modifier.height(4.dp))

            KnowledgeLevelDots(level = knowledgeLevel)
        }
    }

    if (onClick != null) {
        Card(
            onClick  = onClick,
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border   = if (borderColor != Color.Transparent)
                androidx.compose.foundation.BorderStroke(1.5.dp, borderColor) else null,
            modifier = modifier.fillMaxWidth(),
            content  = cardContent,
        )
    } else {
        Card(
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border   = if (borderColor != Color.Transparent)
                androidx.compose.foundation.BorderStroke(1.5.dp, borderColor) else null,
            modifier = modifier.fillMaxWidth(),
            content  = cardContent,
        )
    }
}

@Composable
fun KnowledgeLevelDots(
    level: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 1..7) {
            val filled = i <= level
            val dotColor = when {
                !filled    -> MaterialTheme.colorScheme.outline
                level <= 2 -> MaterialTheme.colorScheme.error
                level <= 4 -> Primary
                else       -> Secondary
            }
            Surface(
                modifier = Modifier.size(8.dp),
                shape    = MaterialTheme.shapes.extraSmall,
                color    = dotColor,
            ) {}
        }
    }
}
