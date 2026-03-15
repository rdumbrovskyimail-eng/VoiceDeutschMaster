package com.voicedeutsch.master.presentation.screen.session

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicedeutsch.master.data.repository.AvatarRepository
import com.voicedeutsch.master.voicecore.audio.AudioOutputCapture
import com.voicedeutsch.master.voicecore.engine.AvatarAudioAnalyzer
import com.voicedeutsch.master.voicecore.engine.AvatarAudioData
import com.voicedeutsch.master.voicecore.engine.AvatarGender
import com.voicedeutsch.master.voicecore.engine.VoiceCoreEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AvatarViewModel(
    private val voiceCoreEngine: VoiceCoreEngine,
    private val avatarRepository: AvatarRepository,
    private val audioAnalyzer: AvatarAudioAnalyzer,
    private val audioCapture: AudioOutputCapture,
) : ViewModel() {

    companion object {
        private const val TAG = "AvatarVM"
    }

    val audioData: StateFlow<AvatarAudioData> = audioAnalyzer.audioData

    val gender: StateFlow<AvatarGender> = avatarRepository.observeGenderChanges()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AvatarGender.FEMALE)

    @Volatile
    private var usingRealAudio = false

    init {
        // CRITICAL: launch on background — Visualizer constructor blocks main thread
        viewModelScope.launch {
            startAudioCapture()
        }
    }

    private suspend fun startAudioCapture() {
        val started = try {
            withContext(Dispatchers.IO) {
                audioCapture.start(audioSessionId = 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Visualizer init crashed: ${e.message}", e)
            false
        }

        if (started) {
            usingRealAudio = true
            Log.d(TAG, "✅ Real audio capture active")

            audioCapture.frames
                .conflate()
                .onEach { frame -> audioAnalyzer.onAudioFrame(frame) }
                .catch { e ->
                    Log.e(TAG, "Audio frame error, fallback: ${e.message}")
                    fallbackToSynthetic()
                }
                .launchIn(viewModelScope)
        } else {
            Log.w(TAG, "⚠ Visualizer unavailable — synthetic fallback")
            fallbackToSynthetic()
        }
    }

    private fun fallbackToSynthetic() {
        usingRealAudio = false
        runCatching { audioCapture.stop() }

        voiceCoreEngine.amplitudeFlow
            .conflate()
            .onEach { amp -> audioAnalyzer.onAmplitude(amp) }
            .catch { e -> Log.w(TAG, "Synthetic amplitude error: ${e.message}") }
            .launchIn(viewModelScope)
    }

    fun triggerHappy() = audioAnalyzer.triggerHappy()

    fun isUsingRealAudio(): Boolean = usingRealAudio

    override fun onCleared() {
        super.onCleared()
        runCatching { audioCapture.stop() }
        audioAnalyzer.reset()
    }
}
