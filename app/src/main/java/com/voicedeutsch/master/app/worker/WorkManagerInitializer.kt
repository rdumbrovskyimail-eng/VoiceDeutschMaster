package com.voicedeutsch.master.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules all periodic workers at app startup.
 * Called from VoiceDeutschApp.onCreate().
 */
object WorkManagerInitializer {

    fun initialize(context: Context) {
        val wm = WorkManager.getInstance(context)

        // ── SRS recalculation — daily ─────────────────────────────────────────
        val srsWork = PeriodicWorkRequestBuilder<SrsRecalculationWorker>(
            1, TimeUnit.DAYS
        ).setConstraints(
            Constraints.Builder().setRequiresBatteryNotLow(true).build()
        ).build()
        wm.enqueueUniquePeriodicWork(
            SrsRecalculationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            srsWork
        )

        // ── Study reminder — daily ────────────────────────────────────────────
        val reminderWork = PeriodicWorkRequestBuilder<ReminderWorker>(
            1, TimeUnit.DAYS
        ).build()
        wm.enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderWork
        )

        // ── Cache cleanup — weekly ────────────────────────────────────────────
        val cacheWork = PeriodicWorkRequestBuilder<CacheCleanupWorker>(
            7, TimeUnit.DAYS
        ).setConstraints(
            Constraints.Builder().setRequiresBatteryNotLow(true).build()
        ).build()
        wm.enqueueUniquePeriodicWork(
            CacheCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cacheWork
        )

        // ── Auto backup — weekly ──────────────────────────────────────────────
        val backupWork = PeriodicWorkRequestBuilder<BackupWorker>(
            7, TimeUnit.DAYS
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()
        ).build()
        wm.enqueueUniquePeriodicWork(
            BackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            backupWork
        )
    }
}