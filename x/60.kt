// Путь: src/test/java/com/voicedeutsch/master/data/local/datastore/UserPreferencesDataStoreTest.kt
package com.voicedeutsch.master.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.voicedeutsch.master.voicecore.engine.GeminiConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(org.robolectric.junit.rules.ErrorCollector::class)
@Config(sdk = [33])
class UserPreferencesDataStoreTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var context: Context
    private lateinit var sut: UserPreferencesDataStore
    private lateinit var testFile: File

    @BeforeEach
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testFile = File.createTempFile("user_preferences_test", ".preferences_pb", context.cacheDir)
        testFile.delete() // DataStore creates it fresh

        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )

        sut = UserPreferencesDataStore(context)
    }

    @AfterEach
    fun tearDown() {
        testScope.cancel()
        testFile.delete()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildGeminiConfig(
        modelName: String = GeminiConfig.MODEL_GEMINI_LIVE,
        temperature: Float = GeminiConfig.DEFAULT_TEMPERATURE,
        topP: Float = GeminiConfig.DEFAULT_TOP_P,
        topK: Int = GeminiConfig.DEFAULT_TOP_K,
        voiceName: String = GeminiConfig.DEFAULT_VOICE,
        maxContextTokens: Int = GeminiConfig.MAX_CONTEXT_TOKENS,
        inputTranscription: Boolean = false,
        outputTranscription: Boolean = false,
    ) = GeminiConfig(
        modelName = modelName,
        temperature = temperature,
        topP = topP,
        topK = topK,
        voiceName = voiceName,
        maxContextTokens = maxContextTokens,
        transcriptionConfig = GeminiConfig.TranscriptionConfig(
            inputTranscriptionEnabled = inputTranscription,
            outputTranscriptionEnabled = outputTranscription,
        ),
    )

    // ── getActiveUserId / setActiveUserId ─────────────────────────────────────

    @Test
    fun getActiveUserId_notSet_returnsNull() = runTest {
        val result = sut.getActiveUserId()
        assertNull(result)
    }

    @Test
    fun setActiveUserId_thenGet_returnsSameValue() = runTest {
        sut.setActiveUserId("user_42")
        assertEquals("user_42", sut.getActiveUserId())
    }

    @Test
    fun setActiveUserId_overwrite_returnsLatestValue() = runTest {
        sut.setActiveUserId("user_1")
        sut.setActiveUserId("user_2")
        assertEquals("user_2", sut.getActiveUserId())
    }

    // ── getThemeFlow / setTheme ───────────────────────────────────────────────

    @Test
    fun getThemeFlow_notSet_emitsSystemDefault() = runTest {
        sut.getThemeFlow().test {
            assertEquals("system", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setTheme_thenFlow_emitsNewTheme() = runTest {
        sut.setTheme("dark")
        sut.getThemeFlow().test {
            assertEquals("dark", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setTheme_multipleValues_flowEmitsLatest() = runTest {
        sut.setTheme("light")
        sut.setTheme("dark")
        sut.getThemeFlow().test {
            assertEquals("dark", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── isOnboardingComplete / setOnboardingComplete ──────────────────────────

    @Test
    fun isOnboardingComplete_notSet_returnsFalse() = runTest {
        assertFalse(sut.isOnboardingComplete())
    }

    @Test
    fun setOnboardingComplete_true_returnsTrue() = runTest {
        sut.setOnboardingComplete(true)
        assertTrue(sut.isOnboardingComplete())
    }

    @Test
    fun setOnboardingComplete_false_returnsFalse() = runTest {
        sut.setOnboardingComplete(true)
        sut.setOnboardingComplete(false)
        assertFalse(sut.isOnboardingComplete())
    }

    // ── isBookLoaded / setBookLoaded ──────────────────────────────────────────

    @Test
    fun isBookLoaded_notSet_returnsFalse() = runTest {
        assertFalse(sut.isBookLoaded())
    }

    @Test
    fun setBookLoaded_true_returnsTrue() = runTest {
        sut.setBookLoaded(true)
        assertTrue(sut.isBookLoaded())
    }

    @Test
    fun setBookLoaded_false_returnsFalse() = runTest {
        sut.setBookLoaded(true)
        sut.setBookLoaded(false)
        assertFalse(sut.isBookLoaded())
    }

    // ── getSessionDuration / setSessionDuration ───────────────────────────────

    @Test
    fun getSessionDuration_notSet_returnsNull() = runTest {
        assertNull(sut.getSessionDuration())
    }

    @Test
    fun setSessionDuration_thenGet_returnsSameValue() = runTest {
        sut.setSessionDuration(30)
        assertEquals(30, sut.getSessionDuration())
    }

    @Test
    fun setSessionDuration_overwrite_returnsLatest() = runTest {
        sut.setSessionDuration(20)
        sut.setSessionDuration(45)
        assertEquals(45, sut.getSessionDuration())
    }

    // ── getDailyGoal / setDailyGoal ───────────────────────────────────────────

    @Test
    fun getDailyGoal_notSet_returnsNull() = runTest {
        assertNull(sut.getDailyGoal())
    }

    @Test
    fun setDailyGoal_thenGet_returnsSameValue() = runTest {
        sut.setDailyGoal(15)
        assertEquals(15, sut.getDailyGoal())
    }

    @Test
    fun setDailyGoal_overwrite_returnsLatest() = runTest {
        sut.setDailyGoal(10)
        sut.setDailyGoal(25)
        assertEquals(25, sut.getDailyGoal())
    }

    // ── loadGeminiConfig ──────────────────────────────────────────────────────

    @Test
    fun loadGeminiConfig_notSet_returnsAllDefaults() = runTest {
        val config = sut.loadGeminiConfig()

        assertEquals(GeminiConfig.MODEL_GEMINI_LIVE, config.modelName)
        assertEquals(GeminiConfig.DEFAULT_TEMPERATURE, config.temperature, 0.001f)
        assertEquals(GeminiConfig.DEFAULT_TOP_P, config.topP, 0.001f)
        assertEquals(GeminiConfig.DEFAULT_TOP_K, config.topK)
        assertEquals(GeminiConfig.DEFAULT_VOICE, config.voiceName)
        assertEquals(GeminiConfig.MAX_CONTEXT_TOKENS, config.maxContextTokens)
        assertFalse(config.transcriptionConfig.inputTranscriptionEnabled)
        assertFalse(config.transcriptionConfig.outputTranscriptionEnabled)
    }

    @Test
    fun loadGeminiConfig_temperatureOutOfRangeHigh_clampedTo2() = runTest {
        // Save a value > 2 directly via saveGeminiConfig is constrained at load time
        sut.saveGeminiConfig(buildGeminiConfig(temperature = 5.0f))
        val config = sut.loadGeminiConfig()
        assertTrue(config.temperature <= 2.0f)
    }

    @Test
    fun loadGeminiConfig_temperatureOutOfRangeLow_clampedTo0() = runTest {
        sut.saveGeminiConfig(buildGeminiConfig(temperature = -1.0f))
        val config = sut.loadGeminiConfig()
        assertTrue(config.temperature >= 0.0f)
    }

    @Test
    fun loadGeminiConfig_topPOutOfRangeHigh_clampedTo1() = runTest {
        sut.saveGeminiConfig(buildGeminiConfig(topP = 1.5f))
        val config = sut.loadGeminiConfig()
        assertTrue(config.topP <= 1.0f)
    }

    @Test
    fun loadGeminiConfig_topPOutOfRangeLow_clampedTo0() = runTest {
        sut.saveGeminiConfig(buildGeminiConfig(topP = -0.5f))
        val config = sut.loadGeminiConfig()
        assertTrue(config.topP >= 0.0f)
    }

    @Test
    fun loadGeminiConfig_topKOutOfRangeHigh_clampedTo100() = runTest {
        sut.saveGeminiConfig(buildGeminiConfig(topK = 200))
        val config = sut.loadGeminiConfig()
        assertTrue(config.topK <= 100)
    }

    @Test
    fun loadGeminiConfig_topKOutOfRangeLow_clampedTo1() = runTest {
        sut.saveGeminiConfig(buildGeminiConfig(topK = 0))
        val config = sut.loadGeminiConfig()
        assertTrue(config.topK >= 1)
    }

    // ── saveGeminiConfig ──────────────────────────────────────────────────────

    @Test
    fun saveGeminiConfig_thenLoad_returnsAllSavedFields() = runTest {
        val config = buildGeminiConfig(
            modelName = "gemini-pro",
            temperature = 1.2f,
            topP = 0.8f,
            topK = 40,
            voiceName = "Kore",
            maxContextTokens = 8000,
            inputTranscription = true,
            outputTranscription = true,
        )

        sut.saveGeminiConfig(config)
        val loaded = sut.loadGeminiConfig()

        assertEquals("gemini-pro", loaded.modelName)
        assertEquals(1.2f, loaded.temperature, 0.001f)
        assertEquals(0.8f, loaded.topP, 0.001f)
        assertEquals(40, loaded.topK)
        assertEquals("Kore", loaded.voiceName)
        assertEquals(8000, loaded.maxContextTokens)
        assertTrue(loaded.transcriptionConfig.inputTranscriptionEnabled)
        assertTrue(loaded.transcriptionConfig.outputTranscriptionEnabled)
    }

    @Test
    fun saveGeminiConfig_overwrite_loadReturnsLatest() = runTest {
        sut.saveGeminiConfig(buildGeminiConfig(temperature = 0.5f))
        sut.saveGeminiConfig(buildGeminiConfig(temperature = 1.8f))
        val loaded = sut.loadGeminiConfig()
        assertEquals(1.8f, loaded.temperature, 0.001f)
    }

    @Test
    fun saveGeminiConfig_transcriptionFalse_loadReturnsFalse() = runTest {
        sut.saveGeminiConfig(buildGeminiConfig(inputTranscription = false, outputTranscription = false))
        val loaded = sut.loadGeminiConfig()
        assertFalse(loaded.transcriptionConfig.inputTranscriptionEnabled)
        assertFalse(loaded.transcriptionConfig.outputTranscriptionEnabled)
    }

    @Test
    fun saveGeminiConfig_transcriptionTrue_loadReturnsTrue() = runTest {
        sut.saveGeminiConfig(buildGeminiConfig(inputTranscription = true, outputTranscription = true))
        val loaded = sut.loadGeminiConfig()
        assertTrue(loaded.transcriptionConfig.inputTranscriptionEnabled)
        assertTrue(loaded.transcriptionConfig.outputTranscriptionEnabled)
    }

    // ── geminiConfigFlow ──────────────────────────────────────────────────────

    @Test
    fun geminiConfigFlow_notSet_emitsDefaults() = runTest {
        sut.geminiConfigFlow().test {
            val config = awaitItem()
            assertEquals(GeminiConfig.MODEL_GEMINI_LIVE, config.modelName)
            assertEquals(GeminiConfig.DEFAULT_TEMPERATURE, config.temperature, 0.001f)
            assertEquals(GeminiConfig.DEFAULT_TOP_P, config.topP, 0.001f)
            assertEquals(GeminiConfig.DEFAULT_TOP_K, config.topK)
            assertEquals(GeminiConfig.DEFAULT_VOICE, config.voiceName)
            assertFalse(config.transcriptionConfig.inputTranscriptionEnabled)
            assertFalse(config.transcriptionConfig.outputTranscriptionEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun geminiConfigFlow_afterSave_emitsUpdatedConfig() = runTest {
        sut.saveGeminiConfig(buildGeminiConfig(temperature = 0.7f, voiceName = "Aoede"))

        sut.geminiConfigFlow().test {
            val config = awaitItem()
            assertEquals(0.7f, config.temperature, 0.001f)
            assertEquals("Aoede", config.voiceName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun geminiConfigFlow_emitsOnEachSave() = runTest {
        sut.geminiConfigFlow().test {
            // Initial emission with defaults
            awaitItem()

            sut.saveGeminiConfig(buildGeminiConfig(temperature = 0.3f))
            val updated = awaitItem()
            assertEquals(0.3f, updated.temperature, 0.001f)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun geminiConfigFlow_modelName_updatedCorrectly() = runTest {
        sut.saveGeminiConfig(buildGeminiConfig(modelName = "gemini-flash"))
        sut.geminiConfigFlow().test {
            assertEquals("gemini-flash", awaitItem().modelName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun geminiConfigFlow_maxContextTokens_updatedCorrectly() = runTest {
        sut.saveGeminiConfig(buildGeminiConfig(maxContextTokens = 4096))
        sut.geminiConfigFlow().test {
            assertEquals(4096, awaitItem().maxContextTokens)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Keys companion object ─────────────────────────────────────────────────

    @Test
    fun preferenceKeys_haveExpectedNames() {
        assertEquals("active_user_id", UserPreferencesDataStore.ACTIVE_USER_ID.name)
        assertEquals("theme", UserPreferencesDataStore.THEME.name)
        assertEquals("is_onboarding_complete", UserPreferencesDataStore.IS_ONBOARDING_COMPLETE.name)
        assertEquals("is_book_loaded", UserPreferencesDataStore.IS_BOOK_LOADED.name)
        assertEquals("session_duration", UserPreferencesDataStore.SESSION_DURATION.name)
        assertEquals("daily_goal", UserPreferencesDataStore.DAILY_GOAL.name)
        assertEquals("gemini_temperature", UserPreferencesDataStore.GEMINI_TEMPERATURE.name)
        assertEquals("gemini_top_p", UserPreferencesDataStore.GEMINI_TOP_P.name)
        assertEquals("gemini_top_k", UserPreferencesDataStore.GEMINI_TOP_K.name)
        assertEquals("gemini_voice_name", UserPreferencesDataStore.GEMINI_VOICE_NAME.name)
        assertEquals("gemini_model_name", UserPreferencesDataStore.GEMINI_MODEL_NAME.name)
        assertEquals("gemini_max_context_tokens", UserPreferencesDataStore.GEMINI_MAX_CONTEXT_TOKENS.name)
        assertEquals("gemini_input_transcription", UserPreferencesDataStore.GEMINI_INPUT_TRANSCRIPTION.name)
        assertEquals("gemini_output_transcription", UserPreferencesDataStore.GEMINI_OUTPUT_TRANSCRIPTION.name)
    }

    // ── Roundtrip ─────────────────────────────────────────────────────────────

    @Test
    fun geminiConfig_roundtrip_saveAndLoadPreservesAllFields() = runTest {
        val original = buildGeminiConfig(
            modelName = "gemini-ultra",
            temperature = 0.9f,
            topP = 0.95f,
            topK = 50,
            voiceName = "Puck",
            maxContextTokens = 16000,
            inputTranscription = true,
            outputTranscription = false,
        )

        sut.saveGeminiConfig(original)
        val loaded = sut.loadGeminiConfig()

        assertEquals(original.modelName, loaded.modelName)
        assertEquals(original.temperature, loaded.temperature, 0.001f)
        assertEquals(original.topP, loaded.topP, 0.001f)
        assertEquals(original.topK, loaded.topK)
        assertEquals(original.voiceName, loaded.voiceName)
        assertEquals(original.maxContextTokens, loaded.maxContextTokens)
        assertEquals(
            original.transcriptionConfig.inputTranscriptionEnabled,
            loaded.transcriptionConfig.inputTranscriptionEnabled
        )
        assertEquals(
            original.transcriptionConfig.outputTranscriptionEnabled,
            loaded.transcriptionConfig.outputTranscriptionEnabled
        )
    }

    @Test
    fun multiplePreferences_independent_doNotInterfere() = runTest {
        sut.setActiveUserId("user_99")
        sut.setTheme("light")
        sut.setOnboardingComplete(true)
        sut.setBookLoaded(true)
        sut.setSessionDuration(60)
        sut.setDailyGoal(20)
        sut.saveGeminiConfig(buildGeminiConfig(temperature = 1.1f))

        assertEquals("user_99", sut.getActiveUserId())
        assertTrue(sut.isOnboardingComplete())
        assertTrue(sut.isBookLoaded())
        assertEquals(60, sut.getSessionDuration())
        assertEquals(20, sut.getDailyGoal())
        assertEquals(1.1f, sut.loadGeminiConfig().temperature, 0.001f)

        sut.getThemeFlow().test {
            assertEquals("light", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
