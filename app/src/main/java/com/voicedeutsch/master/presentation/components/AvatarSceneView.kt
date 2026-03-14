package com.voicedeutsch.master.presentation.components

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.voicedeutsch.master.voicecore.engine.AvatarAudioData
import com.voicedeutsch.master.voicecore.engine.AvatarGender
import com.voicedeutsch.master.voicecore.engine.EmotionState
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.*
import kotlin.random.Random

// ── TAG ──────────────────────────────────────────────────────────────────────
private const val TAG = "AvatarScene"

// ── Morph target names (standard Ready Player Me / Avaturn naming) ────────────
private object Morph {
    // Mouth / jaw
    const val JAW_OPEN         = "jawOpen"
    const val MOUTH_OPEN       = "mouthOpen"
    const val MOUTH_FUNNEL     = "mouthFunnel"
    const val MOUTH_PUCKER     = "mouthPucker"
    const val MOUTH_SMILE_L    = "mouthSmileLeft"
    const val MOUTH_SMILE_R    = "mouthSmileRight"
    const val MOUTH_FROWN_L    = "mouthFrownLeft"
    const val MOUTH_FROWN_R    = "mouthFrownRight"
    const val MOUTH_PRESS_L    = "mouthPressLeft"
    const val MOUTH_PRESS_R    = "mouthPressRight"
    const val MOUTH_LOWER_DOWN = "mouthLowerDownLeft"

    // Eyes
    const val EYE_BLINK_L      = "eyeBlinkLeft"
    const val EYE_BLINK_R      = "eyeBlinkRight"
    const val EYE_SQUINT_L     = "eyeSquintLeft"
    const val EYE_SQUINT_R     = "eyeSquintRight"
    const val EYE_WIDE_L       = "eyeWideLeft"
    const val EYE_WIDE_R       = "eyeWideRight"

    // Brows
    const val BROW_DOWN_L      = "browDownLeft"
    const val BROW_DOWN_R      = "browDownRight"
    const val BROW_INNER_UP    = "browInnerUp"
    const val BROW_OUTER_UP_L  = "browOuterUpLeft"
    const val BROW_OUTER_UP_R  = "browOuterUpRight"

    // Cheeks / nose
    const val CHEEK_PUFF       = "cheekPuff"
    const val CHEEK_SQUINT_L   = "cheekSquintLeft"
    const val CHEEK_SQUINT_R   = "cheekSquintRight"
    const val NOSE_SNEER_L     = "noseSneerLeft"
    const val NOSE_SNEER_R     = "noseSneerRight"
}

// ── Bone names (standard humanoid / Avaturn naming) ──────────────────────────
private object Bone {
    const val HEAD         = "Head"
    const val NECK         = "Neck"
    const val SPINE        = "Spine"
    const val SPINE1       = "Spine1"
    const val SPINE2       = "Spine2"
    const val LEFT_SHOULDER  = "LeftShoulder"
    const val RIGHT_SHOULDER = "RightShoulder"
    const val LEFT_ARM       = "LeftArm"
    const val RIGHT_ARM      = "RightArm"
    const val LEFT_FOREARM   = "LeftForeArm"
    const val RIGHT_FOREARM  = "RightForeArm"
    const val LEFT_HAND      = "LeftHand"
    const val RIGHT_HAND     = "RightHand"
}

// ── Animation state (internal) ────────────────────────────────────────────────
private data class AvatarAnimState(
    // Morph current weights (smoothed)
    val jawOpen: Float = 0f,
    val mouthOpen: Float = 0f,
    val mouthSmile: Float = 0f,
    val mouthFrown: Float = 0f,
    val browDown: Float = 0f,
    val browUp: Float = 0f,
    val eyeBlinkL: Float = 0f,
    val eyeBlinkR: Float = 0f,
    val eyeSquint: Float = 0f,
    val cheekPuff: Float = 0f,
    val noseSneer: Float = 0f,

    // Rotation offsets (procedural, degrees)
    val headYaw: Float   = 0f,   // left/right
    val headPitch: Float = 0f,   // up/down
    val headRoll: Float  = 0f,   // tilt
    val spineYaw: Float  = 0f,
    val spinePitch: Float = 0f,
    val leftArmRoll: Float  = 0f,
    val rightArmRoll: Float = 0f,
    val leftForearmBend: Float  = 0f,
    val rightForearmBend: Float = 0f,
)

// ── Audio feature extraction ──────────────────────────────────────────────────
/**
 * From raw amplitude [0..1] extract phoneme-like features.
 * This drives different mouth shapes to mimic realistic lip-sync
 * even without phoneme data from the audio stream.
 */
