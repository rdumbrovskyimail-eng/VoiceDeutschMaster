package com.voicedeutsch.master.voicecore.prompt

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.GrammarSnapshot
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.domain.model.knowledge.PronunciationSnapshot
import com.voicedeutsch.master.domain.model.knowledge.VocabularySnapshot
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MasterPromptTest {

    @Test
    fun `master prompt is substantial`() {
        val prompt = MasterPrompt.build()
        // Should be at least 3000 chars as per architecture (3000-5000 tokens ≈ 10K+ chars)
        assertTrue(prompt.length > 3000, "Master prompt too short: ${prompt.length} chars")
    }

    @Test
    fun `all strategy prompts are non-empty`() {
        val snapshot = createTestSnapshot()
        LearningStrategy.entries.forEach { strategy ->
            val prompt = PromptTemplates.getStrategyPrompt(strategy, snapshot)
            assertTrue(prompt.isNotBlank(), "Strategy prompt for $strategy is blank")
        }
    }

    private fun createTestSnapshot() = KnowledgeSnapshot(
        vocabulary = VocabularySnapshot(
            totalWords = 50, wordsByLevel = emptyMap(), wordsForReviewToday = 5,
            recentNewWords = emptyList(), problemWords = emptyList()
        ),
        grammar = GrammarSnapshot(
            totalRules = 10, rulesByLevel = emptyMap(), rulesForReviewToday = 2,
            problemRules = emptyList()
        ),
        pronunciation = PronunciationSnapshot(
            overallScore = 0.6f, problemSounds = emptyList(),
            goodSounds = emptyList(), trend = "stable"
        )
    )
}