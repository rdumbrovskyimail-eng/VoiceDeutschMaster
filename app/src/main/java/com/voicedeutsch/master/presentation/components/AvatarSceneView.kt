package com.voicedeutsch.master.presentation.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.voicedeutsch.master.voicecore.engine.AvatarAudioData
import com.voicedeutsch.master.voicecore.engine.AvatarGender
import com.voicedeutsch.master.voicecore.engine.EmotionState
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.*
import kotlin.random.Random

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

private data class AnimState(
    val headYaw: Float   = 0f,
    val headPitch: Float = 0f,
    val headRoll: Float  = 0f,
    val bodyYaw: Float   = 0f,
)

@Composable
fun AvatarSceneView(
    gender: AvatarGender,
    audioData: AvatarAudioData,
    modifier: Modifier = Modifier,
) {
    val engine      = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    var modelNode   by remember { mutableStateOf<ModelNode?>(null) }
    var animState   by remember { mutableStateOf(AnimState()) }
    val context     = androidx.compose.ui.platform.LocalContext.current

    val modelPath = when (gender) {
        AvatarGender.FEMALE -> "avatar_female.glb"
        AvatarGender.MALE   -> "avatar_male.glb"
    }

    // Load model
    LaunchedEffect(gender) {
        modelNode?.destroy()
        modelNode = null
        runCatching {
            val instance = modelLoader.createModelInstance(modelPath)
            val node = ModelNode(
                modelInstance = instance,
                scaleToUnits  = 1.8f,
            ).apply {
                position = Position(x = 0f, y = -0.95f, z = 0f)
                val animator = modelInstance.animator
                if (animator.animationCount > 0) {
                    val idleIdx = (0 until animator.animationCount).firstOrNull { i ->
                        animator.getAnimationName(i).lowercase().contains("idle")
                    } ?: 0
                    runCatching {
                        animator.applyAnimation(idleIdx, 0f)
                        animator.updateBoneMatrices()
                    }
                }
            }

            // Записываем имена костей в файл для отладки
            runCatching {
                val asset = node.modelInstance.asset
                val sb = StringBuilder()
                sb.appendLine("=== BONES / ENTITIES ===")
                asset.entities.forEach { entity ->
                    val name = asset.getName(entity)
                    if (name != null) sb.appendLine(name)
                }
                context.openFileOutput("avatar_debug.txt", android.content.Context.MODE_PRIVATE)
                    .use { it.write(sb.toString().toByteArray()) }
                android.util.Log.d("BONES", sb.toString())
            }

            modelNode = node
        }.onFailure { e ->
            android.util.Log.e("AvatarSceneView", "Model load failed: ${e.message}")
        }
    }

    // Skeleton animation ticker
    LaunchedEffect(Unit) {
        val startMs = System.currentTimeMillis()
        while (isActive) {
            val node = modelNode ?: run { delay(16); continue }
            val animator = node.modelInstance.animator
            if (animator.animationCount > 0) {
                val elapsed  = (System.currentTimeMillis() - startMs) / 1000f
                val duration = animator.getAnimationDuration(0)
                val t = if (duration > 0f) elapsed % duration else 0f
                animator.applyAnimation(0, t)
                animator.updateBoneMatrices()
            }
            delay(16)
        }
    }

    // Procedural head + body motion
    LaunchedEffect(audioData.isSpeaking, audioData.emotion) {
        var time     = 0f
        var nodPhase = 0f
        var lastAmp  = 0f

        while (isActive) {
            val dt = 0.033f
            time += dt

            val node     = modelNode ?: run { delay(33); continue }
            val amp      = audioData.amplitude.coerceIn(0f, 1f)
            val speaking = audioData.isSpeaking

            val swaySpeed = if (speaking) 1.4f else 0.5f
            val swayAmp   = if (speaking) 4.0f else 1.5f

            val yawTarget   = sin(time * swaySpeed) * swayAmp +
                              sin(time * swaySpeed * 0.4f) * swayAmp * 0.35f
            val pitchTarget = cos(time * swaySpeed * 0.7f) * swayAmp * 0.4f

            val spike = (amp - lastAmp).coerceAtLeast(0f)
            if (spike > 0.15f && speaking) nodPhase = 0f
            nodPhase += dt
            val nod = if (nodPhase < 0.5f)
                sin(nodPhase * PI.toFloat() / 0.5f) * spike * 10f
            else 0f

            val rollTarget = if (audioData.emotion == EmotionState.THINKING)
                sin(time * 0.3f) * 3f + 5f
            else 0f

            val bodyYawTarget = sin(time * swaySpeed * 0.5f) * swayAmp * 0.25f +
                                if (speaking) sin(time * 1.0f) * amp * 2f else 0f

            animState = animState.copy(
                headYaw   = lerp(animState.headYaw,   yawTarget,         0.08f),
                headPitch = lerp(animState.headPitch, pitchTarget - nod, 0.10f),
                headRoll  = lerp(animState.headRoll,  rollTarget,        0.05f),
                bodyYaw   = lerp(animState.bodyYaw,   bodyYawTarget,     0.06f),
            )

            node.rotation = Rotation(
                x = animState.headPitch * 0.4f,
                y = animState.headYaw   * 0.5f + animState.bodyYaw,
                z = animState.headRoll  * 0.3f,
            )

            lastAmp = amp
            delay(33)
        }
    }

    // Auto-blink: subtle scale pulse
    LaunchedEffect(Unit) {
        while (isActive) {
            val interval = if (audioData.isSpeaking)
                Random.nextLong(2_000, 3_500)
            else
                Random.nextLong(3_500, 6_000)
            delay(interval)
            val node = modelNode ?: continue
            for (i in 1..3) {
                node.scale = Scale(1f, 1f - i * 0.003f, 1f)
                delay(20)
            }
            delay(25)
            for (i in 1..2) {
                node.scale = Scale(1f, 0.991f + i * 0.003f, 1f)
                delay(20)
            }
            node.scale = Scale(1f, 1f, 1f)
        }
    }

    Scene(
        modifier    = modifier,
        engine      = engine,
        modelLoader = modelLoader,
        childNodes  = listOfNotNull(modelNode),
        isOpaque    = false,
    )

    DisposableEffect(Unit) {
        onDispose { modelNode?.destroy() }
    }
}