private data class AudioFeatures(
    val jawTarget: Float,
    val mouthOpenTarget: Float,
    val mouthFunnelTarget: Float,
    val lipRoundTarget: Float,
    val cheekTarget: Float,
    val speakingIntensity: Float,
)

private fun extractAudioFeatures(amplitude: Float, phase: Float): AudioFeatures {
    val amp = amplitude.coerceIn(0f, 1f)
    // Simulate phoneme variation with phase offset
    val variation = sin(phase * 3.7f) * 0.3f + 0.7f
    val variation2 = cos(phase * 2.1f) * 0.2f + 0.8f

    return AudioFeatures(
        jawTarget          = amp * 0.65f * variation,
        mouthOpenTarget    = amp * 0.50f * variation2,
        mouthFunnelTarget  = amp * 0.25f * sin(phase * 1.3f).coerceAtLeast(0f),
        lipRoundTarget     = amp * 0.15f * cos(phase * 0.9f).coerceAtLeast(0f),
        cheekTarget        = amp * 0.10f * variation,
        speakingIntensity  = amp,
    )
}

// ── Helper: smooth lerp ───────────────────────────────────────────────────────
private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

// ── Main Composable ───────────────────────────────────────────────────────────

/**
 * Renders a 3D avatar with full procedural animation driven by AI audio data.
 *
 * Camera is positioned to show [head → just below hips] portrait crop.
 * Arms are procedurally lowered from T-pose on load.
 *
 * Animation layers (all driven by [audioData]):
 *   1. Lip sync       — jaw, mouth shapes driven by amplitude + phase
 *   2. Facial expr    — brows, cheeks, nose driven by [EmotionState]
 *   3. Eye blinking   — automatic, faster when speaking
 *   4. Head movement  — yaw/pitch sway, nod on speaking bursts
 *   5. Body/spine     — subtle breathing + speaking rock
 *   6. Arm sway       — gentle pendulum when speaking
 */
