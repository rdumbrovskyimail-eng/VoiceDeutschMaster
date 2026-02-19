package com.voicedeutsch.master.app

import android.app.Application
import com.voicedeutsch.master.app.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application entry point.
 *
 * Responsibilities:
 *  1. Initialise Koin DI with all module graphs.
 *  2. (Optional) Initialise Firebase — uncomment when google-services.json is present.
 *
 * Registered in AndroidManifest.xml as `android:name=".app.VoiceDeutschApp"`.
 */
class VoiceDeutschApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // ── Koin DI ──────────────────────────────────────────────────────────
        startKoin {
            // Use ERROR level in production to avoid logcat noise.
            // Switch to Level.DEBUG during development if injection fails.
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@VoiceDeutschApp)
            modules(appModules)
        }

        // ── Firebase (enable after adding google-services.json) ──────────────
        // FirebaseApp.initializeApp(this)
    }
}