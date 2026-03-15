package com.voicedeutsch.master.voicecore.audio

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Real-time spectral feature extraction from Visualizer audio frames.
 *
 * Extracts prosodic features that drive avatar animation:
 *  - RMS energy (amplitude)
 *  - Pitch (F0) via autocorrelation
 *  - Spectral centroid (voice brightness → smile detection)
 *  - Band energy (low/mid/high → lip shape selection)
 *  - Zero crossing rate (voiced vs unvoiced → sibilant detection)
 *  - Spectral flux (change rate → emphasis detection)
 *  - Harmonic-to-noise ratio (voice quality)
 *
 * All features are normalized to 0..1 range for direct use in animation.
 *
 * Design: stateless per-frame extraction + stateful tracking over time
 * for features that need temporal context (pitch contour, smile baseline, etc.)
 */
class SpectralFeatureExtractor {

    /**
     * Complete set of features extracted from one audio frame.
     * All values normalized to 0..1 unless noted.
     */
    data class ProsodicFeatures(
        // ── Energy ────────────────────────────────────────────────────────
        /** RMS energy (overall loudness). */
        val rmsEnergy: Float = 0f,
        /** Peak amplitude in frame. */
        val peakAmplitude: Float = 0f,

        // ── Pitch ─────────────────────────────────────────────────────────
        /** Fundamental frequency (F0) in Hz. 0 = unvoiced/silence. */
        val pitchHz: Float = 0f,
        /** Pitch normalized: 0 = 80Hz (low male), 1 = 500Hz (high female). */
        val pitchNorm: Float = 0f,
        /** Pitch change since last frame: >0 rising, <0 falling. */
        val pitchDelta: Float = 0f,
        /** Confidence of pitch detection 0..1. */
        val pitchConfidence: Float = 0f,

        // ── Spectral shape ────────────────────────────────────────────────
        /** Spectral centroid (brightness). Higher = brighter voice. */
        val spectralCentroid: Float = 0f,
        /** Spectral spread (how concentrated energy is). */
        val spectralSpread: Float = 0f,
        /** Spectral flux (frame-to-frame change). High = transients/emphasis. */
        val spectralFlux: Float = 0f,

        // ── Band energy ───────────────────────────────────────────────────
        /** Energy in 80-300 Hz (chest resonance, fundamental). */
        val bandLow: Float = 0f,
        /** Energy in 300-2000 Hz (main formants F1-F2, vowel character). */
        val bandMid: Float = 0f,
        /** Energy in 2000-8000 Hz (sibilants, fricatives, brightness). */
        val bandHigh: Float = 0f,
        /** Ratio high/low — elevated when smiling. */
        val highLowRatio: Float = 0f,

        // ── Temporal ──────────────────────────────────────────────────────
        /** Zero crossing rate (high = unvoiced/sibilant sounds). */
        val zeroCrossingRate: Float = 0f,
        /** True if this frame is likely voiced speech. */
        val isVoiced: Boolean = false,

        // ── Derived emotional features ────────────────────────────────────
        /** Smile-in-voice score 0..1 (spectral centroid shift up from baseline). */
        val smileScore: Float = 0f,
        /** Emphasis/stress score 0..1 (energy + pitch spikes). */
        val emphasisScore: Float = 0f,
        /** Vowel openness estimate 0..1 (low F1 + energy → open mouth). */
        val vowelOpenness: Float = 0f,
        /** Question intonation detected (terminal pitch rise). */
        val isQuestion: Boolean = false,
    )

    // ── State for temporal tracking ────────────────────────────────────────

    private var prevFft: FloatArray? = null
    private var prevPitchHz = 0f
    private var prevRms = 0f

    // Baseline tracking for smile detection
    private var centroidBaseline = 0f
    private var centroidBaselineCount = 0
    private val BASELINE_FRAMES = 30  // ~1.5 sec at 20Hz to establish baseline

    // Pitch contour buffer for question detection
    private val pitchBuffer = FloatArray(20)  // ~1 sec at 20Hz
    private var pitchBufferIdx = 0
    private var pitchBufferFilled = false

