// src/test/java/com/voicedeutsch/master/util/ExtensionsTest.kt
package com.voicedeutsch.master.util

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExtensionsTest {

    // ── generateUUID ──────────────────────────────────────────────────────

    @Test fun generateUUID_returnsNonEmptyString() { assertTrue(generateUUID().isNotEmpty()) }

    @Test fun generateUUID_matchesUUIDFormat() {
        assertTrue(generateUUID().matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    }

    @Test fun generateUUID_twoCallsProduceDifferentValues() { assertNotEquals(generateUUID(), generateUUID()) }

    // ── String.capitalizeFirst ────────────────────────────────────────────

    @Test fun capitalizeFirst_lowercase_capitalizesFirstChar() { assertEquals("Hello", "hello".capitalizeFirst()) }
    @Test fun capitalizeFirst_alreadyCapitalized_unchanged() { assertEquals("World", "World".capitalizeFirst()) }
    @Test fun capitalizeFirst_emptyString_returnsEmpty() { assertEquals("", "".capitalizeFirst()) }
    @Test fun capitalizeFirst_singleChar_capitalized() { assertEquals("A", "a".capitalizeFirst()) }
    @Test fun capitalizeFirst_allUppercase_onlyFirstChanged() { assertEquals("HELLO", "hELLO".capitalizeFirst()) }
    @Test fun capitalizeFirst_cyrillic_capitalizesCyrillic() { assertEquals("Привет", "привет".capitalizeFirst()) }
    @Test fun capitalizeFirst_unicodeChar_isHandled() { assertEquals("Über", "über".capitalizeFirst()) }

    // ── Float.toPercentString ─────────────────────────────────────────────

    @Test fun toPercentString_zero_returns0Percent() { assertEquals("0%", 0f.toPercentString()) }
    @Test fun toPercentString_one_returns100Percent() { assertEquals("100%", 1f.toPercentString()) }
    @Test fun toPercentString_half_returns50Percent() { assertEquals("50%", 0.5f.toPercentString()) }
    @Test fun toPercentString_0_75_returns75Percent() { assertEquals("75%", 0.75f.toPercentString()) }
    @Test fun toPercentString_truncatesDecimal() { assertEquals("99%", 0.999f.toPercentString()) }
    @Test fun toPercentString_0_01_returns1Percent() { assertEquals("1%", 0.01f.toPercentString()) }

    // ── Float.roundTo ─────────────────────────────────────────────────────

    @Test fun roundTo_0decimals_returnsRoundedToWholeNumber() { assertEquals(3f, 3.4f.roundTo(0), 0.001f) }
    @Test fun roundTo_1decimal_roundsToOnePlace() { assertEquals(3.5f, 3.45f.roundTo(1), 0.001f) }
    @Test fun roundTo_2decimals_roundsToTwoPlaces() { assertEquals(3.14f, 3.141f.roundTo(2), 0.001f) }
    @Test fun roundTo_exactValue_returnsItself() { assertEquals(2.5f, 2.5f.roundTo(1), 0.001f) }
    @Test fun roundTo_zero_returns0() { assertEquals(0f, 0f.roundTo(3), 0.001f) }
    @Test fun roundTo_negativeValue_roundsCorrectly() { assertEquals(-1.5f, (-1.45f).roundTo(1), 0.01f) }

    // ── Int.clamp ─────────────────────────────────────────────────────────

    @Test fun intClamp_withinRange_returnsOriginal() { assertEquals(5, 5.clamp(0, 10)) }
    @Test fun intClamp_belowMin_returnsMin() { assertEquals(0, (-5).clamp(0, 10)) }
    @Test fun intClamp_aboveMax_returnsMax() { assertEquals(10, 15.clamp(0, 10)) }
    @Test fun intClamp_atMin_returnsMin() { assertEquals(0, 0.clamp(0, 10)) }
    @Test fun intClamp_atMax_returnsMax() { assertEquals(10, 10.clamp(0, 10)) }
    @Test fun intClamp_minEqualsMax_returnsMin() { assertEquals(5, 3.clamp(5, 5)) }

    // ── Float.clamp ───────────────────────────────────────────────────────

    @Test fun floatClamp_withinRange_returnsOriginal() { assertEquals(0.5f, 0.5f.clamp(0f, 1f), 0.001f) }
    @Test fun floatClamp_belowMin_returnsMin() { assertEquals(0f, (-0.5f).clamp(0f, 1f), 0.001f) }
    @Test fun floatClamp_aboveMax_returnsMax() { assertEquals(1f, 1.5f.clamp(0f, 1f), 0.001f) }
    @Test fun floatClamp_atMin_returnsMin() { assertEquals(0f, 0f.clamp(0f, 1f), 0.001f) }
    @Test fun floatClamp_atMax_returnsMax() { assertEquals(1f, 1f.clamp(0f, 1f), 0.001f) }

    // ── Long.toMinutes ────────────────────────────────────────────────────

    @Test fun toMinutes_0ms_returnsZero() { assertEquals(0, 0L.toMinutes()) }
    @Test fun toMinutes_60000ms_returnsOne() { assertEquals(1, 60_000L.toMinutes()) }
    @Test fun toMinutes_90000ms_returnsOne() { assertEquals(1, 90_000L.toMinutes()) }
    @Test fun toMinutes_3600000ms_returns60() { assertEquals(60, 3_600_000L.toMinutes()) }
    @Test fun toMinutes_59999ms_returnsZero() { assertEquals(0, 59_999L.toMinutes()) }

    // ── Long.toSeconds ────────────────────────────────────────────────────

    @Test fun toSeconds_0ms_returnsZero() { assertEquals(0, 0L.toSeconds()) }
    @Test fun toSeconds_1000ms_returnsOne() { assertEquals(1, 1_000L.toSeconds()) }
    @Test fun toSeconds_1500ms_returnsOne() { assertEquals(1, 1_500L.toSeconds()) }
    @Test fun toSeconds_60000ms_returns60() { assertEquals(60, 60_000L.toSeconds()) }
    @Test fun toSeconds_999ms_returnsZero() { assertEquals(0, 999L.toSeconds()) }

    // ── safeDivide(Int, Int) ──────────────────────────────────────────────

    @Test fun safeDivideInt_normalCase_returnsCorrectResult() { assertEquals(0.5f, safeDivide(1, 2), 0.001f) }
    @Test fun safeDivideInt_zeroDenominator_returnsZero() { assertEquals(0f, safeDivide(5, 0), 0.001f) }
    @Test fun safeDivideInt_zeroNumerator_returnsZero() { assertEquals(0f, safeDivide(0, 10), 0.001f) }
    @Test fun safeDivideInt_equalValues_returnsOne() { assertEquals(1f, safeDivide(7, 7), 0.001f) }
    @Test fun safeDivideInt_numeratorGreaterThanDenominator_returnsAboveOne() { assertTrue(safeDivide(10, 3) > 1f) }

    // ── safeDivide(Float, Float) ──────────────────────────────────────────

    @Test fun safeDivideFloat_normalCase_returnsCorrectResult() { assertEquals(0.5f, safeDivide(1f, 2f), 0.001f) }
    @Test fun safeDivideFloat_zeroDenominator_returnsZero() { assertEquals(0f, safeDivide(5f, 0f), 0.001f) }
    @Test fun safeDivideFloat_zeroNumerator_returnsZero() { assertEquals(0f, safeDivide(0f, 10f), 0.001f) }
    @Test fun safeDivideFloat_equalValues_returnsOne() { assertEquals(1f, safeDivide(3.14f, 3.14f), 0.001f) }

    // ── onError (Flow) ────────────────────────────────────────────────────

    @Test fun onError_noError_doesNotCallAction() = runTest {
        var errorCalled = false
        val result = flowOf(1, 2, 3).onError { errorCalled = true }.toList()
        assertFalse(errorCalled)
        assertEquals(listOf(1, 2, 3), result)
    }

    @Test fun onError_withException_callsAction() = runTest {
        var caughtError: Throwable? = null
        kotlinx.coroutines.flow.flow<Int> {
            throw RuntimeException("test error")
        }.onError { caughtError = it }.toList()
        assertNotNull(caughtError)
        assertEquals("test error", caughtError!!.message)
    }

    // ── mapItems (Flow<List<T>>) ──────────────────────────────────────────

    @Test fun mapItems_transformsEachItemInList() = runTest {
        val result = flowOf(listOf(1, 2, 3)).mapItems { it * 2 }.toList()
        assertEquals(listOf(listOf(2, 4, 6)), result)
    }

    @Test fun mapItems_emptyList_returnsEmptyList() = runTest {
        val result = flowOf(emptyList<Int>()).mapItems { it + 1 }.toList()
        assertEquals(listOf(emptyList<Int>()), result)
    }
}
