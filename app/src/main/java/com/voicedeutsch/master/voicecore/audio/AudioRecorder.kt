package com.voicedeutsch.master.voicecore.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.voicedeutsch.master.util.AudioUtils
import com.voicedeutsch.master.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Записывает PCM-аудио с микрофона и отдаёт фреймы через [audioFrameFlow].
 *
 * ✅ FIX (Kotlin 2.3 / Android 16):
 * Thread().start() внутри корутинного приложения — утечка памяти и риск краша.
 * Android 16 может убить неконтролируемый поток без предупреждения.
 *
 * БЫЛО: Thread(::recordingLoop, "AudioRecorder-Thread").start()
 *       + fallback совместимости через scope != null
 * СТАЛО: запись строго через корутину scope.launch(Dispatchers.IO).
 *        recordingLoop() удалён — логика встроена в корутину.
 *        scope обязателен — нет смысла запускать запись без контроля жизненного цикла.
 */
class AudioRecorder {

    companion object {
        const val SAMPLE_RATE = Constants.AUDIO_INPUT_SAMPLE_RATE // 16 000 Hz
        const val FRAME_SIZE_SAMPLES = 320 // 20ms at 16kHz

        private val MIN_BUFFER_SIZE: Int = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(FRAME_SIZE_SAMPLES * 2 * 4)
    }

    private var audioRecord: AudioRecord? = null
    private val _isRecording = AtomicBoolean(false)
    private var recordingJob: Job? = null

    @Volatile
    var currentAmplitude: Float = 0f
        private set

    private val frameChannel = Channel<ShortArray>(capacity = Channel.UNLIMITED)
    val audioFrameFlow: Flow<ShortArray> = frameChannel.receiveAsFlow()

    /**
     * Запускает запись в переданном [scope] на [Dispatchers.IO].
     *
     * @param scope — корутинный scope из AudioPipeline / VoiceCoreEngineImpl.
     *   Отмена scope автоматически останавливает запись.
     */
    fun start(scope: CoroutineScope) {
        if (_isRecording.getAndSet(true)) return

        val ar = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            MIN_BUFFER_SIZE,
        )
        check(ar.state == AudioRecord.STATE_INITIALIZED) {
            "AudioRecord failed to initialize. Check RECORD_AUDIO permission."
        }
        audioRecord = ar
        ar.startRecording()

        recordingJob = scope.launch(Dispatchers.IO) {
            val buffer = ShortArray(FRAME_SIZE_SAMPLES)
            while (isActive && _isRecording.get()) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                if (read > 0) {
                    val frame = buffer.copyOf(read)
                    currentAmplitude = AudioUtils.calculateRMS(frame) / Short.MAX_VALUE.toFloat()
                    frameChannel.trySend(frame)
                }
            }
            audioRecord?.stop()
        }
    }

    // УДАЛЕНО: Thread(::recordingLoop, "AudioRecorder-Thread").start()
    // УДАЛЕНО: fallback scope != null / scope == null
    // УДАЛЕНО: private fun recordingLoop()

    fun stop() {
        _isRecording.set(false)
        recordingJob?.cancel()
        recordingJob = null
    }

    fun release() {
        stop()
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                stop()
                release()
            }
        }
        audioRecord = null
        frameChannel.close()
    }
}
