// Путь: src/test/java/com/voicedeutsch/master/domain/model/user/VoiceSettingsTest.kt
package com.voicedeutsch.master.domain.model.user

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VoiceSettingsTest {

    // ── Default values ────────────────────────────────────────────────────

    @Test
    fun constructor_defaultValues_appliedCorrectly() {
        val settings = VoiceSettings()
        assertEquals(1.0f, settings.voiceSpeed, 0.001f)
        assertEquals(0.8f, settings.germanVoiceSpeed, 0.001f)
        assertEquals(0.2f, settings.voiceLanguageMix, 0.001f)
        assertTrue(settings.showTranscription)
        assertTrue(settings.showWaveform)
        assertEquals(AudioQuality.HIGH, settings.audioQuality)
    }

    // ── Custom construction ───────────────────────────────────────────────

    @Test
    fun constructor_customValues_storedCorrectly() {
        val settings = VoiceSettings(
            voiceSpeed = 1.5f,
            germanVoiceSpeed = 0.6f,
            voiceLanguageMix = 0.7f,
            showTranscription = false,
            showWaveform = false,
            audioQuality = AudioQuality.LOW
        )
        assertEquals(1.5f, settings.voiceSpeed, 0.001f)
        assertEquals(0.6f, settings.germanVoiceSpeed, 0.001f)
        assertEquals(0.7f, settings.voiceLanguageMix, 0.001f)
        assertFalse(settings.showTranscription)
        assertFalse(settings.showWaveform)
        assertEquals(AudioQuality.LOW, settings.audioQuality)
    }

    @Test
    fun constructor_mediumQuality_storedCorrectly() {
        val settings = VoiceSettings(audioQuality = AudioQuality.MEDIUM)
        assertEquals(AudioQuality.MEDIUM, settings.audioQuality)
    }

    // ── Boundary values ───────────────────────────────────────────────────

    @Test
    fun constructor_voiceSpeedAtMinimum_storedCorrectly() {
        val settings = VoiceSettings(voiceSpeed = 0.0f)
        assertEquals(0.0f, settings.voiceSpeed, 0.001f)
    }

    @Test
    fun constructor_voiceSpeedAtMaximum_storedCorrectly() {
        val settings = VoiceSettings(voiceSpeed = 2.0f)
        assertEquals(2.0f, settings.voiceSpeed, 0.001f)
    }

    @Test
    fun constructor_languageMixFullGerman_storedCorrectly() {
        val settings = VoiceSettings(voiceLanguageMix = 1.0f)
        assertEquals(1.0f, settings.voiceLanguageMix, 0.001f)
    }

    @Test
    fun constructor_languageMixFullRussian_storedCorrectly() {
        val settings = VoiceSettings(voiceLanguageMix = 0.0f)
        assertEquals(0.0f, settings.voiceLanguageMix, 0.001f)
    }

    // ── copy() ────────────────────────────────────────────────────────────

    @Test
    fun copy_changeVoiceSpeed_onlySpeedChanges() {
        val original = VoiceSettings()
        val modified = original.copy(voiceSpeed = 1.25f)
        assertEquals(1.25f, modified.voiceSpeed, 0.001f)
        assertEquals(original.germanVoiceSpeed, modified.germanVoiceSpeed, 0.001f)
        assertEquals(original.audioQuality, modified.audioQuality)
        assertEquals(original.showTranscription, modified.showTranscription)
    }

    @Test
    fun copy_disableTranscription_onlyTranscriptionChanges() {
        val original = VoiceSettings(showTranscription = true)
        val modified = original.copy(showTranscription = false)
        assertFalse(modified.showTranscription)
        assertTrue(original.showTranscription)
        assertEquals(original.showWaveform, modified.showWaveform)
    }

    @Test
    fun copy_changeAudioQuality_qualityUpdated() {
        val original = VoiceSettings(audioQuality = AudioQuality.HIGH)
        val modified = original.copy(audioQuality = AudioQuality.LOW)
        assertEquals(AudioQuality.LOW, modified.audioQuality)
        assertEquals(AudioQuality.HIGH, original.audioQuality)
    }

    @Test
    fun copy_changeLanguageMix_mixUpdated() {
        val original = VoiceSettings(voiceLanguageMix = 0.2f)
        val modified = original.copy(voiceLanguageMix = 0.8f)
        assertEquals(0.8f, modified.voiceLanguageMix, 0.001f)
        assertEquals(0.2f, original.voiceLanguageMix, 0.001f)
    }

    // ── equals / hashCode ─────────────────────────────────────────────────

    @Test
    fun equals_twoIdenticalInstances_areEqual() {
        val s1 = VoiceSettings(voiceSpeed = 1.2f, audioQuality = AudioQuality.MEDIUM)
        val s2 = VoiceSettings(voiceSpeed = 1.2f, audioQuality = AudioQuality.MEDIUM)
        assertEquals(s1, s2)
    }

    @Test
    fun hashCode_twoIdenticalInstances_sameHashCode() {
        val s1 = VoiceSettings(voiceSpeed = 1.2f, audioQuality = AudioQuality.MEDIUM)
        val s2 = VoiceSettings(voiceSpeed = 1.2f, audioQuality = AudioQuality.MEDIUM)
        assertEquals(s1.hashCode(), s2.hashCode())
    }

    @Test
    fun equals_differentVoiceSpeed_notEqual() {
        val s1 = VoiceSettings(voiceSpeed = 1.0f)
        val s2 = VoiceSettings(voiceSpeed = 1.5f)
        assertNotEquals(s1, s2)
    }

    @Test
    fun equals_differentAudioQuality_notEqual() {
        val s1 = VoiceSettings(audioQuality = AudioQuality.HIGH)
        val s2 = VoiceSettings(audioQuality = AudioQuality.LOW)
        assertNotEquals(s1, s2)
    }

    @Test
    fun equals_differentShowWaveform_notEqual() {
        val s1 = VoiceSettings(showWaveform = true)
        val s2 = VoiceSettings(showWaveform = false)
        assertNotEquals(s1, s2)
    }
}

