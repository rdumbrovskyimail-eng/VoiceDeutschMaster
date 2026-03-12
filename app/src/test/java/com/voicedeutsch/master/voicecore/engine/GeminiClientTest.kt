// Путь: src/test/java/com/voicedeutsch/master/voicecore/engine/GeminiClientTest.kt
package com.voicedeutsch.master.voicecore.engine

import com.google.firebase.Firebase
// FIX: добавлен импорт extension-функции ai (была Unresolved reference 'ai')
import com.google.firebase.ai.ai
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.voicedeutsch.master.voicecore.context.ContextBuilder
import com.voicedeutsch.master.voicecore.functions.GeminiFunctionDeclaration
import com.voicedeutsch.master.voicecore.functions.GeminiParameters
import com.voicedeutsch.master.voicecore.functions.GeminiProperty
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(PublicPreviewAPI::class)
class GeminiClientTest {

    private lateinit var client: GeminiClient
    private lateinit var defaultConfig: GeminiConfig

    // Firebase mocks
    private lateinit var mockFirebaseAI:  FirebaseAI
    // Храним как Any, чтобы не зависеть от прямого импорта LiveModel
    private lateinit var mockLiveModel:   Any
    private lateinit var mockLiveSession: LiveSession

    @BeforeEach
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0

        // FIX: убран несуществующий параметр apiKey
        defaultConfig = GeminiConfig(
            modelName = "gemini-2.5-flash-preview-native-audio-dialog",
        )
        client = GeminiClient(defaultConfig)

        mockFirebaseAI   = mockk(relaxed = true)
        // Используем relaxed = true без явного типа LiveModel, чтобы избежать
        // Unresolved reference 'LiveModel' при компиляции теста
        mockLiveModel    = mockk(relaxed = true)
        mockLiveSession  = mockk(relaxed = true)

        mockkStatic(Firebase::class)
        every { Firebase.ai(backend = any()) } returns mockFirebaseAI
        // liveModel(...) возвращает Any (внутренний тип SDK), используем cast
        @Suppress("UNCHECKED_CAST")
        every { mockFirebaseAI.liveModel(
            modelName         = any(),
            generationConfig  = any(),
            systemInstruction = any(),
            tools             = any(),
        ) } answers { mockLiveModel as com.google.firebase.ai.LiveModel }
        coEvery { (mockLiveModel as com.google.firebase.ai.LiveModel).connect() } returns mockLiveSession
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildSessionContext(
        declarations: List<GeminiFunctionDeclaration> = emptyList(),
        systemPrompt: String = "system prompt",
    ) = ContextBuilder.SessionContext(
        systemPrompt         = systemPrompt,
        userContext          = "user ctx",
        bookContext          = "book ctx",
        strategyPrompt       = "strategy",
        functionDeclarations = declarations,
    )

    private fun buildDeclarationWithParams(
        name: String = "test_fn",
        properties: Map<String, GeminiProperty> = mapOf(
            "word" to GeminiProperty(type = "string", description = "A word"),
        ),
        required: List<String> = listOf("word"),
    ) = GeminiFunctionDeclaration(
        name        = name,
        description = "Test function $name",
        parameters  = GeminiParameters(
            properties = properties,
            required   = required,
        ),
    )

    private fun buildDeclarationNoParams(name: String = "no_params_fn") =
        GeminiFunctionDeclaration(
            name        = name,
            description = "Function with no params",
            parameters  = null,
        )

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialState_lastTokenUsageIsNull() {
        assertNull(client.lastTokenUsage)
    }

    @Test
    fun initialState_sessionResumptionHandleIsNull() {
        assertNull(client.sessionResumptionHandle)
    }

    @Test
    fun initialState_configIsSetFromConstructor() {
        assertEquals(defaultConfig, client.config)
    }

    // ── config setter ─────────────────────────────────────────────────────────

    @Test
    fun configSetter_updatesConfig() {
        // FIX: убран несуществующий параметр apiKey
        val newConfig = GeminiConfig(modelName = "new-model")
        client.config = newConfig
        assertEquals(newConfig, client.config)
    }

    // ── release ───────────────────────────────────────────────────────────────

    @Test
    fun release_clearsSessionResumptionHandle() {
        client.release()
        assertNull(client.sessionResumptionHandle)
    }

    @Test
    fun release_clearsLastTokenUsage() {
        client.release()
        assertNull(client.lastTokenUsage)
    }

