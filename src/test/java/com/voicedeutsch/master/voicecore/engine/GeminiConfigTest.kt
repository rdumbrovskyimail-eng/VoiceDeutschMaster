// Путь: src/test/java/com/voicedeutsch/master/voicecore/engine/GeminiConfigTest.kt
package com.voicedeutsch.master.voicecore.engine

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GeminiConfigTest {

    // ── companion object constants ────────────────────────────────────────────

    @Test
    fun modelGeminiLive_isCorrectValue() {
        assertEquals("gemini-2.5-flash-native-audio-preview-12-2025", GeminiConfig.MODEL_GEMINI_LIVE)
    }

    @Test
    fun maxContextTokens_is131072() {
        assertEquals(131_072, GeminiConfig.MAX_CONTEXT_TOKENS)
    }

    @Test
    fun defaultTemperature_is0_5() {
        assertEquals(0.5f, GeminiConfig.DEFAULT_TEMPERATURE)
    }

    @Test
    fun defaultTopP_is0_95() {
        assertEquals(0.95f, GeminiConfig.DEFAULT_TOP_P)
    }

    @Test
    fun defaultTopK_is40() {
        assertEquals(40, GeminiConfig.DEFAULT_TOP_K)
    }

    @Test
    fun defaultReconnectAttempts_is3() {
        assertEquals(3, GeminiConfig.DEFAULT_RECONNECT_ATTEMPTS)
    }

    @Test
    fun defaultReconnectDelayMs_is2000() {
        assertEquals(2_000L, GeminiConfig.DEFAULT_RECONNECT_DELAY_MS)
    }

    @Test
    fun defaultVoice_isKore() {
        assertEquals("Kore", GeminiConfig.DEFAULT_VOICE)
    }

    // ── default values ────────────────────────────────────────────────────────

    @Test
    fun defaultConfig_modelNameIsModelGeminiLive() {
        val config = GeminiConfig()
        assertEquals(GeminiConfig.MODEL_GEMINI_LIVE, config.modelName)
    }

    @Test
    fun defaultConfig_temperatureIsDefault() {
        assertEquals(GeminiConfig.DEFAULT_TEMPERATURE, GeminiConfig().temperature)
    }

    @Test
    fun defaultConfig_topPIsDefault() {
        assertEquals(GeminiConfig.DEFAULT_TOP_P, GeminiConfig().topP)
    }

    @Test
    fun defaultConfig_topKIsDefault() {
        assertEquals(GeminiConfig.DEFAULT_TOP_K, GeminiConfig().topK)
    }

    @Test
    fun defaultConfig_reconnectMaxAttemptsIsDefault() {
        assertEquals(GeminiConfig.DEFAULT_RECONNECT_ATTEMPTS, GeminiConfig().reconnectMaxAttempts)
    }

    @Test
    fun defaultConfig_reconnectDelayMsIsDefault() {
        assertEquals(GeminiConfig.DEFAULT_RECONNECT_DELAY_MS, GeminiConfig().reconnectDelayMs)
    }

    @Test
    fun defaultConfig_voiceNameIsDefault() {
        assertEquals(GeminiConfig.DEFAULT_VOICE, GeminiConfig().voiceName)
    }

    @Test
    fun defaultConfig_maxContextTokensIsDefault() {
        assertEquals(GeminiConfig.MAX_CONTEXT_TOKENS, GeminiConfig().maxContextTokens)
    }

    @Test
    fun defaultConfig_thinkingBudgetIsNull() {
        assertNull(GeminiConfig().thinkingBudget)
    }

    @Test
    fun defaultConfig_transcriptionInputEnabled() {
        assertTrue(GeminiConfig().transcriptionConfig.inputTranscriptionEnabled)
    }

    @Test
    fun defaultConfig_transcriptionOutputEnabled() {
        assertTrue(GeminiConfig().transcriptionConfig.outputTranscriptionEnabled)
    }

    @Test
    fun defaultConfig_enableSearchGroundingIsFalse() {
        assertFalse(GeminiConfig().enableSearchGrounding)
    }

    @Test
    fun defaultConfig_functionCallingModeIsAuto() {
        assertEquals(GeminiConfig.FunctionCallingMode.AUTO, GeminiConfig().functionCallingMode)
    }

    @Test
    fun defaultConfig_audioInputFormatIsPcm16Khz() {
        assertEquals(GeminiConfig.AudioFormat.PCM_16KHZ_16BIT_MONO, GeminiConfig().audioInputFormat)
    }

    @Test
    fun defaultConfig_audioOutputFormatIsPcm24Khz() {
        assertEquals(GeminiConfig.AudioFormat.PCM_24KHZ_16BIT_MONO, GeminiConfig().audioOutputFormat)
    }

    // ── init: temperature validation ──────────────────────────────────────────

    @Test
    fun init_temperatureAt0_isValid() {
        assertDoesNotThrow { GeminiConfig(temperature = 0f) }
    }

    @Test
    fun init_temperatureAt2_isValid() {
        assertDoesNotThrow { GeminiConfig(temperature = 2f) }
    }

    @Test
    fun init_temperatureAt1_isValid() {
        assertDoesNotThrow { GeminiConfig(temperature = 1f) }
    }

    @Test
    fun init_temperatureNegative_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            GeminiConfig(temperature = -0.1f)
        }
    }

    @Test
    fun init_temperatureAbove2_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            GeminiConfig(temperature = 2.1f)
        }
    }

    @Test
    fun init_temperatureErrorMessage_mentionsRange() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            GeminiConfig(temperature = 3f)
        }
        assertTrue(ex.message?.contains("temperature") == true)
    }

    // ── init: topP validation ─────────────────────────────────────────────────

    @Test
    fun init_topPAt0_isValid() {
        assertDoesNotThrow { GeminiConfig(topP = 0f) }
    }

    @Test
    fun init_topPAt1_isValid() {
        assertDoesNotThrow { GeminiConfig(topP = 1f) }
    }

    @Test
    fun init_topPNegative_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            GeminiConfig(topP = -0.01f)
        }
    }

    @Test
    fun init_topPAbove1_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            GeminiConfig(topP = 1.01f)
        }
    }

    @Test
    fun init_topPErrorMessage_mentionsTopP() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            GeminiConfig(topP = 2f)
        }
        assertTrue(ex.message?.contains("topP") == true)
    }

    // ── init: topK validation ─────────────────────────────────────────────────

    @Test
    fun init_topKAt1_isValid() {
        assertDoesNotThrow { GeminiConfig(topK = 1) }
    }

    @Test
    fun init_topKAt0_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            GeminiConfig(topK = 0)
        }
    }

    @Test
    fun init_topKNegative_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            GeminiConfig(topK = -1)
        }
    }

    @Test
    fun init_topKErrorMessage_mentionsTopK() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            GeminiConfig(topK = 0)
        }
        assertTrue(ex.message?.contains("topK") == true)
    }

    // ── init: reconnectMaxAttempts validation ─────────────────────────────────

    @Test
    fun init_reconnectMaxAttemptsAt1_isValid() {
        assertDoesNotThrow { GeminiConfig(reconnectMaxAttempts = 1) }
    }

    @Test
    fun init_reconnectMaxAttemptsAt0_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            GeminiConfig(reconnectMaxAttempts = 0)
        }
    }

    @Test
    fun init_reconnectMaxAttemptsNegative_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            GeminiConfig(reconnectMaxAttempts = -5)
        }
    }

    @Test
    fun init_reconnectMaxAttemptsErrorMessage_mentionsField() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            GeminiConfig(reconnectMaxAttempts = 0)
        }
        assertTrue(ex.message?.contains("reconnectMaxAttempts") == true)
    }

    // ── init: thinkingBudget validation ───────────────────────────────────────

    @Test
    fun init_thinkingBudgetNull_isValid() {
        assertDoesNotThrow { GeminiConfig(thinkingBudget = null) }
    }

    @Test
    fun init_thinkingBudgetAt0_isValid() {
        assertDoesNotThrow { GeminiConfig(thinkingBudget = 0) }
    }

    @Test
    fun init_thinkingBudgetPositive_isValid() {
        assertDoesNotThrow { GeminiConfig(thinkingBudget = 1024) }
    }

    @Test
    fun init_thinkingBudgetNegative_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            GeminiConfig(thinkingBudget = -1)
        }
    }

    @Test
    fun init_thinkingBudgetErrorMessage_mentionsField() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            GeminiConfig(thinkingBudget = -100)
        }
        assertTrue(ex.message?.contains("thinkingBudget") == true)
    }

    // ── data class: equals / hashCode / copy ──────────────────────────────────

    @Test
    fun equals_sameValues_returnsTrue() {
        val c1 = GeminiConfig(modelName = "model-a", temperature = 1.0f)
        val c2 = GeminiConfig(modelName = "model-a", temperature = 1.0f)
        assertEquals(c1, c2)
    }

    @Test
    fun equals_differentModelName_returnsFalse() {
        val c1 = GeminiConfig(modelName = "model-a")
        val c2 = GeminiConfig(modelName = "model-b")
        assertNotEquals(c1, c2)
    }

    @Test
    fun hashCode_sameValues_equal() {
        val c1 = GeminiConfig(topK = 10)
        val c2 = GeminiConfig(topK = 10)
        assertEquals(c1.hashCode(), c2.hashCode())
    }

    @Test
    fun copy_changesOnlySpecifiedField() {
        val original = GeminiConfig(temperature = 0.8f)
        val copy = original.copy(temperature = 1.2f)
        assertEquals(1.2f, copy.temperature)
        assertEquals(original.modelName, copy.modelName)
        assertEquals(original.topK, copy.topK)
    }

    @Test
    fun copy_invalidField_throwsIllegalArgumentException() {
        val original = GeminiConfig()
        assertThrows(IllegalArgumentException::class.java) {
            original.copy(temperature = 99f)
        }
    }

    // ── VadConfig ─────────────────────────────────────────────────────────────

    @Test
    fun vadConfig_defaultValues() {
        val vad = GeminiConfig.VadConfig()
        assertFalse(vad.disabled)
        assertEquals(GeminiConfig.VadConfig.Sensitivity.DEFAULT, vad.startSensitivity)
        assertEquals(GeminiConfig.VadConfig.Sensitivity.DEFAULT, vad.endSensitivity)
        assertEquals(20, vad.prefixPaddingMs)
        assertEquals(100, vad.silenceDurationMs)
    }

    @Test
    fun vadConfig_equals_sameValues() {
        val v1 = GeminiConfig.VadConfig(disabled = true, prefixPaddingMs = 50)
        val v2 = GeminiConfig.VadConfig(disabled = true, prefixPaddingMs = 50)
        assertEquals(v1, v2)
        assertEquals(v1.hashCode(), v2.hashCode())
    }

    @Test
    fun vadConfig_copy_changesOnlySpecifiedField() {
        val original = GeminiConfig.VadConfig(silenceDurationMs = 200)
        val copy = original.copy(disabled = true)
        assertTrue(copy.disabled)
        assertEquals(200, copy.silenceDurationMs)
    }

    @Test
    fun vadSensitivity_entriesContainDefaultLowHigh() {
        val entries = GeminiConfig.VadConfig.Sensitivity.entries
        assertTrue(GeminiConfig.VadConfig.Sensitivity.DEFAULT in entries)
        assertTrue(GeminiConfig.VadConfig.Sensitivity.LOW in entries)
        assertTrue(GeminiConfig.VadConfig.Sensitivity.HIGH in entries)
        assertEquals(3, entries.size)
    }

    // ── TranscriptionConfig ───────────────────────────────────────────────────

    @Test
    fun transcriptionConfig_defaultValues() {
        val tc = GeminiConfig.TranscriptionConfig()
        assertTrue(tc.inputTranscriptionEnabled)
        assertTrue(tc.outputTranscriptionEnabled)
    }

    @Test
    fun transcriptionConfig_equals_sameValues() {
        val t1 = GeminiConfig.TranscriptionConfig(inputTranscriptionEnabled = false)
        val t2 = GeminiConfig.TranscriptionConfig(inputTranscriptionEnabled = false)
        assertEquals(t1, t2)
        assertEquals(t1.hashCode(), t2.hashCode())
    }

    @Test
    fun transcriptionConfig_copy_changesOnlySpecifiedField() {
        val original = GeminiConfig.TranscriptionConfig()
        val copy = original.copy(outputTranscriptionEnabled = false)
        assertFalse(copy.outputTranscriptionEnabled)
        assertTrue(copy.inputTranscriptionEnabled)
    }

    // ── FunctionCallingMode enum ──────────────────────────────────────────────

    @Test
    fun functionCallingMode_entriesContainAllValues() {
        val entries = GeminiConfig.FunctionCallingMode.entries
        assertTrue(GeminiConfig.FunctionCallingMode.AUTO in entries)
        assertTrue(GeminiConfig.FunctionCallingMode.ANY in entries)
        assertTrue(GeminiConfig.FunctionCallingMode.NONE in entries)
        assertTrue(GeminiConfig.FunctionCallingMode.VALIDATED in entries)
        assertEquals(4, entries.size)
    }

    @Test
    fun functionCallingMode_autoName() {
        assertEquals("AUTO", GeminiConfig.FunctionCallingMode.AUTO.name)
    }

    @Test
    fun functionCallingMode_noneName() {
        assertEquals("NONE", GeminiConfig.FunctionCallingMode.NONE.name)
    }

    // ── AudioFormat enum ──────────────────────────────────────────────────────

    @Test
    fun audioFormat_entriesContainBothFormats() {
        val entries = GeminiConfig.AudioFormat.entries
        assertTrue(GeminiConfig.AudioFormat.PCM_16KHZ_16BIT_MONO in entries)
        assertTrue(GeminiConfig.AudioFormat.PCM_24KHZ_16BIT_MONO in entries)
        assertEquals(2, entries.size)
    }

    @Test
    fun audioFormat_pcm16Khz_sampleRateIs16000() {
        assertEquals(16_000, GeminiConfig.AudioFormat.PCM_16KHZ_16BIT_MONO.sampleRateHz)
    }

    @Test
    fun audioFormat_pcm16Khz_bitsPerSampleIs16() {
        assertEquals(16, GeminiConfig.AudioFormat.PCM_16KHZ_16BIT_MONO.bitsPerSample)
    }

    @Test
    fun audioFormat_pcm16Khz_channelsIs1() {
        assertEquals(1, GeminiConfig.AudioFormat.PCM_16KHZ_16BIT_MONO.channels)
    }

    @Test
    fun audioFormat_pcm24Khz_sampleRateIs24000() {
        assertEquals(24_000, GeminiConfig.AudioFormat.PCM_24KHZ_16BIT_MONO.sampleRateHz)
    }

    @Test
    fun audioFormat_pcm24Khz_bitsPerSampleIs16() {
        assertEquals(16, GeminiConfig.AudioFormat.PCM_24KHZ_16BIT_MONO.bitsPerSample)
    }

    @Test
    fun audioFormat_pcm24Khz_channelsIs1() {
        assertEquals(1, GeminiConfig.AudioFormat.PCM_24KHZ_16BIT_MONO.channels)
    }
}
