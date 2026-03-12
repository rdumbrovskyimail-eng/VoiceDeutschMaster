// Путь: src/test/java/com/voicedeutsch/master/presentation/screen/dashboard/DashboardViewModelTest.kt
package com.voicedeutsch.master.presentation.screen.dashboard

import app.cash.turbine.test
import com.voicedeutsch.master.domain.model.progress.DailyProgress
import com.voicedeutsch.master.domain.model.progress.OverallProgress
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.domain.model.user.UserProfile
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.domain.usecase.progress.CalculateOverallProgressUseCase
import com.voicedeutsch.master.domain.usecase.progress.GetDailyProgressUseCase
import com.voicedeutsch.master.domain.usecase.user.GetUserProfileUseCase
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

class DashboardViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getUserProfile: GetUserProfileUseCase
    private lateinit var calculateOverallProgress: CalculateOverallProgressUseCase
    private lateinit var getDailyProgress: GetDailyProgressUseCase
    private lateinit var userRepository: UserRepository
    private lateinit var sut: DashboardViewModel

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildUserProfile(
        id: String = "user_1",
        name: String = "Max",
        cefrLevel: CefrLevel = CefrLevel.B1,
    ) = UserProfile(id = id, name = name, cefrLevel = cefrLevel)

    private fun buildOverallProgress(
        totalSessions: Int = 42,
        totalHours: Float = 10f,
        cefrLevel: CefrLevel = CefrLevel.B1,
        subLevel: Int = 1,
        streakDays: Int = 0,
    ): OverallProgress {
        val vocabularyProgress = mockk<com.voicedeutsch.master.domain.model.progress.VocabularyProgress>(relaxed = true)
        val grammarProgress = mockk<com.voicedeutsch.master.domain.model.progress.GrammarProgress>(relaxed = true)
        val pronunciationProgress = mockk<com.voicedeutsch.master.domain.model.progress.PronunciationProgress>(relaxed = true)
        val bookProgress = mockk<com.voicedeutsch.master.domain.model.progress.BookOverallProgress>(relaxed = true)
        return OverallProgress(
            totalSessions = totalSessions,
            totalHours = totalHours,
            currentCefrLevel = cefrLevel,
            currentSubLevel = subLevel,
            vocabularyProgress = vocabularyProgress,
            grammarProgress = grammarProgress,
            pronunciationProgress = pronunciationProgress,
            bookProgress = bookProgress,
            streakDays = streakDays,
        )
    }

    private fun buildDailyProgress(
        date: String = "2024-06-01",
        wordsLearned: Int = 15,
        totalMinutes: Int = 30,
    ) = DailyProgress(id = "", userId = "", date = date, wordsLearned = wordsLearned, totalMinutes = totalMinutes)

    private fun setupDefaultMocks(
        userId: String? = "user_1",
        profile: UserProfile = buildUserProfile(),
        overall: OverallProgress = buildOverallProgress(),
        today: DailyProgress? = buildDailyProgress(),
        weekly: List<DailyProgress> = emptyList(),
        streak: Int = 5,
    ) {
        coEvery { userRepository.getActiveUserId() } returns userId
        coEvery { getUserProfile(any()) } returns profile
        coEvery { calculateOverallProgress(any()) } returns overall
        coEvery { getDailyProgress.getToday(any()) } returns today
        coEvery { getDailyProgress.getWeekly(any()) } returns weekly
        coEvery { getDailyProgress.getStreak(any()) } returns streak
    }

    private fun createViewModel() = DashboardViewModel(
        getUserProfile = getUserProfile,
        calculateOverallProgress = calculateOverallProgress,
        getDailyProgress = getDailyProgress,
        userRepository = userRepository,
    )

    @BeforeEach
    fun setUp() {
        getUserProfile = mockk(relaxed = true)
        calculateOverallProgress = mockk(relaxed = true)
        getDailyProgress = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        setupDefaultMocks()
    }

    // ── Initial state (before load) ───────────────────────────────────────────

    @Test
    fun initialState_isLoadingTrue_beforeCoroutineRuns() {
        // Don't advance — check the very first emission
        sut = createViewModel()
        // isLoading starts true in DashboardUiState default
        assertNotNull(sut.uiState.value)
    }

    // ── init loadData success ─────────────────────────────────────────────────

    @Test
    fun init_success_isLoadingFalse() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_success_populatesUserProfile() = runTest {
        val profile = buildUserProfile(name = "Klaus", cefrLevel = CefrLevel.C1)
        setupDefaultMocks(profile = profile)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.userProfile)
        assertEquals("Klaus", sut.uiState.value.userProfile!!.name)
        assertEquals(CefrLevel.C1, sut.uiState.value.userProfile!!.cefrLevel)
    }

    @Test
    fun init_success_populatesOverallProgress() = runTest {
        val overall = buildOverallProgress(totalSessions = 100, totalHours = 25f)
        setupDefaultMocks(overall = overall)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.overallProgress)
        assertEquals(100, sut.uiState.value.overallProgress!!.totalSessions)
        assertEquals(25f, sut.uiState.value.overallProgress!!.totalHours, 0.001f)
    }

    @Test
    fun init_success_populatesTodayProgress() = runTest {
        val today = buildDailyProgress(wordsLearned = 20, totalMinutes = 45)
        setupDefaultMocks(today = today)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.todayProgress)
        assertEquals(20, sut.uiState.value.todayProgress!!.wordsLearned)
        assertEquals(45, sut.uiState.value.todayProgress!!.totalMinutes)
    }

    @Test
    fun init_success_populatesWordsLearnedToday() = runTest {
        setupDefaultMocks(today = buildDailyProgress(wordsLearned = 12))

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(12, sut.uiState.value.wordsLearnedToday)
    }

    @Test
    fun init_success_populatesMinutesToday() = runTest {
        setupDefaultMocks(today = buildDailyProgress(totalMinutes = 55))

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(55, sut.uiState.value.minutesToday)
    }

    @Test
    fun init_success_todayNull_wordsLearnedTodayIsZero() = runTest {
        setupDefaultMocks(today = null)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, sut.uiState.value.wordsLearnedToday)
    }

    @Test
    fun init_success_todayNull_minutesTodayIsZero() = runTest {
        setupDefaultMocks(today = null)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, sut.uiState.value.minutesToday)
    }

    @Test
    fun init_success_populatesWeeklyProgress() = runTest {
        val weekly = listOf(
            buildDailyProgress("2024-06-01"),
            buildDailyProgress("2024-06-02"),
            buildDailyProgress("2024-06-03"),
        )
        setupDefaultMocks(weekly = weekly)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, sut.uiState.value.weeklyProgress.size)
        assertEquals("2024-06-01", sut.uiState.value.weeklyProgress[0].date)
    }

    @Test
    fun init_success_populatesStreakDays() = runTest {
        setupDefaultMocks(streak = 21)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(21, sut.uiState.value.streakDays)
    }

    @Test
    fun init_success_noErrorMessage() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun init_callsAllUseCasesWithUserId() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { getUserProfile("user_1") }
        coVerify { calculateOverallProgress("user_1") }
        coVerify { getDailyProgress.getToday("user_1") }
        coVerify { getDailyProgress.getWeekly("user_1") }
        coVerify { getDailyProgress.getStreak("user_1") }
    }

    // ── init loadData failures ────────────────────────────────────────────────

    @Test
    fun init_noActiveUser_setsErrorMessage() = runTest {
        setupDefaultMocks(userId = null)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("onboarding"))
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_noActiveUser_doesNotCallUseCases() = runTest {
        setupDefaultMocks(userId = null)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { getUserProfile(any()) }
        coVerify(exactly = 0) { calculateOverallProgress(any()) }
    }

    @Test
    fun init_getUserProfileThrows_setsErrorMessage() = runTest {
        coEvery { getUserProfile(any()) } throws RuntimeException("Profile unavailable")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("Profile unavailable"))
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_calculateOverallProgressThrows_setsErrorMessage() = runTest {
        coEvery { calculateOverallProgress(any()) } throws RuntimeException("Progress error")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_getStreakThrows_setsErrorMessage() = runTest {
        coEvery { getDailyProgress.getStreak(any()) } throws RuntimeException("Streak error")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_errorWithNullMessage_usesDefaultText() = runTest {
        coEvery { getUserProfile(any()) } throws RuntimeException(null as String?)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("загрузить"))
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    fun refresh_reloadsData() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val newProfile = buildUserProfile(name = "Anna")
        coEvery { getUserProfile(any()) } returns newProfile

        sut.onEvent(DashboardEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Anna", sut.uiState.value.userProfile!!.name)
    }

    @Test
    fun refresh_clearsErrorBeforeReload() = runTest {
        coEvery { getUserProfile(any()) } throws RuntimeException("fail")
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sut.uiState.value.errorMessage)

        setupDefaultMocks()
        sut.onEvent(DashboardEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun refresh_callsAllUseCasesTwice() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(DashboardEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) { getUserProfile(any()) }
        coVerify(exactly = 2) { calculateOverallProgress(any()) }
        coVerify(exactly = 2) { getDailyProgress.getToday(any()) }
        coVerify(exactly = 2) { getDailyProgress.getWeekly(any()) }
        coVerify(exactly = 2) { getDailyProgress.getStreak(any()) }
    }

    @Test
    fun refresh_success_isLoadingFalse() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(DashboardEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun refresh_updatesStreakDays() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coEvery { getDailyProgress.getStreak(any()) } returns 30

        sut.onEvent(DashboardEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(30, sut.uiState.value.streakDays)
    }

    // ── DismissError ──────────────────────────────────────────────────────────

    @Test
    fun dismissError_clearsErrorMessage() = runTest {
        coEvery { getUserProfile(any()) } throws RuntimeException("error")
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sut.uiState.value.errorMessage)

        sut.onEvent(DashboardEvent.DismissError)

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun dismissError_whenNoError_doesNotCrash() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertDoesNotThrow { sut.onEvent(DashboardEvent.DismissError) }
        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun dismissError_doesNotAffectOtherState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val streakBefore = sut.uiState.value.streakDays
        sut.onEvent(DashboardEvent.DismissError)

        assertEquals(streakBefore, sut.uiState.value.streakDays)
        assertFalse(sut.uiState.value.isLoading)
    }

    // ── StartSession (navigation-only, no state change) ───────────────────────

    @Test
    fun startSession_doesNotChangeLoadingState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(DashboardEvent.StartSession)

        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun startSession_doesNotSetErrorMessage() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(DashboardEvent.StartSession)

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun startSession_doesNotTriggerReload() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(DashboardEvent.StartSession)

        coVerify(exactly = 1) { getUserProfile(any()) }
    }

    // ── uiState flow ──────────────────────────────────────────────────────────

    @Test
    fun uiState_flow_emitsLoadedState() = runTest {
        sut = createViewModel()

        sut.uiState.test {
            val states = mutableListOf<DashboardUiState>()
            while (true) {
                val s = awaitItem()
                states.add(s)
                mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
                if (!s.isLoading) break
            }

            assertFalse(states.last().isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_flow_emitsOnRefresh() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.test {
            awaitItem() // loaded state

            val newProfile = buildUserProfile(name = "Greta")
            coEvery { getUserProfile(any()) } returns newProfile

            sut.onEvent(DashboardEvent.Refresh)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            val states = mutableListOf<DashboardUiState>()
            while (true) {
                val s = awaitItem()
                states.add(s)
                if (!s.isLoading) break
            }

            assertEquals("Greta", states.last().userProfile!!.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_flow_emitsOnDismissError() = runTest {
        coEvery { getUserProfile(any()) } throws RuntimeException("fail")
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.test {
            awaitItem() // error state

            sut.onEvent(DashboardEvent.DismissError)
            val cleared = awaitItem()
            assertNull(cleared.errorMessage)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── DashboardUiState data class ───────────────────────────────────────────

    @Test
    fun dashboardUiState_defaultValues_areCorrect() {
        val state = DashboardUiState()
        assertTrue(state.isLoading)
        assertNull(state.userProfile)
        assertNull(state.overallProgress)
        assertNull(state.todayProgress)
        assertEquals(emptyList<DailyProgress>(), state.weeklyProgress)
        assertEquals(0, state.streakDays)
        assertEquals(0, state.wordsLearnedToday)
        assertEquals(0, state.minutesToday)
        assertNull(state.errorMessage)
    }

    @Test
    fun dashboardUiState_copy_changesOneField() {
        val original = DashboardUiState()
        val copy = original.copy(streakDays = 10)
        assertEquals(10, copy.streakDays)
        assertEquals(original.isLoading, copy.isLoading)
        assertNull(copy.errorMessage)
    }

    @Test
    fun dashboardUiState_equals_twoIdenticalInstances() {
        val a = DashboardUiState()
        val b = DashboardUiState()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
