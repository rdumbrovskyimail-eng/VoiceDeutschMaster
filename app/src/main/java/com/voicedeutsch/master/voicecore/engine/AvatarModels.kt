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
 * Energy distribution across frequency bands.
 *
 * @param low   80-300 Hz   — chest resonance, fundamental frequency
 * @param mid   300-2000 Hz — main formants (F1-F2), vowel character
 * @param high  2000-8000 Hz — sibilants, fricatives, voice brightness
 */
data class BandEnergy(
    val low: Float = 0f,
    val mid: Float = 0f,
    val high: Float = 0f,
)

/**
 * Real-time audio data for avatar animation.
 *
 * Contains both raw prosodic features from spectral analysis and
 * derived emotional/behavioral signals for the animation engine.
 *
 * All float values normalized to 0..1 unless documented otherwise.
 *
 * @param amplitude       Voice loudness 0..1 (RMS energy, drives jaw/mouth base)
 * @param isSpeaking      True when active voiced speech detected
 * @param emotion         Current emotional state derived from audio patterns
 * @param pitch           Fundamental frequency normalized 0..1 (0=80Hz, 1=500Hz)
 * @param pitchDelta      Intonation direction: >0 rising, <0 falling (-1..1)
 * @param spectralCentroid Voice brightness 0..1 (higher when smiling)
 * @param emphasis        Stress/emphasis score 0..1 (energy+pitch spikes)
 * @param smileScore      Smile-in-voice detection 0..1 (spectral centroid shift)
 * @param vowelOpenness   Mouth opening estimate 0..1 (low-band energy + RMS)
 * @param isQuestion      Rising terminal intonation detected
 * @param bandEnergy      Energy per frequency band for lip shape selection
 * @param zeroCrossingRate High = unvoiced/sibilant, low = voiced (0..1)
 * @param spectralFlux    Frame-to-frame spectral change (transients/plosives)
 * @param pitchConfidence Reliability of pitch detection 0..1
 */
data class AvatarAudioData(
    // ── Core (backward compatible) ─────────────────────────────────────
    val amplitude: Float = 0f,
    val isSpeaking: Boolean = false,
    val emotion: EmotionState = EmotionState.NEUTRAL,

    // ── Pitch ──────────────────────────────────────────────────────────
    val pitch: Float = 0f,
    val pitchDelta: Float = 0f,
    val pitchConfidence: Float = 0f,

    // ── Spectral ───────────────────────────────────────────────────────
    val spectralCentroid: Float = 0f,
    val spectralFlux: Float = 0f,
    val bandEnergy: BandEnergy = BandEnergy(),
    val zeroCrossingRate: Float = 0f,

    // ── Derived emotional/behavioral ───────────────────────────────────
    val emphasis: Float = 0f,
    val smileScore: Float = 0f,
    val vowelOpenness: Float = 0f,
    val isQuestion: Boolean = false,
)
