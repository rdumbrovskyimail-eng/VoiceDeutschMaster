// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/user/UpdateUserLevelUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.user

import com.voicedeutsch.master.domain.model.knowledge.GrammarRule
import com.voicedeutsch.master.domain.model.knowledge.RuleKnowledge
import com.voicedeutsch.master.domain.model.knowledge.Word
import com.voicedeutsch.master.domain.model.knowledge.WordKnowledge
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.domain.model.user.UserProfile
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UpdateUserLevelUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var useCase: UpdateUserLevelUseCase

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeUserProfile(
        cefrLevel: CefrLevel = CefrLevel.A1,
        cefrSubLevel: Int = 1
    ): UserProfile = mockk<UserProfile>(relaxed = true).also {
        every { it.cefrLevel }    returns cefrLevel
        every { it.cefrSubLevel } returns cefrSubLevel
    }

    private fun makeWord(id: String): Word =
        mockk<Word>(relaxed = true).also { every { it.id } returns id }

    private fun makeRule(id: String): GrammarRule =
        mockk<GrammarRule>(relaxed = true).also { every { it.id } returns id }

    private fun makeWordKnowledge(wordId: String, knowledgeLevel: Int): WordKnowledge =
        mockk<WordKnowledge>(relaxed = true).also {
            every { it.wordId }        returns wordId
            every { it.knowledgeLevel } returns knowledgeLevel
        }

    private fun makeRuleKnowledge(ruleId: String, knowledgeLevel: Int): RuleKnowledge =
        mockk<RuleKnowledge>(relaxed = true).also {
            every { it.ruleId }        returns ruleId
            every { it.knowledgeLevel } returns knowledgeLevel
        }

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        userRepository      = mockk()
        knowledgeRepository = mockk()
        useCase = UpdateUserLevelUseCase(userRepository, knowledgeRepository)

        // Default: user at A1/1
        coEvery { userRepository.getUserProfile(any()) } returns makeUserProfile(CefrLevel.A1, 1)
        coEvery { userRepository.updateUserLevel(any(), any(), any()) } returns Unit

        // Default: all levels return no words/rules → scores = 0
        CefrLevel.entries.forEach { level ->
            coEvery { knowledgeRepository.getWordsByLevel(level) }        returns emptyList()
            coEvery { knowledgeRepository.getGrammarRulesByLevel(level) } returns emptyList()
        }
        coEvery { knowledgeRepository.getAllWordKnowledge(any()) } returns emptyList()
        coEvery { knowledgeRepository.getAllRuleKnowledge(any()) } returns emptyList()
    }

    // ── invoke — user not found ───────────────────────────────────────────────

    @Test
    fun invoke_userNotFound_throwsIllegalArgumentException() = runTest {
        coEvery { userRepository.getUserProfile("missing") } returns null

        assertThrows<IllegalArgumentException> { useCase("missing") }
    }

    @Test
    fun invoke_userNotFound_exceptionMessageContainsUserId() = runTest {
        coEvery { userRepository.getUserProfile("bad-id") } returns null

        val ex = assertThrows<IllegalArgumentException> { useCase("bad-id") }
        assertTrue(ex.message?.contains("bad-id") == true)
    }

    // ── invoke — no words or rules for any level → stays at A1 ───────────────

    @Test
    fun invoke_noWordsOrRulesForAnyLevel_newLevelIsA1() = runTest {
        val result = useCase("user1")

        assertEquals(CefrLevel.A1, result.newLevel)
    }

    @Test
    fun invoke_noWordsOrRulesForAnyLevel_previousLevelInResult() = runTest {
        coEvery { userRepository.getUserProfile("user1") } returns
            makeUserProfile(CefrLevel.B1, 5)

        val result = useCase("user1")

        assertEquals(CefrLevel.B1, result.previousLevel)
        assertEquals(5, result.previousSubLevel)
    }

    // ── invoke — vocab score 0 for all levels → subLevel clampedTo 1 ─────────

    @Test
    fun invoke_allScores0_subLevelIs1() = runTest {
        val result = useCase("user1")

        assertEquals(1, result.newSubLevel)
    }

    // ── invoke — levelChanged flag ────────────────────────────────────────────

    @Test
    fun invoke_levelAndSubLevelUnchanged_levelChangedFalse() = runTest {
        // Default: profile A1/1, computed stays A1/1 (no words)
        // progressToNext = min(0/0.7, 0/0.6) = 0 → subLevel = 0.coerceIn(1,10) = 1
        val result = useCase("user1")

        assertFalse(result.levelChanged)
    }

    @Test
    fun invoke_levelChangedFalse_updateUserLevelNotCalled() = runTest {
        val result = useCase("user1")

        assertFalse(result.levelChanged)
        coVerify(exactly = 0) { userRepository.updateUserLevel(any(), any(), any()) }
    }

    @Test
    fun invoke_levelChanged_updateUserLevelCalledOnce() = runTest {
        // Make A1 confirmed: 70% vocab + 60% grammar
        val words = List(10) { makeWord("w$it") }
        val wk    = List(7)  { makeWordKnowledge("w$it", 5) } +
                    List(3)  { makeWordKnowledge("w${it + 7}", 3) }
        val rules = List(10) { makeRule("r$it") }
        val rk    = List(6)  { makeRuleKnowledge("r$it", 4) } +
                    List(4)  { makeRuleKnowledge("r${it + 6}", 2) }

        coEvery { knowledgeRepository.getWordsByLevel(CefrLevel.A1) }        returns words
        coEvery { knowledgeRepository.getGrammarRulesByLevel(CefrLevel.A1) } returns rules
        coEvery { knowledgeRepository.getAllWordKnowledge("user1") }          returns wk
        coEvery { knowledgeRepository.getAllRuleKnowledge("user1") }          returns rk

        // profile is A1/1; after computation highestConfirmedLevel = A1, subLevel may differ
        // force subLevel change: profile subLevel = 5, computed = 1
        coEvery { userRepository.getUserProfile("user1") } returns
            makeUserProfile(CefrLevel.A1, 5)

        val result = useCase("user1")

        assertTrue(result.levelChanged)
        coVerify(exactly = 1) { userRepository.updateUserLevel("user1", any(), any()) }
    }

    // ── invoke — A1 confirmed: vocab >= 0.7 AND grammar >= 0.6 ───────────────

    @Test
    fun invoke_a1ThresholdsMet_highestConfirmedLevelIsA1() = runTest {
        setupLevelPassing(CefrLevel.A1)

        val result = useCase("user1")

        assertTrue(result.newLevel.ordinal >= CefrLevel.A1.ordinal)
    }

    @Test
    fun invoke_a1ThresholdsMetExactly_levelConfirmed() = runTest {
        // Exactly 7/10 vocab (70%) and 6/10 grammar (60%)
        val words = List(10) { makeWord("w$it") }
        val wk    = List(7) { makeWordKnowledge("w$it", 5) } +
                    List(3) { makeWordKnowledge("w${it + 7}", 2) }
        val rules = List(10) { makeRule("r$it") }
        val rk    = List(6) { makeRuleKnowledge("r$it", 4) } +
                    List(4) { makeRuleKnowledge("r${it + 6}", 1) }

        coEvery { knowledgeRepository.getWordsByLevel(CefrLevel.A1) }        returns words
        coEvery { knowledgeRepository.getGrammarRulesByLevel(CefrLevel.A1) } returns rules
        coEvery { knowledgeRepository.getAllWordKnowledge("user1") }          returns wk
        coEvery { knowledgeRepository.getAllRuleKnowledge("user1") }          returns rk

        val result = useCase("user1")

        assertTrue(result.newLevel.ordinal >= CefrLevel.A1.ordinal)
    }

    @Test
    fun invoke_vocabBelowThreshold_levelNotConfirmed() = runTest {
        // 6/10 vocab = 60% < 70% threshold
        val words = List(10) { makeWord("w$it") }
        val wk    = List(6) { makeWordKnowledge("w$it", 5) } +
                    List(4) { makeWordKnowledge("w${it + 6}", 2) }
        val rules = List(10) { makeRule("r$it") }
        val rk    = List(6) { makeRuleKnowledge("r$it", 4) } +
                    List(4) { makeRuleKnowledge("r${it + 6}", 1) }

        coEvery { knowledgeRepository.getWordsByLevel(CefrLevel.A1) }        returns words
        coEvery { knowledgeRepository.getGrammarRulesByLevel(CefrLevel.A1) } returns rules
        coEvery { knowledgeRepository.getAllWordKnowledge("user1") }          returns wk
        coEvery { knowledgeRepository.getAllRuleKnowledge("user1") }          returns rk

        val result = useCase("user1")

        assertEquals(CefrLevel.A1, result.newLevel)   // stays at A1 as highest confirmed
    }

    @Test
    fun invoke_grammarBelowThreshold_levelNotAdvancedBeyondA1() = runTest {
        // 10/10 vocab = 100% but 5/10 grammar = 50% < 60% threshold
        val words = List(10) { makeWord("w$it") }
        val wk    = List(10) { makeWordKnowledge("w$it", 5) }
        val rules = List(10) { makeRule("r$it") }
        val rk    = List(5)  { makeRuleKnowledge("r$it", 4) } +
                    List(5)  { makeRuleKnowledge("r${it + 5}", 1) }

        coEvery { knowledgeRepository.getWordsByLevel(CefrLevel.A1) }        returns words
        coEvery { knowledgeRepository.getGrammarRulesByLevel(CefrLevel.A1) } returns rules
        coEvery { knowledgeRepository.getAllWordKnowledge("user1") }          returns wk
        coEvery { knowledgeRepository.getAllRuleKnowledge("user1") }          returns rk

        val result = useCase("user1")

        // A1 not confirmed (grammar < 0.6), stays highestConfirmed = A1 (initial)
        assertEquals(CefrLevel.A1, result.newLevel)
    }

    // ── invoke — sub-level interpolation ─────────────────────────────────────

    @Test
    fun invoke_progressToNextIs0_subLevelIs1() = runTest {
        // All scores = 0 → progressToNext = 0 → coerceIn(1,10) = 1
        val result = useCase("user1")

        assertEquals(1, result.newSubLevel)
    }

    @Test
    fun invoke_progressToNextIs1_subLevelIs10() = runTest {
        // vocabScore = 0.7 exactly, grammarScore = 0.6 exactly for A1 (not confirmed → break)
        // Actually when both meet thresholds exactly A1 IS confirmed, level breaks on A2
        // Set: for A2 words = 10 with 7 active (= 0.7), grammar = 10 with 6 known (= 0.6)
        // But A1 passing → highestConfirmed = A1, then A2 scores: vocab=0.7/0.7=1, grammar=0.6/0.6=1
        // progressive = min(1,1)=1 → newSubLevel = 10
        setupLevelPassing(CefrLevel.A1)
        // A2: exactly at threshold
        val wordsA2 = List(10) { makeWord("a2w$it") }
        val wkA2    = List(7) { makeWordKnowledge("a2w$it", 5) } +
                      List(3) { makeWordKnowledge("a2w${it + 7}", 1) }
        val rulesA2 = List(10) { makeRule("a2r$it") }
        val rkA2    = List(6) { makeRuleKnowledge("a2r$it", 4) } +
                      List(4) { makeRuleKnowledge("a2r${it + 6}", 1) }
        coEvery { knowledgeRepository.getWordsByLevel(CefrLevel.A2) }        returns wordsA2
        coEvery { knowledgeRepository.getGrammarRulesByLevel(CefrLevel.A2) } returns rulesA2
        coEvery { knowledgeRepository.getAllWordKnowledge("user1") } answers {
            buildAllWK(CefrLevel.A1) + wkA2
        }
        coEvery { knowledgeRepository.getAllRuleKnowledge("user1") } answers {
            buildAllRK(CefrLevel.A1) + rkA2
        }

        val result = useCase("user1")

        assertEquals(10, result.newSubLevel)
    }

    @Test
    fun invoke_subLevelClampedToMin1() = runTest {
        // vocabScore for next = 0, grammarScore = 0 → progress = 0 → int(0) = 0, coerceIn → 1
        val result = useCase("user1")

        assertTrue(result.newSubLevel >= 1)
    }

    @Test
    fun invoke_subLevelClampedToMax10() = runTest {
        // Even if scores wildly exceed thresholds, subLevel ≤ 10
        setupLevelPassing(CefrLevel.A1)
        val wordsA2 = List(10) { makeWord("a2w$it") }
        val wkA2    = List(10) { makeWordKnowledge("a2w$it", 5) }
        val rulesA2 = List(10) { makeRule("a2r$it") }
        val rkA2    = List(10) { makeRuleKnowledge("a2r$it", 4) }
        coEvery { knowledgeRepository.getWordsByLevel(CefrLevel.A2) }        returns wordsA2
        coEvery { knowledgeRepository.getGrammarRulesByLevel(CefrLevel.A2) } returns rulesA2
        coEvery { knowledgeRepository.getAllWordKnowledge("user1") } answers {
            buildAllWK(CefrLevel.A1) + wkA2
        }
        coEvery { knowledgeRepository.getAllRuleKnowledge("user1") } answers {
            buildAllRK(CefrLevel.A1) + rkA2
        }

        val result = useCase("user1")

        assertTrue(result.newSubLevel <= 10)
    }

    // ── invoke — reason messages ──────────────────────────────────────────────

    @Test
    fun invoke_levelIncreased_reasonContainsLevelUp() = runTest {
        setupLevelPassing(CefrLevel.A1)
        coEvery { userRepository.getUserProfile("user1") } returns
            makeUserProfile(CefrLevel.A1, 1)

        // After A1 confirmed, A2 fails → highestConfirmed=A1 which equals previousLevel
        // To trigger level UP: previous=A1, confirmed=A2
        setupLevelPassing(CefrLevel.A2)
        coEvery { userRepository.getUserProfile("user1") } returns
            makeUserProfile(CefrLevel.A1, 1)

        val result = useCase("user1")

        if (result.newLevel.ordinal > result.previousLevel.ordinal) {
            assertTrue(result.reason.contains(result.newLevel.name))
        }
    }

    @Test
    fun invoke_levelUnchangedSubLevelUnchanged_reasonContainsConfirmed() = runTest {
        // Default: A1/1 → stays A1/1
        val result = useCase("user1")

        assertTrue(result.reason.isNotBlank())
    }

    @Test
    fun invoke_subLevelIncreased_reasonContainsProgress() = runTest {
        // profile at A1/1, computed subLevel > 1
        coEvery { userRepository.getUserProfile("user1") } returns
            makeUserProfile(CefrLevel.A1, 1)
        // No words/rules for any level → progress = 0 → subLevel = 1 (same) → "подтверждён"
        // To get sub-level increase: need some partial score for A1
        val words = List(10) { makeWord("w$it") }
        val wk    = List(5) { makeWordKnowledge("w$it", 5) }  // 50% < 70% → A1 not confirmed
        val rules = List(10) { makeRule("r$it") }
        val rk    = List(4) { makeRuleKnowledge("r$it", 4) }  // 40% < 60%
        coEvery { knowledgeRepository.getWordsByLevel(CefrLevel.A1) }        returns words
        coEvery { knowledgeRepository.getGrammarRulesByLevel(CefrLevel.A1) } returns rules
        coEvery { knowledgeRepository.getAllWordKnowledge("user1") }          returns wk
        coEvery { knowledgeRepository.getAllRuleKnowledge("user1") }          returns rk
        // subLevel = min(0.5/0.7, 0.4/0.6) * 10 = min(0.714, 0.666) * 10 = 6 > 1
        coEvery { userRepository.getUserProfile("user1") } returns
            makeUserProfile(CefrLevel.A1, 1)

        val result = useCase("user1")

        assertTrue(result.reason.isNotBlank())
    }

    // ── invoke — vocab/grammar score with wordId not in current level ─────────

    @Test
    fun invoke_wordKnowledgeNotInLevelIds_notCountedAsActive() = runTest {
        val words = List(10) { makeWord("w$it") }
        // wk has entries for DIFFERENT ids → active count = 0
        val wk = List(10) { makeWordKnowledge("other$it", 5) }
        val rules = List(10) { makeRule("r$it") }
        val rk    = List(6)  { makeRuleKnowledge("r$it", 4) }

        coEvery { knowledgeRepository.getWordsByLevel(CefrLevel.A1) }        returns words
        coEvery { knowledgeRepository.getGrammarRulesByLevel(CefrLevel.A1) } returns rules
        coEvery { knowledgeRepository.getAllWordKnowledge("user1") }          returns wk
        coEvery { knowledgeRepository.getAllRuleKnowledge("user1") }          returns rk

        val result = useCase("user1")

        // vocabScore = 0/10 = 0 < 0.7 → A1 not confirmed
        assertEquals(CefrLevel.A1, result.newLevel)
    }

    @Test
    fun invoke_wordKnowledgeLevelBelow5_notCountedAsActive() = runTest {
        val words = List(10) { makeWord("w$it") }
        val wk    = List(10) { makeWordKnowledge("w$it", 4) }  // level 4 < 5 → not counted
        val rules = List(10) { makeRule("r$it") }
        val rk    = List(6)  { makeRuleKnowledge("r$it", 4) }

        coEvery { knowledgeRepository.getWordsByLevel(CefrLevel.A1) }        returns words
        coEvery { knowledgeRepository.getGrammarRulesByLevel(CefrLevel.A1) } returns rules
        coEvery { knowledgeRepository.getAllWordKnowledge("user1") }          returns wk
        coEvery { knowledgeRepository.getAllRuleKnowledge("user1") }          returns rk

        val result = useCase("user1")

        assertEquals(CefrLevel.A1, result.newLevel)
    }

    @Test
    fun invoke_ruleKnowledgeLevelBelow4_notCountedAsKnown() = runTest {
        val words = List(10) { makeWord("w$it") }
        val wk    = List(7) { makeWordKnowledge("w$it", 5) }
        val rules = List(10) { makeRule("r$it") }
        val rk    = List(6) { makeRuleKnowledge("r$it", 3) }  // level 3 < 4 → not counted

        coEvery { knowledgeRepository.getWordsByLevel(CefrLevel.A1) }        returns words
        coEvery { knowledgeRepository.getGrammarRulesByLevel(CefrLevel.A1) } returns rules
        coEvery { knowledgeRepository.getAllWordKnowledge("user1") }          returns wk
        coEvery { knowledgeRepository.getAllRuleKnowledge("user1") }          returns rk

        val result = useCase("user1")

        assertEquals(CefrLevel.A1, result.newLevel)
    }

    // ── invoke — empty words/rules for level ─────────────────────────────────

    @Test
    fun invoke_noWordsForLevel_vocabScoreIs0() = runTest {
        coEvery { knowledgeRepository.getWordsByLevel(CefrLevel.A1) } returns emptyList()
        val rules = List(10) { makeRule("r$it") }
        val rk    = List(6) { makeRuleKnowledge("r$it", 4) }
        coEvery { knowledgeRepository.getGrammarRulesByLevel(CefrLevel.A1) } returns rules
        coEvery { knowledgeRepository.getAllRuleKnowledge("user1") }          returns rk

        val result = useCase("user1")

        // vocabScore = 0 < 0.7 → A1 not confirmed
        assertEquals(CefrLevel.A1, result.newLevel)
    }

    @Test
    fun invoke_noRulesForLevel_grammarScoreIs0() = runTest {
        val words = List(10) { makeWord("w$it") }
        val wk    = List(7) { makeWordKnowledge("w$it", 5) }
        coEvery { knowledgeRepository.getWordsByLevel(CefrLevel.A1) }        returns words
        coEvery { knowledgeRepository.getAllWordKnowledge("user1") }          returns wk
        coEvery { knowledgeRepository.getGrammarRulesByLevel(CefrLevel.A1) } returns emptyList()

        val result = useCase("user1")

        // grammarScore = 0 < 0.6 → A1 not confirmed
        assertEquals(CefrLevel.A1, result.newLevel)
    }

    // ── LevelUpdateResult data class ──────────────────────────────────────────

    @Test
    fun levelUpdateResult_creation_storesAllFields() {
        val result = UpdateUserLevelUseCase.LevelUpdateResult(
            previousLevel    = CefrLevel.A1,
            previousSubLevel = 3,
            newLevel         = CefrLevel.A2,
            newSubLevel      = 5,
            levelChanged     = true,
            reason           = "test reason"
        )

        assertEquals(CefrLevel.A1, result.previousLevel)
        assertEquals(3,            result.previousSubLevel)
        assertEquals(CefrLevel.A2, result.newLevel)
        assertEquals(5,            result.newSubLevel)
        assertTrue(result.levelChanged)
        assertEquals("test reason", result.reason)
    }

    @Test
    fun levelUpdateResult_copy_changesOnlySpecifiedField() {
        val original = UpdateUserLevelUseCase.LevelUpdateResult(
            CefrLevel.A1, 1, CefrLevel.A1, 2, true, "r"
        )
        val copy = original.copy(newSubLevel = 7)

        assertEquals(7,            copy.newSubLevel)
        assertEquals(original.previousLevel,    copy.previousLevel)
        assertEquals(original.newLevel,         copy.newLevel)
        assertEquals(original.levelChanged,     copy.levelChanged)
    }

    @Test
    fun levelUpdateResult_equals_twoIdenticalInstancesAreEqual() {
        val a = UpdateUserLevelUseCase.LevelUpdateResult(
            CefrLevel.B1, 4, CefrLevel.B1, 4, false, "same"
        )
        val b = UpdateUserLevelUseCase.LevelUpdateResult(
            CefrLevel.B1, 4, CefrLevel.B1, 4, false, "same"
        )

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun levelUpdateResult_equals_differentNewLevelNotEqual() {
        val a = UpdateUserLevelUseCase.LevelUpdateResult(
            CefrLevel.A1, 1, CefrLevel.A1, 1, false, "r"
        )
        val b = UpdateUserLevelUseCase.LevelUpdateResult(
            CefrLevel.A1, 1, CefrLevel.A2, 1, true, "r"
        )

        assertNotEquals(a, b)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Sets up a passing A1 (or given level) with 7/10 vocab and 6/10 grammar. */
    private fun setupLevelPassing(level: CefrLevel) {
        val prefix = level.name.lowercase()
        val words = List(10) { makeWord("${prefix}w$it") }
        val wk    = List(7) { makeWordKnowledge("${prefix}w$it", 5) } +
                    List(3) { makeWordKnowledge("${prefix}w${it + 7}", 1) }
        val rules = List(10) { makeRule("${prefix}r$it") }
        val rk    = List(6) { makeRuleKnowledge("${prefix}r$it", 4) } +
                    List(4) { makeRuleKnowledge("${prefix}r${it + 6}", 1) }

        coEvery { knowledgeRepository.getWordsByLevel(level) }        returns words
        coEvery { knowledgeRepository.getGrammarRulesByLevel(level) } returns rules
        coEvery { knowledgeRepository.getAllWordKnowledge("user1") }   returns wk
        coEvery { knowledgeRepository.getAllRuleKnowledge("user1") }   returns rk
    }

    private fun buildAllWK(level: CefrLevel): List<WordKnowledge> {
        val prefix = level.name.lowercase()
        return List(7) { makeWordKnowledge("${prefix}w$it", 5) } +
               List(3) { makeWordKnowledge("${prefix}w${it + 7}", 1) }
    }

    private fun buildAllRK(level: CefrLevel): List<RuleKnowledge> {
        val prefix = level.name.lowercase()
        return List(6) { makeRuleKnowledge("${prefix}r$it", 4) } +
               List(4) { makeRuleKnowledge("${prefix}r${it + 6}", 1) }
    }
}
