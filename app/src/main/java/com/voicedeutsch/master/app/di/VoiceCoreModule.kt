package com.voicedeutsch.master.app.di

import com.voicedeutsch.master.domain.repository.SecurityRepository
import com.voicedeutsch.master.voicecore.audio.AudioPipeline
import com.voicedeutsch.master.voicecore.context.BookContextProvider
import com.voicedeutsch.master.voicecore.context.ContextBuilder
import com.voicedeutsch.master.voicecore.context.UserContextProvider
import com.voicedeutsch.master.voicecore.engine.GeminiClient
import com.voicedeutsch.master.voicecore.engine.GeminiConfig
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

/**
 * VoiceCore layer module — all components are [single] because they maintain
 * live audio state and WebSocket connections that must not be duplicated.
 *
 * Dependency graph:
 *   AudioPipeline(context)
 *   UserContextProvider(json)
 *   BookContextProvider(bookRepository)
 *   FunctionRouter(11 params)
 *   ContextBuilder(userContextProvider, bookContextProvider, functionRouter, json)
 *   StrategySelector()
 *   GeminiConfig(securityRepository)         ← factory: свежий ключ на каждой сессии
 *   GeminiClient(config, httpClient, json)   ← factory: пересоздаётся вместе с конфигом
 *   VoiceCoreEngineImpl(contextBuilder, functionRouter, audioPipeline,
 *                        strategySelector, buildKnowledgeSummary,
 *                        startLearningSession, endLearningSession,
 *                        geminiClient)
 */
val voiceCoreModule = module {

    // ─── Audio pipeline ───────────────────────────────────────────────────────
    single { AudioPipeline(androidContext()) }

    // ─── Context builders ─────────────────────────────────────────────────────
    // MasterPrompt — pure object, не инжектируется (нет зависимостей)
    // UserContextProvider(json)
    single { UserContextProvider(get()) }
    // BookContextProvider(bookRepository)
    single { BookContextProvider(get()) }

    // ─── Function routing ─────────────────────────────────────────────────────
    // Объявляем ДО ContextBuilder — он зависит от FunctionRouter.getDeclarations().
    // FunctionRouter(updateWordKnowledge, updateRuleKnowledge, getWordsForRepetition,
    //                getWeakPoints, getCurrentLesson, advanceBookProgress,
    //                updateUserLevel, getUserStatistics, recordPronunciation,
    //                knowledgeRepository, json)
    single {
        FunctionRouter(
            get(), // updateWordKnowledge
            get(), // updateRuleKnowledge
            get(), // getWordsForRepetition
            get(), // getWeakPoints
            get(), // getCurrentLesson
            get(), // advanceBookProgress
            get(), // updateUserLevel
            get(), // getUserStatistics
            get(), // recordPronunciation
            get(), // knowledgeRepository
            get(), // json
        )
    }
    // FunctionRegistry — центральный реестр всех функций (object)
    single { FunctionRegistry }

    // ─── Context builder ──────────────────────────────────────────────────────
    // ContextBuilder(userContextProvider, bookContextProvider, functionRouter, json)
    // ⚠️ 4 параметра — SystemPromptBuilder удалён, MasterPrompt вызывается напрямую
    single { ContextBuilder(get(), get(), get(), get()) }

    // ─── Strategy selection ───────────────────────────────────────────────────
    single { StrategySelector() }

    // ─── Session management ───────────────────────────────────────────────────
    // VoiceSessionManager — управляет жизненным циклом сессии
    single { VoiceSessionManager() }
    // SessionHistory — in-memory история диалога (новый экземпляр на каждую сессию)
    factory { SessionHistory() }

    // ─── Strategy handlers ────────────────────────────────────────────────────
    factory { LinearBookStrategy() }
    factory { RepetitionStrategy() }
    factory { FreePracticeStrategy() }
    factory { GapFillingStrategy() }
    factory { PronunciationStrategy() }
    factory { GrammarStrategy() }
    factory { VocabularyStrategy() }
    factory { ListeningStrategy() }
    factory { AssessmentStrategy() }

    // ─── Engine (the heart of the system) ────────────────────────────────────
    // VoiceCoreEngineImpl(contextBuilder, functionRouter, audioPipeline,
    //                      strategySelector, buildKnowledgeSummary,
    //                      startLearningSession, endLearningSession,
    //                      geminiClient)
    single<VoiceCoreEngine> {
        VoiceCoreEngineImpl(
            contextBuilder        = get(),
            functionRouter        = get(),
            audioPipeline         = get(),
            strategySelector      = get(),
            buildKnowledgeSummary = get(),
            startLearningSession  = get(),
            endLearningSession    = get(),
            securityRepository    = get(),
            httpClient            = get(),
            json                  = get(),
            networkMonitor        = get(), // ← NetworkMonitor из AppModule
        )
    }
}