class AudioQualityTest {

    @Test
    fun entries_size_isThree() {
        assertEquals(3, AudioQuality.entries.size)
    }

    @Test
    fun entries_containsLow() {
        assertTrue(AudioQuality.entries.contains(AudioQuality.LOW))
    }

    @Test
    fun entries_containsMedium() {
        assertTrue(AudioQuality.entries.contains(AudioQuality.MEDIUM))
    }

    @Test
    fun entries_containsHigh() {
        assertTrue(AudioQuality.entries.contains(AudioQuality.HIGH))
    }

    @Test
    fun valueOf_low_returnsLow() {
        assertEquals(AudioQuality.LOW, AudioQuality.valueOf("LOW"))
    }

    @Test
    fun valueOf_medium_returnsMedium() {
        assertEquals(AudioQuality.MEDIUM, AudioQuality.valueOf("MEDIUM"))
    }

    @Test
    fun valueOf_high_returnsHigh() {
        assertEquals(AudioQuality.HIGH, AudioQuality.valueOf("HIGH"))
    }

    @Test
    fun valueOf_unknownValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            AudioQuality.valueOf("ULTRA")
        }
    }

    @Test
    fun ordinal_low_isZero() {
        assertEquals(0, AudioQuality.LOW.ordinal)
    }

    @Test
    fun ordinal_medium_isOne() {
        assertEquals(1, AudioQuality.MEDIUM.ordinal)
    }

    @Test
    fun ordinal_high_isTwo() {
        assertEquals(2, AudioQuality.HIGH.ordinal)
    }

    @Test
    fun defaultVoiceSettings_usesHighQuality() {
        assertEquals(AudioQuality.HIGH, VoiceSettings().audioQuality)
    }
}
