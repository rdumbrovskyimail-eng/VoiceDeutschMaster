package com.voicedeutsch.master.presentation.components

import com.voicedeutsch.master.voicecore.engine.AvatarAudioData
import com.voicedeutsch.master.voicecore.engine.EmotionState
import kotlin.math.*
import kotlin.random.Random

/**
 * Avatar brain v3 — generates realistic procedural behavior from spectral audio features.
 *
 * v3 improvements over v2:
 *  - REAL spectral data drives all animation (not synthetic)
 *  - Smile-in-voice → smile morph driven by spectral centroid shift
 *  - Pitch contour → eyebrow raises, head tilt, question expression
 *  - Emphasis/stress → head nods, gesture triggers, brow emphasis
 *  - Band energy → viseme selection (sibilants, vowels, plosives)
 *  - Vowel openness → jaw opening (spectral, not just amplitude)
 *  - Question intonation → brow raise + head tilt up
 *  - Spectral flux → plosive/transient detection for gesture timing
 */
class AvatarBehaviorEngine {

    enum class AvatarState { IDLE, SPEAKING_SOFT, SPEAKING_ACTIVE, THINKING, HAPPY }

    data class BoneAngles(
        val pitch: Float = 0f,
        val yaw: Float = 0f,
        val roll: Float = 0f,
    )

    data class AvatarFrame(
        val head: BoneAngles = BoneAngles(),
        val neck: BoneAngles = BoneAngles(),
        val spine: BoneAngles = BoneAngles(),
        val spine1: BoneAngles = BoneAngles(),
        val spine2: BoneAngles = BoneAngles(),
        val leftArm: BoneAngles = BoneAngles(),
        val leftForeArm: BoneAngles = BoneAngles(),
        val leftHand: BoneAngles = BoneAngles(),
        val rightArm: BoneAngles = BoneAngles(),
        val rightForeArm: BoneAngles = BoneAngles(),
        val rightHand: BoneAngles = BoneAngles(),
        val leftShoulder: BoneAngles = BoneAngles(),
        val rightShoulder: BoneAngles = BoneAngles(),
        val morphs: Map<String, Float> = emptyMap(),
    )

    // ── Noise generators (different seeds = uncorrelated channels) ─────────
    private val noiseHead = PerlinNoise(seed = 100)
    private val noiseNeck = PerlinNoise(seed = 200)
    private val noiseBody = PerlinNoise(seed = 300)
    private val noiseEyeL = PerlinNoise(seed = 400)
    private val noiseEyeR = PerlinNoise(seed = 500)
    private val noiseGest = PerlinNoise(seed = 600)
    private val noiseBrow = PerlinNoise(seed = 700)
    private val noiseJaw  = PerlinNoise(seed = 800)

    // ── Time & state ──────────────────────────────────────────────────────
    private var time = 0f
    private var state = AvatarState.IDLE
    private var prevState = AvatarState.IDLE
    private var stateBlend = 1f
    private var stateChangeTime = 0f

    // ── Speech tracking ───────────────────────────────────────────────────
    private var lastSpeakingMs = System.currentTimeMillis()
    private var silenceStartMs = 0L
    private var lastAmp = 0f
    private var ampVelocity = 0f

    // ── Gesture system ────────────────────────────────────────────────────
    private var gestureTimer = 0f
    private var gesturePhase = 0
    private var gestureBlend = 0f
    private var gestureIntensity = 0.7f

    // ── Blink system ──────────────────────────────────────────────────────
    private var blinkL = 0f
    private var blinkR = 0f
    private var nextBlinkMs = System.currentTimeMillis() + Random.nextLong(2000, 4000)
    private var doubleBlinkPending = false
    private var blinkPhase = 0

    // ── Eye saccade system ────────────────────────────────────────────────
    private var eyeTargetX = 0f
    private var eyeTargetY = 0f
    private var eyeCurrentX = 0f
    private var eyeCurrentY = 0f
    private var nextSaccadeMs = System.currentTimeMillis() + 500L

    // ── Nod system (now driven by real emphasis) ──────────────────────────
    private var nodTimer = 0f
    private var nodIntensity = 0f

    // ── Prosodic tracking ─────────────────────────────────────────────────
    private var smoothedSmile = 0f
    private var smoothedEmphasis = 0f
    private var smoothedPitch = 0f
    private var smoothedPitchDelta = 0f
    private var smoothedQuestion = 0f  // smooth blend for question expression
    private var questionHoldTimer = 0f

