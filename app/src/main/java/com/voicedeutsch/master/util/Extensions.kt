package com.voicedeutsch.master.util

import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Generates a new random UUID string.
 */
fun generateUUID(): String = UUID.randomUUID().toString()

/**
 * Capitalizes the first character of the string.
 */
fun String.capitalizeFirst(): String =
    this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

/**
 * Converts a float (0.0–1.0) to a percent string like "75%".
 */
fun Float.toPercentString(): String = "${(this * 100).toInt()}%"

/**
 * Rounds a float to the given number of decimal places.
 */
fun Float.roundTo(decimals: Int): Float {
    var multiplier = 1f
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

/**
 * Clamps an [Int] value to the given [min]..[max] range.
 */
fun Int.clamp(min: Int, max: Int): Int = maxOf(min, minOf(max, this))

/**
 * Clamps a [Float] value to the given [min]..[max] range.
 */
fun Float.clamp(min: Float, max: Float): Float = maxOf(min, minOf(max, this))

/**
 * Converts milliseconds to whole minutes.
 */
fun Long.toMinutes(): Int = (this / 60_000).toInt()

/**
 * Converts milliseconds to whole seconds.
 */
fun Long.toSeconds(): Int = (this / 1000).toInt()

/**
 * Catches exceptions in a [Flow] and forwards them to the given [action].
 */
inline fun <T> Flow<T>.onError(crossinline action: (Throwable) -> Unit): Flow<T> =
    catch { action(it) }

/**
 * Maps each item inside a [Flow] of [List].
 */
fun <T> Flow<List<T>>.mapItems(transform: (T) -> T): Flow<List<T>> =
    map { list -> list.map(transform) }

/**
 * Safe integer division — returns 0f when the denominator is 0.
 */
fun safeDivide(numerator: Int, denominator: Int): Float =
    if (denominator == 0) 0f else numerator.toFloat() / denominator.toFloat()

/**
 * Safe float division — returns 0f when the denominator is 0f.
 */
fun safeDivide(numerator: Float, denominator: Float): Float =
    if (denominator == 0f) 0f else numerator / denominator
