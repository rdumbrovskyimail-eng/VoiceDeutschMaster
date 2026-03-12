package com.voicedeutsch.master.presentation.util

import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import io.mockk.every
import io.mockk.mockk

class UiTextTest {

    private val context: Context = mockk()

    // ── DynamicString ──────────────────────────────────────────────────────

    @Test
    fun `DynamicString asString returns value`() {
        val uiText = UiText.DynamicString("Hallo Welt")
        assertEquals("Hallo Welt", uiText.asString(context))
    }

    @Test
    fun `DynamicString with empty string`() {
        val uiText = UiText.DynamicString("")
        assertEquals("", uiText.asString(context))
    }

    @Test
    fun `DynamicString equality`() {
        val a = UiText.DynamicString("test")
        val b = UiText.DynamicString("test")
        assertEquals(a, b)
    }

    @Test
    fun `DynamicString inequality`() {
        val a = UiText.DynamicString("hello")
        val b = UiText.DynamicString("world")
        assertNotEquals(a, b)
    }

    // ── StringResource ─────────────────────────────────────────────────────

    @Test
    fun `StringResource asString resolves via context`() {
        val resId = android.R.string.ok
        every { context.getString(resId) } returns "OK"
        val uiText = UiText.StringResource(resId)
        assertEquals("OK", uiText.asString(context))
    }

    @Test
    fun `StringResource with args asString resolves via context`() {
        val resId = android.R.string.ok
        every { context.getString(resId, "arg1") } returns "OK arg1"
        val uiText = UiText.StringResource(resId, arrayOf("arg1"))
        assertEquals("OK arg1", uiText.asString(context))
    }

    @Test
    fun `StringResource default args is empty array`() {
        val uiText = UiText.StringResource(android.R.string.ok)
        assertEquals(0, uiText.args.size)
    }

    // ── companion of() factories ───────────────────────────────────────────

    @Test
    fun `of(String) returns DynamicString`() {
        val uiText = UiText.of("dynamic")
        assertTrue(uiText is UiText.DynamicString)
        assertEquals("dynamic", (uiText as UiText.DynamicString).value)
    }

    @Test
    fun `of(resId) returns StringResource`() {
        val uiText = UiText.of(android.R.string.ok)
        assertTrue(uiText is UiText.StringResource)
        assertEquals(android.R.string.ok, (uiText as UiText.StringResource).resId)
    }

    @Test
    fun `of(resId, args) stores args in StringResource`() {
        val uiText = UiText.of(android.R.string.ok, "x", 42)
        assertTrue(uiText is UiText.StringResource)
        val sr = uiText as UiText.StringResource
        assertEquals(2, sr.args.size)
        assertEquals("x", sr.args[0])
        assertEquals(42, sr.args[1])
    }

    @Test
    fun `DynamicString is not StringResource`() {
        val uiText = UiText.of("hello")
        assertFalse(uiText is UiText.StringResource)
    }
}
