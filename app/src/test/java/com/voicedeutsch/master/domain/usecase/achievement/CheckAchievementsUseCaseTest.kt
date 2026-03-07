// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/achievement/CheckAchievementsUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.achievement

import com.voicedeutsch.master.domain.model.achievement.Achievement
import com.voicedeutsch.master.domain.model.achievement.AchievementCondition
import com.voicedeutsch.master.domain.model.achievement.UserAchievement
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.domain.model.user.UserProfile
import com.voicedeutsch.master.domain.model.user.UserStatistics
import com.voicedeutsch.master.domain.repository.AchievementRepository
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.domain.repository.ProgressRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CheckAchievementsUseCaseTest {

    private lateinit var achievementRepository: AchievementRepository
    private lateinit var userRepository: UserRepository
    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var progressRepository: ProgressRepository
    private lateinit var useCase: CheckAchievementsUseCase

    private val grantedAchievement = mockk<UserAchievement>(relaxed = true)

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeCondition(type: String, threshold: Int): AchievementCondition =
        mockk<AchievementCondition>(relaxed = true).also {
            every { it.type }      returns type
            every { it.threshold } returns threshold
        }

    private fun makeAchievement(id: String, conditionType: String, threshold: Int): Achievement =
        mockk<Achievement>(relaxed = true).also {
            every { it.id }        returns id
            every { it.condition } returns makeCondition(conditionType, threshold)
        }

    private fun makeProfile(
        streakDays: Int = 0,
        cefrLevel: CefrLevel = CefrLevel.A1
    ): UserProfile = mockk<UserProfile>(relaxed = true).also {
        every { it.streakDays } returns streakDays
        every { it.cefrLevel }  returns cefrLevel
    }

    private fun makeStats(totalMinutes: Int = 0): UserStatistics =
        mockk<UserStatistics>(relaxed = true).also {
            every { it.totalMinutes } returns totalMinutes
        }

    private fun makeCefrLevel(order: Int): CefrLevel =
        mockk<CefrLevel>(relaxed = true).also {
            every { it.order } returns order
        }

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        achievementRepository = mockk()
        userRepository        = mockk()
        knowledgeRepository   = mockk()
        progressRepository    = mockk()
        useCase = CheckAchievementsUseCase(
            achievementRepository, userRepository, knowledgeRepository, progressRepository
        )

        coEvery { achievementRepository.getAllAchievements() }               returns emptyList()
        coEvery { achievementRepository.hasAchievement(any(), any()) }       returns false
        coEvery { achievementRepository.grantAchievement(any(), any()) }     returns null

        coEvery { userRepository.getUserProfile(any()) }     returns makeProfile()
        coEvery { userRepository.getUserStatistics(any()) }  returns makeStats()

        coEvery { knowledgeRepository.getKnownWordsCount(any()) }              returns 0
        coEvery { knowledgeRepository.getKnownRulesCount(any()) }              returns 0
        coEvery { knowledgeRepository.getPerfectPronunciationCount(any()) }    returns 0
        coEvery { knowledgeRepository.getAveragePronunciationScore(any()) }    returns 0f

        coEvery { progressRepository.getCompletedChapterCount(any()) }         returns 0
    }

    // ── invoke — early exits ──────────────────────────────────────────────────

    @Test
    fun invoke_noAchievements_returnsEmptyList() = runTest {
        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_profileNotFound_returnsEmptyList() = runTest {
        coEvery { userRepository.getUserProfile("user1") } returns null
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("a1", "streak_days", 3))

        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_profileNotFound_grantNeverCalled() = runTest {
        coEvery { userRepository.getUserProfile(any()) } returns null
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("a1", "streak_days", 1))

        useCase("user1")

        coVerify(exactly = 0) { achievementRepository.grantAchievement(any(), any()) }
    }

    // ── invoke — already has achievement ─────────────────────────────────────

    @Test
    fun invoke_alreadyHasAchievement_skipped() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("a1", "streak_days", 1))
        coEvery { achievementRepository.hasAchievement("user1", "a1") } returns true
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(streakDays = 10)

        val result = useCase("user1")

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { achievementRepository.grantAchievement(any(), any()) }
    }

    // ── invoke — streak_days ──────────────────────────────────────────────────

    @Test
    fun invoke_streakDaysMet_achievementGranted() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("streak3", "streak_days", 3))
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(streakDays = 3)
        coEvery { achievementRepository.grantAchievement("user1", "streak3") } returns
            grantedAchievement

        val result = useCase("user1")

        assertEquals(listOf(grantedAchievement), result)
    }

    @Test
    fun invoke_streakDaysExceeded_achievementGranted() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("streak3", "streak_days", 3))
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(streakDays = 10)
        coEvery { achievementRepository.grantAchievement("user1", "streak3") } returns
            grantedAchievement

        val result = useCase("user1")

        assertEquals(1, result.size)
    }

    @Test
    fun invoke_streakDaysBelowThreshold_notGranted() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("streak3", "streak_days", 3))
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(streakDays = 2)

        val result = useCase("user1")

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { achievementRepository.grantAchievement(any(), any()) }
    }

    // ── invoke — word_count ───────────────────────────────────────────────────

    @Test
    fun invoke_wordCountMet_achievementGranted() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("words100", "word_count", 100))
        coEvery { knowledgeRepository.getKnownWordsCount("user1") } returns 100
        coEvery { achievementRepository.grantAchievement("user1", "words100") } returns
            grantedAchievement

        val result = useCase("user1")

        assertEquals(listOf(grantedAchievement), result)
    }

    @Test
    fun invoke_wordCountBelowThreshold_notGranted() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("words100", "word_count", 100))
        coEvery { knowledgeRepository.getKnownWordsCount("user1") } returns 99

        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    // ── invoke — rule_count ───────────────────────────────────────────────────

    @Test
    fun invoke_ruleCountMet_achievementGranted() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("rules50", "rule_count", 50))
        coEvery { knowledgeRepository.getKnownRulesCount("user1") } returns 50
        coEvery { achievementRepository.grantAchievement("user1", "rules50") } returns
            grantedAchievement

        val result = useCase("user1")

        assertEquals(listOf(grantedAchievement), result)
    }

    @Test
    fun invoke_ruleCountBelowThreshold_notGranted() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("rules50", "rule_count", 50))
        coEvery { knowledgeRepository.getKnownRulesCount("user1") } returns 49

        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    // ── invoke — total_minutes ────────────────────────────────────────────────

    @Test
    fun invoke_totalMinutesMet_achievementGranted() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("min60", "total_minutes", 60))
        coEvery { userRepository.getUserStatistics("user1") } returns makeStats(totalMinutes = 60)
        coEvery { achievementRepository.grantAchievement("user1", "min60") } returns
            grantedAchievement

        val result = useCase("user1")

        assertEquals(listOf(grantedAchievement), result)
    }

    @Test
    fun invoke_totalMinutesBelowThreshold_notGranted() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("min60", "total_minutes", 60))
        coEvery { userRepository.getUserStatistics("user1") } returns makeStats(totalMinutes = 59)

        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    // ── invoke — chapters_completed ───────────────────────────────────────────

    @Test
    fun invoke_chaptersCompletedMet_achievementGranted() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("ch3", "chapters_completed", 3))
        coEvery { progressRepository.getCompletedChapterCount("user1") } returns 3
        coEvery { achievementRepository.grantAchievement("user1", "ch3") } returns
            grantedAchievement

        val result = useCase("user1")

        assertEquals(listOf(grantedAchievement), result)
    }

    @Test
    fun invoke_chaptersCompletedBelowThreshold_notGranted() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("ch3", "chapters_completed", 3))
        coEvery { progressRepository.getCompletedChapterCount("user1") } returns 2

        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_chaptersCompleted_progressRepositoryCalledWithUserId() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("ch3", "chapters_completed", 3))

        useCase("user42")

        coVerify { progressRepository.getCompletedChapterCount("user42") }
    }

    // ── invoke — cefr_level ───────────────────────────────────────────────────

    @Test
    fun invoke_cefrLevelOrderMet_achievementGranted() = runTest {
        val cefrLevel = makeCefrLevel(order = 3)
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("cefr3", "cefr_level", 3))
        coEvery { userRepository.getUserProfile("user1") } returns mockk<UserProfile>(relaxed = true).also {
            every { it.streakDays } returns 0
            every { it.cefrLevel }  returns cefrLevel
        }
        coEvery { achievementRepository.grantAchievement("user1", "cefr3") } returns
            grantedAchievement

        val result = useCase("user1")

        assertEquals(listOf(grantedAchievement), result)
    }

    @Test
    fun invoke_cefrLevelOrderBelowThreshold_notGranted() = runTest {
        val cefrLevel = makeCefrLevel(order = 2)
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("cefr3", "cefr_level", 3))
        coEvery { userRepository.getUserProfile("user1") } returns mockk<UserProfile>(relaxed = true).also {
            every { it.streakDays } returns 0
            every { it.cefrLevel }  returns cefrLevel
        }

        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    // ── invoke — perfect_pronunciation ───────────────────────────────────────

    @Test
    fun invoke_perfectPronunciationCountMet_achievementGranted() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("perf10", "perfect_pronunciation", 10))
        coEvery { knowledgeRepository.getPerfectPronunciationCount("user1") } returns 10
        coEvery { achievementRepository.grantAchievement("user1", "perf10") } returns
            grantedAchievement

        val result = useCase("user1")

        assertEquals(listOf(grantedAchievement), result)
    }

    @Test
    fun invoke_perfectPronunciationCountBelowThreshold_notGranted() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("perf10", "perfect_pronunciation", 10))
        coEvery { knowledgeRepository.getPerfectPronunciationCount("user1") } returns 9

        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    // ── invoke — avg_pronunciation ────────────────────────────────────────────

    @Test
    fun invoke_avgPronunciationMet_achievementGranted() = runTest {
        // avg=0.80f → (0.80*100).toInt()=80 >= threshold 80
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("pron80", "avg_pronunciation", 80))
        coEvery { knowledgeRepository.getAveragePronunciationScore("user1") } returns 0.80f
        coEvery { achievementRepository.grantAchievement("user1", "pron80") } returns
            grantedAchievement

        val result = useCase("user1")

        assertEquals(listOf(grantedAchievement), result)
    }

    @Test
    fun invoke_avgPronunciationBelowThreshold_notGranted() = runTest {
        // avg=0.79f → (0.79*100).toInt()=79 < 80
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("pron80", "avg_pronunciation", 80))
        coEvery { knowledgeRepository.getAveragePronunciationScore("user1") } returns 0.79f

        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_avgPronunciationExactlyConverts_intComparison() = runTest {
        // avg=0.799f → (0.799*100).toInt()=79 < 80 → not granted
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("pron80", "avg_pronunciation", 80))
        coEvery { knowledgeRepository.getAveragePronunciationScore("user1") } returns 0.799f

        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    // ── invoke — unknown condition type ───────────────────────────────────────

    @Test
    fun invoke_unknownConditionType_notGranted() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("a1", "unknown_type", 5))

        val result = useCase("user1")

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { achievementRepository.grantAchievement(any(), any()) }
    }

    // ── invoke — grantAchievement returns null ────────────────────────────────

    @Test
    fun invoke_grantReturnsNull_notAddedToResult() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns
            listOf(makeAchievement("streak3", "streak_days", 3))
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(streakDays = 5)
        coEvery { achievementRepository.grantAchievement("user1", "streak3") } returns null

        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    // ── invoke — multiple achievements ───────────────────────────────────────

    @Test
    fun invoke_multipleEarned_allGrantedReturned() = runTest {
        val ua1 = mockk<UserAchievement>(relaxed = true)
        val ua2 = mockk<UserAchievement>(relaxed = true)

        coEvery { achievementRepository.getAllAchievements() } returns listOf(
            makeAchievement("streak3",  "streak_days", 3),
            makeAchievement("words100", "word_count",  100)
        )
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(streakDays = 5)
        coEvery { knowledgeRepository.getKnownWordsCount("user1") } returns 100

        coEvery { achievementRepository.grantAchievement("user1", "streak3")  } returns ua1
        coEvery { achievementRepository.grantAchievement("user1", "words100") } returns ua2

        val result = useCase("user1")

        assertEquals(2, result.size)
        assertTrue(result.contains(ua1))
        assertTrue(result.contains(ua2))
    }

    @Test
    fun invoke_someEarnedSomeNot_onlyEarnedReturned() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns listOf(
            makeAchievement("streak3",   "streak_days", 3),
            makeAchievement("words1000", "word_count",  1000)
        )
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(streakDays = 5)
        coEvery { knowledgeRepository.getKnownWordsCount("user1") } returns 50

        coEvery { achievementRepository.grantAchievement("user1", "streak3") } returns
            grantedAchievement

        val result = useCase("user1")

        assertEquals(1, result.size)
        assertEquals(grantedAchievement, result.single())
    }

    @Test
    fun invoke_alreadyGrantedAchievementSkipped_onlyNewOnesReturned() = runTest {
        val ua = mockk<UserAchievement>(relaxed = true)

        coEvery { achievementRepository.getAllAchievements() } returns listOf(
            makeAchievement("streak3",  "streak_days", 3),
            makeAchievement("words100", "word_count",  100)
        )
        coEvery { userRepository.getUserProfile("user1") } returns makeProfile(streakDays = 5)
        coEvery { knowledgeRepository.getKnownWordsCount("user1") } returns 100

        coEvery { achievementRepository.hasAchievement("user1", "streak3") }  returns true
        coEvery { achievementRepository.hasAchievement("user1", "words100") } returns false
        coEvery { achievementRepository.grantAchievement("user1", "words100") } returns ua

        val result = useCase("user1")

        assertEquals(1, result.size)
        assertEquals(ua, result.single())
    }

    // ── invoke — userId propagated correctly ──────────────────────────────────

    @Test
    fun invoke_allRepositoriesCalledWithCorrectUserId() = runTest {
        coEvery { achievementRepository.getAllAchievements() } returns listOf(
            makeAchievement("a1", "chapters_completed", 1),
            makeAchievement("a2", "perfect_pronunciation", 1),
            makeAchievement("a3", "avg_pronunciation", 50)
        )
        coEvery { progressRepository.getCompletedChapterCount("user99") }         returns 0
        coEvery { knowledgeRepository.getPerfectPronunciationCount("user99") }    returns 0
        coEvery { knowledgeRepository.getAveragePronunciationScore("user99") }    returns 0f

        useCase("user99")

        coVerify { userRepository.getUserProfile("user99") }
        coVerify { userRepository.getUserStatistics("user99") }
        coVerify { knowledgeRepository.getKnownWordsCount("user99") }
        coVerify { knowledgeRepository.getKnownRulesCount("user99") }
    }
}
