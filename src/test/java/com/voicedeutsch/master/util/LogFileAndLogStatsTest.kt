// src/test/java/com/voicedeutsch/master/util/LogFileAndLogStatsTest.kt
package com.voicedeutsch.master.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class LogFileAndLogStatsTest {

    @TempDir
    lateinit var tempDir: Path

    private fun createTempFile(name: String, content: String = "a".repeat(1024)): File =
        tempDir.resolve(name).toFile().also { it.writeText(content) }

    // ── LogFile.name ──────────────────────────────────────────────────────

    @Test fun logFileName_returnsFileName() {
        val file = createTempFile("crash_2026-03-06_10-00-00.txt")
        assertEquals("crash_2026-03-06_10-00-00.txt", LogFile(file, LogType.CRASH, file.lastModified()).name)
    }

    // ── LogFile.sizeKB ────────────────────────────────────────────────────

    @Test fun logFileSizeKB_1024Bytes_returnsOne() {
        val file = createTempFile("test.txt", "a".repeat(1024))
        assertEquals(1L, LogFile(file, LogType.LOGCAT, file.lastModified()).sizeKB)
    }

    @Test fun logFileSizeKB_smallFile_returnsZero() {
        val file = createTempFile("tiny.txt", "hello")
        assertEquals(0L, LogFile(file, LogType.SESSION, file.lastModified()).sizeKB)
    }

    @Test fun logFileSizeKB_2048Bytes_returnsTwo() {
        val file = createTempFile("medium.txt", "a".repeat(2048))
        assertEquals(2L, LogFile(file, LogType.CRASH, file.lastModified()).sizeKB)
    }

    // ── LogFile.formattedDate ─────────────────────────────────────────────

    @Test fun logFileFormattedDate_matchesDateTimePattern() {
        val file = createTempFile("log.txt")
        val logFile = LogFile(file, LogType.CRASH, System.currentTimeMillis())
        assertTrue(logFile.formattedDate.matches(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""")),
            "Formatted date '${logFile.formattedDate}' does not match expected pattern")
    }

    // ── LogFile.type ──────────────────────────────────────────────────────

    @Test fun logFile_typeCrash_isCorrect() {
        assertEquals(LogType.CRASH, LogFile(createTempFile("c.txt"), LogType.CRASH, 0L).type)
    }

    @Test fun logFile_typeLogcat_isCorrect() {
        assertEquals(LogType.LOGCAT, LogFile(createTempFile("l.txt"), LogType.LOGCAT, 0L).type)
    }

    @Test fun logFile_typeSession_isCorrect() {
        assertEquals(LogType.SESSION, LogFile(createTempFile("s.txt"), LogType.SESSION, 0L).type)
    }

    // ── LogFile equals / hashCode ─────────────────────────────────────────

    @Test fun logFile_equals_sameFields_returnsTrue() {
        val file = createTempFile("eq.txt")
        val ts = 1_741_000_000_000L
        assertEquals(LogFile(file, LogType.CRASH, ts), LogFile(file, LogType.CRASH, ts))
    }

    @Test fun logFile_equals_differentType_returnsFalse() {
        val file = createTempFile("neq.txt")
        val ts = 1_741_000_000_000L
        assertNotEquals(LogFile(file, LogType.CRASH, ts), LogFile(file, LogType.LOGCAT, ts))
    }

    // ── LogType entries ───────────────────────────────────────────────────

    @Test fun logType_hasExactlyThreeEntries() { assertEquals(3, LogType.entries.size) }
    @Test fun logType_containsCrash() { assertTrue(LogType.entries.contains(LogType.CRASH)) }
    @Test fun logType_containsLogcat() { assertTrue(LogType.entries.contains(LogType.LOGCAT)) }
    @Test fun logType_containsSession() { assertTrue(LogType.entries.contains(LogType.SESSION)) }

    // ── LogStats.totalSizeKB ──────────────────────────────────────────────

    @Test fun logStats_totalSizeKB_calculatedFromBytes() {
        assertEquals(10L, LogStats(1, 2, 3, 10_240L, "/data/logs").totalSizeKB)
    }

    @Test fun logStats_totalSizeKB_zeroBytesReturnsZero() {
        assertEquals(0L, LogStats(0, 0, 0, 0L, "/data/logs").totalSizeKB)
    }

    @Test fun logStats_copy_preservesAllFields() {
        val copied = LogStats(2, 3, 4, 5120L, "/logs").copy(totalCrashes = 10)
        assertEquals(10, copied.totalCrashes)
        assertEquals(3, copied.totalLogCats)
        assertEquals(5120L, copied.totalSizeBytes)
        assertEquals(5L, copied.totalSizeKB)
    }

    @Test fun logStats_equals_sameValues_returnsTrue() {
        assertEquals(LogStats(1, 2, 3, 4096L, "/path"), LogStats(1, 2, 3, 4096L, "/path"))
    }

    @Test fun logStats_equals_differentCrashCount_returnsFalse() {
        assertNotEquals(LogStats(1, 2, 3, 4096L, "/path"), LogStats(5, 2, 3, 4096L, "/path"))
    }
}
