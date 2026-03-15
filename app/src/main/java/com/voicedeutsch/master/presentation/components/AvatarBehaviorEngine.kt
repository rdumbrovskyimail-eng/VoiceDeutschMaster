package com.voicedeutsch.master.presentation.components

import com.voicedeutsch.master.voicecore.engine.AvatarAudioData
import com.voicedeutsch.master.voicecore.engine.EmotionState
import kotlin.math.*
import kotlin.random.Random

/**
 * Мозг аватара — генерирует реалистичное поведение на каждый кадр (30fps).
 *
 * Архитектура:
 *  1. Определяет текущее состояние по аудиоданным
 *  2. Вычисляет целевые углы костей и веса morph targets
 *  3. Применяет EMA-сглаживание ко всем значениям
 *  4. Возвращает AvatarFrame готовый к применению
 *
 * Состояния:
 *  IDLE            — молчание, руки сомкнуты, дыхание
 *  SPEAKING_SOFT   — тихая речь (amp < 0.28)
 *  SPEAKING_ACTIVE — активная речь (amp >= 0.28), жестикуляция
 *  THINKING        — пауза после речи, голова наклонена, рука у подбородка
 *  HAPPY           — позитивная реакция
 */
class AvatarBehaviorEngine {

    // ── Состояние ──────────────────────────────────────────────────────────

    enum class AvatarState { IDLE, SPEAKING_SOFT, SPEAKING_ACTIVE, THINKING, HAPPY }

    data class BoneAngles(
        val pitch: Float = 0f,  // наклон вперёд/назад
        val yaw:   Float = 0f,  // поворот влево/вправо
        val roll:  Float = 0f,  // наклон в сторону
    )

    data class AvatarFrame(
        // Кости
        val head:         BoneAngles = BoneAngles(),
        val neck:         BoneAngles = BoneAngles(),
        val spine:        BoneAngles = BoneAngles(),
        val spine1:       BoneAngles = BoneAngles(),
        val spine2:       BoneAngles = BoneAngles(),
        val leftArm:      BoneAngles = BoneAngles(),
        val leftForeArm:  BoneAngles = BoneAngles(),
        val leftHand:     BoneAngles = BoneAngles(),
        val rightArm:     BoneAngles = BoneAngles(),
        val rightForeArm: BoneAngles = BoneAngles(),
        val rightHand:    BoneAngles = BoneAngles(),
        val leftShoulder: BoneAngles = BoneAngles(),
        val rightShoulder:BoneAngles = BoneAngles(),
        // Morph targets (0..1)
        val morphs: Map<String, Float> = emptyMap(),
    )

    // ── Внутренние переменные ──────────────────────────────────────────────

    private var time = 0f
    private var state = AvatarState.IDLE
    private var lastSpeakingMs = System.currentTimeMillis()
    private var silenceStartMs = 0L
    private var gestureTimer = 0f
    private var gesturePhase = 0  // 0=нет, 1=левая рука, 2=правая рука
    private var gestureBlend = 0f // 0..1, плавный переход жеста
    private var nodPhase = 0f
    private var lastAmp = 0f
    private var visemePhase = 0f
    private var blinkValue = 0f   // 0=открыты, 1=закрыты
    private var nextBlinkMs = System.currentTimeMillis() + Random.nextLong(2000, 5000)

    // EMA-сглаженные целевые значения костей
    private var sHead = BoneAngles()
    private var sNeck = BoneAngles()
    private var sSpine = BoneAngles()
    private var sSpine1 = BoneAngles()
    private var sSpine2 = BoneAngles()
    private var sLArm = BoneAngles()
    private var sLForeArm = BoneAngles()
    private var sLHand = BoneAngles()
    private var sRArm = BoneAngles()
    private var sRForeArm = BoneAngles()
    private var sRHand = BoneAngles()
    private var sLShoulder = BoneAngles()
    private var sRShoulder = BoneAngles()

    private val smoothMorphs = mutableMapOf<String, Float>()

    // ── Утилиты ────────────────────────────────────────────────────────────

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

    private fun BoneAngles.lerp(target: BoneAngles, t: Float) = BoneAngles(
        lerp(pitch, target.pitch, t),
        lerp(yaw,   target.yaw,   t),
        lerp(roll,  target.roll,  t),
    )

    private fun smoothMorph(key: String, target: Float, alpha: Float): Float {
        val cur = smoothMorphs[key] ?: 0f
        val next = lerp(cur, target, alpha)
        smoothMorphs[key] = next
        return next
    }

    // ── Главный метод обновления (вызывать ~30fps) ─────────────────────────