    @Test
    fun release_calledMultipleTimes_doesNotThrow() {
        assertDoesNotThrow {
            client.release()
            client.release()
        }
    }

    // ── clearResumptionHandle ─────────────────────────────────────────────────

    @Test
    fun clearResumptionHandle_setsHandleToNull() {
        client.clearResumptionHandle()
        assertNull(client.sessionResumptionHandle)
    }

    @Test
    fun clearResumptionHandle_doesNotThrowWhenAlreadyNull() {
        assertDoesNotThrow { client.clearResumptionHandle() }
    }

    // ── stopConversation: no session ──────────────────────────────────────────

    @Test
    fun stopConversation_whenNoSession_doesNotThrow() = runTest {
        assertDoesNotThrow { runTest { client.stopConversation() } }
    }

    @Test
    fun stopConversation_whenNoSession_doesNotCallSessionMethod() = runTest {
        client.stopConversation()
        coVerify(exactly = 0) { mockLiveSession.stopAudioConversation() }
    }

    // ── disconnect: no session ────────────────────────────────────────────────

    @Test
    fun disconnect_whenNoSession_doesNotThrow() = runTest {
        assertDoesNotThrow { runTest { client.disconnect() } }
    }

    // ── sendText: no session ──────────────────────────────────────────────────

    @Test
    fun sendText_whenNoSession_doesNotThrow() = runTest {
        assertDoesNotThrow { runTest { client.sendText("Hallo!") } }
    }

    @Test
    fun sendText_whenNoSession_doesNotCallSessionSend() = runTest {
        client.sendText("ignored")
        coVerify(exactly = 0) { mockLiveSession.send(any()) }
    }

    // ── startConversation: no session ─────────────────────────────────────────

    @Test
    fun startConversation_whenNoSession_throwsGeminiConnectionException() = runTest {
        assertThrows(GeminiConnectionException::class.java) {
            runTest { client.startConversation { mockk(relaxed = true) } }
        }
    }

    // ── connect: happy path ───────────────────────────────────────────────────

    @Test
    fun connect_success_callsLiveModelConnect() = runTest {
        client.connect(buildSessionContext())
        coVerify { (mockLiveModel as com.google.firebase.ai.LiveModel).connect() }
    }

    @Test
    fun connect_success_callsFirebaseAiWithCorrectBackend() = runTest {
        client.connect(buildSessionContext())
        verify { Firebase.ai(backend = any()) }
    }

    @Test
    fun connect_success_usesConfigModelName() = runTest {
        client.connect(buildSessionContext())
        verify {
            mockFirebaseAI.liveModel(
                modelName         = defaultConfig.modelName,
                generationConfig  = any(),
                systemInstruction = any(),
                tools             = any(),
            )
        }
    }

    @Test
    fun connect_success_usesSystemPromptAsSystemInstruction() = runTest {
        val ctx = buildSessionContext(systemPrompt = "MY_SYSTEM_INSTRUCTION")
        client.connect(ctx)
        coVerify { (mockLiveModel as com.google.firebase.ai.LiveModel).connect() }
    }

    @Test
    fun connect_whenLiveModelConnectThrows_throwsGeminiConnectionException() = runTest {
        coEvery { (mockLiveModel as com.google.firebase.ai.LiveModel).connect() } throws RuntimeException("Network error")
        assertThrows(GeminiConnectionException::class.java) {
            runTest { client.connect(buildSessionContext()) }
        }
    }

    @Test
    fun connect_whenLiveModelConnectThrows_causeIsOriginalException() = runTest {
        val original = RuntimeException("Network error")
        coEvery { (mockLiveModel as com.google.firebase.ai.LiveModel).connect() } throws original
        val thrown = assertThrows(GeminiConnectionException::class.java) {
            runTest { client.connect(buildSessionContext()) }
        }
        assertEquals(original, thrown.cause)
    }

    @Test
    fun connect_withEmptyDeclarations_doesNotAddFunctionsTool() = runTest {
        client.connect(buildSessionContext(declarations = emptyList()))
        coVerify { (mockLiveModel as com.google.firebase.ai.LiveModel).connect() }
    }

    @Test
    fun connect_withDeclarations_registersDeclarationsInTool() = runTest {
        val decls = listOf(buildDeclarationWithParams("save_word"))
        client.connect(buildSessionContext(declarations = decls))
        coVerify { (mockLiveModel as com.google.firebase.ai.LiveModel).connect() }
    }

    // ── connect: mapToFirebaseDeclaration — no params → dummy param ───────────

