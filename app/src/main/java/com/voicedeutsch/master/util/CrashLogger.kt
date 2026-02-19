package com.voicedeutsch.master.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🔥 CrashLogger - Автоматический перехват крашей + LogCat сохранение
 *
 * Логи сохраняются в: Download/LOG5/
 * Используется MediaStore API — работает БЕЗ разрешений,
 * файлы видны в ЛЮБОМ файловом менеджере.
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
        private const val LOG_FOLDER = "LOG5"
    }

    // Internal backup directory (всегда работает)
    private val internalLogDir: File by lazy {
        File(context.filesDir, LOG_FOLDER).apply { mkdirs() }
    }

    /**
     * 🔥 Устанавливает обработчик необработанных исключений
     */
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
        android.util.Log.i("CrashLogger", "📁 Internal logs: ${internalLogDir.absolutePath}")
        android.util.Log.i("CrashLogger", "📁 Download logs: Download/$LOG_FOLDER/")
    }

    /**
     * 💥 Сохраняет краш-лог при падении приложения
     */
    private fun saveCrashLog(throwable: Throwable, thread: Thread) {
        val timestamp = SimpleDateFormat(
            "yyyy-MM-dd_HH-mm-ss", Locale.getDefault()
        ).format(Date())
        val fileName = "${CRASH_PREFIX}${timestamp}.txt"

        val appVersion = try {
            context.packageManager
                .getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) {
            "unknown"
        }

        val content = buildString {
            appendLine("=" * 80)
            appendLine("🔥 CRASH REPORT - VoiceDeutschMaster")
            appendLine("=" * 80)
            appendLine()
            appendLine("Timestamp: $timestamp")
            appendLine("Thread: ${thread.name}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("App Version: $appVersion")
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
                val process = Runtime.getRuntime()
                    .exec(arrayOf("logcat", "-d", "-t", "500"))
                process.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { appendLine(it) }
                }
            } catch (e: Exception) {
                appendLine("❌ Failed to capture logcat: ${e.message}")
            }

            appendLine("-" * 80)
            appendLine("END OF CRASH REPORT")
            appendLine("=" * 80)
        }

        // 1. Сохраняем в internal storage (100% надёжно)
        try {
            File(internalLogDir, fileName).writeText(content)
            android.util.Log.e("CrashLogger", "✅ Crash saved to internal: ${internalLogDir.absolutePath}/$fileName")
        } catch (e: Exception) {
            android.util.Log.e("CrashLogger", "❌ Failed to write internal crash log", e)
        }

        // 2. Сохраняем в Download/LOG5 через MediaStore (виден в файловых менеджерах)
        try {
            writeToDownloads(fileName, content)
            android.util.Log.e("CrashLogger", "✅ Crash saved to Download/$LOG_FOLDER/$fileName")
        } catch (e: Exception) {
            android.util.Log.e("CrashLogger", "❌ Failed to write Download crash log", e)
        }
    }

    /**
     * 📝 Сохраняет текущий LogCat (ТОЛЬКО ОШИБКИ)
     */
    fun saveLogCatErrors(): File? {
        val timestamp = SimpleDateFormat(
            "yyyy-MM-dd_HH-mm-ss", Locale.getDefault()
        ).format(Date())
        val fileName = "${LOGCAT_PREFIX}${timestamp}.txt"

        val content = buildString {
            appendLine("=" * 80)
            appendLine("📋 LOGCAT ERRORS - VoiceDeutschMaster")
            appendLine("=" * 80)
            appendLine()
            appendLine("Timestamp: $timestamp")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine()
            appendLine("-" * 80)
            appendLine("ERRORS & WARNINGS:")
            appendLine("-" * 80)

            val process = Runtime.getRuntime()
                .exec(arrayOf("logcat", "-d", "-s", "E:*", "W:*"))
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
        }

        return try {
            val file = File(internalLogDir, fileName)
            file.writeText(content)

            // Также в Download/LOG5
            try {
                writeToDownloads(fileName, content)
            } catch (_: Exception) { }

            android.util.Log.i("CrashLogger", "✅ LogCat errors saved: $fileName")
            file
        } catch (e: Exception) {
            android.util.Log.e("CrashLogger", "❌ Failed to save logcat", e)
            null
        }
    }

    /**
     * 📥 Запись файла в Download/LOG5 через MediaStore
     * Работает на Android 10+ БЕЗ разрешений
     */
    private fun writeToDownloads(fileName: String, content: String) {
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$LOG_FOLDER")
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("MediaStore insert returned null")

        resolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
            outputStream.write(content.toByteArray())
            outputStream.flush()
        } ?: throw Exception("Failed to open output stream")
    }

    /**
     * 📋 Получить список ВСЕХ логов из internal storage
     */
    fun getAllLogs(): List<LogFile> {
        return internalLogDir.listFiles()?.mapNotNull { file ->
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

    /**
     * 📄 Получить последний краш-лог
     */
    fun getLatestCrashLog(): File? {
        return getAllLogs()
            .firstOrNull { it.type == LogType.CRASH }
            ?.file
    }

    /**
     * 🗑️ Очистить старые логи
     */
    fun cleanOldLogs(keepCount: Int = 20) {
        val allLogs = getAllLogs()
        if (allLogs.size > keepCount) {
            allLogs.drop(keepCount).forEach { logFile ->
                logFile.file.delete()
                android.util.Log.d("CrashLogger", "🗑️ Deleted old log: ${logFile.file.name}")
            }
        }
    }

    /**
     * 📊 Статистика логов
     */
    fun getStats(): LogStats {
        val logs = getAllLogs()
        return LogStats(
            totalCrashes = logs.count { it.type == LogType.CRASH },
            totalLogCats = logs.count { it.type == LogType.LOGCAT },
            totalSizeBytes = logs.sumOf { it.file.length() },
            location = "Download/$LOG_FOLDER/ + ${internalLogDir.absolutePath}"
        )
    }

    fun getCrashLogDirectory(): String = "Download/$LOG_FOLDER/"

    fun startLogging() {
        android.util.Log.i("CrashLogger", "📁 Logs: Download/$LOG_FOLDER/ + ${internalLogDir.absolutePath}")
    }
}

// ═══════════════════════════════════════════════════════════════════════════════

data class LogFile(
    val file: File,
    val type: LogType,
    val timestamp: Long,
) {
    val name: String get() = file.name
    val sizeKB: Long get() = file.length() / 1024
    val formattedDate: String
        get() = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
        ).format(Date(timestamp))
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