    // ── Smoothed bone outputs ─────────────────────────────────────────────
    private var sHead = BoneAngles()
    private var sNeck = BoneAngles()
    private var sSpine = BoneAngles()
    private var sSpine1 = BoneAngles()
    private var sSpine2 = BoneAngles()
    private var sLArm = BoneAngles()
    private var sLForeArm = BoneAngles()
    private var sLHand = BoneAngles()
    private var sRArm = BoneAngles()
    private var sRForeArm = BoneAngles()
    private var sRHand = BoneAngles()
    private var sLShoulder = BoneAngles()
    private var sRShoulder = BoneAngles()

    private val smoothMorphs = mutableMapOf<String, Float>()

    // ── Utilities ─────────────────────────────────────────────────────────

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

    private fun BoneAngles.lerp(target: BoneAngles, t: Float) = BoneAngles(
        lerp(pitch, target.pitch, t),
        lerp(yaw, target.yaw, t),
        lerp(roll, target.roll, t),
    )

    private fun BoneAngles.plus(other: BoneAngles) = BoneAngles(
        pitch + other.pitch,
        yaw + other.yaw,
        roll + other.roll,
    )

    private fun BoneAngles.scale(s: Float) = BoneAngles(pitch * s, yaw * s, roll * s)

    private fun smoothMorph(key: String, target: Float, alpha: Float): Float {
        val cur = smoothMorphs[key] ?: 0f
        val next = lerp(cur, target, alpha)
        smoothMorphs[key] = next
        return next
    }

    // ── Main update (call at ~30fps) ──────────────────────────────────────