    fun update(audio: AvatarAudioData, dt: Float = 0.033f): AvatarFrame {
        time += dt
        val now = System.currentTimeMillis()
        val amp = audio.amplitude.coerceIn(0f, 1f)
        val speaking = audio.isSpeaking
        val emotion = audio.emotion

        // ── Определяем состояние ──────────────────────────────────────────
        if (speaking) {
            lastSpeakingMs = now
            silenceStartMs = 0L
            state = if (amp >= 0.28f) AvatarState.SPEAKING_ACTIVE
                    else              AvatarState.SPEAKING_SOFT
        } else {
            if (silenceStartMs == 0L) silenceStartMs = now
            val silenceMs = now - silenceStartMs
            state = when {
                emotion == EmotionState.HAPPY                              -> AvatarState.HAPPY
                silenceMs > 1200L && (now - lastSpeakingMs) < 8000L       -> AvatarState.THINKING
                silenceMs > 4000L                                          -> AvatarState.IDLE
                else                                                        -> AvatarState.IDLE
            }
        }

        // ── Жест: обновляем таймер ────────────────────────────────────────
        gestureTimer -= dt
        when {
            state == AvatarState.SPEAKING_ACTIVE && gestureTimer <= 0f -> {
                val interval = Random.nextFloat() * 7f + 6f // 6..13 сек
                gestureTimer = interval
                gesturePhase = if (Random.nextBoolean()) 1 else 2
                gestureBlend = 0f
            }
            state != AvatarState.SPEAKING_ACTIVE -> {
                gesturePhase = 0
                gestureBlend = lerp(gestureBlend, 0f, 0.08f)
            }
        }
        if (gesturePhase > 0) gestureBlend = lerp(gestureBlend, 1f, 0.06f)

        // ── Кивок по спайку амплитуды ─────────────────────────────────────
        val spike = (amp - lastAmp).coerceAtLeast(0f)
        if (spike > 0.18f && speaking) nodPhase = 0f
        nodPhase += dt
        val nodDelta = if (nodPhase < 0.4f) sin(nodPhase * PI.toFloat() / 0.4f) * spike * 7f else 0f

        // ── Дыхание ────────────────────────────────────────────────────────
        val breath = sin(time * 2f * PI.toFloat() * 0.22f) // 0.22 Hz

        // ── Viseme фаза (цикл гласных при речи) ───────────────────────────
        visemePhase += dt * (if (speaking) 0.18f else 0f)

        // ── Моргание ──────────────────────────────────────────────────────
        if (now >= nextBlinkMs) {
            blinkValue = 1f
            nextBlinkMs = now + Random.nextLong(
                if (speaking) 2000L else 3500L,
                if (speaking) 3500L else 6000L
            )
        }
        if (blinkValue > 0f) {
            blinkValue = (blinkValue - dt * 8f).coerceAtLeast(0f)
        }

        // ── Целевые значения по состоянию ─────────────────────────────────

        val tHead: BoneAngles
        val tNeck: BoneAngles
        val tSpine: BoneAngles
        val tSpine1: BoneAngles
        val tSpine2: BoneAngles
        val tLShoulder: BoneAngles
        val tRShoulder: BoneAngles

        // Целевые морфы
        val mJaw:      Float
        val mMouth:    Float
        val mSmile:    Float
        val mBrowInner:Float
        val mBrowDown: Float
        val mEyeLookUp:Float
        val mPucker:   Float

        when (state) {
            AvatarState.IDLE -> {
                tHead    = BoneAngles(
                    pitch = sin(time * 0.28f) * 1.2f,
                    yaw   = sin(time * 0.19f) * 1.8f,
                    roll  = sin(time * 0.13f) * 0.8f
                )
                tNeck    = BoneAngles(pitch = sin(time * 0.24f) * 0.6f)
                tSpine   = BoneAngles(pitch = breath * 1.2f)
                tSpine1  = BoneAngles(pitch = breath * 0.8f)
                tSpine2  = BoneAngles()
                tLShoulder = BoneAngles(pitch = breath * 0.4f)
                tRShoulder = BoneAngles(pitch = breath * 0.4f)
                mJaw = 0f; mMouth = 0f; mSmile = 0.05f
                mBrowInner = 0f; mBrowDown = 0f; mEyeLookUp = 0f; mPucker = 0f
            }
            AvatarState.SPEAKING_SOFT -> {
                tHead = BoneAngles(
                    pitch = sin(time * 0.9f) * 2.5f - nodDelta,
                    yaw   = sin(time * 0.6f) * 4f + sin(time * 0.23f) * 1.5f,
                    roll  = sin(time * 0.4f) * 1.2f
                )
                tNeck    = BoneAngles(pitch = 1.5f + sin(time * 0.7f) * 0.8f)
                tSpine   = BoneAngles(pitch = breath * 1.0f + 1.5f)
                tSpine1  = BoneAngles(pitch = 2.5f + breath * 0.5f)
                tSpine2  = BoneAngles(pitch = 1.0f)
                tLShoulder = BoneAngles(pitch = breath * 0.3f)
                tRShoulder = BoneAngles(pitch = breath * 0.3f)
                mJaw = amp * 0.5f; mMouth = amp * 0.35f; mSmile = 0.1f
                mBrowInner = amp * 0.15f; mBrowDown = 0f; mEyeLookUp = 0f; mPucker = 0f
            }
            AvatarState.SPEAKING_ACTIVE -> {
                tHead = BoneAngles(
                    pitch = sin(time * 1.3f) * 4f - nodDelta * 1.5f,
                    yaw   = sin(time * 0.8f) * 7f + sin(time * 0.3f) * 2.5f,
                    roll  = sin(time * 0.5f) * 2f
                )
                tNeck    = BoneAngles(pitch = 2f + sin(time * 1.0f) * 1.2f)
                tSpine   = BoneAngles(pitch = breath * 0.8f + 2f + amp * 1.5f)
                tSpine1  = BoneAngles(pitch = 3.5f + amp * 2f)
                tSpine2  = BoneAngles(pitch = 1.5f + amp * 1f)
                tLShoulder = BoneAngles(pitch = amp * 2f + breath * 0.3f)
                tRShoulder = BoneAngles(pitch = amp * 2f + breath * 0.3f)
                mJaw = amp * 0.65f; mMouth = amp * 0.45f; mSmile = 0.08f
                mBrowInner = amp * 0.25f + sin(time * 2f) * 0.1f
                mBrowDown = 0f; mEyeLookUp = 0f; mPucker = 0f
            }
            AvatarState.THINKING -> {
                tHead = BoneAngles(
                    pitch = -3f + sin(time * 0.2f) * 0.8f,
                    yaw   = sin(time * 0.15f) * 2f,
                    roll  = 8f + sin(time * 0.18f) * 1f
                )
                tNeck    = BoneAngles(pitch = -1f, roll = 3f)
                tSpine   = BoneAngles(pitch = breath * 1f - 1.5f)
                tSpine1  = BoneAngles(pitch = -2f)
                tSpine2  = BoneAngles(pitch = -1f)
                tLShoulder = BoneAngles()
                tRShoulder = BoneAngles(pitch = -2f)
                mJaw = 0f; mMouth = 0f; mSmile = 0f
                mBrowInner = 0.6f; mBrowDown = 0.2f
                mEyeLookUp = 0.4f; mPucker = 0.1f
            }
            AvatarState.HAPPY -> {
                val bob = sin(time * 3.5f) * 2f
                tHead    = BoneAngles(pitch = bob, yaw = sin(time * 0.8f) * 3f)
                tNeck    = BoneAngles(pitch = 2f + bob * 0.5f)
                tSpine   = BoneAngles(pitch = breath * 1.2f + 2f)
                tSpine1  = BoneAngles(pitch = 2f)
                tSpine2  = BoneAngles()
                tLShoulder = BoneAngles(pitch = 2f + bob * 0.5f)
                tRShoulder = BoneAngles(pitch = 2f + bob * 0.5f)
                mJaw = 0f; mMouth = 0.1f; mSmile = 0.8f
                mBrowInner = 0f; mBrowDown = 0f; mEyeLookUp = 0f; mPucker = 0f
            }
        }

        // ── Руки: базовая поза "сомкнуты" (CLASPED) ──────────────────────
        // Используется в IDLE и SPEAKING_SOFT, плавно уходит при жестах
        val claspBlend = when (state) {
            AvatarState.IDLE          -> 1f - gestureBlend
            AvatarState.SPEAKING_SOFT -> (1f - gestureBlend) * 0.9f
            AvatarState.THINKING      -> 0f // руки в позе "думание"
            else                      -> 1f - gestureBlend
        }

        // Clasped pose: руки подняты вперёд и соединены по центру
        val claspLArm     = BoneAngles(pitch = 42f, yaw = -18f, roll =  8f)
        val claspLForeArm = BoneAngles(pitch = 48f, yaw =   0f, roll =  5f)
        val claspLHand    = BoneAngles(pitch =  0f, yaw = -12f, roll =  0f)
        val claspRArm     = BoneAngles(pitch = 42f, yaw =  18f, roll = -8f)
        val claspRForeArm = BoneAngles(pitch = 48f, yaw =   0f, roll = -5f)
        val claspRHand    = BoneAngles(pitch =  0f, yaw =  12f, roll =  0f)

        // Gesture: левая рука объясняет
        val gestLArm     = BoneAngles(pitch = -25f, yaw = -25f, roll = -10f)
        val gestLForeArm = BoneAngles(pitch =  35f, yaw =   0f, roll =   0f)
        val gestLHand    = BoneAngles(pitch = -15f, yaw = -5f,  roll =   0f)

        // Gesture: правая рука объясняет
        val gestRArm     = BoneAngles(pitch = -25f, yaw =  25f, roll =  10f)
        val gestRForeArm = BoneAngles(pitch =  35f, yaw =   0f, roll =   0f)
        val gestRHand    = BoneAngles(pitch = -15f, yaw =  5f,  roll =   0f)

        // Think: правая рука поднята к подбородку
        val thinkRArm     = BoneAngles(pitch =  35f, yaw =  18f, roll = -8f)
        val thinkRForeArm = BoneAngles(pitch =  85f, yaw =   0f, roll =  0f)
        val thinkRHand    = BoneAngles(pitch = -25f, yaw =   0f, roll =  0f)
        val thinkLArm     = BoneAngles(pitch =  15f, yaw = -10f, roll =  5f)
        val thinkLForeArm = BoneAngles(pitch =  20f, yaw =   0f, roll =  0f)

        // Финальные целевые значения рук с учётом blending
        fun blend(a: BoneAngles, b: BoneAngles, t: Float) = a.lerp(b, t)

        val restL = BoneAngles() // T-pose: руки вниз
        val restR = BoneAngles()

        val tLArm: BoneAngles
        val tLForeArm: BoneAngles
        val tLHand: BoneAngles
        val tRArm: BoneAngles
        val tRForeArm: BoneAngles
        val tRHand: BoneAngles

        when (state) {
            AvatarState.THINKING -> {
                tLArm     = thinkLArm
                tLForeArm = thinkLForeArm
                tLHand    = restL
                tRArm     = thinkRArm
                tRForeArm = thinkRForeArm
                tRHand    = thinkRHand
            }
            AvatarState.HAPPY -> {
                tLArm     = blend(restL, BoneAngles(pitch = -15f, yaw = -10f), 0.6f)
                tLForeArm = blend(restL, BoneAngles(pitch =  10f), 0.5f)
                tLHand    = restL
                tRArm     = blend(restR, BoneAngles(pitch = -15f, yaw =  10f), 0.6f)
                tRForeArm = blend(restR, BoneAngles(pitch =  10f), 0.5f)
                tRHand    = restR
            }
            else -> {
                // Базовая поза + жест поверх
                val leftBase  = blend(restL, claspLArm, claspBlend)
                val lfBase    = blend(restL, claspLForeArm, claspBlend)
                val lhBase    = blend(restL, claspLHand, claspBlend)
                val rightBase = blend(restR, claspRArm, claspBlend)
                val rfBase    = blend(restR, claspRForeArm, claspBlend)
                val rhBase    = blend(restR, claspRHand, claspBlend)

                when (gesturePhase) {
                    1 -> { // Левая рука жестикулирует
                        tLArm     = blend(leftBase, gestLArm, gestureBlend)
                        tLForeArm = blend(lfBase, gestLForeArm, gestureBlend)
                        tLHand    = blend(lhBase, gestLHand, gestureBlend)
                        tRArm = rightBase; tRForeArm = rfBase; tRHand = rhBase
                    }
                    2 -> { // Правая рука жестикулирует
                        tRArm     = blend(rightBase, gestRArm, gestureBlend)
                        tRForeArm = blend(rfBase, gestRForeArm, gestureBlend)
                        tRHand    = blend(rhBase, gestRHand, gestureBlend)
                        tLArm = leftBase; tLForeArm = lfBase; tLHand = lhBase
                    }
                    else -> {
                        tLArm = leftBase; tLForeArm = lfBase; tLHand = lhBase
                        tRArm = rightBase; tRForeArm = rfBase; tRHand = rhBase
                    }
                }
            }
        }

        // ── EMA-сглаживание всех костей ──────────────────────────────────

        val hSpeed  = if (speaking) 0.10f else 0.07f
        val bSpeed  = 0.06f
        val aSpeed  = if (gesturePhase > 0) 0.08f else 0.10f

        sHead      = sHead.lerp(tHead,     hSpeed)
        sNeck      = sNeck.lerp(tNeck,     hSpeed * 0.7f)
        sSpine     = sSpine.lerp(tSpine,   bSpeed)
        sSpine1    = sSpine1.lerp(tSpine1, bSpeed)
        sSpine2    = sSpine2.lerp(tSpine2, bSpeed)
        sLShoulder = sLShoulder.lerp(tLShoulder, bSpeed)
        sRShoulder = sRShoulder.lerp(tRShoulder, bSpeed)
        sLArm      = sLArm.lerp(tLArm,     aSpeed)
        sLForeArm  = sLForeArm.lerp(tLForeArm, aSpeed)
        sLHand     = sLHand.lerp(tLHand,   aSpeed)
        sRArm      = sRArm.lerp(tRArm,     aSpeed)
        sRForeArm  = sRForeArm.lerp(tRForeArm, aSpeed)
        sRHand     = sRHand.lerp(tRHand,   aSpeed)

        // ── Морфы ─────────────────────────────────────────────────────────

        val mAlpha = if (speaking) 0.28f else 0.15f

        // Viseme: просто cycling aa/O при речи — упрощённый lip sync
        val visAa = if (speaking) (sin(visemePhase) * 0.5f + 0.5f) * amp * 0.7f else 0f
        val visO  = if (speaking) (cos(visemePhase * 0.7f) * 0.4f + 0.35f) * amp * 0.4f else 0f
        val visSil = if (!speaking) 0.35f else 0f

        val morphs = buildMap {
            put("jawOpen",       smoothMorph("jawOpen",       mJaw,       mAlpha))
            put("mouthOpen",     smoothMorph("mouthOpen",     mMouth,     mAlpha))
            put("mouthSmile",    smoothMorph("mouthSmile",    mSmile,     0.06f))
            put("browInnerUp",   smoothMorph("browInnerUp",   mBrowInner, 0.07f))
            put("browDownLeft",  smoothMorph("browDownLeft",  mBrowDown,  0.07f))
            put("browDownRight", smoothMorph("browDownRight", mBrowDown,  0.07f))
            put("eyesLookUp",    smoothMorph("eyesLookUp",    mEyeLookUp, 0.05f))
            put("mouthPucker",   smoothMorph("mouthPucker",   mPucker,    0.06f))
            put("viseme_aa",     smoothMorph("viseme_aa",     visAa,      0.25f))
            put("viseme_O",      smoothMorph("viseme_O",      visO,       0.22f))
            put("viseme_sil",    smoothMorph("viseme_sil",    visSil,     0.12f))
            put("eyeBlinkLeft",  smoothMorph("eyeBlinkLeft",  blinkValue, 0.5f))
            put("eyeBlinkRight", smoothMorph("eyeBlinkRight", blinkValue, 0.5f))
            // Squint при улыбке
            val squint = smoothMorphs["mouthSmile"] ?: 0f
            put("eyeSquintLeft",  smoothMorph("eyeSquintLeft",  squint * 0.4f, 0.06f))
            put("eyeSquintRight", smoothMorph("eyeSquintRight", squint * 0.4f, 0.06f))
        }

        lastAmp = amp

        return AvatarFrame(
            head          = sHead,
            neck          = sNeck,
            spine         = sSpine,
            spine1        = sSpine1,
            spine2        = sSpine2,
            leftShoulder  = sLShoulder,
            rightShoulder = sRShoulder,
            leftArm       = sLArm,
            leftForeArm   = sLForeArm,
            leftHand      = sLHand,
            rightArm      = sRArm,
            rightForeArm  = sRForeArm,
            rightHand     = sRHand,
            morphs        = morphs,
        )
    }

    fun reset() {
        time = 0f
        state = AvatarState.IDLE
        gestureTimer = 0f
        gesturePhase = 0
        gestureBlend = 0f
        smoothMorphs.clear()
        sHead = AvatarBehaviorEngine.BoneAngles()
        sNeck = AvatarBehaviorEngine.BoneAngles()
        sSpine = AvatarBehaviorEngine.BoneAngles()
        sSpine1 = AvatarBehaviorEngine.BoneAngles()
        sSpine2 = AvatarBehaviorEngine.BoneAngles()
        sLShoulder = AvatarBehaviorEngine.BoneAngles()
        sRShoulder = AvatarBehaviorEngine.BoneAngles()
        sLArm = AvatarBehaviorEngine.BoneAngles()
        sLForeArm = AvatarBehaviorEngine.BoneAngles()
        sLHand = AvatarBehaviorEngine.BoneAngles()
        sRArm = AvatarBehaviorEngine.BoneAngles()
        sRForeArm = AvatarBehaviorEngine.BoneAngles()
        sRHand = AvatarBehaviorEngine.BoneAngles()
    }
}