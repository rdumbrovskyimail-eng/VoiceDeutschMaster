package com.voicedeutsch.master.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val VoiceDeutschShapes = Shapes(
    // Chips, small badges
    extraSmall = RoundedCornerShape(4.dp),
    // Buttons, text fields, cards
    small      = RoundedCornerShape(8.dp),
    // Cards, dialogs
    medium     = RoundedCornerShape(12.dp),
    // Bottom sheets, large cards
    large      = RoundedCornerShape(16.dp),
    // Full rounded — mic button, floating elements
    extraLarge = RoundedCornerShape(28.dp),
)
