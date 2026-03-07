// Путь: src/test/java/com/voicedeutsch/master/voicecore/audio/AudioConfigTest.kt
package com.voicedeutsch.master.voicecore.audio

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AudioConfigTest {

    @Test
    fun sampleRate_equals16000() {
        assertEquals(16_000, AudioConfig.SAMPLE_RATE)
    }

    @Test
    fun channelCount_equals1() {
        assertEquals(1, AudioConfig.CHANNEL_COUNT)
    }

    @Test
    fun bitsPerSample_equals16() {
        assertEquals(16, AudioConfig.BITS_PER_SAMPLE)
    }

    @Test
    fun encodingPcm16bit_equals2() {
        assertEquals(2, AudioConfig.ENCODING_PCM_16BIT)
    }

    @Test
    fun chunkSizeBytes_equals640() {
        assertEquals(640, AudioConfig.CHUNK_SIZE_BYTES)
    }

    @Test
    fun chunkSizeBytes_represents20msAt16kHz() {
        // 16000 samples/sec * 2 bytes/sample * 0.020 sec = 640 bytes
        val expected = AudioConfig.SAMPLE_RATE * (AudioConfig.BITS_PER_SAMPLE / 8) * 20 / 1000
        assertEquals(expected, AudioConfig.CHUNK_SIZE_BYTES)
    }

    @Test
    fun playbackSampleRate_equals24000() {
        assertEquals(24_000, AudioConfig.PLAYBACK_SAMPLE_RATE)
    }

    @Test
    fun playbackSampleRate_greaterThanRecordingSampleRate() {
        assertTrue(AudioConfig.PLAYBACK_SAMPLE_RATE > AudioConfig.SAMPLE_RATE)
    }

    @Test
    fun audioMimeType_containsPcm() {
        assertTrue(AudioConfig.AUDIO_MIME_TYPE.contains("pcm"))
    }

    @Test
    fun audioMimeType_containsSampleRate() {
        assertTrue(AudioConfig.AUDIO_MIME_TYPE.contains(AudioConfig.SAMPLE_RATE.toString()))
    }

    @Test
    fun audioMimeType_startsWithAudioPrefix() {
        assertTrue(AudioConfig.AUDIO_MIME_TYPE.startsWith("audio/"))
    }

    @Test
    fun bufferSizeMultiplier_equals2() {
        assertEquals(2, AudioConfig.BUFFER_SIZE_MULTIPLIER)
    }

    @Test
    fun bufferSizeMultiplier_atLeast2ForStability() {
        assertTrue(AudioConfig.BUFFER_SIZE_MULTIPLIER >= 2)
    }
}
