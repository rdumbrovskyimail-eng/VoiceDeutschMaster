package com.voicedeutsch.master.voicecore.audio

import android.content.Context
import com.voicedeutsch.master.util.AudioUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AudioPipeline(private val context: Context) {
    private var recorder = AudioRecorder()
    private var player = AudioPlayer()

    private val stateMutex = Mutex()
    private var _isRecording = false
    private var _isPlaying = false
    private var _isInitialized = false

    val isRecording: Boolean get() = _isRecording
    val isPlaying: Boolean get() = _isPlaying

    // Канал для отправки микрофона (без буферизации старья, DROP_OLDEST)
    private val outgoingChannel = Channel<ByteArray>(
        capacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val incomingAudioFlow: Flow<ByteArray> = outgoingChannel.receiveAsFlow()
    fun audioChunks(): Flow<ByteArray> = incomingAudioFlow

    // Входящая очередь от Gemini (24kHz)
    private var playbackQueue = Channel<ByteArray>(
        capacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private var scopeJob = SupervisorJob()
    private var pipelineScope = CoroutineScope(Dispatchers.IO + scopeJob)
    private var recordingJob: Job? = null
    private var playbackJob: Job? = null

    fun initialize() {
        if (_isInitialized) return
        ensureScopeAlive()
        _isInitialized = true
    }

    fun release() {
        if (!_isInitialized) return
        stopAll()
        pipelineScope.cancel()
        _isInitialized = false
    }

    fun startRecording() {
        pipelineScope.launch {
            stateMutex.withLock {
                if (_isRecording) return@withLock
                ensureScopeAlive()
                recorder.start()
                _isRecording = true

                recordingJob = pipelineScope.launch {
                    recorder.audioFrameFlow.collect { pcmShorts ->
                        // НИКАКОГО VAD! Просто шлем сырой PCM 16kHz
                        val bytes = AudioUtils.shortArrayToByteArray(pcmShorts)
                        outgoingChannel.trySend(bytes)
                    }
                }
            }
        }
    }

    fun stopRecording() {
        pipelineScope.launch {
            stateMutex.withLock {
                if (!_isRecording) return@withLock
                recorder.stop()
                recordingJob?.cancel()
                recordingJob = null
                _isRecording = false
            }
        }
    }

    fun enqueueAudio(pcmBytes: ByteArray) {
        if (pcmBytes.isEmpty()) return
        playbackQueue.trySend(pcmBytes)
        ensurePlaybackRunning()
    }

    fun pausePlayback() = player.pause()
    fun resumePlayback() = player.resume()

    // МОМЕНТАЛЬНЫЙ СБРОС (Interruption)
    fun flushPlayback() {
        android.util.Log.d("AudioPipeline", "Interruption: Flushing audio queue")
        // Очищаем корутинный канал
        while (playbackQueue.tryReceive().isSuccess) { }
        // Сбрасываем аппаратный буфер
        player.flush()
    }

    fun stopPlayback() {
        pipelineScope.launch {
            stateMutex.withLock {
                if (!_isPlaying) return@withLock
                playbackJob?.cancel()
                playbackJob = null
                while (playbackQueue.tryReceive().isSuccess) { }
                player.stop()
                _isPlaying = false
            }
        }
    }

    fun stopAll() {
        scopeJob.cancel()
        recorder.release()
        player.release()
        while (playbackQueue.tryReceive().isSuccess) { }
        _isRecording = false
        _isPlaying = false
        recordingJob = null
        playbackJob = null
        ensureScopeAlive()
        recorder = AudioRecorder()
        player = AudioPlayer()
        playbackQueue = Channel(capacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    fun getCurrentAmplitude(): Float = recorder.currentAmplitude

    private fun ensurePlaybackRunning() {
        pipelineScope.launch {
            stateMutex.withLock {
                if (_isPlaying) return@withLock
                _isPlaying = true
                ensureScopeAlive()
                playbackJob = pipelineScope.launch {
                    player.start()
                    try {
                        for (chunk in playbackQueue) {
                            player.write(chunk)
                        }
                    } finally {
                        player.stop()
                        stateMutex.withLock { _isPlaying = false }
                    }
                }
            }
        }
    }

    private fun ensureScopeAlive() {
        if (scopeJob.isCancelled || scopeJob.isCompleted) {
            scopeJob = SupervisorJob()
            pipelineScope = CoroutineScope(Dispatchers.IO + scopeJob)
        }
    }
}
