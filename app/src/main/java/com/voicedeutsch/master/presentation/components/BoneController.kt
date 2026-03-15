package com.voicedeutsch.master.presentation.components

import android.util.Log
import com.google.android.filament.Engine
import io.github.sceneview.node.ModelNode
import kotlin.math.*

/**
 * Controls skeletal bone rotations via Filament's TransformManager.
 *
 * Stores bind-pose transforms and applies delta rotations on top.
 * Supports multiple bone naming conventions (Mixamo, Unity, ReadyPlayerMe, custom).
 *
 * Key improvement: automatic bone name resolution.
 * When you call rotate("Head", ...), it tries:
 *   "Head", "head", "mixamorig:Head", "Armature|Head", "Head_01", etc.
 */
class BoneController(private val engine: Engine) {

    companion object {
        private const val TAG = "BoneController"

        /** Common bone name prefixes used by different model formats. */
        private val PREFIXES = listOf(
            "",                    // direct name
            "mixamorig:",          // Mixamo
            "mixamorig_",          // Mixamo variant
            "Armature|",           // Blender export
            "Armature_",           // Blender variant
            "Bip01_",              // 3ds Max
            "CC_Base_",            // Character Creator
            "Genesis8_",           // DAZ3D
        )

        /** Bone name aliases: canonical name → possible actual names. */
        private val ALIASES = mapOf(
            "Head" to listOf("Head", "head", "HEAD"),
            "Neck" to listOf("Neck", "neck", "NECK"),
            "Spine" to listOf("Spine", "spine", "SPINE"),
            "Spine1" to listOf("Spine1", "spine1", "Spine01", "spine_01", "SPINE1"),
            "Spine2" to listOf("Spine2", "spine2", "Spine02", "spine_02", "SPINE2"),
            "Hips" to listOf("Hips", "hips", "Hip", "Pelvis", "pelvis"),
            "LeftShoulder" to listOf("LeftShoulder", "leftShoulder", "Left_Shoulder", "L_Shoulder", "Shoulder_L"),
            "RightShoulder" to listOf("RightShoulder", "rightShoulder", "Right_Shoulder", "R_Shoulder", "Shoulder_R"),
            "LeftArm" to listOf("LeftArm", "leftArm", "Left_Arm", "L_UpperArm", "UpperArm_L", "L_Arm"),
            "RightArm" to listOf("RightArm", "rightArm", "Right_Arm", "R_UpperArm", "UpperArm_R", "R_Arm"),
            "LeftForeArm" to listOf("LeftForeArm", "leftForeArm", "Left_ForeArm", "L_Forearm", "ForeArm_L", "L_LowerArm", "LowerArm_L"),
            "RightForeArm" to listOf("RightForeArm", "rightForeArm", "Right_ForeArm", "R_Forearm", "ForeArm_R", "R_LowerArm", "LowerArm_R"),
            "LeftHand" to listOf("LeftHand", "leftHand", "Left_Hand", "L_Hand", "Hand_L"),
            "RightHand" to listOf("RightHand", "rightHand", "Right_Hand", "R_Hand", "Hand_R"),
            "LeftUpLeg" to listOf("LeftUpLeg", "Left_UpLeg", "L_UpperLeg", "L_Thigh"),
            "RightUpLeg" to listOf("RightUpLeg", "Right_UpLeg", "R_UpperLeg", "R_Thigh"),
        )
    }

    data class BoneState(
        val entity: Int,
        val bindTransform: FloatArray,
    )

    // Canonical name → BoneState
    private val bones = mutableMapOf<String, BoneState>()
    // All entity names in the model (for debugging)
    private val allEntityNames = mutableListOf<String>()

    /**
     * Scans all entities, discovers bones by matching naming conventions.
     * After init(), you can call rotate("Head", ...) regardless of the actual
     * bone naming convention used in the model.
     */
    fun init(node: ModelNode) {
        bones.clear()
        allEntityNames.clear()

        val asset = node.modelInstance.asset
        val tm = engine.transformManager

        // First pass: collect all entity names
        val entityByName = mutableMapOf<String, Int>()
        asset.entities.forEach { entity ->
            val name = asset.getName(entity)
            if (name != null) {
                entityByName[name] = entity
                allEntityNames.add(name)
            }
        }

        Log.d(TAG, "Model entities (${allEntityNames.size}): $allEntityNames")

        // Second pass: match canonical names to actual entities
        for ((canonical, aliasList) in ALIASES) {
            val entity = findBoneEntity(canonical, aliasList, entityByName) ?: continue
            if (!tm.hasComponent(entity)) continue

            val inst = tm.getInstance(entity)
            val mat = FloatArray(16)
            tm.getTransform(inst, mat)
            bones[canonical] = BoneState(entity, mat.copyOf())
        }

        // Also register any direct matches not in ALIASES
        entityByName.forEach { (name, entity) ->
            if (name !in bones && tm.hasComponent(entity)) {
                val inst = tm.getInstance(entity)
                val mat = FloatArray(16)
                tm.getTransform(inst, mat)
                bones[name] = BoneState(entity, mat.copyOf())
            }
        }

        Log.d(TAG, "Discovered bones (${bones.size}): ${bones.keys.sorted()}")

        // Warn about missing critical bones
        val critical = listOf("Head", "Neck", "Spine")
        critical.forEach { name ->
            if (name !in bones) Log.w(TAG, "⚠ Critical bone '$name' not found in model!")
        }
    }

