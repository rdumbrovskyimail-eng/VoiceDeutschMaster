// src/androidTest/java/com/voicedeutsch/master/app/worker/CacheCleanupWorkerTest.kt
package com.voicedeutsch.master.app.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.voicedeutsch.master.data.local.file.AudioCacheManager
import com.voicedeutsch.master.data.remote.sync.BackupManager
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
class CacheCleanupWorkerTest : KoinTest {

    private lateinit var context: Context
    private lateinit var audioCacheManager: AudioCacheManager
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        audioCacheManager = mockk(relaxed = true)
        backupManager = mockk(relaxed = true)
        startKoin {
            modules(module {
                single { audioCacheManager }
                single { backupManager }
            })
        }
    }

    @After
    fun tearDown() { stopKoin() }

    private suspend fun buildAndRun(): Result =
        TestListenableWorkerBuilder<CacheCleanupWorker>(context).build().doWork()

    // ── Happy path ────────────────────────────────────────────────────────

    @Test
    fun doWork_bothManagersSucceed_returnsSuccess() = runTest {
        assertTrue(buildAndRun() is Result.Success)
    }

    @Test
    fun doWork_callsEvictOlderThan30Days() = runTest {
        buildAndRun()
        coVerify(exactly = 1) { audioCacheManager.evictOlderThan(days = 30) }
    }

    @Test
    fun doWork_callsCleanOldLocalBackupsWith30Days() = runTest {
        buildAndRun()
        coVerify(exactly = 1) { backupManager.cleanOldLocalBackups(keepDays = 30) }
    }

    @Test
    fun doWork_callsBothManagers() = runTest {
        buildAndRun()
        coVerify { audioCacheManager.evictOlderThan(any()) }
        coVerify { backupManager.cleanOldLocalBackups(any()) }
    }

    // ── Exception handling → Result.retry ─────────────────────────────────

    @Test
    fun doWork_audioCacheManagerThrows_returnsRetry() = runTest {
        coEvery { audioCacheManager.evictOlderThan(any()) } throws RuntimeException("disk error")
        assertTrue(buildAndRun() is Result.Retry)
    }

    @Test
    fun doWork_backupManagerThrows_returnsRetry() = runTest {
        coEvery { backupManager.cleanOldLocalBackups(any()) } throws RuntimeException("fs error")
        assertTrue(buildAndRun() is Result.Retry)
    }

    @Test
    fun doWork_audioCacheThrows_doesNotPropagate() = runTest {
        coEvery { audioCacheManager.evictOlderThan(any()) } throws IllegalStateException("unexpected")
        assertNotNull(buildAndRun())
    }

    @Test
    fun doWork_bothThrow_returnsRetry() = runTest {
        coEvery { audioCacheManager.evictOlderThan(any()) } throws RuntimeException("err1")
        coEvery { backupManager.cleanOldLocalBackups(any()) } throws RuntimeException("err2")
        assertTrue(buildAndRun() is Result.Retry)
    }

    // ── Constants ─────────────────────────────────────────────────────────

    @Test fun workName_isCacheCleanup() { assertEquals("cache_cleanup", CacheCleanupWorker.WORK_NAME) }
}
