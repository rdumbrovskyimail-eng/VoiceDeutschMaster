package com.voicedeutsch.master.domain.model.user

import kotlinx.serialization.Serializable

/**
 * Voice interaction settings.
 *
 * Controls playback speed for both languages, the ratio of German to Russian
 * in Gemini responses, transcription display, waveform visibility, and audio quality.
 */
@Serializable
data class VoiceSettings(
    val voiceSpeed: Float = 1.0f,
    val germanVoiceSpeed: Float = 0.8f,
    val voiceLanguageMix: Float = 0.2f,
    val showTranscription: Boolean = true,
    val showWaveform: Boolean = true,
    val audioQuality: AudioQuality = AudioQuality.HIGH
)

/**
 * Audio quality presets affecting sample rate and bitrate.
 */
@Serializable
enum class AudioQuality { LOW, MEDIUM, HIGH }