    // Smoothed values (EMA)
    private var smoothedCentroid = 0f
    private var smoothedRms = 0f
    private var smoothedPitch = 0f

    companion object {
        // Pitch detection range
        private const val MIN_PITCH_HZ = 75f   // low male voice
        private const val MAX_PITCH_HZ = 500f   // high female voice
        private const val PITCH_RANGE = MAX_PITCH_HZ - MIN_PITCH_HZ

        // Frequency band boundaries (Hz)
        private const val BAND_LOW_MIN = 80f
        private const val BAND_LOW_MAX = 300f
        private const val BAND_MID_MAX = 2000f
        private const val BAND_HIGH_MAX = 8000f

        // Thresholds
        private const val VOICE_RMS_THRESHOLD = 0.03f
        private const val VOICE_ZCR_MAX = 0.35f  // voiced speech has lower ZCR

        // Smile detection: spectral centroid must rise >15% above baseline
        private const val SMILE_CENTROID_SHIFT = 0.15f
        private const val SMILE_SENSITIVITY = 3f  // amplification factor

        // EMA smoothing alphas
        private const val EMA_FAST = 0.3f
        private const val EMA_SLOW = 0.05f
        private const val EMA_CENTROID = 0.15f
    }

    /**
     * Extract all prosodic features from one audio frame.
     *
     * @param frame  Raw audio frame from AudioOutputCapture
     * @return Complete set of normalized prosodic features
     */
    fun extract(frame: AudioOutputCapture.AudioFrame): ProsodicFeatures {
        val waveform = frame.waveform
        val fft = frame.fft
        val sampleRate = frame.sampleRate

        // ── RMS energy ────────────────────────────────────────────────────
        val rms = computeRms(waveform)
        val peak = computePeak(waveform)

        // ── Zero crossing rate ────────────────────────────────────────────
        val zcr = computeZeroCrossingRate(waveform)

        // ── Pitch detection (autocorrelation) ─────────────────────────────
        val (pitchHz, pitchConf) = detectPitch(waveform, sampleRate)

        // ── Spectral features from FFT ────────────────────────────────────
        val centroid = computeSpectralCentroid(fft, sampleRate)
        val spread = computeSpectralSpread(fft, centroid, sampleRate)
        val flux = computeSpectralFlux(fft)

        // ── Band energy ───────────────────────────────────────────────────
        val (bLow, bMid, bHigh) = computeBandEnergy(fft, sampleRate)

        // ── Voice detection ───────────────────────────────────────────────
        val isVoiced = rms > VOICE_RMS_THRESHOLD && zcr < VOICE_ZCR_MAX && pitchConf > 0.3f

        // ── Temporal smoothing ────────────────────────────────────────────
        smoothedRms = ema(smoothedRms, rms, EMA_FAST)
        smoothedCentroid = ema(smoothedCentroid, centroid, EMA_CENTROID)
        smoothedPitch = if (pitchHz > 0f) ema(smoothedPitch, pitchHz, EMA_FAST) else smoothedPitch * 0.95f

        // ── Pitch delta ───────────────────────────────────────────────────
        val pitchDelta = if (pitchHz > 0f && prevPitchHz > 0f) {
            ((pitchHz - prevPitchHz) / PITCH_RANGE).coerceIn(-1f, 1f)
        } else 0f

        // ── Pitch contour for question detection ──────────────────────────
        if (pitchHz > 0f) {
            pitchBuffer[pitchBufferIdx] = pitchHz
            pitchBufferIdx = (pitchBufferIdx + 1) % pitchBuffer.size
            if (pitchBufferIdx == 0) pitchBufferFilled = true
        }
        val isQuestion = detectQuestion()

        // ── Smile detection ───────────────────────────────────────────────
        val smileScore = detectSmile(centroid, isVoiced)

        // ── Emphasis detection ────────────────────────────────────────────
        val rmsSpike = if (prevRms > 0.01f) (rms / prevRms - 1f).coerceIn(0f, 2f) else 0f
        val pitchSpike = (pitchDelta.coerceAtLeast(0f) * 2f).coerceAtMost(1f)
        val emphasisScore = ((rmsSpike * 0.6f + pitchSpike * 0.4f + flux * 0.3f) / 1.3f).coerceIn(0f, 1f)

        // ── Vowel openness (low-band energy + overall energy) ─────────────
        val vowelOpenness = if (isVoiced) {
            ((bLow * 0.4f + bMid * 0.4f + rms * 0.2f) * 1.5f).coerceIn(0f, 1f)
        } else 0f

        // ── Normalize pitch to 0..1 ───────────────────────────────────────
        val pitchNorm = if (pitchHz > 0f) {
            ((pitchHz - MIN_PITCH_HZ) / PITCH_RANGE).coerceIn(0f, 1f)
        } else 0f

        // ── High/low ratio ────────────────────────────────────────────────
        val highLowRatio = if (bLow > 0.01f) (bHigh / bLow).coerceIn(0f, 3f) / 3f else 0f

        // ── Update state ──────────────────────────────────────────────────
        prevFft = fft.copyOf()
        prevPitchHz = pitchHz
        prevRms = rms

        return ProsodicFeatures(
            rmsEnergy = rms,
            peakAmplitude = peak,
            pitchHz = pitchHz,
            pitchNorm = pitchNorm,
            pitchDelta = pitchDelta,
            pitchConfidence = pitchConf,
            spectralCentroid = smoothedCentroid,
            spectralSpread = spread,
            spectralFlux = flux,
            bandLow = bLow,
            bandMid = bMid,
            bandHigh = bHigh,
            highLowRatio = highLowRatio,
            zeroCrossingRate = zcr,
            isVoiced = isVoiced,
            smileScore = smileScore,
            emphasisScore = emphasisScore,
            vowelOpenness = vowelOpenness,
            isQuestion = isQuestion,
        )
    }

