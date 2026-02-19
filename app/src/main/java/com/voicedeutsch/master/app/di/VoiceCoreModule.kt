package com.voicedeutsch.master.app.di

import com.voicedeutsch.master.voicecore.audio.AudioPipeline
import com.voicedeutsch.master.voicecore.context.ContextBuilder
import com.voicedeutsch.master.voicecore.context.BookContextProvider
import com.voicedeutsch.master.voicecore.context.SystemPromptBuilder
import com.voicedeutsch.master.voicecore.context.UserContextProvider
import com.voicedeutsch.master.voicecore.engine.VoiceCoreEngine
import com.voicedeutsch.master.voicecore.engine.VoiceCoreEngineImpl
import com.voicedeutsch.master.voicecore.functions.FunctionRouter
import com.voicedeutsch.master.voicecore.strategy.StrategySelector
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * VoiceCore layer module — all components are [single] because they maintain
 * live audio state and WebSocket connections that must not be duplicated.
 *
 * Dependency graph:
 *   AudioPipeline(context)
 *   SystemPromptBuilder()
 *   UserContextProvider(json)
 *   BookContextProvider(bookRepository)
 *   ContextBuilder(systemPromptBuilder, userContextProvider, bookContextProvider, json)
 *   FunctionRouter(11 params)
 *   StrategySelector()
 *   VoiceCoreEngineImpl(contextBuilder, functionRouter, audioPipeline,
 *                        strategySelector, buildKnowledgeSummary,
 *                        startLearningSession, endLearningSession)
 */
val voiceCoreModule = module {

    // ─── Audio pipeline ───────────────────────────────────────────────────────
    // AudioPipeline creates AudioRecorder, AudioPlayer, VADProcessor internally.
    single { AudioPipeline(androidContext()) }

    // ─── Context builders ─────────────────────────────────────────────────────
    single { SystemPromptBuilder() }
    // UserContextProvider(json)
    single { UserContextProvider(get()) }
    // BookContextProvider(bookRepository)
    single { BookContextProvider(get()) }
    // ContextBuilder(systemPromptBuilder, userContextProvider, bookContextProvider, json)
    single { ContextBuilder(get(), get(), get(), get()) }

    // ─── Function routing ─────────────────────────────────────────────────────
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

    // ─── Strategy selection ───────────────────────────────────────────────────
    single { StrategySelector() }

    // ─── Engine (the heart of the system) ────────────────────────────────────
    // VoiceCoreEngineImpl(contextBuilder, functionRouter, audioPipeline,
    //                      strategySelector, buildKnowledgeSummary,
    //                      startLearningSession, endLearningSession)
    single<VoiceCoreEngine> {
        VoiceCoreEngineImpl(
            get(), // contextBuilder
            get(), // functionRouter
            get(), // audioPipeline
            get(), // strategySelector
            get(), // buildKnowledgeSummary
            get(), // startLearningSession
            get(), // endLearningSession
        )
    }
}