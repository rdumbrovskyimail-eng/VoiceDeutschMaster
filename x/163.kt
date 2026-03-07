// src/test/java/com/voicedeutsch/master/util/AppLoggerTest.kt
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

class AppLoggerTest {

    @TempDir lateinit var tempDir: Path
    private lateinit var context: Context
    private lateinit var appLogger: AppLogger

    @BeforeEach fun setUp() {
        val filesDir = tempDir.toFile()
        context = mockk(relaxed = true)
        every { context.filesDir } returns filesDir
        every { context.applicationContext } returns context
        every { context.packageManager } returns mockk(relaxed = true)
        every { context.packageName } returns "com.test.app"
        resetSingleton()
        appLogger = AppLogger.init(context)
    }

    @AfterEach fun tearDown() {
        appLogger.stop()
        resetSingleton()
    }

    private fun resetSingleton() {
        val field: Field = AppLogger::class.java.getDeclaredField("instance")
        field.isAccessible = true
        field.set(null, null)
    }

    // ── init (singleton) ──────────────────────────────────────────────────

    @Test fun init_returnsNonNullInstance() { assertNotNull(appLogger) }

    @Test fun init_calledTwice_returnsSameInstance() {
        assertSame(appLogger, AppLogger.init(context))
    }

    @Test fun getInstance_afterInit_returnsInstance() {
        assertNotNull(AppLogger.getInstance())
        assertSame(appLogger, AppLogger.getInstance())
    }

    @Test fun getInstance_beforeInit_returnsNull() {
        resetSingleton()
        assertNull(AppLogger.getInstance())
    }

    // ── isRunning ─────────────────────────────────────────────────────────

    @Test fun isRunning_beforeStart_isFalse() { assertFalse(appLogger.isRunning) }

    @Test fun stop_afterStart_isRunningIsFalse() {
        appLogger.start()
        appLogger.stop()
        assertFalse(appLogger.isRunning)
    }

    @Test fun start_calledTwice_doesNotCrash() {
        appLogger.start()
        appLogger.start()
        assertTrue(appLogger.isRunning)
        appLogger.stop()
    }

    // ── getBufferSnapshot / lineCount ─────────────────────────────────────

    @Test fun getBufferSnapshot_initiallyEmpty() { assertEquals("", appLogger.getBufferSnapshot()) }
    @Test fun lineCount_initiallyZero() { assertEquals(0, appLogger.lineCount()) }

    // ── dumpToFile ────────────────────────────────────────────────────────

    @Test fun dumpToFile_withTargetFile_createsFile() {
        val targetFile = File(tempDir.toFile(), "test_dump.txt")
        val result = appLogger.dumpToFile(targetFile)
        assertNotNull(result)
        assertTrue(result!!.exists())
    }

    @Test fun dumpToFile_noTargetFile_createsFileInLogDir() {
        val result = appLogger.dumpToFile()
        assertNotNull(result)
        assertTrue(result!!.exists())
    }

    @Test fun dumpToFile_withTargetFile_writesBufferContent() {
        val bufferField = AppLogger::class.java.getDeclaredField("buffer").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val buffer = bufferField.get(appLogger) as ArrayDeque<String>
        val lock = AppLogger::class.java.getDeclaredField("lock").apply { isAccessible = true }.get(appLogger)
        synchronized(lock) { buffer.addLast("test line") }

        val targetFile = File(tempDir.toFile(), "content_test.txt")
        val result = appLogger.dumpToFile(targetFile)
        assertNotNull(result)
        assertTrue(result!!.readText().contains("test line"))
    }

    // ── SESSION_PREFIX constant ───────────────────────────────────────────

    @Test fun sessionPrefix_isCorrectValue() { assertEquals("session_log_", AppLogger.SESSION_PREFIX) }

    // ── stop without start ────────────────────────────────────────────────

    @Test fun stop_withoutStart_doesNotCrash() {
        appLogger.stop()
        assertFalse(appLogger.isRunning)
    }
}
