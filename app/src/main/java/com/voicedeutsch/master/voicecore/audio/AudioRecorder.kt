package com.voicedeutsch.master.voicecore.audio

import kotlinx.coroutines.flow.Flow

/**
 * Конфигурация для записи аудио
 */
data class RecorderConfig(
    val sampleRate: Int = 16000,
    val channels: Int = 1,
    val bitsPerSample: Int = 16,
    val encoding: AudioEncoding = AudioEncoding.PCM,
    val bufferSize: Int = 4096
)

/**
 * Форматы кодирования аудио
 */
enum class AudioEncoding {
    PCM,
    OPUS,
    AAC,
    FLAC
}

/**
 * Состояния рекордера
 */
enum class RecorderState {
    Idle,
    Recording,
    Paused,
    Stopped,
    Error
}

/**
 * Интерфейс для записи аудио
 */
interface AudioRecorder {
    
    /**
     * Начинает запись аудио
     */
    suspend fun startRecording(): Result<Unit>
    
    /**
     * Останавливает запись
     */
    suspend fun stopRecording(): Result<ByteArray>
    
    /**
     * Приостанавливает запись
     */
    suspend fun pauseRecording(): Result<Unit>
    
    /**
     * Возобновляет запись
     */
    suspend fun resumeRecording(): Result<Unit>
    
    /**
     * Получает поток аудиокадров
     */
    fun getAudioStream(): Flow<AudioFrame>
    
    /**
     * Получает текущее состояние
     */
    fun getState(): RecorderState
    
    /**
     * Получает длительность записи в миллисекундах
     */
    fun getDuration(): Long
    
    /**
     * Освобождает ресурсы
     */
    suspend fun release()
}

/**
 * Реализация аудиорекордера
 */
class AudioRecorderImpl(
    private val config: RecorderConfig = RecorderConfig()
) : AudioRecorder {
    
    private var state = RecorderState.Idle
    private var startTime: Long = 0
    private val recordedFrames = mutableListOf<AudioFrame>()
    
    override suspend fun startRecording(): Result<Unit> {
        return try {
            if (state != RecorderState.Idle && state != RecorderState.Stopped) {
                throw IllegalStateException("Recorder is already recording or in error state")
            }
            
            state = RecorderState.Recording
            startTime = System.currentTimeMillis()
            recordedFrames.clear()
            
            // TODO: Initialize actual audio recording from device microphone
            
            Result.success(Unit)
        } catch (e: Exception) {
            state = RecorderState.Error
            Result.failure(e)
        }
    }
    
    override suspend fun stopRecording(): Result<ByteArray> {
        return try {
            if (state != RecorderState.Recording && state != RecorderState.Paused) {
                throw IllegalStateException("Recorder is not recording")
            }
            
            state = RecorderState.Stopped
            
            // Combine all frames into single ByteArray
            val totalSize = recordedFrames.sumOf { it.data.size }
            val combinedData = ByteArray(totalSize)
            var offset = 0
            recordedFrames.forEach { frame ->
                frame.data.copyInto(combinedData, offset)
                offset += frame.data.size
            }
            
            Result.success(combinedData)
        } catch (e: Exception) {
            state = RecorderState.Error
            Result.failure(e)
        }
    }
    
    override suspend fun pauseRecording(): Result<Unit> {
        return try {
            if (state != RecorderState.Recording) {
                throw IllegalStateException("Recorder is not recording")
            }
            
            state = RecorderState.Paused
            Result.success(Unit)
        } catch (e: Exception) {
            state = RecorderState.Error
            Result.failure(e)
        }
    }
    
    override suspend fun resumeRecording(): Result<Unit> {
        return try {
            if (state != RecorderState.Paused) {
                throw IllegalStateException("Recorder is not paused")
            }
            
            state = RecorderState.Recording
            Result.success(Unit)
        } catch (e: Exception) {
            state = RecorderState.Error
            Result.failure(e)
        }
    }
    
    override fun getAudioStream(): Flow<AudioFrame> {
        return kotlinx.coroutines.flow.emptyFlow()
        // TODO: Implement actual audio stream from microphone
    }
    
    override fun getState(): RecorderState {
        return state
    }
    
    override fun getDuration(): Long {
        return if (state == RecorderState.Recording || state == RecorderState.Paused) {
            System.currentTimeMillis() - startTime
        } else {
            0
        }
    }
    
    override suspend fun release() {
        if (state == RecorderState.Recording || state == RecorderState.Paused) {
            stopRecording()
        }
        state = RecorderState.Idle
        recordedFrames.clear()
        // TODO: Release audio resources
    }
    
    /**
     * Добавляет кадр в буфер записи (для тестирования)
     */
    fun addFrame(frame: AudioFrame) {
        if (state == RecorderState.Recording) {
            recordedFrames.add(frame)
        }
    }
}

/**
 * Слушатель для отслеживания уровня громкости во время записи
 */
interface RecorderAmplitudeListener {
    fun onAmplitude(rms: Float, db: Float)
}

/**
 * Рекордер с поддержкой отслеживания громкости
 */
class AmplitudeAwareAudioRecorder(
    config: RecorderConfig = RecorderConfig()
) : AudioRecorderImpl(config) {
    
    private val amplitudeListeners = mutableListOf<RecorderAmplitudeListener>()
    
    fun addAmplitudeListener(listener: RecorderAmplitudeListener) {
        amplitudeListeners.add(listener)
    }
    
    fun removeAmplitudeListener(listener: RecorderAmplitudeListener) {
        amplitudeListeners.remove(listener)
    }
    
    fun notifyAmplitude(rms: Float, db: Float) {
        amplitudeListeners.forEach { it.onAmplitude(rms, db) }
    }
}
