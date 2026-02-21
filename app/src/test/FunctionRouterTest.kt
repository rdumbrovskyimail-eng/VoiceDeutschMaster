package com.voicedeutsch.master.voicecore.functions

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for FunctionRouter.
 * Verifies all declared functions can be resolved.
 */
class FunctionRouterTest {

    @Test
    fun `all function declarations have valid names`() {
        val declarations = FunctionRegistry.getAllDeclarations()
        assertTrue(declarations.isNotEmpty())
        declarations.forEach { decl ->
            assertTrue(decl.name.isNotBlank(), "Function name should not be blank")
            assertTrue(decl.description.isNotBlank(), "Function '${ decl.name}' should have description")
        }
    }

    @Test
    fun `critical functions are declared`() {
        val names = FunctionRegistry.getAllDeclarations().map { it.name }.toSet()
        val required = listOf(
            "save_word_knowledge",
            "save_rule_knowledge",
            "get_words_for_repetition",
            "get_current_lesson",
            "advance_to_next_lesson",
            "mark_lesson_complete",
            "save_pronunciation_result",
            "set_current_strategy",
            "update_user_level",
            "get_user_statistics",
            "log_session_event",
            "get_weak_points"
        )
        required.forEach { fn ->
            assertTrue(fn in names, "Required function '$fn' is missing from declarations")
        }
    }
}