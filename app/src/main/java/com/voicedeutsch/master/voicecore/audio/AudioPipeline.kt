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
    private val _audioSharedFlow = kotlinx.coroutines.flow.MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 64
    )
    val incomingAudioFlow: kotlinx.coroutines.flow.SharedFlow<ByteArray> = _audioSharedFlow
    fun audioChunks(): kotlinx.coroutines.flow.Flow<ByteArray> = _audioSharedFlow

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
                        _audioSharedFlow.tryEmit(bytes)
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

    // 🔥 FIX Deadlock: cancelAndJoin() вынесен ЗА пределы мьютекса.
    // Было: cancelAndJoin() внутри withLock → playbackJob.finally тоже ждёт withLock → дэдлок.
    // Стало: захватываем job-ссылку и сбрасываем состояние внутри мьютекса,
    //        затем отпускаем мьютекс и только потом ждём завершения job.
    fun flushPlayback() {
        pipelineScope.launch {
            android.util.Log.d("AudioPipeline", "Interruption: Flushing audio queue")

            // 1. Захватываем ссылку и сбрасываем состояние под мьютексом
            val jobToCancel = stateMutex.withLock {
                val job = playbackJob
                playbackJob = null
                _isPlaying = false
                playbackQueue.cancel()
                playbackQueue = Channel(capacity = Channel.UNLIMITED)
                job
            }

            // 2. cancelAndJoin() — ВНЕ мьютекса, дэдлок невозможен
            jobToCancel?.cancelAndJoin()

            // 3. Сбрасываем железо после гарантированной остановки job
            player.flush()
        }
    }

    fun stopPlayback() {
        pipelineScope.launch {
            // 1. Захватываем ссылку и сбрасываем состояние под мьютексом
            val jobToCancel = stateMutex.withLock {
                if (!_isPlaying) return@launch
                val job = playbackJob
                playbackJob = null
                _isPlaying = false
                playbackQueue.cancel()
                playbackQueue = Channel(capacity = Channel.UNLIMITED)
                job
            }

            // 2. cancelAndJoin() — ВНЕ мьютекса
            jobToCancel?.cancelAndJoin()

            // 3. Останавливаем железо
            player.stop()
        }
    }

    fun stopAll() {
        runBlocking { flushPlayback() }
        stopRecording()
    }

    fun getCurrentAmplitude(): Float = recorder.currentAmplitude

    /**
     * ✅ НОВОЕ: Уведомление о приостановке аудиопотока.
     * Вызывается VoiceCoreEngineImpl при stopListening().
     * Позволяет серверному VAD корректно обработать паузу.
     */
    fun notifyStreamPaused() {
        // Аудиопоток приостановлен — серверный VAD должен быть уведомлён
        // через GeminiClient.sendAudioStreamEnd() (вызывается выше по стеку)
        android.util.Log.d("AudioPipeline", "Audio stream paused notification")
    }

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