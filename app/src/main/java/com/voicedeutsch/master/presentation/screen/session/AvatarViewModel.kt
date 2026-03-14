package com.voicedeutsch.master.presentation.screen.session

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

class AvatarViewModel(
    private val voiceCoreEngine: VoiceCoreEngine,
    private val avatarRepository: AvatarRepository,
    private val audioAnalyzer: AvatarAudioAnalyzer,
) : ViewModel() {

    val audioData: StateFlow<AvatarAudioData> = audioAnalyzer.audioData

    val gender: StateFlow<AvatarGender> = avatarRepository.observeGenderChanges()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AvatarGender.FEMALE)

    init {
        // Subscribe to amplitude flow and feed the analyzer
        voiceCoreEngine.amplitudeFlow
            .conflate()
            .onEach { amp -> audioAnalyzer.onAmplitude(amp) }
            .catch { e -> android.util.Log.w("AvatarVM", "amplitude error: ${e.message}") }
            .launchIn(viewModelScope)
    }

    fun triggerHappy() = audioAnalyzer.triggerHappy()
}