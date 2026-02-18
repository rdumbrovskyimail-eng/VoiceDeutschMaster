package com.voicedeutsch.master.app.di

import org.koin.dsl.module

/** Заглушка — компоненты голосового ядра будут добавлены в сессиях 5-6 */
val voiceCoreModule = module {
    // ─── Audio (сессия 5) ───────────────────────────────────────
    // AudioRecorder
    // AudioPlayer
    // VADProcessor
    // AudioPipeline

    // ─── Gemini (сессия 5) ──────────────────────────────────────
    // GeminiConfig
    // GeminiClient

    // ─── Context & Strategy (сессия 6) ──────────────────────────
    // ContextBuilder
    // StrategySelector

    // ─── Functions (сессия 6) ───────────────────────────────────
    // FunctionRouter

    // ─── Engine (сессия 6) ──────────────────────────────────────
    // VoiceCoreEngine → VoiceCoreEngineImpl
}