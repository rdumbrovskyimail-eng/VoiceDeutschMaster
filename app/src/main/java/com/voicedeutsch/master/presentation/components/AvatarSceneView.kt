package com.voicedeutsch.master.presentation.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.voicedeutsch.master.voicecore.engine.AvatarAudioData
import com.voicedeutsch.master.voicecore.engine.AvatarGender
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AvatarSceneView(
    gender: AvatarGender,
    audioData: AvatarAudioData,
    modifier: Modifier = Modifier,
) {
    val engine      = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    var modelNode   by remember { mutableStateOf<ModelNode?>(null) }

    val boneCtrl  = remember { BoneController(engine) }
    val morphCtrl = remember { MorphTargetHelper(engine) }
    val behavior  = remember { AvatarBehaviorEngine() }

    val currentAudio = rememberUpdatedState(audioData)

    val modelPath = when (gender) {
        AvatarGender.FEMALE -> "avatar_female.glb"
        AvatarGender.MALE   -> "avatar_male.glb"
    }

    // ── Загрузка модели ────────────────────────────────────────────────────
    LaunchedEffect(gender) {
        modelNode?.destroy()
        boneCtrl.clear()
        morphCtrl.clear()
        behavior.reset()
        modelNode = null

        runCatching {
            val instance = modelLoader.createModelInstance(modelPath)
            val node = ModelNode(modelInstance = instance, scaleToUnits = 1.8f).apply {
                position = Position(x = 0f, y = -0.95f, z = 0f)
            }
            boneCtrl.init(node)
            morphCtrl.init(node)
            modelNode = node
        }.onFailure { e ->
            android.util.Log.e("AvatarSceneView", "Model load failed: ${e.message}")
        }
    }

    // ── Главный цикл анимации (30fps) ─────────────────────────────────────
    LaunchedEffect(Unit) {
        var lastMs = System.currentTimeMillis()
        while (isActive) {
            val now = System.currentTimeMillis()
            val dt = ((now - lastMs) / 1000f).coerceIn(0.01f, 0.1f)
            lastMs = now

            if (boneCtrl.isReady() && morphCtrl.isReady()) {
                val frame = behavior.update(currentAudio.value, dt)
                applyFrame(frame, boneCtrl, morphCtrl)
            }
            delay(33L)
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
        onDispose {
            boneCtrl.clear()
            morphCtrl.clear()
            modelNode?.destroy()
        }
    }
}

/** Применяет AvatarFrame к контроллерам костей и морфов. */
private fun applyFrame(
    frame: AvatarBehaviorEngine.AvatarFrame,
    bones: BoneController,
    morphs: MorphTargetHelper,
) {
    with(frame) {
        bones.rotate("Head",         head.pitch,         head.yaw,         head.roll)
        bones.rotate("Neck",         neck.pitch,         neck.yaw,         neck.roll)
        bones.rotate("Spine",        spine.pitch,        spine.yaw,        spine.roll)
        bones.rotate("Spine1",       spine1.pitch,       spine1.yaw,       spine1.roll)
        bones.rotate("Spine2",       spine2.pitch,       spine2.yaw,       spine2.roll)
        bones.rotate("LeftShoulder", leftShoulder.pitch, leftShoulder.yaw, leftShoulder.roll)
        bones.rotate("RightShoulder",rightShoulder.pitch,rightShoulder.yaw,rightShoulder.roll)
        bones.rotate("LeftArm",      leftArm.pitch,      leftArm.yaw,      leftArm.roll)
        bones.rotate("LeftForeArm",  leftForeArm.pitch,  leftForeArm.yaw,  leftForeArm.roll)
        bones.rotate("LeftHand",     leftHand.pitch,     leftHand.yaw,     leftHand.roll)
        bones.rotate("RightArm",     rightArm.pitch,     rightArm.yaw,     rightArm.roll)
        bones.rotate("RightForeArm", rightForeArm.pitch, rightForeArm.yaw, rightForeArm.roll)
        bones.rotate("RightHand",    rightHand.pitch,    rightHand.yaw,    rightHand.roll)
    }
    morphs.setWeights(frame.morphs)
}