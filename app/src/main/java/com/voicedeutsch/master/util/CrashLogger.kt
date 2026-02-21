package com.voicedeutsch.master.util

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🔥 CrashLogger — перехватчик необработанных исключений.
 *
 * При крэше:
 *  1. Сохраняет детальный stack trace в crash_TIMESTAMP.txt
 *  2. Вызывает AppLogger.dumpToFile() — сохраняет весь сеансовый лог в session_log_TIMESTAMP.txt
 *  3. Передаёт управление стандартному обработчику (Android показывает диалог «приложение остановлено»)
 *
 * Инициализация: вызови [init] самым первым в Application.onCreate() — ДО super().
 */
class CrashLogger private constructor(
    private val context: Context,
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    companion object {
        @Volatile
        private var instance: CrashLogger? = null

        private const val TAG            = "CrashLogger"
        private const val CRASH_PREFIX   = "crash_"
        private const val LOGCAT_PREFIX  = "logcat_errors_"

        fun init(context: Context): CrashLogger {
            return instance ?: synchronized(this) {
                instance ?: CrashLogger(context.applicationContext).also {
                    instance = it
                    it.install()
                }
            }
        }

        fun getInstance(): CrashLogger? = instance
    }

    // ── Директория логов ──────────────────────────────────────────────────────
    private val logDirectory: File by lazy {
        File(context.filesDir, "crash_logs").also { dir ->
            if (!dir.exists()) dir.mkdirs()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Установка перехватчика
    // ─────────────────────────────────────────────────────────────────────────

    private fun install() {
        try {
            Thread.setDefaultUncaughtExceptionHandler(this)
            android.util.Log.i(TAG, "✅ CrashLogger installed")
            android.util.Log.i(TAG, "📁 Logs directory: ${logDirectory.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to install", e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UncaughtExceptionHandler
    // ─────────────────────────────────────────────────────────────────────────

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val timestamp = timestamp()

            // 1️⃣ Сохраняем crash report
            saveCrashLog(throwable, thread, timestamp)

            // 2️⃣ Сбрасываем весь сеансовый лог AppLogger (если он запущен)
            dumpSessionLog(timestamp)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to save crash log", e)
        } finally {
            // 3️⃣ Передаём Android — показывает «приложение остановлено»
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Сохранение crash report
    // ─────────────────────────────────────────────────────────────────────────

    private fun saveCrashLog(throwable: Throwable, thread: Thread, timestamp: String) {
        val crashFile = File(logDirectory, "${CRASH_PREFIX}${timestamp}.txt")
        try {
            crashFile.writeText(buildCrashReport(throwable, thread, timestamp))
            android.util.Log.e(TAG, "✅ Crash log saved: ${crashFile.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to write crash log", e)
        }
    }

    private fun buildCrashReport(throwable: Throwable, thread: Thread, timestamp: String): String =
        buildString {
            append("=".repeat(70)).append("\n")
            append("🔥 CRASH REPORT — VoiceDeutschMaster\n")
            append("=".repeat(70)).append("\n")
            append("Timestamp : $timestamp\n")
            append("Thread    : ${thread.name} (ID: ${thread.id})\n")
            append("Device    : ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Android   : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            try {
                append("App ver   : ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}\n")
            } catch (_: Exception) {
                append("App ver   : unknown\n")
            }
            append("\n")
            append("=".repeat(70)).append("\n")
            append("EXCEPTION\n")
            append("=".repeat(70)).append("\n")
            append("Type    : ${throwable.javaClass.name}\n")
            append("Message : ${throwable.message}\n")
            append("\nStack Trace:\n")
            append(throwable.stackTraceToString())
            append("\n")

            var cause = throwable.cause
            var depth = 0
            while (cause != null && depth < 5) {
                append("=".repeat(70)).append("\n")
                append("CAUSED BY\n")
                append("=".repeat(70)).append("\n")
                append("Type    : ${cause.javaClass.name}\n")
                append("Message : ${cause.message}\n")
                append("\nStack Trace:\n")
                append(cause.stackTraceToString())
                append("\n")
                cause = cause.cause
                depth++
            }

            // Если AppLogger работает — добавляем последние строки буфера прямо в crash report
            val appLogger = AppLogger.getInstance()
            if (appLogger != null) {
                append("\n")
                append("=".repeat(70)).append("\n")
                append("📡 SESSION LOG TAIL (последние строки перед крэшем)\n")
                append("=".repeat(70)).append("\n")
                val snapshot = appLogger.getBufferSnapshot()
                val tail = snapshot.lines().takeLast(100).joinToString("\n")
                append(tail)
                append("\n")
                append("[полный лог сохранён в отдельный файл session_log_${timestamp}.txt]\n")
            }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Сбрасываем полный сеансовый лог
    // ─────────────────────────────────────────────────────────────────────────

    private fun dumpSessionLog(timestamp: String) {
        val appLogger = AppLogger.getInstance() ?: return
        val sessionFile = File(logDirectory, "${AppLogger.SESSION_PREFIX}${timestamp}.txt")
        try {
            appLogger.dumpToFile(sessionFile)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to dump session log", e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public helpers
    // ─────────────────────────────────────────────────────────────────────────

    fun startLogging() {
        android.util.Log.i(TAG, "📁 Logs: ${logDirectory.absolutePath}")
    }

    fun saveLogCatErrors(): File? {
        val appLogger = AppLogger.getInstance() ?: return null
        val ts = timestamp()
        val file = File(logDirectory, "${LOGCAT_PREFIX}${ts}.txt")
        return runCatching {
            val snapshot = appLogger.getBufferSnapshot()
            if (snapshot.isBlank()) return null
            file.writeText(buildString {
                append("=".repeat(70)).append("\n")
                append("📋 APP LOG SNAPSHOT — $ts\n")
                append("=".repeat(70)).append("\n\n")
                append(snapshot)
            })
            file
        }.getOrNull()
    }

    fun getAllLogs(): List<LogFile> {
        return logDirectory.listFiles()?.mapNotNull { file ->
            when {
                file.name.startsWith(CRASH_PREFIX)            -> LogFile(file, LogType.CRASH,   file.lastModified())
                file.name.startsWith(LOGCAT_PREFIX)           -> LogFile(file, LogType.LOGCAT,  file.lastModified())
                file.name.startsWith(AppLogger.SESSION_PREFIX) -> LogFile(file, LogType.SESSION, file.lastModified())
                else -> null
            }
        }?.sortedByDescending { it.timestamp } ?: emptyList()
    }

    fun getLatestCrashLog(): File? = getAllLogs().firstOrNull { it.type == LogType.CRASH }?.file

    fun cleanOldLogs(keepCount: Int = 20) {
        val all = getAllLogs()
        if (all.size > keepCount) {
            all.drop(keepCount).forEach { it.file.delete() }
        }
    }

    fun getStats(): LogStats {
        val logs = getAllLogs()
        return LogStats(
            totalCrashes  = logs.count { it.type == LogType.CRASH },
            totalLogCats  = logs.count { it.type == LogType.LOGCAT },
            totalSessions = logs.count { it.type == LogType.SESSION },
            totalSizeBytes = logs.sumOf { it.file.length() },
            location = logDirectory.absolutePath,
        )
    }

    fun getCrashLogDirectory(): String = logDirectory.absolutePath

    // ─────────────────────────────────────────────────────────────────────────

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
}

// ── Data classes ──────────────────────────────────────────────────────────────

data class LogFile(val file: File, val type: LogType, val timestamp: Long) {
    val name: String         get() = file.name
    val sizeKB: Long         get() = file.length() / 1024
    val formattedDate: String get() =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

enum class LogType { CRASH, LOGCAT, SESSION }

data class LogStats(
    val totalCrashes:  Int,
    val totalLogCats:  Int,
    val totalSessions: Int,
    val totalSizeBytes: Long,
    val location: String,
) {
    val totalSizeKB: Long get() = totalSizeBytes / 1024
}
