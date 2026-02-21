package com.voicedeutsch.master.app.di

import com.voicedeutsch.master.data.local.JsonFactory
import org.koin.dsl.module

/**
 * Shared [Json] instance used across all layers:
 * DataStore serialisation, Room converters, Gemini payloads.
 *
 * FIX: appModule declared BEFORE appModules to avoid forward-reference error.
 *      Previously `appModules` referenced `appModule` before it was defined,
 *      causing "Variable 'appModule' must be initialized" at compile time.
 */
val appModule = module {
    single { JsonFactory.instance }
}

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
