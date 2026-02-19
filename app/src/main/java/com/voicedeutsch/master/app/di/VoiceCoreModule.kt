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
 *   GeminiConfig(securityRepository)
 *   GeminiClient(config, httpClient, json)
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
    // Объявляем ДО ContextBuilder — он зависит от FunctionRouter.getDeclarations()
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
    // API-ключ читается из SecurityRepository (EncryptedSharedPreferences).
    // При смене ключа пользователем нужно пересоздать через destroy() + initialize().
    single {
        val securityRepository =
            get<com.voicedeutsch.master.domain.repository.SecurityRepository>()
        GeminiConfig(
            apiKey = securityRepository.getGeminiApiKey(),
        )
    }

    // ─── Gemini Live WebSocket client ─────────────────────────────────────────
    // HttpClient и Json объявлены в DataModule и разделяются как single.
    single {
        GeminiClient(
            config     = get(), // GeminiConfig
            httpClient = get(), // io.ktor.client.HttpClient из DataModule
            json       = get(), // kotlinx.serialization.json.Json из DataModule
        )
    }

    // ─── Engine (the heart of the system) ────────────────────────────────────
    // VoiceCoreEngineImpl(contextBuilder, functionRouter, audioPipeline,
    //                      strategySelector, buildKnowledgeSummary,
    //                      startLearningSession, endLearningSession,
    //                      geminiClient)
    single<VoiceCoreEngine> {
        VoiceCoreEngineImpl(
            contextBuilder      = get(),
            functionRouter      = get(),
            audioPipeline       = get(),
            strategySelector    = get(),
            buildKnowledgeSummary  = get(),
            startLearningSession   = get(),
            endLearningSession     = get(),
            geminiClient        = get(),
        )
    }
}