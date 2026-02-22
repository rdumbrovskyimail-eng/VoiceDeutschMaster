package com.voicedeutsch.master.app.di

import com.voicedeutsch.master.data.local.JsonFactory
import com.voicedeutsch.master.util.NetworkMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { JsonFactory.instance }
    single { NetworkMonitor(androidContext()) }
}

val appModules = listOf(
    appModule,
    dataModule,
    domainModule,
    voiceCoreModule,
    presentationModule,
)