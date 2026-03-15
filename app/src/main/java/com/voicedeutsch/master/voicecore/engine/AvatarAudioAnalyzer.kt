package com.voicedeutsch.master.voicecore.engine

import com.voicedeutsch.master.voicecore.audio.AudioOutputCapture
import com.voicedeutsch.master.voicecore.audio.SpectralFeatureExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Converts real audio output (via Visualizer) into rich [AvatarAudioData].
 *
 * Pipeline:
 *   AudioOutputCapture → AudioFrame (waveform + FFT)
 *     → SpectralFeatureExtractor → ProsodicFeatures
 *       → AvatarAudioAnalyzer → AvatarAudioData (smoothed, emotion-tagged)
 *         → AvatarBehaviorEngine → bone rotations + morph weights
 *
 * Key features over v1:
 *  - Real audio data from Visualizer (not synthetic)
 *  - Full spectral analysis: pitch, centroid, band energy
 *  - Smile-in-voice detection via spectral centroid shift
 *  - Emphasis detection via energy+pitch spikes
 *  - Question detection via rising terminal pitch
 *  - Multi-speed EMA smoothing with asymmetric attack/release
 *  - Emotion state machine with hysteresis and hold timers
 */
class AvatarAudioAnalyzer {

    private val _audioData = MutableStateFlow(AvatarAudioData())
    val audioData = _audioData.asStateFlow()

    private val featureExtractor = SpectralFeatureExtractor()

    companion object {
        // ── Speech detection ──────────────────────────────────────────────
        private const val SPEAKING_ONSET_THRESHOLD = 0.08f   // start speaking
        private const val SPEAKING_OFFSET_THRESHOLD = 0.04f  // stop speaking (hysteresis)

        // ── Emotion timing ────────────────────────────────────────────────
        private const val SILENCE_THINKING_MS = 1200L
        private const val HAPPY_HOLD_MS = 3000L
        private const val SMILE_TO_HAPPY_THRESHOLD = 0.55f   // smileScore to trigger HAPPY
        private const val SMILE_TO_HAPPY_SUSTAIN_MS = 800L   // must sustain smile this long

        // ── Smoothing alphas (asymmetric EMA) ─────────────────────────────
        private const val ATTACK_ALPHA = 0.35f    // fast attack for speech onset
        private const val RELEASE_ALPHA = 0.08f   // slow release for natural decay
        private const val PITCH_ALPHA = 0.20f
        private const val CENTROID_ALPHA = 0.15f
        private const val SMILE_ALPHA = 0.12f
        private const val EMPHASIS_ALPHA = 0.25f
        private const val VOWEL_ALPHA = 0.30f
        private const val BAND_ALPHA = 0.18f
    }

    // ── Smoothed output values ────────────────────────────────────────────

    private var smoothedAmplitude = 0f
    private var smoothedPitch = 0f
    private var smoothedPitchDelta = 0f
    private var smoothedCentroid = 0f
    private var smoothedSmile = 0f
    private var smoothedEmphasis = 0f
    private var smoothedVowel = 0f
    private var smoothedBandLow = 0f
    private var smoothedBandMid = 0f
    private var smoothedBandHigh = 0f
    private var smoothedFlux = 0f
    private var smoothedZcr = 0f

    // ── State tracking ────────────────────────────────────────────────────

    private var wasSpeaking = false
    private var silenceStartMs = 0L
    private var lastSpeakingMs = 0L
    private var smileAboveThresholdMs = 0L
    private var isSmileTriggered = false

