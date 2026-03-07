// Путь: src/test/java/com/voicedeutsch/master/data/remote/sync/BackupManagerTest.kt
package com.voicedeutsch.master.data.remote.sync

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.voicedeutsch.master.data.local.database.AppDatabase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BackupManagerTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    private lateinit var sut: BackupManager

    // Firebase mocks
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var storageRef: StorageReference
    private lateinit var childRef: StorageReference
    private lateinit var usersCollection: CollectionReference
    private lateinit var userDocument: DocumentReference
    private lateinit var backupsCollection: CollectionReference
    private lateinit var backupDocument: DocumentReference

    // DB mocks
    private lateinit var openHelper: SupportSQLiteOpenHelper
    private lateinit var writableDatabase: SupportSQLiteDatabase

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        db = mockk(relaxed = true)
        firestore = mockk(relaxed = true)
        storage = mockk(relaxed = true)
        auth = mockk(relaxed = true)

        firebaseUser = mockk(relaxed = true)
        storageRef = mockk(relaxed = true)
        childRef = mockk(relaxed = true)
        usersCollection = mockk(relaxed = true)
        userDocument = mockk(relaxed = true)
        backupsCollection = mockk(relaxed = true)
        backupDocument = mockk(relaxed = true)
        openHelper = mockk(relaxed = true)
        writableDatabase = mockk(relaxed = true)

        // Context filesDir → our tempDir
        every { context.filesDir } returns tempDir

        // DB open helper
        every { db.openHelper } returns openHelper
        every { openHelper.writableDatabase } returns writableDatabase
        every { writableDatabase.execSQL(any()) } just Runs

        // Auth
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "test_uid"

        // Storage chain
        every { storage.reference } returns storageRef
        every { storageRef.child(any()) } returns childRef

        // Firestore chain
        every { firestore.collection("users") } returns usersCollection
        every { usersCollection.document("test_uid") } returns userDocument
        every { userDocument.collection("backups") } returns backupsCollection
        every { backupsCollection.document(any()) } returns backupDocument

        // Package manager (for getAppVersion)
        val packageManager = mockk<PackageManager>(relaxed = true)
        val packageInfo = PackageInfo().apply { versionName = "1.0.0" }
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "com.voicedeutsch.master"
        every { packageManager.getPackageInfo("com.voicedeutsch.master", 0) } returns packageInfo

        sut = BackupManager(context, db, firestore, storage, auth)
    }

    // ── createLocalBackup ─────────────────────────────────────────────────────

    @Test
    fun createLocalBackup_vacuumIntoExecuted_returnsBackupPath() = runTest {
        val sqlSlot = slot<String>()
        every { writableDatabase.execSQL(capture(sqlSlot)) } answers {
            // Simulate VACUUM INTO by creating the actual file
            val pathRegex = Regex("VACUUM INTO '(.+)'")
            val match = pathRegex.find(sqlSlot.captured)
            match?.groupValues?.get(1)?.let { File(it).writeBytes(ByteArray(100)) }
        }

        val result = sut.createLocalBackup()

        assertNotNull(result)
        assertTrue(result!!.endsWith(".db"))
        assertTrue(result.contains("backup_"))
        assertTrue(sqlSlot.captured.startsWith("VACUUM INTO '"))
    }

    @Test
    fun createLocalBackup_vacuumProducesEmptyFile_returnsNull() = runTest {
        every { writableDatabase.execSQL(any()) } answers {
            // VACUUM INTO creates an empty file — simulates failure
            val pathRegex = Regex("VACUUM INTO '(.+)'")
            val match = pathRegex.find(firstArg<String>())
            match?.groupValues?.get(1)?.let { File(it).createNewFile() }
        }

        val result = sut.createLocalBackup()

        assertNull(result)
    }

    @Test
    fun createLocalBackup_vacuumThrowsException_returnsNull() = runTest {
        every { writableDatabase.execSQL(any()) } throws RuntimeException("SQLite error")

        val result = sut.createLocalBackup()

        assertNull(result)
    }

    @Test
    fun createLocalBackup_createsBackupsDirectoryIfMissing() = runTest {
        val backupsDir = File(tempDir, "backups")
        assertFalse(backupsDir.exists())

        every { writableDatabase.execSQL(any()) } answers {
            val pathRegex = Regex("VACUUM INTO '(.+)'")
            val match = pathRegex.find(firstArg<String>())
            match?.groupValues?.get(1)?.let { File(it).apply { parentFile?.mkdirs(); writeBytes(ByteArray(50)) } }
        }

        sut.createLocalBackup()

        assertTrue(backupsDir.exists())
    }

    @Test
    fun createLocalBackup_pathContainsEscapedSingleQuotes_ifPathHasThem() = runTest {
        // Verify SQL injection protection: single quotes in path are escaped
        val sqlSlot = slot<String>()
        every { writableDatabase.execSQL(capture(sqlSlot)) } answers {
            val pathRegex = Regex("VACUUM INTO '(.+)'")
            val match = pathRegex.find(sqlSlot.captured)
            match?.groupValues?.get(1)?.let { File(it).writeBytes(ByteArray(100)) }
        }

        sut.createLocalBackup()

        // SQL must not contain unescaped problematic sequences
        assertFalse(sqlSlot.captured.contains("''"))  // normal path — no escaping needed
        assertTrue(sqlSlot.captured.contains("VACUUM INTO '"))
    }

    // ── listLocalBackups ──────────────────────────────────────────────────────

    @Test
    fun listLocalBackups_noBackupsDir_returnsEmptyList() {
        val backupsDir = File(tempDir, "backups")
        assertFalse(backupsDir.exists())

        val result = sut.listLocalBackups()

        assertEquals(emptyList<File>(), result)
    }

    @Test
    fun listLocalBackups_multipleFiles_sortedByLastModifiedDescending() {
        val backupsDir = File(tempDir, "backups").apply { mkdirs() }
        val older = File(backupsDir, "backup_1000.db").apply { writeText("x"); setLastModified(1000L) }
        val newer = File(backupsDir, "backup_2000.db").apply { writeText("x"); setLastModified(2000L) }
        val newest = File(backupsDir, "backup_3000.db").apply { writeText("x"); setLastModified(3000L) }

        val result = sut.listLocalBackups()

        assertEquals(3, result.size)
        assertEquals(newest.name, result[0].name)
        assertEquals(newer.name, result[1].name)
        assertEquals(older.name, result[2].name)
    }

    @Test
    fun listLocalBackups_nonDbFilesIgnored() {
        val backupsDir = File(tempDir, "backups").apply { mkdirs() }
        File(backupsDir, "backup_1000.db").writeText("x")
        File(backupsDir, "notes.txt").writeText("x")
        File(backupsDir, "backup_2000.json").writeText("x")

        val result = sut.listLocalBackups()

        assertEquals(1, result.size)
        assertTrue(result[0].name.endsWith(".db"))
    }

    @Test
    fun listLocalBackups_emptyDir_returnsEmptyList() {
        File(tempDir, "backups").mkdirs()

        val result = sut.listLocalBackups()

        assertEquals(emptyList<File>(), result)
    }

    // ── restoreFromLocalBackup ────────────────────────────────────────────────

    @Test
    fun restoreFromLocalBackup_fileNotFound_returnsFalse() = runTest {
        val result = sut.restoreFromLocalBackup("/nonexistent/path/backup.db")

        assertFalse(result)
    }

    @Test
    fun restoreFromLocalBackup_validFile_closesDbAndCopies() = runTest {
        val backupFile = File(tempDir, "backup_test.db").apply { writeBytes(ByteArray(512)) }
        val dbFile = File(tempDir, "app.db")
        every { db.close() } just Runs
        every { context.getDatabasePath(any()) } returns dbFile

        val result = sut.restoreFromLocalBackup(backupFile.absolutePath)

        assertTrue(result)
        verify { db.close() }
        assertTrue(dbFile.exists())
        assertEquals(512, dbFile.length())
    }

    @Test
    fun restoreFromLocalBackup_dbCloseThrows_returnsFalse() = runTest {
        val backupFile = File(tempDir, "backup_test.db").apply { writeBytes(ByteArray(64)) }
        every { db.close() } throws RuntimeException("DB close error")

        val result = sut.restoreFromLocalBackup(backupFile.absolutePath)

        assertFalse(result)
    }

    // ── cleanOldLocalBackups ──────────────────────────────────────────────────

    @Test
    fun cleanOldLocalBackups_deletesFilesOlderThanKeepDays() = runTest {
        val backupsDir = File(tempDir, "backups").apply { mkdirs() }
        val cutoff = System.currentTimeMillis() - 31 * 24 * 60 * 60 * 1000L
        val oldFile = File(backupsDir, "backup_old.db").apply {
            writeText("x")
            setLastModified(cutoff - 1000L)
        }
        val newFile = File(backupsDir, "backup_new.db").apply {
            writeText("x")
            setLastModified(System.currentTimeMillis())
        }

        sut.cleanOldLocalBackups(keepDays = 30)

        assertFalse(oldFile.exists())
        assertTrue(newFile.exists())
    }

    @Test
    fun cleanOldLocalBackups_noOldFiles_nothingDeleted() = runTest {
        val backupsDir = File(tempDir, "backups").apply { mkdirs() }
        val recentFile = File(backupsDir, "backup_recent.db").apply {
            writeText("x")
            setLastModified(System.currentTimeMillis())
        }

        sut.cleanOldLocalBackups(keepDays = 30)

        assertTrue(recentFile.exists())
    }

    @Test
    fun cleanOldLocalBackups_emptyDir_doesNotThrow() = runTest {
        File(tempDir, "backups").mkdirs()

        assertDoesNotThrow { sut.cleanOldLocalBackups() }
    }

    @Test
    fun cleanOldLocalBackups_noDirExists_doesNotThrow() = runTest {
        assertDoesNotThrow { sut.cleanOldLocalBackups() }
    }

    // ── createCloudBackup ─────────────────────────────────────────────────────

    @Test
    fun createCloudBackup_notAuthenticated_returnsError() = runTest {
        every { auth.currentUser } returns null

        val result = sut.createCloudBackup()

        assertTrue(result is BackupResult.Error)
        assertTrue((result as BackupResult.Error).message.contains("not authenticated"))
    }

    @Test
    fun createCloudBackup_localBackupFails_returnsError() = runTest {
        every { writableDatabase.execSQL(any()) } throws RuntimeException("VACUUM error")

        val result = sut.createCloudBackup()

        assertTrue(result is BackupResult.Error)
        assertTrue((result as BackupResult.Error).message.contains("local backup"))
    }

    @Test
    fun createCloudBackup_storageUploadFails_returnsError() = runTest {
        every { writableDatabase.execSQL(any()) } answers {
            val pathRegex = Regex("VACUUM INTO '(.+)'")
            val match = pathRegex.find(firstArg<String>())
            match?.groupValues?.get(1)?.let { File(it).writeBytes(ByteArray(100)) }
        }

        val uploadTask = mockk<UploadTask>()
        every { childRef.putFile(any(), any()) } returns uploadTask
        every { uploadTask.await() } throws RuntimeException("Upload failed")

        val result = sut.createCloudBackup()

        assertTrue(result is BackupResult.Error)
        assertTrue((result as BackupResult.Error).message.contains("Cloud backup failed"))
    }

    @Test
    fun createCloudBackup_successfulUpload_returnsSuccess() = runTest {
        every { writableDatabase.execSQL(any()) } answers {
            val pathRegex = Regex("VACUUM INTO '(.+)'")
            val match = pathRegex.find(firstArg<String>())
            match?.groupValues?.get(1)?.let { File(it).writeBytes(ByteArray(256)) }
        }

        val uploadTask = mockk<UploadTask>()
        val taskSnapshot = mockk<UploadTask.TaskSnapshot>(relaxed = true)
        every { childRef.putFile(any(), any()) } returns uploadTask
        every { uploadTask.await() } returns taskSnapshot

        val setTask = mockk<Task<Void>>(relaxed = true)
        every { backupDocument.set(any()) } returns setTask
        every { setTask.await() } returns null

        // listCloudBackups for pruning — return few backups (under limit)
        val query = mockk<Query>(relaxed = true)
        val querySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { backupsCollection.orderBy(any<String>(), any()) } returns query
        every { query.get() } returns mockk(relaxed = true) {
            every { await() } returns querySnapshot
        }
        every { querySnapshot.documents } returns emptyList()

        val result = sut.createCloudBackup()

        assertTrue(result is BackupResult.Success)
        val success = result as BackupResult.Success
        assertTrue(success.storagePath.contains("test_uid"))
        assertTrue(success.storagePath.contains("backup_"))
        assertEquals(256L, success.sizeBytes)
    }

    // ── listCloudBackups ──────────────────────────────────────────────────────

    @Test
    fun listCloudBackups_notAuthenticated_returnsEmptyList() = runTest {
        every { auth.currentUser } returns null

        val result = sut.listCloudBackups()

        assertEquals(emptyList<BackupMetadata>(), result)
    }

    @Test
    fun listCloudBackups_firestoreThrows_returnsEmptyList() = runTest {
        val query = mockk<Query>(relaxed = true)
        every { backupsCollection.orderBy(any<String>(), any()) } returns query
        every { query.get() } returns mockk(relaxed = true) {
            every { await() } throws RuntimeException("Firestore error")
        }

        val result = sut.listCloudBackups()

        assertEquals(emptyList<BackupMetadata>(), result)
    }

    @Test
    fun listCloudBackups_validDocuments_parsedToMetadataList() = runTest {
        val doc1 = mockk<DocumentSnapshot>(relaxed = true)
        every { doc1.getString("storagePath") } returns "users/test_uid/backups/backup_1.db"
        every { doc1.getLong("timestamp") } returns 2000L
        every { doc1.getLong("sizeBytes") } returns 1024L
        every { doc1.getString("deviceModel") } returns "Pixel 7"
        every { doc1.getString("appVersion") } returns "1.0.0"
        every { doc1.getString("fileName") } returns "backup_1.db"

        val doc2 = mockk<DocumentSnapshot>(relaxed = true)
        every { doc2.getString("storagePath") } returns "users/test_uid/backups/backup_2.db"
        every { doc2.getLong("timestamp") } returns 1000L
        every { doc2.getLong("sizeBytes") } returns 512L
        every { doc2.getString("deviceModel") } returns "Pixel 6"
        every { doc2.getString("appVersion") } returns "0.9.0"
        every { doc2.getString("fileName") } returns "backup_2.db"

        val querySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { querySnapshot.documents } returns listOf(doc1, doc2)

        val query = mockk<Query>(relaxed = true)
        every { backupsCollection.orderBy(any<String>(), any()) } returns query
        every { query.get() } returns mockk(relaxed = true) {
            every { await() } returns querySnapshot
        }

        val result = sut.listCloudBackups()

        assertEquals(2, result.size)
        assertEquals("users/test_uid/backups/backup_1.db", result[0].storagePath)
        assertEquals(2000L, result[0].timestamp)
        assertEquals(1024L, result[0].sizeBytes)
        assertEquals("Pixel 7", result[0].deviceModel)
        assertEquals("1.0.0", result[0].appVersion)
        assertEquals("backup_1.db", result[0].fileName)
    }

    @Test
    fun listCloudBackups_documentMissingStoragePath_skipped() = runTest {
        val invalidDoc = mockk<DocumentSnapshot>(relaxed = true)
        every { invalidDoc.getString("storagePath") } returns null
        every { invalidDoc.getLong("timestamp") } returns 1000L

        val querySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { querySnapshot.documents } returns listOf(invalidDoc)

        val query = mockk<Query>(relaxed = true)
        every { backupsCollection.orderBy(any<String>(), any()) } returns query
        every { query.get() } returns mockk(relaxed = true) {
            every { await() } returns querySnapshot
        }

        val result = sut.listCloudBackups()

        assertEquals(emptyList<BackupMetadata>(), result)
    }

    @Test
    fun listCloudBackups_documentMissingTimestamp_skipped() = runTest {
        val invalidDoc = mockk<DocumentSnapshot>(relaxed = true)
        every { invalidDoc.getString("storagePath") } returns "users/test_uid/backups/backup.db"
        every { invalidDoc.getLong("timestamp") } returns null

        val querySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { querySnapshot.documents } returns listOf(invalidDoc)

        val query = mockk<Query>(relaxed = true)
        every { backupsCollection.orderBy(any<String>(), any()) } returns query
        every { query.get() } returns mockk(relaxed = true) {
            every { await() } returns querySnapshot
        }

        val result = sut.listCloudBackups()

        assertEquals(emptyList<BackupMetadata>(), result)
    }

    // ── restoreFromCloudBackup ────────────────────────────────────────────────

    @Test
    fun restoreFromCloudBackup_downloadFails_returnsFalse() = runTest {
        val downloadTask = mockk<FileDownloadTask>(relaxed = true)
        every { childRef.getFile(any<File>()) } returns downloadTask
        every { downloadTask.await() } throws RuntimeException("Download failed")

        val result = sut.restoreFromCloudBackup("users/test_uid/backups/backup.db")

        assertFalse(result)
    }

    @Test
    fun restoreFromCloudBackup_successfulDownload_closesDbAndCopies() = runTest {
        val cacheDir = File(tempDir, "cache").apply { mkdirs() }
        every { context.cacheDir } returns cacheDir

        val downloadTask = mockk<FileDownloadTask>(relaxed = true)
        val taskSnapshot = mockk<FileDownloadTask.TaskSnapshot>(relaxed = true)
        every { childRef.getFile(any<File>()) } answers {
            // Simulate download by writing to the temp file
            val file = firstArg<File>()
            file.writeBytes(ByteArray(256))
            downloadTask
        }
        every { downloadTask.await() } returns taskSnapshot

        every { db.close() } just Runs
        val dbFile = File(tempDir, "restored.db")
        every { context.getDatabasePath(any()) } returns dbFile

        val result = sut.restoreFromCloudBackup("users/test_uid/backups/backup.db")

        assertTrue(result)
        verify { db.close() }
        assertTrue(dbFile.exists())
    }

    @Test
    fun restoreFromCloudBackup_dbCloseThrows_returnsFalse() = runTest {
        val cacheDir = File(tempDir, "cache").apply { mkdirs() }
        every { context.cacheDir } returns cacheDir

        val downloadTask = mockk<FileDownloadTask>(relaxed = true)
        val taskSnapshot = mockk<FileDownloadTask.TaskSnapshot>(relaxed = true)
        every { childRef.getFile(any<File>()) } answers {
            val file = firstArg<File>()
            file.writeBytes(ByteArray(64))
            downloadTask
        }
        every { downloadTask.await() } returns taskSnapshot
        every { db.close() } throws RuntimeException("Cannot close DB")

        val result = sut.restoreFromCloudBackup("users/test_uid/backups/backup.db")

        assertFalse(result)
    }

    // ── BackupMetadata data class ─────────────────────────────────────────────

    @Test
    fun backupMetadata_creation_allFieldsStoredCorrectly() {
        val meta = BackupMetadata(
            storagePath = "users/uid/backups/backup_1.db",
            timestamp = 9999L,
            sizeBytes = 2048L,
            deviceModel = "Samsung S23",
            appVersion = "2.1.0",
            fileName = "backup_1.db",
        )
        assertEquals("users/uid/backups/backup_1.db", meta.storagePath)
        assertEquals(9999L, meta.timestamp)
        assertEquals(2048L, meta.sizeBytes)
        assertEquals("Samsung S23", meta.deviceModel)
        assertEquals("2.1.0", meta.appVersion)
        assertEquals("backup_1.db", meta.fileName)
    }

    @Test
    fun backupMetadata_copy_changesOneField() {
        val original = BackupMetadata("path", 1000L, 512L, "Pixel", "1.0", "file.db")
        val copy = original.copy(sizeBytes = 1024L)
        assertEquals(1024L, copy.sizeBytes)
        assertEquals(original.storagePath, copy.storagePath)
        assertEquals(original.timestamp, copy.timestamp)
    }

    @Test
    fun backupMetadata_equals_twoIdenticalInstances() {
        val a = BackupMetadata("path", 1000L, 512L, "Pixel", "1.0", "file.db")
        val b = BackupMetadata("path", 1000L, 512L, "Pixel", "1.0", "file.db")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ── BackupResult sealed class ─────────────────────────────────────────────

    @Test
    fun backupResult_success_holdsStoragePathAndSize() {
        val result = BackupResult.Success("users/uid/backups/file.db", 4096L)
        assertEquals("users/uid/backups/file.db", result.storagePath)
        assertEquals(4096L, result.sizeBytes)
    }

    @Test
    fun backupResult_error_holdsMessage() {
        val result = BackupResult.Error("Something went wrong")
        assertEquals("Something went wrong", result.message)
    }

    @Test
    fun backupResult_success_isInstanceOfBackupResult() {
        val result: BackupResult = BackupResult.Success("path", 0L)
        assertTrue(result is BackupResult.Success)
        assertFalse(result is BackupResult.Error)
    }

    @Test
    fun backupResult_error_isInstanceOfBackupResult() {
        val result: BackupResult = BackupResult.Error("err")
        assertTrue(result is BackupResult.Error)
        assertFalse(result is BackupResult.Success)
    }

    @Test
    fun backupResult_success_copy_changesSize() {
        val original = BackupResult.Success("path", 100L)
        val copy = original.copy(sizeBytes = 200L)
        assertEquals(200L, copy.sizeBytes)
        assertEquals("path", copy.storagePath)
    }

    @Test
    fun backupResult_error_copy_changesMessage() {
        val original = BackupResult.Error("original error")
        val copy = original.copy(message = "new error")
        assertEquals("new error", copy.message)
    }
}
