package com.voicedeutsch.master.util

import android.content.Context
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 📡 AppLogger — фоновая система логирования в реальном времени.
 *
 * Принципы работы:
 *  1. При старте запускает фоновый coroutine, который читает `logcat` потоком.
 *  2. Все строки складываются в кольцевой буфер (по умолчанию 5 000 строк).
 *  3. При крэше CrashLogger вызывает [dumpToFile] — буфер сбрасывается на диск автоматически.
 *  4. Пользователь вручную нажимает "Сохранить лог" → открывается SAF-пикер → вызывается [saveToUri].
 *
 * Инициализация: вызови [start] из Application.onCreate() ПОСЛЕ CrashLogger.init().
 */
class AppLogger private constructor(
    private val context: Context,
) {

    // ── Кольцевой буфер ──────────────────────────────────────────────────────
    private val buffer = ArrayDeque<String>(BUFFER_CAPACITY + 1)
    private val lock = Any()

    // ── Фоновая работа ───────────────────────────────────────────────────────
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var logcatJob: Job? = null
    private var logcatProcess: Process? = null

    // ── Статус ───────────────────────────────────────────────────────────────
    @Volatile var isRunning: Boolean = false
        private set

    // ── Директория для авто-дампов ────────────────────────────────────────────
    private val logDir: File by lazy {
        File(context.filesDir, "crash_logs").also { it.mkdirs() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Запускает перехват logcat в фоновом потоке.
     * Безопасно вызывать повторно — повторный старт игнорируется.
     */
    fun start() {
        if (isRunning) return
        isRunning = true

        // Добавляем заголовок сессии в буфер
        val sessionHeader = buildString {
            val ts = fmt.format(Date())
            append("=" .repeat(70)).append("\n")
            append("📱 SESSION START — $ts\n")
            append("Device  : ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Android : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            try {
                val ver = context.packageManager
                    .getPackageInfo(context.packageName, 0).versionName
                append("App     : $ver\n")
            } catch (_: Exception) { append("App     : unknown\n") }
            append("=".repeat(70)).append("\n")
        }
        appendToBuffer(sessionHeader)

        logcatJob = scope.launch {
            readLogcatContinuously()
        }
    }

    /**
     * Останавливает перехват (вызывать необязательно — живёт весь жизненный цикл).
     */
    fun stop() {
        logcatJob?.cancel()
        logcatProcess?.destroy()
        logcatProcess = null
        isRunning = false
    }

    /**
     * Сохраняет буфер в URI, выбранный пользователем через SAF-пикер.
     * Вызывать из UI (после того как пользователь выбрал файл через
     * ActivityResultContracts.CreateDocument).
     *
     * @return true если успешно
     */
    fun saveToUri(uri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(getBufferSnapshot().toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ saveToUri failed", e)
            false
        }
    }

    /**
     * Сохраняет буфер в файл внутри приватного хранилища приложения.
     * Вызывается автоматически при крэше из CrashLogger.
     *
     * @return сохранённый файл или null при ошибке
     */
    fun dumpToFile(targetFile: File? = null): File? {
        return try {
            val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val file = targetFile ?: File(logDir, "${SESSION_PREFIX}${ts}.txt")
            file.writeText(getBufferSnapshot(), Charsets.UTF_8)
            android.util.Log.i(TAG, "✅ Log dumped → ${file.absolutePath}")
            file
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ dumpToFile failed", e)
            null
        }
    }

    /**
     * Мгновенный снимок текущего буфера (thread-safe).
     */
    fun getBufferSnapshot(): String {
        return synchronized(lock) {
            buffer.joinToString("\n")
        }
    }

    /**
     * Количество строк в буфере (для отображения в UI).
     */
    fun lineCount(): Int = synchronized(lock) { buffer.size }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun appendToBuffer(line: String) {
        synchronized(lock) {
            if (buffer.size >= BUFFER_CAPACITY) {
                buffer.removeFirst()
            }
            buffer.addLast(line)
        }
    }

    /**
     * Бесконечный цикл чтения logcat.
     * При разрыве процесса (например, OOM Killer убил logcat) — перезапускается.
     */
    private suspend fun readLogcatContinuously() {
        while (isRunning && scope.isActive) {
            var reader: BufferedReader? = null
            try {
                // Запускаем logcat: только этот процесс, с фильтрацией по PID
                val pid = android.os.Process.myPid().toString()
                val cmd = arrayOf(
                    "logcat",
                    "-v", "time",          // формат: дата/время тег
                    "--pid=$pid",           // только наш процесс
                    "*:V",                  // все теги, все уровни
                )
                val process = Runtime.getRuntime().exec(cmd)
                logcatProcess = process

                reader = BufferedReader(InputStreamReader(process.inputStream), READER_BUFFER_SIZE)

                var line: String?
                while (scope.isActive) {
                    line = reader.readLine()
                    if (line == null) break // процесс завершился
                    appendToBuffer(line)
                }
            } catch (e: Exception) {
                if (isRunning) {
                    appendToBuffer("⚠️ [AppLogger] logcat reader error: ${e.message}")
                    android.util.Log.w(TAG, "logcat reader error, will retry", e)
                    kotlinx.coroutines.delay(RESTART_DELAY_MS)
                }
            } finally {
                reader?.runCatching { close() }
                logcatProcess?.runCatching { destroy() }
                logcatProcess = null
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Singleton
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        private const val TAG             = "AppLogger"
        private const val BUFFER_CAPACITY = 5_000   // строк в памяти
        private const val READER_BUFFER_SIZE = 8 * 1024
        private const val RESTART_DELAY_MS = 3_000L
        const val SESSION_PREFIX          = "session_log_"

        private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        @Volatile
        private var instance: AppLogger? = null

        /**
         * Инициализирует и возвращает singleton.
         * Идемпотентен — повторные вызовы возвращают уже существующий экземпляр.
         */
        fun init(context: Context): AppLogger {
            return instance ?: synchronized(this) {
                instance ?: AppLogger(context.applicationContext).also {
                    instance = it
                }
            }
        }

        fun getInstance(): AppLogger? = instance
    }
}
