package com.voicedeutsch.master.app.di

import com.voicedeutsch.master.voicecore.audio.AudioPipeline
import com.voicedeutsch.master.voicecore.context.BookContextProvider
import com.voicedeutsch.master.voicecore.context.ContextBuilder
import com.voicedeutsch.master.voicecore.context.SystemPromptBuilder
import com.voicedeutsch.master.voicecore.context.UserContextProvider
import com.voicedeutsch.master.voicecore.engine.GeminiClient
import com.voicedeutsch.master.voicecore.engine.GeminiConfig
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
 *   FunctionRouter(11 params)
 *   ContextBuilder(systemPromptBuilder, userContextProvider, bookContextProvider, functionRouter, json)
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
    single { SystemPromptBuilder() }
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

    // ─── Context builder ──────────────────────────────────────────────────────
    // ContextBuilder(systemPromptBuilder, userContextProvider, bookContextProvider,
    //                functionRouter, json)
    // ⚠️ 5 параметров — добавлен functionRouter для getDeclarations()
    single { ContextBuilder(get(), get(), get(), get(), get()) }

    // ─── Strategy selection ───────────────────────────────────────────────────
    single { StrategySelector() }

    // ─── Gemini configuration ─────────────────────────────────────────────────
    // factory вместо single — ключ читается при каждом обращении,
    // поэтому смена ключа в SettingsScreen подхватится на следующей сессии
    // без перезапуска приложения.
    factory {
        GeminiConfig(
            apiKey = get<com.voicedeutsch.master.domain.repository.SecurityRepository>()
                .getGeminiApiKey(),
        )
    }

    // ─── Gemini Live WebSocket client ─────────────────────────────────────────
    // factory — пересоздаётся вместе с GeminiConfig при каждой новой сессии.
    // HttpClient и Json — single из DataModule, переиспользуются.
    factory {
        GeminiClient(
            config     = get(), // GeminiConfig (factory)
            httpClient = get(), // io.ktor.client.HttpClient (single, DataModule)
            json       = get(), // kotlinx.serialization.json.Json (single, DataModule)
        )
    }

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
            geminiClient          = get(), // factory — свежий экземпляр
        )
    }
}