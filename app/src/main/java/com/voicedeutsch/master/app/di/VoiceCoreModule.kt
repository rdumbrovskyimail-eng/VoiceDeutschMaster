package com.voicedeutsch.master.app.di

import com.voicedeutsch.master.voicecore.audio.AudioPipeline
import com.voicedeutsch.master.voicecore.context.BookContextProvider
import com.voicedeutsch.master.voicecore.context.ContextBuilder
import com.voicedeutsch.master.voicecore.context.UserContextProvider
import com.voicedeutsch.master.voicecore.engine.VoiceCoreEngine
import com.voicedeutsch.master.voicecore.engine.VoiceCoreEngineImpl
import com.voicedeutsch.master.voicecore.functions.FunctionRouter
import com.voicedeutsch.master.voicecore.functions.FunctionRegistry
import com.voicedeutsch.master.voicecore.session.SessionHistory
import com.voicedeutsch.master.voicecore.session.VoiceSessionManager
import com.voicedeutsch.master.voicecore.strategy.StrategySelector
import com.voicedeutsch.master.voicecore.strategy.LinearBookStrategy
import com.voicedeutsch.master.voicecore.strategy.RepetitionStrategy
import com.voicedeutsch.master.voicecore.strategy.FreePracticeStrategy
import com.voicedeutsch.master.voicecore.strategy.GapFillingStrategy
import com.voicedeutsch.master.voicecore.strategy.PronunciationStrategy
import com.voicedeutsch.master.voicecore.strategy.GrammarStrategy
import com.voicedeutsch.master.voicecore.strategy.VocabularyStrategy
import com.voicedeutsch.master.voicecore.strategy.AssessmentStrategy
import com.voicedeutsch.master.voicecore.strategy.ListeningStrategy
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val voiceCoreModule = module {
    single { AudioPipeline(androidContext()) }
    single { UserContextProvider(get()) }
    single { BookContextProvider(get()) }
    single {
        FunctionRouter(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()
        )
    }
    single { FunctionRegistry }
    single { ContextBuilder(get(), get(), get(), get()) }
    single { StrategySelector() }
    single { VoiceSessionManager() }
    factory { SessionHistory() }

    factory { LinearBookStrategy() }
    factory { RepetitionStrategy() }
    factory { FreePracticeStrategy() }
    factory { GapFillingStrategy() }
    factory { PronunciationStrategy() }
    factory { GrammarStrategy() }
    factory { VocabularyStrategy() }
    factory { ListeningStrategy() }
    factory { AssessmentStrategy() }

    single<VoiceCoreEngine> {
        VoiceCoreEngineImpl(
            contextBuilder        = get(),
            functionRouter        = get(),
            audioPipeline         = get(),
            strategySelector      = get(),
            buildKnowledgeSummary = get(),
            startLearningSession  = get(),
            endLearningSession    = get(),
            httpClient            = get(), // Передаем Ktor клиент
            json                  = get(), // Передаем Json парсер
            networkMonitor        = get(),
        )
    }
}