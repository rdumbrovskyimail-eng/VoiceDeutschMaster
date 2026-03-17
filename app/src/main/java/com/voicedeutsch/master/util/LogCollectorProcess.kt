package com.voicedeutsch.master.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 📡 LogCollectorProcess — сбор логов через отдельный UNIX-процесс (shell script).
 *
 * Почему НЕ Android Service:
 *   • Service с android:process создаёт полный Android-процесс со ВСЕМИ ContentProvider'ами
 *     (Firebase, WorkManager, androidx.startup) — они конкурируют за SQLite/SharedPreferences
 *     с основным процессом → deadlock при активной работе (голосовые сессии и т.д.).
 *   • Shell-скрипт через Runtime.exec() — чистый UNIX fork, без ContentProvider'ов,
 *     без Notification, без permissions. Максимально лёгкий.
 *
 * Как работает:
 *   1. [start] запускает shell-скрипт как отдельный процесс.
 *   2. Shell-скрипт запускает `logcat -f <file>` — logcat пишет ПРЯМО в файл (реальное время).
 *   3. Каждые 2 сек проверяет жив ли основной процесс через /proc/<pid>.
 *   4. Если процесс умер — дочитывает crash buffer из `logcat -b crash`.
 *   5. Через 60 сек — убивает logcat, ждёт 5 сек, завершается.
 *
 * При краше основного процесса:
 *   • Shell-скрипт НЕ умирает — он отдельный UNIX-процесс.
 *   • logcat --pid перестаёт получать новые строки (PID мёртв).
 *   • НО файл уже содержит ВСЕ логи до момента краша (logcat -f пишет в реальном времени).
 *   • Скрипт дочитывает crash buffer и сохраняет в тот же файл.
 *   • Ждёт остаток от 60 сек → завершается.
 *
 * Путь: /data/user/0/com.voicedeutsch.master/files/crash_logs/collected_log_<timestamp>.txt
 */
object LogCollectorProcess {

    private const val TAG = "LogCollectorProc"
    private const val FILE_PREFIX = "collected_log_"

    /** Сколько секунд собирать логи */
    private const val COLLECT_SECONDS = 60

    /** Сколько секунд ждать после сохранения */
    private const val SHUTDOWN_SECONDS = 5

    /** Интервал проверки жив ли процесс (сек) */
    private const val CHECK_INTERVAL = 2

    @Volatile
    private var shellProcess: Process? = null

    /**
     * Запускает сбор логов.
     * Вызывать из Application.onCreate() в основном процессе.
     *
     * Безопасно вызывать повторно — предыдущий процесс будет убит.
     */
    fun start(context: Context) {
        // Убиваем предыдущий если был
        stop()

        val pid = android.os.Process.myPid()
        val logDir = File(context.filesDir, "crash_logs").also { it.mkdirs() }
        val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val logFile = File(logDir, "${FILE_PREFIX}${ts}.txt")

        val script = buildScript(pid, logFile.absolutePath)

        try {
            shellProcess = Runtime.getRuntime().exec(arrayOf("sh", "-c", script))
            Log.i(TAG, "✅ Log collector started (PID=$pid, file=${logFile.name})")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start log collector", e)
        }
    }

    /**
     * Останавливает сбор логов (при нормальном выходе).
     */
    fun stop() {
        shellProcess?.let {
            try {
                it.destroy()
            } catch (_: Exception) {}
        }
        shellProcess = null
    }

