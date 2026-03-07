// Путь: src/test/java/com/voicedeutsch/master/voicecore/audio/RmsCalculatorTest.kt
package com.voicedeutsch.master.voicecore.audio

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RmsCalculatorTest {

    // ── edge cases ────────────────────────────────────────────────────────

    @Test
    fun calculate_emptyArray_returnsZero() {
        assertEquals(0f, RmsCalculator.calculate(ByteArray(0)))
    }

    @Test
    fun calculate_singleByte_returnsZero() {
        assertEquals(0f, RmsCalculator.calculate(ByteArray(1) { 0x7F.toByte() }))
    }

    @Test
    fun calculate_allZeroSamples_returnsZero() {
        assertEquals(0f, RmsCalculator.calculate(ByteArray(640) { 0 }))
    }

    // ── normalization bounds ──────────────────────────────────────────────

    @Test
    fun calculate_result_isNonNegative() {
        val pcm = ByteArray(640) { (it % 127).toByte() }
        assertTrue(RmsCalculator.calculate(pcm) >= 0f)
    }

    @Test
    fun calculate_maxPositiveSamples_returnsCloseToOne() {
        // 0x7FFF = 32767 (max positive Int16), little-endian: low=0xFF, high=0x7F
        val pcm = ByteArray(640) { i -> if (i % 2 == 0) 0xFF.toByte() else 0x7F.toByte() }
        val result = RmsCalculator.calculate(pcm)
        assertTrue(result > 0.99f, "Expected ~1.0 but was $result")
        assertTrue(result <= 1.1f, "Should be close to 1.0 but was $result")
    }

    @Test
    fun calculate_maxNegativeSamples_returnsCloseToOne() {
        // 0x8000 = -32768 (min Int16), little-endian: low=0x00, high=0x80
        val pcm = ByteArray(640) { i -> if (i % 2 == 0) 0x00.toByte() else 0x80.toByte() }
        val result = RmsCalculator.calculate(pcm)
        assertTrue(result > 0.99f, "Expected ~1.0 but was $result")
    }

    // ── little-endian decoding ────────────────────────────────────────────

    @Test
    fun calculate_knownSample_returnsCorrectRms() {
        // Sample = 1000: low = 0xE8, high = 0x03 (little-endian)
        // RMS of single sample 1000 = 1000 / 32768 ≈ 0.030518
        val low  = (1000 and 0xFF).toByte()     // 0xE8
        val high = ((1000 shr 8) and 0xFF).toByte() // 0x03
        val pcm  = byteArrayOf(low, high)
        val expected = (1000.0 / 32768.0).toFloat()
        assertEquals(expected, RmsCalculator.calculate(pcm), 0.0001f)
    }

    @Test
    fun calculate_negativeSample_sameRmsAsPositive() {
        // -1000 in little-endian: low = 0x18, high = 0xFC
        val negLow  = (-1000 and 0xFF).toByte()
        val negHigh = ((-1000 shr 8) and 0xFF).toByte()
        val posPcm  = byteArrayOf((1000 and 0xFF).toByte(), ((1000 shr 8) and 0xFF).toByte())
        val negPcm  = byteArrayOf(negLow, negHigh)
        assertEquals(
            RmsCalculator.calculate(posPcm),
            RmsCalculator.calculate(negPcm),
            0.0001f,
        )
    }

    @Test
    fun calculate_singleZeroSample_returnsZero() {
        val pcm = byteArrayOf(0x00, 0x00)
        assertEquals(0f, RmsCalculator.calculate(pcm))
    }

    // ── multiple samples ──────────────────────────────────────────────────

    @Test
    fun calculate_twoEqualSamples_sameAsOneSample() {
        val low  = (2000 and 0xFF).toByte()
        val high = ((2000 shr 8) and 0xFF).toByte()
        val single = byteArrayOf(low, high)
        val double = byteArrayOf(low, high, low, high)
        assertEquals(
            RmsCalculator.calculate(single),
            RmsCalculator.calculate(double),
            0.0001f,
        )
    }

    @Test
    fun calculate_mixedPositiveAndNegativeSamples_positiveResult() {
        // +16384 and -16384 alternate — RMS should equal 16384/32768 = 0.5
        val posLow  = (16384 and 0xFF).toByte()
        val posHigh = ((16384 shr 8) and 0xFF).toByte()
        val negLow  = (-16384 and 0xFF).toByte()
        val negHigh = ((-16384 shr 8) and 0xFF).toByte()
        val pcm = byteArrayOf(posLow, posHigh, negLow, negHigh)
        val result = RmsCalculator.calculate(pcm)
        assertEquals(0.5f, result, 0.001f)
    }

    // ── odd-length input ──────────────────────────────────────────────────

    @Test
    fun calculate_oddLengthArray_ignoresLastByte() {
        // 3 bytes → 1 full sample processed, trailing byte ignored
        val low  = (500 and 0xFF).toByte()
        val high = ((500 shr 8) and 0xFF).toByte()
        val odd   = byteArrayOf(low, high, 0x55)
        val even  = byteArrayOf(low, high)
        assertEquals(
            RmsCalculator.calculate(even),
            RmsCalculator.calculate(odd),
            0.0001f,
        )
    }

    // ── output is finite ──────────────────────────────────────────────────

    @Test
    fun calculate_largeInput_returnsFiniteValue() {
        val pcm = ByteArray(6400) { i -> if (i % 2 == 0) 0xAB.toByte() else 0x12.toByte() }
        assertTrue(RmsCalculator.calculate(pcm).isFinite())
    }

    @Test
    fun calculate_alternatingMaxValues_returnsFiniteValue() {
        val pcm = ByteArray(640) { i -> if (i % 2 == 0) 0xFF.toByte() else 0x7F.toByte() }
        assertTrue(RmsCalculator.calculate(pcm).isFinite())
    }
}
