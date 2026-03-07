// src/test/java/com/voicedeutsch/master/util/AudioUtilsTest.kt
package com.voicedeutsch.master.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class AudioUtilsTest {

    // ── calculateRMS ──────────────────────────────────────────────────────

    @Test fun calculateRMS_emptySamples_returnsZero() {
        assertEquals(0f, AudioUtils.calculateRMS(shortArrayOf()), 0.001f)
    }

    @Test fun calculateRMS_allZeroSamples_returnsZero() {
        assertEquals(0f, AudioUtils.calculateRMS(shortArrayOf(0, 0, 0, 0)), 0.001f)
    }

    @Test fun calculateRMS_uniformPositiveSamples_returnsCorrectValue() {
        assertEquals(100f, AudioUtils.calculateRMS(shortArrayOf(100, 100, 100, 100)), 0.1f)
    }

    @Test fun calculateRMS_mixedSignSamples_returnsPositiveValue() {
        assertEquals(100f, AudioUtils.calculateRMS(shortArrayOf(-100, 100, -100, 100)), 0.1f)
    }

    @Test fun calculateRMS_singleSample_returnsAbsoluteValue() {
        assertEquals(1000f, AudioUtils.calculateRMS(shortArrayOf(1000)), 0.1f)
    }

    @Test fun calculateRMS_maxShortValue_returnsMaxShort() {
        assertEquals(Short.MAX_VALUE.toFloat(), AudioUtils.calculateRMS(shortArrayOf(Short.MAX_VALUE)), 1f)
    }

    @Test fun calculateRMS_differentValues_matchesManualCalculation() {
        // RMS([3, 4]) = sqrt((9+16)/2) = sqrt(12.5) ≈ 3.536
        assertEquals(sqrt(12.5).toFloat(), AudioUtils.calculateRMS(shortArrayOf(3, 4)), 0.01f)
    }

    // ── calculateRMSdB ────────────────────────────────────────────────────

    @Test fun calculateRMSdB_emptySamples_returnsMinus100() {
        assertEquals(-100f, AudioUtils.calculateRMSdB(shortArrayOf()), 0.001f)
    }

    @Test fun calculateRMSdB_allZeroSamples_returnsMinus100() {
        assertEquals(-100f, AudioUtils.calculateRMSdB(shortArrayOf(0, 0, 0)), 0.001f)
    }

    @Test fun calculateRMSdB_maxShortValue_returnsZerodBFS() {
        assertEquals(0f, AudioUtils.calculateRMSdB(shortArrayOf(Short.MAX_VALUE)), 0.5f)
    }

    @Test fun calculateRMSdB_halfMaxAmplitude_returnsMinus6dBFS() {
        val half = (Short.MAX_VALUE / 2).toShort()
        val result = AudioUtils.calculateRMSdB(shortArrayOf(half, half, half, half))
        assertTrue(result < 0f)
        assertTrue(result > -100f)
    }

    @Test fun calculateRMSdB_returnsNegativeForNonMaxAmplitude() {
        assertTrue(AudioUtils.calculateRMSdB(shortArrayOf(1000, 1000, 1000)) < 0f)
    }

    // ── calculatePeakAmplitude ────────────────────────────────────────────

    @Test fun calculatePeakAmplitude_emptySamples_returnsZero() {
        assertEquals(0f, AudioUtils.calculatePeakAmplitude(shortArrayOf()), 0.001f)
    }

    @Test fun calculatePeakAmplitude_allZeroSamples_returnsZero() {
        assertEquals(0f, AudioUtils.calculatePeakAmplitude(shortArrayOf(0, 0, 0)), 0.001f)
    }

    @Test fun calculatePeakAmplitude_maxShortValue_returnsOne() {
        assertEquals(1f, AudioUtils.calculatePeakAmplitude(shortArrayOf(Short.MAX_VALUE)), 0.001f)
    }

    @Test fun calculatePeakAmplitude_negativeMaxValue_returnsOne() {
        assertTrue(AudioUtils.calculatePeakAmplitude(shortArrayOf(Short.MIN_VALUE)) > 0.99f)
    }

    @Test fun calculatePeakAmplitude_mixedValues_returnsMaxAbsolute() {
        val expected = 5000f / Short.MAX_VALUE
        assertEquals(expected, AudioUtils.calculatePeakAmplitude(shortArrayOf(100, -5000, 3000)), 0.001f)
    }

    @Test fun calculatePeakAmplitude_singlePositiveSample_returnsCorrect() {
        assertEquals(0.5f, AudioUtils.calculatePeakAmplitude(shortArrayOf(16384)), 0.01f)
    }

    @Test fun calculatePeakAmplitude_resultIsBetweenZeroAndOne() {
        val result = AudioUtils.calculatePeakAmplitude(shortArrayOf(1000, 2000, -1500))
        assertTrue(result in 0f..1f)
    }

    // ── normalizeForVisualization ─────────────────────────────────────────

    @Test fun normalizeForVisualization_emptySamples_returnsZeroArray() {
        val result = AudioUtils.normalizeForVisualization(shortArrayOf(), 10)
        assertEquals(10, result.size)
        assertTrue(result.all { it == 0f })
    }

    @Test fun normalizeForVisualization_returnsCorrectTargetSize() {
        val result = AudioUtils.normalizeForVisualization(ShortArray(100) { (it % 100).toShort() }, 20)
        assertEquals(20, result.size)
    }

    @Test fun normalizeForVisualization_allValues_between0and1() {
        val result = AudioUtils.normalizeForVisualization(ShortArray(1000) { (it % 1000 - 500).toShort() }, 50)
        assertTrue(result.all { it in 0f..1f })
    }

    @Test fun normalizeForVisualization_allZeroSamples_returnsAllZeros() {
        val result = AudioUtils.normalizeForVisualization(ShortArray(100) { 0 }, 10)
        assertTrue(result.all { it == 0f })
    }

    @Test fun normalizeForVisualization_targetSize1_returnsMaxAmplitude() {
        val result = AudioUtils.normalizeForVisualization(shortArrayOf(Short.MAX_VALUE, 0, 0), 1)
        assertEquals(1, result.size)
        assertEquals(1f, result[0], 0.001f)
    }

    @Test fun normalizeForVisualization_targetSizeEqualsSamplesSize_preservesCount() {
        assertEquals(3, AudioUtils.normalizeForVisualization(shortArrayOf(Short.MAX_VALUE, 0, Short.MAX_VALUE), 3).size)
    }

    // ── shortArrayToByteArray ─────────────────────────────────────────────

    @Test fun shortArrayToByteArray_emptySamples_returnsEmptyByteArray() {
        assertEquals(0, AudioUtils.shortArrayToByteArray(shortArrayOf()).size)
    }

    @Test fun shortArrayToByteArray_returnsDoubleSize() {
        assertEquals(6, AudioUtils.shortArrayToByteArray(shortArrayOf(1, 2, 3)).size)
    }

    @Test fun shortArrayToByteArray_zeroSample_returnsTwoZeroBytes() {
        val result = AudioUtils.shortArrayToByteArray(shortArrayOf(0))
        assertEquals(2, result.size)
        assertEquals(0.toByte(), result[0])
        assertEquals(0.toByte(), result[1])
    }

    @Test fun shortArrayToByteArray_knownValue_returnsCorrectLittleEndian() {
        val result = AudioUtils.shortArrayToByteArray(shortArrayOf(0x0102))
        assertEquals(0x02.toByte(), result[0])
        assertEquals(0x01.toByte(), result[1])
    }

    // ── byteArrayToShortArray ─────────────────────────────────────────────

    @Test fun byteArrayToShortArray_emptyBytes_returnsEmptyShortArray() {
        assertEquals(0, AudioUtils.byteArrayToShortArray(byteArrayOf()).size)
    }

    @Test fun byteArrayToShortArray_returnsHalfSize() {
        assertEquals(3, AudioUtils.byteArrayToShortArray(ByteArray(6)).size)
    }

    @Test fun byteArrayToShortArray_zeroBytes_returnsZeroShorts() {
        val result = AudioUtils.byteArrayToShortArray(byteArrayOf(0, 0))
        assertEquals(1, result.size)
        assertEquals(0.toShort(), result[0])
    }

    @Test fun byteArrayToShortArray_knownLittleEndian_returnsCorrectShort() {
        assertEquals(0x0102.toShort(), AudioUtils.byteArrayToShortArray(byteArrayOf(0x02, 0x01))[0])
    }

    // ── roundtrip ─────────────────────────────────────────────────────────

    @Test fun roundtrip_shortToByteToShort_preservesAllValues() {
        val original = shortArrayOf(0, 1000, -1000, Short.MAX_VALUE, Short.MIN_VALUE, 12345, -6789)
        val restored = AudioUtils.byteArrayToShortArray(AudioUtils.shortArrayToByteArray(original))
        assertArrayEquals(original.toTypedArray(), restored.toTypedArray())
    }

    @Test fun roundtrip_largeArray_preservesAllValues() {
        val original = ShortArray(1000) { (it - 500).toShort() }
        val restored = AudioUtils.byteArrayToShortArray(AudioUtils.shortArrayToByteArray(original))
        assertArrayEquals(original.toTypedArray(), restored.toTypedArray())
    }
}
