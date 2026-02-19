package com.voicedeutsch.master.app.di

import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * Root list of all Koin modules — passed to [startKoin] in [VoiceDeutschApp].
 * Order matters: appModule must be first (Json is a shared dependency).
 */
val appModules = listOf(
    appModule,
    dataModule,
    domainModule,
    voiceCoreModule,
    presentationModule,
)

val appModule = module {
    /**
     * Shared [Json] instance used across all layers:
     * DataStore serialisation, Room converters, Gemini payloads.
     */
    single {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = false
            encodeDefaults = true
            isLenient = true
            coerceInputValues = true
        }
    }
}