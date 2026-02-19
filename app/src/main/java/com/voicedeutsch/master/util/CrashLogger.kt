package com.voicedeutsch.master.util

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🔥 CrashLogger - Автоматический перехват крашей + LogCat сохранение
 *
 * Логи сохраняются в: Download/LOG5/
 */
class CrashLogger private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: CrashLogger? = null

        fun init(context: Context): CrashLogger {
            return instance ?: synchronized(this) {
                instance ?: CrashLogger(context.applicationContext).also {
                    instance = it
                    it.setupUncaughtExceptionHandler()
                }
            }
        }

        fun getInstance(): CrashLogger? = instance

        private const val CRASH_PREFIX = "crash_"
        private const val LOGCAT_PREFIX = "logcat_errors_"
    }

    private val logDirectory: File by lazy {
        val downloadDir = File(Environment.getExternalStorageDirectory(), "Download/LOG5")
        if (downloadDir.exists() || downloadDir.mkdirs()) {
            downloadDir
        } else {
            File(context.filesDir, "LOG5").apply { mkdirs() }
        }
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(throwable, thread)
            } catch (e: Exception) {
                android.util.Log.e("CrashLogger", "❌ Failed to save crash log", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        android.util.Log.i("CrashLogger", "✅ CrashLogger initialized")
        android.util.Log.i("CrashLogger", "📁 Logs directory: ${logDirectory.absolutePath}")
    }

    private fun saveCrashLog(throwable: Throwable, thread: Thread) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val crashFile = File(logDirectory, "${CRASH_PREFIX}${timestamp}.txt")

        try {
            crashFile.writeText(buildString {
                appendLine("=" * 80)
                appendLine("🔥 CRASH REPORT - VoiceDeutschMaster")
                appendLine("=" * 80)
                appendLine()
                appendLine("Timestamp: $timestamp")
                appendLine("Thread: ${thread.name}")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("App Version: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}")
                appendLine("Location: ${crashFile.absolutePath}")
                appendLine()
                appendLine("-" * 80)
                appendLine("EXCEPTION:")
                appendLine("-" * 80)
                appendLine(throwable.stackTraceToString())
                appendLine()
                appendLine("-" * 80)
                appendLine("LOGCAT (Last 500 lines):")
                appendLine("-" * 80)

                try {
                    val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "500"))
                    process.inputStream.bufferedReader().use { reader ->
                        reader.forEachLine { appendLine(it) }
                    }
                } catch (e: Exception) {
                    appendLine("❌ Failed to capture logcat: ${e.message}")
                }

                appendLine("-" * 80)
                appendLine("END OF CRASH REPORT")
                appendLine("=" * 80)
            })

            android.util.Log.e("CrashLogger", "✅ Crash log saved: ${crashFile.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e("CrashLogger", "❌ Failed to write crash log", e)
        }
    }

    fun saveLogCatErrors(): File? {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val logcatFile = File(logDirectory, "${LOGCAT_PREFIX}${timestamp}.txt")

        return try {
            logcatFile.writeText(buildString {
                appendLine("=" * 80)
                appendLine("📋 LOGCAT ERRORS - VoiceDeutschMaster")
                appendLine("=" * 80)
                appendLine()
                appendLine("Timestamp: $timestamp")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("Location: ${logcatFile.absolutePath}")
                appendLine()
                appendLine("-" * 80)
                appendLine("ERRORS & WARNINGS:")
                appendLine("-" * 80)

                val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-s", "E:*", "W:*"))
                process.inputStream.bufferedReader().use { reader ->
                    var lineCount = 0
                    reader.forEachLine { line ->
                        appendLine(line)
                        lineCount++
                    }
                    if (lineCount == 0) {
                        appendLine()
                        appendLine("✅ No errors or warnings found in logcat!")
                    }
                }

                appendLine("-" * 80)
                appendLine("END OF LOGCAT ERRORS")
                appendLine("=" * 80)
            })

            android.util.Log.i("CrashLogger", "✅ LogCat errors saved: ${logcatFile.absolutePath}")
            logcatFile
        } catch (e: Exception) {
            android.util.Log.e("CrashLogger", "❌ Failed to save logcat", e)
            null
        }
    }

    fun getAllLogs(): List<LogFile> {
        return logDirectory.listFiles()?.mapNotNull { file ->
            when {
                file.name.startsWith(CRASH_PREFIX) -> LogFile(
                    file = file,
                    type = LogType.CRASH,
                    timestamp = file.lastModified()
                )
                file.name.startsWith(LOGCAT_PREFIX) -> LogFile(
                    file = file,
                    type = LogType.LOGCAT,
                    timestamp = file.lastModified()
                )
                else -> null
            }
        }?.sortedByDescending { it.timestamp } ?: emptyList()
    }

    fun getLatestCrashLog(): File? {
        return getAllLogs()
            .firstOrNull { it.type == LogType.CRASH }
            ?.file
    }

    fun cleanOldLogs(keepCount: Int = 20) {
        val allLogs = getAllLogs()
        if (allLogs.size > keepCount) {
            allLogs.drop(keepCount).forEach { logFile ->
                logFile.file.delete()
                android.util.Log.d("CrashLogger", "🗑️ Deleted old log: ${logFile.file.name}")
            }
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
        android.util.Log.i("CrashLogger", "📁 Logs will be saved to: ${logDirectory.absolutePath}")
    }
}

data class LogFile(
    val file: File,
    val type: LogType,
    val timestamp: Long,
) {
    val name: String get() = file.name
    val sizeKB: Long get() = file.length() / 1024
    val formattedDate: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

enum class LogType {
    CRASH,
    LOGCAT,
}

data class LogStats(
    val totalCrashes: Int,
    val totalLogCats: Int,
    val totalSizeBytes: Long,
    val location: String,
) {
    val totalSizeKB: Long get() = totalSizeBytes / 1024

    override fun toString(): String = buildString {
        appendLine("Total crashes: $totalCrashes")
        appendLine("Total logcat saves: $totalLogCats")
        appendLine("Total size: $totalSizeKB KB")
        appendLine("Location: $location")
    }
}

private operator fun String.times(count: Int): String = repeat(count)