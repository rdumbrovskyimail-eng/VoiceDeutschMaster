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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Manages avatar animation state by bridging audio capture to the behavior engine.
 *
 * Data flow:
 *   AudioOutputCapture (Visualizer, ~20Hz)
 *     → AudioFrame (waveform + FFT)
 *       → AvatarAudioAnalyzer.onAudioFrame()
 *         → SpectralFeatureExtractor → ProsodicFeatures
 *           → AvatarAudioData (amplitude, pitch, smile, emphasis, emotion...)
 *             → AvatarBehaviorEngine.update() [in AvatarSceneView]
 *               → BoneController + MorphTargetHelper
 *
 * Fallback: if Visualizer fails to start, falls back to synthetic amplitude
 * from VoiceCoreEngine.amplitudeFlow (same as v1 behavior).
 */
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

    /**
     * True when using real Visualizer data, false when using synthetic fallback.
     */
    @Volatile
    private var usingRealAudio = false

    init {
        startAudioCapture()
    }

    /**
     * Attempts to start real audio capture via Visualizer.
     * Falls back to synthetic amplitude if Visualizer fails.
     */
    private fun startAudioCapture() {
        val started = audioCapture.start(audioSessionId = 0)

        if (started) {
            usingRealAudio = true
            Log.d(TAG, "✅ Real audio capture active — using Visualizer spectral data")

            // Subscribe to Visualizer frames → spectral analyzer
            audioCapture.frames
                .conflate()
                .onEach { frame -> audioAnalyzer.onAudioFrame(frame) }
                .catch { e ->
                    Log.e(TAG, "Audio frame error, falling back to synthetic: ${e.message}")
                    fallbackToSynthetic()
                }
                .launchIn(viewModelScope)
        } else {
            Log.w(TAG, "⚠ Visualizer not available — falling back to synthetic amplitude")
            fallbackToSynthetic()
        }
    }

    /**
     * Fallback: use synthetic amplitude from VoiceCoreEngine (v1 behavior).
     * This provides basic jaw movement but no spectral features.
     */
    private fun fallbackToSynthetic() {
        usingRealAudio = false
        audioCapture.stop()

        voiceCoreEngine.amplitudeFlow
            .conflate()
            .onEach { amp -> audioAnalyzer.onAmplitude(amp) }
            .catch { e -> Log.w(TAG, "Synthetic amplitude error: ${e.message}") }
            .launchIn(viewModelScope)
    }

    fun triggerHappy() = audioAnalyzer.triggerHappy()

    /**
     * Returns true if avatar is driven by real spectral audio data.
     * Useful for debugging / UI indicators.
     */
    fun isUsingRealAudio(): Boolean = usingRealAudio

    override fun onCleared() {
        super.onCleared()
        audioCapture.stop()
        audioAnalyzer.reset()
    }
}