    private fun findBoneEntity(
        canonical: String,
        aliases: List<String>,
        entityByName: Map<String, Int>,
    ): Int? {
        // Try each prefix × each alias
        for (alias in aliases) {
            for (prefix in PREFIXES) {
                val fullName = "$prefix$alias"
                entityByName[fullName]?.let { return it }
            }
        }
        return null
    }

    /**
     * Applies a delta rotation on top of the bone's bind-pose.
     *
     * @param name      Canonical bone name (e.g. "Head", "LeftArm")
     * @param pitchDeg  Rotation around X (nod forward/back)
     * @param yawDeg    Rotation around Y (turn left/right)
     * @param rollDeg   Rotation around Z (tilt sideways)
     */
    fun rotate(name: String, pitchDeg: Float, yawDeg: Float, rollDeg: Float) {
        val state = bones[name] ?: return
        val tm = engine.transformManager
        if (!tm.hasComponent(state.entity)) return
        val inst = tm.getInstance(state.entity)

        // Skip if rotation is negligible (optimization)
        if (abs(pitchDeg) < 0.01f && abs(yawDeg) < 0.01f && abs(rollDeg) < 0.01f) {
            tm.setTransform(inst, state.bindTransform)
            return
        }

        val p = pitchDeg * (PI.toFloat() / 180f)
        val y = yawDeg * (PI.toFloat() / 180f)
        val r = rollDeg * (PI.toFloat() / 180f)

        // Quaternion from Euler XYZ (intrinsic)
        val cp = cos(p / 2f); val sp = sin(p / 2f)
        val cy = cos(y / 2f); val sy = sin(y / 2f)
        val cr = cos(r / 2f); val sr = sin(r / 2f)

        val qw = cr * cp * cy + sr * sp * sy
        val qx = sr * cp * cy - cr * sp * sy
        val qy = cr * sp * cy + sr * cp * sy
        val qz = cr * cp * sy - sr * sp * cy

        // Normalize quaternion (prevents drift over time)
        val norm = sqrt(qw * qw + qx * qx + qy * qy + qz * qz)
        val nw = qw / norm; val nx = qx / norm
        val ny = qy / norm; val nz = qz / norm

        // Delta rotation matrix (column-major 4x4)
        val d = FloatArray(16)
        d[0]  = 1f - 2f * (ny * ny + nz * nz)
        d[1]  = 2f * (nx * ny + nz * nw)
        d[2]  = 2f * (nx * nz - ny * nw)
        d[3]  = 0f
        d[4]  = 2f * (nx * ny - nz * nw)
        d[5]  = 1f - 2f * (nx * nx + nz * nz)
        d[6]  = 2f * (ny * nz + nx * nw)
        d[7]  = 0f
        d[8]  = 2f * (nx * nz + ny * nw)
        d[9]  = 2f * (ny * nz - nx * nw)
        d[10] = 1f - 2f * (nx * nx + ny * ny)
        d[11] = 0f
        d[12] = 0f; d[13] = 0f; d[14] = 0f; d[15] = 1f

        // Result = bindPose × deltaRotation (local-space rotation)
        val b = state.bindTransform
        val m = FloatArray(16)

        // Multiply 3x3 rotation parts
        m[0]  = b[0] * d[0] + b[4] * d[1] + b[8]  * d[2]
        m[1]  = b[1] * d[0] + b[5] * d[1] + b[9]  * d[2]
        m[2]  = b[2] * d[0] + b[6] * d[1] + b[10] * d[2]
        m[3]  = 0f
        m[4]  = b[0] * d[4] + b[4] * d[5] + b[8]  * d[6]
        m[5]  = b[1] * d[4] + b[5] * d[5] + b[9]  * d[6]
        m[6]  = b[2] * d[4] + b[6] * d[5] + b[10] * d[6]
        m[7]  = 0f
        m[8]  = b[0] * d[8] + b[4] * d[9] + b[8]  * d[10]
        m[9]  = b[1] * d[8] + b[5] * d[9] + b[9]  * d[10]
        m[10] = b[2] * d[8] + b[6] * d[9] + b[10] * d[10]
        m[11] = 0f
        // Preserve translation from bind-pose
        m[12] = b[12]; m[13] = b[13]; m[14] = b[14]; m[15] = 1f

        tm.setTransform(inst, m)
    }

    /** Resets a bone to its original bind-pose. */
    fun reset(name: String) {
        val state = bones[name] ?: return
        val tm = engine.transformManager
        if (!tm.hasComponent(state.entity)) return
        tm.setTransform(tm.getInstance(state.entity), state.bindTransform)
    }

    /** Resets ALL bones to bind-pose. */
    fun resetAll() {
        bones.keys.forEach { reset(it) }
    }

    /** Check if a specific bone was found in the model. */
    fun hasBone(name: String): Boolean = name in bones

    fun clear() {
        bones.clear()
        allEntityNames.clear()
    }

    fun isReady() = bones.isNotEmpty()

    /** Returns all entity names from the model (for debugging). */
    fun getAllEntityNames(): List<String> = allEntityNames.toList()

    /** Returns all matched canonical bone names. */
    fun getDiscoveredBones(): Set<String> = bones.keys.toSet()
}