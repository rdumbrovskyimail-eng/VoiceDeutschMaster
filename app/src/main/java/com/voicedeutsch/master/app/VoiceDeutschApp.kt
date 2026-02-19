package com.voicedeutsch.master.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.voicedeutsch.master.BuildConfig
import com.voicedeutsch.master.app.di.appModules
import com.voicedeutsch.master.util.CrashLogger
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application entry point.
 *
 * Responsibilities:
 *  1. Initialise CrashLogger BEFORE anything else (catches startup crashes).
 *  2. Initialise Koin DI with all module graphs.
 *  3. Initialise Firebase for crash reporting and analytics.
 *
 * Registered in AndroidManifest.xml as `android:name=".app.VoiceDeutschApp"`.
 */
class VoiceDeutschApp : Application() {

    override fun onCreate() {
        // 🔥 КРИТИЧЕСКИ ВАЖНО: Инициализируем CrashLogger ПЕРВЫМ делом
        // ДО вызова super.onCreate() и любой другой инициализации
        initCrashLogger()

        super.onCreate()

        // ── Koin DI ──────────────────────────────────────────────────────────
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@VoiceDeutschApp)
            modules(appModules)
        }

        // ── Firebase ─────────────────────────────────────────────────────────
        initFirebase()
    }

    /**
     * 🔥 Инициализация системы перехвата крашей.
     * Вызывается ДО всего остального — если упадёт Koin, Firebase или что угодно,
     * краш будет сохранён в файл.
     */
    private fun initCrashLogger() {
        try {
            CrashLogger.init(this).apply {
                startLogging()
                cleanOldLogs(keepCount = 20)
            }
            Log.d(TAG, "✅ CrashLogger initialized successfully")
            Log.d(
                TAG,
                "📁 Crash logs location: ${CrashLogger.getInstance()?.getCrashLogDirectory()}",
            )
        } catch (e: Exception) {
            // Даже если инициализация крашлоггера упала — не роняем приложение
            Log.e(TAG, "❌ Failed to init CrashLogger", e)
        }
    }

    /**
     * Initializes Firebase services (Crashlytics, Analytics, Performance).
     *
     * Wrapped in try/catch so builds without google-services.json don't crash.
     */
    private fun initFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Firebase initialized successfully")
            }
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Firebase initialization failed — crash reporting and analytics " +
                    "are disabled. Ensure google-services.json is present in the " +
                    "app/ directory. Error: ${e.message}",
            )
        }
    }

    private companion object {
        const val TAG = "VoiceDeutschApp"
    }
}
