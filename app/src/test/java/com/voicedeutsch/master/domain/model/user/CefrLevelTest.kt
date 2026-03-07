// Путь: src/test/java/com/voicedeutsch/master/domain/model/user/CefrLevelTest.kt
package com.voicedeutsch.master.domain.model.user

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CefrLevelTest {

    // ── entries ───────────────────────────────────────────────────────────

    @Test
    fun entries_count_equals6() {
        assertEquals(6, CefrLevel.entries.size)
    }

    @Test
    fun entries_containsAllExpectedValues() {
        val expected = setOf(
            CefrLevel.A1, CefrLevel.A2,
            CefrLevel.B1, CefrLevel.B2,
            CefrLevel.C1, CefrLevel.C2,
        )
        assertEquals(expected, CefrLevel.entries.toSet())
    }

    // ── displayName ───────────────────────────────────────────────────────

    @Test
    fun displayName_a1_isNachalnyj() {
        assertEquals("Начальный", CefrLevel.A1.displayName)
    }

    @Test
    fun displayName_a2_isEhlementarnyj() {
        assertEquals("Элементарный", CefrLevel.A2.displayName)
    }

    @Test
    fun displayName_b1_isSrednyj() {
        assertEquals("Средний", CefrLevel.B1.displayName)
    }

    @Test
    fun displayName_b2_isVysheSrednego() {
        assertEquals("Выше среднего", CefrLevel.B2.displayName)
    }

    @Test
    fun displayName_c1_isProdvinutyj() {
        assertEquals("Продвинутый", CefrLevel.C1.displayName)
    }

    @Test
    fun displayName_c2_isMaster() {
        assertEquals("Мастер", CefrLevel.C2.displayName)
    }

    @Test
    fun displayName_allNonBlank() {
        CefrLevel.entries.forEach { assertTrue(it.displayName.isNotBlank()) }
    }

    // ── order ─────────────────────────────────────────────────────────────

    @Test
    fun order_a1_equals1() {
        assertEquals(1, CefrLevel.A1.order)
    }

    @Test
    fun order_a2_equals2() {
        assertEquals(2, CefrLevel.A2.order)
    }

    @Test
    fun order_b1_equals3() {
        assertEquals(3, CefrLevel.B1.order)
    }

    @Test
    fun order_b2_equals4() {
        assertEquals(4, CefrLevel.B2.order)
    }

    @Test
    fun order_c1_equals5() {
        assertEquals(5, CefrLevel.C1.order)
    }

    @Test
    fun order_c2_equals6() {
        assertEquals(6, CefrLevel.C2.order)
    }

    @Test
    fun order_isStrictlyIncreasing() {
        val orders = CefrLevel.entries.map { it.order }
        for (i in 1 until orders.size) {
            assertTrue(orders[i] > orders[i - 1])
        }
    }

    @Test
    fun order_allUnique() {
        val orders = CefrLevel.entries.map { it.order }
        assertEquals(orders.size, orders.toSet().size)
    }

    // ── next() ────────────────────────────────────────────────────────────

    @Test
    fun next_a1_returnsA2() {
        assertEquals(CefrLevel.A2, CefrLevel.A1.next())
    }

    @Test
    fun next_a2_returnsB1() {
        assertEquals(CefrLevel.B1, CefrLevel.A2.next())
    }

    @Test
    fun next_b1_returnsB2() {
        assertEquals(CefrLevel.B2, CefrLevel.B1.next())
    }

    @Test
    fun next_b2_returnsC1() {
        assertEquals(CefrLevel.C1, CefrLevel.B2.next())
    }

    @Test
    fun next_c1_returnsC2() {
        assertEquals(CefrLevel.C2, CefrLevel.C1.next())
    }

    @Test
    fun next_c2_returnsNull() {
        assertNull(CefrLevel.C2.next())
    }

    // ── previous() ───────────────────────────────────────────────────────

    @Test
    fun previous_a1_returnsNull() {
        assertNull(CefrLevel.A1.previous())
    }

    @Test
    fun previous_a2_returnsA1() {
        assertEquals(CefrLevel.A1, CefrLevel.A2.previous())
    }

    @Test
    fun previous_b1_returnsA2() {
        assertEquals(CefrLevel.A2, CefrLevel.B1.previous())
    }

    @Test
    fun previous_b2_returnsB1() {
        assertEquals(CefrLevel.B1, CefrLevel.B2.previous())
    }

    @Test
    fun previous_c1_returnsB2() {
        assertEquals(CefrLevel.B2, CefrLevel.C1.previous())
    }

    @Test
    fun previous_c2_returnsC1() {
        assertEquals(CefrLevel.C1, CefrLevel.C2.previous())
    }

    // ── next/previous symmetry ────────────────────────────────────────────

    @Test
    fun nextThenPrevious_returnsOriginal() {
        CefrLevel.entries.filter { it != CefrLevel.C2 }.forEach { level ->
            assertEquals(level, level.next()?.previous())
        }
    }

    @Test
    fun previousThenNext_returnsOriginal() {
        CefrLevel.entries.filter { it != CefrLevel.A1 }.forEach { level ->
            assertEquals(level, level.previous()?.next())
        }
    }

    // ── fromString ────────────────────────────────────────────────────────

    @Test
    fun fromString_a1_returnsA1() {
        assertEquals(CefrLevel.A1, CefrLevel.fromString("A1"))
    }

    @Test
    fun fromString_b2_returnsB2() {
        assertEquals(CefrLevel.B2, CefrLevel.fromString("B2"))
    }

    @Test
    fun fromString_c2_returnsC2() {
        assertEquals(CefrLevel.C2, CefrLevel.fromString("C2"))
    }

    @Test
    fun fromString_allValidNames_parsedCorrectly() {
        CefrLevel.entries.forEach { level ->
            assertEquals(level, CefrLevel.fromString(level.name))
        }
    }

    @Test
    fun fromString_unknownValue_defaultsToA1() {
        assertEquals(CefrLevel.A1, CefrLevel.fromString("X9"))
    }

    @Test
    fun fromString_emptyString_defaultsToA1() {
        assertEquals(CefrLevel.A1, CefrLevel.fromString(""))
    }

    @Test
    fun fromString_lowercase_defaultsToA1() {
        // Name matching is case-sensitive — "a1" won't match "A1"
        assertEquals(CefrLevel.A1, CefrLevel.fromString("a1"))
    }

    @Test
    fun fromString_randomGibberish_defaultsToA1() {
        assertEquals(CefrLevel.A1, CefrLevel.fromString("Z99"))
    }
}
