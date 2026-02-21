package com.voicedeutsch.master.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.voicedeutsch.master.BuildConfig
import com.voicedeutsch.master.app.di.appModules
import com.voicedeutsch.master.util.AppLogger
import com.voicedeutsch.master.util.CrashLogger
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import com.voicedeutsch.master.app.worker.WorkManagerInitializer

/**
 * Application entry point.
 *
 * Порядок инициализации (критически важен):
 *  1. [CrashLogger] — ПЕРВЫМ, до super(). Перехватывает крэши с самого старта.
 *  2. [AppLogger]   — ВТОРЫМ, сразу после. Начинает запись logcat в кольцевой буфер.
 *  3. super.onCreate()
 *  4. Koin DI
 *  5. Firebase
 */
class VoiceDeutschApp : Application() {

    override fun onCreate() {
        // 🔥 1. CrashLogger — перехватываем крэши ДО всего остального
        initCrashLogger()

        // 📡 2. AppLogger — фоновый перехват logcat сразу после CrashLogger
        initAppLogger()

        super.onCreate()

        // ── 3. Koin DI ───────────────────────────────────────────────────────
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@VoiceDeutschApp)
            modules(appModules)
        }

        // ── 4. Firebase ──────────────────────────────────────────────────────
        initFirebase()

        // ── 5. WorkManager ───────────────────────────────────────────────────
        WorkManagerInitializer.initialize(this)
    }

    /**
     * 🔥 Инициализация перехватчика крэшей.
     * Вызывается ДО всего остального — если упадёт Koin, Firebase или что угодно,
     * краш будет сохранён в файл.
     */
    private fun initCrashLogger() {
        try {
            CrashLogger.init(this).apply {
                startLogging()
                cleanOldLogs(keepCount = 20)
            }
            Log.d(TAG, "✅ CrashLogger initialized")
            Log.d(TAG, "📁 Crash logs: ${CrashLogger.getInstance()?.getCrashLogDirectory()}")
        } catch (e: Exception) {
            // Даже если инициализация крашлоггера упала — не роняем приложение
            Log.e(TAG, "❌ Failed to init CrashLogger", e)
        }
    }

    /**
     * 📡 Инициализация и запуск фонового логгера.
     * AppLogger читает logcat этого процесса в реальном времени и хранит
     * последние BUFFER_CAPACITY строк в памяти. При крэше буфер сбрасывается на диск.
     */
    private fun initAppLogger() {
        try {
            AppLogger.init(this).start()
            Log.d(TAG, "✅ AppLogger started (background logcat capture active)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to init AppLogger", e)
        }
    }

    /**
     * Инициализирует Firebase (Crashlytics, Analytics).
     * Обёрнуто в try/catch — билды без google-services.json не падают.
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
