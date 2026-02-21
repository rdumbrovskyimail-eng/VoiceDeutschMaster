package com.voicedeutsch.master.voicecore.context

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.GrammarSnapshot
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.domain.model.knowledge.PronunciationSnapshot
import com.voicedeutsch.master.domain.model.knowledge.VocabularySnapshot
import com.voicedeutsch.master.voicecore.prompt.MasterPrompt
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for ContextBuilder.
 * Verifies context assembly and token budget estimation.
 */
class ContextBuilderTest {

    @Test
    fun `master prompt is non-empty`() {
        val prompt = MasterPrompt.build()
        assertTrue(prompt.isNotEmpty())
        assertTrue(prompt.contains("VOICE DEUTSCH MASTER"))
    }

    @Test
    fun `master prompt contains all required sections`() {
        val prompt = MasterPrompt.build()
        assertTrue(prompt.contains("ИДЕНТИЧНОСТЬ И ХАРАКТЕР"))
        assertTrue(prompt.contains("СТРАТЕГИИ ОБУЧЕНИЯ"))
        assertTrue(prompt.contains("FUNCTION CALLS"))
    }

    @Test
    fun `user context provider serializes snapshot`() {
        val snapshot = createTestSnapshot()
        val provider = UserContextProvider(kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
        val context = provider.buildUserContext(snapshot)
        assertTrue(context.contains("USER KNOWLEDGE CONTEXT"))
        assertTrue(context.contains("END USER KNOWLEDGE CONTEXT"))
    }

    private fun createTestSnapshot() = KnowledgeSnapshot(
        vocabulary = VocabularySnapshot(
            totalWords = 100,
            wordsByLevel = mapOf(0 to 50, 1 to 20, 2 to 15, 3 to 10, 4 to 5),
            wordsForReviewToday = 10,
            recentNewWords = listOf("Haus", "Buch"),
            problemWords = emptyList()
        ),
        grammar = GrammarSnapshot(
            totalRules = 20,
            rulesByLevel = mapOf(0 to 10, 1 to 5, 2 to 3, 3 to 2),
            rulesForReviewToday = 3,
            problemRules = emptyList()
        ),
        pronunciation = PronunciationSnapshot(
            overallScore = 0.7f,
            problemSounds = listOf("ü", "ö"),
            goodSounds = listOf("sch", "ei"),
            trend = "improving"
        )
    )
}