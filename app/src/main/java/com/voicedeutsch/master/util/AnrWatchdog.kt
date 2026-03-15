package com.voicedeutsch.master.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🔍 ANR Watchdog — ловит зависания main thread.
 *
 * Как работает:
 *  1. При вызове [startWatching] ставит "тик" на main thread.
 *  2. Фоновый поток ждёт [timeoutMs].
 *  3. Если "тик" не прошёл за это время → main thread заблокирован →
 *     дампит стек-трейсы ВСЕХ потоков в файл и (опционально) крашит приложение.
 *
 * Использование:
 *   AnrWatchdog.startWatching(context, timeoutMs = 7000, crashAfterDump = true)
 */
object AnrWatchdog {

    private const val TAG = "AnrWatchdog"

    @Volatile
    private var tickCompleted = false

    @Volatile
    private var isWatching = false

    /**
     * Запускает наблюдение за main thread.
     *
     * @param context        для сохранения файла
     * @param timeoutMs      через сколько мс считать зависанием (по умолчанию 7000)
     * @param crashAfterDump если true — после дампа бросает RuntimeException (CrashLogger его поймает)
     */
    fun startWatching(
        context: android.content.Context,
        timeoutMs: Long = 7_000L,
        crashAfterDump: Boolean = true,
    ) {
        if (isWatching) {
            Log.w(TAG, "Already watching, ignoring duplicate call")
            return
        }
        isWatching = true
        tickCompleted = false

        Log.i(TAG, "⏱ ANR Watchdog started (timeout=${timeoutMs}ms, crash=$crashAfterDump)")

        // Ставим "тик" на main thread — он выполнится когда main thread свободен
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            tickCompleted = true
            Log.d(TAG, "✅ Main thread tick completed — no ANR")
        }

        // Фоновый поток ждёт и проверяет
        val watchThread = Thread({
            try {
                Thread.sleep(timeoutMs)
            } catch (_: InterruptedException) {
                isWatching = false
                return@Thread
            }

            if (tickCompleted) {
                Log.i(TAG, "✅ No ANR detected — main thread responded in time")
                isWatching = false
                return@Thread
            }

            // ⚠️ Main thread не ответил за timeoutMs — ANR!
            Log.e(TAG, "🔥 ANR DETECTED! Main thread blocked for >${timeoutMs}ms!")

            val dump = buildFullThreadDump()
            val file = saveDump(context, dump)

            Log.e(TAG, "📁 Thread dump saved: ${file?.absolutePath ?: "FAILED"}")

            // Печатаем в logcat тоже (первые 200 строк)
            dump.lines().take(200).forEach { Log.e(TAG, it) }

            isWatching = false

            if (crashAfterDump) {
                // Бросаем на main thread — CrashLogger перехватит
                mainHandler.post {
                    throw RuntimeException(
                        "🔥 ANR Watchdog: Main thread blocked >${timeoutMs}ms!\n" +
                        "Thread dump saved to: ${file?.absolutePath ?: "unknown"}\n" +
                        "See crash_logs/ for full details."
                    )
                }
            }
        }, "ANR-Watchdog-Thread")

        watchThread.isDaemon = true
        watchThread.start()
    }

    /**
     * Отменяет наблюдение (например, если сессия завершилась нормально).
     */
    fun cancel() {
        isWatching = false
        tickCompleted = true // предотвращаем ложное срабатывание
        Log.d(TAG, "Watchdog cancelled")
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun buildFullThreadDump(): String = buildString {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())

        append("=".repeat(80)).append("\n")
        append("🔥 ANR THREAD DUMP — $ts\n")
        append("=".repeat(80)).append("\n\n")

        // Сначала main thread — он самый важный
        val mainThread = Looper.getMainLooper().thread
        append("━".repeat(80)).append("\n")
        append("⚡ MAIN THREAD (это то, что заблокировано!):\n")
        append("━".repeat(80)).append("\n")
        appendThreadInfo(mainThread)
        append("\n")

        // Затем все остальные потоки
        val allThreads = Thread.getAllStackTraces()
        val sortedThreads = allThreads.entries
            .filter { it.key != mainThread }
            .sortedBy { it.key.name }

        append("━".repeat(80)).append("\n")
        append("📋 ALL OTHER THREADS (${sortedThreads.size}):\n")
        append("━".repeat(80)).append("\n\n")

        for ((thread, _) in sortedThreads) {
            appendThreadInfo(thread)
            append("\n")
        }

        // Дополнительная инфа
        append("━".repeat(80)).append("\n")
        append("📊 MEMORY INFO:\n")
        append("━".repeat(80)).append("\n")
        val runtime = Runtime.getRuntime()
        val usedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val totalMB = runtime.totalMemory() / (1024 * 1024)
        val maxMB = runtime.maxMemory() / (1024 * 1024)
        append("Used:  ${usedMB} MB\n")
        append("Total: ${totalMB} MB\n")
        append("Max:   ${maxMB} MB\n")
        append("Threads: ${allThreads.size}\n")
    }

    private fun StringBuilder.appendThreadInfo(thread: Thread) {
        val traces = thread.stackTrace
        append("\"${thread.name}\" [${thread.state}] prio=${thread.priority} id=${thread.id}\n")
        if (traces.isEmpty()) {
            append("    (no stack trace)\n")
        } else {
            for (frame in traces) {
                append("    at ${frame.className}.${frame.methodName}")
                if (frame.fileName != null) {
                    append("(${frame.fileName}:${frame.lineNumber})")
                }
                append("\n")
            }
        }
    }

    private fun saveDump(context: android.content.Context, dump: String): File? {
        return try {
            val dir = File(context.filesDir, "crash_logs").also { it.mkdirs() }
            val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val file = File(dir, "anr_dump_${ts}.txt")
            file.writeText(dump, Charsets.UTF_8)
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save dump", e)
            null
        }
    }
}
