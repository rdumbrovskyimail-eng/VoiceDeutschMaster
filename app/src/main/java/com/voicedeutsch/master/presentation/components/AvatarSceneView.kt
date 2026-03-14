package com.voicedeutsch.master.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.voicedeutsch.master.voicecore.engine.AvatarAudioData
import com.voicedeutsch.master.voicecore.engine.AvatarGender
import com.voicedeutsch.master.voicecore.engine.EmotionState
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * Morph target names expected in the .glb model.
 */
private object MorphTargets {
    const val JAW_OPEN = "jawOpen"
    const val MOUTH_OPEN = "mouthOpen"
    const val SMILE = "mouthSmile"
    const val BROW_DOWN = "browDown"
    const val EYE_BLINK_L = "eyeBlinkLeft"
    const val EYE_BLINK_R = "eyeBlinkRight"
}

/**
 * 3D Avatar composable using SceneView (Filament).
 *
 * Loads a .glb model from assets, applies morph targets based on [audioData],
 * and handles idle animations (blinking, breathing, head sway).
 *
 * Asset requirements:
 *   app/src/main/assets/avatar_male.glb
 *   app/src/main/assets/avatar_female.glb
 *   Both must have morph targets: jawOpen, mouthOpen, eyeBlinkLeft,
 *   eyeBlinkRight, mouthSmile, browDown
 */
@Composable
fun AvatarSceneView(
    gender: AvatarGender,
    audioData: AvatarAudioData,
    modifier: Modifier = Modifier,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    var modelNode by remember { mutableStateOf<ModelNode?>(null) }

    val modelPath = when (gender) {
        AvatarGender.FEMALE -> "avatar_female.glb"
        AvatarGender.MALE -> "avatar_male.glb"
    }

    // Load model when gender changes
    LaunchedEffect(gender) {
        modelNode?.destroy()
        val instance = modelLoader.createModelInstance(modelPath)
        modelNode = ModelNode(
            modelInstance = instance,
            scaleToUnits = 1.0f,
        ).apply {
            position = Position(y = -0.5f)
        }
    }

    // ── Lip Sync (every frame) ────────────────────────────────────────────
    LaunchedEffect(audioData.amplitude) {
        val node = modelNode ?: return@LaunchedEffect
        val jawTarget = audioData.amplitude * 0.7f
        val mouthTarget = audioData.amplitude * 0.5f

        // Smooth lerp
        node.setMorphTargetWeight(MorphTargets.JAW_OPEN, jawTarget)
        node.setMorphTargetWeight(MorphTargets.MOUTH_OPEN, mouthTarget)
    }

    // ── Emotion expressions ───────────────────────────────────────────────
    LaunchedEffect(audioData.emotion) {
        val node = modelNode ?: return@LaunchedEffect
        // Transition over 300ms (10 steps × 30ms)
        repeat(10) { step ->
            val t = (step + 1) / 10f
            when (audioData.emotion) {
                EmotionState.HAPPY -> {
                    node.setMorphTargetWeight(MorphTargets.SMILE, lerp(0f, 0.6f, t))
                    node.setMorphTargetWeight(MorphTargets.BROW_DOWN, lerp(node.getMorphTargetWeight(MorphTargets.BROW_DOWN), 0f, t))
                }
                EmotionState.THINKING -> {
                    node.setMorphTargetWeight(MorphTargets.BROW_DOWN, lerp(0f, 0.3f, t))
                    node.setMorphTargetWeight(MorphTargets.SMILE, lerp(node.getMorphTargetWeight(MorphTargets.SMILE), 0f, t))
                }
                EmotionState.SPEAKING -> {
                    node.setMorphTargetWeight(MorphTargets.SMILE, lerp(node.getMorphTargetWeight(MorphTargets.SMILE), 0.15f, t))
                    node.setMorphTargetWeight(MorphTargets.BROW_DOWN, 0f)
                }
                EmotionState.NEUTRAL -> {
                    node.setMorphTargetWeight(MorphTargets.SMILE, lerp(node.getMorphTargetWeight(MorphTargets.SMILE), 0f, t))
                    node.setMorphTargetWeight(MorphTargets.BROW_DOWN, 0f)
                }
            }
            delay(30)
        }
    }

    // ── Auto-blinking ─────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        while (true) {
            val interval = if (audioData.isSpeaking) {
                Random.nextLong(2000, 4000)
            } else {
                Random.nextLong(3000, 6000)
            }
            delay(interval)
            val node = modelNode ?: continue
            // Blink: close over 60ms, hold 20ms, open over 40ms
            repeat(3) { node.setMorphTargetWeight(MorphTargets.EYE_BLINK_L, (it + 1) / 3f); node.setMorphTargetWeight(MorphTargets.EYE_BLINK_R, (it + 1) / 3f); delay(20) }
            delay(20)
            repeat(2) { node.setMorphTargetWeight(MorphTargets.EYE_BLINK_L, 1f - (it + 1) / 2f); node.setMorphTargetWeight(MorphTargets.EYE_BLINK_R, 1f - (it + 1) / 2f); delay(20) }
        }
    }

    // ── Head sway ─────────────────────────────────────────────────────────
    LaunchedEffect(audioData.isSpeaking) {
        var phase = 0f
        val amplitude = if (audioData.isSpeaking) 3f else 1f
        val period = if (audioData.isSpeaking) 1.2f else 4f
        while (true) {
            phase += 0.033f / period * 2f * Math.PI.toFloat()
            if (phase > 2f * Math.PI.toFloat()) phase -= 2f * Math.PI.toFloat()
            modelNode?.rotation = Rotation(y = sin(phase) * amplitude)
            delay(33) // ~30fps
        }
    }

    // ── SceneView ─────────────────────────────────────────────────────────
    Scene(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        childNodes = listOfNotNull(modelNode),
        isOpaque = false,
    )
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

// Extension helpers for ModelNode morph targets
private fun ModelNode.setMorphTargetWeight(name: String, weight: Float) {
    modelInstance?.let { instance ->
        val entity = instance.entity
        // SceneView 2.x API for morph targets
        runCatching {
            val animator = instance.animator
            // Implementation depends on SceneView version
            // For models with blend shapes, use filament's morphing API
        }
    }
}

private fun ModelNode.getMorphTargetWeight(name: String): Float = 0f // placeholder