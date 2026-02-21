package com.voicedeutsch.master.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voicedeutsch.master.data.remote.sync.BackupManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Weekly automatic local backup of the Room database.
 * Architecture lines 1730-1734 (Backup → Cloud).
 */
class BackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val backupManager: BackupManager by inject()

    override suspend fun doWork(): Result {
        return runCatching {
            backupManager.createLocalBackup()
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val WORK_NAME = "auto_backup"
    }
}