    fun update(audio: AvatarAudioData, dt: Float = 0.033f): AvatarFrame {
        time += dt
        val now = System.currentTimeMillis()
        val amp = audio.amplitude.coerceIn(0f, 1f)
        val speaking = audio.isSpeaking
        val emotion = audio.emotion

        // ── Smooth prosodic features ──────────────────────────────────────
        smoothedSmile = lerp(smoothedSmile, audio.smileScore, 0.08f)
        smoothedEmphasis = lerp(smoothedEmphasis, audio.emphasis, 0.15f)
        smoothedPitch = lerp(smoothedPitch, audio.pitch, 0.12f)
        smoothedPitchDelta = lerp(smoothedPitchDelta, audio.pitchDelta, 0.10f)

        // Question expression: blend in quickly, hold, blend out slowly
        if (audio.isQuestion) {
            smoothedQuestion = lerp(smoothedQuestion, 1f, 0.15f)
            questionHoldTimer = 0.8f  // hold for 0.8s after question detected
        } else if (questionHoldTimer > 0f) {
            questionHoldTimer -= dt
        } else {
            smoothedQuestion = lerp(smoothedQuestion, 0f, 0.04f)
        }

        // ── Amplitude velocity ────────────────────────────────────────────
        ampVelocity = (amp - lastAmp) / dt.coerceAtLeast(0.001f)

        // ── State machine ─────────────────────────────────────────────────
        val newState = determineState(speaking, amp, emotion, now)
        if (newState != state) {
            prevState = state
            state = newState
            stateBlend = 0f
            stateChangeTime = time
        }
        stateBlend = (stateBlend + dt / 0.4f).coerceAtMost(1f)

        if (speaking) {
            lastSpeakingMs = now
            silenceStartMs = 0L
        } else if (silenceStartMs == 0L) {
            silenceStartMs = now
        }

        // ── Breathing ─────────────────────────────────────────────────────
        val breathRate = if (speaking) 0.32f else 0.22f
        val breath = sin(time * 2f * PI.toFloat() * breathRate)
        val breathDepth = if (speaking) 0.7f else 1f

        // ── Nod system (driven by real emphasis data) ─────────────────────
        // Real emphasis spikes trigger nods — no more fake random nods
        if (smoothedEmphasis > 0.4f && speaking) {
            if (nodTimer > 0.5f) {  // cooldown between nods
                nodTimer = 0f
                nodIntensity = (smoothedEmphasis * 8f).coerceAtMost(5f)
            }
        }
        nodTimer += dt
        val nodValue = if (nodTimer < 0.35f) {
            sin(nodTimer * PI.toFloat() / 0.35f) * nodIntensity
        } else 0f

        // ── Gesture system (triggered by emphasis, not random) ────────────
        updateGestures(dt, speaking, amp)

        // ── Eye saccades ──────────────────────────────────────────────────
        updateEyeSaccades(dt, now, speaking)

        // ── Blink system ──────────────────────────────────────────────────
        updateBlinks(dt, now, speaking)

        // ── Perlin noise micro-movements ──────────────────────────────────
        val headNoise = BoneAngles(
            pitch = noiseHead.fbm(time * 0.4f) * 1.5f,
            yaw = noiseHead.fbm(time * 0.35f + 10f) * 2f,
            roll = noiseHead.fbm(time * 0.3f + 20f) * 0.8f,
        )
        val neckNoise = BoneAngles(
            pitch = noiseNeck.fbm(time * 0.3f) * 0.6f,
            yaw = noiseNeck.fbm(time * 0.25f + 10f) * 0.8f,
            roll = noiseNeck.fbm(time * 0.2f + 20f) * 0.4f,
        )
        val bodyNoise = BoneAngles(
            pitch = noiseBody.fbm(time * 0.15f) * 0.3f,
            yaw = noiseBody.fbm(time * 0.12f + 10f) * 0.2f,
            roll = noiseBody.fbm(time * 0.1f + 20f) * 0.15f,
        )

        // ── State-specific targets ───────────────────────────────────────
        val prevTargets = computeStateTargets(prevState, audio, breath, breathDepth, nodValue)
        val currTargets = computeStateTargets(state, audio, breath, breathDepth, nodValue)
        val targets = blendTargets(prevTargets, currTargets, stateBlend)

        // ── Add noise (scaled by state) ──────────────────────────────────
        val noiseScale = when (state) {
            AvatarState.THINKING -> 0.6f
            AvatarState.SPEAKING_ACTIVE -> 1.2f
            else -> 1f
        }

        // ── Pitch-driven head movement (real prosody!) ────────────────────
        val pitchHead = BoneAngles(
            pitch = smoothedPitchDelta * -4f,  // rising pitch → slight head raise
            yaw = 0f,
            roll = smoothedPitchDelta * 2f,    // slight tilt on pitch changes
        )

        // ── Question expression: head tilt up + slight forward lean ───────
        val questionHead = BoneAngles(
            pitch = smoothedQuestion * -3f,    // chin up
            roll = smoothedQuestion * 4f,      // slight tilt
        )

        val finalHead = targets.head
            .plus(headNoise.scale(noiseScale))
            .plus(pitchHead)
            .plus(questionHead)

        val finalNeck = targets.neck.plus(neckNoise.scale(noiseScale))
        val finalSpine = targets.spine.plus(bodyNoise.scale(noiseScale * 0.5f))

        // ── Arm targets with gesture blending ─────────────────────────────
        val armTargets = computeArmTargets(targets, amp, breath)

        // ── EMA bone smoothing ────────────────────────────────────────────
        val hSpeed = if (speaking) 0.12f else 0.07f
        val bSpeed = 0.06f
        val aSpeed = if (gesturePhase > 0) 0.07f else 0.09f

        sHead = sHead.lerp(finalHead, hSpeed)
        sNeck = sNeck.lerp(finalNeck, hSpeed * 0.7f)
        sSpine = sSpine.lerp(finalSpine, bSpeed)
        sSpine1 = sSpine1.lerp(targets.spine1, bSpeed)
        sSpine2 = sSpine2.lerp(targets.spine2, bSpeed)
        sLShoulder = sLShoulder.lerp(targets.lShoulder, bSpeed)
        sRShoulder = sRShoulder.lerp(targets.rShoulder, bSpeed)
        sLArm = sLArm.lerp(armTargets.lArm, aSpeed)
        sLForeArm = sLForeArm.lerp(armTargets.lForeArm, aSpeed)
        sLHand = sLHand.lerp(armTargets.lHand, aSpeed)
        sRArm = sRArm.lerp(armTargets.rArm, aSpeed)
        sRForeArm = sRForeArm.lerp(armTargets.rForeArm, aSpeed)
        sRHand = sRHand.lerp(armTargets.rHand, aSpeed)

        // ── Morph targets (spectral-driven) ───────────────────────────────
        val morphs = computeMorphs(targets, audio)

        lastAmp = amp

        return AvatarFrame(
            head = sHead,
            neck = sNeck,
            spine = sSpine,
            spine1 = sSpine1,
            spine2 = sSpine2,
            leftShoulder = sLShoulder,
            rightShoulder = sRShoulder,
            leftArm = sLArm,
            leftForeArm = sLForeArm,
            leftHand = sLHand,
            rightArm = sRArm,
            rightForeArm = sRForeArm,
            rightHand = sRHand,
            morphs = morphs,
        )
    }

    // ── State determination ───────────────────────────────────────────────

    private fun determineState(
        speaking: Boolean,
        amp: Float,
        emotion: EmotionState,
        now: Long,
    ): AvatarState = when {
        emotion == EmotionState.HAPPY -> AvatarState.HAPPY
        speaking && amp >= 0.25f -> AvatarState.SPEAKING_ACTIVE
        speaking -> AvatarState.SPEAKING_SOFT
        silenceStartMs > 0 && (now - silenceStartMs) > 1200L &&
            (now - lastSpeakingMs) < 8000L -> AvatarState.THINKING
        else -> AvatarState.IDLE
    }

