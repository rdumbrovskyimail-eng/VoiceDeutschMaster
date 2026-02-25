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

    fun write(pcmBytes: ByteArray): Int {
        val track = audioTrack ?: return -1
        if (pcmBytes.isEmpty() || _isPaused.get()) return 0
        return track.write(pcmBytes, 0, pcmBytes.size)
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
            audioTrack?.flush() // Аппаратный сброс буфера
            audioTrack?.play()  // Сразу готовы к новому потоку
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
