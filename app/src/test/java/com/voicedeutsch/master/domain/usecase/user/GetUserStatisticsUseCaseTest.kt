// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/user/GetUserStatisticsUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.user

import com.voicedeutsch.master.domain.model.user.UserStatistics
import com.voicedeutsch.master.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetUserStatisticsUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var useCase: GetUserStatisticsUseCase

    private val statistics = mockk<UserStatistics>(relaxed = true)

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        useCase = GetUserStatisticsUseCase(userRepository)

        coEvery { userRepository.getUserStatistics(any()) } returns statistics
    }

    // ── invoke — delegates to repository ─────────────────────────────────────

    @Test
    fun invoke_returnsStatisticsFromRepository() = runTest {
        val result = useCase("user1")

        assertEquals(statistics, result)
    }

    @Test
    fun invoke_repositoryCalledWithCorrectUserId() = runTest {
        useCase("user42")

        coVerify(exactly = 1) { userRepository.getUserStatistics("user42") }
    }

    @Test
    fun invoke_calledMultipleTimes_repositoryCalledEachTime() = runTest {
        useCase("user1")
        useCase("user1")
        useCase("user1")

        coVerify(exactly = 3) { userRepository.getUserStatistics("user1") }
    }

    @Test
    fun invoke_differentUserIds_eachPassedToRepository() = runTest {
        val stats1 = mockk<UserStatistics>(relaxed = true)
        val stats2 = mockk<UserStatistics>(relaxed = true)
        coEvery { userRepository.getUserStatistics("u1") } returns stats1
        coEvery { userRepository.getUserStatistics("u2") } returns stats2

        val result1 = useCase("u1")
        val result2 = useCase("u2")

        assertEquals(stats1, result1)
        assertEquals(stats2, result2)
    }

    @Test
    fun invoke_repositoryThrows_exceptionPropagates() = runTest {
        coEvery { userRepository.getUserStatistics(any()) } throws RuntimeException("db error")

        val ex = org.junit.jupiter.api.assertThrows<RuntimeException> { useCase("user1") }
        assertEquals("db error", ex.message)
    }
}
