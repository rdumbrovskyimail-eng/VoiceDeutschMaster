package com.voicedeutsch.master.app.di

import kotlinx.serialization.json.Json
import org.koin.dsl.module

val appModules = listOf(appModule, dataModule, domainModule, voiceCoreModule, presentationModule)

val appModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = false
            encodeDefaults = true
            isLenient = true
        }
    }
}