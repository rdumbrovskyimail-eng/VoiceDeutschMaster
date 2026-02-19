package com.voicedeutsch.master.util

import android.content.Context
import android.os.Build
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
    }

    private val logDirectory: File by lazy {
        File(context.filesDir, "crash_logs").also { dir ->
            if (!dir.exists()) dir.mkdirs()
        }
    }

    private fun install() {
        try {
            Thread.setDefaultUncaughtExceptionHandler(this)
            android.util.Log.i("CrashLogger", "✅ CrashLogger installed")
            android.util.Log.i("CrashLogger", "📁 Logs directory: ${logDirectory.absolutePath}")
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
        val crashFile = File(logDirectory, "${CRASH_PREFIX}${timestamp}.txt")

        try {
            crashFile.writeText(buildString {
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
                append("EXCEPTION\n")
                append("=".repeat(70)).append("\n")
                append("Type: ${throwable.javaClass.name}\n")
                append("Message: ${throwable.message}\n")
                append("\nStack Trace:\n")
                append(throwable.stackTraceToString())
                append("\n")

                var cause = throwable.cause
                var depth = 0
                while (cause != null && depth < 5) {
                    append("=".repeat(70)).append("\n")
                    append("CAUSED BY\n")
                    append("=".repeat(70)).append("\n")
                    append("Type: ${cause.javaClass.name}\n")
                    append("Message: ${cause.message}\n")
                    append("\nStack Trace:\n")
                    append(cause.stackTraceToString())
                    cause = cause.cause
                    depth++
                }
            })
            android.util.Log.e("CrashLogger", "✅ Crash log saved: ${crashFile.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e("CrashLogger", "❌ Failed to write crash log", e)
        }
    }

    fun getAllLogs(): List<LogFile> {
        return logDirectory.listFiles()?.mapNotNull { file ->
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
            location = logDirectory.absolutePath
        )
    }

    fun getCrashLogDirectory(): String = logDirectory.absolutePath

    fun startLogging() {
        android.util.Log.i("CrashLogger", "📁 Logs: ${logDirectory.absolutePath}")
    }

    fun saveLogCatErrors(): File? = null
}

data class LogFile(val file: File, val type: LogType, val timestamp: Long) {
    val name: String get() = file.name
    val sizeKB: Long get() = file.length() / 1024
    val formattedDate: String get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

enum class LogType { CRASH, LOGCAT }

data class LogStats(val totalCrashes: Int, val totalLogCats: Int, val totalSizeBytes: Long, val location: String) {
    val totalSizeKB: Long get() = totalSizeBytes / 1024
}