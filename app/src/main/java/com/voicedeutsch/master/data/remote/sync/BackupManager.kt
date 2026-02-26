package com.voicedeutsch.master.data.remote.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.voicedeutsch.master.data.local.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * BackupManager — управление локальными и облачными резервными копиями базы данных.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * АРХИТЕКТУРА РЕЗЕРВНОГО КОПИРОВАНИЯ:
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Локальный бекап (Room DB файл → app/files/backups/):
 *   ├── Создаётся перед каждой облачной загрузкой
 *   ├── Хранится до 30 дней (cleanOldLocalBackups)
 *   └── Используется как staging area для Storage upload
 *
 * Облачный бекап (Firebase Storage):
 *   Path: users/{uid}/backups/backup_{timestamp}.db
 *   ├── Загружается через FirebaseStorage с retry (настроен в DataModule)
 *   ├── StorageMetadata: contentType, deviceModel, appVersion
 *   └── Метаданные бекапа пишутся в Firestore для листинга без Storage API
 *
 * Firestore (метаданные бекапов):
 *   Collection: users/{uid}/backups
 *   Document:   {timestamp}
 *   Fields:     storagePath, timestamp, sizeBytes, deviceModel, appVersion
 *
 * ════════════════════════════════════════════════════════════════════════════
 * МЕТОД БЕКАПА — VACUUM INTO (API 27+):
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Почему НЕ FileInputStream + wal_checkpoint:
 *   wal_checkpoint(TRUNCATE) сбрасывает WAL, но между checkpoint и copyTo
 *   фоновый воркер (CloudSyncService, SrsRecalculationWorker) может записать
 *   новую транзакцию → скопированный файл будет битым (corrupted).
 *
 * Почему VACUUM INTO:
 *   SQLite создаёт атомарный снимок БД в момент вызова — без остановки записей,
 *   без race condition. Встроенная гарантия консистентности на уровне движка.
 *   Доступно с Android API 27 (SQLite 3.27.0+, 2019).
 *
 * ════════════════════════════════════════════════════════════════════════════
 * СТРУКТУРА STORAGE:
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   users/
 *   └── {uid}/
 *       └── backups/
 *           ├── backup_1708000000000.db
 *           └── backup_1708100000000.db
 *
 * Security Rules (Storage):
 *   match /users/{uid}/backups/{file} {
 *     allow read, write: if request.auth != null && request.auth.uid == uid;
 *   }
 *
 * Security Rules (Firestore):
 *   match /users/{uid}/backups/{docId} {
 *     allow read, write: if request.auth != null && request.auth.uid == uid;
 *   }
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
) {
    companion object {
        private const val TAG = "BackupManager"
        private const val LOCAL_BACKUP_DIR = "backups"
        private const val BACKUP_EXTENSION = ".db"
        private const val FIRESTORE_BACKUPS_COLLECTION = "backups"
        private const val STORAGE_BACKUPS_PREFIX = "backups"
        private const val MAX_CLOUD_BACKUPS = 5
    }

    // ── Локальный бекап ───────────────────────────────────────────────────────

    /**
     * Создаёт локальную копию Room DB через VACUUM INTO.
     *
     * ✅ FIX: Заменили wal_checkpoint + FileInputStream на VACUUM INTO.
     *
     * Проблема старого подхода:
     *   wal_checkpoint(TRUNCATE) сбрасывает WAL-лог, но не блокирует запись.
     *   Если в промежутке между checkpoint и copyTo фоновый сервис запишет
     *   транзакцию, скопированный файл окажется inconsistent / corrupted.
     *
     * VACUUM INTO (SQLite 3.27.0+, API 27+):
     *   Создаёт атомарный снимок БД в отдельный файл прямо "на горячую".
     *   SQLite сам гарантирует консистентность — race condition невозможен.
     *   Никакого копирования файлов, никаких WAL-трюков.
     *
     * @return Абсолютный путь к файлу бекапа, null при ошибке.
     */
    suspend fun createLocalBackup(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val backupDir = File(context.filesDir, LOCAL_BACKUP_DIR).apply { mkdirs() }
            val backupFile = File(backupDir, "backup_${System.currentTimeMillis()}$BACKUP_EXTENSION")

            // Удаляем файл если вдруг существует — VACUUM INTO не перезаписывает
            if (backupFile.exists()) backupFile.delete()

            // ✅ VACUUM INTO: атомарный снимок без race condition
            // Путь должен быть абсолютным и экранированным для SQL
            val escapedPath = backupFile.absolutePath.replace("'", "''")
            db.openHelper.writableDatabase.execSQL("VACUUM INTO '$escapedPath'")

            if (!backupFile.exists() || backupFile.length() == 0L) {
                Log.w(TAG, "VACUUM INTO produced empty or missing file")
                return@runCatching null
            }

            Log.d(TAG, "✅ Local backup created: ${backupFile.absolutePath} (${backupFile.length()} bytes)")
            backupFile.absolutePath

        }.getOrElse { e ->
            Log.e(TAG, "❌ createLocalBackup failed: ${e.message}", e)
            null
        }
    }

    /**
     * Возвращает список локальных бекапов, отсортированных от новых к старым.
     */
    fun listLocalBackups(): List<File> {
        val backupDir = File(context.filesDir, LOCAL_BACKUP_DIR)
        return backupDir.listFiles { _, name -> name.endsWith(BACKUP_EXTENSION) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Восстанавливает Room DB из локального файла бекапа.
     * ⚠️ Требует перезапуска приложения для применения изменений.
     *
     * @return true если восстановление прошло успешно.
     */
    suspend fun restoreFromLocalBackup(backupPath: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val backupFile = File(backupPath)
            if (!backupFile.exists()) {
                Log.w(TAG, "Backup file not found: $backupPath")
                return@runCatching false
            }

            // Закрываем БД перед перезаписью файла
            db.close()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            FileInputStream(backupFile).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "✅ Restored from local backup: $backupPath")
            true

        }.getOrElse { e ->
            Log.e(TAG, "❌ restoreFromLocalBackup failed: ${e.message}", e)
            false
        }
    }

    /**
     * Удаляет локальные бекапы старше [keepDays] дней.
     */
    suspend fun cleanOldLocalBackups(keepDays: Int = 30) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - keepDays * 24 * 60 * 60 * 1000L
        listLocalBackups()
            .filter { it.lastModified() < cutoff }
            .forEach { file ->
                file.delete()
                Log.d(TAG, "Deleted old local backup: ${file.name}")
            }
    }

    // ── Облачный бекап (Firebase Storage + Firestore) ─────────────────────────

    /**
     * Создаёт облачный бекап: VACUUM INTO → Firebase Storage → метаданные в Firestore.
     *
     * Порядок операций:
     *   1. VACUUM INTO → атомарный локальный файл (staging)
     *   2. Загрузить в Firebase Storage с метаданными
     *   3. Записать метаданные в Firestore (для листинга)
     *   4. Очистить старые облачные бекапы (оставить MAX_CLOUD_BACKUPS)
     *
     * @return [BackupResult] с результатом операции.
     */
    suspend fun createCloudBackup(): BackupResult = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid
            ?: return@withContext BackupResult.Error("User not authenticated")

        val localPath = createLocalBackup()
            ?: return@withContext BackupResult.Error("Failed to create local backup")

        val localFile = File(localPath)
        val timestamp = System.currentTimeMillis()
        val fileName  = "backup_$timestamp$BACKUP_EXTENSION"
        val storagePath = "$STORAGE_BACKUPS_PREFIX/$fileName"

        return@withContext runCatching {

            val storageRef = storage.reference.child("users/$uid/$storagePath")

            val metadata = StorageMetadata.Builder()
                .setContentType("application/octet-stream")
                .setCustomMetadata("timestamp", timestamp.toString())
                .setCustomMetadata("deviceModel", android.os.Build.MODEL)
                .setCustomMetadata("appVersion", getAppVersion())
                .build()

            storageRef.putFile(Uri.fromFile(localFile), metadata).await()

            Log.d(TAG, "✅ Uploaded to Storage: users/$uid/$storagePath (${localFile.length()} bytes)")

            val backupDoc = mapOf(
                "storagePath" to "users/$uid/$storagePath",
                "timestamp"   to timestamp,
                "sizeBytes"   to localFile.length(),
                "deviceModel" to android.os.Build.MODEL,
                "appVersion"  to getAppVersion(),
                "fileName"    to fileName,
            )

            firestore
                .collection("users")
                .document(uid)
                .collection(FIRESTORE_BACKUPS_COLLECTION)
                .document(timestamp.toString())
                .set(backupDoc)
                .await()

            Log.d(TAG, "✅ Backup metadata written to Firestore")

            pruneOldCloudBackups(uid)

            BackupResult.Success(
                storagePath = "users/$uid/$storagePath",
                sizeBytes   = localFile.length(),
            )

        }.getOrElse { e ->
            Log.e(TAG, "❌ createCloudBackup failed: ${e.message}", e)
            BackupResult.Error("Cloud backup failed: ${e.message}")
        }
    }

    /**
     * Возвращает список облачных бекапов пользователя из Firestore.
     * Отсортирован от новых к старым.
     */
    suspend fun listCloudBackups(): List<BackupMetadata> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext emptyList()

        runCatching {
            val snapshot = firestore
                .collection("users")
                .document(uid)
                .collection(FIRESTORE_BACKUPS_COLLECTION)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                runCatching {
                    BackupMetadata(
                        storagePath = doc.getString("storagePath") ?: return@mapNotNull null,
                        timestamp   = doc.getLong("timestamp")     ?: return@mapNotNull null,
                        sizeBytes   = doc.getLong("sizeBytes")     ?: 0L,
                        deviceModel = doc.getString("deviceModel") ?: "",
                        appVersion  = doc.getString("appVersion")  ?: "",
                        fileName    = doc.getString("fileName")    ?: "",
                    )
                }.getOrNull()
            }

        }.getOrElse { e ->
            Log.e(TAG, "❌ listCloudBackups failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Восстанавливает Room DB из облачного бекапа.
     * Скачивает файл из Firebase Storage → записывает поверх Room DB файла.
     * ⚠️ Требует перезапуска приложения для применения изменений.
     */
    suspend fun restoreFromCloudBackup(storagePath: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val tempFile = File(context.cacheDir, "restore_temp$BACKUP_EXTENSION")

            storage.reference.child(storagePath).getFile(tempFile).await()
            Log.d(TAG, "Downloaded from Storage: $storagePath → ${tempFile.absolutePath}")

            // Закрываем БД перед перезаписью файла
            db.close()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            FileInputStream(tempFile).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }

            tempFile.delete()
            Log.d(TAG, "✅ Restored from cloud backup: $storagePath")
            true

        }.getOrElse { e ->
            Log.e(TAG, "❌ restoreFromCloudBackup failed: ${e.message}", e)
            false
        }
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    private suspend fun pruneOldCloudBackups(uid: String) {
        val all = listCloudBackups()
        if (all.size <= MAX_CLOUD_BACKUPS) return

        all.drop(MAX_CLOUD_BACKUPS).forEach { backup ->
            runCatching {
                storage.reference.child(backup.storagePath).delete().await()
                firestore
                    .collection("users").document(uid)
                    .collection(FIRESTORE_BACKUPS_COLLECTION)
                    .document(backup.timestamp.toString())
                    .delete()
                    .await()
                Log.d(TAG, "Pruned old backup: ${backup.fileName}")
            }.onFailure { e ->
                Log.w(TAG, "Failed to prune backup ${backup.fileName}: ${e.message}")
            }
        }
    }

    private fun getAppVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }.getOrDefault("unknown")
}

// ── Data models ───────────────────────────────────────────────────────────────

data class BackupMetadata(
    val storagePath: String,
    val timestamp: Long,
    val sizeBytes: Long,
    val deviceModel: String,
    val appVersion: String,
    val fileName: String,
)

sealed class BackupResult {
    data class Success(val storagePath: String, val sizeBytes: Long) : BackupResult()
    data class Error(val message: String) : BackupResult()
}