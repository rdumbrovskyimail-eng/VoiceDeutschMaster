package com.voicedeutsch.master.presentation.theme

import androidx.compose.ui.graphics.Color

// Gemini-style Palette
val Primary            = Color(0xFF1A73E8) // Google Blue
val PrimaryContainer   = Color(0xFFD3E3FD)
val Secondary          = Color(0xFF9333EA) // Gemini Purple
val SecondaryContainer = Color(0xFFF3E8FF)
val Tertiary           = Color(0xFF10B981) // Success Green
val Error              = Color(0xFFEA4335) // Google Red
val Background         = Color(0xFFF8FAFC) // Slate 50 - airy off-white
val Surface            = Color(0xFFFFFFFF)
val SurfaceVariant     = Color(0xFFF1F5F9) // Slate 100
val Outline            = Color(0xFFE2E8F0)
val OnBackground       = Color(0xFF1E293B) // Slate 800
val OnSurfaceVariant   = Color(0xFF64748B) // Slate 500

// ── Avatar ────────────────────────────────────────────────────────────────────
val AvatarSkin = Color(0xFFFFEDD5)
val AvatarHair = Color(0xFF334155)

// ── Voice state colours ───────────────────────────────────────────────────────
/** Idle / no session — subtle neutral wave */
val WaveIdle       = Color(0xFF94A3B8) // Slate 400
/** User is being recorded */
val WaveListening  = Color(0xFF1A73E8) // Primary blue
/** Gemini is thinking */
val WaveProcessing = Color(0xFFF59E0B) // Amber
/** AI is speaking back */
val WaveSpeaking   = Color(0xFF10B981) // Tertiary green
/** Error state */
val WaveError      = Color(0xFFEA4335) // Error red

// ── Retained for backward compatibility ──────────────────────────────────────
val PrimaryDark        = Color(0xFF1557B0)
val PrimaryLight       = Color(0xFF4A9EFF)
val Warning            = Color(0xFFFBBC04) // Google Yellow — processing, caution
val OnBackgroundMuted  = Color(0xFF64748B) // Slate 500
val OnBackgroundSubtle = Color(0xFF94A3B8) // Slate 400
