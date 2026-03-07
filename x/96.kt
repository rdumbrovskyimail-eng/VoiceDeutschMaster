// Путь: src/test/java/com/voicedeutsch/master/voicecore/functions/UIFunctionsTest.kt
package com.voicedeutsch.master.voicecore.functions

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UIFunctionsTest {

    private val declarations = UIFunctions.declarations

    // ── list integrity ────────────────────────────────────────────────────

    @Test
    fun declarations_containsExactly3Functions() {
        assertEquals(3, declarations.size)
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
    fun declarations_firstIsShowWordCard() {
        assertEquals("show_word_card", declarations[0].name)
    }

    @Test
    fun declarations_secondIsShowGrammarHint() {
        assertEquals("show_grammar_hint", declarations[1].name)
    }

    @Test
    fun declarations_thirdIsTriggerCelebration() {
        assertEquals("trigger_celebration", declarations[2].name)
    }

    // ── show_word_card ────────────────────────────────────────────────────

    @Test
    fun showWordCard_parametersNotNull() {
        assertNotNull(declarations[0].parameters)
    }

    @Test
    fun showWordCard_hasWordParam_typeString() {
        assertEquals("string", declarations[0].parameters!!.properties["word"]?.type)
    }

    @Test
    fun showWordCard_hasTranslationParam_typeString() {
        assertEquals("string", declarations[0].parameters!!.properties["translation"]?.type)
    }

    @Test
    fun showWordCard_hasExampleDeParam_typeString() {
        assertEquals("string", declarations[0].parameters!!.properties["example_de"]?.type)
    }

    @Test
    fun showWordCard_hasExampleRuParam_typeString() {
        assertEquals("string", declarations[0].parameters!!.properties["example_ru"]?.type)
    }

    @Test
    fun showWordCard_hasGenderParam_typeString() {
        assertEquals("string", declarations[0].parameters!!.properties["gender"]?.type)
    }

    @Test
    fun showWordCard_requiredContainsWordAndTranslation() {
        val required = declarations[0].parameters!!.required
        assertTrue(required.contains("word"))
        assertTrue(required.contains("translation"))
    }

    @Test
    fun showWordCard_optionalParamsNotRequired() {
        val required = declarations[0].parameters!!.required
        assertFalse(required.contains("example_de"))
        assertFalse(required.contains("example_ru"))
        assertFalse(required.contains("gender"))
    }

    @Test
    fun showWordCard_containsExactly5Params() {
        assertEquals(5, declarations[0].parameters!!.properties.size)
    }

    // ── show_grammar_hint ─────────────────────────────────────────────────

    @Test
    fun showGrammarHint_parametersNotNull() {
        assertNotNull(declarations[1].parameters)
    }

    @Test
    fun showGrammarHint_hasRuleTitleParam_typeString() {
        assertEquals("string", declarations[1].parameters!!.properties["rule_title"]?.type)
    }

    @Test
    fun showGrammarHint_hasExplanationParam_typeString() {
        assertEquals("string", declarations[1].parameters!!.properties["explanation"]?.type)
    }

    @Test
    fun showGrammarHint_hasExampleParam_typeString() {
        assertEquals("string", declarations[1].parameters!!.properties["example"]?.type)
    }

    @Test
    fun showGrammarHint_requiredContainsRuleTitleAndExplanation() {
        val required = declarations[1].parameters!!.required
        assertTrue(required.contains("rule_title"))
        assertTrue(required.contains("explanation"))
    }

    @Test
    fun showGrammarHint_exampleNotRequired() {
        assertFalse(declarations[1].parameters!!.required.contains("example"))
    }

    @Test
    fun showGrammarHint_containsExactly3Params() {
        assertEquals(3, declarations[1].parameters!!.properties.size)
    }

    // ── trigger_celebration ───────────────────────────────────────────────

    @Test
    fun triggerCelebration_parametersNotNull() {
        assertNotNull(declarations[2].parameters)
    }

    @Test
    fun triggerCelebration_hasTypeParam_typeString() {
        assertEquals("string", declarations[2].parameters!!.properties["type"]?.type)
    }

    @Test
    fun triggerCelebration_hasMessageParam_typeString() {
        assertEquals("string", declarations[2].parameters!!.properties["message"]?.type)
    }

    @Test
    fun triggerCelebration_requiredContainsTypeAndMessage() {
        val required = declarations[2].parameters!!.required
        assertTrue(required.contains("type"))
        assertTrue(required.contains("message"))
    }

    @Test
    fun triggerCelebration_containsExactly2Params() {
        assertEquals(2, declarations[2].parameters!!.properties.size)
    }

    @Test
    fun triggerCelebration_descriptionMentionsCelebrationTypes() {
        val description = declarations[2].description
        assertTrue(description.contains("achievement") || description.contains("lesson") || description.contains("streak"))
    }

    @Test
    fun triggerCelebration_typeParamDescriptionMentionsValidValues() {
        val description = declarations[2].parameters!!.properties["type"]?.description ?: ""
        assertTrue(description.contains("achievement"))
        assertTrue(description.contains("lesson_complete"))
        assertTrue(description.contains("streak"))
        assertTrue(description.contains("level_up"))
    }
}
