package com.voicedeutsch.master.presentation.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Wrapper for strings that can come from resources or be dynamic.
 * Allows ViewModel to emit user-facing text without holding Context.
 *
 * Architecture line 1051 (presentation/util/UiText.kt).
 */
sealed class UiText {

    data class DynamicString(val value: String) : UiText()

    class StringResource(
        @StringRes val resId: Int,
        val args: Array<Any> = emptyArray()
    ) : UiText()

    fun asString(context: Context): String = when (this) {
        is DynamicString -> value
        is StringResource -> context.getString(resId, *args)
    }

    @Composable
    fun asString(): String = asString(LocalContext.current)

    companion object {
        fun of(value: String): UiText = DynamicString(value)
        fun of(@StringRes resId: Int, vararg args: Any): UiText = StringResource(resId, args.toList().toTypedArray())
    }
}