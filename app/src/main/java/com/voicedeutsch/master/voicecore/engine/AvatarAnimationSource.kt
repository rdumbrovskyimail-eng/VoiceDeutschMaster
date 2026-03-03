package com.voicedeutsch.master.voicecore.engine

/**
 * Generates simulated amplitude values for avatar animation.
 *
 * Since Firebase AI SDK manages microphone and playback internally via
 * startAudioConversation/stopAudioConversation, we cannot access real
 * audio amplitude. This generator creates natural-looking animation
 * values based on session state.
 *
 * Usage in VoiceCoreEngineImpl:
 *   override val amplitudeFlow: Flow<Float> = avatarAnimationSource.amplitudeFlow
 *
 * Created: 03.03.2026
 */

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.math.sin
import kotlin.random.Random

class AvatarAnimationSource {

    /**
     * Emits amplitude values (0f..1f) for avatar mouth animation.
     *
     * Pattern: natural speech-like oscillation with random variation.
     * The avatar's VirtualAvatar composable uses these values to
     * animate mouth opening mixed with its own speakPhase animation.
     *
     * Emit rate: ~30 fps (33ms) — sufficient for smooth Canvas animation.
     */
    fun createActiveFlow(): Flow<Float> = flow {
        var phase = 0f
        val baseFreq = 0.15f
        var targetAmp = 0.6f
        var currentAmp = 0f
        var frameCount = 0

        while (currentCoroutineContext().isActive) {
            phase += baseFreq
            if (phase > 2f * Math.PI.toFloat()) {
                phase -= 2f * Math.PI.toFloat()
            }

            // Every ~20 frames, pick a new random target amplitude
            // to simulate natural speech rhythm (louder/quieter moments)
            frameCount++
            if (frameCount % 20 == 0) {
                targetAmp = Random.nextFloat() * 0.6f + 0.3f // 0.3..0.9
            }

            // Smooth interpolation toward target
            currentAmp += (targetAmp - currentAmp) * 0.15f

            // Add sine wave variation for natural feel
            val sineComponent = (sin(phase) * 0.5f + 0.5f) * 0.4f
            val noise = Random.nextFloat() * 0.1f

            val amplitude = (currentAmp * 0.5f + sineComponent + noise).coerceIn(0f, 1f)
            emit(amplitude)

            delay(33L) // ~30 fps
        }
    }

    /**
     * Emits idle breathing amplitude (very subtle, for resting state).
     */
    fun createIdleFlow(): Flow<Float> = flow {
        var phase = 0f
        while (currentCoroutineContext().isActive) {
            phase += 0.03f
            if (phase > 2f * Math.PI.toFloat()) {
                phase -= 2f * Math.PI.toFloat()
            }
            val amplitude = (sin(phase) * 0.05f + 0.05f).coerceIn(0f, 1f)
            emit(amplitude)
            delay(50L)
        }
    }
}
