package com.voicedeutsch.master.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CrashLogger private constructor(
    private val context: Context
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    companion object {
        @Volatile
        private var instance: CrashLogger? = null

        fun init(context: Context): CrashLogger {
            return instance ?: synchronized(this) {
                instance ?: CrashLogger(context.applicationContext).also {
                    instance = it
                    it.install()
                }
            }
        }

        fun getInstance(): CrashLogger? = instance

        private const val CRASH_PREFIX = "crash_"
        private const val LOGCAT_PREFIX = "logcat_errors_"
        private const val LOG_FOLDER = "LOG5"
    }

    // Internal storage — всегда работает, для getAllLogs/getLatestCrashLog
    private val internalLogDir: File by lazy {
        File(context.filesDir, LOG_FOLDER).apply { mkdirs() }
    }

    private fun install() {
        try {
            Thread.setDefaultUncaughtExceptionHandler(this)
            android.util.Log.i("CrashLogger", "✅ CrashLogger installed")
            android.util.Log.i("CrashLogger", "📁 Download path: Download/$LOG_FOLDER/")
            android.util.Log.i("CrashLogger", "📁 Internal path: ${internalLogDir.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e("CrashLogger", "❌ Failed to install", e)
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            saveCrashLog(throwable, thread)
        } catch (e: Exception) {
            android.util.Log.e("CrashLogger", "❌ Failed to save crash log", e)
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashLog(throwable: Throwable, thread: Thread) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val fileName = "${CRASH_PREFIX}${timestamp}.txt"
        val logcatOutput = captureLogcat()

        val content = buildString {
            append("=".repeat(70)).append("\n")
            append("🔥 CRASH REPORT - VoiceDeutschMaster\n")
            append("=".repeat(70)).append("\n")
            append("Timestamp: $timestamp\n")
            append("Thread: ${thread.name} (ID: ${thread.id})\n")
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            try {
                append("App Version: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}\n")
            } catch (_: Exception) {
                append("App Version: unknown\n")
            }
            append("\n")

            append("=".repeat(70)).append("\n")
            append("EXCEPTION DETAILS\n")
            append("=".repeat(70)).append("\n")
            append("Type: ${throwable.javaClass.simpleName}\n")
            append("Message: ${throwable.message}\n")
            append("\nStack Trace:\n")
            append(throwable.stackTraceToString())
            append("\n\n")

            throwable.cause?.let { cause ->
                append("=".repeat(70)).append("\n")
                append("CAUSED BY\n")
                append("=".repeat(70)).append("\n")
                append("Type: ${cause.javaClass.simpleName}\n")
                append("Message: ${cause.message}\n")
                append("\nStack Trace:\n")
                append(cause.stackTraceToString())
                append("\n\n")
            }

            append("=".repeat(70)).append("\n")
            append("LOGCAT DUMP (Last 1000 lines)\n")
            append("=".repeat(70)).append("\n")
            append(logcatOutput)
        }

        // 1. INTERNAL — всегда работает
        try {
            File(internalLogDir, fileName).writeText(content)
            android.util.Log.e("CrashLogger", "✅ Internal: ${internalLogDir.absolutePath}/$fileName")
        } catch (e: Exception) {
            android.util.Log.e("CrashLogger", "❌ Internal failed", e)
        }

        // 2. MEDIASTORE — пишет в Download/LOG5 БЕЗ разрешений
        try {
            writeViaMediaStore(fileName, content)
            android.util.Log.e("CrashLogger", "✅ Download/$LOG_FOLDER/$fileName")
        } catch (e: Exception) {
            android.util.Log.e("CrashLogger", "❌ MediaStore failed", e)
        }
    }

    /**
     * Пишет файл в Download/LOG5 через MediaStore.
     * НЕ требует НИКАКИХ разрешений на Android 10+ (API 29+).
     */
    private fun writeViaMediaStore(fileName: String, content: String) {
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/$LOG_FOLDER"
            )
        }

        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw Exception("MediaStore insert returned null URI")

        resolver.openOutputStream(uri)?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
            stream.flush()
        } ?: throw Exception("Failed to open OutputStream for URI: $uri")
    }

    private fun captureLogcat(): String {
        return try {
            val pid = android.os.Process.myPid()
            val process = Runtime.getRuntime().exec(
                arrayOf(
                    "logcat", "-d",
                    "-t", "1000",
                    "-v", "threadtime",
                    "--pid=$pid",
                    "*:V"
                )
            )
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            output.ifBlank { "(No logcat output captured)" }
        } catch (e: Exception) {
            "(Failed to capture logcat: ${e.message})"
        }
    }

    fun saveLogCatErrors(): File? {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val fileName = "${LOGCAT_PREFIX}${timestamp}.txt"

        val content = buildString {
            append("=".repeat(70)).append("\n")
            append("📋 LOGCAT ERRORS - VoiceDeutschMaster\n")
            append("=".repeat(70)).append("\n\n")

            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-s", "E:*", "W:*"))
            process.inputStream.bufferedReader().use { reader ->
                var lineCount = 0
                reader.forEachLine { line ->
                    append(line).append("\n")
                    lineCount++
                }
                if (lineCount == 0) append("\n✅ No errors or warnings found!\n")
            }
        }

        // Internal
        val file = File(internalLogDir, fileName)
        return try {
            file.writeText(content)
            try { writeViaMediaStore(fileName, content) } catch (_: Exception) {}
            file
        } catch (e: Exception) {
            null
        }
    }

    fun getAllLogs(): List<LogFile> {
        return internalLogDir.listFiles()?.mapNotNull { file ->
            when {
                file.name.startsWith(CRASH_PREFIX) -> LogFile(file, LogType.CRASH, file.lastModified())
                file.name.startsWith(LOGCAT_PREFIX) -> LogFile(file, LogType.LOGCAT, file.lastModified())
                else -> null
            }
        }?.sortedByDescending { it.timestamp } ?: emptyList()
    }

    fun getLatestCrashLog(): File? = getAllLogs().firstOrNull { it.type == LogType.CRASH }?.file

    fun cleanOldLogs(keepCount: Int = 20) {
        val allLogs = getAllLogs()
        if (allLogs.size > keepCount) {
            allLogs.drop(keepCount).forEach { it.file.delete() }
        }
    }

    fun getStats(): LogStats {
        val logs = getAllLogs()
        return LogStats(
            totalCrashes = logs.count { it.type == LogType.CRASH },
            totalLogCats = logs.count { it.type == LogType.LOGCAT },
            totalSizeBytes = logs.sumOf { it.file.length() },
            location = "Download/$LOG_FOLDER/"
        )
    }

    fun getCrashLogDirectory(): String = "Download/$LOG_FOLDER/"
    fun startLogging() {
        android.util.Log.i("CrashLogger", "📁 Logs: Download/$LOG_FOLDER/")
    }
}

data class LogFile(val file: File, val type: LogType, val timestamp: Long) {
    val name: String get() = file.name
    val sizeKB: Long get() = file.length() / 1024
    val formattedDate: String get() =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

enum class LogType { CRASH, LOGCAT }

data class LogStats(
    val totalCrashes: Int,
    val totalLogCats: Int,
    val totalSizeBytes: Long,
    val location: String
) {
    val totalSizeKB: Long get() = totalSizeBytes / 1024
}