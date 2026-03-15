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

/**
 * Manages avatar animation state.
 *
 * IMPORTANT: Visualizer is NOT started in init{}.
 * It requires RECORD_AUDIO permission to be already granted.
 * Call [startCapture] after permission is confirmed and session starts.
 * Until then, synthetic fallback runs automatically.
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

    @Volatile
    private var usingRealAudio = false

    @Volatile
    private var captureStarted = false

    init {
        // Start with synthetic fallback immediately — safe, no permissions needed
        startSyntheticFallback()
    }

    /**
     * Call this AFTER RECORD_AUDIO permission is granted and session is starting.
     * Attempts to upgrade from synthetic to real Visualizer audio.
     * Safe to call multiple times — only runs once.
     */
    fun startCapture() {
        if (captureStarted) return
        captureStarted = true

        viewModelScope.launch {
            tryStartVisualizer()
        }
    }

    private suspend fun tryStartVisualizer() {
        val started = try {
            withContext(Dispatchers.IO) {
                audioCapture.start(audioSessionId = 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Visualizer failed: ${e.message}", e)
            false
        }

        if (started) {
            usingRealAudio = true
            Log.d(TAG, "✅ Upgraded to real audio capture")

            // Cancel synthetic, switch to real
            audioCapture.frames
                .conflate()
                .onEach { frame -> audioAnalyzer.onAudioFrame(frame) }
                .catch { e ->
                    Log.e(TAG, "Audio frame error: ${e.message}")
                    // Don't restart synthetic — it's already running as base
                }
                .launchIn(viewModelScope)
        } else {
            Log.w(TAG, "⚠ Visualizer unavailable — staying on synthetic")
            // Synthetic already running from init, nothing to do
        }
    }

    /**
     * Synthetic amplitude from VoiceCoreEngine — always works, no permissions.
     */
    private fun startSyntheticFallback() {
        voiceCoreEngine.amplitudeFlow
            .conflate()
            .onEach { amp ->
                // Only use synthetic if real audio is not active
                if (!usingRealAudio) {
                    audioAnalyzer.onAmplitude(amp)
                }
            }
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
