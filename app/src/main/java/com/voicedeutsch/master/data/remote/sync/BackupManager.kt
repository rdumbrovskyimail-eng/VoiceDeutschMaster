package com.voicedeutsch.master.data.remote.sync

import android.content.Context
import com.voicedeutsch.master.data.local.database.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Manages local backup and restore of the Room database.
 * Cloud backup is delegated to CloudSyncService (v2.0).
 */
class BackupManager(
    private val context: Context
) {

    /**
     * Creates a backup copy of the Room DB in the app's files directory.
     * @return Backup file path, or null on failure.
     */
    suspend fun createLocalBackup(): String? {
        return runCatching {
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            if (!dbFile.exists()) return null
            val backupDir = File(context.filesDir, "backups")
            backupDir.mkdirs()
            val backupFile = File(backupDir, "backup_${System.currentTimeMillis()}.db")
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }
            backupFile.absolutePath
        }.getOrNull()
    }

    /**
     * Lists available local backups sorted by most recent first.
     */
    fun listBackups(): List<File> {
        val backupDir = File(context.filesDir, "backups")
        return backupDir.listFiles { _, name -> name.endsWith(".db") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Restores the database from a backup file. Requires app restart.
     */
    suspend fun restoreFromBackup(backupPath: String): Boolean {
        return runCatching {
            val backupFile = File(backupPath)
            if (!backupFile.exists()) return false
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            FileInputStream(backupFile).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        }.getOrDefault(false)
    }

    /** Deletes backups older than the given number of days. */
    suspend fun cleanOldBackups(keepDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - keepDays * 24 * 60 * 60 * 1000L
        listBackups().filter { it.lastModified() < cutoff }.forEach { it.delete() }
    }
}