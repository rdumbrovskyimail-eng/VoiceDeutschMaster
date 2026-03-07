// Путь: src/test/java/com/voicedeutsch/master/voicecore/functions/FunctionRegistryTest.kt
package com.voicedeutsch.master.voicecore.functions

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FunctionRegistryTest {

    // ── getAllDeclarations ────────────────────────────────────────────────

    @Test
    fun getAllDeclarations_returnsNonEmptyList() {
        assertTrue(FunctionRegistry.getAllDeclarations().isNotEmpty())
    }

    @Test
    fun getAllDeclarations_containsAllGroupDeclarations() {
        val all = FunctionRegistry.getAllDeclarations()
        val expected = buildList {
            addAll(KnowledgeFunctions.declarations)
            addAll(BookFunctions.declarations)
            addAll(SessionFunctions.declarations)
            addAll(LearningFunctions.declarations)
            addAll(ProgressFunctions.declarations)
            addAll(UIFunctions.declarations)
        }
        assertEquals(expected.size, all.size)
    }

    @Test
    fun getAllDeclarations_namesAreUnique() {
        val names = FunctionRegistry.getAllDeclarations().map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun getAllDeclarations_noDeclarationHasBlankName() {
        FunctionRegistry.getAllDeclarations().forEach { decl ->
            assertTrue(decl.name.isNotBlank(), "Declaration has blank name: $decl")
        }
    }

    @Test
    fun getAllDeclarations_noDeclarationHasBlankDescription() {
        FunctionRegistry.getAllDeclarations().forEach { decl ->
            assertTrue(decl.description.isNotBlank(), "Declaration '${decl.name}' has blank description")
        }
    }

    @Test
    fun getAllDeclarations_orderIsKnowledgeBookSessionLearningProgressUi() {
        val all = FunctionRegistry.getAllDeclarations()
        val knowledgeNames = KnowledgeFunctions.declarations.map { it.name }
        val bookNames = BookFunctions.declarations.map { it.name }
        val sessionNames = SessionFunctions.declarations.map { it.name }
        val learningNames = LearningFunctions.declarations.map { it.name }
        val progressNames = ProgressFunctions.declarations.map { it.name }
        val uiNames = UIFunctions.declarations.map { it.name }

        val allNames = all.map { it.name }
        val knowledgeEnd = knowledgeNames.size
        val bookEnd = knowledgeEnd + bookNames.size
        val sessionEnd = bookEnd + sessionNames.size
        val learningEnd = sessionEnd + learningNames.size
        val progressEnd = learningEnd + progressNames.size

        assertEquals(knowledgeNames, allNames.subList(0, knowledgeEnd))
        assertEquals(bookNames, allNames.subList(knowledgeEnd, bookEnd))
        assertEquals(sessionNames, allNames.subList(bookEnd, sessionEnd))
        assertEquals(learningNames, allNames.subList(sessionEnd, learningEnd))
        assertEquals(progressNames, allNames.subList(learningEnd, progressEnd))
        assertEquals(uiNames, allNames.subList(progressEnd, allNames.size))
    }

    // ── declare — no params ───────────────────────────────────────────────

    @Test
    fun declare_noParams_parametersIsNull() {
        val decl = FunctionRegistry.declare(
            name = "test_func",
            description = "Test description",
        )
        assertNull(decl.parameters)
    }

    @Test
    fun declare_emptyParams_parametersIsNull() {
        val decl = FunctionRegistry.declare(
            name = "test_func",
            description = "Test description",
            params = emptyMap(),
        )
        assertNull(decl.parameters)
    }

    @Test
    fun declare_noParams_nameStoredCorrectly() {
        val decl = FunctionRegistry.declare(name = "my_func", description = "desc")
        assertEquals("my_func", decl.name)
    }

    @Test
    fun declare_noParams_descriptionStoredCorrectly() {
        val decl = FunctionRegistry.declare(name = "f", description = "my description")
        assertEquals("my description", decl.description)
    }

    // ── declare — with params ─────────────────────────────────────────────

    @Test
    fun declare_withParams_parametersIsNotNull() {
        val decl = FunctionRegistry.declare(
            name = "f",
            description = "d",
            params = mapOf("score" to ("number" to "Score 0-1")),
        )
        assertNotNull(decl.parameters)
    }

    @Test
    fun declare_withParams_typeIsObject() {
        val decl = FunctionRegistry.declare(
            name = "f",
            description = "d",
            params = mapOf("score" to ("number" to "Score 0-1")),
        )
        assertEquals("object", decl.parameters?.type)
    }

    @Test
    fun declare_withParams_propertyTypeStoredCorrectly() {
        val decl = FunctionRegistry.declare(
            name = "f",
            description = "d",
            params = mapOf("score" to ("number" to "Score 0-1")),
        )
        assertEquals("number", decl.parameters?.properties?.get("score")?.type)
    }

    @Test
    fun declare_withParams_propertyDescriptionStoredCorrectly() {
        val decl = FunctionRegistry.declare(
            name = "f",
            description = "d",
            params = mapOf("score" to ("number" to "Score 0-1")),
        )
        assertEquals("Score 0-1", decl.parameters?.properties?.get("score")?.description)
    }

    @Test
    fun declare_withMultipleParams_allPropertiesPresent() {
        val decl = FunctionRegistry.declare(
            name = "f",
            description = "d",
            params = mapOf(
                "score" to ("number" to "Score"),
                "index" to ("integer" to "Index"),
                "label" to ("string" to "Label"),
            ),
        )
        val props = decl.parameters?.properties
        assertNotNull(props?.get("score"))
        assertNotNull(props?.get("index"))
        assertNotNull(props?.get("label"))
    }

    @Test
    fun declare_withRequired_requiredStoredInParameters() {
        val decl = FunctionRegistry.declare(
            name = "f",
            description = "d",
            params = mapOf("score" to ("number" to "Score")),
            required = listOf("score"),
        )
        assertEquals(listOf("score"), decl.parameters?.required)
    }

    @Test
    fun declare_withParamsButNoRequired_requiredIsEmpty() {
        val decl = FunctionRegistry.declare(
            name = "f",
            description = "d",
            params = mapOf("score" to ("number" to "Score")),
        )
        assertTrue(decl.parameters?.required.isNullOrEmpty())
    }

    @Test
    fun declare_withMultipleRequired_allStoredCorrectly() {
        val decl = FunctionRegistry.declare(
            name = "f",
            description = "d",
            params = mapOf(
                "a" to ("string" to "A"),
                "b" to ("string" to "B"),
            ),
            required = listOf("a", "b"),
        )
        assertEquals(listOf("a", "b"), decl.parameters?.required)
    }

    // ── declare — property type mapping ──────────────────────────────────

    @Test
    fun declare_integerParam_typeIsInteger() {
        val decl = FunctionRegistry.declare(
            name = "f",
            description = "d",
            params = mapOf("index" to ("integer" to "Index 0-based")),
        )
        assertEquals("integer", decl.parameters?.properties?.get("index")?.type)
    }

    @Test
    fun declare_stringParam_typeIsString() {
        val decl = FunctionRegistry.declare(
            name = "f",
            description = "d",
            params = mapOf("word" to ("string" to "Target word")),
        )
        assertEquals("string", decl.parameters?.properties?.get("word")?.type)
    }

    @Test
    fun declare_booleanParam_typeIsBoolean() {
        val decl = FunctionRegistry.declare(
            name = "f",
            description = "d",
            params = mapOf("flag" to ("boolean" to "A flag")),
        )
        assertEquals("boolean", decl.parameters?.properties?.get("flag")?.type)
    }
}