    // ── Core DSP functions ────────────────────────────────────────────────────

    private fun computeRms(waveform: FloatArray): Float {
        var sum = 0f
        for (s in waveform) sum += s * s
        return sqrt(sum / waveform.size).coerceIn(0f, 1f)
    }

    private fun computePeak(waveform: FloatArray): Float {
        var max = 0f
        for (s in waveform) {
            val a = abs(s)
            if (a > max) max = a
        }
        return max.coerceIn(0f, 1f)
    }

    private fun computeZeroCrossingRate(waveform: FloatArray): Float {
        var crossings = 0
        for (i in 1 until waveform.size) {
            if ((waveform[i] >= 0f && waveform[i - 1] < 0f) ||
                (waveform[i] < 0f && waveform[i - 1] >= 0f)
            ) {
                crossings++
            }
        }
        return crossings.toFloat() / waveform.size
    }

    /**
     * Pitch detection via autocorrelation.
     *
     * For a periodic signal, autocorrelation peaks at the period.
     * We search for the highest peak in the lag range corresponding
     * to MIN_PITCH_HZ..MAX_PITCH_HZ.
     *
     * @return Pair(pitchHz, confidence). pitchHz=0 if unvoiced.
     */
    private fun detectPitch(waveform: FloatArray, sampleRate: Int): Pair<Float, Float> {
        val n = waveform.size
        if (n < 64) return Pair(0f, 0f)

        // Lag range for pitch detection
        val minLag = (sampleRate / MAX_PITCH_HZ).toInt().coerceAtLeast(2)
        val maxLag = (sampleRate / MIN_PITCH_HZ).toInt().coerceAtMost(n / 2)

        if (minLag >= maxLag) return Pair(0f, 0f)

        // Normalized autocorrelation
        // R(lag) = sum(x[i] * x[i+lag]) / sqrt(sum(x[i]²) * sum(x[i+lag]²))

        // Energy of the signal
        var energy = 0f
        for (s in waveform) energy += s * s
        if (energy < 0.0001f) return Pair(0f, 0f)  // silence

        var bestLag = 0
        var bestCorr = -1f

        for (lag in minLag..maxLag) {
            var corr = 0f
            var energyLagged = 0f
            val count = n - lag

            for (i in 0 until count) {
                corr += waveform[i] * waveform[i + lag]
                energyLagged += waveform[i + lag] * waveform[i + lag]
            }

            // Normalize
            val denom = sqrt(energy * energyLagged)
            if (denom > 0.0001f) {
                corr /= denom
            }

            if (corr > bestCorr) {
                bestCorr = corr
                bestLag = lag
            }
        }

        // Confidence threshold
        if (bestCorr < 0.25f || bestLag == 0) return Pair(0f, bestCorr.coerceAtLeast(0f))

        // Parabolic interpolation for sub-sample accuracy
        val pitchHz = if (bestLag > minLag && bestLag < maxLag) {
            // Sample autocorrelation at bestLag-1, bestLag, bestLag+1
            val rMinus = autocorrAt(waveform, bestLag - 1)
            val rCenter = bestCorr
            val rPlus = autocorrAt(waveform, bestLag + 1)
            val shift = (rMinus - rPlus) / (2f * (rMinus - 2f * rCenter + rPlus))
            sampleRate.toFloat() / (bestLag + shift)
        } else {
            sampleRate.toFloat() / bestLag
        }

        return if (pitchHz in MIN_PITCH_HZ..MAX_PITCH_HZ) {
            Pair(pitchHz, bestCorr.coerceIn(0f, 1f))
        } else {
            Pair(0f, 0f)
        }
    }

