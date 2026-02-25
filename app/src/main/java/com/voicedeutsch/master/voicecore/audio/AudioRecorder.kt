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

    // ИЗМЕНЕНО: добавлен recordingJob вместо Thread
    private var recordingJob: Job? = null

    @Volatile
    var currentAmplitude: Float = 0f
        private set

    private val frameChannel = Channel<ShortArray>(capacity = Channel.UNLIMITED)
    val audioFrameFlow: Flow<ShortArray> = frameChannel.receiveAsFlow()

    // ИЗМЕНЕНО: принимает scope, запускает корутину вместо Thread.
    // Kotlin 2.3 — Thread().start() внутри корутинного приложения = утечка памяти.
    fun start(scope: CoroutineScope? = null) {
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

        // ИЗМЕНЕНО: корутина вместо raw Thread
        if (scope != null) {
            recordingJob = scope.launch(Dispatchers.IO) {
                recordingLoop()
            }
        } else {
            // Fallback для обратной совместимости
            Thread(::recordingLoop, "AudioRecorder-Thread").start()
        }
    }

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

    private fun recordingLoop() {
        val buffer = ShortArray(FRAME_SIZE_SAMPLES)
        while (_isRecording.get()) {
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