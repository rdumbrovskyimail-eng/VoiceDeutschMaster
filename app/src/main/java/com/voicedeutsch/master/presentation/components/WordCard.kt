package com.voicedeutsch.master.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicedeutsch.master.presentation.theme.Primary
import com.voicedeutsch.master.presentation.theme.Secondary

/**
 * Reusable word card component.
 *
 * Displays a German word with its Russian translation, knowledge level indicator,
 * part of speech, and an optional audio playback button.
 *
 * Used on:
 *  - KnowledgeScreen (word list tab)
 *  - SessionScreen (vocabulary summary)
 *  - DashboardScreen (recently learned words)
 *
 * @param german          German word (e.g. "der Apfel").
 * @param russian         Russian translation (e.g. "яблоко").
 * @param knowledgeLevel  0-7 — rendered as filled dots.
 * @param partOfSpeech    Optional tag like "сущ.", "гл.", "прил."
 * @param exampleSentence Optional example sentence in German.
 * @param isNew           Highlight card as newly learned.
 * @param onPlayAudio     If non-null, show the speaker icon button.
 * @param onClick         Card click callback.
 */
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
            isNew          -> Secondary
            knowledgeLevel >= 5 -> Primary.copy(alpha = 0.5f)
            else           -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "word_card_border",
    )

    val cardContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // ── Top row: word + audio button ──────────────────────────────────
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
                                text     = "NEW",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = Secondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    onPlayAudio?.let {
                        IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Outlined.VolumeUp,
                                contentDescription = "Произношение",
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            // ── Translation ───────────────────────────────────────────────────
            Text(
                text  = russian,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )

            // ── Example sentence ──────────────────────────────────────────────
            exampleSentence?.let {
                Text(
                    text  = "„$it"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Knowledge level dots ──────────────────────────────────────────
            KnowledgeLevelDots(level = knowledgeLevel)
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            colors  = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border  = if (borderColor != Color.Transparent)
                androidx.compose.foundation.BorderStroke(1.5.dp, borderColor) else null,
            modifier = modifier.fillMaxWidth(),
            content  = cardContent,
        )
    } else {
        Card(
            colors  = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border  = if (borderColor != Color.Transparent)
                androidx.compose.foundation.BorderStroke(1.5.dp, borderColor) else null,
            modifier = modifier.fillMaxWidth(),
            content  = cardContent,
        )
    }
}

// ── Knowledge level dots ──────────────────────────────────────────────────────

/**
 * Row of 7 dots representing knowledge level 0-7.
 * Filled dots use color scaled from red (low) to green (high).
 */
@Composable
fun KnowledgeLevelDots(level: Int, modifier: Modifier = Modifier) {
    val dotColors = listOf(
        Color(0xFF606060), // 0 – not seen
        Color(0xFFE53935), // 1
        Color(0xFFFF7043), // 2
        Color(0xFFFFB300), // 3
        Color(0xFFFFD54F), // 4
        Color(0xFF66BB6A), // 5
        Color(0xFF43A047), // 6
        Color(0xFF2E7D32), // 7 – mastered
    )

    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        repeat(7) { index ->
            val dotLevel = index + 1
            Surface(
                modifier = Modifier.size(8.dp),
                shape    = androidx.compose.foundation.shape.CircleShape,
                color    = if (dotLevel <= level)
                    dotColors.getOrElse(level) { Secondary }
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            ) {}
        }
        Text(
            text  = "Ур.$level",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
