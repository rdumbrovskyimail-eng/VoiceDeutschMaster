// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/user/GetUserProfileUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.user

import app.cash.turbine.test
import com.voicedeutsch.master.domain.model.user.UserProfile
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

class GetUserProfileUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var useCase: GetUserProfileUseCase

    private val profile = mockk<UserProfile>(relaxed = true)

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        useCase = GetUserProfileUseCase(userRepository)

        coEvery { userRepository.getUserProfile(any()) }     returns profile
        coEvery { userRepository.getActiveUserId() }         returns "user1"
        coEvery { userRepository.userExists() }              returns true
        every  { userRepository.getUserProfileFlow(any()) }  returns flowOf(profile)
    }

    // ── invoke (one-shot) ─────────────────────────────────────────────────────

    @Test
    fun invoke_profileExists_returnsProfile() = runTest {
        val result = useCase("user1")

        assertEquals(profile, result)
    }

    @Test
    fun invoke_profileNotFound_returnsNull() = runTest {
        coEvery { userRepository.getUserProfile("unknown") } returns null

        val result = useCase("unknown")

        assertNull(result)
    }

    @Test
    fun invoke_repositoryCalledWithCorrectUserId() = runTest {
        useCase("user42")

        coVerify(exactly = 1) { userRepository.getUserProfile("user42") }
    }

    @Test
    fun invoke_calledMultipleTimes_repositoryCalledEachTime() = runTest {
        useCase("user1")
        useCase("user1")

        coVerify(exactly = 2) { userRepository.getUserProfile("user1") }
    }

    @Test
    fun invoke_differentUserIds_correctProfileReturnedForEach() = runTest {
        val profile1 = mockk<UserProfile>(relaxed = true)
        val profile2 = mockk<UserProfile>(relaxed = true)
        coEvery { userRepository.getUserProfile("u1") } returns profile1
        coEvery { userRepository.getUserProfile("u2") } returns profile2

        assertEquals(profile1, useCase("u1"))
        assertEquals(profile2, useCase("u2"))
    }

    @Test
    fun invoke_repositoryThrows_exceptionPropagates() = runTest {
        coEvery { userRepository.getUserProfile(any()) } throws RuntimeException("db error")

        val ex = org.junit.jupiter.api.assertThrows<RuntimeException> { useCase("user1") }
        assertEquals("db error", ex.message)
    }

    // ── observeProfile (Flow) ─────────────────────────────────────────────────

    @Test
    fun observeProfile_emitsProfileFromRepository() = runTest {
        useCase.observeProfile("user1").test {
            assertEquals(profile, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun observeProfile_repositoryFlowEmitsNull_nullEmitted() = runTest {
        every { userRepository.getUserProfileFlow("user1") } returns flowOf(null)

        useCase.observeProfile("user1").test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun observeProfile_repositoryCalledWithCorrectUserId() = runTest {
        useCase.observeProfile("user99").test { cancelAndIgnoreRemainingEvents() }

        verify(exactly = 1) { userRepository.getUserProfileFlow("user99") }
    }

    @Test
    fun observeProfile_multipleEmissions_allForwarded() = runTest {
        val profile2 = mockk<UserProfile>(relaxed = true)
        every { userRepository.getUserProfileFlow("user1") } returns flowOf(profile, profile2)

        useCase.observeProfile("user1").test {
            assertEquals(profile,  awaitItem())
            assertEquals(profile2, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun observeProfile_emptyFlow_completesWithNoItems() = runTest {
        every { userRepository.getUserProfileFlow("user1") } returns flowOf()

        useCase.observeProfile("user1").test {
            awaitComplete()
        }
    }

    // ── getActiveUserId ───────────────────────────────────────────────────────

    @Test
    fun getActiveUserId_returnsIdFromRepository() = runTest {
        val result = useCase.getActiveUserId()

        assertEquals("user1", result)
    }

    @Test
    fun getActiveUserId_repositoryReturnsNull_returnsNull() = runTest {
        coEvery { userRepository.getActiveUserId() } returns null

        val result = useCase.getActiveUserId()

        assertNull(result)
    }

    @Test
    fun getActiveUserId_repositoryCalledOnce() = runTest {
        useCase.getActiveUserId()

        coVerify(exactly = 1) { userRepository.getActiveUserId() }
    }

    // ── userExists ────────────────────────────────────────────────────────────

    @Test
    fun userExists_repositoryReturnsTrue_returnsTrue() = runTest {
        coEvery { userRepository.userExists() } returns true

        assertTrue(useCase.userExists())
    }

    @Test
    fun userExists_repositoryReturnsFalse_returnsFalse() = runTest {
        coEvery { userRepository.userExists() } returns false

        assertFalse(useCase.userExists())
    }

    @Test
    fun userExists_repositoryCalledOnce() = runTest {
        useCase.userExists()

        coVerify(exactly = 1) { userRepository.userExists() }
    }

    @Test
    fun userExists_calledMultipleTimes_repositoryCalledEachTime() = runTest {
        useCase.userExists()
        useCase.userExists()

        coVerify(exactly = 2) { userRepository.userExists() }
    }
}
