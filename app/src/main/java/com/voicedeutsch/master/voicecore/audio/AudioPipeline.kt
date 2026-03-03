package com.voicedeutsch.master.voicecore.audio

import android.content.Context
import com.voicedeutsch.master.util.AudioUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * AudioPipeline — управляет записью аудио для мониторинга амплитуды (визуализация аватара).
 *
 * В новой архитектуре Firebase AI SDK сам управляет микрофоном и воспроизведением
 * через startAudioConversation/stopAudioConversation. AudioPipeline используется
 * ТОЛЬКО для параллельного мониторинга амплитуды (если доступен второй AudioRecord)
 * или как fallback для будущих сценариев.
 */
class AudioPipeline(private val context: Context) {
    private var recorder = AudioRecorder()

    private val stateMutex = Mutex()
    private var _isRecording = false
    private var _isInitialized = false

    val isRecording: Boolean get() = _isRecording

    private val _audioSharedFlow = kotlinx.coroutines.flow.MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 64
    )
    val incomingAudioFlow: kotlinx.coroutines.flow.SharedFlow<ByteArray> = _audioSharedFlow
    fun audioChunks(): Flow<ByteArray> = _audioSharedFlow

    private var scopeJob = SupervisorJob()
    private var pipelineScope = CoroutineScope(Dispatchers.IO + scopeJob)
    private var recordingJob: Job? = null

    fun initialize() {
        if (_isInitialized) return
        ensureScopeAlive()
        _isInitialized = true
    }

    fun release() {
        if (!_isInitialized) return
        recorder.stop()
        recordingJob?.cancel()
        recordingJob = null
        _isRecording = false
        pipelineScope.cancel()
        _isInitialized = false
    }

    fun startRecording() {
        pipelineScope.launch {
            stateMutex.withLock {
                if (_isRecording) return@withLock
                ensureScopeAlive()

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

    suspend fun stopAll() {
        stopRecording()
    }

    fun getCurrentAmplitude(): Float = recorder.currentAmplitude

    private fun ensureScopeAlive() {
        if (scopeJob.isCancelled || scopeJob.isCompleted) {
            scopeJob = SupervisorJob()
            pipelineScope = CoroutineScope(Dispatchers.IO + scopeJob)
        }
    }
}
