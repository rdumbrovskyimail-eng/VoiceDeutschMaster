package com.voicedeutsch.master.voicecore.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean

class AudioPlayer {
    companion object {
        const val SAMPLE_RATE = 24_000 // Строго 24kHz для вывода Gemini!
        private val MIN_BUFFER_SIZE = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(4096)

        // Размер чанка для порционной записи в WRITE_NON_BLOCKING режиме.
        // ~42 мс при 24kHz / 16bit / mono — баланс между латентностью и overhead.
        // ИСПРАВЛЕНО: убран const — результат getMinBufferSize() не является compile-time константой.
        private val WRITE_CHUNK_SIZE = MIN_BUFFER_SIZE
    }

    private var audioTrack: AudioTrack? = null
    private val _isPlaying = AtomicBoolean(false)
    private val _isPaused  = AtomicBoolean(false)
    val isPlaying: Boolean get() = _isPlaying.get()

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
     * Записывает PCM-данные в AudioTrack порционно в WRITE_NON_BLOCKING режиме.
     *
     * Проблема оригинала:
     *   track.write(pcmBytes, 0, pcmBytes.size) — блокирующий вызов.
     *   Если Gemini прислал большой чанк и аппаратный буфер переполнен,
     *   корутина playbackJob в AudioPipeline зависает на Dispatchers.IO.
     *   Это делает прерывания (interruption / flushPlayback) медленными —
     *   cancelAndJoin() ждёт пока write() не разблокируется.
     *
     * Решение:
     *   WRITE_NON_BLOCKING — пишем сколько влезет прямо сейчас, без блокировки.
     *   Остаток делим на чанки и пишем в цикле с yield() между итерациями.
     *   yield() даёт планировщику корутин возможность обработать cancellation —
     *   при flushPlayback() корутина отменится между чанками, не застревая в write().
     */
    suspend fun write(pcmBytes: ByteArray) {
        val track = audioTrack ?: return

        // ✅ FIX: Защита от STATE_UNINITIALIZED после многократных flush().
        if (track.state == AudioTrack.STATE_UNINITIALIZED) {
            release()
            start()
            return
        }

        if (pcmBytes.isEmpty() || _isPaused.get()) return

        var offset = 0
        while (offset < pcmBytes.size) {
            // Точка отмены — если flushPlayback() отменил корутину, выходим здесь
            kotlinx.coroutines.yield()

            if (_isPaused.get()) break

            val chunkSize = minOf(WRITE_CHUNK_SIZE, pcmBytes.size - offset)
            val written = track.write(
                pcmBytes,
                offset,
                chunkSize,
                AudioTrack.WRITE_NON_BLOCKING, // API 21+ — не блокирует поток
            )

            when {
                written > 0  -> offset += written
                written == 0 -> kotlinx.coroutines.delay(5) // буфер полон, ждём немного
                else         -> break // ERROR_INVALID_OPERATION или ERROR_DEAD_OBJECT
            }
        }
    }

    fun pause() {
        if (_isPaused.getAndSet(true)) return
        audioTrack?.pause()
    }

    fun resume() {
        if (!_isPaused.getAndSet(false)) return
        audioTrack?.play()
    }

    fun flush() {
        runCatching {
            _isPaused.set(false)
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        }
    }

    fun stop() {
        if (!_isPlaying.getAndSet(false)) return
        audioTrack?.apply {
            pause()
            flush()
            stop()
        }
        _isPaused.set(false)
    }

    fun release() {
        stop()
        audioTrack?.release()
        audioTrack = null
    }

    fun setVolume(volume: Float) {
        audioTrack?.setVolume(volume.coerceIn(0f, 1f))
    }
}