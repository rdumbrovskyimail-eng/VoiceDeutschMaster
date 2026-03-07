// src/androidTest/java/com/voicedeutsch/master/app/worker/SrsRecalculationWorkerTest.kt
package com.voicedeutsch.master.app.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.domain.usecase.achievement.CheckAchievementsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

@RunWith(AndroidJUnit4::class)
@SmallTest
class SrsRecalculationWorkerTest : KoinTest {

    private lateinit var context: Context
    private lateinit var userRepository: UserRepository
    private lateinit var checkAchievements: CheckAchievementsUseCase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        userRepository = mockk(relaxed = true)
        checkAchievements = mockk(relaxed = true)
        startKoin {
            modules(module {
                single { userRepository }
                single { checkAchievements }
            })
        }
    }

    @After
    fun tearDown() { stopKoin() }

    private suspend fun buildAndRun(): Result =
        TestListenableWorkerBuilder<SrsRecalculationWorker>(context).build().doWork()

    // ── No users ──────────────────────────────────────────────────────────

    @Test
    fun doWork_noUsers_returnsSuccess() = runTest {
        coEvery { userRepository.getAllUserIds() } returns emptyList()
        assertTrue(buildAndRun() is Result.Success)
    }

    @Test
    fun doWork_noUsers_neverCallsUpdateStreak() = runTest {
        coEvery { userRepository.getAllUserIds() } returns emptyList()
        buildAndRun()
        coVerify(exactly = 0) { userRepository.updateStreakIfNeeded(any()) }
    }

    @Test
    fun doWork_noUsers_neverCallsCheckAchievements() = runTest {
        coEvery { userRepository.getAllUserIds() } returns emptyList()
        buildAndRun()
        coVerify(exactly = 0) { checkAchievements(any()) }
    }

    // ── Single user ───────────────────────────────────────────────────────

    @Test
    fun doWork_singleUser_returnsSuccess() = runTest {
        coEvery { userRepository.getAllUserIds() } returns listOf("user_1")
        assertTrue(buildAndRun() is Result.Success)
    }

    @Test
    fun doWork_singleUser_callsUpdateStreakForThatUser() = runTest {
        coEvery { userRepository.getAllUserIds() } returns listOf("user_1")
        buildAndRun()
        coVerify(exactly = 1) { userRepository.updateStreakIfNeeded("user_1") }
    }

    @Test
    fun doWork_singleUser_callsCheckAchievementsForThatUser() = runTest {
        coEvery { userRepository.getAllUserIds() } returns listOf("user_1")
        buildAndRun()
        coVerify(exactly = 1) { checkAchievements("user_1") }
    }

    // ── Multiple users ────────────────────────────────────────────────────

    @Test
    fun doWork_multipleUsers_callsUpdateStreakForEachUser() = runTest {
        coEvery { userRepository.getAllUserIds() } returns listOf("u1", "u2", "u3")
        buildAndRun()
        coVerify(exactly = 1) { userRepository.updateStreakIfNeeded("u1") }
        coVerify(exactly = 1) { userRepository.updateStreakIfNeeded("u2") }
        coVerify(exactly = 1) { userRepository.updateStreakIfNeeded("u3") }
    }

    @Test
    fun doWork_multipleUsers_callsCheckAchievementsForEachUser() = runTest {
        coEvery { userRepository.getAllUserIds() } returns listOf("u1", "u2", "u3")
        buildAndRun()
        coVerify(exactly = 1) { checkAchievements("u1") }
        coVerify(exactly = 1) { checkAchievements("u2") }
        coVerify(exactly = 1) { checkAchievements("u3") }
    }

    @Test
    fun doWork_multipleUsers_totalCallCount_matchesUserCount() = runTest {
        coEvery { userRepository.getAllUserIds() } returns listOf("u1", "u2", "u3")
        buildAndRun()
        coVerify(exactly = 3) { userRepository.updateStreakIfNeeded(any()) }
        coVerify(exactly = 3) { checkAchievements(any()) }
    }

    @Test
    fun doWork_multipleUsers_returnsSuccess() = runTest {
        coEvery { userRepository.getAllUserIds() } returns listOf("u1", "u2")
        assertTrue(buildAndRun() is Result.Success)
    }

    // ── Exception handling → Result.retry ─────────────────────────────────

    @Test
    fun doWork_getAllUserIdsThrows_returnsRetry() = runTest {
        coEvery { userRepository.getAllUserIds() } throws RuntimeException("DB error")
        assertTrue(buildAndRun() is Result.Retry)
    }

    @Test
    fun doWork_updateStreakThrows_returnsRetry() = runTest {
        coEvery { userRepository.getAllUserIds() } returns listOf("u1")
        coEvery { userRepository.updateStreakIfNeeded(any()) } throws RuntimeException("streak error")
        assertTrue(buildAndRun() is Result.Retry)
    }

    @Test
    fun doWork_checkAchievementsThrows_returnsRetry() = runTest {
        coEvery { userRepository.getAllUserIds() } returns listOf("u1")
        coEvery { checkAchievements(any()) } throws RuntimeException("achievement error")
        assertTrue(buildAndRun() is Result.Retry)
    }

    @Test
    fun doWork_exceptionDuringIteration_doesNotPropagateException() = runTest {
        coEvery { userRepository.getAllUserIds() } returns listOf("u1")
        coEvery { userRepository.updateStreakIfNeeded(any()) } throws OutOfMemoryError("OOM")
        assertNotNull(buildAndRun())
    }

    // ── Constants ─────────────────────────────────────────────────────────

    @Test fun workName_isSrsRecalculation() {
        assertEquals("srs_recalculation", SrsRecalculationWorker.WORK_NAME)
    }
}
