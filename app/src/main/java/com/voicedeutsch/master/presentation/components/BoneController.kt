package com.voicedeutsch.master.presentation.components

import com.google.android.filament.Engine
import io.github.sceneview.node.ModelNode
import kotlin.math.*

/**
 * Управляет поворотом костей скелета через Filament TransformManager.
 * Сохраняет bind-pose каждой кости и применяет дельта-вращение поверх него.
 * Это ключевой класс для процедурной анимации — позволяет поворачивать
 * любую кость скелета по имени, сохраняя оригинальную позу T-pose.
 */
class BoneController(private val engine: Engine) {

    data class BoneState(
        val entity: Int,
        val bindTransform: FloatArray  // 16 float, column-major 4×4
    )

    private val bones = mutableMapOf<String, BoneState>()

    /** Сканирует все bones в модели, запоминает их bind-pose трансформы. */
    fun init(node: ModelNode) {
        bones.clear()
        val asset = node.modelInstance.asset
        val tm = engine.transformManager
        asset.entities.forEach { entity ->
            val name = asset.getName(entity) ?: return@forEach
            if (!tm.hasComponent(entity)) return@forEach
            val inst = tm.getInstance(entity)
            val mat = FloatArray(16)
            tm.getTransform(inst, mat)
            bones[name] = BoneState(entity, mat.copyOf())
        }
    }

    /**
     * Поворачивает кость по имени, накладывая вращение поверх bind-pose.
     * Euler XYZ: pitchDeg — вокруг X (кивок), yawDeg — вокруг Y (поворот),
     * rollDeg — вокруг Z (наклон).
     */
    fun rotate(name: String, pitchDeg: Float, yawDeg: Float, rollDeg: Float) {
        val state = bones[name] ?: return
        val tm = engine.transformManager
        if (!tm.hasComponent(state.entity)) return
        val inst = tm.getInstance(state.entity)

        val p = pitchDeg * PI.toFloat() / 180f
        val y = yawDeg  * PI.toFloat() / 180f
        val r = rollDeg * PI.toFloat() / 180f

        // Quaternion из Euler XYZ (intrinsic)
        val cp = cos(p/2f); val sp = sin(p/2f)
        val cy = cos(y/2f); val sy = sin(y/2f)
        val cr = cos(r/2f); val sr = sin(r/2f)
        val qw = cr*cp*cy + sr*sp*sy
        val qx = sr*cp*cy - cr*sp*sy
        val qy = cr*sp*cy + sr*cp*sy
        val qz = cr*cp*sy - sr*sp*cy

        // Матрица вращения (column-major)
        val d = FloatArray(16)
        d[ 0] = 1f-2f*(qy*qy+qz*qz); d[ 1] = 2f*(qx*qy+qz*qw); d[ 2] = 2f*(qx*qz-qy*qw); d[ 3] = 0f
        d[ 4] = 2f*(qx*qy-qz*qw); d[ 5] = 1f-2f*(qx*qx+qz*qz); d[ 6] = 2f*(qy*qz+qx*qw); d[ 7] = 0f
        d[ 8] = 2f*(qx*qz+qy*qw); d[ 9] = 2f*(qy*qz-qx*qw); d[10] = 1f-2f*(qx*qx+qy*qy); d[11] = 0f
        d[12] = 0f; d[13] = 0f; d[14] = 0f; d[15] = 1f

        // Применяем: result = bindRot × deltaRot (compose rotation)
        val b = state.bindTransform
        val m = FloatArray(16)
        // Перемножаем только 3×3 rotational части
        m[ 0] = b[0]*d[0]+b[4]*d[1]+b[ 8]*d[ 2]
        m[ 1] = b[1]*d[0]+b[5]*d[1]+b[ 9]*d[ 2]
        m[ 2] = b[2]*d[0]+b[6]*d[1]+b[10]*d[ 2]
        m[ 3] = 0f
        m[ 4] = b[0]*d[4]+b[4]*d[5]+b[ 8]*d[ 6]
        m[ 5] = b[1]*d[4]+b[5]*d[5]+b[ 9]*d[ 6]
        m[ 6] = b[2]*d[4]+b[6]*d[5]+b[10]*d[ 6]
        m[ 7] = 0f
        m[ 8] = b[0]*d[8]+b[4]*d[9]+b[ 8]*d[10]
        m[ 9] = b[1]*d[8]+b[5]*d[9]+b[ 9]*d[10]
        m[10] = b[2]*d[8]+b[6]*d[9]+b[10]*d[10]
        m[11] = 0f
        // Сохраняем translation из bind-pose
        m[12] = b[12]; m[13] = b[13]; m[14] = b[14]; m[15] = 1f

        tm.setTransform(inst, m)
    }

    /** Возвращает кость в bind-pose. */
    fun reset(name: String) {
        val state = bones[name] ?: return
        val tm = engine.transformManager
        if (!tm.hasComponent(state.entity)) return
        tm.setTransform(tm.getInstance(state.entity), state.bindTransform)
    }

    fun clear() = bones.clear()
    fun isReady() = bones.isNotEmpty()
}