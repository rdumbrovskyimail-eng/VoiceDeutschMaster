package com.voicedeutsch.master.voicecore.engine

/**
 * Gender determines which .glb model to load.
 */
enum class AvatarGender { MALE, FEMALE }

/**
 * Emotion state derived from audio pattern analysis.
 */
enum class EmotionState { NEUTRAL, SPEAKING, THINKING, HAPPY }

/**
 * Real-time audio data for avatar animation.
 *
 * @param amplitude  Voice loudness 0..1 (drives jaw/mouth morph targets)
 * @param isSpeaking True when active speech detected
 * @param emotion    Current emotional state derived from audio patterns
 */
data class AvatarAudioData(
    val amplitude: Float = 0f,
    val isSpeaking: Boolean = false,
    val emotion: EmotionState = EmotionState.NEUTRAL,
)