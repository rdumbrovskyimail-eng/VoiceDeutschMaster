package com.voicedeutsch.master.presentation.theme

import androidx.compose.ui.graphics.Color

// ── Brand palette ─────────────────────────────────────────────────────────────
val Primary       = Color(0xFF1A73E8) // Google Blue — active, interactive
val PrimaryDark   = Color(0xFF1557B0)
val PrimaryLight  = Color(0xFF4A9EFF)

val Secondary     = Color(0xFF34A853) // Google Green — success, learned
val SecondaryDark = Color(0xFF1E7E34)
val SecondaryLight = Color(0xFF66BB6A)

val Tertiary      = Color(0xFF9C27B0) // Purple — progress / level indicators
val TertiaryLight = Color(0xFFCE93D8)

val Error         = Color(0xFFEA4335) // Google Red — error, correction
val ErrorDark     = Color(0xFFC62828)
val Warning       = Color(0xFFFBBC04) // Google Yellow — processing, caution

// ── Voice state colours ───────────────────────────────────────────────────────
/** Idle / no session — subtle neutral wave */
val WaveIdle      = Color(0xFF757575)
/** User is being recorded */
val WaveListening = Color(0xFF1A73E8) // Primary blue
/** Gemini is thinking */
val WaveProcessing = Color(0xFFFBBC04) // Warning yellow
/** AI is speaking back */
val WaveSpeaking  = Color(0xFF34A853) // Secondary green
/** Error state */
val WaveError     = Color(0xFFEA4335) // Error red

// ── Neutral / surface ─────────────────────────────────────────────────────────
val Background          = Color(0xFF0D1117) // Deep dark — voice-centric
val BackgroundElevated  = Color(0xFF161B22)
val Surface             = Color(0xFF1C2128)
val SurfaceVariant      = Color(0xFF22272E)
val Outline             = Color(0xFF30363D)
val OutlineVariant      = Color(0xFF21262D)

// ── Text ──────────────────────────────────────────────────────────────────────
val OnBackground        = Color(0xFFE6EDF3) // Primary text
val OnBackgroundMuted   = Color(0xFF8B949E) // Secondary text
val OnBackgroundSubtle  = Color(0xFF484F58) // Placeholder / hint

// ── Light theme fallback ──────────────────────────────────────────────────────
// The app is voice-first and defaults to dark, but Material 3 requires both.
val BackgroundLight     = Color(0xFFFAFAFA)
val SurfaceLight        = Color(0xFFFFFFFF)
val OnBackgroundLight   = Color(0xFF1C1B1F)
val OnBackgroundMutedLight = Color(0xFF49454F)
