package com.voicedeutsch.master.voicecore.audio

/**
 * Конфигурация для Voice Activity Detection (VAD)
 */
data class VADConfig(
    val silenceThresholdDb: Float = -40f,
    val speechStartThresholdDb: Float = -35f,
    val minSpeechDurationMs: Int = 300,
    val maxSilenceDurationMs: Int = 500,
    val frameSize: Int = 512,
    val modelPath: String? = null
)

/**
 * Результат обнаружения голоса
 */
data class VADResult(
    val hasVoice: Boolean,
    val confidence: Float, // 0.0 - 1.0
    val voiceAmplitude: Float,
    val noiseLevel: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Состояния VAD
 */
enum class VADState {
    Silence,
    SpeechStarting,
    Speaking,
    SpeechEnding,
    Silence_LongPause
}

/**
 * Интерфейс для обнаружения голоса (Voice Activity Detection)
 */
interface VADProcessor {
    
    /**
     * Инициализирует процессор
     */
    suspend fun initialize(): Result<Unit>
    
    /**
     * Обрабатывает аудиокадр и определяет наличие голоса
     */
    fun processFrame(frame: AudioFrame): VADResult
    
    /**
     * Обрабатывает аудиокадр и возвращает состояние
     */
    fun processFrameWithState(frame: AudioFrame): Pair<VADResult, VADState>
    
    /**
     * Получает текущее состояние VAD
     */
    fun getCurrentState(): VADState
    
    /**
     * Устанавливает порог обнаружения (0.0 - 1.0)
     */
    fun setSensitivity(sensitivity: Float)
    
    /**
     * Сбрасывает состояние
     */
    fun reset()
    
    /**
     * Освобождает ресурсы
     */
    suspend fun release()
}

/**
 * Реализация VAD на основе анализа громкости и энергии
 */
class SimpleVADProcessor(
    private val config: VADConfig = VADConfig()
) : VADProcessor {
    
    private var currentState = VADState.Silence
    private var silenceDuration = 0L
    private var speechDuration = 0L
    private var sensitivity = 0.5f
    private var lastFrameTime = 0L
    
    override suspend fun initialize(): Result<Unit> {
        return try {
            reset()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun processFrame(frame: AudioFrame): VADResult {
        val (result, _) = processFrameWithState(frame)
        return result
    }
    
    override fun processFrameWithState(frame: AudioFrame): Pair<VADResult, VADState> {
        val currentTime = System.currentTimeMillis()
        val frameTime = if (lastFrameTime > 0) currentTime - lastFrameTime else 0
        lastFrameTime = currentTime
        
        // Анализируем энергию аудиосигнала
        val (amplitudeDb, noiseLevel) = analyzeFrame(frame)
        
        // Определяем, есть ли голос
        val hasVoice = amplitudeDb > config.silenceThresholdDb
        val confidence = calculateConfidence(amplitudeDb)
        
        // Обновляем состояние
        updateState(hasVoice, frameTime)
        
        val result = VADResult(
            hasVoice = hasVoice,
            confidence = confidence,
            voiceAmplitude = amplitudeDb,
            noiseLevel = noiseLevel
        )
        
        return Pair(result, currentState)
    }
    
    override fun getCurrentState(): VADState = currentState
    
    override fun setSensitivity(sensitivity: Float) {
        require(sensitivity in 0.0f..1.0f) { "Sensitivity must be between 0.0 and 1.0" }
        this.sensitivity = sensitivity
    }
    
    override fun reset() {
        currentState = VADState.Silence
        silenceDuration = 0
        speechDuration = 0
        lastFrameTime = 0
    }
    
    override suspend fun release() {
        reset()
    }
    
    /**
     * Анализирует кадр и получает амплитуду в дБ и уровень шума
     */
    private fun analyzeFrame(frame: AudioFrame): Pair<Float, Float> {
        if (frame.data.isEmpty()) {
            return Pair(Float.NEGATIVE_INFINITY, 0f)
        }
        
        // Рассчитываем RMS (Root Mean Square) энергию
        var sumSquares = 0.0
        var i = 0
        while (i < frame.data.size step 2) {
            val sample = (frame.data[i].toInt() shl 8) or (frame.data[i + 1].toInt() and 0xFF)
            sumSquares += sample * sample
            i += 2
        }
        
        val rms = Math.sqrt(sumSquares / (frame.data.size / 2))
        val amplitudeDb = 20 * Math.log10(rms / 32768.0).toFloat()
        
        // Оценка уровня шума (простой метод)
        val noiseLevel = Math.max(0f, amplitudeDb + 40) / 40 // Нормализуем от -40dB до 0
        
        return Pair(amplitudeDb, noiseLevel)
    }
    
    /**
     * Рассчитывает уверенность в обнаружении голоса
     */
    private fun calculateConfidence(amplitudeDb: Float): Float {
        val threshold = config.silenceThresholdDb
        val confidence = when {
            amplitudeDb < threshold - 10 -> 0.0f
            amplitudeDb < threshold -> (amplitudeDb - (threshold - 10)) / 10 * sensitivity
            amplitudeDb < threshold + 10 -> 0.5f + ((amplitudeDb - threshold) / 10 * 0.5f) * sensitivity
            else -> Math.min(1.0f, sensitivity)
        }
        
        return confidence.coerceIn(0f, 1f)
    }
    
    /**
     * Обновляет состояние VAD на основе текущего фрейма
     */
    private fun updateState(hasVoice: Boolean, frameTime: Long) {
        when {
            hasVoice -> {
                silenceDuration = 0
                speechDuration += frameTime
                
                currentState = when (currentState) {
                    VADState.Silence, VADState.Silence_LongPause -> {
                        if (speechDuration > config.minSpeechDurationMs) {
                            VADState.Speaking
                        } else {
                            VADState.SpeechStarting
                        }
                    }
                    VADState.SpeechStarting -> {
                        if (speechDuration > config.minSpeechDurationMs) {
                            VADState.Speaking
                        } else {
                            VADState.SpeechStarting
                        }
                    }
                    VADState.SpeechEnding -> VADState.Speaking
                    else -> currentState
                }
            }
            else -> {
                speechDuration = 0
                silenceDuration += frameTime
                
                currentState = when (currentState) {
                    VADState.Speaking, VADState.SpeechStarting -> {
                        VADState.SpeechEnding
                    }
                    VADState.SpeechEnding -> {
                        if (silenceDuration > config.maxSilenceDurationMs) {
                            VADState.Silence
                        } else {
                            VADState.SpeechEnding
                        }
                    }
                    VADState.Silence -> {
                        if (silenceDuration > config.maxSilenceDurationMs * 2) {
                            VADState.Silence_LongPause
                        } else {
                            VADState.Silence
                        }
                    }
                    else -> currentState
                }
            }
        }
    }
}

/**
 * VAD в реальном времени с буферизацией
 */
class BufferedVADProcessor(
    private val config: VADConfig = VADConfig()
) : VADProcessor {
    
    private val vadProcessor = SimpleVADProcessor(config)
    private val audioBuffer = mutableListOf<AudioFrame>()
    private var isInitialized = false
    
    override suspend fun initialize(): Result<Unit> {
        return try {
            vadProcessor.initialize()
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun processFrame(frame: AudioFrame): VADResult {
        val (result, _) = processFrameWithState(frame)
        return result
    }
    
    override fun processFrameWithState(frame: AudioFrame): Pair<VADResult, VADState> {
        audioBuffer.add(frame)
        
        // Ограничиваем размер буфера
        if (audioBuffer.size > 10) {
            audioBuffer.removeAt(0)
        }
        
        return vadProcessor.processFrameWithState(frame)
    }
    
    override fun getCurrentState(): VADState {
        return vadProcessor.getCurrentState()
    }
    
    override fun setSensitivity(sensitivity: Float) {
        vadProcessor.setSensitivity(sensitivity)
    }
    
    override fun reset() {
        vadProcessor.reset()
        audioBuffer.clear()
    }
    
    override suspend fun release() {
        vadProcessor.release()
        audioBuffer.clear()
    }
    
    /**
     * Получает буферизованные аудиокадры
     */
    fun getBufferedFrames(): List<AudioFrame> {
        return audioBuffer.toList()
    }
}