    // ── State-specific target computation ─────────────────────────────────

    private data class StateTargets(
        val head: BoneAngles,
        val neck: BoneAngles,
        val spine: BoneAngles,
        val spine1: BoneAngles,
        val spine2: BoneAngles,
        val lShoulder: BoneAngles,
        val rShoulder: BoneAngles,
        val jawOpen: Float,
        val mouthOpen: Float,
        val smile: Float,
        val browInner: Float,
        val browDown: Float,
        val eyeLookUp: Float,
        val pucker: Float,
    )

    /**
     * State targets now use real prosodic data (audio) instead of just amplitude.
     */
    private fun computeStateTargets(
        targetState: AvatarState,
        audio: AvatarAudioData,
        breath: Float,
        breathDepth: Float,
        nodValue: Float,
    ): StateTargets {
        val amp = audio.amplitude
        val emphasis = smoothedEmphasis
        val pitch = smoothedPitch

        return when (targetState) {

            AvatarState.IDLE -> StateTargets(
                head = BoneAngles(
                    pitch = sin(time * 0.28f) * 1.0f,
                    yaw = sin(time * 0.19f) * 1.5f,
                    roll = sin(time * 0.13f) * 0.6f,
                ),
                neck = BoneAngles(pitch = sin(time * 0.24f) * 0.5f),
                spine = BoneAngles(pitch = breath * breathDepth * 1.0f),
                spine1 = BoneAngles(pitch = breath * breathDepth * 0.6f),
                spine2 = BoneAngles(),
                lShoulder = BoneAngles(pitch = breath * 0.3f),
                rShoulder = BoneAngles(pitch = breath * 0.3f),
                jawOpen = 0f, mouthOpen = 0f, smile = 0.05f,
                browInner = 0f, browDown = 0f, eyeLookUp = 0f, pucker = 0f,
            )

            AvatarState.SPEAKING_SOFT -> StateTargets(
                head = BoneAngles(
                    // Pitch-driven head movement: higher pitch → slight head raise
                    pitch = sin(time * 0.9f) * 2f - nodValue + pitch * 2f,
                    yaw = sin(time * 0.6f) * 3.5f + sin(time * 0.23f) * 1.2f,
                    roll = sin(time * 0.4f) * 1f,
                ),
                neck = BoneAngles(pitch = 1.5f + sin(time * 0.7f) * 0.6f),
                spine = BoneAngles(pitch = breath * breathDepth * 0.8f + 1.5f),
                spine1 = BoneAngles(pitch = 2f + breath * 0.4f),
                spine2 = BoneAngles(pitch = 0.8f),
                lShoulder = BoneAngles(pitch = breath * 0.25f),
                rShoulder = BoneAngles(pitch = breath * 0.25f),
                // Use spectral vowelOpenness for jaw, not just amplitude
                jawOpen = audio.vowelOpenness * 0.5f,
                mouthOpen = audio.vowelOpenness * 0.35f,
                // Real smile from spectral analysis!
                smile = smoothedSmile * 0.7f + 0.05f,
                browInner = emphasis * 0.2f,
                browDown = 0f, eyeLookUp = 0f, pucker = 0f,
            )

            AvatarState.SPEAKING_ACTIVE -> StateTargets(
                head = BoneAngles(
                    pitch = sin(time * 1.3f) * 3.5f - nodValue * 1.5f + pitch * 3f,
                    yaw = sin(time * 0.8f) * 6f + sin(time * 0.3f) * 2f,
                    roll = sin(time * 0.5f) * 1.5f + emphasis * 2f,
                ),
                neck = BoneAngles(pitch = 2f + sin(time * 1.0f) * 1f),
                spine = BoneAngles(pitch = breath * breathDepth * 0.6f + 2f + amp * 1.2f),
                spine1 = BoneAngles(pitch = 3f + amp * 1.5f),
                spine2 = BoneAngles(pitch = 1.2f + amp * 0.8f),
                // Shoulders lift with emphasis
                lShoulder = BoneAngles(pitch = emphasis * 2f + breath * 0.2f),
                rShoulder = BoneAngles(pitch = emphasis * 2f + breath * 0.2f),
                jawOpen = audio.vowelOpenness * 0.65f,
                mouthOpen = audio.vowelOpenness * 0.45f,
                smile = smoothedSmile * 0.6f + 0.03f,
                // Brow raises with emphasis and pitch
                browInner = emphasis * 0.3f + pitch * 0.15f,
                browDown = 0f, eyeLookUp = 0f, pucker = 0f,
            )

            AvatarState.THINKING -> StateTargets(
                head = BoneAngles(
                    pitch = -2.5f + sin(time * 0.2f) * 0.6f,
                    yaw = sin(time * 0.15f) * 1.5f,
                    roll = 7f + sin(time * 0.18f) * 0.8f,
                ),
                neck = BoneAngles(pitch = -0.8f, roll = 2.5f),
                spine = BoneAngles(pitch = breath * breathDepth * 0.8f - 1f),
                spine1 = BoneAngles(pitch = -1.5f),
                spine2 = BoneAngles(pitch = -0.8f),
                lShoulder = BoneAngles(),
                rShoulder = BoneAngles(pitch = -1.5f),
                jawOpen = 0f, mouthOpen = 0f, smile = 0f,
                browInner = 0.55f, browDown = 0.18f,
                eyeLookUp = 0.35f, pucker = 0.08f,
            )

            AvatarState.HAPPY -> {
                val bob = sin(time * 3.5f) * 1.8f
                StateTargets(
                    head = BoneAngles(pitch = bob, yaw = sin(time * 0.8f) * 2.5f),
                    neck = BoneAngles(pitch = 2f + bob * 0.4f),
                    spine = BoneAngles(pitch = breath * breathDepth * 1f + 2f),
                    spine1 = BoneAngles(pitch = 1.8f),
                    spine2 = BoneAngles(),
                    lShoulder = BoneAngles(pitch = 1.5f + bob * 0.4f),
                    rShoulder = BoneAngles(pitch = 1.5f + bob * 0.4f),
                    jawOpen = 0f, mouthOpen = 0.1f,
                    // Strong smile for happy state, boosted by smileScore
                    smile = 0.8f + smoothedSmile * 0.2f,
                    browInner = 0f, browDown = 0f, eyeLookUp = 0f, pucker = 0f,
                )
            }
        }
    }

