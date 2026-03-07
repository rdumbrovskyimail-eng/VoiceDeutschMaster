// Путь: src/test/java/com/voicedeutsch/master/presentation/screen/onboarding/OnboardingViewModelTest.kt
package com.voicedeutsch.master.presentation.screen.onboarding

import app.cash.turbine.test
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.domain.model.user.CefrLevel
import com.voicedeutsch.master.domain.model.user.UserProfile
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.domain.usecase.user.GetUserProfileUseCase
import com.voicedeutsch.master.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class OnboardingViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var userRepository: UserRepository
    private lateinit var preferencesDataStore: UserPreferencesDataStore
    private lateinit var getUserProfile: GetUserProfileUseCase
    private lateinit var sut: OnboardingViewModel

    @BeforeEach
    fun setUp() {
        userRepository = mockk(relaxed = true)
        preferencesDataStore = mockk(relaxed = true)
        getUserProfile = mockk(relaxed = true)

        coEvery { userRepository.createUser(any()) } just Runs
        coEvery { preferencesDataStore.setOnboardingComplete(any()) } returns Unit

        sut = OnboardingViewModel(userRepository, preferencesDataStore, getUserProfile)
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialState_stepIsWelcome() {
        assertEquals(OnboardingStep.WELCOME, sut.uiState.value.step)
    }

    @Test
    fun initialState_nameIsEmpty() {
        assertEquals("", sut.uiState.value.name)
    }

    @Test
    fun initialState_selectedLevelIsA1() {
        assertEquals(CefrLevel.A1, sut.uiState.value.selectedLevel)
    }

    @Test
    fun initialState_isLoadingFalse() {
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun initialState_errorMessageIsNull() {
        assertNull(sut.uiState.value.errorMessage)
    }

    // ── UpdateName ────────────────────────────────────────────────────────────

    @Test
    fun updateName_updatesNameInState() {
        sut.onEvent(OnboardingEvent.UpdateName("Klaus"))
        assertEquals("Klaus", sut.uiState.value.name)
    }

    @Test
    fun updateName_emptyString_updatesStateToEmpty() {
        sut.onEvent(OnboardingEvent.UpdateName("Max"))
        sut.onEvent(OnboardingEvent.UpdateName(""))
        assertEquals("", sut.uiState.value.name)
    }

    @Test
    fun updateName_multipleUpdates_keepsLatest() {
        sut.onEvent(OnboardingEvent.UpdateName("A"))
        sut.onEvent(OnboardingEvent.UpdateName("AB"))
        sut.onEvent(OnboardingEvent.UpdateName("ABC"))
        assertEquals("ABC", sut.uiState.value.name)
    }

    // ── SelectLevel ───────────────────────────────────────────────────────────

    @Test
    fun selectLevel_updatesSelectedLevelInState() {
        sut.onEvent(OnboardingEvent.SelectLevel(CefrLevel.B2))
        assertEquals(CefrLevel.B2, sut.uiState.value.selectedLevel)
    }

    @Test
    fun selectLevel_allLevels_updatedCorrectly() {
        CefrLevel.entries.forEach { level ->
            sut.onEvent(OnboardingEvent.SelectLevel(level))
            assertEquals(level, sut.uiState.value.selectedLevel)
        }
    }

    // ── DismissError ──────────────────────────────────────────────────────────

    @Test
    fun dismissError_clearsErrorMessage() {
        // Trigger an error by going Next on NAME with blank name
        sut.onEvent(OnboardingEvent.Next) // WELCOME → NAME
        sut.onEvent(OnboardingEvent.Next) // NAME with blank → error
        assertNotNull(sut.uiState.value.errorMessage)

        sut.onEvent(OnboardingEvent.DismissError)

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun dismissError_whenNoError_doesNotCrash() {
        assertDoesNotThrow { sut.onEvent(OnboardingEvent.DismissError) }
        assertNull(sut.uiState.value.errorMessage)
    }

    // ── Next: WELCOME → NAME ──────────────────────────────────────────────────

    @Test
    fun next_fromWelcome_goesToName() {
        sut.onEvent(OnboardingEvent.Next)
        assertEquals(OnboardingStep.NAME, sut.uiState.value.step)
    }

    @Test
    fun next_fromWelcome_clearsErrorMessage() {
        // Put a stale error in state first
        sut.onEvent(OnboardingEvent.Next) // → NAME
        sut.onEvent(OnboardingEvent.Next) // blank name → error
        sut.onEvent(OnboardingEvent.Back) // → WELCOME (keeps error cleared on arrive)
        // Manually: go to NAME step with blank name, then back
        // Instead trigger via DismissError then re-test:
        sut.onEvent(OnboardingEvent.DismissError)
        sut.onEvent(OnboardingEvent.Next) // WELCOME → NAME
        assertNull(sut.uiState.value.errorMessage)
    }

    // ── Next: NAME → LEVEL ────────────────────────────────────────────────────

    @Test
    fun next_fromName_blankName_setsErrorMessage() {
        sut.onEvent(OnboardingEvent.Next) // → NAME
        sut.onEvent(OnboardingEvent.Next) // blank name

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("имя"))
        assertEquals(OnboardingStep.NAME, sut.uiState.value.step)
    }

    @Test
    fun next_fromName_whitespaceOnly_setsErrorAndStaysOnName() {
        sut.onEvent(OnboardingEvent.Next) // → NAME
        sut.onEvent(OnboardingEvent.UpdateName("   "))
        sut.onEvent(OnboardingEvent.Next)

        assertNotNull(sut.uiState.value.errorMessage)
        assertEquals(OnboardingStep.NAME, sut.uiState.value.step)
    }

    @Test
    fun next_fromName_validName_goesToLevel() {
        sut.onEvent(OnboardingEvent.Next) // → NAME
        sut.onEvent(OnboardingEvent.UpdateName("Max"))
        sut.onEvent(OnboardingEvent.Next) // → LEVEL

        assertEquals(OnboardingStep.LEVEL, sut.uiState.value.step)
    }

    @Test
    fun next_fromName_validName_clearsError() {
        sut.onEvent(OnboardingEvent.Next) // → NAME
        sut.onEvent(OnboardingEvent.Next) // blank → error
        sut.onEvent(OnboardingEvent.UpdateName("Max"))
        sut.onEvent(OnboardingEvent.Next) // → LEVEL

        assertNull(sut.uiState.value.errorMessage)
    }

    // ── Next: LEVEL → MICROPHONE ──────────────────────────────────────────────

    @Test
    fun next_fromLevel_goesToMicrophone() {
        sut.onEvent(OnboardingEvent.Next) // → NAME
        sut.onEvent(OnboardingEvent.UpdateName("Max"))
        sut.onEvent(OnboardingEvent.Next) // → LEVEL
        sut.onEvent(OnboardingEvent.Next) // → MICROPHONE

        assertEquals(OnboardingStep.MICROPHONE, sut.uiState.value.step)
    }

    // ── Next: MICROPHONE → triggers completeOnboarding ───────────────────────

    @Test
    fun next_fromMicrophone_triggersCompleteOnboarding() = runTest {
        sut.onEvent(OnboardingEvent.Next) // → NAME
        sut.onEvent(OnboardingEvent.UpdateName("Max"))
        sut.onEvent(OnboardingEvent.Next) // → LEVEL
        sut.onEvent(OnboardingEvent.Next) // → MICROPHONE
        sut.onEvent(OnboardingEvent.Next) // → triggers complete
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(OnboardingStep.DONE, sut.uiState.value.step)
        coVerify { userRepository.createUser(any()) }
    }

    // ── Next: DONE ────────────────────────────────────────────────────────────

    @Test
    fun next_fromDone_doesNothing() = runTest {
        // Navigate to DONE
        sut.onEvent(OnboardingEvent.Next)
        sut.onEvent(OnboardingEvent.UpdateName("Max"))
        sut.onEvent(OnboardingEvent.Next)
        sut.onEvent(OnboardingEvent.Next)
        sut.onEvent(OnboardingEvent.Next)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(OnboardingStep.DONE, sut.uiState.value.step)

        sut.onEvent(OnboardingEvent.Next)
        assertEquals(OnboardingStep.DONE, sut.uiState.value.step)
    }

    // ── Back navigation ───────────────────────────────────────────────────────

    @Test
    fun back_fromWelcome_doesNothing() {
        sut.onEvent(OnboardingEvent.Back)
        assertEquals(OnboardingStep.WELCOME, sut.uiState.value.step)
    }

    @Test
    fun back_fromName_goesToWelcome() {
        sut.onEvent(OnboardingEvent.Next) // → NAME
        sut.onEvent(OnboardingEvent.Back)
        assertEquals(OnboardingStep.WELCOME, sut.uiState.value.step)
    }

    @Test
    fun back_fromLevel_goesToName() {
        sut.onEvent(OnboardingEvent.Next) // → NAME
        sut.onEvent(OnboardingEvent.UpdateName("Max"))
        sut.onEvent(OnboardingEvent.Next) // → LEVEL
        sut.onEvent(OnboardingEvent.Back)
        assertEquals(OnboardingStep.NAME, sut.uiState.value.step)
    }

    @Test
    fun back_fromMicrophone_goesToLevel() {
        sut.onEvent(OnboardingEvent.Next) // → NAME
        sut.onEvent(OnboardingEvent.UpdateName("Max"))
        sut.onEvent(OnboardingEvent.Next) // → LEVEL
        sut.onEvent(OnboardingEvent.Next) // → MICROPHONE
        sut.onEvent(OnboardingEvent.Back)
        assertEquals(OnboardingStep.LEVEL, sut.uiState.value.step)
    }

    @Test
    fun back_fromDone_goesToMicrophone() = runTest {
        sut.onEvent(OnboardingEvent.Next)
        sut.onEvent(OnboardingEvent.UpdateName("Max"))
        sut.onEvent(OnboardingEvent.Next)
        sut.onEvent(OnboardingEvent.Next)
        sut.onEvent(OnboardingEvent.Next)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(OnboardingEvent.Back)
        assertEquals(OnboardingStep.MICROPHONE, sut.uiState.value.step)
    }

    @Test
    fun back_clearsErrorMessage() {
        sut.onEvent(OnboardingEvent.Next) // → NAME
        sut.onEvent(OnboardingEvent.Next) // blank → error
        assertNotNull(sut.uiState.value.errorMessage)

        sut.onEvent(OnboardingEvent.Back)
        assertNull(sut.uiState.value.errorMessage)
    }

    // ── Complete: OnboardingEvent.Complete ────────────────────────────────────

    @Test
    fun complete_success_stepIsDone() = runTest {
        sut.onEvent(OnboardingEvent.UpdateName("Helga"))
        sut.onEvent(OnboardingEvent.SelectLevel(CefrLevel.B1))

        sut.onEvent(OnboardingEvent.Complete)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(OnboardingStep.DONE, sut.uiState.value.step)
    }

    @Test
    fun complete_success_isLoadingFalse() = runTest {
        sut.onEvent(OnboardingEvent.UpdateName("Helga"))
        sut.onEvent(OnboardingEvent.Complete)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun complete_success_noErrorMessage() = runTest {
        sut.onEvent(OnboardingEvent.UpdateName("Helga"))
        sut.onEvent(OnboardingEvent.Complete)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun complete_success_callsCreateUser() = runTest {
        sut.onEvent(OnboardingEvent.UpdateName("Helga"))
        sut.onEvent(OnboardingEvent.SelectLevel(CefrLevel.C1))
        sut.onEvent(OnboardingEvent.Complete)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userRepository.createUser(any()) }
    }

    @Test
    fun complete_success_profileHasTrimmedName() = runTest {
        sut.onEvent(OnboardingEvent.UpdateName("  Anna  "))
        sut.onEvent(OnboardingEvent.Complete)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userRepository.createUser(match { it.name == "Anna" }) }
    }

    @Test
    fun complete_success_profileHasCorrectCefrLevel() = runTest {
        sut.onEvent(OnboardingEvent.UpdateName("Anna"))
        sut.onEvent(OnboardingEvent.SelectLevel(CefrLevel.B2))
        sut.onEvent(OnboardingEvent.Complete)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userRepository.createUser(match { it.cefrLevel == CefrLevel.B2 }) }
    }

    @Test
    fun complete_success_setsOnboardingCompleteTrue() = runTest {
        sut.onEvent(OnboardingEvent.UpdateName("Anna"))
        sut.onEvent(OnboardingEvent.Complete)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { preferencesDataStore.setOnboardingComplete(true) }
    }

    @Test
    fun complete_success_profileHasNonBlankId() = runTest {
        sut.onEvent(OnboardingEvent.UpdateName("Anna"))
        sut.onEvent(OnboardingEvent.Complete)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userRepository.createUser(match { it.id.isNotBlank() }) }
    }

    @Test
    fun complete_createUserThrows_setsErrorMessage() = runTest {
        coEvery { userRepository.createUser(any()) } throws RuntimeException("DB unavailable")

        sut.onEvent(OnboardingEvent.UpdateName("Anna"))
        sut.onEvent(OnboardingEvent.Complete)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("Ошибка"))
        assertTrue(sut.uiState.value.errorMessage!!.contains("DB unavailable"))
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun complete_createUserThrows_doesNotNavigateToDone() = runTest {
        coEvery { userRepository.createUser(any()) } throws RuntimeException("error")

        sut.onEvent(OnboardingEvent.UpdateName("Anna"))
        sut.onEvent(OnboardingEvent.Complete)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotEquals(OnboardingStep.DONE, sut.uiState.value.step)
    }

    @Test
    fun complete_dataStoreThrows_setsErrorMessage() = runTest {
        coEvery { preferencesDataStore.setOnboardingComplete(any()) } throws RuntimeException("DataStore error")

        sut.onEvent(OnboardingEvent.UpdateName("Anna"))
        sut.onEvent(OnboardingEvent.Complete)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("DataStore error"))
    }

    // ── Full flow ─────────────────────────────────────────────────────────────

    @Test
    fun fullFlow_welcomeToCompletion_allStepsInOrder() = runTest {
        assertEquals(OnboardingStep.WELCOME, sut.uiState.value.step)

        sut.onEvent(OnboardingEvent.Next)
        assertEquals(OnboardingStep.NAME, sut.uiState.value.step)

        sut.onEvent(OnboardingEvent.UpdateName("Franz"))
        sut.onEvent(OnboardingEvent.Next)
        assertEquals(OnboardingStep.LEVEL, sut.uiState.value.step)

        sut.onEvent(OnboardingEvent.SelectLevel(CefrLevel.B1))
        sut.onEvent(OnboardingEvent.Next)
        assertEquals(OnboardingStep.MICROPHONE, sut.uiState.value.step)

        sut.onEvent(OnboardingEvent.Next)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(OnboardingStep.DONE, sut.uiState.value.step)

        coVerify {
            userRepository.createUser(match {
                it.name == "Franz" && it.cefrLevel == CefrLevel.B1
            })
        }
        coVerify { preferencesDataStore.setOnboardingComplete(true) }
    }

    @Test
    fun fullFlow_viaCompleteEvent_skipsSteps() = runTest {
        sut.onEvent(OnboardingEvent.UpdateName("Greta"))
        sut.onEvent(OnboardingEvent.SelectLevel(CefrLevel.C2))
        sut.onEvent(OnboardingEvent.Complete)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(OnboardingStep.DONE, sut.uiState.value.step)
        coVerify { userRepository.createUser(match { it.name == "Greta" && it.cefrLevel == CefrLevel.C2 }) }
    }

    // ── uiState flow ──────────────────────────────────────────────────────────

    @Test
    fun uiState_emitsOnStepChange() = runTest {
        sut.uiState.test {
            awaitItem() // WELCOME

            sut.onEvent(OnboardingEvent.Next)
            assertEquals(OnboardingStep.NAME, awaitItem().step)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_emitsOnNameUpdate() = runTest {
        sut.uiState.test {
            awaitItem() // initial

            sut.onEvent(OnboardingEvent.UpdateName("Max"))
            assertEquals("Max", awaitItem().name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_emitsOnError() = runTest {
        sut.uiState.test {
            awaitItem() // initial
            sut.onEvent(OnboardingEvent.Next) // → NAME
            awaitItem()

            sut.onEvent(OnboardingEvent.Next) // blank name → error
            val errorState = awaitItem()
            assertNotNull(errorState.errorMessage)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── OnboardingUiState data class ──────────────────────────────────────────

    @Test
    fun onboardingUiState_defaultValues_areCorrect() {
        val state = OnboardingUiState()
        assertEquals(OnboardingStep.WELCOME, state.step)
        assertEquals("", state.name)
        assertEquals(CefrLevel.A1, state.selectedLevel)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun onboardingUiState_copy_changesOneField() {
        val original = OnboardingUiState()
        val copy = original.copy(name = "Test")
        assertEquals("Test", copy.name)
        assertEquals(original.step, copy.step)
    }

    @Test
    fun onboardingUiState_equals_twoIdenticalInstances() {
        val a = OnboardingUiState()
        val b = OnboardingUiState()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ── OnboardingStep enum ───────────────────────────────────────────────────

    @Test
    fun onboardingStep_allValuesPresent() {
        val steps = OnboardingStep.entries
        assertTrue(steps.contains(OnboardingStep.WELCOME))
        assertTrue(steps.contains(OnboardingStep.NAME))
        assertTrue(steps.contains(OnboardingStep.LEVEL))
        assertTrue(steps.contains(OnboardingStep.MICROPHONE))
        assertTrue(steps.contains(OnboardingStep.DONE))
        assertEquals(5, steps.size)
    }
}