    /**
     * Собирает shell-скрипт.
     *
     * Логика:
     *   1. Записывает заголовок сессии в файл
     *   2. Запускает logcat -f (пишет прямо в файл в реальном времени)
     *   3. В цикле проверяет жив ли основной процесс
     *   4. Если процесс умер — дочитывает crash buffer
     *   5. Через 60 сек — убивает logcat
     *   6. Записывает footer
     *   7. Ждёт 5 сек, завершается
     */
    private fun buildScript(mainPid: Int, logFilePath: String): String {
        // Используем heredoc-стиль для читаемости
        return """
            # ═══════════════════════════════════════════════════════════════
            # LogCollector shell script
            # Main PID: $mainPid
            # Output:   $logFilePath
            # ═══════════════════════════════════════════════════════════════

            LOG_FILE="$logFilePath"
            MAIN_PID=$mainPid
            COLLECT_SEC=$COLLECT_SECONDS
            CHECK_SEC=$CHECK_INTERVAL

            # ── Заголовок ─────────────────────────────────────────────────
            echo "======================================================================" > "${'$'}LOG_FILE"
            echo "LOG COLLECTOR SESSION — $(date '+%Y-%m-%d %H:%M:%S')" >> "${'$'}LOG_FILE"
            echo "======================================================================" >> "${'$'}LOG_FILE"
            echo "Main PID  : ${'$'}MAIN_PID" >> "${'$'}LOG_FILE"
            echo "Shell PID : ${'$'}${'$'}" >> "${'$'}LOG_FILE"
            echo "Collect   : ${'$'}{COLLECT_SEC}s" >> "${'$'}LOG_FILE"
            echo "======================================================================" >> "${'$'}LOG_FILE"
            echo "" >> "${'$'}LOG_FILE"

            # ── Запускаем logcat — пишет ПРЯМО в файл в реальном времени ──
            logcat -v threadtime --pid=${'$'}MAIN_PID >> "${'$'}LOG_FILE" 2>&1 &
            LOGCAT_PID=${'$'}!

            # ── Мониторим основной процесс ────────────────────────────────
            ELAPSED=0
            CRASHED=0

            while [ ${'$'}ELAPSED -lt ${'$'}COLLECT_SEC ]; do
                sleep ${'$'}CHECK_SEC
                ELAPSED=${'$'}((ELAPSED + CHECK_SEC))

                # Проверяем жив ли основной процесс
                if [ ! -d "/proc/${'$'}MAIN_PID" ] && [ ${'$'}CRASHED -eq 0 ]; then
                    CRASHED=1
                    echo "" >> "${'$'}LOG_FILE"
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" >> "${'$'}LOG_FILE"
                    echo "MAIN PROCESS (PID=${'$'}MAIN_PID) DIED after ${'$'}{ELAPSED}s" >> "${'$'}LOG_FILE"
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" >> "${'$'}LOG_FILE"
                    echo "" >> "${'$'}LOG_FILE"

                    # Дочитываем crash buffer
                    echo "======================================================================" >> "${'$'}LOG_FILE"
                    echo "CRASH BUFFER (logcat -b crash):" >> "${'$'}LOG_FILE"
                    echo "======================================================================" >> "${'$'}LOG_FILE"
                    logcat -d -b crash -v threadtime >> "${'$'}LOG_FILE" 2>&1
                    echo "======================================================================" >> "${'$'}LOG_FILE"
                    echo "" >> "${'$'}LOG_FILE"
                fi
            done

            # ── Останавливаем logcat ──────────────────────────────────────
            kill ${'$'}LOGCAT_PID 2>/dev/null

            # ── Footer ────────────────────────────────────────────────────
            echo "" >> "${'$'}LOG_FILE"
            echo "======================================================================" >> "${'$'}LOG_FILE"
            echo "COLLECTION COMPLETE — $(date '+%Y-%m-%d %H:%M:%S')" >> "${'$'}LOG_FILE"
            echo "Duration : ${'$'}{ELAPSED}s" >> "${'$'}LOG_FILE"
            echo "Crashed  : ${'$'}CRASHED" >> "${'$'}LOG_FILE"
            echo "======================================================================" >> "${'$'}LOG_FILE"

            # ── Ждём 5 сек и выходим ─────────────────────────────────────
            sleep $SHUTDOWN_SECONDS
        """.trimIndent()
    }
}