    private fun blendTargets(a: StateTargets, b: StateTargets, t: Float): StateTargets =
        StateTargets(
            head = a.head.lerp(b.head, t),
            neck = a.neck.lerp(b.neck, t),
            spine = a.spine.lerp(b.spine, t),
            spine1 = a.spine1.lerp(b.spine1, t),
            spine2 = a.spine2.lerp(b.spine2, t),
            lShoulder = a.lShoulder.lerp(b.lShoulder, t),
            rShoulder = a.rShoulder.lerp(b.rShoulder, t),
            jawOpen = lerp(a.jawOpen, b.jawOpen, t),
            mouthOpen = lerp(a.mouthOpen, b.mouthOpen, t),
            smile = lerp(a.smile, b.smile, t),
            browInner = lerp(a.browInner, b.browInner, t),
            browDown = lerp(a.browDown, b.browDown, t),
            eyeLookUp = lerp(a.eyeLookUp, b.eyeLookUp, t),
            pucker = lerp(a.pucker, b.pucker, t),
        )

    // ── Gesture system (emphasis-driven, not random-timed) ────────────────

    private fun updateGestures(dt: Float, speaking: Boolean, amp: Float) {
        gestureTimer -= dt

        when {
            // Trigger gestures on emphasis peaks during active speech
            state == AvatarState.SPEAKING_ACTIVE && gestureTimer <= 0f && smoothedEmphasis > 0.35f -> {
                gestureTimer = Random.nextFloat() * 3f + 3f  // 3-6 sec cooldown
                gesturePhase = when (Random.nextInt(4)) {
                    0 -> 1; 1 -> 2; 2 -> 3; else -> 1
                }
                // Gesture intensity proportional to emphasis
                gestureIntensity = (smoothedEmphasis * 1.2f).coerceIn(0.4f, 0.9f)
                gestureBlend = 0f
            }
            state != AvatarState.SPEAKING_ACTIVE && state != AvatarState.SPEAKING_SOFT -> {
                gesturePhase = 0
                gestureBlend = lerp(gestureBlend, 0f, 0.06f)
            }
        }
        if (gesturePhase > 0) {
            gestureBlend = lerp(gestureBlend, gestureIntensity, 0.05f)
        }
    }

    // ── Eye saccades ──────────────────────────────────────────────────────

    private fun updateEyeSaccades(dt: Float, now: Long, speaking: Boolean) {
        if (now >= nextSaccadeMs) {
            eyeTargetX = noiseEyeL.sample(time * 2f) * 0.15f
            eyeTargetY = noiseEyeR.sample(time * 2f + 50f) * 0.1f
            nextSaccadeMs = now + Random.nextLong(
                if (speaking) 300 else 600,
                if (speaking) 800 else 2000,
            )
        }
        eyeCurrentX = lerp(eyeCurrentX, eyeTargetX, 0.25f)
        eyeCurrentY = lerp(eyeCurrentY, eyeTargetY, 0.25f)
    }

    // ── Blink system ──────────────────────────────────────────────────────

