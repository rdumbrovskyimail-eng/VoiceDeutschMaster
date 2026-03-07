// Путь: src/test/java/com/voicedeutsch/master/voicecore/service/VoiceSessionServiceTest.kt
package com.voicedeutsch.master.voicecore.service

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.robolectric.annotation.Config

@Config(sdk = [34])
class VoiceSessionServiceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // ── startIntent ───────────────────────────────────────────────────────

    @Test
    fun startIntent_returnsIntent() {
        val intent = VoiceSessionService.startIntent(context)
        assertNotNull(intent)
    }

    @Test
    fun startIntent_componentTargetsVoiceSessionService() {
        val intent = VoiceSessionService.startIntent(context)
        assertEquals(
            VoiceSessionService::class.java.name,
            intent.component?.className,
        )
    }

    @Test
    fun startIntent_actionIsStartSession() {
        val intent = VoiceSessionService.startIntent(context)
        assertEquals("com.voicedeutsch.master.action.START_SESSION", intent.action)
    }

    @Test
    fun startIntent_calledTwice_returnsDifferentInstances() {
        val a = VoiceSessionService.startIntent(context)
        val b = VoiceSessionService.startIntent(context)
        assertNotSame(a, b)
    }

    @Test
    fun startIntent_actionDiffersFromStopAction() {
        val start = VoiceSessionService.startIntent(context)
        val stop  = VoiceSessionService.stopIntent(context)
        assertNotEquals(start.action, stop.action)
    }

    // ── stopIntent ────────────────────────────────────────────────────────

    @Test
    fun stopIntent_returnsIntent() {
        val intent = VoiceSessionService.stopIntent(context)
        assertNotNull(intent)
    }

    @Test
    fun stopIntent_componentTargetsVoiceSessionService() {
        val intent = VoiceSessionService.stopIntent(context)
        assertEquals(
            VoiceSessionService::class.java.name,
            intent.component?.className,
        )
    }

    @Test
    fun stopIntent_actionIsStopSession() {
        val intent = VoiceSessionService.stopIntent(context)
        assertEquals("com.voicedeutsch.master.action.STOP_SESSION", intent.action)
    }

    @Test
    fun stopIntent_calledTwice_returnsDifferentInstances() {
        val a = VoiceSessionService.stopIntent(context)
        val b = VoiceSessionService.stopIntent(context)
        assertNotSame(a, b)
    }

    // ── intent extras / flags ─────────────────────────────────────────────

    @Test
    fun startIntent_hasNoExtras() {
        val intent = VoiceSessionService.startIntent(context)
        val extras = intent.extras
        assertTrue(extras == null || extras.isEmpty)
    }

    @Test
    fun stopIntent_hasNoExtras() {
        val intent = VoiceSessionService.stopIntent(context)
        val extras = intent.extras
        assertTrue(extras == null || extras.isEmpty)
    }

    @Test
    fun startIntent_samePackageAsContext() {
        val intent = VoiceSessionService.startIntent(context)
        assertEquals(context.packageName, intent.component?.packageName)
    }

    @Test
    fun stopIntent_samePackageAsContext() {
        val intent = VoiceSessionService.stopIntent(context)
        assertEquals(context.packageName, intent.component?.packageName)
    }

    // ── onBind ────────────────────────────────────────────────────────────

    @Test
    fun onBind_returnsNull() {
        val service = VoiceSessionService()
        val binder = service.onBind(Intent())
        assertNull(binder)
    }
}
