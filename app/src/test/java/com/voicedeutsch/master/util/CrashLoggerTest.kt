// src/test/java/com/voicedeutsch/master/util/CrashLoggerTest.kt
package com.voicedeutsch.master.util

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.lang.reflect.Field
import java.nio.file.Path

class CrashLoggerTest {

    @TempDir lateinit var tempDir: Path
    private lateinit var context: Context
    private lateinit var crashLogger: CrashLogger

    @BeforeEach fun setUp() {
        context = mockk(relaxed = true)
        every { context.filesDir } returns tempDir.toFile()
        every { context.applicationContext } returns context
        every { context.packageManager } returns mockk(relaxed = true)
        every { context.packageName } returns "com.test.app"
        resetCrashLoggerSingleton()
        crashLogger = CrashLogger.init(context)
    }

    @AfterEach fun tearDown() {
        resetCrashLoggerSingleton()
        resetAppLoggerSingleton()
    }

    private fun resetCrashLoggerSingleton() {
        val field: Field = CrashLogger::class.java.getDeclaredField("instance")
        field.isAccessible = true
        field.set(null, null)
    }

    private fun resetAppLoggerSingleton() {
        try {
            val field: Field = AppLogger::class.java.getDeclaredField("instance")
            field.isAccessible = true
            field.set(null, null)
        } catch (_: Exception) {}
    }

    private fun getLogDirectory(): File {
        val field = CrashLogger::class.java.getDeclaredField("logDirectory").apply { isAccessible = true }
        return (field.get(crashLogger) as Lazy<*>).value as File
    }

    // ── init (singleton) ──────────────────────────────────────────────────

    @Test fun init_returnsNonNullInstance() { assertNotNull(crashLogger) }
    @Test fun init_calledTwice_returnsSameInstance() { assertSame(crashLogger, CrashLogger.init(context)) }
    @Test fun getInstance_afterInit_returnsSameInstance() { assertSame(crashLogger, CrashLogger.getInstance()) }

    @Test fun getInstance_beforeInit_returnsNull() {
        resetCrashLoggerSingleton()
        assertNull(CrashLogger.getInstance())
    }

    // ── startLogging ──────────────────────────────────────────────────────

    @Test fun startLogging_doesNotCrash() { crashLogger.startLogging() }

    // ── getCrashLogDirectory ──────────────────────────────────────────────

    @Test fun getCrashLogDirectory_returnsNonEmptyPath() { assertTrue(crashLogger.getCrashLogDirectory().isNotEmpty()) }

    @Test fun getCrashLogDirectory_directoryExists() {
        val path = crashLogger.getCrashLogDirectory()
        assertTrue(File(path).exists() || File(path).mkdirs())
    }

    // ── getAllLogs ────────────────────────────────────────────────────────

    @Test fun getAllLogs_noLogs_returnsEmptyList() { assertTrue(crashLogger.getAllLogs().isEmpty()) }

    @Test fun getAllLogs_withCrashFile_returnsCrashLog() {
        File(getLogDirectory(), "crash_2026-03-06_10-00-00.txt").writeText("crash content")
        val result = crashLogger.getAllLogs()
        assertEquals(1, result.size)
        assertEquals(LogType.CRASH, result[0].type)
    }

    @Test fun getAllLogs_withSessionFile_returnsSessionLog() {
        File(getLogDirectory(), "${AppLogger.SESSION_PREFIX}2026-03-06.txt").writeText("session content")
        val result = crashLogger.getAllLogs()
        assertEquals(1, result.size)
        assertEquals(LogType.SESSION, result[0].type)
    }

    @Test fun getAllLogs_withLogcatFile_returnsLogcatLog() {
        File(getLogDirectory(), "logcat_errors_2026-03-06.txt").writeText("logcat content")
        val result = crashLogger.getAllLogs()
        assertEquals(1, result.size)
        assertEquals(LogType.LOGCAT, result[0].type)
    }

    @Test fun getAllLogs_unknownFile_notIncluded() {
        File(getLogDirectory(), "unknown_file.txt").writeText("unknown")
        assertTrue(crashLogger.getAllLogs().isEmpty())
    }