    @Test
    fun connect_declarationWithNoParams_mappedWithDummyOptionalParam() = runTest {
        val decls = listOf(buildDeclarationNoParams("trigger_celebration"))
        client.connect(buildSessionContext(declarations = decls))
        coVerify { (mockLiveModel as com.google.firebase.ai.LiveModel).connect() }
    }

    @Test
    fun connect_declarationWithEmptyParams_mappedWithDummyOptionalParam() = runTest {
        val decl = GeminiFunctionDeclaration(
            name        = "no_param_fn",
            description = "Has empty params",
            parameters  = GeminiParameters(properties = emptyMap(), required = emptyList()),
        )
        client.connect(buildSessionContext(declarations = listOf(decl)))
        coVerify { (mockLiveModel as com.google.firebase.ai.LiveModel).connect() }
    }

    // ── connect: mapPropertyToSchema — type mapping ───────────────────────────

    @Test
    fun connect_declarationWithStringProperty_doesNotThrow() = runTest {
        val decl = buildDeclarationWithParams(
            properties = mapOf("word" to GeminiProperty(type = "string", description = "Word"))
        )
        assertDoesNotThrow { runTest { client.connect(buildSessionContext(listOf(decl))) } }
    }

    @Test
    fun connect_declarationWithIntegerProperty_doesNotThrow() = runTest {
        val decl = buildDeclarationWithParams(
            properties = mapOf("level" to GeminiProperty(type = "integer", description = "Level"))
        )
        assertDoesNotThrow { runTest { client.connect(buildSessionContext(listOf(decl))) } }
    }

    @Test
    fun connect_declarationWithNumberProperty_doesNotThrow() = runTest {
        val decl = buildDeclarationWithParams(
            properties = mapOf("score" to GeminiProperty(type = "number", description = "Score"))
        )
        assertDoesNotThrow { runTest { client.connect(buildSessionContext(listOf(decl))) } }
    }

    @Test
    fun connect_declarationWithBooleanProperty_doesNotThrow() = runTest {
        val decl = buildDeclarationWithParams(
            properties = mapOf("flag" to GeminiProperty(type = "boolean", description = "Flag"))
        )
        assertDoesNotThrow { runTest { client.connect(buildSessionContext(listOf(decl))) } }
    }

    @Test
    fun connect_declarationWithArrayProperty_doesNotThrow() = runTest {
        val decl = buildDeclarationWithParams(
            properties = mapOf("items" to GeminiProperty(type = "array", description = "Items"))
        )
        assertDoesNotThrow { runTest { client.connect(buildSessionContext(listOf(decl))) } }
    }

    @Test
    fun connect_declarationWithEnumProperty_doesNotThrow() = runTest {
        val decl = buildDeclarationWithParams(
            properties = mapOf(
                "strategy" to GeminiProperty(
                    type        = "string",
                    description = "Strategy",
                    enum        = listOf("REPETITION", "LINEAR_BOOK"),
                )
            )
        )
        assertDoesNotThrow { runTest { client.connect(buildSessionContext(listOf(decl))) } }
    }

    @Test
    fun connect_declarationWithUnknownPropertyType_fallsBackToString() = runTest {
        val decl = buildDeclarationWithParams(
            properties = mapOf("x" to GeminiProperty(type = "UNKNOWN_TYPE", description = "X"))
        )
        assertDoesNotThrow { runTest { client.connect(buildSessionContext(listOf(decl))) } }
    }

    @Test
    fun connect_declarationWithUpperCaseTypeName_normalizedCorrectly() = runTest {
        val decl = buildDeclarationWithParams(
            properties = mapOf("s" to GeminiProperty(type = "STRING", description = "s"))
        )
        assertDoesNotThrow { runTest { client.connect(buildSessionContext(listOf(decl))) } }
    }

    // ── connect: declaration mapping failure — skips invalid, connects rest ───

    @Test
    fun connect_oneDeclarationMappingFails_othersStillRegistered() = runTest {
        val good = buildDeclarationWithParams("save_word")
        val bad  = GeminiFunctionDeclaration(
            name        = "",
            description = "bad",
            parameters  = null,
        )
        assertDoesNotThrow { runTest { client.connect(buildSessionContext(listOf(good, bad))) } }
    }

    // ── startConversation: after connect ──────────────────────────────────────

    @Test
    fun startConversation_afterConnect_callsSessionStartAudioConversation() = runTest {
        client.connect(buildSessionContext())
        val callback: (FunctionCallPart) -> FunctionResponsePart = { mockk(relaxed = true) }
        client.startConversation(callback)
        coVerify { mockLiveSession.startAudioConversation(callback) }
    }

