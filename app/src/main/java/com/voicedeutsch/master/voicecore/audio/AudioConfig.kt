package com.voicedeutsch.master.voicecore.audio

/**
 * Audio pipeline configuration constants.
 * Architecture line 842 (voicecore/audio/AudioConfig.kt).
 */
object AudioConfig {

    /** Recording sample rate for PCM capture. Gemini expects 16 kHz mono. */
    const val SAMPLE_RATE = 16_000

    /** Single channel (mono). */
    const val CHANNEL_COUNT = 1

    /** Bits per sample (16-bit PCM). */
    const val BITS_PER_SAMPLE = 16

    /** Audio encoding = PCM 16-bit (AudioFormat.ENCODING_PCM_16BIT). */
    const val ENCODING_PCM_16BIT = 2 // android.media.AudioFormat.ENCODING_PCM_16BIT

    /** Size of each audio chunk sent to Gemini (in bytes). ~100ms of audio. */
    const val CHUNK_SIZE_BYTES = SAMPLE_RATE * 2 / 10  // 3200 bytes = 100ms

    /** VAD — energy threshold for speech detection (in dB). */
    const val VAD_ENERGY_THRESHOLD_DB = -35f

    /** VAD — minimum speech duration to consider as valid utterance (ms). */
    const val VAD_MIN_SPEECH_MS = 300

    /** VAD — silence duration after speech to trigger end-of-utterance (ms). */
    const val VAD_SILENCE_TIMEOUT_MS = 1500

    /** Max recording duration per utterance to prevent runaway recordings (ms). */
    const val MAX_UTTERANCE_DURATION_MS = 60_000

    /** Playback sample rate. Gemini may return 24 kHz audio. */
    const val PLAYBACK_SAMPLE_RATE = 24_000

    /** Audio MIME type sent in Gemini WebSocket messages. */
    const val AUDIO_MIME_TYPE = "audio/pcm;rate=$SAMPLE_RATE"

    /** Buffer size multiplier for AudioRecord (2x minimum for stability). */
    const val BUFFER_SIZE_MULTIPLIER = 2
}