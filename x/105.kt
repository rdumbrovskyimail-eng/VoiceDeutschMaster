// Путь: src/test/java/com/voicedeutsch/master/voicecore/engine/AvatarAnimationSourceTest.kt
package com.voicedeutsch.master.voicecore.engine

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AvatarAnimationSourceTest {

    private lateinit var source: AvatarAnimationSource

    @BeforeEach
    fun setUp() {
        source = AvatarAnimationSource()
    }

    // ── createActiveFlow ──────────────────────────────────────────────────

    @Test
    fun createActiveFlow_emitsValues() = runTest {
        source.createActiveFlow().test {
            val value = awaitItem()
            assertNotNull(value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createActiveFlow_emittedValue_inRange0to1() = runTest {
        source.createActiveFlow().test {
            val value = awaitItem()
            assertTrue(value >= 0f, "Expected >= 0f but was $value")
            assertTrue(value <= 1f, "Expected <= 1f but was $value")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createActiveFlow_multipleValues_allInRange0to1() = runTest {
        source.createActiveFlow().test {
            repeat(10) {
                val value = awaitItem()
                assertTrue(value >= 0f, "Value $it out of range: $value")
                assertTrue(value <= 1f, "Value $it out of range: $value")
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createActiveFlow_emitsFiniteValues() = runTest {
        source.createActiveFlow().test {
            repeat(5) {
                assertTrue(awaitItem().isFinite())
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createActiveFlow_emitsNonNaNValues() = runTest {
        source.createActiveFlow().test {
            repeat(5) {
                assertFalse(awaitItem().isNaN())
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createActiveFlow_emitsMultipleDistinctValues() = runTest {
        val values = mutableListOf<Float>()
        source.createActiveFlow().test {
            repeat(30) { values.add(awaitItem()) }
            cancelAndIgnoreRemainingEvents()
        }
        // Natural animation must not be a flat constant line
        val distinct = values.toSet()
        assertTrue(distinct.size > 1, "Expected variation but got: $distinct")
    }

    @Test
    fun createActiveFlow_cancelledExternally_doesNotThrow() = runTest {
        assertDoesNotThrow {
            source.createActiveFlow().test {
                awaitItem()
                cancel()
            }
        }
    }

    @Test
    fun createActiveFlow_newInstance_returnsNewFlow() = runTest {
        val flow1 = source.createActiveFlow()
        val flow2 = source.createActiveFlow()
        assertNotSame(flow1, flow2)
    }

    // ── createIdleFlow ────────────────────────────────────────────────────

    @Test
    fun createIdleFlow_emitsValues() = runTest {
        source.createIdleFlow().test {
            val value = awaitItem()
            assertNotNull(value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createIdleFlow_emittedValue_inRange0to1() = runTest {
        source.createIdleFlow().test {
            val value = awaitItem()
            assertTrue(value >= 0f, "Expected >= 0f but was $value")
            assertTrue(value <= 1f, "Expected <= 1f but was $value")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createIdleFlow_multipleValues_allInRange0to1() = runTest {
        source.createIdleFlow().test {
            repeat(10) {
                val value = awaitItem()
                assertTrue(value >= 0f, "Value $it out of range: $value")
                assertTrue(value <= 1f, "Value $it out of range: $value")
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createIdleFlow_valuesAreLowAmplitude() = runTest {
        // Idle breathing should be subtle — max ≈ 0.1f (sin * 0.05 + 0.05)
        source.createIdleFlow().test {
            repeat(20) {
                val value = awaitItem()
                assertTrue(value <= 0.15f, "Idle amplitude too high: $value")
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createIdleFlow_emitsFiniteValues() = runTest {
        source.createIdleFlow().test {
            repeat(5) { assertTrue(awaitItem().isFinite()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createIdleFlow_emitsNonNaNValues() = runTest {
        source.createIdleFlow().test {
            repeat(5) { assertFalse(awaitItem().isNaN()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createIdleFlow_cancelledExternally_doesNotThrow() = runTest {
        assertDoesNotThrow {
            source.createIdleFlow().test {
                awaitItem()
                cancel()
            }
        }
    }

    // ── idle vs active amplitude comparison ───────────────────────────────

    @Test
    fun idleFlow_averageAmplitude_lowerThanActiveFlow() = runTest {
        val idleValues = mutableListOf<Float>()
        val activeValues = mutableListOf<Float>()

        source.createIdleFlow().test {
            repeat(20) { idleValues.add(awaitItem()) }
            cancelAndIgnoreRemainingEvents()
        }

        source.createActiveFlow().test {
            repeat(20) { activeValues.add(awaitItem()) }
            cancelAndIgnoreRemainingEvents()
        }

        val idleAvg = idleValues.average()
        val activeAvg = activeValues.average()
        assertTrue(idleAvg < activeAvg, "Idle avg $idleAvg should be < active avg $activeAvg")
    }
}
