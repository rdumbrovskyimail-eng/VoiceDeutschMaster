package com.voicedeutsch.master.app

import android.app.Application
import android.content.Context
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

    // ✅ attachBaseContext — вызывается ДО onCreate, это самая ранняя точка
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        // 🔥 CrashLogger стартует здесь — перехватит даже краш в Koin/Firebase init
        try {
            CrashLogger.init(base).apply {
                cleanOldLogs(keepCount = 20)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ CrashLogger init failed", e)
        }
    }

    override fun onCreate() {
        // 📡 AppLogger — после CrashLogger, но до всего остального
        initAppLogger()

        super.onCreate()

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@VoiceDeutschApp)
            modules(appModules)
        }

        initFirebase()
        WorkManagerInitializer.initialize(this)

        // ✅ Копируем последний краш в Downloads (если есть)
        CrashLogger.getInstance()?.copyLatestCrashToDownloads(this)
    }

    private fun initAppLogger() {
        try {
            AppLogger.init(this).start()
            Log.d(TAG, "✅ AppLogger started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ AppLogger failed", e)
        }
    }

    private fun initFirebase() {
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase init failed: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "VoiceDeutschApp"
    }
}
