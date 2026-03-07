// Путь: src/test/java/com/voicedeutsch/master/voicecore/strategy/PronunciationStrategyTest.kt
package com.voicedeutsch.master.voicecore.strategy

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.BookProgressSnapshot
import com.voicedeutsch.master.domain.model.knowledge.GrammarSnapshot
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.domain.model.knowledge.PronunciationSnapshot
import com.voicedeutsch.master.domain.model.knowledge.RecommendationsSnapshot
import com.voicedeutsch.master.domain.model.knowledge.SessionHistorySnapshot
import com.voicedeutsch.master.domain.model.knowledge.VocabularySnapshot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PronunciationStrategyTest {

    private lateinit var strategy: PronunciationStrategy

    @BeforeEach
    fun setUp() {
        strategy = PronunciationStrategy()
    }

    private fun snapshotWithPronunciation(
        problemSounds: List<String>,
        overallScore: Float,
    ) = KnowledgeSnapshot(
        vocabulary = VocabularySnapshot(0, emptyMap(), emptyMap(), emptyList(), emptyList(), 0),
        grammar = GrammarSnapshot(0, emptyMap(), emptyList(), emptyList(), 0),
        pronunciation = PronunciationSnapshot(
            overallScore = overallScore,
            problemSounds = problemSounds,
            goodSounds = emptyList(),
            averageWordScore = 0f,
            trend = ""
        ),
        bookProgress = BookProgressSnapshot(0, 0, 0, 0f, ""),
        sessionHistory = SessionHistorySnapshot("", "", "", 0, 0),
        weakPoints = emptyList(),
        recommendations = RecommendationsSnapshot("", "", emptyList(), "")
    )

    // ── strategy property ────────────────────────────────────────────────

    @Test
    fun strategy_returnsPronunciation() {
        assertEquals(LearningStrategy.PRONUNCIATION, strategy.strategy)
    }

    // ── getStrategyContext ───────────────────────────────────────────────

    @Test
    fun getStrategyContext_returnsNonEmptyString() {
        val context = strategy.getStrategyContext(snapshotWithPronunciation(emptyList(), 0.8f))
        assertTrue(context.isNotBlank())
    }

    @Test
    fun getStrategyContext_containsPronunciationMode() {
        val context = strategy.getStrategyContext(snapshotWithPronunciation(emptyList(), 0.5f))
        assertTrue(context.contains("Тренировка произношения"))
    }

    @Test
    fun getStrategyContext_containsGetPronunciationTargetsReference() {
        val context = strategy.getStrategyContext(snapshotWithPronunciation(emptyList(), 0.5f))
        assertTrue(context.contains("get_pronunciation_targets"))
    }

    @Test
    fun getStrategyContext_containsSavePronunciationResultReference() {
        val context = strategy.getStrategyContext(snapshotWithPronunciation(emptyList(), 0.5f))
        assertTrue(context.contains("save_pronunciation_result"))
    }

    @Test
    fun getStrategyContext_withProblemSounds_containsSoundsInContext() {
        val problems = listOf("ü", "ö", "ch")
        val context = strategy.getStrategyContext(snapshotWithPronunciation(problems, 0.6f))
        problems.forEach { sound ->
            assertTrue(context.contains(sound), "Context should contain problem sound: $sound")
        }
    }

    @Test
    fun getStrategyContext_withProblemSounds_containsProblemSoundsLabel() {
        val context = strategy.getStrategyContext(snapshotWithPronunciation(listOf("r"), 0.7f))
        assertTrue(context.contains("Проблемные звуки"))
    }

    @Test
    fun getStrategyContext_emptyProblemSounds_doesNotContainProblemSoundsLabel() {
        val context = strategy.getStrategyContext(snapshotWithPronunciation(emptyList(), 0.9f))
        assertFalse(context.contains("Проблемные звуки"))
    }

    @Test
    fun getStrategyContext_overallScore_renderedAsPercent() {
        val context = strategy.getStrategyContext(snapshotWithPronunciation(emptyList(), 0.75f))
        assertTrue(context.contains("75%"), "Context should contain score as integer percent")
    }

    @Test
    fun getStrategyContext_overallScoreZero_renderedAsZeroPercent() {
        val context = strategy.getStrategyContext(snapshotWithPronunciation(emptyList(), 0f))
        assertTrue(context.contains("0%"))
    }

    @Test
    fun getStrategyContext_overallScoreOne_renderedAs100Percent() {
        val context = strategy.getStrategyContext(snapshotWithPronunciation(emptyList(), 1f))
        assertTrue(context.contains("100%"))
    }

    @Test
    fun getStrategyContext_containsArticulationHintInstruction() {
        val context = strategy.getStrategyContext(snapshotWithPronunciation(emptyList(), 0.5f))
        assertTrue(context.contains("артикуляци"))
    }

    // ── evaluateMidSession (default) ─────────────────────────────────────

    @Test
    fun evaluateMidSession_zeroValues_returnsNull() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 0,
            errorRate = 0f,
            itemsCompleted = 0,
        )
        assertNull(result)
    }

    @Test
    fun evaluateMidSession_highValues_returnsNull() {
        val result = strategy.evaluateMidSession(
            elapsedMinutes = 999,
            errorRate = 1f,
            itemsCompleted = 999,
        )
        assertNull(result)
    }
}
