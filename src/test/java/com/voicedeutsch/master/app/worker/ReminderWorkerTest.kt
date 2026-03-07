// src/androidTest/java/com/voicedeutsch/master/app/worker/ReminderWorkerTest.kt
package com.voicedeutsch.master.app.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ReminderWorkerTest {

    private lateinit var context: Context

    @Before
    fun setUp() { context = ApplicationProvider.getApplicationContext() }

    private suspend fun buildAndRun(): Result =
        TestListenableWorkerBuilder<ReminderWorker>(context).build().doWork()

    // ── doWork always succeeds ────────────────────────────────────────────

    @Test fun doWork_alwaysReturnsSuccess() = runTest { assertTrue(buildAndRun() is Result.Success) }
    @Test fun doWork_doesNotReturnFailure() = runTest { assertFalse(buildAndRun() is Result.Failure) }
    @Test fun doWork_doesNotReturnRetry() = runTest { assertFalse(buildAndRun() is Result.Retry) }

    @Test
    fun doWork_calledMultipleTimes_alwaysReturnsSuccess() = runTest {
        repeat(3) { i ->
            assertTrue("Expected success on attempt ${i + 1}", buildAndRun() is Result.Success)
        }
    }

    // ── Constants ─────────────────────────────────────────────────────────

    @Test fun workName_isStudyReminder() { assertEquals("study_reminder", ReminderWorker.WORK_NAME) }
    @Test fun channelId_isStudyReminders() { assertEquals("study_reminders", ReminderWorker.CHANNEL_ID) }
    @Test fun notificationId_is1001() { assertEquals(1001, ReminderWorker.NOTIFICATION_ID) }
}
