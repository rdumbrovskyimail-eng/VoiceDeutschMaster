// src/androidTest/java/com/voicedeutsch/master/app/worker/BackupWorkerTest.kt
package com.voicedeutsch.master.app.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.voicedeutsch.master.data.remote.sync.BackupManager
import com.voicedeutsch.master.data.remote.sync.BackupResult
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
class BackupWorkerTest : KoinTest {

    private lateinit var context: Context
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        backupManager = mockk(relaxed = true)
        startKoin { modules(module { single { backupManager } }) }
    }

    @After
    fun tearDown() { stopKoin() }

    private suspend fun buildAndRun(runAttemptCount: Int = 0): Result =
        TestListenableWorkerBuilder<BackupWorker>(context)
            .setRunAttemptCount(runAttemptCount)
            .build()
            .doWork()

    // ── Local backup fails → Result.failure ───────────────────────────────

    @Test
    fun doWork_localBackupReturnsNull_returnsFailure() = runTest {
        coEvery { backupManager.createLocalBackup() } returns null
        val result = buildAndRun()
        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).outputData.getString(BackupWorker.KEY_ERROR)
        assertNotNull(error)
        assertTrue(error!!.contains("Local backup failed"))
    }

    @Test
    fun doWork_localBackupFails_doesNotCallCloudBackup() = runTest {
        coEvery { backupManager.createLocalBackup() } returns null
        buildAndRun()
        coVerify(exactly = 0) { backupManager.createCloudBackup() }
    }

    @Test
    fun doWork_localBackupFails_doesNotCallCleanup() = runTest {
        coEvery { backupManager.createLocalBackup() } returns null
        buildAndRun()
        coVerify(exactly = 0) { backupManager.cleanOldLocalBackups(any()) }
    }

    // ── Local success + cloud success → Result.success ────────────────────

    @Test
    fun doWork_localAndCloudSuccess_returnsSuccess() = runTest {
        coEvery { backupManager.createLocalBackup() } returns "/data/local/backup.db"
        coEvery { backupManager.createCloudBackup() } returns BackupResult.Success(
            storagePath = "gs://bucket/users/user1/backup.db", sizeBytes = 1024L)
        assertTrue(buildAndRun() is Result.Success)
    }

    @Test
    fun doWork_cloudSuccess_outputDataContainsLocalPath() = runTest {
        val localPath = "/data/local/backup.db"
        coEvery { backupManager.createLocalBackup() } returns localPath
        coEvery { backupManager.createCloudBackup() } returns BackupResult.Success(
            storagePath = "gs://bucket/backup.db", sizeBytes = 512L)
        val output = (buildAndRun() as Result.Success).outputData
        assertEquals(localPath, output.getString(BackupWorker.KEY_LOCAL_PATH))
    }

    @Test
    fun doWork_cloudSuccess_outputDataContainsCloudPath() = runTest {
        coEvery { backupManager.createLocalBackup() } returns "/local/backup.db"
        coEvery { backupManager.createCloudBackup() } returns BackupResult.Success(
            storagePath = "gs://bucket/cloud_backup.db", sizeBytes = 512L)
        val output = (buildAndRun() as Result.Success).outputData
        assertEquals("gs://bucket/cloud_backup.db", output.getString(BackupWorker.KEY_CLOUD_PATH))
    }

    @Test
    fun doWork_cloudSuccess_callsCleanOldLocalBackups() = runTest {
        coEvery { backupManager.createLocalBackup() } returns "/local/backup.db"
        coEvery { backupManager.createCloudBackup() } returns BackupResult.Success(
            storagePath = "gs://bucket/backup.db", sizeBytes = 512L)
        buildAndRun()
        coVerify(exactly = 1) { backupManager.cleanOldLocalBackups(keepDays = 30) }
    }

    // ── Local success + cloud error, attempts remaining → Result.retry ────

    @Test
    fun doWork_cloudError_firstAttempt_returnsRetry() = runTest {
        coEvery { backupManager.createLocalBackup() } returns "/local/backup.db"
        coEvery { backupManager.createCloudBackup() } returns BackupResult.Error("Network timeout")
        assertTrue(buildAndRun(runAttemptCount = 0) is Result.Retry)
    }

    @Test
    fun doWork_cloudError_secondAttempt_returnsRetry() = runTest {
        coEvery { backupManager.createLocalBackup() } returns "/local/backup.db"
        coEvery { backupManager.createCloudBackup() } returns BackupResult.Error("Network timeout")
        assertTrue(buildAndRun(runAttemptCount = 1) is Result.Retry)
    }

    @Test
    fun doWork_cloudError_firstAttempt_doesNotCallCleanup() = runTest {
        coEvery { backupManager.createLocalBackup() } returns "/local/backup.db"
        coEvery { backupManager.createCloudBackup() } returns BackupResult.Error("Network error")
        buildAndRun(runAttemptCount = 0)
        coVerify(exactly = 0) { backupManager.cleanOldLocalBackups(any()) }
    }

    // ── Max attempts exhausted → Result.failure ───────────────────────────

    @Test
    fun doWork_cloudError_maxAttemptsExhausted_returnsFailure() = runTest {
        coEvery { backupManager.createLocalBackup() } returns "/local/backup.db"
        coEvery { backupManager.createCloudBackup() } returns BackupResult.Error("Storage quota exceeded")
        assertTrue(buildAndRun(runAttemptCount = BackupWorker.MAX_ATTEMPTS - 1) is Result.Failure)
    }

    @Test
    fun doWork_cloudError_exhausted_outputDataContainsLocalPath() = runTest {
        val localPath = "/local/backup.db"
        coEvery { backupManager.createLocalBackup() } returns localPath
        coEvery { backupManager.createCloudBackup() } returns BackupResult.Error("quota exceeded")
        val output = (buildAndRun(runAttemptCount = BackupWorker.MAX_ATTEMPTS - 1) as Result.Failure).outputData
        assertEquals(localPath, output.getString(BackupWorker.KEY_LOCAL_PATH))
    }

    @Test
    fun doWork_cloudError_exhausted_outputDataContainsErrorMessage() = runTest {
        coEvery { backupManager.createLocalBackup() } returns "/local/backup.db"
        coEvery { backupManager.createCloudBackup() } returns BackupResult.Error("quota exceeded")
        val output = (buildAndRun(runAttemptCount = BackupWorker.MAX_ATTEMPTS - 1) as Result.Failure).outputData
        assertEquals("quota exceeded", output.getString(BackupWorker.KEY_ERROR))
    }

    @Test
    fun doWork_cloudError_exhausted_doesNotCallCleanup() = runTest {
        coEvery { backupManager.createLocalBackup() } returns "/local/backup.db"
        coEvery { backupManager.createCloudBackup() } returns BackupResult.Error("error")
        buildAndRun(runAttemptCount = BackupWorker.MAX_ATTEMPTS - 1)
        coVerify(exactly = 0) { backupManager.cleanOldLocalBackups(any()) }
    }

    // ── Constants ─────────────────────────────────────────────────────────

    @Test fun workName_isAutoBackup() { assertEquals("auto_backup", BackupWorker.WORK_NAME) }
    @Test fun maxAttempts_isThree() { assertEquals(3, BackupWorker.MAX_ATTEMPTS) }
    @Test fun keyLocalPath_isCorrect() { assertEquals("local_backup_path", BackupWorker.KEY_LOCAL_PATH) }
    @Test fun keyCloudPath_isCorrect() { assertEquals("cloud_backup_path", BackupWorker.KEY_CLOUD_PATH) }
    @Test fun keyError_isCorrect() { assertEquals("error_message", BackupWorker.KEY_ERROR) }
}
