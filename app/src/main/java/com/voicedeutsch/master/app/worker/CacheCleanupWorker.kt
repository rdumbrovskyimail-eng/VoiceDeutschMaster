package com.voicedeutsch.master.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voicedeutsch.master.data.local.file.AudioCacheManager
import com.voicedeutsch.master.data.remote.sync.BackupManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Periodic cleanup of old audio cache and backup files.
 * Architecture line 591 (WorkManager — Очистка кэша).
 */
class CacheCleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val audioCacheManager: AudioCacheManager by inject()
    private val backupManager: BackupManager by inject()

    override suspend fun doWork(): Result {
        return runCatching {
            audioCacheManager.evictOlderThan(days = 30)
            // ✅ ИСПРАВЛЕНО: cleanOldBackups → cleanOldLocalBackups (реальный метод BackupManager)
            backupManager.cleanOldLocalBackups(keepDays = 30)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val WORK_NAME = "cache_cleanup"
    }
}