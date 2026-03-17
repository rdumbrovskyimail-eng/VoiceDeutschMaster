package com.voicedeutsch.master.presentation.components

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.voicedeutsch.master.voicecore.engine.AvatarAudioData
import com.voicedeutsch.master.voicecore.engine.AvatarGender
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val TAG = "AvatarSceneView"

@Composable
fun AvatarSceneView(
    gender: AvatarGender,
    audioData: AvatarAudioData,
    modifier: Modifier = Modifier,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    var modelNode by remember { mutableStateOf<ModelNode?>(null) }

    val boneCtrl = remember { BoneController(engine) }
    val morphCtrl = remember { MorphTargetHelper(engine) }
    val behavior = remember { AvatarBehaviorEngine() }

    // ✅ Флаг остановки — устанавливается ДО уничтожения ресурсов
    val isDisposed = remember { mutableStateOf(false) }

    val currentAudio = rememberUpdatedState(audioData)

    val modelPath = when (gender) {
        AvatarGender.FEMALE -> "avatar_female.glb"
        AvatarGender.MALE -> "avatar_male.glb"
    }

    // ── Model loading ──────────────────────────────────────────────────
    LaunchedEffect(gender) {
        if (isDisposed.value) return@LaunchedEffect // ✅ guard
        modelNode?.destroy()
        boneCtrl.clear()
        morphCtrl.clear()
        behavior.reset()
        modelNode = null

        runCatching {
            val instance = modelLoader.createModelInstance(modelPath)
            val node = ModelNode(modelInstance = instance, scaleToUnits = 1.8f).apply {
                position = Position(x = 0f, y = -0.95f, z = 0f)
                rotation = Rotation(x = 0f, y = 0f, z = 0f)
            }
            boneCtrl.init(node)
            morphCtrl.init(node)
            Log.d(TAG, "Bones found: ${boneCtrl.getDiscoveredBones()}")
            Log.d(TAG, "Morph targets found: ${morphCtrl.getAvailableNames()}")
            if (!boneCtrl.isReady()) Log.w(TAG, "⚠ No bones found!")
            if (!morphCtrl.isReady()) Log.w(TAG, "⚠ No morph targets found!")
            modelNode = node
            Log.d(TAG, "Model loaded successfully")
        }.onFailure { e ->
            Log.e(TAG, "Model load failed: ${e.message}", e)
        }
    }

    // ── Animation loop (30fps) ────────────────────────────────────────
    LaunchedEffect(modelNode) {
        val node = modelNode ?: return@LaunchedEffect
        var lastMs = System.currentTimeMillis()
        var animFrameCount = 0
        var lastAnimLogMs = System.currentTimeMillis()

        while (isActive && !isDisposed.value) {
            val now = System.currentTimeMillis()
            val dt = ((now - lastMs) / 1000f).coerceIn(0.008f, 0.1f)
            lastMs = now

            if (boneCtrl.isReady() || morphCtrl.isReady()) {
                if (isDisposed.value) break
                runCatching {
                    val audio = currentAudio.value
                    val frame = behavior.update(audio, dt)
                    applyFrame(frame, boneCtrl, morphCtrl)

                    // ✅ КРИТИЧНО: Принудительно инвалидируем SceneView.
                    // BoneController и MorphTargetHelper меняют Filament entities
                    // через TransformManager/RenderableManager напрямую,
                    // но SceneView об этом не знает и не перерисовывает.
                    // Присвоение position себе же триггерит node.onChanged → requestRender.
                    node.position = node.position

                    animFrameCount++

                    // Диагностика каждые 3 секунды
                    if (now - lastAnimLogMs > 3000L) {
                        val headP = frame.head.pitch
                        val jawOpen = frame.morphs["jawOpen"] ?: 0f
                        val smile = frame.morphs["mouthSmile"] ?: 0f
                        Log.d(TAG, "🦴 Anim: frames=$animFrameCount, " +
                            "headPitch=${"%.1f".format(headP)}, " +
                            "jaw=${"%.3f".format(jawOpen)}, smile=${"%.3f".format(smile)}, " +
                            "audioAmp=${"%.3f".format(audio.amplitude)}, " +
                            "speaking=${audio.isSpeaking}, emotion=${audio.emotion}")
                        lastAnimLogMs = now
                    }
                }.onFailure { e ->
                    if (!isDisposed.value) Log.w(TAG, "Frame apply failed: ${e.message}")
                    break
                }
            }

            delay(33L)
        }
    }

    // ── Scene rendering ────────────────────────────────────────────────
    Scene(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        childNodes = listOfNotNull(modelNode),
        isOpaque = false,
        // ✅ FIX: Включаем постоянный рендер — иначе Filament не обновляет
        //    трансформы костей/морфов без явного запроса нового кадра
        onFrame = { /* no-op, но наличие callback включает continuous render */ },
    )

    // ── Cleanup ────────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "⏹ DisposableEffect onDispose — начинаю cleanup")

            // 1) Поднимаем флаг — останавливаем анимационный цикл
            isDisposed.value = true

            // 2) Очищаем контроллеры (убираем ссылки на Filament entities)
            boneCtrl.clear()
            morphCtrl.clear()

            // 3) Уничтожаем node — engine ещё жив на этом этапе
            runCatching {
                modelNode?.destroy()
            }.onFailure { e ->
                Log.w(TAG, "modelNode.destroy() failed: ${e.message}")
            }
            modelNode = null

            Log.d(TAG, "⏹ Cleanup завершён")
        }
    }
}

/** Applies AvatarFrame to bone and morph controllers. */
private fun applyFrame(
    frame: AvatarBehaviorEngine.AvatarFrame,
    bones: BoneController,
    morphs: MorphTargetHelper,
) {
    with(frame) {
        // Apply bone rotations (silently skips if bone not found)
        bones.rotate("Head", head.pitch, head.yaw, head.roll)
        bones.rotate("Neck", neck.pitch, neck.yaw, neck.roll)
        bones.rotate("Spine", spine.pitch, spine.yaw, spine.roll)
        bones.rotate("Spine1", spine1.pitch, spine1.yaw, spine1.roll)
        bones.rotate("Spine2", spine2.pitch, spine2.yaw, spine2.roll)
        bones.rotate("LeftShoulder", leftShoulder.pitch, leftShoulder.yaw, leftShoulder.roll)
        bones.rotate("RightShoulder", rightShoulder.pitch, rightShoulder.yaw, rightShoulder.roll)
        bones.rotate("LeftArm", leftArm.pitch, leftArm.yaw, leftArm.roll)
        bones.rotate("LeftForeArm", leftForeArm.pitch, leftForeArm.yaw, leftForeArm.roll)
        bones.rotate("LeftHand", leftHand.pitch, leftHand.yaw, leftHand.roll)
        bones.rotate("RightArm", rightArm.pitch, rightArm.yaw, rightArm.roll)
        bones.rotate("RightForeArm", rightForeArm.pitch, rightForeArm.yaw, rightForeArm.roll)
        bones.rotate("RightHand", rightHand.pitch, rightHand.yaw, rightHand.roll)
    }

    // Apply morph targets (silently skips unknown names)
    morphs.setWeights(frame.morphs)
}