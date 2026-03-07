// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/achievement/GetUserAchievementsUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.achievement

import com.voicedeutsch.master.domain.model.achievement.UserAchievement
import com.voicedeutsch.master.domain.repository.AchievementRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetUserAchievementsUseCaseTest {

    private lateinit var achievementRepository: AchievementRepository
    private lateinit var useCase: GetUserAchievementsUseCase

    private val achievement1 = mockk<UserAchievement>(relaxed = true)
    private val achievement2 = mockk<UserAchievement>(relaxed = true)

    @BeforeEach
    fun setUp() {
        achievementRepository = mockk()
        useCase = GetUserAchievementsUseCase(achievementRepository)

        coEvery { achievementRepository.getUserAchievements(any()) } returns emptyList()
    }

    // ── invoke — delegates to repository ─────────────────────────────────────

    @Test
    fun invoke_returnsAchievementsFromRepository() = runTest {
        coEvery { achievementRepository.getUserAchievements("user1") } returns
            listOf(achievement1, achievement2)

        val result = useCase("user1")

        assertEquals(listOf(achievement1, achievement2), result)
    }

    @Test
    fun invoke_repositoryCalledWithCorrectUserId() = runTest {
        useCase("user42")

        coVerify(exactly = 1) { achievementRepository.getUserAchievements("user42") }
    }

    @Test
    fun invoke_emptyRepository_returnsEmptyList() = runTest {
        val result = useCase("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_calledMultipleTimes_repositoryCalledEachTime() = runTest {
        useCase("user1")
        useCase("user1")
        useCase("user1")

        coVerify(exactly = 3) { achievementRepository.getUserAchievements("user1") }
    }

    @Test
    fun invoke_differentUserIds_eachPassedToRepository() = runTest {
        val list1 = listOf(achievement1)
        val list2 = listOf(achievement2)
        coEvery { achievementRepository.getUserAchievements("u1") } returns list1
        coEvery { achievementRepository.getUserAchievements("u2") } returns list2

        assertEquals(list1, useCase("u1"))
        assertEquals(list2, useCase("u2"))
    }

    @Test
    fun invoke_repositoryThrows_exceptionPropagates() = runTest {
        coEvery { achievementRepository.getUserAchievements(any()) } throws
            RuntimeException("db error")

        val ex = org.junit.jupiter.api.assertThrows<RuntimeException> { useCase("user1") }
        assertEquals("db error", ex.message)
    }

    @Test
    fun invoke_singleAchievement_returnsSingleItemList() = runTest {
        coEvery { achievementRepository.getUserAchievements("user1") } returns
            listOf(achievement1)

        val result = useCase("user1")

        assertEquals(1, result.size)
        assertEquals(achievement1, result.single())
    }
}
