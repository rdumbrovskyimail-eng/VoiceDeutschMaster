package com.voicedeutsch.master.voicecore.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.voicedeutsch.master.util.Constants
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Plays PCM audio received from Gemini through the device speaker.
 *
 * Specification (Architecture lines 580-590):
 *   - Sample rate : 24 000 Hz (Gemini output format)
 *   - Bit depth   : 16-bit PCM
 *   - Channels    : Mono
 *   - Mode        : [AudioTrack.MODE_STREAM] (push-based, low-latency)
 *   - Usage       : USAGE_ASSISTANT / CONTENT_TYPE_SPEECH
 *
 * Usage:
 * ```
 * player.start()
 * player.write(pcmChunk)   // call repeatedly as chunks arrive
 * player.stop()
 * player.release()
 * ```
 */
class AudioPlayer {

    companion object {
        /** Output sample rate expected from Gemini Live API. */
        const val SAMPLE_RATE = Constants.AUDIO_OUTPUT_SAMPLE_RATE   // 24 000 Hz

        private val MIN_BUFFER_SIZE = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(4096)
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private var audioTrack: AudioTrack? = null
    private val _isPlaying = AtomicBoolean(false)
    private val _isPaused  = AtomicBoolean(false)

    val isPlaying: Boolean get() = _isPlaying.get()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Creates and starts [AudioTrack] in streaming mode.
     * Safe to call multiple times — no-op if already started.
     */
    fun start() {
        if (_isPlaying.getAndSet(true)) return

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(MIN_BUFFER_SIZE * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()
        _isPaused.set(false)
    }

    /**
     * Writes a PCM byte chunk to the AudioTrack buffer.
     *
     * @param pcmBytes  Little-endian 16-bit mono PCM at [SAMPLE_RATE].
     * @return          Number of bytes actually written, or a negative error code.
     */
    fun write(pcmBytes: ByteArray): Int {
        val track = audioTrack ?: return -1
        if (pcmBytes.isEmpty()) return 0
        if (_isPaused.get()) return 0
        return track.write(pcmBytes, 0, pcmBytes.size)
    }

    /** Pauses playback without clearing the internal buffer. */
    fun pause() {
        if (_isPaused.getAndSet(true)) return
        audioTrack?.pause()
    }

    /** Resumes paused playback. */
    fun resume() {
        if (!_isPaused.getAndSet(false)) return
        audioTrack?.play()
    }

    /**
     * Экстренная очистка очереди воспроизведения (Interruption).
     * Вызывается, когда пользователь перебил ИИ.
     */
    fun flush() {
        runCatching {
            _isPaused.set(false)
            // 1. Останавливаем аппаратное воспроизведение
            audioTrack?.pause()

            // 2. Сбрасываем звук, который уже ушел в аудиобуфер Android
            audioTrack?.flush()

            // 3. Снова переводим трек в состояние PLAY, чтобы он был готов к новому ответу
            audioTrack?.play()

        }.onFailure { e ->
            android.util.Log.e("AudioPlayer", "Ошибка при экстренном сбросе (flush): ${e.message}")
        }
    }

    /** Stops playback and flushes the internal buffer. */
    fun stop() {
        if (!_isPlaying.getAndSet(false)) return
        audioTrack?.apply {
            pause()
            flush()
            stop()
        }
        _isPaused.set(false)
    }

    /** Releases native AudioTrack resources. Must be called after [stop]. */
    fun release() {
        stop()
        audioTrack?.release()
        audioTrack = null
    }

    // ── Volume ────────────────────────────────────────────────────────────────

    /**
     * Sets the playback volume.
     *
     * @param volume  Value in [0.0, 1.0].
     */
    fun setVolume(volume: Float) {
        audioTrack?.setVolume(volume.coerceIn(0f, 1f))
    }
}