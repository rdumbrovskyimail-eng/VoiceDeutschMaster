package com.voicedeutsch.master.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 📡 LogCollectorService — сервис-коллектор логов в ОТДЕЛЬНОМ ПРОЦЕССЕ.
 *
 * Архитектура:
 *   • Запускается в android:process=":logcollector" — отдельный процесс ОС.
 *   • Получает PID основного процесса через Intent extra.
 *   • Читает logcat основного процесса в фоновом потоке.
 *   • Через [COLLECT_DURATION_MS] (60 сек) — сохраняет лог в файл.
 *   • Через [SHUTDOWN_DELAY_MS] (5 сек) после сохранения — останавливается.
 *
 * Путь сохранения: /data/user/0/com.voicedeutsch.master/files/crash_logs/
 * (filesDir привязан к пакету, не к процессу — оба процесса видят одну папку)
 *
 * Главный сценарий:
 *   1. Приложение стартует → VoiceDeutschApp запускает LogCollectorService с PID.
 *   2. Сервис начинает читать logcat основного процесса.
 *   3. Через 60 секунд — сохраняет файл в crash_logs/.
 *   4. Если основной процесс крашится на 40-й секунде:
 *      — logcat перестаёт выдавать новые строки (процесс мёртв),
 *      — НО сервис жив (отдельный процесс!),
 *      — дочитывает crash buffer,
 *      — ждёт до 60-й секунды,
 *      — сохраняет файл в crash_logs/,
 *      — через 5 секунд выключается.
 *   5. При следующем старте приложения — запускается новый экземпляр сервиса.
 */
class LogCollectorService : Service() {

