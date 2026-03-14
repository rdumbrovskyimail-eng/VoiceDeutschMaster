package com.voicedeutsch.master.voicecore.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Converts raw amplitude flow into [AvatarAudioData] with emotion detection.
 *
 * Emotion detection uses a sliding window:
 * - No sound for >1.5s → THINKING
 * - Active speech → SPEAKING
 * - After positive Gemini response → HAPPY (set externally)
 * - Default → NEUTRAL
 */
class AvatarAudioAnalyzer {

    private val _audioData = MutableStateFlow(AvatarAudioData())
    val audioData = _audioData.asStateFlow()

    // Sliding window for emotion detection
    private val amplitudeHistory = ArrayDeque<Float>(15) // ~500ms at 30fps
    private var silenceStartMs: Long = 0L
    private var lastSpeakingMs: Long = 0L

    companion object {
        private const val SPEAKING_THRESHOLD = 0.08f
        private const val SILENCE_THINKING_MS = 1500L
        private const val WINDOW_SIZE = 15
    }

    /**
     * Call this on each amplitude frame (~30fps).
     */
    fun onAmplitude(amplitude: Float) {
        val now = System.currentTimeMillis()
        val clamped = amplitude.coerceIn(0f, 1f)

        // Update sliding window
        if (amplitudeHistory.size >= WINDOW_SIZE) amplitudeHistory.removeFirst()
        amplitudeHistory.addLast(clamped)

        val isSpeaking = clamped > SPEAKING_THRESHOLD
        if (isSpeaking) {
            lastSpeakingMs = now
            silenceStartMs = 0L
        } else if (silenceStartMs == 0L) {
            silenceStartMs = now
        }

        val emotion = when {
            _audioData.value.emotion == EmotionState.HAPPY &&
                (now - lastSpeakingMs) < 3000L -> EmotionState.HAPPY
            isSpeaking -> EmotionState.SPEAKING
            silenceStartMs > 0 && (now - silenceStartMs) > SILENCE_THINKING_MS ->
                EmotionState.THINKING
            else -> EmotionState.NEUTRAL
        }

        _audioData.update {
            AvatarAudioData(
                amplitude = clamped,
                isSpeaking = isSpeaking,
                emotion = emotion,
            )
        }
    }

    /**
     * Set HAPPY emotion externally (e.g., after positive Gemini response).
     */
    fun triggerHappy() {
        _audioData.update { it.copy(emotion = EmotionState.HAPPY) }
    }

    fun reset() {
        amplitudeHistory.clear()
        silenceStartMs = 0L
        lastSpeakingMs = 0L
        _audioData.value = AvatarAudioData()
    }
}