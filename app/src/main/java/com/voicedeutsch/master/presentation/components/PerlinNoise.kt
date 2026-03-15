package com.voicedeutsch.master.presentation.components

import kotlin.math.floor

/**
 * Simple 1D/2D Perlin-like noise for organic micro-movements.
 * 
 * Used by AvatarBehaviorEngine to add natural jitter to bone rotations,
 * eye saccades, and breathing variations. Much better than sin() + Random.
 *
 * Usage:
 *   val noise = PerlinNoise(seed = 42)
 *   val value = noise.sample(time * 0.5f)  // returns -1..1
 */
class PerlinNoise(seed: Long = System.nanoTime()) {

    private val perm = IntArray(512)

    init {
        val base = IntArray(256) { it }
        val rng = java.util.Random(seed)
        // Fisher-Yates shuffle
        for (i in 255 downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = base[i]; base[i] = base[j]; base[j] = tmp
        }
        for (i in 0 until 512) perm[i] = base[i and 255]
    }

    /** 1D noise, returns value in approximately -1..1 range. */
    fun sample(x: Float): Float {
        val xi = floor(x).toInt()
        val xf = x - floor(x)
        val u = fade(xf)

        val a = grad1d(perm[xi and 255], xf)
        val b = grad1d(perm[(xi + 1) and 255], xf - 1f)
        return lerp(a, b, u)
    }

    /** 2D noise, returns value in approximately -1..1 range. */
    fun sample(x: Float, y: Float): Float {
        val xi = floor(x).toInt() and 255
        val yi = floor(y).toInt() and 255
        val xf = x - floor(x)
        val yf = y - floor(y)
        val u = fade(xf)
        val v = fade(yf)

        val aa = perm[perm[xi] + yi]
        val ab = perm[perm[xi] + yi + 1]
        val ba = perm[perm[xi + 1] + yi]
        val bb = perm[perm[xi + 1] + yi + 1]

        val x1 = lerp(grad2d(aa, xf, yf), grad2d(ba, xf - 1f, yf), u)
        val x2 = lerp(grad2d(ab, xf, yf - 1f), grad2d(bb, xf - 1f, yf - 1f), u)
        return lerp(x1, x2, v)
    }

    /** Fractal Brownian Motion — layered noise for richer variation. */
    fun fbm(x: Float, octaves: Int = 3, lacunarity: Float = 2f, gain: Float = 0.5f): Float {
        var sum = 0f
        var amp = 1f
        var freq = 1f
        var maxAmp = 0f
        for (i in 0 until octaves) {
            sum += sample(x * freq) * amp
            maxAmp += amp
            amp *= gain
            freq *= lacunarity
        }
        return sum / maxAmp
    }

    private fun fade(t: Float): Float = t * t * t * (t * (t * 6f - 15f) + 10f)
    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun grad1d(hash: Int, x: Float): Float =
        if (hash and 1 == 0) x else -x

    private fun grad2d(hash: Int, x: Float, y: Float): Float = when (hash and 3) {
        0 -> x + y
        1 -> -x + y
        2 -> x - y
        else -> -x - y
    }
}