    private fun autocorrAt(waveform: FloatArray, lag: Int): Float {
        if (lag < 0 || lag >= waveform.size / 2) return 0f
        var corr = 0f
        val count = waveform.size - lag
        for (i in 0 until count) {
            corr += waveform[i] * waveform[i + lag]
        }
        return corr / count
    }

    /**
     * Spectral centroid = weighted mean of frequencies.
     * Higher centroid = brighter voice.
     * Smiling raises centroid by ~10-15% due to vocal tract shortening.
     */
    private fun computeSpectralCentroid(fft: FloatArray, sampleRate: Int): Float {
        val binWidth = sampleRate.toFloat() / (fft.size * 2)  // Hz per bin
        var weightedSum = 0f
        var totalMagnitude = 0f

        for (i in fft.indices) {
            val freq = i * binWidth
            val mag = fft[i]
            weightedSum += freq * mag
            totalMagnitude += mag
        }

        if (totalMagnitude < 0.001f) return 0f

        val centroidHz = weightedSum / totalMagnitude
        // Normalize: speech centroid typically 500-3000 Hz
        return (centroidHz / 4000f).coerceIn(0f, 1f)
    }

    /**
     * Spectral spread — standard deviation of spectrum around centroid.
     */
    private fun computeSpectralSpread(fft: FloatArray, centroid: Float, sampleRate: Int): Float {
        val binWidth = sampleRate.toFloat() / (fft.size * 2)
        val centroidHz = centroid * 4000f
        var weightedVariance = 0f
        var totalMag = 0f

        for (i in fft.indices) {
            val freq = i * binWidth
            val mag = fft[i]
            val diff = freq - centroidHz
            weightedVariance += diff * diff * mag
            totalMag += mag
        }

        if (totalMag < 0.001f) return 0f
        return (sqrt(weightedVariance / totalMag) / 3000f).coerceIn(0f, 1f)
    }

    /**
     * Spectral flux — frame-to-frame change in spectrum.
     * High flux = transient/plosive/emphasis.
     */
    private fun computeSpectralFlux(fft: FloatArray): Float {
        val prev = prevFft ?: return 0f
        val len = minOf(fft.size, prev.size)
        var flux = 0f

        for (i in 0 until len) {
            val diff = fft[i] - prev[i]
            // Half-wave rectification: only count increases (onset detection)
            if (diff > 0) flux += diff * diff
        }

        return (sqrt(flux / len) * 5f).coerceIn(0f, 1f)
    }