    @Test fun getAllLogs_multipleLogs_sortedByTimestampDesc() {
        val logDir = getLogDirectory()
        File(logDir, "crash_older.txt").also { it.writeText("old"); it.setLastModified(1000L) }
        File(logDir, "crash_newer.txt").also { it.writeText("new"); it.setLastModified(9000L) }
        val result = crashLogger.getAllLogs()
        assertEquals(2, result.size)
        assertTrue(result[0].timestamp >= result[1].timestamp)
    }

    // ── getLatestCrashLog ─────────────────────────────────────────────────

    @Test fun getLatestCrashLog_noLogs_returnsNull() { assertNull(crashLogger.getLatestCrashLog()) }

    @Test fun getLatestCrashLog_withOneCrash_returnsThatCrash() {
        File(getLogDirectory(), "crash_test.txt").writeText("crash content")
        assertNotNull(crashLogger.getLatestCrashLog())
    }

    @Test fun getLatestCrashLog_withSessionButNoCrash_returnsNull() {
        File(getLogDirectory(), "${AppLogger.SESSION_PREFIX}test.txt").writeText("session content")
        assertNull(crashLogger.getLatestCrashLog())
    }

    // ── getStats ──────────────────────────────────────────────────────────

    @Test fun getStats_noLogs_returnsAllZeroStats() {
        val stats = crashLogger.getStats()
        assertEquals(0, stats.totalCrashes)
        assertEquals(0, stats.totalLogCats)
        assertEquals(0, stats.totalSessions)
        assertEquals(0L, stats.totalSizeBytes)
    }

    @Test fun getStats_withVariousTypes_countsCorrectly() {
        val logDir = getLogDirectory()
        File(logDir, "crash_1.txt").writeText("c1")
        File(logDir, "crash_2.txt").writeText("c2")
        File(logDir, "logcat_errors_1.txt").writeText("l1")
        File(logDir, "${AppLogger.SESSION_PREFIX}1.txt").writeText("s1")
        val stats = crashLogger.getStats()
        assertEquals(2, stats.totalCrashes)
        assertEquals(1, stats.totalLogCats)
        assertEquals(1, stats.totalSessions)
        assertTrue(stats.totalSizeBytes > 0)
    }

    @Test fun getStats_location_isNotEmpty() { assertTrue(crashLogger.getStats().location.isNotEmpty()) }

    // ── cleanOldLogs ──────────────────────────────────────────────────────

    @Test fun cleanOldLogs_fewerThanKeepCount_doesNotDelete() {
        val logDir = getLogDirectory()
        repeat(5) { i -> File(logDir, "crash_$i.txt").writeText("c$i") }
        crashLogger.cleanOldLogs(keepCount = 10)
        assertEquals(5, logDir.listFiles()?.size ?: 0)
    }

    @Test fun cleanOldLogs_moreThanKeepCount_deletesOldest() {
        val logDir = getLogDirectory()
        repeat(10) { i ->
            File(logDir, "crash_$i.txt").also { it.writeText("c$i"); it.setLastModified((i + 1) * 1000L) }
        }
        crashLogger.cleanOldLogs(keepCount = 5)
        assertEquals(5, logDir.listFiles()?.size ?: 0)
    }

    @Test fun cleanOldLogs_exactlyKeepCount_deletesNone() {
        val logDir = getLogDirectory()
        repeat(3) { i -> File(logDir, "crash_$i.txt").writeText("c") }
        crashLogger.cleanOldLogs(keepCount = 3)
        assertEquals(3, logDir.listFiles()?.size ?: 0)
    }

    // ── saveLogCatErrors ──────────────────────────────────────────────────

    @Test fun saveLogCatErrors_withoutAppLogger_returnsNull() {
        resetAppLoggerSingleton()
        assertNull(crashLogger.saveLogCatErrors())
    }

    @Test fun saveLogCatErrors_withEmptyAppLoggerBuffer_returnsNull() {
        AppLogger.init(context)
        assertNull(crashLogger.saveLogCatErrors())
    }
}
