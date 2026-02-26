// app/src/main/java/com/voicedeutsch/master/app/di/VoiceCoreModule.kt
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

    single { AudioPipeline(androidContext()) }

    single { UserContextProvider(get()) }
    single { BookContextProvider(get()) }
    single { ContextBuilder(get(), get(), get()) }

    single { FunctionRegistry }
    single {
        FunctionRouter(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()
        )
    }

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

    single { VoiceSessionManager() }
    factory { SessionHistory() }

    factory {
        GeminiConfig(
            modelName   = "gemini-2.5-flash-native-audio-preview-12-2025",
            voiceName   = "Aoede",
            temperature = 0.8f,
            topP        = 0.95f,
            topK        = 40,
            audioInputFormat  = GeminiConfig.AudioFormat.PCM_16KHZ_16BIT_MONO,
            audioOutputFormat = GeminiConfig.AudioFormat.PCM_24KHZ_16BIT_MONO,
        )
    }

    factory {
        GeminiClient(
            config = get<GeminiConfig>(),
            json   = get(),
        )
    }

    single<VoiceCoreEngine> {
        VoiceCoreEngineImpl(
            contextBuilder        = get(),
            functionRouter        = get(),
            audioPipeline         = get(),
            strategySelector      = get(),
            geminiClient          = get(),
            buildKnowledgeSummary = get(),
            startLearningSession  = get(),
            endLearningSession    = get(),
            networkMonitor        = get(),
            flushKnowledgeSync    = get(), 
        )
    }
}