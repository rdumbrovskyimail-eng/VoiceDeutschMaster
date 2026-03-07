// Путь: src/test/java/com/voicedeutsch/master/voicecore/context/ContextBuilderTest.kt
package com.voicedeutsch.master.voicecore.context

import com.voicedeutsch.master.domain.model.LearningStrategy
import com.voicedeutsch.master.domain.model.knowledge.KnowledgeSnapshot
import com.voicedeutsch.master.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ContextBuilderTest {

    private lateinit var builder: ContextBuilder

    private val userContextProvider = mockk<UserContextProvider>(relaxed = true)
    private val bookContextProvider = mockk<BookContextProvider>(relaxed = true)
    private val userRepository      = mockk<UserRepository>(relaxed = true)

    private val userId    = "user_test"
    private val snapshot  = mockk<KnowledgeSnapshot>(relaxed = true)

    @BeforeEach
    fun setUp() {
        builder = ContextBuilder(
            userContextProvider = userContextProvider,
            bookContextProvider = bookContextProvider,
            userRepository      = userRepository,
        )
        coEvery { userContextProvider.buildUserContext(any()) } returns "USER_CONTEXT"
        coEvery { bookContextProvider.buildBookContext(any(), any()) } returns "BOOK_CONTEXT"
        coEvery { userRepository.getUserProfile(any()) } returns null
    }

    // ── SessionContext data class ─────────────────────────────────────────

    @Test
    fun sessionContext_fullContext_returnsSameAsSystemPrompt() {
        val ctx = ContextBuilder.SessionContext(
            systemPrompt         = "SYSTEM",
            userContext          = "USER",
            bookContext          = "BOOK",
            strategyPrompt       = "STRATEGY",
            functionDeclarations = emptyList(),
        )
        assertEquals("SYSTEM", ctx.fullContext)
    }

    @Test
    fun sessionContext_totalEstimatedTokens_positiveForNonEmptyPrompt() {
        val ctx = ContextBuilder.SessionContext(
            systemPrompt         = "A".repeat(400),
            userContext          = "",
            bookContext          = "",
            strategyPrompt       = "",
            functionDeclarations = emptyList(),
        )
        assertTrue(ctx.totalEstimatedTokens() > 0)
    }

    @Test
    fun sessionContext_totalEstimatedTokens_emptyPromptReturnsZero() {
        val ctx = ContextBuilder.SessionContext(
            systemPrompt         = "",
            userContext          = "",
            bookContext          = "",
            strategyPrompt       = "",
            functionDeclarations = emptyList(),
        )
        assertEquals(0, ctx.totalEstimatedTokens())
    }

    @Test
    fun sessionContext_totalEstimatedTokens_includesFunctionDeclarations() {
        val decl = com.voicedeutsch.master.voicecore.functions.GeminiFunctionDeclaration(
            name        = "test_func",
            description = "A test function description",
        )
        val withDecl = ContextBuilder.SessionContext(
            systemPrompt         = "",
            userContext          = "",
            bookContext          = "",
            strategyPrompt       = "",
            functionDeclarations = listOf(decl),
        )
        val withoutDecl = ContextBuilder.SessionContext(
            systemPrompt         = "",
            userContext          = "",
            bookContext          = "",
            strategyPrompt       = "",
            functionDeclarations = emptyList(),
        )
        assertTrue(withDecl.totalEstimatedTokens() > withoutDecl.totalEstimatedTokens())
    }

    @Test
    fun sessionContext_equals_twoIdentical() {
        val a = ContextBuilder.SessionContext("s", "u", "b", "st", emptyList())
        val b = ContextBuilder.SessionContext("s", "u", "b", "st", emptyList())
        assertEquals(a, b)
    }

    @Test
    fun sessionContext_copy_changesOnlySpecifiedField() {
        val original = ContextBuilder.SessionContext("s", "u", "b", "st", emptyList())
        val copied = original.copy(userContext = "NEW_USER")
        assertEquals("s", copied.systemPrompt)
        assertEquals("NEW_USER", copied.userContext)
    }

    // ── buildSessionContext — invocations ─────────────────────────────────

    @Test
    fun buildSessionContext_invokesUserContextProvider() = runTest {
        builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        coVerify(exactly = 1) { userContextProvider.buildUserContext(snapshot) }
    }

    @Test
    fun buildSessionContext_invokesBookContextProviderWithCorrectArgs() = runTest {
        builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 3, 7)
        coVerify(exactly = 1) { bookContextProvider.buildBookContext(3, 7) }
    }

    @Test
    fun buildSessionContext_invokesUserRepository() = runTest {
        builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        coVerify(exactly = 1) { userRepository.getUserProfile(userId) }
    }

    // ── buildSessionContext — returned SessionContext ──────────────────────

    @Test
    fun buildSessionContext_systemPromptIsNotBlank() = runTest {
        val ctx = builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        assertTrue(ctx.systemPrompt.isNotBlank())
    }

    @Test
    fun buildSessionContext_userContextMatchesProviderOutput() = runTest {
        coEvery { userContextProvider.buildUserContext(snapshot) } returns "MY_USER_CTX"
        val ctx = builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        assertEquals("MY_USER_CTX", ctx.userContext)
    }

    @Test
    fun buildSessionContext_bookContextMatchesProviderOutput() = runTest {
        coEvery { bookContextProvider.buildBookContext(2, 5) } returns "MY_BOOK_CTX"
        val ctx = builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 2, 5)
        assertEquals("MY_BOOK_CTX", ctx.bookContext)
    }

    @Test
    fun buildSessionContext_functionDeclarationsNotEmpty() = runTest {
        val ctx = builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        assertTrue(ctx.functionDeclarations.isNotEmpty())
    }

    // ── buildSessionContext — user profile null (defaults) ────────────────

    @Test
    fun buildSessionContext_nullProfile_systemPromptContainsDefaultName() = runTest {
        coEvery { userRepository.getUserProfile(any()) } returns null
        val ctx = builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        assertTrue(ctx.systemPrompt.contains("Ученик"))
    }

    @Test
    fun buildSessionContext_nullProfile_systemPromptContainsDefaultLevel() = runTest {
        coEvery { userRepository.getUserProfile(any()) } returns null
        val ctx = builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        assertTrue(ctx.systemPrompt.contains("A1"))
    }

    @Test
    fun buildSessionContext_nullProfile_systemPromptContainsDefaultStrictness() = runTest {
        coEvery { userRepository.getUserProfile(any()) } returns null
        val ctx = builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        assertTrue(ctx.systemPrompt.contains("MODERATE"))
    }

    @Test
    fun buildSessionContext_nullProfile_systemPromptContainsDefaultPace() = runTest {
        coEvery { userRepository.getUserProfile(any()) } returns null
        val ctx = builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        assertTrue(ctx.systemPrompt.contains("NORMAL"))
    }

    // ── buildSessionContext — user profile present ────────────────────────

    @Test
    fun buildSessionContext_profileWithName_systemPromptContainsName() = runTest {
        val profile = buildProfile(name = "Максим")
        coEvery { userRepository.getUserProfile(userId) } returns profile
        val ctx = builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        assertTrue(ctx.systemPrompt.contains("Максим"))
    }

    @Test
    fun buildSessionContext_profileWithLevel_systemPromptContainsLevel() = runTest {
        val profile = buildProfile(cefrLevel = "B2")
        coEvery { userRepository.getUserProfile(userId) } returns profile
        val ctx = builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        assertTrue(ctx.systemPrompt.contains("B2"))
    }

    @Test
    fun buildSessionContext_profileWithStrictPronunciation_systemPromptContainsStrict() = runTest {
        val profile = buildProfile(pronunciationStrictness = "STRICT")
        coEvery { userRepository.getUserProfile(userId) } returns profile
        val ctx = builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        assertTrue(ctx.systemPrompt.contains("STRICT"))
    }

    @Test
    fun buildSessionContext_profileWithFastPace_systemPromptContainsFast() = runTest {
        val profile = buildProfile(learningPace = "FAST")
        coEvery { userRepository.getUserProfile(userId) } returns profile
        val ctx = builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        assertTrue(ctx.systemPrompt.contains("FAST"))
    }

    // ── buildSessionContext — settings prompt sections ────────────────────

    @Test
    fun buildSessionContext_systemPromptContainsUserSettingsSection() = runTest {
        val ctx = builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        assertTrue(ctx.systemPrompt.contains("USER SETTINGS") || ctx.systemPrompt.contains("НАСТРОЙКИ"))
    }

    @Test
    fun buildSessionContext_systemPromptContainsProfileSection() = runTest {
        val ctx = builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        assertTrue(ctx.systemPrompt.contains("ПРОФИЛЬ") || ctx.systemPrompt.contains("PROFILE"))
    }

    // ── buildSessionContext — token budget ───────────────────────────────

    @Test
    fun buildSessionContext_defaultMaxTokens_totalTokensBelowLimit() = runTest {
        val ctx = builder.buildSessionContext(userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1)
        assertTrue(ctx.totalEstimatedTokens() < ContextBuilder.LIVE_API_TOTAL_TOKEN_LIMIT)
    }

    @Test
    fun buildSessionContext_smallMaxTokens_systemPromptStillNotBlank() = runTest {
        val ctx = builder.buildSessionContext(
            userId, snapshot, LearningStrategy.LINEAR_BOOK, 1, 1,
            maxContextTokens = 1_000,
        )
        assertTrue(ctx.systemPrompt.isNotBlank())
    }

    // ── constants ─────────────────────────────────────────────────────────

    @Test
    fun constants_liveApiTotalTokenLimit_equals131072() {
        assertEquals(131_072, ContextBuilder.LIVE_API_TOTAL_TOKEN_LIMIT)
    }

    @Test
    fun constants_safeContextTokenBudget_isPositive() {
        assertTrue(ContextBuilder.SAFE_CONTEXT_TOKEN_BUDGET > 0)
    }

    @Test
    fun constants_safeContextTokenBudget_lessThanTotalLimit() {
        assertTrue(ContextBuilder.SAFE_CONTEXT_TOKEN_BUDGET < ContextBuilder.LIVE_API_TOTAL_TOKEN_LIMIT)
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private fun buildProfile(
        name: String = "Тест",
        cefrLevel: String = "A1",
        pronunciationStrictness: String = "MODERATE",
        learningPace: String = "NORMAL",
        voiceSpeed: Float = 0.8f,
    ) = mockk<com.voicedeutsch.master.domain.model.user.UserProfile>(relaxed = true) {
        io.mockk.every { this@mockk.name } returns name
        io.mockk.every { age } returns 25
        io.mockk.every { hobbies } returns "спорт"
        io.mockk.every { learningGoals } returns "путешествия"
        io.mockk.every { nativeLanguage } returns "ru"
        io.mockk.every { this@mockk.cefrLevel.name } returns cefrLevel
        val prefs = mockk<com.voicedeutsch.master.domain.model.user.UserPreferences>(relaxed = true) {
            io.mockk.every { this@mockk.pronunciationStrictness.name } returns pronunciationStrictness
            io.mockk.every { this@mockk.learningPace.name } returns learningPace
        }
        io.mockk.every { preferences } returns prefs
        val voice = mockk<com.voicedeutsch.master.domain.model.user.VoiceSettings>(relaxed = true) {
            io.mockk.every { germanVoiceSpeed } returns voiceSpeed
        }
        io.mockk.every { voiceSettings } returns voice
    }
}