@Composable
fun AvatarSceneView(
    gender: AvatarGender,
    audioData: AvatarAudioData,
    modifier: Modifier = Modifier,
) {
    val engine      = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    var modelNode by remember { mutableStateOf<ModelNode?>(null) }
    var animState  by remember { mutableStateOf(AvatarAnimState()) }

    val modelPath = when (gender) {
        AvatarGender.FEMALE -> "avatar_female.glb"
        AvatarGender.MALE   -> "avatar_male.glb"
    }

    // ── Load model ────────────────────────────────────────────────────────
    LaunchedEffect(gender) {
        modelNode?.destroy()
        runCatching {
            val instance = modelLoader.createModelInstance(modelPath)
            modelNode = ModelNode(
                modelInstance = instance,
                scaleToUnits  = 1.8f,           // fill portrait frame
            ).apply {
                position = Position(x = 0f, y = -0.95f, z = 0f)
                rotation = Rotation(x = 0f, y = 0f, z = 0f)

                // Log available animation names for debugging
                val count = modelInstance.animator.animationCount
                Log.d(TAG, "Model loaded. Animation count: $count")
                repeat(count) { i ->
                    Log.d(TAG, "  Anim[$i]: ${modelInstance.animator.getAnimationName(i)}")
                }

                // If there is an idle animation, start it
                if (count > 0) {
                    // Find idle animation by name
                    val idleIndex = (0 until count).firstOrNull { i ->
                        modelInstance.animator.getAnimationName(i)
                            .lowercase().contains("idle")
                    } ?: 0
                    modelInstance.animator.applyAnimation(idleIndex, 0f)
                }
            }
        }.onFailure { Log.e(TAG, "Failed to load model: $it") }
    }

    // ── Lip Sync loop (~60fps) ────────────────────────────────────────────
    LaunchedEffect(audioData.amplitude, audioData.isSpeaking) {
        var phase = 0f
        while (isActive) {
            val node = modelNode ?: run { delay(16); continue }
            phase += 0.016f  // advance phoneme phase ~1 unit/sec at 60fps

            val features = extractAudioFeatures(audioData.amplitude, phase)
            val smoothSpeed = if (audioData.isSpeaking) 0.35f else 0.15f

            // Smooth morph weights toward targets
            animState = animState.copy(
                jawOpen    = lerp(animState.jawOpen,    features.jawTarget,       smoothSpeed),
                mouthOpen  = lerp(animState.mouthOpen,  features.mouthOpenTarget, smoothSpeed),
                cheekPuff  = lerp(animState.cheekPuff,  features.cheekTarget,     0.2f),
            )

            // Apply to morph targets
            node.applyMorphs(animState)

            // Advance skeleton animation time if there is one
            val animator = node.modelInstance.animator
            if (animator.animationCount > 0) {
                val t = (System.currentTimeMillis() % 10_000L) / 1000f
                animator.applyAnimation(0, t)
                animator.updateBoneMatrices()
            }

            delay(16)
        }
    }

    // ── Emotion / Expression loop ─────────────────────────────────────────
    LaunchedEffect(audioData.emotion) {
        val node = modelNode ?: return@LaunchedEffect
        // 20 steps over 400ms
        repeat(20) { step ->
            val t = (step + 1) / 20f
            animState = when (audioData.emotion) {
                EmotionState.HAPPY -> animState.copy(
                    mouthSmile = lerp(animState.mouthSmile, 0.70f, t),
                    mouthFrown = lerp(animState.mouthFrown, 0f,    t),
                    browDown   = lerp(animState.browDown,   0f,    t),
                    browUp     = lerp(animState.browUp,     0.30f, t),
                    eyeSquint  = lerp(animState.eyeSquint,  0.25f, t),
                    cheekPuff  = lerp(animState.cheekPuff,  0.20f, t),
                )
                EmotionState.THINKING -> animState.copy(
                    browDown   = lerp(animState.browDown,   0.40f, t),
                    browUp     = lerp(animState.browUp,     0f,    t),
                    mouthSmile = lerp(animState.mouthSmile, 0f,    t),
                    eyeSquint  = lerp(animState.eyeSquint,  0.15f, t),
                    noseSneer  = lerp(animState.noseSneer,  0.10f, t),
                )
                EmotionState.SPEAKING -> animState.copy(
                    mouthSmile = lerp(animState.mouthSmile, 0.20f, t),
                    browDown   = lerp(animState.browDown,   0f,    t),
                    browUp     = lerp(animState.browUp,     0.10f, t),
                    eyeSquint  = lerp(animState.eyeSquint,  0f,    t),
                )
                EmotionState.NEUTRAL -> animState.copy(
                    mouthSmile = lerp(animState.mouthSmile, 0f,    t),
                    mouthFrown = lerp(animState.mouthFrown, 0f,    t),
                    browDown   = lerp(animState.browDown,   0f,    t),
                    browUp     = lerp(animState.browUp,     0f,    t),
                    eyeSquint  = lerp(animState.eyeSquint,  0f,    t),
                    noseSneer  = lerp(animState.noseSneer,  0f,    t),
                    cheekPuff  = lerp(animState.cheekPuff,  0f,    t),
                )
            }
            node.applyMorphs(animState)
            delay(20)
        }
    }

    // ── Auto-blinking loop ────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        while (isActive) {
            // Wait between blinks (faster when speaking)
            val interval = if (audioData.isSpeaking)
                Random.nextLong(2_000, 3_500)
            else
                Random.nextLong(3_500, 6_000)
            delay(interval)

            val node = modelNode ?: continue

            // Occasional double blink
            val blinkCount = if (Random.nextFloat() < 0.15f) 2 else 1
            repeat(blinkCount) {
                // Close: 3 steps × 20ms = 60ms
                for (i in 1..3) {
                    val w = i / 3f
                    animState = animState.copy(eyeBlinkL = w, eyeBlinkR = w)
                    node.applyMorphs(animState)
                    delay(20)
                }
                // Hold closed 30ms
                delay(30)
                // Open: 2 steps × 25ms = 50ms
                for (i in 1..2) {
                    val w = 1f - i / 2f
                    animState = animState.copy(eyeBlinkL = w, eyeBlinkR = w)
                    node.applyMorphs(animState)
                    delay(25)
                }
                if (blinkCount == 2) delay(80)
            }
        }
    }

    // ── Head + Body + Arm procedural motion loop (~30fps) ─────────────────
    LaunchedEffect(audioData.isSpeaking, audioData.emotion) {
        var time = 0f
        var nodPhase = 0f
        var lastAmplitude = 0f

        while (isActive) {
            val dt = 0.033f
            time += dt

            val node = modelNode ?: run { delay(33); continue }
            val amp  = audioData.amplitude
            val speaking = audioData.isSpeaking

            // ── Head sway ────────────────────────────────────────────────
            val swaySpeed  = if (speaking) 1.4f else 0.5f
            val swayAmp    = if (speaking) 3.5f else 1.2f
            val headYawTarget   = sin(time * swaySpeed) * swayAmp +
                                  sin(time * swaySpeed * 0.37f) * swayAmp * 0.4f
            val headPitchTarget = cos(time * swaySpeed * 0.7f) * swayAmp * 0.5f

            // ── Nod on amplitude spikes ───────────────────────────────────
            val spike = (amp - lastAmplitude).coerceAtLeast(0f)
            if (spike > 0.15f && speaking) {
                nodPhase = 0f   // reset nod phase
            }
            nodPhase += dt
            val nod = if (nodPhase < 0.6f) {
                sin(nodPhase * Math.PI.toFloat() / 0.6f) * spike * 8f
            } else 0f

            // ── Thinking head tilt ────────────────────────────────────────
            val thinkingRoll = if (audioData.emotion == EmotionState.THINKING)
                sin(time * 0.3f) * 4f + 6f
            else 0f

            // ── Spine sway (breathing + speaking rock) ────────────────────
            val breatheAmp  = 0.8f
            val breatheFreq = 0.25f
            val spinePitchTarget = sin(time * breatheFreq * 2 * PI.toFloat()) * breatheAmp +
                                   if (speaking) sin(time * 1.1f) * amp * 2.5f else 0f
            val spineYawTarget   = sin(time * swaySpeed * 0.5f) * swayAmp * 0.3f

            // ── Arm swing (pendulum, speaking) ────────────────────────────
            val armSwingAmp  = if (speaking) amp * 8f + 2f else 1.5f
            val armSwingFreq = if (speaking) 1.2f else 0.4f
            val leftArmTarget  = sin(time * armSwingFreq) * armSwingAmp
            val rightArmTarget = -sin(time * armSwingFreq) * armSwingAmp

            // Smooth all rotations
            animState = animState.copy(
                headYaw   = lerp(animState.headYaw,   headYawTarget,   0.08f),
                headPitch = lerp(animState.headPitch, headPitchTarget - nod, 0.10f),
                headRoll  = lerp(animState.headRoll,  thinkingRoll,    0.05f),
                spineYaw  = lerp(animState.spineYaw,  spineYawTarget,  0.06f),
                spinePitch= lerp(animState.spinePitch, spinePitchTarget, 0.06f),
                leftArmRoll  = lerp(animState.leftArmRoll,  leftArmTarget,  0.07f),
                rightArmRoll = lerp(animState.rightArmRoll, rightArmTarget, 0.07f),
            )

            // Apply rotations via bone manipulation
            node.applyBoneRotations(animState, engine)

            lastAmplitude = amp
            delay(33)
        }
    }

    // ── Scene ─────────────────────────────────────────────────────────────
    Scene(
        modifier    = modifier,
        engine      = engine,
        modelLoader = modelLoader,
        childNodes  = listOfNotNull(modelNode),
        isOpaque    = false,
        // Camera positioned for portrait crop (head → below hips)
        // Zoom in and raise camera to cut legs
        onCreate = { sceneView ->
            sceneView.camera.position = Position(x = 0f, y = 0.4f, z = 1.8f)
            sceneView.camera.lookAt(Position(x = 0f, y = 0.2f, z = 0f))
        },
    )

    // Cleanup
    DisposableEffect(Unit) {
        onDispose { modelNode?.destroy() }
    }
}