    /**
     * Energy in frequency bands.
     * Returns Triple(low, mid, high) all normalized 0..1.
     */
    private fun computeBandEnergy(fft: FloatArray, sampleRate: Int): Triple<Float, Float, Float> {
        val binWidth = sampleRate.toFloat() / (fft.size * 2)

        var low = 0f
        var mid = 0f
        var high = 0f
        var lowCount = 0
        var midCount = 0
        var highCount = 0

        for (i in fft.indices) {
            val freq = i * binWidth
            val mag = fft[i] * fft[i]  // power

            when {
                freq in BAND_LOW_MIN..BAND_LOW_MAX -> { low += mag; lowCount++ }
                freq in BAND_LOW_MAX..BAND_MID_MAX -> { mid += mag; midCount++ }
                freq in BAND_MID_MAX..BAND_HIGH_MAX -> { high += mag; highCount++ }
            }
        }

        // Average power per band, then sqrt for amplitude
        val bLow = if (lowCount > 0) sqrt(low / lowCount) else 0f
        val bMid = if (midCount > 0) sqrt(mid / midCount) else 0f
        val bHigh = if (highCount > 0) sqrt(high / highCount) else 0f

        // Normalize (empirical scaling for speech signals through 8-bit Visualizer)
        return Triple(
            (bLow * 4f).coerceIn(0f, 1f),
            (bMid * 4f).coerceIn(0f, 1f),
            (bHigh * 6f).coerceIn(0f, 1f),  // high band is typically quieter
        )
    }

    // ── Derived features ──────────────────────────────────────────────────────

    /**
     * Detects smile-in-voice.
     *
     * When someone smiles while speaking, the vocal tract shortens slightly
     * because lip spreading pulls the tract walls forward. This shifts
     * formants (F2, F3) upward, raising the spectral centroid by ~10-15%.
     *
     * We compare current centroid to a running baseline established during
     * the first ~1.5 seconds of speech.
     */
    private fun detectSmile(centroid: Float, isVoiced: Boolean): Float {
        if (!isVoiced || centroid < 0.01f) return 0f

        // Build baseline during initial speech
        if (centroidBaselineCount < BASELINE_FRAMES) {
            centroidBaseline = ema(centroidBaseline, centroid, 0.1f)
            centroidBaselineCount++
            return 0f
        }

        // Slowly adapt baseline (accounts for long-term drift, but much slower than smile)
        centroidBaseline = ema(centroidBaseline, centroid, 0.005f)

        if (centroidBaseline < 0.01f) return 0f

        // Relative shift above baseline
        val shift = (smoothedCentroid - centroidBaseline) / centroidBaseline

        // Score: 0 at baseline, 1 at +15% shift
        return ((shift - 0.03f) * SMILE_SENSITIVITY / SMILE_CENTROID_SHIFT)
            .coerceIn(0f, 1f)
    }

    /**
     * Detects question intonation (rising terminal pitch).
     *
     * Looks at the last ~0.5s of pitch data and checks if it trends upward.
     */
    private fun detectQuestion(): Boolean {
        if (!pitchBufferFilled && pitchBufferIdx < 10) return false

        // Look at last 10 frames (~0.5s)
        val lookback = 10
        val start = if (pitchBufferFilled) {
            (pitchBufferIdx - lookback + pitchBuffer.size) % pitchBuffer.size
        } else {
            (pitchBufferIdx - lookback).coerceAtLeast(0)
        }

        var validCount = 0
        var risingCount = 0
        var prevPitch = 0f

        for (i in 0 until lookback) {
            val idx = (start + i) % pitchBuffer.size
            val p = pitchBuffer[idx]
            if (p > 0f) {
                if (prevPitch > 0f) {
                    validCount++
                    if (p > prevPitch * 1.02f) risingCount++  // 2% rise per frame
                }
                prevPitch = p
            }
        }

        // Question: >60% of the last frames show rising pitch
        return validCount >= 4 && risingCount.toFloat() / validCount > 0.6f
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private fun ema(current: Float, target: Float, alpha: Float): Float =
        current + (target - current) * alpha

    /**
     * Reset all state (call between sessions).
     */
    fun reset() {
        prevFft = null
        prevPitchHz = 0f
        prevRms = 0f
        centroidBaseline = 0f
        centroidBaselineCount = 0
        smoothedCentroid = 0f
        smoothedRms = 0f
        smoothedPitch = 0f
        pitchBuffer.fill(0f)
        pitchBufferIdx = 0
        pitchBufferFilled = false
    }
}