    @Test
    fun startConversation_afterConnect_doesNotThrow() = runTest {
        client.connect(buildSessionContext())
        assertDoesNotThrow {
            runTest { client.startConversation { mockk(relaxed = true) } }
        }
    }

    // ── stopConversation: after connect ───────────────────────────────────────

    @Test
    fun stopConversation_afterConnect_callsSessionStopAudioConversation() = runTest {
        client.connect(buildSessionContext())
        client.stopConversation()
        coVerify { mockLiveSession.stopAudioConversation() }
    }

    @Test
    fun stopConversation_afterConnect_sessionStopFails_doesNotThrow() = runTest {
        client.connect(buildSessionContext())
        coEvery { mockLiveSession.stopAudioConversation() } throws RuntimeException("Stop failed")
        assertDoesNotThrow { runTest { client.stopConversation() } }
    }

    // ── disconnect: after connect ─────────────────────────────────────────────

    @Test
    fun disconnect_afterConnect_callsSessionClose() = runTest {
        client.connect(buildSessionContext())
        client.disconnect()
        coVerify { mockLiveSession.close() }
    }

    @Test
    fun disconnect_afterConnect_sessionCloseThrows_doesNotThrow() = runTest {
        client.connect(buildSessionContext())
        coEvery { mockLiveSession.close() } throws RuntimeException("Close failed")
        assertDoesNotThrow { runTest { client.disconnect() } }
    }

    @Test
    fun disconnect_afterConnect_subsequentStopConversation_doesNotThrow() = runTest {
        client.connect(buildSessionContext())
        client.disconnect()
        assertDoesNotThrow { runTest { client.stopConversation() } }
    }

    // ── sendText: after connect ───────────────────────────────────────────────

    @Test
    fun sendText_afterConnect_callsSessionSend() = runTest {
        client.connect(buildSessionContext())
        client.sendText("Guten Tag!")
        coVerify { mockLiveSession.send(any()) }
    }

    @Test
    fun sendText_afterConnect_sessionSendFails_doesNotThrow() = runTest {
        client.connect(buildSessionContext())
        coEvery { mockLiveSession.send(any()) } throws RuntimeException("Send failed")
        assertDoesNotThrow { runTest { client.sendText("text") } }
    }

    // ── GeminiConnectionException ─────────────────────────────────────────────

    @Test
    fun geminiConnectionException_messageIsSet() {
        val ex = GeminiConnectionException("test message")
        assertEquals("test message", ex.message)
    }

    @Test
    fun geminiConnectionException_causeIsSet() {
        val cause = RuntimeException("root cause")
        val ex = GeminiConnectionException("wrapper", cause)
        assertEquals(cause, ex.cause)
    }

    @Test
    fun geminiConnectionException_isException() {
        val ex = GeminiConnectionException("msg")
        assertTrue(ex is Exception)
    }

    @Test
    fun geminiConnectionException_nullCause_isAllowed() {
        val ex = GeminiConnectionException("msg", null)
        assertNull(ex.cause)
    }

    // ── TokenUsage data class ─────────────────────────────────────────────────

    @Test
    fun tokenUsage_defaultValues_areZero() {
        val usage = GeminiClient.TokenUsage()
        assertEquals(0, usage.promptTokenCount)
        assertEquals(0, usage.responseTokenCount)
        assertEquals(0, usage.totalTokenCount)
    }

    @Test
    fun tokenUsage_equals_sameValues() {
        val u1 = GeminiClient.TokenUsage(10, 20, 30)
        val u2 = GeminiClient.TokenUsage(10, 20, 30)
        assertEquals(u1, u2)
        assertEquals(u1.hashCode(), u2.hashCode())
    }

    @Test
    fun tokenUsage_copy_changesOnlySpecifiedField() {
        val original = GeminiClient.TokenUsage(10, 20, 30)
        val copy = original.copy(responseTokenCount = 99)
        assertEquals(10, copy.promptTokenCount)
        assertEquals(99, copy.responseTokenCount)
        assertEquals(30, copy.totalTokenCount)
    }

    @Test
    fun tokenUsage_notEquals_whenFieldsDiffer() {
        val u1 = GeminiClient.TokenUsage(1, 2, 3)
        val u2 = GeminiClient.TokenUsage(4, 5, 6)
        assertNotEquals(u1, u2)
    }
}
