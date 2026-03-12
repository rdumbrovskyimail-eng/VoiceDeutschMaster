// Путь: src/test/java/com/voicedeutsch/master/presentation/screen/statistics/StatisticsViewModelTest.kt
package com.voicedeutsch.master.presentation.screen.statistics

import app.cash.turbine.test
import com.voicedeutsch.master.domain.model.progress.DailyProgress
import com.voicedeutsch.master.domain.model.progress.OverallProgress
import com.voicedeutsch.master.domain.model.progress.SkillProgress
import com.voicedeutsch.master.domain.repository.ProgressRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.domain.usecase.progress.CalculateOverallProgressUseCase
import com.voicedeutsch.master.domain.usecase.progress.GetDailyProgressUseCase
import com.voicedeutsch.master.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class StatisticsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var calculateOverallProgress: CalculateOverallProgressUseCase
    private lateinit var getDailyProgress: GetDailyProgressUseCase
    private lateinit var progressRepository: ProgressRepository
    private lateinit var userRepository: UserRepository
    private lateinit var sut: StatisticsViewModel

    // ── Helpers ───────────────────────────────────────────────────────────────

    // FIX: OverallProgress структура изменилась — используем mockk чтобы не
    // зависеть от всех вложенных типов (VocabularyProgress, GrammarProgress и т.д.)
    private fun buildOverallProgress(
        totalSessions: Int = 42,
        totalHours: Float = 12.5f,
    ) = mockk<OverallProgress>(relaxed = true).also {
        every { it.totalSessions } returns totalSessions
        every { it.totalHours } returns totalHours
    }

    // FIX: DailyProgress теперь требует id и userId; minutesStudied → totalMinutes
    private fun buildDailyProgress(date: String = "2024-01-01", minutes: Int = 30) =
        DailyProgress(
            id = date,
            userId = "user_1",
            date = date,
            totalMinutes = minutes,
        )

    // FIX: SkillProgress поля переименованы:
    //   vocabularyLevel → vocabulary
    //   grammarLevel    → grammar
    //   listeningLevel  → listening
    //   speakingLevel   → speaking
    //   добавлено поле pronunciation
    private fun buildSkillProgress(
        vocabulary: Float = 0.6f,
        grammar: Float = 0.5f,
        listening: Float = 0.7f,
        speaking: Float = 0.4f,
        pronunciation: Float = 0.5f,
    ) = SkillProgress(
        vocabulary = vocabulary,
        grammar = grammar,
        listening = listening,
        speaking = speaking,
        pronunciation = pronunciation,
    )

    private fun setupDefaultMocks(
        userId: String? = "user_1",
        overall: OverallProgress = buildOverallProgress(),
        weekly: List<DailyProgress> = emptyList(),
        monthly: List<DailyProgress> = emptyList(),
        skill: SkillProgress = buildSkillProgress(),
        streak: Int = 5,
    ) {
        coEvery { userRepository.getActiveUserId() } returns userId
        coEvery { calculateOverallProgress(any()) } returns overall
        coEvery { getDailyProgress.getWeekly(any()) } returns weekly
        coEvery { getDailyProgress.getMonthly(any()) } returns monthly
        coEvery { progressRepository.getSkillProgress(any()) } returns skill
        coEvery { getDailyProgress.getStreak(any()) } returns streak
    }

    private fun createViewModel() = StatisticsViewModel(
        calculateOverallProgress = calculateOverallProgress,
        getDailyProgress = getDailyProgress,
        progressRepository = progressRepository,
        userRepository = userRepository,
    )

    @BeforeEach
    fun setUp() {
        calculateOverallProgress = mockk(relaxed = true)
        getDailyProgress = mockk(relaxed = true)
        progressRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        setupDefaultMocks()
    }

    // ── Initial load ──────────────────────────────────────────────────────────

    @Test
    fun init_success_isLoadingFalse() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_success_populatesOverallProgress() = runTest {
        val overall = buildOverallProgress(totalSessions = 10, totalHours = 5f)
        setupDefaultMocks(overall = overall)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(overall, sut.uiState.value.overallProgress)
    }

    @Test
    fun init_success_populatesTotalSessions() = runTest {
        setupDefaultMocks(overall = buildOverallProgress(totalSessions = 99))

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(99, sut.uiState.value.totalSessions)
    }

    @Test
    fun init_success_populatesTotalHours() = runTest {
        setupDefaultMocks(overall = buildOverallProgress(totalHours = 7.5f))

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(7.5f, sut.uiState.value.totalHours, 0.001f)
    }

    @Test
    fun init_success_populatesWeeklyProgress() = runTest {
        val weekly = listOf(buildDailyProgress("2024-01-01"), buildDailyProgress("2024-01-02"))
        setupDefaultMocks(weekly = weekly)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, sut.uiState.value.weeklyProgress.size)
        assertEquals("2024-01-01", sut.uiState.value.weeklyProgress[0].date)
    }

    @Test
    fun init_success_populatesMonthlyProgress() = runTest {
        val monthly = (1..30).map { buildDailyProgress("2024-01-${it.toString().padStart(2, '0')}") }
        setupDefaultMocks(monthly = monthly)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(30, sut.uiState.value.monthlyProgress.size)
    }

    @Test
    fun init_success_populatesSkillProgress() = runTest {
        // FIX: параметры переименованы
        val skill = buildSkillProgress(vocabulary = 0.9f, grammar = 0.3f)
        setupDefaultMocks(skill = skill)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // FIX: поля переименованы vocabularyLevel → vocabulary, grammarLevel → grammar
        assertEquals(0.9f, sut.uiState.value.skillProgress!!.vocabulary, 0.001f)
        assertEquals(0.3f, sut.uiState.value.skillProgress!!.grammar, 0.001f)
    }

    @Test
    fun init_success_populatesStreak() = runTest {
        setupDefaultMocks(streak = 14)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(14, sut.uiState.value.streak)
    }

    @Test
    fun init_success_noErrorMessage() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun init_noActiveUser_setsErrorMessage() = runTest {
        setupDefaultMocks(userId = null)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("No active user"))
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_calculateOverallProgressThrows_setsErrorMessage() = runTest {
        coEvery { calculateOverallProgress(any()) } throws RuntimeException("Progress error")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("Progress error"))
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_getDailyProgressThrows_setsErrorMessage() = runTest {
        coEvery { getDailyProgress.getWeekly(any()) } throws RuntimeException("Weekly error")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_skillProgressThrows_setsErrorMessage() = runTest {
        coEvery { progressRepository.getSkillProgress(any()) } throws RuntimeException("Skill error")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_callsUseCasesWithActiveUserId() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { calculateOverallProgress("user_1") }
        coVerify { getDailyProgress.getWeekly("user_1") }
        coVerify { getDailyProgress.getMonthly("user_1") }
        coVerify { progressRepository.getSkillProgress("user_1") }
        coVerify { getDailyProgress.getStreak("user_1") }
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    fun refresh_reloadsData() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val newOverall = buildOverallProgress(totalSessions = 100)
        coEvery { calculateOverallProgress(any()) } returns newOverall

        sut.onEvent(StatisticsEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(100, sut.uiState.value.totalSessions)
    }

    @Test
    fun refresh_clearsErrorBeforeReload() = runTest {
        coEvery { calculateOverallProgress(any()) } throws RuntimeException("fail")
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sut.uiState.value.errorMessage)

        setupDefaultMocks()
        sut.onEvent(StatisticsEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun refresh_callsAllUseCasesAgain() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(StatisticsEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) { calculateOverallProgress(any()) }
        coVerify(exactly = 2) { getDailyProgress.getWeekly(any()) }
        coVerify(exactly = 2) { getDailyProgress.getMonthly(any()) }
        coVerify(exactly = 2) { progressRepository.getSkillProgress(any()) }
        coVerify(exactly = 2) { getDailyProgress.getStreak(any()) }
    }

    @Test
    fun refresh_success_isLoadingFalse() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(StatisticsEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.isLoading)
    }

    // ── SelectTab ─────────────────────────────────────────────────────────────

    @Test
    fun selectTab_overview_updatesSelectedTab() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(StatisticsEvent.SelectTab(StatsTab.OVERVIEW))
        assertEquals(StatsTab.OVERVIEW, sut.uiState.value.selectedTab)
    }

    @Test
    fun selectTab_weekly_updatesSelectedTab() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(StatisticsEvent.SelectTab(StatsTab.WEEKLY))
        assertEquals(StatsTab.WEEKLY, sut.uiState.value.selectedTab)
    }

    @Test
    fun selectTab_monthly_updatesSelectedTab() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(StatisticsEvent.SelectTab(StatsTab.MONTHLY))
        assertEquals(StatsTab.MONTHLY, sut.uiState.value.selectedTab)
    }

    @Test
    fun selectTab_skills_updatesSelectedTab() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(StatisticsEvent.SelectTab(StatsTab.SKILLS))
        assertEquals(StatsTab.SKILLS, sut.uiState.value.selectedTab)
    }

    @Test
    fun selectTab_doesNotTriggerReload() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(StatisticsEvent.SelectTab(StatsTab.WEEKLY))

        coVerify(exactly = 1) { calculateOverallProgress(any()) }
    }

    @Test
    fun selectTab_multipleTimes_keepsLatest() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(StatisticsEvent.SelectTab(StatsTab.WEEKLY))
        sut.onEvent(StatisticsEvent.SelectTab(StatsTab.MONTHLY))
        sut.onEvent(StatisticsEvent.SelectTab(StatsTab.SKILLS))

        assertEquals(StatsTab.SKILLS, sut.uiState.value.selectedTab)
    }

    // ── DismissError ──────────────────────────────────────────────────────────

    @Test
    fun dismissError_clearsErrorMessage() = runTest {
        coEvery { calculateOverallProgress(any()) } throws RuntimeException("error")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sut.uiState.value.errorMessage)

        sut.onEvent(StatisticsEvent.DismissError)

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun dismissError_whenNoError_doesNotCrash() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertDoesNotThrow { sut.onEvent(StatisticsEvent.DismissError) }
        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun dismissError_doesNotAffectOtherState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val tabBefore = sut.uiState.value.selectedTab
        val sessionsBefore = sut.uiState.value.totalSessions

        sut.onEvent(StatisticsEvent.DismissError)

        assertEquals(tabBefore, sut.uiState.value.selectedTab)
        assertEquals(sessionsBefore, sut.uiState.value.totalSessions)
    }

    // ── uiState flow ──────────────────────────────────────────────────────────

    @Test
    fun uiState_flow_emitsOnTabChange() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.test {
            awaitItem()

            sut.onEvent(StatisticsEvent.SelectTab(StatsTab.WEEKLY))
            val updated = awaitItem()
            assertEquals(StatsTab.WEEKLY, updated.selectedTab)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_flow_emitsOnRefresh() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.test {
            awaitItem()

            sut.onEvent(StatisticsEvent.Refresh)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            val states = mutableListOf<StatisticsUiState>()
            while (true) {
                val s = awaitItem()
                states.add(s)
                if (!s.isLoading) break
            }
            assertFalse(states.last().isLoading)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── StatisticsUiState data class ──────────────────────────────────────────

    @Test
    fun statisticsUiState_defaultValues_areCorrect() {
        val state = StatisticsUiState()
        assertTrue(state.isLoading)
        assertEquals(StatsTab.OVERVIEW, state.selectedTab)
        assertNull(state.overallProgress)
        assertEquals(emptyList<DailyProgress>(), state.weeklyProgress)
        assertEquals(emptyList<DailyProgress>(), state.monthlyProgress)
        assertNull(state.skillProgress)
        assertEquals(0, state.streak)
        assertEquals(0, state.totalSessions)
        assertEquals(0f, state.totalHours, 0.001f)
        assertNull(state.errorMessage)
    }

    @Test
    fun statisticsUiState_copy_changesOneField() {
        val original = StatisticsUiState()
        val copy = original.copy(streak = 7)
        assertEquals(7, copy.streak)
        assertEquals(original.selectedTab, copy.selectedTab)
    }

    @Test
    fun statisticsUiState_equals_twoIdenticalInstances() {
        val a = StatisticsUiState()
        val b = StatisticsUiState()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ── StatsTab enum ─────────────────────────────────────────────────────────

    @Test
    fun statsTab_allValuesPresent() {
        val tabs = StatsTab.entries
        assertTrue(tabs.contains(StatsTab.OVERVIEW))
        assertTrue(tabs.contains(StatsTab.WEEKLY))
        assertTrue(tabs.contains(StatsTab.MONTHLY))
        assertTrue(tabs.contains(StatsTab.SKILLS))
        assertEquals(4, tabs.size)
    }
}
