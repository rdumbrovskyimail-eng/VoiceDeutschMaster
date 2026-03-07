// src/test/java/com/voicedeutsch/master/util/CrashTestUtilTest.kt
package com.voicedeutsch.master.util

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.lang.reflect.Field
import java.nio.file.Path

class CrashTestUtilTest {

    @TempDir lateinit var tempDir: Path
    private lateinit var context: Context

    @BeforeEach fun setUp() {
        context = mockk(relaxed = true)
        every { context.filesDir } returns tempDir.toFile()
        every { context.applicationContext } returns context
        every { context.packageManager } returns mockk(relaxed = true)
        every { context.packageName } returns "com.test.app"
        resetSingletons()
    }

    @AfterEach fun tearDown() { resetSingletons() }

    private fun resetSingletons() {
        listOf("com.voicedeutsch.master.util.CrashLogger",
               "com.voicedeutsch.master.util.AppLogger").forEach { className ->
            try {
                val field: Field = Class.forName(className).getDeclaredField("instance")
                field.isAccessible = true
                field.set(null, null)
            } catch (_: Exception) {}
        }
    }

    // ── triggerTestCrash ──────────────────────────────────────────────────

    @Test fun triggerTestCrash_throwsRuntimeException() {
        val thrown = assertThrows<RuntimeException> { CrashTestUtil.triggerTestCrash() }
        assertTrue(thrown.message?.contains("TEST CRASH") == true)
    }

    @Test fun triggerTestCrash_messageContainsIntentionalKeyword() {
        val thrown = assertThrows<RuntimeException> { CrashTestUtil.triggerTestCrash() }
        assertTrue(thrown.message?.contains("intentional") == true ||
                thrown.message?.contains("testing") == true)
    }

    // ── saveLogCatErrors ──────────────────────────────────────────────────

    @Test fun saveLogCatErrors_withoutCrashLogger_returnsNull() {
        assertNull(CrashTestUtil.saveLogCatErrors())
    }

    @Test fun saveLogCatErrors_withCrashLoggerButNoAppLogger_returnsNull() {
        CrashLogger.init(context)
        assertNull(CrashTestUtil.saveLogCatErrors())
    }

    // ── getLogStats ───────────────────────────────────────────────────────

    @Test fun getLogStats_withoutCrashLogger_returnsNull() {
        assertNull(CrashTestUtil.getLogStats())
    }

    @Test fun getLogStats_withCrashLogger_returnsStats() {
        CrashLogger.init(context)
        assertNotNull(CrashTestUtil.getLogStats())
    }

    @Test fun getLogStats_withCrashLogger_noLogs_hasZeroCrashes() {
        CrashLogger.init(context)
        assertEquals(0, CrashTestUtil.getLogStats()!!.totalCrashes)
    }

    @Test fun getLogStats_withCrashLogger_locationIsNotEmpty() {
        CrashLogger.init(context)
        assertTrue(CrashTestUtil.getLogStats()!!.location.isNotEmpty())
    }

    // ── shareLatestCrashLog ───────────────────────────────────────────────

    @Test fun shareLatestCrashLog_withoutCrashLogger_doesNotCrash() {
        CrashTestUtil.shareLatestCrashLog(context)
    }

    @Test fun shareLatestCrashLog_withCrashLoggerButNoCrashFile_doesNotCrash() {
        CrashLogger.init(context)
        CrashTestUtil.shareLatestCrashLog(context)
    }
}
