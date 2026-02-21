package com.voicedeutsch.master.voicecore.functions

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FunctionRegistryTest {

    @Test
    fun `all declarations have unique names`() {
        val all = FunctionRegistry.getAllDeclarations()
        val names = all.map { it.name }
        assertEquals(names.size, names.distinct().size, "Duplicate function names found: ${names.groupBy { it }.filter { it.value.size > 1 }.keys}")
    }

    @Test
    fun `total function count matches expected`() {
        val all = FunctionRegistry.getAllDeclarations()
        assertTrue(all.size >= 14, "Expected at least 14 function declarations, got ${all.size}")
    }

    private fun assertEquals(expected: Int, actual: Int, message: String) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message)
    }
}