// src/androidTest/java/com/voicedeutsch/master/app/worker/WorkManagerInitializerTest.kt
package com.voicedeutsch.master.app.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class WorkManagerInitializerTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
    }

    @After
    fun tearDown() { workManager.cancelAllWork() }

    // ── initialize() enqueues all 4 workers ───────────────────────────────

    @Test
    fun initialize_enqueuesSrsRecalculationWork() {
        WorkManagerInitializer.initialize(context)
        assertFalse(workManager.getWorkInfosForUniqueWork(SrsRecalculationWorker.WORK_NAME).get().isEmpty())
    }

    @Test
    fun initialize_enqueuesReminderWork() {
        WorkManagerInitializer.initialize(context)
        assertFalse(workManager.getWorkInfosForUniqueWork(ReminderWorker.WORK_NAME).get().isEmpty())
    }

    @Test
    fun initialize_enqueuesCacheCleanupWork() {
        WorkManagerInitializer.initialize(context)
        assertFalse(workManager.getWorkInfosForUniqueWork(CacheCleanupWorker.WORK_NAME).get().isEmpty())
    }

    @Test
    fun initialize_enqueuesBackupWork() {
        WorkManagerInitializer.initialize(context)
        assertFalse(workManager.getWorkInfosForUniqueWork(BackupWorker.WORK_NAME).get().isEmpty())
    }

    // ── KEEP policy — second call does not replace existing work ──────────

    @Test
    fun initialize_calledTwice_doesNotDuplicateSrsWork() {
        WorkManagerInitializer.initialize(context)
        WorkManagerInitializer.initialize(context)
        assertEquals(1, workManager.getWorkInfosForUniqueWork(SrsRecalculationWorker.WORK_NAME).get().size)
    }

    @Test
    fun initialize_calledTwice_doesNotDuplicateBackupWork() {
        WorkManagerInitializer.initialize(context)
        WorkManagerInitializer.initialize(context)
        assertEquals(1, workManager.getWorkInfosForUniqueWork(BackupWorker.WORK_NAME).get().size)
    }

    @Test
    fun initialize_calledTwice_doesNotDuplicateCacheCleanupWork() {
        WorkManagerInitializer.initialize(context)
        WorkManagerInitializer.initialize(context)
        assertEquals(1, workManager.getWorkInfosForUniqueWork(CacheCleanupWorker.WORK_NAME).get().size)
    }

    @Test
    fun initialize_calledTwice_doesNotDuplicateReminderWork() {
        WorkManagerInitializer.initialize(context)
        WorkManagerInitializer.initialize(context)
        assertEquals(1, workManager.getWorkInfosForUniqueWork(ReminderWorker.WORK_NAME).get().size)
    }

    // ── Enqueued work states ──────────────────────────────────────────────

    @Test
    fun initialize_srsWork_isEnqueuedOrRunning() {
        WorkManagerInitializer.initialize(context)
        val state = workManager.getWorkInfosForUniqueWork(SrsRecalculationWorker.WORK_NAME).get().firstOrNull()?.state
        assertTrue("Expected ENQUEUED or RUNNING, got $state",
            state == WorkInfo.State.ENQUEUED || state == WorkInfo.State.RUNNING)
    }

    @Test
    fun initialize_backupWork_isEnqueuedOrRunning() {
        WorkManagerInitializer.initialize(context)
        val state = workManager.getWorkInfosForUniqueWork(BackupWorker.WORK_NAME).get().firstOrNull()?.state
        assertTrue("Expected ENQUEUED or RUNNING, got $state",
            state == WorkInfo.State.ENQUEUED || state == WorkInfo.State.RUNNING)
    }

    @Test
    fun initialize_cacheCleanupWork_isEnqueuedOrRunning() {
        WorkManagerInitializer.initialize(context)
        val state = workManager.getWorkInfosForUniqueWork(CacheCleanupWorker.WORK_NAME).get().firstOrNull()?.state
        assertTrue("Expected ENQUEUED or RUNNING, got $state",
            state == WorkInfo.State.ENQUEUED || state == WorkInfo.State.RUNNING)
    }

    @Test
    fun initialize_reminderWork_isEnqueuedOrRunning() {
        WorkManagerInitializer.initialize(context)
        val state = workManager.getWorkInfosForUniqueWork(ReminderWorker.WORK_NAME).get().firstOrNull()?.state
        assertTrue("Expected ENQUEUED or RUNNING, got $state",
            state == WorkInfo.State.ENQUEUED || state == WorkInfo.State.RUNNING)
    }

    // ── Work name constants ───────────────────────────────────────────────

    @Test
    fun srsWorkName_isUnique_notEqualToOthers() {
        assertEquals(4, setOf(
            SrsRecalculationWorker.WORK_NAME, ReminderWorker.WORK_NAME,
            CacheCleanupWorker.WORK_NAME, BackupWorker.WORK_NAME,
        ).size)
    }

    @Test
    fun allWorkNames_areNonEmpty() {
        listOf(SrsRecalculationWorker.WORK_NAME, ReminderWorker.WORK_NAME,
            CacheCleanupWorker.WORK_NAME, BackupWorker.WORK_NAME,
        ).forEach { assertTrue("Work name '$it' must not be empty", it.isNotEmpty()) }
    }
}
