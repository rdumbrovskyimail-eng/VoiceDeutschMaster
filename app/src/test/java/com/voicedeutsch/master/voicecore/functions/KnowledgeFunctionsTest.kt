// Путь: src/test/java/com/voicedeutsch/master/voicecore/functions/KnowledgeFunctionsTest.kt
package com.voicedeutsch.master.voicecore.functions

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KnowledgeFunctionsTest {

    private val declarations = KnowledgeFunctions.declarations

    // ── list integrity ────────────────────────────────────────────────────

    @Test
    fun declarations_containsExactly5Functions() {
        assertEquals(5, declarations.size)
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
    fun declarations_firstIsSaveWordKnowledge() {
        assertEquals("save_word_knowledge", declarations[0].name)
    }

    @Test
    fun declarations_secondIsSaveRuleKnowledge() {
        assertEquals("save_rule_knowledge", declarations[1].name)
    }

    @Test
    fun declarations_thirdIsRecordMistake() {
        assertEquals("record_mistake", declarations[2].name)
    }

    @Test
    fun declarations_fourthIsGetWordsForRepetition() {
        assertEquals("get_words_for_repetition", declarations[3].name)
    }

    @Test
    fun declarations_fifthIsGetWeakPoints() {
        assertEquals("get_weak_points", declarations[4].name)
    }

    // ── save_word_knowledge ───────────────────────────────────────────────

    @Test
    fun saveWordKnowledge_parametersNotNull() {
        assertNotNull(declarations[0].parameters)
    }

    @Test
    fun saveWordKnowledge_hasWordParam_typeString() {
        val params = declarations[0].parameters!!
        assertEquals("string", params.properties["word"]?.type)
    }

    @Test
    fun saveWordKnowledge_hasTranslationParam_typeString() {
        val params = declarations[0].parameters!!
        assertEquals("string", params.properties["translation"]?.type)
    }

    @Test
    fun saveWordKnowledge_hasLevelParam_typeInteger() {
        val params = declarations[0].parameters!!
        assertEquals("integer", params.properties["level"]?.type)
    }

    @Test
    fun saveWordKnowledge_hasQualityParam_typeInteger() {
        val params = declarations[0].parameters!!
        assertEquals("integer", params.properties["quality"]?.type)
    }

    @Test
    fun saveWordKnowledge_hasPronunciationScoreParam_typeNumber() {
        val params = declarations[0].parameters!!
        assertEquals("number", params.properties["pronunciation_score"]?.type)
    }

    @Test
    fun saveWordKnowledge_hasContextParam_typeString() {
        val params = declarations[0].parameters!!
        assertEquals("string", params.properties["context"]?.type)
    }

    @Test
    fun saveWordKnowledge_requiredContainsWordTranslationLevelQuality() {
        val required = declarations[0].parameters!!.required
        assertTrue(required.contains("word"))
        assertTrue(required.contains("translation"))
        assertTrue(required.contains("level"))
        assertTrue(required.contains("quality"))
    }

    @Test
    fun saveWordKnowledge_pronunciationScoreAndContextNotRequired() {
        val required = declarations[0].parameters!!.required
        assertFalse(required.contains("pronunciation_score"))
        assertFalse(required.contains("context"))
    }

    // ── save_rule_knowledge ───────────────────────────────────────────────

    @Test
    fun saveRuleKnowledge_parametersNotNull() {
        assertNotNull(declarations[1].parameters)
    }

    @Test
    fun saveRuleKnowledge_hasRuleIdParam_typeString() {
        val params = declarations[1].parameters!!
        assertEquals("string", params.properties["rule_id"]?.type)
    }

    @Test
    fun saveRuleKnowledge_hasLevelParam_typeInteger() {
        val params = declarations[1].parameters!!
        assertEquals("integer", params.properties["level"]?.type)
    }

    @Test
    fun saveRuleKnowledge_hasQualityParam_typeInteger() {
        val params = declarations[1].parameters!!
        assertEquals("integer", params.properties["quality"]?.type)
    }

    @Test
    fun saveRuleKnowledge_requiredContainsRuleIdAndQuality() {
        val required = declarations[1].parameters!!.required
        assertTrue(required.contains("rule_id"))
        assertTrue(required.contains("quality"))
    }

    @Test
    fun saveRuleKnowledge_levelNotRequired() {
        val required = declarations[1].parameters!!.required
        assertFalse(required.contains("level"))
    }

    // ── record_mistake ────────────────────────────────────────────────────

    @Test
    fun recordMistake_parametersNotNull() {
        assertNotNull(declarations[2].parameters)
    }

    @Test
    fun recordMistake_hasMistakeTypeParam_typeString() {
        val params = declarations[2].parameters!!
        assertEquals("string", params.properties["mistake_type"]?.type)
    }

    @Test
    fun recordMistake_hasUserInputParam_typeString() {
        val params = declarations[2].parameters!!
        assertEquals("string", params.properties["user_input"]?.type)
    }

    @Test
    fun recordMistake_hasCorrectFormParam_typeString() {
        val params = declarations[2].parameters!!
        assertEquals("string", params.properties["correct_form"]?.type)
    }

    @Test
    fun recordMistake_hasExplanationParam_typeString() {
        val params = declarations[2].parameters!!
        assertEquals("string", params.properties["explanation"]?.type)
    }

    @Test
    fun recordMistake_hasContextParam_typeString() {
        val params = declarations[2].parameters!!
        assertEquals("string", params.properties["context"]?.type)
    }

    @Test
    fun recordMistake_requiredContainsMistakeTypeUserInputCorrectForm() {
        val required = declarations[2].parameters!!.required
        assertTrue(required.contains("mistake_type"))
        assertTrue(required.contains("user_input"))
        assertTrue(required.contains("correct_form"))
    }

    @Test
    fun recordMistake_explanationAndContextNotRequired() {
        val required = declarations[2].parameters!!.required
        assertFalse(required.contains("explanation"))
        assertFalse(required.contains("context"))
    }

    @Test
    fun recordMistake_descriptionMentionsAllMistakeTypes() {
        val description = declarations[2].parameters!!.properties["mistake_type"]?.description ?: ""
        assertTrue(description.contains("grammar"))
        assertTrue(description.contains("vocabulary"))
        assertTrue(description.contains("pronunciation"))
        assertTrue(description.contains("phrase"))
    }

    // ── get_words_for_repetition ──────────────────────────────────────────

    @Test
    fun getWordsForRepetition_parametersNotNull() {
        assertNotNull(declarations[3].parameters)
    }

    @Test
    fun getWordsForRepetition_hasLimitParam_typeInteger() {
        val params = declarations[3].parameters!!
        assertEquals("integer", params.properties["limit"]?.type)
    }

    @Test
    fun getWordsForRepetition_limitNotRequired() {
        val required = declarations[3].parameters!!.required
        assertFalse(required.contains("limit"))
    }

    @Test
    fun getWordsForRepetition_requiredIsEmpty() {
        assertTrue(declarations[3].parameters!!.required.isEmpty())
    }

    // ── get_weak_points ───────────────────────────────────────────────────

    @Test
    fun getWeakPoints_parametersIsNull() {
        assertNull(declarations[4].parameters)
    }

    @Test
    fun getWeakPoints_descriptionIsNotBlank() {
        assertTrue(declarations[4].description.isNotBlank())
    }
}