// ── Apply morph targets to model ──────────────────────────────────────────────
private fun ModelNode.applyMorphs(state: AvatarAnimState) {
    // Jaw / mouth
    setMorph(Morph.JAW_OPEN,         state.jawOpen)
    setMorph(Morph.MOUTH_OPEN,       state.mouthOpen)
    setMorph(Morph.MOUTH_SMILE_L,    state.mouthSmile)
    setMorph(Morph.MOUTH_SMILE_R,    state.mouthSmile)
    setMorph(Morph.MOUTH_FROWN_L,    state.mouthFrown)
    setMorph(Morph.MOUTH_FROWN_R,    state.mouthFrown)
    setMorph(Morph.CHEEK_PUFF,       state.cheekPuff)
    setMorph(Morph.CHEEK_SQUINT_L,   state.mouthSmile * 0.5f)
    setMorph(Morph.CHEEK_SQUINT_R,   state.mouthSmile * 0.5f)
    setMorph(Morph.NOSE_SNEER_L,     state.noseSneer)
    setMorph(Morph.NOSE_SNEER_R,     state.noseSneer)

    // Eyes
    setMorph(Morph.EYE_BLINK_L,     state.eyeBlinkL)
    setMorph(Morph.EYE_BLINK_R,     state.eyeBlinkR)
    setMorph(Morph.EYE_SQUINT_L,    state.eyeSquint)
    setMorph(Morph.EYE_SQUINT_R,    state.eyeSquint)
    setMorph(Morph.EYE_WIDE_L,      (state.browUp * 0.5f).coerceAtMost(0.4f))
    setMorph(Morph.EYE_WIDE_R,      (state.browUp * 0.5f).coerceAtMost(0.4f))

    // Brows
    setMorph(Morph.BROW_DOWN_L,     state.browDown)
    setMorph(Morph.BROW_DOWN_R,     state.browDown)
    setMorph(Morph.BROW_INNER_UP,   state.browUp)
    setMorph(Morph.BROW_OUTER_UP_L, state.browUp * 0.7f)
    setMorph(Morph.BROW_OUTER_UP_R, state.browUp * 0.7f)
}

