package com.voicedeutsch.master.voicecore.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Converts raw amplitude flow into [AvatarAudioData] with smoothing and emotion detection.
 *
 * Key improvements over v1:
 *  - Multi-speed EMA: fast attack (speech onset), slow release (natural decay)
 *  - Speech segment tracking: detects start/end of speech bursts
 *  - Energy accumulator: overall energy level for gesture intensity
 *  - Smoothed amplitude output: removes jarring jumps
 *  - Better silence threshold with hysteresis (prevents flickering)
 */
class AvatarAudioAnalyzer {

    private val _audioData = MutableStateFlow(AvatarAudioData())
    val audioData = _audioData.asStateFlow()

    companion object {
        private const val SPEAKING_ONSET_THRESHOLD = 0.10f   // start speaking
        private const val SPEAKING_OFFSET_THRESHOLD = 0.06f  // stop speaking (hysteresis)
        private const val SILENCE_THINKING_MS = 1500L
        private const val HAPPY_HOLD_MS = 3000L

        // EMA alphas
        private const val ATTACK_ALPHA = 0.35f   // fast attack for speech onset
        private const val RELEASE_ALPHA = 0.08f   // slow release for natural decay
        private const val ENERGY_ALPHA = 0.02f     // very slow for overall energy level
    }

    // Smoothed values
    private var smoothedAmplitude = 0f
    private var energyLevel = 0f          // slow-moving average (0..1)
    private var peakAmplitude = 0f        // recent peak for emphasis detection

    // State tracking
    private var wasSpeaking = false
    private var silenceStartMs = 0L
    private var lastSpeakingMs = 0L
    private var speechSegmentStartMs = 0L // when current speech burst started
    private var speechSegmentCount = 0    // total speech segments in session

    /**
     * Process one amplitude frame. Call at ~30fps.
     */
    fun onAmplitude(amplitude: Float) {
        val now = System.currentTimeMillis()
        val raw = amplitude.coerceIn(0f, 1f)

        // Asymmetric EMA: fast attack, slow release
        val alpha = if (raw > smoothedAmplitude) ATTACK_ALPHA else RELEASE_ALPHA
        smoothedAmplitude += (raw - smoothedAmplitude) * alpha

        // Energy level (very slow, represents overall activity)
        energyLevel += (raw - energyLevel) * ENERGY_ALPHA

        // Peak tracking (decays slowly)
        if (raw > peakAmplitude) peakAmplitude = raw
        else peakAmplitude *= 0.995f

        // Speaking detection with hysteresis to prevent flickering
        val threshold = if (wasSpeaking) SPEAKING_OFFSET_THRESHOLD else SPEAKING_ONSET_THRESHOLD
        val isSpeaking = smoothedAmplitude > threshold

        // Track speech segments
        if (isSpeaking && !wasSpeaking) {
            speechSegmentStartMs = now
            speechSegmentCount++
        }

        if (isSpeaking) {
            lastSpeakingMs = now
            silenceStartMs = 0L
        } else if (silenceStartMs == 0L) {
            silenceStartMs = now
        }

        wasSpeaking = isSpeaking

        // Determine emotion
        val emotion = when {
            // Hold HAPPY for a duration after being set externally
            _audioData.value.emotion == EmotionState.HAPPY &&
                (now - lastSpeakingMs) < HAPPY_HOLD_MS -> EmotionState.HAPPY

            isSpeaking -> EmotionState.SPEAKING

            silenceStartMs > 0 && (now - silenceStartMs) > SILENCE_THINKING_MS &&
                (now - lastSpeakingMs) < 8000L -> EmotionState.THINKING

            else -> EmotionState.NEUTRAL
        }

        _audioData.update {
            AvatarAudioData(
                amplitude = smoothedAmplitude,
                isSpeaking = isSpeaking,
                emotion = emotion,
            )
        }
    }

    fun triggerHappy() {
        lastSpeakingMs = System.currentTimeMillis()
        _audioData.update { it.copy(emotion = EmotionState.HAPPY) }
    }

    fun reset() {
        smoothedAmplitude = 0f
        energyLevel = 0f
        peakAmplitude = 0f
        wasSpeaking = false
        silenceStartMs = 0L
        lastSpeakingMs = 0L
        speechSegmentStartMs = 0L
        speechSegmentCount = 0
        _audioData.value = AvatarAudioData()
    }
}