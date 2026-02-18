package com.voicedeutsch.master.app

import android.app.Application
import com.voicedeutsch.master.app.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class VoiceDeutschApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@VoiceDeutschApp)
            modules(appModules)
        }
    }
}