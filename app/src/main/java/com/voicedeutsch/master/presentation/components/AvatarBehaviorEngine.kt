package com.voicedeutsch.master.presentation.components

import com.voicedeutsch.master.voicecore.engine.AvatarAudioData
import com.voicedeutsch.master.voicecore.engine.EmotionState
import kotlin.math.*
import kotlin.random.Random

/**
 * Avatar brain — generates realistic procedural behavior every frame (30fps).
 *
 * v2 improvements:
 *  - Perlin noise for ALL micro-movements (not sin+Random)
 *  - Proper state machine with blend transitions (no instant snaps)
 *  - Eye micro-saccades (tiny random eye movements that make it feel alive)
 *  - Breathing modulated by speech (faster when speaking)
 *  - Better gesture system with arm IK-like blending
 *  - Head movement follows speech emphasis (amplitude-driven nods)
 *  - Natural blink patterns (double blinks, speech-correlated)
 *  - Viseme cycling with variety (not just aa/O)
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
    private var stateBlend = 1f  // 0 = fully prevState, 1 = fully currentState
    private var stateChangeTime = 0f

    // ── Speech tracking ───────────────────────────────────────────────────
    private var lastSpeakingMs = System.currentTimeMillis()
    private var silenceStartMs = 0L
    private var lastAmp = 0f
    private var ampVelocity = 0f  // rate of amplitude change

    // ── Gesture system ────────────────────────────────────────────────────
    private var gestureTimer = 0f
    private var gesturePhase = 0   // 0=none, 1=left, 2=right, 3=both
    private var gestureBlend = 0f
    private var gestureIntensity = 0.7f

    // ── Blink system ──────────────────────────────────────────────────────
    private var blinkL = 0f
    private var blinkR = 0f
    private var nextBlinkMs = System.currentTimeMillis() + Random.nextLong(2000, 4000)
    private var doubleBlinkPending = false
    private var blinkPhase = 0 // 0=open, 1=closing, 2=opening

    // ── Eye saccade system ────────────────────────────────────────────────
    private var eyeTargetX = 0f
    private var eyeTargetY = 0f
    private var eyeCurrentX = 0f
    private var eyeCurrentY = 0f
    private var nextSaccadeMs = System.currentTimeMillis() + 500L

    // ── Nod system ────────────────────────────────────────────────────────
    private var nodTimer = 0f
    private var nodIntensity = 0f

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

        // ── Amplitude velocity (for emphasis detection) ───────────────────
        ampVelocity = (amp - lastAmp) / dt.coerceAtLeast(0.001f)

        // ── State machine ─────────────────────────────────────────────────
        val newState = determineState(speaking, amp, emotion, now)
        if (newState != state) {
            prevState = state
            state = newState
            stateBlend = 0f
            stateChangeTime = time
        }
        // Blend between states over 0.4s
        stateBlend = (stateBlend + dt / 0.4f).coerceAtMost(1f)

        if (speaking) {
            lastSpeakingMs = now
            silenceStartMs = 0L
        } else if (silenceStartMs == 0L) {
            silenceStartMs = now
        }

        // ── Breathing ─────────────────────────────────────────────────────
        val breathRate = if (speaking) 0.32f else 0.22f // faster when speaking
        val breath = sin(time * 2f * PI.toFloat() * breathRate)
        val breathDepth = if (speaking) 0.7f else 1f

        // ── Nod system (triggered by amplitude spikes) ────────────────────
        val spike = (amp - lastAmp).coerceAtLeast(0f)
        if (spike > 0.15f && speaking) {
            nodTimer = 0f
            nodIntensity = (spike * 12f).coerceAtMost(5f)
        }
        nodTimer += dt
        val nodValue = if (nodTimer < 0.35f) {
            sin(nodTimer * PI.toFloat() / 0.35f) * nodIntensity
        } else 0f

        // ── Gesture system ────────────────────────────────────────────────
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

        val prevTargets = computeStateTargets(prevState, amp, breath, breathDepth, nodValue, speaking)
        val currTargets = computeStateTargets(state, amp, breath, breathDepth, nodValue, speaking)

        // Blend between previous and current state
        val targets = blendTargets(prevTargets, currTargets, stateBlend)

        // Add noise to head/neck/body
        val noiseScale = when (state) {
            AvatarState.THINKING -> 0.6f  // less noise when thinking (focused)
            AvatarState.SPEAKING_ACTIVE -> 1.2f // more noise when animated
            else -> 1f
        }

        val finalHead = targets.head.plus(headNoise.scale(noiseScale))
        val finalNeck = targets.neck.plus(neckNoise.scale(noiseScale))
        val finalSpine = targets.spine.plus(bodyNoise.scale(noiseScale * 0.5f))

        // ── Compute arm targets with gesture blending ─────────────────────
        val armTargets = computeArmTargets(targets, amp, breath)

        // ── EMA smoothing ─────────────────────────────────────────────────
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

        // ── Morph targets ─────────────────────────────────────────────────
        val morphs = computeMorphs(targets, amp, speaking)

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
        // Morph base values
        val jawOpen: Float,
        val mouthOpen: Float,
        val smile: Float,
        val browInner: Float,
        val browDown: Float,
        val eyeLookUp: Float,
        val pucker: Float,
    )

    private fun computeStateTargets(
        targetState: AvatarState,
        amp: Float,
        breath: Float,
        breathDepth: Float,
        nodValue: Float,
        speaking: Boolean,
    ): StateTargets = when (targetState) {

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
                pitch = sin(time * 0.9f) * 2f - nodValue,
                yaw = sin(time * 0.6f) * 3.5f + sin(time * 0.23f) * 1.2f,
                roll = sin(time * 0.4f) * 1f,
            ),
            neck = BoneAngles(pitch = 1.5f + sin(time * 0.7f) * 0.6f),
            spine = BoneAngles(pitch = breath * breathDepth * 0.8f + 1.5f),
            spine1 = BoneAngles(pitch = 2f + breath * 0.4f),
            spine2 = BoneAngles(pitch = 0.8f),
            lShoulder = BoneAngles(pitch = breath * 0.25f),
            rShoulder = BoneAngles(pitch = breath * 0.25f),
            jawOpen = amp * 0.5f,
            mouthOpen = amp * 0.35f,
            smile = 0.1f,
            browInner = amp * 0.15f,
            browDown = 0f, eyeLookUp = 0f, pucker = 0f,
        )

        AvatarState.SPEAKING_ACTIVE -> StateTargets(
            head = BoneAngles(
                pitch = sin(time * 1.3f) * 3.5f - nodValue * 1.5f,
                yaw = sin(time * 0.8f) * 6f + sin(time * 0.3f) * 2f,
                roll = sin(time * 0.5f) * 1.5f,
            ),
            neck = BoneAngles(pitch = 2f + sin(time * 1.0f) * 1f),
            spine = BoneAngles(pitch = breath * breathDepth * 0.6f + 2f + amp * 1.2f),
            spine1 = BoneAngles(pitch = 3f + amp * 1.5f),
            spine2 = BoneAngles(pitch = 1.2f + amp * 0.8f),
            lShoulder = BoneAngles(pitch = amp * 1.5f + breath * 0.2f),
            rShoulder = BoneAngles(pitch = amp * 1.5f + breath * 0.2f),
            jawOpen = amp * 0.65f,
            mouthOpen = amp * 0.45f,
            smile = 0.06f,
            browInner = amp * 0.25f + noiseHead.sample(time * 2f) * 0.08f,
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
                jawOpen = 0f, mouthOpen = 0.1f, smile = 0.8f,
                browInner = 0f, browDown = 0f, eyeLookUp = 0f, pucker = 0f,
            )
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

    // ── Gesture system ────────────────────────────────────────────────────

    private fun updateGestures(dt: Float, speaking: Boolean, amp: Float) {
        gestureTimer -= dt

        when {
            state == AvatarState.SPEAKING_ACTIVE && gestureTimer <= 0f -> {
                val interval = Random.nextFloat() * 5f + 4f // 4-9 seconds
                gestureTimer = interval
                gesturePhase = when (Random.nextInt(4)) {
                    0 -> 1  // left hand
                    1 -> 2  // right hand
                    2 -> 3  // both hands
                    else -> 1
                }
                gestureIntensity = Random.nextFloat() * 0.4f + 0.5f // 0.5-0.9
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
            // Small random eye movement
            eyeTargetX = noiseEyeL.sample(time * 2f) * 0.15f
            eyeTargetY = noiseEyeR.sample(time * 2f + 50f) * 0.1f
            // Shorter intervals during conversation
            nextSaccadeMs = now + Random.nextLong(
                if (speaking) 300 else 600,
                if (speaking) 800 else 2000,
            )
        }
        // Fast saccade movement (eyes are quick)
        eyeCurrentX = lerp(eyeCurrentX, eyeTargetX, 0.25f)
        eyeCurrentY = lerp(eyeCurrentY, eyeTargetY, 0.25f)
    }

    // ── Blink system ──────────────────────────────────────────────────────

    private fun updateBlinks(dt: Float, now: Long, speaking: Boolean) {
        if (now >= nextBlinkMs && blinkPhase == 0) {
            blinkPhase = 1  // start closing
            // 20% chance of double blink
            doubleBlinkPending = Random.nextFloat() < 0.2f
        }

        when (blinkPhase) {
            1 -> { // Closing
                blinkL = (blinkL + dt * 12f).coerceAtMost(1f)
                blinkR = (blinkR + dt * 12f).coerceAtMost(1f)
                if (blinkL >= 1f) blinkPhase = 2
            }
            2 -> { // Opening
                blinkL = (blinkL - dt * 8f).coerceAtLeast(0f)
                blinkR = (blinkR - dt * 8f).coerceAtLeast(0f)
                if (blinkL <= 0f) {
                    if (doubleBlinkPending) {
                        doubleBlinkPending = false
                        blinkPhase = 1 // another blink
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

        // Base poses
        val claspL = BoneAngles(pitch = 42f, yaw = -18f, roll = 8f)
        val claspLF = BoneAngles(pitch = 48f, yaw = 0f, roll = 5f)
        val claspLH = BoneAngles(pitch = 0f, yaw = -12f, roll = 0f)
        val claspR = BoneAngles(pitch = 42f, yaw = 18f, roll = -8f)
        val claspRF = BoneAngles(pitch = 48f, yaw = 0f, roll = -5f)
        val claspRH = BoneAngles(pitch = 0f, yaw = 12f, roll = 0f)

        // Gesture poses with noise-driven variation
        val gNoise = noiseGest.sample(time * 0.5f) * 5f
        val gestLA = BoneAngles(pitch = -25f + gNoise, yaw = -25f, roll = -10f)
        val gestLF = BoneAngles(pitch = 35f + gNoise * 0.5f, yaw = 0f, roll = 0f)
        val gestLH = BoneAngles(pitch = -15f, yaw = -5f + gNoise * 0.3f, roll = 0f)
        val gestRA = BoneAngles(pitch = -25f - gNoise, yaw = 25f, roll = 10f)
        val gestRF = BoneAngles(pitch = 35f - gNoise * 0.5f, yaw = 0f, roll = 0f)
        val gestRH = BoneAngles(pitch = -15f, yaw = 5f - gNoise * 0.3f, roll = 0f)

        // Thinking pose
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
                // Clasp blend
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

    // ── Morph target computation ──────────────────────────────────────────

    private fun computeMorphs(
        targets: StateTargets,
        amp: Float,
        speaking: Boolean,
    ): Map<String, Float> {
        val mAlpha = if (speaking) 0.30f else 0.12f

        // Viseme system: procedural lip shapes driven by amplitude and noise
        // Cycles through different mouth shapes for variety
        val visemeT = time * 8f  // ~8Hz cycle for syllable rate
        val visemePhase2 = time * 3.5f  // slower secondary modulation

        // Different viseme weights create varied mouth shapes
        val visAA = if (speaking) {
            val base = (sin(visemeT) * 0.5f + 0.5f) * amp
            val mod = (sin(visemePhase2) * 0.3f + 0.7f)
            base * mod * 0.6f
        } else 0f

        val visO = if (speaking) {
            val base = (cos(visemeT * 0.7f) * 0.5f + 0.5f) * amp
            val mod = (cos(visemePhase2 * 0.6f) * 0.3f + 0.5f)
            base * mod * 0.4f
        } else 0f

        val visE = if (speaking) {
            val base = (sin(visemeT * 1.3f + 1.5f) * 0.5f + 0.5f) * amp
            base * 0.25f
        } else 0f

        val visSil = if (!speaking) 0.3f else {
            // Small amount of sil viseme during speech pauses
            (1f - amp).coerceIn(0f, 0.3f) * 0.2f
        }

        // Brow noise (subtle expressiveness during speech)
        val browNoise = if (speaking) noiseBrow.sample(time * 1.5f) * 0.1f else 0f

        // Jaw noise for natural variation
        val jawNoise = if (speaking) noiseJaw.sample(time * 6f) * amp * 0.1f else 0f

        return buildMap {
            // Jaw & mouth
            put("jawOpen", smoothMorph("jawOpen", targets.jawOpen + jawNoise, mAlpha))
            put("mouthOpen", smoothMorph("mouthOpen", targets.mouthOpen, mAlpha))
            put("mouthSmile", smoothMorph("mouthSmile", targets.smile, 0.06f))
            put("mouthPucker", smoothMorph("mouthPucker", targets.pucker, 0.06f))

            // Brows
            put("browInnerUp", smoothMorph("browInnerUp", targets.browInner + browNoise, 0.07f))
            put("browDownLeft", smoothMorph("browDownLeft", targets.browDown, 0.07f))
            put("browDownRight", smoothMorph("browDownRight", targets.browDown, 0.07f))

            // Eyes
            put("eyeLookUpLeft", smoothMorph("eyeLookUpLeft", targets.eyeLookUp + eyeCurrentY, 0.1f))
            put("eyeLookUpRight", smoothMorph("eyeLookUpRight", targets.eyeLookUp + eyeCurrentY, 0.1f))

            // Visemes
            put("viseme_aa", smoothMorph("viseme_aa", visAA, 0.28f))
            put("viseme_O", smoothMorph("viseme_O", visO, 0.25f))
            put("viseme_E", smoothMorph("viseme_E", visE, 0.22f))
            put("viseme_sil", smoothMorph("viseme_sil", visSil, 0.10f))

            // Blinks
            put("eyeBlinkLeft", smoothMorph("eyeBlinkLeft", blinkL, 0.6f))
            put("eyeBlinkRight", smoothMorph("eyeBlinkRight", blinkR, 0.6f))

            // Squint follows smile
            val smileVal = smoothMorphs["mouthSmile"] ?: 0f
            put("eyeSquintLeft", smoothMorph("eyeSquintLeft", smileVal * 0.4f, 0.06f))
            put("eyeSquintRight", smoothMorph("eyeSquintRight", smileVal * 0.4f, 0.06f))
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
        nodTimer = 10f
        nodIntensity = 0f
        lastAmp = 0f
        ampVelocity = 0f
        smoothMorphs.clear()

        val zero = BoneAngles()
        sHead = zero; sNeck = zero; sSpine = zero
        sSpine1 = zero; sSpine2 = zero
        sLShoulder = zero; sRShoulder = zero
        sLArm = zero; sLForeArm = zero; sLHand = zero
        sRArm = zero; sRForeArm = zero; sRHand = zero
    }
}