    companion object {
        private const val TAG = "LogCollectorSvc"

        /** Ключ для передачи PID основного процесса */
        const val EXTRA_MAIN_PID = "extra_main_pid"

        /** Ключ для передачи имени пакета (для логирования) */
        const val EXTRA_PACKAGE_NAME = "extra_package_name"

        /** Сколько миллисекунд собирать логи */
        private const val COLLECT_DURATION_MS = 60_000L  // 60 секунд

        /** Сколько ждать после сохранения перед выключением */
        private const val SHUTDOWN_DELAY_MS = 5_000L     // 5 секунд

        /** Префикс файлов */
        private const val FILE_PREFIX = "collected_log_"

        /** Размер буфера чтения */
        private const val READER_BUFFER_SIZE = 16 * 1024

        /** Интервал проверки жив ли основной процесс */
        private const val PROCESS_CHECK_INTERVAL_MS = 2_000L

        /**
         * Удобный метод запуска сервиса.
         * Вызывать из основного процесса (VoiceDeutschApp.onCreate).
         */
        fun start(context: Context) {
            val intent = Intent(context, LogCollectorService::class.java).apply {
                putExtra(EXTRA_MAIN_PID, android.os.Process.myPid())
                putExtra(EXTRA_PACKAGE_NAME, context.packageName)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.i(TAG, "✅ LogCollectorService start requested")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start LogCollectorService", e)
            }
        }

        /**
         * Останавливает сервис (например, при нормальном выходе из приложения).
         */
        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, LogCollectorService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop service", e)
            }
        }
    }

    // ── Состояние ────────────────────────────────────────────────────────────

    private var mainPid: Int = -1
    private var appPackageName: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private val logBuffer = StringBuilder(512 * 1024) // ~512 KB начальная ёмкость
    private val bufferLock = Any()

    @Volatile
    private var isCollecting = false

    @Volatile
    private var mainProcessDead = false

    private var logcatProcess: Process? = null
    private var readerThread: Thread? = null
    private var processMonitorThread: Thread? = null

    private var collectionStartTime: Long = 0L

    // ─────────────────────────────────────────────────────────────────────────
    // Service lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "📡 LogCollectorService created (PID=${android.os.Process.myPid()})")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Запускаем foreground notification СРАЗУ — Android требует в течение 5 сек
        val notification = LogCollectorNotification.create(this)
        startForeground(LogCollectorNotification.NOTIFICATION_ID, notification)

        // Получаем PID основного процесса
        mainPid = intent?.getIntExtra(EXTRA_MAIN_PID, -1) ?: -1
        appPackageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: packageName

        if (mainPid == -1) {
            Log.e(TAG, "❌ No main PID provided, stopping")
            stopSelfDelayed(1000)
            return START_NOT_STICKY
        }

        Log.i(TAG, "🚀 Starting log collection for main PID=$mainPid")

        // Если уже собираем — перезапускаем (новый старт приложения после краша)
        if (isCollecting) {
            Log.w(TAG, "⚠️ Already collecting, restarting for new PID")
            stopCollection()
        }

        startCollection()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "🛑 LogCollectorService destroyed")
        stopCollection()
        super.onDestroy()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Collection logic
    // ─────────────────────────────────────────────────────────────────────────

    private fun startCollection() {
        isCollecting = true
        mainProcessDead = false
        collectionStartTime = System.currentTimeMillis()

        // Очищаем буфер
        synchronized(bufferLock) {
            logBuffer.setLength(0)
        }

        // Записываем заголовок сессии
        writeHeader()

        // Запускаем чтение logcat в отдельном потоке
        readerThread = Thread({
            readLogcat()
        }, "LogCollector-Reader").also {
            it.isDaemon = true
            it.start()
        }

        // Запускаем мониторинг основного процесса
        processMonitorThread = Thread({
            monitorMainProcess()
        }, "LogCollector-Monitor").also {
            it.isDaemon = true
            it.start()
        }

        // Таймер на 60 секунд — сохранить и выключиться
        handler.postDelayed({
            Log.i(TAG, "⏰ Collection time elapsed (${COLLECT_DURATION_MS}ms)")
            saveAndShutdown()
        }, COLLECT_DURATION_MS)
    }

    private fun stopCollection() {
        isCollecting = false
        handler.removeCallbacksAndMessages(null)

        logcatProcess?.let {
            try { it.destroy() } catch (_: Exception) {}
        }
        logcatProcess = null

        readerThread?.interrupt()
        readerThread = null

        processMonitorThread?.interrupt()
        processMonitorThread = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Logcat reading (runs on background thread)
    // ─────────────────────────────────────────────────────────────────────────

    private fun readLogcat() {
        var reader: BufferedReader? = null
        try {
            // logcat --pid фильтрует по PID основного процесса
            // -v threadtime — подробный формат с датой, временем, PID, TID, тегом
            val cmd = arrayOf(
                "logcat",
                "-v", "threadtime",
                "--pid=$mainPid",
                "*:V",
            )
            val process = Runtime.getRuntime().exec(cmd)
            logcatProcess = process

            reader = BufferedReader(
                InputStreamReader(process.inputStream),
                READER_BUFFER_SIZE
            )

            while (isCollecting) {
                val line = reader.readLine()
                if (line == null) {
                    // Logcat stream закрылся — основной процесс скорее всего мёртв
                    if (isCollecting) {
                        bufferAppend("⚠️ [LogCollector] logcat stream ended (main process likely dead)")
                        mainProcessDead = true
                    }
                    break
                }
                bufferAppend(line)
            }
        } catch (e: Exception) {
            if (isCollecting) {
                bufferAppend("⚠️ [LogCollector] reader error: ${e.message}")
                Log.w(TAG, "Logcat reader error", e)
            }
        } finally {
            reader?.runCatching { close() }
            logcatProcess?.runCatching { destroy() }
            logcatProcess = null
        }

        // Если основной процесс умер и logcat закончился — дочитываем crash buffer
        if (mainProcessDead && isCollecting) {
            readCrashBuffer()
        }
    }

    /**
     * После смерти основного процесса — читаем logcat -b crash для финального стектрейса.
     */
    private fun readCrashBuffer() {
        try {
            bufferAppend("")
            bufferAppend("=".repeat(70))
            bufferAppend("🔥 MAIN PROCESS DIED — reading crash buffer...")
            bufferAppend("=".repeat(70))

            val process = Runtime.getRuntime().exec(
                arrayOf("logcat", "-d", "-b", "crash", "-v", "threadtime")
            )
            val lines = process.inputStream.bufferedReader().readLines()

            // Берём строки, относящиеся к PID основного процесса
            val relevant = lines.filter { it.contains(mainPid.toString()) }
            val toAppend = if (relevant.isNotEmpty()) relevant else lines.takeLast(100)

            toAppend.forEach { bufferAppend(it) }

            bufferAppend("=".repeat(70))
            bufferAppend("📋 Crash buffer: ${toAppend.size} lines captured")
            bufferAppend("=".repeat(70))
        } catch (e: Exception) {
            bufferAppend("⚠️ [LogCollector] Failed to read crash buffer: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Process monitoring (runs on background thread)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Периодически проверяет, жив ли основной процесс через /proc/[pid].
     * Если мёртв — помечаем это в логе, но НЕ останавливаемся (ждём таймер на 60 сек).
     */
    private fun monitorMainProcess() {
        try {
            while (isCollecting && !mainProcessDead) {
                Thread.sleep(PROCESS_CHECK_INTERVAL_MS)

                if (!isProcessAlive(mainPid)) {
                    mainProcessDead = true
                    val elapsed = System.currentTimeMillis() - collectionStartTime
                    bufferAppend("")
                    bufferAppend("━".repeat(70))
                    bufferAppend("💀 MAIN PROCESS (PID=$mainPid) DIED after ${elapsed}ms")
                    bufferAppend("━".repeat(70))
                    bufferAppend("📡 LogCollector will continue until ${COLLECT_DURATION_MS}ms mark, " +
                            "remaining: ${COLLECT_DURATION_MS - elapsed}ms")
                    bufferAppend("")

                    Log.w(TAG, "💀 Main process died at ${elapsed}ms, continuing collection...")

                    // Обновляем notification — показываем что краш обнаружен
                    try {
                        val notification = LogCollectorNotification.createCrashDetected(this)
                        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                        nm.notify(LogCollectorNotification.NOTIFICATION_ID, notification)
                    } catch (_: Exception) {}
                }
            }
        } catch (_: InterruptedException) {
            // Нормальное завершение
        }
    }

    /**
     * Проверяет, жив ли процесс по PID через /proc/[pid].
     * Когда процесс умирает, его директория в /proc исчезает.
     */
    private fun isProcessAlive(pid: Int): Boolean {
        return try {
            File("/proc/$pid").exists()
        } catch (_: Exception) {
            false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Buffer operations (thread-safe)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Thread-safe запись строки в буфер.
     * Имя bufferAppend чтобы не конфликтовать с StringBuilder.appendLine.
     */
    private fun bufferAppend(line: String) {
        synchronized(bufferLock) {
            logBuffer.append(line).append('\n')
        }
    }

    private fun writeHeader() {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        bufferAppend("=".repeat(70))
        bufferAppend("📡 LOG COLLECTOR SESSION — $ts")
        bufferAppend("=".repeat(70))
        bufferAppend("Collector PID : ${android.os.Process.myPid()}")
        bufferAppend("Main app PID  : $mainPid")
        bufferAppend("Package       : $appPackageName")
        bufferAppend("Device        : ${Build.MANUFACTURER} ${Build.MODEL}")
        bufferAppend("Android       : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        bufferAppend("Collect time  : ${COLLECT_DURATION_MS}ms")
        bufferAppend("=".repeat(70))
        bufferAppend("")
    }

    private fun getBufferContent(): String {
        return synchronized(bufferLock) {
            logBuffer.toString()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save & shutdown — файл сохраняется ТОЛЬКО в crash_logs/
    // ─────────────────────────────────────────────────────────────────────────

    private fun saveAndShutdown() {
        // Останавливаем сбор
        isCollecting = false
        logcatProcess?.runCatching { destroy() }

        // Добавляем footer
        val elapsed = System.currentTimeMillis() - collectionStartTime
        bufferAppend("")
        bufferAppend("=".repeat(70))
        bufferAppend("📊 COLLECTION COMPLETE")
        bufferAppend("=".repeat(70))
        bufferAppend("Duration           : ${elapsed}ms")
        bufferAppend("Main process died  : $mainProcessDead")
        bufferAppend("Saved at           : ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        bufferAppend("=".repeat(70))

        // Сохраняем файл ТОЛЬКО в crash_logs/
        val file = saveToFile()

        if (file != null) {
            Log.i(TAG, "✅ Log saved: ${file.absolutePath} (${file.length() / 1024} KB)")
        } else {
            Log.e(TAG, "❌ Failed to save log file")
        }

        // Через 5 секунд — выключаемся
        Log.i(TAG, "⏳ Shutting down in ${SHUTDOWN_DELAY_MS}ms...")
        stopSelfDelayed(SHUTDOWN_DELAY_MS)
    }

    /**
     * Сохраняет собранный лог в файл.
     * Путь: /data/user/0/com.voicedeutsch.master/files/crash_logs/collected_log_<timestamp>[_CRASH].txt
     *
     * filesDir одинаковый для обоих процессов (привязан к пакету, не к процессу).
     */
    private fun saveToFile(): File? {
        return try {
            val logDir = File(filesDir, "crash_logs").also { it.mkdirs() }
            val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val suffix = if (mainProcessDead) "_CRASH" else ""
            val file = File(logDir, "${FILE_PREFIX}${ts}${suffix}.txt")
            file.writeText(getBufferContent(), Charsets.UTF_8)
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save log to crash_logs/", e)
            null
        }
    }

    private fun stopSelfDelayed(delayMs: Long) {
        handler.postDelayed({
            Log.i(TAG, "🛑 LogCollectorService stopping self")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }, delayMs)
    }
}
