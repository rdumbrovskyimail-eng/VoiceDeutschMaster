// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/progress/CalculateOverallProgressUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.progress

import app.cash.turbine.test
import com.voicedeutsch.master.domain.model.progress.OverallProgress
import com.voicedeutsch.master.domain.repository.ProgressRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CalculateOverallProgressUseCaseTest {

    private lateinit var progressRepository: ProgressRepository
    private lateinit var userRepository: UserRepository
    private lateinit var useCase: CalculateOverallProgressUseCase

    private val progress = mockk<OverallProgress>(relaxed = true)

    @BeforeEach
    fun setUp() {
        progressRepository = mockk()
        userRepository     = mockk()
        useCase = CalculateOverallProgressUseCase(progressRepository, userRepository)

        coEvery { progressRepository.calculateOverallProgress(any()) } returns progress
        every  { progressRepository.getOverallProgressFlow(any()) }    returns flowOf(progress)
    }

    // ── invoke (one-shot) ─────────────────────────────────────────────────────

    @Test
    fun invoke_returnsProgressFromRepository() = runTest {
        val result = useCase("user1")

        assertEquals(progress, result)
    }

    @Test
    fun invoke_repositoryCalledWithCorrectUserId() = runTest {
        useCase("user42")

        coVerify(exactly = 1) { progressRepository.calculateOverallProgress("user42") }
    }

    @Test
    fun invoke_calledMultipleTimes_repositoryCalledEachTime() = runTest {
        useCase("user1")
        useCase("user1")
        useCase("user1")

        coVerify(exactly = 3) { progressRepository.calculateOverallProgress("user1") }
    }

    @Test
    fun invoke_differentUserIds_correctProgressReturnedForEach() = runTest {
        val progress1 = mockk<OverallProgress>(relaxed = true)
        val progress2 = mockk<OverallProgress>(relaxed = true)
        coEvery { progressRepository.calculateOverallProgress("u1") } returns progress1
        coEvery { progressRepository.calculateOverallProgress("u2") } returns progress2

        assertEquals(progress1, useCase("u1"))
        assertEquals(progress2, useCase("u2"))
    }

    @Test
    fun invoke_repositoryThrows_exceptionPropagates() = runTest {
        coEvery { progressRepository.calculateOverallProgress(any()) } throws
            RuntimeException("db error")

        val ex = org.junit.jupiter.api.assertThrows<RuntimeException> { useCase("user1") }
        assertEquals("db error", ex.message)
    }

    @Test
    fun invoke_userRepositoryNotCalled() = runTest {
        useCase("user1")

        coVerify(exactly = 0) { userRepository.getUserProfile(any()) }
    }

    // ── observeProgress (Flow) ────────────────────────────────────────────────

    @Test
    fun observeProgress_emitsProgressFromRepository() = runTest {
        useCase.observeProgress("user1").test {
            assertEquals(progress, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun observeProgress_repositoryFlowEmitsMultipleItems_allForwarded() = runTest {
        val progress2 = mockk<OverallProgress>(relaxed = true)
        every { progressRepository.getOverallProgressFlow("user1") } returns
            flowOf(progress, progress2)

        useCase.observeProgress("user1").test {
            assertEquals(progress,  awaitItem())
            assertEquals(progress2, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun observeProgress_repositoryCalledWithCorrectUserId() = runTest {
        useCase.observeProgress("user99").test { cancelAndIgnoreRemainingEvents() }

        verify(exactly = 1) { progressRepository.getOverallProgressFlow("user99") }
    }

    @Test
    fun observeProgress_emptyFlow_completesWithNoItems() = runTest {
        every { progressRepository.getOverallProgressFlow("user1") } returns flowOf()

        useCase.observeProgress("user1").test {
            awaitComplete()
        }
    }

    @Test
    fun observeProgress_userRepositoryNotCalled() = runTest {
        useCase.observeProgress("user1").test { cancelAndIgnoreRemainingEvents() }

        coVerify(exactly = 0) { userRepository.getUserProfile(any()) }
    }
}