    /**
     * Process one audio frame from the Visualizer.
     * Call this from the AudioOutputCapture frame flow (~20Hz).
     */
    fun onAudioFrame(frame: AudioOutputCapture.AudioFrame) {
        val now = System.currentTimeMillis()

        // ── Extract prosodic features ─────────────────────────────────────
        val features = featureExtractor.extract(frame)

        // ── Asymmetric smoothing ──────────────────────────────────────────
        val ampAlpha = if (features.rmsEnergy > smoothedAmplitude) ATTACK_ALPHA else RELEASE_ALPHA
        smoothedAmplitude = ema(smoothedAmplitude, features.rmsEnergy, ampAlpha)
        smoothedPitch = ema(smoothedPitch, features.pitchNorm, PITCH_ALPHA)
        smoothedPitchDelta = ema(smoothedPitchDelta, features.pitchDelta, PITCH_ALPHA)
        smoothedCentroid = ema(smoothedCentroid, features.spectralCentroid, CENTROID_ALPHA)
        smoothedSmile = ema(smoothedSmile, features.smileScore, SMILE_ALPHA)
        smoothedEmphasis = ema(smoothedEmphasis, features.emphasisScore, EMPHASIS_ALPHA)
        smoothedVowel = ema(smoothedVowel, features.vowelOpenness, VOWEL_ALPHA)
        smoothedBandLow = ema(smoothedBandLow, features.bandLow, BAND_ALPHA)
        smoothedBandMid = ema(smoothedBandMid, features.bandMid, BAND_ALPHA)
        smoothedBandHigh = ema(smoothedBandHigh, features.bandHigh, BAND_ALPHA)
        smoothedFlux = ema(smoothedFlux, features.spectralFlux, EMPHASIS_ALPHA)
        smoothedZcr = ema(smoothedZcr, features.zeroCrossingRate, BAND_ALPHA)

        // ── Speaking detection with hysteresis ────────────────────────────
        val threshold = if (wasSpeaking) SPEAKING_OFFSET_THRESHOLD else SPEAKING_ONSET_THRESHOLD
        val isSpeaking = smoothedAmplitude > threshold && features.isVoiced

        if (isSpeaking) {
            lastSpeakingMs = now
            silenceStartMs = 0L
        } else if (silenceStartMs == 0L) {
            silenceStartMs = now
        }

        wasSpeaking = isSpeaking

        // ── Smile → HAPPY trigger ─────────────────────────────────────────
        if (smoothedSmile > SMILE_TO_HAPPY_THRESHOLD && isSpeaking) {
            if (smileAboveThresholdMs == 0L) smileAboveThresholdMs = now
            if (now - smileAboveThresholdMs > SMILE_TO_HAPPY_SUSTAIN_MS) {
                isSmileTriggered = true
            }
        } else {
            smileAboveThresholdMs = 0L
        }

        // ── Emotion state machine ─────────────────────────────────────────
        val emotion = when {
            // Hold HAPPY from external trigger
            _audioData.value.emotion == EmotionState.HAPPY &&
                (now - lastSpeakingMs) < HAPPY_HOLD_MS -> EmotionState.HAPPY

            // Smile-in-voice triggered HAPPY
            isSmileTriggered && isSpeaking -> {
                EmotionState.HAPPY
            }

            isSpeaking -> EmotionState.SPEAKING

            silenceStartMs > 0 && (now - silenceStartMs) > SILENCE_THINKING_MS &&
                (now - lastSpeakingMs) < 8000L -> EmotionState.THINKING

            else -> EmotionState.NEUTRAL
        }

        // Reset smile trigger when emotion changes away from HAPPY
        if (emotion != EmotionState.HAPPY) {
            isSmileTriggered = false
        }

        // ── Emit enriched audio data ──────────────────────────────────────
        _audioData.update {
            AvatarAudioData(
                amplitude = smoothedAmplitude,
                isSpeaking = isSpeaking,
                emotion = emotion,
                pitch = smoothedPitch,
                pitchDelta = smoothedPitchDelta,
                pitchConfidence = features.pitchConfidence,
                spectralCentroid = smoothedCentroid,
                spectralFlux = smoothedFlux,
                bandEnergy = BandEnergy(
                    low = smoothedBandLow,
                    mid = smoothedBandMid,
                    high = smoothedBandHigh,
                ),
                zeroCrossingRate = smoothedZcr,
                emphasis = smoothedEmphasis,
                smileScore = smoothedSmile,
                vowelOpenness = smoothedVowel,
                isQuestion = features.isQuestion,
            )
        }
    }

    /**
     * Legacy method: process a single amplitude float.
     * Used as fallback when Visualizer is not available.
     * Provides basic functionality without spectral features.
     */
    fun onAmplitude(amplitude: Float) {
        val now = System.currentTimeMillis()
        val raw = amplitude.coerceIn(0f, 1f)

        val alpha = if (raw > smoothedAmplitude) ATTACK_ALPHA else RELEASE_ALPHA
        smoothedAmplitude = ema(smoothedAmplitude, raw, alpha)

        val threshold = if (wasSpeaking) SPEAKING_OFFSET_THRESHOLD else SPEAKING_ONSET_THRESHOLD
        val isSpeaking = smoothedAmplitude > threshold

        if (isSpeaking) {
            lastSpeakingMs = now
            silenceStartMs = 0L
        } else if (silenceStartMs == 0L) {
            silenceStartMs = now
        }
        wasSpeaking = isSpeaking

        val emotion = when {
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
                // Estimate vowelOpenness from amplitude alone (fallback)
                vowelOpenness = (smoothedAmplitude * 1.2f).coerceIn(0f, 1f),
            )
        }
    }

    fun triggerHappy() {
        lastSpeakingMs = System.currentTimeMillis()
        _audioData.update { it.copy(emotion = EmotionState.HAPPY) }
    }

    fun reset() {
        smoothedAmplitude = 0f
        smoothedPitch = 0f
        smoothedPitchDelta = 0f
        smoothedCentroid = 0f
        smoothedSmile = 0f
        smoothedEmphasis = 0f
        smoothedVowel = 0f
        smoothedBandLow = 0f
        smoothedBandMid = 0f
        smoothedBandHigh = 0f
        smoothedFlux = 0f
        smoothedZcr = 0f

        wasSpeaking = false
        silenceStartMs = 0L
        lastSpeakingMs = 0L
        smileAboveThresholdMs = 0L
        isSmileTriggered = false

        featureExtractor.reset()
        _audioData.value = AvatarAudioData()
    }

    private fun ema(current: Float, target: Float, alpha: Float): Float =
        current + (target - current) * alpha
}
