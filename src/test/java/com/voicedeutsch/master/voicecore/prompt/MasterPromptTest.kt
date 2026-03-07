// Путь: src/test/java/com/voicedeutsch/master/voicecore/prompt/MasterPromptTest.kt
package com.voicedeutsch.master.voicecore.prompt

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MasterPromptTest {

    // ── build ─────────────────────────────────────────────────────────────────

    @Test
    fun build_returnsNonEmptyString() {
        assertTrue(MasterPrompt.build().isNotEmpty())
    }

    @Test
    fun build_calledTwice_returnsSameContent() {
        assertEquals(MasterPrompt.build(), MasterPrompt.build())
    }

    @Test
    fun build_doesNotStartWithWhitespace() {
        val result = MasterPrompt.build()
        assertEquals(result.trimStart(), result)
    }

    @Test
    fun build_doesNotEndWithWhitespace() {
        val result = MasterPrompt.build()
        assertEquals(result.trimEnd(), result)
    }

    // ── critical language rule block ──────────────────────────────────────────

    @Test
    fun build_containsCriticalLanguageRule() {
        assertTrue(MasterPrompt.build().contains("КРИТИЧЕСКОЕ ПРАВИЛО ЯЗЫКА"))
    }

    @Test
    fun build_languageRuleAppearsBeforeRoleDefinition() {
        val prompt = MasterPrompt.build()
        val ruleIndex = prompt.indexOf("КРИТИЧЕСКОЕ ПРАВИЛО ЯЗЫКА")
        val roleIndex = prompt.indexOf("РАЗДЕЛ 1")
        assertTrue(ruleIndex < roleIndex)
    }

    @Test
    fun build_containsRussianAsMainLanguageInstruction() {
        assertTrue(MasterPrompt.build().contains("РУССКИЙ"))
    }

    // ── all sections present ──────────────────────────────────────────────────

    @Test
    fun build_containsSection1Identity() {
        assertTrue(MasterPrompt.build().contains("РАЗДЕЛ 1"))
    }

    @Test
    fun build_containsSection2Languages() {
        assertTrue(MasterPrompt.build().contains("РАЗДЕЛ 2"))
    }

    @Test
    fun build_containsSection3SessionStart() {
        assertTrue(MasterPrompt.build().contains("РАЗДЕЛ 3"))
    }

    @Test
    fun build_containsSection4ErrorHandling() {
        assertTrue(MasterPrompt.build().contains("РАЗДЕЛ 4"))
    }

    @Test
    fun build_containsSection5Strategies() {
        assertTrue(MasterPrompt.build().contains("РАЗДЕЛ 5"))
    }

    @Test
    fun build_containsSection6Book() {
        assertTrue(MasterPrompt.build().contains("РАЗДЕЛ 6"))
    }

    @Test
    fun build_containsSection7FunctionCalls() {
        assertTrue(MasterPrompt.build().contains("РАЗДЕЛ 7"))
    }

    @Test
    fun build_containsSection8Motivation() {
        assertTrue(MasterPrompt.build().contains("РАЗДЕЛ 8"))
    }

    @Test
    fun build_containsSection9Restrictions() {
        assertTrue(MasterPrompt.build().contains("РАЗДЕЛ 9"))
    }

    @Test
    fun build_containsSection10SessionEnd() {
        assertTrue(MasterPrompt.build().contains("РАЗДЕЛ 10"))
    }

    @Test
    fun build_containsSection11Technical() {
        assertTrue(MasterPrompt.build().contains("РАЗДЕЛ 11"))
    }

    // ── function call instructions ────────────────────────────────────────────

    @Test
    fun build_mentionsSaveWordKnowledge() {
        assertTrue(MasterPrompt.build().contains("save_word_knowledge"))
    }

    @Test
    fun build_mentionsSaveRuleKnowledge() {
        assertTrue(MasterPrompt.build().contains("save_rule_knowledge"))
    }

    @Test
    fun build_mentionsRecordMistake() {
        assertTrue(MasterPrompt.build().contains("record_mistake"))
    }

    @Test
    fun build_mentionsMarkLessonComplete() {
        assertTrue(MasterPrompt.build().contains("mark_lesson_complete"))
    }

    @Test
    fun build_mentionsAdvanceToNextLesson() {
        assertTrue(MasterPrompt.build().contains("advance_to_next_lesson"))
    }

    @Test
    fun build_mentionsReadLessonParagraph() {
        assertTrue(MasterPrompt.build().contains("read_lesson_paragraph"))
    }

    @Test
    fun build_mentionsSetCurrentStrategy() {
        assertTrue(MasterPrompt.build().contains("set_current_strategy"))
    }

    @Test
    fun build_mentionsGetWordsForRepetition() {
        assertTrue(MasterPrompt.build().contains("get_words_for_repetition"))
    }

    @Test
    fun build_mentionsGetWeakPoints() {
        assertTrue(MasterPrompt.build().contains("get_weak_points"))
    }

    @Test
    fun build_mentionsSavePronunciationResult() {
        assertTrue(MasterPrompt.build().contains("save_pronunciation_result"))
    }

    // ── text_truncated pagination fix ─────────────────────────────────────────

    @Test
    fun build_mentionsTextTruncated() {
        assertTrue(MasterPrompt.build().contains("text_truncated"))
    }

    @Test
    fun build_mentionsHasNext() {
        assertTrue(MasterPrompt.build().contains("has_next"))
    }

    @Test
    fun build_readLessonParagraphInstructionAppearsInSection6() {
        val prompt = MasterPrompt.build()
        val section6Start = prompt.indexOf("РАЗДЕЛ 6")
        val section7Start = prompt.indexOf("РАЗДЕЛ 7")
        val paragraphInstruction = prompt.indexOf("read_lesson_paragraph(index)")
        assertTrue(paragraphInstruction in section6Start until section7Start)
    }

    // ── strategy names ────────────────────────────────────────────────────────

    @Test
    fun build_containsLinearBookStrategy() {
        assertTrue(MasterPrompt.build().contains("LINEAR_BOOK"))
    }

    @Test
    fun build_containsRepetitionStrategy() {
        assertTrue(MasterPrompt.build().contains("REPETITION"))
    }

    @Test
    fun build_containsGapFillingStrategy() {
        assertTrue(MasterPrompt.build().contains("GAP_FILLING"))
    }

    @Test
    fun build_containsGrammarDrillStrategy() {
        assertTrue(MasterPrompt.build().contains("GRAMMAR_DRILL"))
    }

    @Test
    fun build_containsVocabularyBoostStrategy() {
        assertTrue(MasterPrompt.build().contains("VOCABULARY_BOOST"))
    }

    @Test
    fun build_containsPronunciationStrategy() {
        assertTrue(MasterPrompt.build().contains("PRONUNCIATION"))
    }

    @Test
    fun build_containsFreePracticeStrategy() {
        assertTrue(MasterPrompt.build().contains("FREE_PRACTICE"))
    }

    // ── SRS quality scale ─────────────────────────────────────────────────────

    @Test
    fun build_containsQualityScale0to5() {
        val prompt = MasterPrompt.build()
        assertTrue(prompt.contains("0"))
        assertTrue(prompt.contains("5 —"))
    }

    // ── prohibitions ─────────────────────────────────────────────────────────

    @Test
    fun build_containsProhibitionSection() {
        assertTrue(MasterPrompt.build().contains("ЗАПРЕЩЕНО"))
    }

    @Test
    fun build_containsGermanOnlyForExamplesRule() {
        val prompt = MasterPrompt.build()
        assertTrue(prompt.contains("ТОЛЬКО") && prompt.contains("РУССКОМ"))
    }
}
