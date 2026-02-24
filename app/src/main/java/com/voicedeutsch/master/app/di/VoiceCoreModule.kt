package com.voicedeutsch.master.app.di

import com.voicedeutsch.master.voicecore.audio.AudioPipeline
import com.voicedeutsch.master.voicecore.context.BookContextProvider
import com.voicedeutsch.master.voicecore.context.ContextBuilder
import com.voicedeutsch.master.voicecore.context.UserContextProvider
import com.voicedeutsch.master.voicecore.engine.GeminiClient
import com.voicedeutsch.master.voicecore.engine.GeminiConfig
import com.voicedeutsch.master.voicecore.engine.VoiceCoreEngine
import com.voicedeutsch.master.voicecore.engine.VoiceCoreEngineImpl
import com.voicedeutsch.master.voicecore.functions.FunctionRegistry
import com.voicedeutsch.master.voicecore.functions.FunctionRouter
import com.voicedeutsch.master.voicecore.session.SessionHistory
import com.voicedeutsch.master.voicecore.session.VoiceSessionManager
import com.voicedeutsch.master.voicecore.strategy.AssessmentStrategy
import com.voicedeutsch.master.voicecore.strategy.FreePracticeStrategy
import com.voicedeutsch.master.voicecore.strategy.GapFillingStrategy
import com.voicedeutsch.master.voicecore.strategy.GrammarStrategy
import com.voicedeutsch.master.voicecore.strategy.LinearBookStrategy
import com.voicedeutsch.master.voicecore.strategy.ListeningStrategy
import com.voicedeutsch.master.voicecore.strategy.PronunciationStrategy
import com.voicedeutsch.master.voicecore.strategy.RepetitionStrategy
import com.voicedeutsch.master.voicecore.strategy.StrategySelector
import com.voicedeutsch.master.voicecore.strategy.VocabularyStrategy
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val voiceCoreModule = module {

    // ─── Audio ───────────────────────────────────────────────────────────────
    single { AudioPipeline(androidContext()) }

    // ─── Context providers ────────────────────────────────────────────────────
    single { UserContextProvider(get()) }
    single { BookContextProvider(get()) }
    single { ContextBuilder(get(), get(), get(), get()) }

    // ─── Functions ────────────────────────────────────────────────────────────
    single { FunctionRegistry }
    single {
        FunctionRouter(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()
        )
    }

    // ─── Strategy ─────────────────────────────────────────────────────────────
    single { StrategySelector() }

    factory { LinearBookStrategy() }
    factory { RepetitionStrategy() }
    factory { FreePracticeStrategy() }
    factory { GapFillingStrategy() }
    factory { PronunciationStrategy() }
    factory { GrammarStrategy() }
    factory { VocabularyStrategy() }
    factory { ListeningStrategy() }
    factory { AssessmentStrategy() }

    // ─── Session ──────────────────────────────────────────────────────────────
    single { VoiceSessionManager() }
    factory { SessionHistory() }

    // ─── Gemini (firebase-ai) ─────────────────────────────────────────────────
    // GeminiConfig: параметры модели — вынесены отдельно для удобства изменения
    // без пересоздания GeminiClient (например, через Remote Config в будущем).
    //
    // factory (не single): новый GeminiConfig/GeminiClient на каждую сессию —
    // гарантирует чистое состояние LiveSession между сессиями.
    // Если GeminiClient держит соединение между сессиями — смените на single.
    factory {
        GeminiConfig(
            // Актуальная модель на февраль 2026: Native Audio + Thinking + Live API.
            // Обновите через Firebase Remote Config без перевыпуска APK.
            modelName   = "gemini-2.5-flash-native-audio-preview-12-2025",
            voiceName   = "Aoede",        // Естественный немецкий акцент
            temperature = 0.8f,
            topP        = 0.95f,
            topK        = 40,
            // ✅ ИСПРАВЛЕНО: используем enum AudioFormat из GeminiConfig,
            // а не AudioConfig object (который содержит только константы).
            // Аудио вход: PCM 16-bit, 16 kHz, mono (совпадает с AudioRecorder)
            // Аудио выход: PCM 16-bit, 24 kHz, mono (AudioPlayer ресемплирует при необходимости)
            audioInputFormat  = GeminiConfig.AudioFormat.PCM_16KHZ_16BIT_MONO,
            audioOutputFormat = GeminiConfig.AudioFormat.PCM_24KHZ_16BIT_MONO,
        )
    }

    factory {
        GeminiClient(
            config = get<GeminiConfig>(),
            json   = get(),
            // ✅ EphemeralTokenService УДАЛЁН:
            // App Check + firebase-ai SDK управляют авторизацией прозрачно.
            // HttpClient для WebSocket УДАЛЁН:
            // firebase-ai SDK управляет соединением внутри LiveSession.
        )
    }

    // ─── VoiceCoreEngine ──────────────────────────────────────────────────────
    single<VoiceCoreEngine> {
        VoiceCoreEngineImpl(
            contextBuilder        = get(),
            functionRouter        = get(),
            audioPipeline         = get(),
            strategySelector      = get(),
            geminiClient          = get(),          // ✅ Прямой GeminiClient (firebase-ai)
            buildKnowledgeSummary = get(),
            startLearningSession  = get(),
            endLearningSession    = get(),
            networkMonitor        = get(),
            // httpClient         УДАЛЁН — не нужен для firebase-ai
            // ephemeralTokenService УДАЛЁН — заменён App Check
        )
    }
}