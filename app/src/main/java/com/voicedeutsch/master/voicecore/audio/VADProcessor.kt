package com.voicedeutsch.master.voicecore.audio

import com.voicedeutsch.master.util.AudioUtils
import com.voicedeutsch.master.util.Constants
import java.util.LinkedList

/**
 * Voice Activity Detection (VAD) processor.
 *
 * Architecture lines 615-622 (VAD parameters):
 *   - Speech-start threshold : adaptive (based on noise floor estimate)
 *   - Speech-end silence     : [SILENCE_END_FRAMES] * frame_duration ≈ 1 500 ms
 *   - Minimum speech length  : [MIN_SPEECH_FRAMES] * frame_duration ≈ 300 ms
 *
 * State machine:
 * ```
 *  SILENCE ──(energy > threshold)──→ SPEECH
 *  SPEECH  ──(energy < threshold, count >= SILENCE_END_FRAMES)──→ SPEECH_END
 *  SPEECH_END ──(always next frame)──→ SILENCE
 * ```
 *
 * Adaptive noise floor:
 *   The [noiseFloor] is continuously updated from silent frames using an EMA
 *   (exponential moving average). This prevents false triggers in noisy
 *   environments and adapts when background noise changes.
 */
class VADProcessor {

    // ── Public state ──────────────────────────────────────────────────────────

    enum class VadState {
        /** No voice detected — background noise / silence. */
        SILENCE,
        /** Voice activity detected — user is speaking. */
        SPEECH,
        /** Silence after speech; the utterance has just ended. */
        SPEECH_END,
    }

    @Volatile
    var currentState: VadState = VadState.SILENCE
        private set

    /** Estimated noise floor RMS (0.0–1.0, normalized to Short.MAX_VALUE). */
    @Volatile
    var noiseFloor: Float = INITIAL_NOISE_FLOOR
        private set

    // ── Internal counters ─────────────────────────────────────────────────────

    /** Consecutive silent frames during speech — resets on every speech frame. */
    private var silentFrameCount   = 0

    /** Consecutive speech frames — used to discard spurious blips. */
    private var speechFrameCount   = 0

    /** Whether we are currently inside a speech segment. */
    private var inSpeech = false

    /** Recent RMS history for adaptive threshold (ring buffer). */
    private val recentRmsHistory = LinkedList<Float>()

    // ── Processing ────────────────────────────────────────────────────────────

    /**
     * Processes a single PCM frame and returns the new [VadState].
     *
     * @param samples  16-bit PCM frame ([AudioRecorder.FRAME_SIZE_SAMPLES] samples).
     * @return         The VAD state after processing this frame.
     */
    fun process(samples: ShortArray): VadState {
        val rms = AudioUtils.calculateRMS(samples) / Short.MAX_VALUE.toFloat()

        // Maintain recent RMS history
        recentRmsHistory.add(rms)
        if (recentRmsHistory.size > HISTORY_LENGTH) recentRmsHistory.poll()

        val threshold = computeAdaptiveThreshold()

        return when {
            rms > threshold -> onEnergyAboveThreshold(rms)
            else            -> onEnergyBelowThreshold(rms)
        }.also { currentState = it }
    }

    /** Resets internal state (call between utterances or after a session restart). */
    fun reset() {
        silentFrameCount = 0
        speechFrameCount = 0
        inSpeech         = false
        currentState     = VadState.SILENCE
        recentRmsHistory.clear()
        noiseFloor       = INITIAL_NOISE_FLOOR
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun onEnergyAboveThreshold(rms: Float): VadState {
        silentFrameCount = 0
        speechFrameCount++

        if (!inSpeech && speechFrameCount >= MIN_SPEECH_FRAMES) {
            inSpeech = true
        }
        return if (inSpeech) VadState.SPEECH else VadState.SILENCE
    }

    private fun onEnergyBelowThreshold(rms: Float): VadState {
        if (!inSpeech) {
            speechFrameCount = 0
            // Update noise floor using EMA — only on clearly quiet frames
            noiseFloor = NOISE_EMA_ALPHA * rms + (1f - NOISE_EMA_ALPHA) * noiseFloor
            return VadState.SILENCE
        }

        silentFrameCount++

        return if (silentFrameCount >= SILENCE_END_FRAMES) {
            // Long enough silence after speech → end of utterance
            inSpeech         = false
            silentFrameCount = 0
            speechFrameCount = 0
            VadState.SPEECH_END
        } else {
            // Short pause inside speech — still considered speaking
            VadState.SPEECH
        }
    }

    /**
     * Computes an adaptive speech-start threshold.
     *
     * Base threshold = [Constants.VAD_SPEECH_START_THRESHOLD].
     * Adaptive factor = noiseFloor * [NOISE_MARGIN_FACTOR].
     *
     * The larger of the two is used, ensuring we always require speech
     * to be meaningfully louder than the noise floor.
     */
    private fun computeAdaptiveThreshold(): Float {
        val adaptiveThreshold = noiseFloor * NOISE_MARGIN_FACTOR
        return maxOf(Constants.VAD_SPEECH_START_THRESHOLD, adaptiveThreshold)
    }

    // ── Timing utilities ──────────────────────────────────────────────────────

    /**
     * Converts a frame count to milliseconds.
     * Frame duration = [AudioRecorder.FRAME_SIZE_SAMPLES] / [AudioRecorder.SAMPLE_RATE].
     */
    fun framesToMs(frames: Int): Long =
        (frames.toLong() * AudioRecorder.FRAME_SIZE_SAMPLES * 1000L) / AudioRecorder.SAMPLE_RATE

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        /** Initial (cold-start) noise floor estimate. */
        const val INITIAL_NOISE_FLOOR = 0.01f

        /**
         * Number of consecutive silent frames required to end an utterance.
         * At 10 ms / frame → 150 frames × 10 ms = 1 500 ms (matches architecture spec).
         */
        const val SILENCE_END_FRAMES = 150   // ≈ 1 500 ms

        /**
         * Minimum speech frames before declaring voice onset.
         * At 10 ms / frame → 30 × 10 ms = 300 ms (filters out brief clicks).
         */
        const val MIN_SPEECH_FRAMES = 30     // ≈ 300 ms

        /** Number of recent RMS values kept for threshold adaptation. */
        const val HISTORY_LENGTH = 50

        /** EMA coefficient for noise floor update (0 = never update, 1 = instant). */
        const val NOISE_EMA_ALPHA = 0.05f

        /**
         * How many times louder than the noise floor speech must be
         * to trigger voice onset. Higher = less sensitive.
         */
        const val NOISE_MARGIN_FACTOR = 4.0f
    }
}