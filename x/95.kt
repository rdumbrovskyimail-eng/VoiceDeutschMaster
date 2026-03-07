// Путь: src/test/java/com/voicedeutsch/master/voicecore/functions/SessionFunctionsTest.kt
package com.voicedeutsch.master.voicecore.functions

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SessionFunctionsTest {

    private val declarations = SessionFunctions.declarations

    // ── list integrity ────────────────────────────────────────────────────

    @Test
    fun declarations_containsExactly2Functions() {
        assertEquals(2, declarations.size)
    }

    @Test
    fun declarations_namesAreUnique() {
        val names = declarations.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun declarations_noBlankNames() {
        declarations.forEach { assertTrue(it.name.isNotBlank()) }
    }

    @Test
    fun declarations_noBlankDescriptions() {
        declarations.forEach { assertTrue(it.description.isNotBlank()) }
    }

    // ── declaration order ─────────────────────────────────────────────────

    @Test
    fun declarations_firstIsSetCurrentStrategy() {
        assertEquals("set_current_strategy", declarations[0].name)
    }

    @Test
    fun declarations_secondIsLogSessionEvent() {
        assertEquals("log_session_event", declarations[1].name)
    }

    // ── set_current_strategy ──────────────────────────────────────────────

    @Test
    fun setCurrentStrategy_parametersNotNull() {
        assertNotNull(declarations[0].parameters)
    }

    @Test
    fun setCurrentStrategy_parametersTypeIsObject() {
        assertEquals("object", declarations[0].parameters!!.type)
    }

    @Test
    fun setCurrentStrategy_hasStrategyParam_typeString() {
        val params = declarations[0].parameters!!
        assertEquals("string", params.properties["strategy"]?.type)
    }

    @Test
    fun setCurrentStrategy_hasReasonParam_typeString() {
        val params = declarations[0].parameters!!
        assertEquals("string", params.properties["reason"]?.type)
    }

    @Test
    fun setCurrentStrategy_strategyParamHasEnum() {
        val prop = declarations[0].parameters!!.properties["strategy"]!!
        assertNotNull(prop.enum)
        assertTrue(prop.enum!!.isNotEmpty())
    }

    @Test
    fun setCurrentStrategy_enumContainsAllNineStrategies() {
        val enum = declarations[0].parameters!!.properties["strategy"]!!.enum!!
        assertEquals(9, enum.size)
        assertTrue(enum.contains("REPETITION"))
        assertTrue(enum.contains("LINEAR_BOOK"))
        assertTrue(enum.contains("FREE_PRACTICE"))
        assertTrue(enum.contains("PRONUNCIATION"))
        assertTrue(enum.contains("GAP_FILLING"))
        assertTrue(enum.contains("GRAMMAR_DRILL"))
        assertTrue(enum.contains("VOCABULARY_BOOST"))
        assertTrue(enum.contains("LISTENING"))
        assertTrue(enum.contains("ASSESSMENT"))
    }

    @Test
    fun setCurrentStrategy_requiredContainsStrategy() {
        assertTrue(declarations[0].parameters!!.required.contains("strategy"))
    }

    @Test
    fun setCurrentStrategy_reasonNotRequired() {
        assertFalse(declarations[0].parameters!!.required.contains("reason"))
    }

    @Test
    fun setCurrentStrategy_reasonParamHasNoEnum() {
        val prop = declarations[0].parameters!!.properties["reason"]!!
        assertNull(prop.enum)
    }

    // ── log_session_event ─────────────────────────────────────────────────

    @Test
    fun logSessionEvent_parametersNotNull() {
        assertNotNull(declarations[1].parameters)
    }

    @Test
    fun logSessionEvent_hasEventTypeParam_typeString() {
        val params = declarations[1].parameters!!
        assertEquals("string", params.properties["event_type"]?.type)
    }

    @Test
    fun logSessionEvent_hasDetailsParam_typeString() {
        val params = declarations[1].parameters!!
        assertEquals("string", params.properties["details"]?.type)
    }

    @Test
    fun logSessionEvent_requiredContainsEventType() {
        assertTrue(declarations[1].parameters!!.required.contains("event_type"))
    }

    @Test
    fun logSessionEvent_detailsNotRequired() {
        assertFalse(declarations[1].parameters!!.required.contains("details"))
    }

    @Test
    fun logSessionEvent_eventTypeHasNoEnum() {
        assertNull(declarations[1].parameters!!.properties["event_type"]?.enum)
    }
}
