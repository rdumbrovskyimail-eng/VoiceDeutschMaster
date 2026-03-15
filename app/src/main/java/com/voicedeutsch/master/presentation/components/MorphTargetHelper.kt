package com.voicedeutsch.master.presentation.components

import android.util.Log
import com.google.android.filament.Engine
import io.github.sceneview.node.ModelNode

/**
 * Controls morph targets (blend shapes) on a glTF model via Filament's RenderableManager.
 *
 * Morph targets enable facial animation: lip sync, blinking, eyebrow raises, smiles.
 * Standard ARKit/ReadyPlayerMe blend shapes are supported:
 *   jawOpen, mouthOpen, mouthSmile, eyeBlinkLeft, eyeBlinkRight,
 *   browInnerUp, browDownLeft, browDownRight, viseme_aa, viseme_O, etc.
 *
 * Architecture:
 *  1. init() scans all entities in the glTF asset for morph targets
 *  2. Builds name→(entity, index) lookup map
 *  3. setWeights() batches all changes and applies via setMorphWeights()
 *
 * Thread safety: call only from the render/animation thread (LaunchedEffect).
 */
class MorphTargetHelper(private val engine: Engine) {

    companion object {
        private const val TAG = "MorphTargetHelper"
    }

    /**
     * One morphable entity in the glTF scene graph.
     * A model can have multiple entities with morph targets
     * (e.g., separate face, teeth, tongue meshes).
     */
    private data class MorphableEntity(
        val entity: Int,
        val nameToIndex: Map<String, Int>,
        val weights: FloatArray,
        val count: Int,
    )

    private val morphableEntities = mutableListOf<MorphableEntity>()
    private val nameToEntityIndex = mutableMapOf<String, Int>()

    // Common morph target name aliases (different models use different names)
    private val aliases = mapOf(
        // Jaw
        "jawOpen" to listOf("jawOpen", "Jaw_Open", "jaw_open", "MouthOpen", "mouth_open"),
        // Mouth
        "mouthOpen" to listOf("mouthOpen", "Mouth_Open", "mouth_open"),
        "mouthSmile" to listOf(
            "mouthSmile", "mouthSmileLeft", "mouthSmileRight",
            "Mouth_Smile", "mouth_smile", "smile"
        ),
        "mouthPucker" to listOf("mouthPucker", "Mouth_Pucker", "mouth_pucker", "pucker"),
        // Brows
        "browInnerUp" to listOf("browInnerUp", "Brow_Inner_Up", "brow_inner_up"),
        "browDownLeft" to listOf("browDownLeft", "Brow_Down_Left", "brow_down_left"),
        "browDownRight" to listOf("browDownRight", "Brow_Down_Right", "brow_down_right"),
        // Eyes
        "eyeBlinkLeft" to listOf("eyeBlinkLeft", "Eye_Blink_Left", "eye_blink_left", "blinkLeft"),
        "eyeBlinkRight" to listOf("eyeBlinkRight", "Eye_Blink_Right", "eye_blink_right", "blinkRight"),
        "eyeSquintLeft" to listOf("eyeSquintLeft", "Eye_Squint_Left", "eye_squint_left"),
        "eyeSquintRight" to listOf("eyeSquintRight", "Eye_Squint_Right", "eye_squint_right"),
        "eyeLookUpLeft" to listOf("eyeLookUpLeft", "Eye_Look_Up_Left"),
        "eyeLookUpRight" to listOf("eyeLookUpRight", "Eye_Look_Up_Right"),
        // Visemes
        "viseme_aa" to listOf("viseme_aa", "Viseme_aa", "viseme_AA", "aa"),
        "viseme_O" to listOf("viseme_O", "Viseme_O", "viseme_oh", "O"),
        "viseme_sil" to listOf("viseme_sil", "Viseme_sil", "viseme_silence", "sil"),
        "viseme_E" to listOf("viseme_E", "Viseme_E", "viseme_ee", "E"),
        "viseme_U" to listOf("viseme_U", "Viseme_U", "viseme_oo", "U"),
        "viseme_FF" to listOf("viseme_FF", "Viseme_FF", "viseme_ff"),
        "viseme_TH" to listOf("viseme_TH", "Viseme_TH", "viseme_th"),
        "viseme_SS" to listOf("viseme_SS", "Viseme_SS", "viseme_ss"),
        "viseme_CH" to listOf("viseme_CH", "Viseme_CH", "viseme_ch"),
        "viseme_DD" to listOf("viseme_DD", "Viseme_DD", "viseme_dd"),
        "viseme_RR" to listOf("viseme_RR", "Viseme_RR", "viseme_rr"),
        "viseme_nn" to listOf("viseme_nn", "Viseme_nn"),
        "viseme_kk" to listOf("viseme_kk", "Viseme_kk"),
        "viseme_PP" to listOf("viseme_PP", "Viseme_PP"),
    )

