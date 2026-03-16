package com.voicedeutsch.master.presentation.screen.session

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicedeutsch.master.data.repository.AvatarRepository
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
 * Manages avatar animation state.
 *
 * Subscribes to real amplitude from VoiceCoreEngine (fed via GeminiClient PCM data).
 */
class AvatarViewModel(
    private val voiceCoreEngine: VoiceCoreEngine,
    private val avatarRepository: AvatarRepository,
    private val audioAnalyzer: AvatarAudioAnalyzer,
) : ViewModel() {

    companion object { private const val TAG = "AvatarVM" }

    val audioData: StateFlow<AvatarAudioData> = audioAnalyzer.audioData

    val gender: StateFlow<AvatarGender> = avatarRepository.observeGenderChanges()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AvatarGender.FEMALE)

    init {
        // Подписываемся на реальную амплитуду из PCM (через GeminiClient → VoiceCoreEngine)
        voiceCoreEngine.amplitudeFlow
            .conflate()
            .onEach { amp -> audioAnalyzer.onAmplitude(amp) }
            .catch { e -> Log.w(TAG, "amplitudeFlow error: ${e.message}") }
            .launchIn(viewModelScope)
    }

    fun triggerHappy() = audioAnalyzer.triggerHappy()

    override fun onCleared() {
        super.onCleared()
        audioAnalyzer.reset()
    }
}
