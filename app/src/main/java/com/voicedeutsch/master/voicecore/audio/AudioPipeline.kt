package com.voicedeutsch.master.voicecore.audio

import android.content.Context
import com.voicedeutsch.master.util.AudioUtils
import kotlinx.coroutines.*
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

    // Микрофон
    private val outgoingChannel = Channel<ByteArray>(capacity = 10)
    val incomingAudioFlow: Flow<ByteArray> = outgoingChannel.receiveAsFlow()
    fun audioChunks(): Flow<ByteArray> = incomingAudioFlow

    // 🔥 FIX: Заменили DROP_OLDEST на UNLIMITED для плавности речи ИИ
    private var playbackQueue = Channel<ByteArray>(capacity = Channel.UNLIMITED)

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

                // ✅ FIX: recorder.start(pipelineScope) вместо recorder.start().
                // AudioRecorder больше не создаёт raw Thread — запись идёт в корутине
                // под контролем pipelineScope. При stopAll() / release() scope отменяется
                // и корутина записи завершается автоматически без утечки потока.
                recorder.start(pipelineScope)
                _isRecording = true

                recordingJob = pipelineScope.launch {
                    recorder.audioFrameFlow.collect { pcmShorts ->
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

    // 🔥 FIX: Жесткая остановка без Race Conditions
    fun flushPlayback() {
        pipelineScope.launch {
            stateMutex.withLock {
                android.util.Log.d("AudioPipeline", "Interruption: Flushing audio queue")
                // Убиваем текущую задачу воспроизведения
                playbackJob?.cancelAndJoin()
                playbackJob = null
                _isPlaying = false

                // Полностью пересоздаем канал
                playbackQueue.cancel()
                playbackQueue = Channel(capacity = Channel.UNLIMITED)

                // Сбрасываем железо
                player.flush()
            }
        }
    }

    fun stopPlayback() {
        pipelineScope.launch {
            stateMutex.withLock {
                if (!_isPlaying) return@withLock
                playbackJob?.cancelAndJoin()
                playbackJob = null
                playbackQueue.cancel()
                playbackQueue = Channel(capacity = Channel.UNLIMITED)
                player.stop()
                _isPlaying = false
            }
        }
    }

    fun stopAll() {
        runBlocking { flushPlayback() }
        stopRecording()
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