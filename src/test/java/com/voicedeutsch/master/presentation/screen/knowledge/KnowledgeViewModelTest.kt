// Путь: src/test/java/com/voicedeutsch/master/presentation/screen/knowledge/KnowledgeViewModelTest.kt
package com.voicedeutsch.master.presentation.screen.knowledge

import app.cash.turbine.test
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.domain.usecase.knowledge.GetUserKnowledgeUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.GetWeakPointsUseCase
import com.voicedeutsch.master.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class KnowledgeViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getUserKnowledge: GetUserKnowledgeUseCase
    private lateinit var getWeakPoints: GetWeakPointsUseCase
    private lateinit var userRepository: UserRepository
    private lateinit var sut: KnowledgeViewModel

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildOverview(
        totalWords: Int = 100,
        masteredWords: Int = 60,
        totalRules: Int = 30,
        masteredRules: Int = 15,
        totalPhrases: Int = 20,
        masteredPhrases: Int = 10,
    ) = GetUserKnowledgeUseCase.UserKnowledgeOverview(
        totalWords = totalWords,
        masteredWords = masteredWords,
        totalRules = totalRules,
        masteredRules = masteredRules,
        totalPhrases = totalPhrases,
        masteredPhrases = masteredPhrases,
    )

    private fun buildWeakPoint(
        itemId: String = "word_1",
        itemType: String = "WORD",
        retentionScore: Float = 0.3f,
        label: String = "der Hund",
    ) = GetWeakPointsUseCase.WeakPoint(
        itemId = itemId,
        itemType = itemType,
        retentionScore = retentionScore,
        label = label,
    )

    private fun setupDefaultMocks(
        userId: String? = "user_1",
        overview: GetUserKnowledgeUseCase.UserKnowledgeOverview = buildOverview(),
        weakPoints: List<GetWeakPointsUseCase.WeakPoint> = emptyList(),
    ) {
        coEvery { userRepository.getActiveUserId() } returns userId
        coEvery { getUserKnowledge(any()) } returns overview
        coEvery { getWeakPoints(any()) } returns weakPoints
    }

    private fun createViewModel() = KnowledgeViewModel(
        getUserKnowledge = getUserKnowledge,
        getWeakPoints = getWeakPoints,
        userRepository = userRepository,
    )

    @BeforeEach
    fun setUp() {
        getUserKnowledge = mockk(relaxed = true)
        getWeakPoints = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        setupDefaultMocks()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun init_success_isLoadingFalse() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_success_populatesOverview() = runTest {
        val overview = buildOverview(totalWords = 200, masteredWords = 150)
        setupDefaultMocks(overview = overview)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.overview)
        assertEquals(200, sut.uiState.value.overview!!.totalWords)
        assertEquals(150, sut.uiState.value.overview!!.masteredWords)
    }

    @Test
    fun init_success_populatesWeakPoints() = runTest {
        val weakPoints = listOf(
            buildWeakPoint(itemId = "word_1", label = "der Hund"),
            buildWeakPoint(itemId = "word_2", label = "die Katze"),
        )
        setupDefaultMocks(weakPoints = weakPoints)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.weakPoints)
        assertEquals(2, sut.uiState.value.weakPoints!!.size)
        assertEquals("word_1", sut.uiState.value.weakPoints!![0].itemId)
        assertEquals("die Katze", sut.uiState.value.weakPoints!![1].label)
    }

    @Test
    fun init_success_noErrorMessage() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun init_success_defaultTabIsOverview() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(KnowledgeTab.OVERVIEW, sut.uiState.value.selectedTab)
    }

    @Test
    fun init_success_showAllWordsFalse() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.showAllWords)
    }

    @Test
    fun init_callsUseCasesWithActiveUserId() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { getUserKnowledge("user_1") }
        coVerify { getWeakPoints("user_1") }
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
    fun init_getUserKnowledgeThrows_setsErrorMessage() = runTest {
        coEvery { getUserKnowledge(any()) } throws RuntimeException("Knowledge error")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("Knowledge error"))
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_getWeakPointsThrows_setsErrorMessage() = runTest {
        coEvery { getWeakPoints(any()) } throws RuntimeException("Weak points error")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_noActiveUser_doesNotCallUseCases() = runTest {
        setupDefaultMocks(userId = null)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { getUserKnowledge(any()) }
        coVerify(exactly = 0) { getWeakPoints(any()) }
    }

    @Test
    fun init_emptyWeakPoints_weakPointsIsEmptyList() = runTest {
        setupDefaultMocks(weakPoints = emptyList())

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.weakPoints)
        assertEquals(emptyList<GetWeakPointsUseCase.WeakPoint>(), sut.uiState.value.weakPoints)
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    fun refresh_reloadsData() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val newOverview = buildOverview(totalWords = 999)
        coEvery { getUserKnowledge(any()) } returns newOverview

        sut.onEvent(KnowledgeEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(999, sut.uiState.value.overview!!.totalWords)
    }

    @Test
    fun refresh_clearsErrorBeforeReload() = runTest {
        coEvery { getUserKnowledge(any()) } throws RuntimeException("fail")
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sut.uiState.value.errorMessage)

        setupDefaultMocks()
        sut.onEvent(KnowledgeEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun refresh_callsUseCasesTwice() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(KnowledgeEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) { getUserKnowledge(any()) }
        coVerify(exactly = 2) { getWeakPoints(any()) }
    }

    @Test
    fun refresh_success_isLoadingFalse() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(KnowledgeEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun refresh_updatesWeakPoints() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val newWeakPoints = listOf(buildWeakPoint(itemId = "new_word"))
        coEvery { getWeakPoints(any()) } returns newWeakPoints

        sut.onEvent(KnowledgeEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, sut.uiState.value.weakPoints!!.size)
        assertEquals("new_word", sut.uiState.value.weakPoints!![0].itemId)
    }

    // ── SelectTab ─────────────────────────────────────────────────────────────

    @Test
    fun selectTab_words_updatesSelectedTab() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(KnowledgeEvent.SelectTab(KnowledgeTab.WORDS))

        assertEquals(KnowledgeTab.WORDS, sut.uiState.value.selectedTab)
    }

    @Test
    fun selectTab_grammar_updatesSelectedTab() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(KnowledgeEvent.SelectTab(KnowledgeTab.GRAMMAR))

        assertEquals(KnowledgeTab.GRAMMAR, sut.uiState.value.selectedTab)
    }

    @Test
    fun selectTab_weakPoints_updatesSelectedTab() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(KnowledgeEvent.SelectTab(KnowledgeTab.WEAK_POINTS))

        assertEquals(KnowledgeTab.WEAK_POINTS, sut.uiState.value.selectedTab)
    }

    @Test
    fun selectTab_overview_updatesSelectedTab() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(KnowledgeEvent.SelectTab(KnowledgeTab.WORDS))
        sut.onEvent(KnowledgeEvent.SelectTab(KnowledgeTab.OVERVIEW))

        assertEquals(KnowledgeTab.OVERVIEW, sut.uiState.value.selectedTab)
    }

    @Test
    fun selectTab_doesNotTriggerReload() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(KnowledgeEvent.SelectTab(KnowledgeTab.GRAMMAR))

        coVerify(exactly = 1) { getUserKnowledge(any()) }
    }

    @Test
    fun selectTab_multipleTimes_keepsLatest() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(KnowledgeEvent.SelectTab(KnowledgeTab.WORDS))
        sut.onEvent(KnowledgeEvent.SelectTab(KnowledgeTab.GRAMMAR))
        sut.onEvent(KnowledgeEvent.SelectTab(KnowledgeTab.WEAK_POINTS))

        assertEquals(KnowledgeTab.WEAK_POINTS, sut.uiState.value.selectedTab)
    }

    // ── DismissError ──────────────────────────────────────────────────────────

    @Test
    fun dismissError_clearsErrorMessage() = runTest {
        coEvery { getUserKnowledge(any()) } throws RuntimeException("error")
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sut.uiState.value.errorMessage)

        sut.onEvent(KnowledgeEvent.DismissError)

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun dismissError_whenNoError_doesNotCrash() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertDoesNotThrow { sut.onEvent(KnowledgeEvent.DismissError) }
        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun dismissError_doesNotAffectOtherState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(KnowledgeEvent.SelectTab(KnowledgeTab.GRAMMAR))
        val tabBefore = sut.uiState.value.selectedTab

        sut.onEvent(KnowledgeEvent.DismissError)

        assertEquals(tabBefore, sut.uiState.value.selectedTab)
        assertFalse(sut.uiState.value.isLoading)
    }

    // ── ToggleShowAllWords ────────────────────────────────────────────────────

    @Test
    fun toggleShowAllWords_fromFalse_becomesTrue() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(sut.uiState.value.showAllWords)

        sut.onEvent(KnowledgeEvent.ToggleShowAllWords)

        assertTrue(sut.uiState.value.showAllWords)
    }

    @Test
    fun toggleShowAllWords_fromTrue_becomesFalse() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(KnowledgeEvent.ToggleShowAllWords)
        assertTrue(sut.uiState.value.showAllWords)

        sut.onEvent(KnowledgeEvent.ToggleShowAllWords)

        assertFalse(sut.uiState.value.showAllWords)
    }

    @Test
    fun toggleShowAllWords_threeToggles_endsFalse() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(KnowledgeEvent.ToggleShowAllWords)
        sut.onEvent(KnowledgeEvent.ToggleShowAllWords)
        sut.onEvent(KnowledgeEvent.ToggleShowAllWords)

        assertTrue(sut.uiState.value.showAllWords)
    }

    @Test
    fun toggleShowAllWords_doesNotTriggerReload() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(KnowledgeEvent.ToggleShowAllWords)

        coVerify(exactly = 1) { getUserKnowledge(any()) }
    }

    // ── uiState flow ──────────────────────────────────────────────────────────

    @Test
    fun uiState_flow_emitsOnTabChange() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.test {
            awaitItem() // current loaded state

            sut.onEvent(KnowledgeEvent.SelectTab(KnowledgeTab.WORDS))
            val updated = awaitItem()
            assertEquals(KnowledgeTab.WORDS, updated.selectedTab)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_flow_emitsOnToggleShowAllWords() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.test {
            awaitItem()

            sut.onEvent(KnowledgeEvent.ToggleShowAllWords)
            assertTrue(awaitItem().showAllWords)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_flow_emitsOnRefresh() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.test {
            awaitItem()

            sut.onEvent(KnowledgeEvent.Refresh)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            val states = mutableListOf<KnowledgeUiState>()
            while (true) {
                val s = awaitItem()
                states.add(s)
                if (!s.isLoading) break
            }

            assertFalse(states.last().isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── KnowledgeUiState data class ───────────────────────────────────────────

    @Test
    fun knowledgeUiState_defaultValues_areCorrect() {
        val state = KnowledgeUiState()
        assertTrue(state.isLoading)
        assertNull(state.overview)
        assertNull(state.weakPoints)
        assertNull(state.errorMessage)
        assertEquals(KnowledgeTab.OVERVIEW, state.selectedTab)
        assertFalse(state.showAllWords)
    }

    @Test
    fun knowledgeUiState_copy_changesOneField() {
        val original = KnowledgeUiState()
        val copy = original.copy(showAllWords = true)
        assertTrue(copy.showAllWords)
        assertEquals(original.selectedTab, copy.selectedTab)
        assertEquals(original.isLoading, copy.isLoading)
    }

    @Test
    fun knowledgeUiState_equals_twoIdenticalInstances() {
        val a = KnowledgeUiState()
        val b = KnowledgeUiState()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ── KnowledgeTab enum ─────────────────────────────────────────────────────

    @Test
    fun knowledgeTab_allValuesPresent() {
        val tabs = KnowledgeTab.entries
        assertTrue(tabs.contains(KnowledgeTab.OVERVIEW))
        assertTrue(tabs.contains(KnowledgeTab.WORDS))
        assertTrue(tabs.contains(KnowledgeTab.GRAMMAR))
        assertTrue(tabs.contains(KnowledgeTab.WEAK_POINTS))
        assertEquals(4, tabs.size)
    }

    // ── WeakPoint data class ──────────────────────────────────────────────────

    @Test
    fun weakPoint_creation_allFieldsStoredCorrectly() {
        val wp = buildWeakPoint(
            itemId = "rule_5",
            itemType = "RULE",
            retentionScore = 0.15f,
            label = "Dativ",
        )
        assertEquals("rule_5", wp.itemId)
        assertEquals("RULE", wp.itemType)
        assertEquals(0.15f, wp.retentionScore, 0.001f)
        assertEquals("Dativ", wp.label)
    }

    @Test
    fun weakPoint_copy_changesRetentionScore() {
        val original = buildWeakPoint(retentionScore = 0.2f)
        val copy = original.copy(retentionScore = 0.9f)
        assertEquals(0.9f, copy.retentionScore, 0.001f)
        assertEquals(original.itemId, copy.itemId)
    }

    @Test
    fun weakPoint_equals_twoIdenticalInstances() {
        val a = buildWeakPoint()
        val b = buildWeakPoint()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