    // Reverse lookup: actual model name → canonical name
    private val actualToCanonical = mutableMapOf<String, String>()

    /**
     * Scans the model for all morph targets and builds lookup tables.
     * Must be called after model is loaded.
     */
    fun init(node: ModelNode) {
        morphableEntities.clear()
        nameToEntityIndex.clear()
        actualToCanonical.clear()

        val asset = node.modelInstance.asset
        val rm = engine.renderableManager

        // Build reverse alias map
        val reverseAliases = mutableMapOf<String, String>()
        aliases.forEach { (canonical, aliasList) ->
            aliasList.forEach { alias ->
                reverseAliases[alias.lowercase()] = canonical
            }
        }

        var totalMorphs = 0

        for (entity in asset.entities) {
            if (!rm.hasComponent(entity)) continue

            val ri = rm.getInstance(entity)
            val count = rm.getMorphTargetCount(ri)
            if (count <= 0) continue

            val nameMap = mutableMapOf<String, Int>()

            // Try to get morph target names
            try {
                val names = asset.getMorphTargetNames(entity)
                names.forEachIndexed { index, name ->
                    if (name.isNotBlank()) {
                        nameMap[name] = index
                        // Also register canonical name
                        val canonical = reverseAliases[name.lowercase()]
                        if (canonical != null) {
                            actualToCanonical[name] = canonical
                            nameMap[canonical] = index
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback: use index-based naming
                Log.w(TAG, "getMorphTargetNames failed for entity, using indices: ${e.message}")
                for (i in 0 until count) {
                    nameMap["morph_$i"] = i
                }
            }

            if (nameMap.isNotEmpty()) {
                val idx = morphableEntities.size
                morphableEntities.add(MorphableEntity(entity, nameMap, FloatArray(count), count))
                nameMap.keys.forEach { name -> nameToEntityIndex[name] = idx }
                totalMorphs += count
            }
        }

        Log.d(TAG, "Initialized: ${morphableEntities.size} morphable entities, $totalMorphs total morph targets")
        if (morphableEntities.isNotEmpty()) {
            val allNames = morphableEntities.flatMap { it.nameToIndex.keys }.sorted()
            Log.d(TAG, "Available morph targets: $allNames")
        }
    }

    /**
     * Sets multiple morph target weights in one batch.
     * Names can be canonical (e.g., "jawOpen") or model-specific.
     * Unrecognized names are silently ignored.
     */
    fun setWeights(weights: Map<String, Float>) {
        if (morphableEntities.isEmpty()) return

        val rm = engine.renderableManager

        // Track which entities were modified
        val modified = BooleanArray(morphableEntities.size)

        // Reset all weights first
        morphableEntities.forEachIndexed { idx, me ->
            me.weights.fill(0f)
            modified[idx] = false
        }

        // Apply requested weights
        for ((name, weight) in weights) {
            val entityIdx = nameToEntityIndex[name] ?: continue
            val me = morphableEntities[entityIdx]
            val morphIdx = me.nameToIndex[name] ?: continue
            me.weights[morphIdx] = weight.coerceIn(0f, 1f)
            modified[entityIdx] = true
        }

        // Flush to Filament
        for (idx in morphableEntities.indices) {
            if (!modified[idx]) continue
            val me = morphableEntities[idx]
            if (!rm.hasComponent(me.entity)) continue
            val ri = rm.getInstance(me.entity)
            rm.setMorphWeights(ri, me.weights, me.count)
        }
    }

    /** Returns all discovered morph target names (for debugging). */
    fun getAvailableNames(): Set<String> =
        morphableEntities.flatMap { it.nameToIndex.keys }.toSet()

    fun clear() {
        morphableEntities.clear()
        nameToEntityIndex.clear()
        actualToCanonical.clear()
    }

    fun isReady() = morphableEntities.isNotEmpty()
}