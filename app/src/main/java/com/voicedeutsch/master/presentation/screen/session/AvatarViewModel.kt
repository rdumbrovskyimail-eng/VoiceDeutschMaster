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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
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
    private val context: android.content.Context,
) : ViewModel() {

    companion object {
        private const val TAG = "AvatarVM"
    }

    val audioData: StateFlow<AvatarAudioData> = audioAnalyzer.audioData

    val gender: StateFlow<AvatarGender> = avatarRepository.observeGenderChanges()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AvatarGender.FEMALE)

    init {
        voiceCoreEngine.amplitudeFlow
            .conflate()
            .onEach { amp -> audioAnalyzer.onAmplitude(amp) }
            .catch { e -> Log.w(TAG, "synthetic error: ${e.message}") }
            .launchIn(viewModelScope)
    }

    fun startCapture() {
        viewModelScope.launch {
            val maxWaitMs = 15_000L
            val startMs = System.currentTimeMillis()
            while (System.currentTimeMillis() - startMs < maxWaitMs) {
                val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                if (am.activePlaybackConfigurations.isNotEmpty()) break
                kotlinx.coroutines.delay(500L)
            }
            kotlinx.coroutines.delay(1000L)

            val started = try {
                withContext(Dispatchers.IO) { audioCapture.startWithDiscovery(context) }
            } catch (e: Exception) {
                Log.e(TAG, "Visualizer start failed", e)
                false
            }

            if (started) {
                Log.d(TAG, "✅ Visualizer active")
                audioCapture.frames
                    .conflate()
                    .onEach { frame -> audioAnalyzer.onAudioFrame(frame) }
                    .catch { e -> Log.e(TAG, "Frame error", e) }
                    .launchIn(viewModelScope)
            } else {
                Log.w(TAG, "⚠ Visualizer unavailable — synthetic fallback")
            }
        }
    }

    fun triggerHappy() = audioAnalyzer.triggerHappy()

    override fun onCleared() {
        super.onCleared()
        runCatching { audioCapture.stop() }
        audioAnalyzer.reset()
    }
}