    private fun updateBlinks(dt: Float, now: Long, speaking: Boolean) {
        if (now >= nextBlinkMs && blinkPhase == 0) {
            blinkPhase = 1
            doubleBlinkPending = Random.nextFloat() < 0.2f
        }

        when (blinkPhase) {
            1 -> {
                blinkL = (blinkL + dt * 12f).coerceAtMost(1f)
                blinkR = (blinkR + dt * 12f).coerceAtMost(1f)
                if (blinkL >= 1f) blinkPhase = 2
            }
            2 -> {
                blinkL = (blinkL - dt * 8f).coerceAtLeast(0f)
                blinkR = (blinkR - dt * 8f).coerceAtLeast(0f)
                if (blinkL <= 0f) {
                    if (doubleBlinkPending) {
                        doubleBlinkPending = false
                        blinkPhase = 1
                    } else {
                        blinkPhase = 0
                        nextBlinkMs = now + Random.nextLong(
                            if (speaking) 1800 else 3000,
                            if (speaking) 3500 else 5500,
                        )
                    }
                }
            }
        }
    }

    // ── Arm computation ───────────────────────────────────────────────────

    private data class ArmTargets(
        val lArm: BoneAngles,
        val lForeArm: BoneAngles,
        val lHand: BoneAngles,
        val rArm: BoneAngles,
        val rForeArm: BoneAngles,
        val rHand: BoneAngles,
    )

    private fun computeArmTargets(
        targets: StateTargets,
        amp: Float,
        breath: Float,
    ): ArmTargets {
        val rest = BoneAngles()

        val claspL = BoneAngles(pitch = 42f, yaw = -18f, roll = 8f)
        val claspLF = BoneAngles(pitch = 48f, yaw = 0f, roll = 5f)
        val claspLH = BoneAngles(pitch = 0f, yaw = -12f, roll = 0f)
        val claspR = BoneAngles(pitch = 42f, yaw = 18f, roll = -8f)
        val claspRF = BoneAngles(pitch = 48f, yaw = 0f, roll = -5f)
        val claspRH = BoneAngles(pitch = 0f, yaw = 12f, roll = 0f)

        val gNoise = noiseGest.sample(time * 0.5f) * 5f
        val gestLA = BoneAngles(pitch = -25f + gNoise, yaw = -25f, roll = -10f)
        val gestLF = BoneAngles(pitch = 35f + gNoise * 0.5f, yaw = 0f, roll = 0f)
        val gestLH = BoneAngles(pitch = -15f, yaw = -5f + gNoise * 0.3f, roll = 0f)
        val gestRA = BoneAngles(pitch = -25f - gNoise, yaw = 25f, roll = 10f)
        val gestRF = BoneAngles(pitch = 35f - gNoise * 0.5f, yaw = 0f, roll = 0f)
        val gestRH = BoneAngles(pitch = -15f, yaw = 5f - gNoise * 0.3f, roll = 0f)

        val thinkRA = BoneAngles(pitch = 35f, yaw = 18f, roll = -8f)
        val thinkRF = BoneAngles(pitch = 85f, yaw = 0f, roll = 0f)
        val thinkRH = BoneAngles(pitch = -25f, yaw = 0f, roll = 0f)
        val thinkLA = BoneAngles(pitch = 15f, yaw = -10f, roll = 5f)
        val thinkLF = BoneAngles(pitch = 20f, yaw = 0f, roll = 0f)

        return when (state) {
            AvatarState.THINKING -> ArmTargets(
                lArm = thinkLA, lForeArm = thinkLF, lHand = rest,
                rArm = thinkRA, rForeArm = thinkRF, rHand = thinkRH,
            )
            AvatarState.HAPPY -> ArmTargets(
                lArm = rest.lerp(BoneAngles(pitch = -15f, yaw = -10f), 0.6f),
                lForeArm = rest.lerp(BoneAngles(pitch = 10f), 0.5f),
                lHand = rest,
                rArm = rest.lerp(BoneAngles(pitch = -15f, yaw = 10f), 0.6f),
                rForeArm = rest.lerp(BoneAngles(pitch = 10f), 0.5f),
                rHand = rest,
            )
            else -> {
                val claspAmount = when (state) {
                    AvatarState.IDLE -> 1f - gestureBlend
                    AvatarState.SPEAKING_SOFT -> (1f - gestureBlend) * 0.9f
                    else -> 1f - gestureBlend
                }

                val baseLA = rest.lerp(claspL, claspAmount)
                val baseLF = rest.lerp(claspLF, claspAmount)
                val baseLH = rest.lerp(claspLH, claspAmount)
                val baseRA = rest.lerp(claspR, claspAmount)
                val baseRF = rest.lerp(claspRF, claspAmount)
                val baseRH = rest.lerp(claspRH, claspAmount)

                when (gesturePhase) {
                    1 -> ArmTargets(
                        lArm = baseLA.lerp(gestLA, gestureBlend),
                        lForeArm = baseLF.lerp(gestLF, gestureBlend),
                        lHand = baseLH.lerp(gestLH, gestureBlend),
                        rArm = baseRA, rForeArm = baseRF, rHand = baseRH,
                    )
                    2 -> ArmTargets(
                        lArm = baseLA, lForeArm = baseLF, lHand = baseLH,
                        rArm = baseRA.lerp(gestRA, gestureBlend),
                        rForeArm = baseRF.lerp(gestRF, gestureBlend),
                        rHand = baseRH.lerp(gestRH, gestureBlend),
                    )
                    3 -> ArmTargets(
                        lArm = baseLA.lerp(gestLA, gestureBlend * 0.8f),
                        lForeArm = baseLF.lerp(gestLF, gestureBlend * 0.8f),
                        lHand = baseLH.lerp(gestLH, gestureBlend * 0.7f),
                        rArm = baseRA.lerp(gestRA, gestureBlend * 0.8f),
                        rForeArm = baseRF.lerp(gestRF, gestureBlend * 0.8f),
                        rHand = baseRH.lerp(gestRH, gestureBlend * 0.7f),
                    )
                    else -> ArmTargets(
                        lArm = baseLA, lForeArm = baseLF, lHand = baseLH,
                        rArm = baseRA, rForeArm = baseRF, rHand = baseRH,
                    )
                }
            }
        }
    }

