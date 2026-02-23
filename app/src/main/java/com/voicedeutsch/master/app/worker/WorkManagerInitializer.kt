package com.voicedeutsch.master.app.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules all periodic workers at app startup.
 * Called from VoiceDeutschApp.onCreate().
 */
object WorkManagerInitializer {

    private const val TAG = "WorkManagerInitializer"

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
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()
        wm.enqueueUniquePeriodicWork(
            BackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            backupWork
        )

        // Наблюдаем за результатом BackupWorker — парсим outputData
        // после каждого завершённого запуска (SUCCEEDED / FAILED).
        observeBackupWorker(context)
    }

    /**
     * Подписывается на WorkInfo BackupWorker'а и логирует результат.
     * KEY_LOCAL_PATH  — путь к локальному staging-файлу бекапа
     * KEY_CLOUD_PATH  — путь в Firebase Storage (gs://bucket/users/uid/backups/...)
     * KEY_ERROR       — сообщение об ошибке если worker завершился с FAILED
     *
     * Используйте эти данные для отображения статуса в SettingsScreen
     * или для аналитики через Firebase Crashlytics.
     */
    private fun observeBackupWorker(context: Context) {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(BackupWorker.WORK_NAME)
            .observeForever { workInfoList ->
                val info = workInfoList?.firstOrNull() ?: return@observeForever

                when (info.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val localPath  = info.outputData.getString(BackupWorker.KEY_LOCAL_PATH)
                        val cloudPath  = info.outputData.getString(BackupWorker.KEY_CLOUD_PATH)
                        Log.d(TAG, "Backup succeeded — local=$localPath, cloud=$cloudPath")
                    }
                    WorkInfo.State.FAILED -> {
                        val error      = info.outputData.getString(BackupWorker.KEY_ERROR)
                        val localPath  = info.outputData.getString(BackupWorker.KEY_LOCAL_PATH)
                        // Локальный бекап мог сохраниться даже при облачной ошибке
                        Log.e(TAG, "Backup failed — error=$error, localFallback=$localPath")
                    }
                    else -> Unit // RUNNING / ENQUEUED / BLOCKED — ничего не делаем
                }
            }
    }
}