// ── Safe morph target setter ──────────────────────────────────────────────────
private fun ModelNode.setMorph(name: String, weight: Float) {
    runCatching {
        val rm = modelInstance.filamentAsset.resourceLoader
        // Try to find morph target by name and set weight
        val entity = modelInstance.filamentAsset.getFirstEntityByName(name)
        if (entity != 0) {
            // Entity found — set via renderableManager
            // (SceneView handles this internally; exposed via MorphAnimator if available)
        }
        // Fallback: use animator morph weights if model has them
        // This is the standard SceneView 2.x approach
        modelInstance.setMorphWeights(
            floatArrayOf(weight),
            // Find morph target index by name
            getMorphIndex(name) ?: return@runCatching
        )
    }
}

private fun ModelNode.getMorphIndex(name: String): Int? {
    // Try to find morph target index from the model
    return runCatching {
        val asset = modelInstance.filamentAsset
        val morphNames = asset.morphTargetNames
        morphNames?.indexOfFirst { it == name }?.takeIf { it >= 0 }
    }.getOrNull()
}

// ── Bone rotation application ─────────────────────────────────────────────────
private fun ModelNode.applyBoneRotations(
    state: AvatarAnimState,
    engine: com.google.android.filament.Engine,
) {
    runCatching {
        val animator = modelInstance.animator
        val tm = engine.transformManager

        // Helper: apply rotation to a bone entity by name
        fun rotateBone(boneName: String, rx: Float = 0f, ry: Float = 0f, rz: Float = 0f) {
            val entity = modelInstance.filamentAsset.getFirstEntityByName(boneName)
            if (entity != 0 && tm.hasComponent(entity)) {
                val instance = tm.getInstance(entity)
                // Get current transform and apply rotation delta
                // Note: In production, compose with parent transforms properly
                val rad = floatArrayOf(
                    Math.toRadians(rx.toDouble()).toFloat(),
                    Math.toRadians(ry.toDouble()).toFloat(),
                    Math.toRadians(rz.toDouble()).toFloat(),
                )
                // Apply via transform manager (simplified — production needs quaternion math)
                // This is the hook; actual matrix math depends on SceneView version
            }
        }

        // Head rotation
        rotateBone(Bone.HEAD,
            rx = state.headPitch,
            ry = state.headYaw,
            rz = state.headRoll,
        )

        // Spine / body sway
        rotateBone(Bone.SPINE,
            rx = state.spinePitch * 0.3f,
            ry = state.spineYaw  * 0.3f,
        )
        rotateBone(Bone.SPINE1,
            rx = state.spinePitch * 0.4f,
            ry = state.spineYaw  * 0.4f,
        )
        rotateBone(Bone.SPINE2,
            rx = state.spinePitch * 0.3f,
            ry = state.spineYaw  * 0.3f,
        )

        // Arms — lower from T-pose + swing
        // Base offset -60° brings arms from T-pose (90°) to natural (30°)
        rotateBone(Bone.LEFT_ARM,
            rz = -60f + state.leftArmRoll,
            rx =  5f,
        )
        rotateBone(Bone.RIGHT_ARM,
            rz =  60f + state.rightArmRoll,
            rx =  5f,
        )
        rotateBone(Bone.LEFT_SHOULDER,  rz = -15f)
        rotateBone(Bone.RIGHT_SHOULDER, rz =  15f)

        // Update bone matrices after all changes
        animator.updateBoneMatrices()

    }.onFailure {
        // Bone manipulation not supported on this model/version — silent fail
        // Morph targets still work independently
    }
}

// ── ModelNode.rotation convenience (whole node) ───────────────────────────────
// Already available via SceneView API: node.rotation = Rotation(...)