    // ── Morph target computation (spectral-driven) ────────────────────────

    /**
     * v3: Morphs driven by real spectral features:
     *  - Jaw/mouth: vowelOpenness from band energy (not just amplitude)
     *  - Smile: smileScore from spectral centroid shift
     *  - Brows: emphasis + pitch from real prosody
     *  - Visemes: band energy selects lip shapes (sibilants vs vowels)
     *  - Question: brow raise from rising pitch detection
     */
    private fun computeMorphs(
        targets: StateTargets,
        audio: AvatarAudioData,
    ): Map<String, Float> {
        val speaking = audio.isSpeaking
        val amp = audio.amplitude
        val mAlpha = if (speaking) 0.30f else 0.12f

        // ── Viseme system: REAL band energy drives lip shapes ─────────────
        // Low band (80-300Hz) → open vowels (aa, O)
        // Mid band (300-2kHz) → mid vowels (E, U)
        // High band (2k-8kHz) → sibilants/fricatives (SS, FF, TH)

        val bLow = audio.bandEnergy.low
        val bMid = audio.bandEnergy.mid
        val bHigh = audio.bandEnergy.high

        val visAA = if (speaking) {
            // Open vowels driven by low-frequency energy + amplitude
            (bLow * 0.5f + audio.vowelOpenness * 0.3f + amp * 0.2f).coerceIn(0f, 0.7f)
        } else 0f

        val visO = if (speaking) {
            // Rounded vowels: mid-band dominant, low-band present
            val midDominance = if (bLow > 0.01f) (bMid / (bLow + 0.01f)).coerceIn(0f, 2f) / 2f else 0f
            (midDominance * amp * 0.5f).coerceIn(0f, 0.5f)
        } else 0f

        val visE = if (speaking) {
            // Front vowels: mid-band with moderate opening
            (bMid * 0.4f * amp).coerceIn(0f, 0.35f)
        } else 0f

        val visSS = if (speaking) {
            // Sibilants (s, z, sh): high-frequency energy + high ZCR
            (bHigh * 0.6f + audio.zeroCrossingRate * 0.3f).coerceIn(0f, 0.4f) * amp
        } else 0f

        val visFF = if (speaking) {
            // Fricatives (f, v, th): moderate high-frequency + lower ZCR than sibilants
            val fricative = bHigh * 0.4f * (1f - audio.zeroCrossingRate * 0.5f)
            (fricative * amp).coerceIn(0f, 0.3f)
        } else 0f

        val visSil = if (!speaking) 0.3f else {
            (1f - amp).coerceIn(0f, 0.3f) * 0.2f
        }

        // ── Brow: real emphasis + pitch + question ────────────────────────
        val browEmphasis = if (speaking) smoothedEmphasis * 0.25f else 0f
        val browPitch = if (speaking) smoothedPitch * 0.12f else 0f
        val browQuestion = smoothedQuestion * 0.4f  // strong raise for questions

        // ── Jaw: spectral vowelOpenness (much better than amplitude alone) ─
        val jawNoise = if (speaking) noiseJaw.sample(time * 6f) * amp * 0.05f else 0f

        // ── Smile: REAL smile from spectral centroid analysis! ────────────
        // smoothedSmile comes from SpectralFeatureExtractor → AvatarAudioAnalyzer
        // This is the key quality improvement — no more fake smiles
        val realSmile = smoothedSmile

        return buildMap {
            // Jaw & mouth — driven by spectral vowelOpenness
            put("jawOpen", smoothMorph("jawOpen", targets.jawOpen + jawNoise, mAlpha))
            put("mouthOpen", smoothMorph("mouthOpen", targets.mouthOpen, mAlpha))

            // Smile — REAL spectral smile detection!
            put("mouthSmile", smoothMorph("mouthSmile",
                targets.smile.coerceAtLeast(realSmile * 0.8f), 0.08f))
            put("mouthPucker", smoothMorph("mouthPucker", targets.pucker, 0.06f))

            // Brows — driven by real emphasis, pitch, and question intonation
            put("browInnerUp", smoothMorph("browInnerUp",
                targets.browInner + browEmphasis + browPitch + browQuestion, 0.09f))
            put("browDownLeft", smoothMorph("browDownLeft", targets.browDown, 0.07f))
            put("browDownRight", smoothMorph("browDownRight", targets.browDown, 0.07f))

            // Eyes
            put("eyeLookUpLeft", smoothMorph("eyeLookUpLeft",
                targets.eyeLookUp + eyeCurrentY + smoothedQuestion * 0.15f, 0.1f))
            put("eyeLookUpRight", smoothMorph("eyeLookUpRight",
                targets.eyeLookUp + eyeCurrentY + smoothedQuestion * 0.15f, 0.1f))

            // Visemes — driven by REAL band energy!
            put("viseme_aa", smoothMorph("viseme_aa", visAA, 0.28f))
            put("viseme_O", smoothMorph("viseme_O", visO, 0.25f))
            put("viseme_E", smoothMorph("viseme_E", visE, 0.22f))
            put("viseme_SS", smoothMorph("viseme_SS", visSS, 0.30f))
            put("viseme_FF", smoothMorph("viseme_FF", visFF, 0.25f))
            put("viseme_sil", smoothMorph("viseme_sil", visSil, 0.10f))

            // Blinks
            put("eyeBlinkLeft", smoothMorph("eyeBlinkLeft", blinkL, 0.6f))
            put("eyeBlinkRight", smoothMorph("eyeBlinkRight", blinkR, 0.6f))

            // Squint follows smile (real smile → real squint)
            val smileVal = smoothMorphs["mouthSmile"] ?: 0f
            put("eyeSquintLeft", smoothMorph("eyeSquintLeft", smileVal * 0.5f, 0.07f))
            put("eyeSquintRight", smoothMorph("eyeSquintRight", smileVal * 0.5f, 0.07f))

            // Cheek puff on emphasis (subtle)
            if (speaking && smoothedEmphasis > 0.5f) {
                put("cheekPuff", smoothMorph("cheekPuff", smoothedEmphasis * 0.15f, 0.05f))
            } else {
                put("cheekPuff", smoothMorph("cheekPuff", 0f, 0.03f))
            }

            // Nose wrinkle on strong smile
            val strongSmile = (smileVal - 0.5f).coerceAtLeast(0f) * 2f
            put("noseSneerLeft", smoothMorph("noseSneerLeft", strongSmile * 0.2f, 0.05f))
            put("noseSneerRight", smoothMorph("noseSneerRight", strongSmile * 0.2f, 0.05f))
        }
    }

    // ── Reset ─────────────────────────────────────────────────────────────

    fun reset() {
        time = 0f
        state = AvatarState.IDLE
        prevState = AvatarState.IDLE
        stateBlend = 1f
        gestureTimer = 0f
        gesturePhase = 0
        gestureBlend = 0f
        blinkL = 0f
        blinkR = 0f
        blinkPhase = 0
        eyeCurrentX = 0f
        eyeCurrentY = 0f
        nodTimer = 0f  // Позволяет кивкам начаться сразу при emphasis > 0.4
        nodIntensity = 0f
        lastAmp = 0f
        ampVelocity = 0f
        smoothedSmile = 0f
        smoothedEmphasis = 0f
        smoothedPitch = 0f
        smoothedPitchDelta = 0f
        smoothedQuestion = 0f
        questionHoldTimer = 0f
        smoothMorphs.clear()

        val zero = BoneAngles()
        sHead = zero; sNeck = zero; sSpine = zero
        sSpine1 = zero; sSpine2 = zero
        sLShoulder = zero; sRShoulder = zero
        sLArm = zero; sLForeArm = zero; sLHand = zero
        sRArm = zero; sRForeArm = zero; sRHand = zero
    }
}
