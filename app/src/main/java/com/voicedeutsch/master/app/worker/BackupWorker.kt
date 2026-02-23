package com.voicedeutsch.master.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.voicedeutsch.master.data.remote.sync.BackupManager
import com.voicedeutsch.master.data.remote.sync.BackupResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * BackupWorker — еженедельный автоматический бекап Room DB.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * МИГРАЦИЯ: локальный бекап → локальный + облачный (Firebase Storage)
 * ════════════════════════════════════════════════════════════════════════════
 *
 * БЫЛО:
 *   backupManager.createLocalBackup()   — только локальная копия в app/files/backups/
 *
 * СТАЛО:
 *   1. createLocalBackup()              — staging-копия в app/files/backups/
 *   2. createCloudBackup()              — загрузка в Firebase Storage +
 *                                         метаданные в Firestore
 *   3. cleanOldLocalBackups(keepDays=30) — чистка старых локальных копий
 *
 * ════════════════════════════════════════════════════════════════════════════
 * RETRY POLICY (настраивается в WorkManagerInitializer):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   Result.retry() — WorkManager повторит с экспоненциальным backoff.
 *   Используем retry только при сетевых ошибках (облачный бекап).
 *   Локальная ошибка → Result.failure() с диагностическими данными в outputData.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * OUTPUT DATA (доступно через WorkInfo.outputData):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   KEY_LOCAL_PATH   — абсолютный путь локального бекапа (при успехе)
 *   KEY_CLOUD_PATH   — Storage path облачного бекапа (при успехе)
 *   KEY_ERROR        — сообщение об ошибке (при failure)
 */
class BackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val backupManager: BackupManager by inject()

    override suspend fun doWork(): Result {
        Log.d(TAG, "BackupWorker started [attempt=${runAttemptCount + 1}/$MAX_ATTEMPTS]")

        // 1. Локальный бекап — staging area для облака
        val localPath = backupManager.createLocalBackup()
        if (localPath == null) {
            Log.e(TAG, "❌ Local backup failed — no database file found")
            // Локальная ошибка не лечится retry — сразу failure с диагностикой
            return Result.failure(
                workDataOf(KEY_ERROR to "Local backup failed: database file not found")
            )
        }
        Log.d(TAG, "✅ Local backup created: $localPath")

        // 2. Облачный бекап — Firebase Storage + Firestore метаданные
        val cloudResult = backupManager.createCloudBackup()
        return when (cloudResult) {
            is BackupResult.Success -> {
                // 3. Чистка старых локальных бекапов — только при полном успехе
                backupManager.cleanOldLocalBackups(keepDays = 30)
                Log.d(TAG, "✅ Cloud backup complete: ${cloudResult.storagePath} (${cloudResult.sizeBytes} bytes)")

                Result.success(
                    workDataOf(
                        KEY_LOCAL_PATH to localPath,
                        KEY_CLOUD_PATH to cloudResult.storagePath,
                    )
                )
            }

            is BackupResult.Error -> {
                Log.w(TAG, "⚠️ Cloud backup failed: ${cloudResult.message} [attempt=${runAttemptCount + 1}]")

                if (runAttemptCount < MAX_ATTEMPTS - 1) {
                    // Сетевая ошибка — WorkManager повторит с экспоненциальным backoff.
                    // Локальный бекап уже создан — он не пропадёт при retry.
                    Log.d(TAG, "Scheduling retry...")
                    Result.retry()
                } else {
                    // Исчерпаны все попытки — сообщаем failure, но локальный бекап есть.
                    Log.e(TAG, "❌ Cloud backup failed after $MAX_ATTEMPTS attempts")
                    Result.failure(
                        workDataOf(
                            KEY_LOCAL_PATH to localPath,
                            KEY_ERROR      to cloudResult.message,
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "BackupWorker"

        /** Имя уникальной задачи WorkManager (используется в WorkManagerInitializer). */
        const val WORK_NAME = "auto_backup"

        /** Максимальное число попыток (включая первую). */
        const val MAX_ATTEMPTS = 3

        // Output data keys
        const val KEY_LOCAL_PATH = "local_backup_path"
        const val KEY_CLOUD_PATH = "cloud_backup_path"
        const val KEY_ERROR      = "error_message"
    }
}
